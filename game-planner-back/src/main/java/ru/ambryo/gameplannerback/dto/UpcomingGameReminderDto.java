package ru.ambryo.gameplannerback.dto;

public class UpcomingGameReminderDto {
    private Integer minutesBefore;
    private Boolean enabled;
    
    public UpcomingGameReminderDto() {
    }
    
    public UpcomingGameReminderDto(Integer minutesBefore, Boolean enabled) {
        this.minutesBefore = minutesBefore;
        this.enabled = enabled;
    }
    
    public Integer getMinutesBefore() {
        return minutesBefore;
    }
    
    public void setMinutesBefore(Integer minutesBefore) {
        this.minutesBefore = minutesBefore;
    }
    
    public Boolean getEnabled() {
        return enabled;
    }
    
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
