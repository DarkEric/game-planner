# Тестирование Telegram интеграции

## Подготовка к тестированию

### 1. Создание тестового бота

```
1. Откройте @BotFather в Telegram
2. Отправьте: /newbot
3. Имя: Game Planner Test Bot
4. Username: your_game_planner_test_bot
5. Сохраните токен
```

### 2. Получение Chat ID

#### Для личного чата:
```bash
# 1. Отправьте боту /start
# 2. Откройте в браузере:
https://api.telegram.org/bot<ВАШ_ТОКЕН>/getUpdates

# 3. Найдите в JSON:
{
  "message": {
    "chat": {
      "id": 123456789  // <-- Это ваш Chat ID
    }
  }
}
```

#### Для группы:
```bash
# 1. Создайте группу
# 2. Добавьте бота в группу
# 3. Отправьте сообщение в группу
# 4. Откройте:
https://api.telegram.org/bot<ВАШ_ТОКЕН>/getUpdates

# 5. Chat ID будет отрицательным:
"chat": {
  "id": -1001234567890  // <-- Chat ID группы
}
```

## Конфигурация для тестирования

### .env для локального тестирования

```env
# Database
POSTGRES_DB=game_planner
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres

# Backend
JWT_SECRET=test-secret-key-for-development-only
CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:3000,http://localhost

# Telegram (ТЕСТОВЫЕ ДАННЫЕ)
TELEGRAM_BOT_ENABLED=true
TELEGRAM_BOT_TOKEN=123456789:ABCdefGHIjklMNOpqrsTUVwxyz
TELEGRAM_BOT_CHAT_ID=123456789
FRONTEND_URL=http://localhost:5173
```

## Тестовые сценарии

### Тест 1: Базовое уведомление

**Цель:** Проверить отправку простого уведомления

**Шаги:**
1. Запустите приложение с настроенным Telegram
2. Войдите в систему
3. Создайте игру:
   - Название: "Тестовая игра"
   - Описание: "Проверка уведомлений"
   - Время: любое доступное
4. Проверьте Telegram

**Ожидаемый результат:**
```
🎮 Новая игра запланирована!

📌 Тестовая игра
📝 Проверка уведомлений

🕐 Время: 17.11.2025 18:00 - 17.11.2025 21:00
👤 Организатор: Ваше имя
👥 Участники: 1

🔗 Открыть игру
```

### Тест 2: Игра без названия

**Цель:** Проверить уведомление для игры без названия

**Шаги:**
1. Создайте игру без названия и описания
2. Проверьте Telegram

**Ожидаемый результат:**
```
🎮 Новая игра запланирована!

🕐 Время: 17.11.2025 18:00 - 17.11.2025 21:00
👤 Организатор: Ваше имя
👥 Участники: 1

🔗 Открыть игру
```

### Тест 3: Игра с несколькими участниками

**Цель:** Проверить отображение количества участников

**Шаги:**
1. Создайте несколько пользователей
2. Отметьте доступность на одно время
3. Создайте игру с несколькими участниками
4. Проверьте Telegram

**Ожидаемый результат:**
```
👥 Участники: 3  // или другое число
```

### Тест 4: Специальные символы

**Цель:** Проверить экранирование HTML

**Шаги:**
1. Создайте игру с названием: `<Test> & "Special" 'Chars'`
2. Проверьте Telegram

**Ожидаемый результат:**
Символы должны отображаться корректно, без HTML-тегов

### Тест 5: Ссылка на игру

**Цель:** Проверить работу ссылки

**Шаги:**
1. Создайте игру
2. Кликните на ссылку в Telegram
3. Проверьте, что открылась правильная игра

**Ожидаемый результат:**
Открывается приложение с параметром `?gameId=X`

### Тест 6: Отключенные уведомления

**Цель:** Проверить, что при `TELEGRAM_BOT_ENABLED=false` уведомления не отправляются

**Шаги:**
1. Установите `TELEGRAM_BOT_ENABLED=false`
2. Перезапустите приложение
3. Создайте игру
4. Проверьте Telegram

**Ожидаемый результат:**
Уведомление НЕ должно прийти

### Тест 7: Неверный токен

**Цель:** Проверить обработку ошибок

**Шаги:**
1. Установите неверный `TELEGRAM_BOT_TOKEN`
2. Перезапустите приложение
3. Создайте игру
4. Проверьте логи

**Ожидаемый результат:**
- Игра создается успешно
- В логах ошибка Telegram
- Приложение продолжает работать

### Тест 8: Групповой чат

**Цель:** Проверить отправку в группу

**Шаги:**
1. Создайте группу в Telegram
2. Добавьте бота в группу
3. Получите Chat ID группы (отрицательное число)
4. Настройте `TELEGRAM_BOT_CHAT_ID`
5. Создайте игру

**Ожидаемый результат:**
Уведомление приходит в группу

### Тест 9: Топики в супергруппе

**Цель:** Проверить отправку в конкретный топик

