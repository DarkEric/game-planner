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

### Временные слоты

- `POST /api/players/{id}/time-slots/toggle` - Переключить временной слот (добавить/удалить)
  ```json
  {
    "start": "2024-12-15T10:00:00",
    "duration": 1
  }
  ```
  Возвращает обновленного игрока со всеми временными слотами.

- `POST /api/players/{id}/time-slots/toggle-batch` - Переключить несколько временных слотов (для массового выбора)
  ```json
  {
    "slots": [
      {
        "start": "2024-12-15T10:00:00",
        "duration": 1
      },
      {
        "start": "2024-12-15T11:00:00",
        "duration": 1
      }
    ]
  }
  ```
  Возвращает обновленного игрока со всеми временными слотами.

## Структура проекта

- `entity/` - JPA сущности (Player, TimeSlot)
- `repository/` - Репозитории для работы с БД
- `service/` - Бизнес-логика
- `controller/` - REST контроллеры
- `dto/` - Data Transfer Objects для API
- `resources/db/changelog/` - Liquibase миграции

## Миграции базы данных

Миграции выполняются автоматически при запуске приложения через Liquibase.

