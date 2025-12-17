package ru.ambryo.gameplannerback.dto;

import java.time.Instant;

public class AdminUserDto {
    private Long id;
    private String username;
    private String email;
    private Boolean isAdmin;
    private Boolean telegramSubscribed;
    private Instant createdAt;
    
    public AdminUserDto() {
    }
    
    public AdminUserDto(Long id, String username, String email, Boolean isAdmin, Boolean telegramSubscribed, Instant createdAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.isAdmin = isAdmin;
        this.telegramSubscribed = telegramSubscribed;
        this.createdAt = createdAt;
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
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public Boolean getIsAdmin() {
        return isAdmin;
    }
    
    public void setIsAdmin(Boolean isAdmin) {
        this.isAdmin = isAdmin;
    }
    
    public Boolean getTelegramSubscribed() {
        return telegramSubscribed;
    }
    
    public void setTelegramSubscribed(Boolean telegramSubscribed) {
        this.telegramSubscribed = telegramSubscribed;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
