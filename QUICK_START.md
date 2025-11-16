# Быстрый старт Game Planner

## Вариант 1: Со встроенным Caddy (проще всего)

```bash
# Windows
start.bat

# Linux/Mac
./start.sh
```

Приложение доступно на http://localhost

---

## Вариант 2: С внешним Caddy (если у вас уже есть Caddy)

### Шаг 1: Настройте порты (опционально)

Создайте `.env` файл:
```bash
BACKEND_PORT=8080
FRONTEND_PORT=3000
```

### Шаг 2: Запустите приложение

```bash
# Windows
start-external-caddy.bat

# Linux/Mac
./start-external-caddy.sh
```

### Шаг 3: Настройте ваш Caddy

Добавьте в ваш Caddyfile:

```caddyfile
game-planner.localhost {
    handle /api/* {
        reverse_proxy localhost:8080
    }
    handle {
        reverse_proxy localhost:3000
    }
}
```

### Шаг 4: Перезагрузите Caddy

```bash
caddy reload
# или
sudo systemctl reload caddy
```

Приложение доступно на http://game-planner.localhost

---

## Вариант 3: Production с HTTPS

### Шаг 1: Настройте .env

Создайте `.env` файл:
```bash
DOMAIN=your-domain.com
JWT_SECRET=your-very-long-random-secret-key
CORS_ALLOWED_ORIGINS=https://your-domain.com,https://www.your-domain.com
```

### Шаг 2: Запустите

```bash
docker-compose -f docker-compose.prod.yml up -d --build
```

Caddy автоматически получит SSL сертификат от Let's Encrypt.

**Подробнее:** [PRODUCTION_SETUP.md](PRODUCTION_SETUP.md)

---

## Полезные команды

```bash
# Просмотр логов
docker-compose logs -f

# Остановка
docker-compose down

# Полная очистка (включая данные)
docker-compose down -v

# Перезапуск
docker-compose restart
```

---

## Порты по умолчанию

| Сервис | Порт | Описание |
|--------|------|----------|
| Frontend | 80 | Встроенный Caddy |
| Frontend | 3000 | Внешний Caddy |
| Backend | 8080 | API |
| PostgreSQL | - | Только внутри Docker (безопасность) |

---

## Первый запуск

1. Зарегистрируйтесь на http://localhost
2. Войдите в систему
3. Настройте свой профиль (имя и цвет)
4. Отметьте доступное время в календаре
5. Пригласите друзей!

---

## Troubleshooting

### Порт уже занят

Измените порты в `.env`:
```bash
BACKEND_PORT=8081
FRONTEND_PORT=3001
```

### Ошибка подключения к БД

Подождите 10-15 секунд после запуска - PostgreSQL инициализируется.

### CORS ошибки

**Для разработки:** Проверьте, что фронтенд обращается к API через правильный URL.

**Для production:** Убедитесь, что ваш домен указан в `CORS_ALLOWED_ORIGINS`:
```bash
CORS_ALLOWED_ORIGINS=https://your-domain.com,https://www.your-domain.com
```

Перезапустите backend после изменения:
```bash
docker-compose restart backend
```

---

Подробная документация: [README.md](README.md)
