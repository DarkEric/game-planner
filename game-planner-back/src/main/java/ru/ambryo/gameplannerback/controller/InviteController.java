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
import ru.ambryo.gameplannerback.dto.CreateInviteRequest;
import ru.ambryo.gameplannerback.dto.InviteDto;
import ru.ambryo.gameplannerback.entity.User;
import ru.ambryo.gameplannerback.service.InviteService;

import java.util.List;

@RestController
@RequestMapping("/api/invites")
@Tag(name = "Инвайт-коды", description = "API для управления инвайт-кодами регистрации")
@SecurityRequirement(name = "Bearer Authentication")
public class InviteController {
    
    @Autowired
    private InviteService inviteService;
    
    @Operation(
        summary = "Создать инвайт-код",
        description = "Создает новый инвайт-код для регистрации новых пользователей"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Инвайт-код успешно создан"),
        @ApiResponse(responseCode = "400", description = "Неверные данные запроса"),
        @ApiResponse(responseCode = "401", description = "Требуется авторизация")
    })
    @PostMapping
    public ResponseEntity<InviteDto> createInvite(
            @RequestBody CreateInviteRequest request,
            Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            InviteDto invite = inviteService.createInvite(user, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(invite);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @Operation(
        summary = "Проверить инвайт-код",
        description = "Проверяет валидность инвайт-кода. Публичный эндпоинт, не требует авторизации."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Инвайт-код найден и валиден"),
        @ApiResponse(responseCode = "404", description = "Инвайт-код не найден или истек")
    })
    @GetMapping("/{code}")
    public ResponseEntity<InviteDto> getInviteByCode(
            @Parameter(description = "Код инвайта", required = true)
            @PathVariable String code) {
        try {
            InviteDto invite = inviteService.getInviteByCode(code);
            return ResponseEntity.ok(invite);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @Operation(
        summary = "Получить мои инвайт-коды",
        description = "Возвращает список всех инвайт-кодов, созданных текущим пользователем"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Список инвайт-кодов успешно получен"),
        @ApiResponse(responseCode = "401", description = "Требуется авторизация")
    })
    @GetMapping("/my")
    public ResponseEntity<List<InviteDto>> getMyInvites(Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            List<InviteDto> invites = inviteService.getMyInvites(user);
            return ResponseEntity.ok(invites);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @Operation(
        summary = "Удалить инвайт-код",
        description = "Удаляет инвайт-код. Доступно только создателю инвайт-кода."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Инвайт-код успешно удален"),
        @ApiResponse(responseCode = "403", description = "Недостаточно прав (только создатель может удалять)"),
        @ApiResponse(responseCode = "401", description = "Требуется авторизация")
    })
    @DeleteMapping("/{inviteId}")
    public ResponseEntity<Void> deleteInvite(
            @Parameter(description = "ID инвайт-кода", required = true)
            @PathVariable Long inviteId,
            Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            inviteService.deleteInvite(inviteId, user);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }
}
