package ru.ambryo.gameplannerback.bot;

import java.util.List;

/**
 * Контекст выполнения команды бота, уже очищенной от префикса (/menu -> menu).
 * Используется CommandRouter/CommandHandler-ами независимо от конкретной платформы.
 */
public class BotCommandContext {

    private final String platformId;
    private final String chatId;
    private final Long userId;
    private final String command;
    private final List<String> args;

    public BotCommandContext(String platformId, String chatId, Long userId, String command, List<String> args) {
        this.platformId = platformId;
        this.chatId = chatId;
        this.userId = userId;
        this.command = command;
        this.args = args;
    }

    public String getPlatformId() {
        return platformId;
    }

    public String getChatId() {
        return chatId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getCommand() {
        return command;
    }

    public List<String> getArgs() {
        return args;
    }
}

