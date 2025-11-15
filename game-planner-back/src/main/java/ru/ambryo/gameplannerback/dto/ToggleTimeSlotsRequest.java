package ru.ambryo.gameplannerback.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ToggleTimeSlotsRequest {
    private List<TimeSlotRequest> slots;
    
    public ToggleTimeSlotsRequest() {
    }
    
    public ToggleTimeSlotsRequest(List<TimeSlotRequest> slots) {
        this.slots = slots;
    }
    
    public List<TimeSlotRequest> getSlots() {
        return slots;
    }
    
    public void setSlots(List<TimeSlotRequest> slots) {
        this.slots = slots;
    }
    
    public static class TimeSlotRequest {
        private LocalDateTime start;
        private Integer duration;
        
        public TimeSlotRequest() {
        }
        
        public TimeSlotRequest(LocalDateTime start, Integer duration) {
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
}

