# API Usage Guide

## Работа с временными слотами

### Переключение одного слота

```javascript
import { playerApi } from './services/api'

// Переключить один временной слот
const date = new Date(2025, 10, 18, 15, 0, 0) // 18 ноября 2025, 15:00
const duration = 2 // 2 часа

const updatedPlayer = await playerApi.toggleTimeSlot(date, duration)
```

### Массовое переключение слотов

```javascript
import { playerApi } from './services/api'

// Подготовить массив слотов
const slots = [
  { date: new Date(2025, 10, 18), hour: 15 },
  { date: new Date(2025, 10, 18), hour: 16 },
  { date: new Date(2025, 10, 18), hour: 17 }
]

// Вариант 1: Все слоты с одинаковой длительностью
const duration = 1 // 1 час для каждого слота
const updatedPlayer = await playerApi.toggleTimeSlots(slots, duration)

// Вариант 2: Каждый слот со своей длительностью
const slotsWithDuration = [
  { date: new Date(2025, 10, 18), hour: 15, duration: 2 }, // 2 часа
  { date: new Date(2025, 10, 18), hour: 17, duration: 1 }, // 1 час
  { date: new Date(2025, 10, 19), hour: 10, duration: 3 }  // 3 часа
]
const updatedPlayer2 = await playerApi.toggleTimeSlots(slotsWithDuration)
```

### Параметры

#### `toggleTimeSlot(start, duration)`

- **start** (Date, required) - Дата и время начала слота
- **duration** (Number, optional, default: 1) - Длительность в часах

**Возвращает:** Promise<PlayerDto>

#### `toggleTimeSlots(slots, duration)`

- **slots** (Array, required) - Массив объектов слотов
  - **date** (Date, required) - Дата слота
  - **hour** (Number, required) - Час начала (0-23)
  - **duration** (Number, optional) - Длительность конкретного слота в часах
- **duration** (Number, optional, default: 1) - Длительность по умолчанию для всех слотов

**Возвращает:** Promise<PlayerDto>

**Примечание:** Если у слота указана своя `duration`, она имеет приоритет над общей `duration`.

## Работа с профилем

### Получить текущего игрока

```javascript
const player = await playerApi.getCurrentPlayer()
console.log(player)
// {
//   id: 1,
//   name: "Игрок 1",
//   color: "#646cff",
//   timezone: "Europe/Moscow",
//   availableTimes: [...]
// }
```

### Обновить профиль

```javascript
// Обновить имя и цвет
await playerApi.updateCurrentPlayer('Новое имя', '#ff6464')

// Обновить с явным указанием timezone
await playerApi.updateCurrentPlayer('Новое имя', '#ff6464', 'America/New_York')

// Timezone определяется автоматически, если не указан
await playerApi.updateCurrentPlayer('Новое имя', '#ff6464')
// Автоматически использует: Intl.DateTimeFormat().resolvedOptions().timeZone
```

### Получить всех игроков

```javascript
const players = await playerApi.getAllPlayers()
console.log(players)
// [
//   { id: 1, name: "Игрок 1", color: "#646cff", timezone: "Europe/Moscow", ... },
//   { id: 2, name: "Игрок 2", color: "#64ff64", timezone: "America/New_York", ... }
// ]
```

## Работа с датами

### Парсинг дат из API

```javascript
import { parseLocalDateTime } from './utils/dateUtils'

// API возвращает: "2025-11-18T15:00:00"
const dateStr = "2025-11-18T15:00:00"
const date = parseLocalDateTime(dateStr)
// Date объект в локальном времени пользователя
```

### Форматирование дат для API

```javascript
import { formatLocalDateTime } from './utils/dateUtils'

const date = new Date(2025, 10, 18, 15, 0, 0)
const formatted = formatLocalDateTime(date)
// "2025-11-18T15:00:00"
```

### Получение timezone пользователя

```javascript
import { getUserTimezone } from './utils/dateUtils'

const tz = getUserTimezone()
console.log(tz)
// {
//   timezone: "Europe/Moscow",
//   offset: 3,
//   offsetStr: "UTC+3"
// }
```

## Примеры использования в компонентах

### Пример 1: Переключение одного слота при клике

