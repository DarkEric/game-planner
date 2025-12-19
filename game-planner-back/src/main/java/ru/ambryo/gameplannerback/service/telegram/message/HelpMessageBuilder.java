package ru.ambryo.gameplannerback.service.telegram.message;

import org.springframework.stereotype.Component;

/**
 * Билдер сообщений справки
 */
@Component
public class HelpMessageBuilder {
    
    public String buildHelpMessage() {
        StringBuilder message = new StringBuilder();
        message.append("📖 <b>Справка по командам</b>\n\n");
        message.append("<b>Основные команды:</b>\n");
        message.append("/start - Начать работу с ботом\n");
        message.append("/menu - Главное меню\n");
        message.append("/help - Показать эту справку\n");
        message.append("/stop - Отписаться от уведомлений\n\n");
        
        message.append("<b>Игры:</b>\n");
        message.append("/games - Список предстоящих игр\n");
        message.append("/game <id> - Детали игры\n\n");
        
        message.append("<b>Регистрация и авторизация:</b>\n");
        message.append("/register - Регистрация нового аккаунта\n");
        message.append("/auth - Авторизация через логин/пароль\n");
        message.append("/link <token> - Привязка через токен\n\n");
        
        message.append("<b>Временные слоты:</b>\n");
        message.append("/mark - Разметить доступное время\n");
        message.append("/myslots - Показать мои временные слоты\n\n");
        
        message.append("<b>Инвайт-коды:</b>\n");
        message.append("/invite - Создать инвайт-код\n");
        message.append("/myinvites - Показать мои инвайт-коды\n\n");
        
        message.append("💡 Используйте /menu для доступа ко всем функциям через удобное меню.");
        
        return message.toString();
    }
}

