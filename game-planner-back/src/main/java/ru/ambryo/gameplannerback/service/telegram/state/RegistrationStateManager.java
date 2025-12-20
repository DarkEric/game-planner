package ru.ambryo.gameplannerback.service.telegram.state;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Менеджер состояний регистрации
 */
@Component
public class RegistrationStateManager extends AbstractStateManager<RegistrationStateManager.RegistrationState> {
    
    // Класс для хранения данных регистрации
    public static class RegistrationData {
        public String inviteCode;
        public String username;
        public String name;
        public String email;
        public String password;
    }
    
    // Хранение данных регистрации: chatId -> RegistrationData
    private final Map<String, RegistrationData> data = new ConcurrentHashMap<>();
    
    // Защита от спама регистрации: chatId -> AttemptInfo
    private static class AttemptInfo {
        int attempts;
        final java.time.Instant firstAttempt;
        java.time.Instant blockedUntil;
        
        AttemptInfo() {
            this.attempts = 0;
            this.firstAttempt = java.time.Instant.now();
            this.blockedUntil = null;
        }
    }
    
    private final Map<String, AttemptInfo> attempts = new ConcurrentHashMap<>();
    
    // Константы для регистрации
    private static final int MAX_REGISTRATION_ATTEMPTS = 3;
    private static final long REGISTRATION_ATTEMPT_WINDOW_SECONDS = 3600; // 1 час
    private static final long REGISTRATION_STATE_TIMEOUT_SECONDS = 600; // 10 минут таймаут состояния
    
    public enum RegistrationState {
        WAITING_INVITE,
        WAITING_USERNAME,
        WAITING_NAME,
        WAITING_EMAIL,
        WAITING_PASSWORD,
        WAITING_PASSWORD_CONFIRM
    }
    
    public RegistrationStateManager() {
        super(REGISTRATION_STATE_TIMEOUT_SECONDS);
    }
    
    public RegistrationData getData(String chatId) {
        return data.get(chatId);
    }
    
    public void setData(String chatId, RegistrationData registrationData) {
        data.put(chatId, registrationData);
    }
    
    @Override
    protected void clearAdditionalData(String chatId) {
        data.remove(chatId);
    }
    
    public boolean isBlocked(String chatId) {
        AttemptInfo info = attempts.get(chatId);
        if (info == null) {
            return false;
        }
        
        if (info.blockedUntil != null && java.time.Instant.now().isBefore(info.blockedUntil)) {
            return true;
        }
        
        if (info.blockedUntil != null && java.time.Instant.now().isAfter(info.blockedUntil)) {
            attempts.remove(chatId);
            return false;
        }
        
        if (java.time.Instant.now().isAfter(info.firstAttempt.plusSeconds(REGISTRATION_ATTEMPT_WINDOW_SECONDS))) {
            attempts.remove(chatId);
            return false;
        }
        
        return false;
    }
    
    public void recordAttempt(String chatId, boolean success) {
        if (success) {
            attempts.remove(chatId);
            return;
        }
        
        AttemptInfo info = attempts.computeIfAbsent(chatId, k -> new AttemptInfo());
        info.attempts++;
        
        if (info.attempts >= MAX_REGISTRATION_ATTEMPTS) {
            info.blockedUntil = java.time.Instant.now().plusSeconds(REGISTRATION_ATTEMPT_WINDOW_SECONDS);
        }
    }
    
    public int getRemainingAttempts(String chatId) {
        AttemptInfo info = attempts.get(chatId);
        if (info == null) {
            return MAX_REGISTRATION_ATTEMPTS;
        }
        
        if (java.time.Instant.now().isAfter(info.firstAttempt.plusSeconds(REGISTRATION_ATTEMPT_WINDOW_SECONDS))) {
            return MAX_REGISTRATION_ATTEMPTS;
        }
        
        return Math.max(0, MAX_REGISTRATION_ATTEMPTS - info.attempts);
    }
    
    public long getBlockTimeRemaining(String chatId) {
        AttemptInfo info = attempts.get(chatId);
        if (info == null || info.blockedUntil == null) {
            return 0;
        }
        
        if (java.time.Instant.now().isAfter(info.blockedUntil)) {
            return 0;
        }
        
        return java.time.Duration.between(java.time.Instant.now(), info.blockedUntil).getSeconds();
    }
}

