package ru.ambryo.gameplannerback.dto;

import java.time.Instant;
import java.util.List;

public class GameDto {
    private Long id;
    private Instant startTime;
    private Instant endTime;
    private Long creatorId;
    private String creatorName;
    private String title;
    private String description;
    private List<ParticipantDto> participants;
    private Instant createdAt;
    private boolean isHeld;
    private String keyEvents;
    
    public GameDto() {
    }
    
    public GameDto(Long id, Instant startTime, Instant endTime, Long creatorId, String creatorName, 
                   String title, String description, List<ParticipantDto> participants, Instant createdAt,
                   boolean isHeld, String keyEvents) {
        this.id = id;
        this.startTime = startTime;
        this.endTime = endTime;
        this.creatorId = creatorId;
        this.creatorName = creatorName;
        this.title = title;
        this.description = description;
        this.participants = participants;
        this.createdAt = createdAt;
        this.isHeld = isHeld;
        this.keyEvents = keyEvents;
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
    
    public Long getCreatorId() {
        return creatorId;
    }
    
    public void setCreatorId(Long creatorId) {
        this.creatorId = creatorId;
    }
    
    public String getCreatorName() {
        return creatorName;
    }
    
    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }
    
    public List<ParticipantDto> getParticipants() {
        return participants;
    }
    
    public void setParticipants(List<ParticipantDto> participants) {
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
    
    public static class ParticipantDto {
        private Long id;
        private String name;
        private String color;
        
        public ParticipantDto() {
        }
        
        public ParticipantDto(Long id, String name, String color) {
            this.id = id;
            this.name = name;
            this.color = color;
        }
        
        public Long getId() {
            return id;
        }
        
        public void setId(Long id) {
            this.id = id;
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
    }
}
