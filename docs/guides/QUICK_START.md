# 🚀 Быстрый старт Game Planner

[English version](QUICK_START.en.md) | Русская версия

## Минимальная установка (5 минут)

### 1. Установите Docker

- **Windows**: [Docker Desktop](https://www.docker.com/products/docker-desktop)
- **Linux**: `sudo apt install docker.io docker-compose`
- **Mac**: [Docker Desktop](https://www.docker.com/products/docker-desktop)

### 2. Клонируйте проект

```bash
git clone https://github.com/DarkEric/game-planner.git
cd game-planner
```

### 3. Создайте .env файл

```bash
# Windows
copy .env.example .env

# Linux/Mac
cp .env.example .env
```

### 4. Запустите

```bash
# Windows - просто запустите
start.bat

# Linux/Mac
docker-compose up -d
```

### 5. Откройте браузер

Перейдите на http://localhost

### 6. Зарегистрируйтесь

Используйте инвайт-код: `FIRST-USER-INVITE-2025`

## ✅ Готово!

Теперь вы можете:
- Отмечать свободное время
- Приглашать друзей (создавайте инвайт-коды)
- Планировать игры

## 🔧 Команды

```bash
# Запуск
docker-compose up -d

# Остановка
docker-compose down

# Просмотр логов
docker-compose logs -f

# Перезапуск
docker-compose restart

# Обновление
git pull
docker-compose down
docker-compose up -d --build
```

## 🆘 Проблемы?

### Порт 80 занят

Измените порт в `docker-compose.yml`:
```yaml
ports:
  - "8080:80"  # Теперь доступно на localhost:8080
```

### База данных не запускается

Удалите volume и пересоздайте:
```bash
docker-compose down -v
docker-compose up -d
```

### Не работает после обновления

Пересоберите контейнеры:
```bash
docker-compose down
docker-compose up -d --build
```

## 📚 Дальше

- [Production Setup](PRODUCTION_SETUP.md) - для публичного развертывания
- [Troubleshooting](TROUBLESHOOTING.md) - решение проблем
- [README](README.md) - полная документация
