package ru.ambryo.gameplannerback.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ru.ambryo.gameplannerback.dto.CreateGameRequest;
import ru.ambryo.gameplannerback.dto.GameDto;
import ru.ambryo.gameplannerback.dto.MarkGameHeldRequest;
import ru.ambryo.gameplannerback.entity.User;
import ru.ambryo.gameplannerback.service.GameService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequestMapping("/api/games")
public class GameController {
    
    @Autowired
    private GameService gameService;
    
    @PostMapping
    public ResponseEntity<GameDto> createGame(
            @RequestBody CreateGameRequest request,
            Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            GameDto game = gameService.createGame(request, user);
            return ResponseEntity.status(HttpStatus.CREATED).body(game);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping
    public ResponseEntity<List<GameDto>> getGames(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            Instant start = startDate != null ? Instant.parse(startDate) : Instant.now();
            Instant end = endDate != null ? Instant.parse(endDate) : start.plus(30, ChronoUnit.DAYS);
            
            List<GameDto> games = gameService.getGamesBetween(start, end);
            return ResponseEntity.ok(games);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/my")
    public ResponseEntity<List<GameDto>> getMyGames(Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            List<GameDto> games = gameService.getUpcomingGamesForUser(user.getId());
            return ResponseEntity.ok(games);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/{gameId}")
    public ResponseEntity<GameDto> getGameById(@PathVariable Long gameId) {
        try {
            GameDto game = gameService.getGameById(gameId);
            return ResponseEntity.ok(game);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/{gameId}")
    public ResponseEntity<Void> deleteGame(
            @PathVariable Long gameId,
            @RequestParam(required = false) String cancellationReason,
            Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            gameService.deleteGame(gameId, user, cancellationReason);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }
    
    @PostMapping("/{gameId}/join")
    public ResponseEntity<GameDto> joinGame(
            @PathVariable Long gameId,
            Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            GameDto game = gameService.joinGame(gameId, user);
            return ResponseEntity.ok(game);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping("/{gameId}/leave")
    public ResponseEntity<GameDto> leaveGame(
            @PathVariable Long gameId,
            Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            GameDto game = gameService.leaveGame(gameId, user);
            return ResponseEntity.ok(game);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{gameId}/hold")
    public ResponseEntity<GameDto> markGameAsHeld(
            @PathVariable Long gameId,
            @RequestBody MarkGameHeldRequest request,
            Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            GameDto game = gameService.markGameAsHeld(gameId, request.getKeyEvents(), user);
            return ResponseEntity.ok(game);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
