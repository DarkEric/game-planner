package ru.ambryo.gameplannerback.service.telegram.keyboard;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Билдер клавиатуры меню разметки времени
 */
@Component
public class TimeMenuKeyboardBuilder {
    
    public InlineKeyboardMarkup build() {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        
        rows.add(createButtonRow("➕ Разметить время", "menu_time_mark"));
        rows.add(createButtonRow("📅 Мои слоты", "menu_time_slots"));
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

