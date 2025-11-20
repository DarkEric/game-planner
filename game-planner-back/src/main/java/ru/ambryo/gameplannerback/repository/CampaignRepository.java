package ru.ambryo.gameplannerback.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.ambryo.gameplannerback.entity.Campaign;
import ru.ambryo.gameplannerback.entity.User;

import java.util.List;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long> {
    
    List<Campaign> findByCreator(User creator);
    
    @Query("SELECT DISTINCT c FROM Campaign c " +
           "LEFT JOIN c.players cp " +
           "WHERE c.creator = :user OR cp.player = :user")
    List<Campaign> findByUserInvolvement(@Param("user") User user);
}
