package ru.ambryo.gameplannerback.service.telegram.menu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * Утилита для обновления сообщений меню
 */
@Component
public class MenuMessageUpdater {
    
    private static final Logger logger = LoggerFactory.getLogger(MenuMessageUpdater.class);
    
    private final AbsSender bot;
    
    @Autowired
    public MenuMessageUpdater(@Lazy AbsSender bot) {
        this.bot = bot;
    }
    
    /**
     * Обновляет сообщение меню
     */
    public void updateMessage(String chatId, Integer messageId, String message, InlineKeyboardMarkup keyboard) {
        try {
            EditMessageText editMessage = new EditMessageText();
            editMessage.setChatId(chatId);
            editMessage.setMessageId(messageId);
            editMessage.setText(message);
            editMessage.setParseMode("HTML");
            editMessage.setReplyMarkup(keyboard);
            bot.execute(editMessage);
        } catch (TelegramApiException e) {
            logger.error("Failed to update menu message", e);
        }
    }
    
    /**
     * Отвечает на callback query
     */
    public void answerCallback(String callbackQueryId) {
        answerCallback(callbackQueryId, null);
    }
    
    /**
     * Отвечает на callback query с текстом
     */
    public void answerCallback(String callbackQueryId, String text) {
        try {
            AnswerCallbackQuery answer = new AnswerCallbackQuery();
            answer.setCallbackQueryId(callbackQueryId);
            if (text != null && !text.isEmpty()) {
                answer.setText(text);
            }
            bot.execute(answer);
        } catch (TelegramApiException e) {
            logger.error("Failed to answer callback query", e);
        }
    }
}

