# 🐳 Docker Images

Game Planner provides pre-built Docker images hosted on GitHub Container Registry for easy deployment.

## 📦 Available Images

### Backend
```
ghcr.io/darkeric/game-planner-backend:latest
ghcr.io/darkeric/game-planner-backend:1.0.0
```

### Frontend
```
ghcr.io/darkeric/game-planner-frontend:latest
ghcr.io/darkeric/game-planner-frontend:1.0.0
```

## 🚀 Quick Start with Pre-built Images

### Option 1: Using docker-compose.ghcr.yml (Recommended)

```bash
# Clone repository
git clone https://github.com/DarkEric/game-planner.git
cd game-planner

# Setup environment
cp .env.example .env

# Run with pre-built images
docker-compose -f docker-compose.ghcr.yml up -d
```

### Option 2: Manual Docker Pull

```bash
# Pull images
docker pull ghcr.io/darkeric/game-planner-backend:latest
docker pull ghcr.io/darkeric/game-planner-frontend:latest

# Run containers
docker run -d \
  --name game-planner-backend \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/game_planner \
  ghcr.io/darkeric/game-planner-backend:latest

docker run -d \
  --name game-planner-frontend \
  -p 80:80 \
  ghcr.io/darkeric/game-planner-frontend:latest
```

## 🏷️ Image Tags

- `latest` - Latest stable release from main branch
- `1.0.0`, `1.0`, `1` - Semantic version tags
- `v1.0.0` - Git tag versions

## 🔄 Updating Images

```bash
# Pull latest images
docker-compose -f docker-compose.ghcr.yml pull

# Restart services
docker-compose -f docker-compose.ghcr.yml up -d
```

## 🛠️ Building Images Locally

If you prefer to build images yourself:

```bash
# Build all services
docker-compose build

# Build specific service
docker-compose build backend
docker-compose build frontend

# Run locally built images
docker-compose up -d
```

## 📊 Image Sizes

| Image | Size (Compressed) | Platforms |
|-------|------------------|-----------|
| Backend | ~200 MB | linux/amd64, linux/arm64 |
| Frontend | ~50 MB | linux/amd64, linux/arm64 |

## 🔐 Authentication

GitHub Container Registry images are public and don't require authentication for pulling.

For pushing images (maintainers only):
```bash
echo $GITHUB_TOKEN | docker login ghcr.io -u USERNAME --password-stdin
```

## 🏗️ Multi-Architecture Support

Images are built for multiple architectures:
- `linux/amd64` - Intel/AMD 64-bit (most servers)
- `linux/arm64` - ARM 64-bit (Apple Silicon, Raspberry Pi 4+)

Docker automatically pulls the correct architecture for your system.

## 🔍 Inspecting Images

```bash
# View image details
docker inspect ghcr.io/darkeric/game-planner-backend:latest

# View image layers
docker history ghcr.io/darkeric/game-planner-backend:latest

# View image labels
docker inspect ghcr.io/darkeric/game-planner-backend:latest | jq '.[0].Config.Labels'
```

## 📝 Image Labels

Images include metadata labels:
- `org.opencontainers.image.source` - GitHub repository URL
- `org.opencontainers.image.version` - Version tag
- `org.opencontainers.image.created` - Build timestamp
- `org.opencontainers.image.revision` - Git commit SHA

## 🚨 Troubleshooting

### Image Pull Errors

```bash
# Check if image exists
docker manifest inspect ghcr.io/darkeric/game-planner-backend:latest

# Force pull latest version
docker pull --no-cache ghcr.io/darkeric/game-planner-backend:latest
```

### Rate Limiting

GitHub Container Registry has generous rate limits:
- Anonymous: 5000 requests/hour
- Authenticated: 15000 requests/hour

### Disk Space

```bash
# Clean up old images
docker image prune -a

# Remove specific image
docker rmi ghcr.io/darkeric/game-planner-backend:old-version
```

## 🔄 CI/CD Pipeline

Images are automatically built and published when:
- A new version tag is pushed (e.g., `v1.0.0`)
- Manually triggered via GitHub Actions

See `.github/workflows/docker-publish.yml` for details.

## 📚 Related Documentation

- [Quick Start Guide](QUICK_START.md)
- [Production Setup](PRODUCTION_SETUP.md)
- [Troubleshooting](TROUBLESHOOTING.md)

## 🆘 Support

Issues with Docker images? [Open an issue](https://github.com/DarkEric/game-planner/issues)
