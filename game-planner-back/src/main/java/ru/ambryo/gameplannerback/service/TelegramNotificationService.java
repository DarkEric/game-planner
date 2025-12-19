package ru.ambryo.gameplannerback.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import jakarta.annotation.PostConstruct;
import ru.ambryo.gameplannerback.dto.GameDto;
import ru.ambryo.gameplannerback.dto.UserNotificationSettingsDto;
import ru.ambryo.gameplannerback.dto.UpcomingGameReminderDto;
import ru.ambryo.gameplannerback.entity.User;
import ru.ambryo.gameplannerback.repository.UserRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

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
    
    @Autowired
    private AuthService authService;
    
    @Autowired
    private InviteService inviteService;
    
    @Autowired
    private UserService userService;
    
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
    private ru.ambryo.gameplannerback.service.telegram.config.TelegramBotProperties telegramBotProperties;
    
    @Autowired
    private ru.ambryo.gameplannerback.service.telegram.message.TimeSlotMessageBuilder timeSlotMessageBuilder;
    
    @Autowired
    private ru.ambryo.gameplannerback.service.telegram.message.GameMessageBuilder gameMessageBuilder;
    
    @Autowired
    private ru.ambryo.gameplannerback.service.telegram.message.InviteMessageBuilder inviteMessageBuilder;
    
    @Autowired
    private ru.ambryo.gameplannerback.service.telegram.keyboard.GamesMenuKeyboardBuilder gamesMenuKeyboardBuilder;
    
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
    
    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        // Простая проверка формата email
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }
    
    // Методы парсинга даты, времени и продолжительности
    
    /**
     * Парсит дату в русском формате (15.01.2025, 15/01/2025) или относительную дату (сегодня, завтра, послезавтра)
     * @param dateStr строка с датой
     * @param userTimezone часовой пояс пользователя
     * @return LocalDate или null если не удалось распарсить
     */
    private LocalDate parseDate(String dateStr, ZoneId userTimezone) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        
        String trimmed = dateStr.trim().toLowerCase();
        
        // Относительные даты
        LocalDate now = LocalDate.now(userTimezone);
        if (trimmed.equals("сегодня") || trimmed.equals("today")) {
            return now;
        } else if (trimmed.equals("завтра") || trimmed.equals("tomorrow")) {
            return now.plusDays(1);
        } else if (trimmed.equals("послезавтра") || trimmed.equals("day after tomorrow")) {
            return now.plusDays(2);
        }
        
        // Русский формат: DD.MM.YYYY или DD/MM/YYYY
        DateTimeFormatter[] formatters = {
            DateTimeFormatter.ofPattern("d.M.yyyy"),
            DateTimeFormatter.ofPattern("dd.MM.yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("d.M.yyyy"),
            DateTimeFormatter.ofPattern("dd.MM.yyyy")
        };
        
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(trimmed, formatter);
            } catch (DateTimeParseException e) {
                // Пробуем следующий формат
            }
        }
        
        return null;
    }
    
    /**
     * Парсит время в формате HH:mm или HH
     * @param timeStr строка со временем
     * @return LocalTime или null если не удалось распарсить
     */
    private LocalTime parseTime(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            return null;
        }
        
        String trimmed = timeStr.trim();
        
        // Формат HH:mm
        if (trimmed.contains(":")) {
            try {
                return LocalTime.parse(trimmed, DateTimeFormatter.ofPattern("H:mm"));
            } catch (DateTimeParseException e) {
                try {
                    return LocalTime.parse(trimmed, DateTimeFormatter.ofPattern("HH:mm"));
                } catch (DateTimeParseException e2) {
                    return null;
                }
            }
        }
        
        // Формат HH (только часы)
        try {
            int hours = Integer.parseInt(trimmed);
            if (hours >= 0 && hours <= 23) {
                return LocalTime.of(hours, 0);
            }
        } catch (NumberFormatException e) {
            // Не число
        }
        
        return null;
    }
    
    /**
     * Парсит продолжительность в часах
     * @param durationStr строка с продолжительностью
     * @return Integer (количество часов) или null если не удалось распарсить
     */
    private Integer parseDuration(String durationStr) {
        if (durationStr == null || durationStr.trim().isEmpty()) {
            return null;
        }
        
        try {
            // Убираем слово "час" или "часов" если есть
            String trimmed = durationStr.trim().toLowerCase()
                    .replaceAll("\\s*час(ов|а)?\\s*", "");
            
            int duration = Integer.parseInt(trimmed);
            if (duration > 0 && duration <= 24) {
                return duration;
            }
        } catch (NumberFormatException e) {
            // Не число
        }
        
        return null;
    }
    
    /**
     * Конвертирует локальное время пользователя в UTC
     * @param localDate дата в локальном времени
     * @param localTime время в локальном времени
     * @param userTimezone часовой пояс пользователя
     * @return Instant в UTC
     */
    private Instant convertToUTC(LocalDate localDate, LocalTime localTime, ZoneId userTimezone) {
        LocalDateTime localDateTime = LocalDateTime.of(localDate, localTime);
        ZonedDateTime zonedDateTime = localDateTime.atZone(userTimezone);
        return zonedDateTime.toInstant();
    }
    
    @Override
    public String getBotUsername() {
        return "GamePlannerBot";
    }
    
    @Override
    public String getBotToken() {
        return botToken;
    }
    
    @PostConstruct
    public void registerBotCommands() {
        if (!enabled || botToken == null || botToken.isEmpty()) {
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
            
            // Проверяем, не истекло ли состояние авторизации
            if (authStateManager.hasState(chatIdStr) && authStateManager.isStateExpired(chatIdStr)) {
                authStateManager.clearState(chatIdStr);
                sendPersonalMessage(chatIdStr, "⏱️ Время ожидания истекло. Процесс авторизации отменен.\n\nИспользуйте /auth для начала заново.");
            }
            
            // Проверяем, не истекло ли состояние регистрации
            if (registrationStateManager.hasState(chatIdStr) && registrationStateManager.isStateExpired(chatIdStr)) {
                registrationStateManager.clearState(chatIdStr);
                sendPersonalMessage(chatIdStr, "⏱️ Время ожидания истекло. Процесс регистрации отменен.\n\nИспользуйте /register для начала заново.");
            }
            
            // Проверяем, не истекло ли состояние разметки времени
            if (timeSlotMarkingStateManager.hasState(chatIdStr) && timeSlotMarkingStateManager.isStateExpired(chatIdStr)) {
                timeSlotMarkingStateManager.clearState(chatIdStr);
                sendPersonalMessage(chatIdStr, "⏱️ Время ожидания истекло. Процесс разметки времени отменен.\n\nИспользуйте /mark для начала заново.");
            }
            
            // Проверяем, не истекло ли состояние смены часового пояса
            if (timezoneChangeStateManager.hasState(chatIdStr) && timezoneChangeStateManager.isStateExpired(chatIdStr)) {
                timezoneChangeStateManager.clearState(chatIdStr);
                sendPersonalMessage(chatIdStr, "⏱️ Время ожидания истекло. Процесс смены часового пояса отменен.\n\nИспользуйте меню для начала заново.");
            }
            
            // Проверяем, не истекло ли состояние настроек уведомлений
            if (notificationStateManager.hasState(chatIdStr) && notificationStateManager.isStateExpired(chatIdStr)) {
                notificationStateManager.clearState(chatIdStr);
                sendPersonalMessage(chatIdStr, "⏱️ Время ожидания истекло. Процесс настройки уведомлений отменен.\n\nИспользуйте меню для начала заново.");
            }
            
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
                // Обработка состояний (если не команда)
                // Проверяем в порядке приоритета: регистрация -> авторизация -> разметка времени -> смена часового пояса -> настройки уведомлений
                var regState = registrationStateManager.getState(chatIdStr);
                if (regState != null) {
                    handleRegistrationState(telegramUserId, chatIdStr, text, regState);
                } else {
                    var authState = authStateManager.getState(chatIdStr);
                    if (authState != null) {
                        handleAuthState(telegramUserId, chatIdStr, text, authState);
                    } else {
                        var markingState = timeSlotMarkingStateManager.getState(chatIdStr);
                        if (markingState != null) {
                            handleTimeSlotMarkingState(telegramUserId, chatIdStr, text, markingState);
                        } else {
                            var timezoneState = timezoneChangeStateManager.getState(chatIdStr);
                            if (timezoneState != null) {
                                handleTimezoneChangeState(telegramUserId, chatIdStr, text, timezoneState);
                            } else {
                                var notificationState = notificationStateManager.getState(chatIdStr);
                                if (notificationState != null) {
                                    handleNotificationState(telegramUserId, chatIdStr, text, notificationState);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Старые методы команд удалены - теперь используется CommandRouter с CommandHandler классами
    
    // Метод handleCancelCommand больше не используется напрямую,
    // обработка команды /cancel теперь происходит через CancelCommandHandler
    
    private void handleAuthState(Long telegramUserId, String chatId, String text, ru.ambryo.gameplannerback.service.telegram.state.AuthStateManager.AuthState state) {
        try {
            authStateManager.updateTimestamp(chatId);
            
            if (state == ru.ambryo.gameplannerback.service.telegram.state.AuthStateManager.AuthState.WAITING_USERNAME) {
                // Сохраняем username и переходим к паролю
                String username = text.trim();
                if (username.isEmpty()) {
                    sendPersonalMessage(chatId, "❌ Логин не может быть пустым. Введите ваш логин:");
                    return;
                }
                
                authStateManager.setUsername(chatId, username);
                authStateManager.setState(chatId, ru.ambryo.gameplannerback.service.telegram.state.AuthStateManager.AuthState.WAITING_PASSWORD);
                
                sendPersonalMessage(chatId, "🔑 Теперь введите ваш пароль:\n\n" +
                        "💡 Используйте /cancel для отмены.");
                
            } else if (state == ru.ambryo.gameplannerback.service.telegram.state.AuthStateManager.AuthState.WAITING_PASSWORD) {
                // Проверяем учетные данные и связываем аккаунт
                String password = text.trim();
                String username = authStateManager.getUsername(chatId);
                
                if (password.isEmpty()) {
                    sendPersonalMessage(chatId, "❌ Пароль не может быть пустым. Введите ваш пароль:");
                    return;
                }
                
                if (username == null || username.isEmpty()) {
                    // Не должно произойти, но на всякий случай
                    authStateManager.clearState(chatId);
                    sendPersonalMessage(chatId, "❌ Ошибка: логин не найден. Начните заново с /auth.");
                    return;
                }
                
                // Проверяем блокировку перед попыткой
                if (authStateManager.isBlocked(chatId)) {
                    long remainingSeconds = authStateManager.getBlockTimeRemaining(chatId);
                    long remainingMinutes = remainingSeconds / 60;
                    authStateManager.clearState(chatId);
                    sendPersonalMessage(chatId, "⛔ Слишком много неудачных попыток авторизации.\n\n" +
                            "Попробуйте снова через " + remainingMinutes + " минут.");
                    return;
                }
                
                try {
                    // Пытаемся связать аккаунт
                    notificationSettingsService.linkTelegramAccountByCredentials(
                            username, password, telegramUserId, chatId);
                    
                    // Успешная авторизация
                    authStateManager.recordAttempt(chatId, true);
                    authStateManager.clearState(chatId);
                    
                    sendPersonalMessage(chatId, "✅ <b>Аккаунт успешно связан!</b>\n\n" +
                            "Теперь вы будете получать персональные уведомления.\n\n" +
                            "Доступные команды:\n" +
                            "/games - Список предстоящих игр\n" +
                            "/help - Справка по командам\n" +
                            "/stop - Отписаться от уведомлений");
                    
                    logger.info("Telegram account linked via auth for chatId: {}", chatId);
                    
                } catch (RuntimeException e) {
                    // Неудачная попытка
                    authStateManager.recordAttempt(chatId, false);
                    
                    int remainingAttempts = authStateManager.getRemainingAttempts(chatId);
                    
                    if (authStateManager.isBlocked(chatId)) {
                        long remainingSeconds = authStateManager.getBlockTimeRemaining(chatId);
                        long remainingMinutes = remainingSeconds / 60;
                        authStateManager.clearState(chatId);
                        sendPersonalMessage(chatId, "⛔ <b>Слишком много неудачных попыток</b>\n\n" +
                                "Авторизация заблокирована на " + remainingMinutes + " минут.\n\n" +
                                "Попробуйте снова позже или используйте /link <token> для привязки через токен.");
                        logger.warn("Telegram auth blocked for chatId: {} after failed attempt", chatId);
                    } else {
                        // Ошибка авторизации, но еще есть попытки
                        String errorMessage = e.getMessage();
                        if (errorMessage != null && errorMessage.contains("Invalid username or password")) {
                            sendPersonalMessage(chatId, "❌ <b>Неверный логин или пароль</b>\n\n" +
                                    "Осталось попыток: " + remainingAttempts + "\n\n" +
                                    "Введите пароль еще раз или используйте /cancel для отмены.");
                        } else if (errorMessage != null && errorMessage.contains("уже связан")) {
                            authStateManager.clearState(chatId);
                            sendPersonalMessage(chatId, "❌ " + errorMessage + "\n\n" +
                                    "Используйте /start для проверки статуса.");
                        } else {
                            sendPersonalMessage(chatId, "❌ Ошибка: " + (errorMessage != null ? errorMessage : "Неизвестная ошибка") + "\n\n" +
                                    "Осталось попыток: " + remainingAttempts + "\n\n" +
                                    "Попробуйте снова или используйте /cancel для отмены.");
                        }
                        logger.warn("Telegram auth failed for chatId: {}, remaining attempts: {}", chatId, remainingAttempts);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error handling auth state", e);
            authStateManager.clearState(chatId);
            sendPersonalMessage(chatId, "❌ Произошла ошибка при обработке авторизации. Попробуйте позже или используйте /link <token>.");
        }
    }
    
    private void handleRegistrationState(Long telegramUserId, String chatId, String text, ru.ambryo.gameplannerback.service.telegram.state.RegistrationStateManager.RegistrationState state) {
        try {
            registrationStateManager.updateTimestamp(chatId);
            var data = registrationStateManager.getData(chatId);
            
            if (data == null) {
                registrationStateManager.clearState(chatId);
                sendPersonalMessage(chatId, "❌ Ошибка: данные регистрации не найдены. Начните заново с /register.");
                return;
            }
            
            switch (state) {
                case WAITING_INVITE:
                    handleInviteInput(telegramUserId, chatId, text.trim(), data);
                    break;
                case WAITING_USERNAME:
                    handleUsernameInput(chatId, text.trim(), data);
                    break;
                case WAITING_NAME:
                    handleNameInput(chatId, text.trim(), data);
                    break;
                case WAITING_EMAIL:
                    handleEmailInput(chatId, text.trim(), data);
                    break;
                case WAITING_PASSWORD:
                    handlePasswordInput(chatId, text.trim(), data);
                    break;
                case WAITING_PASSWORD_CONFIRM:
                    handlePasswordConfirmInput(telegramUserId, chatId, text.trim(), data);
                    break;
            }
        } catch (Exception e) {
            logger.error("Error handling registration state", e);
            registrationStateManager.clearState(chatId);
            sendPersonalMessage(chatId, "❌ Произошла ошибка при обработке регистрации. Попробуйте позже.");
        }
    }
    
    private void handleInviteInput(Long telegramUserId, String chatId, String inviteCode, ru.ambryo.gameplannerback.service.telegram.state.RegistrationStateManager.RegistrationData data) {
        if (inviteCode.isEmpty()) {
            sendPersonalMessage(chatId, "❌ Инвайт-код не может быть пустым. Введите инвайт-код:");
            return;
        }
        
        try {
            // Проверяем валидность инвайт-кода
            inviteService.getInviteByCode(inviteCode);
            data.inviteCode = inviteCode;
            registrationStateManager.setState(chatId, ru.ambryo.gameplannerback.service.telegram.state.RegistrationStateManager.RegistrationState.WAITING_USERNAME);
            
            sendPersonalMessage(chatId, "✅ Инвайт-код принят!\n\n" +
                    "Введите логин (имя пользователя):\n\n" +
                    "💡 Используйте /cancel для отмены.");
        } catch (RuntimeException e) {
            String errorMsg = e.getMessage();
            if (errorMsg != null && (errorMsg.contains("not found") || errorMsg.contains("Invalid"))) {
                sendPersonalMessage(chatId, "❌ <b>Неверный инвайт-код</b>\n\n" +
                        "Проверьте правильность кода и попробуйте снова.\n\n" +
                        "💡 Используйте /cancel для отмены.");
            } else if (errorMsg != null && (errorMsg.contains("expired") || errorMsg.contains("used"))) {
                sendPersonalMessage(chatId, "❌ <b>Инвайт-код недействителен</b>\n\n" +
                        "Код истек или уже использован.\n\n" +
                        "💡 Используйте /cancel для отмены.");
            } else {
                sendPersonalMessage(chatId, "❌ Ошибка: " + (errorMsg != null ? errorMsg : "Неизвестная ошибка") + "\n\n" +
                        "Попробуйте снова или используйте /cancel для отмены.");
            }
            logger.warn("Invalid invite code for registration: {}", inviteCode);
        }
    }
    
    private void handleUsernameInput(String chatId, String username, ru.ambryo.gameplannerback.service.telegram.state.RegistrationStateManager.RegistrationData data) {
        if (username.isEmpty()) {
            sendPersonalMessage(chatId, "❌ Логин не может быть пустым. Введите логин:");
            return;
        }
        
        // Проверяем уникальность логина
        if (userRepository.existsByUsername(username)) {
            sendPersonalMessage(chatId, "❌ <b>Логин уже занят</b>\n\n" +
                    "Выберите другой логин:\n\n" +
                    "💡 Используйте /cancel для отмены.");
            return;
        }
        
        data.username = username;
        registrationStateManager.setState(chatId, ru.ambryo.gameplannerback.service.telegram.state.RegistrationStateManager.RegistrationState.WAITING_NAME);
        
        sendPersonalMessage(chatId, "✅ Логин принят!\n\n" +
                "Введите ваше имя (или нажмите Enter, чтобы использовать логин):\n\n" +
                "💡 Используйте /cancel для отмены.");
    }
    
    private void handleNameInput(String chatId, String name, ru.ambryo.gameplannerback.service.telegram.state.RegistrationStateManager.RegistrationData data) {
        // Имя опционально, если пустое - используем username
        if (name.trim().isEmpty()) {
            data.name = data.username;
        } else {
            data.name = name.trim();
        }
        
        registrationStateManager.setState(chatId, ru.ambryo.gameplannerback.service.telegram.state.RegistrationStateManager.RegistrationState.WAITING_EMAIL);
        
        sendPersonalMessage(chatId, "✅ Имя принято!\n\n" +
                "Введите ваш email:\n\n" +
                "💡 Используйте /cancel для отмены.");
    }
    
    private void handleEmailInput(String chatId, String email, ru.ambryo.gameplannerback.service.telegram.state.RegistrationStateManager.RegistrationData data) {
        if (email.isEmpty()) {
            sendPersonalMessage(chatId, "❌ Email не может быть пустым. Введите email:");
            return;
        }
        
        // Проверяем формат email
        if (!isValidEmail(email)) {
            sendPersonalMessage(chatId, "❌ <b>Неверный формат email</b>\n\n" +
                    "Введите корректный email адрес:\n\n" +
                    "💡 Используйте /cancel для отмены.");
            return;
        }
        
        // Проверяем уникальность email
        if (userRepository.existsByEmail(email)) {
            sendPersonalMessage(chatId, "❌ <b>Email уже используется</b>\n\n" +
                    "Используйте другой email:\n\n" +
                    "💡 Используйте /cancel для отмены.");
            return;
        }
        
        data.email = email;
        registrationStateManager.setState(chatId, ru.ambryo.gameplannerback.service.telegram.state.RegistrationStateManager.RegistrationState.WAITING_PASSWORD);
        
        sendPersonalMessage(chatId, "✅ Email принят!\n\n" +
                "Введите пароль (минимум " + telegramBotProperties.getMinPasswordLength() + " символов):\n\n" +
                "💡 Используйте /cancel для отмены.");
    }
    
    private void handlePasswordInput(String chatId, String password, ru.ambryo.gameplannerback.service.telegram.state.RegistrationStateManager.RegistrationData data) {
        if (password.isEmpty()) {
            sendPersonalMessage(chatId, "❌ Пароль не может быть пустым. Введите пароль:");
            return;
        }
        
        int minPasswordLength = telegramBotProperties.getMinPasswordLength();
        if (password.length() < minPasswordLength) {
            sendPersonalMessage(chatId, "❌ <b>Пароль слишком короткий</b>\n\n" +
                    "Пароль должен содержать минимум " + minPasswordLength + " символов.\n\n" +
                    "Введите пароль еще раз:\n\n" +
                    "💡 Используйте /cancel для отмены.");
            return;
        }
        
        data.password = password;
        registrationStateManager.setState(chatId, ru.ambryo.gameplannerback.service.telegram.state.RegistrationStateManager.RegistrationState.WAITING_PASSWORD_CONFIRM);
        
        sendPersonalMessage(chatId, "✅ Пароль принят!\n\n" +
                "Подтвердите пароль (введите его еще раз):\n\n" +
                "💡 Используйте /cancel для отмены.");
    }
    
    private void handlePasswordConfirmInput(Long telegramUserId, String chatId, String passwordConfirm, ru.ambryo.gameplannerback.service.telegram.state.RegistrationStateManager.RegistrationData data) {
        if (passwordConfirm.isEmpty()) {
            sendPersonalMessage(chatId, "❌ Подтверждение пароля не может быть пустым. Введите пароль еще раз:");
            return;
        }
        
        if (!passwordConfirm.equals(data.password)) {
            sendPersonalMessage(chatId, "❌ <b>Пароли не совпадают</b>\n\n" +
                    "Введите пароль еще раз:\n\n" +
                    "💡 Используйте /cancel для отмены.");
            // Возвращаемся к вводу пароля
            registrationStateManager.setState(chatId, ru.ambryo.gameplannerback.service.telegram.state.RegistrationStateManager.RegistrationState.WAITING_PASSWORD);
            return;
        }
        
        // Все данные собраны, выполняем регистрацию
        try {
            // Проверяем блокировку перед попыткой
            if (registrationStateManager.isBlocked(chatId)) {
                long remainingSeconds = registrationStateManager.getBlockTimeRemaining(chatId);
                long remainingMinutes = remainingSeconds / 60;
                registrationStateManager.clearState(chatId);
                sendPersonalMessage(chatId, "⛔ Слишком много неудачных попыток регистрации.\n\n" +
                        "Попробуйте снова через " + remainingMinutes + " минут.");
                return;
            }
            
            // Выполняем регистрацию через AuthService
            authService.register(
                    data.username,
                    data.password,
                    data.email,
                    data.inviteCode,
                    data.name
            );
            
            // Регистрация успешна - автоматически привязываем Telegram
            User registeredUser = userRepository.findByUsername(data.username)
                    .orElseThrow(() -> new RuntimeException("User not found after registration"));
            
            registeredUser.setTelegramUserId(telegramUserId);
            registeredUser.setTelegramChatId(chatId);
            registeredUser.setTelegramSubscribed(true);
            userRepository.save(registeredUser);
            
            // Успешная регистрация
            registrationStateManager.recordAttempt(chatId, true);
            registrationStateManager.clearState(chatId);
            
            sendPersonalMessage(chatId, "🎉 <b>Регистрация успешна!</b>\n\n" +
                    "Ваш аккаунт создан и автоматически привязан к Telegram.\n\n" +
                    "Теперь вы будете получать персональные уведомления.\n\n" +
                    "Доступные команды:\n" +
                    "/games - Список предстоящих игр\n" +
                    "/help - Справка по командам\n" +
                    "/stop - Отписаться от уведомлений");
            
            logger.info("Telegram registration successful for chatId: {}, username: {}", chatId, data.username);
            
        } catch (RuntimeException e) {
            // Неудачная попытка регистрации
            registrationStateManager.recordAttempt(chatId, false);
            
            int remainingAttempts = registrationStateManager.getRemainingAttempts(chatId);
            String errorMsg = e.getMessage();
            
            if (registrationStateManager.isBlocked(chatId)) {
                long remainingSeconds = registrationStateManager.getBlockTimeRemaining(chatId);
                long remainingMinutes = remainingSeconds / 60;
                registrationStateManager.clearState(chatId);
                sendPersonalMessage(chatId, "⛔ <b>Слишком много неудачных попыток</b>\n\n" +
                        "Регистрация заблокирована на " + remainingMinutes + " минут.\n\n" +
                        "Попробуйте снова позже.");
                logger.warn("Telegram registration blocked for chatId: {} after failed attempt", chatId);
            } else {
                // Ошибка регистрации, но еще есть попытки
                if (errorMsg != null && errorMsg.contains("already exists")) {
                    if (errorMsg.contains("Username")) {
                        sendPersonalMessage(chatId, "❌ <b>Логин уже занят</b>\n\n" +
                                "Начните регистрацию заново с /register и выберите другой логин.\n\n" +
                                "Осталось попыток: " + remainingAttempts);
                    } else if (errorMsg.contains("Email")) {
                        sendPersonalMessage(chatId, "❌ <b>Email уже используется</b>\n\n" +
                                "Начните регистрацию заново с /register и используйте другой email.\n\n" +
                                "Осталось попыток: " + remainingAttempts);
                    } else {
                        sendPersonalMessage(chatId, "❌ Ошибка: " + errorMsg + "\n\n" +
                                "Осталось попыток: " + remainingAttempts + "\n\n" +
                                "Начните регистрацию заново с /register.");
                    }
                } else if (errorMsg != null && errorMsg.contains("Invite")) {
                    sendPersonalMessage(chatId, "❌ <b>Ошибка с инвайт-кодом</b>\n\n" +
                            errorMsg + "\n\n" +
                            "Начните регистрацию заново с /register.\n\n" +
                            "Осталось попыток: " + remainingAttempts);
                } else {
                    sendPersonalMessage(chatId, "❌ Ошибка регистрации: " + (errorMsg != null ? errorMsg : "Неизвестная ошибка") + "\n\n" +
                            "Осталось попыток: " + remainingAttempts + "\n\n" +
                            "Начните регистрацию заново с /register или используйте /cancel для отмены.");
                }
                logger.warn("Telegram registration failed for chatId: {}, error: {}, remaining attempts: {}", 
                        chatId, errorMsg, remainingAttempts);
            }
        }
    }
    
    // Старые методы команд удалены - теперь используется CommandRouter с CommandHandler классами
    
    private void handleTimeSlotMarkingState(Long telegramUserId, String chatId, String text, ru.ambryo.gameplannerback.service.telegram.state.TimeSlotMarkingStateManager.TimeSlotMarkingState state) {
        try {
            timeSlotMarkingStateManager.updateTimestamp(chatId);
            var data = timeSlotMarkingStateManager.getData(chatId);
            User user = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
            
            if (data == null || user == null) {
                timeSlotMarkingStateManager.clearState(chatId);
                sendPersonalMessage(chatId, "❌ Ошибка: данные разметки не найдены. Начните заново с /mark.");
                return;
            }
            
            // Получаем часовой пояс пользователя
            ZoneId userTimezone;
            try {
                userTimezone = ZoneId.of(user.getTimezone());
            } catch (Exception e) {
                timeSlotMarkingStateManager.clearState(chatId);
                sendPersonalMessage(chatId, "❌ <b>Неверный часовой пояс</b>\n\n" +
                        "Установите корректный часовой пояс в настройках профиля на веб-сайте.");
                return;
            }
            
            switch (state) {
                case WAITING_DATE:
                    handleDateInput(chatId, text.trim(), data, userTimezone);
                    break;
                case WAITING_TIME:
                    handleTimeInput(chatId, text.trim(), data, userTimezone);
                    break;
                case WAITING_DURATION:
                    handleDurationInput(telegramUserId, chatId, text.trim(), data, user, userTimezone);
                    break;
            }
        } catch (Exception e) {
            logger.error("Error handling time slot marking state", e);
            timeSlotMarkingStateManager.clearState(chatId);
            sendPersonalMessage(chatId, "❌ Произошла ошибка при обработке разметки времени. Попробуйте позже.");
        }
    }
    
    private void handleDateInput(String chatId, String dateStr, ru.ambryo.gameplannerback.service.telegram.state.TimeSlotMarkingStateManager.TimeSlotMarkingData data, ZoneId userTimezone) {
        if (dateStr.isEmpty()) {
            sendPersonalMessage(chatId, "❌ Дата не может быть пустой. Введите дату:");
            return;
        }
        
        LocalDate localDate = parseDate(dateStr, userTimezone);
        if (localDate == null) {
            sendPersonalMessage(chatId, "❌ <b>Неверный формат даты</b>\n\n" +
                    "Используйте формат ДД.ММ.ГГГГ (например: 15.01.2025)\n" +
                    "Или используйте: сегодня, завтра, послезавтра\n\n" +
                    "💡 Используйте /cancel для отмены.");
            return;
        }
        
        data.dateStr = dateStr;
        data.dateInstant = localDate.atStartOfDay(userTimezone).toInstant();
        timeSlotMarkingStateManager.setState(chatId, ru.ambryo.gameplannerback.service.telegram.state.TimeSlotMarkingStateManager.TimeSlotMarkingState.WAITING_TIME);
        
        sendPersonalMessage(chatId, "✅ Дата принята: " + formatLocalDate(localDate) + "\n\n" +
                "Введите время начала в формате ЧЧ:ММ или ЧЧ (например: 18:00 или 18):\n\n" +
                "💡 Используйте /cancel для отмены.");
    }
    
    private void handleTimeInput(String chatId, String timeStr, ru.ambryo.gameplannerback.service.telegram.state.TimeSlotMarkingStateManager.TimeSlotMarkingData data, ZoneId userTimezone) {
        if (timeStr.isEmpty()) {
            sendPersonalMessage(chatId, "❌ Время не может быть пустым. Введите время:");
            return;
        }
        
        LocalTime localTime = parseTime(timeStr);
        if (localTime == null) {
            sendPersonalMessage(chatId, "❌ <b>Неверный формат времени</b>\n\n" +
                    "Используйте формат ЧЧ:ММ (например: 18:00) или ЧЧ (например: 18)\n\n" +
                    "💡 Используйте /cancel для отмены.");
            return;
        }
        
        data.timeStr = timeStr;
        // Пока сохраняем только время, финальный Instant создадим после получения продолжительности
        timeSlotMarkingStateManager.setState(chatId, ru.ambryo.gameplannerback.service.telegram.state.TimeSlotMarkingStateManager.TimeSlotMarkingState.WAITING_DURATION);
        
        sendPersonalMessage(chatId, "✅ Время принято: " + formatLocalTime(localTime) + "\n\n" +
                "Введите продолжительность в часах (например: 1, 2, 3):\n\n" +
                "💡 Используйте /cancel для отмены.");
    }
    
    private void handleDurationInput(Long telegramUserId, String chatId, String durationStr, 
                                     ru.ambryo.gameplannerback.service.telegram.state.TimeSlotMarkingStateManager.TimeSlotMarkingData data, User user, ZoneId userTimezone) {
        if (durationStr.isEmpty()) {
            sendPersonalMessage(chatId, "❌ Продолжительность не может быть пустой. Введите количество часов:");
            return;
        }
        
        Integer duration = parseDuration(durationStr);
        if (duration == null) {
            sendPersonalMessage(chatId, "❌ <b>Неверный формат продолжительности</b>\n\n" +
                    "Введите число от 1 до 24 (количество часов)\n\n" +
                    "💡 Используйте /cancel для отмены.");
            return;
        }
        
        data.duration = duration;
        
        // Создаем финальный Instant: дата + время в часовом поясе пользователя, конвертируем в UTC
        LocalDate localDate = LocalDate.ofInstant(data.dateInstant, userTimezone);
        LocalTime localTime = parseTime(data.timeStr);
        if (localTime == null) {
            timeSlotMarkingStateManager.clearState(chatId);
            sendPersonalMessage(chatId, "❌ Ошибка: время не найдено. Начните заново с /mark.");
            return;
        }
        
        Instant startInstant = convertToUTC(localDate, localTime, userTimezone);
        data.startInstant = startInstant;
        
        // Вызываем UserService.toggleTimeSlot для создания/удаления слота
        try {
            userService.toggleTimeSlot(user, startInstant, duration);
            
            // Успешная разметка
            timeSlotMarkingStateManager.clearState(chatId);
            
            // Используем TimeSlotMessageBuilder для форматирования сообщения
            String message = timeSlotMessageBuilder.buildTimeSlotMarkedMessage(localDate, localTime, duration, userTimezone);
            sendPersonalMessage(chatId, message);
            
            logger.info("Time slot marked via Telegram for user: {}, chatId: {}, start: {}, duration: {}", 
                    user.getUsername(), chatId, startInstant, duration);
        } catch (Exception e) {
            logger.error("Error toggling time slot via Telegram", e);
            timeSlotMarkingStateManager.clearState(chatId);
            sendPersonalMessage(chatId, "❌ Ошибка при сохранении временного слота. Попробуйте позже.");
        }
    }
    
    // Старые методы команд удалены - теперь используется CommandRouter с CommandHandler классами
    
    // Методы buildMainMenuKeyboard, buildGamesMenuKeyboard, buildTimeMenuKeyboard, buildInvitesMenuKeyboard, buildSettingsMenuKeyboard, handleHelpCommand удалены - теперь используются KeyboardBuilder классы и HelpCommandHandler
    
    // Метод handleCallbackQuery больше не используется напрямую,
    // обработка callback'ов теперь происходит через MenuRouter в onUpdateReceived
    
    // Метод handleMenuCallback больше не используется напрямую,
    // обработка всех callback'ов теперь происходит через MenuRouter в onUpdateReceived
    
    private void updateMenuMessage(String chatId, Integer messageId, String message, InlineKeyboardMarkup keyboard) {
        try {
            EditMessageText editMessage = new EditMessageText();
            editMessage.setChatId(chatId);
            editMessage.setMessageId(messageId);
            editMessage.setText(message);
            editMessage.setParseMode("HTML");
            editMessage.setReplyMarkup(keyboard);
            execute(editMessage);
        } catch (TelegramApiException e) {
            logger.error("Failed to update menu message", e);
        }
    }
    
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
    
    private void handleTimezoneChangeState(Long telegramUserId, String chatId, String text, ru.ambryo.gameplannerback.service.telegram.state.TimezoneChangeStateManager.TimezoneChangeState state) {
        try {
            if (state == ru.ambryo.gameplannerback.service.telegram.state.TimezoneChangeStateManager.TimezoneChangeState.WAITING_TIMEZONE) {
                timezoneChangeStateManager.updateTimestamp(chatId);
                
                User user = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
                if (user == null) {
                    timezoneChangeStateManager.clearState(chatId);
                    sendPersonalMessage(chatId, "❌ Ваш аккаунт не связан. Используйте /link для связывания.");
                    return;
                }
                
                String timezoneInput = text.trim();
                
                // Проверяем валидность часового пояса
                ZoneId zoneId;
                try {
                    zoneId = ZoneId.of(timezoneInput);
                } catch (Exception e) {
                    sendPersonalMessage(chatId, "❌ <b>Неверный часовой пояс</b>\n\n" +
                            "Часовой пояс '" + escapeHtml(timezoneInput) + "' не найден.\n\n" +
                            "Пожалуйста, введите корректный IANA часовой пояс (например: Europe/Moscow)\n\n" +
                            "💡 Используйте /cancel для отмены.");
                    return;
                }
                
                // Обновляем часовой пояс пользователя
                userService.updateUserProfile(user, user.getName(), user.getColor(), zoneId.getId());
                
                // Очищаем состояние
                timezoneChangeStateManager.clearState(chatId);
                
                // Отправляем подтверждение
                sendPersonalMessage(chatId, "✅ <b>Часовой пояс успешно изменен!</b>\n\n" +
                        "Новый часовой пояс: <b>" + escapeHtml(zoneId.getId()) + "</b>\n\n" +
                        "Теперь вы можете использовать разметку времени через /mark");
                
                logger.info("Timezone changed via Telegram (manual) for user: {}, new timezone: {}", user.getUsername(), zoneId.getId());
            }
        } catch (Exception e) {
            logger.error("Error handling timezone change state", e);
            timezoneChangeStateManager.clearState(chatId);
            sendPersonalMessage(chatId, "❌ Произошла ошибка при смене часового пояса. Попробуйте позже.");
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
    
    // Старые методы обработки callback'ов игр удалены - теперь используется GameActionHandler и GamesMenuHandler в MenuRouter
    // Метод buildGameKeyboardWithBack удален - теперь используется GamesMenuKeyboardBuilder.buildGameKeyboardWithBack
    // Метод buildUpcomingGamesListMessage удален - теперь используется GameMessageBuilder.buildUpcomingGamesListMessage
    
    // Методы buildUpcomingGamesListMessage, buildGameDetailsMessage, buildGameKeyboard удалены - теперь используются GameMessageBuilder и GamesMenuKeyboardBuilder
    // Методы buildInviteCreatedMessage и buildMyInvitesListMessage удалены - теперь используется InviteMessageBuilder
    
    private String formatLocalDate(LocalDate date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        return date.format(formatter);
    }
    
    private String formatLocalTime(LocalTime time) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return time.format(formatter);
    }
    
    // Метод formatInstantInTimezone удален - теперь используется TimeSlotMessageBuilder
    
    // Методы buildTimeSlotMarkedMessage и buildMySlotsListMessage удалены - теперь используется TimeSlotMessageBuilder
    
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
            String message = gameMessageBuilder.buildGameNotificationMessage(game);
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
    
    // Метод buildGameNotificationMessage удален - теперь используется GameMessageBuilder.buildGameNotificationMessage
    
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
            String message = gameMessageBuilder.buildGameCancelledMessage(game, cancellationReason);
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
            String message = gameMessageBuilder.buildGameHeldMessage(game);
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
    
    // Методы buildGameCancelledMessage и buildGameHeldMessage удалены - теперь используется GameMessageBuilder
    
    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }

    // Метод sanitizeHtmlForTelegram удален - теперь используется TelegramHtmlFormatter
    
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
                String message = gameMessageBuilder.buildGameDetailsMessage(game, user);
                InlineKeyboardMarkup keyboard = gamesMenuKeyboardBuilder.buildGameKeyboard(game, user);
                
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
            String message = gameMessageBuilder.buildGameCancelledMessage(game, null);
            sendPersonalMessage(user.getTelegramChatId(), message);
        }
    }
    
    public void sendGameHeldPersonalNotification(GameDto game, User user) {
        if (user.getTelegramSubscribed() && user.getTelegramChatId() != null) {
            String message = gameMessageBuilder.buildGameHeldMessage(game);
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
    
    // Методы для работы с настройками уведомлений
    
    private void handleMenuNotifications(Long telegramUserId, String chatId, Integer messageId) {
        try {
            User user = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
            
            if (user == null) {
                answerCallbackQuery("", "❌ Ваш аккаунт не связан. Используйте /link для связывания.");
                return;
            }
            
            // Получаем текущие настройки
            UserNotificationSettingsDto settings = notificationSettingsService.getSettings(user.getId());
            
            String message = buildNotificationSettingsMessage(settings);
            InlineKeyboardMarkup keyboard = buildNotificationsMenuKeyboard(settings);
            
            updateMenuMessage(chatId, messageId, message, keyboard);
        } catch (Exception e) {
            logger.error("Error handling menu notifications", e);
            answerCallbackQuery("", "❌ Ошибка при получении настроек уведомлений.");
        }
    }
    
    private String buildNotificationSettingsMessage(UserNotificationSettingsDto settings) {
        StringBuilder message = new StringBuilder();
        message.append("🔔 <b>Настройки уведомлений</b>\n\n");
        message.append("<b>Текущие настройки:</b>\n");
        
        // Игра создана
        String gameCreatedText = switch (settings.getGameCreated()) {
            case "ALL" -> "Все игры";
            case "MY_GAMES" -> "Только мои игры";
            case "NONE" -> "Не получать";
            default -> settings.getGameCreated();
        };
        message.append("• Игра создана: ").append(gameCreatedText).append("\n");
        
        // Игра отменена
        String gameCancelledText = switch (settings.getGameCancelled()) {
            case "ALL" -> "Все игры";
            case "MY_GAMES" -> "Только мои игры";
            case "NONE" -> "Не получать";
            default -> settings.getGameCancelled();
        };
        message.append("• Игра отменена: ").append(gameCancelledText).append("\n");
        
        // Игра проведена
        String gameHeldText = switch (settings.getGameHeld()) {
            case "ALL" -> "Все игры";
            case "MY_GAMES" -> "Только мои игры";
            case "NONE" -> "Не получать";
            default -> settings.getGameHeld();
        };
        message.append("• Игра проведена: ").append(gameHeldText).append("\n");
        
        // Исключили из игры
        String removedText = "ALL".equals(settings.getGameRemovedFromGame()) ? "Получать" : "Не получать";
        message.append("• Исключили из игры: ").append(removedText).append("\n");
        
        // Напоминания о предстоящих играх
        List<UpcomingGameReminderDto> reminders = settings.getUpcomingGameReminders();
        if (reminders == null) {
            reminders = new java.util.ArrayList<>();
        }
        long activeReminders = reminders.stream()
                .filter(r -> r.getEnabled() != null && r.getEnabled())
                .count();
        message.append("• Напоминания о предстоящих играх: ").append(activeReminders).append(" активных\n");
        
        // Напоминание разметить время
        if (settings.getTimeSlotReminderEnabled() != null && settings.getTimeSlotReminderEnabled()) {
            String cronText = formatCronToReadable(settings.getTimeSlotReminderCron());
            message.append("• Напоминание разметить время: Включено");
            if (cronText != null && !cronText.isEmpty()) {
                message.append(" (").append(cronText).append(")");
            }
            message.append("\n");
        } else {
            message.append("• Напоминание разметить время: Выключено\n");
        }
        
        // Напоминание завершить игру
        String completionText = (settings.getGameCompletionReminderEnabled() != null && settings.getGameCompletionReminderEnabled()) 
                ? "Включено" : "Выключено";
        message.append("• Напоминание завершить игру: ").append(completionText).append("\n");
        
        message.append("\nВыберите настройку для изменения:");
        
        return message.toString();
    }
    
    private InlineKeyboardMarkup buildNotificationsMenuKeyboard(UserNotificationSettingsDto settings) {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new java.util.ArrayList<>();
        
        // Игра создана
        List<InlineKeyboardButton> gameCreatedRow = new java.util.ArrayList<>();
        InlineKeyboardButton gameCreatedButton = new InlineKeyboardButton();
        String gameCreatedText = switch (settings.getGameCreated()) {
            case "ALL" -> "✓ Все игры";
            case "MY_GAMES" -> "✓ Только мои";
            case "NONE" -> "✓ Не получать";
            default -> "Игра создана";
        };
        gameCreatedButton.setText("🎮 " + gameCreatedText);
        gameCreatedButton.setCallbackData("notification_set_gameCreated");
        gameCreatedRow.add(gameCreatedButton);
        rows.add(gameCreatedRow);
        
        // Игра отменена
        List<InlineKeyboardButton> gameCancelledRow = new java.util.ArrayList<>();
        InlineKeyboardButton gameCancelledButton = new InlineKeyboardButton();
        String gameCancelledText = switch (settings.getGameCancelled()) {
            case "ALL" -> "✓ Все игры";
            case "MY_GAMES" -> "✓ Только мои";
            case "NONE" -> "✓ Не получать";
            default -> "Игра отменена";
        };
        gameCancelledButton.setText("❌ " + gameCancelledText);
        gameCancelledButton.setCallbackData("notification_set_gameCancelled");
        gameCancelledRow.add(gameCancelledButton);
        rows.add(gameCancelledRow);
        
        // Игра проведена
        List<InlineKeyboardButton> gameHeldRow = new java.util.ArrayList<>();
        InlineKeyboardButton gameHeldButton = new InlineKeyboardButton();
        String gameHeldText = switch (settings.getGameHeld()) {
            case "ALL" -> "✓ Все игры";
            case "MY_GAMES" -> "✓ Только мои";
            case "NONE" -> "✓ Не получать";
            default -> "Игра проведена";
        };
        gameHeldButton.setText("✅ " + gameHeldText);
        gameHeldButton.setCallbackData("notification_set_gameHeld");
        gameHeldRow.add(gameHeldButton);
        rows.add(gameHeldRow);
        
        // Исключили из игры
        List<InlineKeyboardButton> removedRow = new java.util.ArrayList<>();
        InlineKeyboardButton removedButton = new InlineKeyboardButton();
        String removedText = "ALL".equals(settings.getGameRemovedFromGame()) ? "✓ Получать" : "✓ Не получать";
        removedButton.setText("🚫 " + removedText);
        removedButton.setCallbackData("notification_set_gameRemovedFromGame");
        removedRow.add(removedButton);
        rows.add(removedRow);
        
        // Напоминания о предстоящих играх
        List<InlineKeyboardButton> remindersRow = new java.util.ArrayList<>();
        InlineKeyboardButton remindersButton = new InlineKeyboardButton();
        List<UpcomingGameReminderDto> reminders = settings.getUpcomingGameReminders();
        if (reminders == null) {
            reminders = new java.util.ArrayList<>();
        }
        long activeCount = reminders.stream()
                .filter(r -> r.getEnabled() != null && r.getEnabled())
                .count();
        remindersButton.setText("⏰ Напоминания (" + activeCount + "/" + reminders.size() + ")");
        remindersButton.setCallbackData("notification_reminders");
        remindersRow.add(remindersButton);
        rows.add(remindersRow);
        
        // Напоминание разметить время
        List<InlineKeyboardButton> timeSlotRow = new java.util.ArrayList<>();
        InlineKeyboardButton timeSlotButton = new InlineKeyboardButton();
        String timeSlotText = (settings.getTimeSlotReminderEnabled() != null && settings.getTimeSlotReminderEnabled()) 
                ? "✓ Включено" : "Выключено";
        timeSlotButton.setText("📅 " + timeSlotText);
        timeSlotButton.setCallbackData("notification_timeslot_reminder");
        timeSlotRow.add(timeSlotButton);
        rows.add(timeSlotRow);
        
        // Напоминание завершить игру
        List<InlineKeyboardButton> completionRow = new java.util.ArrayList<>();
        InlineKeyboardButton completionButton = new InlineKeyboardButton();
        String completionText = (settings.getGameCompletionReminderEnabled() != null && settings.getGameCompletionReminderEnabled()) 
                ? "✓ Включено" : "Выключено";
        completionButton.setText("📝 " + completionText);
        completionButton.setCallbackData("notification_set_gameCompletionReminder");
        completionRow.add(completionButton);
        rows.add(completionRow);
        
        // Кнопка "Назад"
        List<InlineKeyboardButton> backRow = new java.util.ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("◀️ Назад");
        backButton.setCallbackData("menu_settings");
        backRow.add(backButton);
        rows.add(backRow);
        
        keyboard.setKeyboard(rows);
        return keyboard;
    }
    
    private void handleNotificationSettingChange(Long telegramUserId, String chatId, Integer messageId, String callbackData) {
        try {
            User user = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
            
            if (user == null) {
                answerCallbackQuery("", "❌ Ваш аккаунт не связан. Используйте /link для связывания.");
                return;
            }
            
            // Получаем текущие настройки
            UserNotificationSettingsDto settings = notificationSettingsService.getSettings(user.getId());
            
            // Определяем, какая настройка изменяется
            if (callbackData.equals("notification_set_gameCreated")) {
                // Показываем меню выбора для "Игра создана"
                showGameCreatedMenu(chatId, messageId, settings);
            } else if (callbackData.equals("notification_set_gameCancelled")) {
                // Показываем меню выбора для "Игра отменена"
                showGameCancelledMenu(chatId, messageId, settings);
            } else if (callbackData.equals("notification_set_gameHeld")) {
                // Показываем меню выбора для "Игра проведена"
                showGameHeldMenu(chatId, messageId, settings);
            } else if (callbackData.equals("notification_set_gameRemovedFromGame")) {
                // Переключаем "Исключили из игры"
                String newValue = "ALL".equals(settings.getGameRemovedFromGame()) ? "NONE" : "ALL";
                settings.setGameRemovedFromGame(newValue);
                notificationSettingsService.updateSettings(user.getId(), settings);
                answerCallbackQuery("", "✅ Настройка изменена!");
                handleMenuNotifications(telegramUserId, chatId, messageId);
            } else if (callbackData.equals("notification_set_gameCompletionReminder")) {
                // Переключаем "Напоминание завершить игру"
                boolean newValue = !(settings.getGameCompletionReminderEnabled() != null && settings.getGameCompletionReminderEnabled());
                settings.setGameCompletionReminderEnabled(newValue);
                notificationSettingsService.updateSettings(user.getId(), settings);
                answerCallbackQuery("", "✅ Настройка изменена!");
                handleMenuNotifications(telegramUserId, chatId, messageId);
            } else if (callbackData.startsWith("notification_set_gameCreated_")) {
                // Установка значения для "Игра создана"
                String value = callbackData.substring("notification_set_gameCreated_".length());
                settings.setGameCreated(value);
                notificationSettingsService.updateSettings(user.getId(), settings);
                answerCallbackQuery("", "✅ Настройка изменена!");
                handleMenuNotifications(telegramUserId, chatId, messageId);
            } else if (callbackData.startsWith("notification_set_gameCancelled_")) {
                // Установка значения для "Игра отменена"
                String value = callbackData.substring("notification_set_gameCancelled_".length());
                settings.setGameCancelled(value);
                notificationSettingsService.updateSettings(user.getId(), settings);
                answerCallbackQuery("", "✅ Настройка изменена!");
                handleMenuNotifications(telegramUserId, chatId, messageId);
            } else if (callbackData.startsWith("notification_set_gameHeld_")) {
                // Установка значения для "Игра проведена"
                String value = callbackData.substring("notification_set_gameHeld_".length());
                settings.setGameHeld(value);
                notificationSettingsService.updateSettings(user.getId(), settings);
                answerCallbackQuery("", "✅ Настройка изменена!");
                handleMenuNotifications(telegramUserId, chatId, messageId);
            }
        } catch (Exception e) {
            logger.error("Error handling notification setting change", e);
            answerCallbackQuery("", "❌ Ошибка при изменении настройки");
        }
    }
    
    private void showGameCreatedMenu(String chatId, Integer messageId, UserNotificationSettingsDto settings) {
        String message = "🎮 <b>Игра создана</b>\n\n" +
                "Выберите, когда получать уведомления:";
        
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new java.util.ArrayList<>();
        
        // Все игры
        List<InlineKeyboardButton> allRow = new java.util.ArrayList<>();
        InlineKeyboardButton allButton = new InlineKeyboardButton();
        allButton.setText("ALL".equals(settings.getGameCreated()) ? "✓ Все игры" : "Все игры");
        allButton.setCallbackData("notification_set_gameCreated_ALL");
        allRow.add(allButton);
        rows.add(allRow);
        
        // Только мои игры
        List<InlineKeyboardButton> myRow = new java.util.ArrayList<>();
        InlineKeyboardButton myButton = new InlineKeyboardButton();
        myButton.setText("MY_GAMES".equals(settings.getGameCreated()) ? "✓ Только мои игры" : "Только мои игры");
        myButton.setCallbackData("notification_set_gameCreated_MY_GAMES");
        myRow.add(myButton);
        rows.add(myRow);
        
        // Не получать
        List<InlineKeyboardButton> noneRow = new java.util.ArrayList<>();
        InlineKeyboardButton noneButton = new InlineKeyboardButton();
        noneButton.setText("NONE".equals(settings.getGameCreated()) ? "✓ Не получать" : "Не получать");
        noneButton.setCallbackData("notification_set_gameCreated_NONE");
        noneRow.add(noneButton);
        rows.add(noneRow);
        
        // Назад
        List<InlineKeyboardButton> backRow = new java.util.ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("◀️ Назад");
        backButton.setCallbackData("menu_settings_notifications");
        backRow.add(backButton);
        rows.add(backRow);
        
        keyboard.setKeyboard(rows);
        updateMenuMessage(chatId, messageId, message, keyboard);
    }
    
    private void showGameCancelledMenu(String chatId, Integer messageId, UserNotificationSettingsDto settings) {
        String message = "❌ <b>Игра отменена</b>\n\n" +
                "Выберите, когда получать уведомления:";
        
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new java.util.ArrayList<>();
        
        // Все игры
        List<InlineKeyboardButton> allRow = new java.util.ArrayList<>();
        InlineKeyboardButton allButton = new InlineKeyboardButton();
        allButton.setText("ALL".equals(settings.getGameCancelled()) ? "✓ Все игры" : "Все игры");
        allButton.setCallbackData("notification_set_gameCancelled_ALL");
        allRow.add(allButton);
        rows.add(allRow);
        
        // Только мои игры
        List<InlineKeyboardButton> myRow = new java.util.ArrayList<>();
        InlineKeyboardButton myButton = new InlineKeyboardButton();
        myButton.setText("MY_GAMES".equals(settings.getGameCancelled()) ? "✓ Только мои игры" : "Только мои игры");
        myButton.setCallbackData("notification_set_gameCancelled_MY_GAMES");
        myRow.add(myButton);
        rows.add(myRow);
        
        // Не получать
        List<InlineKeyboardButton> noneRow = new java.util.ArrayList<>();
        InlineKeyboardButton noneButton = new InlineKeyboardButton();
        noneButton.setText("NONE".equals(settings.getGameCancelled()) ? "✓ Не получать" : "Не получать");
        noneButton.setCallbackData("notification_set_gameCancelled_NONE");
        noneRow.add(noneButton);
        rows.add(noneRow);
        
        // Назад
        List<InlineKeyboardButton> backRow = new java.util.ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("◀️ Назад");
        backButton.setCallbackData("menu_settings_notifications");
        backRow.add(backButton);
        rows.add(backRow);
        
        keyboard.setKeyboard(rows);
        updateMenuMessage(chatId, messageId, message, keyboard);
    }
    
    private void showGameHeldMenu(String chatId, Integer messageId, UserNotificationSettingsDto settings) {
        String message = "✅ <b>Игра проведена</b>\n\n" +
                "Выберите, когда получать уведомления:";
        
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new java.util.ArrayList<>();
        
        // Все игры
        List<InlineKeyboardButton> allRow = new java.util.ArrayList<>();
        InlineKeyboardButton allButton = new InlineKeyboardButton();
        allButton.setText("ALL".equals(settings.getGameHeld()) ? "✓ Все игры" : "Все игры");
        allButton.setCallbackData("notification_set_gameHeld_ALL");
        allRow.add(allButton);
        rows.add(allRow);
        
        // Только мои игры
        List<InlineKeyboardButton> myRow = new java.util.ArrayList<>();
        InlineKeyboardButton myButton = new InlineKeyboardButton();
        myButton.setText("MY_GAMES".equals(settings.getGameHeld()) ? "✓ Только мои игры" : "Только мои игры");
        myButton.setCallbackData("notification_set_gameHeld_MY_GAMES");
        myRow.add(myButton);
        rows.add(myRow);
        
        // Не получать
        List<InlineKeyboardButton> noneRow = new java.util.ArrayList<>();
        InlineKeyboardButton noneButton = new InlineKeyboardButton();
        noneButton.setText("NONE".equals(settings.getGameHeld()) ? "✓ Не получать" : "Не получать");
        noneButton.setCallbackData("notification_set_gameHeld_NONE");
        noneRow.add(noneButton);
        rows.add(noneRow);
        
        // Назад
        List<InlineKeyboardButton> backRow = new java.util.ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("◀️ Назад");
        backButton.setCallbackData("menu_settings_notifications");
        backRow.add(backButton);
        rows.add(backRow);
        
        keyboard.setKeyboard(rows);
        updateMenuMessage(chatId, messageId, message, keyboard);
    }
    
    private void handleMenuReminders(Long telegramUserId, String chatId, Integer messageId) {
        try {
            User user = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
            
            if (user == null) {
                answerCallbackQuery("", "❌ Ваш аккаунт не связан. Используйте /link для связывания.");
                return;
            }
            
            UserNotificationSettingsDto settings = notificationSettingsService.getSettings(user.getId());
            List<UpcomingGameReminderDto> reminders = settings.getUpcomingGameReminders();
            if (reminders == null) {
                reminders = new java.util.ArrayList<>();
            }
            
            String message = buildRemindersListMessage(reminders);
            InlineKeyboardMarkup keyboard = buildRemindersMenuKeyboard(reminders);
            
            updateMenuMessage(chatId, messageId, message, keyboard);
        } catch (Exception e) {
            logger.error("Error handling menu reminders", e);
            answerCallbackQuery("", "❌ Ошибка при получении списка напоминаний.");
        }
    }
    
    private String buildRemindersListMessage(List<UpcomingGameReminderDto> reminders) {
        StringBuilder message = new StringBuilder();
        message.append("⏰ <b>Напоминания о предстоящих играх</b>\n\n");
        
        if (reminders.isEmpty()) {
            message.append("У вас пока нет настроенных напоминаний.\n\n");
            message.append("Нажмите кнопку ниже, чтобы добавить напоминание.");
        } else {
            message.append("Всего: ").append(reminders.size()).append(" (максимум 5)\n\n");
            
            for (int i = 0; i < reminders.size(); i++) {
                UpcomingGameReminderDto reminder = reminders.get(i);
                String displayValue = formatReminderValue(reminder.getMinutesBefore());
                String status = (reminder.getEnabled() != null && reminder.getEnabled()) ? "✅" : "❌";
                
                message.append("<b>").append(i + 1).append(".</b> ").append(status).append(" ");
                message.append(displayValue).append("\n");
            }
        }
        
        return message.toString();
    }
    
    private InlineKeyboardMarkup buildRemindersMenuKeyboard(List<UpcomingGameReminderDto> reminders) {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new java.util.ArrayList<>();
        
        // Кнопки для каждого напоминания
        for (int i = 0; i < reminders.size(); i++) {
            UpcomingGameReminderDto reminder = reminders.get(i);
            String displayValue = formatReminderValue(reminder.getMinutesBefore());
            String status = (reminder.getEnabled() != null && reminder.getEnabled()) ? "✅" : "❌";
            
            List<InlineKeyboardButton> reminderRow = new java.util.ArrayList<>();
            
            // Кнопка редактирования
            InlineKeyboardButton editButton = new InlineKeyboardButton();
            editButton.setText("✏️ " + (i + 1) + ". " + status + " " + displayValue);
            editButton.setCallbackData("notification_reminder_edit_" + i);
            reminderRow.add(editButton);
            rows.add(reminderRow);
            
            // Кнопки управления
            List<InlineKeyboardButton> controlRow = new java.util.ArrayList<>();
            
            // Включить/выключить
            InlineKeyboardButton toggleButton = new InlineKeyboardButton();
            toggleButton.setText((reminder.getEnabled() != null && reminder.getEnabled()) ? "❌ Выкл" : "✅ Вкл");
            toggleButton.setCallbackData("notification_reminder_toggle_" + i);
            controlRow.add(toggleButton);
            
            // Удалить
            InlineKeyboardButton deleteButton = new InlineKeyboardButton();
            deleteButton.setText("🗑️ Удалить");
            deleteButton.setCallbackData("notification_reminder_delete_" + i);
            controlRow.add(deleteButton);
            
            rows.add(controlRow);
        }
        
        // Кнопка добавления (если меньше 5)
        if (reminders.size() < 5) {
            List<InlineKeyboardButton> addRow = new java.util.ArrayList<>();
            InlineKeyboardButton addButton = new InlineKeyboardButton();
            addButton.setText("➕ Добавить напоминание");
            addButton.setCallbackData("notification_reminder_add");
            addRow.add(addButton);
            rows.add(addRow);
        }
        
        // Назад
        List<InlineKeyboardButton> backRow = new java.util.ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("◀️ Назад");
        backButton.setCallbackData("menu_settings_notifications");
        backRow.add(backButton);
        rows.add(backRow);
        
        keyboard.setKeyboard(rows);
        return keyboard;
    }
    
    private String formatReminderValue(Integer minutesBefore) {
        if (minutesBefore == null) {
            return "0 минут";
        }
        
        if (minutesBefore % (24 * 60) == 0 && minutesBefore >= 24 * 60) {
            int days = minutesBefore / (24 * 60);
            return days + " " + (days == 1 ? "день" : (days < 5 ? "дня" : "дней"));
        } else if (minutesBefore % 60 == 0 && minutesBefore >= 60) {
            int hours = minutesBefore / 60;
            return hours + " " + (hours == 1 ? "час" : (hours < 5 ? "часа" : "часов"));
        } else {
            return minutesBefore + " " + (minutesBefore == 1 ? "минута" : (minutesBefore < 5 ? "минуты" : "минут"));
        }
    }
    
    private void handleReminderAdd(Long telegramUserId, String chatId, Integer messageId) {
        try {
            User user = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
            
            if (user == null) {
                answerCallbackQuery("", "❌ Ваш аккаунт не связан. Используйте /link для связывания.");
                return;
            }
            
            UserNotificationSettingsDto settings = notificationSettingsService.getSettings(user.getId());
            List<UpcomingGameReminderDto> reminders = settings.getUpcomingGameReminders();
            if (reminders == null) {
                reminders = new java.util.ArrayList<>();
            }
            
            if (reminders.size() >= 5) {
                answerCallbackQuery("", "❌ Максимум 5 напоминаний");
                return;
            }
            
            // Инициализируем состояние добавления напоминания
            notificationStateManager.setState(chatId, ru.ambryo.gameplannerback.service.telegram.state.NotificationStateManager.NotificationState.WAITING_REMINDER_VALUE);
            var data = new ru.ambryo.gameplannerback.service.telegram.state.NotificationStateManager.NotificationData();
            data.reminderIndex = -1; // -1 означает новое напоминание
            notificationStateManager.setData(chatId, data);
            
            String message = "⏰ <b>Добавление напоминания</b>\n\n" +
                    "Введите значение (например: 60 для 60 минут, 2 для 2 часов, 1 для 1 дня):\n\n" +
                    "💡 Используйте /cancel для отмены.";
            
            sendPersonalMessage(chatId, message);
            
            // Обновляем меню
            String menuMessage = buildRemindersListMessage(reminders);
            InlineKeyboardMarkup keyboard = buildRemindersMenuKeyboard(reminders);
            updateMenuMessage(chatId, messageId, menuMessage, keyboard);
        } catch (Exception e) {
            logger.error("Error handling reminder add", e);
            answerCallbackQuery("", "❌ Ошибка при добавлении напоминания");
        }
    }
    
    private void handleReminderEdit(Long telegramUserId, String chatId, Integer messageId, int index) {
        try {
            User user = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
            
            if (user == null) {
                answerCallbackQuery("", "❌ Ваш аккаунт не связан. Используйте /link для связывания.");
                return;
            }
            
            UserNotificationSettingsDto settings = notificationSettingsService.getSettings(user.getId());
            List<UpcomingGameReminderDto> reminders = settings.getUpcomingGameReminders();
            if (reminders == null || index < 0 || index >= reminders.size()) {
                answerCallbackQuery("", "❌ Напоминание не найдено");
                return;
            }
            
            // Инициализируем состояние редактирования напоминания
            notificationStateManager.setState(chatId, ru.ambryo.gameplannerback.service.telegram.state.NotificationStateManager.NotificationState.WAITING_REMINDER_VALUE);
            var data = new ru.ambryo.gameplannerback.service.telegram.state.NotificationStateManager.NotificationData();
            data.reminderIndex = index;
            UpcomingGameReminderDto reminder = reminders.get(index);
            data.reminderValue = reminder.getMinutesBefore();
            notificationStateManager.setData(chatId, data);
            
            String currentValue = formatReminderValue(reminder.getMinutesBefore());
            String message = "✏️ <b>Редактирование напоминания</b>\n\n" +
                    "Текущее значение: <b>" + escapeHtml(currentValue) + "</b>\n\n" +
                    "Введите новое значение (например: 60 для 60 минут, 2 для 2 часов, 1 для 1 дня):\n\n" +
                    "💡 Используйте /cancel для отмены.";
            
            sendPersonalMessage(chatId, message);
            
            // Обновляем меню
            String menuMessage = buildRemindersListMessage(reminders);
            InlineKeyboardMarkup keyboard = buildRemindersMenuKeyboard(reminders);
            updateMenuMessage(chatId, messageId, menuMessage, keyboard);
        } catch (Exception e) {
            logger.error("Error handling reminder edit", e);
            answerCallbackQuery("", "❌ Ошибка при редактировании напоминания");
        }
    }
    
    private void handleReminderDelete(Long telegramUserId, String chatId, Integer messageId, int index) {
        try {
            User user = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
            
            if (user == null) {
                answerCallbackQuery("", "❌ Ваш аккаунт не связан. Используйте /link для связывания.");
                return;
            }
            
            UserNotificationSettingsDto settings = notificationSettingsService.getSettings(user.getId());
            List<UpcomingGameReminderDto> reminders = settings.getUpcomingGameReminders();
            if (reminders == null || index < 0 || index >= reminders.size()) {
                answerCallbackQuery("", "❌ Напоминание не найдено");
                return;
            }
            
            reminders.remove(index);
            settings.setUpcomingGameReminders(reminders);
            notificationSettingsService.updateSettings(user.getId(), settings);
            
            answerCallbackQuery("", "✅ Напоминание удалено!");
            handleMenuReminders(telegramUserId, chatId, messageId);
        } catch (Exception e) {
            logger.error("Error handling reminder delete", e);
            answerCallbackQuery("", "❌ Ошибка при удалении напоминания");
        }
    }
    
    private void handleReminderToggle(Long telegramUserId, String chatId, Integer messageId, int index) {
        try {
            User user = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
            
            if (user == null) {
                answerCallbackQuery("", "❌ Ваш аккаунт не связан. Используйте /link для связывания.");
                return;
            }
            
            UserNotificationSettingsDto settings = notificationSettingsService.getSettings(user.getId());
            List<UpcomingGameReminderDto> reminders = settings.getUpcomingGameReminders();
            if (reminders == null || index < 0 || index >= reminders.size()) {
                answerCallbackQuery("", "❌ Напоминание не найдено");
                return;
            }
            
            UpcomingGameReminderDto reminder = reminders.get(index);
            boolean newValue = !(reminder.getEnabled() != null && reminder.getEnabled());
            reminder.setEnabled(newValue);
            settings.setUpcomingGameReminders(reminders);
            notificationSettingsService.updateSettings(user.getId(), settings);
            
            answerCallbackQuery("", "✅ Напоминание " + (newValue ? "включено" : "выключено") + "!");
            handleMenuReminders(telegramUserId, chatId, messageId);
        } catch (Exception e) {
            logger.error("Error handling reminder toggle", e);
            answerCallbackQuery("", "❌ Ошибка при изменении напоминания");
        }
    }
    
    private void handleReminderUnitSelect(Long telegramUserId, String chatId, Integer messageId, String unit) {
        try {
            User user = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
            
            if (user == null) {
                answerCallbackQuery("", "❌ Ваш аккаунт не связан. Используйте /link для связывания.");
                return;
            }
            
            var data = notificationStateManager.getData(chatId);
            if (data == null) {
                notificationStateManager.clearState(chatId);
                sendPersonalMessage(chatId, "❌ Ошибка: данные не найдены. Начните заново.");
                return;
            }
            
            data.reminderUnit = unit;
            notificationStateManager.setData(chatId, data);
            
            // Переходим к вопросу о включении/выключении
            notificationStateManager.setState(chatId, ru.ambryo.gameplannerback.service.telegram.state.NotificationStateManager.NotificationState.WAITING_REMINDER_UNIT); // Используем это состояние для финального подтверждения
            
            String valueText = data.reminderValue != null ? String.valueOf(data.reminderValue) : "0";
            String unitText = switch (unit) {
                case "minutes" -> "минут";
                case "hours" -> "часов";
                case "days" -> "дней";
                default -> unit;
            };
            
            String message = "✅ Значение принято!\n\n" +
                    "Напоминание: <b>" + valueText + " " + unitText + "</b>\n\n" +
                    "Включить это напоминание? (да/нет):\n\n" +
                    "💡 Используйте /cancel для отмены.";
            
            sendPersonalMessage(chatId, message);
        } catch (Exception e) {
            logger.error("Error handling reminder unit select", e);
            answerCallbackQuery("", "❌ Ошибка при выборе единицы");
        }
    }
    
    private void handleMenuTimeSlotReminder(Long telegramUserId, String chatId, Integer messageId) {
        try {
            User user = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
            
            if (user == null) {
                answerCallbackQuery("", "❌ Ваш аккаунт не связан. Используйте /link для связывания.");
                return;
            }
            
            UserNotificationSettingsDto settings = notificationSettingsService.getSettings(user.getId());
            
            String message = "📅 <b>Напоминание разметить время</b>\n\n";
            
            if (settings.getTimeSlotReminderEnabled() != null && settings.getTimeSlotReminderEnabled()) {
                String cronText = formatCronToReadable(settings.getTimeSlotReminderCron());
                message += "Статус: <b>Включено</b>\n";
                if (cronText != null && !cronText.isEmpty()) {
                    message += "Расписание: <b>" + escapeHtml(cronText) + "</b>\n";
                }
            } else {
                message += "Статус: <b>Выключено</b>\n";
            }
            
            message += "\nВыберите действие:";
            
            InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new java.util.ArrayList<>();
            
            // Включить/выключить
            List<InlineKeyboardButton> toggleRow = new java.util.ArrayList<>();
            InlineKeyboardButton toggleButton = new InlineKeyboardButton();
            toggleButton.setText((settings.getTimeSlotReminderEnabled() != null && settings.getTimeSlotReminderEnabled()) 
                    ? "❌ Выключить" : "✅ Включить");
            toggleButton.setCallbackData("notification_timeslot_reminder_toggle");
            toggleRow.add(toggleButton);
            rows.add(toggleRow);
            
            // Настроить расписание (только если включено)
            if (settings.getTimeSlotReminderEnabled() != null && settings.getTimeSlotReminderEnabled()) {
                List<InlineKeyboardButton> cronRow = new java.util.ArrayList<>();
                InlineKeyboardButton cronButton = new InlineKeyboardButton();
                cronButton.setText("⚙️ Настроить расписание");
                cronButton.setCallbackData("notification_timeslot_reminder_cron");
                cronRow.add(cronButton);
                rows.add(cronRow);
            }
            
            // Назад
            List<InlineKeyboardButton> backRow = new java.util.ArrayList<>();
            InlineKeyboardButton backButton = new InlineKeyboardButton();
            backButton.setText("◀️ Назад");
            backButton.setCallbackData("menu_settings_notifications");
            backRow.add(backButton);
            rows.add(backRow);
            
            keyboard.setKeyboard(rows);
            updateMenuMessage(chatId, messageId, message, keyboard);
        } catch (Exception e) {
            logger.error("Error handling menu time slot reminder", e);
            answerCallbackQuery("", "❌ Ошибка при получении настроек");
        }
    }
    
    private void handleTimeSlotReminderToggle(Long telegramUserId, String chatId, Integer messageId) {
        try {
            User user = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
            
            if (user == null) {
                answerCallbackQuery("", "❌ Ваш аккаунт не связан. Используйте /link для связывания.");
                return;
            }
            
            UserNotificationSettingsDto settings = notificationSettingsService.getSettings(user.getId());
            boolean newValue = !(settings.getTimeSlotReminderEnabled() != null && settings.getTimeSlotReminderEnabled());
            settings.setTimeSlotReminderEnabled(newValue);
            
            // Если выключаем, очищаем cron
            if (!newValue) {
                settings.setTimeSlotReminderCron(null);
            } else if (settings.getTimeSlotReminderCron() == null || settings.getTimeSlotReminderCron().trim().isEmpty()) {
                // Если включаем и cron не установлен, устанавливаем по умолчанию
                settings.setTimeSlotReminderCron("0 0 9 * * *"); // Ежедневно в 9:00
            }
            
            notificationSettingsService.updateSettings(user.getId(), settings);
            
            answerCallbackQuery("", "✅ Настройка изменена!");
            handleMenuTimeSlotReminder(telegramUserId, chatId, messageId);
        } catch (Exception e) {
            logger.error("Error handling time slot reminder toggle", e);
            answerCallbackQuery("", "❌ Ошибка при изменении настройки");
        }
    }
    
    private void handleTimeSlotReminderCron(Long telegramUserId, String chatId, Integer messageId) {
        try {
            User user = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
            
            if (user == null) {
                answerCallbackQuery("", "❌ Ваш аккаунт не связан. Используйте /link для связывания.");
                return;
            }
            
            UserNotificationSettingsDto settings = notificationSettingsService.getSettings(user.getId());
            
            // Инициализируем состояние настройки cron
            notificationStateManager.setState(chatId, ru.ambryo.gameplannerback.service.telegram.state.NotificationStateManager.NotificationState.WAITING_CRON_FREQUENCY);
            var data = new ru.ambryo.gameplannerback.service.telegram.state.NotificationStateManager.NotificationData();
            if (settings.getTimeSlotReminderCron() != null && !settings.getTimeSlotReminderCron().trim().isEmpty()) {
                // Парсим существующий cron
                parseCronToData(settings.getTimeSlotReminderCron(), data);
            }
            notificationStateManager.setData(chatId, data);
            
            String message = "⚙️ <b>Настройка расписания</b>\n\n" +
                    "Выберите частоту напоминания:";
            
            InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new java.util.ArrayList<>();
            
            // Ежедневно
            List<InlineKeyboardButton> dailyRow = new java.util.ArrayList<>();
            InlineKeyboardButton dailyButton = new InlineKeyboardButton();
            dailyButton.setText("📅 Ежедневно");
            dailyButton.setCallbackData("notification_cron_frequency_daily");
            dailyRow.add(dailyButton);
            rows.add(dailyRow);
            
            // Еженедельно
            List<InlineKeyboardButton> weeklyRow = new java.util.ArrayList<>();
            InlineKeyboardButton weeklyButton = new InlineKeyboardButton();
            weeklyButton.setText("📆 Еженедельно");
            weeklyButton.setCallbackData("notification_cron_frequency_weekly");
            weeklyRow.add(weeklyButton);
            rows.add(weeklyRow);
            
            // Ежемесячно
            List<InlineKeyboardButton> monthlyRow = new java.util.ArrayList<>();
            InlineKeyboardButton monthlyButton = new InlineKeyboardButton();
            monthlyButton.setText("🗓️ Ежемесячно");
            monthlyButton.setCallbackData("notification_cron_frequency_monthly");
            monthlyRow.add(monthlyButton);
            rows.add(monthlyRow);
            
            // Назад
            List<InlineKeyboardButton> backRow = new java.util.ArrayList<>();
            InlineKeyboardButton backButton = new InlineKeyboardButton();
            backButton.setText("◀️ Назад");
            backButton.setCallbackData("notification_timeslot_reminder");
            backRow.add(backButton);
            rows.add(backRow);
            
            keyboard.setKeyboard(rows);
            updateMenuMessage(chatId, messageId, message, keyboard);
        } catch (Exception e) {
            logger.error("Error handling time slot reminder cron", e);
            answerCallbackQuery("", "❌ Ошибка при настройке расписания");
        }
    }
    
    private void handleCronFrequencySelect(Long telegramUserId, String chatId, Integer messageId, String frequency) {
        try {
            User user = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
            
            if (user == null) {
                answerCallbackQuery("", "❌ Ваш аккаунт не связан. Используйте /link для связывания.");
                return;
            }
            
            var data = notificationStateManager.getData(chatId);
            if (data == null) {
                notificationStateManager.clearState(chatId);
                sendPersonalMessage(chatId, "❌ Ошибка: данные не найдены. Начните заново.");
                return;
            }
            
            data.cronFrequency = frequency;
            notificationStateManager.setData(chatId, data);
            
            if ("daily".equals(frequency)) {
                // Для ежедневного - сразу переходим к времени
                notificationStateManager.setState(chatId, ru.ambryo.gameplannerback.service.telegram.state.NotificationStateManager.NotificationState.WAITING_CRON_TIME);
                
                String message = "✅ Частота выбрана: <b>Ежедневно</b>\n\n" +
                        "Введите время в формате ЧЧ:ММ (например: 09:00):\n\n" +
                        "💡 Используйте /cancel для отмены.";
                
                sendPersonalMessage(chatId, message);
            } else if ("weekly".equals(frequency)) {
                // Для еженедельного - выбираем день недели
                notificationStateManager.setState(chatId, ru.ambryo.gameplannerback.service.telegram.state.NotificationStateManager.NotificationState.WAITING_CRON_DAY);
                
                String message = "✅ Частота выбрана: <b>Еженедельно</b>\n\n" +
                        "Выберите день недели:";
                
                InlineKeyboardMarkup keyboard = buildDayOfWeekKeyboard();
                updateMenuMessage(chatId, messageId, message, keyboard);
            } else if ("monthly".equals(frequency)) {
                // Для ежемесячного - вводим день месяца
                notificationStateManager.setState(chatId, ru.ambryo.gameplannerback.service.telegram.state.NotificationStateManager.NotificationState.WAITING_CRON_DAY);
                
                String message = "✅ Частота выбрана: <b>Ежемесячно</b>\n\n" +
                        "Введите день месяца (1-31):\n\n" +
                        "💡 Используйте /cancel для отмены.";
                
                sendPersonalMessage(chatId, message);
            }
        } catch (Exception e) {
            logger.error("Error handling cron frequency select", e);
            answerCallbackQuery("", "❌ Ошибка при выборе частоты");
        }
    }
    
    private InlineKeyboardMarkup buildDayOfWeekKeyboard() {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new java.util.ArrayList<>();
        
        String[] days = {"Воскресенье", "Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота"};
        
        // Первая строка: Пн, Вт, Ср
        List<InlineKeyboardButton> row1 = new java.util.ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(days[i]);
            button.setCallbackData("notification_cron_day_" + i);
            row1.add(button);
        }
        rows.add(row1);
        
        // Вторая строка: Чт, Пт, Сб
        List<InlineKeyboardButton> row2 = new java.util.ArrayList<>();
        for (int i = 4; i <= 6; i++) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(days[i]);
            button.setCallbackData("notification_cron_day_" + i);
            row2.add(button);
        }
        rows.add(row2);
        
        // Третья строка: Вс
        List<InlineKeyboardButton> row3 = new java.util.ArrayList<>();
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(days[0]);
        button.setCallbackData("notification_cron_day_0");
        row3.add(button);
        rows.add(row3);
        
        // Назад
        List<InlineKeyboardButton> backRow = new java.util.ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("◀️ Назад");
        backButton.setCallbackData("notification_timeslot_reminder_cron");
        backRow.add(backButton);
        rows.add(backRow);
        
        keyboard.setKeyboard(rows);
        return keyboard;
    }
    
    private void handleCronDaySelect(Long telegramUserId, String chatId, Integer messageId, int day) {
        try {
            User user = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
            
            if (user == null) {
                answerCallbackQuery("", "❌ Ваш аккаунт не связан. Используйте /link для связывания.");
                return;
            }
            
            var data = notificationStateManager.getData(chatId);
            if (data == null) {
                notificationStateManager.clearState(chatId);
                sendPersonalMessage(chatId, "❌ Ошибка: данные не найдены. Начните заново.");
                return;
            }
            
            data.cronDay = day;
            notificationStateManager.setData(chatId, data);
            notificationStateManager.setState(chatId, ru.ambryo.gameplannerback.service.telegram.state.NotificationStateManager.NotificationState.WAITING_CRON_TIME);
            
            String dayText = switch (day) {
                case 0 -> "воскресенье";
                case 1 -> "понедельник";
                case 2 -> "вторник";
                case 3 -> "среду";
                case 4 -> "четверг";
                case 5 -> "пятницу";
                case 6 -> "субботу";
                default -> "день " + day;
            };
            
            String message = "✅ День выбран: <b>" + dayText + "</b>\n\n" +
                    "Введите время в формате ЧЧ:ММ (например: 09:00):\n\n" +
                    "💡 Используйте /cancel для отмены.";
            
            sendPersonalMessage(chatId, message);
        } catch (Exception e) {
            logger.error("Error handling cron day select", e);
            answerCallbackQuery("", "❌ Ошибка при выборе дня");
        }
    }
    
    private void handleNotificationState(Long telegramUserId, String chatId, String text, ru.ambryo.gameplannerback.service.telegram.state.NotificationStateManager.NotificationState state) {
        try {
            notificationStateManager.updateTimestamp(chatId);
            
            User user = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
            if (user == null) {
                notificationStateManager.clearState(chatId);
                sendPersonalMessage(chatId, "❌ Ваш аккаунт не связан. Используйте /link для связывания.");
                return;
            }
            
            var data = notificationStateManager.getData(chatId);
            if (data == null) {
                notificationStateManager.clearState(chatId);
                sendPersonalMessage(chatId, "❌ Ошибка: данные не найдены. Начните заново.");
                return;
            }
            
            if (state == ru.ambryo.gameplannerback.service.telegram.state.NotificationStateManager.NotificationState.WAITING_REMINDER_VALUE) {
                // Ожидание значения напоминания
                try {
                    int value = Integer.parseInt(text.trim());
                    if (value <= 0) {
                        sendPersonalMessage(chatId, "❌ Значение должно быть положительным числом. Введите значение:");
                        return;
                    }
                    
                    data.reminderValue = value;
                    notificationStateManager.setData(chatId, data);
                    
                    // Переходим к выбору единицы
                    notificationStateManager.setState(chatId, ru.ambryo.gameplannerback.service.telegram.state.NotificationStateManager.NotificationState.WAITING_REMINDER_UNIT);
                    
                    String message = "✅ Значение принято: <b>" + value + "</b>\n\n" +
                            "Выберите единицу измерения:";
                    
                    InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
                    List<List<InlineKeyboardButton>> rows = new java.util.ArrayList<>();
                    
                    // Минуты
                    List<InlineKeyboardButton> minutesRow = new java.util.ArrayList<>();
                    InlineKeyboardButton minutesButton = new InlineKeyboardButton();
                    minutesButton.setText("⏱️ Минуты");
                    minutesButton.setCallbackData("notification_reminder_unit_minutes");
                    minutesRow.add(minutesButton);
                    rows.add(minutesRow);
                    
                    // Часы
                    List<InlineKeyboardButton> hoursRow = new java.util.ArrayList<>();
                    InlineKeyboardButton hoursButton = new InlineKeyboardButton();
                    hoursButton.setText("🕐 Часы");
                    hoursButton.setCallbackData("notification_reminder_unit_hours");
                    hoursRow.add(hoursButton);
                    rows.add(hoursRow);
                    
                    // Дни
                    List<InlineKeyboardButton> daysRow = new java.util.ArrayList<>();
                    InlineKeyboardButton daysButton = new InlineKeyboardButton();
                    daysButton.setText("📅 Дни");
                    daysButton.setCallbackData("notification_reminder_unit_days");
                    daysRow.add(daysButton);
                    rows.add(daysRow);
                    
                    keyboard.setKeyboard(rows);
                    sendPersonalMessage(chatId, message);
                    // Отправляем клавиатуру отдельным сообщением
                    SendMessage sendMessage = new SendMessage();
                    sendMessage.setChatId(chatId);
                    sendMessage.setText("Выберите единицу:");
                    sendMessage.setReplyMarkup(keyboard);
                    execute(sendMessage);
                } catch (NumberFormatException e) {
                    sendPersonalMessage(chatId, "❌ Неверный формат. Введите положительное число:");
                    return;
                }
                
            } else if (state == ru.ambryo.gameplannerback.service.telegram.state.NotificationStateManager.NotificationState.WAITING_REMINDER_UNIT) {
                // Это состояние используется для финального подтверждения (включить/выключить)
                // после выбора единицы через callback
                String lowerText = text.trim().toLowerCase();
                boolean enabled = lowerText.equals("да") || lowerText.equals("yes") || lowerText.equals("включить") || lowerText.equals("on");
                
                // Сохраняем напоминание
                UserNotificationSettingsDto settings = notificationSettingsService.getSettings(user.getId());
                List<UpcomingGameReminderDto> reminders = settings.getUpcomingGameReminders();
                if (reminders == null) {
                    reminders = new java.util.ArrayList<>();
                }
                
                // Вычисляем minutesBefore на основе значения и единицы
                int minutesBefore = convertToMinutes(data.reminderValue, data.reminderUnit);
                
                if (data.reminderIndex == -1) {
                    // Новое напоминание
                    if (reminders.size() >= 5) {
                        notificationStateManager.clearState(chatId);
                        sendPersonalMessage(chatId, "❌ Максимум 5 напоминаний. Удалите одно из существующих.");
                        return;
                    }
                    reminders.add(new UpcomingGameReminderDto(minutesBefore, enabled));
                } else {
                    // Редактирование существующего
                    if (data.reminderIndex >= 0 && data.reminderIndex < reminders.size()) {
                        UpcomingGameReminderDto reminder = reminders.get(data.reminderIndex);
                        reminder.setMinutesBefore(minutesBefore);
                        reminder.setEnabled(enabled);
                    } else {
                        notificationStateManager.clearState(chatId);
                        sendPersonalMessage(chatId, "❌ Напоминание не найдено.");
                        return;
                    }
                }
                
                settings.setUpcomingGameReminders(reminders);
                notificationSettingsService.updateSettings(user.getId(), settings);
                
                notificationStateManager.clearState(chatId);
                
                String displayValue = formatReminderValue(minutesBefore);
                sendPersonalMessage(chatId, "✅ <b>Напоминание " + (data.reminderIndex == -1 ? "добавлено" : "изменено") + "!</b>\n\n" +
                        "Значение: <b>" + escapeHtml(displayValue) + "</b>\n" +
                        "Статус: <b>" + (enabled ? "Включено" : "Выключено") + "</b>");
                
                logger.info("Reminder {} via Telegram for user: {}, value: {} minutes, enabled: {}", 
                        data.reminderIndex == -1 ? "added" : "updated", user.getUsername(), minutesBefore, enabled);
                
            } else if (state == ru.ambryo.gameplannerback.service.telegram.state.NotificationStateManager.NotificationState.WAITING_CRON_TIME) {
                // Ожидание времени для cron
                LocalTime time = parseTime(text.trim());
                if (time == null) {
                    sendPersonalMessage(chatId, "❌ <b>Неверный формат времени</b>\n\n" +
                            "Введите время в формате ЧЧ:ММ (например: 09:00):\n\n" +
                            "💡 Используйте /cancel для отмены.");
                    return;
                }
                
                data.cronTime = String.format("%02d:%02d", time.getHour(), time.getMinute());
                notificationStateManager.setData(chatId, data);
                
                // Сохраняем cron
                String cron = buildCronFromData(data);
                UserNotificationSettingsDto settings = notificationSettingsService.getSettings(user.getId());
                settings.setTimeSlotReminderCron(cron);
                notificationSettingsService.updateSettings(user.getId(), settings);
                
                notificationStateManager.clearState(chatId);
                
                String cronText = formatCronToReadable(cron);
                sendPersonalMessage(chatId, "✅ <b>Расписание настроено!</b>\n\n" +
                        "Расписание: <b>" + escapeHtml(cronText) + "</b>");
                
                logger.info("Time slot reminder cron updated via Telegram for user: {}, cron: {}", user.getUsername(), cron);
                
            } else if (state == ru.ambryo.gameplannerback.service.telegram.state.NotificationStateManager.NotificationState.WAITING_CRON_DAY && data.cronFrequency != null && "monthly".equals(data.cronFrequency)) {
                // Ожидание дня месяца для ежемесячного расписания
                try {
                    int dayOfMonth = Integer.parseInt(text.trim());
                    if (dayOfMonth < 1 || dayOfMonth > 31) {
                        sendPersonalMessage(chatId, "❌ <b>Неверный день месяца</b>\n\n" +
                                "Введите число от 1 до 31:\n\n" +
                                "💡 Используйте /cancel для отмены.");
                        return;
                    }
                    
                    data.cronDay = dayOfMonth;
                    notificationStateManager.setData(chatId, data);
                    notificationStateManager.setState(chatId, ru.ambryo.gameplannerback.service.telegram.state.NotificationStateManager.NotificationState.WAITING_CRON_TIME);
                    notificationStateManager.updateTimestamp(chatId);
                    
                    sendPersonalMessage(chatId, "✅ День месяца принят: <b>" + dayOfMonth + "</b>\n\n" +
                            "Введите время в формате ЧЧ:ММ (например: 09:00):\n\n" +
                            "💡 Используйте /cancel для отмены.");
                } catch (NumberFormatException e) {
                    sendPersonalMessage(chatId, "❌ <b>Неверный формат</b>\n\n" +
                            "Введите число от 1 до 31:\n\n" +
                            "💡 Используйте /cancel для отмены.");
                }
            }
        } catch (Exception e) {
            logger.error("Error handling notification state", e);
            notificationStateManager.clearState(chatId);
            sendPersonalMessage(chatId, "❌ Произошла ошибка. Попробуйте позже.");
        }
    }
    
    private int convertToMinutes(Integer value, String unit) {
        if (value == null) {
            return 0;
        }
        return switch (unit) {
            case "days" -> value * 24 * 60;
            case "hours" -> value * 60;
            case "minutes" -> value;
            default -> value;
        };
    }
    
    private void parseCronToData(String cron, ru.ambryo.gameplannerback.service.telegram.state.NotificationStateManager.NotificationData data) {
        if (cron == null || cron.trim().isEmpty()) {
            return;
        }
        
        try {
            String[] parts = cron.trim().split("\\s+");
            if (parts.length >= 6) {
                int minute = Integer.parseInt(parts[1]);
                int hour = Integer.parseInt(parts[2]);
                String dayOfMonth = parts[3];
                String dayOfWeek = parts[5];
                
                data.cronTime = String.format("%02d:%02d", hour, minute);
                
                if (!"*".equals(dayOfWeek)) {
                    data.cronFrequency = "weekly";
                    data.cronDay = Integer.parseInt(dayOfWeek);
                } else if (!"*".equals(dayOfMonth)) {
                    data.cronFrequency = "monthly";
                    data.cronDay = Integer.parseInt(dayOfMonth);
                } else {
                    data.cronFrequency = "daily";
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to parse cron: {}", cron, e);
        }
    }
    
    private String buildCronFromData(ru.ambryo.gameplannerback.service.telegram.state.NotificationStateManager.NotificationData data) {
        if (data.cronFrequency == null || data.cronTime == null) {
            return "0 0 9 * * *"; // По умолчанию ежедневно в 9:00
        }
        
        String[] timeParts = data.cronTime.split(":");
        int hour = Integer.parseInt(timeParts[0]);
        int minute = Integer.parseInt(timeParts[1]);
        
        // Spring cron формат: секунды минуты часы день_месяца месяц день_недели
        if ("weekly".equals(data.cronFrequency)) {
            int dayOfWeek = data.cronDay != null ? data.cronDay : 1;
            return String.format("0 %d %d * * %d", minute, hour, dayOfWeek);
        } else if ("monthly".equals(data.cronFrequency)) {
            int dayOfMonth = data.cronDay != null ? data.cronDay : 1;
            return String.format("0 %d %d %d * *", minute, hour, dayOfMonth);
        } else {
            // daily
            return String.format("0 %d %d * * *", minute, hour);
        }
    }
    
    private String formatCronToReadable(String cron) {
        if (cron == null || cron.trim().isEmpty()) {
            return "не настроено";
        }
        
        try {
            String[] parts = cron.trim().split("\\s+");
            if (parts.length < 6) {
                return cron;
            }
            
            int minute = Integer.parseInt(parts[1]);
            int hour = Integer.parseInt(parts[2]);
            String dayOfMonth = parts[3];
            String dayOfWeek = parts[5];
            
            String timeStr = String.format("%02d:%02d", hour, minute);
            
            if (!"*".equals(dayOfWeek)) {
                int day = Integer.parseInt(dayOfWeek);
                String[] days = {"воскресенье", "понедельник", "вторник", "среду", "четверг", "пятницу", "субботу"};
                if (day >= 0 && day < days.length) {
                    return "каждый " + days[day] + " в " + timeStr;
                }
            } else if (!"*".equals(dayOfMonth)) {
                int day = Integer.parseInt(dayOfMonth);
                return "каждое " + day + " число в " + timeStr;
            } else {
                return "ежедневно в " + timeStr;
            }
        } catch (Exception e) {
            logger.warn("Failed to format cron: {}", cron, e);
        }
        
        return cron;
    }
}
