package ru.ambryo.gameplannerback.dto;

import ru.ambryo.gameplannerback.entity.CampaignInvite;

import java.time.Instant;

public class CampaignInviteDto {
    private Long id;
    private Long campaignId;
    private String campaignName;
    private Long invitedUserId;
    private String invitedUserName;
    private CampaignInvite.InviteStatus status;
    private Instant createdAt;
    private Instant respondedAt;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(Long campaignId) {
        this.campaignId = campaignId;
    }

    public String getCampaignName() {
        return campaignName;
    }

    public void setCampaignName(String campaignName) {
        this.campaignName = campaignName;
    }

    public Long getInvitedUserId() {
        return invitedUserId;
    }

    public void setInvitedUserId(Long invitedUserId) {
        this.invitedUserId = invitedUserId;
    }

    public String getInvitedUserName() {
        return invitedUserName;
    }

    public void setInvitedUserName(String invitedUserName) {
        this.invitedUserName = invitedUserName;
    }

    public CampaignInvite.InviteStatus getStatus() {
        return status;
    }

    public void setStatus(CampaignInvite.InviteStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(Instant respondedAt) {
        this.respondedAt = respondedAt;
    }
}
