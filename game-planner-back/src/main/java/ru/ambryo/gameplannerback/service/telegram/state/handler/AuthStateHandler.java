package ru.ambryo.gameplannerback.service.telegram.state.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.bots.AbsSender;
import ru.ambryo.gameplannerback.service.NotificationSettingsService;
import ru.ambryo.gameplannerback.service.telegram.state.AuthStateManager;
import ru.ambryo.gameplannerback.service.telegram.util.TelegramMessageSender;

/**
 * Обработчик состояний авторизации
 */
@Component
public class AuthStateHandler implements StateHandler<AuthStateManager.AuthState> {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthStateHandler.class);
    
    private final AuthStateManager authStateManager;
    private final NotificationSettingsService notificationSettingsService;
    private final TelegramMessageSender messageSender;
    
    @Autowired
    public AuthStateHandler(
            AuthStateManager authStateManager,
            NotificationSettingsService notificationSettingsService,
            AbsSender bot) {
        this.authStateManager = authStateManager;
        this.notificationSettingsService = notificationSettingsService;
        this.messageSender = new TelegramMessageSender(bot);
    }
    
    @Override
    public boolean canHandle(String chatId, AuthStateManager.AuthState state) {
        return authStateManager.hasState(chatId) && authStateManager.getState(chatId) == state;
    }
    
    @Override
    public void handle(Long telegramUserId, String chatId, String text, AuthStateManager.AuthState state) {
        try {
            authStateManager.updateTimestamp(chatId);
            
            if (state == AuthStateManager.AuthState.WAITING_USERNAME) {
                handleUsernameInput(telegramUserId, chatId, text);
            } else if (state == AuthStateManager.AuthState.WAITING_PASSWORD) {
                handlePasswordInput(telegramUserId, chatId, text);
            }
        } catch (Exception e) {
            logger.error("Error handling auth state", e);
            authStateManager.clearState(chatId);
            messageSender.sendPersonalMessage(chatId, "❌ Произошла ошибка при обработке авторизации. Попробуйте позже или используйте /link <token>.");
        }
    }
    
    private void handleUsernameInput(Long telegramUserId, String chatId, String text) {
        String username = text.trim();
        if (username.isEmpty()) {
            messageSender.sendPersonalMessage(chatId, "❌ Логин не может быть пустым. Введите ваш логин:");
            return;
        }
        
        authStateManager.setUsername(chatId, username);
        authStateManager.setState(chatId, AuthStateManager.AuthState.WAITING_PASSWORD);
        
        messageSender.sendPersonalMessage(chatId, """
            🔑 Теперь введите ваш пароль:
            
            💡 Используйте /cancel для отмены.""");
    }
    
    private void handlePasswordInput(Long telegramUserId, String chatId, String text) {
        String password = text.trim();
        String username = authStateManager.getUsername(chatId);
        
        if (password.isEmpty()) {
            messageSender.sendPersonalMessage(chatId, "❌ Пароль не может быть пустым. Введите ваш пароль:");
            return;
        }
        
        if (username == null || username.isEmpty()) {
            authStateManager.clearState(chatId);
            messageSender.sendPersonalMessage(chatId, "❌ Ошибка: логин не найден. Начните заново с /auth.");
            return;
        }
        
        // Проверяем блокировку перед попыткой
        if (authStateManager.isBlocked(chatId)) {
            long remainingSeconds = authStateManager.getBlockTimeRemaining(chatId);
            long remainingMinutes = remainingSeconds / 60;
            authStateManager.clearState(chatId);
            messageSender.sendPersonalMessage(chatId, "⛔ Слишком много неудачных попыток авторизации.\n\n" +
                    "Попробуйте снова через " + remainingMinutes + " минут.");
            return;
        }
        
        try {
            // Пытаемся связать аккаунт
            notificationSettingsService.linkTelegramAccountByCredentials(
                    username, password, telegramUserId, chatId);
            
            // Успешная авторизация
            authStateManager.recordAttempt(chatId, true);
            authStateManager.clearState(chatId);
            
            messageSender.sendPersonalMessage(chatId, """
                ✅ <b>Аккаунт успешно связан!</b>
                
                Теперь вы будете получать персональные уведомления.
                
                Доступные команды:
                /games - Список предстоящих игр
                /help - Справка по командам
                /stop - Отписаться от уведомлений""");
            
            logger.info("Telegram account linked via auth for chatId: {}", chatId);
            
        } catch (RuntimeException e) {
            // Неудачная попытка
            authStateManager.recordAttempt(chatId, false);
            
            int remainingAttempts = authStateManager.getRemainingAttempts(chatId);
            
            if (authStateManager.isBlocked(chatId)) {
                long remainingSeconds = authStateManager.getBlockTimeRemaining(chatId);
                long remainingMinutes = remainingSeconds / 60;
                authStateManager.clearState(chatId);
                messageSender.sendPersonalMessage(chatId, "⛔ <b>Слишком много неудачных попыток</b>\n\n" +
                        "Авторизация заблокирована на " + remainingMinutes + " минут.\n\n" +
                        "Попробуйте снова позже или используйте /link <token> для привязки через токен.");
                logger.warn("Telegram auth blocked for chatId: {} after failed attempt", chatId);
            } else {
                // Ошибка авторизации, но еще есть попытки
                String errorMessage = e.getMessage();
                if (errorMessage != null && errorMessage.contains("Invalid username or password")) {
                    messageSender.sendPersonalMessage(chatId, "❌ <b>Неверный логин или пароль</b>\n\n" +
                            "Осталось попыток: " + remainingAttempts + "\n\n" +
                            "Введите пароль еще раз или используйте /cancel для отмены.");
                } else if (errorMessage != null && errorMessage.contains("уже связан")) {
                    authStateManager.clearState(chatId);
                    messageSender.sendPersonalMessage(chatId, "❌ " + errorMessage + "\n\n" +
                            "Используйте /start для проверки статуса.");
                } else {
                    messageSender.sendPersonalMessage(chatId, "❌ Ошибка: " + (errorMessage != null ? errorMessage : "Неизвестная ошибка") + "\n\n" +
                            "Осталось попыток: " + remainingAttempts + "\n\n" +
                            "Попробуйте снова или используйте /cancel для отмены.");
                }
                logger.warn("Telegram auth failed for chatId: {}, remaining attempts: {}", chatId, remainingAttempts);
            }
        }
    }
}


