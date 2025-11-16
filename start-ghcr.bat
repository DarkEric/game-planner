@echo off
echo ========================================
echo   Game Planner - Quick Start (GHCR)
echo   Using pre-built Docker images
echo ========================================
echo.

REM Check if Docker is running
docker info >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Docker is not running!
    echo Please start Docker Desktop and try again.
    pause
    exit /b 1
)

REM Check if .env exists
if not exist .env (
    echo [INFO] Creating .env file from .env.example...
    copy .env.example .env
    echo [SUCCESS] .env file created!
    echo.
)

echo [INFO] Pulling latest images from GitHub Container Registry...
docker-compose -f docker-compose.ghcr.yml pull

echo.
echo [INFO] Starting Game Planner with pre-built images...
docker-compose -f docker-compose.ghcr.yml up -d

echo.
echo ========================================
echo   Game Planner is starting!
echo ========================================
echo.
echo Application will be available at:
echo   http://localhost
echo.
echo First time? Use invite code:
echo   FIRST-USER-INVITE-2025
echo.
echo Useful commands:
echo   docker-compose -f docker-compose.ghcr.yml logs -f    - View logs
echo   docker-compose -f docker-compose.ghcr.yml down       - Stop application
echo   docker-compose -f docker-compose.ghcr.yml restart    - Restart application
echo.
pause
