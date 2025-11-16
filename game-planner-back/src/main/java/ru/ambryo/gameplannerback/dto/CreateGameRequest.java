package ru.ambryo.gameplannerback.dto;

import java.time.Instant;
import java.util.List;

public class CreateGameRequest {
    private Instant startTime;
    private Instant endTime;
    private String title;
    private String description;
    private List<Long> participantIds;
    
    public CreateGameRequest() {
    }
    
    public CreateGameRequest(Instant startTime, Instant endTime, String title, String description, List<Long> participantIds) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.title = title;
        this.description = description;
        this.participantIds = participantIds;
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
    
    public List<Long> getParticipantIds() {
        return participantIds;
    }
    
    public void setParticipantIds(List<Long> participantIds) {
        this.participantIds = participantIds;
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
}
