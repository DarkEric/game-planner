# Функция учета часовых поясов игроков

## Обзор

Начиная с версии 1.1, Game Planner поддерживает сохранение и отображение часового пояса каждого игрока. Это позволяет игрокам из разных часовых поясов видеть время друг друга корректно.

## Как это работает

### 1. Сохранение часового пояса

При обновлении профиля игрока автоматически сохраняется его часовой пояс:

```javascript
// Фронтенд автоматически определяет timezone
const timezone = Intl.DateTimeFormat().resolvedOptions().timeZone
// Например: "Europe/Moscow", "America/New_York", "Asia/Tokyo"

// Отправляется на бэкенд при обновлении профиля
await playerApi.updateCurrentPlayer(name, color, timezone)
```

### 2. Хранение в базе данных

```sql
-- Новая колонка в таблице users
ALTER TABLE users ADD COLUMN timezone VARCHAR(50);

-- Примеры значений:
-- "Europe/Moscow"     (UTC+3)
-- "America/New_York"  (UTC-5)
-- "Asia/Tokyo"        (UTC+9)
```

### 3. Отображение времени

Каждый игрок видит время в своем локальном часовом поясе, но может видеть timezone других игроков.

## API

### Получить текущего игрока

```http
GET /api/players/me
Authorization: Bearer {token}
```

**Ответ:**
```json
{
  "id": 1,
  "name": "Игрок 1",
  "color": "#646cff",
  "timezone": "Europe/Moscow",
  "availableTimes": [
    {
      "id": 1,
      "start": "2025-11-18T15:00:00",
      "duration": 2
    }
  ]
}
```

### Обновить профиль

```http
PUT /api/players/me
Authorization: Bearer {token}
Content-Type: application/json

{
  "name": "Новое имя",
  "color": "#ff6464",
  "timezone": "Europe/Moscow"
}
```

### Получить всех игроков

```http
GET /api/players
Authorization: Bearer {token}
```

**Ответ:**
```json
[
  {
    "id": 1,
    "name": "Игрок 1",
    "color": "#646cff",
    "timezone": "Europe/Moscow",
    "availableTimes": [...]
  },
  {
    "id": 2,
    "name": "Игрок 2",
    "color": "#64ff64",
    "timezone": "America/New_York",
    "availableTimes": [...]
  }
]
```

## Использование на фронтенде

### Автоматическое определение timezone

```javascript
import { getUserTimezone } from '../utils/dateUtils'

const tz = getUserTimezone()
console.log(tz)
// {
//   timezone: "Europe/Moscow",
//   offset: 3,
//   offsetStr: "UTC+3"
// }
```

### Обновление профиля с timezone

```javascript
import { playerApi } from '../services/api'

// Timezone определяется автоматически
await playerApi.updateCurrentPlayer('Мое имя', '#646cff')

// Или можно указать явно
await playerApi.updateCurrentPlayer('Мое имя', '#646cff', 'Europe/Moscow')
```

### Отображение timezone игрока

```javascript
// В компоненте
const player = await playerApi.getCurrentPlayer()

console.log(`Игрок: ${player.name}`)
console.log(`Часовой пояс: ${player.timezone || 'Не указан'}`)
```

## Примеры использования

### Пример 1: Игроки в разных часовых поясах

**Игрок 1 (Москва, UTC+3):**
- Выбирает время: 18:00
- Сохраняется: "2025-11-18T18:00:00"
- Timezone: "Europe/Moscow"

**Игрок 2 (Нью-Йорк, UTC-5):**
- Видит время игрока 1 как: 10:00 (его локальное время)
- Выбирает время: 14:00
- Сохраняется: "2025-11-18T14:00:00"
- Timezone: "America/New_York"

**Игрок 1 видит:**
- Свое время: 18:00
- Время игрока 2: 22:00 (конвертировано в его timezone)

### Пример 2: Отображение информации о timezone

```javascript
// В компоненте списка игроков
players.map(player => (
  <div key={player.id}>
    <span>{player.name}</span>
    {player.timezone && (
      <span className="timezone-badge">
        {player.timezone}
      </span>
    )}
  </div>
))
```

## Миграция существующих данных

Для существующих пользователей без timezone:

1. При первом обновлении профиля timezone будет установлен автоматически
2. Или можно запустить миграцию для установки timezone по умолчанию:

```sql
-- Установить UTC для всех пользователей без timezone
UPDATE users 
SET timezone = 'UTC' 
WHERE timezone IS NULL;
```

## Поддерживаемые форматы timezone

Используются IANA timezone идентификаторы:

- **Европа:** `Europe/Moscow`, `Europe/London`, `Europe/Paris`
- **Америка:** `America/New_York`, `America/Los_Angeles`, `America/Chicago`
- **Азия:** `Asia/Tokyo`, `Asia/Shanghai`, `Asia/Dubai`
- **Австралия:** `Australia/Sydney`, `Australia/Melbourne`
- **UTC:** `UTC`

Полный список: https://en.wikipedia.org/wiki/List_of_tz_database_time_zones

## Будущие улучшения

### Планируется:

1. **Конвертация времени в UI**
   - Показывать время других игроков в их timezone
   - Tooltip с информацией о разнице во времени

2. **Умные предложения времени**
   - Учитывать timezone при поиске лучших слотов
   - Предлагать время, удобное для всех timezone

3. **Визуализация timezone**
   - Показывать текущее время каждого игрока
   - Индикатор "день/ночь" для каждого игрока

4. **Настройки отображения**
   - Выбор: показывать время в своем timezone или в timezone игрока
   - Формат отображения времени (12/24 часа)

## Troubleshooting

### Timezone не сохраняется

**Проблема:** После обновления профиля timezone остается null

**Решение:**
1. Проверьте, что миграция 006 применена:
```bash
docker-compose exec backend ./gradlew liquibaseStatus
```

2. Проверьте логи:
```bash
docker-compose logs backend | grep timezone
```

3. Проверьте, что фронтенд отправляет timezone:
```javascript
// В DevTools Console
const tz = Intl.DateTimeFormat().resolvedOptions().timeZone
console.log('My timezone:', tz)
```

### Неправильный timezone

**Проблема:** Timezone определяется неправильно

**Причина:** Настройки браузера или системы

**Решение:**
1. Проверьте системные настройки времени
2. Проверьте настройки браузера
3. Можно указать timezone вручную при обновлении профиля

---

Для получения помощи см. [TROUBLESHOOTING.md](TROUBLESHOOTING.md)
