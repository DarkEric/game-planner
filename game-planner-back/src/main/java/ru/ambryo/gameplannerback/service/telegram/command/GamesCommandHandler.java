package ru.ambryo.gameplannerback.service.telegram.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;
import ru.ambryo.gameplannerback.dto.GameDto;
import ru.ambryo.gameplannerback.entity.User;
import ru.ambryo.gameplannerback.repository.UserRepository;
import ru.ambryo.gameplannerback.service.GameService;
import ru.ambryo.gameplannerback.service.telegram.config.TelegramBotProperties;
import ru.ambryo.gameplannerback.service.telegram.message.GameMessageBuilder;
import ru.ambryo.gameplannerback.service.telegram.util.TelegramMessageSender;

import java.util.List;

/**
 * Обработчик команды /games и /upcoming
 */
@Component
public class GamesCommandHandler implements CommandHandler {
    
    private final UserRepository userRepository;
    private final GameService gameService;
    private final GameMessageBuilder messageBuilder;
    private final TelegramMessageSender messageSender;
    private final int gamesPerPage;
    
    @Autowired
    public GamesCommandHandler(
            UserRepository userRepository,
            GameService gameService,
            GameMessageBuilder messageBuilder,
            TelegramBotProperties properties,
            AbsSender bot) {
        this.userRepository = userRepository;
        this.gameService = gameService;
        this.messageBuilder = messageBuilder;
        this.messageSender = new TelegramMessageSender(bot);
        this.gamesPerPage = properties.getGamesPerPage();
    }
    
    @Override
    public boolean canHandle(String command) {
        return "games".equals(command) || "upcoming".equals(command);
    }
    
    @Override
    public void handle(Message message, Long telegramUserId, String chatId) {
        try {
            User user = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
            
            if (user == null) {
                messageSender.sendPersonalMessage(chatId, "❌ Ваш аккаунт не связан с веб-сайтом.\n\nИспользуйте /link <token> для связывания аккаунта.");
                return;
            }
            
            List<GameDto> upcomingGames = gameService.getUpcomingGamesForUser(user.getId());
            
            if (upcomingGames.isEmpty()) {
                messageSender.sendPersonalMessage(chatId, "📅 <b>Предстоящие игры</b>\n\nУ вас пока нет запланированных игр.");
                return;
            }
            
            int totalPages = (int) Math.ceil((double) upcomingGames.size() / gamesPerPage);
            String messageText = messageBuilder.buildUpcomingGamesListMessage(upcomingGames, 0, totalPages);
            messageSender.sendPersonalMessage(chatId, messageText);
        } catch (Exception e) {
            messageSender.sendPersonalMessage(chatId, "❌ Ошибка при получении списка игр. Попробуйте позже.");
        }
    }
}

