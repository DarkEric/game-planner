package ru.ambryo.gameplannerback.bot;

/**
 * Универсальное представление входящего события от платформы.
 * Telegram/Discord/другие каналы должны преобразовывать свои события к этому виду.
 */
public class BotUpdate {

    public enum Type {
        COMMAND,
        TEXT,
        CALLBACK
    }

    private final Type type;
    private final String platformId;
    private final String chatId;
    private final String messageId;
    private final Long userId;
    private final String text;
    private final String callbackData;

    private BotUpdate(Builder builder) {
        this.type = builder.type;
        this.platformId = builder.platformId;
        this.chatId = builder.chatId;
        this.messageId = builder.messageId;
        this.userId = builder.userId;
        this.text = builder.text;
        this.callbackData = builder.callbackData;
    }

    public Type getType() {
        return type;
    }

    public String getPlatformId() {
        return platformId;
    }

    public String getChatId() {
        return chatId;
    }

    public String getMessageId() {
        return messageId;
    }

    /**
     * Внешний идентификатор пользователя в рамках платформы.
     * Связка с внутренним пользователем приложения выполняется отдельно.
     */
    public Long getUserId() {
        return userId;
    }

    public String getText() {
        return text;
    }

    public String getCallbackData() {
        return callbackData;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Type type;
        private String platformId;
        private String chatId;
        private String messageId;
        private Long userId;
        private String text;
        private String callbackData;

        public Builder type(Type type) {
            this.type = type;
            return this;
        }

        public Builder platformId(String platformId) {
            this.platformId = platformId;
            return this;
        }

        public Builder chatId(String chatId) {
            this.chatId = chatId;
            return this;
        }

        public Builder messageId(String messageId) {
            this.messageId = messageId;
            return this;
        }

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder text(String text) {
            this.text = text;
            return this;
        }

        public Builder callbackData(String callbackData) {
            this.callbackData = callbackData;
            return this;
        }

        public BotUpdate build() {
            return new BotUpdate(this);
        }
    }
}

