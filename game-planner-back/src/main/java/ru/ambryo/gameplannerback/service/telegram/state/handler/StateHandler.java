package ru.ambryo.gameplannerback.service.telegram.state.handler;

/**
 * Интерфейс для обработчиков состояний диалогов в Telegram боте
 * @param <T> тип состояния (enum)
 */
public interface StateHandler<T> {
    
    /**
     * Проверяет, может ли обработчик обработать данное состояние
     * @param chatId ID чата
     * @param state состояние
     * @return true если обработчик может обработать состояние
     */
    boolean canHandle(String chatId, T state);
    
    /**
     * Обрабатывает состояние
     * @param telegramUserId ID пользователя Telegram
     * @param chatId ID чата
     * @param text текст сообщения от пользователя
     * @param state текущее состояние
     */
    void handle(Long telegramUserId, String chatId, String text, T state);
}


