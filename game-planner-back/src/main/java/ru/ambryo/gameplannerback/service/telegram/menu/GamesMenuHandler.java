package ru.ambryo.gameplannerback.service.telegram.menu;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.ambryo.gameplannerback.dto.GameDto;
import ru.ambryo.gameplannerback.entity.User;
import ru.ambryo.gameplannerback.repository.UserRepository;
import ru.ambryo.gameplannerback.service.GameService;
import ru.ambryo.gameplannerback.service.telegram.config.TelegramBotProperties;
import ru.ambryo.gameplannerback.service.telegram.keyboard.GamesMenuKeyboardBuilder;
import ru.ambryo.gameplannerback.service.telegram.message.GameMessageBuilder;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Обработчик меню игр
 */
@Component
public class GamesMenuHandler implements MenuHandler {
    
    private final UserRepository userRepository;
    private final GameService gameService;
    private final GameMessageBuilder messageBuilder;
    private final GamesMenuKeyboardBuilder keyboardBuilder;
    private final MenuMessageUpdater messageUpdater;
    private final int gamesPerPage;

    @Autowired
    public GamesMenuHandler(
            UserRepository userRepository,
            GameService gameService,
            GameMessageBuilder messageBuilder,
            GamesMenuKeyboardBuilder keyboardBuilder,
            TelegramBotProperties properties,
            MenuMessageUpdater messageUpdater) {
        this.userRepository = userRepository;
        this.gameService = gameService;
        this.messageBuilder = messageBuilder;
        this.keyboardBuilder = keyboardBuilder;
        this.messageUpdater = messageUpdater;
        this.gamesPerPage = properties.getGamesPerPage();
    }
    
    @Override
    public boolean canHandle(String callbackData) {
        return callbackData.equals("menu_games") ||
               callbackData.equals("menu_games_list") ||
               callbackData.startsWith("menu_games_page_") ||
               callbackData.startsWith("view_game_");
    }
    
    @Override
    public void handle(CallbackQuery callbackQuery, Long telegramUserId, String chatId, Integer messageId) {
        String data = callbackQuery.getData();
        User user = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
        
        if (user == null) {
            messageUpdater.answerCallback(callbackQuery.getId(), "❌ Аккаунт не связан. Зарегистрируйтесь для доступа к функциям.");
            return;
        }
        
        if (data.equals("menu_games")) {
            String message = "🎮 <b>Игры</b>\n\nВыберите действие:";
            var keyboard = keyboardBuilder.buildGamesMenu();
            messageUpdater.updateMessage(chatId, messageId, message, keyboard);
            
        } else if (data.equals("menu_games_list")) {
            handleGamesList(telegramUserId, chatId, messageId, 0);
            
        } else if (data.startsWith("menu_games_page_")) {
            if (data.equals("menu_games_page_separator")) {
                // Игнорируем нажатие на индикатор страницы
                return;
            }
            int page = Integer.parseInt(data.substring("menu_games_page_".length()));
            handleGamesList(telegramUserId, chatId, messageId, page);
            
        } else if (data.startsWith("view_game_")) {
            Long gameId = Long.parseLong(data.substring("view_game_".length()));
            handleViewGame(telegramUserId, chatId, messageId, gameId);
        }
    }
    
    private void handleGamesList(Long telegramUserId, String chatId, Integer messageId, int page) {
        try {
            User user = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
            if (user == null) {
                messageUpdater.answerCallback("", "❌ Ваш аккаунт не связан. Используйте /link для связывания.");
                return;
            }
            
            List<GameDto> upcomingGames = gameService.getUpcomingGamesForUser(user.getId());
            
            String message;
            var keyboard = keyboardBuilder.buildGamesMenu();
            
            if (upcomingGames.isEmpty()) {
                message = "📅 <b>Предстоящие игры</b>\n\nУ вас пока нет запланированных игр.";
            } else {
                List<GameDto> sortedGames = upcomingGames.stream()
                    .sorted(Comparator.comparing(GameDto::getStartTime))
                    .collect(Collectors.toList());
                
                int totalPages = (int) Math.ceil((double) sortedGames.size() / gamesPerPage);
                if (page < 0) page = 0;
                if (page >= totalPages) page = totalPages - 1;
                
                message = messageBuilder.buildUpcomingGamesListMessage(sortedGames, page, totalPages);
                keyboard = keyboardBuilder.buildGamesList(sortedGames, page, totalPages);
            }
            
            messageUpdater.updateMessage(chatId, messageId, message, keyboard);
        } catch (Exception e) {
            messageUpdater.answerCallback("", "❌ Ошибка при получении списка игр.");
        }
    }
    
    private void handleViewGame(Long telegramUserId, String chatId, Integer messageId, Long gameId) {
        try {
            User user = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
            if (user == null) {
                messageUpdater.answerCallback("", "❌ Ваш аккаунт не связан. Используйте /link для связывания.");
                return;
            }
            
            var game = gameService.getGameById(gameId);
            String message = messageBuilder.buildGameDetailsMessage(game, user);
            var keyboard = keyboardBuilder.buildGameKeyboard(game, user);
            
            messageUpdater.updateMessage(chatId, messageId, message, keyboard);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                messageUpdater.answerCallback("", "❌ Игра не найдена.");
            } else {
                messageUpdater.answerCallback("", "❌ Ошибка: " + e.getMessage());
            }
        } catch (Exception e) {
            messageUpdater.answerCallback("", "❌ Произошла ошибка при получении информации об игре.");
        }
    }
}

