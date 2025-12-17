package ru.ambryo.gameplannerback.dto;

public class PasswordResetRequestDto {
    private String username;
    
    public PasswordResetRequestDto() {
    }
    
    public PasswordResetRequestDto(String username) {
        this.username = username;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
}
