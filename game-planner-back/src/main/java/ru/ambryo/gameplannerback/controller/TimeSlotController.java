package ru.ambryo.gameplannerback.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ru.ambryo.gameplannerback.dto.CreateTimeSlotRequest;
import ru.ambryo.gameplannerback.dto.TimeSlotDto;
import ru.ambryo.gameplannerback.entity.User;
import ru.ambryo.gameplannerback.service.UserService;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/players/{playerId}/time-slots")
@CrossOrigin(origins = "*")
public class TimeSlotController {
    
    @Autowired
    private UserService userService;
    
    @PostMapping
    public ResponseEntity<TimeSlotDto> addTimeSlot(
            @PathVariable Long playerId,
            @RequestBody CreateTimeSlotRequest request,
            Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            if (!user.getId().equals(playerId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            Integer duration = request.getDuration() != null ? request.getDuration() : 1;
            userService.toggleTimeSlot(user, request.getStart(), duration);
            
            // Возвращаем созданный слот
            TimeSlotDto timeSlot = new TimeSlotDto(null, request.getStart(), duration);
            return ResponseEntity.status(HttpStatus.CREATED).body(timeSlot);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @DeleteMapping
    public ResponseEntity<Void> removeTimeSlot(
            @PathVariable Long playerId,
            @RequestParam String start,
            Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            if (!user.getId().equals(playerId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            LocalDateTime startDateTime = LocalDateTime.parse(start);
            userService.toggleTimeSlot(user, startDateTime, 1);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    
}

