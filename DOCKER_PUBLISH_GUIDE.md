# 📦 Руководство по публикации Docker образов

## Автоматическая публикация (Рекомендуется)

Docker образы автоматически публикуются в GitHub Container Registry при создании нового тега версии.

### Шаги для автоматической публикации:

1. **Создайте и запушьте тег версии:**
```bash
git tag -a v1.0.0 -m "Release v1.0.0"
git push origin v1.0.0
```

2. **GitHub Actions автоматически:**
   - Соберет образы для backend и frontend
   - Создаст образы для архитектур amd64 и arm64
   - Опубликует образы с тегами:
     - `ghcr.io/darkeric/game-planner-backend:1.0.0`
     - `ghcr.io/darkeric/game-planner-backend:1.0`
     - `ghcr.io/darkeric/game-planner-backend:1`
     - `ghcr.io/darkeric/game-planner-backend:latest`
     - То же для frontend

3. **Проверьте статус сборки:**
   - Перейдите на: https://github.com/DarkEric/game-planner/actions
   - Найдите workflow "Build and Push Docker Images"
   - Дождитесь завершения (обычно 5-10 минут)

4. **Проверьте опубликованные образы:**
   - Перейдите на: https://github.com/DarkEric?tab=packages
   - Вы должны увидеть:
     - `game-planner-backend`
     - `game-planner-frontend`

## Ручная публикация (Для разработчиков)

### Предварительные требования:

1. **Создайте Personal Access Token (PAT):**
   - Перейдите: https://github.com/settings/tokens
   - Нажмите "Generate new token (classic)"
   - Выберите scopes:
     - `write:packages`
     - `read:packages`
     - `delete:packages` (опционально)
   - Сохраните токен в безопасном месте

2. **Войдите в GitHub Container Registry:**
```bash
echo YOUR_TOKEN | docker login ghcr.io -u YOUR_USERNAME --password-stdin
```

### Публикация образов:

#### Backend:

```bash
# Сборка
docker build -t ghcr.io/darkeric/game-planner-backend:1.0.0 ./game-planner-back

# Теги
docker tag ghcr.io/darkeric/game-planner-backend:1.0.0 ghcr.io/darkeric/game-planner-backend:1.0
docker tag ghcr.io/darkeric/game-planner-backend:1.0.0 ghcr.io/darkeric/game-planner-backend:1
docker tag ghcr.io/darkeric/game-planner-backend:1.0.0 ghcr.io/darkeric/game-planner-backend:latest

# Публикация
docker push ghcr.io/darkeric/game-planner-backend:1.0.0
docker push ghcr.io/darkeric/game-planner-backend:1.0
docker push ghcr.io/darkeric/game-planner-backend:1
docker push ghcr.io/darkeric/game-planner-backend:latest
```

#### Frontend:

```bash
# Сборка
docker build -t ghcr.io/darkeric/game-planner-frontend:1.0.0 ./game-planer-front

# Теги
docker tag ghcr.io/darkeric/game-planner-frontend:1.0.0 ghcr.io/darkeric/game-planner-frontend:1.0
docker tag ghcr.io/darkeric/game-planner-frontend:1.0.0 ghcr.io/darkeric/game-planner-frontend:1
docker tag ghcr.io/darkeric/game-planner-frontend:1.0.0 ghcr.io/darkeric/game-planner-frontend:latest

# Публикация
docker push ghcr.io/darkeric/game-planner-frontend:1.0.0
docker push ghcr.io/darkeric/game-planner-frontend:1.0
docker push ghcr.io/darkeric/game-planner-frontend:1
docker push ghcr.io/darkeric/game-planner-frontend:latest
```

## Настройка видимости пакетов

По умолчанию пакеты приватные. Чтобы сделать их публичными:

1. Перейдите на страницу пакета:
   - https://github.com/users/DarkEric/packages/container/game-planner-backend
   - https://github.com/users/DarkEric/packages/container/game-planner-frontend

2. Нажмите "Package settings"

3. В разделе "Danger Zone" найдите "Change package visibility"

4. Выберите "Public" и подтвердите

## Мультиархитектурная сборка

Для поддержки разных архитектур (amd64, arm64):

```bash
# Создайте builder
docker buildx create --name multiarch --use

# Backend
docker buildx build \
  --platform linux/amd64,linux/arm64 \
  -t ghcr.io/darkeric/game-planner-backend:1.0.0 \
  --push \
  ./game-planner-back

# Frontend
docker buildx build \
  --platform linux/amd64,linux/arm64 \
  -t ghcr.io/darkeric/game-planner-frontend:1.0.0 \
  --push \
  ./game-planer-front
```

## Проверка опубликованных образов

```bash
# Проверка манифеста
docker manifest inspect ghcr.io/darkeric/game-planner-backend:1.0.0

# Скачивание и тест
docker pull ghcr.io/darkeric/game-planner-backend:1.0.0
docker run --rm ghcr.io/darkeric/game-planner-backend:1.0.0 --version
```

## Удаление старых версий

```bash
# Через GitHub UI
# 1. Перейдите на страницу пакета
# 2. Выберите версию
# 3. Нажмите "Delete version"

# Через API (требует токен с delete:packages)
curl -X DELETE \
  -H "Authorization: Bearer YOUR_TOKEN" \
  https://api.github.com/user/packages/container/game-planner-backend/versions/VERSION_ID
```

## Troubleshooting

### Ошибка: "denied: permission_denied"

**Решение:** Проверьте права токена и видимость репозитория.

### Ошибка: "manifest unknown"

**Решение:** Образ еще не опубликован или неверное имя.

### Медленная загрузка

**Решение:** Используйте кеширование в GitHub Actions:
```yaml
cache-from: type=gha
cache-to: type=gha,mode=max
```

## Мониторинг использования

GitHub предоставляет статистику:
- Количество скачиваний
- Размер хранилища
- Активные версии

Доступно на странице пакета в разделе "Insights".

## Лимиты GitHub Container Registry

- **Хранилище:** 500 MB бесплатно (публичные репозитории)
- **Трафик:** 1 GB/месяц бесплатно
- **Приватные образы:** Включены в GitHub Free

Для больших проектов рассмотрите GitHub Pro или Team.

## Автоматизация с GitHub Actions

Workflow уже настроен в `.github/workflows/docker-publish.yml`:

- ✅ Автоматическая сборка при создании тега
- ✅ Мультиархитектурная поддержка
- ✅ Кеширование слоев
- ✅ Автоматическое тегирование
- ✅ Публикация в GHCR

## Связанные документы

- [DOCKER_IMAGES.md](DOCKER_IMAGES.md) - Использование образов
- [PRODUCTION_SETUP.md](PRODUCTION_SETUP.md) - Production deployment
- [GitHub Packages Documentation](https://docs.github.com/en/packages)
