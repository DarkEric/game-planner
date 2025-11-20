package ru.ambryo.gameplannerback.dto;

public class CreateCampaignRequest {
    private String name;
    private String description;
    private Integer totalMilestones;

    // Constructors
    public CreateCampaignRequest() {
    }

    public CreateCampaignRequest(String name, String description, Integer totalMilestones) {
        this.name = name;
        this.description = description;
        this.totalMilestones = totalMilestones;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getTotalMilestones() {
        return totalMilestones;
    }

    public void setTotalMilestones(Integer totalMilestones) {
        this.totalMilestones = totalMilestones;
    }
}
