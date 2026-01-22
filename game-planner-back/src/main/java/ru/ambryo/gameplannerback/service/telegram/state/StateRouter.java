package ru.ambryo.gameplannerback.service.telegram.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.ambryo.gameplannerback.service.telegram.exception.TelegramExceptionHandler;
import ru.ambryo.gameplannerback.service.telegram.state.handler.*;

/**
 * Роутер для обработки состояний диалогов в Telegram боте
 */
@Component
public class StateRouter {
    
    private static final Logger logger = LoggerFactory.getLogger(StateRouter.class);
    
    private final AuthStateManager authStateManager;
    private final RegistrationStateManager registrationStateManager;
    private final TimeSlotMarkingStateManager timeSlotMarkingStateManager;
    private final TimezoneChangeStateManager timezoneChangeStateManager;
    private final NotificationStateManager notificationStateManager;
    
    private final AuthStateHandler authStateHandler;
    private final RegistrationStateHandler registrationStateHandler;
    private final TimeSlotMarkingStateHandler timeSlotMarkingStateHandler;
    private final TimezoneChangeStateHandler timezoneChangeStateHandler;
    private final NotificationStateHandler notificationStateHandler;
    
    private final TelegramExceptionHandler exceptionHandler;
    
    @Autowired
    public StateRouter(
            AuthStateManager authStateManager,
            RegistrationStateManager registrationStateManager,
            TimeSlotMarkingStateManager timeSlotMarkingStateManager,
            TimezoneChangeStateManager timezoneChangeStateManager,
            NotificationStateManager notificationStateManager,
            AuthStateHandler authStateHandler,
            RegistrationStateHandler registrationStateHandler,
            TimeSlotMarkingStateHandler timeSlotMarkingStateHandler,
            TimezoneChangeStateHandler timezoneChangeStateHandler,
            NotificationStateHandler notificationStateHandler,
            TelegramExceptionHandler exceptionHandler) {
        this.authStateManager = authStateManager;
        this.registrationStateManager = registrationStateManager;
        this.timeSlotMarkingStateManager = timeSlotMarkingStateManager;
        this.timezoneChangeStateManager = timezoneChangeStateManager;
        this.notificationStateManager = notificationStateManager;
        this.authStateHandler = authStateHandler;
        this.registrationStateHandler = registrationStateHandler;
        this.timeSlotMarkingStateHandler = timeSlotMarkingStateHandler;
        this.timezoneChangeStateHandler = timezoneChangeStateHandler;
        this.notificationStateHandler = notificationStateHandler;
        this.exceptionHandler = exceptionHandler;
    }
    
    /**
     * Обрабатывает текстовое сообщение, проверяя активные состояния
     * @param telegramUserId ID пользователя Telegram
     * @param chatId ID чата
     * @param text текст сообщения
     */
    public void handle(Long telegramUserId, String chatId, String text) {
        // Проверяем состояния в порядке приоритета
        // Регистрация -> Авторизация -> Разметка времени -> Смена часового пояса -> Настройки уведомлений
        
        try {
            // Проверяем регистрацию
            if (registrationStateManager.hasState(chatId)) {
                var state = registrationStateManager.getState(chatId);
                if (state != null && registrationStateHandler.canHandle(chatId, state)) {
                    registrationStateHandler.handle(telegramUserId, chatId, text, state);
                    return;
                }
            }
            
            // Проверяем авторизацию
            if (authStateManager.hasState(chatId)) {
                var state = authStateManager.getState(chatId);
                if (state != null && authStateHandler.canHandle(chatId, state)) {
                    authStateHandler.handle(telegramUserId, chatId, text, state);
                    return;
                }
            }
            
            // Проверяем разметку времени
            if (timeSlotMarkingStateManager.hasState(chatId)) {
                var state = timeSlotMarkingStateManager.getState(chatId);
                if (state != null && timeSlotMarkingStateHandler.canHandle(chatId, state)) {
                    timeSlotMarkingStateHandler.handle(telegramUserId, chatId, text, state);
                    return;
                }
            }
            
            // Проверяем смену часового пояса
            if (timezoneChangeStateManager.hasState(chatId)) {
                var state = timezoneChangeStateManager.getState(chatId);
                if (state != null && timezoneChangeStateHandler.canHandle(chatId, state)) {
                    timezoneChangeStateHandler.handle(telegramUserId, chatId, text, state);
                    return;
                }
            }
            
            // Проверяем настройки уведомлений
            if (notificationStateManager.hasState(chatId)) {
                var state = notificationStateManager.getState(chatId);
                if (state != null && notificationStateHandler.canHandle(chatId, state)) {
                    notificationStateHandler.handle(telegramUserId, chatId, text, state);
                }
            }
        } catch (Exception e) {
            logger.error("Error in StateRouter handling state", e);
            exceptionHandler.handleException(chatId, e, "❌ Произошла ошибка при обработке состояния. Попробуйте позже.");
        }
    }
}




