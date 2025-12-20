package ru.ambryo.gameplannerback.service.telegram.state;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Менеджер состояний настроек уведомлений
 */
@Component
public class NotificationStateManager extends AbstractStateManager<NotificationStateManager.NotificationState> {
    
    // Класс для хранения данных настроек уведомлений
    public static class NotificationData {
        public Integer reminderIndex;        // Индекс редактируемого напоминания
        public Integer reminderValue;        // Временное значение
        public String reminderUnit;          // Временная единица (minutes/hours/days)
        public String cronFrequency;        // Частота cron (daily/weekly/monthly)
        public Integer cronDay;              // День для cron
        public String cronTime;              // Время для cron (HH:mm)
    }
    
    // Хранение данных настроек уведомлений: chatId -> NotificationData
    private final Map<String, NotificationData> data = new ConcurrentHashMap<>();
    
    private static final long NOTIFICATION_STATE_TIMEOUT_SECONDS = 300; // 5 минут таймаут состояния
    
    public enum NotificationState {
        WAITING_REMINDER_VALUE,      // Ожидание значения напоминания
        WAITING_REMINDER_UNIT,       // Ожидание единицы (минуты/часы/дни)
        WAITING_CRON_FREQUENCY,      // Ожидание частоты cron (daily/weekly/monthly)
        WAITING_CRON_DAY,            // Ожидание дня для weekly/monthly
        WAITING_CRON_TIME            // Ожидание времени для cron
    }
    
    public NotificationStateManager() {
        super(NOTIFICATION_STATE_TIMEOUT_SECONDS);
    }
    
    public NotificationData getData(String chatId) {
        return data.get(chatId);
    }
    
    public void setData(String chatId, NotificationData notificationData) {
        data.put(chatId, notificationData);
    }
    
    @Override
    protected void clearAdditionalData(String chatId) {
        data.remove(chatId);
    }
}

