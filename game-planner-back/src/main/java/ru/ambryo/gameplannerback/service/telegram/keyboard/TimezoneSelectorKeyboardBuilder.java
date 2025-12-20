package ru.ambryo.gameplannerback.service.telegram.keyboard;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Билдер клавиатуры выбора часового пояса
 */
@Component
public class TimezoneSelectorKeyboardBuilder {
    
    private static final String[][] RUSSIAN_TIMEZONES = {
        {"Europe/Moscow", "Москва"},
        {"Europe/Kaliningrad", "Калининград"},
        {"Europe/Samara", "Самара"},
        {"Asia/Yekaterinburg", "Екатеринбург"},
        {"Asia/Omsk", "Омск"},
        {"Asia/Krasnoyarsk", "Красноярск"},
        {"Asia/Irkutsk", "Иркутск"},
        {"Asia/Yakutsk", "Якутск"},
        {"Asia/Vladivostok", "Владивосток"},
        {"Asia/Magadan", "Магадан"},
        {"Asia/Kamchatka", "Камчатка"}
    };
    
    private static final String[][] OTHER_TIMEZONES = {
        {"Europe/London", "Лондон"},
        {"Europe/Berlin", "Берлин"},
        {"Europe/Paris", "Париж"},
        {"America/New_York", "Нью-Йорк"},
        {"America/Chicago", "Чикаго"},
        {"America/Los_Angeles", "Лос-Анджелес"},
        {"Asia/Tokyo", "Токио"},
        {"Asia/Shanghai", "Шанхай"},
        {"Asia/Dubai", "Дубай"},
        {"Australia/Sydney", "Сидней"}
    };
    
    public InlineKeyboardMarkup build(String currentTimezone) {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        
        // Российские часовые пояса (по 2 в ряд)
        for (int i = 0; i < RUSSIAN_TIMEZONES.length; i += 2) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            
            String timezone1 = RUSSIAN_TIMEZONES[i][0];
            String label1 = RUSSIAN_TIMEZONES[i][1];
            String display1 = timezone1.equals(currentTimezone) ? "✓ " + label1 : label1;
            row.add(createButton(display1, "timezone_select_" + timezone1));
            
            if (i + 1 < RUSSIAN_TIMEZONES.length) {
                String timezone2 = RUSSIAN_TIMEZONES[i + 1][0];
                String label2 = RUSSIAN_TIMEZONES[i + 1][1];
                String display2 = timezone2.equals(currentTimezone) ? "✓ " + label2 : label2;
                row.add(createButton(display2, "timezone_select_" + timezone2));
            }
            
            rows.add(row);
        }
        
        // Разделитель
        rows.add(createButtonRow("━━━━━━━━━━━━━━━━", "timezone_separator"));
        
        // Другие часовые пояса (по 2 в ряд)
        for (int i = 0; i < OTHER_TIMEZONES.length; i += 2) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            
            String timezone1 = OTHER_TIMEZONES[i][0];
            String label1 = OTHER_TIMEZONES[i][1];
            String display1 = timezone1.equals(currentTimezone) ? "✓ " + label1 : label1;
            row.add(createButton(display1, "timezone_select_" + timezone1));

            String timezone2 = OTHER_TIMEZONES[i + 1][0];
            String label2 = OTHER_TIMEZONES[i + 1][1];
            String display2 = timezone2.equals(currentTimezone) ? "✓ " + label2 : label2;
            row.add(createButton(display2, "timezone_select_" + timezone2));

            rows.add(row);
        }
        
        rows.add(createButtonRow("✏️ Ввести вручную", "timezone_manual"));
        rows.add(createButtonRow("◀️ Назад", "menu_settings"));
        
        keyboard.setKeyboard(rows);
        return keyboard;
    }
    
    private InlineKeyboardButton createButton(String text, String callbackData) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(callbackData);
        return button;
    }
    
    private List<InlineKeyboardButton> createButtonRow(String text, String callbackData) {
        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(createButton(text, callbackData));
        return row;
    }
}

