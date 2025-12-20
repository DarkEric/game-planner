package ru.ambryo.gameplannerback.service.telegram.menu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.ambryo.gameplannerback.dto.UpcomingGameReminderDto;
import ru.ambryo.gameplannerback.dto.UserNotificationSettingsDto;
import ru.ambryo.gameplannerback.entity.User;
import ru.ambryo.gameplannerback.repository.UserRepository;
import ru.ambryo.gameplannerback.service.NotificationSettingsService;
import ru.ambryo.gameplannerback.service.telegram.keyboard.NotificationsMenuKeyboardBuilder;
import ru.ambryo.gameplannerback.service.telegram.message.NotificationMessageBuilder;
import ru.ambryo.gameplannerback.service.telegram.state.NotificationStateManager;
import ru.ambryo.gameplannerback.service.telegram.util.CronExpressionBuilder;
import ru.ambryo.gameplannerback.service.telegram.util.CronExpressionParser;
import ru.ambryo.gameplannerback.service.telegram.util.TelegramHtmlFormatter;

import java.util.List;

/**
 * Обработчик меню уведомлений
 */
@Component
public class NotificationsMenuHandler implements MenuHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationsMenuHandler.class);
    
    private final UserRepository userRepository;
    private final NotificationSettingsService notificationSettingsService;
    private final NotificationMessageBuilder messageBuilder;
    private final NotificationsMenuKeyboardBuilder keyboardBuilder;
    private final MenuMessageUpdater messageUpdater;
    private final NotificationStateManager notificationStateManager;
    private final AbsSender bot;
    
    @Autowired
    public NotificationsMenuHandler(
            UserRepository userRepository,
            NotificationSettingsService notificationSettingsService,
            NotificationMessageBuilder messageBuilder,
            NotificationsMenuKeyboardBuilder keyboardBuilder,
            MenuMessageUpdater messageUpdater,
            NotificationStateManager notificationStateManager,
            AbsSender bot) {
        this.userRepository = userRepository;
        this.notificationSettingsService = notificationSettingsService;
        this.messageBuilder = messageBuilder;
        this.keyboardBuilder = keyboardBuilder;
        this.messageUpdater = messageUpdater;
        this.notificationStateManager = notificationStateManager;
        this.bot = bot;
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
        
        try {
            if (data.equals("menu_settings_notifications")) {
                handleNotificationsMenu(user, chatId, messageId);
            } else if (data.startsWith("notification_set_")) {
                handleNotificationSettingChange(user, chatId, messageId, data, callbackQuery.getId());
            } else if (data.equals("notification_reminders")) {
                handleMenuReminders(user, chatId, messageId);
            } else if (data.equals("notification_reminder_add")) {
                handleReminderAdd(user, chatId, messageId);
            } else if (data.startsWith("notification_reminder_edit_")) {
                int index = Integer.parseInt(data.substring("notification_reminder_edit_".length()));
                handleReminderEdit(user, chatId, messageId, index);
            } else if (data.startsWith("notification_reminder_delete_")) {
                int index = Integer.parseInt(data.substring("notification_reminder_delete_".length()));
                handleReminderDelete(user, chatId, messageId, index, callbackQuery.getId());
            } else if (data.startsWith("notification_reminder_toggle_")) {
                int index = Integer.parseInt(data.substring("notification_reminder_toggle_".length()));
                handleReminderToggle(user, chatId, messageId, index, callbackQuery.getId());
            } else if (data.startsWith("notification_reminder_unit_")) {
                String unit = data.substring("notification_reminder_unit_".length());
                handleReminderUnitSelect(user, chatId, messageId, unit, callbackQuery.getId());
            } else if (data.equals("notification_timeslot_reminder")) {
                handleMenuTimeSlotReminder(user, chatId, messageId);
            } else if (data.equals("notification_timeslot_reminder_toggle")) {
                handleTimeSlotReminderToggle(user, chatId, messageId, callbackQuery.getId());
            } else if (data.equals("notification_timeslot_reminder_cron")) {
                handleTimeSlotReminderCron(user, chatId, messageId);
            } else if (data.startsWith("notification_cron_frequency_")) {
                String frequency = data.substring("notification_cron_frequency_".length());
                handleCronFrequencySelect(user, chatId, messageId, frequency);
            } else if (data.startsWith("notification_cron_day_")) {
                int day = Integer.parseInt(data.substring("notification_cron_day_".length()));
                handleCronDaySelect(user, chatId, messageId, day, callbackQuery.getId());
            }
        } catch (Exception e) {
            logger.error("Error handling notification callback: {}", data, e);
            messageUpdater.answerCallback(callbackQuery.getId(), "❌ Произошла ошибка.");
        }
    }
    
    private void handleNotificationsMenu(User user, String chatId, Integer messageId) {
        try {
            var settings = notificationSettingsService.getSettings(user.getId());
            String message = messageBuilder.buildNotificationSettingsMessage(settings);
            var keyboard = keyboardBuilder.buildNotificationsMenu(settings);
            messageUpdater.updateMessage(chatId, messageId, message, keyboard);
        } catch (Exception e) {
            logger.error("Error handling notifications menu", e);
            messageUpdater.answerCallback("", "❌ Ошибка при получении настроек уведомлений.");
        }
    }
    
    private void handleNotificationSettingChange(User user, String chatId, Integer messageId, String callbackData, String callbackQueryId) {
        try {
            UserNotificationSettingsDto settings = notificationSettingsService.getSettings(user.getId());
            
            if (callbackData.equals("notification_set_gameCreated")) {
                showGameCreatedMenu(chatId, messageId, settings);
            } else if (callbackData.equals("notification_set_gameCancelled")) {
                showGameCancelledMenu(chatId, messageId, settings);
            } else if (callbackData.equals("notification_set_gameHeld")) {
                showGameHeldMenu(chatId, messageId, settings);
            } else if (callbackData.equals("notification_set_gameRemovedFromGame")) {
                String newValue = "ALL".equals(settings.getGameRemovedFromGame()) ? "NONE" : "ALL";
                settings.setGameRemovedFromGame(newValue);
                notificationSettingsService.updateSettings(user.getId(), settings);
                messageUpdater.answerCallback(callbackQueryId, "✅ Настройка изменена!");
                handleNotificationsMenu(user, chatId, messageId);
            } else if (callbackData.equals("notification_set_gameCompletionReminder")) {
                boolean newValue = !(settings.getGameCompletionReminderEnabled() != null && settings.getGameCompletionReminderEnabled());
                settings.setGameCompletionReminderEnabled(newValue);
                notificationSettingsService.updateSettings(user.getId(), settings);
                messageUpdater.answerCallback(callbackQueryId, "✅ Настройка изменена!");
                handleNotificationsMenu(user, chatId, messageId);
            } else if (callbackData.startsWith("notification_set_gameCreated_")) {
                String value = callbackData.substring("notification_set_gameCreated_".length());
                settings.setGameCreated(value);
                notificationSettingsService.updateSettings(user.getId(), settings);
                messageUpdater.answerCallback(callbackQueryId, "✅ Настройка изменена!");
                handleNotificationsMenu(user, chatId, messageId);
            } else if (callbackData.startsWith("notification_set_gameCancelled_")) {
                String value = callbackData.substring("notification_set_gameCancelled_".length());
                settings.setGameCancelled(value);
                notificationSettingsService.updateSettings(user.getId(), settings);
                messageUpdater.answerCallback(callbackQueryId, "✅ Настройка изменена!");
                handleNotificationsMenu(user, chatId, messageId);
            } else if (callbackData.startsWith("notification_set_gameHeld_")) {
                String value = callbackData.substring("notification_set_gameHeld_".length());
                settings.setGameHeld(value);
                notificationSettingsService.updateSettings(user.getId(), settings);
                messageUpdater.answerCallback(callbackQueryId, "✅ Настройка изменена!");
                handleNotificationsMenu(user, chatId, messageId);
            }
        } catch (Exception e) {
            logger.error("Error handling notification setting change", e);
            messageUpdater.answerCallback(callbackQueryId, "❌ Ошибка при изменении настройки");
        }
    }
    
    private void showGameCreatedMenu(String chatId, Integer messageId, UserNotificationSettingsDto settings) {
        String message = "🎮 <b>Игра создана</b>\n\n" +
                "Выберите, когда получать уведомления:";
        var keyboard = keyboardBuilder.buildGameSettingMenu("notification_set_gameCreated", settings.getGameCreated());
        messageUpdater.updateMessage(chatId, messageId, message, keyboard);
    }
    
    private void showGameCancelledMenu(String chatId, Integer messageId, UserNotificationSettingsDto settings) {
        String message = "❌ <b>Игра отменена</b>\n\n" +
                "Выберите, когда получать уведомления:";
        var keyboard = keyboardBuilder.buildGameSettingMenu("notification_set_gameCancelled", settings.getGameCancelled());
        messageUpdater.updateMessage(chatId, messageId, message, keyboard);
    }
    
    private void showGameHeldMenu(String chatId, Integer messageId, UserNotificationSettingsDto settings) {
        String message = "✅ <b>Игра проведена</b>\n\n" +
                "Выберите, когда получать уведомления:";
        var keyboard = keyboardBuilder.buildGameSettingMenu("notification_set_gameHeld", settings.getGameHeld());
        messageUpdater.updateMessage(chatId, messageId, message, keyboard);
    }
    
    private void handleMenuReminders(User user, String chatId, Integer messageId) {
        try {
            UserNotificationSettingsDto settings = notificationSettingsService.getSettings(user.getId());
            List<UpcomingGameReminderDto> reminders = settings.getUpcomingGameReminders();
            if (reminders == null) {
                reminders = new java.util.ArrayList<>();
            }
            
            String message = messageBuilder.buildRemindersListMessage(reminders);
            var keyboard = keyboardBuilder.buildRemindersMenu(reminders);
            messageUpdater.updateMessage(chatId, messageId, message, keyboard);
        } catch (Exception e) {
            logger.error("Error handling menu reminders", e);
            messageUpdater.answerCallback("", "❌ Ошибка при получении списка напоминаний.");
        }
    }
    
    private void handleReminderAdd(User user, String chatId, Integer messageId) {
        try {
            UserNotificationSettingsDto settings = notificationSettingsService.getSettings(user.getId());
            List<UpcomingGameReminderDto> reminders = settings.getUpcomingGameReminders();
            if (reminders == null) {
                reminders = new java.util.ArrayList<>();
            }
            
            if (reminders.size() >= 5) {
                messageUpdater.answerCallback("", "❌ Максимум 5 напоминаний");
                return;
            }
            
            notificationStateManager.setState(chatId, NotificationStateManager.NotificationState.WAITING_REMINDER_VALUE);
            var data = new NotificationStateManager.NotificationData();
            data.reminderIndex = -1;
            notificationStateManager.setData(chatId, data);
            
            String message = "⏰ <b>Добавление напоминания</b>\n\n" +
                    "Введите значение (например: 60 для 60 минут, 2 для 2 часов, 1 для 1 дня):\n\n" +
                    "💡 Используйте /cancel для отмены.";
            
            sendPersonalMessage(chatId, message);
            
            String menuMessage = messageBuilder.buildRemindersListMessage(reminders);
            var keyboard = keyboardBuilder.buildRemindersMenu(reminders);
            messageUpdater.updateMessage(chatId, messageId, menuMessage, keyboard);
        } catch (Exception e) {
            logger.error("Error handling reminder add", e);
            messageUpdater.answerCallback("", "❌ Ошибка при добавлении напоминания");
        }
    }
    
    private void handleReminderEdit(User user, String chatId, Integer messageId, int index) {
        try {
            UserNotificationSettingsDto settings = notificationSettingsService.getSettings(user.getId());
            List<UpcomingGameReminderDto> reminders = settings.getUpcomingGameReminders();
            if (reminders == null || index < 0 || index >= reminders.size()) {
                messageUpdater.answerCallback("", "❌ Напоминание не найдено");
                return;
            }
            
            notificationStateManager.setState(chatId, NotificationStateManager.NotificationState.WAITING_REMINDER_VALUE);
            var data = new NotificationStateManager.NotificationData();
            data.reminderIndex = index;
            UpcomingGameReminderDto reminder = reminders.get(index);
            data.reminderValue = reminder.getMinutesBefore();
            notificationStateManager.setData(chatId, data);
            
            String currentValue = formatReminderValue(reminder.getMinutesBefore());
            String message = "✏️ <b>Редактирование напоминания</b>\n\n" +
                    "Текущее значение: <b>" + TelegramHtmlFormatter.escapeHtml(currentValue) + "</b>\n\n" +
                    "Введите новое значение (например: 60 для 60 минут, 2 для 2 часов, 1 для 1 дня):\n\n" +
                    "💡 Используйте /cancel для отмены.";
            
            sendPersonalMessage(chatId, message);
            
            String menuMessage = messageBuilder.buildRemindersListMessage(reminders);
            var keyboard = keyboardBuilder.buildRemindersMenu(reminders);
            messageUpdater.updateMessage(chatId, messageId, menuMessage, keyboard);
        } catch (Exception e) {
            logger.error("Error handling reminder edit", e);
            messageUpdater.answerCallback("", "❌ Ошибка при редактировании напоминания");
        }
    }
    
    private void handleReminderDelete(User user, String chatId, Integer messageId, int index, String callbackQueryId) {
        try {
            UserNotificationSettingsDto settings = notificationSettingsService.getSettings(user.getId());
            List<UpcomingGameReminderDto> reminders = settings.getUpcomingGameReminders();
            if (reminders == null || index < 0 || index >= reminders.size()) {
                messageUpdater.answerCallback(callbackQueryId, "❌ Напоминание не найдено");
                return;
            }
            
            reminders.remove(index);
            settings.setUpcomingGameReminders(reminders);
            notificationSettingsService.updateSettings(user.getId(), settings);
            
            messageUpdater.answerCallback(callbackQueryId, "✅ Напоминание удалено!");
            handleMenuReminders(user, chatId, messageId);
        } catch (Exception e) {
            logger.error("Error handling reminder delete", e);
            messageUpdater.answerCallback(callbackQueryId, "❌ Ошибка при удалении напоминания");
        }
    }
    
    private void handleReminderToggle(User user, String chatId, Integer messageId, int index, String callbackQueryId) {
        try {
            UserNotificationSettingsDto settings = notificationSettingsService.getSettings(user.getId());
            List<UpcomingGameReminderDto> reminders = settings.getUpcomingGameReminders();
            if (reminders == null || index < 0 || index >= reminders.size()) {
                messageUpdater.answerCallback(callbackQueryId, "❌ Напоминание не найдено");
                return;
            }
            
            UpcomingGameReminderDto reminder = reminders.get(index);
            boolean newValue = !(reminder.getEnabled() != null && reminder.getEnabled());
            reminder.setEnabled(newValue);
            settings.setUpcomingGameReminders(reminders);
            notificationSettingsService.updateSettings(user.getId(), settings);
            
            messageUpdater.answerCallback(callbackQueryId, "✅ Напоминание " + (newValue ? "включено" : "выключено") + "!");
            handleMenuReminders(user, chatId, messageId);
        } catch (Exception e) {
            logger.error("Error handling reminder toggle", e);
            messageUpdater.answerCallback(callbackQueryId, "❌ Ошибка при изменении напоминания");
        }
    }
    
    private void handleReminderUnitSelect(User user, String chatId, Integer messageId, String unit, String callbackQueryId) {
        try {
            var data = notificationStateManager.getData(chatId);
            if (data == null) {
                notificationStateManager.clearState(chatId);
                sendPersonalMessage(chatId, "❌ Ошибка: данные не найдены. Начните заново.");
                return;
            }
            
            data.reminderUnit = unit;
            notificationStateManager.setData(chatId, data);
            notificationStateManager.setState(chatId, NotificationStateManager.NotificationState.WAITING_REMINDER_UNIT);
            
            String valueText = data.reminderValue != null ? String.valueOf(data.reminderValue) : "0";
            String unitText = switch (unit) {
                case "minutes" -> "минут";
                case "hours" -> "часов";
                case "days" -> "дней";
                default -> unit;
            };
            
            String message = "✅ Значение принято!\n\n" +
                    "Напоминание: <b>" + valueText + " " + unitText + "</b>\n\n" +
                    "Включить это напоминание? (да/нет):\n\n" +
                    "💡 Используйте /cancel для отмены.";
            
            sendPersonalMessage(chatId, message);
        } catch (Exception e) {
            logger.error("Error handling reminder unit select", e);
            messageUpdater.answerCallback(callbackQueryId, "❌ Ошибка при выборе единицы");
        }
    }
    
    private void handleMenuTimeSlotReminder(User user, String chatId, Integer messageId) {
        try {
            UserNotificationSettingsDto settings = notificationSettingsService.getSettings(user.getId());
            
            String message = "📅 <b>Напоминание разметить время</b>\n\n";
            
            if (settings.getTimeSlotReminderEnabled() != null && settings.getTimeSlotReminderEnabled()) {
                String cronText = CronExpressionBuilder.formatCronToReadable(settings.getTimeSlotReminderCron());
                message += "Статус: <b>Включено</b>\n";
                if (cronText != null && !cronText.isEmpty()) {
                    message += "Расписание: <b>" + TelegramHtmlFormatter.escapeHtml(cronText) + "</b>\n";
                }
            } else {
                message += "Статус: <b>Выключено</b>\n";
            }
            
            message += "\nВыберите действие:";
            
            var keyboard = keyboardBuilder.buildTimeSlotReminderMenu(settings.getTimeSlotReminderEnabled());
            messageUpdater.updateMessage(chatId, messageId, message, keyboard);
        } catch (Exception e) {
            logger.error("Error handling menu time slot reminder", e);
            messageUpdater.answerCallback("", "❌ Ошибка при получении настроек");
        }
    }
    
    private void handleTimeSlotReminderToggle(User user, String chatId, Integer messageId, String callbackQueryId) {
        try {
            UserNotificationSettingsDto settings = notificationSettingsService.getSettings(user.getId());
            boolean newValue = !(settings.getTimeSlotReminderEnabled() != null && settings.getTimeSlotReminderEnabled());
            settings.setTimeSlotReminderEnabled(newValue);
            
            if (!newValue) {
                settings.setTimeSlotReminderCron(null);
            } else if (settings.getTimeSlotReminderCron() == null || settings.getTimeSlotReminderCron().trim().isEmpty()) {
                settings.setTimeSlotReminderCron("0 0 9 * * *");
            }
            
            notificationSettingsService.updateSettings(user.getId(), settings);
            messageUpdater.answerCallback(callbackQueryId, "✅ Настройка изменена!");
            handleMenuTimeSlotReminder(user, chatId, messageId);
        } catch (Exception e) {
            logger.error("Error handling time slot reminder toggle", e);
            messageUpdater.answerCallback(callbackQueryId, "❌ Ошибка при изменении настройки");
        }
    }
    
    private void handleTimeSlotReminderCron(User user, String chatId, Integer messageId) {
        try {
            UserNotificationSettingsDto settings = notificationSettingsService.getSettings(user.getId());
            
            notificationStateManager.setState(chatId, NotificationStateManager.NotificationState.WAITING_CRON_FREQUENCY);
            var data = new NotificationStateManager.NotificationData();
            if (settings.getTimeSlotReminderCron() != null && !settings.getTimeSlotReminderCron().trim().isEmpty()) {
                CronExpressionParser.CronData cronData = new CronExpressionParser.CronData();
                CronExpressionParser.parseCronToData(settings.getTimeSlotReminderCron(), cronData);
                data.cronFrequency = cronData.cronFrequency;
                data.cronDay = cronData.cronDay;
                data.cronTime = cronData.cronTime;
            }
            notificationStateManager.setData(chatId, data);
            
            String message = "⚙️ <b>Настройка расписания</b>\n\n" +
                    "Выберите частоту напоминания:";
            
            var keyboard = keyboardBuilder.buildCronFrequencyMenu();
            messageUpdater.updateMessage(chatId, messageId, message, keyboard);
        } catch (Exception e) {
            logger.error("Error handling time slot reminder cron", e);
            messageUpdater.answerCallback("", "❌ Ошибка при настройке расписания");
        }
    }
    
    private void handleCronFrequencySelect(User user, String chatId, Integer messageId, String frequency) {
        try {
            var data = notificationStateManager.getData(chatId);
            if (data == null) {
                notificationStateManager.clearState(chatId);
                sendPersonalMessage(chatId, "❌ Ошибка: данные не найдены. Начните заново.");
                return;
            }
            
            data.cronFrequency = frequency;
            notificationStateManager.setData(chatId, data);
            
            if ("daily".equals(frequency)) {
                notificationStateManager.setState(chatId, NotificationStateManager.NotificationState.WAITING_CRON_TIME);
                String message = "✅ Частота выбрана: <b>Ежедневно</b>\n\n" +
                        "Введите время в формате ЧЧ:ММ (например: 09:00):\n\n" +
                        "💡 Используйте /cancel для отмены.";
                sendPersonalMessage(chatId, message);
            } else if ("weekly".equals(frequency)) {
                notificationStateManager.setState(chatId, NotificationStateManager.NotificationState.WAITING_CRON_DAY);
                String message = "✅ Частота выбрана: <b>Еженедельно</b>\n\n" +
                        "Выберите день недели:";
                var keyboard = keyboardBuilder.buildDayOfWeekKeyboard();
                messageUpdater.updateMessage(chatId, messageId, message, keyboard);
            } else if ("monthly".equals(frequency)) {
                notificationStateManager.setState(chatId, NotificationStateManager.NotificationState.WAITING_CRON_DAY);
                String message = "✅ Частота выбрана: <b>Ежемесячно</b>\n\n" +
                        "Введите день месяца (1-31):\n\n" +
                        "💡 Используйте /cancel для отмены.";
                sendPersonalMessage(chatId, message);
            }
        } catch (Exception e) {
            logger.error("Error handling cron frequency select", e);
            messageUpdater.answerCallback("", "❌ Ошибка при выборе частоты");
        }
    }
    
    private void handleCronDaySelect(User user, String chatId, Integer messageId, int day, String callbackQueryId) {
        try {
            var data = notificationStateManager.getData(chatId);
            if (data == null) {
                notificationStateManager.clearState(chatId);
                sendPersonalMessage(chatId, "❌ Ошибка: данные не найдены. Начните заново.");
                return;
            }
            
            data.cronDay = day;
            notificationStateManager.setData(chatId, data);
            notificationStateManager.setState(chatId, NotificationStateManager.NotificationState.WAITING_CRON_TIME);
            notificationStateManager.updateTimestamp(chatId);
            
            String dayText = switch (day) {
                case 0 -> "воскресенье";
                case 1 -> "понедельник";
                case 2 -> "вторник";
                case 3 -> "среду";
                case 4 -> "четверг";
                case 5 -> "пятницу";
                case 6 -> "субботу";
                default -> "день " + day;
            };
            
            String message = "✅ День выбран: <b>" + dayText + "</b>\n\n" +
                    "Введите время в формате ЧЧ:ММ (например: 09:00):\n\n" +
                    "💡 Используйте /cancel для отмены.";
            
            sendPersonalMessage(chatId, message);
        } catch (Exception e) {
            logger.error("Error handling cron day select", e);
            messageUpdater.answerCallback(callbackQueryId, "❌ Ошибка при выборе дня");
        }
    }
    
    private void sendPersonalMessage(String chatId, String text) {
        try {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText(text);
            sendMessage.setParseMode("HTML");
            bot.execute(sendMessage);
        } catch (TelegramApiException e) {
            logger.error("Failed to send personal message", e);
        }
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
