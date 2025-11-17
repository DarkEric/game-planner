# Настройка часового пояса для Telegram уведомлений

## Описание

По умолчанию время в Telegram уведомлениях отображается в UTC+0. Теперь вы можете настроить часовой пояс для отображения времени в удобном для вас формате.

## Настройка

### Переменная окружения

Добавьте в `.env`:

```env
TELEGRAM_BOT_TIMEZONE=Europe/Moscow
```

### Поддерживаемые часовые пояса

#### Россия

| Часовой пояс | Город | Смещение |
|--------------|-------|----------|
| `Europe/Kaliningrad` | Калининград | UTC+2 |
| `Europe/Moscow` | Москва | UTC+3 |
| `Europe/Samara` | Самара | UTC+4 |
| `Asia/Yekaterinburg` | Екатеринбург | UTC+5 |
| `Asia/Omsk` | Омск | UTC+6 |
| `Asia/Krasnoyarsk` | Красноярск | UTC+7 |
| `Asia/Irkutsk` | Иркутск | UTC+8 |
| `Asia/Yakutsk` | Якутск | UTC+9 |
| `Asia/Vladivostok` | Владивосток | UTC+10 |
| `Asia/Magadan` | Магадан | UTC+11 |
| `Asia/Kamchatka` | Камчатка | UTC+12 |

#### Другие популярные

| Часовой пояс | Описание |
|--------------|----------|
| `Europe/London` | Лондон (UTC+0/+1) |
| `Europe/Paris` | Париж (UTC+1/+2) |
| `Europe/Kiev` | Киев (UTC+2/+3) |
| `Asia/Dubai` | Дубай (UTC+4) |
| `Asia/Bangkok` | Бангкок (UTC+7) |
| `Asia/Tokyo` | Токио (UTC+9) |
| `America/New_York` | Нью-Йорк (UTC-5/-4) |
| `America/Los_Angeles` | Лос-Анджелес (UTC-8/-7) |

Полный список: https://en.wikipedia.org/wiki/List_of_tz_database_time_zones

## Формат уведомлений

### До настройки (UTC+0)

```
🕐 Время: 17.11.2025 15:00 - 17.11.2025 18:00
```

### После настройки (Europe/Moscow)

```
🕐 Время: 17.11.2025 18:00 - 17.11.2025 21:00 (по Москве)
```

### Другие примеры

**Europe/Kaliningrad:**
```
🕐 Время: 17.11.2025 17:00 - 17.11.2025 20:00 (по Калининграду)
```

**Asia/Vladivostok:**
```
🕐 Время: 18.11.2025 01:00 - 18.11.2025 04:00 (по Владивостоку)
```

**America/New_York:**
```
🕐 Время: 17.11.2025 10:00 - 17.11.2025 13:00 (UTC-5)
```

## Автоматическое определение названия

Для популярных российских часовых поясов автоматически добавляется название города:

- `Europe/Moscow` → "по Москве"
- `Europe/Kaliningrad` → "по Калининграду"
- `Asia/Vladivostok` → "по Владивостоку"
- и т.д.

Для других поясов отображается смещение UTC:
- `America/New_York` → "UTC-5"
- `Asia/Dubai` → "UTC+4"

## Примеры конфигурации

### Для Москвы (по умолчанию)

```env
TELEGRAM_BOT_TIMEZONE=Europe/Moscow
```

### Для Калининграда

```env
TELEGRAM_BOT_TIMEZONE=Europe/Kaliningrad
```

### Для Владивостока

```env
TELEGRAM_BOT_TIMEZONE=Asia/Vladivostok
```

### Для международной команды (UTC)

```env
TELEGRAM_BOT_TIMEZONE=UTC
```

## Применение изменений

После изменения переменной окружения перезапустите приложение:

```bash
docker-compose down
docker-compose up -d --build
```

## Проверка конфигурации

Проверьте логи при старте:

```bash
docker-compose logs backend | grep "Telegram Bot Configuration" -A 6
```

Ожидаемый вывод:
```
=== Telegram Bot Configuration ===
Enabled: true
Chat ID: -1001234567890
Thread ID: 5
Frontend URL: http://localhost:5173
Timezone: Europe/Moscow (по Москве)
==================================
```

## Тестирование

1. Настройте часовой пояс
2. Перезапустите приложение
3. Создайте тестовую игру
4. Проверьте уведомление в Telegram

**Ожидаемый результат:**
Время отображается в указанном часовом поясе с соответствующей подписью.

## Устранение проблем

### Неверный часовой пояс

**Симптомы:**
- В логах: `Invalid timezone 'XXX', falling back to Europe/Moscow`
- Время отображается по Москве

**Решение:**
1. Проверьте правильность написания часового пояса
2. Используйте формат из списка: https://en.wikipedia.org/wiki/List_of_tz_database_time_zones
3. Примеры правильных: `Europe/Moscow`, `Asia/Tokyo`, `America/New_York`
4. Примеры неправильных: `Moscow`, `MSK`, `GMT+3`

### Время не изменилось

**Решение:**
1. Проверьте, что переменная установлена:
   ```bash
   docker exec game-planner-backend env | grep TIMEZONE
   ```
2. Перезапустите контейнеры:
   ```bash
   docker-compose down
   docker-compose up -d --build
   ```
3. Проверьте логи конфигурации

### Неправильное название города

Если для вашего часового пояса отображается "UTC+X" вместо названия города, это нормально. Названия добавлены только для популярных российских городов.

Вы можете добавить свой город в код:
```java
// В TelegramNotificationService.java
private String getTimezoneName() {
    return switch (zone.getId()) {
        case "Europe/Moscow" -> "по Москве";
        case "Your/Timezone" -> "по Вашему городу";
        // ...
    };
}
```

## Летнее/зимнее время

Часовые пояса автоматически учитывают переход на летнее/зимнее время (DST) там, где это применимо.

Например:
- `Europe/London`: UTC+0 зимой, UTC+1 летом
- `America/New_York`: UTC-5 зимой, UTC-4 летом

В России переход на летнее время отменен с 2014 года, поэтому российские часовые пояса имеют фиксированное смещение.

## API

### Конфигурация

**application.properties:**
```properties
telegram.bot.timezone=${TELEGRAM_BOT_TIMEZONE:Europe/Moscow}
```

**Java код:**
```java
@Value("${telegram.bot.timezone:Europe/Moscow}")
private String timezoneId;

private ZoneId getNotificationZone() {
    return ZoneId.of(timezoneId);
}
```

## Полезные ссылки

- [Список часовых поясов IANA](https://en.wikipedia.org/wiki/List_of_tz_database_time_zones)
- [Часовые пояса России](https://ru.wikipedia.org/wiki/Часовые_пояса_России)
- [Java ZoneId Documentation](https://docs.oracle.com/javase/8/docs/api/java/time/ZoneId.html)

---

**Приятного использования! 🌍**
