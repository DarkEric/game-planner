package ru.ambryo.gameplannerback.service.telegram.util;

/**
 * Утилита для форматирования HTML для Telegram
 */
public class TelegramHtmlFormatter {
    
    /**
     * Экранирует HTML символы для безопасного отображения в Telegram
     * @param text исходный текст
     * @return экранированный текст
     */
    public static String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }
    
    /**
     * Адаптирует HTML для Telegram (упрощенная санация)
     * Telegram поддерживает ограниченный набор тегов: b, strong, i, em, u, ins, s, strike, del, a, code, pre
     * @param html исходный HTML
     * @return адаптированный HTML
     */
    public static String sanitizeHtmlForTelegram(String html) {
        if (html == null) {
            return "";
        }
        // Простая адаптация HTML для Telegram
        // Заменяем переводы строк и параграфы на \n
        String result = html.replaceAll("(?i)<br\\s*/?>", "\n")
                           .replaceAll("(?i)<p.*?>", "")
                           .replaceAll("(?i)</p>", "\n");
        
        // Telegram поддерживает ограниченный набор тегов: b, strong, i, em, u, ins, s, strike, del, a, code, pre
        // Мы предполагаем, что пользователь (админ) вводит корректный HTML или использует редактор, который генерирует валидный HTML.
        // Полная санация сложна без парсера, поэтому оставляем как есть, полагаясь на валидацию Telegram API.
        // Если Telegram вернет ошибку парсинга, сообщение не отправится, но это будет залогировано.
        
        return result;
    }
}

