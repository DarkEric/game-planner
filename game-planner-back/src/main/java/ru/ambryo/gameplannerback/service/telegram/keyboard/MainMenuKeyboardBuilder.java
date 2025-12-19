package ru.ambryo.gameplannerback.service.telegram.keyboard;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Билдер клавиатуры главного меню
 */
@Component
public class MainMenuKeyboardBuilder {
    
    public InlineKeyboardMarkup build(boolean isLinked) {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        
        if (isLinked) {
            // Для связанных пользователей - полное меню
            rows.add(createButtonRow("🎮 Игры", "menu_games"));
            rows.add(createButtonRow("📅 Разметка времени", "menu_time"));
            rows.add(createButtonRow("⚙️ Настройки", "menu_settings"));
            rows.add(createButtonRow("📖 Помощь", "menu_help"));
        } else {
            // Для несвязанных пользователей
            rows.add(createButtonRow("📝 Зарегистрироваться", "menu_register"));
            rows.add(createButtonRow("🔐 Авторизоваться", "menu_auth"));
            rows.add(createButtonRow("🔗 Связать", "menu_link"));
        }
        
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

