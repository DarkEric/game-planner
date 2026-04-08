package ru.ambryo.gameplannerback.bot;

import java.util.Map;

/**
 * Дополнительные опции при отправке сообщений.
 * Набор полей минимален и намеренно абстрактен, чтобы не тянуть
 * конкретные типы Telegram/Discord в доменный код.
 */
public class BotMessageOptions {

    private final String parseMode;
    private final Map<String, Object> metadata;

    private BotMessageOptions(Builder builder) {
        this.parseMode = builder.parseMode;
        this.metadata = builder.metadata;
    }

    public String getParseMode() {
        return parseMode;
    }

    /**
     * Произвольные дополнительные параметры, которые может интерпретировать конкретная платформа.
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static BotMessageOptions defaultOptions() {
        return builder().build();
    }

    public static class Builder {
        private String parseMode;
        private Map<String, Object> metadata;

        public Builder parseMode(String parseMode) {
            this.parseMode = parseMode;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public BotMessageOptions build() {
            return new BotMessageOptions(this);
        }
    }
}

