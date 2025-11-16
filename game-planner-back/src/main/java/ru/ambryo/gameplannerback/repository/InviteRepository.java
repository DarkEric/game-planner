package ru.ambryo.gameplannerback.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.ambryo.gameplannerback.entity.Invite;
import ru.ambryo.gameplannerback.entity.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface InviteRepository extends JpaRepository<Invite, Long> {
    
    Optional<Invite> findByCode(String code);
    
    List<Invite> findByCreatedBy(User user);
    
    List<Invite> findByCreatedByOrderByCreatedAtDesc(User user);
}
