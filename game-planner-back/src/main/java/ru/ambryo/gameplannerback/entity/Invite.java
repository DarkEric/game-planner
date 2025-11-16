package ru.ambryo.gameplannerback.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "invites")
public class Invite {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String code; // Уникальный код инвайта (UUID)
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdBy; // Кто создал инвайт
    
    @Column(nullable = false)
    private Instant createdAt;
    
    @Column
    private Instant expiresAt; // Когда истекает (null = бессрочный)
    
    @Column(nullable = false)
    private Boolean used = false; // Использован ли инвайт
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "used_by_user_id")
    private User usedBy; // Кто использовал инвайт
    
    @Column
    private Instant usedAt; // Когда использован
    
    @Column
    private Integer maxUses; // Максимальное количество использований (null = неограниченно)
    
    @Column(nullable = false)
    private Integer usesCount = 0; // Сколько раз использован
    
    public Invite() {
        this.code = UUID.randomUUID().toString();
        this.createdAt = Instant.now();
    }
    
    public Invite(User createdBy) {
        this();
        this.createdBy = createdBy;
    }
    
    public Invite(User createdBy, Instant expiresAt, Integer maxUses) {
        this();
        this.createdBy = createdBy;
        this.expiresAt = expiresAt;
        this.maxUses = maxUses;
    }
    
    public boolean isValid() {
        // Проверяем, не истек ли инвайт
        if (expiresAt != null && Instant.now().isAfter(expiresAt)) {
            return false;
        }
        
        // Проверяем, не превышено ли количество использований
        if (maxUses != null && usesCount >= maxUses) {
            return false;
        }
        
        return true;
    }
    
    public void markAsUsed(User user) {
        this.usedBy = user;
        this.usedAt = Instant.now();
        this.used = true;
        this.usesCount++;
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
    
    public User getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
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
    
    public User getUsedBy() {
        return usedBy;
    }
    
    public void setUsedBy(User usedBy) {
        this.usedBy = usedBy;
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
}
