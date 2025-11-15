#!/bin/bash

echo "🚀 Запуск Game Planner..."

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
docker-compose down

# Сборка и запуск контейнеров
echo "🔨 Сборка и запуск контейнеров..."
docker-compose up -d --build

# Ожидание запуска
echo "⏳ Ожидание запуска сервисов..."
sleep 10

# Проверка статуса
echo "📊 Статус контейнеров:"
docker-compose ps

echo ""
echo "✅ Game Planner запущен!"
echo "🌐 Фронтенд: http://localhost"
echo "🔧 Backend API: http://localhost:8080"
echo "🗄️  PostgreSQL: localhost:5432"
echo ""
echo "Для просмотра логов: docker-compose logs -f"
echo "Для остановки: docker-compose down"
