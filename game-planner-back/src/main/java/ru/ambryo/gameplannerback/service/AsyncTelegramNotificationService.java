package ru.ambryo.gameplannerback.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import ru.ambryo.gameplannerback.dto.GameDto;
import ru.ambryo.gameplannerback.entity.Game;
import ru.ambryo.gameplannerback.entity.User;

import java.util.List;

@Service
public class AsyncTelegramNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(AsyncTelegramNotificationService.class);

    private final TelegramNotificationService telegramNotificationService;

    public AsyncTelegramNotificationService(TelegramNotificationService telegramNotificationService) {
        this.telegramNotificationService = telegramNotificationService;
    }

    @Async
    public void sendGameCreatedNotificationsAsync(GameDto gameDto, Game game, List<User> allUsers) {
        try {
            telegramNotificationService.sendGameCreatedNotification(gameDto);
        } catch (Exception e) {
            logger.error("Failed to send Telegram game created notification", e);
        }

        try {
            telegramNotificationService.sendPersonalGameCreatedNotifications(gameDto, game, allUsers);
        } catch (Exception e) {
            logger.error("Failed to send personal game created notifications", e);
        }
    }

    @Async
    public void sendGameCancelledNotificationsAsync(GameDto gameDto, Game game, String cancellationReason) {
        try {
            telegramNotificationService.sendGameCancelledNotification(gameDto, cancellationReason);
        } catch (Exception e) {
            logger.error("Failed to send Telegram game cancelled notification", e);
        }

        try {
            telegramNotificationService.sendPersonalGameCancelledNotifications(gameDto, game);
        } catch (Exception e) {
            logger.error("Failed to send personal game cancelled notifications", e);
        }
    }

    @Async
    public void sendGameHeldNotificationsAsync(GameDto gameDto, Game game) {
        try {
            telegramNotificationService.sendGameHeldNotification(gameDto);
        } catch (Exception e) {
            logger.error("Failed to send Telegram game held notification", e);
        }

        try {
            telegramNotificationService.sendPersonalGameHeldNotifications(gameDto, game);
        } catch (Exception e) {
            logger.error("Failed to send personal game held notifications", e);
        }
    }

    @Async
    public void sendPlayerRemovedNotificationAsync(GameDto gameDto, User removedPlayer) {
        try {
            telegramNotificationService.sendPlayerRemovedFromGameNotification(gameDto, removedPlayer);
        } catch (Exception e) {
            logger.error("Failed to send player removed from game notification", e);
        }
    }

    @Async
    public void sendPersonalMessageAsync(String chatId, String message) {
        try {
            telegramNotificationService.sendPersonalMessage(chatId, message);
        } catch (Exception e) {
            logger.error("Failed to send personal message via Telegram", e);
        }
    }
}

