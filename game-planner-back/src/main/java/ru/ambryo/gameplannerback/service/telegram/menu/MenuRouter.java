package ru.ambryo.gameplannerback.service.telegram.menu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

import java.util.List;

/**
 * Роутер для обработки callback'ов меню Telegram бота
 */
@Component
public class MenuRouter {
    
    private static final Logger logger = LoggerFactory.getLogger(MenuRouter.class);
    
    private final List<MenuHandler> handlers;
    
    @Autowired
    public MenuRouter(List<MenuHandler> handlers) {
        this.handlers = handlers;
    }
    
    /**
     * Обрабатывает callback query
     * @param callbackQuery callback query
     * @param telegramUserId ID пользователя Telegram
     * @param chatId ID чата
     * @param messageId ID сообщения
     */
    public void handle(CallbackQuery callbackQuery, Long telegramUserId, String chatId, Integer messageId) {
        String data = callbackQuery.getData();
        
        for (MenuHandler handler : handlers) {
            if (handler.canHandle(data)) {
                try {
                    handler.handle(callbackQuery, telegramUserId, chatId, messageId);
                    return;
                } catch (Exception e) {
                    logger.error("Error handling menu callback {} with handler {}", data, handler.getClass().getSimpleName(), e);
                }
            }
        }
        
        logger.warn("No handler found for menu callback: {}", data);
    }
}

