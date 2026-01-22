package ru.ambryo.gameplannerback.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.ambryo.gameplannerback.dto.*;
import ru.ambryo.gameplannerback.service.AuthService;
import ru.ambryo.gameplannerback.service.PasswordResetService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Аутентификация", description = "API для регистрации, входа и сброса пароля")
public class AuthController {
    
    @Autowired
    private AuthService authService;
    
    @Autowired
    private PasswordResetService passwordResetService;
    
    @Operation(
        summary = "Регистрация нового пользователя",
        description = "Создает новый аккаунт пользователя. Требует инвайт-код для регистрации."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Пользователь успешно зарегистрирован"),
        @ApiResponse(responseCode = "400", description = "Неверные данные или инвайт-код")
    })
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        try {
            AuthResponse response = authService.register(
                    request.getUsername(),
                    request.getPassword(),
                    request.getEmail(),
                    request.getInviteCode(),
                    request.getName()
            );
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
    
    @Operation(
        summary = "Вход в систему",
        description = "Авторизует пользователя и возвращает JWT токен для доступа к API"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Успешная авторизация"),
        @ApiResponse(responseCode = "401", description = "Неверные учетные данные")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(
                    request.getUsername(),
                    request.getPassword()
            );
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
    
    @Operation(
        summary = "Запрос сброса пароля",
        description = "Отправляет код для сброса пароля в Telegram (если аккаунт привязан) или возвращает токен"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Запрос на сброс пароля обработан"),
        @ApiResponse(responseCode = "400", description = "Пользователь не найден или ошибка при отправке")
    })
    @PostMapping("/password-reset/request")
    public ResponseEntity<Map<String, String>> requestPasswordReset(@RequestBody PasswordResetRequestDto request) {
        try {
            passwordResetService.requestPasswordReset(request.getUsername());
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Если ваш аккаунт связан с Telegram ботом, код для сброса будет отправлен в Telegram");
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }
    
    @Operation(
        summary = "Подтверждение сброса пароля",
        description = "Устанавливает новый пароль по токену сброса"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Пароль успешно изменен"),
        @ApiResponse(responseCode = "400", description = "Неверный или истекший токен")
    })
    @PostMapping("/password-reset/confirm")
    public ResponseEntity<Map<String, String>> confirmPasswordReset(@RequestBody PasswordResetConfirmDto request) {
        try {
            passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Пароль успешно изменен");
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }
}

