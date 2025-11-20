package ru.ambryo.gameplannerback.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ambryo.gameplannerback.dto.*;
import ru.ambryo.gameplannerback.entity.*;
import ru.ambryo.gameplannerback.repository.CampaignPlayerRepository;
import ru.ambryo.gameplannerback.repository.CampaignRepository;
import ru.ambryo.gameplannerback.repository.GameRepository;
import ru.ambryo.gameplannerback.repository.UserRepository;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final CampaignPlayerRepository campaignPlayerRepository;
    private final UserRepository userRepository;
    private final GameRepository gameRepository;

    public CampaignService(CampaignRepository campaignRepository,
                          CampaignPlayerRepository campaignPlayerRepository,
                          UserRepository userRepository,
                          GameRepository gameRepository) {
        this.campaignRepository = campaignRepository;
        this.campaignPlayerRepository = campaignPlayerRepository;
        this.userRepository = userRepository;
        this.gameRepository = gameRepository;
    }

    @Transactional
    public CampaignDto createCampaign(CreateCampaignRequest request, Long userId) {
        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Campaign campaign = new Campaign();
        campaign.setName(request.getName());
        campaign.setDescription(request.getDescription());
        campaign.setCreator(creator);
        campaign.setTotalMilestones(request.getTotalMilestones());
        campaign.setCompletedMilestones(0);
        campaign.setStatus(CampaignStatus.ACTIVE);

        Campaign saved = campaignRepository.save(campaign);
        return convertToDto(saved, userId);
    }

    @Transactional(readOnly = true)
    public List<CampaignDto> getUserCampaigns(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Campaign> campaigns = campaignRepository.findByUserInvolvement(user);
        return campaigns.stream()
                .map(campaign -> convertToDto(campaign, userId))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CampaignDto getCampaignDetails(Long campaignId, Long userId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));

        return convertToDto(campaign, userId);
    }

    @Transactional
    public CampaignDto updateCampaignStatus(Long campaignId, CampaignStatus status, Long userId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));

        if (!campaign.getCreator().getId().equals(userId)) {
            throw new RuntimeException("Only campaign creator can update status");
        }

        campaign.setStatus(status);
        Campaign saved = campaignRepository.save(campaign);
        return convertToDto(saved, userId);
    }

    @Transactional
    public CampaignDto updateMilestones(Long campaignId, Integer completedMilestones, Integer totalMilestones, Long userId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));

        if (!campaign.getCreator().getId().equals(userId)) {
            throw new RuntimeException("Only campaign master can update milestones");
        }

        if (completedMilestones != null) {
            campaign.setCompletedMilestones(completedMilestones);
        }
        if (totalMilestones != null) {
            campaign.setTotalMilestones(totalMilestones);
        }
        Campaign saved = campaignRepository.save(campaign);
        return convertToDto(saved, userId);
    }

    @Transactional
    public CampaignDto addGameToCampaign(Long campaignId, Long gameId, Long userId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));

        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game not found"));

        if (!campaign.getCreator().getId().equals(userId)) {
            throw new RuntimeException("Only campaign creator can add games");
        }

        game.setCampaign(campaign);
        gameRepository.save(game);

        return convertToDto(campaign, userId);
    }

    @Transactional
    public CampaignDto removeGameFromCampaign(Long gameId, Long userId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game not found"));

        if (game.getCampaign() == null) {
            throw new RuntimeException("Game is not part of any campaign");
        }

        Campaign campaign = game.getCampaign();
        if (!campaign.getCreator().getId().equals(userId)) {
            throw new RuntimeException("Only campaign creator can remove games");
        }

        game.setCampaign(null);
        gameRepository.save(game);

        return convertToDto(campaign, userId);
    }

    @Transactional
    public CampaignPlayerDto addPlayerToCampaign(Long campaignId, Long playerId, 
                                                 Long joinedInGameId, String characterName, 
                                                 String characterClass, String characterNotes, 
                                                 Long userId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));

        if (!campaign.getCreator().getId().equals(userId)) {
            throw new RuntimeException("Only campaign creator can add players");
        }

        User player = userRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Player not found"));

        Game joinedInGame = null;
        if (joinedInGameId != null) {
            joinedInGame = gameRepository.findById(joinedInGameId)
                    .orElseThrow(() -> new RuntimeException("Game not found"));
        }

        CampaignPlayer campaignPlayer = new CampaignPlayer();
        campaignPlayer.setCampaign(campaign);
        campaignPlayer.setPlayer(player);
        campaignPlayer.setJoinedInGame(joinedInGame);
        campaignPlayer.setCharacterName(characterName);
        campaignPlayer.setCharacterClass(characterClass);
        campaignPlayer.setCharacterNotes(characterNotes);

        CampaignPlayer saved = campaignPlayerRepository.save(campaignPlayer);
        return convertPlayerToDto(saved, campaign);
    }

    @Transactional
    public CampaignPlayerDto updateCharacter(Long campaignPlayerId, String characterName,
                                            String characterClass, String characterNotes,
                                            Long userId) {
        CampaignPlayer campaignPlayer = campaignPlayerRepository.findById(campaignPlayerId)
                .orElseThrow(() -> new RuntimeException("Campaign player not found"));

        Campaign campaign = campaignPlayer.getCampaign();
        if (!campaign.getCreator().getId().equals(userId)) {
            throw new RuntimeException("Only campaign creator can update characters");
        }

        campaignPlayer.setCharacterName(characterName);
        campaignPlayer.setCharacterClass(characterClass);
        campaignPlayer.setCharacterNotes(characterNotes);

        CampaignPlayer saved = campaignPlayerRepository.save(campaignPlayer);
        return convertPlayerToDto(saved, campaign);
    }

    @Transactional
    public void removePlayerFromCampaign(Long campaignId, Long playerId, Long userId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));

        if (!campaign.getCreator().getId().equals(userId)) {
            throw new RuntimeException("Only campaign creator can remove players");
        }

        User player = userRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Player not found"));

        campaignPlayerRepository.deleteByCampaignAndPlayer(campaign, player);
    }

    private CampaignDto convertToDto(Campaign campaign, Long currentUserId) {
        CampaignDto dto = new CampaignDto();
        dto.setId(campaign.getId());
        dto.setName(campaign.getName());
        dto.setDescription(campaign.getDescription());
        
        UserInfo creator = new UserInfo(
                campaign.getCreator().getId(),
                campaign.getCreator().getName()
        );
        dto.setCreator(creator);
        
        dto.setStatus(campaign.getStatus());
        
        // Only show milestone details to campaign creator
        if (campaign.getCreator().getId().equals(currentUserId)) {
            dto.setTotalMilestones(campaign.getTotalMilestones());
            dto.setCompletedMilestones(campaign.getCompletedMilestones());
        } else {
            // Players only see if there are milestones, but not the exact numbers
            dto.setTotalMilestones(campaign.getTotalMilestones() != null && campaign.getTotalMilestones() > 0 ? 1 : null);
            dto.setCompletedMilestones(campaign.getCompletedMilestones());
        }
        
        List<CampaignPlayerDto> players = campaign.getPlayers().stream()
                .map(cp -> convertPlayerToDto(cp, campaign))
                .collect(Collectors.toList());
        dto.setPlayers(players);
        
        List<Long> gameIds = campaign.getGames().stream()
                .sorted(Comparator.comparing(Game::getStartTime))
                .map(Game::getId)
                .collect(Collectors.toList());
        dto.setGameIds(gameIds);
        
        dto.setCreatedAt(campaign.getCreatedAt());
        dto.setUpdatedAt(campaign.getUpdatedAt());
        
        return dto;
    }

    private CampaignPlayerDto convertPlayerToDto(CampaignPlayer cp, Campaign campaign) {
        CampaignPlayerDto dto = new CampaignPlayerDto();
        dto.setId(cp.getId());
        dto.setPlayerId(cp.getPlayer().getId());
        dto.setPlayerName(cp.getPlayer().getName());
        dto.setJoinedInGameId(cp.getJoinedInGame() != null ? cp.getJoinedInGame().getId() : null);
        
        // Calculate session number based on game date
        if (cp.getJoinedInGame() != null) {
            List<Game> sortedGames = campaign.getGames().stream()
                    .sorted(Comparator.comparing(Game::getStartTime))
                    .collect(Collectors.toList());
            
            for (int i = 0; i < sortedGames.size(); i++) {
                if (sortedGames.get(i).getId().equals(cp.getJoinedInGame().getId())) {
                    dto.setSessionNumber(i + 1);
                    break;
                }
            }
        }
        
        dto.setCharacterName(cp.getCharacterName());
        dto.setCharacterClass(cp.getCharacterClass());
        dto.setCharacterNotes(cp.getCharacterNotes());
        
        return dto;
    }
}
