package ru.ambryo.gameplannerback.service.telegram.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.ambryo.gameplannerback.service.telegram.util.TelegramMessageSender;

/**
 * Централизованная обработка ошибок Telegram бота
 */
@Component
public class TelegramExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(TelegramExceptionHandler.class);
    
    private final TelegramMessageSender messageSender;
    
    @Autowired
    public TelegramExceptionHandler(AbsSender bot) {
        this.messageSender = new TelegramMessageSender(bot);
    }
    
    /**
     * Обрабатывает исключение и отправляет пользователю сообщение об ошибке
     * @param chatId ID чата
     * @param exception исключение
     * @param defaultMessage сообщение по умолчанию
     */
    public void handleException(String chatId, Exception exception, String defaultMessage) {
        logger.error("Telegram bot error in chat {}: {}", chatId, exception.getMessage(), exception);
        
        String userMessage = defaultMessage;
        
        // Специфичная обработка для разных типов ошибок
        if (exception instanceof TelegramApiException) {
            TelegramApiException telegramException = (TelegramApiException) exception;
            String errorMessage = telegramException.getMessage();
            if (errorMessage != null) {
                if (errorMessage.contains("400") || errorMessage.contains("Bad Request")) {
                    userMessage = "❌ Ошибка: Неверный запрос. Проверьте формат данных.";
                } else if (errorMessage.contains("403") || errorMessage.contains("Forbidden")) {
                    userMessage = "❌ Ошибка: Доступ запрещен. Возможно, бот был заблокирован.";
                } else if (errorMessage.contains("429") || errorMessage.contains("Too Many Requests")) {
                    userMessage = "❌ Ошибка: Превышен лимит запросов. Попробуйте позже.";
                }
            }
        }
        
        try {
            messageSender.sendPersonalMessage(chatId, userMessage);
        } catch (Exception e) {
            logger.error("Failed to send error message to chat {}", chatId, e);
        }
    }
    
    /**
     * Обрабатывает исключение без отправки сообщения пользователю
     * @param exception исключение
     * @param context контекст ошибки
     */
    public void logException(Exception exception, String context) {
        logger.error("Telegram bot error in context '{}': {}", context, exception.getMessage(), exception);
    }
}

