# Game Planner

Приложение для планирования игровых сессий с друзьями.

> 📖 **[Быстрый старт →](QUICK_START.md)** | 🔧 **[Интеграция с Caddy →](CADDY_INTEGRATION.md)** | 🚀 **[Production Setup →](PRODUCTION_SETUP.md)** | 🔍 **[Troubleshooting →](TROUBLESHOOTING.md)**
> 
> 📚 **[API Usage →](game-planer-front/API_USAGE.md)** | ⏱️ **[Duration Feature →](DURATION_FEATURE_SUMMARY.md)** | 🎲 **[Game Scheduling →](GAME_SCHEDULING_FEATURE.md)** | 🧹 **[Cleanup →](CLEANUP_FEATURE.md)** | 🔐 **[Invite System →](INVITE_SYSTEM.md)** | ⚡ **[API Optimization →](API_OPTIMIZATION.md)**

## Возможности

- 👥 Регистрация и авторизация пользователей
- 📅 Управление доступностью по времени
- 🎲 **Планирование игр** с выбором даты, времени и участников
- 📝 **Запись на игры** с автоматическим проставлением доступности
- ⭐ Просмотр топ-10 лучших временных слотов для игры
- 🗓️ Календарь с визуализацией доступности всех игроков и запланированных игр
- 🔄 Автоматическое объединение последовательных временных слотов
- 🌍 Поддержка часовых поясов с автоматической конвертацией времени
- 🧹 Автоматическая очистка устаревших данных

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

## Особенности

### Работа с часовыми поясами

**Текущая реализация (v2.0):**
- ✅ Время хранится в UTC на сервере
- ✅ Автоматическая конвертация между часовыми поясами
- ✅ Каждый пользователь видит время в своем часовом поясе
- ✅ UI для выбора и смены часового пояса
- ✅ Корректное отображение времени других игроков

Подробнее: 
- [game-planer-front/TIMEZONE_HANDLING.md](game-planer-front/TIMEZONE_HANDLING.md) - Техническая документация
- [TIMEZONE_FEATURE.md](TIMEZONE_FEATURE.md) - Функция учета часовых поясов
- [game-planer-front/TIMEZONE_SELECTOR_GUIDE.md](game-planer-front/TIMEZONE_SELECTOR_GUIDE.md) - Руководство по выбору timezone

### Планирование игр

**Возможности:**
- 🎲 Создание игр с выбором даты и времени
- 📊 Топ-10 лучших временных слотов с максимальным количеством доступных игроков
- 📝 Запись на игры с автоматическим проставлением доступности
- 👥 Просмотр списка участников игры
- 🚪 Выход из игры или удаление (для создателя)
- 📅 Визуализация игр в календаре

Подробнее: [GAME_SCHEDULING_FEATURE.md](GAME_SCHEDULING_FEATURE.md)

## Быстрый запуск с Docker

### Предварительные требования
- Docker Desktop (Windows/Mac) или Docker Engine + Docker Compose (Linux)

### Вариант 1: Со встроенным Caddy (рекомендуется)

**Автоматический запуск:**

Windows:
```bash
start.bat
```

Linux/Mac:
```bash
chmod +x start.sh
./start.sh
```

**Ручной запуск:**

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
   - **Backend API**: http://localhost:8080 (через Caddy: http://localhost/api)
   - **PostgreSQL**: localhost:5432

### Вариант 2: С внешним Caddy

Если у вас уже запущен Caddy на хосте:

1. Запустите приложение без встроенного Caddy:
```bash
docker-compose -f docker-compose.external-caddy.yml up -d --build
```

2. Добавьте конфигурацию в ваш Caddyfile (см. `Caddyfile.external`)

3. Перезагрузите Caddy:
```bash
caddy reload
```

4. Приложение будет доступно:
   - **Фронтенд**: http://localhost:3000 (напрямую) или через ваш Caddy
   - **Backend API**: http://localhost:8080 (напрямую) или через ваш Caddy
   - **PostgreSQL**: доступна только внутри Docker сети (безопасность)

**Примечание:** Порты можно настроить через `.env`:
```bash
BACKEND_PORT=8081
FRONTEND_PORT=3001
```

### Вариант 3: Production с автоматическим HTTPS

1. Настройте `.env`:
```bash
DOMAIN=your-domain.com
```

2. Запустите:
```bash
docker-compose -f docker-compose.prod.yml up -d --build
```

Caddy автоматически получит SSL сертификат от Let's Encrypt

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

**Перезагрузка Caddy конфигурации:**
```bash
docker-compose exec caddy caddy reload --config /etc/caddy/Caddyfile
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
- `PUT /api/players/me` - Обновить профиль (имя, цвет, timezone)

### Временные слоты
- `POST /api/players/me/time-slots/toggle` - Переключить слот
- `POST /api/players/me/time-slots/toggle-batch` - Переключить несколько слотов

### Игры
- `POST /api/games` - Создать игру
- `GET /api/games?startDate=...&endDate=...` - Получить игры за период
- `GET /api/games/my` - Получить мои предстоящие игры
- `POST /api/games/{gameId}/join` - Записаться на игру
- `POST /api/games/{gameId}/leave` - Покинуть игру
- `DELETE /api/games/{gameId}` - Удалить игру (только создатель)

## Лицензия

MIT
