package ru.ambryo.gameplannerback.service.telegram.state;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Менеджер состояний разметки времени
 */
@Component
public class TimeSlotMarkingStateManager extends AbstractStateManager<TimeSlotMarkingStateManager.TimeSlotMarkingState> {
    
    // Класс для хранения данных разметки времени
    public static class TimeSlotMarkingData {
        public String dateStr;  // Введенная дата как строка
        public String timeStr;  // Введенное время как строка
        public Instant dateInstant;  // Парсированная дата (начало дня в UTC)
        public Instant startInstant;  // Финальный Instant для слота (в UTC)
        public Integer duration;  // Продолжительность в часах
    }
    
    // Хранение данных разметки времени: chatId -> TimeSlotMarkingData
    private final Map<String, TimeSlotMarkingData> data = new ConcurrentHashMap<>();
    
    private static final long TIME_SLOT_MARKING_STATE_TIMEOUT_SECONDS = 300; // 5 минут таймаут состояния
    
    public enum TimeSlotMarkingState {
        WAITING_DATE,
        WAITING_TIME,
        WAITING_DURATION
    }
    
    public TimeSlotMarkingStateManager() {
        super(TIME_SLOT_MARKING_STATE_TIMEOUT_SECONDS);
    }
    
    public TimeSlotMarkingData getData(String chatId) {
        return data.get(chatId);
    }
    
    public void setData(String chatId, TimeSlotMarkingData markingData) {
        data.put(chatId, markingData);
    }
    
    @Override
    protected void clearAdditionalData(String chatId) {
        data.remove(chatId);
    }
}

