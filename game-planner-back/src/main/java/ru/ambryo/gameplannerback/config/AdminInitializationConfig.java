package ru.ambryo.gameplannerback.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.ambryo.gameplannerback.entity.User;
import ru.ambryo.gameplannerback.repository.UserRepository;

import jakarta.annotation.PostConstruct;
import java.util.Optional;

@Component
public class AdminInitializationConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(AdminInitializationConfig.class);
    
    @Autowired
    private UserRepository userRepository;
    
    @PostConstruct
    public void initializeAdmin() {
        logger.info("🟢 [INIT] AdminInitializationConfig.initializeAdmin() called");
        try {
            // Проверяем наличие администраторов в системе
            long adminCount = userRepository.countByIsAdminTrue();
            logger.info("🟢 [INIT] Current admin count: {}", adminCount);
            
            if (adminCount == 0) {
                logger.info("🟢 [INIT] ⚠️ No administrators found in the system. Assigning first user as administrator...");
                
                // Если нет администраторов, назначаем первого реального пользователя (исключая системного)
                var allUsers = userRepository.findAll();
                logger.info("🟢 [INIT] Total users in DB: {}", allUsers.size());
                logger.info("🟢 [INIT] All users: {}", allUsers.stream()
                        .map(u -> String.format("id=%d, username='%s', isAdmin=%s", 
                                u.getId(), u.getUsername(), u.getIsAdmin()))
                        .toList());
                
                Optional<User> firstRealUserOpt = allUsers.stream()
                        .filter(u -> {
                            boolean matches = u.getId() != null && u.getId() > 0 && !"system".equals(u.getUsername());
                            if (!matches) {
                                logger.debug("🟢 [INIT] Filtered out user: id={}, username='{}'", u.getId(), u.getUsername());
                            }
                            return matches;
                        })
                        .sorted((u1, u2) -> Long.compare(u1.getId(), u2.getId()))
                        .findFirst();
                
                logger.info("🟢 [INIT] First real user found: {}", firstRealUserOpt.isPresent());
                
                if (firstRealUserOpt.isPresent()) {
                    User firstUser = firstRealUserOpt.get();
                    logger.info("🟢 [INIT] ✅ Found first real user - id: {}, username: '{}', isAdmin before: {}", 
                            firstUser.getId(), firstUser.getUsername(), firstUser.getIsAdmin());
                    
                    firstUser.setIsAdmin(true);
                    logger.info("🟢 [INIT] isAdmin flag set to: {} (before save)", firstUser.getIsAdmin());
                    
                    userRepository.save(firstUser);
                    logger.info("🟢 [INIT] User saved");
                    
                    // Проверяем, что значение сохранилось
                    User verifyUser = userRepository.findById(firstUser.getId()).orElse(null);
                    logger.info("🟢 [INIT] ✅ Verification - id: {}, username: {}, isAdmin in DB: {}", 
                            firstUser.getId(), firstUser.getUsername(), 
                            verifyUser != null ? verifyUser.getIsAdmin() : "NULL");
                    
                    logger.info("🟢 [INIT] ✅ User '{}' (ID: {}) has been assigned as administrator", 
                            firstUser.getUsername(), firstUser.getId());
                } else {
                    logger.warn("🟢 [INIT] ⚠️ No real users found in the system. First registered user will become administrator.");
                }
            } else {
                logger.info("🟢 [INIT] ✅ Found {} administrator(s) in the system", adminCount);
                
                // Показываем, кто именно является администратором
                var allAdmins = userRepository.findAll().stream()
                        .filter(u -> u.getIsAdmin() != null && u.getIsAdmin())
                        .toList();
                logger.info("🟢 [INIT] Administrator details: {}", allAdmins.stream()
                        .map(u -> String.format("id=%d, username='%s'", u.getId(), u.getUsername()))
                        .toList());
                
                // Убираем флаг администратора у системного пользователя, если он установлен
                Optional<User> systemUserOpt = userRepository.findByUsername("system");
                if (systemUserOpt.isPresent()) {
                    User systemUser = systemUserOpt.get();
                    if (systemUser.getIsAdmin() != null && systemUser.getIsAdmin()) {
                        logger.warn("🟢 [INIT] ⚠️ System user has admin flag. Removing it...");
                        systemUser.setIsAdmin(false);
                        userRepository.save(systemUser);
                        logger.info("🟢 [INIT] ✅ System user admin flag removed");
                        
                        // Пересчитываем количество администраторов после снятия флага у системного
                        adminCount = userRepository.countByIsAdminTrue();
                        logger.info("🟢 [INIT] Admin count after removing system user flag: {}", adminCount);
                        
                        // Обновляем список администраторов после снятия флага у системного
                        allAdmins = userRepository.findAll().stream()
                                .filter(u -> u.getIsAdmin() != null && u.getIsAdmin())
                                .toList();
                    }
                }
                
                // Проверяем, есть ли среди администраторов реальные пользователи (не системный)
                boolean hasRealAdmin = allAdmins.stream()
                        .anyMatch(u -> u.getId() != null && u.getId() > 0 && !"system".equals(u.getUsername()));
                
                if (!hasRealAdmin && adminCount > 0) {
                    logger.warn("🟢 [INIT] ⚠️ WARNING: Only system user is administrator! No real admin found.");
                    logger.warn("🟢 [INIT] ⚠️ Assigning first real user as administrator...");
                    
                    var allUsers = userRepository.findAll();
                    Optional<User> firstRealUserOpt = allUsers.stream()
                            .filter(u -> u.getId() != null && u.getId() > 0 && !"system".equals(u.getUsername()))
                            .sorted((u1, u2) -> Long.compare(u1.getId(), u2.getId()))
                            .findFirst();
                    
                    if (firstRealUserOpt.isPresent()) {
                        User firstUser = firstRealUserOpt.get();
                        logger.info("🟢 [INIT] ✅ Found first real user - id: {}, username: '{}'", 
                                firstUser.getId(), firstUser.getUsername());
                        
                        firstUser.setIsAdmin(true);
                        userRepository.save(firstUser);
                        
                        logger.info("🟢 [INIT] ✅ User '{}' (ID: {}) has been assigned as administrator", 
                                firstUser.getUsername(), firstUser.getId());
                    } else {
                        logger.warn("🟢 [INIT] ⚠️ No real users found. First registered user will become administrator.");
                    }
                }
            }
        } catch (Exception e) {
            logger.error("🟢 [INIT] ❌ Error during admin initialization", e);
        }
    }
}
