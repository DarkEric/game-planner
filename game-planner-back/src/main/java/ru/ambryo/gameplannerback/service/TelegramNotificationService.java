package ru.ambryo.gameplannerback.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.ambryo.gameplannerback.dto.GameDto;
import ru.ambryo.gameplannerback.entity.Game;
import ru.ambryo.gameplannerback.entity.GameNotification;
import ru.ambryo.gameplannerback.entity.User;
import ru.ambryo.gameplannerback.entity.UserNotificationSettings;
import ru.ambryo.gameplannerback.repository.GameNotificationRepository;
import ru.ambryo.gameplannerback.repository.UserNotificationSettingsRepository;
import ru.ambryo.gameplannerback.repository.UserRepository;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

@Service
public class TelegramNotificationService extends TelegramLongPollingBot {
    
    private static final Logger logger = LoggerFactory.getLogger(TelegramNotificationService.class);

    private final String configuredBotToken;
    
    public TelegramNotificationService(
            @Autowired(required = false) DefaultBotOptions telegramBotOptions,
            @Value("${telegram.bot.token:}") String botToken) {
        super(telegramBotOptions != null ? telegramBotOptions : new DefaultBotOptions(), botToken);
        this.configuredBotToken = botToken;
    }
    
    @Value("${telegram.bot.enabled:false}")
    private boolean enabled;
    
    @Value("${telegram.bot.chat-id:}")
    private String chatId;
    
    @Value("${telegram.bot.thread-id:}")
    private String threadId;
    
    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;
    
    @Value("${telegram.bot.timezone:Europe/Moscow}")
    private String timezoneId;

    /** Только для лога: при непустом значении включён {@link java.net.Authenticator} для SOCKS5. */
    @Value("${telegram.bot.proxy.username:}")
    private String proxyAuthUsername;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ru.ambryo.gameplannerback.service.telegram.command.CommandRouter commandRouter;
    
    @Autowired
    private ru.ambryo.gameplannerback.service.telegram.menu.MenuRouter menuRouter;
    
    @Autowired
    private ru.ambryo.gameplannerback.service.telegram.state.AuthStateManager authStateManager;
    
    @Autowired
    private ru.ambryo.gameplannerback.service.telegram.state.RegistrationStateManager registrationStateManager;
    
    @Autowired
    private ru.ambryo.gameplannerback.service.telegram.state.TimeSlotMarkingStateManager timeSlotMarkingStateManager;
    
    @Autowired
    private ru.ambryo.gameplannerback.service.telegram.state.TimezoneChangeStateManager timezoneChangeStateManager;
    
    @Autowired
    private ru.ambryo.gameplannerback.service.telegram.state.NotificationStateManager notificationStateManager;
    
    @Autowired
    private ru.ambryo.gameplannerback.service.telegram.state.StateRouter stateRouter;
    
    @Autowired
    private ru.ambryo.gameplannerback.service.telegram.notification.GroupNotificationSender groupNotificationSender;
    
    @Autowired
    private ru.ambryo.gameplannerback.service.telegram.notification.GameNotificationSender gameNotificationSender;
    
    @Autowired
    private ru.ambryo.gameplannerback.service.telegram.notification.PersonalNotificationSender personalNotificationSender;
    
    @Autowired
    private ru.ambryo.gameplannerback.service.telegram.util.TelegramMessageSender messageSender;

    @Autowired
    private UserNotificationSettingsRepository settingsRepository;

    @Autowired
    private GameNotificationRepository gameNotificationRepository;
    
    // Старые системы состояний, enum'ы и Maps удалены - теперь используется StateManager классы
    // Старые константы удалены - теперь используется TelegramBotProperties
    
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
    
    // Старые вспомогательные методы для работы с состояниями удалены - теперь используется StateManager классы
    // Методы парсинга и форматирования удалены - теперь используются утилиты (TelegramDateParser, TelegramTimeFormatter, TelegramHtmlFormatter)
    
    @Override
    public String getBotUsername() {
        return "GamePlannerBot";
    }
    
