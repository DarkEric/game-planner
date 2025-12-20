package ru.ambryo.gameplannerback.service.telegram.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.ambryo.gameplannerback.entity.User;
import ru.ambryo.gameplannerback.repository.UserRepository;
import ru.ambryo.gameplannerback.service.telegram.keyboard.MainMenuKeyboardBuilder;
import ru.ambryo.gameplannerback.service.telegram.util.TelegramHtmlFormatter;
import ru.ambryo.gameplannerback.service.telegram.util.TelegramMessageSender;

/**
 * Обработчик команды /menu
 */
@Component
public class MenuCommandHandler implements CommandHandler {
    
    private final UserRepository userRepository;
    private final MainMenuKeyboardBuilder keyboardBuilder;
    private final TelegramMessageSender messageSender;
    private final AbsSender bot;
    
    @Autowired
    public MenuCommandHandler(
            UserRepository userRepository,
            MainMenuKeyboardBuilder keyboardBuilder,
            AbsSender bot) {
        this.userRepository = userRepository;
        this.keyboardBuilder = keyboardBuilder;
        this.bot = bot;
        this.messageSender = new TelegramMessageSender(bot);
    }
    
    @Override
    public boolean canHandle(String command) {
        return "menu".equals(command);
    }
    
    @Override
    public void handle(Message message, Long telegramUserId, String chatId) {
        try {
            User user = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
            boolean isLinked = user != null;
            
            String menuMessage = "📱 <b>Главное меню</b>\n\n";
            if (isLinked) {
                menuMessage += "✅ Аккаунт связан\n";
                menuMessage += "👤 Пользователь: " + TelegramHtmlFormatter.escapeHtml(user.getUsername()) + "\n\n";
                menuMessage += "Выберите раздел:";
            } else {
                menuMessage += "❌ Аккаунт не связан\n\n";
                menuMessage += "Для доступа ко всем функциям необходимо связать аккаунт.\n\n";
                menuMessage += "Выберите способ связывания:";
            }
            
            var keyboard = keyboardBuilder.build(isLinked);
            
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText(menuMessage);
            sendMessage.setParseMode("HTML");
            sendMessage.setReplyMarkup(keyboard);
            
            bot.execute(sendMessage);
        } catch (TelegramApiException e) {
            messageSender.sendPersonalMessage(chatId, "❌ Произошла ошибка при открытии меню. Попробуйте позже.");
        }
    }
}

