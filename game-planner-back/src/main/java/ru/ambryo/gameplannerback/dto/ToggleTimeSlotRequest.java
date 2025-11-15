package ru.ambryo.gameplannerback.dto;

import java.time.LocalDateTime;

public class ToggleTimeSlotRequest {
    private LocalDateTime start;
    private Integer duration;
    
    public ToggleTimeSlotRequest() {
    }
    
    public ToggleTimeSlotRequest(LocalDateTime start, Integer duration) {
        this.start = start;
        this.duration = duration;
    }
    
    public LocalDateTime getStart() {
        return start;
    }
    
    public void setStart(LocalDateTime start) {
        this.start = start;
    }
    
    public Integer getDuration() {
        return duration;
    }
    
    public void setDuration(Integer duration) {
        this.duration = duration;
    }
}

