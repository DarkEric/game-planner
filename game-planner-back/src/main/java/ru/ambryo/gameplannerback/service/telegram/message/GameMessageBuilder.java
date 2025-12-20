package ru.ambryo.gameplannerback.service.telegram.message;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.ambryo.gameplannerback.dto.GameDto;
import ru.ambryo.gameplannerback.entity.User;
import ru.ambryo.gameplannerback.service.telegram.util.TelegramHtmlFormatter;
import ru.ambryo.gameplannerback.service.telegram.util.TelegramTimeFormatter;

import java.time.ZoneId;

/**
 * Билдер сообщений об играх
 */
@Component
public class GameMessageBuilder {
    
    private final TelegramTimeFormatter timeFormatter;
    private final String frontendUrl;
    private final ZoneId notificationZone;
    
    public GameMessageBuilder(
            TelegramTimeFormatter timeFormatter,
            @Value("${app.frontend.url:}") String frontendUrl,
            @Value("${app.notification.timezone:Europe/Moscow}") String timezone) {
        this.timeFormatter = timeFormatter;
        this.frontendUrl = frontendUrl;
        this.notificationZone = ZoneId.of(timezone);
    }
    
    /**
     * Строит сообщение с деталями игры
     */
    public String buildGameDetailsMessage(GameDto game, User user) {
        StringBuilder message = new StringBuilder();
        message.append("🎮 <b>Детали игры</b>\n\n");
        
        if (game.getTitle() != null && !game.getTitle().isEmpty()) {
            message.append("📌 <b>").append(TelegramHtmlFormatter.escapeHtml(game.getTitle())).append("</b>\n");
        } else {
            message.append("📌 <b>Игра</b>\n");
        }
        
        if (game.getDescription() != null && !game.getDescription().isEmpty()) {
            message.append("📝 ").append(TelegramHtmlFormatter.escapeHtml(game.getDescription())).append("\n\n");
        } else {
            message.append("\n");
        }
        
        message.append("🕐 <b>Время:</b> ")
            .append(timeFormatter.formatInstant(game.getStartTime()))
            .append(" - ")
            .append(timeFormatter.formatInstant(game.getEndTime()))
            .append(" (")
            .append(TelegramTimeFormatter.getTimezoneName(notificationZone))
            .append(")\n");
        
        message.append("👤 <b>Организатор:</b> ").append(TelegramHtmlFormatter.escapeHtml(game.getCreatorName())).append("\n");
        
        // Подсчет участников без создателя
        long participantCount = game.getParticipants() != null 
            ? game.getParticipants().stream()
                .filter(p -> !p.getId().equals(game.getCreatorId()))
                .count()
            : 0;
        
        Integer maxParticipants = game.getMaxParticipants();
        if (maxParticipants != null) {
            message.append("👥 <b>Участники:</b> ").append(participantCount).append("/").append(maxParticipants);
            if (participantCount >= maxParticipants) {
                message.append(" (Заполнена)");
            }
        } else {
            message.append("👥 <b>Участники:</b> ").append(participantCount);
        }
        message.append("\n");
        
        if (maxParticipants != null) {
            message.append("📊 <b>Максимум участников:</b> ").append(maxParticipants).append("\n");
        }
        
        if (game.getCampaignName() != null) {
            message.append("📚 <b>Кампания:</b> ").append(TelegramHtmlFormatter.escapeHtml(game.getCampaignName())).append("\n");
        }
        
        String gameUrl = frontendUrl + "?gameId=" + game.getId();
        message.append("\n🔗 <a href=\"").append(gameUrl).append("\">Открыть в веб-интерфейсе</a>");
        
        return message.toString();
    }
    
