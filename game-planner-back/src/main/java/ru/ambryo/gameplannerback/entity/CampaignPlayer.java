package ru.ambryo.gameplannerback.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "campaign_players")
public class CampaignPlayer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @ManyToOne
    @JoinColumn(name = "player_id", nullable = false)
    private User player;

    @ManyToOne
    @JoinColumn(name = "joined_in_game_id")
    private Game joinedInGame;

    @Column(name = "character_name")
    private String characterName;

    @Column(name = "character_class", length = 100)
    private String characterClass;

    @Column(name = "character_notes", columnDefinition = "TEXT")
    private String characterNotes;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public void setCampaign(Campaign campaign) {
        this.campaign = campaign;
    }

    public User getPlayer() {
        return player;
    }

    public void setPlayer(User player) {
        this.player = player;
    }

    public Game getJoinedInGame() {
        return joinedInGame;
    }

    public void setJoinedInGame(Game joinedInGame) {
        this.joinedInGame = joinedInGame;
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
