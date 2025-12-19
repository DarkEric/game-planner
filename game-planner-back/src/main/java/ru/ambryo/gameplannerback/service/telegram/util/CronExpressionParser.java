package ru.ambryo.gameplannerback.service.telegram.util;

/**
 * Утилита для парсинга cron выражений
 */
public class CronExpressionParser {
    
    /**
     * Парсит cron выражение и заполняет данные
     * @param cron cron выражение
     * @param data объект для заполнения данных
     */
    public static void parseCronToData(String cron, CronData data) {
        if (cron == null || cron.trim().isEmpty()) {
            return;
        }
        
        try {
            String[] parts = cron.trim().split("\\s+");
            if (parts.length >= 6) {
                int minute = Integer.parseInt(parts[1]);
                int hour = Integer.parseInt(parts[2]);
                String dayOfMonth = parts[3];
                String dayOfWeek = parts[5];
                
                data.cronTime = String.format("%02d:%02d", hour, minute);
                
                if (!"*".equals(dayOfWeek)) {
                    data.cronFrequency = "weekly";
                    data.cronDay = Integer.parseInt(dayOfWeek);
                } else if (!"*".equals(dayOfMonth)) {
                    data.cronFrequency = "monthly";
                    data.cronDay = Integer.parseInt(dayOfMonth);
                } else {
                    data.cronFrequency = "daily";
                }
            }
        } catch (Exception e) {
            // Игнорируем ошибки парсинга
        }
    }
    
    /**
     * Класс для хранения данных cron
     */
    public static class CronData {
        public String cronFrequency;
        public Integer cronDay;
        public String cronTime;
    }
}

