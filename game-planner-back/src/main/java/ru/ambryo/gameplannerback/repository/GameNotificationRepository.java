package ru.ambryo.gameplannerback.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.ambryo.gameplannerback.entity.Game;
import ru.ambryo.gameplannerback.entity.GameNotification;
import ru.ambryo.gameplannerback.entity.User;

import java.util.Optional;

@Repository
public interface GameNotificationRepository extends JpaRepository<GameNotification, Long> {
    
    @Query("SELECT gn FROM GameNotification gn WHERE gn.game = :game AND gn.notificationType = :type AND gn.user IS NULL")
    Optional<GameNotification> findGroupNotification(@Param("game") Game game, @Param("type") String type);
    
    @Query("SELECT gn FROM GameNotification gn WHERE gn.game = :game AND gn.notificationType = :type AND gn.user = :user")
    Optional<GameNotification> findPersonalNotification(@Param("game") Game game, @Param("type") String type, @Param("user") User user);
}
