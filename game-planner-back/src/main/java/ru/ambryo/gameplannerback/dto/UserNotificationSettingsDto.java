package ru.ambryo.gameplannerback.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public class UserNotificationSettingsDto {
    
    @NotNull
    private String gameCreated; // "ALL", "MY_GAMES", "NONE"
    
    @NotNull
    private String gameCancelled; // "ALL", "MY_GAMES", "NONE"
    
    @NotNull
    private String gameHeld; // "ALL", "MY_GAMES", "NONE"
    
    @NotNull
    private String gameRemovedFromGame; // "ALL", "NONE"
    
    private List<UpcomingGameReminderDto> upcomingGameReminders; // до 5 штук
    
    @NotNull
    private Boolean timeSlotReminderEnabled;
    
    private Instant timeSlotReminderDateTime; // Устаревшее поле
    
    private String timeSlotReminderCron; // Cron выражение для регулярных напоминаний
    
    @NotNull
    private Boolean gameCompletionReminderEnabled;
    
    private Boolean telegramSubscribed; // Статус подписки на Telegram бота
    
    public UserNotificationSettingsDto() {
    }
    
    public String getGameCreated() {
        return gameCreated;
    }
    
    public void setGameCreated(String gameCreated) {
        this.gameCreated = gameCreated;
    }
    
    public String getGameCancelled() {
        return gameCancelled;
    }
    
    public void setGameCancelled(String gameCancelled) {
        this.gameCancelled = gameCancelled;
    }
    
    public String getGameHeld() {
        return gameHeld;
    }
    
    public void setGameHeld(String gameHeld) {
        this.gameHeld = gameHeld;
    }
    
    public String getGameRemovedFromGame() {
        return gameRemovedFromGame;
    }
    
    public void setGameRemovedFromGame(String gameRemovedFromGame) {
        this.gameRemovedFromGame = gameRemovedFromGame;
    }
    
    public List<UpcomingGameReminderDto> getUpcomingGameReminders() {
        return upcomingGameReminders;
    }
    
    public void setUpcomingGameReminders(List<UpcomingGameReminderDto> upcomingGameReminders) {
        this.upcomingGameReminders = upcomingGameReminders;
    }
    
    public Boolean getTimeSlotReminderEnabled() {
        return timeSlotReminderEnabled;
    }
    
    public void setTimeSlotReminderEnabled(Boolean timeSlotReminderEnabled) {
        this.timeSlotReminderEnabled = timeSlotReminderEnabled;
    }
    
    public Instant getTimeSlotReminderDateTime() {
        return timeSlotReminderDateTime;
    }
    
    public void setTimeSlotReminderDateTime(Instant timeSlotReminderDateTime) {
        this.timeSlotReminderDateTime = timeSlotReminderDateTime;
    }
    
    public Boolean getGameCompletionReminderEnabled() {
        return gameCompletionReminderEnabled;
    }
    
    public void setGameCompletionReminderEnabled(Boolean gameCompletionReminderEnabled) {
        this.gameCompletionReminderEnabled = gameCompletionReminderEnabled;
    }
    
    public Boolean getTelegramSubscribed() {
        return telegramSubscribed;
    }
    
    public void setTelegramSubscribed(Boolean telegramSubscribed) {
        this.telegramSubscribed = telegramSubscribed;
    }
    
    public String getTimeSlotReminderCron() {
        return timeSlotReminderCron;
    }
    
    public void setTimeSlotReminderCron(String timeSlotReminderCron) {
        this.timeSlotReminderCron = timeSlotReminderCron;
    }
}
