package ru.ambryo.gameplannerback.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.ambryo.gameplannerback.entity.User;
import ru.ambryo.gameplannerback.repository.UserRepository;
import ru.ambryo.gameplannerback.service.PasswordResetService;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class AdminEmergencyResetConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(AdminEmergencyResetConfig.class);
    
    @Value("${admin.emergency.reset.enabled:false}")
    private boolean emergencyResetEnabled;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordResetService passwordResetService;
    
    @PostConstruct
    public void generateEmergencyResetToken() {
        if (!emergencyResetEnabled) {
            logger.debug("Emergency admin password reset is disabled");
            return;
        }
        
        try {
            // Найти первого администратора (пользователь с минимальным ID и isAdmin = true)
            List<User> allAdmins = userRepository.findAll().stream()
                    .filter(user -> user.getIsAdmin() != null && user.getIsAdmin())
                    .sorted((u1, u2) -> Long.compare(u1.getId(), u2.getId()))
                    .toList();
            
            if (allAdmins.isEmpty()) {
                logger.warn("Emergency reset enabled but no admin found in the system");
                return;
            }
            
            User firstAdmin = allAdmins.get(0);
            
            // Генерируем токен
            String token = passwordResetService.setResetTokenForUser(firstAdmin, 24); // 24 часа
            
            // Форматируем дату истечения
            Instant expiry = firstAdmin.getPasswordResetExpiry();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault());
            String expiryStr = formatter.format(expiry);
            
            // Выводим в консоль
            logger.warn("");
            logger.warn("=".repeat(70));
            logger.warn("⚠️  EMERGENCY ADMIN PASSWORD RESET TOKEN  ⚠️");
            logger.warn("=".repeat(70));
            logger.warn("Username: {}", firstAdmin.getUsername());
            logger.warn("Token: {}", token);
            logger.warn("Expires: {}", expiryStr);
            logger.warn("Use this token at: /api/auth/password-reset/confirm");
            logger.warn("");
            logger.warn("⚠️  DELETE THIS TOKEN FROM LOGS AFTER USE  ⚠️");
            logger.warn("=".repeat(70));
            logger.warn("");
            
        } catch (Exception e) {
            logger.error("Error generating emergency admin password reset token", e);
        }
    }
}
