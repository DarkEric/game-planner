package ru.ambryo.gameplannerback.service.telegram.state.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.bots.AbsSender;
import ru.ambryo.gameplannerback.entity.User;
import ru.ambryo.gameplannerback.repository.UserRepository;
import ru.ambryo.gameplannerback.service.UserService;
import ru.ambryo.gameplannerback.service.telegram.state.TimezoneChangeStateManager;
import ru.ambryo.gameplannerback.service.telegram.util.TelegramHtmlFormatter;
import ru.ambryo.gameplannerback.service.telegram.util.TelegramMessageSender;

import java.time.ZoneId;

/**
 * Обработчик состояний смены часового пояса
 */
@Component
public class TimezoneChangeStateHandler implements StateHandler<TimezoneChangeStateManager.TimezoneChangeState> {
    
    private static final Logger logger = LoggerFactory.getLogger(TimezoneChangeStateHandler.class);
    
    private final TimezoneChangeStateManager timezoneChangeStateManager;
    private final UserRepository userRepository;
    private final UserService userService;
    private final TelegramMessageSender messageSender;
    
    @Autowired
    public TimezoneChangeStateHandler(
            TimezoneChangeStateManager timezoneChangeStateManager,
            UserRepository userRepository,
            UserService userService,
            AbsSender bot) {
        this.timezoneChangeStateManager = timezoneChangeStateManager;
        this.userRepository = userRepository;
        this.userService = userService;
        this.messageSender = new TelegramMessageSender(bot);
    }
    
    @Override
    public boolean canHandle(String chatId, TimezoneChangeStateManager.TimezoneChangeState state) {
        return timezoneChangeStateManager.hasState(chatId) && timezoneChangeStateManager.getState(chatId) == state;
    }
    
    @Override
    public void handle(Long telegramUserId, String chatId, String text, TimezoneChangeStateManager.TimezoneChangeState state) {
        try {
            if (state == TimezoneChangeStateManager.TimezoneChangeState.WAITING_TIMEZONE) {
                timezoneChangeStateManager.updateTimestamp(chatId);
                
                User user = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
                if (user == null) {
                    timezoneChangeStateManager.clearState(chatId);
                    messageSender.sendPersonalMessage(chatId, "❌ Ваш аккаунт не связан. Используйте /link для связывания.");
                    return;
                }
                
                String timezoneInput = text.trim();
                
                // Проверяем валидность часового пояса
                ZoneId zoneId;
                try {
                    zoneId = ZoneId.of(timezoneInput);
                } catch (Exception e) {
                    messageSender.sendPersonalMessage(chatId, "❌ <b>Неверный часовой пояс</b>\n\n" +
                            "Часовой пояс '" + TelegramHtmlFormatter.escapeHtml(timezoneInput) + "' не найден.\n\n" +
                            "Пожалуйста, введите корректный IANA часовой пояс (например: Europe/Moscow)\n\n" +
                            "💡 Используйте /cancel для отмены.");
                    return;
                }
                
                // Обновляем часовой пояс пользователя
                userService.updateUserProfile(user, user.getName(), user.getColor(), zoneId.getId());
                
                // Очищаем состояние
                timezoneChangeStateManager.clearState(chatId);
                
                // Отправляем подтверждение
                messageSender.sendPersonalMessage(chatId, "✅ <b>Часовой пояс успешно изменен!</b>\n\n" +
                        "Новый часовой пояс: <b>" + TelegramHtmlFormatter.escapeHtml(zoneId.getId()) + "</b>\n\n" +
                        "Теперь вы можете использовать разметку времени через /mark");
                
                logger.info("Timezone changed via Telegram (manual) for user: {}, new timezone: {}", user.getUsername(), zoneId.getId());
            }
        } catch (Exception e) {
            logger.error("Error handling timezone change state", e);
            timezoneChangeStateManager.clearState(chatId);
            messageSender.sendPersonalMessage(chatId, "❌ Произошла ошибка при смене часового пояса. Попробуйте позже.");
        }
    }
}


