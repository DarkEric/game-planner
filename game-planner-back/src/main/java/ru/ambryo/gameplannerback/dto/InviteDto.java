package ru.ambryo.gameplannerback.dto;

import java.time.Instant;

public class InviteDto {
    private Long id;
    private String code;
    private String createdByName;
    private Instant createdAt;
    private Instant expiresAt;
    private Boolean used;
    private String usedByName;
    private Instant usedAt;
    private Integer maxUses;
    private Integer usesCount;
    private Boolean isValid;
    
    public InviteDto() {
    }
    
    public InviteDto(Long id, String code, String createdByName, Instant createdAt, 
                     Instant expiresAt, Boolean used, String usedByName, Instant usedAt,
                     Integer maxUses, Integer usesCount, Boolean isValid) {
        this.id = id;
        this.code = code;
        this.createdByName = createdByName;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.used = used;
        this.usedByName = usedByName;
        this.usedAt = usedAt;
        this.maxUses = maxUses;
        this.usesCount = usesCount;
        this.isValid = isValid;
    }
    
    // Getters and Setters
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getCreatedByName() {
        return createdByName;
    }
    
    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public Instant getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
    
    public Boolean getUsed() {
        return used;
    }
    
    public void setUsed(Boolean used) {
        this.used = used;
    }
    
    public String getUsedByName() {
        return usedByName;
    }
    
    public void setUsedByName(String usedByName) {
        this.usedByName = usedByName;
    }
    
    public Instant getUsedAt() {
        return usedAt;
    }
    
    public void setUsedAt(Instant usedAt) {
        this.usedAt = usedAt;
    }
    
    public Integer getMaxUses() {
        return maxUses;
    }
    
    public void setMaxUses(Integer maxUses) {
        this.maxUses = maxUses;
    }
    
    public Integer getUsesCount() {
        return usesCount;
    }
    
    public void setUsesCount(Integer usesCount) {
        this.usesCount = usesCount;
    }
    
    public Boolean getIsValid() {
        return isValid;
    }
    
    public void setIsValid(Boolean isValid) {
        this.isValid = isValid;
    }
}
