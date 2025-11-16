# Production Setup Guide

## Подготовка к production развертыванию

### 1. Создайте production .env файл

```bash
cp .env.example .env
```

Отредактируйте `.env`:

```bash
# Database
POSTGRES_DB=game_planner
POSTGRES_USER=your_db_user
POSTGRES_PASSWORD=strong_password_here

# Backend
JWT_SECRET=your-very-long-secret-key-at-least-256-bits-for-production-security
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/game_planner

# CORS - ВАЖНО! Укажите ваши домены
CORS_ALLOWED_ORIGINS=https://your-domain.com,https://www.your-domain.com

# Domain для Caddy
DOMAIN=your-domain.com

# Ports (если нужно изменить)
BACKEND_PORT=8080
FRONTEND_PORT=3000
```

### 2. Настройка CORS для production

**ВАЖНО:** Переменная `CORS_ALLOWED_ORIGINS` должна содержать все домены, с которых будет доступно приложение.

#### Примеры:

**Один домен:**
```bash
CORS_ALLOWED_ORIGINS=https://game-planner.example.com
```

**Несколько доменов (без пробелов!):**
```bash
CORS_ALLOWED_ORIGINS=https://game-planner.example.com,https://www.game-planner.example.com,https://app.example.com
```

**С поддоменами:**
```bash
CORS_ALLOWED_ORIGINS=https://game-planner.example.com,https://api.example.com
```

**Для тестирования (НЕ используйте в production!):**
```bash
CORS_ALLOWED_ORIGINS=*
```

### 3. Запуск production версии

#### Вариант A: Со встроенным Caddy (рекомендуется)

```bash
docker-compose -f docker-compose.prod.yml up -d --build
```

Caddy автоматически:
- Получит SSL сертификат от Let's Encrypt
- Настроит HTTPS
- Будет автоматически обновлять сертификаты

#### Вариант B: С внешним Caddy

1. Запустите приложение:
```bash
docker-compose -f docker-compose.external-caddy.yml up -d --build
```

2. Настройте ваш Caddy (см. `Caddyfile.external`):
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

    encode gzip zstd
}
```

3. Перезагрузите Caddy:
```bash
caddy reload
```

### 4. Проверка развертывания

#### Проверьте статус контейнеров:
```bash
docker-compose -f docker-compose.prod.yml ps
```

Все контейнеры должны быть в статусе `Up`.

#### Проверьте логи:
```bash
# Все логи
docker-compose -f docker-compose.prod.yml logs -f

# Только backend
docker-compose -f docker-compose.prod.yml logs -f backend

# Только Caddy
docker-compose -f docker-compose.prod.yml logs -f caddy
```

#### Проверьте доступность:
```bash
# Проверка HTTPS
curl https://your-domain.com

# Проверка API
curl https://your-domain.com/api/auth/login
```

### 5. Безопасность

#### Обязательные шаги:

1. **Смените JWT_SECRET** на случайную строку минимум 256 бит:
```bash
# Генерация случайного ключа
openssl rand -base64 64
```

2. **Используйте сильный пароль для PostgreSQL**

3. **Настройте firewall** (если используете внешний Caddy):
```bash
# Разрешить только необходимые порты
ufw allow 80/tcp
ufw allow 443/tcp
ufw enable
```

4. **Регулярно обновляйте Docker образы**:
```bash
docker-compose -f docker-compose.prod.yml pull
docker-compose -f docker-compose.prod.yml up -d
```

### 6. Backup базы данных

#### Создание backup:
```bash
docker exec game-planner-db-prod pg_dump -U your_db_user game_planner > backup_$(date +%Y%m%d_%H%M%S).sql
```

#### Восстановление из backup:
```bash
docker exec -i game-planner-db-prod psql -U your_db_user game_planner < backup_20250115_120000.sql
```

#### Автоматический backup (cron):
```bash
# Добавьте в crontab (crontab -e)
0 2 * * * docker exec game-planner-db-prod pg_dump -U your_db_user game_planner > /backups/game_planner_$(date +\%Y\%m\%d).sql
```

### 7. Мониторинг

#### Проверка использования ресурсов:
```bash
docker stats
```

#### Проверка места на диске:
```bash
docker system df
```

#### Очистка неиспользуемых ресурсов:
```bash
docker system prune -a
```

### 8. Обновление приложения

```bash
# 1. Остановите приложение
docker-compose -f docker-compose.prod.yml down

# 2. Получите последние изменения
git pull

# 3. Пересоберите и запустите
docker-compose -f docker-compose.prod.yml up -d --build

# 4. Проверьте логи
docker-compose -f docker-compose.prod.yml logs -f
```

### 9. Troubleshooting

#### CORS ошибки

Проверьте, что `CORS_ALLOWED_ORIGINS` содержит правильные домены:
```bash
docker-compose -f docker-compose.prod.yml exec backend env | grep CORS
```

Если нужно изменить:
1. Обновите `.env`
2. Перезапустите backend:
```bash
docker-compose -f docker-compose.prod.yml restart backend
```

#### SSL сертификат не получен

Проверьте логи Caddy:
```bash
docker-compose -f docker-compose.prod.yml logs caddy
```

Убедитесь что:
- Домен указывает на ваш сервер (DNS настроен)
- Порты 80 и 443 открыты
- Домен указан правильно в `.env`

#### База данных недоступна

```bash
# Проверьте статус PostgreSQL
docker-compose -f docker-compose.prod.yml exec postgres pg_isready

# Проверьте логи
docker-compose -f docker-compose.prod.yml logs postgres
```

### 10. Масштабирование

Для высоконагруженных систем рассмотрите:

1. **Внешняя база данных** (AWS RDS, DigitalOcean Managed Database)
2. **Load balancer** для нескольких инстансов backend
3. **CDN** для статических файлов фронтенда
4. **Redis** для кеширования сессий

---

## Контрольный список перед запуском

- [ ] `.env` файл создан и заполнен
- [ ] `JWT_SECRET` изменен на случайное значение
- [ ] `CORS_ALLOWED_ORIGINS` содержит production домены
- [ ] Пароль PostgreSQL изменен
- [ ] DNS настроен и указывает на сервер
- [ ] Порты 80 и 443 открыты
- [ ] Firewall настроен
- [ ] Backup стратегия определена
- [ ] Мониторинг настроен

---

Для получения помощи см. [README.md](README.md) или [CADDY_INTEGRATION.md](CADDY_INTEGRATION.md)
