# Руководство по выбору часового пояса

## Обзор

Компонент `TimezoneSelector` позволяет пользователям выбирать и изменять свой часовой пояс прямо в интерфейсе приложения.

## Функции

### 1. Автоматическое определение

Кнопка "🎯 Определить автоматически" использует API браузера для определения текущего часового пояса пользователя.

```javascript
const detected = Intl.DateTimeFormat().resolvedOptions().timeZone
// Например: "Europe/Moscow", "America/New_York"
```

### 2. Поиск

Пользователи могут искать часовой пояс по:
- Названию города (например, "Москва", "Нью-Йорк")
- IANA идентификатору (например, "Europe/Moscow")
- Смещению UTC (например, "UTC+3")

### 3. Популярные часовые пояса

Список наиболее часто используемых часовых поясов отображается в начале для быстрого доступа:

- Москва (UTC+3)
- Лондон (UTC+0)
- Париж (UTC+1)
- Нью-Йорк (UTC-5)
- Лос-Анджелес (UTC-8)
- Токио (UTC+9)
- И другие...

### 4. Все часовые пояса

Полный список доступных часовых поясов, включая:
- Европейские города
- Американские города
- Азиатские города
- Австралийские города
- UTC

## Использование

### В профиле пользователя

Компонент автоматически интегрирован в профиль пользователя:

1. Откройте приложение
2. В левой панели найдите раздел "Ваш профиль"
3. Под полями "Имя" и "Цвет" увидите "Часовой пояс"
4. Кликните на текущий часовой пояс
5. Выберите новый из списка или используйте поиск

### Программное использование

```javascript
import TimezoneSelector from './components/TimezoneSelector'

<TimezoneSelector
  currentTimezone={player.timezone}
  onTimezoneChange={(newTimezone) => {
    // Обработка изменения часового пояса
    updatePlayerTimezone(newTimezone)
  }}
/>
```

## API

### Props

#### `currentTimezone` (string, optional)
Текущий часовой пояс пользователя в формате IANA.

**Пример:** `"Europe/Moscow"`, `"America/New_York"`

#### `onTimezoneChange` (function, required)
Callback функция, вызываемая при изменении часового пояса.

**Сигнатура:** `(timezone: string) => void`

**Пример:**
```javascript
const handleTimezoneChange = (timezone) => {
  console.log('New timezone:', timezone)
  // Сохранить на сервере
  await playerApi.updateCurrentPlayer(name, color, timezone)
}
```

## Поддерживаемые часовые пояса

### Европа
- Europe/Moscow - Москва (UTC+3)
- Europe/London - Лондон (UTC+0)
- Europe/Paris - Париж (UTC+1)
- Europe/Berlin - Берлин (UTC+1)
- Europe/Kaliningrad - Калининград (UTC+2)
- Europe/Minsk - Минск (UTC+3)
- Europe/Istanbul - Стамбул (UTC+3)

### Америка
- America/New_York - Нью-Йорк (UTC-5)
- America/Los_Angeles - Лос-Анджелес (UTC-8)
- America/Chicago - Чикаго (UTC-6)
- America/Toronto - Торонто (UTC-5)
- America/Mexico_City - Мехико (UTC-6)
- America/Sao_Paulo - Сан-Паулу (UTC-3)

### Азия
- Asia/Tokyo - Токио (UTC+9)
- Asia/Shanghai - Шанхай (UTC+8)
- Asia/Dubai - Дубай (UTC+4)
- Asia/Singapore - Сингапур (UTC+8)
- Asia/Seoul - Сеул (UTC+9)
- Asia/Yekaterinburg - Екатеринбург (UTC+5)
- Asia/Novosibirsk - Новосибирск (UTC+7)
- Asia/Vladivostok - Владивосток (UTC+10)

### Австралия и Океания
- Australia/Sydney - Сидней (UTC+11)
- Pacific/Auckland - Окленд (UTC+13)

### Другие
- UTC - Всемирное координированное время (UTC+0)

## Добавление новых часовых поясов

Чтобы добавить новый часовой пояс, отредактируйте массив `allTimezones` в `TimezoneSelector.jsx`:

```javascript
const allTimezones = [
  // ... существующие
  { 
    value: 'Asia/Kolkata',      // IANA идентификатор
    label: 'Мумбаи (UTC+5:30)', // Отображаемое название
    offset: 5.5                  // Смещение от UTC
  }
]
```

## Стилизация

