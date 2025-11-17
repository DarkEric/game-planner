# 🎉 Game Planner v1.1.0 - Telegram Notifications

**Release Date:** November 17, 2025

## 🆕 What's New

### 📱 Telegram Notifications

Теперь Game Planner может отправлять уведомления в Telegram при создании новых игр!

**Основные возможности:**
- 🔔 Автоматические уведомления о новых играх
- 💬 Поддержка личных чатов, групп и каналов
- 🎯 Отправка в конкретные топики супергрупп
- 🎨 Красивые HTML-форматированные сообщения
- 🔗 Прямые ссылки на игры
- ⚙️ Опциональная функция (по умолчанию выключена)

**Пример уведомления:**
```
🎮 Запланирована новая игра!

📌 D&D Session #42
📝 Продолжаем кампанию "Проклятие Страда"

🕐 Время: 17.11.2025 18:00 - 17.11.2025 21:00
👤 Организатор: Иван Иванов

🔗 Посмотреть и записаться на игру
```

### 🎯 Поддержка топиков (Topics)

Отправляйте уведомления в конкретные топики супергрупп Telegram:
- Организуйте уведомления по темам
- Разделяйте разные типы игр
- Держите чат структурированным

## 📖 Быстрый старт

### 1. Создайте бота

```bash
# 1. Откройте @BotFather в Telegram
# 2. Отправьте /newbot
# 3. Следуйте инструкциям
# 4. Сохраните токен
```

### 2. Настройте .env

```env
TELEGRAM_BOT_ENABLED=true
TELEGRAM_BOT_TOKEN=123456789:ABCdefGHIjklMNOpqrsTUVwxyz
TELEGRAM_BOT_CHAT_ID=123456789
TELEGRAM_BOT_THREAD_ID=  # Опционально, для топиков
FRONTEND_URL=http://localhost:5173
```

### 3. Перезапустите

```bash
docker-compose down
docker-compose up -d --build
```

✅ Готово! Теперь при создании игры придет уведомление в Telegram.

## 📚 Документация

### Руководства по настройке
- 📖 [TELEGRAM_SETUP.md](TELEGRAM_SETUP.md) - Подробная инструкция
- ⚡ [TELEGRAM_QUICK_START.md](TELEGRAM_QUICK_START.md) - Быстрый старт за 3 шага
- 🎯 [docs/TELEGRAM_TOPICS.md](docs/TELEGRAM_TOPICS.md) - Настройка топиков

### Техническая документация
- 🔧 [docs/TELEGRAM_INTEGRATION.md](docs/TELEGRAM_INTEGRATION.md) - Техническая документация
- 🧪 [docs/TELEGRAM_TESTING.md](docs/TELEGRAM_TESTING.md) - Руководство по тестированию
- 🔍 [docs/TELEGRAM_DEBUG.md](docs/TELEGRAM_DEBUG.md) - Отладка и решение проблем

### Дополнительно
- 📝 [TELEGRAM_FEATURE_SUMMARY.md](TELEGRAM_FEATURE_SUMMARY.md) - Обзор функциональности
- 🚀 [TELEGRAM_TOPICS_QUICK.md](TELEGRAM_TOPICS_QUICK.md) - Быстрая настройка топиков
- ⚠️ [TELEGRAM_TROUBLESHOOTING_QUICK.md](TELEGRAM_TROUBLESHOOTING_QUICK.md) - Быстрое решение проблем

## 🔧 Технические улучшения

### Backend
- Добавлен `TelegramNotificationService` для отправки уведомлений
- Добавлен `TelegramBotConfig` с условной активацией
- Интеграция в `GameService.createGame()`
- Зависимость: `telegrambots-spring-boot-starter:6.9.7.1`

