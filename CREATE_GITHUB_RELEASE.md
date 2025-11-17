# Создание релиза v1.1.0 на GitHub

## Автоматически (через GitHub CLI)

Если у вас установлен GitHub CLI:

```bash
gh release create v1.1.0 \
  --title "v1.1.0 - Telegram Notifications" \
  --notes-file RELEASE_NOTES_1.1.0.md
```

## Вручную через веб-интерфейс

### Шаг 1: Откройте страницу релизов

Перейдите на: https://github.com/DarkEric/game-planner/releases/new

### Шаг 2: Выберите тег

- **Choose a tag:** `v1.1.0` (должен появиться в списке)
- Или введите `v1.1.0` и выберите "Create new tag: v1.1.0 on publish"

### Шаг 3: Заполните информацию

**Release title:**
```
v1.1.0 - Telegram Notifications
```

**Description:** (скопируйте из RELEASE_NOTES_1.1.0.md)

```markdown
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

### 🎯 Поддержка топиков (Topics)

Отправляйте уведомления в конкретные топики супергрупп Telegram

## 📖 Быстрый старт

### 1. Создайте бота в Telegram (@BotFather)
### 2. Настройте .env:

\`\`\`env
TELEGRAM_BOT_ENABLED=true
TELEGRAM_BOT_TOKEN=ваш_токен
TELEGRAM_BOT_CHAT_ID=ваш_chat_id
TELEGRAM_BOT_THREAD_ID=  # Опционально
FRONTEND_URL=http://localhost:5173
\`\`\`

### 3. Перезапустите:

\`\`\`bash
docker-compose down
docker-compose up -d --build
\`\`\`

## 📚 Документация

- 📖 [TELEGRAM_SETUP.md](https://github.com/DarkEric/game-planner/blob/main/TELEGRAM_SETUP.md) - Подробная инструкция
- ⚡ [TELEGRAM_QUICK_START.md](https://github.com/DarkEric/game-planner/blob/main/TELEGRAM_QUICK_START.md) - Быстрый старт
- 🎯 [docs/TELEGRAM_TOPICS.md](https://github.com/DarkEric/game-planner/blob/main/docs/TELEGRAM_TOPICS.md) - Настройка топиков
- 🔍 [docs/TELEGRAM_DEBUG.md](https://github.com/DarkEric/game-planner/blob/main/docs/TELEGRAM_DEBUG.md) - Отладка

## 🔄 Обновление с 1.0.0

\`\`\`bash
git pull origin main
docker-compose down
docker-compose up -d --build
\`\`\`

## 📊 Изменения

- ✨ Добавлены Telegram уведомления
- 🎯 Поддержка топиков в супергруппах
- 🔧 Улучшенное логирование
- 📚 8 новых документов
- 🐛 Исправления обработки Thread ID

**Full Changelog:** https://github.com/DarkEric/game-planner/blob/main/CHANGELOG.md

---

**Приятного использования! 🎮**
```

### Шаг 4: Настройки релиза

- ✅ **Set as the latest release** - отметьте
- ⬜ **Set as a pre-release** - НЕ отмечайте
- ⬜ **Create a discussion for this release** - по желанию

### Шаг 5: Опубликуйте

Нажмите **"Publish release"**

## Проверка

После публикации проверьте:

1. Релиз появился на https://github.com/DarkEric/game-planner/releases
2. Тег v1.1.0 виден в списке тегов
3. Docker образы начали собираться (если настроен GitHub Actions)

## Docker образы

Если настроен GitHub Actions для публикации образов, они автоматически соберутся и опубликуются в GitHub Container Registry:

- `ghcr.io/darkeric/game-planner-backend:1.1.0`
- `ghcr.io/darkeric/game-planner-backend:latest`
- `ghcr.io/darkeric/game-planner-frontend:1.1.0`
- `ghcr.io/darkeric/game-planner-frontend:latest`

Проверить статус сборки: https://github.com/DarkEric/game-planner/actions

## Готово! 🎉

Релиз v1.1.0 опубликован!

Пользователи могут:
- Скачать исходный код
- Использовать готовые Docker образы
- Прочитать release notes
- Обновиться с версии 1.0.0
