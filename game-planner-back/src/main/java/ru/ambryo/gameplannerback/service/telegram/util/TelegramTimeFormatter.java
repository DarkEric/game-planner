package ru.ambryo.gameplannerback.service.telegram.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Утилита для форматирования времени для Telegram сообщений
 */
public class TelegramTimeFormatter {
    
    private final ZoneId defaultZone;
    
    public TelegramTimeFormatter(ZoneId defaultZone) {
        this.defaultZone = defaultZone;
    }
    
    /**
     * Форматирует Instant в строку с датой и временем
     * @param instant момент времени
     * @return отформатированная строка
     */
    public String formatInstant(Instant instant) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
            .withZone(defaultZone);
        return formatter.format(instant);
    }
    
    /**
     * Форматирует Instant в строку с учетом часового пояса пользователя
     * @param instant момент времени
     * @param timezone часовой пояс пользователя
     * @return отформатированная строка
     */
    public String formatInstantInTimezone(Instant instant, String timezone) {
        try {
            ZoneId zoneId = ZoneId.of(timezone);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                    .withZone(zoneId);
            return formatter.format(instant);
        } catch (Exception e) {
            // Fallback к общему формату
            return formatInstant(instant);
        }
    }
    
    /**
     * Форматирует LocalDate в строку
     * @param date дата
     * @return отформатированная строка
     */
    public static String formatLocalDate(LocalDate date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        return date.format(formatter);
    }
    
    /**
     * Форматирует LocalTime в строку
     * @param time время
     * @return отформатированная строка
     */
    public static String formatLocalTime(LocalTime time) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return time.format(formatter);
    }
    
    /**
     * Получает название часового пояса на русском языке
     * @param zoneId часовой пояс
     * @return название на русском
     */
    public static String getTimezoneName(ZoneId zoneId) {
        // Маппинг популярных часовых поясов на русские названия
        return switch (zoneId.getId()) {
            case "Europe/Moscow" -> "по Москве";
            case "Europe/Kaliningrad" -> "по Калининграду";
            case "Europe/Samara" -> "по Самаре";
            case "Asia/Yekaterinburg" -> "по Екатеринбургу";
            case "Asia/Omsk" -> "по Омску";
            case "Asia/Krasnoyarsk" -> "по Красноярску";
            case "Asia/Irkutsk" -> "по Иркутску";
            case "Asia/Yakutsk" -> "по Якутску";
            case "Asia/Vladivostok" -> "по Владивостоку";
            case "Asia/Magadan" -> "по Магадану";
            case "Asia/Kamchatka" -> "по Камчатке";
            default -> "UTC" + zoneId.getRules().getOffset(Instant.now());
        };
    }
}