    /**
     * Строит сообщение о создании игры для группового уведомления
     */
    public String buildGameNotificationMessage(GameDto game) {
        StringBuilder message = new StringBuilder();
        message.append("🎮 <b>Запланирована новая игра!</b>\n\n");
        
        if (game.getTitle() != null && !game.getTitle().isEmpty()) {
            message.append("📌 <b>").append(TelegramHtmlFormatter.escapeHtml(game.getTitle())).append("</b>\n");
        }
        
        if (game.getDescription() != null && !game.getDescription().isEmpty()) {
            message.append("📝 ").append(TelegramHtmlFormatter.escapeHtml(game.getDescription())).append("\n\n");
        } else {
            message.append("\n");
        }
        
        message.append("🕐 <b>Время:</b> ")
            .append(timeFormatter.formatInstant(game.getStartTime()))
            .append(" - ")
            .append(timeFormatter.formatInstant(game.getEndTime()))
            .append(" (")
            .append(TelegramTimeFormatter.getTimezoneName(notificationZone))
            .append(")\n");
        
        message.append("👤 <b>Организатор:</b> ").append(TelegramHtmlFormatter.escapeHtml(game.getCreatorName())).append("\n");
        
        String gameUrl = frontendUrl + "?gameId=" + game.getId();
        message.append("\n🔗 <a href=\"").append(gameUrl).append("\">Посмотреть и записаться на игру</a>");
        
        return message.toString();
    }
    
    /**
     * Строит сообщение об отмене игры
     */
    public String buildGameCancelledMessage(GameDto game, String cancellationReason) {
        StringBuilder message = new StringBuilder();
        message.append("❌ <b>Игра отменена</b>\n\n");
        
        if (game.getTitle() != null && !game.getTitle().isEmpty()) {
            message.append("📌 <b>").append(TelegramHtmlFormatter.escapeHtml(game.getTitle())).append("</b>\n");
        }
        
        message.append("🕐 <b>Время:</b> ")
            .append(timeFormatter.formatInstant(game.getStartTime()))
            .append(" - ")
            .append(timeFormatter.formatInstant(game.getEndTime()))
            .append(" (")
            .append(TelegramTimeFormatter.getTimezoneName(notificationZone))
            .append(")\n");
        
        message.append("👤 <b>Организатор:</b> ").append(TelegramHtmlFormatter.escapeHtml(game.getCreatorName())).append("\n");
        
        if (cancellationReason != null && !cancellationReason.trim().isEmpty()) {
            message.append("\n💬 <b>Причина отмены:</b>\n")
                .append(TelegramHtmlFormatter.escapeHtml(cancellationReason));
        }
        
        return message.toString();
    }
    
    /**
     * Строит сообщение о проведенной игре
     */
    public String buildGameHeldMessage(GameDto game) {
        StringBuilder message = new StringBuilder();
        message.append("✅ <b>Игра состоялась!</b>\n\n");
        
        if (game.getTitle() != null && !game.getTitle().isEmpty()) {
            message.append("📌 <b>").append(TelegramHtmlFormatter.escapeHtml(game.getTitle())).append("</b>\n");
        }
        
        message.append("🕐 <b>Время:</b> ")
            .append(timeFormatter.formatInstant(game.getStartTime()))
            .append(" - ")
            .append(timeFormatter.formatInstant(game.getEndTime()))
            .append(" (")
            .append(TelegramTimeFormatter.getTimezoneName(notificationZone))
            .append(")\n");
        
        message.append("👤 <b>Организатор:</b> ").append(TelegramHtmlFormatter.escapeHtml(game.getCreatorName())).append("\n");
        
        if (game.getKeyEvents() != null && !game.getKeyEvents().trim().isEmpty()) {
            message.append("\n📝 <b>Ключевые события:</b>\n\n")
                .append(TelegramHtmlFormatter.sanitizeHtmlForTelegram(game.getKeyEvents()));
        }
        
        return message.toString();
    }
    
    /**
     * Строит сообщение о списке предстоящих игр
     */
    public String buildUpcomingGamesListMessage(java.util.List<GameDto> games, int page, int totalPages) {
        StringBuilder message = new StringBuilder();
        message.append("🎮 <b>Предстоящие игры</b>\n\n");
        
        if (games.isEmpty()) {
            message.append("Нет предстоящих игр.");
            return message.toString();
        }
        
        int startIndex = page * 5;
        int endIndex = Math.min(startIndex + 5, games.size());
        
        message.append("Всего игр: ").append(games.size());
        if (totalPages > 1) {
            message.append(" (Страница ").append(page + 1).append("/").append(totalPages).append(")");
        }
        message.append("\n\n");
        
        for (int i = startIndex; i < endIndex; i++) {
            GameDto game = games.get(i);
            message.append("<b>").append(i + 1).append(".</b> ");
            
            if (game.getTitle() != null && !game.getTitle().isEmpty()) {
                String title = game.getTitle();
                if (title.length() > 50) {
                    title = title.substring(0, 47) + "...";
                }
                message.append(TelegramHtmlFormatter.escapeHtml(title));
            } else {
                message.append("Игра");
            }
            
            message.append("\n🕐 ").append(timeFormatter.formatInstant(game.getStartTime())).append("\n\n");
        }
        
        return message.toString();
    }
    
