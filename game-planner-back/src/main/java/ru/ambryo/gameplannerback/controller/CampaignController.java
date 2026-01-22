package ru.ambryo.gameplannerback.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ru.ambryo.gameplannerback.dto.CampaignDto;
import ru.ambryo.gameplannerback.dto.CampaignInviteDto;
import ru.ambryo.gameplannerback.dto.CampaignPlayerDto;
import ru.ambryo.gameplannerback.dto.CreateCampaignRequest;
import ru.ambryo.gameplannerback.entity.CampaignStatus;
import ru.ambryo.gameplannerback.entity.User;
import ru.ambryo.gameplannerback.service.CampaignService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/campaigns")
@Tag(name = "Кампании", description = "API для управления D&D кампаниями")
@SecurityRequirement(name = "Bearer Authentication")
public class CampaignController {

    private final CampaignService campaignService;

    public CampaignController(CampaignService campaignService) {
        this.campaignService = campaignService;
    }

    @Operation(
        summary = "Создать кампанию",
        description = "Создает новую D&D кампанию. Создателем становится текущий пользователь."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Кампания успешно создана"),
        @ApiResponse(responseCode = "400", description = "Неверные данные запроса"),
        @ApiResponse(responseCode = "401", description = "Требуется авторизация")
    })
    @PostMapping
    public ResponseEntity<CampaignDto> createCampaign(
            @RequestBody CreateCampaignRequest request,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        CampaignDto campaign = campaignService.createCampaign(request, user.getId());
        return ResponseEntity.ok(campaign);
    }

    @Operation(
        summary = "Получить мои кампании",
        description = "Возвращает список всех кампаний, в которых участвует текущий пользователь"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Список кампаний успешно получен"),
        @ApiResponse(responseCode = "401", description = "Требуется авторизация")
    })
    @GetMapping
    public ResponseEntity<List<CampaignDto>> getUserCampaigns(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        List<CampaignDto> campaigns = campaignService.getUserCampaigns(user.getId());
        return ResponseEntity.ok(campaigns);
    }

    @Operation(
        summary = "Получить детали кампании",
        description = "Возвращает детальную информацию о кампании по её ID"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Кампания найдена"),
        @ApiResponse(responseCode = "404", description = "Кампания не найдена"),
        @ApiResponse(responseCode = "401", description = "Требуется авторизация")
    })
    @GetMapping("/{id}")
    public ResponseEntity<CampaignDto> getCampaignDetails(
            @Parameter(description = "ID кампании", required = true)
            @PathVariable Long id,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        CampaignDto campaign = campaignService.getCampaignDetails(id, user.getId());
        return ResponseEntity.ok(campaign);
    }

    @Operation(
        summary = "Обновить статус кампании",
        description = "Изменяет статус кампании (ACTIVE, ON_HIATUS, COMPLETED, CANCELLED)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Статус кампании успешно обновлен"),
        @ApiResponse(responseCode = "400", description = "Неверный статус или недостаточно прав"),
        @ApiResponse(responseCode = "401", description = "Требуется авторизация")
    })
    @PutMapping("/{id}/status")
    public ResponseEntity<CampaignDto> updateCampaignStatus(
            @Parameter(description = "ID кампании", required = true)
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        CampaignStatus status = CampaignStatus.valueOf(request.get("status"));
        CampaignDto campaign = campaignService.updateCampaignStatus(id, status, user.getId());
        return ResponseEntity.ok(campaign);
    }

    @Operation(
        summary = "Обновить вехи кампании",
        description = "Обновляет количество пройденных и общее количество вех кампании"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Вехи кампании успешно обновлены"),
        @ApiResponse(responseCode = "400", description = "Неверные данные или недостаточно прав"),
        @ApiResponse(responseCode = "401", description = "Требуется авторизация")
    })
    @PutMapping("/{id}/milestones")
    public ResponseEntity<CampaignDto> updateMilestones(
            @Parameter(description = "ID кампании", required = true)
            @PathVariable Long id,
            @RequestBody Map<String, Integer> request,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Integer completedMilestones = request.get("completedMilestones");
        Integer totalMilestones = request.get("totalMilestones");
        CampaignDto campaign = campaignService.updateMilestones(id, completedMilestones, totalMilestones, user.getId());
        return ResponseEntity.ok(campaign);
    }

    @Operation(
        summary = "Обновить кампанию",
        description = "Обновляет название и описание кампании. Доступно только создателю."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Кампания успешно обновлена"),
        @ApiResponse(responseCode = "400", description = "Неверные данные"),
        @ApiResponse(responseCode = "403", description = "Недостаточно прав (только создатель может обновлять)"),
        @ApiResponse(responseCode = "401", description = "Требуется авторизация")
    })
    @PutMapping("/{id}")
    public ResponseEntity<CampaignDto> updateCampaign(
            @Parameter(description = "ID кампании", required = true)
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        String name = request.get("name");
        String description = request.get("description");
        CampaignDto campaign = campaignService.updateCampaign(id, name, description, user.getId());
        return ResponseEntity.ok(campaign);
    }

    @Operation(
        summary = "Удалить кампанию",
        description = "Удаляет кампанию. Доступно только создателю."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Кампания успешно удалена"),
        @ApiResponse(responseCode = "403", description = "Недостаточно прав (только создатель может удалять)"),
        @ApiResponse(responseCode = "404", description = "Кампания не найдена"),
        @ApiResponse(responseCode = "401", description = "Требуется авторизация")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCampaign(
            @Parameter(description = "ID кампании", required = true)
            @PathVariable Long id,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        campaignService.deleteCampaign(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Добавить игру в кампанию",
        description = "Связывает игру с кампанией. Доступно только создателю кампании."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Игра успешно добавлена в кампанию"),
        @ApiResponse(responseCode = "400", description = "Игра или кампания не найдены"),
        @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
        @ApiResponse(responseCode = "401", description = "Требуется авторизация")
    })
    @PostMapping("/{id}/games/{gameId}")
    public ResponseEntity<CampaignDto> addGameToCampaign(
            @Parameter(description = "ID кампании", required = true)
            @PathVariable Long id,
            @Parameter(description = "ID игры", required = true)
            @PathVariable Long gameId,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        CampaignDto campaign = campaignService.addGameToCampaign(id, gameId, user.getId());
        return ResponseEntity.ok(campaign);
    }

    @Operation(
        summary = "Удалить игру из кампании",
        description = "Удаляет связь между игрой и кампанией. Доступно только создателю кампании."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Игра успешно удалена из кампании"),
        @ApiResponse(responseCode = "400", description = "Игра или кампания не найдены"),
        @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
        @ApiResponse(responseCode = "401", description = "Требуется авторизация")
    })
    @DeleteMapping("/{id}/games/{gameId}")
    public ResponseEntity<CampaignDto> removeGameFromCampaign(
            @Parameter(description = "ID игры", required = true)
            @PathVariable Long gameId,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        CampaignDto campaign = campaignService.removeGameFromCampaign(gameId, user.getId());
        return ResponseEntity.ok(campaign);
    }

    @Operation(
        summary = "Добавить игрока в кампанию",
        description = "Добавляет игрока в кампанию с информацией о персонаже. Доступно только создателю кампании."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Игрок успешно добавлен в кампанию"),
        @ApiResponse(responseCode = "400", description = "Неверные данные или игрок уже в кампании"),
        @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
        @ApiResponse(responseCode = "401", description = "Требуется авторизация")
    })
    @PostMapping("/{id}/players")
    public ResponseEntity<CampaignPlayerDto> addPlayerToCampaign(
            @Parameter(description = "ID кампании", required = true)
            @PathVariable Long id,
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        Long playerId = Long.valueOf(request.get("playerId").toString());
        Long joinedInGameId = request.get("joinedInGameId") != null 
                ? Long.valueOf(request.get("joinedInGameId").toString()) 
                : null;
        String characterName = (String) request.get("characterName");
        String characterClass = (String) request.get("characterClass");
        String characterNotes = (String) request.get("characterNotes");
        
        CampaignPlayerDto player = campaignService.addPlayerToCampaign(
                id, playerId, joinedInGameId, characterName, characterClass, characterNotes, user.getId());
        return ResponseEntity.ok(player);
    }

    @Operation(
        summary = "Обновить информацию о персонаже",
        description = "Обновляет информацию о персонаже игрока в кампании (имя, класс, заметки)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Информация о персонаже успешно обновлена"),
        @ApiResponse(responseCode = "400", description = "Неверные данные"),
        @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
        @ApiResponse(responseCode = "401", description = "Требуется авторизация")
    })
    @PutMapping("/{id}/players/{playerId}")
    public ResponseEntity<CampaignPlayerDto> updateCharacter(
            @Parameter(description = "ID игрока", required = true)
            @PathVariable Long playerId,
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        String characterName = request.get("characterName");
        String characterClass = request.get("characterClass");
        String characterNotes = request.get("characterNotes");
        
        CampaignPlayerDto player = campaignService.updateCharacter(
                playerId, characterName, characterClass, characterNotes, user.getId());
        return ResponseEntity.ok(player);
    }

    @Operation(
        summary = "Удалить игрока из кампании",
        description = "Удаляет игрока из кампании. Доступно только создателю кампании."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Игрок успешно удален из кампании"),
        @ApiResponse(responseCode = "400", description = "Игрок не найден в кампании"),
        @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
        @ApiResponse(responseCode = "401", description = "Требуется авторизация")
    })
    @DeleteMapping("/{id}/players/{playerId}")
    public ResponseEntity<Void> removePlayerFromCampaign(
            @Parameter(description = "ID кампании", required = true)
            @PathVariable Long id,
            @Parameter(description = "ID игрока", required = true)
            @PathVariable Long playerId,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        campaignService.removePlayerFromCampaign(id, playerId, user.getId());
        return ResponseEntity.ok().build();
    }

    @Operation(
        summary = "Пригласить игрока в кампанию",
        description = "Создает приглашение для игрока присоединиться к кампании. Доступно только создателю кампании."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Приглашение успешно создано"),
        @ApiResponse(responseCode = "400", description = "Игрок не найден или уже приглашен"),
        @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
        @ApiResponse(responseCode = "401", description = "Требуется авторизация")
    })
    @PostMapping("/{id}/invites")
    public ResponseEntity<CampaignInviteDto> invitePlayer(
            @Parameter(description = "ID кампании", required = true)
            @PathVariable Long id,
            @RequestBody Map<String, Long> request,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Long playerId = request.get("playerId");
        CampaignInviteDto invite = campaignService.invitePlayerToCampaign(id, playerId, user.getId());
        return ResponseEntity.ok(invite);
    }

    @Operation(
        summary = "Получить приглашения кампании",
        description = "Возвращает список всех приглашений для указанной кампании. Доступно только создателю кампании."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Список приглашений успешно получен"),
        @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
        @ApiResponse(responseCode = "401", description = "Требуется авторизация")
    })
    @GetMapping("/{id}/invites")
    public ResponseEntity<List<CampaignInviteDto>> getCampaignInvites(
            @Parameter(description = "ID кампании", required = true)
            @PathVariable Long id,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        List<CampaignInviteDto> invites = campaignService.getCampaignInvites(id, user.getId());
        return ResponseEntity.ok(invites);
    }

    @Operation(
        summary = "Получить ожидающие приглашения",
        description = "Возвращает список приглашений в кампании, ожидающих ответа от текущего пользователя"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Список ожидающих приглашений успешно получен"),
        @ApiResponse(responseCode = "401", description = "Требуется авторизация")
    })
    @GetMapping("/invites/pending")
    public ResponseEntity<List<CampaignInviteDto>> getPendingInvites(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        List<CampaignInviteDto> invites = campaignService.getPendingInvites(user.getId());
        return ResponseEntity.ok(invites);
    }

    @Operation(
        summary = "Принять приглашение в кампанию",
        description = "Принимает приглашение в кампанию и добавляет текущего пользователя как игрока с указанным персонажем"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Приглашение успешно принято"),
        @ApiResponse(responseCode = "400", description = "Приглашение не найдено или уже обработано"),
        @ApiResponse(responseCode = "401", description = "Требуется авторизация")
    })
    @PostMapping("/invites/{inviteId}/accept")
    public ResponseEntity<CampaignDto> acceptInvite(
            @Parameter(description = "ID приглашения", required = true)
            @PathVariable Long inviteId,
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        String characterName = request.get("characterName");
        String characterClass = request.get("characterClass");
        String characterNotes = request.get("characterNotes");
        CampaignDto campaign = campaignService.acceptCampaignInvite(
                inviteId, characterName, characterClass, characterNotes, user.getId());
        return ResponseEntity.ok(campaign);
    }

    @Operation(
        summary = "Отклонить приглашение в кампанию",
        description = "Отклоняет приглашение в кампанию"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Приглашение успешно отклонено"),
        @ApiResponse(responseCode = "400", description = "Приглашение не найдено или уже обработано"),
        @ApiResponse(responseCode = "401", description = "Требуется авторизация")
    })
    @PostMapping("/invites/{inviteId}/decline")
    public ResponseEntity<Void> declineInvite(
            @Parameter(description = "ID приглашения", required = true)
            @PathVariable Long inviteId,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        campaignService.declineCampaignInvite(inviteId, user.getId());
        return ResponseEntity.ok().build();
    }
}
