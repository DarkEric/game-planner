package ru.ambryo.gameplannerback.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.ambryo.gameplannerback.entity.Game;

import java.time.Instant;
import java.util.List;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {
    
    @Query("SELECT g FROM Game g WHERE g.startTime >= :startDate AND g.startTime < :endDate")
    List<Game> findGamesBetween(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);
    
    @Query("SELECT g FROM Game g JOIN g.participants p WHERE p.id = :userId AND g.startTime >= :startDate")
    List<Game> findUpcomingGamesByUser(@Param("userId") Long userId, @Param("startDate") Instant startDate);
    
    @Query("SELECT g FROM Game g WHERE g.startTime >= :start AND g.startTime < :end")
    List<Game> findGamesStartingBetween(@Param("start") Instant start, @Param("end") Instant end);
    
    @Query("SELECT g FROM Game g JOIN g.participants p WHERE p.id = :userId AND g.startTime >= :start AND g.startTime < :end")
    List<Game> findGamesByParticipantStartingBetween(@Param("userId") Long userId, @Param("start") Instant start, @Param("end") Instant end);
    
    @Query("SELECT g FROM Game g WHERE g.endTime < :before AND g.isHeld = false")
    List<Game> findGamesEndedButNotHeld(@Param("before") Instant before);
    
    @Query("SELECT DISTINCT g FROM Game g LEFT JOIN g.participants p WHERE g.creator.id = :userId OR p.id = :userId ORDER BY g.startTime DESC")
    List<Game> findAllGamesByUser(@Param("userId") Long userId);
    
    @Query("SELECT g FROM Game g WHERE g.creator.id = :userId ORDER BY g.startTime DESC")
    List<Game> findGamesCreatedByUser(@Param("userId") Long userId);
}
