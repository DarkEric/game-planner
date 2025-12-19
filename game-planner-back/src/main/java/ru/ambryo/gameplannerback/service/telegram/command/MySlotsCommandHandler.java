package ru.ambryo.gameplannerback.service.telegram.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;
import ru.ambryo.gameplannerback.entity.User;
import ru.ambryo.gameplannerback.repository.UserRepository;
import ru.ambryo.gameplannerback.service.UserService;
import ru.ambryo.gameplannerback.service.telegram.message.TimeSlotMessageBuilder;
import ru.ambryo.gameplannerback.service.telegram.util.TelegramMessageSender;

import java.time.Instant;

/**
 * Обработчик команды /myslots
 */
@Component
public class MySlotsCommandHandler implements CommandHandler {
    
    private final UserRepository userRepository;
    private final UserService userService;
    private final TimeSlotMessageBuilder messageBuilder;
    private final TelegramMessageSender messageSender;
    
    @Autowired
    public MySlotsCommandHandler(
            UserRepository userRepository,
            UserService userService,
            TimeSlotMessageBuilder messageBuilder,
            AbsSender bot) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.messageBuilder = messageBuilder;
        this.messageSender = new TelegramMessageSender(bot);
    }
    
    @Override
    public boolean canHandle(String command) {
        return "myslots".equals(command);
    }
    
    @Override
    public void handle(Message message, Long telegramUserId, String chatId) {
        try {
            User user = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
            
            if (user == null) {
                messageSender.sendPersonalMessage(chatId, "❌ Ваш аккаунт не связан с веб-сайтом.\n\n" +
                        "Используйте /register для регистрации или /auth для привязки существующего аккаунта.");
                return;
            }
            
            // Получаем текущего пользователя как PlayerDto с временными слотами
            Instant now = Instant.now();
            Instant endDate = now.plusSeconds(30L * 24 * 60 * 60); // 30 дней вперед
            var player = userService.getUserAsPlayerWithTimeSlots(user, now, endDate);
            
            var slots = player.getAvailableTimes();
            
            if (slots == null || slots.isEmpty()) {
                messageSender.sendPersonalMessage(chatId, "📅 <b>Мои временные слоты</b>\n\n" +
                        "У вас пока нет размеченного времени.\n\n" +
                        "Используйте /mark для разметки свободного времени.");
                return;
            }
            
            String messageText = messageBuilder.buildMySlotsListMessage(slots, user.getTimezone());
            messageSender.sendPersonalMessage(chatId, messageText);
        } catch (Exception e) {
            messageSender.sendPersonalMessage(chatId, "❌ Ошибка при получении списка временных слотов. Попробуйте позже.");
        }
    }
}

