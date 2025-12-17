package ru.ambryo.gameplannerback.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String username;
    
    @Column(nullable = false)
    private String password;
    
    @Column(nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private String color;
    
    @Column(name = "timezone")
    private String timezone; // IANA timezone (например: "Europe/Moscow", "America/New_York")
    
    @Column(name = "telegram_user_id", unique = true)
    private Long telegramUserId; // Telegram User ID для персональных уведомлений
    
    @Column(name = "telegram_chat_id")
    private String telegramChatId; // Chat ID для персональных уведомлений (может отличаться от User ID)
    
    @Column(name = "telegram_subscribed", nullable = false)
    private Boolean telegramSubscribed = false; // Флаг подписки на бота
    
    @Column(name = "password_reset_token")
    private String passwordResetToken;
    
    @Column(name = "password_reset_expiry")
    private java.time.Instant passwordResetExpiry;
    
    @Column(name = "is_admin", nullable = false)
    private Boolean isAdmin = false;
    
    @OneToMany(mappedBy = "user", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<TimeSlot> availableTimes = new ArrayList<>();
    
    public User() {
    }
    
    public User(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.name = username; // По умолчанию имя = username
        this.color = "#646cff"; // Цвет по умолчанию
    }
    
    public User(String username, String password, String email, String name, String color) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.name = name;
        this.color = color;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getColor() {
        return color;
    }
    
    public void setColor(String color) {
        this.color = color;
    }
    
    public List<TimeSlot> getAvailableTimes() {
        return availableTimes;
    }
    
    public void setAvailableTimes(List<TimeSlot> availableTimes) {
        this.availableTimes = availableTimes;
    }
    
    public String getTimezone() {
        return timezone;
    }
    
    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }
    
    public Long getTelegramUserId() {
        return telegramUserId;
    }
    
    public void setTelegramUserId(Long telegramUserId) {
        this.telegramUserId = telegramUserId;
    }
    
    public String getTelegramChatId() {
        return telegramChatId;
    }
    
    public void setTelegramChatId(String telegramChatId) {
        this.telegramChatId = telegramChatId;
    }
    
    public Boolean getTelegramSubscribed() {
        return telegramSubscribed;
    }
    
    public void setTelegramSubscribed(Boolean telegramSubscribed) {
        this.telegramSubscribed = telegramSubscribed;
    }
    
    public String getPasswordResetToken() {
        return passwordResetToken;
    }
    
    public void setPasswordResetToken(String passwordResetToken) {
        this.passwordResetToken = passwordResetToken;
    }
    
    public java.time.Instant getPasswordResetExpiry() {
        return passwordResetExpiry;
    }
    
    public void setPasswordResetExpiry(java.time.Instant passwordResetExpiry) {
        this.passwordResetExpiry = passwordResetExpiry;
    }
    
    public Boolean getIsAdmin() {
        return isAdmin != null ? isAdmin : false;
    }
    
    public void setIsAdmin(Boolean isAdmin) {
        this.isAdmin = isAdmin != null ? isAdmin : false;
    }
}