Компонент использует CSS модуль `TimezoneSelector.css` и поддерживает:
- Темную тему (по умолчанию)
- Светлую тему (через `prefers-color-scheme: light`)
- Адаптивный дизайн
- Кастомные scrollbar

### Переопределение стилей

```css
/* В вашем CSS файле */
.timezone-selector .timezone-display {
  background-color: your-color;
  border-color: your-border-color;
}

.timezone-selector .timezone-dropdown {
  max-height: 600px; /* Изменить высоту dropdown */
}
```

## Интеграция с бэкендом

### Сохранение timezone

```javascript
const handleTimezoneChange = async (timezone) => {
  try {
    // Отправить на сервер
    const updatedPlayer = await playerApi.updateCurrentPlayer(
      player.name,
      player.color,
      timezone
    )
    
    // Обновить локальное состояние
    setPlayer(updatedPlayer)
  } catch (error) {
    console.error('Failed to update timezone:', error)
  }
}
```

### API запрос

```http
PUT /api/players/me
Authorization: Bearer {token}
Content-Type: application/json

{
  "name": "Игрок",
  "color": "#646cff",
  "timezone": "Europe/Moscow"
}
```

### API ответ

```json
{
  "id": 1,
  "name": "Игрок",
  "color": "#646cff",
  "timezone": "Europe/Moscow",
  "availableTimes": [...]
}
```

## Примеры использования

### Пример 1: Базовое использование

```javascript
import { useState } from 'react'
import TimezoneSelector from './components/TimezoneSelector'

function Profile() {
  const [timezone, setTimezone] = useState('Europe/Moscow')
  
  return (
    <div>
      <h2>Профиль</h2>
      <TimezoneSelector
        currentTimezone={timezone}
        onTimezoneChange={setTimezone}
      />
    </div>
  )
}
```

### Пример 2: С сохранением на сервере

```javascript
import { useState } from 'react'
import TimezoneSelector from './components/TimezoneSelector'
import { playerApi } from './services/api'

function Profile({ player }) {
  const [saving, setSaving] = useState(false)
  
  const handleTimezoneChange = async (timezone) => {
    setSaving(true)
    try {
      await playerApi.updateCurrentPlayer(
        player.name,
        player.color,
        timezone
      )
    } catch (error) {
      alert('Не удалось сохранить часовой пояс')
    } finally {
      setSaving(false)
    }
  }
  
  return (
    <div>
      <TimezoneSelector
        currentTimezone={player.timezone}
        onTimezoneChange={handleTimezoneChange}
      />
      {saving && <span>Сохранение...</span>}
    </div>
  )
}
```

### Пример 3: С валидацией

```javascript
const handleTimezoneChange = async (timezone) => {
  // Проверка валидности timezone
  const validTimezones = Intl.supportedValuesOf('timeZone')
  
  if (!validTimezones.includes(timezone)) {
    console.error('Invalid timezone:', timezone)
    return
  }
  
  // Сохранить
  await saveTimezone(timezone)
}
```

## Troubleshooting

### Проблема: Timezone не сохраняется

**Решение:**
1. Проверьте, что `onTimezoneChange` вызывается
2. Проверьте Network запрос в DevTools
3. Проверьте логи бэкенда

### Проблема: Dropdown не закрывается

**Решение:**
Кликните на кнопку "×" или выберите часовой пояс из списка.

### Проблема: Не все часовые пояса отображаются

**Решение:**
Используйте поиск или добавьте нужный часовой пояс в массив `allTimezones`.

### Проблема: Автоопределение не работает

**Решение:**
Проверьте, что браузер поддерживает `Intl.DateTimeFormat().resolvedOptions().timeZone`.

## Accessibility

Компонент поддерживает:
- ✅ Keyboard navigation (Tab, Enter, Escape)
- ✅ Screen readers
- ✅ High contrast mode
- ✅ Focus indicators

## Browser Support

- ✅ Chrome 24+
- ✅ Firefox 29+
- ✅ Safari 10+
- ✅ Edge 14+

## Дополнительные ресурсы

- [IANA Time Zone Database](https://www.iana.org/time-zones)
- [MDN: Intl.DateTimeFormat](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl/DateTimeFormat)
- [TIMEZONE_FEATURE.md](../TIMEZONE_FEATURE.md) - Функция учета часовых поясов
- [dateUtils.js](src/utils/dateUtils.js) - Утилиты для работы с датами
