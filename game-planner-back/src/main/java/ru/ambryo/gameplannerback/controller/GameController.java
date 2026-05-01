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
import ru.ambryo.gameplannerback.dto.CreateGameRequest;
import ru.ambryo.gameplannerback.dto.GameDto;
import ru.ambryo.gameplannerback.dto.MarkGameHeldRequest;
import ru.ambryo.gameplannerback.dto.UpdateGameRequest;
import ru.ambryo.gameplannerback.entity.User;
import ru.ambryo.gameplannerback.service.GameService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequestMapping("/api/games")
@Tag(name = "Игры", description = "API для управления играми")
@SecurityRequirement(name = "Bearer Authentication")
public class GameController {
    
    @Autowired
    private GameService gameService;
    
    @Operation(
        summary = "Создать игру",
        description = "Создает новую игру. Автором становится текущий авторизованный пользователь."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Игра успешно создана"),
        @ApiResponse(responseCode = "400", description = "Неверные данные запроса"),
        @ApiResponse(responseCode = "401", description = "Требуется авторизация")
    })
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
    
    @Operation(
        summary = "Получить список игр",
        description = "Возвращает список игр в указанном временном диапазоне. По умолчанию возвращает игры на ближайшие 30 дней."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Список игр успешно получен"),
        @ApiResponse(responseCode = "400", description = "Неверный формат даты")
    })
    @GetMapping
    public ResponseEntity<List<GameDto>> getGames(
            @Parameter(description = "Начальная дата в формате ISO-8601 (например: 2024-01-01T00:00:00Z)")
            @RequestParam(required = false) String startDate,
            @Parameter(description = "Конечная дата в формате ISO-8601 (например: 2024-01-31T23:59:59Z)")
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
    
    @Operation(
        summary = "Получить мои игры",
        description = "Возвращает список всех игр, в которых участвует текущий пользователь"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Список игр успешно получен"),
        @ApiResponse(responseCode = "401", description = "Требуется авторизация")
    })
    @GetMapping("/my")
    public ResponseEntity<List<GameDto>> getMyGames(Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            List<GameDto> games = gameService.getAllGamesForUser(user.getId());
            return ResponseEntity.ok(games);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @Operation(
        summary = "Получить созданные мной игры",
        description = "Возвращает список всех игр, созданных текущим пользователем"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Список игр успешно получен"),
        @ApiResponse(responseCode = "401", description = "Требуется авторизация")
    })
    @GetMapping("/my/created")
    public ResponseEntity<List<GameDto>> getMyCreatedGames(Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            List<GameDto> games = gameService.getGamesCreatedByUser(user.getId());
            return ResponseEntity.ok(games);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @Operation(
        summary = "Получить игру по ID",
        description = "Возвращает детальную информацию об игре по её идентификатору"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Игра найдена"),
        @ApiResponse(responseCode = "404", description = "Игра не найдена")
    })
    @GetMapping("/{gameId}")
    public ResponseEntity<GameDto> getGameById(
            @Parameter(description = "ID игры", required = true)
            @PathVariable Long gameId) {
        try {
            GameDto game = gameService.getGameById(gameId);
            return ResponseEntity.ok(game);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @Operation(
        summary = "Обновить игру",
        description = "Обновляет параметры игры (время, название, описание, лимит участников). Доступно только создателю."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Игра обновлена"),
        @ApiResponse(responseCode = "400", description = "Неверные данные"),
        @ApiResponse(responseCode = "403", description = "Только создатель может редактировать игру"),
        @ApiResponse(responseCode = "404", description = "Игра не найдена")
    })
    @PutMapping("/{gameId}")
    public ResponseEntity<GameDto> updateGame(
            @Parameter(description = "ID игры", required = true)
            @PathVariable Long gameId,
            @RequestBody UpdateGameRequest request,
            Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            GameDto game = gameService.updateGame(gameId, request, user);
            return ResponseEntity.ok(game);
        } catch (RuntimeException e) {
            String message = e.getMessage();
            if ("Game not found".equals(message)) {
                return ResponseEntity.notFound().build();
            }
            if (message != null && message.contains("Only creator")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(
        summary = "Удалить игру",
        description = "Удаляет игру. Доступно только создателю игры. Можно указать причину отмены."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Игра успешно удалена"),
        @ApiResponse(responseCode = "403", description = "Недостаточно прав (только создатель может удалить игру)"),
        @ApiResponse(responseCode = "404", description = "Игра не найдена")
    })
    @DeleteMapping("/{gameId}")
    public ResponseEntity<Void> deleteGame(
            @Parameter(description = "ID игры", required = true)
            @PathVariable Long gameId,
            @Parameter(description = "Причина отмены игры")
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
    
    @Operation(
        summary = "Присоединиться к игре",
        description = "Добавляет текущего пользователя в список участников игры"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Успешно присоединились к игре"),
        @ApiResponse(responseCode = "400", description = "Не удалось присоединиться (игра не найдена или уже участвуете)"),
        @ApiResponse(responseCode = "401", description = "Требуется авторизация")
    })
    @PostMapping("/{gameId}/join")
    public ResponseEntity<GameDto> joinGame(
            @Parameter(description = "ID игры", required = true)
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
    
    @Operation(
        summary = "Покинуть игру",
        description = "Удаляет текущего пользователя из списка участников игры"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Успешно покинули игру"),
        @ApiResponse(responseCode = "400", description = "Не удалось покинуть игру (игра не найдена или не участвуете)"),
        @ApiResponse(responseCode = "401", description = "Требуется авторизация")
    })
    @PostMapping("/{gameId}/leave")
    public ResponseEntity<GameDto> leaveGame(
            @Parameter(description = "ID игры", required = true)
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
    
    @Operation(
        summary = "Удалить игрока из игры",
        description = "Удаляет указанного игрока из списка участников игры. Доступно только создателю игры."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Игрок успешно удален из игры"),
        @ApiResponse(responseCode = "400", description = "Игра или игрок не найдены"),
        @ApiResponse(responseCode = "403", description = "Недостаточно прав (только создатель может удалять игроков)")
    })
    @DeleteMapping("/{gameId}/players/{playerId}")
    public ResponseEntity<GameDto> removePlayerFromGame(
            @Parameter(description = "ID игры", required = true)
            @PathVariable Long gameId,
            @Parameter(description = "ID игрока", required = true)
            @PathVariable Long playerId,
            Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            GameDto game = gameService.removePlayerFromGame(gameId, playerId, user);
            return ResponseEntity.ok(game);
        } catch (RuntimeException e) {
            String message = e.getMessage();
            if (message != null && message.contains("Only creator")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(
        summary = "Отметить игру как проведенную",
        description = "Отмечает игру как проведенную и позволяет указать ключевые события игры"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Игра успешно отмечена как проведенная"),
        @ApiResponse(responseCode = "400", description = "Игра не найдена или уже отмечена"),
        @ApiResponse(responseCode = "401", description = "Требуется авторизация")
    })
    @PostMapping("/{gameId}/hold")
    public ResponseEntity<GameDto> markGameAsHeld(
            @Parameter(description = "ID игры", required = true)
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
