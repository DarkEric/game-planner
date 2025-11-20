package ru.ambryo.gameplannerback.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.ambryo.gameplannerback.entity.Campaign;
import ru.ambryo.gameplannerback.entity.CampaignInvite;
import ru.ambryo.gameplannerback.entity.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface CampaignInviteRepository extends JpaRepository<CampaignInvite, Long> {
    
    List<CampaignInvite> findByInvitedUserAndStatus(User invitedUser, CampaignInvite.InviteStatus status);
    
    List<CampaignInvite> findByCampaign(Campaign campaign);
    
    Optional<CampaignInvite> findByCampaignAndInvitedUser(Campaign campaign, User invitedUser);
    
    boolean existsByCampaignAndInvitedUserAndStatus(Campaign campaign, User invitedUser, CampaignInvite.InviteStatus status);
}
