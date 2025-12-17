package ru.ambryo.gameplannerback.dto;

public class ResetPasswordResponse {
    private String temporaryPassword;
    private Boolean sentViaTelegram;
    private String message;
    
    public ResetPasswordResponse() {
    }
    
    public ResetPasswordResponse(String temporaryPassword, Boolean sentViaTelegram, String message) {
        this.temporaryPassword = temporaryPassword;
        this.sentViaTelegram = sentViaTelegram;
        this.message = message;
    }
    
    public String getTemporaryPassword() {
        return temporaryPassword;
    }
    
    public void setTemporaryPassword(String temporaryPassword) {
        this.temporaryPassword = temporaryPassword;
    }
    
    public Boolean getSentViaTelegram() {
        return sentViaTelegram;
    }
    
    public void setSentViaTelegram(Boolean sentViaTelegram) {
        this.sentViaTelegram = sentViaTelegram;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}
