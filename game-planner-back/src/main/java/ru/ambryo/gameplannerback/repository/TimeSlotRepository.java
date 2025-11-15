package ru.ambryo.gameplannerback.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.ambryo.gameplannerback.entity.TimeSlot;
import ru.ambryo.gameplannerback.entity.User;

import java.util.List;

@Repository
public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {
    List<TimeSlot> findByUser(User user);
}

