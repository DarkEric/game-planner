@echo off
echo Starting Game Planner with external Caddy...

REM Check if Docker is installed
docker --version >nul 2>&1
if errorlevel 1 (
    echo Docker is not installed. Please install Docker and try again.
    exit /b 1
)

REM Check if Docker Compose is installed
docker-compose --version >nul 2>&1
if errorlevel 1 (
    echo Docker Compose is not installed. Please install Docker Compose and try again.
    exit /b 1
)

REM Create .env file if it doesn't exist
if not exist .env (
    echo Creating .env file...
    copy .env.example .env
)

REM Stop and remove old containers
echo Cleaning up old containers...
docker-compose -f docker-compose.external-caddy.yml down

REM Build and start containers
echo Building and starting containers...
docker-compose -f docker-compose.external-caddy.yml up -d --build

REM Wait for services to start
echo Waiting for services to start...
timeout /t 10 /nobreak >nul

REM Check status
echo Container status:
docker-compose -f docker-compose.external-caddy.yml ps

echo.
echo Game Planner is running!
echo Frontend: http://localhost:%FRONTEND_PORT% (default: 3000)
echo Backend API: http://localhost:%BACKEND_PORT% (default: 8080)
echo PostgreSQL: Available only inside Docker network
echo.
echo IMPORTANT: Add Caddyfile.external configuration to your Caddy and reload it!
echo.
echo To view logs: docker-compose -f docker-compose.external-caddy.yml logs -f
echo To stop: docker-compose -f docker-compose.external-caddy.yml down
