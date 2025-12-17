package ru.ambryo.gameplannerback.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ambryo.gameplannerback.dto.AuthResponse;
import ru.ambryo.gameplannerback.entity.User;
import ru.ambryo.gameplannerback.repository.UserRepository;
import ru.ambryo.gameplannerback.util.JwtUtil;

@Service
public class AuthService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private InviteService inviteService;
    
    @Transactional
    public AuthResponse register(String username, String password, String email, String inviteCode, String name) {
        // Проверяем инвайт-код (обязательно)
        if (inviteCode == null || inviteCode.trim().isEmpty()) {
            throw new RuntimeException("Invite code is required");
        }
        
        // Валидируем инвайт-код перед созданием пользователя
        inviteService.getInviteByCode(inviteCode); // Проверяем существование
        
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already exists");
        }
        
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);
        // Используем переданное имя, если оно есть, иначе используем username
        user.setName(name != null && !name.trim().isEmpty() ? name.trim() : username);
        user.setColor("#646cff"); // Цвет по умолчанию
        
        User savedUser = userRepository.save(user);
        logger.info("🔵 [AUTH] User registered - username: {}, id: {}, isAdmin before check: {}", 
                username, savedUser.getId(), savedUser.getIsAdmin());
        
        // Проверяем, является ли это первым реальным пользователем (исключая системного пользователя)
        // Системный пользователь имеет ID=0 и username="system"
        var allUsers = userRepository.findAll();
        long totalCount = allUsers.size();
        logger.info("🔵 [AUTH] Total users in DB: {}", totalCount);
        
        long realUserCount = allUsers.stream()
                .filter(u -> u.getId() != null && u.getId() > 0 && !"system".equals(u.getUsername()))
                .count();
        
        logger.info("🔵 [AUTH] Real users count (excluding system): {}", realUserCount);
        logger.info("🔵 [AUTH] All users details: {}", allUsers.stream()
                .map(u -> String.format("id=%d, username='%s', isAdmin=%s", 
                        u.getId(), u.getUsername(), u.getIsAdmin()))
                .toList());
        
        if (realUserCount == 1) {
            logger.info("🔵 [AUTH] ✅ First real user detected! Setting isAdmin=true for user {}", savedUser.getId());
            savedUser.setIsAdmin(true);
            logger.info("🔵 [AUTH] isAdmin flag set to: {} (before save)", savedUser.getIsAdmin());
            
            savedUser = userRepository.save(savedUser);
            
            // Проверяем, что значение сохранилось
            User verifyUser = userRepository.findById(savedUser.getId()).orElse(null);
            logger.info("🔵 [AUTH] ✅ User saved. Verification - id: {}, username: {}, isAdmin in DB: {}", 
                    savedUser.getId(), savedUser.getUsername(), 
                    verifyUser != null ? verifyUser.getIsAdmin() : "NULL");
            
            logger.info("🔵 [AUTH] ✅ First real user '{}' (ID: {}) automatically assigned as administrator", 
                    savedUser.getUsername(), savedUser.getId());
        } else {
            logger.warn("🔵 [AUTH] ⚠️ NOT first user. realUserCount={}, expected 1. Admin NOT assigned.", realUserCount);
        }
        
        // Отмечаем инвайт как использованный
        inviteService.validateAndUseInvite(inviteCode, savedUser);
        
        String token = jwtUtil.generateToken(savedUser.getUsername(), savedUser.getId());
        return new AuthResponse(token, savedUser.getUsername(), savedUser.getId());
    }
    
    public AuthResponse login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Invalid username or password"));
        
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid username or password");
        }
        
        String token = jwtUtil.generateToken(user.getUsername(), user.getId());
        return new AuthResponse(token, user.getUsername(), user.getId());
    }
}

