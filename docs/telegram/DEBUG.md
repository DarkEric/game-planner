# Отладка Telegram интеграции

## Проверка конфигурации

### 1. Проверка переменных окружения в контейнере

```bash
docker exec game-planner-backend env | grep TELEGRAM
```

**Ожидаемый вывод:**
```
TELEGRAM_BOT_ENABLED=true
TELEGRAM_BOT_TOKEN=123456789:ABC...
TELEGRAM_BOT_CHAT_ID=-1001234567890
TELEGRAM_BOT_THREAD_ID=5
# при SOCKS5 дополнительно:
TELEGRAM_BOT_PROXY_ENABLED=true
TELEGRAM_BOT_PROXY_HOST=127.0.0.1
TELEGRAM_BOT_PROXY_PORT=1080
TELEGRAM_BOT_PROXY_USERNAME=
TELEGRAM_BOT_PROXY_PASSWORD=
```

**Проблемы:**
- Если переменные не отображаются → проверьте `.env` файл
- Если `TELEGRAM_BOT_THREAD_ID` пустая → проверьте, что она указана в `.env`

### 2. Проверка логов при старте

```bash
docker-compose logs backend | grep -A 10 "Telegram Bot Configuration"
```

**Ожидаемый вывод:**
```
=== Telegram Bot Configuration ===
Enabled: true
Chat ID: -1001234567890
Thread ID: 5
Frontend URL: http://localhost:5173
Proxy: SOCKS5 127.0.0.1:1080
Proxy auth: enabled (user: myuser)
==================================
```

**Проблемы:**
- `Thread ID: NOT SET` → переменная не передается в контейнер
- Секция не отображается → бот не инициализирован (проверьте `TELEGRAM_BOT_ENABLED`)
- Ожидали прокси, в логах `Proxy: none` → проверьте `TELEGRAM_BOT_PROXY_ENABLED` и что переменные попали в контейнер (`env | grep PROXY`)
- Падение при старте с текстом про `proxy.host` → при `TELEGRAM_BOT_PROXY_ENABLED=true` нужен непустой `TELEGRAM_BOT_PROXY_HOST`

### 3. Проверка логов при отправке

```bash
docker-compose logs backend | grep -i telegram | tail -20
```

**Ожидаемый вывод при успешной отправке:**
```
INFO  TelegramNotificationService - Preparing to send Telegram notification for game: Название игры
DEBUG TelegramNotificationService - Chat ID: -1001234567890, Thread ID: '5'
INFO  TelegramNotificationService - Sending to thread ID: 5
INFO  TelegramNotificationService - Telegram notification successfully sent for game: Название игры
```

**Ожидаемый вывод без Thread ID:**
```
INFO  TelegramNotificationService - Preparing to send Telegram notification for game: Название игры
DEBUG TelegramNotificationService - Chat ID: -1001234567890, Thread ID: ''
DEBUG TelegramNotificationService - No thread ID specified, sending to main chat
INFO  TelegramNotificationService - Telegram notification successfully sent for game: Название игры
```

## Типичные проблемы

### Проблема 1: Thread ID не применяется

**Симптомы:**
- Сообщения идут в основной чат вместо топика
- В логах: `No thread ID specified, sending to main chat`

**Решение:**

1. Проверьте `.env` файл:
   ```bash
   cat .env | grep THREAD
   ```
   
2. Убедитесь, что нет пробелов:
   ```env
   # НЕПРАВИЛЬНО:
   TELEGRAM_BOT_THREAD_ID = 5
   TELEGRAM_BOT_THREAD_ID= 5
   
   # ПРАВИЛЬНО:
   TELEGRAM_BOT_THREAD_ID=5
   ```

3. Перезапустите контейнеры:
   ```bash
   docker-compose down
   docker-compose up -d --build
   ```

4. Проверьте переменные в контейнере:
   ```bash
   docker exec game-planner-backend env | grep THREAD
   ```

### Проблема 2: Invalid thread ID format

**Симптомы:**
- В логах: `Invalid thread ID format: 'abc'`

**Причины:**
- Thread ID содержит нечисловые символы
- Лишние пробелы или символы

**Решение:**
```env
# НЕПРАВИЛЬНО:
TELEGRAM_BOT_THREAD_ID=abc
TELEGRAM_BOT_THREAD_ID=5.0
TELEGRAM_BOT_THREAD_ID= 5 

# ПРАВИЛЬНО:
TELEGRAM_BOT_THREAD_ID=5
```

