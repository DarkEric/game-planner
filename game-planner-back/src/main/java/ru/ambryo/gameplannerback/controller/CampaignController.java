package ru.ambryo.gameplannerback.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ru.ambryo.gameplannerback.dto.CampaignDto;
import ru.ambryo.gameplannerback.dto.CampaignInviteDto;
import ru.ambryo.gameplannerback.dto.CampaignPlayerDto;
import ru.ambryo.gameplannerback.dto.CreateCampaignRequest;
import ru.ambryo.gameplannerback.entity.CampaignStatus;
import ru.ambryo.gameplannerback.entity.User;
import ru.ambryo.gameplannerback.service.CampaignService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/campaigns")
@CrossOrigin(origins = "*")
public class CampaignController {

    private final CampaignService campaignService;

    public CampaignController(CampaignService campaignService) {
        this.campaignService = campaignService;
    }

    @PostMapping
    public ResponseEntity<CampaignDto> createCampaign(
            @RequestBody CreateCampaignRequest request,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        CampaignDto campaign = campaignService.createCampaign(request, user.getId());
        return ResponseEntity.ok(campaign);
    }

    @GetMapping
    public ResponseEntity<List<CampaignDto>> getUserCampaigns(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        List<CampaignDto> campaigns = campaignService.getUserCampaigns(user.getId());
        return ResponseEntity.ok(campaigns);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CampaignDto> getCampaignDetails(
            @PathVariable Long id,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        CampaignDto campaign = campaignService.getCampaignDetails(id, user.getId());
        return ResponseEntity.ok(campaign);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<CampaignDto> updateCampaignStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        CampaignStatus status = CampaignStatus.valueOf(request.get("status"));
        CampaignDto campaign = campaignService.updateCampaignStatus(id, status, user.getId());
        return ResponseEntity.ok(campaign);
    }

    @PutMapping("/{id}/milestones")
    public ResponseEntity<CampaignDto> updateMilestones(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> request,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Integer completedMilestones = request.get("completedMilestones");
        Integer totalMilestones = request.get("totalMilestones");
        CampaignDto campaign = campaignService.updateMilestones(id, completedMilestones, totalMilestones, user.getId());
        return ResponseEntity.ok(campaign);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CampaignDto> updateCampaign(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        String name = request.get("name");
        String description = request.get("description");
        CampaignDto campaign = campaignService.updateCampaign(id, name, description, user.getId());
        return ResponseEntity.ok(campaign);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCampaign(
            @PathVariable Long id,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        campaignService.deleteCampaign(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/games/{gameId}")
    public ResponseEntity<CampaignDto> addGameToCampaign(
            @PathVariable Long id,
            @PathVariable Long gameId,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        CampaignDto campaign = campaignService.addGameToCampaign(id, gameId, user.getId());
        return ResponseEntity.ok(campaign);
    }

    @DeleteMapping("/{id}/games/{gameId}")
    public ResponseEntity<CampaignDto> removeGameFromCampaign(
            @PathVariable Long gameId,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        CampaignDto campaign = campaignService.removeGameFromCampaign(gameId, user.getId());
        return ResponseEntity.ok(campaign);
    }

    @PostMapping("/{id}/players")
    public ResponseEntity<CampaignPlayerDto> addPlayerToCampaign(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        Long playerId = Long.valueOf(request.get("playerId").toString());
        Long joinedInGameId = request.get("joinedInGameId") != null 
                ? Long.valueOf(request.get("joinedInGameId").toString()) 
                : null;
        String characterName = (String) request.get("characterName");
        String characterClass = (String) request.get("characterClass");
        String characterNotes = (String) request.get("characterNotes");
        
        CampaignPlayerDto player = campaignService.addPlayerToCampaign(
                id, playerId, joinedInGameId, characterName, characterClass, characterNotes, user.getId());
        return ResponseEntity.ok(player);
    }

    @PutMapping("/{id}/players/{playerId}")
    public ResponseEntity<CampaignPlayerDto> updateCharacter(
            @PathVariable Long playerId,
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        String characterName = request.get("characterName");
        String characterClass = request.get("characterClass");
        String characterNotes = request.get("characterNotes");
        
        CampaignPlayerDto player = campaignService.updateCharacter(
                playerId, characterName, characterClass, characterNotes, user.getId());
        return ResponseEntity.ok(player);
    }

    @DeleteMapping("/{id}/players/{playerId}")
    public ResponseEntity<Void> removePlayerFromCampaign(
            @PathVariable Long id,
            @PathVariable Long playerId,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        campaignService.removePlayerFromCampaign(id, playerId, user.getId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/invites")
    public ResponseEntity<CampaignInviteDto> invitePlayer(
            @PathVariable Long id,
            @RequestBody Map<String, Long> request,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Long playerId = request.get("playerId");
        CampaignInviteDto invite = campaignService.invitePlayerToCampaign(id, playerId, user.getId());
        return ResponseEntity.ok(invite);
    }

    @GetMapping("/{id}/invites")
    public ResponseEntity<List<CampaignInviteDto>> getCampaignInvites(
            @PathVariable Long id,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        List<CampaignInviteDto> invites = campaignService.getCampaignInvites(id, user.getId());
        return ResponseEntity.ok(invites);
    }

    @GetMapping("/invites/pending")
    public ResponseEntity<List<CampaignInviteDto>> getPendingInvites(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        List<CampaignInviteDto> invites = campaignService.getPendingInvites(user.getId());
        return ResponseEntity.ok(invites);
    }

    @PostMapping("/invites/{inviteId}/accept")
    public ResponseEntity<CampaignDto> acceptInvite(
            @PathVariable Long inviteId,
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        String characterName = request.get("characterName");
        String characterClass = request.get("characterClass");
        String characterNotes = request.get("characterNotes");
        CampaignDto campaign = campaignService.acceptCampaignInvite(
                inviteId, characterName, characterClass, characterNotes, user.getId());
        return ResponseEntity.ok(campaign);
    }

    @PostMapping("/invites/{inviteId}/decline")
    public ResponseEntity<Void> declineInvite(
            @PathVariable Long inviteId,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        campaignService.declineCampaignInvite(inviteId, user.getId());
        return ResponseEntity.ok().build();
    }
}
