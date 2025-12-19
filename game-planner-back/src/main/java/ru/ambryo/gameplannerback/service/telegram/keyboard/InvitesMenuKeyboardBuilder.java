package ru.ambryo.gameplannerback.service.telegram.keyboard;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Билдер клавиатуры меню инвайтов
 */
@Component
public class InvitesMenuKeyboardBuilder {
    
    public InlineKeyboardMarkup build() {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        
        rows.add(createButtonRow("➕ Создать инвайт-код", "menu_invites_create"));
        rows.add(createButtonRow("📋 Мои инвайт-коды", "menu_invites_list"));
        rows.add(createButtonRow("◀️ Назад", "menu_settings"));
        
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

