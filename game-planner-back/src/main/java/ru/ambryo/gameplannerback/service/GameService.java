package ru.ambryo.gameplannerback.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ambryo.gameplannerback.dto.CreateGameRequest;
import ru.ambryo.gameplannerback.dto.GameDto;
import ru.ambryo.gameplannerback.entity.Game;
import ru.ambryo.gameplannerback.entity.User;
import ru.ambryo.gameplannerback.repository.GameRepository;
import ru.ambryo.gameplannerback.repository.UserRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GameService {
    
    @Autowired
    private GameRepository gameRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserService userService;
    
    @Transactional
    public GameDto createGame(CreateGameRequest request, User creator) {
        Game game = new Game(
            request.getStartTime(), 
            request.getEndTime(), 
            request.getTitle(),
            request.getDescription(),
            creator
        );
        
        // Создаем список участников
        List<User> participants = new ArrayList<>();
        
        // Всегда добавляем создателя в участники
        User managedCreator = userRepository.findById(creator.getId())
                .orElseThrow(() -> new RuntimeException("Creator not found"));
        participants.add(managedCreator);
        
        // Добавляем других участников (только тех, кто доступен на это время)
        if (request.getParticipantIds() != null && !request.getParticipantIds().isEmpty()) {
            List<User> otherParticipants = userRepository.findAllById(request.getParticipantIds());
            // Исключаем создателя, если он уже в списке
            otherParticipants.stream()
                    .filter(user -> !user.getId().equals(creator.getId()))
                    .forEach(participants::add);
        }
        
        game.setParticipants(participants);
        game = gameRepository.save(game);
        return convertToDto(game);
    }
    
    @Transactional(readOnly = true)
    public List<GameDto> getGamesBetween(Instant startDate, Instant endDate) {
        return gameRepository.findGamesBetween(startDate, endDate)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<GameDto> getUpcomingGamesForUser(Long userId) {
        return gameRepository.findUpcomingGamesByUser(userId, Instant.now())
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public void deleteGame(Long gameId, User user) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game not found"));
        
        if (!game.getCreator().getId().equals(user.getId())) {
            throw new RuntimeException("Only creator can delete the game");
        }
        
        gameRepository.delete(game);
    }
    
    @Transactional
    public GameDto joinGame(Long gameId, User user) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game not found"));
        
        User managedUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Проверяем, не записан ли уже пользователь
        if (game.getParticipants().stream().anyMatch(p -> p.getId().equals(user.getId()))) {
            throw new RuntimeException("User already joined this game");
        }
        
        // Добавляем пользователя в участники
        game.getParticipants().add(managedUser);
        
        // Автоматически проставляем доступность на время игры
        Instant gameStart = game.getStartTime();
        Instant gameEnd = game.getEndTime();
        
        // Вычисляем количество часов
        long durationHours = (gameEnd.getEpochSecond() - gameStart.getEpochSecond()) / 3600;
        
        // Проставляем слоты по часам
        for (long i = 0; i < durationHours; i++) {
            Instant slotStart = Instant.ofEpochSecond(gameStart.getEpochSecond() + i * 3600);
            
            // Проверяем, нет ли уже такого слота
            boolean slotExists = managedUser.getAvailableTimes().stream()
                    .anyMatch(ts -> {
                        long startEpochHour = slotStart.getEpochSecond() / 3600;
                        long slotStartEpochHour = ts.getStart().getEpochSecond() / 3600;
                        return startEpochHour == slotStartEpochHour;
                    });
            
            if (!slotExists) {
                userService.toggleTimeSlot(managedUser, slotStart, 1);
            }
        }
        
        game = gameRepository.save(game);
        return convertToDto(game);
    }
    
    @Transactional
    public GameDto leaveGame(Long gameId, User user) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game not found"));
        
        // Создатель не может покинуть игру, только удалить её
        if (game.getCreator().getId().equals(user.getId())) {
            throw new RuntimeException("Creator cannot leave the game, delete it instead");
        }
        
        User managedUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Автоматически удаляем доступность на время игры
        Instant gameStart = game.getStartTime();
        Instant gameEnd = game.getEndTime();
        
        // Вычисляем количество часов
        long durationHours = (gameEnd.getEpochSecond() - gameStart.getEpochSecond()) / 3600;
        
        // Удаляем слоты по часам
        for (long i = 0; i < durationHours; i++) {
            Instant slotStart = Instant.ofEpochSecond(gameStart.getEpochSecond() + i * 3600);
            
            // Проверяем, есть ли такой слот
            boolean slotExists = managedUser.getAvailableTimes().stream()
                    .anyMatch(ts -> {
                        long startEpochHour = slotStart.getEpochSecond() / 3600;
                        long slotStartEpochHour = ts.getStart().getEpochSecond() / 3600;
                        return startEpochHour == slotStartEpochHour;
                    });
            
            if (slotExists) {
                // Удаляем слот (toggleTimeSlot удалит существующий слот)
                userService.toggleTimeSlot(managedUser, slotStart, 1);
            }
        }
        
        // Удаляем пользователя из участников
        game.getParticipants().removeIf(p -> p.getId().equals(user.getId()));
        
        game = gameRepository.save(game);
        return convertToDto(game);
    }
    
    private GameDto convertToDto(Game game) {
        List<GameDto.ParticipantDto> participants = game.getParticipants().stream()
                .map(user -> new GameDto.ParticipantDto(user.getId(), user.getName(), user.getColor()))
                .collect(Collectors.toList());
        
        return new GameDto(
                game.getId(),
                game.getStartTime(),
                game.getEndTime(),
                game.getCreator().getId(),
                game.getCreator().getName(),
                game.getTitle(),
                game.getDescription(),
                participants,
                game.getCreatedAt()
        );
    }
}