    /**
     * Строит сообщение о напоминании о предстоящей игре
     */
    public String buildUpcomingGameReminderMessage(GameDto game, int minutesBefore) {
        StringBuilder message = new StringBuilder();
        message.append("⏰ <b>Напоминание о предстоящей игре</b>\n\n");
        
        if (game.getTitle() != null && !game.getTitle().isEmpty()) {
            message.append("📌 <b>").append(TelegramHtmlFormatter.escapeHtml(game.getTitle())).append("</b>\n");
        }
        
        message.append("🕐 <b>Время:</b> ")
            .append(timeFormatter.formatInstant(game.getStartTime()))
            .append(" - ")
            .append(timeFormatter.formatInstant(game.getEndTime()))
            .append(" (")
            .append(TelegramTimeFormatter.getTimezoneName(notificationZone))
            .append(")\n");
        
        if (minutesBefore >= 60) {
            int hours = minutesBefore / 60;
            message.append("⏳ Игра начнется через ").append(hours).append(" ").append(hours == 1 ? "час" : "часа");
        } else {
            message.append("⏳ Игра начнется через ").append(minutesBefore).append(" ").append(minutesBefore == 1 ? "минуту" : "минут");
        }
        
        String gameUrl = frontendUrl + "?gameId=" + game.getId();
        message.append("\n\n🔗 <a href=\"").append(gameUrl).append("\">Посмотреть игру</a>");
        
        return message.toString();
    }
    
    /**
     * Строит сообщение о напоминании завершить игру
     */
    public String buildGameCompletionReminderMessage(GameDto game) {
        StringBuilder message = new StringBuilder();
        message.append("📝 <b>Напоминание</b>\n\n");
        message.append("Игра завершилась, но еще не помечена как проведенная.\n\n");
        
        if (game.getTitle() != null && !game.getTitle().isEmpty()) {
            message.append("📌 <b>").append(TelegramHtmlFormatter.escapeHtml(game.getTitle())).append("</b>\n");
        }
        
        message.append("🕐 <b>Время:</b> ")
            .append(timeFormatter.formatInstant(game.getStartTime()))
            .append(" - ")
            .append(timeFormatter.formatInstant(game.getEndTime()))
            .append("\n");
        
        String gameUrl = frontendUrl + "?gameId=" + game.getId();
        message.append("\n🔗 <a href=\"").append(gameUrl).append("\">Завершить игру</a>");
        
        return message.toString();
    }
    
    /**
     * Строит сообщение об исключении игрока из игры
     */
    public String buildPlayerRemovedFromGameMessage(GameDto game) {
        StringBuilder message = new StringBuilder();
        message.append("ℹ️ <b>Ваша запись на игру была отменена</b>\n\n");
        
        if (game.getTitle() != null && !game.getTitle().isEmpty()) {
            message.append("📌 <b>").append(TelegramHtmlFormatter.escapeHtml(game.getTitle())).append("</b>\n");
        }
        
        message.append("🕐 <b>Время:</b> ")
            .append(timeFormatter.formatInstant(game.getStartTime()))
            .append(" - ")
            .append(timeFormatter.formatInstant(game.getEndTime()))
            .append(" (")
            .append(TelegramTimeFormatter.getTimezoneName(notificationZone))
            .append(")\n");
        
        message.append("👤 <b>Организатор:</b> ").append(TelegramHtmlFormatter.escapeHtml(game.getCreatorName())).append("\n");
        
        String gameUrl = frontendUrl + "?gameId=" + game.getId();
        message.append("\n🔗 <a href=\"").append(gameUrl).append("\">Посмотреть игру</a>");
        
        return message.toString();
    }
}

