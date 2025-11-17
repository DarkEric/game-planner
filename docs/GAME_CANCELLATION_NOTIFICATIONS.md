# Уведомления об отмене игр

## Описание

При удалении игры организатором в Telegram автоматически отправляется уведомление об отмене с возможностью указать причину.

## Как это работает

### 1. Удаление игры

Когда организатор удаляет игру:
1. Появляется диалог с запросом причины отмены
2. Причина опциональна - можно оставить пустой или нажать "Отмена"
3. Если указана причина, она будет включена в уведомление

### 2. Уведомление в Telegram

Формат уведомления:

**С причиной:**
```
❌ Игра отменена

📌 D&D Session #42
🕐 Время: 17.11.2025 18:00 - 17.11.2025 21:00
👤 Организатор: Иван Иванов

💬 Причина отмены:
Не хватает игроков
```

**Без причины:**
```
❌ Игра отменена

📌 D&D Session #42
🕐 Время: 17.11.2025 18:00 - 17.11.2025 21:00
👤 Организатор: Иван Иванов
```

## Технические детали

### Backend

**TelegramNotificationService:**
```java
public void sendGameCancelledNotification(GameDto game, String cancellationReason)
```

**GameService:**
```java
public void deleteGame(Long gameId, User user, String cancellationReason)
```

**GameController:**
```java
@DeleteMapping("/{gameId}")
public ResponseEntity<Void> deleteGame(
    @PathVariable Long gameId,
    @RequestParam(required = false) String cancellationReason,
    Authentication authentication)
```

### Frontend

**GameDetails.jsx:**
```javascript
onClick={() => {
  const reason = prompt('Причина отмены игры (опционально):')
  if (reason !== null) {
    onDelete(game.id, reason)
  }
}}
```

**gameApi.js:**
```javascript
async deleteGame(gameId, cancellationReason) {
  const params = new URLSearchParams()
  if (cancellationReason && cancellationReason.trim()) {
    params.append('cancellationReason', cancellationReason.trim())
  }
  // ...
}
```

## Примеры использования

### Пример 1: Отмена с причиной

1. Организатор открывает детали игры
2. Нажимает "Удалить игру"
3. В диалоге вводит: "Не хватает игроков"
4. Нажимает OK
5. Игра удаляется, уведомление отправляется в Telegram

### Пример 2: Отмена без причины

1. Организатор открывает детали игры
2. Нажимает "Удалить игру"
3. Оставляет поле пустым или нажимает OK
4. Игра удаляется, уведомление отправляется без причины

### Пример 3: Отмена действия

1. Организатор открывает детали игры
2. Нажимает "Удалить игру"
3. Нажимает "Отмена" в диалоге
4. Игра НЕ удаляется, уведомление НЕ отправляется

## Особенности

### Безопасность
- Только организатор может удалить игру
- Причина экранируется для безопасности (HTML escape)
- Пустые причины не отправляются

### Обработка ошибок
- Ошибка отправки уведомления не блокирует удаление игры
- Логируется в консоль для отладки
- Graceful degradation

### Локализация
- Диалог на русском языке
- Можно добавить поддержку других языков через i18n

## Настройка

Уведомления об отмене работают автоматически, если настроены Telegram уведомления:

```env
TELEGRAM_BOT_ENABLED=true
TELEGRAM_BOT_TOKEN=ваш_токен
TELEGRAM_BOT_CHAT_ID=ваш_chat_id
```

См. [TELEGRAM_SETUP.md](../TELEGRAM_SETUP.md) для полной настройки.

## Тестирование

### Тест 1: Отмена с причиной

1. Создайте игру
2. Откройте детали игры
3. Нажмите "Удалить игру"
4. Введите причину: "Тестовая отмена"
5. Проверьте Telegram

**Ожидаемый результат:**
```
❌ Игра отменена
...
💬 Причина отмены:
Тестовая отмена
```

### Тест 2: Отмена без причины

1. Создайте игру
2. Удалите без указания причины
3. Проверьте Telegram

**Ожидаемый результат:**
Уведомление без блока "Причина отмены"

### Тест 3: Специальные символы

1. Создайте игру
2. Удалите с причиной: `<script>alert('test')</script>`
3. Проверьте Telegram

**Ожидаемый результат:**
Символы экранированы, отображаются как текст

### Тест 4: Длинная причина

1. Создайте игру
2. Удалите с длинной причиной (200+ символов)
3. Проверьте Telegram

**Ожидаемый результат:**
Причина отображается полностью

## Логи

### Успешная отправка

```
INFO  TelegramNotificationService - Preparing to send game cancellation notification for game: D&D Session
DEBUG TelegramNotificationService - Chat ID: -1001234567890, Thread ID: '5'
INFO  TelegramNotificationService - Sending cancellation to thread ID: 5
INFO  TelegramNotificationService - Telegram cancellation notification successfully sent for game: D&D Session
```

### Ошибка отправки

```
ERROR TelegramNotificationService - Failed to send Telegram cancellation notification for game: D&D Session
```

## Будущие улучшения

- [ ] Подтверждение удаления с чекбоксом вместо prompt
- [ ] Многострочное поле для причины
- [ ] Шаблоны причин (выбор из списка)
- [ ] Уведомление участников по email
- [ ] История отмененных игр
- [ ] Статистика причин отмены

## API

### DELETE /api/games/{gameId}

**Query Parameters:**
- `cancellationReason` (optional) - Причина отмены игры

**Response:**
- `204 No Content` - Игра успешно удалена
- `403 Forbidden` - Только организатор может удалить игру
- `404 Not Found` - Игра не найдена

**Example:**
```bash
curl -X DELETE "http://localhost:8080/api/games/123?cancellationReason=Не%20хватает%20игроков" \
  -H "Authorization: Bearer <token>"
```

---

**Приятного использования! 🎮**
