# 🚀 Game Planner Quick Start

[English version](QUICK_START.en.md) | [Русская версия](QUICK_START.md)

## Minimal Setup (5 minutes)

### 1. Install Docker

- **Windows**: [Docker Desktop](https://www.docker.com/products/docker-desktop)
- **Linux**: `sudo apt install docker.io docker-compose`
- **Mac**: [Docker Desktop](https://www.docker.com/products/docker-desktop)

### 2. Clone the Project

```bash
git clone https://github.com/DarkEric/game-planner.git
cd game-planner
```

### 3. Create .env File

```bash
# Windows
copy .env.example .env

# Linux/Mac
cp .env.example .env
```

### 4. Start

```bash
# Windows - just run
start.bat

# Linux/Mac
docker-compose up -d
```

### 5. Open Browser

Go to http://localhost

### 6. Register

Use invite code: `FIRST-USER-INVITE-2025`

## ✅ Done!

Now you can:
- Mark your available time
- Invite friends (create invite codes)
- Schedule games

## 🗺️ Interface Guide (Russian → English)

### Main Buttons
- **Запланировать игру** = Schedule Game
- **Создать инвайт** = Create Invite
- **Выйти** = Logout

### Calendar
- **← Предыдущие** = ← Previous
- **Сегодня** = Today
- **Следующие →** = Next →

### Profile
- **Ваш профиль** = Your Profile
- **Имя** = Name
- **Цвет** = Color
- **Часовой пояс** = Timezone

### Game Scheduler
- **Название игры** = Game Title
- **Описание** = Description
- **Начало** = Start
- **Конец** = End
- **Топ-10 лучших слотов** = Top-10 Best Slots
- **Отмена** = Cancel
- **Запланировать игру** = Schedule Game

### Game Details
- **Участники** = Participants
- **Записаться** = Join
- **Покинуть** = Leave
- **Удалить игру** = Delete Game
- **Закрыть** = Close

### Invites
- **Мои инвайт-коды** = My Invite Codes
- **+ Создать инвайт** = + Create Invite
- **✓ Активен** = Active
- **✗ Неактивен** = Inactive
- **Использований** = Uses

## 🔧 Commands

```bash
# Start
docker-compose up -d

# Stop
docker-compose down

# View logs
docker-compose logs -f

# Restart
docker-compose restart

# Update
git pull
docker-compose down
docker-compose up -d --build
```

## 🆘 Problems?

### Port 80 is Busy

Change port in `docker-compose.yml`:
```yaml
ports:
  - "8080:80"  # Now available at localhost:8080
```

### Database Won't Start

Remove volume and recreate:
```bash
docker-compose down -v
docker-compose up -d
```

### Not Working After Update

Rebuild containers:
```bash
docker-compose down
docker-compose up -d --build
```

## 📚 Next Steps

- [Production Setup](PRODUCTION_SETUP.md) - for public deployment
- [Troubleshooting](TROUBLESHOOTING.md) - problem solving
- [README](README.en.md) - full documentation

## 💡 Tips

1. **Timezone**: The app auto-detects your timezone, but you can change it in profile settings
2. **Invite Codes**: Each code is single-use. Create new ones for each friend
3. **Calendar**: Click or drag to mark your available time
4. **Best Slots**: The app automatically finds times when most players are available
5. **Games**: Only players with matching availability are added automatically

## 🌍 Language Note

The interface is currently in Russian, but this guide helps you navigate all features. English localization is planned for future releases.
