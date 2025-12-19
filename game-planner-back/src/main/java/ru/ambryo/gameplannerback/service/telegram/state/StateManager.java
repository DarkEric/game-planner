package ru.ambryo.gameplannerback.service.telegram.state;

import java.time.Instant;

/**
 * Интерфейс для управления состояниями диалогов в Telegram боте
 * @param <T> тип состояния (enum)
 */
public interface StateManager<T> {
    
    /**
     * Получает текущее состояние для chatId
     * @param chatId ID чата
     * @return состояние или null если состояние не установлено
     */
    T getState(String chatId);
    
    /**
     * Устанавливает состояние для chatId
     * @param chatId ID чата
     * @param state состояние
     */
    void setState(String chatId, T state);
    
    /**
     * Очищает состояние для chatId
     * @param chatId ID чата
     */
    void clearState(String chatId);
    
    /**
     * Проверяет, истекло ли состояние по таймауту
     * @param chatId ID чата
     * @return true если состояние истекло или не существует
     */
    boolean isStateExpired(String chatId);
    
    /**
     * Обновляет timestamp последнего действия
     * @param chatId ID чата
     */
    void updateTimestamp(String chatId);
    
    /**
     * Проверяет, существует ли состояние для chatId
     * @param chatId ID чата
     * @return true если состояние существует
     */
    boolean hasState(String chatId);
}

