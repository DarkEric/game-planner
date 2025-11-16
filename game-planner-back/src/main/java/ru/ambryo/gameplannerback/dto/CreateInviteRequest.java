package ru.ambryo.gameplannerback.dto;

import java.time.Instant;

public class CreateInviteRequest {
    private Instant expiresAt;
    private Integer maxUses;
    
    public CreateInviteRequest() {
    }
    
    public CreateInviteRequest(Instant expiresAt, Integer maxUses) {
        this.expiresAt = expiresAt;
        this.maxUses = maxUses;
    }
    
    public Instant getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
    
    public Integer getMaxUses() {
        return maxUses;
    }
    
    public void setMaxUses(Integer maxUses) {
        this.maxUses = maxUses;
    }
}
