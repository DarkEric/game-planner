package ru.ambryo.gameplannerback.service.telegram.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * Утилита для отправки сообщений в Telegram
 */
public class TelegramMessageSender {
    
    private static final Logger logger = LoggerFactory.getLogger(TelegramMessageSender.class);
    
    private final AbsSender bot;
    
    public TelegramMessageSender(AbsSender bot) {
        this.bot = bot;
    }
    
    /**
     * Отправляет персональное сообщение пользователю
     * @param chatId ID чата
     * @param text текст сообщения
     */
    public void sendPersonalMessage(String chatId, String text) {
        try {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText(text);
            sendMessage.setParseMode("HTML");
            bot.execute(sendMessage);
        } catch (TelegramApiException e) {
            logger.error("Failed to send personal message to chat {}", chatId, e);
        }
    }
    
    /**
     * Отправляет сообщение с клавиатурой
     * @param chatId ID чата
     * @param text текст сообщения
     * @param keyboard клавиатура
     */
    public void sendMessageWithKeyboard(String chatId, String text, InlineKeyboardMarkup keyboard) {
        try {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText(text);
            sendMessage.setParseMode("HTML");
            sendMessage.setReplyMarkup(keyboard);
            bot.execute(sendMessage);
        } catch (TelegramApiException e) {
            logger.error("Failed to send message with keyboard to chat {}", chatId, e);
        }
    }
    
    /**
     * Обновляет сообщение с клавиатурой
     * @param chatId ID чата
     * @param messageId ID сообщения
     * @param text новый текст
     * @param keyboard новая клавиатура
     */
    public void updateMenuMessage(String chatId, Integer messageId, String text, InlineKeyboardMarkup keyboard) {
        try {
            EditMessageText editMessage = new EditMessageText();
            editMessage.setChatId(chatId);
            editMessage.setMessageId(messageId);
            editMessage.setText(text);
            editMessage.setParseMode("HTML");
            editMessage.setReplyMarkup(keyboard);
            bot.execute(editMessage);
        } catch (TelegramApiException e) {
            logger.error("Failed to update menu message in chat {}", chatId, e);
        }
    }
    
    /**
     * Отправляет сообщение в групповой чат с поддержкой thread ID
     * @param chatId ID чата
     * @param threadId ID топика (опционально)
     * @param text текст сообщения
     */
    public void sendGroupMessage(String chatId, String threadId, String text) {
        try {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText(text);
            sendMessage.setParseMode("HTML");
            
            if (threadId != null && !threadId.trim().isEmpty()) {
                try {
                    int threadIdInt = Integer.parseInt(threadId.trim());
                    sendMessage.setMessageThreadId(threadIdInt);
                } catch (NumberFormatException e) {
                    logger.warn("Invalid thread ID format: '{}', sending to main chat", threadId, e);
                }
            }
            
            bot.execute(sendMessage);
        } catch (TelegramApiException e) {
            logger.error("Failed to send group message to chat {}", chatId, e);
        }
    }
}

