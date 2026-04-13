# Game Planner Backend

Backend приложение для планирования D&D игр на Spring Boot.

## Требования

- Java 21
- PostgreSQL 12+
- Gradle 7+

## Настройка базы данных

1. Создайте базу данных PostgreSQL:
```sql
CREATE DATABASE game_planner;
```

1. Обновите настройки подключения в `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/game_planner
spring.datasource.username=your_username
spring.datasource.password=your_password
```

## Запуск приложения

```bash
./gradlew bootRun
```

Или через IDE запустите класс `GamePlannerBackApplication`.

Приложение будет доступно по адресу: http://localhost:8080

## API Endpoints

### Игроки

- `GET /api/players` - Получить всех игроков
- `GET /api/players/{id}` - Получить игрока по ID
- `POST /api/players` - Создать нового игрока
  ```json
  {
    "name": "Имя игрока",
    "color": "#646cff"
  }
  ```
- `DELETE /api/players/{id}` - Удалить игрока

### Временные слоты (текущий игрок, JWT)

- `POST /api/players/me/time-slots/add-batch` — добавить слоты (тело `{ "slots": [ { "start": "...", "duration": 1 } ] }`).
- `POST /api/players/me/time-slots/remove-batch` — удалить слоты (то же тело).
- `DELETE /api/players/me/time-slots` — удалить все слоты текущего игрока.

Также: `POST /api/players/{playerId}/time-slots` — добавить один слот; `DELETE /api/players/{playerId}/time-slots?start=...` — удалить по времени начала.

## Структура проекта

- `entity/` - JPA сущности (Player, TimeSlot)
- `repository/` - Репозитории для работы с БД
- `service/` - Бизнес-логика
- `controller/` - REST контроллеры
- `dto/` - Data Transfer Objects для API
- `resources/db/changelog/` - Liquibase миграции

## Миграции базы данных

Миграции выполняются автоматически при запуске приложения через Liquibase.

