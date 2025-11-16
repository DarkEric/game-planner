# Обработка часовых поясов в Game Planner

## Проблема

При работе с датами и временем в веб-приложениях возникает проблема часовых поясов:
- Пользователи могут находиться в разных часовых поясах
- Бэкенд хранит время в базе данных
- Фронтенд должен отображать время в локальном часовом поясе пользователя

## Решение

### Архитектура

1. **База данных (PostgreSQL)**: Хранит `TIMESTAMP WITHOUT TIME ZONE`
2. **Бэкенд (Java/Spring Boot)**: Использует `LocalDateTime` (без timezone)
3. **API**: Передает даты в формате ISO без timezone (`2025-11-15T15:00:00`)
4. **Фронтенд (React)**: Интерпретирует даты как локальное время пользователя

### Принцип работы

```
Пользователь выбирает: "15 ноября 2025, 15:00"
                ↓
Фронтенд отправляет: "2025-11-15T15:00:00" (без timezone)
                ↓
Бэкенд сохраняет: LocalDateTime(2025, 11, 15, 15, 0, 0)
                ↓
База данных: TIMESTAMP '2025-11-15 15:00:00' (без timezone)
                ↓
Бэкенд возвращает: "2025-11-15T15:00:00"
                ↓
Фронтенд парсит: Date в локальном времени пользователя
                ↓
Отображается: "15 ноября 2025, 15:00" (в локальном времени)
```

### Важно!

**Все время хранится и передается БЕЗ информации о часовом поясе.**

Это означает:
- Если пользователь в Москве (UTC+3) выбирает 15:00, сохраняется 15:00
- Если пользователь в Лондоне (UTC+0) выбирает 15:00, сохраняется 15:00
- Каждый пользователь видит время в своем локальном часовом поясе

## Использование в коде

### Парсинг дат из API

```javascript
import { parseLocalDateTime } from '../utils/dateUtils'

// Получаем от API
const apiResponse = {
  start: "2025-11-15T15:00:00"
}

// Парсим в локальное время
const date = parseLocalDateTime(apiResponse.start)
// date будет интерпретирована как 15:00 в локальном времени пользователя
```

### Отправка дат на API

```javascript
import { formatLocalDateTime } from '../utils/dateUtils'

// Пользователь выбрал дату
const selectedDate = new Date(2025, 10, 15, 15, 0, 0) // 15 ноября 2025, 15:00

// Форматируем для отправки
const formatted = formatLocalDateTime(selectedDate)
// "2025-11-15T15:00:00"

// Отправляем на API
await api.post('/time-slots', { start: formatted })
```

### Отладка

```javascript
import { debugDate, getUserTimezone } from '../utils/dateUtils'

// Получить информацию о timezone пользователя
const tz = getUserTimezone()
console.log(tz)
// { timezone: "Europe/Moscow", offset: 3, offsetStr: "UTC+3" }

// Отладка даты
const date = new Date()
debugDate('Текущая дата', date)
// Выведет подробную информацию о дате
```

## Тестирование

### Проверка в разных часовых поясах

1. Откройте DevTools
2. Перейдите в Settings → Sensors
3. Измените Location на другой город
4. Перезагрузите страницу
5. Проверьте, что время отображается корректно

### Ручное тестирование

```javascript
// В консоли браузера
import { parseLocalDateTime, formatLocalDateTime } from './utils/dateUtils'

// Тест парсинга
const parsed = parseLocalDateTime("2025-11-15T15:00:00")
console.log(parsed.toString()) // Должно показать 15:00 в вашем локальном времени

// Тест форматирования
const date = new Date(2025, 10, 15, 15, 0, 0)
console.log(formatLocalDateTime(date)) // "2025-11-15T15:00:00"
```

## Известные проблемы и решения

### Проблема: Время сдвигается на несколько часов

**Причина**: Использование `new Date(dateString)` с ISO строкой, содержащей 'Z' или timezone

**Решение**: Всегда используйте `parseLocalDateTime()` для парсинга дат из API

### Проблема: Разные пользователи видят разное время

**Это нормально!** Каждый пользователь видит время в своем локальном часовом поясе.

Если нужно показывать одинаковое время всем:
1. Храните timezone вместе с датой
2. Конвертируйте в нужный timezone при отображении
3. Используйте библиотеку типа `date-fns-tz` или `luxon`

### Проблема: Летнее/зимнее время (DST)

JavaScript автоматически учитывает переход на летнее/зимнее время для локального часового пояса пользователя.

## Best Practices

1. **Всегда используйте утилиты из `dateUtils.js`**
   ```javascript
   // ✅ Правильно
   import { parseLocalDateTime } from '../utils/dateUtils'
   const date = parseLocalDateTime(apiDate)
   
   // ❌ Неправильно
   const date = new Date(apiDate)
   ```

2. **Не используйте `toISOString()`** для отправки на бэкенд
   ```javascript
   // ✅ Правильно
   import { formatLocalDateTime } from '../utils/dateUtils'
   const formatted = formatLocalDateTime(date)
   
   // ❌ Неправильно (добавит 'Z' и timezone)
   const formatted = date.toISOString()
   ```

3. **Используйте `toDateString()` для сравнения дат**
   ```javascript
   // ✅ Правильно
   if (date1.toDateString() === date2.toDateString()) {
     // Один день
   }
   
   // ❌ Неправильно (сравнивает миллисекунды)
   if (date1 === date2) {
     // Никогда не сработает
   }
   ```

4. **Добавляйте отладочные логи при проблемах**
   ```javascript
   import { debugDate } from '../utils/dateUtils'
   
   debugDate('Дата из API', parsedDate)
   debugDate('Дата для отправки', dateToSend)
   ```

## Дополнительные ресурсы

- [MDN: Date](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Date)
- [Java LocalDateTime](https://docs.oracle.com/javase/8/docs/api/java/time/LocalDateTime.html)
- [PostgreSQL TIMESTAMP](https://www.postgresql.org/docs/current/datatype-datetime.html)
