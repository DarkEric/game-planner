package ru.ambryo.gameplannerback.dto;

import ru.ambryo.gameplannerback.entity.CampaignStatus;

import java.time.Instant;
import java.util.List;

public class CampaignDto {
    private Long id;
    private String name;
    private String description;
    private UserInfo creator;
    private CampaignStatus status;
    private Integer totalMilestones;
    private Integer completedMilestones;
    private List<CampaignPlayerDto> players;
    private List<Long> gameIds;
    private Instant createdAt;
    private Instant updatedAt;

    // Constructors
    public CampaignDto() {
    }

    public CampaignDto(Long id, String name, String description, UserInfo creator,
                       CampaignStatus status, Integer totalMilestones, Integer completedMilestones,
                       List<CampaignPlayerDto> players, List<Long> gameIds,
                       Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.creator = creator;
        this.status = status;
        this.totalMilestones = totalMilestones;
        this.completedMilestones = completedMilestones;
        this.players = players;
        this.gameIds = gameIds;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public UserInfo getCreator() {
        return creator;
    }

    public void setCreator(UserInfo creator) {
        this.creator = creator;
    }

    public CampaignStatus getStatus() {
        return status;
    }

    public void setStatus(CampaignStatus status) {
        this.status = status;
    }

    public Integer getTotalMilestones() {
        return totalMilestones;
    }

    public void setTotalMilestones(Integer totalMilestones) {
        this.totalMilestones = totalMilestones;
    }

    public Integer getCompletedMilestones() {
        return completedMilestones;
    }

    public void setCompletedMilestones(Integer completedMilestones) {
        this.completedMilestones = completedMilestones;
    }

    public List<CampaignPlayerDto> getPlayers() {
        return players;
    }

    public void setPlayers(List<CampaignPlayerDto> players) {
        this.players = players;
    }

    public List<Long> getGameIds() {
        return gameIds;
    }

    public void setGameIds(List<Long> gameIds) {
        this.gameIds = gameIds;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Helper method to calculate progress percentage
    public int getProgressPercentage() {
        if (totalMilestones == null || totalMilestones == 0) {
            return 0;
        }
        return (int) ((completedMilestones * 100.0) / totalMilestones);
    }
}
