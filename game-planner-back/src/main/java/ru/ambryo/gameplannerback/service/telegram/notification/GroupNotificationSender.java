package ru.ambryo.gameplannerback.service.telegram.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.bots.AbsSender;
import ru.ambryo.gameplannerback.dto.GameDto;
import ru.ambryo.gameplannerback.service.telegram.message.GameMessageBuilder;

/**
 * Отправитель групповых уведомлений
 */
@Component
public class GroupNotificationSender extends NotificationSender {
    
    private static final Logger logger = LoggerFactory.getLogger(GroupNotificationSender.class);
    
    private final GameMessageBuilder messageBuilder;
    private final boolean enabled;
    
    @Autowired
    public GroupNotificationSender(
            AbsSender bot,
            @Value("${telegram.chat.id:}") String chatId,
            @Value("${telegram.thread.id:}") String threadId,
            @Value("${telegram.enabled:false}") boolean enabled,
            GameMessageBuilder messageBuilder) {
        super(bot, chatId, threadId);
        this.enabled = enabled;
        this.messageBuilder = messageBuilder;
    }
    
    public void sendGameCreatedNotification(GameDto game) {
        if (!enabled || chatId == null || chatId.isEmpty()) {
            logger.debug("Telegram notifications disabled or chat ID not configured");
            return;
        }
        
        logger.info("Preparing to send Telegram notification for game: {}", game.getTitle());
        
        try {
            String message = messageBuilder.buildGameNotificationMessage(game);
            sendGroupMessage(message);
            logger.info("Telegram notification successfully sent for game: {}", game.getTitle());
        } catch (Exception e) {
            logger.error("Failed to send Telegram notification for game: {}", game.getTitle(), e);
        }
    }
    
    public void sendGameCancelledNotification(GameDto game, String cancellationReason) {
        if (!enabled || chatId == null || chatId.isEmpty()) {
            logger.debug("Telegram notifications disabled or chat ID not configured");
            return;
        }
        
        logger.info("Preparing to send game cancellation notification for game: {}", game.getTitle());
        
        try {
            String message = messageBuilder.buildGameCancelledMessage(game, cancellationReason);
            sendGroupMessage(message);
            logger.info("Telegram cancellation notification successfully sent for game: {}", game.getTitle());
        } catch (Exception e) {
            logger.error("Failed to send Telegram cancellation notification for game: {}", game.getTitle(), e);
        }
    }
    
    public void sendGameHeldNotification(GameDto game) {
        if (!enabled || chatId == null || chatId.isEmpty()) {
            logger.debug("Telegram notifications disabled or chat ID not configured");
            return;
        }
        
        logger.info("Preparing to send game held notification for game: {}", game.getTitle());
        
        try {
            String message = messageBuilder.buildGameHeldMessage(game);
            sendGroupMessage(message);
            logger.info("Telegram held notification successfully sent for game: {}", game.getTitle());
        } catch (Exception e) {
            logger.error("Failed to send Telegram held notification for game: {}", game.getTitle(), e);
        }
    }
    
    public void sendGroupTimeSlotReminder() {
        if (!enabled || chatId == null || chatId.isEmpty()) {
            logger.debug("Telegram notifications disabled or chat ID not configured");
            return;
        }
        
        logger.info("Preparing to send group time slot reminder");
        
        try {
            String message = "📅 <b>Напоминание</b>\n\nНе забудьте разметить ваше доступное время в календаре!";
            sendGroupMessage(message);
            logger.info("Group time slot reminder successfully sent");
        } catch (Exception e) {
            logger.error("Failed to send group time slot reminder", e);
        }
    }
}

