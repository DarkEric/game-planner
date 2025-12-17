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
        try {
            // Проверяем наличие администраторов в системе
            long adminCount = userRepository.countByIsAdminTrue();
            
            if (adminCount == 0) {
                logger.info("No administrators found in the system. Assigning first user as administrator...");
                
                // Если нет администраторов, назначаем первого реального пользователя (исключая системного)
                Optional<User> firstRealUserOpt = userRepository.findAll().stream()
                        .filter(u -> u.getId() != null && u.getId() > 0 && !"system".equals(u.getUsername()))
                        .sorted((u1, u2) -> Long.compare(u1.getId(), u2.getId()))
                        .findFirst();
                
                if (firstRealUserOpt.isPresent()) {
                    User firstUser = firstRealUserOpt.get();
                    firstUser.setIsAdmin(true);
                    userRepository.save(firstUser);
                    logger.info("User '{}' (ID: {}) has been assigned as administrator", 
                            firstUser.getUsername(), firstUser.getId());
                } else {
                    logger.warn("No real users found in the system. First registered user will become administrator.");
                }
            } else {
                logger.debug("Found {} administrator(s) in the system", adminCount);
            }
        } catch (Exception e) {
            logger.error("Error during admin initialization", e);
        }
    }
}
