package ru.ambryo.gameplannerback.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ambryo.gameplannerback.dto.CreateInviteRequest;
import ru.ambryo.gameplannerback.dto.InviteDto;
import ru.ambryo.gameplannerback.entity.Invite;
import ru.ambryo.gameplannerback.entity.User;
import ru.ambryo.gameplannerback.repository.InviteRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class InviteService {
    
    @Autowired
    private InviteRepository inviteRepository;
    
    @Transactional
    public InviteDto createInvite(User creator, CreateInviteRequest request) {
        Invite invite = new Invite(creator, request.getExpiresAt(), request.getMaxUses());
        invite = inviteRepository.save(invite);
        return convertToDto(invite);
    }
    
    @Transactional(readOnly = true)
    public InviteDto getInviteByCode(String code) {
        Invite invite = inviteRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Invite not found"));
        return convertToDto(invite);
    }
    
    @Transactional(readOnly = true)
    public List<InviteDto> getMyInvites(User user) {
        return inviteRepository.findByCreatedByOrderByCreatedAtDesc(user).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public void validateAndUseInvite(String code, User user) {
        Invite invite = inviteRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Invalid invite code"));
        
        if (!invite.isValid()) {
            throw new RuntimeException("Invite is expired or already used");
        }
        
        invite.markAsUsed(user);
        inviteRepository.save(invite);
    }
    
    @Transactional
    public void deleteInvite(Long inviteId, User user) {
        Invite invite = inviteRepository.findById(inviteId)
                .orElseThrow(() -> new RuntimeException("Invite not found"));
        
        if (!invite.getCreatedBy().getId().equals(user.getId())) {
            throw new RuntimeException("You can only delete your own invites");
        }
        
        inviteRepository.delete(invite);
    }
    
    private InviteDto convertToDto(Invite invite) {
        return new InviteDto(
                invite.getId(),
                invite.getCode(),
                invite.getCreatedBy().getName(),
                invite.getCreatedAt(),
                invite.getExpiresAt(),
                invite.getUsed(),
                invite.getUsedBy() != null ? invite.getUsedBy().getName() : null,
                invite.getUsedAt(),
                invite.getMaxUses(),
                invite.getUsesCount(),
                invite.isValid()
        );
    }
}
