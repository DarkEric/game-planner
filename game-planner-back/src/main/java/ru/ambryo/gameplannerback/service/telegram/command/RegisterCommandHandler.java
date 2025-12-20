package ru.ambryo.gameplannerback.service.telegram.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;
import ru.ambryo.gameplannerback.entity.User;
import ru.ambryo.gameplannerback.repository.UserRepository;
import ru.ambryo.gameplannerback.service.telegram.state.RegistrationStateManager;
import ru.ambryo.gameplannerback.service.telegram.util.TelegramMessageSender;

/**
 * Обработчик команды /register
 */
@Component
public class RegisterCommandHandler implements CommandHandler {
    
    private final UserRepository userRepository;
    private final RegistrationStateManager stateManager;
    private final TelegramMessageSender messageSender;
    
    @Autowired
    public RegisterCommandHandler(
            UserRepository userRepository,
            RegistrationStateManager stateManager,
            @Lazy AbsSender bot) {
        this.userRepository = userRepository;
        this.stateManager = stateManager;
        this.messageSender = new TelegramMessageSender(bot);
    }
    
    @Override
    public boolean canHandle(String command) {
        return "register".equals(command);
    }
    
    @Override
    public void handle(Message message, Long telegramUserId, String chatId) {
        try {
            // Проверяем, не зарегистрирован ли уже пользователь
            User existingUser = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
            if (existingUser != null) {
                messageSender.sendPersonalMessage(chatId, """
                    ✅ Вы уже зарегистрированы!
                    
                    Используйте /games для получения списка предстоящих игр.""");
                return;
            }
            
            // Проверяем блокировку регистрации
            if (stateManager.isBlocked(chatId)) {
                long remainingSeconds = stateManager.getBlockTimeRemaining(chatId);
                long remainingMinutes = remainingSeconds / 60;
                messageSender.sendPersonalMessage(chatId, "⛔ Слишком много неудачных попыток регистрации.\n\n" +
                        "Попробуйте снова через " + remainingMinutes + " минут.");
                return;
            }
            
            // Инициализируем состояние регистрации
            stateManager.setState(chatId, RegistrationStateManager.RegistrationState.WAITING_INVITE);
            stateManager.setData(chatId, new RegistrationStateManager.RegistrationData());
            
            messageSender.sendPersonalMessage(chatId, """
                📝 <b>Регистрация нового аккаунта</b>
                
                Введите инвайт-код для регистрации:
                
                💡 Используйте /cancel для отмены.""");
        } catch (Exception e) {
            stateManager.clearState(chatId);
            messageSender.sendPersonalMessage(chatId, "❌ Произошла ошибка при инициализации регистрации. Попробуйте позже.");
        }
    }
}

