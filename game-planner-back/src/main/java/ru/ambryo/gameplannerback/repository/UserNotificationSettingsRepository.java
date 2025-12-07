package ru.ambryo.gameplannerback.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.ambryo.gameplannerback.entity.User;
import ru.ambryo.gameplannerback.entity.UserNotificationSettings;

import java.util.Optional;

@Repository
public interface UserNotificationSettingsRepository extends JpaRepository<UserNotificationSettings, Long> {
    
    Optional<UserNotificationSettings> findByUserId(Long userId);
    
    Optional<UserNotificationSettings> findByUser(User user);
}