    @PostConstruct
    public void registerBotCommands() {
        if (!enabled || configuredBotToken == null || configuredBotToken.isEmpty()) {
            logger.debug("Telegram bot disabled or token not set, skipping command registration");
            return;
        }
        
        try {
            SetMyCommands setMyCommands = new SetMyCommands();
            List<BotCommand> commands = new java.util.ArrayList<>();
            
            BotCommand menuCommand = new BotCommand();
            menuCommand.setCommand("menu");
            menuCommand.setDescription("Главное меню");
            commands.add(menuCommand);
            
            setMyCommands.setCommands(commands);
            execute(setMyCommands);
            
            logger.info("Bot commands registered successfully");
        } catch (TelegramApiException e) {
            logger.error("Failed to register bot commands", e);
        }
    }
    
    @Override
    public void onUpdateReceived(Update update) {
        if (!enabled) {
            return;
        }
        
        // Обработка callback query (нажатия на inline кнопки)
        if (update.hasCallbackQuery()) {
            CallbackQuery callbackQuery = update.getCallbackQuery();
            Long telegramUserId = callbackQuery.getFrom().getId();
            String chatId = callbackQuery.getMessage().getChatId().toString();
            Integer messageId = callbackQuery.getMessage().getMessageId();
            
            // Отвечаем на callback query, чтобы убрать индикатор загрузки
            answerCallbackQuery(callbackQuery.getId());
            
            // Используем MenuRouter для обработки callback'ов
            menuRouter.handle(callbackQuery, telegramUserId, chatId, messageId);
            return;
        }
        
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message message = update.getMessage();
            String text = message.getText();
            String chatIdStr = message.getChatId().toString();
            Long telegramUserId = message.getFrom().getId();
            
            // Проверяем истечение состояний (StateRouter обработает это внутри)
            
            // Обработка команд (команды имеют приоритет над состояниями)
            if (text.startsWith("/")) {
                // Очищаем все состояния перед обработкой команды
                authStateManager.clearState(chatIdStr);
                registrationStateManager.clearState(chatIdStr);
                timeSlotMarkingStateManager.clearState(chatIdStr);
                timezoneChangeStateManager.clearState(chatIdStr);
                notificationStateManager.clearState(chatIdStr);
                
                // Используем CommandRouter для обработки команд
                commandRouter.handle(message, telegramUserId, chatIdStr);
            } else {
                // Используем StateRouter для обработки состояний
                stateRouter.handle(telegramUserId, chatIdStr, text);
            }
        }
    }
    
    // Старые методы команд удалены - теперь используется CommandRouter с CommandHandler классами
    // Методы обработки состояний удалены - теперь используется StateRouter с StateHandler классами
    
    // Старые методы команд удалены - теперь используется CommandRouter с CommandHandler классами
    
    // Методы buildMainMenuKeyboard, buildGamesMenuKeyboard, buildTimeMenuKeyboard, buildInvitesMenuKeyboard, buildSettingsMenuKeyboard, handleHelpCommand удалены - теперь используются KeyboardBuilder классы и HelpCommandHandler
    
    // Метод handleCallbackQuery больше не используется напрямую,
    // обработка callback'ов теперь происходит через MenuRouter в onUpdateReceived
    
    // Метод handleMenuCallback больше не используется напрямую,
    // обработка всех callback'ов теперь происходит через MenuRouter в onUpdateReceived
    // Метод updateMenuMessage удален - теперь используется MenuMessageUpdater
    // Методы handleMenuGamesList больше не используются напрямую,
    // обработка menu_games_list и menu_games_page_ теперь происходит через GamesMenuHandler в MenuRouter
    
    // Метод buildGamesListKeyboard удален - теперь используется GamesMenuKeyboardBuilder
    
    // Метод handleMenuTimeMark больше не используется напрямую,
    // обработка menu_time_mark теперь происходит через TimeMenuHandler в MenuRouter
    
    // Метод handleMenuTimeSlots больше не используется напрямую,
    // обработка menu_time_slots теперь происходит через TimeMenuHandler в MenuRouter
    
    // Старые методы обработки меню удалены - теперь используется MenuRouter с MenuHandler классами
    
    // Метод buildTimezoneSelectorKeyboard удален - теперь используется TimezoneSelectorKeyboardBuilder
    // Старые методы обработки callback'ов часового пояса удалены - теперь используется TimezoneMenuHandler в MenuRouter
    // Методы обработки состояний удалены - теперь используется StateRouter с StateHandler классами
    
    private void answerCallbackQuery(String callbackQueryId) {
        try {
            AnswerCallbackQuery answer = new AnswerCallbackQuery();
            answer.setCallbackQueryId(callbackQueryId);
            execute(answer);
        } catch (TelegramApiException e) {
            logger.error("Failed to answer callback query", e);
        }
    }
    
    // Старые методы обработки callback'ов игр удалены - теперь используется GameActionHandler и GamesMenuHandler в MenuRouter
    // Метод buildGameKeyboardWithBack удален - теперь используется GamesMenuKeyboardBuilder.buildGameKeyboardWithBack
    // Метод buildUpcomingGamesListMessage удален - теперь используется GameMessageBuilder.buildUpcomingGamesListMessage
    
    // Методы buildUpcomingGamesListMessage, buildGameDetailsMessage, buildGameKeyboard удалены - теперь используются GameMessageBuilder и GamesMenuKeyboardBuilder
    // Методы buildInviteCreatedMessage и buildMyInvitesListMessage удалены - теперь используется InviteMessageBuilder
    
    // Методы форматирования удалены - теперь используются утилиты (TelegramTimeFormatter, TelegramHtmlFormatter)
    
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
        DefaultBotOptions opts = getOptions();
        if (opts.getProxyType() != DefaultBotOptions.ProxyType.NO_PROXY) {
            logger.info("Proxy: {} {}:{}", opts.getProxyType(), opts.getProxyHost(), opts.getProxyPort());
            if (proxyAuthUsername != null && !proxyAuthUsername.isBlank()) {
                logger.info("Proxy auth: enabled (user: {})", proxyAuthUsername);
            }
        } else {
            logger.info("Proxy: none");
        }
        logger.info("==================================");
    }
    
    // Методы форматирования удалены - теперь используются утилиты (TelegramTimeFormatter, TelegramHtmlFormatter)
    
    // Групповые уведомления делегируются в GroupNotificationSender
    public void sendGameCreatedNotification(GameDto game) {
        groupNotificationSender.sendGameCreatedNotification(game);
    }
    
    public void sendGameCancelledNotification(GameDto game, String cancellationReason) {
        groupNotificationSender.sendGameCancelledNotification(game, cancellationReason);
    }

    public void sendGameHeldNotification(GameDto game) {
        groupNotificationSender.sendGameHeldNotification(game);
    }
    
    // Персональные уведомления делегируются в PersonalNotificationSender и GameNotificationSender
    
    /**
     * Отправляет персональное сообщение по chatId
     * Используется для обратной совместимости с существующим кодом
     */
    public void sendPersonalMessage(String chatId, String message) {
        if (!enabled) {
            return;
        }
        
        try {
            messageSender.sendPersonalMessage(chatId, message);
        } catch (Exception e) {
            logger.error("Failed to send personal message to chat {}", chatId, e);
        }
    }
    
    public void sendPersonalNotification(Long telegramUserId, String message) {
        if (!enabled) {
            return;
        }
        
        User user = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
        if (user == null || !user.getTelegramSubscribed() || user.getTelegramChatId() == null) {
            return;
        }
        
        try {
            messageSender.sendPersonalMessage(user.getTelegramChatId(), message);
        } catch (Exception e) {
            logger.error("Failed to send personal notification to user {}", telegramUserId, e);
        }
    }
    
    public void sendGameCreatedPersonalNotification(GameDto game, User user) {
        gameNotificationSender.sendGameCreatedNotification(game, user);
    }
    
    public void sendGameCancelledPersonalNotification(GameDto game, User user) {
        gameNotificationSender.sendGameCancelledNotification(game, user);
    }
    
    public void sendGameHeldPersonalNotification(GameDto game, User user) {
        gameNotificationSender.sendGameHeldNotification(game, user);
    }

    public void sendPersonalGameCreatedNotifications(GameDto gameDto, Game game, List<User> allUsers) {
        for (User user : allUsers) {
            if (user.getTelegramSubscribed() == null || !user.getTelegramSubscribed()) {
                continue;
            }

            UserNotificationSettings settings = settingsRepository.findByUserId(user.getId()).orElse(null);
            if (settings == null) {
                continue;
            }

            String setting = settings.getGameCreated();
            boolean shouldNotify = false;

            if ("ALL".equals(setting)) {
                shouldNotify = true;
            } else if ("MY_GAMES".equals(setting)) {
                boolean isParticipant = game.getParticipants().stream()
                        .anyMatch(p -> p.getId().equals(user.getId()));
                boolean isCreator = game.getCreator().getId().equals(user.getId());
                shouldNotify = isParticipant || isCreator;
            }

            if (shouldNotify) {
                GameNotification existing = gameNotificationRepository.findPersonalNotification(
                        game, "GAME_CREATED", user).orElse(null);

                if (existing == null) {
                    gameNotificationSender.sendGameCreatedNotification(gameDto, user);

                    GameNotification notification = new GameNotification(game, "GAME_CREATED", user);
                    gameNotificationRepository.save(notification);
                }
            }
        }
    }

    public void sendPersonalGameCancelledNotifications(GameDto gameDto, Game game) {
        for (User participant : game.getParticipants()) {
            if (participant.getTelegramSubscribed() == null || !participant.getTelegramSubscribed()) {
                continue;
            }

            UserNotificationSettings settings = settingsRepository.findByUserId(participant.getId()).orElse(null);
            if (settings == null) {
                continue;
            }

            String setting = settings.getGameCancelled();
            boolean shouldNotify = "ALL".equals(setting) || "MY_GAMES".equals(setting);

            if (shouldNotify) {
                GameNotification existing = gameNotificationRepository.findPersonalNotification(
                        game, "GAME_CANCELLED", participant).orElse(null);

                if (existing == null) {
                    gameNotificationSender.sendGameCancelledNotification(gameDto, participant);
                }
            }
        }
    }

    public void sendPersonalGameHeldNotifications(GameDto gameDto, Game game) {
        for (User participant : game.getParticipants()) {
            if (participant.getTelegramSubscribed() == null || !participant.getTelegramSubscribed()) {
                continue;
            }

            UserNotificationSettings settings = settingsRepository.findByUserId(participant.getId()).orElse(null);
            if (settings == null) {
                continue;
            }

            String setting = settings.getGameHeld();
            boolean shouldNotify = "ALL".equals(setting) || "MY_GAMES".equals(setting);

            if (shouldNotify) {
                GameNotification existing = gameNotificationRepository.findPersonalNotification(
                        game, "GAME_HELD", participant).orElse(null);

                if (existing == null) {
                    gameNotificationSender.sendGameHeldNotification(gameDto, participant);

                    GameNotification notification = new GameNotification(game, "GAME_HELD", participant);
                    gameNotificationRepository.save(notification);
                }
            }
        }
    }
    
    public void sendPlayerRemovedFromGameNotification(GameDto game, User removedPlayer) {
        gameNotificationSender.sendPlayerRemovedFromGameNotification(game, removedPlayer);
    }
    
    public void sendUpcomingGameReminder(GameDto game, User user, int minutesBefore) {
        gameNotificationSender.sendUpcomingGameReminder(game, user, minutesBefore);
    }
    
    public void sendTimeSlotReminder(User user) {
        personalNotificationSender.sendTimeSlotReminder(user);
    }
    
    public void sendPasswordResetToken(User user, String token) {
        if (!enabled) {
            logger.debug("Telegram notifications disabled, cannot send password reset token");
            return;
        }
        personalNotificationSender.sendPasswordResetToken(user, token);
    }
    
    public void sendGameCompletionReminder(GameDto game, User creator) {
        gameNotificationSender.sendGameCompletionReminder(game, creator);
    }
    
    public void sendGroupTimeSlotReminder() {
        groupNotificationSender.sendGroupTimeSlotReminder();
    }
    
    // Методы для работы с настройками уведомлений удалены - теперь используется NotificationsMenuHandler
    // Методы обработки состояний уведомлений удалены - теперь используется NotificationStateHandler в StateRouter
}
