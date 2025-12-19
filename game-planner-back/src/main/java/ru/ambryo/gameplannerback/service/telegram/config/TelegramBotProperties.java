package ru.ambryo.gameplannerback.service.telegram.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Конфигурационные свойства Telegram бота
 */
@Component
@ConfigurationProperties(prefix = "telegram.bot")
public class TelegramBotProperties {
    
    private boolean enabled = false;
    private String token;
    private String chatId;
    private String threadId;
    private String timezone = "Europe/Moscow";
    
    // Константы для защиты от брутфорса
    private int maxAuthAttempts = 3;
    private long authAttemptWindowSeconds = 900; // 15 минут
    private long authBlockDurationSeconds = 900; // 15 минут блокировки
    private long authStateTimeoutSeconds = 300; // 5 минут таймаут состояния
    
    // Константы для регистрации
    private int maxRegistrationAttempts = 3;
    private long registrationAttemptWindowSeconds = 3600; // 1 час
    private long registrationStateTimeoutSeconds = 600; // 10 минут таймаут состояния
    private int minPasswordLength = 6;
    
    // Константы для разметки времени
    private long timeSlotMarkingStateTimeoutSeconds = 300; // 5 минут таймаут состояния
    
    // Константы для смены часового пояса
    private long timezoneChangeStateTimeoutSeconds = 300; // 5 минут таймаут состояния
    
    // Константы для настроек уведомлений
    private long notificationStateTimeoutSeconds = 300; // 5 минут таймаут состояния
    
    // Константы для пагинации
    private int gamesPerPage = 5;
    
    // Getters and Setters
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public String getChatId() {
        return chatId;
    }
    
    public void setChatId(String chatId) {
        this.chatId = chatId;
    }
    
    public String getThreadId() {
        return threadId;
    }
    
    public void setThreadId(String threadId) {
        this.threadId = threadId;
    }
    
    public String getTimezone() {
        return timezone;
    }
    
    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }
    
    public int getMaxAuthAttempts() {
        return maxAuthAttempts;
    }
    
    public void setMaxAuthAttempts(int maxAuthAttempts) {
        this.maxAuthAttempts = maxAuthAttempts;
    }
    
    public long getAuthAttemptWindowSeconds() {
        return authAttemptWindowSeconds;
    }
    
    public void setAuthAttemptWindowSeconds(long authAttemptWindowSeconds) {
        this.authAttemptWindowSeconds = authAttemptWindowSeconds;
    }
    
    public long getAuthBlockDurationSeconds() {
        return authBlockDurationSeconds;
    }
    
    public void setAuthBlockDurationSeconds(long authBlockDurationSeconds) {
        this.authBlockDurationSeconds = authBlockDurationSeconds;
    }
    
    public long getAuthStateTimeoutSeconds() {
        return authStateTimeoutSeconds;
    }
    
    public void setAuthStateTimeoutSeconds(long authStateTimeoutSeconds) {
        this.authStateTimeoutSeconds = authStateTimeoutSeconds;
    }
    
    public int getMaxRegistrationAttempts() {
        return maxRegistrationAttempts;
    }
    
    public void setMaxRegistrationAttempts(int maxRegistrationAttempts) {
        this.maxRegistrationAttempts = maxRegistrationAttempts;
    }
    
    public long getRegistrationAttemptWindowSeconds() {
        return registrationAttemptWindowSeconds;
    }
    
    public void setRegistrationAttemptWindowSeconds(long registrationAttemptWindowSeconds) {
        this.registrationAttemptWindowSeconds = registrationAttemptWindowSeconds;
    }
    
    public long getRegistrationStateTimeoutSeconds() {
        return registrationStateTimeoutSeconds;
    }
    
    public void setRegistrationStateTimeoutSeconds(long registrationStateTimeoutSeconds) {
        this.registrationStateTimeoutSeconds = registrationStateTimeoutSeconds;
    }
    
    public int getMinPasswordLength() {
        return minPasswordLength;
    }
    
    public void setMinPasswordLength(int minPasswordLength) {
        this.minPasswordLength = minPasswordLength;
    }
    
    public long getTimeSlotMarkingStateTimeoutSeconds() {
        return timeSlotMarkingStateTimeoutSeconds;
    }
    
    public void setTimeSlotMarkingStateTimeoutSeconds(long timeSlotMarkingStateTimeoutSeconds) {
        this.timeSlotMarkingStateTimeoutSeconds = timeSlotMarkingStateTimeoutSeconds;
    }
    
    public long getTimezoneChangeStateTimeoutSeconds() {
        return timezoneChangeStateTimeoutSeconds;
    }
    
    public void setTimezoneChangeStateTimeoutSeconds(long timezoneChangeStateTimeoutSeconds) {
        this.timezoneChangeStateTimeoutSeconds = timezoneChangeStateTimeoutSeconds;
    }
    
    public long getNotificationStateTimeoutSeconds() {
        return notificationStateTimeoutSeconds;
    }
    
    public void setNotificationStateTimeoutSeconds(long notificationStateTimeoutSeconds) {
        this.notificationStateTimeoutSeconds = notificationStateTimeoutSeconds;
    }
    
    public int getGamesPerPage() {
        return gamesPerPage;
    }
    
    public void setGamesPerPage(int gamesPerPage) {
        this.gamesPerPage = gamesPerPage;
    }
}

