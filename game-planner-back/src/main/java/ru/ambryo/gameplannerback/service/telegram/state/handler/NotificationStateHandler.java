package ru.ambryo.gameplannerback.service.telegram.state.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.bots.AbsSender;
import ru.ambryo.gameplannerback.dto.UpcomingGameReminderDto;
import ru.ambryo.gameplannerback.dto.UserNotificationSettingsDto;
import ru.ambryo.gameplannerback.entity.User;
import ru.ambryo.gameplannerback.repository.UserRepository;
import ru.ambryo.gameplannerback.service.NotificationSettingsService;
import ru.ambryo.gameplannerback.service.telegram.keyboard.NotificationsMenuKeyboardBuilder;
import ru.ambryo.gameplannerback.service.telegram.state.NotificationStateManager;
import ru.ambryo.gameplannerback.service.telegram.util.CronExpressionBuilder;
import ru.ambryo.gameplannerback.service.telegram.util.TelegramDateParser;
import ru.ambryo.gameplannerback.service.telegram.util.TelegramHtmlFormatter;
import ru.ambryo.gameplannerback.service.telegram.util.TelegramMessageSender;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Обработчик состояний настроек уведомлений
 */
@Component
public class NotificationStateHandler implements StateHandler<NotificationStateManager.NotificationState> {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationStateHandler.class);
    
    private final NotificationStateManager notificationStateManager;
    private final UserRepository userRepository;
    private final NotificationSettingsService notificationSettingsService;
    private final NotificationsMenuKeyboardBuilder notificationsMenuKeyboardBuilder;
    private final TelegramMessageSender messageSender;
    
    @Autowired
    public NotificationStateHandler(
            NotificationStateManager notificationStateManager,
            UserRepository userRepository,
            NotificationSettingsService notificationSettingsService,
            NotificationsMenuKeyboardBuilder notificationsMenuKeyboardBuilder,
            @Lazy AbsSender bot) {
        this.notificationStateManager = notificationStateManager;
        this.userRepository = userRepository;
        this.notificationSettingsService = notificationSettingsService;
        this.notificationsMenuKeyboardBuilder = notificationsMenuKeyboardBuilder;
        this.messageSender = new TelegramMessageSender(bot);
    }
    
    @Override
    public boolean canHandle(String chatId, NotificationStateManager.NotificationState state) {
        return notificationStateManager.hasState(chatId) && notificationStateManager.getState(chatId) == state;
    }
    
    @Override
    public void handle(Long telegramUserId, String chatId, String text, NotificationStateManager.NotificationState state) {
        try {
            notificationStateManager.updateTimestamp(chatId);
            
            User user = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
            if (user == null) {
                notificationStateManager.clearState(chatId);
                messageSender.sendPersonalMessage(chatId, "❌ Ваш аккаунт не связан. Используйте /link для связывания.");
                return;
            }
            
            var data = notificationStateManager.getData(chatId);
            if (data == null) {
                notificationStateManager.clearState(chatId);
                messageSender.sendPersonalMessage(chatId, "❌ Ошибка: данные не найдены. Начните заново.");
                return;
            }
            
            if (state == NotificationStateManager.NotificationState.WAITING_REMINDER_VALUE) {
                handleReminderValueInput(user, chatId, text, data);
            } else if (state == NotificationStateManager.NotificationState.WAITING_REMINDER_UNIT) {
                handleReminderUnitConfirmation(user, chatId, text, data);
            } else if (state == NotificationStateManager.NotificationState.WAITING_CRON_TIME) {
                handleCronTimeInput(user, chatId, text, data);
            } else if (state == NotificationStateManager.NotificationState.WAITING_CRON_DAY && "monthly".equals(data.cronFrequency)) {
                handleCronDayOfMonthInput(user, chatId, text, data);
            }
        } catch (Exception e) {
            logger.error("Error handling notification state", e);
            notificationStateManager.clearState(chatId);
            messageSender.sendPersonalMessage(chatId, "❌ Произошла ошибка. Попробуйте позже.");
        }
    }
    
    private void handleReminderValueInput(User user, String chatId, String text, NotificationStateManager.NotificationData data) {
        try {
            int value = Integer.parseInt(text.trim());
            if (value <= 0) {
                messageSender.sendPersonalMessage(chatId, "❌ Значение должно быть положительным числом. Введите значение:");
                return;
            }
            
            data.reminderValue = value;
            notificationStateManager.setData(chatId, data);
            notificationStateManager.setState(chatId, NotificationStateManager.NotificationState.WAITING_REMINDER_UNIT);
            
            String message = "✅ Значение принято: <b>" + value + "</b>\n\n" +
                    "Выберите единицу измерения:";
            
            InlineKeyboardMarkup keyboard = notificationsMenuKeyboardBuilder.buildReminderUnitMenu();
            messageSender.sendPersonalMessage(chatId, message);
            messageSender.sendMessageWithKeyboard(chatId, "Выберите единицу:", keyboard);
        } catch (NumberFormatException e) {
            messageSender.sendPersonalMessage(chatId, "❌ Неверный формат. Введите положительное число:");
        }
    }
    
    private void handleReminderUnitConfirmation(User user, String chatId, String text, NotificationStateManager.NotificationData data) {
        String lowerText = text.trim().toLowerCase();
        boolean enabled = lowerText.equals("да") || lowerText.equals("yes") || lowerText.equals("включить") || lowerText.equals("on");
        
        UserNotificationSettingsDto settings = notificationSettingsService.getSettings(user.getId());
        List<UpcomingGameReminderDto> reminders = settings.getUpcomingGameReminders();
        if (reminders == null) {
            reminders = new ArrayList<>();
        }
        
        int minutesBefore = convertToMinutes(data.reminderValue, data.reminderUnit);
        
        if (data.reminderIndex == -1) {
            if (reminders.size() >= 5) {
                notificationStateManager.clearState(chatId);
                messageSender.sendPersonalMessage(chatId, "❌ Максимум 5 напоминаний. Удалите одно из существующих.");
                return;
            }
            reminders.add(new UpcomingGameReminderDto(minutesBefore, enabled));
        } else {
            if (data.reminderIndex >= 0 && data.reminderIndex < reminders.size()) {
                UpcomingGameReminderDto reminder = reminders.get(data.reminderIndex);
                reminder.setMinutesBefore(minutesBefore);
                reminder.setEnabled(enabled);
            } else {
                notificationStateManager.clearState(chatId);
                messageSender.sendPersonalMessage(chatId, "❌ Напоминание не найдено.");
                return;
            }
        }
        
        settings.setUpcomingGameReminders(reminders);
        notificationSettingsService.updateSettings(user.getId(), settings);
        notificationStateManager.clearState(chatId);
        
        String displayValue = formatReminderValue(minutesBefore);
        messageSender.sendPersonalMessage(chatId, "✅ <b>Напоминание " + (data.reminderIndex == -1 ? "добавлено" : "изменено") + "!</b>\n\n" +
                "Значение: <b>" + TelegramHtmlFormatter.escapeHtml(displayValue) + "</b>\n" +
                "Статус: <b>" + (enabled ? "Включено" : "Выключено") + "</b>");
        
        logger.info("Reminder {} via Telegram for user: {}, value: {} minutes, enabled: {}", 
                data.reminderIndex == -1 ? "added" : "updated", user.getUsername(), minutesBefore, enabled);
    }
    
    private void handleCronTimeInput(User user, String chatId, String text, NotificationStateManager.NotificationData data) {
        LocalTime time = TelegramDateParser.parseTime(text.trim());
        if (time == null) {
            messageSender.sendPersonalMessage(chatId, """
                ❌ <b>Неверный формат времени</b>
                
                Введите время в формате ЧЧ:ММ (например: 09:00):
                
                💡 Используйте /cancel для отмены.""");
            return;
        }
        
        data.cronTime = String.format("%02d:%02d", time.getHour(), time.getMinute());
        notificationStateManager.setData(chatId, data);
        
        String cron = CronExpressionBuilder.buildCron(data.cronFrequency, data.cronTime, data.cronDay);
        UserNotificationSettingsDto settings = notificationSettingsService.getSettings(user.getId());
        settings.setTimeSlotReminderCron(cron);
        notificationSettingsService.updateSettings(user.getId(), settings);
        
        notificationStateManager.clearState(chatId);
        
        String cronText = CronExpressionBuilder.formatCronToReadable(cron);
        messageSender.sendPersonalMessage(chatId, "✅ <b>Расписание настроено!</b>\n\n" +
                "Расписание: <b>" + TelegramHtmlFormatter.escapeHtml(cronText) + "</b>");
        
        logger.info("Time slot reminder cron updated via Telegram for user: {}, cron: {}", user.getUsername(), cron);
    }
    
    private void handleCronDayOfMonthInput(User user, String chatId, String text, NotificationStateManager.NotificationData data) {
        try {
            int dayOfMonth = Integer.parseInt(text.trim());
            if (dayOfMonth < 1 || dayOfMonth > 31) {
                messageSender.sendPersonalMessage(chatId, """
                    ❌ <b>Неверный день месяца</b>
                    
                    Введите число от 1 до 31:
                    
                    💡 Используйте /cancel для отмены.""");
                return;
            }
            
            data.cronDay = dayOfMonth;
            notificationStateManager.setData(chatId, data);
            notificationStateManager.setState(chatId, NotificationStateManager.NotificationState.WAITING_CRON_TIME);
            notificationStateManager.updateTimestamp(chatId);
            
            messageSender.sendPersonalMessage(chatId, "✅ День месяца принят: <b>" + dayOfMonth + "</b>\n\n" +
                    "Введите время в формате ЧЧ:ММ (например: 09:00):\n\n" +
                    "💡 Используйте /cancel для отмены.");
        } catch (NumberFormatException e) {
            messageSender.sendPersonalMessage(chatId, """
                ❌ <b>Неверный формат</b>
                
                Введите число от 1 до 31:
                
                💡 Используйте /cancel для отмены.""");
        }
    }
    
    private int convertToMinutes(Integer value, String unit) {
        if (value == null) {
            return 0;
        }
        return switch (unit) {
            case "days" -> value * 24 * 60;
            case "hours" -> value * 60;
            default -> value;
        };
    }
    
    private String formatReminderValue(Integer minutesBefore) {
        if (minutesBefore == null) {
            return "0 минут";
        }
        
        if (minutesBefore % (24 * 60) == 0 && minutesBefore >= 24 * 60) {
            int days = minutesBefore / (24 * 60);
            return days + " " + (days == 1 ? "день" : (days < 5 ? "дня" : "дней"));
        } else if (minutesBefore % 60 == 0 && minutesBefore >= 60) {
            int hours = minutesBefore / 60;
            return hours + " " + (hours == 1 ? "час" : (hours < 5 ? "часа" : "часов"));
        } else {
            return minutesBefore + " " + (minutesBefore == 1 ? "минута" : (minutesBefore < 5 ? "минуты" : "минут"));
        }
    }
}


