package ru.ambryo.gameplannerback.service.telegram.menu;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.ambryo.gameplannerback.dto.CreateInviteRequest;
import ru.ambryo.gameplannerback.entity.User;
import ru.ambryo.gameplannerback.repository.UserRepository;
import ru.ambryo.gameplannerback.service.InviteService;
import ru.ambryo.gameplannerback.service.telegram.keyboard.InvitesMenuKeyboardBuilder;
import ru.ambryo.gameplannerback.service.telegram.message.InviteMessageBuilder;

/**
 * Обработчик меню инвайтов
 */
@Component
public class InvitesMenuHandler implements MenuHandler {
    
    private final UserRepository userRepository;
    private final InviteService inviteService;
    private final InviteMessageBuilder messageBuilder;
    private final InvitesMenuKeyboardBuilder keyboardBuilder;
    private final MenuMessageUpdater messageUpdater;
    
    @Autowired
    public InvitesMenuHandler(
            UserRepository userRepository,
            InviteService inviteService,
            InviteMessageBuilder messageBuilder,
            InvitesMenuKeyboardBuilder keyboardBuilder,
            MenuMessageUpdater messageUpdater) {
        this.userRepository = userRepository;
        this.inviteService = inviteService;
        this.messageBuilder = messageBuilder;
        this.keyboardBuilder = keyboardBuilder;
        this.messageUpdater = messageUpdater;
    }
    
    @Override
    public boolean canHandle(String callbackData) {
        return callbackData.equals("menu_invites") ||
               callbackData.equals("menu_invites_create") ||
               callbackData.equals("menu_invites_list");
    }
    
    @Override
    public void handle(CallbackQuery callbackQuery, Long telegramUserId, String chatId, Integer messageId) {
        String data = callbackQuery.getData();
        User user = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
        
        if (user == null) {
            messageUpdater.answerCallback(callbackQuery.getId(), "❌ Аккаунт не связан. Используйте /link для связывания.");
            return;
        }
        
        if (data.equals("menu_invites")) {
            String message = "🎫 <b>Инвайты</b>\n\nВыберите действие:";
            var keyboard = keyboardBuilder.build();
            messageUpdater.updateMessage(chatId, messageId, message, keyboard);
            
        } else if (data.equals("menu_invites_create")) {
            handleCreateInvite(user, chatId, messageId);
            
        } else if (data.equals("menu_invites_list")) {
            handleListInvites(user, chatId, messageId);
        }
    }
    
    private void handleCreateInvite(User user, String chatId, Integer messageId) {
        try {
            // Создаем бессрочный одноразовый инвайт-код
            CreateInviteRequest request = new CreateInviteRequest(null, 1);
            var invite = inviteService.createInvite(user, request);
            
            String message = messageBuilder.buildInviteCreatedMessage(invite);
            var keyboard = keyboardBuilder.build();
            messageUpdater.updateMessage(chatId, messageId, message, keyboard);
        } catch (Exception e) {
            messageUpdater.answerCallback("", "❌ Ошибка при создании инвайт-кода.");
        }
    }
    
    private void handleListInvites(User user, String chatId, Integer messageId) {
        try {
            var invites = inviteService.getMyInvites(user);
            
            String message;
            var keyboard = keyboardBuilder.build();
            
            if (invites.isEmpty()) {
                message = "📋 <b>Мои инвайт-коды</b>\n\n" +
                        "У вас пока нет созданных инвайт-кодов.\n\n" +
                        "Используйте кнопку 'Создать инвайт-код' для создания нового.";
            } else {
                message = messageBuilder.buildMyInvitesListMessage(invites);
            }
            
            messageUpdater.updateMessage(chatId, messageId, message, keyboard);
        } catch (Exception e) {
            messageUpdater.answerCallback("", "❌ Ошибка при получении списка инвайт-кодов.");
        }
    }
}
