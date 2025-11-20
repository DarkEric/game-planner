package ru.ambryo.gameplannerback.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "games")
public class Game {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Instant startTime; // Время начала игры в UTC
    
    @Column(nullable = false)
    private Instant endTime; // Время окончания игры в UTC
    
    @Column(length = 255)
    private String title; // Название игры
    
    @Column(length = 1000)
    private String description; // Описание игры
    
    @Column(nullable = false)
    private boolean isHeld = false; // Состоялась ли игра
    
    @Column(columnDefinition = "TEXT")
    private String keyEvents; // Ключевые события (Markdown)
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator; // Создатель игры
    
    @ManyToMany
    @JoinTable(
        name = "game_participants",
        joinColumns = @JoinColumn(name = "game_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> participants = new ArrayList<>();
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id")
    private Campaign campaign;
    
    @Column(nullable = false)
    private Instant createdAt;
    
    public Game() {
        this.createdAt = Instant.now();
    }
    
    public Game(Instant startTime, Instant endTime, User creator) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.creator = creator;
        this.createdAt = Instant.now();
    }
    
    public Game(Instant startTime, Instant endTime, String title, String description, User creator) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.title = title;
        this.description = description;
        this.creator = creator;
        this.createdAt = Instant.now();
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Instant getStartTime() {
        return startTime;
    }
    
    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }
    
    public Instant getEndTime() {
        return endTime;
    }
    
    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }
    
    public User getCreator() {
        return creator;
    }
    
    public void setCreator(User creator) {
        this.creator = creator;
    }
    
    public List<User> getParticipants() {
        return participants;
    }
    
    public void setParticipants(List<User> participants) {
        this.participants = participants;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isHeld() {
        return isHeld;
    }

    public void setHeld(boolean held) {
        isHeld = held;
    }

    public String getKeyEvents() {
        return keyEvents;
    }

    public void setKeyEvents(String keyEvents) {
        this.keyEvents = keyEvents;
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public void setCampaign(Campaign campaign) {
        this.campaign = campaign;
    }
}
