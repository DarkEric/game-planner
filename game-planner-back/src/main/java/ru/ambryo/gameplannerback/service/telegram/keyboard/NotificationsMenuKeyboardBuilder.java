package ru.ambryo.gameplannerback.service.telegram.keyboard;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.ambryo.gameplannerback.dto.UpcomingGameReminderDto;
import ru.ambryo.gameplannerback.dto.UserNotificationSettingsDto;

import java.util.ArrayList;
import java.util.List;

/**
 * Билдер клавиатур для меню уведомлений
 */
@Component
public class NotificationsMenuKeyboardBuilder {
    
    public InlineKeyboardMarkup buildNotificationsMenu(UserNotificationSettingsDto settings) {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        
        String gameCreatedText = switch (settings.getGameCreated()) {
            case "ALL" -> "✓ Все игры";
            case "MY_GAMES" -> "✓ Только мои";
            case "NONE" -> "✓ Не получать";
            default -> "Игра создана";
        };
        rows.add(createButtonRow("🎮 " + gameCreatedText, "notification_set_gameCreated"));
        
        String gameCancelledText = switch (settings.getGameCancelled()) {
            case "ALL" -> "✓ Все игры";
            case "MY_GAMES" -> "✓ Только мои";
            case "NONE" -> "✓ Не получать";
            default -> "Игра отменена";
        };
        rows.add(createButtonRow("❌ " + gameCancelledText, "notification_set_gameCancelled"));
        
        String gameHeldText = switch (settings.getGameHeld()) {
            case "ALL" -> "✓ Все игры";
            case "MY_GAMES" -> "✓ Только мои";
            case "NONE" -> "✓ Не получать";
            default -> "Игра проведена";
        };
        rows.add(createButtonRow("✅ " + gameHeldText, "notification_set_gameHeld"));
        
        String removedText = "ALL".equals(settings.getGameRemovedFromGame()) ? "✓ Получать" : "✓ Не получать";
        rows.add(createButtonRow("🚫 " + removedText, "notification_set_gameRemovedFromGame"));
        
        List<UpcomingGameReminderDto> reminders = settings.getUpcomingGameReminders();
        if (reminders == null) {
            reminders = new ArrayList<>();
        }
        long activeCount = reminders.stream()
                .filter(r -> r.getEnabled() != null && r.getEnabled())
                .count();
        rows.add(createButtonRow("⏰ Напоминания (" + activeCount + "/" + reminders.size() + ")", "notification_reminders"));
        
        String timeSlotText = (settings.getTimeSlotReminderEnabled() != null && settings.getTimeSlotReminderEnabled()) 
                ? "✓ Включено" : "Выключено";
        rows.add(createButtonRow("📅 " + timeSlotText, "notification_timeslot_reminder"));
        
        String completionText = (settings.getGameCompletionReminderEnabled() != null && settings.getGameCompletionReminderEnabled()) 
                ? "✓ Включено" : "Выключено";
        rows.add(createButtonRow("📝 " + completionText, "notification_set_gameCompletionReminder"));
        
        rows.add(createButtonRow("◀️ Назад", "menu_settings"));
        
        keyboard.setKeyboard(rows);
        return keyboard;
    }
    
    public InlineKeyboardMarkup buildRemindersMenu(List<UpcomingGameReminderDto> reminders) {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        
        for (int i = 0; i < reminders.size(); i++) {
            UpcomingGameReminderDto reminder = reminders.get(i);
            String displayValue = formatReminderValue(reminder.getMinutesBefore());
            String status = (reminder.getEnabled() != null && reminder.getEnabled()) ? "✅" : "❌";
            
            rows.add(createButtonRow("✏️ " + (i + 1) + ". " + status + " " + displayValue, "notification_reminder_edit_" + i));
            
            List<InlineKeyboardButton> controlRow = new ArrayList<>();
            
            InlineKeyboardButton toggleButton = new InlineKeyboardButton();
            toggleButton.setText((reminder.getEnabled() != null && reminder.getEnabled()) ? "❌ Выкл" : "✅ Вкл");
            toggleButton.setCallbackData("notification_reminder_toggle_" + i);
            controlRow.add(toggleButton);
            
            InlineKeyboardButton deleteButton = new InlineKeyboardButton();
            deleteButton.setText("🗑️ Удалить");
            deleteButton.setCallbackData("notification_reminder_delete_" + i);
            controlRow.add(deleteButton);
            
            rows.add(controlRow);
        }
        
        if (reminders.size() < 5) {
            rows.add(createButtonRow("➕ Добавить напоминание", "notification_reminder_add"));
        }
        
        rows.add(createButtonRow("◀️ Назад", "menu_settings_notifications"));
        
        keyboard.setKeyboard(rows);
        return keyboard;
    }
    
    public InlineKeyboardMarkup buildDayOfWeekKeyboard() {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        
        String[] days = {"Воскресенье", "Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота"};
        
        // Первая строка: Пн, Вт, Ср
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(days[i]);
            button.setCallbackData("notification_cron_day_" + i);
            row1.add(button);
        }
        rows.add(row1);
        
        // Вторая строка: Чт, Пт, Сб
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        for (int i = 4; i <= 6; i++) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(days[i]);
            button.setCallbackData("notification_cron_day_" + i);
            row2.add(button);
        }
        rows.add(row2);
        
        // Третья строка: Вс
        List<InlineKeyboardButton> row3 = new ArrayList<>();
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(days[0]);
        button.setCallbackData("notification_cron_day_0");
        row3.add(button);
        rows.add(row3);
        
        rows.add(createButtonRow("◀️ Назад", "notification_timeslot_reminder_cron"));
        
        keyboard.setKeyboard(rows);
        return keyboard;
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
    
    private List<InlineKeyboardButton> createButtonRow(String text, String callbackData) {
        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(callbackData);
        row.add(button);
        return row;
    }
}

