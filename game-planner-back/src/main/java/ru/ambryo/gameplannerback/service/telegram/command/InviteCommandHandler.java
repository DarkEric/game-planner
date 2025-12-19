package ru.ambryo.gameplannerback.service.telegram.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;
import ru.ambryo.gameplannerback.dto.CreateInviteRequest;
import ru.ambryo.gameplannerback.entity.User;
import ru.ambryo.gameplannerback.repository.UserRepository;
import ru.ambryo.gameplannerback.service.InviteService;
import ru.ambryo.gameplannerback.service.telegram.message.InviteMessageBuilder;
import ru.ambryo.gameplannerback.service.telegram.util.TelegramMessageSender;

/**
 * Обработчик команды /invite
 */
@Component
public class InviteCommandHandler implements CommandHandler {
    
    private final UserRepository userRepository;
    private final InviteService inviteService;
    private final InviteMessageBuilder messageBuilder;
    private final TelegramMessageSender messageSender;
    
    @Autowired
    public InviteCommandHandler(
            UserRepository userRepository,
            InviteService inviteService,
            InviteMessageBuilder messageBuilder,
            AbsSender bot) {
        this.userRepository = userRepository;
        this.inviteService = inviteService;
        this.messageBuilder = messageBuilder;
        this.messageSender = new TelegramMessageSender(bot);
    }
    
    @Override
    public boolean canHandle(String command) {
        return "invite".equals(command);
    }
    
    @Override
    public void handle(Message message, Long telegramUserId, String chatId) {
        try {
            User user = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
            
            if (user == null) {
                messageSender.sendPersonalMessage(chatId, "❌ Ваш аккаунт не связан с веб-сайтом.\n\n" +
                        "Используйте /register для регистрации или /auth для привязки существующего аккаунта.");
                return;
            }
            
            // Создаем бессрочный одноразовый инвайт-код
            CreateInviteRequest request = new CreateInviteRequest(null, 1);
            var invite = inviteService.createInvite(user, request);
            
            String messageText = messageBuilder.buildInviteCreatedMessage(invite);
            messageSender.sendPersonalMessage(chatId, messageText);
        } catch (Exception e) {
            messageSender.sendPersonalMessage(chatId, "❌ Ошибка при создании инвайт-кода. Попробуйте позже.");
        }
    }
}

