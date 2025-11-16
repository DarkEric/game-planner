# 🔍 Проверка статуса сборки Docker образов

## Текущая ситуация

Тег `v1.0.0` создан и запушен. GitHub Actions должен автоматически запустить сборку.

## Как проверить статус сборки

### 1. Откройте страницу Actions

**Ссылка:** https://github.com/DarkEric/game-planner/actions

### 2. Найдите workflow "Build and Push Docker Images"

Должен быть запущен workflow с:
- Название: "Build and Push Docker Images"
- Триггер: "push" на тег v1.0.0
- Статус: Running (🟡) или Completed (✅/❌)

### 3. Проверьте детали сборки

Кликните на workflow, чтобы увидеть:
- **2 джобы:** backend и frontend (параллельная сборка)
- **Время выполнения:** 
  - Frontend: ~3-5 минут
  - Backend: ~8-12 минут (Java сборка дольше)

### 4. Если сборка провалилась

#### Backend провалился:

**Возможные причины:**
1. Ошибка компиляции Java
2. Проблемы с Gradle
3. Недостаточно памяти для сборки

**Решение:**
```bash
# Проверьте локальную сборку
cd game-planner-back
docker build -t test-backend .

# Если ошибка - посмотрите логи
docker build --progress=plain -t test-backend .
```

#### Frontend провалился:

**Возможные причины:**
1. Ошибка npm install
2. Ошибка сборки React

**Решение:**
```bash
# Проверьте локальную сборку
cd game-planer-front
docker build -t test-frontend .
```

### 5. Проверьте опубликованные образы

После успешной сборки:

**Перейдите на:** https://github.com/DarkEric?tab=packages

Должны появиться:
- ✅ `game-planner-backend` (если backend собрался)
- ✅ `game-planner-frontend` (уже есть)

### 6. Сделайте пакеты публичными

Для каждого пакета:

1. Откройте страницу пакета
2. Нажмите "Package settings" (справа)
3. Прокрутите до "Danger Zone"
4. "Change package visibility" → "Public"
5. Подтвердите

## Текущие изменения в workflow

### Что исправлено:

1. **fail-fast: false** - джобы выполняются независимо
2. **Backend: только amd64** - избегаем проблем с cross-compilation Java
3. **Frontend: amd64 + arm64** - легкий образ, поддерживает обе архитектуры
4. **Gradle wrapper** - используем локальный gradlew вместо глобального gradle

### Конфигурация:

```yaml
Backend:
  - Platform: linux/amd64
  - Build time: ~8-12 минут
  - Image size: ~200 MB

Frontend:
  - Platforms: linux/amd64, linux/arm64
  - Build time: ~3-5 минут
  - Image size: ~50 MB
```

## Альтернативный вариант: Ручная сборка и публикация

Если автоматическая сборка не работает, можно собрать и опубликовать вручную:

### 1. Войдите в GitHub Container Registry

```bash
# Создайте Personal Access Token на https://github.com/settings/tokens
# Scope: write:packages, read:packages

echo YOUR_TOKEN | docker login ghcr.io -u DarkEric --password-stdin
```

### 2. Соберите образы

```bash
# Backend
docker build -t ghcr.io/darkeric/game-planner-backend:1.0.0 ./game-planner-back
docker tag ghcr.io/darkeric/game-planner-backend:1.0.0 ghcr.io/darkeric/game-planner-backend:latest

# Frontend
docker build -t ghcr.io/darkeric/game-planner-frontend:1.0.0 ./game-planer-front
docker tag ghcr.io/darkeric/game-planner-frontend:1.0.0 ghcr.io/darkeric/game-planner-frontend:latest
```

### 3. Опубликуйте образы

```bash
# Backend
docker push ghcr.io/darkeric/game-planner-backend:1.0.0
docker push ghcr.io/darkeric/game-planner-backend:latest

# Frontend
docker push ghcr.io/darkeric/game-planner-frontend:1.0.0
docker push ghcr.io/darkeric/game-planner-frontend:latest
```

## Проверка работоспособности

После публикации образов:

```bash
# Скачайте образы
docker pull ghcr.io/darkeric/game-planner-backend:1.0.0
docker pull ghcr.io/darkeric/game-planner-frontend:1.0.0

# Запустите с готовыми образами
docker-compose -f docker-compose.ghcr.yml up -d

# Проверьте
curl http://localhost
```

## Следующие шаги

После успешной публикации образов:

1. ✅ Сделайте пакеты публичными
2. ✅ Создайте GitHub Release
3. ✅ Протестируйте установку
4. ✅ Обновите документацию (если нужно)

## Помощь

Если проблемы продолжаются:

1. Проверьте логи GitHub Actions
2. Попробуйте локальную сборку
3. Проверьте права доступа (GITHUB_TOKEN)
4. Убедитесь, что репозиторий публичный

**Документация:**
- [GitHub Actions Logs](https://github.com/DarkEric/game-planner/actions)
- [GitHub Packages](https://github.com/DarkEric?tab=packages)
- [Troubleshooting Guide](TROUBLESHOOTING.md)
