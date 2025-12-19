package ru.ambryo.gameplannerback.service.telegram.command;

import org.springframework.beans.factory.annotation.Autowired;
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
    public StartCommandHandler(UserRepository userRepository, AbsSender bot) {
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
            messageSender.sendPersonalMessage(chatId, "✅ Вы уже подписаны на уведомления!\n\n" +
                    "Доступные команды:\n" +
                    "/games - Список предстоящих игр\n" +
                    "/help - Справка по командам\n" +
                    "/stop - Отписаться от уведомлений");
        } else {
            messageSender.sendPersonalMessage(chatId, "👋 Добро пожаловать!\n\n" +
                    "Для получения персональных уведомлений необходимо связать ваш Telegram аккаунт с аккаунтом на веб-сайте.\n\n" +
                    "<b>Способ 1: Регистрация нового аккаунта</b>\n" +
                    "Используйте команду: /register\n\n" +
                    "<b>Способ 2: Авторизация через логин/пароль</b>\n" +
                    "Используйте команду: /auth\n\n" +
                    "<b>Способ 3: Привязка через токен</b>\n" +
                    "1. Откройте настройки профиля на веб-сайте\n" +
                    "2. Получите токен для связывания\n" +
                    "3. Отправьте команду: /link <token>\n\n" +
                    "После связывания вы сможете использовать команду /games для получения списка предстоящих игр.");
        }
    }
}

