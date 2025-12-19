package ru.ambryo.gameplannerback.service.telegram.state;

import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Менеджер состояний авторизации
 */
@Component
public class AuthStateManager extends AbstractStateManager<AuthStateManager.AuthState> {
    
    // Хранение временных данных: chatId -> username
    private final Map<String, String> usernames = new ConcurrentHashMap<>();
    
    // Защита от брутфорса: chatId -> AttemptInfo
    private static class AttemptInfo {
        int attempts;
        java.time.Instant firstAttempt;
        java.time.Instant blockedUntil;
        
        AttemptInfo() {
            this.attempts = 0;
            this.firstAttempt = java.time.Instant.now();
            this.blockedUntil = null;
        }
    }
    
    private final Map<String, AttemptInfo> attempts = new ConcurrentHashMap<>();
    
    // Константы для защиты от брутфорса
    private static final int MAX_AUTH_ATTEMPTS = 3;
    private static final long AUTH_ATTEMPT_WINDOW_SECONDS = 900; // 15 минут
    private static final long AUTH_BLOCK_DURATION_SECONDS = 900; // 15 минут блокировки
    private static final long AUTH_STATE_TIMEOUT_SECONDS = 300; // 5 минут таймаут состояния
    
    public enum AuthState {
        WAITING_USERNAME,
        WAITING_PASSWORD
    }
    
    public AuthStateManager() {
        super(AUTH_STATE_TIMEOUT_SECONDS);
    }
    
    public String getUsername(String chatId) {
        return usernames.get(chatId);
    }
    
    public void setUsername(String chatId, String username) {
        usernames.put(chatId, username);
    }
    
    @Override
    protected void clearAdditionalData(String chatId) {
        usernames.remove(chatId);
    }
    
    public boolean isBlocked(String chatId) {
        AttemptInfo info = attempts.get(chatId);
        if (info == null) {
            return false;
        }
        
        if (info.blockedUntil != null && java.time.Instant.now().isBefore(info.blockedUntil)) {
            return true;
        }
        
        // Если блокировка истекла, сбрасываем попытки
        if (info.blockedUntil != null && java.time.Instant.now().isAfter(info.blockedUntil)) {
            attempts.remove(chatId);
            return false;
        }
        
        // Проверяем, не истекло ли окно попыток
        if (java.time.Instant.now().isAfter(info.firstAttempt.plusSeconds(AUTH_ATTEMPT_WINDOW_SECONDS))) {
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
        
        if (info.attempts >= MAX_AUTH_ATTEMPTS) {
            info.blockedUntil = java.time.Instant.now().plusSeconds(AUTH_BLOCK_DURATION_SECONDS);
        }
    }
    
    public int getRemainingAttempts(String chatId) {
        AttemptInfo info = attempts.get(chatId);
        if (info == null) {
            return MAX_AUTH_ATTEMPTS;
        }
        
        if (java.time.Instant.now().isAfter(info.firstAttempt.plusSeconds(AUTH_ATTEMPT_WINDOW_SECONDS))) {
            return MAX_AUTH_ATTEMPTS;
        }
        
        return Math.max(0, MAX_AUTH_ATTEMPTS - info.attempts);
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

