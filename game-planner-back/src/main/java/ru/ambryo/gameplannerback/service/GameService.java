package ru.ambryo.gameplannerback.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ambryo.gameplannerback.dto.CreateGameRequest;
import ru.ambryo.gameplannerback.dto.GameDto;
import ru.ambryo.gameplannerback.entity.Campaign;
import ru.ambryo.gameplannerback.entity.Game;
import ru.ambryo.gameplannerback.entity.GameNotification;
import ru.ambryo.gameplannerback.entity.User;
import ru.ambryo.gameplannerback.entity.UserNotificationSettings;
import ru.ambryo.gameplannerback.repository.CampaignRepository;
import ru.ambryo.gameplannerback.repository.GameNotificationRepository;
import ru.ambryo.gameplannerback.repository.GameRepository;
import ru.ambryo.gameplannerback.repository.UserNotificationSettingsRepository;
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
    
    @Autowired
    @Lazy
    private TelegramNotificationService telegramNotificationService;
    
    @Autowired
    private UserNotificationSettingsRepository settingsRepository;
    
    @Autowired
    private GameNotificationRepository gameNotificationRepository;
    
    @Autowired
    private CampaignRepository campaignRepository;
    
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
        
        // Добавляем других участников только если autoAddPlayers == true
        // По умолчанию (если поле не передано) autoAddPlayers == null, что считается как false
        Boolean autoAddPlayers = request.getAutoAddPlayers();
        if (Boolean.TRUE.equals(autoAddPlayers) && 
            request.getParticipantIds() != null && !request.getParticipantIds().isEmpty()) {
            List<User> otherParticipants = userRepository.findAllById(request.getParticipantIds());
            // Исключаем создателя, если он уже в списке
            otherParticipants.stream()
                    .filter(user -> !user.getId().equals(creator.getId()))
                    .forEach(participants::add);
        }
        
        game.setParticipants(participants);
        game.setMaxParticipants(request.getMaxParticipants());
        
        // Устанавливаем кампанию, если указана
        if (request.getCampaignId() != null) {
            Campaign campaign = campaignRepository.findById(request.getCampaignId())
                    .orElseThrow(() -> new RuntimeException("Campaign not found"));
            
            // Проверяем, что создатель игры является создателем кампании
            if (!campaign.getCreator().getId().equals(creator.getId())) {
                throw new RuntimeException("Only campaign creator can create games for this campaign");
            }
            
            game.setCampaign(campaign);
        }
        
        game = gameRepository.save(game);
        
        GameDto gameDto = convertToDto(game);
        
        // Отправляем общее уведомление в Telegram
        try {
            telegramNotificationService.sendGameCreatedNotification(gameDto);
        } catch (Exception e) {
            // Логируем ошибку, но не прерываем создание игры
            System.err.println("Failed to send Telegram notification: " + e.getMessage());
        }
        
        // Отправляем персональные уведомления
        try {
            sendPersonalGameCreatedNotifications(gameDto, game);
        } catch (Exception e) {
            System.err.println("Failed to send personal notifications: " + e.getMessage());
        }
        
        return gameDto;
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
    
    @Transactional(readOnly = true)
    public List<GameDto> getAllGamesForUser(Long userId) {
        return gameRepository.findAllGamesByUser(userId)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<GameDto> getGamesCreatedByUser(Long userId) {
        return gameRepository.findGamesCreatedByUser(userId)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public GameDto getGameById(Long gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game not found"));
        return convertToDto(game);
    }
    
    @Transactional
    public void deleteGame(Long gameId, User user, String cancellationReason) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game not found"));
        
        if (!game.getCreator().getId().equals(user.getId())) {
            throw new RuntimeException("Only creator can delete the game");
        }
        
        // Сохраняем DTO до удаления для отправки уведомления
        GameDto gameDto = convertToDto(game);
        
        // Отправляем общее уведомление об отмене в Telegram
        try {
            telegramNotificationService.sendGameCancelledNotification(gameDto, cancellationReason);
        } catch (Exception e) {
            // Логируем ошибку, но не прерываем удаление игры
            System.err.println("Failed to send Telegram cancellation notification: " + e.getMessage());
        }
        
        // Отправляем персональные уведомления об отмене
        try {
            sendPersonalGameCancelledNotifications(gameDto, game);
        } catch (Exception e) {
            System.err.println("Failed to send personal cancellation notifications: " + e.getMessage());
        }
        
        // Удаляем игру после отправки всех уведомлений
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
        
        // Проверяем лимит участников (создатель не учитывается в лимите)
        Integer maxParticipants = game.getMaxParticipants();
        if (maxParticipants != null) {
            // Сохраняем ID создателя в final переменную для использования в лямбде
            final Long creatorId = game.getCreator().getId();
            // Количество участников без создателя
            long participantCount = game.getParticipants().stream()
                    .filter(p -> !p.getId().equals(creatorId))
                    .count();
            
            if (participantCount >= maxParticipants) {
                throw new RuntimeException("Game is full. Maximum number of participants reached");
            }
        }
        
        // Добавляем пользователя в участники
        game.getParticipants().add(managedUser);
        
        // Автоматически проставляем доступность на время игры
        // Сохраняем в final переменные для использования в цикле
        final Instant gameStart = game.getStartTime();
        final Instant gameEnd = game.getEndTime();
        
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
        GameDto gameDto = convertToDto(game);
        
        return gameDto;
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
        
        // Удаляем пользователя из участников
        game.getParticipants().removeIf(p -> p.getId().equals(user.getId()));
        
        game = gameRepository.save(game);
        return convertToDto(game);
    }
    
    @Transactional
    public GameDto removePlayerFromGame(Long gameId, Long playerId, User creator) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game not found"));
        
        // Проверяем, что пользователь является создателем игры
        if (!game.getCreator().getId().equals(creator.getId())) {
            throw new RuntimeException("Only creator can remove players from the game");
        }
        
        // Проверяем, что удаляемый игрок не является создателем
        if (game.getCreator().getId().equals(playerId)) {
            throw new RuntimeException("Cannot remove creator from the game");
        }
        
        // Проверяем, что игрок является участником игры
        boolean isParticipant = game.getParticipants().stream()
                .anyMatch(p -> p.getId().equals(playerId));
        if (!isParticipant) {
            throw new RuntimeException("Player is not a participant of this game");
        }
        
        // Сохраняем информацию об удаляемом игроке для уведомления
        User removedPlayer = userRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Player not found"));
        
        // Удаляем игрока из участников
        game.getParticipants().removeIf(p -> p.getId().equals(playerId));
        
        game = gameRepository.save(game);
        GameDto gameDto = convertToDto(game);
        
        // Отправляем уведомление удаленному игроку
        try {
            if (removedPlayer.getTelegramSubscribed() != null && removedPlayer.getTelegramSubscribed()) {
                UserNotificationSettings settings = settingsRepository.findByUserId(removedPlayer.getId()).orElse(null);
                if (settings != null && "ALL".equals(settings.getGameRemovedFromGame())) {
                    telegramNotificationService.sendPlayerRemovedFromGameNotification(gameDto, removedPlayer);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to send removed from game notification: " + e.getMessage());
        }
        
        return gameDto;
    }
    
    @Transactional
    public GameDto markGameAsHeld(Long gameId, String keyEvents, User user) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game not found"));

        if (!game.getCreator().getId().equals(user.getId())) {
            throw new RuntimeException("Only creator can mark the game as held");
        }

        game.setHeld(true);
        game.setKeyEvents(keyEvents);
        game = gameRepository.save(game);
        
        GameDto gameDto = convertToDto(game);
        
        // Отправляем общее уведомление в Telegram
        try {
            telegramNotificationService.sendGameHeldNotification(gameDto);
        } catch (Exception e) {
            System.err.println("Failed to send Telegram notification: " + e.getMessage());
        }
        
        // Отправляем персональные уведомления о проведении
        try {
            sendPersonalGameHeldNotifications(gameDto, game);
        } catch (Exception e) {
            System.err.println("Failed to send personal held notifications: " + e.getMessage());
        }
        
        return gameDto;
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
                game.getCreatedAt(),
                game.isHeld(),
                game.getKeyEvents(),
                game.getCampaign() != null ? game.getCampaign().getId() : null,
                game.getCampaign() != null ? game.getCampaign().getName() : null,
                game.getMaxParticipants()
        );
    }
    
    private void sendPersonalGameCreatedNotifications(GameDto gameDto, Game game) {
        List<User> allUsers = userRepository.findAll();
        
        for (User user : allUsers) {
            if (user.getTelegramSubscribed() == null || !user.getTelegramSubscribed()) {
                continue;
            }
            
            UserNotificationSettings settings = settingsRepository.findByUserId(user.getId()).orElse(null);
            if (settings == null) {
                continue;
            }
            
            String setting = settings.getGameCreated();
            boolean shouldNotify = false;
            
            if ("ALL".equals(setting)) {
                shouldNotify = true;
            } else if ("MY_GAMES".equals(setting)) {
                // Проверяем, является ли пользователь участником или создателем
                boolean isParticipant = game.getParticipants().stream()
                        .anyMatch(p -> p.getId().equals(user.getId()));
                boolean isCreator = game.getCreator().getId().equals(user.getId());
                shouldNotify = isParticipant || isCreator;
            }
            
            if (shouldNotify) {
                // Проверяем, не отправляли ли уже это уведомление
                GameNotification existing = gameNotificationRepository.findPersonalNotification(
                        game, "GAME_CREATED", user).orElse(null);
                
                if (existing == null) {
                    telegramNotificationService.sendGameCreatedPersonalNotification(gameDto, user);
                    
                    // Сохраняем запись об отправке
                    GameNotification notification = new GameNotification(game, "GAME_CREATED", user);
                    gameNotificationRepository.save(notification);
                }
            }
        }
    }
    
    private void sendPersonalGameCancelledNotifications(GameDto gameDto, Game game) {
        // Отправляем уведомления участникам игры
        for (User participant : game.getParticipants()) {
            if (participant.getTelegramSubscribed() == null || !participant.getTelegramSubscribed()) {
                continue;
            }
            
            UserNotificationSettings settings = settingsRepository.findByUserId(participant.getId()).orElse(null);
            if (settings == null) {
                continue;
            }
            
            String setting = settings.getGameCancelled();
            boolean shouldNotify = "ALL".equals(setting) || "MY_GAMES".equals(setting);
            
            if (shouldNotify) {
                GameNotification existing = gameNotificationRepository.findPersonalNotification(
                        game, "GAME_CANCELLED", participant).orElse(null);
                
                if (existing == null) {
                    telegramNotificationService.sendGameCancelledPersonalNotification(gameDto, participant);
                    
                    // Не сохраняем GameNotification для отмененных игр, так как игра будет удалена
                    // GameNotification notification = new GameNotification(game, "GAME_CANCELLED", participant);
                    // gameNotificationRepository.save(notification);
                }
            }
        }
    }
    
    private void sendPersonalGameHeldNotifications(GameDto gameDto, Game game) {
        // Отправляем уведомления участникам игры
        for (User participant : game.getParticipants()) {
            if (participant.getTelegramSubscribed() == null || !participant.getTelegramSubscribed()) {
                continue;
            }
            
            UserNotificationSettings settings = settingsRepository.findByUserId(participant.getId()).orElse(null);
            if (settings == null) {
                continue;
            }
            
            String setting = settings.getGameHeld();
            boolean shouldNotify = "ALL".equals(setting) || "MY_GAMES".equals(setting);
            
            if (shouldNotify) {
                GameNotification existing = gameNotificationRepository.findPersonalNotification(
                        game, "GAME_HELD", participant).orElse(null);
                
                if (existing == null) {
                    telegramNotificationService.sendGameHeldPersonalNotification(gameDto, participant);
                    
                    GameNotification notification = new GameNotification(game, "GAME_HELD", participant);
                    gameNotificationRepository.save(notification);
                }
            }
        }
    }
}
