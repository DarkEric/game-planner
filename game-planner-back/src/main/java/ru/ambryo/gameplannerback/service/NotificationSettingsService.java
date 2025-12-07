package ru.ambryo.gameplannerback.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ambryo.gameplannerback.dto.UpcomingGameReminderDto;
import ru.ambryo.gameplannerback.dto.UserNotificationSettingsDto;
import ru.ambryo.gameplannerback.entity.User;
import ru.ambryo.gameplannerback.entity.UserNotificationSettings;
import ru.ambryo.gameplannerback.repository.UserNotificationSettingsRepository;
import ru.ambryo.gameplannerback.repository.UserRepository;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class NotificationSettingsService {
    
    @Autowired
    private UserNotificationSettingsRepository settingsRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    // In-memory storage for link tokens: token -> userId, expires at
    private final Map<String, TokenInfo> linkTokens = new ConcurrentHashMap<>();
    
    private static class TokenInfo {
        final Long userId;
        final Instant expiresAt;
        
        TokenInfo(Long userId, Instant expiresAt) {
            this.userId = userId;
            this.expiresAt = expiresAt;
        }
    }
    
    @Transactional(readOnly = true)
    public UserNotificationSettingsDto getSettings(Long userId) {
        UserNotificationSettings settings = settingsRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultSettings(userId));
        
        return convertToDto(settings);
    }
    
    @Transactional
    public UserNotificationSettingsDto updateSettings(Long userId, UserNotificationSettingsDto dto) {
        UserNotificationSettings settings = settingsRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultSettings(userId));
        
        // Валидация: до 5 напоминаний
        if (dto.getUpcomingGameReminders() != null && dto.getUpcomingGameReminders().size() > 5) {
            throw new IllegalArgumentException("Maximum 5 upcoming game reminders allowed");
        }
        
        settings.setGameCreated(dto.getGameCreated());
        settings.setGameCancelled(dto.getGameCancelled());
        settings.setGameHeld(dto.getGameHeld());
        settings.setGameAddedToGame(dto.getGameAddedToGame());
        settings.setTimeSlotReminderEnabled(dto.getTimeSlotReminderEnabled());
        settings.setTimeSlotReminderDateTime(dto.getTimeSlotReminderDateTime());
        settings.setGameCompletionReminderEnabled(dto.getGameCompletionReminderEnabled());
        
        // Сохраняем напоминания как JSON
        try {
            String remindersJson = objectMapper.writeValueAsString(dto.getUpcomingGameReminders());
            settings.setUpcomingGameReminders(remindersJson);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize upcoming game reminders", e);
        }
        
        settings = settingsRepository.save(settings);
        return convertToDto(settings);
    }
    
    @Transactional
    public String generateLinkToken(Long userId) {
        // Генерируем уникальный токен
        String token = UUID.randomUUID().toString();
        // Токен действителен 1 час
        Instant expiresAt = Instant.now().plusSeconds(3600);
        
        linkTokens.put(token, new TokenInfo(userId, expiresAt));
        
        // Очищаем истекшие токены
        cleanupExpiredTokens();
        
        return token;
    }
    
    @Transactional
    public void linkTelegramAccount(String token, Long telegramUserId, String chatId) {
        cleanupExpiredTokens();
        
        TokenInfo tokenInfo = linkTokens.get(token);
        if (tokenInfo == null) {
            throw new RuntimeException("Invalid or expired token");
        }
        
        if (tokenInfo.expiresAt.isBefore(Instant.now())) {
            linkTokens.remove(token);
            throw new RuntimeException("Token expired");
        }
        
        Long userId = tokenInfo.userId;
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setTelegramUserId(telegramUserId);
        user.setTelegramChatId(chatId);
        user.setTelegramSubscribed(true);
        
        userRepository.save(user);
        
        // Удаляем использованный токен
        linkTokens.remove(token);
    }
    
    @Transactional
    public void unlinkTelegramAccount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setTelegramUserId(null);
        user.setTelegramChatId(null);
        user.setTelegramSubscribed(false);
        
        userRepository.save(user);
    }
    
    private UserNotificationSettings createDefaultSettings(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        UserNotificationSettings settings = new UserNotificationSettings(user);
        settings = settingsRepository.save(settings);
        return settings;
    }
    
    private UserNotificationSettingsDto convertToDto(UserNotificationSettings settings) {
        UserNotificationSettingsDto dto = new UserNotificationSettingsDto();
        dto.setGameCreated(settings.getGameCreated());
        dto.setGameCancelled(settings.getGameCancelled());
        dto.setGameHeld(settings.getGameHeld());
        dto.setGameAddedToGame(settings.getGameAddedToGame());
        dto.setTimeSlotReminderEnabled(settings.getTimeSlotReminderEnabled());
        dto.setTimeSlotReminderDateTime(settings.getTimeSlotReminderDateTime());
        dto.setGameCompletionReminderEnabled(settings.getGameCompletionReminderEnabled());
        
        // Парсим JSON напоминаний
        try {
            if (settings.getUpcomingGameReminders() != null && !settings.getUpcomingGameReminders().isEmpty()) {
                List<UpcomingGameReminderDto> reminders = objectMapper.readValue(
                        settings.getUpcomingGameReminders(),
                        new TypeReference<List<UpcomingGameReminderDto>>() {}
                );
                dto.setUpcomingGameReminders(reminders);
            } else {
                dto.setUpcomingGameReminders(new ArrayList<>());
            }
        } catch (Exception e) {
            dto.setUpcomingGameReminders(new ArrayList<>());
        }
        
        return dto;
    }
    
    private void cleanupExpiredTokens() {
        Instant now = Instant.now();
        linkTokens.entrySet().removeIf(entry -> entry.getValue().expiresAt.isBefore(now));
    }
}
