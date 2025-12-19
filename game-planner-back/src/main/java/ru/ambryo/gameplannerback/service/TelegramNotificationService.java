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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
    
    @Autowired
    private AuthService authService;
    
    @Autowired
    private InviteService inviteService;
    
    @Autowired
    private UserService userService;
    
    // Система состояний для авторизации через логин/пароль
    private enum AuthState {
        WAITING_USERNAME,
        WAITING_PASSWORD
    }
    
    // Хранение состояний авторизации: chatId -> AuthState
    private final Map<String, AuthState> authStates = new ConcurrentHashMap<>();
    
    // Хранение временных данных: chatId -> username
    private final Map<String, String> authUsernames = new ConcurrentHashMap<>();
    
    // Хранение времени последнего действия: chatId -> Instant
    private final Map<String, Instant> authTimestamps = new ConcurrentHashMap<>();
    
    // Защита от брутфорса: chatId -> AttemptInfo
    private static class AttemptInfo {
        int attempts;
        Instant firstAttempt;
        Instant blockedUntil;
        
        AttemptInfo() {
            this.attempts = 0;
            this.firstAttempt = Instant.now();
            this.blockedUntil = null;
        }
    }
    
    private final Map<String, AttemptInfo> authAttempts = new ConcurrentHashMap<>();
    
    // Константы для защиты от брутфорса
    private static final int MAX_AUTH_ATTEMPTS = 3;
    private static final long AUTH_ATTEMPT_WINDOW_SECONDS = 900; // 15 минут
    private static final long AUTH_BLOCK_DURATION_SECONDS = 900; // 15 минут блокировки
    private static final long AUTH_STATE_TIMEOUT_SECONDS = 300; // 5 минут таймаут состояния
    
    // Система состояний для регистрации через Telegram
    private enum RegistrationState {
        WAITING_INVITE,
        WAITING_USERNAME,
        WAITING_NAME,
        WAITING_EMAIL,
        WAITING_PASSWORD,
        WAITING_PASSWORD_CONFIRM
    }
    
    // Класс для хранения данных регистрации
    private static class RegistrationData {
        String inviteCode;
        String username;
        String name;
        String email;
        String password;
    }
    
    // Хранение состояний регистрации: chatId -> RegistrationState
    private final Map<String, RegistrationState> registrationStates = new ConcurrentHashMap<>();
    
    // Хранение данных регистрации: chatId -> RegistrationData
    private final Map<String, RegistrationData> registrationData = new ConcurrentHashMap<>();
    
    // Хранение времени последнего действия регистрации: chatId -> Instant
    private final Map<String, Instant> registrationTimestamps = new ConcurrentHashMap<>();
    
    // Защита от спама регистрации: chatId -> AttemptInfo
    private final Map<String, AttemptInfo> registrationAttempts = new ConcurrentHashMap<>();
    
    // Константы для регистрации
    private static final int MAX_REGISTRATION_ATTEMPTS = 3;
    private static final long REGISTRATION_ATTEMPT_WINDOW_SECONDS = 3600; // 1 час
    private static final long REGISTRATION_STATE_TIMEOUT_SECONDS = 600; // 10 минут таймаут состояния
    private static final int MIN_PASSWORD_LENGTH = 6;
    
    // Система состояний для разметки времени через Telegram
    private enum TimeSlotMarkingState {
        WAITING_DATE,
        WAITING_TIME,
        WAITING_DURATION
    }
    
    // Класс для хранения данных разметки времени
    private static class TimeSlotMarkingData {
        String dateStr;  // Введенная дата как строка
        String timeStr;  // Введенное время как строка
        Instant dateInstant;  // Парсированная дата (начало дня в UTC)
        Instant startInstant;  // Финальный Instant для слота (в UTC)
        Integer duration;  // Продолжительность в часах
    }
    
    // Хранение состояний разметки времени: chatId -> TimeSlotMarkingState
    private final Map<String, TimeSlotMarkingState> timeSlotMarkingStates = new ConcurrentHashMap<>();
    
    // Хранение данных разметки времени: chatId -> TimeSlotMarkingData
    private final Map<String, TimeSlotMarkingData> timeSlotMarkingData = new ConcurrentHashMap<>();
    
    // Хранение времени последнего действия разметки: chatId -> Instant
    private final Map<String, Instant> timeSlotMarkingTimestamps = new ConcurrentHashMap<>();
    
    // Константы для разметки времени
    private static final long TIME_SLOT_MARKING_STATE_TIMEOUT_SECONDS = 300; // 5 минут таймаут состояния
    
    // Система состояний для смены часового пояса через Telegram
    private enum TimezoneChangeState {
        WAITING_TIMEZONE
    }
    
    // Хранение состояний смены часового пояса: chatId -> TimezoneChangeState
    private final Map<String, TimezoneChangeState> timezoneChangeStates = new ConcurrentHashMap<>();
    
    // Хранение времени последнего действия смены часового пояса: chatId -> Instant
    private final Map<String, Instant> timezoneChangeTimestamps = new ConcurrentHashMap<>();
    
    // Константы для смены часового пояса
    private static final long TIMEZONE_CHANGE_STATE_TIMEOUT_SECONDS = 300; // 5 минут таймаут состояния
    
    // Система состояний для настроек уведомлений через Telegram
    private enum NotificationState {
        WAITING_REMINDER_VALUE,      // Ожидание значения напоминания
        WAITING_REMINDER_UNIT,       // Ожидание единицы (минуты/часы/дни)
        WAITING_CRON_FREQUENCY,      // Ожидание частоты cron (daily/weekly/monthly)
        WAITING_CRON_DAY,            // Ожидание дня для weekly/monthly
        WAITING_CRON_TIME            // Ожидание времени для cron
    }
    
    // Класс для хранения данных настроек уведомлений
    private static class NotificationData {
        Integer reminderIndex;        // Индекс редактируемого напоминания
        Integer reminderValue;        // Временное значение
        String reminderUnit;          // Временная единица (minutes/hours/days)
        String cronFrequency;        // Частота cron (daily/weekly/monthly)
        Integer cronDay;              // День для cron
        String cronTime;              // Время для cron (HH:mm)
    }
    
    // Хранение состояний настроек уведомлений: chatId -> NotificationState
    private final Map<String, NotificationState> notificationStates = new ConcurrentHashMap<>();
    
    // Хранение данных настроек уведомлений: chatId -> NotificationData
    private final Map<String, NotificationData> notificationData = new ConcurrentHashMap<>();
    
    // Хранение времени последнего действия настроек уведомлений: chatId -> Instant
    private final Map<String, Instant> notificationTimestamps = new ConcurrentHashMap<>();
    
    // Константы для настроек уведомлений
    private static final long NOTIFICATION_STATE_TIMEOUT_SECONDS = 300; // 5 минут таймаут состояния
    
    // Хранение текущей страницы списка игр: chatId -> page (0-based)
    private final Map<String, Integer> gamesListPage = new ConcurrentHashMap<>();
    
    // Константы для списка игр
    private static final int GAMES_PER_PAGE = 5;
    
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
    
    // Вспомогательные методы для работы с состояниями авторизации
    
    private void clearAuthState(String chatId) {
        authStates.remove(chatId);
        authUsernames.remove(chatId);
        authTimestamps.remove(chatId);
    }
    
    private boolean isAuthStateExpired(String chatId) {
        Instant timestamp = authTimestamps.get(chatId);
        if (timestamp == null) {
            return true;
        }
        return Instant.now().isAfter(timestamp.plusSeconds(AUTH_STATE_TIMEOUT_SECONDS));
    }
    
    private void updateAuthTimestamp(String chatId) {
        authTimestamps.put(chatId, Instant.now());
    }
    
    private boolean isBlocked(String chatId) {
        AttemptInfo info = authAttempts.get(chatId);
        if (info == null) {
            return false;
        }
        
        if (info.blockedUntil != null && Instant.now().isBefore(info.blockedUntil)) {
            return true;
        }
        
        // Если блокировка истекла, сбрасываем попытки
        if (info.blockedUntil != null && Instant.now().isAfter(info.blockedUntil)) {
            authAttempts.remove(chatId);
            return false;
        }
        
        // Проверяем, не истекло ли окно попыток
        if (Instant.now().isAfter(info.firstAttempt.plusSeconds(AUTH_ATTEMPT_WINDOW_SECONDS))) {
            // Окно истекло, сбрасываем попытки
            authAttempts.remove(chatId);
            return false;
        }
        
        return false;
    }
    
    private void recordAuthAttempt(String chatId, boolean success) {
        if (success) {
            // Успешная авторизация - сбрасываем попытки
            authAttempts.remove(chatId);
            return;
        }
        
        AttemptInfo info = authAttempts.computeIfAbsent(chatId, k -> new AttemptInfo());
        info.attempts++;
        
        if (info.attempts >= MAX_AUTH_ATTEMPTS) {
            // Блокируем на 15 минут
            info.blockedUntil = Instant.now().plusSeconds(AUTH_BLOCK_DURATION_SECONDS);
            logger.warn("Telegram auth blocked for chatId {} after {} failed attempts", chatId, info.attempts);
        }
    }
    
    private int getRemainingAttempts(String chatId) {
        AttemptInfo info = authAttempts.get(chatId);
        if (info == null) {
            return MAX_AUTH_ATTEMPTS;
        }
        
        if (Instant.now().isAfter(info.firstAttempt.plusSeconds(AUTH_ATTEMPT_WINDOW_SECONDS))) {
            return MAX_AUTH_ATTEMPTS;
        }
        
        return Math.max(0, MAX_AUTH_ATTEMPTS - info.attempts);
    }
    
    private long getBlockTimeRemaining(String chatId) {
        AttemptInfo info = authAttempts.get(chatId);
        if (info == null || info.blockedUntil == null) {
            return 0;
        }
        
        if (Instant.now().isAfter(info.blockedUntil)) {
            return 0;
        }
        
        return java.time.Duration.between(Instant.now(), info.blockedUntil).getSeconds();
    }
    
    // Вспомогательные методы для работы с состояниями регистрации
    
    private void clearRegistrationState(String chatId) {
        registrationStates.remove(chatId);
        registrationData.remove(chatId);
        registrationTimestamps.remove(chatId);
    }
    
    private boolean isRegistrationStateExpired(String chatId) {
        Instant timestamp = registrationTimestamps.get(chatId);
        if (timestamp == null) {
            return true;
        }
        return Instant.now().isAfter(timestamp.plusSeconds(REGISTRATION_STATE_TIMEOUT_SECONDS));
    }
    
    private void updateRegistrationTimestamp(String chatId) {
        registrationTimestamps.put(chatId, Instant.now());
    }
    
    private boolean isRegistrationBlocked(String chatId) {
        AttemptInfo info = registrationAttempts.get(chatId);
        if (info == null) {
            return false;
        }
        
        if (info.blockedUntil != null && Instant.now().isBefore(info.blockedUntil)) {
            return true;
        }
        
        // Если блокировка истекла, сбрасываем попытки
        if (info.blockedUntil != null && Instant.now().isAfter(info.blockedUntil)) {
            registrationAttempts.remove(chatId);
            return false;
        }
        
        // Проверяем, не истекло ли окно попыток
        if (Instant.now().isAfter(info.firstAttempt.plusSeconds(REGISTRATION_ATTEMPT_WINDOW_SECONDS))) {
            // Окно истекло, сбрасываем попытки
            registrationAttempts.remove(chatId);
            return false;
        }
        
        return false;
    }
    
    private void recordRegistrationAttempt(String chatId, boolean success) {
        if (success) {
            // Успешная регистрация - сбрасываем попытки
            registrationAttempts.remove(chatId);
            return;
        }
        
        AttemptInfo info = registrationAttempts.computeIfAbsent(chatId, k -> new AttemptInfo());
        info.attempts++;
        
        if (info.attempts >= MAX_REGISTRATION_ATTEMPTS) {
            // Блокируем на время окна попыток
            info.blockedUntil = Instant.now().plusSeconds(REGISTRATION_ATTEMPT_WINDOW_SECONDS);
            logger.warn("Telegram registration blocked for chatId {} after {} failed attempts", chatId, info.attempts);
        }
    }
    
    private int getRemainingRegistrationAttempts(String chatId) {
        AttemptInfo info = registrationAttempts.get(chatId);
        if (info == null) {
            return MAX_REGISTRATION_ATTEMPTS;
        }
        
        if (Instant.now().isAfter(info.firstAttempt.plusSeconds(REGISTRATION_ATTEMPT_WINDOW_SECONDS))) {
            return MAX_REGISTRATION_ATTEMPTS;
        }
        
        return Math.max(0, MAX_REGISTRATION_ATTEMPTS - info.attempts);
    }
    
    private long getRegistrationBlockTimeRemaining(String chatId) {
        AttemptInfo info = registrationAttempts.get(chatId);
        if (info == null || info.blockedUntil == null) {
            return 0;
        }
        
        if (Instant.now().isAfter(info.blockedUntil)) {
            return 0;
        }
        
        return java.time.Duration.between(Instant.now(), info.blockedUntil).getSeconds();
    }
    
    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        // Простая проверка формата email
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }
    
    // Вспомогательные методы для работы с состояниями разметки времени
    
    private void clearTimeSlotMarkingState(String chatId) {
        timeSlotMarkingStates.remove(chatId);
        timeSlotMarkingData.remove(chatId);
        timeSlotMarkingTimestamps.remove(chatId);
    }
    
    private boolean isTimeSlotMarkingStateExpired(String chatId) {
        Instant timestamp = timeSlotMarkingTimestamps.get(chatId);
        if (timestamp == null) {
            return true;
        }
        return Instant.now().isAfter(timestamp.plusSeconds(TIME_SLOT_MARKING_STATE_TIMEOUT_SECONDS));
    }
    
    private void updateTimeSlotMarkingTimestamp(String chatId) {
        timeSlotMarkingTimestamps.put(chatId, Instant.now());
    }
    
    // Вспомогательные методы для работы с состояниями смены часового пояса
    
    private void clearTimezoneChangeState(String chatId) {
        timezoneChangeStates.remove(chatId);
        timezoneChangeTimestamps.remove(chatId);
    }
    
    private boolean isTimezoneChangeStateExpired(String chatId) {
        Instant timestamp = timezoneChangeTimestamps.get(chatId);
        if (timestamp == null) {
            return true;
        }
        return Instant.now().isAfter(timestamp.plusSeconds(TIMEZONE_CHANGE_STATE_TIMEOUT_SECONDS));
    }
    
    private void updateTimezoneChangeTimestamp(String chatId) {
        timezoneChangeTimestamps.put(chatId, Instant.now());
    }
    
    // Вспомогательные методы для работы с состояниями настроек уведомлений
    
    private void clearNotificationState(String chatId) {
        notificationStates.remove(chatId);
        notificationData.remove(chatId);
        notificationTimestamps.remove(chatId);
    }
    
    private boolean isNotificationStateExpired(String chatId) {
        Instant timestamp = notificationTimestamps.get(chatId);
        if (timestamp == null) {
            return true;
        }
        return Instant.now().isAfter(timestamp.plusSeconds(NOTIFICATION_STATE_TIMEOUT_SECONDS));
    }
    
    private void updateNotificationTimestamp(String chatId) {
        notificationTimestamps.put(chatId, Instant.now());
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
            handleCallbackQuery(update.getCallbackQuery());
            return;
        }
        
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message message = update.getMessage();
            String text = message.getText();
            String chatIdStr = message.getChatId().toString();
            Long telegramUserId = message.getFrom().getId();
            
            // Проверяем, не истекло ли состояние авторизации
            if (authStates.containsKey(chatIdStr) && isAuthStateExpired(chatIdStr)) {
                clearAuthState(chatIdStr);
                sendPersonalMessage(chatIdStr, "⏱️ Время ожидания истекло. Процесс авторизации отменен.\n\nИспользуйте /auth для начала заново.");
            }
            
            // Проверяем, не истекло ли состояние регистрации
            if (registrationStates.containsKey(chatIdStr) && isRegistrationStateExpired(chatIdStr)) {
                clearRegistrationState(chatIdStr);
                sendPersonalMessage(chatIdStr, "⏱️ Время ожидания истекло. Процесс регистрации отменен.\n\nИспользуйте /register для начала заново.");
            }
            
            // Проверяем, не истекло ли состояние разметки времени
            if (timeSlotMarkingStates.containsKey(chatIdStr) && isTimeSlotMarkingStateExpired(chatIdStr)) {
                clearTimeSlotMarkingState(chatIdStr);
                sendPersonalMessage(chatIdStr, "⏱️ Время ожидания истекло. Процесс разметки времени отменен.\n\nИспользуйте /mark для начала заново.");
            }
            
            // Проверяем, не истекло ли состояние смены часового пояса
            if (timezoneChangeStates.containsKey(chatIdStr) && isTimezoneChangeStateExpired(chatIdStr)) {
                clearTimezoneChangeState(chatIdStr);
                sendPersonalMessage(chatIdStr, "⏱️ Время ожидания истекло. Процесс смены часового пояса отменен.\n\nИспользуйте меню для начала заново.");
            }
            
            // Проверяем, не истекло ли состояние настроек уведомлений
            if (notificationStates.containsKey(chatIdStr) && isNotificationStateExpired(chatIdStr)) {
                clearNotificationState(chatIdStr);
                sendPersonalMessage(chatIdStr, "⏱️ Время ожидания истекло. Процесс настройки уведомлений отменен.\n\nИспользуйте меню для начала заново.");
            }
            
            // Обработка команд (команды имеют приоритет над состояниями)
            if (text.startsWith("/start")) {
                clearAuthState(chatIdStr);
                clearRegistrationState(chatIdStr);
                clearTimeSlotMarkingState(chatIdStr);
                clearTimezoneChangeState(chatIdStr);
                clearNotificationState(chatIdStr);
                handleStartCommand(telegramUserId, chatIdStr);
            } else if (text.startsWith("/stop")) {
                clearAuthState(chatIdStr);
                clearRegistrationState(chatIdStr);
                clearTimeSlotMarkingState(chatIdStr);
                clearTimezoneChangeState(chatIdStr);
                clearNotificationState(chatIdStr);
                handleStopCommand(telegramUserId, chatIdStr);
            } else if (text.startsWith("/register")) {
                clearAuthState(chatIdStr);
                clearRegistrationState(chatIdStr);
                clearTimeSlotMarkingState(chatIdStr);
                clearTimezoneChangeState(chatIdStr);
                clearNotificationState(chatIdStr);
                handleRegisterCommand(telegramUserId, chatIdStr);
            } else if (text.startsWith("/auth")) {
                clearAuthState(chatIdStr);
                clearRegistrationState(chatIdStr);
                clearTimeSlotMarkingState(chatIdStr);
                clearTimezoneChangeState(chatIdStr);
                clearNotificationState(chatIdStr);
                handleAuthCommand(telegramUserId, chatIdStr);
            } else if (text.startsWith("/cancel")) {
                clearTimezoneChangeState(chatIdStr);
                clearNotificationState(chatIdStr);
                handleCancelCommand(chatIdStr);
            } else if (text.startsWith("/link")) {
                clearAuthState(chatIdStr);
                clearRegistrationState(chatIdStr);
                clearTimeSlotMarkingState(chatIdStr);
                clearTimezoneChangeState(chatIdStr);
                clearNotificationState(chatIdStr);
                String[] parts = text.split("\\s+", 2);
                if (parts.length == 2) {
                    handleLinkCommand(telegramUserId, chatIdStr, parts[1]);
                } else {
                    sendPersonalMessage(chatIdStr, "Использование: /link <token>\n\nПолучите токен в настройках профиля на веб-сайте.");
                }
            } else if (text.startsWith("/games") || text.startsWith("/upcoming")) {
                clearAuthState(chatIdStr);
                clearRegistrationState(chatIdStr);
                clearTimeSlotMarkingState(chatIdStr);
                clearTimezoneChangeState(chatIdStr);
                clearNotificationState(chatIdStr);
                handleGamesCommand(telegramUserId, chatIdStr);
            } else if (text.startsWith("/game")) {
                clearAuthState(chatIdStr);
                clearRegistrationState(chatIdStr);
                clearTimeSlotMarkingState(chatIdStr);
                clearTimezoneChangeState(chatIdStr);
                clearNotificationState(chatIdStr);
                String[] parts = text.split("\\s+", 2);
                if (parts.length == 2) {
                    try {
                        Long gameId = Long.parseLong(parts[1]);
                        handleGameDetailsCommand(telegramUserId, chatIdStr, gameId);
                    } catch (NumberFormatException e) {
                        sendPersonalMessage(chatIdStr, "❌ Неверный формат ID игры.\n\nИспользование: /game <id>\n\nПолучите ID из списка игр командой /games");
                    }
                } else {
                    sendPersonalMessage(chatIdStr, "Использование: /game <id>\n\nПолучите ID из списка игр командой /games");
                }
            } else if (text.startsWith("/invite")) {
                clearAuthState(chatIdStr);
                clearRegistrationState(chatIdStr);
                clearTimeSlotMarkingState(chatIdStr);
                clearTimezoneChangeState(chatIdStr);
                clearNotificationState(chatIdStr);
                handleInviteCommand(telegramUserId, chatIdStr);
            } else if (text.startsWith("/myinvites")) {
                clearAuthState(chatIdStr);
                clearRegistrationState(chatIdStr);
                clearTimeSlotMarkingState(chatIdStr);
                clearTimezoneChangeState(chatIdStr);
                clearNotificationState(chatIdStr);
                handleMyInvitesCommand(telegramUserId, chatIdStr);
            } else if (text.startsWith("/mark")) {
                clearAuthState(chatIdStr);
                clearRegistrationState(chatIdStr);
                clearTimeSlotMarkingState(chatIdStr);
                clearTimezoneChangeState(chatIdStr);
                clearNotificationState(chatIdStr);
                handleMarkCommand(telegramUserId, chatIdStr);
            } else if (text.startsWith("/myslots")) {
                clearAuthState(chatIdStr);
                clearRegistrationState(chatIdStr);
                clearTimeSlotMarkingState(chatIdStr);
                clearTimezoneChangeState(chatIdStr);
                clearNotificationState(chatIdStr);
                handleMySlotsCommand(telegramUserId, chatIdStr);
            } else if (text.startsWith("/menu")) {
                clearAuthState(chatIdStr);
                clearRegistrationState(chatIdStr);
                clearTimeSlotMarkingState(chatIdStr);
                clearTimezoneChangeState(chatIdStr);
                clearNotificationState(chatIdStr);
                handleMenuCommand(telegramUserId, chatIdStr);
            } else if (text.startsWith("/help")) {
                clearAuthState(chatIdStr);
                clearRegistrationState(chatIdStr);
                clearTimeSlotMarkingState(chatIdStr);
                clearTimezoneChangeState(chatIdStr);
                clearNotificationState(chatIdStr);
                handleHelpCommand(chatIdStr);
            } else {
                // Обработка состояний (если не команда)
                // Проверяем в порядке приоритета: регистрация -> авторизация -> разметка времени -> смена часового пояса -> настройки уведомлений
                RegistrationState regState = registrationStates.get(chatIdStr);
                if (regState != null) {
                    handleRegistrationState(telegramUserId, chatIdStr, text, regState);
                } else {
                    AuthState authState = authStates.get(chatIdStr);
                    if (authState != null) {
                        handleAuthState(telegramUserId, chatIdStr, text, authState);
                    } else {
                        TimeSlotMarkingState markingState = timeSlotMarkingStates.get(chatIdStr);
                        if (markingState != null) {
                            handleTimeSlotMarkingState(telegramUserId, chatIdStr, text, markingState);
                        } else {
                            TimezoneChangeState timezoneState = timezoneChangeStates.get(chatIdStr);
                            if (timezoneState != null) {
                                handleTimezoneChangeState(telegramUserId, chatIdStr, text, timezoneState);
                            } else {
                                NotificationState notificationState = notificationStates.get(chatIdStr);
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
                        "<b>Способ 1: Регистрация нового аккаунта</b>\n" +
                        "Используйте команду: /register\n\n" +
                        "<b>Способ 2: Авторизация через логин/пароль</b>\n" +
                        "Используйте команду: /auth\n\n" +
                        "<b>Способ 3: Привязка через токен</b>\n" +
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
    
    private void handleAuthCommand(Long telegramUserId, String chatId) {
        try {
            // Проверяем, не связан ли уже аккаунт
            User existingUser = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
            if (existingUser != null) {
                sendPersonalMessage(chatId, "✅ Ваш аккаунт уже связан!\n\n" +
                        "Используйте /games для получения списка предстоящих игр.");
                return;
            }
            
            // Проверяем блокировку
            if (isBlocked(chatId)) {
                long remainingSeconds = getBlockTimeRemaining(chatId);
                long remainingMinutes = remainingSeconds / 60;
                sendPersonalMessage(chatId, "⛔ Слишком много неудачных попыток авторизации.\n\n" +
                        "Попробуйте снова через " + remainingMinutes + " минут.");
                return;
            }
            
            // Инициализируем состояние авторизации
            authStates.put(chatId, AuthState.WAITING_USERNAME);
            updateAuthTimestamp(chatId);
            
            sendPersonalMessage(chatId, "🔐 <b>Авторизация для привязки аккаунта</b>\n\n" +
                    "Введите ваш логин (имя пользователя):\n\n" +
                    "💡 Используйте /cancel для отмены.");
        } catch (Exception e) {
            logger.error("Error handling /auth command", e);
            clearAuthState(chatId);
            sendPersonalMessage(chatId, "❌ Произошла ошибка при инициализации авторизации. Попробуйте позже.");
        }
    }
    
    private void handleRegisterCommand(Long telegramUserId, String chatId) {
        try {
            // Проверяем, не зарегистрирован ли уже пользователь
            User existingUser = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
            if (existingUser != null) {
                sendPersonalMessage(chatId, "✅ Вы уже зарегистрированы!\n\n" +
                        "Используйте /games для получения списка предстоящих игр.");
                return;
            }
            
            // Проверяем блокировку регистрации
            if (isRegistrationBlocked(chatId)) {
                long remainingSeconds = getRegistrationBlockTimeRemaining(chatId);
                long remainingMinutes = remainingSeconds / 60;
                sendPersonalMessage(chatId, "⛔ Слишком много неудачных попыток регистрации.\n\n" +
                        "Попробуйте снова через " + remainingMinutes + " минут.");
                return;
            }
            
            // Инициализируем состояние регистрации
            registrationStates.put(chatId, RegistrationState.WAITING_INVITE);
            registrationData.put(chatId, new RegistrationData());
            updateRegistrationTimestamp(chatId);
            
            sendPersonalMessage(chatId, "📝 <b>Регистрация нового аккаунта</b>\n\n" +
                    "Введите инвайт-код для регистрации:\n\n" +
                    "💡 Используйте /cancel для отмены.");
        } catch (Exception e) {
            logger.error("Error handling /register command", e);
            clearRegistrationState(chatId);
            sendPersonalMessage(chatId, "❌ Произошла ошибка при инициализации регистрации. Попробуйте позже.");
        }
    }
    
    private void handleCancelCommand(String chatId) {
        boolean hasAuth = authStates.containsKey(chatId);
        boolean hasRegistration = registrationStates.containsKey(chatId);
        boolean hasMarking = timeSlotMarkingStates.containsKey(chatId);
        boolean hasTimezoneChange = timezoneChangeStates.containsKey(chatId);
        boolean hasNotification = notificationStates.containsKey(chatId);
        
        if (hasRegistration) {
            clearRegistrationState(chatId);
            sendPersonalMessage(chatId, "✅ Процесс регистрации отменен.\n\n" +
                    "Используйте /register для начала заново.");
        } else if (hasAuth) {
            clearAuthState(chatId);
            sendPersonalMessage(chatId, "✅ Процесс авторизации отменен.\n\n" +
                    "Используйте /auth для начала заново или /link <token> для привязки через токен.");
        } else if (hasMarking) {
            clearTimeSlotMarkingState(chatId);
            sendPersonalMessage(chatId, "✅ Процесс разметки времени отменен.\n\n" +
                    "Используйте /mark для начала заново.");
        } else if (hasTimezoneChange) {
            clearTimezoneChangeState(chatId);
            sendPersonalMessage(chatId, "✅ Процесс смены часового пояса отменен.\n\n" +
                    "Используйте меню для начала заново.");
        } else if (hasNotification) {
            clearNotificationState(chatId);
            sendPersonalMessage(chatId, "✅ Процесс настройки уведомлений отменен.\n\n" +
                    "Используйте меню для начала заново.");
        } else {
            sendPersonalMessage(chatId, "ℹ️ Нет активного процесса для отмены.");
        }
    }
    
    private void handleAuthState(Long telegramUserId, String chatId, String text, AuthState state) {
        try {
            updateAuthTimestamp(chatId);
            
            if (state == AuthState.WAITING_USERNAME) {
                // Сохраняем username и переходим к паролю
                String username = text.trim();
                if (username.isEmpty()) {
                    sendPersonalMessage(chatId, "❌ Логин не может быть пустым. Введите ваш логин:");
                    return;
                }
                
                authUsernames.put(chatId, username);
                authStates.put(chatId, AuthState.WAITING_PASSWORD);
                updateAuthTimestamp(chatId);
                
                sendPersonalMessage(chatId, "🔑 Теперь введите ваш пароль:\n\n" +
                        "💡 Используйте /cancel для отмены.");
                
            } else if (state == AuthState.WAITING_PASSWORD) {
                // Проверяем учетные данные и связываем аккаунт
                String password = text.trim();
                String username = authUsernames.get(chatId);
                
                if (password.isEmpty()) {
                    sendPersonalMessage(chatId, "❌ Пароль не может быть пустым. Введите ваш пароль:");
                    return;
                }
                
                if (username == null || username.isEmpty()) {
                    // Не должно произойти, но на всякий случай
                    clearAuthState(chatId);
                    sendPersonalMessage(chatId, "❌ Ошибка: логин не найден. Начните заново с /auth.");
                    return;
                }
                
                // Проверяем блокировку перед попыткой
                if (isBlocked(chatId)) {
                    long remainingSeconds = getBlockTimeRemaining(chatId);
                    long remainingMinutes = remainingSeconds / 60;
                    clearAuthState(chatId);
                    sendPersonalMessage(chatId, "⛔ Слишком много неудачных попыток авторизации.\n\n" +
                            "Попробуйте снова через " + remainingMinutes + " минут.");
                    return;
                }
                
                try {
                    // Пытаемся связать аккаунт
                    notificationSettingsService.linkTelegramAccountByCredentials(
                            username, password, telegramUserId, chatId);
                    
                    // Успешная авторизация
                    recordAuthAttempt(chatId, true);
                    clearAuthState(chatId);
                    
                    sendPersonalMessage(chatId, "✅ <b>Аккаунт успешно связан!</b>\n\n" +
                            "Теперь вы будете получать персональные уведомления.\n\n" +
                            "Доступные команды:\n" +
                            "/games - Список предстоящих игр\n" +
                            "/help - Справка по командам\n" +
                            "/stop - Отписаться от уведомлений");
                    
                    logger.info("Telegram account linked via auth for chatId: {}", chatId);
                    
                } catch (RuntimeException e) {
                    // Неудачная попытка
                    recordAuthAttempt(chatId, false);
                    
                    int remainingAttempts = getRemainingAttempts(chatId);
                    
                    if (isBlocked(chatId)) {
                        long remainingSeconds = getBlockTimeRemaining(chatId);
                        long remainingMinutes = remainingSeconds / 60;
                        clearAuthState(chatId);
                        sendPersonalMessage(chatId, "⛔ <b>Слишком много неудачных попыток</b>\n\n" +
                                "Авторизация заблокирована на " + remainingMinutes + " минут.\n\n" +
                                "Попробуйте снова позже или используйте /link <token> для привязки через токен.");
                        logger.warn("Telegram auth blocked for chatId: {} after failed attempt", chatId);
                    } else {
                        // Ошибка авторизации, но еще есть попытки
                        String errorMessage = e.getMessage();
                        if (errorMessage.contains("Invalid username or password")) {
                            sendPersonalMessage(chatId, "❌ <b>Неверный логин или пароль</b>\n\n" +
                                    "Осталось попыток: " + remainingAttempts + "\n\n" +
                                    "Введите пароль еще раз или используйте /cancel для отмены.");
                        } else if (errorMessage.contains("уже связан")) {
                            clearAuthState(chatId);
                            sendPersonalMessage(chatId, "❌ " + errorMessage + "\n\n" +
                                    "Используйте /start для проверки статуса.");
                        } else {
                            sendPersonalMessage(chatId, "❌ Ошибка: " + errorMessage + "\n\n" +
                                    "Осталось попыток: " + remainingAttempts + "\n\n" +
                                    "Попробуйте снова или используйте /cancel для отмены.");
                        }
                        logger.warn("Telegram auth failed for chatId: {}, remaining attempts: {}", chatId, remainingAttempts);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error handling auth state", e);
            clearAuthState(chatId);
            sendPersonalMessage(chatId, "❌ Произошла ошибка при обработке авторизации. Попробуйте позже или используйте /link <token>.");
        }
    }
    
    private void handleRegistrationState(Long telegramUserId, String chatId, String text, RegistrationState state) {
        try {
            updateRegistrationTimestamp(chatId);
            RegistrationData data = registrationData.get(chatId);
            
            if (data == null) {
                clearRegistrationState(chatId);
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
            clearRegistrationState(chatId);
            sendPersonalMessage(chatId, "❌ Произошла ошибка при обработке регистрации. Попробуйте позже.");
        }
    }
    
    private void handleInviteInput(Long telegramUserId, String chatId, String inviteCode, RegistrationData data) {
        if (inviteCode.isEmpty()) {
            sendPersonalMessage(chatId, "❌ Инвайт-код не может быть пустым. Введите инвайт-код:");
            return;
        }
        
        try {
            // Проверяем валидность инвайт-кода
            inviteService.getInviteByCode(inviteCode);
            data.inviteCode = inviteCode;
            registrationStates.put(chatId, RegistrationState.WAITING_USERNAME);
            updateRegistrationTimestamp(chatId);
            
            sendPersonalMessage(chatId, "✅ Инвайт-код принят!\n\n" +
                    "Введите логин (имя пользователя):\n\n" +
                    "💡 Используйте /cancel для отмены.");
        } catch (RuntimeException e) {
            String errorMsg = e.getMessage();
            if (errorMsg.contains("not found") || errorMsg.contains("Invalid")) {
                sendPersonalMessage(chatId, "❌ <b>Неверный инвайт-код</b>\n\n" +
                        "Проверьте правильность кода и попробуйте снова.\n\n" +
                        "💡 Используйте /cancel для отмены.");
            } else if (errorMsg.contains("expired") || errorMsg.contains("used")) {
                sendPersonalMessage(chatId, "❌ <b>Инвайт-код недействителен</b>\n\n" +
                        "Код истек или уже использован.\n\n" +
                        "💡 Используйте /cancel для отмены.");
            } else {
                sendPersonalMessage(chatId, "❌ Ошибка: " + errorMsg + "\n\n" +
                        "Попробуйте снова или используйте /cancel для отмены.");
            }
            logger.warn("Invalid invite code for registration: {}", inviteCode);
        }
    }
    
    private void handleUsernameInput(String chatId, String username, RegistrationData data) {
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
        registrationStates.put(chatId, RegistrationState.WAITING_NAME);
        updateRegistrationTimestamp(chatId);
        
        sendPersonalMessage(chatId, "✅ Логин принят!\n\n" +
                "Введите ваше имя (или нажмите Enter, чтобы использовать логин):\n\n" +
                "💡 Используйте /cancel для отмены.");
    }
    
    private void handleNameInput(String chatId, String name, RegistrationData data) {
        // Имя опционально, если пустое - используем username
        if (name.trim().isEmpty()) {
            data.name = data.username;
        } else {
            data.name = name.trim();
        }
        
        registrationStates.put(chatId, RegistrationState.WAITING_EMAIL);
        updateRegistrationTimestamp(chatId);
        
        sendPersonalMessage(chatId, "✅ Имя принято!\n\n" +
                "Введите ваш email:\n\n" +
                "💡 Используйте /cancel для отмены.");
    }
    
    private void handleEmailInput(String chatId, String email, RegistrationData data) {
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
        registrationStates.put(chatId, RegistrationState.WAITING_PASSWORD);
        updateRegistrationTimestamp(chatId);
        
        sendPersonalMessage(chatId, "✅ Email принят!\n\n" +
                "Введите пароль (минимум " + MIN_PASSWORD_LENGTH + " символов):\n\n" +
                "💡 Используйте /cancel для отмены.");
    }
    
    private void handlePasswordInput(String chatId, String password, RegistrationData data) {
        if (password.isEmpty()) {
            sendPersonalMessage(chatId, "❌ Пароль не может быть пустым. Введите пароль:");
            return;
        }
        
        if (password.length() < MIN_PASSWORD_LENGTH) {
            sendPersonalMessage(chatId, "❌ <b>Пароль слишком короткий</b>\n\n" +
                    "Пароль должен содержать минимум " + MIN_PASSWORD_LENGTH + " символов.\n\n" +
                    "Введите пароль еще раз:\n\n" +
                    "💡 Используйте /cancel для отмены.");
            return;
        }
        
        data.password = password;
        registrationStates.put(chatId, RegistrationState.WAITING_PASSWORD_CONFIRM);
        updateRegistrationTimestamp(chatId);
        
        sendPersonalMessage(chatId, "✅ Пароль принят!\n\n" +
                "Подтвердите пароль (введите его еще раз):\n\n" +
                "💡 Используйте /cancel для отмены.");
    }
    
    private void handlePasswordConfirmInput(Long telegramUserId, String chatId, String passwordConfirm, RegistrationData data) {
        if (passwordConfirm.isEmpty()) {
            sendPersonalMessage(chatId, "❌ Подтверждение пароля не может быть пустым. Введите пароль еще раз:");
            return;
        }
        
        if (!passwordConfirm.equals(data.password)) {
            sendPersonalMessage(chatId, "❌ <b>Пароли не совпадают</b>\n\n" +
                    "Введите пароль еще раз:\n\n" +
                    "💡 Используйте /cancel для отмены.");
            // Возвращаемся к вводу пароля
            registrationStates.put(chatId, RegistrationState.WAITING_PASSWORD);
            updateRegistrationTimestamp(chatId);
            return;
        }
        
        // Все данные собраны, выполняем регистрацию
        try {
            // Проверяем блокировку перед попыткой
            if (isRegistrationBlocked(chatId)) {
                long remainingSeconds = getRegistrationBlockTimeRemaining(chatId);
                long remainingMinutes = remainingSeconds / 60;
                clearRegistrationState(chatId);
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
            recordRegistrationAttempt(chatId, true);
            clearRegistrationState(chatId);
            
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
            recordRegistrationAttempt(chatId, false);
            
            int remainingAttempts = getRemainingRegistrationAttempts(chatId);
            String errorMsg = e.getMessage();
            
            if (isRegistrationBlocked(chatId)) {
                long remainingSeconds = getRegistrationBlockTimeRemaining(chatId);
                long remainingMinutes = remainingSeconds / 60;
                clearRegistrationState(chatId);
                sendPersonalMessage(chatId, "⛔ <b>Слишком много неудачных попыток</b>\n\n" +
                        "Регистрация заблокирована на " + remainingMinutes + " минут.\n\n" +
                        "Попробуйте снова позже.");
                logger.warn("Telegram registration blocked for chatId: {} after failed attempt", chatId);
            } else {
                // Ошибка регистрации, но еще есть попытки
                if (errorMsg.contains("already exists")) {
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
                } else if (errorMsg.contains("Invite")) {
                    sendPersonalMessage(chatId, "❌ <b>Ошибка с инвайт-кодом</b>\n\n" +
                            errorMsg + "\n\n" +
                            "Начните регистрацию заново с /register.\n\n" +
                            "Осталось попыток: " + remainingAttempts);
                } else {
                    sendPersonalMessage(chatId, "❌ Ошибка регистрации: " + errorMsg + "\n\n" +
                            "Осталось попыток: " + remainingAttempts + "\n\n" +
                            "Начните регистрацию заново с /register или используйте /cancel для отмены.");
                }
                logger.warn("Telegram registration failed for chatId: {}, error: {}, remaining attempts: {}", 
                        chatId, errorMsg, remainingAttempts);
            }
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
            
            // Для команды /games показываем все игры без пагинации
            int totalPages = (int) Math.ceil((double) upcomingGames.size() / GAMES_PER_PAGE);
            String message = buildUpcomingGamesListMessage(upcomingGames, 0, totalPages);
            sendPersonalMessage(chatId, message);
        } catch (Exception e) {
            logger.error("Error handling /games command", e);
            sendPersonalMessage(chatId, "❌ Ошибка при получении списка игр. Попробуйте позже.");
        }
    }
    
    private void handleInviteCommand(Long telegramUserId, String chatId) {
        try {
            User user = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
            
            if (user == null) {
                sendPersonalMessage(chatId, "❌ Ваш аккаунт не связан с веб-сайтом.\n\n" +
                        "Используйте /register для регистрации или /auth для привязки существующего аккаунта.");
                return;
            }
            
            // Создаем бессрочный одноразовый инвайт-код
            ru.ambryo.gameplannerback.dto.CreateInviteRequest request = 
                    new ru.ambryo.gameplannerback.dto.CreateInviteRequest(null, 1);
            
            ru.ambryo.gameplannerback.dto.InviteDto invite = inviteService.createInvite(user, request);
            
            // Форматируем и отправляем сообщение с инвайт-кодом
            String message = buildInviteCreatedMessage(invite);
            sendPersonalMessage(chatId, message);
            
            logger.info("Invite code created via Telegram for user: {}, chatId: {}", user.getUsername(), chatId);
        } catch (Exception e) {
            logger.error("Error handling /invite command", e);
            sendPersonalMessage(chatId, "❌ Ошибка при создании инвайт-кода. Попробуйте позже.");
        }
    }
    
    private void handleMyInvitesCommand(Long telegramUserId, String chatId) {
        try {
            User user = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
            
            if (user == null) {
                sendPersonalMessage(chatId, "❌ Ваш аккаунт не связан с веб-сайтом.\n\n" +
                        "Используйте /register для регистрации или /auth для привязки существующего аккаунта.");
                return;
            }
            
            // Получаем список инвайт-кодов пользователя
            List<ru.ambryo.gameplannerback.dto.InviteDto> invites = inviteService.getMyInvites(user);
            
            if (invites.isEmpty()) {
                sendPersonalMessage(chatId, "📋 <b>Мои инвайт-коды</b>\n\n" +
                        "У вас пока нет созданных инвайт-кодов.\n\n" +
                        "Используйте /invite для создания нового инвайт-кода.");
                return;
            }
            
            String message = buildMyInvitesListMessage(invites);
            sendPersonalMessage(chatId, message);
        } catch (Exception e) {
            logger.error("Error handling /myinvites command", e);
            sendPersonalMessage(chatId, "❌ Ошибка при получении списка инвайт-кодов. Попробуйте позже.");
        }
    }
    
    private void handleMarkCommand(Long telegramUserId, String chatId) {
        try {
            User user = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
            
            if (user == null) {
                sendPersonalMessage(chatId, "❌ Ваш аккаунт не связан с веб-сайтом.\n\n" +
                        "Используйте /register для регистрации или /auth для привязки существующего аккаунта.");
                return;
            }
            
            // Проверяем наличие часового пояса
            if (user.getTimezone() == null || user.getTimezone().trim().isEmpty()) {
                sendPersonalMessage(chatId, "❌ <b>Часовой пояс не установлен</b>\n\n" +
                        "Для разметки времени необходимо установить часовой пояс.\n\n" +
                        "Вы можете установить часовой пояс:\n" +
                        "• Через меню: /menu → Настройки → Часовой пояс\n" +
                        "• В настройках профиля на веб-сайте\n\n" +
                        "После установки часового пояса вы сможете использовать команду /mark для разметки времени.");
                return;
            }
            
            // Инициализируем состояние разметки времени
            timeSlotMarkingStates.put(chatId, TimeSlotMarkingState.WAITING_DATE);
            timeSlotMarkingData.put(chatId, new TimeSlotMarkingData());
            updateTimeSlotMarkingTimestamp(chatId);
            
            sendPersonalMessage(chatId, "📅 <b>Разметка свободного времени</b>\n\n" +
                    "Введите дату в формате ДД.ММ.ГГГГ (например: 15.01.2025)\n" +
                    "Или используйте: сегодня, завтра, послезавтра\n\n" +
                    "💡 Используйте /cancel для отмены.");
        } catch (Exception e) {
            logger.error("Error handling /mark command", e);
            clearTimeSlotMarkingState(chatId);
            sendPersonalMessage(chatId, "❌ Произошла ошибка при инициализации разметки времени. Попробуйте позже.");
        }
    }
    
    private void handleTimeSlotMarkingState(Long telegramUserId, String chatId, String text, TimeSlotMarkingState state) {
        try {
            updateTimeSlotMarkingTimestamp(chatId);
            TimeSlotMarkingData data = timeSlotMarkingData.get(chatId);
            User user = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
            
            if (data == null || user == null) {
                clearTimeSlotMarkingState(chatId);
                sendPersonalMessage(chatId, "❌ Ошибка: данные разметки не найдены. Начните заново с /mark.");
                return;
            }
            
            // Получаем часовой пояс пользователя
            ZoneId userTimezone;
            try {
                userTimezone = ZoneId.of(user.getTimezone());
            } catch (Exception e) {
                clearTimeSlotMarkingState(chatId);
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
            clearTimeSlotMarkingState(chatId);
            sendPersonalMessage(chatId, "❌ Произошла ошибка при обработке разметки времени. Попробуйте позже.");
        }
    }
    
    private void handleDateInput(String chatId, String dateStr, TimeSlotMarkingData data, ZoneId userTimezone) {
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
        timeSlotMarkingStates.put(chatId, TimeSlotMarkingState.WAITING_TIME);
        updateTimeSlotMarkingTimestamp(chatId);
        
        sendPersonalMessage(chatId, "✅ Дата принята: " + formatLocalDate(localDate) + "\n\n" +
                "Введите время начала в формате ЧЧ:ММ или ЧЧ (например: 18:00 или 18):\n\n" +
                "💡 Используйте /cancel для отмены.");
    }
    
    private void handleTimeInput(String chatId, String timeStr, TimeSlotMarkingData data, ZoneId userTimezone) {
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
        timeSlotMarkingStates.put(chatId, TimeSlotMarkingState.WAITING_DURATION);
        updateTimeSlotMarkingTimestamp(chatId);
        
        sendPersonalMessage(chatId, "✅ Время принято: " + formatLocalTime(localTime) + "\n\n" +
                "Введите продолжительность в часах (например: 1, 2, 3):\n\n" +
                "💡 Используйте /cancel для отмены.");
    }
    
    private void handleDurationInput(Long telegramUserId, String chatId, String durationStr, 
                                     TimeSlotMarkingData data, User user, ZoneId userTimezone) {
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
            clearTimeSlotMarkingState(chatId);
            sendPersonalMessage(chatId, "❌ Ошибка: время не найдено. Начните заново с /mark.");
            return;
        }
        
        Instant startInstant = convertToUTC(localDate, localTime, userTimezone);
        data.startInstant = startInstant;
        
        // Вызываем UserService.toggleTimeSlot для создания/удаления слота
        try {
            userService.toggleTimeSlot(user, startInstant, duration);
            
            // Успешная разметка
            clearTimeSlotMarkingState(chatId);
            
            String message = buildTimeSlotMarkedMessage(localDate, localTime, duration, userTimezone);
            sendPersonalMessage(chatId, message);
            
            logger.info("Time slot marked via Telegram for user: {}, chatId: {}, start: {}, duration: {}", 
                    user.getUsername(), chatId, startInstant, duration);
        } catch (Exception e) {
            logger.error("Error toggling time slot via Telegram", e);
            clearTimeSlotMarkingState(chatId);
            sendPersonalMessage(chatId, "❌ Ошибка при сохранении временного слота. Попробуйте позже.");
        }
    }
    
    private void handleMySlotsCommand(Long telegramUserId, String chatId) {
        try {
            User user = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
            
            if (user == null) {
                sendPersonalMessage(chatId, "❌ Ваш аккаунт не связан с веб-сайтом.\n\n" +
                        "Используйте /register для регистрации или /auth для привязки существующего аккаунта.");
                return;
            }
            
            // Получаем текущего пользователя как PlayerDto с временными слотами
            Instant now = Instant.now();
            Instant endDate = now.plusSeconds(30L * 24 * 60 * 60); // 30 дней вперед
            ru.ambryo.gameplannerback.dto.PlayerDto player = userService.getUserAsPlayerWithTimeSlots(user, now, endDate);
            
            List<ru.ambryo.gameplannerback.dto.TimeSlotDto> slots = player.getAvailableTimes();
            
            if (slots == null || slots.isEmpty()) {
                sendPersonalMessage(chatId, "📅 <b>Мои временные слоты</b>\n\n" +
                        "У вас пока нет размеченного времени.\n\n" +
                        "Используйте /mark для разметки свободного времени.");
                return;
            }
            
            String message = buildMySlotsListMessage(slots, user.getTimezone());
            sendPersonalMessage(chatId, message);
        } catch (Exception e) {
            logger.error("Error handling /myslots command", e);
            sendPersonalMessage(chatId, "❌ Ошибка при получении списка временных слотов. Попробуйте позже.");
        }
    }
    
    private void handleMenuCommand(Long telegramUserId, String chatId) {
        try {
            User user = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
            boolean isLinked = user != null;
            
            String message = "📱 <b>Главное меню</b>\n\n";
            if (isLinked && user != null) {
                message += "✅ Аккаунт связан\n";
                message += "👤 Пользователь: " + escapeHtml(user.getUsername()) + "\n\n";
                message += "Выберите раздел:";
            } else {
                message += "❌ Аккаунт не связан\n\n";
                message += "Для доступа ко всем функциям необходимо связать аккаунт.\n\n";
                message += "Выберите способ связывания:";
            }
            
            InlineKeyboardMarkup keyboard = buildMainMenuKeyboard(isLinked);
            
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText(message);
            sendMessage.setParseMode("HTML");
            sendMessage.setReplyMarkup(keyboard);
            
            execute(sendMessage);
        } catch (Exception e) {
            logger.error("Error handling /menu command", e);
            sendPersonalMessage(chatId, "❌ Произошла ошибка при открытии меню. Попробуйте позже.");
        }
    }
    
    private InlineKeyboardMarkup buildMainMenuKeyboard(boolean isLinked) {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new java.util.ArrayList<>();
        
        if (isLinked) {
            // Для связанных пользователей - полное меню
            // Кнопка "Игры"
            List<InlineKeyboardButton> gamesRow = new java.util.ArrayList<>();
            InlineKeyboardButton gamesButton = new InlineKeyboardButton();
            gamesButton.setText("🎮 Игры");
            gamesButton.setCallbackData("menu_games");
            gamesRow.add(gamesButton);
            rows.add(gamesRow);
            
            // Кнопка "Разметка времени"
            List<InlineKeyboardButton> timeRow = new java.util.ArrayList<>();
            InlineKeyboardButton timeButton = new InlineKeyboardButton();
            timeButton.setText("📅 Разметка времени");
            timeButton.setCallbackData("menu_time");
            timeRow.add(timeButton);
            rows.add(timeRow);
            
            // Кнопка "Настройки"
            List<InlineKeyboardButton> settingsRow = new java.util.ArrayList<>();
            InlineKeyboardButton settingsButton = new InlineKeyboardButton();
            settingsButton.setText("⚙️ Настройки");
            settingsButton.setCallbackData("menu_settings");
            settingsRow.add(settingsButton);
            rows.add(settingsRow);
            
            // Кнопка "Помощь"
            List<InlineKeyboardButton> helpRow = new java.util.ArrayList<>();
            InlineKeyboardButton helpButton = new InlineKeyboardButton();
            helpButton.setText("📖 Помощь");
            helpButton.setCallbackData("menu_help");
            helpRow.add(helpButton);
            rows.add(helpRow);
        } else {
            // Для несвязанных пользователей - регистрация, авторизация и связывание
            // Кнопка "Зарегистрироваться"
            List<InlineKeyboardButton> registerRow = new java.util.ArrayList<>();
            InlineKeyboardButton registerButton = new InlineKeyboardButton();
            registerButton.setText("📝 Зарегистрироваться");
            registerButton.setCallbackData("menu_register");
            registerRow.add(registerButton);
            rows.add(registerRow);
            
            // Кнопка "Авторизоваться"
            List<InlineKeyboardButton> authRow = new java.util.ArrayList<>();
            InlineKeyboardButton authButton = new InlineKeyboardButton();
            authButton.setText("🔐 Авторизоваться");
            authButton.setCallbackData("menu_auth");
            authRow.add(authButton);
            rows.add(authRow);
            
            // Кнопка "Связать"
            List<InlineKeyboardButton> linkRow = new java.util.ArrayList<>();
            InlineKeyboardButton linkButton = new InlineKeyboardButton();
            linkButton.setText("🔗 Связать");
            linkButton.setCallbackData("menu_link");
            linkRow.add(linkButton);
            rows.add(linkRow);
        }
        
        keyboard.setKeyboard(rows);
        return keyboard;
    }
    
    private InlineKeyboardMarkup buildGamesMenuKeyboard() {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new java.util.ArrayList<>();
        
        // Кнопка "Список предстоящих игр"
        List<InlineKeyboardButton> listRow = new java.util.ArrayList<>();
        InlineKeyboardButton listButton = new InlineKeyboardButton();
        listButton.setText("📋 Список предстоящих игр");
        listButton.setCallbackData("menu_games_list");
        listRow.add(listButton);
        rows.add(listRow);
        
        // Кнопка "Назад"
        List<InlineKeyboardButton> backRow = new java.util.ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("◀️ Назад");
        backButton.setCallbackData("menu_main");
        backRow.add(backButton);
        rows.add(backRow);
        
        keyboard.setKeyboard(rows);
        return keyboard;
    }
    
    private InlineKeyboardMarkup buildTimeMenuKeyboard() {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new java.util.ArrayList<>();
        
        // Кнопка "Разметить время"
        List<InlineKeyboardButton> markRow = new java.util.ArrayList<>();
        InlineKeyboardButton markButton = new InlineKeyboardButton();
        markButton.setText("➕ Разметить время");
        markButton.setCallbackData("menu_time_mark");
        markRow.add(markButton);
        rows.add(markRow);
        
        // Кнопка "Мои слоты"
        List<InlineKeyboardButton> slotsRow = new java.util.ArrayList<>();
        InlineKeyboardButton slotsButton = new InlineKeyboardButton();
        slotsButton.setText("📅 Мои слоты");
        slotsButton.setCallbackData("menu_time_slots");
        slotsRow.add(slotsButton);
        rows.add(slotsRow);
        
        // Кнопка "Назад"
        List<InlineKeyboardButton> backRow = new java.util.ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("◀️ Назад");
        backButton.setCallbackData("menu_main");
        backRow.add(backButton);
        rows.add(backRow);
        
        keyboard.setKeyboard(rows);
        return keyboard;
    }
    
    private InlineKeyboardMarkup buildInvitesMenuKeyboard() {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new java.util.ArrayList<>();
        
        // Кнопка "Создать инвайт-код"
        List<InlineKeyboardButton> createRow = new java.util.ArrayList<>();
        InlineKeyboardButton createButton = new InlineKeyboardButton();
        createButton.setText("➕ Создать инвайт-код");
        createButton.setCallbackData("menu_invites_create");
        createRow.add(createButton);
        rows.add(createRow);
        
        // Кнопка "Мои инвайт-коды"
        List<InlineKeyboardButton> listRow = new java.util.ArrayList<>();
        InlineKeyboardButton listButton = new InlineKeyboardButton();
        listButton.setText("📋 Мои инвайт-коды");
        listButton.setCallbackData("menu_invites_list");
        listRow.add(listButton);
        rows.add(listRow);
        
        // Кнопка "Назад" (возврат в настройки)
        List<InlineKeyboardButton> backRow = new java.util.ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("◀️ Назад");
        backButton.setCallbackData("menu_settings");
        backRow.add(backButton);
        rows.add(backRow);
        
        keyboard.setKeyboard(rows);
        return keyboard;
    }
    
    private InlineKeyboardMarkup buildSettingsMenuKeyboard(boolean isLinked) {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new java.util.ArrayList<>();
        
        // Кнопка "Профиль"
        List<InlineKeyboardButton> profileRow = new java.util.ArrayList<>();
        InlineKeyboardButton profileButton = new InlineKeyboardButton();
        profileButton.setText("👤 Профиль");
        profileButton.setCallbackData("menu_settings_profile");
        profileRow.add(profileButton);
        rows.add(profileRow);
        
        // Кнопка "Часовой пояс" (только если аккаунт связан)
        if (isLinked) {
            List<InlineKeyboardButton> timezoneRow = new java.util.ArrayList<>();
            InlineKeyboardButton timezoneButton = new InlineKeyboardButton();
            timezoneButton.setText("🌍 Часовой пояс");
            timezoneButton.setCallbackData("menu_settings_timezone");
            timezoneRow.add(timezoneButton);
            rows.add(timezoneRow);
        }
        
        // Кнопка "Уведомления" (только если аккаунт связан)
        if (isLinked) {
            List<InlineKeyboardButton> notificationsRow = new java.util.ArrayList<>();
            InlineKeyboardButton notificationsButton = new InlineKeyboardButton();
            notificationsButton.setText("🔔 Уведомления");
            notificationsButton.setCallbackData("menu_settings_notifications");
            notificationsRow.add(notificationsButton);
            rows.add(notificationsRow);
        }
        
        // Кнопка "Инвайты" (только если аккаунт связан)
        if (isLinked) {
            List<InlineKeyboardButton> invitesRow = new java.util.ArrayList<>();
            InlineKeyboardButton invitesButton = new InlineKeyboardButton();
            invitesButton.setText("🎫 Инвайты");
            invitesButton.setCallbackData("menu_invites");
            invitesRow.add(invitesButton);
            rows.add(invitesRow);
        }
        
        // Кнопка "Назад"
        List<InlineKeyboardButton> backRow = new java.util.ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("◀️ Назад");
        backButton.setCallbackData("menu_main");
        backRow.add(backButton);
        rows.add(backRow);
        
        keyboard.setKeyboard(rows);
        return keyboard;
    }
    
    private void handleHelpCommand(String chatId) {
        StringBuilder help = new StringBuilder();
        help.append("📖 <b>Доступные команды:</b>\n\n");
        help.append("/start - Подписаться на уведомления\n");
        help.append("/stop - Отписаться от уведомлений\n");
        help.append("/register - Зарегистрировать новый аккаунт\n");
        help.append("/auth - Связать аккаунт через логин/пароль\n");
        help.append("/link &lt;token&gt; - Связать аккаунт через токен\n");
        help.append("/cancel - Отменить процесс регистрации/авторизации/разметки\n");
        help.append("/invite - Создать инвайт-код\n");
        help.append("/myinvites - Показать список моих инвайт-кодов\n");
        help.append("/mark - Разметить свободное время\n");
        help.append("/myslots - Показать размеченные временные слоты\n");
        help.append("/games - Получить список предстоящих игр\n");
        help.append("/upcoming - То же, что и /games\n");
        help.append("/game &lt;id&gt; - Посмотреть детали игры\n");
        help.append("/menu - Главное меню\n");
        help.append("/help - Показать эту справку\n\n");
        help.append("<b>Способы работы с аккаунтом:</b>\n");
        help.append("1. /register - регистрация нового аккаунта через Telegram\n");
        help.append("2. /auth - авторизация через логин и пароль для привязки существующего аккаунта\n");
        help.append("3. /link &lt;token&gt; - привязка через токен (получите токен в настройках профиля на веб-сайте)\n\n");
        help.append("<b>Управление инвайт-кодами:</b>\n");
        help.append("/invite - создать новый бессрочный одноразовый инвайт-код\n");
        help.append("/myinvites - просмотреть все созданные вами инвайт-коды\n\n");
        help.append("<b>Разметка времени:</b>\n");
        help.append("/mark - разметить свободное время (последовательный диалог: дата → время → продолжительность)\n");
        help.append("/myslots - просмотреть все размеченные временные слоты\n\n");
        help.append("💡 Используйте /menu для удобной навигации через меню.\n");
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
            
            // Обработка меню (префикс menu_)
            if (data.startsWith("menu_")) {
                handleMenuCallback(telegramUserId, chatId.toString(), messageId, data);
            } else if (data.startsWith("timezone_select_")) {
                String timezoneId = data.substring("timezone_select_".length());
                handleTimezoneSelectCallback(telegramUserId, chatId.toString(), messageId, timezoneId);
            } else if (data.equals("timezone_manual")) {
                handleTimezoneManualCallback(telegramUserId, chatId.toString(), messageId);
            } else if (data.equals("timezone_separator")) {
                // Игнорируем нажатие на разделитель (не делаем ничего)
                answerCallbackQuery(callbackQuery.getId());
                return;
            } else if (data.startsWith("join_game_")) {
                Long gameId = Long.parseLong(data.substring("join_game_".length()));
                handleJoinGameCallback(telegramUserId, chatId.toString(), messageId, gameId);
            } else if (data.startsWith("leave_game_")) {
                Long gameId = Long.parseLong(data.substring("leave_game_".length()));
                handleLeaveGameCallback(telegramUserId, chatId.toString(), messageId, gameId);
            } else if (data.startsWith("refresh_game_")) {
                Long gameId = Long.parseLong(data.substring("refresh_game_".length()));
                handleRefreshGameCallback(telegramUserId, chatId.toString(), messageId, gameId);
            } else if (data.startsWith("view_game_")) {
                Long gameId = Long.parseLong(data.substring("view_game_".length()));
                handleViewGameFromMenu(telegramUserId, chatId.toString(), messageId, gameId);
            }
        } catch (Exception e) {
            logger.error("Error handling callback query", e);
            answerCallbackQuery(callbackQuery.getId(), "❌ Произошла ошибка. Попробуйте позже.");
        }
    }
    
    private void handleMenuCallback(Long telegramUserId, String chatId, Integer messageId, String data) {
        try {
            User user = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
            boolean isLinked = user != null;
            
            if (data.equals("menu_main")) {
                // Возврат в главное меню
                String message = "📱 <b>Главное меню</b>\n\n";
                if (isLinked && user != null) {
                    message += "✅ Аккаунт связан\n";
                    message += "👤 Пользователь: " + escapeHtml(user.getUsername()) + "\n\n";
                } else {
                    message += "❌ Аккаунт не связан\n\n";
                }
                message += "Выберите раздел:";
                
                InlineKeyboardMarkup keyboard = buildMainMenuKeyboard(isLinked);
                updateMenuMessage(chatId, messageId, message, keyboard);
                
            } else if (data.equals("menu_register")) {
                // Регистрация через меню
                if (isLinked) {
                    answerCallbackQuery("", "✅ Вы уже зарегистрированы!");
                    return;
                }
                // Инициализируем регистрацию
                handleRegisterCommand(telegramUserId, chatId);
                // Возвращаемся в главное меню
                String menuMessage = "📱 <b>Главное меню</b>\n\n❌ Аккаунт не связан\n\nДля доступа ко всем функциям необходимо зарегистрироваться.\n\nНажмите кнопку ниже, чтобы начать регистрацию:";
                InlineKeyboardMarkup keyboard = buildMainMenuKeyboard(false);
                updateMenuMessage(chatId, messageId, menuMessage, keyboard);
                
            } else if (data.equals("menu_auth")) {
                // Авторизация через меню
                if (isLinked) {
                    answerCallbackQuery("", "✅ Ваш аккаунт уже связан!");
                    return;
                }
                // Инициализируем авторизацию
                handleAuthCommand(telegramUserId, chatId);
                // Возвращаемся в главное меню
                String menuMessage = "📱 <b>Главное меню</b>\n\n❌ Аккаунт не связан\n\nДля доступа ко всем функциям необходимо связать аккаунт.\n\nВыберите способ связывания:";
                InlineKeyboardMarkup keyboard = buildMainMenuKeyboard(false);
                updateMenuMessage(chatId, messageId, menuMessage, keyboard);
                
            } else if (data.equals("menu_link")) {
                // Связывание через токен через меню
                if (isLinked) {
                    answerCallbackQuery("", "✅ Ваш аккаунт уже связан!");
                    return;
                }
                // Отправляем инструкцию по использованию токена
                sendPersonalMessage(chatId, "🔗 <b>Связывание аккаунта через токен</b>\n\n" +
                        "Для связывания аккаунта через токен:\n\n" +
                        "1. Откройте настройки профиля на веб-сайте\n" +
                        "2. Получите токен для связывания Telegram\n" +
                        "3. Отправьте команду: <code>/link &lt;token&gt;</code>\n\n" +
                        "Например: <code>/link abc123xyz</code>\n\n" +
                        "💡 Используйте /cancel для отмены.");
                // Возвращаемся в главное меню
                String menuMessage = "📱 <b>Главное меню</b>\n\n❌ Аккаунт не связан\n\nДля доступа ко всем функциям необходимо связать аккаунт.\n\nВыберите способ связывания:";
                InlineKeyboardMarkup keyboard = buildMainMenuKeyboard(false);
                updateMenuMessage(chatId, messageId, menuMessage, keyboard);
                
            } else if (data.equals("menu_games")) {
                // Подменю игр
                if (!isLinked) {
                    answerCallbackQuery("", "❌ Аккаунт не связан. Зарегистрируйтесь для доступа к функциям.");
                    return;
                }
                String message = "🎮 <b>Игры</b>\n\nВыберите действие:";
                InlineKeyboardMarkup keyboard = buildGamesMenuKeyboard();
                updateMenuMessage(chatId, messageId, message, keyboard);
                
            } else if (data.equals("menu_time")) {
                // Подменю разметки времени
                if (!isLinked) {
                    answerCallbackQuery("", "❌ Аккаунт не связан. Используйте /link для связывания.");
                    return;
                }
                String message = "📅 <b>Разметка времени</b>\n\nВыберите действие:";
                InlineKeyboardMarkup keyboard = buildTimeMenuKeyboard();
                updateMenuMessage(chatId, messageId, message, keyboard);
                
            } else if (data.equals("menu_invites")) {
                // Подменю инвайтов
                if (!isLinked) {
                    answerCallbackQuery("", "❌ Аккаунт не связан. Используйте /link для связывания.");
                    return;
                }
                String message = "🎫 <b>Инвайты</b>\n\nВыберите действие:";
                InlineKeyboardMarkup keyboard = buildInvitesMenuKeyboard();
                updateMenuMessage(chatId, messageId, message, keyboard);
                
            } else if (data.equals("menu_settings")) {
                // Подменю настроек
                if (!isLinked) {
                    answerCallbackQuery("", "❌ Аккаунт не связан. Зарегистрируйтесь для доступа к функциям.");
                    return;
                }
                String message = "⚙️ <b>Настройки</b>\n\nВыберите действие:";
                InlineKeyboardMarkup keyboard = buildSettingsMenuKeyboard(isLinked);
                updateMenuMessage(chatId, messageId, message, keyboard);
                
            } else if (data.equals("menu_help")) {
                // Помощь
                if (!isLinked || user == null) {
                    answerCallbackQuery("", "❌ Аккаунт не связан. Зарегистрируйтесь для доступа к функциям.");
                    return;
                }
                handleHelpCommand(chatId);
                // Возвращаемся в главное меню
                String menuMessage = "📱 <b>Главное меню</b>\n\n✅ Аккаунт связан\n👤 Пользователь: " + escapeHtml(user.getUsername()) + "\n\nВыберите раздел:";
                InlineKeyboardMarkup keyboard = buildMainMenuKeyboard(isLinked);
                updateMenuMessage(chatId, messageId, menuMessage, keyboard);
                
            } else if (data.equals("menu_games_list")) {
                // Действие: список игр
                if (!isLinked) {
                    answerCallbackQuery("", "❌ Аккаунт не связан. Зарегистрируйтесь для доступа к функциям.");
                    return;
                }
                handleMenuGamesList(telegramUserId, chatId, messageId);
                
            } else if (data.startsWith("menu_games_page_")) {
                // Пагинация списка игр
                if (!isLinked) {
                    answerCallbackQuery("", "❌ Аккаунт не связан. Зарегистрируйтесь для доступа к функциям.");
                    return;
                }
                if (data.equals("menu_games_page_separator")) {
                    // Игнорируем нажатие на индикатор страницы
                    return;
                }
                int page = Integer.parseInt(data.substring("menu_games_page_".length()));
                handleMenuGamesList(telegramUserId, chatId, messageId, page);
                
            } else if (data.startsWith("view_game_")) {
                // Просмотр деталей игры
                if (!isLinked) {
                    answerCallbackQuery("", "❌ Аккаунт не связан. Зарегистрируйтесь для доступа к функциям.");
                    return;
                }
                Long gameId = Long.parseLong(data.substring("view_game_".length()));
                handleViewGameFromMenu(telegramUserId, chatId, messageId, gameId);
                
            } else if (data.equals("menu_time_mark")) {
                // Действие: разметка времени
                if (!isLinked) {
                    answerCallbackQuery("", "❌ Аккаунт не связан. Используйте /link для связывания.");
                    return;
                }
                handleMenuTimeMark(telegramUserId, chatId, messageId);
                
            } else if (data.equals("menu_time_slots")) {
                // Действие: мои слоты
                if (!isLinked) {
                    answerCallbackQuery("", "❌ Аккаунт не связан. Используйте /link для связывания.");
                    return;
                }
                handleMenuTimeSlots(telegramUserId, chatId, messageId);
                
            } else if (data.equals("menu_invites_create")) {
                // Действие: создать инвайт
                if (!isLinked) {
                    answerCallbackQuery("", "❌ Аккаунт не связан. Используйте /link для связывания.");
                    return;
                }
                handleMenuInvitesCreate(telegramUserId, chatId, messageId);
                
            } else if (data.equals("menu_invites_list")) {
                // Действие: список инвайтов
                if (!isLinked) {
                    answerCallbackQuery("", "❌ Аккаунт не связан. Используйте /link для связывания.");
                    return;
                }
                handleMenuInvitesList(telegramUserId, chatId, messageId);
                
            } else if (data.equals("menu_settings_profile")) {
                // Действие: профиль
                handleMenuSettingsProfile(telegramUserId, chatId, messageId);
                
            } else if (data.equals("menu_settings_timezone")) {
                // Действие: смена часового пояса
                if (!isLinked) {
                    answerCallbackQuery("", "❌ Аккаунт не связан. Зарегистрируйтесь для доступа к функциям.");
                    return;
                }
                handleMenuSettingsTimezone(telegramUserId, chatId, messageId);
                
            } else if (data.equals("menu_settings_notifications")) {
                // Действие: настройки уведомлений
                if (!isLinked) {
                    answerCallbackQuery("", "❌ Аккаунт не связан. Зарегистрируйтесь для доступа к функциям.");
                    return;
                }
                handleMenuNotifications(telegramUserId, chatId, messageId);
                
            } else if (data.startsWith("notification_set_")) {
                // Обработка изменения простых настроек уведомлений
                if (!isLinked) {
                    answerCallbackQuery("", "❌ Аккаунт не связан. Зарегистрируйтесь для доступа к функциям.");
                    return;
                }
                handleNotificationSettingChange(telegramUserId, chatId, messageId, data);
                
            } else if (data.equals("notification_reminders")) {
                // Показать список напоминаний о предстоящих играх
                if (!isLinked) {
                    answerCallbackQuery("", "❌ Аккаунт не связан. Зарегистрируйтесь для доступа к функциям.");
                    return;
                }
                handleMenuReminders(telegramUserId, chatId, messageId);
                
            } else if (data.equals("notification_reminder_add")) {
                // Начать добавление нового напоминания
                if (!isLinked) {
                    answerCallbackQuery("", "❌ Аккаунт не связан. Зарегистрируйтесь для доступа к функциям.");
                    return;
                }
                handleReminderAdd(telegramUserId, chatId, messageId);
                
            } else if (data.startsWith("notification_reminder_edit_")) {
                // Редактирование существующего напоминания
                if (!isLinked) {
                    answerCallbackQuery("", "❌ Аккаунт не связан. Зарегистрируйтесь для доступа к функциям.");
                    return;
                }
                int index = Integer.parseInt(data.substring("notification_reminder_edit_".length()));
                handleReminderEdit(telegramUserId, chatId, messageId, index);
                
            } else if (data.startsWith("notification_reminder_delete_")) {
                // Удаление напоминания
                if (!isLinked) {
                    answerCallbackQuery("", "❌ Аккаунт не связан. Зарегистрируйтесь для доступа к функциям.");
                    return;
                }
                int index = Integer.parseInt(data.substring("notification_reminder_delete_".length()));
                handleReminderDelete(telegramUserId, chatId, messageId, index);
                
            } else if (data.startsWith("notification_reminder_toggle_")) {
                // Включить/выключить напоминание
                if (!isLinked) {
                    answerCallbackQuery("", "❌ Аккаунт не связан. Зарегистрируйтесь для доступа к функциям.");
                    return;
                }
                int index = Integer.parseInt(data.substring("notification_reminder_toggle_".length()));
                handleReminderToggle(telegramUserId, chatId, messageId, index);
                
            } else if (data.equals("notification_timeslot_reminder")) {
                // Показать настройки напоминания о разметке времени
                if (!isLinked) {
                    answerCallbackQuery("", "❌ Аккаунт не связан. Зарегистрируйтесь для доступа к функциям.");
                    return;
                }
                handleMenuTimeSlotReminder(telegramUserId, chatId, messageId);
                
            } else if (data.equals("notification_timeslot_reminder_toggle")) {
                // Включить/выключить напоминание о разметке времени
                if (!isLinked) {
                    answerCallbackQuery("", "❌ Аккаунт не связан. Зарегистрируйтесь для доступа к функциям.");
                    return;
                }
                handleTimeSlotReminderToggle(telegramUserId, chatId, messageId);
                
            } else if (data.equals("notification_timeslot_reminder_cron")) {
                // Настройка cron для напоминания о разметке времени
                if (!isLinked) {
                    answerCallbackQuery("", "❌ Аккаунт не связан. Зарегистрируйтесь для доступа к функциям.");
                    return;
                }
                handleTimeSlotReminderCron(telegramUserId, chatId, messageId);
                
            } else if (data.startsWith("notification_cron_frequency_")) {
                // Выбор частоты cron
                if (!isLinked) {
                    answerCallbackQuery("", "❌ Аккаунт не связан. Зарегистрируйтесь для доступа к функциям.");
                    return;
                }
                String frequency = data.substring("notification_cron_frequency_".length());
                handleCronFrequencySelect(telegramUserId, chatId, messageId, frequency);
                
            } else if (data.startsWith("notification_cron_day_")) {
                // Выбор дня для cron
                if (!isLinked) {
                    answerCallbackQuery("", "❌ Аккаунт не связан. Зарегистрируйтесь для доступа к функциям.");
                    return;
                }
                int day = Integer.parseInt(data.substring("notification_cron_day_".length()));
                handleCronDaySelect(telegramUserId, chatId, messageId, day);
                
            } else if (data.startsWith("notification_reminder_unit_")) {
                // Выбор единицы для напоминания
                if (!isLinked) {
                    answerCallbackQuery("", "❌ Аккаунт не связан. Зарегистрируйтесь для доступа к функциям.");
                    return;
                }
                String unit = data.substring("notification_reminder_unit_".length());
                handleReminderUnitSelect(telegramUserId, chatId, messageId, unit);
            }
        } catch (Exception e) {
            logger.error("Error handling menu callback", e);
            answerCallbackQuery("", "❌ Произошла ошибка. Попробуйте позже.");
        }
    }
    
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
    
    private void handleMenuGamesList(Long telegramUserId, String chatId, Integer messageId) {
        handleMenuGamesList(telegramUserId, chatId, messageId, 0);
    }
    
    private void handleMenuGamesList(Long telegramUserId, String chatId, Integer messageId, int page) {
        try {
            User user = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
            
            if (user == null) {
                answerCallbackQuery("", "❌ Ваш аккаунт не связан. Используйте /link для связывания.");
                return;
            }
            
            List<GameDto> upcomingGames = gameService.getUpcomingGamesForUser(user.getId());
            
            // Сохраняем текущую страницу
            gamesListPage.put(chatId, page);
            
            String message;
            InlineKeyboardMarkup keyboard;
            
            if (upcomingGames.isEmpty()) {
                message = "📅 <b>Предстоящие игры</b>\n\nУ вас пока нет запланированных игр.";
                keyboard = buildGamesMenuKeyboard();
            } else {
                // Сортируем игры по времени начала
                List<GameDto> sortedGames = upcomingGames.stream()
                    .sorted(Comparator.comparing(GameDto::getStartTime))
                    .collect(Collectors.toList());
                
                int totalPages = (int) Math.ceil((double) sortedGames.size() / GAMES_PER_PAGE);
                if (page < 0) page = 0;
                if (page >= totalPages) page = totalPages - 1;
                
                message = buildUpcomingGamesListMessage(sortedGames, page, totalPages);
                keyboard = buildGamesListKeyboard(sortedGames, page, totalPages);
            }
            
            updateMenuMessage(chatId, messageId, message, keyboard);
        } catch (Exception e) {
            logger.error("Error handling menu games list", e);
            answerCallbackQuery("", "❌ Ошибка при получении списка игр.");
        }
    }
    
    private InlineKeyboardMarkup buildGamesListKeyboard(List<GameDto> games, int page, int totalPages) {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new java.util.ArrayList<>();
        
        int startIndex = page * GAMES_PER_PAGE;
        int endIndex = Math.min(startIndex + GAMES_PER_PAGE, games.size());
        List<GameDto> pageGames = games.subList(startIndex, endIndex);
        
        // Создаем кнопки для каждой игры на текущей странице
        for (int i = 0; i < pageGames.size(); i++) {
            GameDto game = pageGames.get(i);
            int globalIndex = startIndex + i;
            
            List<InlineKeyboardButton> gameRow = new java.util.ArrayList<>();
            InlineKeyboardButton gameButton = new InlineKeyboardButton();
            
            // Формируем текст кнопки: номер, название (или "Игра"), время
            String buttonText = (globalIndex + 1) + ". ";
            if (game.getTitle() != null && !game.getTitle().isEmpty()) {
                String title = game.getTitle();
                // Ограничиваем длину названия для кнопки (максимум ~30 символов)
                if (title.length() > 30) {
                    title = title.substring(0, 27) + "...";
                }
                buttonText += title;
            } else {
                buttonText += "Игра";
            }
            buttonText += " - " + formatInstant(game.getStartTime());
            
            gameButton.setText(buttonText);
            gameButton.setCallbackData("view_game_" + game.getId());
            gameRow.add(gameButton);
            rows.add(gameRow);
        }
        
        // Пагинация (если больше одной страницы)
        if (totalPages > 1) {
            List<InlineKeyboardButton> paginationRow = new java.util.ArrayList<>();
            
            // Кнопка "Предыдущая"
            if (page > 0) {
                InlineKeyboardButton prevButton = new InlineKeyboardButton();
                prevButton.setText("◀️ Предыдущая");
                prevButton.setCallbackData("menu_games_page_" + (page - 1));
                paginationRow.add(prevButton);
            }
            
            // Индикатор страницы
            InlineKeyboardButton pageButton = new InlineKeyboardButton();
            pageButton.setText((page + 1) + "/" + totalPages);
            pageButton.setCallbackData("menu_games_page_separator");
            paginationRow.add(pageButton);
            
            // Кнопка "Следующая"
            if (page < totalPages - 1) {
                InlineKeyboardButton nextButton = new InlineKeyboardButton();
                nextButton.setText("Следующая ▶️");
                nextButton.setCallbackData("menu_games_page_" + (page + 1));
                paginationRow.add(nextButton);
            }
            
            rows.add(paginationRow);
        }
        
        // Кнопка "Назад"
        List<InlineKeyboardButton> backRow = new java.util.ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("◀️ Назад");
        backButton.setCallbackData("menu_games");
        backRow.add(backButton);
        rows.add(backRow);
        
        keyboard.setKeyboard(rows);
        return keyboard;
    }
    
    private void handleMenuTimeMark(Long telegramUserId, String chatId, Integer messageId) {
        try {
            User user = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
            
            if (user == null) {
                answerCallbackQuery("", "❌ Ваш аккаунт не связан. Используйте /link для связывания.");
                return;
            }
            
            // Проверяем наличие часового пояса
            if (user.getTimezone() == null || user.getTimezone().trim().isEmpty()) {
                String errorMessage = "❌ <b>Часовой пояс не установлен</b>\n\n" +
                        "Для разметки времени необходимо установить часовой пояс.\n\n" +
                        "Вы можете установить часовой пояс:\n" +
                        "• Через меню: Настройки → Часовой пояс\n" +
                        "• В настройках профиля на веб-сайте\n\n" +
                        "После установки часового пояса вы сможете использовать разметку времени.";
                InlineKeyboardMarkup keyboard = buildTimeMenuKeyboard();
                updateMenuMessage(chatId, messageId, errorMessage, keyboard);
                return;
            }
            
            // Инициализируем состояние разметки времени
            timeSlotMarkingStates.put(chatId, TimeSlotMarkingState.WAITING_DATE);
            timeSlotMarkingData.put(chatId, new TimeSlotMarkingData());
            updateTimeSlotMarkingTimestamp(chatId);
            
            String message = "📅 <b>Разметка свободного времени</b>\n\n" +
                    "Введите дату в формате ДД.ММ.ГГГГ (например: 15.01.2025)\n" +
                    "Или используйте: сегодня, завтра, послезавтра\n\n" +
                    "💡 Используйте /cancel для отмены.";
            
            // Отправляем новое сообщение для диалога разметки
            sendPersonalMessage(chatId, message);
            
            // Возвращаемся в подменю разметки времени
            String menuMessage = "📅 <b>Разметка времени</b>\n\nВыберите действие:";
            InlineKeyboardMarkup keyboard = buildTimeMenuKeyboard();
            updateMenuMessage(chatId, messageId, menuMessage, keyboard);
        } catch (Exception e) {
            logger.error("Error handling menu time mark", e);
            answerCallbackQuery("", "❌ Ошибка при инициализации разметки времени.");
        }
    }
    
    private void handleMenuTimeSlots(Long telegramUserId, String chatId, Integer messageId) {
        try {
            User user = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
            
            if (user == null) {
                answerCallbackQuery("", "❌ Ваш аккаунт не связан. Используйте /link для связывания.");
                return;
            }
            
            Instant now = Instant.now();
            Instant endDate = now.plusSeconds(30L * 24 * 60 * 60); // 30 дней вперед
            ru.ambryo.gameplannerback.dto.PlayerDto player = userService.getUserAsPlayerWithTimeSlots(user, now, endDate);
            
            List<ru.ambryo.gameplannerback.dto.TimeSlotDto> slots = player.getAvailableTimes();
            
            String message;
            InlineKeyboardMarkup keyboard = buildTimeMenuKeyboard();
            
            if (slots == null || slots.isEmpty()) {
                message = "📅 <b>Мои временные слоты</b>\n\n" +
                        "У вас пока нет размеченного времени.\n\n" +
                        "Используйте кнопку 'Разметить время' для добавления слотов.";
            } else {
                message = buildMySlotsListMessage(slots, user.getTimezone());
            }
            
            updateMenuMessage(chatId, messageId, message, keyboard);
        } catch (Exception e) {
            logger.error("Error handling menu time slots", e);
            answerCallbackQuery("", "❌ Ошибка при получении списка слотов.");
        }
    }
    
    private void handleMenuInvitesCreate(Long telegramUserId, String chatId, Integer messageId) {
        try {
            User user = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
            
            if (user == null) {
                answerCallbackQuery("", "❌ Ваш аккаунт не связан. Используйте /link для связывания.");
                return;
            }
            
            // Создаем бессрочный одноразовый инвайт-код
            ru.ambryo.gameplannerback.dto.CreateInviteRequest request = 
                    new ru.ambryo.gameplannerback.dto.CreateInviteRequest(null, 1);
            
            ru.ambryo.gameplannerback.dto.InviteDto invite = inviteService.createInvite(user, request);
            
            String message = buildInviteCreatedMessage(invite);
            InlineKeyboardMarkup keyboard = buildInvitesMenuKeyboard();
            
            updateMenuMessage(chatId, messageId, message, keyboard);
            
            logger.info("Invite code created via menu for user: {}, chatId: {}", user.getUsername(), chatId);
        } catch (Exception e) {
            logger.error("Error handling menu invites create", e);
            answerCallbackQuery("", "❌ Ошибка при создании инвайт-кода.");
        }
    }
    
    private void handleMenuInvitesList(Long telegramUserId, String chatId, Integer messageId) {
        try {
            User user = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
            
            if (user == null) {
                answerCallbackQuery("", "❌ Ваш аккаунт не связан. Используйте /link для связывания.");
                return;
            }
            
            List<ru.ambryo.gameplannerback.dto.InviteDto> invites = inviteService.getMyInvites(user);
            
            String message;
            InlineKeyboardMarkup keyboard = buildInvitesMenuKeyboard();
            
            if (invites.isEmpty()) {
                message = "📋 <b>Мои инвайт-коды</b>\n\n" +
                        "У вас пока нет созданных инвайт-кодов.\n\n" +
                        "Используйте кнопку 'Создать инвайт-код' для создания нового.";
            } else {
                message = buildMyInvitesListMessage(invites);
            }
            
            updateMenuMessage(chatId, messageId, message, keyboard);
        } catch (Exception e) {
            logger.error("Error handling menu invites list", e);
            answerCallbackQuery("", "❌ Ошибка при получении списка инвайт-кодов.");
        }
    }
    
    private void handleMenuSettingsProfile(Long telegramUserId, String chatId, Integer messageId) {
        try {
            User user = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
            
            StringBuilder message = new StringBuilder();
            message.append("👤 <b>Профиль</b>\n\n");
            
            if (user == null) {
                message.append("❌ Аккаунт не связан\n\n");
                message.append("Для доступа ко всем функциям необходимо связать аккаунт:\n");
                message.append("• /register - регистрация нового аккаунта\n");
                message.append("• /auth - авторизация через логин/пароль\n");
                message.append("• /link <token> - привязка через токен");
            } else {
                message.append("✅ Аккаунт связан\n\n");
                message.append("👤 <b>Логин:</b> ").append(escapeHtml(user.getUsername())).append("\n");
                message.append("📝 <b>Имя:</b> ").append(escapeHtml(user.getName() != null ? user.getName() : "Не указано")).append("\n");
                message.append("📧 <b>Email:</b> ").append(escapeHtml(user.getEmail() != null ? user.getEmail() : "Не указан")).append("\n");
                
                if (user.getTimezone() != null && !user.getTimezone().trim().isEmpty()) {
                    message.append("🌍 <b>Часовой пояс:</b> ").append(escapeHtml(user.getTimezone())).append("\n");
                } else {
                    message.append("🌍 <b>Часовой пояс:</b> Не установлен\n");
                    message.append("⚠️ Установите часовой пояс для использования разметки времени");
                }
            }
            
            InlineKeyboardMarkup keyboard = buildSettingsMenuKeyboard(user != null);
            updateMenuMessage(chatId, messageId, message.toString(), keyboard);
        } catch (Exception e) {
            logger.error("Error handling menu settings profile", e);
            answerCallbackQuery("", "❌ Ошибка при получении информации о профиле.");
        }
    }
    
    private void handleMenuSettingsTimezone(Long telegramUserId, String chatId, Integer messageId) {
        try {
            User user = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
            
            if (user == null) {
                answerCallbackQuery("", "❌ Ваш аккаунт не связан. Используйте /link для связывания.");
                return;
            }
            
            String currentTimezone = user.getTimezone() != null && !user.getTimezone().trim().isEmpty() 
                    ? user.getTimezone() 
                    : "Не установлен";
            
            String message = "🌍 <b>Смена часового пояса</b>\n\n" +
                    "Текущий часовой пояс: <b>" + escapeHtml(currentTimezone) + "</b>\n\n" +
                    "Выберите новый часовой пояс из списка:";
            
            InlineKeyboardMarkup keyboard = buildTimezoneSelectorKeyboard(user.getTimezone());
            updateMenuMessage(chatId, messageId, message, keyboard);
        } catch (Exception e) {
            logger.error("Error handling menu settings timezone", e);
            answerCallbackQuery("", "❌ Ошибка при инициализации смены часового пояса.");
        }
    }
    
    private InlineKeyboardMarkup buildTimezoneSelectorKeyboard(String currentTimezone) {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new java.util.ArrayList<>();
        
        // Популярные часовые пояса России (по 2 в ряд)
        String[][] russianTimezones = {
            {"Europe/Moscow", "Москва"},
            {"Europe/Kaliningrad", "Калининград"},
            {"Europe/Samara", "Самара"},
            {"Asia/Yekaterinburg", "Екатеринбург"},
            {"Asia/Omsk", "Омск"},
            {"Asia/Krasnoyarsk", "Красноярск"},
            {"Asia/Irkutsk", "Иркутск"},
            {"Asia/Yakutsk", "Якутск"},
            {"Asia/Vladivostok", "Владивосток"},
            {"Asia/Magadan", "Магадан"},
            {"Asia/Kamchatka", "Камчатка"}
        };
        
        for (int i = 0; i < russianTimezones.length; i += 2) {
            List<InlineKeyboardButton> row = new java.util.ArrayList<>();
            
            // Первая кнопка в ряду
            InlineKeyboardButton button1 = new InlineKeyboardButton();
            String timezone1 = russianTimezones[i][0];
            String label1 = russianTimezones[i][1];
            String display1 = label1;
            if (timezone1.equals(currentTimezone)) {
                display1 = "✓ " + label1;
            }
            button1.setText(display1);
            button1.setCallbackData("timezone_select_" + timezone1);
            row.add(button1);
            
            // Вторая кнопка в ряду (если есть)
            if (i + 1 < russianTimezones.length) {
                InlineKeyboardButton button2 = new InlineKeyboardButton();
                String timezone2 = russianTimezones[i + 1][0];
                String label2 = russianTimezones[i + 1][1];
                String display2 = label2;
                if (timezone2.equals(currentTimezone)) {
                    display2 = "✓ " + label2;
                }
                button2.setText(display2);
                button2.setCallbackData("timezone_select_" + timezone2);
                row.add(button2);
            }
            
            rows.add(row);
        }
        
        // Разделитель
        List<InlineKeyboardButton> separatorRow = new java.util.ArrayList<>();
        InlineKeyboardButton separatorButton = new InlineKeyboardButton();
        separatorButton.setText("━━━━━━━━━━━━━━━━");
        separatorButton.setCallbackData("timezone_separator");
        separatorRow.add(separatorButton);
        rows.add(separatorRow);
        
        // Популярные часовые пояса других стран (по 2 в ряд)
        String[][] otherTimezones = {
            {"Europe/London", "Лондон"},
            {"Europe/Berlin", "Берлин"},
            {"Europe/Paris", "Париж"},
            {"America/New_York", "Нью-Йорк"},
            {"America/Chicago", "Чикаго"},
            {"America/Los_Angeles", "Лос-Анджелес"},
            {"Asia/Tokyo", "Токио"},
            {"Asia/Shanghai", "Шанхай"},
            {"Asia/Dubai", "Дубай"},
            {"Australia/Sydney", "Сидней"}
        };
        
        for (int i = 0; i < otherTimezones.length; i += 2) {
            List<InlineKeyboardButton> row = new java.util.ArrayList<>();
            
            // Первая кнопка в ряду
            InlineKeyboardButton button1 = new InlineKeyboardButton();
            String timezone1 = otherTimezones[i][0];
            String label1 = otherTimezones[i][1];
            String display1 = label1;
            if (timezone1.equals(currentTimezone)) {
                display1 = "✓ " + label1;
            }
            button1.setText(display1);
            button1.setCallbackData("timezone_select_" + timezone1);
            row.add(button1);
            
            // Вторая кнопка в ряду (если есть)
            if (i + 1 < otherTimezones.length) {
                InlineKeyboardButton button2 = new InlineKeyboardButton();
                String timezone2 = otherTimezones[i + 1][0];
                String label2 = otherTimezones[i + 1][1];
                String display2 = label2;
                if (timezone2.equals(currentTimezone)) {
                    display2 = "✓ " + label2;
                }
                button2.setText(display2);
                button2.setCallbackData("timezone_select_" + timezone2);
                row.add(button2);
            }
            
            rows.add(row);
        }
        
        // Кнопка "Ввести вручную"
        List<InlineKeyboardButton> manualRow = new java.util.ArrayList<>();
        InlineKeyboardButton manualButton = new InlineKeyboardButton();
        manualButton.setText("✏️ Ввести вручную");
        manualButton.setCallbackData("timezone_manual");
        manualRow.add(manualButton);
        rows.add(manualRow);
        
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
    
    private void handleTimezoneSelectCallback(Long telegramUserId, String chatId, Integer messageId, String timezoneId) {
        try {
            User user = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
            
            if (user == null) {
                answerCallbackQuery("", "❌ Ваш аккаунт не связан. Используйте /link для связывания.");
                return;
            }
            
            // Проверяем валидность часового пояса
            ZoneId zoneId;
            try {
                zoneId = ZoneId.of(timezoneId);
            } catch (Exception e) {
                answerCallbackQuery("", "❌ Неверный часовой пояс");
                logger.error("Invalid timezone selected: {}", timezoneId, e);
                return;
            }
            
            // Обновляем часовой пояс пользователя
            userService.updateUserProfile(user, user.getName(), user.getColor(), zoneId.getId());
            
            // Обновляем сообщение с подтверждением
            String message = "✅ <b>Часовой пояс успешно изменен!</b>\n\n" +
                    "Новый часовой пояс: <b>" + escapeHtml(zoneId.getId()) + "</b>\n\n" +
                    "Теперь вы можете использовать разметку времени через /mark";
            
            InlineKeyboardMarkup keyboard = buildSettingsMenuKeyboard(true);
            updateMenuMessage(chatId, messageId, message, keyboard);
            
            answerCallbackQuery("", "✅ Часовой пояс изменен!");
            
            logger.info("Timezone changed via Telegram for user: {}, new timezone: {}", user.getUsername(), zoneId.getId());
        } catch (Exception e) {
            logger.error("Error handling timezone select callback", e);
            answerCallbackQuery("", "❌ Ошибка при смене часового пояса");
        }
    }
    
    private void handleTimezoneManualCallback(Long telegramUserId, String chatId, Integer messageId) {
        try {
            User user = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
            
            if (user == null) {
                answerCallbackQuery("", "❌ Ваш аккаунт не связан. Используйте /link для связывания.");
                return;
            }
            
            // Инициализируем состояние смены часового пояса для ручного ввода
            timezoneChangeStates.put(chatId, TimezoneChangeState.WAITING_TIMEZONE);
            updateTimezoneChangeTimestamp(chatId);
            
            String currentTimezone = user.getTimezone() != null && !user.getTimezone().trim().isEmpty() 
                    ? user.getTimezone() 
                    : "Не установлен";
            
            String message = "🌍 <b>Смена часового пояса</b>\n\n" +
                    "Текущий часовой пояс: <b>" + escapeHtml(currentTimezone) + "</b>\n\n" +
                    "Введите новый часовой пояс в формате IANA (например: Europe/Moscow, America/New_York, Asia/Tokyo)\n\n" +
                    "💡 Используйте /cancel для отмены.\n" +
                    "💡 Полный список: https://en.wikipedia.org/wiki/List_of_tz_database_time_zones";
            
            // Отправляем новое сообщение для диалога
            sendPersonalMessage(chatId, message);
            
            // Возвращаемся в подменю настроек
            String menuMessage = "⚙️ <b>Настройки</b>\n\nВыберите действие:";
            InlineKeyboardMarkup keyboard = buildSettingsMenuKeyboard(true);
            updateMenuMessage(chatId, messageId, menuMessage, keyboard);
        } catch (Exception e) {
            logger.error("Error handling timezone manual callback", e);
            answerCallbackQuery("", "❌ Ошибка при инициализации ручного ввода");
        }
    }
    
    private void handleTimezoneChangeState(Long telegramUserId, String chatId, String text, TimezoneChangeState state) {
        try {
            if (state == TimezoneChangeState.WAITING_TIMEZONE) {
                updateTimezoneChangeTimestamp(chatId);
                
                User user = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
                if (user == null) {
                    clearTimezoneChangeState(chatId);
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
                clearTimezoneChangeState(chatId);
                
                // Отправляем подтверждение
                sendPersonalMessage(chatId, "✅ <b>Часовой пояс успешно изменен!</b>\n\n" +
                        "Новый часовой пояс: <b>" + escapeHtml(zoneId.getId()) + "</b>\n\n" +
                        "Теперь вы можете использовать разметку времени через /mark");
                
                logger.info("Timezone changed via Telegram (manual) for user: {}, new timezone: {}", user.getUsername(), zoneId.getId());
            }
        } catch (Exception e) {
            logger.error("Error handling timezone change state", e);
            clearTimezoneChangeState(chatId);
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
    
    private void handleJoinGameCallback(Long telegramUserId, String chatId, Integer messageId, Long gameId) {
        try {
            User user = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
            
            if (user == null) {
                answerCallbackQuery("", "❌ Ваш аккаунт не связан. Используйте /link для связывания.");
                return;
            }
            
            GameDto game = gameService.joinGame(gameId, user);
            String message = buildGameDetailsMessage(game, user);
            // Используем клавиатуру с кнопкой "Назад", если игра открыта из меню
            InlineKeyboardMarkup keyboard = gamesListPage.containsKey(chatId) 
                ? buildGameKeyboardWithBack(game, user, chatId) 
                : buildGameKeyboard(game, user);
            
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
            // Используем клавиатуру с кнопкой "Назад", если игра открыта из меню
            InlineKeyboardMarkup keyboard = gamesListPage.containsKey(chatId) 
                ? buildGameKeyboardWithBack(game, user, chatId) 
                : buildGameKeyboard(game, user);
            
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
            // Используем клавиатуру с кнопкой "Назад", если игра открыта из меню
            InlineKeyboardMarkup keyboard = gamesListPage.containsKey(chatId) 
                ? buildGameKeyboardWithBack(game, user, chatId) 
                : buildGameKeyboard(game, user);
            
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
    
    private void handleViewGameFromMenu(Long telegramUserId, String chatId, Integer messageId, Long gameId) {
        try {
            User user = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
            
            if (user == null) {
                answerCallbackQuery("", "❌ Ваш аккаунт не связан. Используйте /link для связывания.");
                return;
            }
            
            GameDto game = gameService.getGameById(gameId);
            String message = buildGameDetailsMessage(game, user);
            InlineKeyboardMarkup keyboard = buildGameKeyboardWithBack(game, user, chatId);
            
            updateMenuMessage(chatId, messageId, message, keyboard);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                answerCallbackQuery("", "❌ Игра не найдена.");
            } else {
                answerCallbackQuery("", "❌ Ошибка: " + e.getMessage());
            }
            logger.error("Error handling view game from menu", e);
        } catch (Exception e) {
            answerCallbackQuery("", "❌ Произошла ошибка при получении информации об игре.");
            logger.error("Error handling view game from menu", e);
        }
    }
    
    private InlineKeyboardMarkup buildGameKeyboardWithBack(GameDto game, User user, String chatId) {
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
        
        // Кнопка "Назад к списку"
        List<InlineKeyboardButton> backRow = new java.util.ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("◀️ Назад к списку");
        // Получаем текущую страницу или используем 0
        int currentPage = gamesListPage.getOrDefault(chatId, 0);
        backButton.setCallbackData("menu_games_page_" + currentPage);
        backRow.add(backButton);
        rows.add(backRow);
        
        keyboard.setKeyboard(rows);
        return keyboard;
    }
    
    private String buildUpcomingGamesListMessage(List<GameDto> games, int page, int totalPages) {
        StringBuilder message = new StringBuilder();
        message.append("📅 <b>Предстоящие игры</b>\n\n");
        
        if (games.isEmpty()) {
            message.append("У вас пока нет запланированных игр.");
            return message.toString();
        }
        
        // Сортируем игры по времени начала
        List<GameDto> sortedGames = games.stream()
            .sorted(Comparator.comparing(GameDto::getStartTime))
            .collect(Collectors.toList());
        
        int startIndex = page * GAMES_PER_PAGE;
        int endIndex = Math.min(startIndex + GAMES_PER_PAGE, sortedGames.size());
        List<GameDto> pageGames = sortedGames.subList(startIndex, endIndex);
        
        message.append("Всего игр: ").append(sortedGames.size());
        if (totalPages > 1) {
            message.append(" (страница ").append(page + 1).append(" из ").append(totalPages).append(")");
        }
        message.append("\n\n");
        
        for (int i = 0; i < pageGames.size(); i++) {
            GameDto game = pageGames.get(i);
            int globalIndex = startIndex + i;
            
            message.append("🎮 <b>").append(globalIndex + 1).append(".</b> ");
            
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
            
            if (i < pageGames.size() - 1) {
                message.append("\n");
            }
        }
        
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
    
    private String buildInviteCreatedMessage(ru.ambryo.gameplannerback.dto.InviteDto invite) {
        StringBuilder message = new StringBuilder();
        message.append("🎫 <b>Инвайт-код создан!</b>\n\n");
        message.append("📋 <b>Код:</b> <code>").append(escapeHtml(invite.getCode())).append("</code>\n\n");
        
        // Статус инвайта
        if (invite.getExpiresAt() == null) {
            message.append("⏰ <b>Срок действия:</b> Бессрочный\n");
        } else {
            message.append("⏰ <b>Срок действия:</b> До ").append(formatInstant(invite.getExpiresAt())).append("\n");
        }
        
        if (invite.getMaxUses() != null) {
            message.append("🔢 <b>Использований:</b> ").append(invite.getUsesCount() != null ? invite.getUsesCount() : 0)
                    .append("/").append(invite.getMaxUses()).append("\n");
        } else {
            message.append("🔢 <b>Использований:</b> Неограниченно\n");
        }
        
        message.append("\n💡 Отправьте этот код другу для регистрации.\n");
        message.append("💡 Используйте /myinvites для просмотра всех ваших инвайт-кодов.");
        
        return message.toString();
    }
    
    private String buildMyInvitesListMessage(List<ru.ambryo.gameplannerback.dto.InviteDto> invites) {
        StringBuilder message = new StringBuilder();
        message.append("📋 <b>Мои инвайт-коды</b>\n\n");
        message.append("Всего: ").append(invites.size()).append("\n\n");
        
        for (int i = 0; i < invites.size(); i++) {
            ru.ambryo.gameplannerback.dto.InviteDto invite = invites.get(i);
            
            message.append("<b>").append(i + 1).append(".</b> ");
            message.append("<code>").append(escapeHtml(invite.getCode())).append("</code>\n");
            
            // Статус
            if (invite.getIsValid() != null && invite.getIsValid()) {
                message.append("✅ Действителен\n");
            } else {
                message.append("❌ Недействителен\n");
            }
            
            // Дата создания
            if (invite.getCreatedAt() != null) {
                message.append("📅 Создан: ").append(formatInstant(invite.getCreatedAt())).append("\n");
            }
            
            // Срок действия
            if (invite.getExpiresAt() == null) {
                message.append("⏰ Бессрочный\n");
            } else {
                message.append("⏰ Действителен до: ").append(formatInstant(invite.getExpiresAt())).append("\n");
            }
            
            // Использования
            if (invite.getMaxUses() != null) {
                message.append("🔢 Использований: ").append(invite.getUsesCount() != null ? invite.getUsesCount() : 0)
                        .append("/").append(invite.getMaxUses()).append("\n");
            } else {
                message.append("🔢 Использований: ").append(invite.getUsesCount() != null ? invite.getUsesCount() : 0)
                        .append(" (неограниченно)\n");
            }
            
            // Использован
            if (invite.getUsed() != null && invite.getUsed()) {
                if (invite.getUsedByName() != null) {
                    message.append("👤 Использован: ").append(escapeHtml(invite.getUsedByName())).append("\n");
                }
                if (invite.getUsedAt() != null) {
                    message.append("🕐 Дата использования: ").append(formatInstant(invite.getUsedAt())).append("\n");
                }
            }
            
            if (i < invites.size() - 1) {
                message.append("\n");
            }
        }
        
        message.append("\n💡 Используйте /invite для создания нового инвайт-кода.");
        
        return message.toString();
    }
    
    private String formatLocalDate(LocalDate date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        return date.format(formatter);
    }
    
    private String formatLocalTime(LocalTime time) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return time.format(formatter);
    }
    
    private String formatInstantInTimezone(Instant instant, String timezone) {
        try {
            ZoneId zoneId = ZoneId.of(timezone);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                    .withZone(zoneId);
            return formatter.format(instant);
        } catch (Exception e) {
            // Fallback к общему формату
            return formatInstant(instant);
        }
    }
    
    private String buildTimeSlotMarkedMessage(LocalDate localDate, LocalTime localTime, Integer duration, ZoneId userTimezone) {
        StringBuilder message = new StringBuilder();
        message.append("✅ <b>Временной слот размечен!</b>\n\n");
        message.append("📅 <b>Дата:</b> ").append(formatLocalDate(localDate)).append("\n");
        message.append("🕐 <b>Время:</b> ").append(formatLocalTime(localTime)).append("\n");
        message.append("⏱️ <b>Продолжительность:</b> ").append(duration).append(" ").append(duration == 1 ? "час" : "часа").append("\n\n");
        message.append("💡 Используйте /myslots для просмотра всех размеченных слотов.\n");
        message.append("💡 Повторная разметка того же времени удалит слот.");
        
        return message.toString();
    }
    
    private String buildMySlotsListMessage(List<ru.ambryo.gameplannerback.dto.TimeSlotDto> slots, String userTimezone) {
        StringBuilder message = new StringBuilder();
        message.append("📅 <b>Мои временные слоты</b>\n\n");
        message.append("Всего: ").append(slots.size()).append("\n\n");
        
        if (slots.isEmpty()) {
            message.append("У вас пока нет размеченного времени.\n\n");
            message.append("💡 Используйте /mark для разметки свободного времени.");
            return message.toString();
        }
        
        for (int i = 0; i < slots.size(); i++) {
            ru.ambryo.gameplannerback.dto.TimeSlotDto slot = slots.get(i);
            
            message.append("<b>").append(i + 1).append(".</b> ");
            
            // Форматируем время в часовом поясе пользователя
            String startTime = formatInstantInTimezone(slot.getStart(), userTimezone);
            Instant endTime = slot.getStart().plusSeconds(slot.getDuration() * 3600L);
            String endTimeStr = formatInstantInTimezone(endTime, userTimezone);
            
            message.append(startTime).append(" - ").append(endTimeStr).append("\n");
            message.append("⏱️ Продолжительность: ").append(slot.getDuration()).append(" ").append(slot.getDuration() == 1 ? "час" : "часа").append("\n");
            
            if (i < slots.size() - 1) {
                message.append("\n");
            }
        }
        
        message.append("\n💡 Используйте /mark для разметки нового времени.\n");
        message.append("💡 Повторная разметка того же времени удалит слот.");
        
        return message.toString();
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
            notificationStates.put(chatId, NotificationState.WAITING_REMINDER_VALUE);
            NotificationData data = new NotificationData();
            data.reminderIndex = -1; // -1 означает новое напоминание
            notificationData.put(chatId, data);
            updateNotificationTimestamp(chatId);
            
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
            notificationStates.put(chatId, NotificationState.WAITING_REMINDER_VALUE);
            NotificationData data = new NotificationData();
            data.reminderIndex = index;
            UpcomingGameReminderDto reminder = reminders.get(index);
            data.reminderValue = reminder.getMinutesBefore();
            notificationData.put(chatId, data);
            updateNotificationTimestamp(chatId);
            
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
            
            NotificationData data = notificationData.get(chatId);
            if (data == null) {
                clearNotificationState(chatId);
                sendPersonalMessage(chatId, "❌ Ошибка: данные не найдены. Начните заново.");
                return;
            }
            
            data.reminderUnit = unit;
            notificationData.put(chatId, data);
            
            // Переходим к вопросу о включении/выключении
            notificationStates.put(chatId, NotificationState.WAITING_REMINDER_UNIT); // Используем это состояние для финального подтверждения
            updateNotificationTimestamp(chatId);
            
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
            notificationStates.put(chatId, NotificationState.WAITING_CRON_FREQUENCY);
            NotificationData data = new NotificationData();
            if (settings.getTimeSlotReminderCron() != null && !settings.getTimeSlotReminderCron().trim().isEmpty()) {
                // Парсим существующий cron
                parseCronToData(settings.getTimeSlotReminderCron(), data);
            }
            notificationData.put(chatId, data);
            updateNotificationTimestamp(chatId);
            
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
            
            NotificationData data = notificationData.get(chatId);
            if (data == null) {
                clearNotificationState(chatId);
                sendPersonalMessage(chatId, "❌ Ошибка: данные не найдены. Начните заново.");
                return;
            }
            
            data.cronFrequency = frequency;
            notificationData.put(chatId, data);
            
            if ("daily".equals(frequency)) {
                // Для ежедневного - сразу переходим к времени
                notificationStates.put(chatId, NotificationState.WAITING_CRON_TIME);
                updateNotificationTimestamp(chatId);
                
                String message = "✅ Частота выбрана: <b>Ежедневно</b>\n\n" +
                        "Введите время в формате ЧЧ:ММ (например: 09:00):\n\n" +
                        "💡 Используйте /cancel для отмены.";
                
                sendPersonalMessage(chatId, message);
            } else if ("weekly".equals(frequency)) {
                // Для еженедельного - выбираем день недели
                notificationStates.put(chatId, NotificationState.WAITING_CRON_DAY);
                updateNotificationTimestamp(chatId);
                
                String message = "✅ Частота выбрана: <b>Еженедельно</b>\n\n" +
                        "Выберите день недели:";
                
                InlineKeyboardMarkup keyboard = buildDayOfWeekKeyboard();
                updateMenuMessage(chatId, messageId, message, keyboard);
            } else if ("monthly".equals(frequency)) {
                // Для ежемесячного - вводим день месяца
                notificationStates.put(chatId, NotificationState.WAITING_CRON_DAY);
                updateNotificationTimestamp(chatId);
                
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
            
            NotificationData data = notificationData.get(chatId);
            if (data == null) {
                clearNotificationState(chatId);
                sendPersonalMessage(chatId, "❌ Ошибка: данные не найдены. Начните заново.");
                return;
            }
            
            data.cronDay = day;
            notificationData.put(chatId, data);
            notificationStates.put(chatId, NotificationState.WAITING_CRON_TIME);
            updateNotificationTimestamp(chatId);
            
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
    
    private void handleNotificationState(Long telegramUserId, String chatId, String text, NotificationState state) {
        try {
            updateNotificationTimestamp(chatId);
            
            User user = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
            if (user == null) {
                clearNotificationState(chatId);
                sendPersonalMessage(chatId, "❌ Ваш аккаунт не связан. Используйте /link для связывания.");
                return;
            }
            
            NotificationData data = notificationData.get(chatId);
            if (data == null) {
                clearNotificationState(chatId);
                sendPersonalMessage(chatId, "❌ Ошибка: данные не найдены. Начните заново.");
                return;
            }
            
            if (state == NotificationState.WAITING_REMINDER_VALUE) {
                // Ожидание значения напоминания
                try {
                    int value = Integer.parseInt(text.trim());
                    if (value <= 0) {
                        sendPersonalMessage(chatId, "❌ Значение должно быть положительным числом. Введите значение:");
                        return;
                    }
                    
                    data.reminderValue = value;
                    notificationData.put(chatId, data);
                    
                    // Переходим к выбору единицы
                    notificationStates.put(chatId, NotificationState.WAITING_REMINDER_UNIT);
                    updateNotificationTimestamp(chatId);
                    
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
                
            } else if (state == NotificationState.WAITING_REMINDER_UNIT) {
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
                        clearNotificationState(chatId);
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
                        clearNotificationState(chatId);
                        sendPersonalMessage(chatId, "❌ Напоминание не найдено.");
                        return;
                    }
                }
                
                settings.setUpcomingGameReminders(reminders);
                notificationSettingsService.updateSettings(user.getId(), settings);
                
                clearNotificationState(chatId);
                
                String displayValue = formatReminderValue(minutesBefore);
                sendPersonalMessage(chatId, "✅ <b>Напоминание " + (data.reminderIndex == -1 ? "добавлено" : "изменено") + "!</b>\n\n" +
                        "Значение: <b>" + escapeHtml(displayValue) + "</b>\n" +
                        "Статус: <b>" + (enabled ? "Включено" : "Выключено") + "</b>");
                
                logger.info("Reminder {} via Telegram for user: {}, value: {} minutes, enabled: {}", 
                        data.reminderIndex == -1 ? "added" : "updated", user.getUsername(), minutesBefore, enabled);
                
            } else if (state == NotificationState.WAITING_CRON_TIME) {
                // Ожидание времени для cron
                LocalTime time = parseTime(text.trim());
                if (time == null) {
                    sendPersonalMessage(chatId, "❌ <b>Неверный формат времени</b>\n\n" +
                            "Введите время в формате ЧЧ:ММ (например: 09:00):\n\n" +
                            "💡 Используйте /cancel для отмены.");
                    return;
                }
                
                data.cronTime = String.format("%02d:%02d", time.getHour(), time.getMinute());
                notificationData.put(chatId, data);
                
                // Сохраняем cron
                String cron = buildCronFromData(data);
                UserNotificationSettingsDto settings = notificationSettingsService.getSettings(user.getId());
                settings.setTimeSlotReminderCron(cron);
                notificationSettingsService.updateSettings(user.getId(), settings);
                
                clearNotificationState(chatId);
                
                String cronText = formatCronToReadable(cron);
                sendPersonalMessage(chatId, "✅ <b>Расписание настроено!</b>\n\n" +
                        "Расписание: <b>" + escapeHtml(cronText) + "</b>");
                
                logger.info("Time slot reminder cron updated via Telegram for user: {}, cron: {}", user.getUsername(), cron);
                
            } else if (state == NotificationState.WAITING_CRON_DAY && data.cronFrequency != null && "monthly".equals(data.cronFrequency)) {
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
                    notificationData.put(chatId, data);
                    notificationStates.put(chatId, NotificationState.WAITING_CRON_TIME);
                    updateNotificationTimestamp(chatId);
                    
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
            clearNotificationState(chatId);
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
    
    private void parseCronToData(String cron, NotificationData data) {
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
    
    private String buildCronFromData(NotificationData data) {
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
