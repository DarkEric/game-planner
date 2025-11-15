# Game Planner

Приложение для планирования игровых сессий с друзьями.

## Возможности

- Регистрация и авторизация пользователей
- Управление доступностью по времени
- Просмотр лучших временных слотов для игры
- Календарь с визуализацией доступности всех игроков

## Технологии

### Backend
- Java 21
- Spring Boot 3.5
- PostgreSQL 17
- JWT Authentication
- Liquibase

### Frontend
- React 18
- Vite
- CSS3

## Быстрый запуск с Docker

### Предварительные требования
- Docker Desktop (Windows/Mac) или Docker Engine + Docker Compose (Linux)

### Автоматический запуск

**Windows:**
```bash
start.bat
```

**Linux/Mac:**
```bash
chmod +x start.sh
./start.sh
```

### Ручной запуск

1. Создайте `.env` файл (опционально):
```bash
cp .env.example .env
```

2. Запустите приложение:
```bash
docker-compose up -d --build
```

3. Приложение будет доступно:
   - **Фронтенд**: http://localhost
   - **Backend API**: http://localhost:8080
   - **PostgreSQL**: localhost:5432

### Управление

**Просмотр логов:**
```bash
docker-compose logs -f
```

**Просмотр логов конкретного сервиса:**
```bash
docker-compose logs -f backend
docker-compose logs -f frontend
docker-compose logs -f postgres
```

**Остановка:**
```bash
docker-compose down
```

**Остановка с удалением данных:**
```bash
docker-compose down -v
```

**Перезапуск:**
```bash
docker-compose restart
```

## Разработка

### Backend

```bash
cd game-planner-back
./gradlew bootRun
```

### Frontend

```bash
cd game-planer-front
npm install
npm run dev
```

## API Endpoints

### Аутентификация
- `POST /api/auth/register` - Регистрация
- `POST /api/auth/login` - Вход

### Игроки
- `GET /api/players` - Список всех игроков
- `GET /api/players/me` - Текущий пользователь
- `PUT /api/players/me` - Обновить профиль

### Временные слоты
- `POST /api/players/me/time-slots/toggle` - Переключить слот
- `POST /api/players/me/time-slots/toggle-batch` - Переключить несколько слотов

## Лицензия

MIT
