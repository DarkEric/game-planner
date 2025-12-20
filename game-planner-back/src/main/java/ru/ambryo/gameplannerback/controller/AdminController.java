package ru.ambryo.gameplannerback.controller;

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
public class AdminController {
    
    @Autowired
    private CleanupService cleanupService;
    
    @Autowired
    private AdminService adminService;
    
    /**
     * Получить список всех пользователей
     * Требует прав администратора
     */
    @GetMapping("/users")
    public ResponseEntity<java.util.List<AdminUserDto>> getAllUsers() {
        try {
            java.util.List<AdminUserDto> users = adminService.getAllUsers();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Сброс пароля пользователя
     * Требует прав администратора
     */
    @PostMapping("/users/{userId}/reset-password")
    public ResponseEntity<ResetPasswordResponse> resetUserPassword(
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
    
    /**
     * Назначить права администратора пользователю
     * Требует прав администратора (проверяется через AdminInterceptor)
     */
    @PostMapping("/users/{userId}/grant-admin")
    public ResponseEntity<Map<String, String>> grantAdminRights(@PathVariable Long userId) {
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
    
    /**
     * Отозвать права администратора у пользователя
     * Требует прав администратора
     */
    @PostMapping("/users/{userId}/revoke-admin")
    public ResponseEntity<Map<String, String>> revokeAdminRights(
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
    
    /**
     * Проверить, является ли текущий пользователь администратором
     * Публичный endpoint (для проверки отображения админ-панели)
     */
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
    
    /**
     * Ручной запуск очистки устаревших данных
     * Требует прав администратора (проверяется через AdminInterceptor)
     */
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
    
    /**
     * Получить информацию о конфигурации очистки
     */
    @GetMapping("/cleanup/info")
    public ResponseEntity<Map<String, Object>> getCleanupInfo() {
        Map<String, Object> info = cleanupService.getCleanupInfo();
        return ResponseEntity.ok(info);
    }
    
    /**
     * Удалить пользователя
     * Требует прав администратора и подтверждения паролем
     */
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Map<String, String>> deleteUser(
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
