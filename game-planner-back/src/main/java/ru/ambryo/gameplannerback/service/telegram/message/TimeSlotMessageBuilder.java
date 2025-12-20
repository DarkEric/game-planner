package ru.ambryo.gameplannerback.service.telegram.message;

import org.springframework.stereotype.Component;
import ru.ambryo.gameplannerback.dto.TimeSlotDto;
import ru.ambryo.gameplannerback.service.telegram.util.TelegramTimeFormatter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Билдер сообщений о временных слотах
 */
@Component
public class TimeSlotMessageBuilder {
    
    private final TelegramTimeFormatter timeFormatter;
    
    public TimeSlotMessageBuilder(TelegramTimeFormatter timeFormatter) {
        this.timeFormatter = timeFormatter;
    }
    
    public String buildTimeSlotMarkedMessage(LocalDate localDate, LocalTime localTime, Integer duration, ZoneId userTimezone) {
        String message = "✅ <b>Временной слот размечен!</b>\n\n" +
            "📅 <b>Дата:</b> " + TelegramTimeFormatter.formatLocalDate(localDate) + "\n" +
            "🕐 <b>Время:</b> " + TelegramTimeFormatter.formatLocalTime(localTime) + "\n" +
            "⏱️ <b>Продолжительность:</b> " + duration + " " + (duration == 1 ? "час" : "часа") + "\n\n" +
            "💡 Используйте /myslots для просмотра всех размеченных слотов.\n" +
            "💡 Повторная разметка того же времени удалит слот.";
        
        return message;
    }
    
    public String buildMySlotsListMessage(List<TimeSlotDto> slots, String userTimezone) {
        StringBuilder message = new StringBuilder();
        message.append("📅 <b>Мои временные слоты</b>\n\n");
        message.append("Всего: ").append(slots.size()).append("\n\n");
        
        if (slots.isEmpty()) {
            message.append("У вас пока нет размеченного времени.\n\n");
            message.append("💡 Используйте /mark для разметки свободного времени.");
            return message.toString();
        }
        
        for (int i = 0; i < slots.size(); i++) {
            TimeSlotDto slot = slots.get(i);
            
            message.append("<b>").append(i + 1).append(".</b> ");
            
            String startTime = timeFormatter.formatInstantInTimezone(slot.getStart(), userTimezone);
            java.time.Instant endTime = slot.getStart().plusSeconds(slot.getDuration() * 3600L);
            String endTimeStr = timeFormatter.formatInstantInTimezone(endTime, userTimezone);
            
            message.append(startTime).append(" - ").append(endTimeStr).append("\n");
            message.append("⏱️ Продолжительность: ").append(slot.getDuration()).append(" ").append(slot.getDuration() == 1 ? "час" : "часа").append("\n");
            
            if (i < slots.size() - 1) {
                message.append("\n");
            }
        }
        
        message.append("\n💡 Используйте /mark для разметки нового времени.\n");
        message.append("💡 Повторная разметка того же времени удалит слот.");
        
        return message.toString();
    }
}

