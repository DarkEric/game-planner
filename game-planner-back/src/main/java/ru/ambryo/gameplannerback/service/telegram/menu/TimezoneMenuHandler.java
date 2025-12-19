package ru.ambryo.gameplannerback.service.telegram.menu;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.ambryo.gameplannerback.entity.User;
import ru.ambryo.gameplannerback.repository.UserRepository;
import ru.ambryo.gameplannerback.service.UserService;
import ru.ambryo.gameplannerback.service.telegram.keyboard.SettingsMenuKeyboardBuilder;
import ru.ambryo.gameplannerback.service.telegram.keyboard.TimezoneSelectorKeyboardBuilder;
import ru.ambryo.gameplannerback.service.telegram.state.TimezoneChangeStateManager;
import ru.ambryo.gameplannerback.service.telegram.util.TelegramHtmlFormatter;
import ru.ambryo.gameplannerback.service.telegram.util.TelegramMessageSender;

import java.time.ZoneId;

/**
 * Обработчик меню часового пояса
 */
@Component
public class TimezoneMenuHandler implements MenuHandler {
    
    private final UserRepository userRepository;
    private final UserService userService;
    private final TimezoneChangeStateManager stateManager;
    private final TimezoneSelectorKeyboardBuilder keyboardBuilder;
    private final SettingsMenuKeyboardBuilder settingsKeyboardBuilder;
    private final TelegramMessageSender messageSender;
    private final MenuMessageUpdater messageUpdater;
    
    @Autowired
    public TimezoneMenuHandler(
            UserRepository userRepository,
            UserService userService,
            TimezoneChangeStateManager stateManager,
            TimezoneSelectorKeyboardBuilder keyboardBuilder,
            SettingsMenuKeyboardBuilder settingsKeyboardBuilder,
            org.telegram.telegrambots.meta.bots.AbsSender bot,
            MenuMessageUpdater messageUpdater) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.stateManager = stateManager;
        this.keyboardBuilder = keyboardBuilder;
        this.settingsKeyboardBuilder = settingsKeyboardBuilder;
        this.messageSender = new TelegramMessageSender(bot);
        this.messageUpdater = messageUpdater;
    }
    
    @Override
    public boolean canHandle(String callbackData) {
        return callbackData.startsWith("timezone_select_") ||
               callbackData.equals("timezone_manual") ||
               callbackData.equals("timezone_separator");
    }
    
    @Override
    public void handle(CallbackQuery callbackQuery, Long telegramUserId, String chatId, Integer messageId) {
        String data = callbackQuery.getData();
        User user = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
        
        if (user == null) {
            messageUpdater.answerCallback(callbackQuery.getId(), "❌ Ваш аккаунт не связан. Используйте /link для связывания.");
            return;
        }
        
        if (data.equals("timezone_separator")) {
            // Игнорируем нажатие на разделитель
            messageUpdater.answerCallback(callbackQuery.getId());
            return;
        }
        
        if (data.startsWith("timezone_select_")) {
            String timezoneId = data.substring("timezone_select_".length());
            handleTimezoneSelect(user, chatId, messageId, timezoneId, callbackQuery.getId());
            
        } else if (data.equals("timezone_manual")) {
            handleTimezoneManual(user, chatId, messageId);
        }
    }
    
    private void handleTimezoneSelect(User user, String chatId, Integer messageId, String timezoneId, String callbackQueryId) {
        try {
            // Проверяем валидность часового пояса
            ZoneId zoneId;
            try {
                zoneId = ZoneId.of(timezoneId);
            } catch (Exception e) {
                messageUpdater.answerCallback(callbackQueryId, "❌ Неверный часовой пояс");
                return;
            }
            
            // Обновляем часовой пояс пользователя
            userService.updateUserProfile(user, user.getName(), user.getColor(), zoneId.getId());
            
            // Обновляем сообщение с подтверждением
            String message = "✅ <b>Часовой пояс успешно изменен!</b>\n\n" +
                    "Новый часовой пояс: <b>" + TelegramHtmlFormatter.escapeHtml(zoneId.getId()) + "</b>\n\n" +
                    "Теперь вы можете использовать разметку времени через /mark";
            
            var keyboard = settingsKeyboardBuilder.build(true);
            messageUpdater.updateMessage(chatId, messageId, message, keyboard);
            messageUpdater.answerCallback(callbackQueryId, "✅ Часовой пояс изменен!");
            
        } catch (Exception e) {
            messageUpdater.answerCallback(callbackQueryId, "❌ Ошибка при смене часового пояса");
        }
    }
    
    private void handleTimezoneManual(User user, String chatId, Integer messageId) {
        try {
            // Инициализируем состояние смены часового пояса для ручного ввода
            stateManager.setState(chatId, TimezoneChangeStateManager.TimezoneChangeState.WAITING_TIMEZONE);
            
            String currentTimezone = user.getTimezone() != null && !user.getTimezone().trim().isEmpty() 
                    ? user.getTimezone() 
                    : "Не установлен";
            
            String message = "🌍 <b>Смена часового пояса</b>\n\n" +
                    "Текущий часовой пояс: <b>" + TelegramHtmlFormatter.escapeHtml(currentTimezone) + "</b>\n\n" +
                    "Введите новый часовой пояс в формате IANA (например: Europe/Moscow, America/New_York, Asia/Tokyo)\n\n" +
                    "💡 Используйте /cancel для отмены.\n" +
                    "💡 Полный список: https://en.wikipedia.org/wiki/List_of_tz_database_time_zones";
            
            // Отправляем новое сообщение для диалога
            messageSender.sendPersonalMessage(chatId, message);
            
            // Возвращаемся в подменю настроек
            String menuMessage = "⚙️ <b>Настройки</b>\n\nВыберите действие:";
            var keyboard = settingsKeyboardBuilder.build(true);
            messageUpdater.updateMessage(chatId, messageId, menuMessage, keyboard);
        } catch (Exception e) {
            messageUpdater.answerCallback("", "❌ Ошибка при инициализации ручного ввода");
        }
    }
}
