package ru.ambryo.gameplannerback.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.ambryo.gameplannerback.dto.GameDto;
import ru.ambryo.gameplannerback.entity.User;
import ru.ambryo.gameplannerback.repository.UserRepository;

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
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private NotificationSettingsService notificationSettingsService;
    
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
        if (!enabled) {
            return;
        }
        
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message message = update.getMessage();
            String text = message.getText();
            Long chatId = message.getChatId();
            Long telegramUserId = message.getFrom().getId();
            
            if (text.startsWith("/start")) {
                handleStartCommand(telegramUserId, chatId.toString());
            } else if (text.startsWith("/stop")) {
                handleStopCommand(telegramUserId, chatId.toString());
            } else if (text.startsWith("/link")) {
                String[] parts = text.split("\\s+", 2);
                if (parts.length == 2) {
                    handleLinkCommand(telegramUserId, chatId.toString(), parts[1]);
                } else {
                    sendPersonalMessage(chatId.toString(), "Использование: /link <token>\n\nПолучите токен в настройках профиля на веб-сайте.");
                }
            }
        }
    }
    
    private void handleStartCommand(Long telegramUserId, String chatId) {
        try {
            User user = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
            
            if (user != null) {
                // Пользователь уже связан
                user.setTelegramChatId(chatId);
                user.setTelegramSubscribed(true);
                userRepository.save(user);
                sendPersonalMessage(chatId, "✅ Вы уже подписаны на уведомления!\n\nИспользуйте /stop для отписки.");
            } else {
                // Пользователь не связан
                sendPersonalMessage(chatId, "👋 Добро пожаловать!\n\n" +
                        "Для получения персональных уведомлений необходимо связать ваш Telegram аккаунт с аккаунтом на веб-сайте.\n\n" +
                        "1. Откройте настройки профиля на веб-сайте\n" +
                        "2. Получите токен для связывания\n" +
                        "3. Отправьте команду: /link <token>");
            }
        } catch (Exception e) {
            logger.error("Error handling /start command", e);
        }
    }
    
    private void handleStopCommand(Long telegramUserId, String chatId) {
        try {
            User user = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
            
            if (user != null && user.getTelegramSubscribed()) {
                user.setTelegramSubscribed(false);
                userRepository.save(user);
                sendPersonalMessage(chatId, "✅ Вы отписались от уведомлений.\n\nИспользуйте /start для повторной подписки.");
            } else {
                sendPersonalMessage(chatId, "Вы не подписаны на уведомления.");
            }
        } catch (Exception e) {
            logger.error("Error handling /stop command", e);
        }
    }
    
    private void handleLinkCommand(Long telegramUserId, String chatId, String token) {
        try {
            notificationSettingsService.linkTelegramAccount(token, telegramUserId, chatId);
            sendPersonalMessage(chatId, "✅ Аккаунт успешно связан!\n\nТеперь вы будете получать персональные уведомления.\n\nИспользуйте /stop для отписки.");
        } catch (Exception e) {
            logger.error("Error handling /link command", e);
            sendPersonalMessage(chatId, "❌ Ошибка: " + e.getMessage() + "\n\nПроверьте правильность токена и попробуйте снова.");
        }
    }
    
    private void sendPersonalMessage(String chatId, String text) {
        try {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText(text);
            sendMessage.setParseMode("HTML");
            execute(sendMessage);
        } catch (TelegramApiException e) {
            logger.error("Failed to send personal message to chat {}", chatId, e);
        }
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

    public void sendGameHeldNotification(GameDto game) {
        if (!enabled || chatId == null || chatId.isEmpty()) {
            logger.debug("Telegram notifications disabled or chat ID not configured");
            return;
        }
        
        logger.info("Preparing to send game held notification for game: {}", game.getTitle());
        
        try {
            String message = buildGameHeldMessage(game);
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText(message);
            sendMessage.setParseMode("HTML");
            
            if (threadId != null && !threadId.trim().isEmpty()) {
                try {
                    int threadIdInt = Integer.parseInt(threadId.trim());
                    sendMessage.setMessageThreadId(threadIdInt);
                } catch (NumberFormatException e) {
                    logger.warn("Invalid thread ID format: '{}', sending to main chat", threadId, e);
                }
            }
            
            execute(sendMessage);
            logger.info("Telegram held notification successfully sent for game: {}", game.getTitle());
        } catch (TelegramApiException e) {
            logger.error("Failed to send Telegram held notification for game: {}", game.getTitle(), e);
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

    private String buildGameHeldMessage(GameDto game) {
        StringBuilder message = new StringBuilder();
        message.append("✅ <b>Игра состоялась!</b>\n\n");
        
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
        
            if (game.getKeyEvents() != null && !game.getKeyEvents().trim().isEmpty()) {
                message.append("\n📝 <b>Ключевые события:</b>\n\n")
                    .append(sanitizeHtmlForTelegram(game.getKeyEvents()));
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

    private String sanitizeHtmlForTelegram(String html) {
        if (html == null) {
            return "";
        }
        // Простая адаптация HTML для Telegram
        // Заменяем переводы строк и параграфы на \n
        String result = html.replaceAll("(?i)<br\\s*/?>", "\n")
                           .replaceAll("(?i)<p.*?>", "")
                           .replaceAll("(?i)</p>", "\n");
        
        // Telegram поддерживает ограниченный набор тегов: b, strong, i, em, u, ins, s, strike, del, a, code, pre
        // Мы предполагаем, что пользователь (админ) вводит корректный HTML или использует редактор, который генерирует валидный HTML.
        // Полная санация сложна без парсера, поэтому оставляем как есть, полагаясь на валидацию Telegram API.
        // Если Telegram вернет ошибку парсинга, сообщение не отправится, но это будет залогировано.
        
        return result;
    }
    
    // Персональные уведомления
    
    public void sendPersonalNotification(Long telegramUserId, String message) {
        if (!enabled) {
            return;
        }
        
        User user = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
        if (user == null || !user.getTelegramSubscribed() || user.getTelegramChatId() == null) {
            return;
        }
        
        try {
            sendPersonalMessage(user.getTelegramChatId(), message);
        } catch (Exception e) {
            logger.error("Failed to send personal notification to user {}", telegramUserId, e);
        }
    }
    
    public void sendGameCreatedPersonalNotification(GameDto game, User user) {
        if (user.getTelegramSubscribed() && user.getTelegramChatId() != null) {
            String message = buildGameNotificationMessage(game);
            sendPersonalMessage(user.getTelegramChatId(), message);
        }
    }
    
    public void sendGameCancelledPersonalNotification(GameDto game, User user) {
        if (user.getTelegramSubscribed() && user.getTelegramChatId() != null) {
            String message = buildGameCancelledMessage(game, null);
            sendPersonalMessage(user.getTelegramChatId(), message);
        }
    }
    
    public void sendGameHeldPersonalNotification(GameDto game, User user) {
        if (user.getTelegramSubscribed() && user.getTelegramChatId() != null) {
            String message = buildGameHeldMessage(game);
            sendPersonalMessage(user.getTelegramChatId(), message);
        }
    }
    
    public void sendPlayerRemovedFromGameNotification(GameDto game, User removedPlayer) {
        if (removedPlayer.getTelegramSubscribed() && removedPlayer.getTelegramChatId() != null) {
            StringBuilder message = new StringBuilder();
            message.append("ℹ️ <b>Ваша запись на игру была отменена</b>\n\n");
            
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
            
            String gameUrl = frontendUrl + "?gameId=" + game.getId();
            message.append("\n🔗 <a href=\"").append(gameUrl).append("\">Посмотреть игру</a>");
            
            sendPersonalMessage(removedPlayer.getTelegramChatId(), message.toString());
        }
    }
    
    public void sendUpcomingGameReminder(GameDto game, User user, int minutesBefore) {
        if (user.getTelegramSubscribed() && user.getTelegramChatId() != null) {
            StringBuilder message = new StringBuilder();
            message.append("⏰ <b>Напоминание о предстоящей игре</b>\n\n");
            
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
            
            if (minutesBefore >= 60) {
                int hours = minutesBefore / 60;
                message.append("⏳ Игра начнется через ").append(hours).append(" ").append(hours == 1 ? "час" : "часа");
            } else {
                message.append("⏳ Игра начнется через ").append(minutesBefore).append(" ").append(minutesBefore == 1 ? "минуту" : "минут");
            }
            
            String gameUrl = frontendUrl + "?gameId=" + game.getId();
            message.append("\n\n🔗 <a href=\"").append(gameUrl).append("\">Посмотреть игру</a>");
            
            sendPersonalMessage(user.getTelegramChatId(), message.toString());
        }
    }
    
    public void sendTimeSlotReminder(User user) {
        if (user.getTelegramSubscribed() && user.getTelegramChatId() != null) {
            String message = "📅 <b>Напоминание</b>\n\nНе забудьте разметить ваше доступное время в календаре!";
            sendPersonalMessage(user.getTelegramChatId(), message);
        }
    }
    
    public void sendGameCompletionReminder(GameDto game, User creator) {
        if (creator.getTelegramSubscribed() && creator.getTelegramChatId() != null) {
            StringBuilder message = new StringBuilder();
            message.append("📝 <b>Напоминание</b>\n\n");
            message.append("Игра завершилась, но еще не помечена как проведенная.\n\n");
            
            if (game.getTitle() != null && !game.getTitle().isEmpty()) {
                message.append("📌 <b>").append(escapeHtml(game.getTitle())).append("</b>\n");
            }
            
            message.append("🕐 <b>Время:</b> ")
                .append(formatInstant(game.getStartTime()))
                .append(" - ")
                .append(formatInstant(game.getEndTime()))
                .append("\n");
            
            String gameUrl = frontendUrl + "?gameId=" + game.getId();
            message.append("\n🔗 <a href=\"").append(gameUrl).append("\">Завершить игру</a>");
            
            sendPersonalMessage(creator.getTelegramChatId(), message.toString());
        }
    }
    
    /**
     * Отправляет групповое напоминание о разметке времени в общий чат
     */
    public void sendGroupTimeSlotReminder() {
        if (!enabled || chatId == null || chatId.isEmpty()) {
            logger.debug("Telegram notifications disabled or chat ID not configured");
            return;
        }
        
        logger.info("Preparing to send group time slot reminder");
        
        try {
            String message = "📅 <b>Напоминание</b>\n\nНе забудьте разметить ваше доступное время в календаре!";
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText(message);
            sendMessage.setParseMode("HTML");
            
            // Если указан Thread ID (для топиков в супергруппах)
            if (threadId != null && !threadId.trim().isEmpty()) {
                try {
                    int threadIdInt = Integer.parseInt(threadId.trim());
                    sendMessage.setMessageThreadId(threadIdInt);
                    logger.info("Sending group reminder to thread ID: {}", threadIdInt);
                } catch (NumberFormatException e) {
                    logger.warn("Invalid thread ID format: '{}', sending to main chat", threadId, e);
                }
            } else {
                logger.debug("No thread ID specified, sending to main chat");
            }
            
            execute(sendMessage);
            logger.info("Group time slot reminder successfully sent");
        } catch (TelegramApiException e) {
            logger.error("Failed to send group time slot reminder", e);
        }
    }
}
