# 🎲 Game Planner

A web application for planning board game sessions with friends. Mark your available time, find common slots, and schedule games together!

> **Note**: The interface is currently in Russian. English localization is planned for future releases. However, the app is fully functional and this guide will help you navigate it.

## ✨ Features

- 📅 **Interactive Calendar** - mark available time with drag-and-drop
- 🎯 **Smart Slot Finder** - automatically finds the best time for everyone
- 🎮 **Game Scheduling** - create games with title, description, and participants
- 🌍 **Timezone Support** - everyone sees time in their own timezone
- 🔐 **Invite System** - registration by invitation only
- 🎨 **Dark Theme** - easy on the eyes
- 📱 **Responsive Design** - works on any screen size

## 🚀 Quick Start

### Requirements

- Docker and Docker Compose
- Git

### Installation

1. Clone the repository:
```bash
git clone https://github.com/DarkEric/game-planner.git
cd game-planner
```

2. Create `.env` file from example:
```bash
# Windows
copy .env.example .env

# Linux/Mac
cp .env.example .env
```

3. Start the application:
```bash
# Windows
start.bat

# Linux/Mac
docker-compose up -d
```

4. Open in browser: http://localhost

### First Launch

For the first registration, use the invite code:
```
FIRST-USER-INVITE-2025
```

After registration, you can create new invite codes for your friends.

## 📖 Usage Guide (Interface Translation)

### Main Interface Elements

- **Ваш профиль** = Your Profile
- **Имя** = Name
- **Цвет** = Color
- **Часовой пояс** = Timezone
- **Запланировать игру** = Schedule Game
- **Мои инвайт-коды** = My Invite Codes
- **Выйти** = Logout

### Marking Available Time

1. Log in to the system
2. Click or drag mouse over calendar cells
3. Your time will be saved automatically

**Calendar Navigation:**
- **← Предыдущие** = ← Previous
- **Сегодня** = Today
- **Следующие →** = Next →

### Scheduling a Game

1. Click **"🎲 Запланировать игру"** (Schedule Game) button
2. Choose time from **"Топ-10 лучших слотов"** (Top-10 best slots) or set manually
3. Add title and description (optional)
4. The game will be created with participants who have availability at that time

**Game Scheduler Fields:**
- **Название игры** = Game Title
- **Описание** = Description
- **Начало** = Start
- **Конец** = End
- **Отмена** = Cancel
- **Запланировать игру** = Schedule Game

### Managing Invites

1. In **"Мои инвайт-коды"** (My Invite Codes) section, click **"+ Создать инвайт"** (Create Invite)
2. Copy the code and send it to a friend
3. Each invite is single-use

**Invite Status:**
- **✓ Активен** = Active
- **✗ Неактивен** = Inactive
- **Использований** = Uses
- **📋** = Copy button
- **🗑️** = Delete button

### Game Details

When you click on a game in the calendar:
- **Игра** = Game
- **Участники** = Participants
- **Записаться** = Join
- **Покинуть** = Leave
- **Удалить игру** = Delete Game
- **Закрыть** = Close

## 🛠 Technologies

### Backend
- Java 17 + Spring Boot
- PostgreSQL
- Liquibase for migrations
- JWT authentication

### Frontend
- React 18
- Vite
- CSS Modules

### Deployment
- Docker & Docker Compose
- Caddy (automatic HTTPS)

## 📁 Project Structure

```
game-planner/
├── game-planner-back/     # Backend (Spring Boot)
├── game-planer-front/     # Frontend (React)
├── docker-compose.yml     # Development setup
├── docker-compose.prod.yml # Production setup
├── start.bat              # Windows launcher
└── README.md
```

## 🔧 Configuration

### Environment Variables

Create `.env` file in project root:

```env
# Database
POSTGRES_DB=game_planner
POSTGRES_USER=postgres
POSTGRES_PASSWORD=your_secure_password

# Backend
JWT_SECRET=your_jwt_secret_key_here
SPRING_PROFILES_ACTIVE=prod

# Frontend
VITE_API_URL=http://localhost:8080
```

### Production Deployment

For production use `docker-compose.prod.yml`:

```bash
docker-compose -f docker-compose.prod.yml up -d
```

See [PRODUCTION_SETUP.md](PRODUCTION_SETUP.md) for details.

## 🌍 Timezone Configuration

The app automatically detects your timezone, but you can change it:

1. Go to your profile
2. Click on **"Часовой пояс"** (Timezone) dropdown
3. Select your timezone or click **"Определить автоматически"** (Auto-detect)

## 🐛 Troubleshooting

See [TROUBLESHOOTING.md](TROUBLESHOOTING.md)

### Common Issues

**Port 80 already in use:**
```yaml
# In docker-compose.yml change port
services:
  caddy:
    ports:
      - "8080:80"  # Instead of "80:80"
```

**Database not starting:**
```bash
docker-compose down -v
docker-compose up -d
```

**App not working after update:**
```bash
docker-compose down
docker-compose up -d --build
```

## 📝 License

MIT License

## 👥 Author

DarkEric

## 🤝 Contributing

Pull requests are welcome! For major changes, please open an issue first.

## 🗺️ Roadmap

- [ ] English interface localization
- [ ] Mobile app
- [ ] Game library integration
- [ ] Notifications system
- [ ] Calendar export (iCal)

## 💬 Support

- Create an [Issue](https://github.com/DarkEric/game-planner/issues) on GitHub
- Check [Troubleshooting Guide](TROUBLESHOOTING.md)

---

**Note for English speakers**: While the interface is in Russian, all functionality works perfectly. Use this guide to navigate the app. English localization is coming soon!
