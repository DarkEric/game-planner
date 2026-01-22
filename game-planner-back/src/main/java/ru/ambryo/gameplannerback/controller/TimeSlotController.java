package ru.ambryo.gameplannerback.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ru.ambryo.gameplannerback.dto.CreateTimeSlotRequest;
import ru.ambryo.gameplannerback.dto.TimeSlotDto;
import ru.ambryo.gameplannerback.entity.User;
import ru.ambryo.gameplannerback.service.UserService;

import java.time.Instant;

@RestController
@RequestMapping("/api/players/{playerId}/time-slots")
@Tag(name = "Временные слоты", description = "API для управления временными слотами игроков")
@SecurityRequirement(name = "Bearer Authentication")
public class TimeSlotController {
    
    @Autowired
    private UserService userService;
    
    @Operation(
        summary = "Добавить временной слот",
        description = "Добавляет временной слот для указанного игрока. Можно добавлять только свои слоты."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Временной слот успешно добавлен"),
        @ApiResponse(responseCode = "400", description = "Неверные данные запроса"),
        @ApiResponse(responseCode = "403", description = "Недостаточно прав (можно добавлять только свои слоты)"),
        @ApiResponse(responseCode = "401", description = "Требуется авторизация")
    })
    @PostMapping
    public ResponseEntity<TimeSlotDto> addTimeSlot(
            @Parameter(description = "ID игрока", required = true)
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
    
    @Operation(
        summary = "Удалить временной слот",
        description = "Удаляет временной слот у указанного игрока. Можно удалять только свои слоты."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Временной слот успешно удален"),
        @ApiResponse(responseCode = "400", description = "Неверный формат даты"),
        @ApiResponse(responseCode = "403", description = "Недостаточно прав (можно удалять только свои слоты)"),
        @ApiResponse(responseCode = "404", description = "Временной слот не найден"),
        @ApiResponse(responseCode = "401", description = "Требуется авторизация")
    })
    @DeleteMapping
    public ResponseEntity<Void> removeTimeSlot(
            @Parameter(description = "ID игрока", required = true)
            @PathVariable Long playerId,
            @Parameter(description = "Время начала слота в формате ISO-8601", required = true)
            @RequestParam String start,
            Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            if (!user.getId().equals(playerId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            Instant startInstant = Instant.parse(start);
            userService.toggleTimeSlot(user, startInstant, 1);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    
}

