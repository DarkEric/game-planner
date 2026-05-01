package ru.ambryo.gameplannerback.service.telegram.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.bots.AbsSender;
import ru.ambryo.gameplannerback.dto.GameDto;
import ru.ambryo.gameplannerback.entity.User;
import ru.ambryo.gameplannerback.service.telegram.keyboard.GamesMenuKeyboardBuilder;
import ru.ambryo.gameplannerback.service.telegram.message.GameMessageBuilder;

import java.time.Instant;

/**
 * Отправитель уведомлений об играх
 */
@Component
public class GameNotificationSender extends PersonalNotificationSender {
    
    private static final Logger logger = LoggerFactory.getLogger(GameNotificationSender.class);
    
    private final GameMessageBuilder messageBuilder;
    private final GamesMenuKeyboardBuilder keyboardBuilder;
    
    @Autowired
    public GameNotificationSender(
            @Lazy AbsSender bot,
            GameMessageBuilder messageBuilder,
            GamesMenuKeyboardBuilder keyboardBuilder) {
        super(bot);
        this.messageBuilder = messageBuilder;
        this.keyboardBuilder = keyboardBuilder;
    }
    
    public void sendGameCreatedNotification(GameDto game, User user) {
        if (canSendToUser(user)) {
            return;
        }
        
        try {
            String message = messageBuilder.buildGameDetailsMessage(game, user);
            org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup keyboard = 
                keyboardBuilder.buildGameKeyboard(game, user);
            
            org.telegram.telegrambots.meta.api.methods.send.SendMessage sendMessage = 
                new org.telegram.telegrambots.meta.api.methods.send.SendMessage();
            sendMessage.setChatId(user.getTelegramChatId());
            sendMessage.setText(message);
            sendMessage.setParseMode("HTML");
            sendMessage.setReplyMarkup(keyboard);
            
            bot.execute(sendMessage);
        } catch (org.telegram.telegrambots.meta.exceptions.TelegramApiException e) {
            logger.error("Failed to send personal game created notification to user {}", user.getId(), e);
        } catch (Exception e) {
            logger.error("Unexpected error while sending personal game created notification to user {}", user.getId(), e);
        }
    }
    
    public void sendGameCancelledNotification(GameDto game, User user) {
        if (canSendToUser(user)) {
            return;
        }
        
        String message = messageBuilder.buildGameCancelledMessage(game, null);
        sendPersonalMessage(user.getTelegramChatId(), message);
    }
    
    public void sendGameHeldNotification(GameDto game, User user) {
        if (canSendToUser(user)) {
            return;
        }
        
        String message = messageBuilder.buildGameHeldMessage(game);
        sendPersonalMessage(user.getTelegramChatId(), message);
    }
    
    public void sendUpcomingGameReminder(GameDto game, User user, int minutesBefore) {
        if (canSendToUser(user)) {
            return;
        }
        
        String message = messageBuilder.buildUpcomingGameReminderMessage(game, minutesBefore);
        sendPersonalMessage(user.getTelegramChatId(), message);
    }
    
    public void sendGameCompletionReminder(GameDto game, User creator) {
        if (canSendToUser(creator)) {
            return;
        }
        
        String message = messageBuilder.buildGameCompletionReminderMessage(game);
        sendPersonalMessage(creator.getTelegramChatId(), message);
    }
    
    public void sendPlayerRemovedFromGameNotification(GameDto game, User removedPlayer) {
        if (canSendToUser(removedPlayer)) {
            return;
        }
        
        String message = messageBuilder.buildPlayerRemovedFromGameMessage(game);
        sendPersonalMessage(removedPlayer.getTelegramChatId(), message);
    }

    public void sendGameRescheduledNotification(GameDto game, User user, Instant oldStart, Instant oldEnd) {
        if (canSendToUser(user)) {
            return;
        }

        try {
            String message = messageBuilder.buildGameRescheduledMessage(game, oldStart, oldEnd);
            org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup keyboard =
                    keyboardBuilder.buildGameKeyboard(game, user);

            org.telegram.telegrambots.meta.api.methods.send.SendMessage sendMessage =
                    new org.telegram.telegrambots.meta.api.methods.send.SendMessage();
            sendMessage.setChatId(user.getTelegramChatId());
            sendMessage.setText(message);
            sendMessage.setParseMode("HTML");
            sendMessage.setReplyMarkup(keyboard);

            bot.execute(sendMessage);
        } catch (org.telegram.telegrambots.meta.exceptions.TelegramApiException e) {
            logger.error("Failed to send game rescheduled notification to user {}", user.getId(), e);
        } catch (Exception e) {
            logger.error("Unexpected error sending game rescheduled notification to user {}", user.getId(), e);
        }
    }

    public void sendGameTitleChangedNotification(GameDto game, User user, String oldTitle) {
        if (canSendToUser(user)) {
            return;
        }

        try {
            String message = messageBuilder.buildGameTitleChangedMessage(game, oldTitle);
            org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup keyboard =
                    keyboardBuilder.buildGameKeyboard(game, user);

            org.telegram.telegrambots.meta.api.methods.send.SendMessage sendMessage =
                    new org.telegram.telegrambots.meta.api.methods.send.SendMessage();
            sendMessage.setChatId(user.getTelegramChatId());
            sendMessage.setText(message);
            sendMessage.setParseMode("HTML");
            sendMessage.setReplyMarkup(keyboard);

            bot.execute(sendMessage);
        } catch (org.telegram.telegrambots.meta.exceptions.TelegramApiException e) {
            logger.error("Failed to send game title changed notification to user {}", user.getId(), e);
        } catch (Exception e) {
            logger.error("Unexpected error sending game title changed notification to user {}", user.getId(), e);
        }
    }
}

