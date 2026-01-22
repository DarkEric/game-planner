package ru.ambryo.gameplannerback.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ru.ambryo.gameplannerback.dto.AdminUserDto;
import ru.ambryo.gameplannerback.dto.DeleteUserRequest;
import ru.ambryo.gameplannerback.dto.ResetPasswordResponse;
import ru.ambryo.gameplannerback.entity.User;
import ru.ambryo.gameplannerback.service.AdminService;
import ru.ambryo.gameplannerback.service.CleanupService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Администрирование", description = "API для управления пользователями и системой (требует права администратора)")
@SecurityRequirement(name = "Bearer Authentication")
public class AdminController {
    
    @Autowired
    private CleanupService cleanupService;
    
    @Autowired
    private AdminService adminService;
    
    @Operation(
        summary = "Получить список всех пользователей",
        description = "Возвращает список всех пользователей системы с их данными"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Список пользователей успешно получен"),
        @ApiResponse(responseCode = "403", description = "Недостаточно прав (требуется роль администратора)"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @GetMapping("/users")
    public ResponseEntity<java.util.List<AdminUserDto>> getAllUsers() {
        try {
            java.util.List<AdminUserDto> users = adminService.getAllUsers();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Operation(
        summary = "Сброс пароля пользователя",
        description = "Генерирует новый временный пароль для указанного пользователя. Пароль отправляется в Telegram, если аккаунт привязан."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Пароль успешно сброшен"),
        @ApiResponse(responseCode = "400", description = "Пользователь не найден"),
        @ApiResponse(responseCode = "403", description = "Недостаточно прав (требуется роль администратора)")
    })
    @PostMapping("/users/{userId}/reset-password")
    public ResponseEntity<ResetPasswordResponse> resetUserPassword(
            @Parameter(description = "ID пользователя", required = true)
            @PathVariable Long userId) {
        try {
            ResetPasswordResponse response = adminService.resetUserPassword(userId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            ResetPasswordResponse errorResponse = new ResetPasswordResponse();
            errorResponse.setMessage(e.getMessage());
            errorResponse.setSentViaTelegram(false);
            return ResponseEntity.status(org.springframework.http.HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }
    
    @Operation(
        summary = "Назначить права администратора",
        description = "Назначает права администратора указанному пользователю"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Права администратора успешно назначены"),
        @ApiResponse(responseCode = "400", description = "Пользователь не найден или уже является администратором"),
        @ApiResponse(responseCode = "403", description = "Недостаточно прав (требуется роль администратора)")
    })
    @PostMapping("/users/{userId}/grant-admin")
    public ResponseEntity<Map<String, String>> grantAdminRights(
            @Parameter(description = "ID пользователя", required = true)
            @PathVariable Long userId) {
        try {
            adminService.grantAdminRights(userId);
            Map<String, String> response = new HashMap<>();
            response.put("success", "true");
            response.put("message", "Права администратора назначены");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(org.springframework.http.HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }
    
    @Operation(
        summary = "Отозвать права администратора",
        description = "Отзывает права администратора у указанного пользователя. Нельзя отозвать права у самого себя или последнего администратора."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Права администратора успешно отозваны"),
        @ApiResponse(responseCode = "400", description = "Пользователь не найден, не является администратором, или это последний администратор"),
        @ApiResponse(responseCode = "403", description = "Недостаточно прав (требуется роль администратора)")
    })
    @PostMapping("/users/{userId}/revoke-admin")
    public ResponseEntity<Map<String, String>> revokeAdminRights(
            @Parameter(description = "ID пользователя", required = true)
            @PathVariable Long userId,
            Authentication authentication) {
        try {
            User currentUser = (User) authentication.getPrincipal();
            adminService.revokeAdminRights(userId, currentUser.getId());
            Map<String, String> response = new HashMap<>();
            response.put("success", "true");
            response.put("message", "Права администратора отозваны");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(org.springframework.http.HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }
    
    @Operation(
        summary = "Проверить права администратора",
        description = "Проверяет, является ли текущий авторизованный пользователь администратором. Публичный endpoint."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Результат проверки прав администратора")
    })
    @GetMapping("/users/me/is-admin")
    public ResponseEntity<Map<String, Boolean>> checkIsAdmin(Authentication authentication) {
        Map<String, Boolean> response = new HashMap<>();
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            response.put("isAdmin", adminService.isAdmin(user));
        } else {
            response.put("isAdmin", false);
        }
        return ResponseEntity.ok(response);
    }
    
    @Operation(
        summary = "Запустить очистку данных",
        description = "Вручную запускает очистку устаревших данных (игры, временные слоты и т.д.)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Очистка успешно выполнена"),
        @ApiResponse(responseCode = "500", description = "Ошибка при выполнении очистки"),
        @ApiResponse(responseCode = "403", description = "Недостаточно прав (требуется роль администратора)")
    })
    @PostMapping("/cleanup")
    public ResponseEntity<Map<String, String>> triggerCleanup() {
        try {
            cleanupService.manualCleanup();
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Cleanup completed successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Cleanup failed: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    @Operation(
        summary = "Получить информацию о конфигурации очистки",
        description = "Возвращает информацию о настройках автоматической очистки данных"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Информация о конфигурации очистки")
    })
    @GetMapping("/cleanup/info")
    public ResponseEntity<Map<String, Object>> getCleanupInfo() {
        Map<String, Object> info = cleanupService.getCleanupInfo();
        return ResponseEntity.ok(info);
    }
    
    @Operation(
        summary = "Удалить пользователя",
        description = "Удаляет пользователя из системы. Требует подтверждения паролем администратора. Нельзя удалить самого себя или последнего администратора."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Пользователь успешно удален"),
        @ApiResponse(responseCode = "400", description = "Неверный пароль или пользователь не может быть удален"),
        @ApiResponse(responseCode = "403", description = "Недостаточно прав (требуется роль администратора)")
    })
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Map<String, String>> deleteUser(
            @Parameter(description = "ID пользователя для удаления", required = true)
            @PathVariable Long userId,
            @RequestBody DeleteUserRequest request,
            Authentication authentication) {
        try {
            User currentUser = (User) authentication.getPrincipal();
            adminService.deleteUser(userId, currentUser.getId(), request.getPassword());
            Map<String, String> response = new HashMap<>();
            response.put("success", "true");
            response.put("message", "Пользователь успешно удален");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(org.springframework.http.HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }
}
