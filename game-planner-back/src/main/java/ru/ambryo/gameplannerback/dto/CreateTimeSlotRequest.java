package ru.ambryo.gameplannerback.dto;

import java.time.Instant;

public class CreateTimeSlotRequest {
    private Instant start; // UTC время
    private Integer duration;
    
    public CreateTimeSlotRequest() {
    }
    
    public CreateTimeSlotRequest(Instant start, Integer duration) {
        this.start = start;
        this.duration = duration;
    }
    
    public Instant getStart() {
        return start;
    }
    
    public void setStart(Instant start) {
        this.start = start;
    }
    
    public Integer getDuration() {
        return duration;
    }
    
    public void setDuration(Integer duration) {
        this.duration = duration;
    }
}

