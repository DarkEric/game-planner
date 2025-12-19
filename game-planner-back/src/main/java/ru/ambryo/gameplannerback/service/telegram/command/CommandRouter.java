package ru.ambryo.gameplannerback.service.telegram.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.List;

/**
 * Роутер команд для Telegram бота
 */
@Component
public class CommandRouter {
    
    private static final Logger logger = LoggerFactory.getLogger(CommandRouter.class);
    
    private final List<CommandHandler> handlers;
    
    @Autowired
    public CommandRouter(List<CommandHandler> handlers) {
        this.handlers = handlers;
    }
    
    /**
     * Обрабатывает сообщение с командой
     * @param message сообщение
     * @param telegramUserId ID пользователя Telegram
     * @param chatId ID чата
     */
    public void handle(Message message, Long telegramUserId, String chatId) {
        String text = message.getText();
        if (text == null || !text.startsWith("/")) {
            return;
        }
        
        String command = extractCommand(text);
        
        for (CommandHandler handler : handlers) {
            if (handler.canHandle(command)) {
                try {
                    handler.handle(message, telegramUserId, chatId);
                    return;
                } catch (Exception e) {
                    logger.error("Error handling command {} with handler {}", command, handler.getClass().getSimpleName(), e);
                }
            }
        }
        
        logger.warn("No handler found for command: {}", command);
    }
    
    private String extractCommand(String text) {
        int spaceIndex = text.indexOf(' ');
        if (spaceIndex > 0) {
            return text.substring(1, spaceIndex);
        }
        return text.substring(1);
    }
}

