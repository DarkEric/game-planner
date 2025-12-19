package ru.ambryo.gameplannerback.service.telegram.menu;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.bots.AbsSender;
import ru.ambryo.gameplannerback.entity.User;
import ru.ambryo.gameplannerback.repository.UserRepository;
import ru.ambryo.gameplannerback.service.telegram.state.RegistrationStateManager;
import ru.ambryo.gameplannerback.service.telegram.state.AuthStateManager;
import ru.ambryo.gameplannerback.service.telegram.keyboard.MainMenuKeyboardBuilder;
import ru.ambryo.gameplannerback.service.telegram.util.TelegramHtmlFormatter;
import ru.ambryo.gameplannerback.service.telegram.util.TelegramMessageSender;

/**
 * Обработчик главного меню
 */
@Component
public class MainMenuHandler implements MenuHandler {
    
    private final UserRepository userRepository;
    private final MainMenuKeyboardBuilder keyboardBuilder;
    private final RegistrationStateManager registrationStateManager;
    private final AuthStateManager authStateManager;
    private final TelegramMessageSender messageSender;
    private final MenuMessageUpdater messageUpdater;
    
    @Autowired
    public MainMenuHandler(
            UserRepository userRepository,
            MainMenuKeyboardBuilder keyboardBuilder,
            RegistrationStateManager registrationStateManager,
            AuthStateManager authStateManager,
            AbsSender bot,
            MenuMessageUpdater messageUpdater) {
        this.userRepository = userRepository;
        this.keyboardBuilder = keyboardBuilder;
        this.registrationStateManager = registrationStateManager;
        this.authStateManager = authStateManager;
        this.messageSender = new TelegramMessageSender(bot);
        this.messageUpdater = messageUpdater;
    }
    
    @Override
    public boolean canHandle(String callbackData) {
        return callbackData.equals("menu_main") ||
               callbackData.equals("menu_register") ||
               callbackData.equals("menu_auth") ||
               callbackData.equals("menu_link");
    }
    
    @Override
    public void handle(CallbackQuery callbackQuery, Long telegramUserId, String chatId, Integer messageId) {
        String data = callbackQuery.getData();
        User user = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
        boolean isLinked = user != null;
        
        if (data.equals("menu_main")) {
            String message = "📱 <b>Главное меню</b>\n\n";
            if (isLinked && user != null) {
                message += "✅ Аккаунт связан\n";
                message += "👤 Пользователь: " + TelegramHtmlFormatter.escapeHtml(user.getUsername()) + "\n\n";
            } else {
                message += "❌ Аккаунт не связан\n\n";
            }
            message += "Выберите раздел:";
            
            var keyboard = keyboardBuilder.build(isLinked);
            messageUpdater.updateMessage(chatId, messageId, message, keyboard);
            
        } else if (data.equals("menu_register")) {
            if (isLinked) {
                messageUpdater.answerCallback(callbackQuery.getId(), "✅ Вы уже зарегистрированы!");
                return;
            }
            // Инициализируем регистрацию
            registrationStateManager.setState(chatId, RegistrationStateManager.RegistrationState.WAITING_INVITE);
            registrationStateManager.setData(chatId, new RegistrationStateManager.RegistrationData());
            messageSender.sendPersonalMessage(chatId, "📝 <b>Регистрация нового аккаунта</b>\n\n" +
                    "Введите инвайт-код для регистрации:\n\n" +
                    "💡 Используйте /cancel для отмены.");
            
            String menuMessage = "📱 <b>Главное меню</b>\n\n❌ Аккаунт не связан\n\nДля доступа ко всем функциям необходимо зарегистрироваться.\n\nНажмите кнопку ниже, чтобы начать регистрацию:";
            var keyboard = keyboardBuilder.build(false);
            messageUpdater.updateMessage(chatId, messageId, menuMessage, keyboard);
            
        } else if (data.equals("menu_auth")) {
            if (isLinked) {
                messageUpdater.answerCallback(callbackQuery.getId(), "✅ Ваш аккаунт уже связан!");
                return;
            }
            // Инициализируем авторизацию
            authStateManager.setState(chatId, AuthStateManager.AuthState.WAITING_USERNAME);
            messageSender.sendPersonalMessage(chatId, "🔐 <b>Авторизация для привязки аккаунта</b>\n\n" +
                    "Введите ваш логин (имя пользователя):\n\n" +
                    "💡 Используйте /cancel для отмены.");
            
            String menuMessage = "📱 <b>Главное меню</b>\n\n❌ Аккаунт не связан\n\nДля доступа ко всем функциям необходимо связать аккаунт.\n\nВыберите способ связывания:";
            var keyboard = keyboardBuilder.build(false);
            messageUpdater.updateMessage(chatId, messageId, menuMessage, keyboard);
            
        } else if (data.equals("menu_link")) {
            if (isLinked) {
                messageUpdater.answerCallback(callbackQuery.getId(), "✅ Ваш аккаунт уже связан!");
                return;
            }
            // Отправляем инструкцию по использованию токена
            messageSender.sendPersonalMessage(chatId, "🔗 <b>Связывание аккаунта через токен</b>\n\n" +
                    "Для связывания аккаунта через токен:\n\n" +
                    "1. Откройте настройки профиля на веб-сайте\n" +
                    "2. Получите токен для связывания Telegram\n" +
                    "3. Отправьте команду: <code>/link &lt;token&gt;</code>\n\n" +
                    "Например: <code>/link abc123xyz</code>\n\n" +
                    "💡 Используйте /cancel для отмены.");
            
            String menuMessage = "📱 <b>Главное меню</b>\n\n❌ Аккаунт не связан\n\nДля доступа ко всем функциям необходимо связать аккаунт.\n\nВыберите способ связывания:";
            var keyboard = keyboardBuilder.build(false);
            messageUpdater.updateMessage(chatId, messageId, menuMessage, keyboard);
        }
    }
}

