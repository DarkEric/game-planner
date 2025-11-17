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
    
    private static final DateTimeFormatter DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
            .withZone(ZoneId.systemDefault());
    
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
    
    public void sendGameCreatedNotification(GameDto game) {
        if (!enabled || chatId == null || chatId.isEmpty()) {
            logger.debug("Telegram notifications disabled or chat ID not configured");
            return;
        }
        
        try {
            String message = buildGameNotificationMessage(game);
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText(message);
            sendMessage.setParseMode("HTML");
            
            // Если указан Thread ID (для топиков в супергруппах)
            if (threadId != null && !threadId.isEmpty()) {
                try {
                    sendMessage.setMessageThreadId(Integer.parseInt(threadId));
                    logger.debug("Sending to thread ID: {}", threadId);
                } catch (NumberFormatException e) {
                    logger.warn("Invalid thread ID format: {}", threadId);
                }
            }
            
            execute(sendMessage);
            logger.info("Telegram notification sent for game: {}", game.getTitle());
        } catch (TelegramApiException e) {
            logger.error("Failed to send Telegram notification", e);
        }
    }
    
    private String buildGameNotificationMessage(GameDto game) {
        StringBuilder message = new StringBuilder();
        message.append("🎮 <b>Новая игра запланирована!</b>\n\n");
        
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
            .append("\n");
        
        message.append("👤 <b>Организатор:</b> ").append(escapeHtml(game.getCreatorName())).append("\n");
        
        if (game.getParticipants() != null && !game.getParticipants().isEmpty()) {
            message.append("👥 <b>Участники:</b> ").append(game.getParticipants().size()).append("\n");
        }
        
        String gameUrl = frontendUrl + "?gameId=" + game.getId();
        message.append("\n🔗 <a href=\"").append(gameUrl).append("\">Открыть игру</a>");
        
        return message.toString();
    }
    
    private String formatInstant(Instant instant) {
        return DATE_FORMATTER.format(instant);
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
