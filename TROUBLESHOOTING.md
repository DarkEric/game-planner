# Troubleshooting Guide

## Проблемы с API

### 400 Bad Request при создании временного слота

**Симптомы:**
```
POST /api/players/me/time-slots/toggle
Status: 400 Bad Request
Payload: {start: "2025-11-18T04:00:00", duration: 1}
```

**Возможные причины:**

#### 1. Проблема с десериализацией LocalDateTime

**Решение:** Убедитесь, что Jackson правильно настроен для работы с Java 8 Date/Time API.

Проверьте логи бэкенда:
```bash
docker-compose logs -f backend | grep -i "error\|exception"
```

Если видите ошибку типа:
```
Cannot deserialize value of type `java.time.LocalDateTime`
```

Это означает, что Jackson не может распарсить строку даты.

**Исправление:**
- Убедитесь, что `JacksonConfig.java` присутствует в проекте
- Перезапустите бэкенд:
```bash
docker-compose restart backend
```

#### 2. Неправильный формат даты

**Проверка:** Дата должна быть в формате ISO без timezone:
```
Правильно:  "2025-11-18T04:00:00"
Неправильно: "2025-11-18T04:00:00Z"
Неправильно: "2025-11-18T04:00:00+03:00"
```

**Решение:** Убедитесь, что фронтенд использует `formatLocalDateTime()` из `dateUtils.js`

#### 3. Null значения

**Проверка:** Убедитесь, что `start` не null:
```javascript
console.log('Sending:', { start, duration })
```

**Решение:** Проверьте логику создания даты на фронтенде

### 403 Forbidden

**Причина:** Проблема с JWT токеном или CORS

**Решение:**

1. Проверьте, что токен отправляется:
```javascript
// В DevTools Console
localStorage.getItem('authToken')
```

2. Проверьте CORS настройки:
```bash
# В .env
CORS_ALLOWED_ORIGINS=https://your-domain.com
```

3. Перезапустите backend:
```bash
docker-compose restart backend
```

### 401 Unauthorized

**Причина:** Токен истек или невалиден

**Решение:**
1. Выйдите и войдите снова
2. Проверьте срок действия токена (по умолчанию 24 часа)

## Проблемы с датами и временем

### Время отображается неправильно

**Симптомы:** Время сдвинуто на несколько часов

**Причина:** Проблема с часовыми поясами

**Решение:**

1. Проверьте, что используются утилиты из `dateUtils.js`:
```javascript
import { parseLocalDateTime, formatLocalDateTime } from '../utils/dateUtils'
```

2. Проверьте timezone пользователя:
```javascript
import { getUserTimezone, debugDate } from '../utils/dateUtils'

const tz = getUserTimezone()
console.log('Timezone:', tz)

const date = new Date()
debugDate('Current date', date)
```

3. Очистите кеш браузера и перезагрузите страницу

**Подробнее:** См. [game-planer-front/TIMEZONE_HANDLING.md](game-planer-front/TIMEZONE_HANDLING.md)

## Проблемы с Docker

### Контейнер не запускается

**Проверка статуса:**
```bash
docker-compose ps
docker-compose logs backend
```

**Частые причины:**

#### 1. Порт уже занят

```
Error: bind: address already in use
```

**Решение:** Измените порт в `.env`:
```bash
BACKEND_PORT=8081
FRONTEND_PORT=3001
```

#### 2. База данных недоступна

```
Connection refused: postgres:5432
```

**Решение:**
```bash
# Перезапустите PostgreSQL
docker-compose restart postgres

# Подождите 10-15 секунд
sleep 15

# Перезапустите backend
docker-compose restart backend
```

#### 3. Ошибка сборки

```
BUILD FAILED
```

**Решение:**
```bash
# Очистите и пересоберите
docker-compose down
docker-compose build --no-cache
docker-compose up -d
```

### Контейнер постоянно перезапускается

**Проверка:**
```bash
docker-compose ps
# Если видите "Restarting"
```

**Решение:**
```bash
# Посмотрите логи
docker-compose logs --tail=100 backend

# Обычно это проблема с подключением к БД или ошибка в коде
```

## Проблемы с базой данных

### Ошибка миграции Liquibase

```
Liquibase failed to start
```

**Решение:**

1. Проверьте, что PostgreSQL запущена:
```bash
docker-compose exec postgres pg_isready
```

2. Проверьте changelog файлы:
```bash
ls -la game-planner-back/src/main/resources/db/changelog/
```

3. Сбросьте базу данных (ВНИМАНИЕ: удалит все данные!):
```bash
docker-compose down -v
docker-compose up -d
```

### Данные потеряны после перезапуска

**Причина:** Volume не создан или удален

**Решение:**
```bash
# Проверьте volumes
docker volume ls | grep game-planner

# Если нет, создайте заново
docker-compose up -d
```

**Backup перед удалением:**
```bash
docker exec game-planner-db pg_dump -U postgres game_planner > backup.sql
```

## Проблемы с CORS

### Запросы блокируются CORS

**Симптомы:**
```
Access to fetch at 'http://localhost:8080/api/...' from origin 'http://localhost:3000' 
has been blocked by CORS policy
```

**Решение:**

1. Проверьте `CORS_ALLOWED_ORIGINS` в `.env`:
```bash
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173,https://your-domain.com
```

2. Убедитесь, что нет пробелов в списке origins

3. Перезапустите backend:
```bash
docker-compose restart backend
```

4. Проверьте, что origin правильный:
```javascript
// В DevTools Console
console.log(window.location.origin)
```

## Проблемы с production

### SSL сертификат не получен

**Симптомы:** Caddy не может получить сертификат от Let's Encrypt

**Решение:**

1. Проверьте, что домен указывает на ваш сервер:
```bash
nslookup your-domain.com
```

2. Проверьте, что порты 80 и 443 открыты:
```bash
sudo ufw status
```

3. Проверьте логи Caddy:
```bash
docker-compose -f docker-compose.prod.yml logs caddy
```

4. Убедитесь, что домен правильно указан в `.env`:
```bash
DOMAIN=your-domain.com
```

### Приложение недоступно

**Проверка:**
```bash
# Проверьте статус всех контейнеров
docker-compose -f docker-compose.prod.yml ps

# Все должны быть "Up"
```

**Решение:**
```bash
# Перезапустите все
docker-compose -f docker-compose.prod.yml restart

# Если не помогло, пересоберите
docker-compose -f docker-compose.prod.yml down
docker-compose -f docker-compose.prod.yml up -d --build
```

## Получение помощи

### Сбор информации для отчета об ошибке

```bash
# 1. Версии
docker --version
docker-compose --version

# 2. Статус контейнеров
docker-compose ps

# 3. Логи (последние 100 строк)
docker-compose logs --tail=100 > logs.txt

# 4. Переменные окружения (без паролей!)
docker-compose config

# 5. Системная информация
docker info
```

### Полезные команды для отладки

```bash
# Войти в контейнер
docker-compose exec backend sh

# Проверить переменные окружения
docker-compose exec backend env

# Проверить подключение к БД
docker-compose exec backend nc -zv postgres 5432

# Проверить логи в реальном времени
docker-compose logs -f backend

# Проверить использование ресурсов
docker stats
```

---

Если проблема не решена, создайте issue с:
- Описанием проблемы
- Шагами для воспроизведения
- Логами (из команд выше)
- Версиями ПО
