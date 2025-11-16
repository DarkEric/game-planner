# 🔧 Troubleshooting

## Проблемы при запуске

### Порт 80 уже занят

**Симптомы**: Ошибка `port is already allocated`

**Решение**:
```yaml
# В docker-compose.yml измените порт
services:
  caddy:
    ports:
      - "8080:80"  # Вместо "80:80"
```

Теперь приложение будет доступно на http://localhost:8080

### Docker не запускается

**Симптомы**: `Cannot connect to the Docker daemon`

**Решение**:
1. Убедитесь что Docker Desktop запущен (Windows/Mac)
2. Linux: `sudo systemctl start docker`
3. Проверьте: `docker ps`

### База данных не инициализируется

**Симптомы**: Backend не может подключиться к БД

**Решение**:
```bash
# Удалите volume и пересоздайте
docker-compose down -v
docker-compose up -d

# Подождите 30 секунд для инициализации
docker-compose logs -f postgres
```

## Проблемы с аутентификацией

### Не могу зарегистрироваться

**Проблема**: "Invite code is required"

**Решение**: Используйте инвайт-код `FIRST-USER-INVITE-2025` для первого пользователя

### Инвайт-код не работает

**Проблема**: "Invalid invite code"

**Возможные причины**:
1. Код уже использован (одноразовый)
2. Код удален создателем
3. Опечатка в коде

**Решение**: Попросите новый инвайт-код у администратора

### Токен истек

**Проблема**: Постоянно выбрасывает на логин

**Решение**:
1. Очистите cookies браузера
2. Перелогиньтесь
3. Проверьте что JWT_SECRET не изменился в .env

## Проблемы с интерфейсом

### Календарь не отображается

**Проблема**: Пустой экран вместо календаря

**Решение**:
1. Откройте консоль браузера (F12)
2. Проверьте ошибки
3. Очистите кэш браузера (Ctrl+Shift+Del)
4. Перезагрузите страницу (Ctrl+F5)

### Время отображается неправильно

**Проблема**: Неверный часовой пояс

**Решение**:
1. Откройте профиль
2. Выберите правильный часовой пояс
3. Или нажмите "Определить автоматически"

### Игры не отображаются

**Проблема**: Созданные игры не видны

**Решение**:
1. Проверьте что вы в правильном диапазоне дат
2. Используйте кнопки навигации "Предыдущие"/"Следующие"
3. Нажмите "Сегодня" чтобы вернуться к текущей дате

## Проблемы с производительностью

### Медленная загрузка

**Решение**:
1. Проверьте логи: `docker-compose logs -f`
2. Проверьте ресурсы: `docker stats`
3. Увеличьте лимиты памяти в Docker Desktop

### База данных переполнена

**Решение**: Автоматическая очистка работает каждый день в 3:00.
Старые игры (>30 дней) удаляются автоматически.

Ручная очистка:
```bash
docker-compose exec postgres psql -U postgres -d game_planner
DELETE FROM games WHERE start_time < NOW() - INTERVAL '30 days';
```

## Проблемы после обновления

### Приложение не работает после git pull

**Решение**:
```bash
# Пересоберите контейнеры
docker-compose down
docker-compose up -d --build

# Если не помогло, удалите образы
docker-compose down --rmi all
docker-compose up -d --build
```

### Ошибки миграции базы данных

**Симптомы**: Backend не запускается, ошибки Liquibase

**Решение**:
```bash
# Посмотрите логи
docker-compose logs backend

# Если миграция застряла, сбросьте БД
docker-compose down -v
docker-compose up -d
```

## Логи и отладка

### Просмотр логов

```bash
# Все сервисы
docker-compose logs -f

# Только backend
docker-compose logs -f backend

# Только frontend
docker-compose logs -f frontend

# Только база данных
docker-compose logs -f postgres

# Последние 100 строк
docker-compose logs --tail=100
```

### Проверка состояния

```bash
# Статус контейнеров
docker-compose ps

# Использование ресурсов
docker stats

# Проверка сети
docker network ls
docker network inspect game-planner_default
```

## Сброс к начальному состоянию

**ВНИМАНИЕ**: Удалит все данные!

```bash
# Полный сброс
docker-compose down -v --rmi all
docker-compose up -d --build
```

## Получение помощи

Если проблема не решена:

1. Проверьте [Issues на GitHub](https://github.com/DarkEric/game-planner/issues)
2. Создайте новый Issue с описанием проблемы и логами
3. Укажите версию Docker: `docker --version`
4. Приложите логи: `docker-compose logs > logs.txt`
