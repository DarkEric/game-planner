package ru.ambryo.gameplannerback.dto;

public class CampaignPlayerDto {
    private Long id;
    private Long playerId;
    private String playerName;
    private Long joinedInGameId;
    private Integer sessionNumber;
    private String characterName;
    private String characterClass;
    private String characterNotes;

    // Constructors
    public CampaignPlayerDto() {
    }

    public CampaignPlayerDto(Long id, Long playerId, String playerName,
                             Long joinedInGameId, Integer sessionNumber,
                             String characterName, String characterClass,
                             String characterNotes) {
        this.id = id;
        this.playerId = playerId;
        this.playerName = playerName;
        this.joinedInGameId = joinedInGameId;
        this.sessionNumber = sessionNumber;
        this.characterName = characterName;
        this.characterClass = characterClass;
        this.characterNotes = characterNotes;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public Long getJoinedInGameId() {
        return joinedInGameId;
    }

    public void setJoinedInGameId(Long joinedInGameId) {
        this.joinedInGameId = joinedInGameId;
    }

    public Integer getSessionNumber() {
        return sessionNumber;
    }

    public void setSessionNumber(Integer sessionNumber) {
        this.sessionNumber = sessionNumber;
    }

    public String getCharacterName() {
        return characterName;
    }

    public void setCharacterName(String characterName) {
        this.characterName = characterName;
    }

    public String getCharacterClass() {
        return characterClass;
    }

    public void setCharacterClass(String characterClass) {
        this.characterClass = characterClass;
    }

    public String getCharacterNotes() {
        return characterNotes;
    }

    public void setCharacterNotes(String characterNotes) {
        this.characterNotes = characterNotes;
    }
}
