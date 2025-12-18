package ru.ambryo.gameplannerback.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.ambryo.gameplannerback.dto.GameDto;
import ru.ambryo.gameplannerback.entity.User;
import ru.ambryo.gameplannerback.repository.UserRepository;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

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
    
    @Autowired
    private GameService gameService;
    
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
        
        // Обработка callback query (нажатия на inline кнопки)
        if (update.hasCallbackQuery()) {
            handleCallbackQuery(update.getCallbackQuery());
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
            } else if (text.startsWith("/games") || text.startsWith("/upcoming")) {
                handleGamesCommand(telegramUserId, chatId.toString());
            } else if (text.startsWith("/game")) {
                String[] parts = text.split("\\s+", 2);
                if (parts.length == 2) {
                    try {
                        Long gameId = Long.parseLong(parts[1]);
                        handleGameDetailsCommand(telegramUserId, chatId.toString(), gameId);
                    } catch (NumberFormatException e) {
                        sendPersonalMessage(chatId.toString(), "❌ Неверный формат ID игры.\n\nИспользование: /game <id>\n\nПолучите ID из списка игр командой /games");
                    }
                } else {
                    sendPersonalMessage(chatId.toString(), "Использование: /game <id>\n\nПолучите ID из списка игр командой /games");
                }
            } else if (text.startsWith("/help")) {
                handleHelpCommand(chatId.toString());
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
                sendPersonalMessage(chatId, "✅ Вы уже подписаны на уведомления!\n\n" +
                        "Доступные команды:\n" +
                        "/games - Список предстоящих игр\n" +
                        "/help - Справка по командам\n" +
                        "/stop - Отписаться от уведомлений");
            } else {
                // Пользователь не связан
                sendPersonalMessage(chatId, "👋 Добро пожаловать!\n\n" +
                        "Для получения персональных уведомлений необходимо связать ваш Telegram аккаунт с аккаунтом на веб-сайте.\n\n" +
                        "1. Откройте настройки профиля на веб-сайте\n" +
                        "2. Получите токен для связывания\n" +
                        "3. Отправьте команду: /link <token>\n\n" +
                        "После связывания вы сможете использовать команду /games для получения списка предстоящих игр.");
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
            sendPersonalMessage(chatId, "✅ Аккаунт успешно связан!\n\n" +
                    "Теперь вы будете получать персональные уведомления.\n\n" +
                    "Доступные команды:\n" +
                    "/games - Список предстоящих игр\n" +
                    "/help - Справка по командам\n" +
                    "/stop - Отписаться от уведомлений");
        } catch (Exception e) {
            logger.error("Error handling /link command", e);
            sendPersonalMessage(chatId, "❌ Ошибка: " + e.getMessage() + "\n\nПроверьте правильность токена и попробуйте снова.");
        }
    }
    
    private void handleGamesCommand(Long telegramUserId, String chatId) {
        try {
            User user = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
            
            if (user == null) {
                sendPersonalMessage(chatId, "❌ Ваш аккаунт не связан с веб-сайтом.\n\nИспользуйте /link <token> для связывания аккаунта.");
                return;
            }
            
            // Получаем список предстоящих игр для пользователя
            List<GameDto> upcomingGames = gameService.getUpcomingGamesForUser(user.getId());
            
            if (upcomingGames.isEmpty()) {
                sendPersonalMessage(chatId, "📅 <b>Предстоящие игры</b>\n\nУ вас пока нет запланированных игр.");
                return;
            }
            
            String message = buildUpcomingGamesListMessage(upcomingGames);
            sendPersonalMessage(chatId, message);
        } catch (Exception e) {
            logger.error("Error handling /games command", e);
            sendPersonalMessage(chatId, "❌ Ошибка при получении списка игр. Попробуйте позже.");
        }
    }
    
    private void handleHelpCommand(String chatId) {
        StringBuilder help = new StringBuilder();
        help.append("📖 <b>Доступные команды:</b>\n\n");
        help.append("/start - Подписаться на уведомления\n");
        help.append("/stop - Отписаться от уведомлений\n");
        help.append("/link &lt;token&gt; - Связать аккаунт с веб-сайтом\n");
        help.append("/games - Получить список предстоящих игр\n");
        help.append("/upcoming - То же, что и /games\n");
        help.append("/game &lt;id&gt; - Посмотреть детали игры\n");
        help.append("/help - Показать эту справку\n\n");
        help.append("💡 Для получения токена связывания откройте настройки профиля на веб-сайте.\n\n");
        help.append("💡 Используйте кнопки под играми для быстрой записи/отписки.");
        
        sendPersonalMessage(chatId, help.toString());
    }
    
    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        try {
            String data = callbackQuery.getData();
            Long telegramUserId = callbackQuery.getFrom().getId();
            Long chatId = callbackQuery.getMessage().getChatId();
            Integer messageId = callbackQuery.getMessage().getMessageId();
            
            // Отвечаем на callback query, чтобы убрать индикатор загрузки
            answerCallbackQuery(callbackQuery.getId());
            
            if (data.startsWith("join_game_")) {
                Long gameId = Long.parseLong(data.substring("join_game_".length()));
                handleJoinGameCallback(telegramUserId, chatId.toString(), messageId, gameId);
            } else if (data.startsWith("leave_game_")) {
                Long gameId = Long.parseLong(data.substring("leave_game_".length()));
                handleLeaveGameCallback(telegramUserId, chatId.toString(), messageId, gameId);
            } else if (data.startsWith("refresh_game_")) {
                Long gameId = Long.parseLong(data.substring("refresh_game_".length()));
                handleRefreshGameCallback(telegramUserId, chatId.toString(), messageId, gameId);
            }
        } catch (Exception e) {
            logger.error("Error handling callback query", e);
            answerCallbackQuery(callbackQuery.getId(), "❌ Произошла ошибка. Попробуйте позже.");
        }
    }
    
    private void answerCallbackQuery(String callbackQueryId) {
        try {
            AnswerCallbackQuery answer = new AnswerCallbackQuery();
            answer.setCallbackQueryId(callbackQueryId);
            execute(answer);
        } catch (TelegramApiException e) {
            logger.error("Failed to answer callback query", e);
        }
    }
    
    private void answerCallbackQuery(String callbackQueryId, String text) {
        try {
            AnswerCallbackQuery answer = new AnswerCallbackQuery();
            answer.setCallbackQueryId(callbackQueryId);
            answer.setText(text);
            execute(answer);
        } catch (TelegramApiException e) {
            logger.error("Failed to answer callback query", e);
        }
    }
    
    private void handleJoinGameCallback(Long telegramUserId, String chatId, Integer messageId, Long gameId) {
        try {
            User user = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
            
            if (user == null) {
                answerCallbackQuery("", "❌ Ваш аккаунт не связан. Используйте /link для связывания.");
                return;
            }
            
            GameDto game = gameService.joinGame(gameId, user);
            String message = buildGameDetailsMessage(game, user);
            InlineKeyboardMarkup keyboard = buildGameKeyboard(game, user);
            
            EditMessageText editMessage = new EditMessageText();
            editMessage.setChatId(chatId);
            editMessage.setMessageId(messageId);
            editMessage.setText(message);
            editMessage.setParseMode("HTML");
            editMessage.setReplyMarkup(keyboard);
            
            execute(editMessage);
            answerCallbackQuery("", "✅ Вы записались на игру!");
        } catch (RuntimeException e) {
            String errorMsg = e.getMessage();
            if (errorMsg.contains("already joined")) {
                answerCallbackQuery("", "ℹ️ Вы уже записаны на эту игру.");
            } else if (errorMsg.contains("full") || errorMsg.contains("Maximum")) {
                answerCallbackQuery("", "❌ Игра заполнена. Достигнуто максимальное количество участников.");
            } else {
                answerCallbackQuery("", "❌ Ошибка: " + errorMsg);
            }
            logger.error("Error joining game via callback", e);
        } catch (Exception e) {
            answerCallbackQuery("", "❌ Произошла ошибка при записи на игру.");
            logger.error("Error handling join game callback", e);
        }
    }
    
    private void handleLeaveGameCallback(Long telegramUserId, String chatId, Integer messageId, Long gameId) {
        try {
            User user = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
            
            if (user == null) {
                answerCallbackQuery("", "❌ Ваш аккаунт не связан. Используйте /link для связывания.");
                return;
            }
            
            GameDto game = gameService.leaveGame(gameId, user);
            String message = buildGameDetailsMessage(game, user);
            InlineKeyboardMarkup keyboard = buildGameKeyboard(game, user);
            
            EditMessageText editMessage = new EditMessageText();
            editMessage.setChatId(chatId);
            editMessage.setMessageId(messageId);
            editMessage.setText(message);
            editMessage.setParseMode("HTML");
            editMessage.setReplyMarkup(keyboard);
            
            execute(editMessage);
            answerCallbackQuery("", "✅ Вы покинули игру.");
        } catch (RuntimeException e) {
            String errorMsg = e.getMessage();
            if (errorMsg.contains("Creator cannot leave")) {
                answerCallbackQuery("", "ℹ️ Организатор не может покинуть игру. Используйте удаление игры.");
            } else {
                answerCallbackQuery("", "❌ Ошибка: " + errorMsg);
            }
            logger.error("Error leaving game via callback", e);
        } catch (Exception e) {
            answerCallbackQuery("", "❌ Произошла ошибка при отписке от игры.");
            logger.error("Error handling leave game callback", e);
        }
    }
    
    private void handleRefreshGameCallback(Long telegramUserId, String chatId, Integer messageId, Long gameId) {
        try {
            User user = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
            
            if (user == null) {
                answerCallbackQuery("", "❌ Ваш аккаунт не связан. Используйте /link для связывания.");
                return;
            }
            
            GameDto game = gameService.getGameById(gameId);
            String message = buildGameDetailsMessage(game, user);
            InlineKeyboardMarkup keyboard = buildGameKeyboard(game, user);
            
            EditMessageText editMessage = new EditMessageText();
            editMessage.setChatId(chatId);
            editMessage.setMessageId(messageId);
            editMessage.setText(message);
            editMessage.setParseMode("HTML");
            editMessage.setReplyMarkup(keyboard);
            
            execute(editMessage);
        } catch (Exception e) {
            answerCallbackQuery("", "❌ Произошла ошибка при обновлении информации.");
            logger.error("Error refreshing game via callback", e);
        }
    }
    
    private void handleGameDetailsCommand(Long telegramUserId, String chatId, Long gameId) {
        try {
            User user = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
            
            if (user == null) {
                sendPersonalMessage(chatId, "❌ Ваш аккаунт не связан с веб-сайтом.\n\nИспользуйте /link <token> для связывания аккаунта.");
                return;
            }
            
            GameDto game = gameService.getGameById(gameId);
            String message = buildGameDetailsMessage(game, user);
            InlineKeyboardMarkup keyboard = buildGameKeyboard(game, user);
            
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText(message);
            sendMessage.setParseMode("HTML");
            sendMessage.setReplyMarkup(keyboard);
            
            execute(sendMessage);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                sendPersonalMessage(chatId, "❌ Игра не найдена.");
            } else {
                sendPersonalMessage(chatId, "❌ Ошибка: " + e.getMessage());
            }
            logger.error("Error handling /game command", e);
        } catch (Exception e) {
            sendPersonalMessage(chatId, "❌ Произошла ошибка при получении информации об игре.");
            logger.error("Error handling /game command", e);
        }
    }
    
    private String buildUpcomingGamesListMessage(List<GameDto> games) {
        StringBuilder message = new StringBuilder();
        message.append("📅 <b>Предстоящие игры</b>\n\n");
        
        // Сортируем игры по времени начала
        List<GameDto> sortedGames = games.stream()
            .sorted(Comparator.comparing(GameDto::getStartTime))
            .collect(Collectors.toList());
        
        for (int i = 0; i < sortedGames.size(); i++) {
            GameDto game = sortedGames.get(i);
            
            message.append("🎮 <b>").append(i + 1).append(".</b> ");
            
            if (game.getTitle() != null && !game.getTitle().isEmpty()) {
                message.append("<b>").append(escapeHtml(game.getTitle())).append("</b>\n");
            } else {
                message.append("<b>Игра</b>\n");
            }
            
            message.append("🕐 ").append(formatInstant(game.getStartTime()))
                .append(" - ")
                .append(formatInstant(game.getEndTime()))
                .append(" (")
                .append(getTimezoneName())
                .append(")\n");
            
            message.append("👤 Организатор: ").append(escapeHtml(game.getCreatorName())).append("\n");
            
            // Подсчет участников без создателя
            long participantCount = game.getParticipants() != null 
                ? game.getParticipants().stream()
                    .filter(p -> !p.getId().equals(game.getCreatorId()))
                    .count()
                : 0;
            
            Integer maxParticipants = game.getMaxParticipants();
            if (maxParticipants != null) {
                message.append("👥 Участники: ").append(participantCount).append("/").append(maxParticipants);
                if (participantCount >= maxParticipants) {
                    message.append(" (Заполнена)");
                }
            } else {
                message.append("👥 Участники: ").append(participantCount);
            }
            message.append("\n");
            message.append("🆔 ID: <code>").append(game.getId()).append("</code>\n");
            
            if (i < sortedGames.size() - 1) {
                message.append("\n");
            }
        }
        
        message.append("\n💡 Используйте /game &lt;id&gt; для просмотра деталей и записи на игру.");
        
        return message.toString();
    }
    
    private String buildGameDetailsMessage(GameDto game, User user) {
        StringBuilder message = new StringBuilder();
        message.append("🎮 <b>Детали игры</b>\n\n");
        
        if (game.getTitle() != null && !game.getTitle().isEmpty()) {
            message.append("📌 <b>").append(escapeHtml(game.getTitle())).append("</b>\n");
        } else {
            message.append("📌 <b>Игра</b>\n");
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
            message.append("📚 <b>Кампания:</b> ").append(escapeHtml(game.getCampaignName())).append("\n");
        }
        
        String gameUrl = frontendUrl + "?gameId=" + game.getId();
        message.append("\n🔗 <a href=\"").append(gameUrl).append("\">Открыть в веб-интерфейсе</a>");
        
        return message.toString();
    }
    
    private InlineKeyboardMarkup buildGameKeyboard(GameDto game, User user) {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new java.util.ArrayList<>();
        
        boolean isParticipant = game.getParticipants() != null 
            && game.getParticipants().stream().anyMatch(p -> p.getId().equals(user.getId()));
        boolean isCreator = game.getCreatorId().equals(user.getId());
        
        // Подсчет участников без создателя
        long participantCount = game.getParticipants() != null 
            ? game.getParticipants().stream()
                .filter(p -> !p.getId().equals(game.getCreatorId()))
                .count()
            : 0;
        
        Integer maxParticipants = game.getMaxParticipants();
        boolean isFull = maxParticipants != null && participantCount >= maxParticipants;
        
        List<InlineKeyboardButton> buttonRow = new java.util.ArrayList<>();
        
        if (isCreator) {
            // Создатель не может записаться/отписаться
            InlineKeyboardButton viewButton = new InlineKeyboardButton();
            viewButton.setText("👁️ Просмотр (вы организатор)");
            viewButton.setCallbackData("refresh_game_" + game.getId());
            buttonRow.add(viewButton);
        } else if (isParticipant) {
            // Пользователь уже записан - кнопка отписки
            InlineKeyboardButton leaveButton = new InlineKeyboardButton();
            leaveButton.setText("❌ Покинуть игру");
            leaveButton.setCallbackData("leave_game_" + game.getId());
            buttonRow.add(leaveButton);
        } else {
            // Пользователь не записан - кнопка записи
            InlineKeyboardButton joinButton = new InlineKeyboardButton();
            if (isFull) {
                joinButton.setText("🔒 Игра заполнена");
                joinButton.setCallbackData("refresh_game_" + game.getId());
            } else {
                joinButton.setText("✅ Записаться на игру");
                joinButton.setCallbackData("join_game_" + game.getId());
            }
            buttonRow.add(joinButton);
        }
        
        rows.add(buttonRow);
        
        // Кнопка обновления
        List<InlineKeyboardButton> refreshRow = new java.util.ArrayList<>();
        InlineKeyboardButton refreshButton = new InlineKeyboardButton();
        refreshButton.setText("🔄 Обновить");
        refreshButton.setCallbackData("refresh_game_" + game.getId());
        refreshRow.add(refreshButton);
        rows.add(refreshRow);
        
        keyboard.setKeyboard(rows);
        return keyboard;
    }
    
    public void sendPersonalMessage(String chatId, String text) {
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
            try {
                String message = buildGameDetailsMessage(game, user);
                InlineKeyboardMarkup keyboard = buildGameKeyboard(game, user);
                
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(user.getTelegramChatId());
                sendMessage.setText(message);
                sendMessage.setParseMode("HTML");
                sendMessage.setReplyMarkup(keyboard);
                
                execute(sendMessage);
            } catch (TelegramApiException e) {
                logger.error("Failed to send personal game created notification to user {}", user.getId(), e);
            }
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
    
    /**
     * Отправляет токен сброса пароля пользователю в Telegram
     * @param user пользователь
     * @param token токен для сброса пароля
     */
    public void sendPasswordResetToken(User user, String token) {
        if (!enabled) {
            logger.debug("Telegram notifications disabled, cannot send password reset token");
            return;
        }
        
        if (user.getTelegramSubscribed() == null || !user.getTelegramSubscribed() 
                || user.getTelegramChatId() == null) {
            logger.debug("User {} is not subscribed to Telegram or chat ID not available", user.getUsername());
            return;
        }
        
        try {
            StringBuilder message = new StringBuilder();
            message.append("🔐 <b>Сброс пароля</b>\n\n");
            message.append("Вы запросили сброс пароля для аккаунта: <b>").append(escapeHtml(user.getUsername())).append("</b>\n\n");
            message.append("Ваш код для сброса: <code>").append(token).append("</code>\n\n");
            message.append("Используйте этот код на странице восстановления пароля.\n\n");
            message.append("⚠️ Код действителен 1 час.\n\n");
            message.append("Если вы не запрашивали сброс пароля, проигнорируйте это сообщение.");
            
            sendPersonalMessage(user.getTelegramChatId(), message.toString());
            logger.info("Password reset token sent to user {} via Telegram", user.getUsername());
        } catch (Exception e) {
            logger.error("Failed to send password reset token to user {} via Telegram", user.getUsername(), e);
            throw e; // Пробрасываем исключение, чтобы вызывающий код мог его обработать
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
