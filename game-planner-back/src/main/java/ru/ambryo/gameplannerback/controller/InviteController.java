package ru.ambryo.gameplannerback.controller;

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
public class InviteController {
    
    @Autowired
    private InviteService inviteService;
    
    /**
     * Создать новый инвайт
     */
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
    
    /**
     * Получить информацию об инвайте по коду (публичный эндпоинт)
     */
    @GetMapping("/{code}")
    public ResponseEntity<InviteDto> getInviteByCode(@PathVariable String code) {
        try {
            InviteDto invite = inviteService.getInviteByCode(code);
            return ResponseEntity.ok(invite);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Получить список моих инвайтов
     */
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
    
    /**
     * Удалить инвайт
     */
    @DeleteMapping("/{inviteId}")
    public ResponseEntity<Void> deleteInvite(
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
