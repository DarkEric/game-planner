package ru.ambryo.gameplannerback.bot;

import java.util.List;

/**
 * Универсальное представление меню с кнопками.
 * Конкретная платформа сама решает, во что это меню превратить
 * (inline клавиатура, обычные кнопки, select-элементы и т.п.).
 */
public class BotMenu {

    private final String text;
    private final List<BotMenuRow> rows;

    public BotMenu(String text, List<BotMenuRow> rows) {
        this.text = text;
        this.rows = rows;
    }

    public String getText() {
        return text;
    }

    public List<BotMenuRow> getRows() {
        return rows;
    }

    public static class BotMenuRow {
        private final List<BotMenuButton> buttons;

        public BotMenuRow(List<BotMenuButton> buttons) {
            this.buttons = buttons;
        }

        public List<BotMenuButton> getButtons() {
            return buttons;
        }
    }

    public static class BotMenuButton {
        private final String label;
        private final String callbackData;

        public BotMenuButton(String label, String callbackData) {
            this.label = label;
            this.callbackData = callbackData;
        }

        public String getLabel() {
            return label;
        }

        public String getCallbackData() {
            return callbackData;
        }
    }
}

