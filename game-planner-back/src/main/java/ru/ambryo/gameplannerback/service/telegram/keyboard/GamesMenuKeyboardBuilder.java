package ru.ambryo.gameplannerback.service.telegram.keyboard;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.ambryo.gameplannerback.dto.GameDto;
import ru.ambryo.gameplannerback.entity.User;
import ru.ambryo.gameplannerback.service.telegram.util.TelegramTimeFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Билдер клавиатур для меню игр
 */
@Component
public class GamesMenuKeyboardBuilder {
    
    private static final int GAMES_PER_PAGE = 5;
    private final TelegramTimeFormatter timeFormatter;
    
    public GamesMenuKeyboardBuilder(TelegramTimeFormatter timeFormatter) {
        this.timeFormatter = timeFormatter;
    }
    
    public InlineKeyboardMarkup buildGamesMenu() {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        
        rows.add(createButtonRow("📋 Список предстоящих игр", "menu_games_list"));
        rows.add(createButtonRow("◀️ Назад", "menu_main"));
        
        keyboard.setKeyboard(rows);
        return keyboard;
    }
    
    public InlineKeyboardMarkup buildGamesList(List<GameDto> games, int page, int totalPages) {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        
        int startIndex = page * GAMES_PER_PAGE;
        int endIndex = Math.min(startIndex + GAMES_PER_PAGE, games.size());
        List<GameDto> pageGames = games.subList(startIndex, endIndex);
        
        // Кнопки для каждой игры
        for (int i = 0; i < pageGames.size(); i++) {
            GameDto game = pageGames.get(i);
            int globalIndex = startIndex + i;
            
            String buttonText = (globalIndex + 1) + ". ";
            if (game.getTitle() != null && !game.getTitle().isEmpty()) {
                String title = game.getTitle();
                if (title.length() > 30) {
                    title = title.substring(0, 27) + "...";
                }
                buttonText += title;
            } else {
                buttonText += "Игра";
            }
            buttonText += " - " + timeFormatter.formatInstant(game.getStartTime());
            
            rows.add(createButtonRow(buttonText, "view_game_" + game.getId()));
        }
        
        // Пагинация
        if (totalPages > 1) {
            List<InlineKeyboardButton> paginationRow = new ArrayList<>();
            
            if (page > 0) {
                InlineKeyboardButton prevButton = new InlineKeyboardButton();
                prevButton.setText("◀️ Предыдущая");
                prevButton.setCallbackData("menu_games_page_" + (page - 1));
                paginationRow.add(prevButton);
            }
            
            InlineKeyboardButton pageButton = new InlineKeyboardButton();
            pageButton.setText((page + 1) + "/" + totalPages);
            pageButton.setCallbackData("menu_games_page_separator");
            paginationRow.add(pageButton);
            
            if (page < totalPages - 1) {
                InlineKeyboardButton nextButton = new InlineKeyboardButton();
                nextButton.setText("Следующая ▶️");
                nextButton.setCallbackData("menu_games_page_" + (page + 1));
                paginationRow.add(nextButton);
            }
            
            rows.add(paginationRow);
        }
        
        rows.add(createButtonRow("◀️ Назад", "menu_games"));
        
        keyboard.setKeyboard(rows);
        return keyboard;
    }
    
    public InlineKeyboardMarkup buildGameKeyboard(GameDto game, User user) {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        
        boolean isParticipant = game.getParticipants() != null 
            && game.getParticipants().stream().anyMatch(p -> p.getId().equals(user.getId()));
        boolean isCreator = game.getCreatorId().equals(user.getId());
        
        long participantCount = game.getParticipants() != null 
            ? game.getParticipants().stream()
                .filter(p -> !p.getId().equals(game.getCreatorId()))
                .count()
            : 0;
        
        Integer maxParticipants = game.getMaxParticipants();
        boolean isFull = maxParticipants != null && participantCount >= maxParticipants;
        
        List<InlineKeyboardButton> buttonRow = new ArrayList<>();
        
        if (isCreator) {
            InlineKeyboardButton viewButton = new InlineKeyboardButton();
            viewButton.setText("👁️ Просмотр (вы организатор)");
            viewButton.setCallbackData("refresh_game_" + game.getId());
            buttonRow.add(viewButton);
        } else if (isParticipant) {
            InlineKeyboardButton leaveButton = new InlineKeyboardButton();
            leaveButton.setText("❌ Покинуть игру");
            leaveButton.setCallbackData("leave_game_" + game.getId());
            buttonRow.add(leaveButton);
        } else {
            InlineKeyboardButton joinButton = new InlineKeyboardButton();
            if (isFull) {
                joinButton.setText("🔒 Игра заполнена");
                joinButton.setCallbackData("refresh_game_" + game.getId());
            } else {
                joinButton.setText("✅ Записаться на игру");
                joinButton.setCallbackData("join_game_" + game.getId());
            }
            buttonRow.add(joinButton);
        }
        
        rows.add(buttonRow);
        rows.add(createButtonRow("🔄 Обновить", "refresh_game_" + game.getId()));
        
        keyboard.setKeyboard(rows);
        return keyboard;
    }
    
    public InlineKeyboardMarkup buildGameKeyboardWithBack(GameDto game, User user, String chatId, Map<String, Integer> gamesListPage) {
        InlineKeyboardMarkup keyboard = buildGameKeyboard(game, user);
        
        // Добавляем кнопку "Назад к списку"
        List<List<InlineKeyboardButton>> rows = keyboard.getKeyboard();
        int currentPage = gamesListPage.getOrDefault(chatId, 0);
        rows.add(createButtonRow("◀️ Назад к списку", "menu_games_page_" + currentPage));
        
        return keyboard;
    }
    
    private List<InlineKeyboardButton> createButtonRow(String text, String callbackData) {
        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(callbackData);
        row.add(button);
        return row;
    }
}

