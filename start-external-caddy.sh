#!/bin/bash

echo "🚀 Запуск Game Planner с внешним Caddy..."

# Проверка наличия Docker
if ! command -v docker &> /dev/null; then
    echo "❌ Docker не установлен. Установите Docker и попробуйте снова."
    exit 1
fi

# Проверка наличия Docker Compose
if ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose не установлен. Установите Docker Compose и попробуйте снова."
    exit 1
fi

# Создание .env файла если его нет
if [ ! -f .env ]; then
    echo "📝 Создание .env файла..."
    cp .env.example .env
fi

# Остановка и удаление старых контейнеров
echo "🧹 Очистка старых контейнеров..."
docker-compose -f docker-compose.external-caddy.yml down

# Сборка и запуск контейнеров
echo "🔨 Сборка и запуск контейнеров..."
docker-compose -f docker-compose.external-caddy.yml up -d --build

# Ожидание запуска
echo "⏳ Ожидание запуска сервисов..."
sleep 10

# Проверка статуса
echo "📊 Статус контейнеров:"
docker-compose -f docker-compose.external-caddy.yml ps

echo ""
echo "✅ Game Planner запущен!"
echo "🌐 Фронтенд: http://localhost:${FRONTEND_PORT:-3000}"
echo "🔧 Backend API: http://localhost:${BACKEND_PORT:-8080}"
echo "🗄️  PostgreSQL: Доступна только внутри Docker сети"
echo ""
echo "⚠️  ВАЖНО: Добавьте конфигурацию из Caddyfile.external в ваш Caddy и перезагрузите его!"
echo ""
echo "Для просмотра логов: docker-compose -f docker-compose.external-caddy.yml logs -f"
echo "Для остановки: docker-compose -f docker-compose.external-caddy.yml down"
