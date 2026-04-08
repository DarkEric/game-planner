# Тестовая среда в Docker (отдельные порты от основного docker-compose.yml)
# Требуется: Docker Desktop / Docker Engine

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

if (-not (Test-Path ".env")) {
    Write-Host "Creating .env from .env.example..."
    Copy-Item ".env.example" ".env"
}

Write-Host "Building and starting game-planner-dev stack..."
docker compose -f docker-compose.dev.yml up -d --build

if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

Start-Sleep -Seconds 3
docker compose -f docker-compose.dev.yml ps

Write-Host ""
Write-Host "Game Planner (dev stack) is up:" -ForegroundColor Green
Write-Host "  Web UI (Caddy):  http://localhost:8888"
Write-Host "  Backend API:     http://localhost:8080/api"
Write-Host "  PostgreSQL:      localhost:5433 (user/pass: postgres/postgres, DB: game_planner)"
Write-Host ""
Write-Host "Logs:    docker compose -f docker-compose.dev.yml logs -f"
Write-Host "Stop:    docker compose -f docker-compose.dev.yml down"
Write-Host ""
Write-Host "Local Vite dev against this API: set VITE_API_URL=http://localhost:8080/api then npm run dev"
