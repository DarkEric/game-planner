package ru.ambryo.gameplannerback.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.ambryo.gameplannerback.dto.GameDto;
import ru.ambryo.gameplannerback.dto.UpcomingGameReminderDto;
import ru.ambryo.gameplannerback.entity.Game;
import ru.ambryo.gameplannerback.entity.GameNotification;
import ru.ambryo.gameplannerback.entity.User;
import ru.ambryo.gameplannerback.entity.UserNotificationSettings;
import ru.ambryo.gameplannerback.repository.GameNotificationRepository;
import ru.ambryo.gameplannerback.repository.GameRepository;
import ru.ambryo.gameplannerback.repository.UserNotificationSettingsRepository;
import ru.ambryo.gameplannerback.repository.UserRepository;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Service
public class PersonalNotificationScheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(PersonalNotificationScheduler.class);
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserNotificationSettingsRepository settingsRepository;
    
    @Autowired
    private GameRepository gameRepository;
    
    @Autowired
    private GameNotificationRepository gameNotificationRepository;
    
    @Autowired
    private TelegramNotificationService telegramNotificationService;
    
    @Autowired
    private GameService gameService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Value("${telegram.notifications.scheduled.enabled:true}")
    private boolean scheduledEnabled;
    
    @Value("${telegram.notifications.check-interval:60000}")
    private long checkInterval;
    
    @Value("${telegram.notifications.group.time-slot-reminder-enabled:false}")
    private boolean groupTimeSlotReminderEnabled;
    
    @Scheduled(fixedRateString = "${telegram.notifications.check-interval:60000}")
    @Transactional(readOnly = false)
    public void checkAndSendUpcomingGameReminders() {
        if (!scheduledEnabled) {
            return;
        }
        
        try {
            List<User> subscribedUsers = userRepository.findByTelegramSubscribedTrue();
            
            for (User user : subscribedUsers) {
                try {
                    UserNotificationSettings settings = settingsRepository.findByUserId(user.getId()).orElse(null);
                    if (settings == null) {
                        continue;
                    }
                    
                    // Парсим настройки напоминаний
                    List<UpcomingGameReminderDto> reminders;
                    try {
                        if (settings.getUpcomingGameReminders() != null && !settings.getUpcomingGameReminders().isEmpty()) {
                            reminders = objectMapper.readValue(
                                    settings.getUpcomingGameReminders(),
                                    new TypeReference<List<UpcomingGameReminderDto>>() {}
                            );
                        } else {
                            reminders = List.of();
                        }
                    } catch (Exception e) {
                        logger.warn("Failed to parse reminders for user {}", user.getId(), e);
                        continue;
                    }
                    
                    // Проверяем каждое напоминание
                    for (UpcomingGameReminderDto reminder : reminders) {
                        if (!reminder.getEnabled() || reminder.getMinutesBefore() == null) {
                            continue;
                        }
                        
                        Instant now = Instant.now();
                        // Игра должна начаться через reminder.getMinutesBefore() минут
                        // Ищем игры, которые начинаются в окне: [now + minutesBefore - tolerance, now + minutesBefore + tolerance]
                        long minutesBeforeSeconds = reminder.getMinutesBefore() * 60L;
                        long toleranceSeconds = checkInterval / 1000; // Толерантность = интервал проверки
                        Instant gameStartTarget = now.plusSeconds(minutesBeforeSeconds);
                        Instant windowStart = gameStartTarget.minusSeconds(toleranceSeconds);
                        Instant windowEnd = gameStartTarget.plusSeconds(toleranceSeconds);
                        
                        // Находим игры пользователя, которые начинаются в этом окне
                        List<Game> games = gameRepository.findGamesByParticipantStartingBetween(
                                user.getId(), windowStart, windowEnd);
                        
                        for (Game game : games) {
                            // Проверяем, не отправляли ли уже это уведомление
                            String notificationType = reminder.getMinutesBefore() + "_MINUTES_BEFORE";
                            GameNotification existing = gameNotificationRepository.findPersonalNotification(
                                    game, notificationType, user).orElse(null);
                            
                            if (existing == null) {
                                // Отправляем уведомление
                                GameDto gameDto = gameService.getGameById(game.getId());
                                telegramNotificationService.sendUpcomingGameReminder(
                                        gameDto, user, reminder.getMinutesBefore());
                                
                                // Сохраняем запись об отправке
                                GameNotification notification = new GameNotification(game, notificationType, user);
                                gameNotificationRepository.save(notification);
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error processing reminders for user {}", user.getId(), e);
                }
            }
        } catch (Exception e) {
            logger.error("Error in checkAndSendUpcomingGameReminders", e);
        }
    }
    
    @Scheduled(fixedRateString = "${telegram.notifications.check-interval:60000}")
    @Transactional(readOnly = false)
    public void checkAndSendTimeSlotReminders() {
        if (!scheduledEnabled) {
            return;
        }
        
        try {
            Instant now = Instant.now();
            ZonedDateTime nowZoned = now.atZone(ZoneId.systemDefault());
            
            // Персональные напоминания
            List<User> subscribedUsers = userRepository.findByTelegramSubscribedTrue();
            
            for (User user : subscribedUsers) {
                try {
                    UserNotificationSettings settings = settingsRepository.findByUserId(user.getId()).orElse(null);
                    if (settings == null || !settings.getTimeSlotReminderEnabled()) {
                        continue;
                    }
                    
                    boolean shouldSend = false;
                    
                    // Проверяем cron-выражение (новый способ)
                    String cronExpression = settings.getTimeSlotReminderCron();
                    if (cronExpression != null && !cronExpression.trim().isEmpty()) {
                        try {
                            CronExpression cron = CronExpression.parse(cronExpression);
                            
                            // Проверяем, соответствует ли текущее время cron-выражению
                            // Используем метод: вычисляем next() от времени минус небольшая дельта
                            // Если результат равен текущему времени (с точностью до окна), значит время соответствует
                            long checkIntervalSeconds = checkInterval / 1000;
                            // Используем окно в 2 раза больше интервала проверки для надежности
                            long windowSeconds = checkIntervalSeconds * 2;
                            
                            // Вычисляем следующее выполнение от времени минус окно
                            ZonedDateTime checkTime = nowZoned.minusSeconds(windowSeconds);
                            ZonedDateTime nextExecution = cron.next(checkTime);
                            
                            if (nextExecution != null) {
                                // Проверяем, попадает ли следующее выполнение в текущее окно
                                long secondsUntilExecution = nextExecution.toInstant().getEpochSecond() - now.getEpochSecond();
                                
                                // Если следующее выполнение уже наступило или наступит в ближайшее время
                                if (secondsUntilExecution >= -windowSeconds && secondsUntilExecution <= windowSeconds) {
                                    // Проверяем, не отправляли ли мы уже напоминание для этого выполнения
                                    Instant lastSent = settings.getTimeSlotReminderDateTime();
                                    
                                    if (lastSent == null) {
                                        // Никогда не отправляли - отправляем
                                        shouldSend = true;
                                    } else {
                                        // Проверяем, было ли последнее отправление для другого времени выполнения
                                        // Вычисляем время выполнения cron, которое соответствует текущему моменту
                                        // (округляем до минуты для сравнения)
                                        long currentExecutionMinute = nextExecution.toInstant().getEpochSecond() / 60;
                                        long lastSentMinute = lastSent.getEpochSecond() / 60;
                                        
                                        // Если это другое выполнение (разные минуты), отправляем
                                        if (currentExecutionMinute != lastSentMinute) {
                                            shouldSend = true;
                                        }
                                    }
                                }
                            }
                            
                        } catch (Exception e) {
                            logger.warn("Invalid cron expression '{}' for user {}, skipping", cronExpression, user.getId(), e);
                            continue;
                        }
                    } else {
                        // Старый способ: проверяем timeSlotReminderDateTime (для обратной совместимости)
                        if (settings.getTimeSlotReminderDateTime() != null &&
                                settings.getTimeSlotReminderDateTime().isBefore(now)) {
                            shouldSend = true;
                        }
                    }
                    
                    if (shouldSend) {
                        // Отправляем напоминание
                        telegramNotificationService.sendTimeSlotReminder(user);
                        
                        // Обновляем время последнего выполнения
                        updateTimeSlotReminderDateTime(settings.getId(), now);
                    }
                } catch (Exception e) {
                    logger.error("Error processing time slot reminder for user {}", user.getId(), e);
                }
            }
            
        } catch (Exception e) {
            logger.error("Error in checkAndSendTimeSlotReminders", e);
        }
    }
    
    @Value("${telegram.notifications.group.time-slot-reminder-cron:}")
    private String groupTimeSlotReminderCron;
    
    /**
     * Групповое напоминание о разметке времени в общий чат
     * Запускается по cron выражению из конфигурации
     * Метод не будет запускаться, если cron выражение не задано
     */
    @Scheduled(cron = "${telegram.notifications.group.time-slot-reminder-cron:0 0 0 * * ?}")
    @Transactional(readOnly = false)
    public void checkAndSendGroupTimeSlotReminder() {
        // Проверяем, что cron выражение задано (не пустое)
        if (groupTimeSlotReminderCron == null || groupTimeSlotReminderCron.trim().isEmpty()) {
            return;
        }
        
        if (!scheduledEnabled || !groupTimeSlotReminderEnabled) {
            return;
        }
        
        try {
            logger.info("Sending group time slot reminder");
            telegramNotificationService.sendGroupTimeSlotReminder();
        } catch (Exception e) {
            logger.error("Error sending group time slot reminder", e);
        }
    }
    
    @Scheduled(fixedRateString = "${telegram.notifications.check-interval:60000}")
    @Transactional(readOnly = false)
    public void checkAndSendGameCompletionReminders() {
        if (!scheduledEnabled) {
            return;
        }
        
        try {
            Instant now = Instant.now();
            // Ищем игры, которые закончились более 1 часа назад, но не помечены как проведенные
            Instant oneHourAgo = now.minusSeconds(3600);
            List<Game> games = gameRepository.findGamesEndedButNotHeld(oneHourAgo);
            
            for (Game game : games) {
                try {
                    User creator = game.getCreator();
                    if (creator == null || !creator.getTelegramSubscribed()) {
                        continue;
                    }
                    
                    UserNotificationSettings settings = settingsRepository.findByUserId(creator.getId()).orElse(null);
                    if (settings == null || !settings.getGameCompletionReminderEnabled()) {
                        continue;
                    }
                    
                    // Проверяем, не отправляли ли уже это уведомление
                    GameNotification existing = gameNotificationRepository.findPersonalNotification(
                            game, "GAME_COMPLETION_REMINDER", creator).orElse(null);
                    
                    if (existing == null) {
                        GameDto gameDto = gameService.getGameById(game.getId());
                        telegramNotificationService.sendGameCompletionReminder(gameDto, creator);
                        
                        // Сохраняем запись об отправке
                        GameNotification notification = new GameNotification(
                                game, "GAME_COMPLETION_REMINDER", creator);
                        gameNotificationRepository.save(notification);
                    }
                } catch (Exception e) {
                    logger.error("Error processing completion reminder for game {}", game.getId(), e);
                }
            }
        } catch (Exception e) {
            logger.error("Error in checkAndSendGameCompletionReminders", e);
        }
    }
    
    /**
     * Обновляет время последнего выполнения напоминания
     * Используется для отслеживания последнего времени отправки cron-напоминаний
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = false)
    private void updateTimeSlotReminderDateTime(Long settingsId, Instant executionTime) {
        UserNotificationSettings settings = settingsRepository.findById(settingsId)
                .orElse(null);
        if (settings != null) {
            settings.setTimeSlotReminderDateTime(executionTime);
            settingsRepository.save(settings);
        }
    }
}
