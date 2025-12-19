package ru.ambryo.gameplannerback.service.telegram.notification;

import org.telegram.telegrambots.meta.bots.AbsSender;

/**
 * Базовый класс для отправки уведомлений
 */
public abstract class NotificationSender {
    
    protected final AbsSender bot;
    protected final String chatId;
    protected final String threadId;
    
    public NotificationSender(AbsSender bot, String chatId, String threadId) {
        this.bot = bot;
        this.chatId = chatId;
        this.threadId = threadId;
    }
    
    protected void sendGroupMessage(String message) {
        try {
            org.telegram.telegrambots.meta.api.methods.send.SendMessage sendMessage = 
                new org.telegram.telegrambots.meta.api.methods.send.SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText(message);
            sendMessage.setParseMode("HTML");
            
            if (threadId != null && !threadId.trim().isEmpty()) {
                try {
                    int threadIdInt = Integer.parseInt(threadId.trim());
                    sendMessage.setMessageThreadId(threadIdInt);
                } catch (NumberFormatException e) {
                    // Игнорируем ошибку парсинга
                }
            }
            
            bot.execute(sendMessage);
        } catch (org.telegram.telegrambots.meta.exceptions.TelegramApiException e) {
            // Логирование должно быть в подклассах
        }
    }
}

