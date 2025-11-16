# Duration Feature - Summary

## Что было сделано

### Проблема
При массовом выборе временных слотов (drag selection) duration всегда была жестко закодирована как `1` час, что не позволяло создавать слоты с разной длительностью.

### Решение
Добавлена поддержка гибкой настройки duration для массового выбора слотов.

## Изменения

### Frontend

#### 1. `game-planer-front/src/services/api.js`

**Было:**
```javascript
async toggleTimeSlots(slots) {
  const slotsData = slots.map(slot => ({
    start: formatLocalDateTime(slotDate),
    duration: 1  // Всегда 1!
  }))
}
```

**Стало:**
```javascript
async toggleTimeSlots(slots, duration = 1) {
  const slotsData = slots.map(slot => ({
    start: formatLocalDateTime(slotDate),
    duration: slot.duration || duration  // Гибкая настройка
  }))
}
```

**Приоритет:**
1. `slot.duration` - индивидуальная duration для конкретного слота
2. `duration` - общая duration для всех слотов
3. `1` - значение по умолчанию

#### 2. `game-planer-front/src/App.jsx`

**Было:**
```javascript
const handleTimeSlotsSelect = async (slots) => {
  const updatedPlayer = await playerApi.toggleTimeSlots(slots)
}
```

**Стало:**
```javascript
const handleTimeSlotsSelect = async (slots, duration = 1) => {
  const updatedPlayer = await playerApi.toggleTimeSlots(slots, duration)
}
```

### Документация

Созданы следующие документы:

1. **API_USAGE.md** - Полное руководство по использованию API
   - Примеры для всех сценариев
   - Best practices
   - Обработка ошибок

2. **TESTING_DURATION.md** - Детальные сценарии тестирования
   - Ручное тестирование
   - Проверка в DevTools
   - Проверка в БД
   - Автоматизированные тесты

3. **HOW_TO_TEST_DURATION.md** - Быстрый гайд по тестированию
   - 3 способа тестирования
   - Что проверять
   - Troubleshooting

4. **DurationTest.jsx** - Тестовый компонент
   - 3 готовых теста
   - UI панель для быстрого тестирования
   - Отображение результатов

5. **DURATION_FEATURE_CHECKLIST.md** - Чеклист для проверки
   - Реализация
   - Тестирование
   - Edge cases
   - Production ready

## Использование

### Вариант 1: Все слоты с одинаковой duration

```javascript
const slots = [
  { date: new Date(2025, 10, 18), hour: 15 },
  { date: new Date(2025, 10, 18), hour: 16 }
]

// Все слоты будут иметь duration = 2 часа
await playerApi.toggleTimeSlots(slots, 2)
```

### Вариант 2: Каждый слот со своей duration

```javascript
const slots = [
  { date: new Date(2025, 10, 18), hour: 15, duration: 2 }, // 2 часа
  { date: new Date(2025, 10, 18), hour: 17, duration: 1 }, // 1 час
  { date: new Date(2025, 10, 19), hour: 10, duration: 3 }  // 3 часа
]

// Каждый слот использует свою duration
await playerApi.toggleTimeSlots(slots)
```

### Вариант 3: Комбинированный

```javascript
const slots = [
  { date: new Date(2025, 10, 18), hour: 15, duration: 3 }, // 3 часа (свое значение)
  { date: new Date(2025, 10, 18), hour: 18 },              // 2 часа (общее значение)
  { date: new Date(2025, 10, 19), hour: 10 }               // 2 часа (общее значение)
]

// Первый слот: 3 часа, остальные: 2 часа
await playerApi.toggleTimeSlots(slots, 2)
```

## API Request/Response

### Request

```http
POST /api/players/me/time-slots/toggle-batch
Authorization: Bearer {token}
Content-Type: application/json

{
  "slots": [
    {
      "start": "2025-11-18T15:00:00",
      "duration": 2
    },
    {
      "start": "2025-11-18T17:00:00",
      "duration": 1
    }
  ]
}
```

### Response

```json
{
  "id": 1,
  "name": "Игрок",
  "color": "#646cff",
  "timezone": "Europe/Moscow",
  "availableTimes": [
    {
      "id": 1,
      "start": "2025-11-18T15:00:00",
      "duration": 2
    },
    {
      "id": 2,
      "start": "2025-11-18T17:00:00",
      "duration": 1
    }
  ]
}
```

## Тестирование

### Быстрый тест в Console

```javascript
// 1. Откройте DevTools → Console
// 2. Выполните:

const { playerApi } = await import('./services/api.js')

// Тест с разными duration
const result = await playerApi.toggleTimeSlots([
  { date: new Date(2025, 10, 18), hour: 15, duration: 2 },
  { date: new Date(2025, 10, 18), hour: 17, duration: 3 }
])

console.log('Result:', result)
```

### Использование тестового компонента

1. Добавьте в `App.jsx`:
```javascript
import DurationTest from './components/DurationTest'

// В return:
<DurationTest />
```

2. Увидите панель тестирования в правом нижнем углу

3. Нажмите кнопки для тестирования разных сценариев

## Обратная совместимость

✅ **Полностью обратно совместимо**

- Если не указать `duration`, используется значение по умолчанию `1`
- Старый код продолжит работать без изменений
- Новый функционал опционален

```javascript
// Старый код - продолжит работать
await playerApi.toggleTimeSlots(slots)  // duration = 1

// Новый код - с кастомной duration
await playerApi.toggleTimeSlots(slots, 2)  // duration = 2
```

## Производительность

- ✅ Нет дополнительных запросов к API
- ✅ Нет изменений в структуре БД
- ✅ Минимальные изменения в коде
- ✅ Нет влияния на существующую функциональность

## Следующие шаги

### Возможные улучшения:

1. **UI для выбора duration**
   - Dropdown или slider для выбора duration при drag selection
   - Показывать текущую duration в tooltip

2. **Визуализация duration**
   - Показывать длительность слота в календаре
   - Разные цвета для разных duration

3. **Валидация**
   - Ограничение максимальной duration
   - Предупреждение при перекрытии слотов

4. **Аналитика**
   - Статистика по duration
   - Средняя длительность сессий

## Файлы изменены

### Frontend
- `game-planer-front/src/services/api.js` - ✏️ Modified
- `game-planer-front/src/App.jsx` - ✏️ Modified

### Документация
- `game-planer-front/API_USAGE.md` - ✨ New
- `game-planer-front/TESTING_DURATION.md` - ✨ New
- `game-planer-front/HOW_TO_TEST_DURATION.md` - ✨ New
- `game-planer-front/src/components/DurationTest.jsx` - ✨ New
- `DURATION_FEATURE_CHECKLIST.md` - ✨ New
- `DURATION_FEATURE_SUMMARY.md` - ✨ New (этот файл)

## Контакты

Для вопросов и предложений:
- См. [TROUBLESHOOTING.md](TROUBLESHOOTING.md)
- См. [API_USAGE.md](game-planer-front/API_USAGE.md)

---

**Версия:** 1.1.0  
**Дата:** 2025-11-16  
**Статус:** ✅ Ready for testing
