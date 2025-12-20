package ru.ambryo.gameplannerback.service.telegram.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;
import ru.ambryo.gameplannerback.entity.User;
import ru.ambryo.gameplannerback.repository.UserRepository;
import ru.ambryo.gameplannerback.service.telegram.util.TelegramMessageSender;

/**
 * Обработчик команды /stop
 */
@Component
public class StopCommandHandler implements CommandHandler {
    
    private final UserRepository userRepository;
    private final TelegramMessageSender messageSender;
    
    @Autowired
    public StopCommandHandler(UserRepository userRepository, @Lazy AbsSender bot) {
        this.userRepository = userRepository;
        this.messageSender = new TelegramMessageSender(bot);
    }
    
    @Override
    public boolean canHandle(String command) {
        return "stop".equals(command);
    }
    
    @Override
    public void handle(Message message, Long telegramUserId, String chatId) {
        try {
            User user = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
            
            if (user != null && user.getTelegramSubscribed()) {
                user.setTelegramSubscribed(false);
                userRepository.save(user);
                messageSender.sendPersonalMessage(chatId, "✅ Вы отписались от уведомлений.\n\nИспользуйте /start для повторной подписки.");
            } else {
                messageSender.sendPersonalMessage(chatId, "Вы не подписаны на уведомления.");
            }
        } catch (Exception e) {
            // Логирование ошибки должно быть в TelegramExceptionHandler
            messageSender.sendPersonalMessage(chatId, "❌ Произошла ошибка при отписке от уведомлений.");
        }
    }
}

