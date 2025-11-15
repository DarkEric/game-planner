package ru.ambryo.gameplannerback.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ru.ambryo.gameplannerback.dto.CreatePlayerRequest;
import ru.ambryo.gameplannerback.dto.PlayerDto;
import ru.ambryo.gameplannerback.dto.ToggleTimeSlotRequest;
import ru.ambryo.gameplannerback.dto.ToggleTimeSlotsRequest;
import ru.ambryo.gameplannerback.entity.User;
import ru.ambryo.gameplannerback.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/players")
@CrossOrigin(origins = "*")
public class PlayerController {

    private static final Logger log = LoggerFactory.getLogger(PlayerController.class);
    @Autowired
    private UserService userService;
    
    @GetMapping
    public ResponseEntity<List<PlayerDto>> getAllPlayers() {
        List<PlayerDto> players = userService.getAllUsers();
        return ResponseEntity.ok(players);
    }
    
    @GetMapping("/me")
    public ResponseEntity<PlayerDto> getCurrentPlayer(Authentication authentication) {
        try {
            log.info("getCurrentPlayer {}", authentication);
            User user = (User) authentication.getPrincipal();
            log.info("getCurrentPlayer {}", user);
            PlayerDto player = userService.getUserAsPlayer(user);
            log.info("getCurrentPlayer {}", player);
            return ResponseEntity.ok(player);
        } catch (RuntimeException e) {
            log.error("getCurrentPlayer", e);
            return ResponseEntity.notFound().build();
        }
    }
    
    @PutMapping("/me")
    public ResponseEntity<PlayerDto> updateCurrentPlayer(
            @RequestBody CreatePlayerRequest request,
            Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            PlayerDto player = userService.updateUserProfile(user, request.getName(), request.getColor());
            return ResponseEntity.ok(player);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping(value = "/me/time-slots/toggle", consumes = "application/json", produces = "application/json")
    public ResponseEntity<PlayerDto> toggleTimeSlot(
            @RequestBody ToggleTimeSlotRequest request,
            Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            if (request == null || request.getStart() == null) {
                return ResponseEntity.badRequest().build();
            }
            Integer duration = request.getDuration() != null ? request.getDuration() : 1;
            userService.toggleTimeSlot(user, request.getStart(), duration);
            PlayerDto updatedPlayer = userService.getUserAsPlayer(user);
            return ResponseEntity.ok(updatedPlayer);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
    
    @PostMapping("/me/time-slots/toggle-batch")
    public ResponseEntity<PlayerDto> toggleTimeSlots(
            @RequestBody ToggleTimeSlotsRequest request,
            Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            // Обрабатываем каждый слот
            for (ToggleTimeSlotsRequest.TimeSlotRequest slot : request.getSlots()) {
                userService.toggleTimeSlot(user, slot.getStart(), slot.getDuration());
            }
            PlayerDto updatedPlayer = userService.getUserAsPlayer(user);
            return ResponseEntity.ok(updatedPlayer);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}

