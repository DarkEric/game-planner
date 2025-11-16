# ✅ Release Checklist v1.0.0

## Статус выполнения

### ✅ Подготовка кода
- [x] Все изменения закоммичены
- [x] Код запушен в main
- [x] Тесты пройдены (если есть)
- [x] Документация обновлена

### ✅ Docker образы
- [x] GitHub Actions workflow создан (`.github/workflows/docker-publish.yml`)
- [x] docker-compose.ghcr.yml создан
- [x] Документация по Docker образам создана
- [x] Тег v1.0.0 создан и запушен
- [ ] **ДЕЙСТВИЕ ТРЕБУЕТСЯ:** Проверить сборку образов в GitHub Actions

### ⏳ GitHub Release
- [ ] **ДЕЙСТВИЕ ТРЕБУЕТСЯ:** Создать Release на GitHub
- [ ] **ДЕЙСТВИЕ ТРЕБУЕТСЯ:** Сделать пакеты публичными

### ⏳ Проверка
- [ ] Протестировать установку с готовыми образами
- [ ] Проверить документацию

---

## 🚀 Следующие шаги

### 1. Проверьте сборку Docker образов

**Перейдите на:** https://github.com/DarkEric/game-planner/actions

Вы должны увидеть запущенный workflow "Build and Push Docker Images".

**Ожидаемое время:** 5-10 минут

**Что происходит:**
- Сборка backend образа (Java + Spring Boot)
- Сборка frontend образа (React + Nginx)
- Создание образов для amd64 и arm64
- Публикация в GitHub Container Registry

**Если сборка успешна:**
- ✅ Зеленая галочка
- Образы доступны на: https://github.com/DarkEric?tab=packages

**Если сборка провалилась:**
- ❌ Красный крестик
- Нажмите на workflow для просмотра логов
- Исправьте ошибки и создайте новый тег

### 2. Сделайте Docker пакеты публичными

После успешной сборки:

**Backend:**
1. Перейдите: https://github.com/users/DarkEric/packages/container/game-planner-backend
2. Нажмите "Package settings" (справа)
3. Прокрутите до "Danger Zone"
4. Найдите "Change package visibility"
5. Выберите "Public"
6. Введите название пакета для подтверждения
7. Нажмите "I understand, change package visibility"

**Frontend:**
1. Перейдите: https://github.com/users/DarkEric/packages/container/game-planner-frontend
2. Повторите те же шаги

### 3. Создайте GitHub Release

**Перейдите на:** https://github.com/DarkEric/game-planner/releases

1. Нажмите **"Draft a new release"**

2. Заполните форму:
   - **Tag:** `v1.0.0` (выберите из списка)
   - **Title:** `🎲 Game Planner v1.0.0 - First Stable Release`
   - **Description:** Скопируйте из `RELEASE_NOTES_1.0.0.md`

3. Настройки:
   - ✅ Set as the latest release
   - ⬜ Set as a pre-release (НЕ отмечать)

4. Нажмите **"Publish release"**

### 4. Протестируйте установку

После публикации образов протестируйте установку:

```bash
# Создайте тестовую директорию
mkdir test-install
cd test-install

# Скачайте только необходимые файлы
curl -O https://raw.githubusercontent.com/DarkEric/game-planner/v1.0.0/docker-compose.ghcr.yml
curl -O https://raw.githubusercontent.com/DarkEric/game-planner/v1.0.0/.env.example
curl -O https://raw.githubusercontent.com/DarkEric/game-planner/v1.0.0/Caddyfile

# Настройте
copy .env.example .env

# Запустите
docker-compose -f docker-compose.ghcr.yml up -d

# Проверьте
curl http://localhost
```

### 5. Обновите README badges (Опционально)

Добавьте в README.md в начало:

```markdown
![Version](https://img.shields.io/github/v/release/DarkEric/game-planner)
![Docker Pulls](https://img.shields.io/docker/pulls/darkeric/game-planner-backend)
![License](https://img.shields.io/github/license/DarkEric/game-planner)
```

---

## 📊 Проверка результатов

После выполнения всех шагов проверьте:

### GitHub Release
- [ ] Release опубликован: https://github.com/DarkEric/game-planner/releases/tag/v1.0.0
- [ ] Исходники доступны для скачивания
- [ ] Описание корректно отображается

### Docker Images
- [ ] Backend образ публичный: https://github.com/DarkEric/packages/container/game-planner-backend
- [ ] Frontend образ публичный: https://github.com/DarkEric/packages/container/game-planner-frontend
- [ ] Образы можно скачать без авторизации:
  ```bash
  docker pull ghcr.io/darkeric/game-planner-backend:1.0.0
  docker pull ghcr.io/darkeric/game-planner-frontend:1.0.0
  ```

### Документация
- [ ] README.md обновлен
- [ ] README.en.md обновлен
- [ ] DOCKER_IMAGES.md создан
- [ ] DOCKER_PUBLISH_GUIDE.md создан
- [ ] RELEASE_NOTES_1.0.0.md обновлен

---

## 🎉 Готово!

После выполнения всех шагов:

1. **Анонсируйте релиз** в социальных сетях
2. **Мониторьте Issues** на GitHub
3. **Собирайте feedback** от пользователей
4. **Планируйте v1.1.0** на основе отзывов

---

## 📞 Помощь

Если что-то пошло не так:

- **GitHub Actions не запустился:** Проверьте, что тег создан правильно
- **Сборка провалилась:** Проверьте логи в Actions
- **Образы не публичные:** Измените видимость в Package settings
- **Образы не скачиваются:** Проверьте имена образов и теги

**Документация:**
- [GitHub Actions](https://docs.github.com/en/actions)
- [GitHub Packages](https://docs.github.com/en/packages)
- [Docker Documentation](https://docs.docker.com/)
