package ru.ambryo.gameplannerback.service.telegram.state.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.bots.AbsSender;
import ru.ambryo.gameplannerback.entity.User;
import ru.ambryo.gameplannerback.repository.UserRepository;
import ru.ambryo.gameplannerback.service.AuthService;
import ru.ambryo.gameplannerback.service.InviteService;
import ru.ambryo.gameplannerback.service.telegram.config.TelegramBotProperties;
import ru.ambryo.gameplannerback.service.telegram.state.RegistrationStateManager;
import ru.ambryo.gameplannerback.service.telegram.util.TelegramMessageSender;
import ru.ambryo.gameplannerback.service.telegram.util.TelegramValidationUtils;

/**
 * Обработчик состояний регистрации
 */
@Component
public class RegistrationStateHandler implements StateHandler<RegistrationStateManager.RegistrationState> {
    
    private static final Logger logger = LoggerFactory.getLogger(RegistrationStateHandler.class);
    
    private final RegistrationStateManager registrationStateManager;
    private final InviteService inviteService;
    private final UserRepository userRepository;
    private final AuthService authService;
    private final TelegramBotProperties telegramBotProperties;
    private final TelegramMessageSender messageSender;
    
    @Autowired
    public RegistrationStateHandler(
            RegistrationStateManager registrationStateManager,
            InviteService inviteService,
            UserRepository userRepository,
            AuthService authService,
            TelegramBotProperties telegramBotProperties,
            AbsSender bot) {
        this.registrationStateManager = registrationStateManager;
        this.inviteService = inviteService;
        this.userRepository = userRepository;
        this.authService = authService;
        this.telegramBotProperties = telegramBotProperties;
        this.messageSender = new TelegramMessageSender(bot);
    }
    
    @Override
    public boolean canHandle(String chatId, RegistrationStateManager.RegistrationState state) {
        return registrationStateManager.hasState(chatId) && registrationStateManager.getState(chatId) == state;
    }
    
    @Override
    public void handle(Long telegramUserId, String chatId, String text, RegistrationStateManager.RegistrationState state) {
        try {
            registrationStateManager.updateTimestamp(chatId);
            var data = registrationStateManager.getData(chatId);
            
            if (data == null) {
                registrationStateManager.clearState(chatId);
                messageSender.sendPersonalMessage(chatId, "❌ Ошибка: данные регистрации не найдены. Начните заново с /register.");
                return;
            }
            
            switch (state) {
                case WAITING_INVITE:
                    handleInviteInput(telegramUserId, chatId, text.trim(), data);
                    break;
                case WAITING_USERNAME:
                    handleUsernameInput(chatId, text.trim(), data);
                    break;
                case WAITING_NAME:
                    handleNameInput(chatId, text.trim(), data);
                    break;
                case WAITING_EMAIL:
                    handleEmailInput(chatId, text.trim(), data);
                    break;
                case WAITING_PASSWORD:
                    handlePasswordInput(chatId, text.trim(), data);
                    break;
                case WAITING_PASSWORD_CONFIRM:
                    handlePasswordConfirmInput(telegramUserId, chatId, text.trim(), data);
                    break;
            }
        } catch (Exception e) {
            logger.error("Error handling registration state", e);
            registrationStateManager.clearState(chatId);
            messageSender.sendPersonalMessage(chatId, "❌ Произошла ошибка при обработке регистрации. Попробуйте позже.");
        }
    }
    
    private void handleInviteInput(Long telegramUserId, String chatId, String inviteCode, RegistrationStateManager.RegistrationData data) {
        if (inviteCode.isEmpty()) {
            messageSender.sendPersonalMessage(chatId, "❌ Инвайт-код не может быть пустым. Введите инвайт-код:");
            return;
        }
        
        try {
            // Проверяем валидность инвайт-кода
            inviteService.getInviteByCode(inviteCode);
            data.inviteCode = inviteCode;
            registrationStateManager.setState(chatId, RegistrationStateManager.RegistrationState.WAITING_USERNAME);
            
            messageSender.sendPersonalMessage(chatId, """
                ✅ Инвайт-код принят!
                
                Введите логин (имя пользователя):
                
                💡 Используйте /cancel для отмены.""");
        } catch (RuntimeException e) {
            String errorMsg = e.getMessage();
            if (errorMsg != null && (errorMsg.contains("not found") || errorMsg.contains("Invalid"))) {
                messageSender.sendPersonalMessage(chatId, """
                    ❌ <b>Неверный инвайт-код</b>
                    
                    Проверьте правильность кода и попробуйте снова.
                    
                    💡 Используйте /cancel для отмены.""");
            } else if (errorMsg != null && (errorMsg.contains("expired") || errorMsg.contains("used"))) {
                messageSender.sendPersonalMessage(chatId, """
                    ❌ <b>Инвайт-код недействителен</b>
                    
                    Код истек или уже использован.
                    
                    💡 Используйте /cancel для отмены.""");
            } else {
                messageSender.sendPersonalMessage(chatId, "❌ Ошибка: " + (errorMsg != null ? errorMsg : "Неизвестная ошибка") + "\n\n" +
                        "Попробуйте снова или используйте /cancel для отмены.");
            }
            logger.warn("Invalid invite code for registration: {}", inviteCode);
        }
    }
    
