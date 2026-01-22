package ru.ambryo.gameplannerback.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ru.ambryo.gameplannerback.dto.UserNotificationSettingsDto;
import ru.ambryo.gameplannerback.entity.User;
import ru.ambryo.gameplannerback.service.NotificationSettingsService;

@RestController
@RequestMapping("/api/notification-settings")
@Tag(name = "Настройки уведомлений", description = "API для управления настройками уведомлений и привязкой Telegram")
@SecurityRequirement(name = "Bearer Authentication")
public class NotificationSettingsController {
    
    private static final Logger log = LoggerFactory.getLogger(NotificationSettingsController.class);
    
    @Autowired
    private NotificationSettingsService notificationSettingsService;
    
    @Operation(
        summary = "Получить настройки уведомлений",
        description = "Возвращает текущие настройки уведомлений для авторизованного пользователя"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Настройки уведомлений успешно получены"),
        @ApiResponse(responseCode = "400", description = "Ошибка при получении настроек"),
        @ApiResponse(responseCode = "401", description = "Требуется авторизация")
    })
    @GetMapping
    public ResponseEntity<UserNotificationSettingsDto> getSettings(Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            UserNotificationSettingsDto settings = notificationSettingsService.getSettings(user.getId());
            return ResponseEntity.ok(settings);
        } catch (Exception e) {
            log.error("getSettings error", e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    @Operation(
        summary = "Обновить настройки уведомлений",
        description = "Обновляет настройки уведомлений для авторизованного пользователя"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Настройки уведомлений успешно обновлены"),
        @ApiResponse(responseCode = "400", description = "Неверные данные запроса"),
        @ApiResponse(responseCode = "401", description = "Требуется авторизация")
    })
    @PutMapping
    public ResponseEntity<UserNotificationSettingsDto> updateSettings(
            @RequestBody UserNotificationSettingsDto dto,
            Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            UserNotificationSettingsDto updated = notificationSettingsService.updateSettings(user.getId(), dto);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.error("updateSettings validation error", e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("updateSettings error", e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    @Operation(
        summary = "Получить токен для привязки Telegram",
        description = "Генерирует токен для привязки Telegram аккаунта к текущему пользователю"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Токен успешно сгенерирован"),
        @ApiResponse(responseCode = "400", description = "Ошибка при генерации токена"),
        @ApiResponse(responseCode = "401", description = "Требуется авторизация")
    })
    @GetMapping("/telegram/link-token")
    public ResponseEntity<String> getLinkToken(Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            String token = notificationSettingsService.generateLinkToken(user.getId());
            return ResponseEntity.ok(token);
        } catch (Exception e) {
            log.error("getLinkToken error", e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    @Operation(
        summary = "Отвязать Telegram аккаунт",
        description = "Отвязывает Telegram аккаунт от текущего пользователя"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Telegram аккаунт успешно отвязан"),
        @ApiResponse(responseCode = "400", description = "Ошибка при отвязке"),
        @ApiResponse(responseCode = "401", description = "Требуется авторизация")
    })
    @PostMapping("/telegram/unlink")
    public ResponseEntity<Void> unlinkTelegramAccount(Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            notificationSettingsService.unlinkTelegramAccount(user.getId());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("unlinkTelegramAccount error", e);
            return ResponseEntity.badRequest().build();
        }
    }
}
