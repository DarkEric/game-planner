# Telegram - Быстрое решение проблем

## Сообщения не идут в топик

### Шаг 1: Проверьте переменные

```bash
docker exec game-planner-backend env | grep TELEGRAM
```

Должно быть:
```
TELEGRAM_BOT_ENABLED=true
TELEGRAM_BOT_TOKEN=123456789:ABC...
TELEGRAM_BOT_CHAT_ID=-1001234567890
TELEGRAM_BOT_THREAD_ID=5
```

### Шаг 2: Проверьте логи при старте

```bash
docker-compose logs backend | grep "Telegram Bot Configuration" -A 5
```

Должно быть:
```
=== Telegram Bot Configuration ===
Enabled: true
Chat ID: -1001234567890
Thread ID: 5
```

Если `Thread ID: NOT SET`:
1. Проверьте `.env` файл
2. Убедитесь, что нет пробелов: `TELEGRAM_BOT_THREAD_ID=5`
3. Перезапустите: `docker-compose down && docker-compose up -d --build`

### Шаг 3: Проверьте логи при отправке

```bash
docker-compose logs backend | grep -i "sending to thread" | tail -5
```

Должно быть:
```
INFO  TelegramNotificationService - Sending to thread ID: 5
```

Если нет:
- Проверьте, что Thread ID правильный (положительное число)
- Убедитесь, что топик существует
- Проверьте, что бот - администратор группы

### Шаг 4: Ручной тест

```bash
curl -X POST "https://api.telegram.org/bot<ВАШ_ТОКЕН>/sendMessage" \
  -H "Content-Type: application/json" \
  -d '{
    "chat_id": "-1001234567890",
    "message_thread_id": 5,
    "text": "Test"
  }'
```

Если ошибка "message thread not found":
- Thread ID неверный
- Топик удален
- Бот не администратор

## Быстрые исправления

### Исправление 1: Пересоздать контейнеры

```bash
docker-compose down
docker-compose up -d --build
```

### Исправление 2: Проверить .env

```bash
cat .env | grep THREAD
```

Должно быть без пробелов:
```
TELEGRAM_BOT_THREAD_ID=5
```

### Исправление 3: Получить Thread ID заново

1. Отправьте сообщение в топик
2. Откройте: `https://api.telegram.org/bot<ТОКЕН>/getUpdates`
3. Найдите: `"message_thread_id": 5`
4. Обновите `.env`
5. Перезапустите

---

📖 **Полное руководство:** [docs/TELEGRAM_DEBUG.md](docs/TELEGRAM_DEBUG.md)
