# Работа с топиками (Topics) в Telegram

Топики (или темы/threads) - это подразделы внутри супергрупп Telegram, которые позволяют организовать обсуждения по разным темам в одной группе.

## Что такое топики?

Топики доступны только в **супергруппах** (supergroups) и позволяют:
- Разделить обсуждения по темам
- Отправлять сообщения в конкретный топик
- Организовать структурированное общение

## Настройка отправки в топик

### Шаг 1: Создайте супергруппу с топиками

1. Создайте группу в Telegram
2. Преобразуйте её в супергруппу:
   - Настройки группы → Тип группы → Супергруппа
3. Включите топики:
   - Настройки группы → Топики → Включить

### Шаг 2: Создайте топик

1. В супергруппе нажмите на "+" или "Создать топик"
2. Введите название (например, "🎮 Игры")
3. Выберите иконку и цвет

### Шаг 3: Добавьте бота в группу

1. Добавьте вашего бота в супергруппу
2. Сделайте бота администратором:
   - Настройки группы → Администраторы → Добавить администратора
   - Выберите вашего бота
   - Дайте права на отправку сообщений

### Шаг 4: Получите Thread ID

#### Способ 1: Через API

1. Отправьте сообщение в нужный топик
2. Откройте в браузере:
   ```
   https://api.telegram.org/bot<ВАШ_ТОКЕН>/getUpdates
   ```
3. Найдите в JSON ответе:
   ```json
   {
     "message": {
       "message_id": 123,
       "message_thread_id": 5,  // <-- Это Thread ID
       "chat": {
         "id": -1001234567890,
         "type": "supergroup"
       }
     }
   }
   ```

#### Способ 2: Через веб-версию Telegram

1. Откройте топик в веб-версии Telegram (web.telegram.org)
2. URL будет выглядеть так:
   ```
   https://web.telegram.org/k/#-1001234567890_5
   ```
   Число после `_` - это Thread ID (в примере: `5`)

### Шаг 5: Настройте переменные окружения

Добавьте в `.env`:

```env
TELEGRAM_BOT_ENABLED=true
TELEGRAM_BOT_TOKEN=123456789:ABCdefGHIjklMNOpqrsTUVwxyz
TELEGRAM_BOT_CHAT_ID=-1001234567890
TELEGRAM_BOT_THREAD_ID=5
FRONTEND_URL=http://localhost:5173
```

**Важно:**
- `TELEGRAM_BOT_CHAT_ID` - это ID супергруппы (отрицательное число)
- `TELEGRAM_BOT_THREAD_ID` - это ID конкретного топика (положительное число)

### Шаг 6: Перезапустите приложение

```bash
docker-compose down
docker-compose up -d --build
```

## Примеры использования

### Пример 1: Отправка в основной чат группы

```env
TELEGRAM_BOT_CHAT_ID=-1001234567890
TELEGRAM_BOT_THREAD_ID=
```

Сообщения будут отправляться в основной чат группы (General).

### Пример 2: Отправка в конкретный топик

```env
TELEGRAM_BOT_CHAT_ID=-1001234567890
TELEGRAM_BOT_THREAD_ID=5
```

Сообщения будут отправляться в топик с ID 5.

### Пример 3: Несколько топиков для разных целей

Можно создать несколько топиков:
- 🎮 Игры (Thread ID: 5) - для уведомлений о новых играх
- 📅 Расписание (Thread ID: 7) - для изменений расписания
- 💬 Обсуждения (Thread ID: 9) - для общения

Текущая реализация поддерживает один топик. Для отправки в разные топики нужно расширить функциональность.

## Проверка работы

### Тест отправки в топик

1. Создайте игру в приложении
2. Проверьте, что уведомление пришло в нужный топик
3. Проверьте логи:
   ```bash
   docker-compose logs backend | grep -i "thread"
   ```

Ожидаемый лог:
```
DEBUG TelegramNotificationService - Sending to thread ID: 5
INFO  TelegramNotificationService - Telegram notification sent for game: Название игры
```

## Устранение проблем

### Сообщения не приходят в топик

**Проверьте:**
1. Бот добавлен в группу как администратор
2. Thread ID правильный (положительное число)
3. Chat ID правильный (отрицательное число для групп)
4. Топики включены в настройках группы

**Проверка через curl:**
```bash
curl -X POST "https://api.telegram.org/bot<TOKEN>/sendMessage" \
  -H "Content-Type: application/json" \
  -d '{
    "chat_id": "-1001234567890",
    "message_thread_id": 5,
    "text": "Test message to topic"
  }'
```

### Ошибка "Bad Request: message thread not found"

**Причины:**
- Thread ID неверный
- Топик был удален
- Топики отключены в группе

**Решение:**
1. Получите Thread ID заново
2. Убедитесь, что топик существует
3. Проверьте, что топики включены

### Ошибка "Forbidden: bot is not a member of the supergroup"

**Решение:**
1. Добавьте бота в группу
2. Сделайте его администратором

## Дополнительные возможности

### Автоматическое создание топика (будущее улучшение)

Можно добавить функцию автоматического создания топика:

```java
public Integer createTopic(String name, String iconColor) {
    CreateForumTopic createTopic = new CreateForumTopic();
    createTopic.setChatId(chatId);
    createTopic.setName(name);
    createTopic.setIconColor(iconColor);
    
    ForumTopic topic = execute(createTopic);
    return topic.getMessageThreadId();
}
```

### Отправка в разные топики в зависимости от типа игры

Можно расширить логику:

```java
private Integer getThreadIdForGame(GameDto game) {
    // Логика выбора топика в зависимости от игры
    if (game.getTitle().contains("D&D")) {
        return 5; // Топик для D&D
    } else if (game.getTitle().contains("Мафия")) {
        return 7; // Топик для Мафии
    }
    return defaultThreadId;
}
```

## Полезные ссылки

- [Telegram Bot API - Forum Topics](https://core.telegram.org/bots/api#forum-topics)
- [Telegram Bot API - sendMessage](https://core.telegram.org/bots/api#sendmessage)
- [Документация по супергруппам](https://telegram.org/blog/topics-in-groups-collectible-usernames)

## FAQ

**Q: Можно ли отправлять в несколько топиков одновременно?**  
A: Да, но нужно расширить код для поддержки списка Thread ID.

**Q: Работают ли топики в обычных группах?**  
A: Нет, только в супергруппах.

**Q: Можно ли отправлять в топик без прав администратора?**  
A: Нет, бот должен быть администратором группы.

**Q: Как узнать все топики в группе?**  
A: Используйте метод API `getForumTopicIconStickers` или просмотрите через getUpdates.

**Q: Что будет, если указать несуществующий Thread ID?**  
A: Telegram вернет ошибку "message thread not found", но создание игры не прервется.

---

**Успешной настройки! 🎯**
