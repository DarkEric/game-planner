# Changelog

All notable changes to this project will be documented in this file.

## [1.0.0] - 2024-11-16

### 🎉 Initial Release

First stable release of Game Planner - a web application for scheduling board game sessions with friends.

### ✨ Features

#### Core Functionality
- **Interactive Calendar** - Mark available time with drag-and-drop
- **Smart Slot Finder** - Automatically finds best time slots for all players
- **Game Scheduling** - Create games with title, description, and participants
- **Timezone Support** - Each user sees time in their own timezone
- **Invite System** - Registration by invitation only for controlled access
- **Auto Cleanup** - Automatic removal of old games (30+ days)

#### User Interface
- **Dark Theme** - Eye-friendly dark interface
- **Responsive Design** - Works on any screen size
- **Adaptive Calendar** - Shows 7-21 days depending on screen width
- **Language Switcher** - RU/EN interface localization (partial)
- **Auto-scroll** - Calendar automatically scrolls to 12:00 for convenience

#### Technical Features
- **JWT Authentication** - Secure user authentication
- **PostgreSQL Database** - Reliable data storage
- **Docker Deployment** - Easy setup with Docker Compose
- **Automatic HTTPS** - Caddy integration for production
- **API Optimization** - Merged consecutive time slots for better performance

### 🛠 Technologies

**Backend:**
- Java 17 + Spring Boot
- PostgreSQL
- Liquibase migrations
- JWT authentication

**Frontend:**
- React 18
- Vite
- CSS Modules
- i18n support

**Deployment:**
- Docker & Docker Compose
- Caddy (automatic HTTPS)

### 📦 Installation

```bash
git clone https://github.com/DarkEric/game-planner.git
cd game-planner
cp .env.example .env
docker-compose up -d
```

Open http://localhost and use invite code: `FIRST-USER-INVITE-2025`

### 📚 Documentation

- [README (English)](README.en.md)
- [README (Russian)](README.md)
- [Quick Start Guide](QUICK_START.en.md)
- [Production Setup](PRODUCTION_SETUP.md)
- [Troubleshooting](TROUBLESHOOTING.md)

### 🌍 Localization

- Interface: Partial (RU/EN)
- Documentation: Full (RU/EN)
- Auto-detection of browser language

### 🐛 Known Issues

- Interface localization is partial (only main elements translated)
- Mobile optimization can be improved

### 🗺️ Roadmap

- [ ] Complete English interface localization
- [ ] Mobile app
- [ ] Game library integration
- [ ] Notifications system
- [ ] Calendar export (iCal)

### 👥 Contributors

- DarkEric - Initial work

### 📝 License

MIT License - see LICENSE file for details
