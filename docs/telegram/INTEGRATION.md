# Telegram Integration - Technical Documentation

## Architecture

The Telegram notification system is implemented as an optional feature that can be enabled/disabled via configuration.

### Components

1. **TelegramNotificationService** - Main service for sending notifications
2. **TelegramBotConfig** - Configuration class for bot initialization
3. **GameService** - Integration point where notifications are triggered

## Implementation Details

### 1. TelegramNotificationService

Located: `game-planner-back/src/main/java/ru/ambryo/gameplannerback/service/TelegramNotificationService.java`

**Key features:**
- Extends `TelegramLongPollingBot` from telegram-bots library
- Configured via Spring properties
- Only sends notifications (doesn't process incoming messages)
- Handles errors gracefully without breaking game creation

**Main method:**
```java
public void sendGameCreatedNotification(GameDto game)
```

### 2. TelegramBotConfig

Located: `game-planner-back/src/main/java/ru/ambryo/gameplannerback/config/TelegramBotConfig.java`

**Key features:**
- Conditional configuration (`@ConditionalOnProperty`)
- Only activates when `telegram.bot.enabled=true`
- Registers bot with Telegram API on startup

### 3. GameService Integration

Located: `game-planner-back/src/main/java/ru/ambryo/gameplannerback/service/GameService.java`

**Integration point:**
```java
@Transactional
public GameDto createGame(CreateGameRequest request, User creator) {
    // ... game creation logic ...
    
    GameDto gameDto = convertToDto(game);
    
    // Send Telegram notification
    try {
        telegramNotificationService.sendGameCreatedNotification(gameDto);
    } catch (Exception e) {
        // Log error but don't break game creation
        System.err.println("Failed to send Telegram notification: " + e.getMessage());
    }
    
    return gameDto;
}
```

## Configuration

### Application Properties

```properties
# Telegram Bot Configuration
telegram.bot.enabled=${TELEGRAM_BOT_ENABLED:false}
telegram.bot.token=${TELEGRAM_BOT_TOKEN:}
telegram.bot.chat-id=${TELEGRAM_BOT_CHAT_ID:}
app.frontend.url=${FRONTEND_URL:http://localhost:5173}
```

### Environment Variables

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `TELEGRAM_BOT_ENABLED` | No | `false` | Enable/disable notifications |
| `TELEGRAM_BOT_TOKEN` | Yes* | - | Bot token from @BotFather |
| `TELEGRAM_BOT_CHAT_ID` | Yes* | - | Target chat/channel ID |
| `TELEGRAM_BOT_THREAD_ID` | No | - | Thread ID for topics in supergroups |
| `FRONTEND_URL` | No | `http://localhost:5173` | Frontend URL for links |

*Required only when `TELEGRAM_BOT_ENABLED=true`

## Dependencies

### Gradle

```gradle
implementation 'org.telegram:telegrambots-spring-boot-starter:6.9.7.1'
```

This dependency includes:
- Telegram Bots API
- Spring Boot auto-configuration
- WebHook and Long Polling support

## Message Format

Messages are sent in HTML format with the following structure:

```html
🎮 <b>Новая игра запланирована!</b>

📌 <b>Game Title</b>
📝 Game Description

🕐 <b>Время:</b> 17.11.2025 18:00 - 17.11.2025 21:00
👤 <b>Организатор:</b> Creator Name
👥 <b>Участники:</b> 3

🔗 <a href="http://localhost:5173?gameId=123">Открыть игру</a>
```

## Error Handling

The system implements graceful error handling:

1. **Configuration errors** - Bot doesn't start if disabled or misconfigured
2. **Runtime errors** - Logged but don't prevent game creation
3. **API errors** - Caught and logged with details

## Testing

### Manual Testing

1. Set up bot credentials in `.env`
2. Create a new game
3. Check Telegram for notification

### Debugging

Enable debug logging:
```properties
logging.level.ru.ambryo.gameplannerback.service.TelegramNotificationService=DEBUG
```

Check logs:
```bash
docker-compose logs backend | grep Telegram
```

## Security Considerations

1. **Token Security**
   - Never commit tokens to Git
   - Use environment variables
   - Rotate tokens if exposed

2. **Chat ID Privacy**
   - Chat IDs are sensitive
   - Don't expose in logs or errors

3. **HTML Injection**
   - All user input is escaped
   - Uses `escapeHtml()` method

## Extending Functionality

### Adding More Notification Types

1. Add new method in `TelegramNotificationService`:
```java
public void sendGameUpdatedNotification(GameDto game) {
    // Implementation
}
```

2. Call from appropriate service:
```java
telegramNotificationService.sendGameUpdatedNotification(gameDto);
```

### Using Topics in Supergroups

Topics (threads) are supported via `TELEGRAM_BOT_THREAD_ID`:

```env
TELEGRAM_BOT_CHAT_ID=-1001234567890
TELEGRAM_BOT_THREAD_ID=5
```

The service automatically adds `message_thread_id` to the message if Thread ID is configured.

See [TELEGRAM_TOPICS.md](TELEGRAM_TOPICS.md) for detailed setup instructions.

### Supporting Multiple Chats

Current implementation supports single chat. To support multiple:

1. Change configuration to accept list:
```properties
telegram.bot.chat-ids=${TELEGRAM_BOT_CHAT_IDS:}
```

2. Update service to iterate:
```java
for (String chatId : chatIds) {
    sendMessage.setChatId(chatId);
    execute(sendMessage);
}
```

### Custom Message Templates

Create template system:

1. Add templates to resources
2. Load with Spring's `ResourceLoader`
3. Replace placeholders with game data

## Troubleshooting

### Bot not sending messages

**Check:**
1. `TELEGRAM_BOT_ENABLED=true`
2. Token is correct
3. Chat ID is correct
4. Bot is added to group/channel (if applicable)

**Logs:**
```bash
docker-compose logs backend | grep -i telegram
```

### "Unauthorized" error

- Token is invalid or revoked
- Create new bot with @BotFather

### "Chat not found" error

- Chat ID is incorrect
- Bot not added to group/channel
- Bot doesn't have admin rights (for channels)

## Performance Considerations

- Notifications are sent synchronously
- Wrapped in try-catch to not block game creation
- Consider async execution for high-load scenarios:

```java
@Async
public void sendGameCreatedNotification(GameDto game) {
    // Implementation
}
```

## Future Improvements

- [ ] Async notification sending
- [ ] Notification queue with retry logic
- [ ] Multiple chat support
- [ ] Customizable message templates
- [ ] Notification preferences per user
- [ ] Rich media support (images, buttons)
- [ ] Webhook support instead of long polling