    private void handleUsernameInput(String chatId, String username, RegistrationStateManager.RegistrationData data) {
        if (username.isEmpty()) {
            messageSender.sendPersonalMessage(chatId, "❌ Логин не может быть пустым. Введите логин:");
            return;
        }
        
        // Проверяем уникальность логина
        if (userRepository.existsByUsername(username)) {
            messageSender.sendPersonalMessage(chatId, """
                ❌ <b>Логин уже занят</b>
                
                Выберите другой логин:
                
                💡 Используйте /cancel для отмены.""");
            return;
        }
        
        data.username = username;
        registrationStateManager.setState(chatId, RegistrationStateManager.RegistrationState.WAITING_NAME);
        
        messageSender.sendPersonalMessage(chatId, """
            ✅ Логин принят!
            
            Введите ваше имя (или нажмите Enter, чтобы использовать логин):
            
            💡 Используйте /cancel для отмены.""");
    }
    
    private void handleNameInput(String chatId, String name, RegistrationStateManager.RegistrationData data) {
        // Имя опционально, если пустое - используем username
        if (name.trim().isEmpty()) {
            data.name = data.username;
        } else {
            data.name = name.trim();
        }
        
        registrationStateManager.setState(chatId, RegistrationStateManager.RegistrationState.WAITING_EMAIL);
        
        messageSender.sendPersonalMessage(chatId, """
            ✅ Имя принято!
            
            Введите ваш email:
            
            💡 Используйте /cancel для отмены.""");
    }
    
    private void handleEmailInput(String chatId, String email, RegistrationStateManager.RegistrationData data) {
        if (email.isEmpty()) {
            messageSender.sendPersonalMessage(chatId, "❌ Email не может быть пустым. Введите email:");
            return;
        }
        
        // Проверяем формат email
        if (!TelegramValidationUtils.isValidEmail(email)) {
            messageSender.sendPersonalMessage(chatId, """
                ❌ <b>Неверный формат email</b>
                
                Введите корректный email адрес:
                
                💡 Используйте /cancel для отмены.""");
            return;
        }
        
        // Проверяем уникальность email
        if (userRepository.existsByEmail(email)) {
            messageSender.sendPersonalMessage(chatId, """
                ❌ <b>Email уже используется</b>
                
                Используйте другой email:
                
                💡 Используйте /cancel для отмены.""");
            return;
        }
        
        data.email = email;
        registrationStateManager.setState(chatId, RegistrationStateManager.RegistrationState.WAITING_PASSWORD);
        
        messageSender.sendPersonalMessage(chatId, "✅ Email принят!\n\n" +
                "Введите пароль (минимум " + telegramBotProperties.getMinPasswordLength() + " символов):\n\n" +
                "💡 Используйте /cancel для отмены.");
    }
    
    private void handlePasswordInput(String chatId, String password, RegistrationStateManager.RegistrationData data) {
        if (password.isEmpty()) {
            messageSender.sendPersonalMessage(chatId, "❌ Пароль не может быть пустым. Введите пароль:");
            return;
        }
        
        int minPasswordLength = telegramBotProperties.getMinPasswordLength();
        if (password.length() < minPasswordLength) {
            messageSender.sendPersonalMessage(chatId, "❌ <b>Пароль слишком короткий</b>\n\n" +
                    "Пароль должен содержать минимум " + minPasswordLength + " символов.\n\n" +
                    "Введите пароль еще раз:\n\n" +
                    "💡 Используйте /cancel для отмены.");
            return;
        }
        
        data.password = password;
        registrationStateManager.setState(chatId, RegistrationStateManager.RegistrationState.WAITING_PASSWORD_CONFIRM);
        
        messageSender.sendPersonalMessage(chatId, """
            ✅ Пароль принят!
            
            Подтвердите пароль (введите его еще раз):
            
            💡 Используйте /cancel для отмены.""");
    }
    
