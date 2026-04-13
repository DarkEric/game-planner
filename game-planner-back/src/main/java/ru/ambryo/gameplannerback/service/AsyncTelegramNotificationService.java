package ru.ambryo.gameplannerback.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import ru.ambryo.gameplannerback.dto.GameDto;
import ru.ambryo.gameplannerback.entity.Game;
import ru.ambryo.gameplannerback.entity.User;

import java.util.List;

@Service
public class AsyncTelegramNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(AsyncTelegramNotificationService.class);

    /** Без {@code @Lazy} на боте — иначе CGLIB-прокси конфликтует с final-методами {@code DefaultAbsSender}. */
    private final ObjectProvider<TelegramNotificationService> telegramNotificationService;

    public AsyncTelegramNotificationService(
            ObjectProvider<TelegramNotificationService> telegramNotificationService) {
        this.telegramNotificationService = telegramNotificationService;
    }

    private TelegramNotificationService telegram() {
        return telegramNotificationService.getObject();
    }

    @Async
    public void sendGameCreatedNotificationsAsync(GameDto gameDto, Game game, List<User> allUsers) {
        try {
            telegram().sendGameCreatedNotification(gameDto);
        } catch (Exception e) {
            logger.error("Failed to send Telegram game created notification", e);
        }

        try {
            telegram().sendPersonalGameCreatedNotifications(gameDto, game, allUsers);
        } catch (Exception e) {
            logger.error("Failed to send personal game created notifications", e);
        }
    }

    @Async
    public void sendGameCancelledNotificationsAsync(GameDto gameDto, Game game, String cancellationReason) {
        try {
            telegram().sendGameCancelledNotification(gameDto, cancellationReason);
        } catch (Exception e) {
            logger.error("Failed to send Telegram game cancelled notification", e);
        }

        try {
            telegram().sendPersonalGameCancelledNotifications(gameDto, game);
        } catch (Exception e) {
            logger.error("Failed to send personal game cancelled notifications", e);
        }
    }

    @Async
    public void sendGameHeldNotificationsAsync(GameDto gameDto, Game game) {
        try {
            telegram().sendGameHeldNotification(gameDto);
        } catch (Exception e) {
            logger.error("Failed to send Telegram game held notification", e);
        }

        try {
            telegram().sendPersonalGameHeldNotifications(gameDto, game);
        } catch (Exception e) {
            logger.error("Failed to send personal game held notifications", e);
        }
    }

    @Async
    public void sendPlayerRemovedNotificationAsync(GameDto gameDto, User removedPlayer) {
        try {
            telegram().sendPlayerRemovedFromGameNotification(gameDto, removedPlayer);
        } catch (Exception e) {
            logger.error("Failed to send player removed from game notification", e);
        }
    }

    @Async
    public void sendPersonalMessageAsync(String chatId, String message) {
        try {
            telegram().sendPersonalMessage(chatId, message);
        } catch (Exception e) {
            logger.error("Failed to send personal message via Telegram", e);
        }
    }
}

