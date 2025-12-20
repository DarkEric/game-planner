package ru.ambryo.gameplannerback.service.telegram.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;
import ru.ambryo.gameplannerback.entity.User;
import ru.ambryo.gameplannerback.repository.UserRepository;
import ru.ambryo.gameplannerback.service.InviteService;
import ru.ambryo.gameplannerback.service.telegram.message.InviteMessageBuilder;
import ru.ambryo.gameplannerback.service.telegram.util.TelegramMessageSender;

/**
 * Обработчик команды /myinvites
 */
@Component
public class MyInvitesCommandHandler implements CommandHandler {
    
    private final UserRepository userRepository;
    private final InviteService inviteService;
    private final InviteMessageBuilder messageBuilder;
    private final TelegramMessageSender messageSender;
    
    @Autowired
    public MyInvitesCommandHandler(
            UserRepository userRepository,
            InviteService inviteService,
            InviteMessageBuilder messageBuilder,
            @Lazy AbsSender bot) {
        this.userRepository = userRepository;
        this.inviteService = inviteService;
        this.messageBuilder = messageBuilder;
        this.messageSender = new TelegramMessageSender(bot);
    }
    
    @Override
    public boolean canHandle(String command) {
        return "myinvites".equals(command);
    }
    
    @Override
    public void handle(Message message, Long telegramUserId, String chatId) {
        try {
            User user = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
            
            if (user == null) {
                messageSender.sendPersonalMessage(chatId, """
                    ❌ Ваш аккаунт не связан с веб-сайтом.
                    
                    Используйте /register для регистрации или /auth для привязки существующего аккаунта.""");
                return;
            }
            
            var invites = inviteService.getMyInvites(user);
            
            if (invites.isEmpty()) {
                messageSender.sendPersonalMessage(chatId, """
                    📋 <b>Мои инвайт-коды</b>
                    
                    У вас пока нет созданных инвайт-кодов.
                    
                    Используйте /invite для создания нового инвайт-кода.""");
                return;
            }
            
            String messageText = messageBuilder.buildMyInvitesListMessage(invites);
            messageSender.sendPersonalMessage(chatId, messageText);
        } catch (Exception e) {
            messageSender.sendPersonalMessage(chatId, "❌ Ошибка при получении списка инвайт-кодов. Попробуйте позже.");
        }
    }
}

