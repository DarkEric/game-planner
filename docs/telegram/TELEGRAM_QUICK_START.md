# Telegram уведомления - Быстрый старт

## 3 шага для настройки

### 1️⃣ Создайте бота
1. Откройте [@BotFather](https://t.me/BotFather) в Telegram
2. Отправьте `/newbot`
3. Следуйте инструкциям
4. Сохраните токен (например: `123456789:ABCdefGHIjklMNOpqrsTUVwxyz`)

### 2️⃣ Получите Chat ID
1. Найдите вашего бота в Telegram
2. Отправьте боту `/start`
3. Откройте: `https://api.telegram.org/bot<ВАШ_ТОКЕН>/getUpdates`
4. Найдите `"chat":{"id":123456789}`

### 3️⃣ Настройте .env
```env
TELEGRAM_BOT_ENABLED=true
TELEGRAM_BOT_TOKEN=123456789:ABCdefGHIjklMNOpqrsTUVwxyz
TELEGRAM_BOT_CHAT_ID=123456789
TELEGRAM_BOT_THREAD_ID=  # Опционально, для топиков
FRONTEND_URL=http://localhost:5173
```

**Для топиков в супергруппах:** см. [docs/TELEGRAM_TOPICS.md](docs/TELEGRAM_TOPICS.md)

### Перезапустите
```bash
docker-compose down
docker-compose up -d --build
```

✅ Готово! Теперь при создании игры придет уведомление в Telegram.

---

📖 Подробная инструкция: [TELEGRAM_SETUP.md](TELEGRAM_SETUP.md)
