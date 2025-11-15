package ru.ambryo.gameplannerback.dto;

import java.util.ArrayList;
import java.util.List;

public class PlayerDto {
    private Long id;
    private String name;
    private String color;
    private List<TimeSlotDto> availableTimes = new ArrayList<>();
    
    public PlayerDto() {
    }
    
    public PlayerDto(Long id, String name, String color) {
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
    
    public List<TimeSlotDto> getAvailableTimes() {
        return availableTimes;
    }
    
    public void setAvailableTimes(List<TimeSlotDto> availableTimes) {
        this.availableTimes = availableTimes;
    }
}

