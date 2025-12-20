package ru.ambryo.gameplannerback.service.telegram.message;

import org.springframework.stereotype.Component;
import ru.ambryo.gameplannerback.dto.UpcomingGameReminderDto;
import ru.ambryo.gameplannerback.dto.UserNotificationSettingsDto;
import ru.ambryo.gameplannerback.service.telegram.util.CronExpressionBuilder;

import java.util.List;

/**
 * Билдер сообщений о настройках уведомлений
 */
@Component
public class NotificationMessageBuilder {
    
    public String buildNotificationSettingsMessage(UserNotificationSettingsDto settings) {
        StringBuilder message = new StringBuilder();
        message.append("🔔 <b>Настройки уведомлений</b>\n\n");
        message.append("<b>Текущие настройки:</b>\n");
        
        String gameCreatedText = switch (settings.getGameCreated()) {
            case "ALL" -> "Все игры";
            case "MY_GAMES" -> "Только мои игры";
            case "NONE" -> "Не получать";
            default -> settings.getGameCreated();
        };
        message.append("• Игра создана: ").append(gameCreatedText).append("\n");
        
        String gameCancelledText = switch (settings.getGameCancelled()) {
            case "ALL" -> "Все игры";
            case "MY_GAMES" -> "Только мои игры";
            case "NONE" -> "Не получать";
            default -> settings.getGameCancelled();
        };
        message.append("• Игра отменена: ").append(gameCancelledText).append("\n");
        
        String gameHeldText = switch (settings.getGameHeld()) {
            case "ALL" -> "Все игры";
            case "MY_GAMES" -> "Только мои игры";
            case "NONE" -> "Не получать";
            default -> settings.getGameHeld();
        };
        message.append("• Игра проведена: ").append(gameHeldText).append("\n");
        
        String removedText = "ALL".equals(settings.getGameRemovedFromGame()) ? "Получать" : "Не получать";
        message.append("• Исключили из игры: ").append(removedText).append("\n");
        
        List<UpcomingGameReminderDto> reminders = settings.getUpcomingGameReminders();
        if (reminders == null) {
            reminders = new java.util.ArrayList<>();
        }
        long activeReminders = reminders.stream()
                .filter(r -> r.getEnabled() != null && r.getEnabled())
                .count();
        message.append("• Напоминания о предстоящих играх: ").append(activeReminders).append(" активных\n");
        
        if (settings.getTimeSlotReminderEnabled() != null && settings.getTimeSlotReminderEnabled()) {
            String cronText = CronExpressionBuilder.formatCronToReadable(settings.getTimeSlotReminderCron());
            message.append("• Напоминание разметить время: Включено");
            if (!cronText.isEmpty()) {
                message.append(" (").append(cronText).append(")");
            }
            message.append("\n");
        } else {
            message.append("• Напоминание разметить время: Выключено\n");
        }
        
        String completionText = (settings.getGameCompletionReminderEnabled() != null && settings.getGameCompletionReminderEnabled()) 
                ? "Включено" : "Выключено";
        message.append("• Напоминание завершить игру: ").append(completionText).append("\n");
        
        message.append("\nВыберите настройку для изменения:");
        
        return message.toString();
    }
    
    public String buildRemindersListMessage(List<UpcomingGameReminderDto> reminders) {
        StringBuilder message = new StringBuilder();
        message.append("⏰ <b>Напоминания о предстоящих играх</b>\n\n");
        
        if (reminders.isEmpty()) {
            message.append("У вас пока нет настроенных напоминаний.\n\n");
            message.append("Нажмите кнопку ниже, чтобы добавить напоминание.");
        } else {
            message.append("Всего: ").append(reminders.size()).append(" (максимум 5)\n\n");
            
            for (int i = 0; i < reminders.size(); i++) {
                UpcomingGameReminderDto reminder = reminders.get(i);
                String displayValue = formatReminderValue(reminder.getMinutesBefore());
                String status = (reminder.getEnabled() != null && reminder.getEnabled()) ? "✅" : "❌";
                
                message.append("<b>").append(i + 1).append(".</b> ").append(status).append(" ");
                message.append(displayValue).append("\n");
            }
        }
        
        return message.toString();
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

