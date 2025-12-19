package ru.ambryo.gameplannerback.service.telegram.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
            if (telegramException.getErrorCode() == 400) {
                userMessage = "❌ Ошибка: Неверный запрос. Проверьте формат данных.";
            } else if (telegramException.getErrorCode() == 403) {
                userMessage = "❌ Ошибка: Доступ запрещен. Возможно, бот был заблокирован.";
            } else if (telegramException.getErrorCode() == 429) {
                userMessage = "❌ Ошибка: Превышен лимит запросов. Попробуйте позже.";
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

