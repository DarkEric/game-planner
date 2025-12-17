package ru.ambryo.gameplannerback.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.ambryo.gameplannerback.entity.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByTelegramUserId(Long telegramUserId);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    List<User> findByTelegramSubscribedTrue();
    long countByIsAdminTrue();
    java.util.Optional<User> findFirstByOrderByIdAsc();
}

