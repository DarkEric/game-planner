package ru.ambryo.gameplannerback.service.telegram.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;
import ru.ambryo.gameplannerback.service.NotificationSettingsService;
import ru.ambryo.gameplannerback.service.telegram.util.TelegramMessageSender;

/**
 * Обработчик команды /link
 */
@Component
public class LinkCommandHandler implements CommandHandler {
    
    private final NotificationSettingsService notificationSettingsService;
    private final TelegramMessageSender messageSender;
    
    @Autowired
    public LinkCommandHandler(NotificationSettingsService notificationSettingsService, @Lazy AbsSender bot) {
        this.notificationSettingsService = notificationSettingsService;
        this.messageSender = new TelegramMessageSender(bot);
    }
    
    @Override
    public boolean canHandle(String command) {
        return "link".equals(command);
    }
    
    @Override
    public void handle(Message message, Long telegramUserId, String chatId) {
        String text = message.getText();
        String[] parts = text.split("\\s+", 2);
        
        if (parts.length < 2) {
            messageSender.sendPersonalMessage(chatId, "Использование: /link <token>\n\nПолучите токен в настройках профиля на веб-сайте.");
            return;
        }
        
        String token = parts[1];
        
        try {
            notificationSettingsService.linkTelegramAccount(token, telegramUserId, chatId);
            messageSender.sendPersonalMessage(chatId, """
                ✅ Аккаунт успешно связан!
                
                Теперь вы будете получать персональные уведомления.
                
                Доступные команды:
                /games - Список предстоящих игр
                /help - Справка по командам
                /stop - Отписаться от уведомлений""");
        } catch (Exception e) {
            messageSender.sendPersonalMessage(chatId, "❌ Ошибка: " + e.getMessage() + "\n\nПроверьте правильность токена и попробуйте снова.");
        }
    }
}

