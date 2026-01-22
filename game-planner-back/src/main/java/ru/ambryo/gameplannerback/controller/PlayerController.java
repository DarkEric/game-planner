package ru.ambryo.gameplannerback.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Игроки", description = "API для управления профилями игроков и временными слотами")
@SecurityRequirement(name = "Bearer Authentication")
public class PlayerController {

    private static final Logger log = LoggerFactory.getLogger(PlayerController.class);
    @Autowired
    private UserService userService;
    
    @Operation(
        summary = "Получить список всех игроков",
        description = "Возвращает список всех игроков с их временными слотами в указанном диапазоне дат"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Список игроков успешно получен"),
        @ApiResponse(responseCode = "400", description = "Неверный формат даты")
    })
    @GetMapping
    public ResponseEntity<List<PlayerDto>> getAllPlayers(
            @Parameter(description = "Начальная дата в формате ISO-8601")
            @RequestParam(required = false) String startDate,
            @Parameter(description = "Конечная дата в формате ISO-8601")
            @RequestParam(required = false) String endDate) {
        try {
            java.time.Instant start = startDate != null ? java.time.Instant.parse(startDate) : null;
            java.time.Instant end = endDate != null ? java.time.Instant.parse(endDate) : null;
            
            List<PlayerDto> players = userService.getAllUsersWithTimeSlots(start, end);
            return ResponseEntity.ok(players);
        } catch (Exception e) {
            log.error("getAllPlayers error", e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    @Operation(
        summary = "Получить текущего игрока",
        description = "Возвращает информацию о текущем авторизованном игроке с временными слотами"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Информация об игроке успешно получена"),
        @ApiResponse(responseCode = "401", description = "Требуется авторизация"),
        @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    })
    @GetMapping("/me")
    public ResponseEntity<PlayerDto> getCurrentPlayer(
            Authentication authentication,
            @Parameter(description = "Начальная дата в формате ISO-8601")
            @RequestParam(required = false) String startDate,
            @Parameter(description = "Конечная дата в формате ISO-8601")
            @RequestParam(required = false) String endDate) {
        try {
            log.info("getCurrentPlayer {}", authentication);
            User user = (User) authentication.getPrincipal();
            log.info("getCurrentPlayer {}", user);
            
            java.time.Instant start = startDate != null ? java.time.Instant.parse(startDate) : null;
            java.time.Instant end = endDate != null ? java.time.Instant.parse(endDate) : null;
            
            PlayerDto player = userService.getUserAsPlayerWithTimeSlots(user, start, end);
            log.info("getCurrentPlayer {}", player);
            return ResponseEntity.ok(player);
        } catch (RuntimeException e) {
            log.error("getCurrentPlayer", e);
            return ResponseEntity.notFound().build();
        }
    }
    
    @Operation(
        summary = "Обновить профиль игрока",
        description = "Обновляет информацию о текущем игроке (имя, цвет, часовой пояс)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Профиль успешно обновлен"),
        @ApiResponse(responseCode = "400", description = "Неверные данные запроса"),
        @ApiResponse(responseCode = "401", description = "Требуется авторизация")
    })
    @PutMapping("/me")
    public ResponseEntity<PlayerDto> updateCurrentPlayer(
            @RequestBody CreatePlayerRequest request,
            Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            PlayerDto player = userService.updateUserProfile(
                user, 
                request.getName(), 
                request.getColor(),
                request.getTimezone()
            );
            return ResponseEntity.ok(player);
        } catch (RuntimeException e) {
            log.error("updateCurrentPlayer error", e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    @Operation(
        summary = "Переключить временной слот",
        description = "Добавляет или удаляет временной слот для текущего игрока. Если слот существует - удаляет, если нет - добавляет."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Временной слот успешно переключен"),
        @ApiResponse(responseCode = "400", description = "Неверные данные запроса"),
        @ApiResponse(responseCode = "401", description = "Требуется авторизация")
    })
    @PostMapping(value = "/me/time-slots/toggle", consumes = "application/json", produces = "application/json")
    public ResponseEntity<PlayerDto> toggleTimeSlot(
            @RequestBody ToggleTimeSlotRequest request,
            Authentication authentication) {
        try {
            log.info("toggleTimeSlot request: {}", request);
            log.info("toggleTimeSlot start: {}, duration: {}", 
                request != null ? request.getStart() : null, 
                request != null ? request.getDuration() : null);
            
            User user = (User) authentication.getPrincipal();
            if (request == null || request.getStart() == null) {
                log.warn("toggleTimeSlot: invalid request - request or start is null");
                return ResponseEntity.badRequest().build();
            }
            
            Integer duration = request.getDuration() != null ? request.getDuration() : 1;
            log.info("toggleTimeSlot: user={}, start={}, duration={}", 
                user.getId(), request.getStart(), duration);
            
            PlayerDto updatedPlayer = userService.toggleTimeSlot(user, request.getStart(), duration);
            
            log.info("toggleTimeSlot: success");
            return ResponseEntity.ok(updatedPlayer);
        } catch (Exception e) {
            log.error("toggleTimeSlot: error", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
    
    @Operation(
        summary = "Переключить несколько временных слотов",
        description = "Массовое добавление/удаление временных слотов для текущего игрока"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Временные слоты успешно переключены"),
        @ApiResponse(responseCode = "400", description = "Неверные данные запроса"),
        @ApiResponse(responseCode = "401", description = "Требуется авторизация")
    })
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