### Проблема 3: Message thread not found

**Симптомы:**
- В логах: `Failed to send Telegram notification`
- Ошибка от Telegram: "Bad Request: message thread not found"

**Причины:**
1. Thread ID неверный
2. Топик был удален
3. Топики отключены в группе
4. Бот не является администратором

**Решение:**

1. Получите Thread ID заново:
   ```bash
   # Отправьте сообщение в топик, затем:
   curl "https://api.telegram.org/bot<TOKEN>/getUpdates" | jq '.result[-1].message.message_thread_id'
   ```

2. Проверьте, что топик существует в группе

3. Убедитесь, что бот - администратор:
   - Настройки группы → Администраторы
   - Найдите вашего бота
   - Проверьте права на отправку сообщений

4. Проверьте, что топики включены:
   - Настройки группы → Топики → Включено

### Проблема 4: Переменная не читается из .env

**Симптомы:**
- В контейнере переменная пустая или отсутствует
- В `.env` файле переменная указана

**Решение:**

1. Проверьте, что используете правильный docker-compose файл:
   ```bash
   # Для разработки:
   docker-compose up -d
   
   # Для production:
   docker-compose -f docker-compose.prod.yml up -d
   ```

2. Проверьте синтаксис в docker-compose.yml:
   ```yaml
   environment:
     TELEGRAM_BOT_THREAD_ID: ${TELEGRAM_BOT_THREAD_ID:-}
   ```

3. Пересоберите контейнеры:
   ```bash
   docker-compose down
   docker-compose up -d --build
   ```

## Ручное тестирование

### Тест 1: Отправка в основной чат

```bash
curl -X POST "https://api.telegram.org/bot<TOKEN>/sendMessage" \
  -H "Content-Type: application/json" \
  -d '{
    "chat_id": "-1001234567890",
    "text": "Test message to main chat"
  }'
```

### Тест 2: Отправка в топик

```bash
curl -X POST "https://api.telegram.org/bot<TOKEN>/sendMessage" \
  -H "Content-Type: application/json" \
  -d '{
    "chat_id": "-1001234567890",
    "message_thread_id": 5,
    "text": "Test message to topic"
  }'
```

**Если тест 2 не работает:**
- Проверьте Thread ID
- Убедитесь, что бот - администратор
- Проверьте, что топик существует

## Включение DEBUG логов

Для более подробных логов добавьте в `application.properties`:

```properties
logging.level.ru.ambryo.gameplannerback.service.TelegramNotificationService=DEBUG
```

Или через переменную окружения в docker-compose:

```yaml
environment:
  LOGGING_LEVEL_RU_AMBRYO_GAMEPLANNERBACK_SERVICE_TELEGRAMNOTIFICATIONSERVICE: DEBUG
```

Перезапустите:
```bash
docker-compose down
docker-compose up -d
```

## Полезные команды

### Просмотр всех логов Telegram

```bash
docker-compose logs backend | grep -i telegram
```

### Просмотр последних 50 строк

```bash
docker-compose logs --tail=50 backend | grep -i telegram
```

### Следить за логами в реальном времени

```bash
docker-compose logs -f backend | grep -i telegram
```

### Проверка конфигурации Spring

```bash
docker exec game-planner-backend cat /app/application.properties | grep telegram
```

## Чек-лист отладки

- [ ] Переменные окружения установлены в `.env`
- [ ] Переменные видны в контейнере (`docker exec ... env`)
- [ ] Конфигурация логируется при старте
- [ ] Thread ID - положительное число
- [ ] Chat ID - отрицательное число (для групп)
- [ ] Бот добавлен в группу как администратор
- [ ] Топики включены в настройках группы
- [ ] Топик с указанным ID существует
- [ ] Ручная отправка через curl работает
- [ ] Логи показывают правильный Thread ID

## Получение помощи

Если проблема не решается:

1. Соберите информацию:
   ```bash
   # Конфигурация
   docker exec game-planner-backend env | grep TELEGRAM > telegram-config.txt
   
   # Логи
   docker-compose logs backend | grep -i telegram > telegram-logs.txt
   ```

2. Создайте Issue на GitHub с:
   - Описанием проблемы
   - Файлами telegram-config.txt и telegram-logs.txt (удалите токены!)
   - Версией приложения
   - Используемым docker-compose файлом

---

**Успешной отладки! 🔍**
