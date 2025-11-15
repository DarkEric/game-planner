package ru.ambryo.gameplannerback.dto;

import java.time.LocalDateTime;

public class TimeSlotDto {
    private Long id;
    private LocalDateTime start;
    private Integer duration;
    
    public TimeSlotDto() {
    }
    
    public TimeSlotDto(Long id, LocalDateTime start, Integer duration) {
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

