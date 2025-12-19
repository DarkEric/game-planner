package ru.ambryo.gameplannerback.service.telegram.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;
import ru.ambryo.gameplannerback.entity.User;
import ru.ambryo.gameplannerback.repository.UserRepository;
import ru.ambryo.gameplannerback.service.telegram.state.AuthStateManager;
import ru.ambryo.gameplannerback.service.telegram.util.TelegramMessageSender;

/**
 * Обработчик команды /auth
 */
@Component
public class AuthCommandHandler implements CommandHandler {
    
    private final UserRepository userRepository;
    private final AuthStateManager stateManager;
    private final TelegramMessageSender messageSender;
    
    @Autowired
    public AuthCommandHandler(
            UserRepository userRepository,
            AuthStateManager stateManager,
            AbsSender bot) {
        this.userRepository = userRepository;
        this.stateManager = stateManager;
        this.messageSender = new TelegramMessageSender(bot);
    }
    
    @Override
    public boolean canHandle(String command) {
        return "auth".equals(command);
    }
    
    @Override
    public void handle(Message message, Long telegramUserId, String chatId) {
        try {
            // Проверяем, не связан ли уже аккаунт
            User existingUser = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
            if (existingUser != null) {
                messageSender.sendPersonalMessage(chatId, "✅ Ваш аккаунт уже связан!\n\n" +
                        "Используйте /games для получения списка предстоящих игр.");
                return;
            }
            
            // Проверяем блокировку
            if (stateManager.isBlocked(chatId)) {
                long remainingSeconds = stateManager.getBlockTimeRemaining(chatId);
                long remainingMinutes = remainingSeconds / 60;
                messageSender.sendPersonalMessage(chatId, "⛔ Слишком много неудачных попыток авторизации.\n\n" +
                        "Попробуйте снова через " + remainingMinutes + " минут.");
                return;
            }
            
            // Инициализируем состояние авторизации
            stateManager.setState(chatId, AuthStateManager.AuthState.WAITING_USERNAME);
            
            messageSender.sendPersonalMessage(chatId, "🔐 <b>Авторизация для привязки аккаунта</b>\n\n" +
                    "Введите ваш логин (имя пользователя):\n\n" +
                    "💡 Используйте /cancel для отмены.");
        } catch (Exception e) {
            stateManager.clearState(chatId);
            messageSender.sendPersonalMessage(chatId, "❌ Произошла ошибка при инициализации авторизации. Попробуйте позже.");
        }
    }
}

