# 🎲 Game Planner

[English version](README.en.md) | Русская версия | **[📖 Документация](docs/INDEX.md)**

Веб-приложение для планирования настольных игр с друзьями. Отмечайте свободное время, находите общие слоты и планируйте игры вместе!

> **Новичок?** Начните с [простого гайда](docs/guides/SIMPLE_GUIDE.md) - всё объяснено простым языком!  
> **Вся документация:** [docs/INDEX.md](docs/INDEX.md)

## ✨ Возможности

- 📅 **Интерактивный календарь** - отмечайте доступное время drag-and-drop
- 🎯 **Умный поиск слотов** - автоматический поиск лучшего времени для всех
- 🎮 **Планирование игр** - создавайте игры с названием, описанием и участниками
- 🌍 **Поддержка часовых поясов** - каждый видит время в своем часовом поясе
- 🔐 **Система инвайтов** - регистрация только по приглашениям
- 📱 **Telegram уведомления** - получайте уведомления о новых играх (опционально)
- 🎨 **Темная тема** - приятный интерфейс для глаз
- 📱 **Адаптивный дизайн** - работает на любых экранах

<img width="2510" height="1289" alt="image" src="https://github.com/user-attachments/assets/261ebd6a-3faa-4a38-95a7-9da5c0e0cff5" />
<img width="2506" height="1283" alt="image" src="https://github.com/user-attachments/assets/9d58d6fb-a505-4e70-b1ae-91732154d633" />
<img width="2510" height="1289" alt="image" src="https://github.com/user-attachments/assets/6536e76c-2ad7-45ef-a313-37942b8b471d" />

## 🚀 Быстрый старт

### Требования

- Docker и Docker Compose
- Git

### Вариант 1: Готовые Docker образы (Рекомендуется)

Используйте предсобранные образы из GitHub Container Registry:

```bash
git clone https://github.com/DarkEric/game-planner.git
cd game-planner
copy .env.example .env
docker-compose -f docker-compose.ghcr.yml up -d
```

### Вариант 2: Сборка из исходников

1. Клонируйте репозиторий:
```bash
git clone https://github.com/DarkEric/game-planner.git
cd game-planner
```

2. Создайте файл `.env` из примера:
```bash
copy .env.example .env
```

3. Запустите приложение:
```bash
# Windows
start.bat

# Linux/Mac
docker-compose up -d
```

4. Откройте в браузере: http://localhost

📦 **Подробнее о Docker образах:** [DOCKER_IMAGES.md](DOCKER_IMAGES.md)

### Первый запуск

При первом запуске используйте инвайт-код для регистрации:
```
FIRST-USER-INVITE-2025
```

После регистрации вы сможете создавать новые инвайт-коды для друзей.

## 📖 Использование

### Отметка доступного времени

1. Войдите в систему
2. Кликните или протяните мышью по ячейкам календаря
3. Ваше время автоматически сохранится

### Планирование игры

1. Нажмите кнопку "🎲 Запланировать игру"
2. Выберите время из топ-10 лучших слотов или укажите вручную
3. Добавьте название и описание (опционально)
4. Игра создастся с участниками, у которых есть доступность на это время

### Управление инвайтами

1. В разделе "Мои инвайт-коды" нажмите "+ Создать инвайт"
2. Скопируйте код и отправьте другу
3. Каждый инвайт одноразовый

## 🛠 Технологии

### Backend
- Java 17 + Spring Boot
- PostgreSQL
- Liquibase для миграций
- JWT аутентификация

### Frontend
- React 18
- Vite
- CSS Modules

### Deployment
- Docker & Docker Compose
- Caddy (автоматический HTTPS)

## 📁 Структура проекта

```
game-planner/
├── game-planner-back/     # Backend (Spring Boot)
├── game-planer-front/     # Frontend (React)
├── docker-compose.yml     # Development setup
├── docker-compose.prod.yml # Production setup
├── start.bat              # Windows launcher
└── README.md
```

## 🔧 Конфигурация

### Переменные окружения

Создайте `.env` файл в корне проекта:

```env
# Database
POSTGRES_DB=game_planner
POSTGRES_USER=postgres
POSTGRES_PASSWORD=your_secure_password

# Backend
JWT_SECRET=your_jwt_secret_key_here
SPRING_PROFILES_ACTIVE=prod

# Frontend
VITE_API_URL=http://localhost:8080
```

### Production deployment

Для production используйте `docker-compose.prod.yml`:

```bash
docker-compose -f docker-compose.prod.yml up -d
```

Подробнее см. [docs/setup/PRODUCTION_SETUP.md](docs/setup/PRODUCTION_SETUP.md)

## � Telebgram уведомления

Game Planner поддерживает отправку уведомлений в Telegram при создании новых игр.

### Быстрая настройка

1. Создайте бота через [@BotFather](https://t.me/BotFather)
2. Получите токен и Chat ID
3. Добавьте в `.env`:
```env
TELEGRAM_BOT_ENABLED=true
TELEGRAM_BOT_TOKEN=ваш_токен
TELEGRAM_BOT_CHAT_ID=ваш_chat_id
FRONTEND_URL=http://localhost:5173
```
4. Перезапустите приложение

📖 **Подробная инструкция:** [docs/telegram/TELEGRAM_SETUP.md](docs/telegram/TELEGRAM_SETUP.md)

## 🐛 Troubleshooting

См. [docs/setup/TROUBLESHOOTING.md](docs/setup/TROUBLESHOOTING.md)

## 📝 Лицензия

MIT License

## 👥 Автор

DarkEric

## 🤝 Вклад

Pull requests приветствуются! Для больших изменений сначала откройте issue.