### Конфигурация
- Новые переменные окружения:
  - `TELEGRAM_BOT_ENABLED` - включить/выключить
  - `TELEGRAM_BOT_TOKEN` - токен бота
  - `TELEGRAM_BOT_CHAT_ID` - ID чата/канала
  - `TELEGRAM_BOT_THREAD_ID` - ID топика (опционально)
  - `FRONTEND_URL` - URL фронтенда для ссылок

### Логирование
- Конфигурация при старте бота
- Подробные логи отправки сообщений
- Улучшенные сообщения об ошибках
- Debug-режим для отладки

## 🐛 Исправления

- Исправлена обработка Thread ID с пробелами
- Улучшена обработка ошибок при неверной конфигурации
- Добавлены отсутствующие debug-логи

## 🔄 Обновление с 1.0.0

### Для пользователей Docker Compose

```bash
# 1. Обновите код
git pull origin main

# 2. Пересоберите контейнеры
docker-compose down
docker-compose up -d --build
```

### Для пользователей готовых образов

```bash
# 1. Обновите код
git pull origin main

# 2. Используйте новые образы
docker-compose -f docker-compose.ghcr.yml pull
docker-compose -f docker-compose.ghcr.yml up -d
```

### Миграция не требуется

Telegram уведомления - опциональная функция. Если не настраивать, приложение работает как раньше.

## ⚙️ Конфигурация

### Минимальная (без Telegram)

Ничего не меняйте - приложение работает как в 1.0.0

### С Telegram (личный чат)

```env
TELEGRAM_BOT_ENABLED=true
TELEGRAM_BOT_TOKEN=ваш_токен
TELEGRAM_BOT_CHAT_ID=ваш_chat_id
```

### С Telegram (топик в супергруппе)

```env
TELEGRAM_BOT_ENABLED=true
TELEGRAM_BOT_TOKEN=ваш_токен
TELEGRAM_BOT_CHAT_ID=-1001234567890
TELEGRAM_BOT_THREAD_ID=5
```

## 🎯 Примеры использования

### Для личного использования
Отправляйте уведомления себе в личные сообщения

### Для команды
Создайте группу и получайте уведомления всей командой

### Для сообщества
Создайте канал и делитесь анонсами игр с подписчиками

### С топиками
Организуйте уведомления по типам игр в разных топиках

## 🔒 Безопасность

- Токены хранятся в переменных окружения
- Не коммитятся в Git
- Graceful error handling - ошибки не ломают создание игр
- Экранирование HTML для безопасности

## 🌟 Особенности

- ✅ Опциональная функция (по умолчанию выключена)
- ✅ Обратная совместимость с 1.0.0
- ✅ Работает с личными чатами, группами, каналами
- ✅ Поддержка топиков в супергруппах
- ✅ Подробное логирование для отладки
- ✅ Graceful degradation при ошибках
- ✅ Настраиваемый URL фронтенда

## 📊 Статистика

- **20 файлов изменено** в первом коммите
- **1226+ строк добавлено** кода и документации
- **8 новых документов** создано
- **3 коммита** в релизе

## 🙏 Благодарности

Спасибо всем, кто тестировал и давал обратную связь!

## 🗺️ Что дальше?

Планы на будущие версии:
- [ ] Уведомления об изменении игр
- [ ] Уведомления об отмене игр
- [ ] Персональные настройки уведомлений
- [ ] Поддержка нескольких чатов
- [ ] Интерактивные кнопки в сообщениях
- [ ] Webhook вместо long polling

## 📞 Поддержка

Если возникли проблемы:
1. Проверьте [TELEGRAM_TROUBLESHOOTING_QUICK.md](TELEGRAM_TROUBLESHOOTING_QUICK.md)
2. Изучите [docs/TELEGRAM_DEBUG.md](docs/TELEGRAM_DEBUG.md)
3. Создайте [Issue на GitHub](https://github.com/DarkEric/game-planner/issues)

---

**Приятного использования! 🎮**

[Скачать релиз](https://github.com/DarkEric/game-planner/releases/tag/v1.1.0)
