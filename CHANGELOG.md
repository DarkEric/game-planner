# Changelog

All notable changes to this project will be documented in this file.

## [1.2.0] - 2025-11-17

### ✨ Added
- **Game Cancellation Notifications** - Telegram notifications when games are cancelled
  - Optional cancellation reason field
  - Formatted notification with reason
- **Player Tooltips** - Hover over time slots to see who is available
  - Custom styled tooltip component
  - Shows player names with colors
  - Smooth hover interactions
- **Configurable Timezone for Telegram** - Set timezone for notifications
  - Support for all IANA timezones
  - Automatic timezone name mapping for Russian cities
  - Display timezone in notifications (e.g., "по Москве")
  - Default: Europe/Moscow

### 📚 Documentation
- **Reorganized Documentation Structure**
  - Moved all docs to organized folders (docs/guides, docs/telegram, docs/setup)
  - Created docs/INDEX.md as main documentation index
  - Added SIMPLE_GUIDE.md for beginners
  - Added QUICK_START_VISUAL.md with ASCII diagrams
  - Added comprehensive FAQ.md (50+ questions)
  - Cleaner root directory

### 🔧 Improved
- Better documentation navigation
- Simplified guides for beginners
- Clear folder structure

## [1.1.0] - 2025-11-17

### ✨ Added
- **Telegram Notifications** - Send notifications to Telegram when new games are created
  - Optional feature (disabled by default)
  - Configurable via environment variables
  - Supports personal chats, groups, and channels
  - Rich HTML-formatted messages with game details and direct link
  - See [TELEGRAM_SETUP.md](TELEGRAM_SETUP.md) for setup instructions
- **Telegram Topics Support** - Send notifications to specific topics in supergroups
  - Configure via `TELEGRAM_BOT_THREAD_ID` environment variable
  - Automatic thread ID handling with graceful fallback
  - See [docs/TELEGRAM_TOPICS.md](docs/TELEGRAM_TOPICS.md) for details

### 🔧 Improved
- Enhanced logging for Telegram integration
  - Configuration logging on startup
  - Detailed debug logs for message sending
  - Better error messages for troubleshooting
- Thread ID parsing with whitespace trimming
- Comprehensive error handling for invalid configurations

### 📚 Documentation
- Added [TELEGRAM_SETUP.md](TELEGRAM_SETUP.md) - Complete setup guide
- Added [TELEGRAM_QUICK_START.md](TELEGRAM_QUICK_START.md) - 3-step quick start
- Added [docs/TELEGRAM_TOPICS.md](docs/TELEGRAM_TOPICS.md) - Topics setup guide
- Added [docs/TELEGRAM_DEBUG.md](docs/TELEGRAM_DEBUG.md) - Troubleshooting guide
- Added [docs/TELEGRAM_INTEGRATION.md](docs/TELEGRAM_INTEGRATION.md) - Technical docs
- Added [docs/TELEGRAM_TESTING.md](docs/TELEGRAM_TESTING.md) - Testing guide
- Updated README.md and README.en.md with Telegram features

### 🐛 Fixed
- Thread ID not being applied due to whitespace in configuration
- Missing debug logs for thread ID operations

### 🔄 Changed
- Updated all docker-compose files with Telegram environment variables
- Updated .env.example and .env.production.example with Telegram configuration

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
