package ru.ambryo.gameplannerback.service.telegram.menu;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.ambryo.gameplannerback.entity.User;
import ru.ambryo.gameplannerback.repository.UserRepository;
import ru.ambryo.gameplannerback.service.NotificationSettingsService;
import ru.ambryo.gameplannerback.service.telegram.keyboard.NotificationsMenuKeyboardBuilder;
import ru.ambryo.gameplannerback.service.telegram.message.NotificationMessageBuilder;

/**
 * Обработчик меню уведомлений
 * 
 * Примечание: Обработка уведомлений очень сложная и включает много методов.
 * Этот обработчик обрабатывает основные callback'ы, остальная логика может оставаться в основном сервисе
 * до полной миграции всех методов обработки уведомлений.
 */
@Component
public class NotificationsMenuHandler implements MenuHandler {
    
    private final UserRepository userRepository;
    private final NotificationSettingsService notificationSettingsService;
    private final NotificationMessageBuilder messageBuilder;
    private final NotificationsMenuKeyboardBuilder keyboardBuilder;
    private final MenuMessageUpdater messageUpdater;
    
    @Autowired
    public NotificationsMenuHandler(
            UserRepository userRepository,
            NotificationSettingsService notificationSettingsService,
            NotificationMessageBuilder messageBuilder,
            NotificationsMenuKeyboardBuilder keyboardBuilder,
            MenuMessageUpdater messageUpdater) {
        this.userRepository = userRepository;
        this.notificationSettingsService = notificationSettingsService;
        this.messageBuilder = messageBuilder;
        this.keyboardBuilder = keyboardBuilder;
        this.messageUpdater = messageUpdater;
    }
    
    @Override
    public boolean canHandle(String callbackData) {
        return callbackData.equals("menu_settings_notifications") ||
               callbackData.startsWith("notification_set_") ||
               callbackData.equals("notification_reminders") ||
               callbackData.equals("notification_reminder_add") ||
               callbackData.startsWith("notification_reminder_") ||
               callbackData.equals("notification_timeslot_reminder") ||
               callbackData.startsWith("notification_timeslot_reminder_") ||
               callbackData.startsWith("notification_cron_");
    }
    
    @Override
    public void handle(CallbackQuery callbackQuery, Long telegramUserId, String chatId, Integer messageId) {
        String data = callbackQuery.getData();
        User user = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
        
        if (user == null) {
            messageUpdater.answerCallback(callbackQuery.getId(), "❌ Аккаунт не связан. Зарегистрируйтесь для доступа к функциям.");
            return;
        }
        
        if (data.equals("menu_settings_notifications")) {
            handleNotificationsMenu(user, chatId, messageId);
        } else {
            // Остальные callback'ы уведомлений требуют более сложной логики
            // и могут обрабатываться через делегирование в основной сервис
            // или через дополнительные специализированные обработчики
            messageUpdater.answerCallback(callbackQuery.getId(), "ℹ️ Обработка этого callback требует дополнительной логики");
        }
    }
    
    private void handleNotificationsMenu(User user, String chatId, Integer messageId) {
        try {
            var settings = notificationSettingsService.getSettings(user.getId());
            String message = messageBuilder.buildNotificationSettingsMessage(settings);
            var keyboard = keyboardBuilder.buildNotificationsMenu(settings);
            messageUpdater.updateMessage(chatId, messageId, message, keyboard);
        } catch (Exception e) {
            messageUpdater.answerCallback("", "❌ Ошибка при получении настроек уведомлений.");
        }
    }
}