```javascript
const handleTimeSlotClick = async (date, hour) => {
  const slotDate = new Date(date)
  slotDate.setHours(hour, 0, 0, 0)
  
  try {
    const updatedPlayer = await playerApi.toggleTimeSlot(slotDate, 1)
    setCurrentPlayer(updatedPlayer)
  } catch (error) {
    console.error('Failed to toggle slot:', error)
  }
}
```

### Пример 2: Массовое переключение при drag selection

```javascript
const handleTimeSlotsSelect = async (slots) => {
  if (!slots || slots.length === 0) return
  
  try {
    // Все слоты будут иметь длительность 1 час
    const updatedPlayer = await playerApi.toggleTimeSlots(slots, 1)
    setCurrentPlayer(updatedPlayer)
  } catch (error) {
    console.error('Failed to toggle slots:', error)
  }
}
```

### Пример 3: Создание слотов с разной длительностью

```javascript
const createCustomSlots = async () => {
  const slots = [
    // Понедельник 15:00-17:00 (2 часа)
    { date: new Date(2025, 10, 18), hour: 15, duration: 2 },
    
    // Вторник 10:00-13:00 (3 часа)
    { date: new Date(2025, 10, 19), hour: 10, duration: 3 },
    
    // Среда 18:00-19:00 (1 час)
    { date: new Date(2025, 10, 20), hour: 18, duration: 1 }
  ]
  
  try {
    const updatedPlayer = await playerApi.toggleTimeSlots(slots)
    setCurrentPlayer(updatedPlayer)
  } catch (error) {
    console.error('Failed to create slots:', error)
  }
}
```

## Обработка ошибок

### Стандартная обработка

```javascript
try {
  const player = await playerApi.getCurrentPlayer()
  // Успех
} catch (error) {
  if (error.message === 'Failed to fetch current player') {
    // Обработка ошибки загрузки
  }
  console.error('Error:', error)
}
```

### Обработка ошибок авторизации

API автоматически обрабатывает ошибки 401/403:
- Удаляет токен из localStorage
- Перезагружает страницу (пользователь увидит форму входа)

```javascript
// Это происходит автоматически
const handleAuthError = (response) => {
  if (response.status === 401 || response.status === 403) {
    removeToken()
    window.location.reload()
  }
}
```

## Best Practices

### 1. Всегда используйте утилиты для работы с датами

```javascript
// ✅ Правильно
import { parseLocalDateTime, formatLocalDateTime } from './utils/dateUtils'
const date = parseLocalDateTime(apiDate)
const formatted = formatLocalDateTime(date)

// ❌ Неправильно
const date = new Date(apiDate) // Может неправильно интерпретировать timezone
const formatted = date.toISOString() // Добавит 'Z' и timezone
```

### 2. Проверяйте данные перед отправкой

```javascript
// ✅ Правильно
const handleToggle = async (slots) => {
  if (!slots || slots.length === 0) {
    console.warn('No slots to toggle')
    return
  }
  
  const updatedPlayer = await playerApi.toggleTimeSlots(slots)
  // ...
}

// ❌ Неправильно
const handleToggle = async (slots) => {
  const updatedPlayer = await playerApi.toggleTimeSlots(slots) // Может упасть
}
```

### 3. Обновляйте локальное состояние после изменений

```javascript
// ✅ Правильно
const updatedPlayer = await playerApi.toggleTimeSlot(date, duration)
setCurrentPlayer(updatedPlayer)
setAllPlayers(allPlayers.map(p => 
  p.id === updatedPlayer.id ? updatedPlayer : p
))

// ❌ Неправильно
await playerApi.toggleTimeSlot(date, duration)
// Состояние не обновлено, UI не синхронизирован с сервером
```

### 4. Используйте try-catch для всех API вызовов

```javascript
// ✅ Правильно
try {
  const player = await playerApi.getCurrentPlayer()
  setPlayer(player)
} catch (error) {
  setError('Не удалось загрузить данные')
  console.error(error)
}

// ❌ Неправильно
const player = await playerApi.getCurrentPlayer() // Необработанная ошибка
setPlayer(player)
```

## Дополнительные ресурсы

- [TIMEZONE_HANDLING.md](TIMEZONE_HANDLING.md) - Работа с часовыми поясами
- [dateUtils.js](src/utils/dateUtils.js) - Утилиты для работы с датами
- [api.js](src/services/api.js) - Исходный код API
