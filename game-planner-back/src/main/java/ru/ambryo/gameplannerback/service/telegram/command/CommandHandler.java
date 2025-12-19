package ru.ambryo.gameplannerback.service.telegram.command;

import org.telegram.telegrambots.meta.api.objects.Message;

/**
 * Интерфейс для обработчиков команд Telegram бота
 */
public interface CommandHandler {
    
    /**
     * Проверяет, может ли этот обработчик обработать данную команду
     * @param command команда (без префикса /)
     * @return true если обработчик может обработать команду
     */
    boolean canHandle(String command);
    
    /**
     * Обрабатывает команду
     * @param message сообщение с командой
     * @param telegramUserId ID пользователя Telegram
     * @param chatId ID чата
     */
    void handle(Message message, Long telegramUserId, String chatId);
}

