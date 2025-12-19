package ru.ambryo.gameplannerback.service.telegram.util;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Утилита для парсинга дат и времени из пользовательского ввода
 */
public class TelegramDateParser {
    
    /**
     * Парсит дату в русском формате (15.01.2025, 15/01/2025) или относительную дату (сегодня, завтра, послезавтра)
     * @param dateStr строка с датой
     * @param userTimezone часовой пояс пользователя
     * @return LocalDate или null если не удалось распарсить
     */
    public static LocalDate parseDate(String dateStr, ZoneId userTimezone) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        
        String trimmed = dateStr.trim().toLowerCase();
        
        // Относительные даты
        LocalDate now = LocalDate.now(userTimezone);
        if (trimmed.equals("сегодня") || trimmed.equals("today")) {
            return now;
        } else if (trimmed.equals("завтра") || trimmed.equals("tomorrow")) {
            return now.plusDays(1);
        } else if (trimmed.equals("послезавтра") || trimmed.equals("day after tomorrow")) {
            return now.plusDays(2);
        }
        
        // Русский формат: DD.MM.YYYY или DD/MM/YYYY
        DateTimeFormatter[] formatters = {
            DateTimeFormatter.ofPattern("d.M.yyyy"),
            DateTimeFormatter.ofPattern("dd.MM.yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy")
        };
        
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(trimmed, formatter);
            } catch (DateTimeParseException e) {
                // Пробуем следующий формат
            }
        }
        
        return null;
    }
    
    /**
     * Парсит время в формате HH:mm или HH
     * @param timeStr строка со временем
     * @return LocalTime или null если не удалось распарсить
     */
    public static java.time.LocalTime parseTime(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            return null;
        }
        
        String trimmed = timeStr.trim();
        
        // Формат HH:mm
        if (trimmed.contains(":")) {
            try {
                return java.time.LocalTime.parse(trimmed, DateTimeFormatter.ofPattern("H:mm"));
            } catch (DateTimeParseException e) {
                try {
                    return java.time.LocalTime.parse(trimmed, DateTimeFormatter.ofPattern("HH:mm"));
                } catch (DateTimeParseException e2) {
                    return null;
                }
            }
        }
        
        // Формат HH (только часы)
        try {
            int hours = Integer.parseInt(trimmed);
            if (hours >= 0 && hours <= 23) {
                return java.time.LocalTime.of(hours, 0);
            }
        } catch (NumberFormatException e) {
            // Не число
        }
        
        return null;
    }
    
    /**
     * Парсит продолжительность в часах
     * @param durationStr строка с продолжительностью
     * @return Integer (количество часов) или null если не удалось распарсить
     */
    public static Integer parseDuration(String durationStr) {
        if (durationStr == null || durationStr.trim().isEmpty()) {
            return null;
        }
        
        try {
            // Убираем слово "час" или "часов" если есть
            String trimmed = durationStr.trim().toLowerCase()
                    .replaceAll("\\s*час(ов|а)?\\s*", "");
            
            int duration = Integer.parseInt(trimmed);
            if (duration > 0 && duration <= 24) {
                return duration;
            }
        } catch (NumberFormatException e) {
            // Не число
        }
        
        return null;
    }
    
    /**
     * Конвертирует локальное время пользователя в UTC
     * @param localDate дата в локальном времени
     * @param localTime время в локальном времени
     * @param userTimezone часовой пояс пользователя
     * @return Instant в UTC
     */
    public static java.time.Instant convertToUTC(java.time.LocalDate localDate, java.time.LocalTime localTime, ZoneId userTimezone) {
        java.time.LocalDateTime localDateTime = java.time.LocalDateTime.of(localDate, localTime);
        java.time.ZonedDateTime zonedDateTime = localDateTime.atZone(userTimezone);
        return zonedDateTime.toInstant();
    }
}

