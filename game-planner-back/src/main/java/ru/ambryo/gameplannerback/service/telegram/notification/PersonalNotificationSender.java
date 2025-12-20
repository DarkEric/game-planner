package ru.ambryo.gameplannerback.service.telegram.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.bots.AbsSender;
import ru.ambryo.gameplannerback.entity.User;
import ru.ambryo.gameplannerback.service.telegram.util.TelegramHtmlFormatter;

/**
 * Базовый класс для отправки персональных уведомлений
 */
public abstract class PersonalNotificationSender {
    
    protected static final Logger logger = LoggerFactory.getLogger(PersonalNotificationSender.class);
    
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
        return user == null
            || user.getTelegramSubscribed() == null
            || !user.getTelegramSubscribed()
            || user.getTelegramChatId() == null;
    }
    
    /**
     * Отправляет напоминание о разметке времени
     * @param user пользователь
     */
    public void sendTimeSlotReminder(User user) {
        if (canSendToUser(user)) {
            return;
        }
        
        String message = "📅 <b>Напоминание</b>\n\nНе забудьте разметить ваше доступное время в календаре!";
        sendPersonalMessage(user.getTelegramChatId(), message);
    }
    
    /**
     * Отправляет токен сброса пароля пользователю
     * @param user пользователь
     * @param token токен для сброса пароля
     */
    public void sendPasswordResetToken(User user, String token) {
        if (canSendToUser(user)) {
            logger.debug("User {} is not subscribed to Telegram or chat ID not available", user.getUsername());
            return;
        }
        
        try {
            String message = "🔐 <b>Сброс пароля</b>\n\n" +
                "Вы запросили сброс пароля для аккаунта: <b>" + TelegramHtmlFormatter.escapeHtml(user.getUsername()) + "</b>\n\n" +
                "Ваш код для сброса: <code>" + token + "</code>\n\n" +
                "Используйте этот код на странице восстановления пароля.\n\n" +
                "⚠️ Код действителен 1 час.\n\n" +
                "Если вы не запрашивали сброс пароля, проигнорируйте это сообщение.";
            
            sendPersonalMessage(user.getTelegramChatId(), message);
            logger.info("Password reset token sent to user {} via Telegram", user.getUsername());
        } catch (Exception e) {
            logger.error("Failed to send password reset token to user {} via Telegram", user.getUsername(), e);
            throw e; // Пробрасываем исключение, чтобы вызывающий код мог его обработать
        }
    }
}

