package ru.ambryo.gameplannerback.dto;

import java.time.Instant;
import java.util.List;

public class TimeSlotsBatchRequest {
    private List<TimeSlotItem> slots;

    public TimeSlotsBatchRequest() {
    }

    public TimeSlotsBatchRequest(List<TimeSlotItem> slots) {
        this.slots = slots;
    }

    public List<TimeSlotItem> getSlots() {
        return slots;
    }

    public void setSlots(List<TimeSlotItem> slots) {
        this.slots = slots;
    }

    public static class TimeSlotItem {
        private Instant start;
        private Integer duration;

        public TimeSlotItem() {
        }

        public TimeSlotItem(Instant start, Integer duration) {
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
