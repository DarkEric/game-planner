.PHONY: help build up down restart logs clean dev prod

help:
	@echo "Game Planner - Makefile команды:"
	@echo ""
	@echo "  make build     - Собрать Docker образы"
	@echo "  make up        - Запустить приложение"
	@echo "  make down      - Остановить приложение"
	@echo "  make restart   - Перезапустить приложение"
	@echo "  make logs      - Показать логи"
	@echo "  make clean     - Удалить контейнеры и volumes"
	@echo "  make dev       - Запустить в режиме разработки"
	@echo "  make prod      - Запустить в production режиме"

build:
	docker-compose build

up:
	docker-compose up -d

down:
	docker-compose down

restart:
	docker-compose restart

logs:
	docker-compose logs -f

clean:
	docker-compose down -v
	docker system prune -f

dev:
	docker-compose up --build

prod:
	docker-compose -f docker-compose.prod.yml up -d --build
