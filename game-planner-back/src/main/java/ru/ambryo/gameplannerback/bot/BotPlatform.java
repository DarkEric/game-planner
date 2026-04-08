package ru.ambryo.gameplannerback.bot;

/**
 * Абстракция платформы бота (Telegram, Discord, и т.п.).
 * Все команды и сценарии диалогов должны работать через этот интерфейс,
 * не завязываясь на конкретные API мессенджеров.
 */
public interface BotPlatform {

    /**
     * Уникальный идентификатор платформы, например: telegram, discord, vk.
     */
    String getPlatformId();

    /**
     * Отправка простого текстового сообщения.
     *
     * @param chatId идентификатор чата/канала в рамках платформы
     * @param text   текст сообщения
     * @param options дополнительные параметры сообщения (parse mode, reply markup и т.п.)
     */
    void sendText(String chatId, String text, BotMessageOptions options);

    /**
     * Обновление ранее отправленного сообщения.
     *
     * @param chatId    идентификатор чата
     * @param messageId идентификатор сообщения в рамках платформы
     * @param text      новый текст
     * @param options   дополнительные параметры сообщения
     */
    void editMessage(String chatId, String messageId, String text, BotMessageOptions options);

    /**
     * Отправка меню с кнопками.
     *
     * @param chatId идентификатор чата
     * @param menu   описание меню, независимое от конкретной платформы
     */
    void sendMenu(String chatId, BotMenu menu);
}

