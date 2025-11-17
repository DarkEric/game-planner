package ru.ambryo.gameplannerback.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.ambryo.gameplannerback.dto.GameDto;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
public class TelegramNotificationService extends TelegramLongPollingBot {
    
    private static final Logger logger = LoggerFactory.getLogger(TelegramNotificationService.class);
    
    @Value("${telegram.bot.enabled:false}")
    private boolean enabled;
    
    @Value("${telegram.bot.token:}")
    private String botToken;
    
    @Value("${telegram.bot.chat-id:}")
    private String chatId;
    
    @Value("${telegram.bot.thread-id:}")
    private String threadId;
    
    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;
    
    @Value("${telegram.bot.timezone:Europe/Moscow}")
    private String timezoneId;
    
    private ZoneId getNotificationZone() {
        try {
            return ZoneId.of(timezoneId);
        } catch (Exception e) {
            logger.warn("Invalid timezone '{}', falling back to Europe/Moscow", timezoneId);
            return ZoneId.of("Europe/Moscow");
        }
    }
    
    private String getTimezoneName() {
        ZoneId zone = getNotificationZone();
        // Маппинг популярных часовых поясов на русские названия
        return switch (zone.getId()) {
            case "Europe/Moscow" -> "по Москве";
            case "Europe/Kaliningrad" -> "по Калининграду";
            case "Europe/Samara" -> "по Самаре";
            case "Asia/Yekaterinburg" -> "по Екатеринбургу";
            case "Asia/Omsk" -> "по Омску";
            case "Asia/Krasnoyarsk" -> "по Красноярску";
            case "Asia/Irkutsk" -> "по Иркутску";
            case "Asia/Yakutsk" -> "по Якутску";
            case "Asia/Vladivostok" -> "по Владивостоку";
            case "Asia/Magadan" -> "по Магадану";
            case "Asia/Kamchatka" -> "по Камчатке";
            default -> "UTC" + zone.getRules().getOffset(Instant.now());
        };
    }
    
    @Override
    public String getBotUsername() {
        return "GamePlannerBot";
    }
    
    @Override
    public String getBotToken() {
        return botToken;
    }
    
    @Override
    public void onUpdateReceived(Update update) {
        // Бот не обрабатывает входящие сообщения, только отправляет уведомления
    }
    
    /**
     * Логирует текущую конфигурацию бота (для отладки)
     */
    public void logConfiguration() {
        logger.info("=== Telegram Bot Configuration ===");
        logger.info("Enabled: {}", enabled);
        logger.info("Chat ID: {}", chatId != null && !chatId.isEmpty() ? chatId : "NOT SET");
        logger.info("Thread ID: {}", threadId != null && !threadId.trim().isEmpty() ? threadId : "NOT SET");
        logger.info("Frontend URL: {}", frontendUrl);
        logger.info("Timezone: {} ({})", timezoneId, getTimezoneName());
        logger.info("==================================");
    }
    
    public void sendGameCreatedNotification(GameDto game) {
        if (!enabled || chatId == null || chatId.isEmpty()) {
            logger.debug("Telegram notifications disabled or chat ID not configured");
            return;
        }
        
        logger.info("Preparing to send Telegram notification for game: {}", game.getTitle());
        logger.debug("Chat ID: {}, Thread ID: '{}'", chatId, threadId);
        
        try {
            String message = buildGameNotificationMessage(game);
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText(message);
            sendMessage.setParseMode("HTML");
            
            // Если указан Thread ID (для топиков в супергруппах)
            if (threadId != null && !threadId.trim().isEmpty()) {
                try {
                    int threadIdInt = Integer.parseInt(threadId.trim());
                    sendMessage.setMessageThreadId(threadIdInt);
                    logger.info("Sending to thread ID: {}", threadIdInt);
                } catch (NumberFormatException e) {
                    logger.warn("Invalid thread ID format: '{}', sending to main chat", threadId, e);
                }
            } else {
                logger.debug("No thread ID specified, sending to main chat");
            }
            
            execute(sendMessage);
            logger.info("Telegram notification successfully sent for game: {}", game.getTitle());
        } catch (TelegramApiException e) {
            logger.error("Failed to send Telegram notification for game: {}", game.getTitle(), e);
        }
    }
    
