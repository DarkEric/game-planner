package ru.ambryo.gameplannerback.service.telegram.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.ambryo.gameplannerback.entity.User;
import ru.ambryo.gameplannerback.repository.UserRepository;
import ru.ambryo.gameplannerback.service.GameService;
import ru.ambryo.gameplannerback.service.telegram.keyboard.GamesMenuKeyboardBuilder;
import ru.ambryo.gameplannerback.service.telegram.message.GameMessageBuilder;
import ru.ambryo.gameplannerback.service.telegram.util.TelegramMessageSender;

/**
 * Обработчик команды /game
 */
@Component
public class GameCommandHandler implements CommandHandler {
    
    private final UserRepository userRepository;
    private final GameService gameService;
    private final GameMessageBuilder messageBuilder;
    private final GamesMenuKeyboardBuilder keyboardBuilder;
    private final TelegramMessageSender messageSender;
    private final AbsSender bot;
    
    @Autowired
    public GameCommandHandler(
            UserRepository userRepository,
            GameService gameService,
            GameMessageBuilder messageBuilder,
            GamesMenuKeyboardBuilder keyboardBuilder,
            AbsSender bot) {
        this.userRepository = userRepository;
        this.gameService = gameService;
        this.messageBuilder = messageBuilder;
        this.keyboardBuilder = keyboardBuilder;
        this.bot = bot;
        this.messageSender = new TelegramMessageSender(bot);
    }
    
    @Override
    public boolean canHandle(String command) {
        return "game".equals(command);
    }
    
    @Override
    public void handle(Message message, Long telegramUserId, String chatId) {
        String text = message.getText();
        String[] parts = text.split("\\s+", 2);
        
        if (parts.length < 2) {
            messageSender.sendPersonalMessage(chatId, "Использование: /game <id>\n\nПолучите ID из списка игр командой /games");
            return;
        }
        
        try {
            Long gameId = Long.parseLong(parts[1]);
            handleGameDetails(telegramUserId, chatId, gameId);
        } catch (NumberFormatException e) {
            messageSender.sendPersonalMessage(chatId, "❌ Неверный формат ID игры.\n\nИспользование: /game <id>\n\nПолучите ID из списка игр командой /games");
        }
    }
    
    private void handleGameDetails(Long telegramUserId, String chatId, Long gameId) {
        try {
            User user = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
            
            if (user == null) {
                messageSender.sendPersonalMessage(chatId, "❌ Ваш аккаунт не связан с веб-сайтом.\n\nИспользуйте /link <token> для связывания аккаунта.");
                return;
            }
            
            var game = gameService.getGameById(gameId);
            String messageText = messageBuilder.buildGameDetailsMessage(game, user);
            var keyboard = keyboardBuilder.buildGameKeyboard(game, user);
            
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText(messageText);
            sendMessage.setParseMode("HTML");
            sendMessage.setReplyMarkup(keyboard);
            
            bot.execute(sendMessage);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                messageSender.sendPersonalMessage(chatId, "❌ Игра не найдена.");
            } else {
                messageSender.sendPersonalMessage(chatId, "❌ Ошибка: " + e.getMessage());
            }
        } catch (TelegramApiException e) {
            messageSender.sendPersonalMessage(chatId, "❌ Произошла ошибка при получении информации об игре.");
        }
    }
}

