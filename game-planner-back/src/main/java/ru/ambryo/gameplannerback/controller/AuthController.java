package ru.ambryo.gameplannerback.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.ambryo.gameplannerback.dto.AuthResponse;
import ru.ambryo.gameplannerback.dto.LoginRequest;
import ru.ambryo.gameplannerback.dto.PasswordResetConfirmDto;
import ru.ambryo.gameplannerback.dto.PasswordResetRequestDto;
import ru.ambryo.gameplannerback.dto.RegisterRequest;
import ru.ambryo.gameplannerback.service.AuthService;
import ru.ambryo.gameplannerback.service.PasswordResetService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @Autowired
    private AuthService authService;
    
    @Autowired
    private PasswordResetService passwordResetService;
    
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        try {
            AuthResponse response = authService.register(
                    request.getUsername(),
                    request.getPassword(),
                    request.getEmail(),
                    request.getInviteCode()
            );
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
    
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