    private String buildGameNotificationMessage(GameDto game) {
        StringBuilder message = new StringBuilder();
        message.append("🎮 <b>Запланирована новая игра!</b>\n\n");
        
        if (game.getTitle() != null && !game.getTitle().isEmpty()) {
            message.append("📌 <b>").append(escapeHtml(game.getTitle())).append("</b>\n");
        }
        
        if (game.getDescription() != null && !game.getDescription().isEmpty()) {
            message.append("📝 ").append(escapeHtml(game.getDescription())).append("\n\n");
        } else {
            message.append("\n");
        }
        
        message.append("🕐 <b>Время:</b> ")
            .append(formatInstant(game.getStartTime()))
            .append(" - ")
            .append(formatInstant(game.getEndTime()))
            .append(" (")
            .append(getTimezoneName())
            .append(")\n");
        
        message.append("👤 <b>Организатор:</b> ").append(escapeHtml(game.getCreatorName())).append("\n");
        
        String gameUrl = frontendUrl + "?gameId=" + game.getId();
        message.append("\n🔗 <a href=\"").append(gameUrl).append("\">Посмотреть и записаться на игру</a>");
        
        return message.toString();
    }
    
    private String formatInstant(Instant instant) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
            .withZone(getNotificationZone());
        return formatter.format(instant);
    }
    
    public void sendGameCancelledNotification(GameDto game, String cancellationReason) {
        if (!enabled || chatId == null || chatId.isEmpty()) {
            logger.debug("Telegram notifications disabled or chat ID not configured");
            return;
        }
        
        logger.info("Preparing to send game cancellation notification for game: {}", game.getTitle());
        logger.debug("Chat ID: {}, Thread ID: '{}'", chatId, threadId);
        
        try {
            String message = buildGameCancelledMessage(game, cancellationReason);
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText(message);
            sendMessage.setParseMode("HTML");
            
            // Если указан Thread ID (для топиков в супергруппах)
            if (threadId != null && !threadId.trim().isEmpty()) {
                try {
                    int threadIdInt = Integer.parseInt(threadId.trim());
                    sendMessage.setMessageThreadId(threadIdInt);
                    logger.info("Sending cancellation to thread ID: {}", threadIdInt);
                } catch (NumberFormatException e) {
                    logger.warn("Invalid thread ID format: '{}', sending to main chat", threadId, e);
                }
            } else {
                logger.debug("No thread ID specified, sending to main chat");
            }
            
            execute(sendMessage);
            logger.info("Telegram cancellation notification successfully sent for game: {}", game.getTitle());
        } catch (TelegramApiException e) {
            logger.error("Failed to send Telegram cancellation notification for game: {}", game.getTitle(), e);
        }
    }
    
    private String buildGameCancelledMessage(GameDto game, String cancellationReason) {
        StringBuilder message = new StringBuilder();
        message.append("❌ <b>Игра отменена</b>\n\n");
        
        if (game.getTitle() != null && !game.getTitle().isEmpty()) {
            message.append("📌 <b>").append(escapeHtml(game.getTitle())).append("</b>\n");
        }
        
        message.append("🕐 <b>Время:</b> ")
            .append(formatInstant(game.getStartTime()))
            .append(" - ")
            .append(formatInstant(game.getEndTime()))
            .append(" (")
            .append(getTimezoneName())
            .append(")\n");
        
        message.append("👤 <b>Организатор:</b> ").append(escapeHtml(game.getCreatorName())).append("\n");
        
        if (cancellationReason != null && !cancellationReason.trim().isEmpty()) {
            message.append("\n💬 <b>Причина отмены:</b>\n")
                .append(escapeHtml(cancellationReason));
        }
        
        return message.toString();
    }
    
    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }
}
