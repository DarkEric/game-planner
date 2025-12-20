package ru.ambryo.gameplannerback.service.telegram.message;

import org.springframework.stereotype.Component;

/**
 * Билдер сообщений справки
 */
@Component
public class HelpMessageBuilder {
    
    public String buildHelpMessage() {

        String message = "📖 <b>Справка по командам</b>\n\n" +
            "<b>Основные команды:</b>\n" +
            "/start - Начать работу с ботом\n" +
            "/menu - Главное меню\n" +
            "/help - Показать эту справку\n" +
            "/stop - Отписаться от уведомлений\n\n" +
            "<b>Игры:</b>\n" +
            "/games - Список предстоящих игр\n" +
            "/game <id> - Детали игры\n\n" +
            "<b>Регистрация и авторизация:</b>\n" +
            "/register - Регистрация нового аккаунта\n" +
            "/auth - Авторизация через логин/пароль\n" +
            "/link <token> - Привязка через токен\n\n" +
            "<b>Временные слоты:</b>\n" +
            "/mark - Разметить доступное время\n" +
            "/myslots - Показать мои временные слоты\n\n" +
            "<b>Инвайт-коды:</b>\n" +
            "/invite - Создать инвайт-код\n" +
            "/myinvites - Показать мои инвайт-коды\n\n" +
            "💡 Используйте /menu для доступа ко всем функциям через удобное меню.";
        
        return message;
    }
}

