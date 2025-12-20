package ru.ambryo.gameplannerback.service.telegram.menu;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.ambryo.gameplannerback.entity.User;
import ru.ambryo.gameplannerback.repository.UserRepository;
import ru.ambryo.gameplannerback.service.telegram.command.HelpCommandHandler;
import ru.ambryo.gameplannerback.service.telegram.keyboard.SettingsMenuKeyboardBuilder;
import ru.ambryo.gameplannerback.service.telegram.util.TelegramHtmlFormatter;

/**
 * Обработчик меню настроек
 */
@Component
public class SettingsMenuHandler implements MenuHandler {
    
    private final UserRepository userRepository;
    private final SettingsMenuKeyboardBuilder keyboardBuilder;
    private final HelpCommandHandler helpCommandHandler;
    private final MenuMessageUpdater messageUpdater;
    
    @Autowired
    public SettingsMenuHandler(
            UserRepository userRepository,
            SettingsMenuKeyboardBuilder keyboardBuilder,
            HelpCommandHandler helpCommandHandler,
            MenuMessageUpdater messageUpdater) {
        this.userRepository = userRepository;
        this.keyboardBuilder = keyboardBuilder;
        this.helpCommandHandler = helpCommandHandler;
        this.messageUpdater = messageUpdater;
    }
    
    @Override
    public boolean canHandle(String callbackData) {
        return callbackData.equals("menu_settings") ||
               callbackData.equals("menu_settings_profile") ||
               callbackData.equals("menu_settings_timezone") ||
               callbackData.equals("menu_settings_notifications") ||
               callbackData.equals("menu_help");
    }
    
    @Override
    public void handle(CallbackQuery callbackQuery, Long telegramUserId, String chatId, Integer messageId) {
        String data = callbackQuery.getData();
        User user = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
        boolean isLinked = user != null;

        switch (data) {
            case "menu_settings" -> {
                if (!isLinked) {
                    messageUpdater.answerCallback(callbackQuery.getId(), "❌ Аккаунт не связан. Зарегистрируйтесь для доступа к функциям.");
                    return;
                }
                String message = "⚙️ <b>Настройки</b>\n\nВыберите действие:";
                var keyboard = keyboardBuilder.build(true);
                messageUpdater.updateMessage(chatId, messageId, message, keyboard);

            }
            case "menu_settings_profile" -> handleProfile(user, chatId, messageId);
            case "menu_settings_timezone" -> {
                if (!isLinked) {
                    messageUpdater.answerCallback(callbackQuery.getId(), "❌ Аккаунт не связан. Зарегистрируйтесь для доступа к функциям.");
                    return;
                }
                handleTimezone(user, chatId, messageId);
            }
            case "menu_settings_notifications" -> {
                if (!isLinked) {
                    messageUpdater.answerCallback(callbackQuery.getId(), "❌ Аккаунт не связан. Зарегистрируйтесь для доступа к функциям.");
                    return;
                }
                // Обработка menu_settings_notifications будет в NotificationsMenuHandler
                messageUpdater.answerCallback(callbackQuery.getId(), "ℹ️ Настройки уведомлений обрабатываются отдельным обработчиком");
            }
            case "menu_help" -> {
                if (!isLinked) {
                    messageUpdater.answerCallback(callbackQuery.getId(), "❌ Аккаунт не связан. Зарегистрируйтесь для доступа к функциям.");
                    return;
                }
                // Вызываем help через команду
                var message = new org.telegram.telegrambots.meta.api.objects.Message();
                message.setText("/help");
                helpCommandHandler.handle(message, telegramUserId, chatId);

                // Возвращаемся в главное меню
                String menuMessage = "📱 <b>Главное меню</b>\n\n✅ Аккаунт связан\n👤 Пользователь: " + TelegramHtmlFormatter.escapeHtml(user.getUsername()) + "\n\nВыберите раздел:";
                var keyboard = new ru.ambryo.gameplannerback.service.telegram.keyboard.MainMenuKeyboardBuilder().build(true);
                messageUpdater.updateMessage(chatId, messageId, menuMessage, keyboard);
            }
        }
    }
    
    private void handleProfile(User user, String chatId, Integer messageId) {
        StringBuilder message = new StringBuilder();
        message.append("👤 <b>Профиль</b>\n\n");
        
        if (user == null) {
            message.append("❌ Аккаунт не связан\n\n");
            message.append("Для доступа ко всем функциям необходимо связать аккаунт:\n");
            message.append("• /register - регистрация нового аккаунта\n");
            message.append("• /auth - авторизация через логин/пароль\n");
            message.append("• /link <token> - привязка через токен");
        } else {
            message.append("✅ Аккаунт связан\n\n");
            message.append("👤 <b>Логин:</b> ").append(TelegramHtmlFormatter.escapeHtml(user.getUsername())).append("\n");
            message.append("📝 <b>Имя:</b> ").append(TelegramHtmlFormatter.escapeHtml(user.getName() != null ? user.getName() : "Не указано")).append("\n");
            message.append("📧 <b>Email:</b> ").append(TelegramHtmlFormatter.escapeHtml(user.getEmail() != null ? user.getEmail() : "Не указан")).append("\n");
            
            if (user.getTimezone() != null && !user.getTimezone().trim().isEmpty()) {
                message.append("🌍 <b>Часовой пояс:</b> ").append(TelegramHtmlFormatter.escapeHtml(user.getTimezone())).append("\n");
            } else {
                message.append("🌍 <b>Часовой пояс:</b> Не установлен\n");
                message.append("⚠️ Установите часовой пояс для использования разметки времени");
            }
        }
        
        var keyboard = keyboardBuilder.build(user != null);
        messageUpdater.updateMessage(chatId, messageId, message.toString(), keyboard);
    }
    
    private void handleTimezone(User user, String chatId, Integer messageId) {
        String currentTimezone = user.getTimezone() != null && !user.getTimezone().trim().isEmpty() 
                ? user.getTimezone() 
                : "Не установлен";
        
        String message = "🌍 <b>Смена часового пояса</b>\n\n" +
                "Текущий часовой пояс: <b>" + TelegramHtmlFormatter.escapeHtml(currentTimezone) + "</b>\n\n" +
                "Выберите новый часовой пояс из списка:";
        
        var keyboard = new ru.ambryo.gameplannerback.service.telegram.keyboard.TimezoneSelectorKeyboardBuilder().build(user.getTimezone());
        messageUpdater.updateMessage(chatId, messageId, message, keyboard);
    }
}
