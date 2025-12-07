package ru.ambryo.gameplannerback.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "game_notifications", 
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_game_notification_personal", 
                           columnNames = {"game_id", "notification_type", "user_id"})
       })
public class GameNotification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;
    
    @Column(name = "notification_type", nullable = false)
    private String notificationType; // "24H_BEFORE", "X_MINUTES_BEFORE", "GAME_CREATED", "GAME_CANCELLED", "GAME_HELD", "ADDED_TO_GAME", "TIME_SLOT_REMINDER", "GAME_COMPLETION_REMINDER"
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // null для общих уведомлений, не null для персональных
    
    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;
    
    public GameNotification() {
        this.sentAt = Instant.now();
    }
    
    public GameNotification(Game game, String notificationType, User user) {
        this.game = game;
        this.notificationType = notificationType;
        this.user = user;
        this.sentAt = Instant.now();
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Game getGame() {
        return game;
    }
    
    public void setGame(Game game) {
        this.game = game;
    }
    
    public String getNotificationType() {
        return notificationType;
    }
    
    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public Instant getSentAt() {
        return sentAt;
    }
    
    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }
}
