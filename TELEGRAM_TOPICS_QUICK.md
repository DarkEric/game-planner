# Топики в Telegram - Быстрая инструкция

## Что это?

Топики (Topics/Threads) - это подразделы в супергруппах Telegram. Позволяют отправлять уведомления в конкретный раздел группы.

## Быстрая настройка

### 1. Создайте супергруппу с топиками

1. Создайте группу → Преобразуйте в супергруппу
2. Настройки → Топики → Включить
3. Создайте топик "🎮 Игры"

### 2. Получите Thread ID

**Способ 1 - Через API:**
```
1. Отправьте сообщение в топик
2. Откройте: https://api.telegram.org/bot<ТОКЕН>/getUpdates
3. Найдите: "message_thread_id": 5
```

**Способ 2 - Через веб-версию:**
```
1. Откройте топик на web.telegram.org
2. URL: https://web.telegram.org/k/#-1001234567890_5
3. Число после "_" - это Thread ID (5)
```

### 3. Настройте .env

```env
TELEGRAM_BOT_ENABLED=true
TELEGRAM_BOT_TOKEN=ваш_токен
TELEGRAM_BOT_CHAT_ID=-1001234567890  # ID супергруппы
TELEGRAM_BOT_THREAD_ID=5              # ID топика
FRONTEND_URL=http://localhost:5173
```

### 4. Перезапустите

```bash
docker-compose down
docker-compose up -d --build
```

## Проверка

Создайте игру → Уведомление должно прийти в указанный топик

Проверьте логи:
```bash
docker-compose logs backend | grep "thread"
```

## Важно

- ✅ Бот должен быть администратором группы
- ✅ Топики должны быть включены
- ✅ Thread ID - положительное число
- ✅ Chat ID группы - отрицательное число

## Без топика

Если не указать `TELEGRAM_BOT_THREAD_ID`, сообщения пойдут в основной чат группы (General).

---

📖 **Подробная инструкция:** [docs/TELEGRAM_TOPICS.md](docs/TELEGRAM_TOPICS.md)
