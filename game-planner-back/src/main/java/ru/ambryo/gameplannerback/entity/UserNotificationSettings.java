package ru.ambryo.gameplannerback.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "user_notification_settings")
public class UserNotificationSettings {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;
    
    @Column(name = "game_created", nullable = false)
    private String gameCreated = "ALL"; // "ALL", "MY_GAMES", "NONE"
    
    @Column(name = "game_cancelled", nullable = false)
    private String gameCancelled = "ALL"; // "ALL", "MY_GAMES", "NONE"
    
    @Column(name = "game_held", nullable = false)
    private String gameHeld = "ALL"; // "ALL", "MY_GAMES", "NONE"
    
    @Column(name = "game_removed_from_game", nullable = false)
    private String gameRemovedFromGame = "ALL"; // "ALL", "NONE"
    
    @Column(name = "upcoming_game_reminders", columnDefinition = "TEXT")
    private String upcomingGameReminders = "[]"; // JSON массив настроек напоминаний
    
    @Column(name = "time_slot_reminder_enabled", nullable = false)
    private Boolean timeSlotReminderEnabled = false;
    
    @Column(name = "time_slot_reminder_datetime")
    private Instant timeSlotReminderDateTime; // Устаревшее поле, оставлено для обратной совместимости
    
    @Column(name = "time_slot_reminder_cron")
    private String timeSlotReminderCron; // Cron выражение для регулярных напоминаний (например: "0 9 * * 1" - каждый понедельник в 9:00)
    
    @Column(name = "game_completion_reminder_enabled", nullable = false)
    private Boolean gameCompletionReminderEnabled = false;
    
    public UserNotificationSettings() {
    }
    
    public UserNotificationSettings(User user) {
        this.user = user;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
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
    
    public String getUpcomingGameReminders() {
        return upcomingGameReminders;
    }
    
    public void setUpcomingGameReminders(String upcomingGameReminders) {
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
    
    public String getTimeSlotReminderCron() {
        return timeSlotReminderCron;
    }
    
    public void setTimeSlotReminderCron(String timeSlotReminderCron) {
        this.timeSlotReminderCron = timeSlotReminderCron;
    }
}
