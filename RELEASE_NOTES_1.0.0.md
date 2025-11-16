# 🎲 Game Planner v1.0.0

**First stable release!** 🎉

Game Planner is a web application for scheduling board game sessions with friends. Mark your available time, find common slots, and plan games together!

## ✨ What's New

### Core Features
- 📅 **Interactive Calendar** with drag-and-drop time selection
- 🎯 **Smart Slot Finder** - automatically finds best times for everyone
- 🎮 **Game Scheduling** with title, description, and auto-participant selection
- 🌍 **Timezone Support** - everyone sees time in their timezone
- 🔐 **Invite System** - controlled registration by invitation only
- 🧹 **Auto Cleanup** - removes old games automatically

### User Experience
- 🎨 **Dark Theme** - easy on the eyes
- 📱 **Responsive Design** - works on any screen
- 🌐 **Language Switcher** - RU/EN (partial localization)
- ⏰ **Auto-scroll to 12:00** - shows relevant time immediately
- 📊 **Adaptive Calendar** - 7-21 days depending on screen width

### Technical Highlights
- ⚡ **Fast Setup** - Docker Compose one-command deployment
- 🔒 **Secure** - JWT authentication
- 🚀 **Production Ready** - Caddy with automatic HTTPS
- 📈 **Optimized API** - merged time slots for better performance

## 🚀 Quick Start

```bash
# Clone repository
git clone https://github.com/DarkEric/game-planner.git
cd game-planner

# Setup
cp .env.example .env

# Run (Windows)
start.bat

# Run (Linux/Mac)
docker-compose up -d
```

Open http://localhost and register with invite code: **FIRST-USER-INVITE-2025**

## 📚 Documentation

- [English README](README.en.md) - Full documentation
- [Russian README](README.md) - Полная документация
- [Quick Start Guide](QUICK_START.en.md) - 5-minute setup
- [Production Setup](PRODUCTION_SETUP.md) - Deploy to production
- [Troubleshooting](TROUBLESHOOTING.md) - Common issues

## 🛠 Tech Stack

**Backend:** Java 17, Spring Boot, PostgreSQL, Liquibase  
**Frontend:** React 18, Vite  
**Deployment:** Docker, Docker Compose, Caddy

## 🌍 Localization

- **Interface:** Partial (RU/EN) - main elements translated
- **Documentation:** Complete (RU/EN)
- **Auto-detection:** Browser language on first launch

## 📦 What's Included

- ✅ User authentication with JWT
- ✅ Interactive calendar with timezone support
- ✅ Game scheduling and management
- ✅ Invite system for controlled access
- ✅ Automatic data cleanup
- ✅ Docker deployment setup
- ✅ Production-ready configuration
- ✅ Comprehensive documentation

## 🐛 Known Issues

- Interface localization is partial (work in progress)
- Mobile UI can be improved

## 🗺️ Roadmap

- Complete English interface localization
- Mobile app
- Game library integration
- Notifications system
- Calendar export (iCal)

## 💬 Feedback

Found a bug? Have a suggestion? [Open an issue](https://github.com/DarkEric/game-planner/issues)!

## 📝 License

MIT License

---

**Full Changelog**: https://github.com/DarkEric/game-planner/commits/v1.0.0
