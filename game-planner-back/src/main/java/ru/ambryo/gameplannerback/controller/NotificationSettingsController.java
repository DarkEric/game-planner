package ru.ambryo.gameplannerback.controller;

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
@CrossOrigin(origins = "*")
public class NotificationSettingsController {
    
    private static final Logger log = LoggerFactory.getLogger(NotificationSettingsController.class);
    
    @Autowired
    private NotificationSettingsService notificationSettingsService;
    
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
