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
 * Обработчик команды /start
 */
@Component
public class StartCommandHandler implements CommandHandler {
    
    private final UserRepository userRepository;
    private final TelegramMessageSender messageSender;
    
    @Autowired
    public StartCommandHandler(UserRepository userRepository, @Lazy AbsSender bot) {
        this.userRepository = userRepository;
        this.messageSender = new TelegramMessageSender(bot);
    }
    
    @Override
    public boolean canHandle(String command) {
        return "start".equals(command);
    }
    
    @Override
    public void handle(Message message, Long telegramUserId, String chatId) {
        User user = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
        
        if (user != null) {
            user.setTelegramChatId(chatId);
            user.setTelegramSubscribed(true);
            userRepository.save(user);
            messageSender.sendPersonalMessage(chatId, """
                ✅ Вы уже подписаны на уведомления!
                
                Доступные команды:
                /games - Список предстоящих игр
                /help - Справка по командам
                /stop - Отписаться от уведомлений""");
        } else {
            messageSender.sendPersonalMessage(chatId, """
                👋 Добро пожаловать!
                
                Для получения персональных уведомлений необходимо связать ваш Telegram аккаунт с аккаунтом на веб-сайте.
                
                <b>Способ 1: Регистрация нового аккаунта</b>
                Используйте команду: /register
                
                <b>Способ 2: Авторизация через логин/пароль</b>
                Используйте команду: /auth
                
                <b>Способ 3: Привязка через токен</b>
                1. Откройте настройки профиля на веб-сайте
                2. Получите токен для связывания
                3. Отправьте команду: /link <token>
                
                После связывания вы сможете использовать команду /games для получения списка предстоящих игр.""");
        }
    }
}

