package ru.ambryo.gameplannerback.dto;

import java.time.Instant;
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
        private Instant start; // UTC время
        private Integer duration;
        
        public TimeSlotRequest() {
        }
        
        public TimeSlotRequest(Instant start, Integer duration) {
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
}

