package ru.ambryo.gameplannerback.service.telegram.state.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.bots.AbsSender;
import ru.ambryo.gameplannerback.entity.User;
import ru.ambryo.gameplannerback.repository.UserRepository;
import ru.ambryo.gameplannerback.service.UserService;
import ru.ambryo.gameplannerback.service.telegram.message.TimeSlotMessageBuilder;
import ru.ambryo.gameplannerback.service.telegram.state.TimeSlotMarkingStateManager;
import ru.ambryo.gameplannerback.service.telegram.util.TelegramDateParser;
import ru.ambryo.gameplannerback.service.telegram.util.TelegramMessageSender;
import ru.ambryo.gameplannerback.service.telegram.util.TelegramTimeFormatter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Set;

/**
 * Обработчик состояний разметки времени
 */
@Component
public class TimeSlotMarkingStateHandler implements StateHandler<TimeSlotMarkingStateManager.TimeSlotMarkingState> {
    
    private static final Logger logger = LoggerFactory.getLogger(TimeSlotMarkingStateHandler.class);
    
    private final TimeSlotMarkingStateManager timeSlotMarkingStateManager;
    private final UserRepository userRepository;
    private final UserService userService;
    private final TimeSlotMessageBuilder timeSlotMessageBuilder;
    private final TelegramMessageSender messageSender;
    
    @Autowired
    public TimeSlotMarkingStateHandler(
            TimeSlotMarkingStateManager timeSlotMarkingStateManager,
            UserRepository userRepository,
            UserService userService,
            TimeSlotMessageBuilder timeSlotMessageBuilder,
            @Lazy AbsSender bot) {
        this.timeSlotMarkingStateManager = timeSlotMarkingStateManager;
        this.userRepository = userRepository;
        this.userService = userService;
        this.timeSlotMessageBuilder = timeSlotMessageBuilder;
        this.messageSender = new TelegramMessageSender(bot);
    }
    
    @Override
    public boolean canHandle(String chatId, TimeSlotMarkingStateManager.TimeSlotMarkingState state) {
        return timeSlotMarkingStateManager.hasState(chatId) && timeSlotMarkingStateManager.getState(chatId) == state;
    }
    
