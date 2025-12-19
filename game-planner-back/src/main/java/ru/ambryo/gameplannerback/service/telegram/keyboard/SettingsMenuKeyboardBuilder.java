package ru.ambryo.gameplannerback.service.telegram.keyboard;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Билдер клавиатуры меню настроек
 */
@Component
public class SettingsMenuKeyboardBuilder {
    
    public InlineKeyboardMarkup build(boolean isLinked) {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        
        rows.add(createButtonRow("👤 Профиль", "menu_settings_profile"));
        
        if (isLinked) {
            rows.add(createButtonRow("🌍 Часовой пояс", "menu_settings_timezone"));
            rows.add(createButtonRow("🔔 Уведомления", "menu_settings_notifications"));
            rows.add(createButtonRow("🎫 Инвайты", "menu_invites"));
        }
        
        rows.add(createButtonRow("◀️ Назад", "menu_main"));
        
        keyboard.setKeyboard(rows);
        return keyboard;
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

