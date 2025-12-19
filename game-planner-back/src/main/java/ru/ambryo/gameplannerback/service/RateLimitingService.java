package ru.ambryo.gameplannerback.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Сервис для ограничения частоты запросов (rate limiting).
 * Использует in-memory хранилище для отслеживания попыток.
 */
@Service
public class RateLimitingService {
    
    private static final Logger logger = LoggerFactory.getLogger(RateLimitingService.class);
    
    // Хранилище запросов: ключ -> список временных меток
    private final Map<String, List<Instant>> requestHistory = new ConcurrentHashMap<>();
    
    // Конфигурация по умолчанию
    @Value("${rate.limit.login.requests:5}")
    private int defaultLoginRequests;
    
    @Value("${rate.limit.login.window.minutes:15}")
    private long defaultLoginWindowMinutes;
    
    @Value("${rate.limit.register.requests:3}")
    private int defaultRegisterRequests;
    
    @Value("${rate.limit.register.window.minutes:60}")
    private long defaultRegisterWindowMinutes;
    
    @Value("${rate.limit.password.reset.requests:3}")
    private int defaultPasswordResetRequests;
    
    @Value("${rate.limit.password.reset.window.hours:1}")
    private long defaultPasswordResetWindowHours;
    
    /**
     * Проверяет, превышен ли лимит запросов для указанного ключа.
     * 
     * @param key ключ для идентификации (например, username, IP-адрес)
     * @param maxRequests максимальное количество запросов
     * @param windowMinutes временное окно в минутах
     * @return true, если лимит превышен
     */
    public boolean isRateLimited(String key, int maxRequests, long windowMinutes) {
        if (key == null || key.trim().isEmpty()) {
            return false;
        }
        
        List<Instant> requests = requestHistory.get(key);
        if (requests == null || requests.isEmpty()) {
            return false;
        }
        
        Instant windowStart = Instant.now().minusSeconds(windowMinutes * 60);
        
        // Фильтруем запросы в пределах временного окна
        List<Instant> recentRequests = requests.stream()
                .filter(instant -> instant.isAfter(windowStart))
                .collect(Collectors.toList());
        
        // Обновляем историю
        if (recentRequests.size() != requests.size()) {
            if (recentRequests.isEmpty()) {
                requestHistory.remove(key);
            } else {
                requestHistory.put(key, recentRequests);
            }
        }
        
        boolean limited = recentRequests.size() >= maxRequests;
        if (limited) {
            logger.warn("Rate limit exceeded for key: {} ({} requests in {} minutes)", 
                    key, recentRequests.size(), windowMinutes);
        }
        
        return limited;
    }
    
    /**
     * Проверяет лимит для логина (использует настройки по умолчанию).
     */
    public boolean isLoginRateLimited(String username) {
        return isRateLimited(username, defaultLoginRequests, defaultLoginWindowMinutes);
    }
    
    /**
     * Проверяет лимит для регистрации (использует настройки по умолчанию).
     */
    public boolean isRegisterRateLimited(String identifier) {
        return isRateLimited(identifier, defaultRegisterRequests, defaultRegisterWindowMinutes);
    }
    
    /**
     * Проверяет лимит для сброса пароля (использует настройки по умолчанию).
     */
    public boolean isPasswordResetRateLimited(String username) {
        return isRateLimited(username, defaultPasswordResetRequests, defaultPasswordResetWindowHours * 60);
    }
    
    /**
     * Регистрирует запрос для указанного ключа.
     * 
     * @param key ключ для идентификации
     */
    public void recordRequest(String key) {
        if (key == null || key.trim().isEmpty()) {
            return;
        }
        
        requestHistory.computeIfAbsent(key, k -> new ArrayList<>())
                .add(Instant.now());
    }
    
    /**
     * Очищает старые записи из истории (для предотвращения утечки памяти).
     * Запускается каждый час.
     */
    @Scheduled(fixedRate = 3600000) // Каждый час (3600000 мс)
    public void cleanupOldEntries() {
        Instant oneDayAgo = Instant.now().minusSeconds(24 * 60 * 60);
        
        int beforeSize = requestHistory.size();
        requestHistory.entrySet().removeIf(entry -> {
            List<Instant> requests = entry.getValue();
            requests.removeIf(instant -> instant.isBefore(oneDayAgo));
            return requests.isEmpty();
        });
        int afterSize = requestHistory.size();
        
        if (beforeSize != afterSize) {
            logger.debug("Rate limiting cleanup: removed {} old entries (before: {}, after: {})", 
                    beforeSize - afterSize, beforeSize, afterSize);
        }
    }
    
    /**
     * Очищает все записи для указанного ключа.
     */
    public void clearKey(String key) {
        if (key != null) {
            requestHistory.remove(key);
        }
    }
}
