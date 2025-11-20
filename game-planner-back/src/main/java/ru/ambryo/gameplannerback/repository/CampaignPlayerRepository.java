package ru.ambryo.gameplannerback.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.ambryo.gameplannerback.entity.Campaign;
import ru.ambryo.gameplannerback.entity.CampaignPlayer;
import ru.ambryo.gameplannerback.entity.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface CampaignPlayerRepository extends JpaRepository<CampaignPlayer, Long> {
    
    List<CampaignPlayer> findByCampaign(Campaign campaign);
    
    Optional<CampaignPlayer> findByCampaignAndPlayer(Campaign campaign, User player);
    
    void deleteByCampaignAndPlayer(Campaign campaign, User player);
}
