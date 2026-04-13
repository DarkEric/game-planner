# Настройка Telegram уведомлений

Game Planner поддерживает отправку уведомлений в Telegram при создании новых игр.

## Быстрая настройка

### 1. Создание бота

1. Откройте Telegram и найдите [@BotFather](https://t.me/BotFather)
2. Отправьте команду `/newbot`
3. Следуйте инструкциям: введите имя и username для бота
4. Сохраните полученный токен (выглядит как `123456789:ABCdefGHIjklMNOpqrsTUVwxyz`)

### 2. Получение Chat ID

#### Для личных сообщений:
1. Найдите вашего бота в Telegram по username
2. Отправьте боту любое сообщение (например, `/start`)
3. Откройте в браузере: `https://api.telegram.org/bot<ВАШ_ТОКЕН>/getUpdates`
4. Найдите в ответе `"chat":{"id":123456789}` - это ваш Chat ID

#### Для группы или канала:
1. Добавьте бота в группу/канал как администратора
2. Отправьте сообщение в группу/канал
3. Откройте в браузере: `https://api.telegram.org/bot<ВАШ_ТОКЕН>/getUpdates`
4. Найдите Chat ID (для групп начинается с `-`, например `-1001234567890`)

### 3. Настройка переменных окружения

Добавьте в ваш `.env` файл:

```env
TELEGRAM_BOT_ENABLED=true
TELEGRAM_BOT_TOKEN=123456789:ABCdefGHIjklMNOpqrsTUVwxyz
TELEGRAM_BOT_CHAT_ID=123456789
TELEGRAM_BOT_THREAD_ID=
TELEGRAM_BOT_TIMEZONE=Europe/Moscow
FRONTEND_URL=http://localhost:5173
```

Для production в `.env`:
```env
TELEGRAM_BOT_ENABLED=true
TELEGRAM_BOT_TOKEN=ваш_токен
TELEGRAM_BOT_CHAT_ID=ваш_chat_id
TELEGRAM_BOT_THREAD_ID=
TELEGRAM_BOT_TIMEZONE=Europe/Moscow
FRONTEND_URL=https://your-domain.com
```

### 3.1. SOCKS5-прокси (опционально)

Если сервер не имеет прямого доступа к `api.telegram.org`, можно направить **весь** трафик бота (long polling и отправка сообщений) через SOCKS5.

```env
TELEGRAM_BOT_PROXY_ENABLED=true
TELEGRAM_BOT_PROXY_HOST=127.0.0.1
TELEGRAM_BOT_PROXY_PORT=1080
# Если прокси требует логин/пароль:
TELEGRAM_BOT_PROXY_USERNAME=myuser
TELEGRAM_BOT_PROXY_PASSWORD=mypassword
```

- При `TELEGRAM_BOT_PROXY_ENABLED=true` обязательно задайте непустой `TELEGRAM_BOT_PROXY_HOST`.
- Порт по умолчанию: `1080`.
- Если `TELEGRAM_BOT_PROXY_USERNAME` не пустой, для авторизации SOCKS5 используется `java.net.Authenticator` **на весь процесс JVM** (учитывайте, если в том же процессе есть другой код с SOCKS на тот же хост:порт).

### 3.2. Прокси на хост-машине, приложение в Docker

Внутри контейнера `127.0.0.1` — это **сам контейнер**, а не ваш ПК/сервер. Чтобы достучаться до SOCKS5, слушающего на хосте:

**Docker Desktop (Windows, macOS)** — обычно уже есть имя хоста:

```env
TELEGRAM_BOT_PROXY_ENABLED=true
TELEGRAM_BOT_PROXY_HOST=host.docker.internal
TELEGRAM_BOT_PROXY_PORT=1080
```

**Linux (docker compose)** — добавьте маппинг и используйте тот же хост:

```yaml
services:
  backend:
    extra_hosts:
      - "host.docker.internal:host-gateway"
```

```env
TELEGRAM_BOT_PROXY_HOST=host.docker.internal
```

Альтернатива на Linux без `extra_hosts`: IP шлюза bridge (часто `172.17.0.1`) или реальный LAN-IP хоста — зависит от сети.

**На стороне прокси на хосте** убедитесь, что он слушает не только `127.0.0.1`, если вы подключаетесь с «внешнего» IP шлюза: для локальной разработки часто достаточно `0.0.0.0:1080` или явной привязки к `host.docker.internal` после проверки политикой безопасности.

### 4. Перезапуск приложения

```bash
# Для Docker Compose
docker-compose down
docker-compose up -d --build

# Или используйте скрипты
./start.bat  # Windows
./start.sh   # Linux/Mac
```

## Формат уведомлений

При создании новой игры в Telegram будет отправлено сообщение:

```
🎮 Новая игра запланирована!

📌 Название игры
📝 Описание игры

🕐 Время: 17.11.2025 18:00 - 17.11.2025 21:00
👤 Организатор: Иван Иванов
👥 Участники: 3

🔗 Открыть игру
```

## Отключение уведомлений

Чтобы отключить уведомления, установите в `.env`:

```env
TELEGRAM_BOT_ENABLED=false
```

Или просто удалите/закомментируйте эти переменные.

## Устранение проблем

### Бот не отправляет сообщения

1. Проверьте, что `TELEGRAM_BOT_ENABLED=true`
2. Убедитесь, что токен правильный
3. Проверьте Chat ID (для групп должен начинаться с `-`)
4. Убедитесь, что бот добавлен в группу/канал как администратор
5. Проверьте логи приложения: `docker-compose logs backend | grep -i telegram`

📖 **Подробное руководство по отладке:** [docs/TELEGRAM_DEBUG.md](docs/TELEGRAM_DEBUG.md)

### Ошибка "Unauthorized"

- Токен неверный или устарел
- Создайте нового бота у @BotFather

### Ошибка "Chat not found"

- Chat ID неверный
- Для групп/каналов: убедитесь, что бот добавлен как администратор
- Попробуйте получить Chat ID заново

## Дополнительные возможности

### Использование канала для уведомлений

1. Создайте публичный или приватный канал
2. Добавьте бота как администратора канала
3. Получите Chat ID канала (начинается с `-100`)
4. Используйте этот ID в `TELEGRAM_BOT_CHAT_ID`

### Использование топиков в супергруппах

Топики позволяют организовать уведомления в отдельном разделе супергруппы:

1. Создайте супергруппу и включите топики
2. Создайте топик (например, "🎮 Игры")
3. Получите Thread ID топика
4. Добавьте в `.env`:
   ```env
   TELEGRAM_BOT_CHAT_ID=-1001234567890
   TELEGRAM_BOT_THREAD_ID=5
   ```

📖 **Подробная инструкция:** [docs/TELEGRAM_TOPICS.md](docs/TELEGRAM_TOPICS.md)

### Несколько чатов

В текущей версии поддерживается только один чат. Для отправки в несколько чатов можно:
- Создать группу и добавить туда всех участников
- Или доработать код для поддержки нескольких Chat ID

## Безопасность

⚠️ **Важно:**
- Никогда не публикуйте токен бота в открытом доступе
- Не коммитьте `.env` файл в Git
- Используйте `.env.example` как шаблон
- Храните токены в безопасном месте
