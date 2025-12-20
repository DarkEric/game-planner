package ru.ambryo.gameplannerback.service.telegram.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;
import ru.ambryo.gameplannerback.service.telegram.message.HelpMessageBuilder;
import ru.ambryo.gameplannerback.service.telegram.util.TelegramMessageSender;

/**
 * Обработчик команды /help
 */
@Component
public class HelpCommandHandler implements CommandHandler {
    
    private final HelpMessageBuilder helpMessageBuilder;
    private final TelegramMessageSender messageSender;
    
    @Autowired
    public HelpCommandHandler(HelpMessageBuilder helpMessageBuilder, @Lazy AbsSender bot) {
        this.helpMessageBuilder = helpMessageBuilder;
        this.messageSender = new TelegramMessageSender(bot);
    }
    
    @Override
    public boolean canHandle(String command) {
        return "help".equals(command);
    }
    
    @Override
    public void handle(Message message, Long telegramUserId, String chatId) {
        String helpMessage = helpMessageBuilder.buildHelpMessage();
        messageSender.sendPersonalMessage(chatId, helpMessage);
    }
}

