package ru.ambryo.gameplannerback.service.telegram.message;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.ambryo.gameplannerback.dto.InviteDto;
import ru.ambryo.gameplannerback.service.telegram.util.TelegramHtmlFormatter;
import ru.ambryo.gameplannerback.service.telegram.util.TelegramTimeFormatter;

import java.time.ZoneId;
import java.util.List;

/**
 * Билдер сообщений об инвайтах
 */
@Component
public class InviteMessageBuilder {
    
    private final TelegramTimeFormatter timeFormatter;
    
    public InviteMessageBuilder(
            TelegramTimeFormatter timeFormatter,
            @Value("${app.notification.timezone:Europe/Moscow}") String timezone) {
        this.timeFormatter = timeFormatter;
    }
    
    public String buildInviteCreatedMessage(InviteDto invite) {
        StringBuilder message = new StringBuilder();
        message.append("🎫 <b>Инвайт-код создан!</b>\n\n");
        message.append("📋 <b>Код:</b> <code>").append(TelegramHtmlFormatter.escapeHtml(invite.getCode())).append("</code>\n\n");
        
        if (invite.getExpiresAt() == null) {
            message.append("⏰ <b>Срок действия:</b> Бессрочный\n");
        } else {
            message.append("⏰ <b>Срок действия:</b> До ").append(timeFormatter.formatInstant(invite.getExpiresAt())).append("\n");
        }
        
        if (invite.getMaxUses() != null) {
            message.append("🔢 <b>Использований:</b> ").append(invite.getUsesCount() != null ? invite.getUsesCount() : 0)
                    .append("/").append(invite.getMaxUses()).append("\n");
        } else {
            message.append("🔢 <b>Использований:</b> Неограниченно\n");
        }
        
        message.append("\n💡 Отправьте этот код другу для регистрации.\n");
        message.append("💡 Используйте /myinvites для просмотра всех ваших инвайт-кодов.");
        
        return message.toString();
    }
    
    public String buildMyInvitesListMessage(List<InviteDto> invites) {
        StringBuilder message = new StringBuilder();
        message.append("📋 <b>Мои инвайт-коды</b>\n\n");
        message.append("Всего: ").append(invites.size()).append("\n\n");
        
        for (int i = 0; i < invites.size(); i++) {
            InviteDto invite = invites.get(i);
            
            message.append("<b>").append(i + 1).append(".</b> ");
            message.append("<code>").append(TelegramHtmlFormatter.escapeHtml(invite.getCode())).append("</code>\n");
            
            if (invite.getIsValid() != null && invite.getIsValid()) {
                message.append("✅ Действителен\n");
            } else {
                message.append("❌ Недействителен\n");
            }
            
            if (invite.getCreatedAt() != null) {
                message.append("📅 Создан: ").append(timeFormatter.formatInstant(invite.getCreatedAt())).append("\n");
            }
            
            if (invite.getExpiresAt() == null) {
                message.append("⏰ Бессрочный\n");
            } else {
                message.append("⏰ Действителен до: ").append(timeFormatter.formatInstant(invite.getExpiresAt())).append("\n");
            }
            
            if (invite.getMaxUses() != null) {
                message.append("🔢 Использований: ").append(invite.getUsesCount() != null ? invite.getUsesCount() : 0)
                        .append("/").append(invite.getMaxUses()).append("\n");
            } else {
                message.append("🔢 Использований: ").append(invite.getUsesCount() != null ? invite.getUsesCount() : 0)
                        .append(" (неограниченно)\n");
            }
            
            if (invite.getUsed() != null && invite.getUsed()) {
                if (invite.getUsedByName() != null) {
                    message.append("👤 Использован: ").append(TelegramHtmlFormatter.escapeHtml(invite.getUsedByName())).append("\n");
                }
                if (invite.getUsedAt() != null) {
                    message.append("🕐 Дата использования: ").append(timeFormatter.formatInstant(invite.getUsedAt())).append("\n");
                }
            }
            
            if (i < invites.size() - 1) {
                message.append("\n");
            }
        }
        
        message.append("\n💡 Используйте /invite для создания нового инвайт-кода.");
        
        return message.toString();
    }
}

