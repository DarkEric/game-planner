package ru.ambryo.gameplannerback.service.telegram.state;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Базовая реализация StateManager с общими методами
 * @param <T> тип состояния (enum)
 */
public abstract class AbstractStateManager<T> implements StateManager<T> {
    
    protected final Map<String, T> states = new ConcurrentHashMap<>();
    protected final Map<String, Instant> timestamps = new ConcurrentHashMap<>();
    protected final long timeoutSeconds;
    
    public AbstractStateManager(long timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }
    
    @Override
    public T getState(String chatId) {
        return states.get(chatId);
    }
    
    @Override
    public void setState(String chatId, T state) {
        states.put(chatId, state);
        updateTimestamp(chatId);
    }
    
    @Override
    public void clearState(String chatId) {
        states.remove(chatId);
        timestamps.remove(chatId);
        clearAdditionalData(chatId);
    }
    
    @Override
    public boolean isStateExpired(String chatId) {
        Instant timestamp = timestamps.get(chatId);
        if (timestamp == null) {
            return true;
        }
        return Instant.now().isAfter(timestamp.plusSeconds(timeoutSeconds));
    }
    
    @Override
    public void updateTimestamp(String chatId) {
        timestamps.put(chatId, Instant.now());
    }
    
    @Override
    public boolean hasState(String chatId) {
        return states.containsKey(chatId);
    }
    
    /**
     * Очищает дополнительные данные состояния (переопределяется в подклассах)
     * @param chatId ID чата
     */
    protected void clearAdditionalData(String chatId) {
        // По умолчанию ничего не делаем, подклассы могут переопределить
    }
}