    @Override
    public void handle(Long telegramUserId, String chatId, String text, TimeSlotMarkingStateManager.TimeSlotMarkingState state) {
        try {
            timeSlotMarkingStateManager.updateTimestamp(chatId);
            var data = timeSlotMarkingStateManager.getData(chatId);
            User user = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
            
            if (data == null || user == null) {
                timeSlotMarkingStateManager.clearState(chatId);
                messageSender.sendPersonalMessage(chatId, "❌ Ошибка: данные разметки не найдены. Начните заново с /mark.");
                return;
            }
            
            // Получаем часовой пояс пользователя
            ZoneId userTimezone;
            try {
                userTimezone = ZoneId.of(user.getTimezone());
            } catch (Exception e) {
                timeSlotMarkingStateManager.clearState(chatId);
                messageSender.sendPersonalMessage(chatId, """
                    ❌ <b>Неверный часовой пояс</b>
                    
                    Установите корректный часовой пояс в настройках профиля на веб-сайте.""");
                return;
            }
            
            switch (state) {
                case WAITING_CLEAR_BEFORE_ADD:
                    handleClearBeforeAddChoice(chatId, text.trim(), data);
                    break;
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
            messageSender.sendPersonalMessage(chatId, "❌ Произошла ошибка при обработке разметки времени. Попробуйте позже.");
        }
    }
    
    private static final Set<String> YES_ANSWERS = Set.of("да", "д", "yes", "y", "+", "1", "true");
    private static final Set<String> NO_ANSWERS = Set.of("нет", "н", "no", "n", "-", "0", "false");

    /**
     * @return true = очистить все слоты перед добавлением, false = только добавить, null = не распознано
     */
    private Boolean parseYesNo(String raw) {
        if (raw == null) {
            return null;
        }
        String t = raw.trim().toLowerCase(Locale.ROOT);
        if (t.isEmpty()) {
            return null;
        }
        if (YES_ANSWERS.contains(t)) {
            return true;
        }
        if (NO_ANSWERS.contains(t)) {
            return false;
        }
        return null;
    }

    private void handleClearBeforeAddChoice(String chatId, String text, TimeSlotMarkingStateManager.TimeSlotMarkingData data) {
        Boolean choice = parseYesNo(text);
        if (choice == null) {
            messageSender.sendPersonalMessage(chatId, """
                    ❌ Не понял ответ. Введите <b>да</b> или <b>нет</b> (д/н, yes/no).
                    
                    💡 Используйте /cancel для отмены.""");
            return;
        }
        data.clearAllBeforeAdd = choice;
        timeSlotMarkingStateManager.setState(chatId, TimeSlotMarkingStateManager.TimeSlotMarkingState.WAITING_DATE);
        messageSender.sendPersonalMessage(chatId, """
                Введите дату в формате ДД.ММ.ГГГГ (например: 15.01.2025)
                Или используйте: сегодня, завтра, послезавтра
                
                💡 Используйте /cancel для отмены.""");
    }

    private void handleDateInput(String chatId, String dateStr, TimeSlotMarkingStateManager.TimeSlotMarkingData data, ZoneId userTimezone) {
        if (dateStr.isEmpty()) {
            messageSender.sendPersonalMessage(chatId, "❌ Дата не может быть пустой. Введите дату:");
            return;
        }
        
        LocalDate localDate = TelegramDateParser.parseDate(dateStr, userTimezone);
        if (localDate == null) {
            messageSender.sendPersonalMessage(chatId, """
                ❌ <b>Неверный формат даты</b>
                
                Используйте формат ДД.ММ.ГГГГ (например: 15.01.2025)
                Или используйте: сегодня, завтра, послезавтра
                
                💡 Используйте /cancel для отмены.""");
            return;
        }
        
        data.dateStr = dateStr;
        data.dateInstant = localDate.atStartOfDay(userTimezone).toInstant();
        timeSlotMarkingStateManager.setState(chatId, TimeSlotMarkingStateManager.TimeSlotMarkingState.WAITING_TIME);
        
        messageSender.sendPersonalMessage(chatId, "✅ Дата принята: " + TelegramTimeFormatter.formatLocalDate(localDate) + "\n\n" +
                "Введите время начала в формате ЧЧ:ММ или ЧЧ (например: 18:00 или 18):\n\n" +
                "💡 Используйте /cancel для отмены.");
    }
    
    private void handleTimeInput(String chatId, String timeStr, TimeSlotMarkingStateManager.TimeSlotMarkingData data, ZoneId userTimezone) {
        if (timeStr.isEmpty()) {
            messageSender.sendPersonalMessage(chatId, "❌ Время не может быть пустым. Введите время:");
            return;
        }
        
        LocalTime localTime = TelegramDateParser.parseTime(timeStr);
        if (localTime == null) {
            messageSender.sendPersonalMessage(chatId, """
                ❌ <b>Неверный формат времени</b>
                
                Используйте формат ЧЧ:ММ (например: 18:00) или ЧЧ (например: 18)
                
                💡 Используйте /cancel для отмены.""");
            return;
        }
        
        data.timeStr = timeStr;
        // Пока сохраняем только время, финальный Instant создадим после получения продолжительности
        timeSlotMarkingStateManager.setState(chatId, TimeSlotMarkingStateManager.TimeSlotMarkingState.WAITING_DURATION);
        
        messageSender.sendPersonalMessage(chatId, "✅ Время принято: " + TelegramTimeFormatter.formatLocalTime(localTime) + "\n\n" +
                "Введите продолжительность в часах (например: 1, 2, 3):\n\n" +
                "💡 Используйте /cancel для отмены.");
    }
    
    private void handleDurationInput(Long telegramUserId, String chatId, String durationStr, 
                                     TimeSlotMarkingStateManager.TimeSlotMarkingData data, User user, ZoneId userTimezone) {
        if (durationStr.isEmpty()) {
            messageSender.sendPersonalMessage(chatId, "❌ Продолжительность не может быть пустой. Введите количество часов:");
            return;
        }
        
        Integer duration = TelegramDateParser.parseDuration(durationStr);
        if (duration == null) {
            messageSender.sendPersonalMessage(chatId, """
                ❌ <b>Неверный формат продолжительности</b>
                
                Введите число от 1 до 24 (количество часов)
                
                💡 Используйте /cancel для отмены.""");
            return;
        }
        
        data.duration = duration;
        
        // Создаем финальный Instant: дата + время в часовом поясе пользователя, конвертируем в UTC
        LocalDate localDate = LocalDate.ofInstant(data.dateInstant, userTimezone);
        LocalTime localTime = TelegramDateParser.parseTime(data.timeStr);
        if (localTime == null) {
            timeSlotMarkingStateManager.clearState(chatId);
            messageSender.sendPersonalMessage(chatId, "❌ Ошибка: время не найдено. Начните заново с /mark.");
            return;
        }
        
        Instant startInstant = TelegramDateParser.convertToUTC(localDate, localTime, userTimezone);
        data.startInstant = startInstant;
        
        try {
            userService.addTimeSlotClearingAllIfRequested(
                    user,
                    startInstant,
                    duration,
                    Boolean.TRUE.equals(data.clearAllBeforeAdd));

            // Успешная разметка
            timeSlotMarkingStateManager.clearState(chatId);
            
            // Используем TimeSlotMessageBuilder для форматирования сообщения
            String message = timeSlotMessageBuilder.buildTimeSlotMarkedMessage(localDate, localTime, duration, userTimezone);
            messageSender.sendPersonalMessage(chatId, message);
            
            logger.info("Time slot marked via Telegram for user: {}, chatId: {}, start: {}, duration: {}", 
                    user.getUsername(), chatId, startInstant, duration);
        } catch (Exception e) {
            logger.error("Error saving time slot via Telegram", e);
            timeSlotMarkingStateManager.clearState(chatId);
            messageSender.sendPersonalMessage(chatId, "❌ Ошибка при сохранении временного слота. Попробуйте позже.");
        }
    }
}


