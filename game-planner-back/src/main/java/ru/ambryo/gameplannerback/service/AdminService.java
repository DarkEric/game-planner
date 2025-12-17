package ru.ambryo.gameplannerback.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ambryo.gameplannerback.dto.AdminUserDto;
import ru.ambryo.gameplannerback.dto.ResetPasswordResponse;
import ru.ambryo.gameplannerback.entity.User;
import ru.ambryo.gameplannerback.repository.UserRepository;

import java.security.SecureRandom;
import java.time.Instant;
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
    private TelegramNotificationService telegramNotificationService;
    
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
        
        // Установка нового пароля
        user.setPassword(passwordEncoder.encode(temporaryPassword));
        userRepository.save(user);
        
        boolean sentViaTelegram = false;
        
        // Попытка отправить через Telegram
        if (user.getTelegramSubscribed() != null && user.getTelegramSubscribed() 
                && user.getTelegramChatId() != null) {
            try {
                StringBuilder message = new StringBuilder();
                message.append("🔐 <b>Сброс пароля администратором</b>\n\n");
                message.append("Ваш пароль был сброшен администратором.\n\n");
                message.append("Новый пароль: <code>").append(temporaryPassword).append("</code>\n\n");
                message.append("Рекомендуется изменить пароль после входа в систему.");
                
                telegramNotificationService.sendPersonalMessage(user.getTelegramChatId(), message.toString());
                sentViaTelegram = true;
                logger.info("Temporary password sent via Telegram to user: {}", user.getUsername());
            } catch (Exception e) {
                logger.error("Failed to send temporary password via Telegram to user: {}", user.getUsername(), e);
            }
        }
        
        ResetPasswordResponse response = new ResetPasswordResponse();
        response.setSentViaTelegram(sentViaTelegram);
        
        if (sentViaTelegram) {
            response.setMessage("Новый пароль отправлен пользователю в Telegram");
            response.setTemporaryPassword(null);
        } else {
            response.setMessage("Новый пароль сгенерирован");
            response.setTemporaryPassword(temporaryPassword);
        }
        
        return response;
    }
    
    /**
     * Генерирует безопасный временный пароль (8-12 символов, только a-zA-Z0-9)
     */
    private String generateTemporaryPassword() {
        SecureRandom random = new SecureRandom();
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        int length = 8 + random.nextInt(5); // 8-12 символов
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < length; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        return password.toString();
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
