package ru.ambryo.gameplannerback.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ambryo.gameplannerback.repository.GameRepository;
import ru.ambryo.gameplannerback.repository.TimeSlotRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class CleanupService {
    
    private static final Logger logger = LoggerFactory.getLogger(CleanupService.class);
    
    @Autowired
    private GameRepository gameRepository;
    
    @Autowired
    private TimeSlotRepository timeSlotRepository;
    
    @Value("${app.cleanup.retention-days:15}")
    private int retentionDays;
    
    @Value("${app.cleanup.enabled:true}")
    private boolean cleanupEnabled;
    
    @Value("${app.cleanup.cron:0 0 3 * * ?}")
    private String cronExpression;
    
    // Логируем конфигурацию при инициализации
    @jakarta.annotation.PostConstruct
    public void init() {
        logger.info("CleanupService initialized:");
        logger.info("  - Enabled: {}", cleanupEnabled);
        logger.info("  - Retention days: {}", retentionDays);
        logger.info("  - Cron expression: {}", cronExpression);
    }
    
    /**
     * Запускается по расписанию из конфигурации
     */
    @Scheduled(cron = "${app.cleanup.cron:0 0 3 * * ?}")
    @Transactional
    public void cleanupOldData() {
        if (!cleanupEnabled) {
            logger.info("Cleanup is disabled");
            return;
        }
        
        logger.info("Starting cleanup of old data (retention: {} days)", retentionDays);
        
        Instant cutoffDate = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        
        // Удаляем старые игры
        int deletedGames = cleanupOldGames(cutoffDate);
        
        // Удаляем старые временные слоты
        int deletedTimeSlots = cleanupOldTimeSlots(cutoffDate);
        
        logger.info("Cleanup completed: deleted {} games and {} time slots", deletedGames, deletedTimeSlots);
    }
    
    private int cleanupOldGames(Instant cutoffDate) {
        try {
            // Находим и удаляем игры, которые закончились раньше cutoffDate
            var oldGames = gameRepository.findAll().stream()
                    .filter(game -> game.getEndTime().isBefore(cutoffDate))
                    .toList();
            
            int count = oldGames.size();
            gameRepository.deleteAll(oldGames);
            
            logger.debug("Deleted {} old games (ended before {})", count, cutoffDate);
            return count;
        } catch (Exception e) {
            logger.error("Error cleaning up old games", e);
            return 0;
        }
    }
    
    private int cleanupOldTimeSlots(Instant cutoffDate) {
        try {
            // Находим и удаляем временные слоты, которые начались раньше cutoffDate
            var oldTimeSlots = timeSlotRepository.findAll().stream()
                    .filter(slot -> slot.getStart().isBefore(cutoffDate))
                    .toList();
            
            int count = oldTimeSlots.size();
            timeSlotRepository.deleteAll(oldTimeSlots);
            
            logger.debug("Deleted {} old time slots (started before {})", count, cutoffDate);
            return count;
        } catch (Exception e) {
            logger.error("Error cleaning up old time slots", e);
            return 0;
        }
    }
    
    /**
     * Ручной запуск очистки (для тестирования или административных целей)
     */
    @Transactional
    public void manualCleanup() {
        logger.info("Manual cleanup triggered");
        cleanupOldData();
    }
    
    /**
     * Получить информацию о конфигурации очистки
     */
    public java.util.Map<String, Object> getCleanupInfo() {
        java.util.Map<String, Object> info = new java.util.HashMap<>();
        info.put("enabled", cleanupEnabled);
        info.put("retentionDays", retentionDays);
        info.put("cronExpression", cronExpression);
        
        Instant cutoffDate = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        info.put("cutoffDate", cutoffDate.toString());
        
        // Подсчитываем количество данных для удаления
        long oldGamesCount = gameRepository.findAll().stream()
                .filter(game -> game.getEndTime().isBefore(cutoffDate))
                .count();
        
        long oldTimeSlotsCount = timeSlotRepository.findAll().stream()
                .filter(slot -> slot.getStart().isBefore(cutoffDate))
                .count();
        
        info.put("oldGamesCount", oldGamesCount);
        info.put("oldTimeSlotsCount", oldTimeSlotsCount);
        
        return info;
    }
}