**Шаги:**
1. Создайте супергруппу с включенными топиками
2. Создайте топик "🎮 Игры"
3. Добавьте бота как администратора
4. Получите Thread ID топика
5. Настройте:
   ```env
   TELEGRAM_BOT_CHAT_ID=-1001234567890
   TELEGRAM_BOT_THREAD_ID=5
   ```
6. Создайте игру

**Ожидаемый результат:**
- Уведомление приходит в указанный топик
- В логах: `Sending to thread ID: 5`

### Тест 10: Неверный Thread ID

**Цель:** Проверить обработку неверного Thread ID

**Шаги:**
1. Установите несуществующий `TELEGRAM_BOT_THREAD_ID=999`
2. Создайте игру
3. Проверьте логи

**Ожидаемый результат:**
- Игра создается успешно
- В логах ошибка "message thread not found"
- Приложение продолжает работать

## Проверка логов

### Просмотр логов Telegram

```bash
# Все логи Telegram
docker-compose logs backend | grep -i telegram

# Только ошибки
docker-compose logs backend | grep -i "telegram.*error"

# Последние 50 строк
docker-compose logs --tail=50 backend | grep -i telegram
```

### Ожидаемые логи при успехе

```
INFO  TelegramNotificationService - Telegram notification sent for game: Тестовая игра
```

### Ожидаемые логи при ошибке

```
ERROR TelegramNotificationService - Failed to send Telegram notification
DEBUG TelegramNotificationService - Telegram notifications disabled or chat ID not configured
```

## Отладка

### Проверка конфигурации

```bash
# Проверить переменные окружения в контейнере
docker exec game-planner-backend env | grep TELEGRAM
```

**Ожидаемый вывод:**
```
TELEGRAM_BOT_ENABLED=true
TELEGRAM_BOT_TOKEN=123456789:ABC...
TELEGRAM_BOT_CHAT_ID=123456789
```

### Проверка подключения к Telegram API

```bash
# Проверить, что бот работает
curl "https://api.telegram.org/bot<ВАШ_ТОКЕН>/getMe"
```

**Ожидаемый ответ:**
```json
{
  "ok": true,
  "result": {
    "id": 123456789,
    "is_bot": true,
    "first_name": "Game Planner Test Bot",
    "username": "your_game_planner_test_bot"
  }
}
```

### Ручная отправка тестового сообщения

```bash
curl -X POST "https://api.telegram.org/bot<ВАШ_ТОКЕН>/sendMessage" \
  -H "Content-Type: application/json" \
  -d '{
    "chat_id": "<ВАШ_CHAT_ID>",
    "text": "Test message",
    "parse_mode": "HTML"
  }'
```

## Автоматизированное тестирование

### Unit тесты (будущее улучшение)

```java
@Test
void testSendGameCreatedNotification() {
    // Arrange
    GameDto game = createTestGame();
    
    // Act
    telegramNotificationService.sendGameCreatedNotification(game);
    
    // Assert
    verify(telegramBot).execute(any(SendMessage.class));
}
```

### Integration тесты

```java
@SpringBootTest
@TestPropertySource(properties = {
    "telegram.bot.enabled=true",
    "telegram.bot.token=test-token",
    "telegram.bot.chat-id=123456789"
})
class TelegramIntegrationTest {
    // Tests
}
```

## Чек-лист тестирования

- [ ] Базовое уведомление отправляется
- [ ] Игра без названия работает
- [ ] Несколько участников отображаются
- [ ] Специальные символы экранируются
- [ ] Ссылка работает корректно
- [ ] Отключение работает
- [ ] Ошибки не ломают создание игры
- [ ] Групповой чат работает
- [ ] Логи пишутся корректно
- [ ] Переменные окружения применяются

## Известные проблемы

### Проблема: "Unauthorized"
**Решение:** Проверьте токен, создайте нового бота

### Проблема: "Chat not found"
**Решение:** 
- Проверьте Chat ID
- Для групп: убедитесь, что бот добавлен
- Для каналов: бот должен быть администратором

### Проблема: Уведомления не приходят
**Решение:**
1. Проверьте `TELEGRAM_BOT_ENABLED=true`
2. Проверьте логи
3. Проверьте токен и Chat ID
4. Попробуйте ручную отправку через curl

## Производительность

### Время отправки

Измерьте время отправки уведомления:

```java
long start = System.currentTimeMillis();
telegramNotificationService.sendGameCreatedNotification(game);
long duration = System.currentTimeMillis() - start;
logger.info("Notification sent in {} ms", duration);
```

**Ожидаемое время:** 100-500ms

### Нагрузочное тестирование

Создайте 10 игр подряд и проверьте:
- Все уведомления отправлены
- Нет ошибок rate limiting
- Приложение работает стабильно

## Безопасность

### Проверка экранирования

Попробуйте создать игру с:
```
Название: <script>alert('XSS')</script>
Описание: <img src=x onerror=alert('XSS')>
```

**Ожидаемый результат:**
Теги должны быть экранированы и отображаться как текст

### Проверка токена

Убедитесь, что токен:
- Не логируется
- Не отображается в ошибках
- Не доступен через API

---

**Успешного тестирования! 🧪**
