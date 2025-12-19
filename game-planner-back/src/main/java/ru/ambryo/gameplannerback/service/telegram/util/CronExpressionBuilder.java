package ru.ambryo.gameplannerback.service.telegram.util;

/**
 * Утилита для построения cron выражений из пользовательских данных
 */
public class CronExpressionBuilder {
    
    /**
     * Строит cron выражение из данных
     * @param frequency частота (daily/weekly/monthly)
     * @param cronTime время в формате HH:mm
     * @param cronDay день (для weekly - день недели 0-6, для monthly - день месяца 1-31)
     * @return cron выражение в формате Spring (секунды минуты часы день_месяца месяц день_недели)
     */
    public static String buildCron(String frequency, String cronTime, Integer cronDay) {
        if (frequency == null || cronTime == null) {
            return "0 0 9 * * *"; // По умолчанию ежедневно в 9:00
        }
        
        String[] timeParts = cronTime.split(":");
        int hour = Integer.parseInt(timeParts[0]);
        int minute = Integer.parseInt(timeParts[1]);
        
        // Spring cron формат: секунды минуты часы день_месяца месяц день_недели
        if ("weekly".equals(frequency)) {
            int dayOfWeek = cronDay != null ? cronDay : 1;
            return String.format("0 %d %d * * %d", minute, hour, dayOfWeek);
        } else if ("monthly".equals(frequency)) {
            int dayOfMonth = cronDay != null ? cronDay : 1;
            return String.format("0 %d %d %d * *", minute, hour, dayOfMonth);
        } else {
            // daily
            return String.format("0 %d %d * * *", minute, hour);
        }
    }
    
    /**
     * Форматирует cron выражение в читаемый текст
     * @param cron cron выражение
     * @return читаемый текст
     */
    public static String formatCronToReadable(String cron) {
        if (cron == null || cron.trim().isEmpty()) {
            return "не настроено";
        }
        
        try {
            String[] parts = cron.trim().split("\\s+");
            if (parts.length < 6) {
                return cron;
            }
            
            int minute = Integer.parseInt(parts[1]);
            int hour = Integer.parseInt(parts[2]);
            String dayOfMonth = parts[3];
            String dayOfWeek = parts[5];
            
            String timeStr = String.format("%02d:%02d", hour, minute);
            
            if (!"*".equals(dayOfWeek)) {
                int day = Integer.parseInt(dayOfWeek);
                String[] days = {"воскресенье", "понедельник", "вторник", "среду", "четверг", "пятницу", "субботу"};
                if (day >= 0 && day < days.length) {
                    return "каждый " + days[day] + " в " + timeStr;
                }
            } else if (!"*".equals(dayOfMonth)) {
                int day = Integer.parseInt(dayOfMonth);
                return "каждое " + day + " число в " + timeStr;
            } else {
                return "ежедневно в " + timeStr;
            }
        } catch (Exception e) {
            // Игнорируем ошибки парсинга
        }
        
        return cron;
    }
}

