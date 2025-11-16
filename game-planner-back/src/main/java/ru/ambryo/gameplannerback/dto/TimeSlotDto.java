package ru.ambryo.gameplannerback.dto;

import java.time.Instant;

public class TimeSlotDto {
    private Long id;
    private Instant start; // UTC время
    private Integer duration;
    
    public TimeSlotDto() {
    }
    
    public TimeSlotDto(Long id, Instant start, Integer duration) {
        this.id = id;
        this.start = start;
        this.duration = duration;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
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

