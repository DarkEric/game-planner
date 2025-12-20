package ru.ambryo.gameplannerback.service.telegram.menu;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.ambryo.gameplannerback.entity.User;
import ru.ambryo.gameplannerback.repository.UserRepository;
import ru.ambryo.gameplannerback.service.GameService;
import ru.ambryo.gameplannerback.service.telegram.keyboard.GamesMenuKeyboardBuilder;
import ru.ambryo.gameplannerback.service.telegram.message.GameMessageBuilder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Обработчик действий с играми (join, leave, refresh)
 */
@Component
public class GameActionHandler implements MenuHandler {
    
    private final UserRepository userRepository;
    private final GameService gameService;
    private final GameMessageBuilder messageBuilder;
    private final GamesMenuKeyboardBuilder keyboardBuilder;
    private final MenuMessageUpdater messageUpdater;
    
    // Хранение текущей страницы для каждого чата (для определения, нужно ли показывать кнопку "Назад")
    private final Map<String, Integer> gamesListPage = new ConcurrentHashMap<>();
    
    @Autowired
    public GameActionHandler(
            UserRepository userRepository,
            GameService gameService,
            GameMessageBuilder messageBuilder,
            GamesMenuKeyboardBuilder keyboardBuilder,
            MenuMessageUpdater messageUpdater) {
        this.userRepository = userRepository;
        this.gameService = gameService;
        this.messageBuilder = messageBuilder;
        this.keyboardBuilder = keyboardBuilder;
        this.messageUpdater = messageUpdater;
    }
    
    @Override
    public boolean canHandle(String callbackData) {
        return callbackData.startsWith("join_game_") ||
               callbackData.startsWith("leave_game_") ||
               callbackData.startsWith("refresh_game_");
    }
    
    @Override
    public void handle(CallbackQuery callbackQuery, Long telegramUserId, String chatId, Integer messageId) {
        String data = callbackQuery.getData();
        User user = userRepository.findByTelegramUserId(telegramUserId).orElse(null);
        
        if (user == null) {
            messageUpdater.answerCallback(callbackQuery.getId(), "❌ Ваш аккаунт не связан. Используйте /link для связывания.");
            return;
        }
        
        try {
            long gameId;
            if (data.startsWith("join_game_")) {
                gameId = Long.parseLong(data.substring("join_game_".length()));
                handleJoinGame(user, chatId, messageId, gameId, callbackQuery.getId());
            } else if (data.startsWith("leave_game_")) {
                gameId = Long.parseLong(data.substring("leave_game_".length()));
                handleLeaveGame(user, chatId, messageId, gameId, callbackQuery.getId());
            } else if (data.startsWith("refresh_game_")) {
                gameId = Long.parseLong(data.substring("refresh_game_".length()));
                handleRefreshGame(user, chatId, messageId, gameId);
            }
        } catch (Exception e) {
            messageUpdater.answerCallback(callbackQuery.getId(), "❌ Произошла ошибка.");
        }
    }
    
    private void handleJoinGame(User user, String chatId, Integer messageId, Long gameId, String callbackQueryId) {
        try {
            var game = gameService.joinGame(gameId, user);
            String message = messageBuilder.buildGameDetailsMessage(game, user);
            var keyboard = gamesListPage.containsKey(chatId) 
                ? keyboardBuilder.buildGameKeyboardWithBack(game, user, chatId, gamesListPage)
                : keyboardBuilder.buildGameKeyboard(game, user);
            
            messageUpdater.updateMessage(chatId, messageId, message, keyboard);
            messageUpdater.answerCallback(callbackQueryId, "✅ Вы записались на игру!");
        } catch (RuntimeException e) {
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.contains("already joined")) {
                messageUpdater.answerCallback(callbackQueryId, "ℹ️ Вы уже записаны на эту игру.");
            } else if (errorMsg != null && (errorMsg.contains("full") || errorMsg.contains("Maximum"))) {
                messageUpdater.answerCallback(callbackQueryId, "❌ Игра заполнена. Достигнуто максимальное количество участников.");
            } else {
                messageUpdater.answerCallback(callbackQueryId, "❌ Ошибка: " + errorMsg);
            }
        }
    }
    
    private void handleLeaveGame(User user, String chatId, Integer messageId, Long gameId, String callbackQueryId) {
        try {
            var game = gameService.leaveGame(gameId, user);
            String message = messageBuilder.buildGameDetailsMessage(game, user);
            var keyboard = gamesListPage.containsKey(chatId)
                ? keyboardBuilder.buildGameKeyboardWithBack(game, user, chatId, gamesListPage)
                : keyboardBuilder.buildGameKeyboard(game, user);
            
            messageUpdater.updateMessage(chatId, messageId, message, keyboard);
            messageUpdater.answerCallback(callbackQueryId, "✅ Вы покинули игру.");
        } catch (RuntimeException e) {
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.contains("Creator cannot leave")) {
                messageUpdater.answerCallback(callbackQueryId, "ℹ️ Организатор не может покинуть игру. Используйте удаление игры.");
            } else {
                messageUpdater.answerCallback(callbackQueryId, "❌ Ошибка: " + errorMsg);
            }
        }
    }
    
    private void handleRefreshGame(User user, String chatId, Integer messageId, Long gameId) {
        try {
            var game = gameService.getGameById(gameId);
            String message = messageBuilder.buildGameDetailsMessage(game, user);
            var keyboard = gamesListPage.containsKey(chatId)
                ? keyboardBuilder.buildGameKeyboardWithBack(game, user, chatId, gamesListPage)
                : keyboardBuilder.buildGameKeyboard(game, user);
            
            messageUpdater.updateMessage(chatId, messageId, message, keyboard);
        } catch (Exception e) {
            messageUpdater.answerCallback("", "❌ Произошла ошибка при обновлении информации.");
        }
    }
}

