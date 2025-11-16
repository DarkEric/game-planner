# 🎲 Game Planner

[English version](README.en.md) | Русская версия

Веб-приложение для планирования настольных игр с друзьями. Отмечайте свободное время, находите общие слоты и планируйте игры вместе!

## ✨ Возможности

- 📅 **Интерактивный календарь** - отмечайте доступное время drag-and-drop
- 🎯 **Умный поиск слотов** - автоматический поиск лучшего времени для всех
- 🎮 **Планирование игр** - создавайте игры с названием, описанием и участниками
- 🌍 **Поддержка часовых поясов** - каждый видит время в своем часовом поясе
- 🔐 **Система инвайтов** - регистрация только по приглашениям
- 🎨 **Темная тема** - приятный интерфейс для глаз
- 📱 **Адаптивный дизайн** - работает на любых экранах

## 🚀 Быстрый старт

### Требования

- Docker и Docker Compose
- Git

### Установка

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

Подробнее см. [PRODUCTION_SETUP.md](PRODUCTION_SETUP.md)

## 🐛 Troubleshooting

См. [TROUBLESHOOTING.md](TROUBLESHOOTING.md)

## 📝 Лицензия

MIT License

## 👥 Автор

DarkEric

## 🤝 Вклад

Pull requests приветствуются! Для больших изменений сначала откройте issue.
