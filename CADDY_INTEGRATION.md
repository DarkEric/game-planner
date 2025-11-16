# Интеграция с внешним Caddy

Если у вас уже запущен Caddy на хосте, следуйте этим инструкциям.

## Быстрый старт

### 1. Запустите приложение без встроенного Caddy

**Windows:**
```bash
start-external-caddy.bat
```

**Linux/Mac:**
```bash
chmod +x start-external-caddy.sh
./start-external-caddy.sh
```

**Или вручную:**
```bash
docker-compose -f docker-compose.external-caddy.yml up -d --build
```

### 2. Настройте ваш Caddy

Добавьте следующую конфигурацию в ваш существующий Caddyfile.

**Важно:** Если вы изменили порты в `.env`, обновите их в конфигурации Caddy!

#### Для локальной разработки:

```caddyfile
game-planner.localhost {
    handle /api/* {
        reverse_proxy localhost:8080  # Замените на ваш BACKEND_PORT
    }

    handle {
        reverse_proxy localhost:3000  # Замените на ваш FRONTEND_PORT
    }
}
```

#### С переменными окружения (рекомендуется):

Используйте `Caddyfile.external.env` для автоматического чтения портов из переменных окружения:

```caddyfile
game-planner.localhost {
    handle /api/* {
        reverse_proxy localhost:{$BACKEND_PORT:8080}
    }

    handle {
        reverse_proxy localhost:{$FRONTEND_PORT:3000}
    }
}
```

#### Для production:

```caddyfile
your-domain.com {
    handle /api/* {
        reverse_proxy localhost:8080 {
            header_up X-Real-IP {remote_host}
            header_up X-Forwarded-For {remote_host}
            header_up X-Forwarded-Proto {scheme}
        }
    }

    handle {
        reverse_proxy localhost:3000
    }

    # Заголовки безопасности
    header {
        Strict-Transport-Security "max-age=31536000; includeSubDomains; preload"
        X-Content-Type-Options "nosniff"
        X-Frame-Options "DENY"
        X-XSS-Protection "1; mode=block"
        -Server
    }

    encode gzip zstd
}
```

### 3. Перезагрузите Caddy

#### Если используете переменные окружения:

Экспортируйте переменные перед запуском Caddy:
```bash
export BACKEND_PORT=8080
export FRONTEND_PORT=3000
caddy reload
```

Или для systemd, добавьте в `/etc/systemd/system/caddy.service`:
```ini
[Service]
Environment="BACKEND_PORT=8080"
Environment="FRONTEND_PORT=3000"
```

Затем:
```bash
sudo systemctl daemon-reload
sudo systemctl reload caddy
```

#### Обычная перезагрузка:

```bash
caddy reload
```

Или если Caddy запущен как systemd сервис:
```bash
sudo systemctl reload caddy
```

## Порты

При использовании внешнего Caddy приложение использует следующие порты:

- **Frontend**: `3000` (по умолчанию, настраивается через `FRONTEND_PORT` в `.env`)
- **Backend**: `8080` (по умолчанию, настраивается через `BACKEND_PORT` в `.env`)
- **PostgreSQL**: доступна только внутри Docker сети (не публикуется на хост)

### Изменение портов

Если порты 3000 или 8080 уже заняты, создайте `.env` файл:

```bash
BACKEND_PORT=8081
FRONTEND_PORT=3001
```

И обновите конфигурацию Caddy соответственно:

```caddyfile
game-planner.localhost {
    handle /api/* {
        reverse_proxy localhost:8081  # Измененный порт
    }

    handle {
        reverse_proxy localhost:3001  # Измененный порт
    }
}
```

## Проверка

После настройки проверьте доступность:

```bash
# Прямой доступ
curl http://localhost:3000
curl http://localhost:8080/api/auth/login

# Через Caddy
curl http://game-planner.localhost
curl http://game-planner.localhost/api/auth/login
```

## Логи

Просмотр логов приложения:
```bash
docker-compose -f docker-compose.external-caddy.yml logs -f
```

Просмотр логов конкретного сервиса:
```bash
docker-compose -f docker-compose.external-caddy.yml logs -f backend
docker-compose -f docker-compose.external-caddy.yml logs -f frontend
```

## Остановка

```bash
docker-compose -f docker-compose.external-caddy.yml down
```

## Troubleshooting

### Приложение недоступно через Caddy

1. Проверьте, что контейнеры запущены:
```bash
docker-compose -f docker-compose.external-caddy.yml ps
```

2. Проверьте логи Caddy:
```bash
caddy logs
# или
sudo journalctl -u caddy -f
```

3. Проверьте, что порты не заняты:
```bash
netstat -tulpn | grep -E '3000|8080'
```

### Ошибки CORS

Убедитесь, что в SecurityConfig бэкенда разрешен ваш домен:
```java
configuration.setAllowedOrigins(Arrays.asList(
    "http://localhost:5173",
    "http://localhost:3000",
    "http://game-planner.localhost",
    "https://your-domain.com"
));
```
