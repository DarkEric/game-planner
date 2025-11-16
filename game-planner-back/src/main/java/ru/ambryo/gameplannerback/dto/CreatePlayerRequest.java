package ru.ambryo.gameplannerback.dto;

public class CreatePlayerRequest {
    private String name;
    private String color;
    private String timezone;
    
    public CreatePlayerRequest() {
    }
    
    public CreatePlayerRequest(String name, String color) {
        this.name = name;
        this.color = color;
    }
    
    public CreatePlayerRequest(String name, String color, String timezone) {
        this.name = name;
        this.color = color;
        this.timezone = timezone;
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
    
    public String getTimezone() {
        return timezone;
    }
    
    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }
}

