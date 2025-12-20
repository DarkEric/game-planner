package ru.ambryo.gameplannerback.service.telegram.menu;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.ambryo.gameplannerback.entity.User;
import ru.ambryo.gameplannerback.repository.UserRepository;
import ru.ambryo.gameplannerback.service.UserService;
import ru.ambryo.gameplannerback.service.telegram.keyboard.TimeMenuKeyboardBuilder;
import ru.ambryo.gameplannerback.service.telegram.message.TimeSlotMessageBuilder;
import ru.ambryo.gameplannerback.service.telegram.state.TimeSlotMarkingStateManager;
import ru.ambryo.gameplannerback.service.telegram.util.TelegramMessageSender;

import java.time.Instant;

/**
 * Обработчик меню разметки времени
 */
@Component
public class TimeMenuHandler implements MenuHandler {
    
    private final UserRepository userRepository;
    private final UserService userService;
    private final TimeSlotMarkingStateManager stateManager;
    private final TimeMenuKeyboardBuilder keyboardBuilder;
    private final TimeSlotMessageBuilder messageBuilder;
    private final TelegramMessageSender messageSender;
    private final MenuMessageUpdater messageUpdater;
    
    @Autowired
    public TimeMenuHandler(
            UserRepository userRepository,
            UserService userService,
            TimeSlotMarkingStateManager stateManager,
            TimeMenuKeyboardBuilder keyboardBuilder,
            TimeSlotMessageBuilder messageBuilder,
            @Lazy org.telegram.telegrambots.meta.bots.AbsSender bot,
            MenuMessageUpdater messageUpdater) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.stateManager = stateManager;
        this.keyboardBuilder = keyboardBuilder;
        this.messageBuilder = messageBuilder;
        this.messageSender = new TelegramMessageSender(bot);
        this.messageUpdater = messageUpdater;
    }
    
    @Override
    public boolean canHandle(String callbackData) {
        return callbackData.equals("menu_time") ||
               callbackData.equals("menu_time_mark") ||
               callbackData.equals("menu_time_slots");
    }
    
    @Override
    public void handle(CallbackQuery callbackQuery, Long telegramUserId, String chatId, Integer messageId) {
        String data = callbackQuery.getData();
        User user = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
        
        if (user == null) {
            messageUpdater.answerCallback(callbackQuery.getId(), "❌ Аккаунт не связан. Используйте /link для связывания.");
            return;
        }

        switch (data) {
            case "menu_time" -> {
                String message = "📅 <b>Разметка времени</b>\n\nВыберите действие:";
                var keyboard = keyboardBuilder.build();
                messageUpdater.updateMessage(chatId, messageId, message, keyboard);
            }
            case "menu_time_mark" -> handleTimeMark(user, chatId, messageId);
            case "menu_time_slots" -> handleTimeSlots(user, chatId, messageId);
        }
    }
    
    private void handleTimeMark(User user, String chatId, Integer messageId) {
        // Проверяем наличие часового пояса
        if (user.getTimezone() == null || user.getTimezone().trim().isEmpty()) {
            String errorMessage = """
                ❌ <b>Часовой пояс не установлен</b>
                
                Для разметки времени необходимо установить часовой пояс.
                
                Вы можете установить часовой пояс:
                • Через меню: Настройки → Часовой пояс
                • В настройках профиля на веб-сайте
                
                После установки часового пояса вы сможете использовать разметку времени.""";
            var keyboard = keyboardBuilder.build();
            messageUpdater.updateMessage(chatId, messageId, errorMessage, keyboard);
            return;
        }
        
        // Инициализируем состояние разметки времени
        stateManager.setState(chatId, TimeSlotMarkingStateManager.TimeSlotMarkingState.WAITING_DATE);
        stateManager.setData(chatId, new TimeSlotMarkingStateManager.TimeSlotMarkingData());
        
        String message = """
            📅 <b>Разметка свободного времени</b>
            
            Введите дату в формате ДД.ММ.ГГГГ (например: 15.01.2025)
            Или используйте: сегодня, завтра, послезавтра
            
            💡 Используйте /cancel для отмены.""";
        
        // Отправляем новое сообщение для диалога разметки
        messageSender.sendPersonalMessage(chatId, message);
        
        // Возвращаемся в подменю разметки времени
        String menuMessage = "📅 <b>Разметка времени</b>\n\nВыберите действие:";
        var keyboard = keyboardBuilder.build();
        messageUpdater.updateMessage(chatId, messageId, menuMessage, keyboard);
    }
    
    private void handleTimeSlots(User user, String chatId, Integer messageId) {
        try {
            // Получаем текущего пользователя как PlayerDto с временными слотами
            Instant now = Instant.now();
            Instant endDate = now.plusSeconds(30L * 24 * 60 * 60); // 30 дней вперед
            var player = userService.getUserAsPlayerWithTimeSlots(user, now, endDate);
            
            var slots = player.getAvailableTimes();
            
            String message;
            if (slots == null || slots.isEmpty()) {
                message = "📅 <b>Мои временные слоты</b>\n\nУ вас пока нет размеченного времени.";
            } else {
                message = messageBuilder.buildMySlotsListMessage(slots, user.getTimezone());
            }
            
            var keyboard = keyboardBuilder.build();
            messageUpdater.updateMessage(chatId, messageId, message, keyboard);
        } catch (Exception e) {
            messageUpdater.answerCallback("", "❌ Ошибка при получении списка временных слотов.");
        }
    }
}
