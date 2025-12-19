package ru.ambryo.gameplannerback.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ambryo.gameplannerback.entity.User;
import ru.ambryo.gameplannerback.repository.UserRepository;

import java.time.Instant;
import java.util.*;

@Service
public class PasswordResetService {
    
    private static final Logger logger = LoggerFactory.getLogger(PasswordResetService.class);
    private static final long TOKEN_EXPIRY_HOURS = 1;
    private static final long EMERGENCY_TOKEN_EXPIRY_HOURS = 24;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private TelegramNotificationService telegramNotificationService;
    
    @Autowired
    private RateLimitingService rateLimitingService;
    
    /**
     * Запрашивает сброс пароля для указанного пользователя
     * @param username имя пользователя
     */
    @Transactional
    public void requestPasswordReset(String username) {
        // Проверка rate limiting
        if (rateLimitingService.isPasswordResetRateLimited(username)) {
            logger.warn("Password reset rate limit exceeded for user: {}", username);
            throw new RuntimeException("Превышен лимит запросов. Попробуйте позже.");
        }
        
        // Поиск пользователя
        User user = userRepository.findByUsername(username)
                .orElse(null);
        
        // Молча возвращаем успех, если пользователь не найден (безопасность)
        if (user == null) {
            logger.debug("Password reset requested for non-existent user: {}", username);
            // Регистрируем попытку даже для несуществующего пользователя (для защиты от перебора)
            rateLimitingService.recordRequest(username);
            return;
        }
        
        // Генерация токена
        String token = generateResetToken();
        
        // Аннулирование существующего токена (установка expiry в прошлое)
        if (user.getPasswordResetToken() != null) {
            user.setPasswordResetExpiry(Instant.now().minusSeconds(3600));
            userRepository.save(user);
        }
        
        // Сохранение нового токена
        user.setPasswordResetToken(token);
        user.setPasswordResetExpiry(Instant.now().plusSeconds(TOKEN_EXPIRY_HOURS * 3600));
        userRepository.save(user);
        
        // Регистрация запроса для rate limiting
        rateLimitingService.recordRequest(username);
        
        // Отправка в Telegram, если пользователь подписан
        if (user.getTelegramSubscribed() != null && user.getTelegramSubscribed() 
                && user.getTelegramChatId() != null) {
            try {
                telegramNotificationService.sendPasswordResetToken(user, token);
                logger.info("Password reset token sent via Telegram for user: {}", username);
            } catch (Exception e) {
                logger.error("Failed to send password reset token via Telegram for user: {}", username, e);
                // Молча продолжаем - токен сохранен, пользователь может его использовать
            }
        } else {
            logger.debug("User {} is not subscribed to Telegram or chat ID not available, skipping notification", username);
        }
        
        // Всегда возвращаем успех (не раскрываем информацию о подписке)
    }
    
    /**
     * Проверяет валидность токена сброса пароля
     * @param token токен для проверки
     * @return пользователь, если токен валиден, null в противном случае
     */
    @Transactional(readOnly = true)
    public User validateToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        
        // Поиск пользователя по токену
        Optional<User> userOpt = userRepository.findAll().stream()
                .filter(u -> token.equals(u.getPasswordResetToken()))
                .findFirst();
        
        if (userOpt.isEmpty()) {
            logger.debug("Invalid password reset token provided");
            return null;
        }
        
        User user = userOpt.get();
        
        // Проверка срока действия токена
        if (user.getPasswordResetExpiry() == null || 
            user.getPasswordResetExpiry().isBefore(Instant.now())) {
            logger.debug("Password reset token expired for user: {}", user.getUsername());
            return null;
        }
        
        return user;
    }
    
    /**
     * Сбрасывает пароль используя токен
     * @param token токен сброса пароля
     * @param newPassword новый пароль
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        // Валидация токена
        User user = validateToken(token);
        if (user == null) {
            throw new RuntimeException("Неверный или истекший токен сброса пароля");
        }
        
        // Валидация нового пароля
        if (newPassword == null || newPassword.length() < 6) {
            throw new RuntimeException("Пароль должен содержать минимум 6 символов");
        }
        
        // Установка нового пароля
        user.setPassword(passwordEncoder.encode(newPassword));
        
        // Удаление токена из БД (установка в null)
        user.setPasswordResetToken(null);
        user.setPasswordResetExpiry(null);
        userRepository.save(user);
        
        logger.info("Password reset successful for user: {}", user.getUsername());
    }
    
    /**
     * Генерирует безопасный токен для сброса пароля
     * @return UUID токен
     */
    private String generateResetToken() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * Устанавливает токен сброса пароля для пользователя (для emergency recovery)
     * @param user пользователь
     * @param expiryHours срок действия токена в часах
     * @return сгенерированный токен
     */
    @Transactional
    public String setResetTokenForUser(User user, long expiryHours) {
        String token = generateResetToken();
        user.setPasswordResetToken(token);
        user.setPasswordResetExpiry(Instant.now().plusSeconds(expiryHours * 3600));
        userRepository.save(user);
        return token;
    }
}
