package ru.ambryo.gameplannerback.service.telegram.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;
import ru.ambryo.gameplannerback.service.telegram.state.*;
import ru.ambryo.gameplannerback.service.telegram.util.TelegramMessageSender;

/**
 * Обработчик команды /cancel
 */
@Component
public class CancelCommandHandler implements CommandHandler {
    
    private final TelegramMessageSender messageSender;
    private final AuthStateManager authStateManager;
    private final RegistrationStateManager registrationStateManager;
    private final TimeSlotMarkingStateManager timeSlotMarkingStateManager;
    private final TimezoneChangeStateManager timezoneChangeStateManager;
    private final NotificationStateManager notificationStateManager;
    
    @Autowired
    public CancelCommandHandler(
            @Lazy AbsSender bot,
            AuthStateManager authStateManager,
            RegistrationStateManager registrationStateManager,
            TimeSlotMarkingStateManager timeSlotMarkingStateManager,
            TimezoneChangeStateManager timezoneChangeStateManager,
            NotificationStateManager notificationStateManager) {
        this.messageSender = new TelegramMessageSender(bot);
        this.authStateManager = authStateManager;
        this.registrationStateManager = registrationStateManager;
        this.timeSlotMarkingStateManager = timeSlotMarkingStateManager;
        this.timezoneChangeStateManager = timezoneChangeStateManager;
        this.notificationStateManager = notificationStateManager;
    }
    
    @Override
    public boolean canHandle(String command) {
        return "cancel".equals(command);
    }
    
    @Override
    public void handle(Message message, Long telegramUserId, String chatId) {
        boolean hasAuth = authStateManager.hasState(chatId);
        boolean hasRegistration = registrationStateManager.hasState(chatId);
        boolean hasMarking = timeSlotMarkingStateManager.hasState(chatId);
        boolean hasTimezoneChange = timezoneChangeStateManager.hasState(chatId);
        boolean hasNotification = notificationStateManager.hasState(chatId);
        
        if (hasRegistration) {
            registrationStateManager.clearState(chatId);
            messageSender.sendPersonalMessage(chatId, """
                ✅ Процесс регистрации отменен.
                
                Используйте /register для начала заново.""");
        } else if (hasAuth) {
            authStateManager.clearState(chatId);
            messageSender.sendPersonalMessage(chatId, """
                ✅ Процесс авторизации отменен.
                
                Используйте /auth для начала заново или /link <token> для привязки через токен.""");
        } else if (hasMarking) {
            timeSlotMarkingStateManager.clearState(chatId);
            messageSender.sendPersonalMessage(chatId, """
                ✅ Процесс разметки времени отменен.
                
                Используйте /mark для начала заново.""");
        } else if (hasTimezoneChange) {
            timezoneChangeStateManager.clearState(chatId);
            messageSender.sendPersonalMessage(chatId, """
                ✅ Процесс смены часового пояса отменен.
                
                Используйте меню для начала заново.""");
        } else if (hasNotification) {
            notificationStateManager.clearState(chatId);
            messageSender.sendPersonalMessage(chatId, """
                ✅ Процесс настройки уведомлений отменен.
                
                Используйте меню для начала заново.""");
        } else {
            messageSender.sendPersonalMessage(chatId, "ℹ️ Нет активного процесса для отмены.");
        }
    }
}

