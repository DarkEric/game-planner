package ru.ambryo.gameplannerback.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ambryo.gameplannerback.dto.AdminUserDto;
import ru.ambryo.gameplannerback.dto.ResetPasswordResponse;
import ru.ambryo.gameplannerback.entity.*;
import ru.ambryo.gameplannerback.repository.*;

import java.security.SecureRandom;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminService {
    
    private static final Logger logger = LoggerFactory.getLogger(AdminService.class);
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private PasswordResetService passwordResetService;
    
    @Autowired
    private AsyncTelegramNotificationService asyncTelegramNotificationService;
    
    @Autowired
    private CampaignPlayerRepository campaignPlayerRepository;
    
    @Autowired
    private GameRepository gameRepository;
    
    @Autowired
    private CampaignRepository campaignRepository;
    
    @Autowired
    private CampaignInviteRepository campaignInviteRepository;
    
    @Autowired
    private InviteRepository inviteRepository;
    
    @Autowired
    private GameNotificationRepository gameNotificationRepository;
    
    @Autowired
    private UserNotificationSettingsRepository userNotificationSettingsRepository;
    
    /**
     * Проверяет, является ли пользователь администратором
     */
    public boolean isAdmin(User user) {
        return user != null && user.getIsAdmin() != null && user.getIsAdmin();
    }
    
    /**
     * Получает список всех пользователей для админ-панели
     */
    @Transactional(readOnly = true)
    public List<AdminUserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToAdminUserDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Получает количество администраторов в системе
     */
    @Transactional(readOnly = true)
    public long getAdminCount() {
        return userRepository.countByIsAdminTrue();
    }
    
    /**
     * Назначает права администратора пользователю
     */
    @Transactional
    public void grantAdminRights(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        
        if (user.getIsAdmin()) {
            throw new RuntimeException("Пользователь уже является администратором");
        }
        
        user.setIsAdmin(true);
        userRepository.save(user);
        logger.info("Admin rights granted to user: {} (ID: {})", user.getUsername(), user.getId());
    }
    
    /**
     * Отзывает права администратора у пользователя
     */
    @Transactional
    public void revokeAdminRights(Long userId, Long currentAdminId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        
        if (!user.getIsAdmin()) {
            throw new RuntimeException("Пользователь не является администратором");
        }
        
        // Проверка: администратор не может отозвать права у самого себя
        if (user.getId().equals(currentAdminId)) {
            throw new RuntimeException("Вы не можете отозвать права администратора у самого себя");
        }
        
        // Проверка минимального количества администраторов
        long adminCount = getAdminCount();
        if (adminCount <= 1) {
            throw new RuntimeException("Невозможно отозвать права: в системе должен остаться минимум один администратор");
        }
        
        user.setIsAdmin(false);
        userRepository.save(user);
        logger.info("Admin rights revoked from user: {} (ID: {})", user.getUsername(), user.getId());
    }
    
    /**
     * Сбрасывает пароль пользователя администратором
     */
    @Transactional
    public ResetPasswordResponse resetUserPassword(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        
        // Генерация временного пароля
        String temporaryPassword = generateTemporaryPassword();
        logger.info("Generated temporary password for user {} (ID: {}), length: {}", 
                user.getUsername(), userId, temporaryPassword.length());
        
        // Кодирование пароля
        String encodedPassword = passwordEncoder.encode(temporaryPassword);
        logger.debug("Password encoded successfully for user: {}", user.getUsername());
        
        // Установка нового пароля
        user.setPassword(encodedPassword);
        User savedUser = userRepository.save(user);
        
        // Проверка, что пароль сохранился правильно
        User verifyUser = userRepository.findById(userId).orElse(null);
        if (verifyUser != null) {
            boolean passwordMatches = passwordEncoder.matches(temporaryPassword, verifyUser.getPassword());
            logger.info("Password verification after save for user {} (ID: {}): {}", 
                    user.getUsername(), userId, passwordMatches ? "SUCCESS" : "FAILED");
            if (!passwordMatches) {
                logger.error("CRITICAL: Password verification failed after save for user {} (ID: {})", 
                        user.getUsername(), userId);
            }
        }
        
        boolean sentViaTelegram = false;
        
        if (user.getTelegramSubscribed() != null && user.getTelegramSubscribed() 
                && user.getTelegramChatId() != null) {
            String message = "🔐 <b>Сброс пароля администратором</b>\n\n" +
                "Ваш пароль был сброшен администратором.\n\n" +
                "Новый пароль: <code>" + temporaryPassword + "</code>\n\n" +
                "Рекомендуется изменить пароль после входа в систему.";
            
            asyncTelegramNotificationService.sendPersonalMessageAsync(user.getTelegramChatId(), message);
            sentViaTelegram = true;
            logger.info("Temporary password send scheduled via Telegram to user: {}", user.getUsername());
        }
        
        ResetPasswordResponse response = new ResetPasswordResponse();
        response.setSentViaTelegram(sentViaTelegram);
        
        if (sentViaTelegram) {
            response.setMessage("Новый пароль отправлен пользователю в Telegram");
            response.setTemporaryPassword(null);
        } else {
            response.setMessage("Новый пароль сгенерирован");
            response.setTemporaryPassword(temporaryPassword);
            logger.info("Temporary password returned in response for user: {} (ID: {}), password length: {}", 
                    user.getUsername(), userId, temporaryPassword != null ? temporaryPassword.length() : 0);
        }
        
        return response;
    }
    
    /**
     * Генерирует безопасный временный пароль (12 символов, только a-zA-Z0-9)
     * Фиксированная длина для избежания проблем с передачей
     */
    private String generateTemporaryPassword() {
        SecureRandom random = new SecureRandom();
        // Используем только безопасные символы, которые точно передаются через JSON
        // Исключаем похожие символы: 0/O, 1/I/l для избежания путаницы
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
        int length = 12; // Фиксированная длина для удобства
        StringBuilder password = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        String generatedPassword = password.toString();
        logger.debug("Generated temporary password: {} (length: {})", generatedPassword, generatedPassword.length());
        return generatedPassword;
    }
    
    /**
     * Удаляет пользователя с подтверждением паролем администратора
     * Требует прав администратора
     */
    @Transactional
    public void deleteUser(Long userId, Long currentAdminId, String adminPassword) {
        // Проверяем пароль администратора
        User admin = userRepository.findById(currentAdminId)
                .orElseThrow(() -> new RuntimeException("Администратор не найден"));
        
        if (adminPassword == null || adminPassword.trim().isEmpty()) {
            throw new RuntimeException("Пароль не может быть пустым");
        }
        
        if (!passwordEncoder.matches(adminPassword, admin.getPassword())) {
            throw new RuntimeException("Неверный пароль");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        
        // Проверка: нельзя удалить самого себя
        if (user.getId().equals(currentAdminId)) {
            throw new RuntimeException("Вы не можете удалить самого себя");
        }
        
        // Проверка: нельзя удалить системного пользователя
        if ("system".equals(user.getUsername())) {
            throw new RuntimeException("Нельзя удалить системного пользователя");
        }
        
        // Проверка: нельзя удалить последнего администратора
        if (user.getIsAdmin() != null && user.getIsAdmin()) {
            long adminCount = getAdminCount();
            if (adminCount <= 1) {
                throw new RuntimeException("Невозможно удалить пользователя: в системе должен остаться минимум один администратор");
            }
        }
        
        // Получаем системного пользователя для передачи владения
        User systemUser = userRepository.findByUsername("system")
                .orElseThrow(() -> new RuntimeException("Системный пользователь не найден"));
        
        // Удаляем связи с CampaignPlayer
        List<CampaignPlayer> campaignPlayers = campaignPlayerRepository.findAll().stream()
                .filter(cp -> cp.getPlayer().getId().equals(userId))
                .collect(Collectors.toList());
        campaignPlayerRepository.deleteAll(campaignPlayers);
        logger.info("Deleted {} campaign player entries for user: {} (ID: {})", 
                campaignPlayers.size(), user.getUsername(), user.getId());
        
        // Обрабатываем игры: передаем создателя системному пользователю, удаляем из участников
        List<Game> gamesCreated = gameRepository.findGamesCreatedByUser(userId);
        for (Game game : gamesCreated) {
            game.setCreator(systemUser);
            gameRepository.save(game);
        }
        logger.info("Transferred {} games to system user for deleted user: {} (ID: {})", 
                gamesCreated.size(), user.getUsername(), user.getId());
        
        // Удаляем пользователя из участников игр
        List<Game> allGames = gameRepository.findAll();
        for (Game game : allGames) {
            if (game.getParticipants().removeIf(p -> p.getId().equals(userId))) {
                gameRepository.save(game);
            }
        }
        
        // Обрабатываем кампании: передаем создателя системному пользователю
        List<Campaign> campaignsCreated = campaignRepository.findByCreator(user);
        for (Campaign campaign : campaignsCreated) {
            campaign.setCreator(systemUser);
            campaignRepository.save(campaign);
        }
        logger.info("Transferred {} campaigns to system user for deleted user: {} (ID: {})", 
                campaignsCreated.size(), user.getUsername(), user.getId());
        
        // Удаляем приглашения в кампании
        List<CampaignInvite> campaignInvites = campaignInviteRepository.findAll().stream()
                .filter(ci -> ci.getInvitedUser().getId().equals(userId))
                .collect(Collectors.toList());
        campaignInviteRepository.deleteAll(campaignInvites);
        logger.info("Deleted {} campaign invites for user: {} (ID: {})", 
                campaignInvites.size(), user.getUsername(), user.getId());
        
        // Обрабатываем инвайты: передаем создателя системному пользователю, очищаем usedBy
        List<Invite> invitesCreated = inviteRepository.findByCreatedBy(user);
        for (Invite invite : invitesCreated) {
            invite.setCreatedBy(systemUser);
            inviteRepository.save(invite);
        }
        logger.info("Transferred {} invites to system user for deleted user: {} (ID: {})", 
                invitesCreated.size(), user.getUsername(), user.getId());
        
        // Очищаем usedBy в инвайтах (если пользователь использовал инвайт)
        List<Invite> allInvites = inviteRepository.findAll();
        for (Invite invite : allInvites) {
            if (invite.getUsedBy() != null && invite.getUsedBy().getId().equals(userId)) {
                invite.setUsedBy(null);
                inviteRepository.save(invite);
            }
        }
        
        // Удаляем уведомления игр
        List<GameNotification> gameNotifications = gameNotificationRepository.findAll().stream()
                .filter(gn -> gn.getUser() != null && gn.getUser().getId().equals(userId))
                .collect(Collectors.toList());
        gameNotificationRepository.deleteAll(gameNotifications);
        logger.info("Deleted {} game notifications for user: {} (ID: {})", 
                gameNotifications.size(), user.getUsername(), user.getId());
        
        // Удаляем настройки уведомлений
        userNotificationSettingsRepository.findByUser(user).ifPresent(settings -> {
            userNotificationSettingsRepository.delete(settings);
            logger.info("Deleted notification settings for user: {} (ID: {})", 
                    user.getUsername(), user.getId());
        });
        
        // TimeSlot удаляются каскадно через CASCADE в БД
        
        // Удаляем пользователя
        userRepository.delete(user);
        logger.info("User deleted: {} (ID: {})", user.getUsername(), user.getId());
    }
    
    /**
     * Конвертирует User в AdminUserDto
     */
    private AdminUserDto convertToAdminUserDto(User user) {
        AdminUserDto dto = new AdminUserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setIsAdmin(user.getIsAdmin() != null ? user.getIsAdmin() : false);
        dto.setTelegramSubscribed(user.getTelegramSubscribed() != null ? user.getTelegramSubscribed() : false);
        
        // createdAt пока не используется (можно добавить поле в User entity позже)
        dto.setCreatedAt(null);
        
        return dto;
    }
}
