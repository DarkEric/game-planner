package ru.ambryo.gameplannerback.service.telegram.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;
import ru.ambryo.gameplannerback.entity.User;
import ru.ambryo.gameplannerback.repository.UserRepository;
import ru.ambryo.gameplannerback.service.telegram.state.TimeSlotMarkingStateManager;
import ru.ambryo.gameplannerback.service.telegram.util.TelegramMessageSender;

/**
 * Обработчик команды /mark
 */
@Component
public class MarkCommandHandler implements CommandHandler {
    
    private final UserRepository userRepository;
    private final TimeSlotMarkingStateManager stateManager;
    private final TelegramMessageSender messageSender;
    
    @Autowired
    public MarkCommandHandler(
            UserRepository userRepository,
            TimeSlotMarkingStateManager stateManager,
            @Lazy AbsSender bot) {
        this.userRepository = userRepository;
        this.stateManager = stateManager;
        this.messageSender = new TelegramMessageSender(bot);
    }
    
    @Override
    public boolean canHandle(String command) {
        return "mark".equals(command);
    }
    
    @Override
    public void handle(Message message, Long telegramUserId, String chatId) {
        try {
            User user = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
            
            if (user == null) {
                messageSender.sendPersonalMessage(chatId, """
                    ❌ Ваш аккаунт не связан с веб-сайтом.
                    
                    Используйте /register для регистрации или /auth для привязки существующего аккаунта.""");
                return;
            }
            
            // Проверяем наличие часового пояса
            if (user.getTimezone() == null || user.getTimezone().trim().isEmpty()) {
                messageSender.sendPersonalMessage(chatId, """
                    ❌ <b>Часовой пояс не установлен</b>
                    
                    Для разметки времени необходимо установить часовой пояс.
                    
                    Вы можете установить часовой пояс:
                    • Через меню: /menu → Настройки → Часовой пояс
                    • В настройках профиля на веб-сайте
                    
                    После установки часового пояса вы сможете использовать команду /mark для разметки времени.""");
                return;
            }
            
            stateManager.setData(chatId, new TimeSlotMarkingStateManager.TimeSlotMarkingData());
            stateManager.setState(chatId, TimeSlotMarkingStateManager.TimeSlotMarkingState.WAITING_CLEAR_BEFORE_ADD);

            messageSender.sendPersonalMessage(chatId, """
                📅 <b>Разметка свободного времени</b>
                
                Удалить <b>все</b> другие размеченные слоты перед добавлением нового интервала?
                Ответьте: <b>да</b> или <b>нет</b> (также: д/н, yes/no).
                
                💡 Используйте /cancel для отмены.""");
        } catch (Exception e) {
            stateManager.clearState(chatId);
            messageSender.sendPersonalMessage(chatId, "❌ Произошла ошибка при инициализации разметки времени. Попробуйте позже.");
        }
    }
}

