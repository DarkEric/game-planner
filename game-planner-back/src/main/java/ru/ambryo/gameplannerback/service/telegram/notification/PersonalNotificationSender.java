package ru.ambryo.gameplannerback.service.telegram.notification;

import org.telegram.telegrambots.meta.bots.AbsSender;
import ru.ambryo.gameplannerback.entity.User;

/**
 * Базовый класс для отправки персональных уведомлений
 */
public abstract class PersonalNotificationSender {
    
    protected final AbsSender bot;
    
    public PersonalNotificationSender(AbsSender bot) {
        this.bot = bot;
    }
    
    protected void sendPersonalMessage(String chatId, String message) {
        try {
            org.telegram.telegrambots.meta.api.methods.send.SendMessage sendMessage = 
                new org.telegram.telegrambots.meta.api.methods.send.SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText(message);
            sendMessage.setParseMode("HTML");
            bot.execute(sendMessage);
        } catch (org.telegram.telegrambots.meta.exceptions.TelegramApiException e) {
            // Логирование должно быть в подклассах
        }
    }
    
    protected boolean canSendToUser(User user) {
        return user != null 
            && user.getTelegramSubscribed() != null 
            && user.getTelegramSubscribed() 
            && user.getTelegramChatId() != null;
    }
}

