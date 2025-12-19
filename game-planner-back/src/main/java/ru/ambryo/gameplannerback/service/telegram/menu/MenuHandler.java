package ru.ambryo.gameplannerback.service.telegram.menu;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

/**
 * Интерфейс для обработчиков меню Telegram бота
 */
public interface MenuHandler {
    
    /**
     * Проверяет, может ли этот обработчик обработать данный callback
     * @param callbackData данные callback
     * @return true если обработчик может обработать callback
     */
    boolean canHandle(String callbackData);
    
    /**
     * Обрабатывает callback меню
     * @param callbackQuery callback query
     * @param telegramUserId ID пользователя Telegram
     * @param chatId ID чата
     * @param messageId ID сообщения
     */
    void handle(CallbackQuery callbackQuery, Long telegramUserId, String chatId, Integer messageId);
}