    private void handlePasswordConfirmInput(Long telegramUserId, String chatId, String passwordConfirm, RegistrationStateManager.RegistrationData data) {
        if (passwordConfirm.isEmpty()) {
            messageSender.sendPersonalMessage(chatId, "❌ Подтверждение пароля не может быть пустым. Введите пароль еще раз:");
            return;
        }
        
        if (!passwordConfirm.equals(data.password)) {
            messageSender.sendPersonalMessage(chatId, """
                ❌ <b>Пароли не совпадают</b>
                
                Введите пароль еще раз:
                
                💡 Используйте /cancel для отмены.""");
            // Возвращаемся к вводу пароля
            registrationStateManager.setState(chatId, RegistrationStateManager.RegistrationState.WAITING_PASSWORD);
            return;
        }
        
        // Все данные собраны, выполняем регистрацию
        try {
            // Проверяем блокировку перед попыткой
            if (registrationStateManager.isBlocked(chatId)) {
                long remainingSeconds = registrationStateManager.getBlockTimeRemaining(chatId);
                long remainingMinutes = remainingSeconds / 60;
                registrationStateManager.clearState(chatId);
                messageSender.sendPersonalMessage(chatId, "⛔ Слишком много неудачных попыток регистрации.\n\n" +
                        "Попробуйте снова через " + remainingMinutes + " минут.");
                return;
            }
            
            // Выполняем регистрацию через AuthService
            authService.register(
                    data.username,
                    data.password,
                    data.email,
                    data.inviteCode,
                    data.name
            );
            
            // Регистрация успешна - автоматически привязываем Telegram
            User registeredUser = userRepository.findByUsername(data.username)
                    .orElseThrow(() -> new RuntimeException("User not found after registration"));
            
            registeredUser.setTelegramUserId(telegramUserId);
            registeredUser.setTelegramChatId(chatId);
            registeredUser.setTelegramSubscribed(true);
            userRepository.save(registeredUser);
            
            // Успешная регистрация
            registrationStateManager.recordAttempt(chatId, true);
            registrationStateManager.clearState(chatId);
            
            messageSender.sendPersonalMessage(chatId, """
                🎉 <b>Регистрация успешна!</b>
                
                Ваш аккаунт создан и автоматически привязан к Telegram.
                
                Теперь вы будете получать персональные уведомления.
                
                Доступные команды:
                /games - Список предстоящих игр
                /help - Справка по командам
                /stop - Отписаться от уведомлений""");
            
            logger.info("Telegram registration successful for chatId: {}, username: {}", chatId, data.username);
            
        } catch (RuntimeException e) {
            // Неудачная попытка регистрации
            registrationStateManager.recordAttempt(chatId, false);
            
            int remainingAttempts = registrationStateManager.getRemainingAttempts(chatId);
            String errorMsg = e.getMessage();
            
            if (registrationStateManager.isBlocked(chatId)) {
                long remainingSeconds = registrationStateManager.getBlockTimeRemaining(chatId);
                long remainingMinutes = remainingSeconds / 60;
                registrationStateManager.clearState(chatId);
                messageSender.sendPersonalMessage(chatId, "⛔ <b>Слишком много неудачных попыток</b>\n\n" +
                        "Регистрация заблокирована на " + remainingMinutes + " минут.\n\n" +
                        "Попробуйте снова позже.");
                logger.warn("Telegram registration blocked for chatId: {} after failed attempt", chatId);
            } else {
                // Ошибка регистрации, но еще есть попытки
                if (errorMsg != null && errorMsg.contains("already exists")) {
                    if (errorMsg.contains("Username")) {
                        messageSender.sendPersonalMessage(chatId, "❌ <b>Логин уже занят</b>\n\n" +
                                "Начните регистрацию заново с /register и выберите другой логин.\n\n" +
                                "Осталось попыток: " + remainingAttempts);
                    } else if (errorMsg.contains("Email")) {
                        messageSender.sendPersonalMessage(chatId, "❌ <b>Email уже используется</b>\n\n" +
                                "Начните регистрацию заново с /register и используйте другой email.\n\n" +
                                "Осталось попыток: " + remainingAttempts);
                    } else {
                        messageSender.sendPersonalMessage(chatId, "❌ Ошибка: " + errorMsg + "\n\n" +
                                "Осталось попыток: " + remainingAttempts + "\n\n" +
                                "Начните регистрацию заново с /register.");
                    }
                } else if (errorMsg != null && errorMsg.contains("Invite")) {
                    messageSender.sendPersonalMessage(chatId, "❌ <b>Ошибка с инвайт-кодом</b>\n\n" +
                            errorMsg + "\n\n" +
                            "Начните регистрацию заново с /register.\n\n" +
                            "Осталось попыток: " + remainingAttempts);
                } else {
                    messageSender.sendPersonalMessage(chatId, "❌ Ошибка регистрации: " + (errorMsg != null ? errorMsg : "Неизвестная ошибка") + "\n\n" +
                            "Осталось попыток: " + remainingAttempts + "\n\n" +
                            "Начните регистрацию заново с /register или используйте /cancel для отмены.");
                }
                logger.warn("Telegram registration failed for chatId: {}, error: {}, remaining attempts: {}", 
                        chatId, errorMsg, remainingAttempts);
            }
        }
    }
}


