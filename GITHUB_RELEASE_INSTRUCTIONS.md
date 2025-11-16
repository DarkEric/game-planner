# 📦 Инструкция по созданию GitHub Release v1.0.0

## Шаги для создания релиза на GitHub

### 1. Перейдите на страницу релизов

Откройте: https://github.com/DarkEric/game-planner/releases

### 2. Нажмите "Draft a new release"

### 3. Заполните форму релиза

**Tag version:** `v1.0.0` (уже создан и запушен)

**Release title:** `🎲 Game Planner v1.0.0 - First Stable Release`

**Description:** Скопируйте содержимое из файла `RELEASE_NOTES_1.0.0.md`

### 4. Отметьте чекбоксы

- ✅ Set as the latest release
- ⬜ Set as a pre-release (НЕ отмечать)

### 5. Опционально: Добавьте assets

Можно добавить:
- Скриншоты приложения
- Архив с исходниками (GitHub создаст автоматически)

### 6. Нажмите "Publish release"

## ✅ Готово!

Релиз опубликован и доступен по адресу:
https://github.com/DarkEric/game-planner/releases/tag/v1.0.0

## 📢 Что дальше?

### Анонсируйте релиз

1. **README Badge** - добавьте бейдж версии:
```markdown
![Version](https://img.shields.io/github/v/release/DarkEric/game-planner)
```

2. **Social Media** - поделитесь ссылкой на релиз

3. **Community** - расскажите в сообществах настольных игр

### Мониторинг

- Следите за Issues: https://github.com/DarkEric/game-planner/issues
- Отвечайте на вопросы пользователей
- Собирайте feedback для следующих версий

## 🔄 Для следующих релизов

### Версионирование (Semantic Versioning)

- **MAJOR** (x.0.0) - несовместимые изменения API
- **MINOR** (1.x.0) - новые функции, обратно совместимые
- **PATCH** (1.0.x) - исправления багов

### Процесс релиза

```bash
# 1. Обновите CHANGELOG.md
# 2. Создайте коммит
git add CHANGELOG.md
git commit -m "docs: update changelog for vX.Y.Z"

# 3. Создайте тег
git tag -a vX.Y.Z -m "Release vX.Y.Z"

# 4. Запушьте
git push origin main
git push origin vX.Y.Z

# 5. Создайте Release на GitHub
```

## 📊 Метрики успеха

Отслеживайте:
- ⭐ Stars на GitHub
- 🍴 Forks
- 📥 Downloads
- 🐛 Issues (открытые/закрытые)
- 💬 Discussions

## 🎯 Цели для v1.1.0

- Полная локализация интерфейса на английский
- Улучшение мобильной версии
- Система уведомлений
- Экспорт календаря в iCal

---

**Поздравляем с первым релизом!** 🎉
