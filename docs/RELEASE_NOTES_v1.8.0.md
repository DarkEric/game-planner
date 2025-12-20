# Release Notes v1.8.0

## Дата релиза
2025-12-20

## Основные изменения

### ✨ Новые возможности

#### Масштабный рефакторинг TelegramNotificationService
- **Разделение монолитного класса на модульную архитектуру**
  - Класс сокращен с 5228 строк до 341 строки (сокращение на 93.5%)
  - Создано 63 класса в пакете `telegram` для организации кода
  - Разделение на 8 логических подпакетов: `util`, `state`, `command`, `menu`, `message`, `keyboard`, `notification`, `config`, `exception`

#### Улучшенная архитектура Telegram бота
- **Модульная структура компонентов:**
  - 14 обработчиков команд (`CommandHandler`) с единым интерфейсом
  - 9 обработчиков меню (`MenuHandler`) для callback'ов
  - 5 систем состояний через унифицированный `StateManager`
  - 5 обработчиков состояний (`StateHandler`) для диалогов
  - 5 билдеров сообщений (`MessageBuilder`)
  - 7 билдеров клавиатур (`KeyboardBuilder`)
  - 3 класса для отправки уведомлений (`NotificationSender`)

#### Улучшенные настройки уведомлений
- **Более понятные описания кнопок настроек**
  - Вместо статуса ("✓ Включено") теперь показывается описание настройки ("Напоминание о разметке: Включено")
  - Пользователи сразу видят, за что отвечает каждая настройка
  - Примеры: "🎮 Игра создана: Все игры", "📅 Напоминание о разметке: Включено"

#### Исправление списка игр в Telegram
- **Список предстоящих игр теперь показывает все игры**
  - Ранее показывались только игры, на которые записан пользователь
  - Теперь показываются все предстоящие игры на 60 дней вперед
  - Пользователи могут видеть доступные игры и записываться на них

### 🔧 Улучшения

#### Исправление циклических зависимостей
- **Устранены все циклические зависимости Spring**
  - Добавлена аннотация `@Lazy` к `AbsSender` во всех компонентах
  - Исправлены циклы: `TelegramNotificationService` → `CommandRouter` → `CommandHandler` → `AbsSender`
  - Исправлены циклы: `TelegramNotificationService` → `MenuRouter` → `MenuHandler` → `MenuMessageUpdater` → `AbsSender`
  - Приложение теперь корректно запускается без ошибок

#### Конфигурируемость
- **Все константы вынесены в конфигурацию**
  - Создан `TelegramBotProperties` с `@ConfigurationProperties`
  - Все таймауты, лимиты попыток и другие параметры настраиваются через `application.properties`
  - Используется префикс `telegram.bot` для всех настроек

#### Централизованная обработка ошибок
- **Создан `TelegramExceptionHandler`**
  - Единая точка обработки всех ошибок Telegram API
  - Специфичные сообщения для разных типов ошибок (400, 403, 429)
  - Интегрирован во все компоненты

#### Улучшение утилит
- **Создан пакет `telegram.util` с утилитами:**
  - `TelegramMessageSender` - обертка для отправки сообщений
  - `TelegramHtmlFormatter` - форматирование HTML
  - `TelegramDateParser` - парсинг дат и времени
  - `TelegramTimeFormatter` - форматирование времени (теперь Spring компонент)
  - `CronExpressionBuilder` - построение cron выражений
  - `CronExpressionParser` - парсинг cron выражений
  - `TelegramValidationUtils` - утилиты валидации

### 🐛 Исправления

- Исправлена ошибка компиляции с `getErrorCode()` в `TelegramExceptionHandler`
- Исправлена ошибка отсутствия бина `TelegramTimeFormatter`
- Исправлена ошибка отсутствия бина `TelegramMessageSender`
- Исправлено предупреждение о deprecated API в `TelegramNotificationService`
- Исправлен список игр в Telegram (теперь показывает все предстоящие игры)

### 📚 Backend

#### Новые классы и структура
- **Пакет `telegram.command`** (14 классов):
  - `CommandRouter`, `CommandHandler` (интерфейс)
  - Обработчики: `StartCommandHandler`, `StopCommandHandler`, `RegisterCommandHandler`, `AuthCommandHandler`, `LinkCommandHandler`, `GamesCommandHandler`, `GameCommandHandler`, `InviteCommandHandler`, `MyInvitesCommandHandler`, `MarkCommandHandler`, `MySlotsCommandHandler`, `MenuCommandHandler`, `HelpCommandHandler`, `CancelCommandHandler`

- **Пакет `telegram.menu`** (9 классов):
  - `MenuRouter`, `MenuHandler` (интерфейс)
  - Обработчики: `MainMenuHandler`, `GamesMenuHandler`, `TimeMenuHandler`, `SettingsMenuHandler`, `InvitesMenuHandler`, `NotificationsMenuHandler`, `TimezoneMenuHandler`, `GameActionHandler`
  - `MenuMessageUpdater` - утилита для обновления сообщений

- **Пакет `telegram.state`** (11 классов):
  - `StateManager` (интерфейс), `AbstractStateManager`
  - Менеджеры: `AuthStateManager`, `RegistrationStateManager`, `TimeSlotMarkingStateManager`, `TimezoneChangeStateManager`, `NotificationStateManager`
  - `StateRouter` - роутер для обработки состояний
  - Обработчики: `AuthStateHandler`, `RegistrationStateHandler`, `TimeSlotMarkingStateHandler`, `TimezoneChangeStateHandler`, `NotificationStateHandler`

- **Пакет `telegram.message`** (5 классов):
  - `GameMessageBuilder`, `InviteMessageBuilder`, `TimeSlotMessageBuilder`, `NotificationMessageBuilder`, `HelpMessageBuilder`

- **Пакет `telegram.keyboard`** (7 классов):
  - `MainMenuKeyboardBuilder`, `GamesMenuKeyboardBuilder`, `TimeMenuKeyboardBuilder`, `SettingsMenuKeyboardBuilder`, `InvitesMenuKeyboardBuilder`, `NotificationsMenuKeyboardBuilder`, `TimezoneSelectorKeyboardBuilder`

- **Пакет `telegram.notification`** (4 класса):
  - `NotificationSender` (базовый класс), `GroupNotificationSender`, `PersonalNotificationSender`, `GameNotificationSender`

- **Пакет `telegram.config`**:
  - `TelegramBotProperties` - конфигурационные свойства

- **Пакет `telegram.exception`**:
  - `TelegramExceptionHandler` - централизованная обработка ошибок

- **Пакет `telegram.util`** (7 классов):
  - Утилиты для форматирования, парсинга и валидации

#### Изменения в существующих классах
- `TelegramNotificationService` - упрощен до тонкого координатора (341 строка вместо 5228)
- `GameService` - обновлен метод получения игр для Telegram
- `AdminService` - добавлена функциональность удаления пользователей

### 📱 Frontend

- Обновлен `AdminUserList.jsx` с функциональностью удаления пользователей
- Добавлен `DeleteUserModal.jsx` для подтверждения удаления

## Метрики рефакторинга

### До рефакторинга:
- **Размер класса:** 5228 строк
- **Количество методов:** 197
- **Количество Map'ов:** 15+
- **Системы состояний:** 5 независимых с дублированием

### После рефакторинга:
- **Размер основного класса:** 341 строка (сокращение на **93.5%**)
- **Количество модулей:** 63 класса в пакете telegram
- **Системы состояний:** 5 унифицированных через StateManager
- **Обработчики команд:** 14 классов
- **Обработчики меню:** 9 классов
- **Message Builders:** 5 классов
- **Keyboard Builders:** 7 классов

## Технические детали

### Архитектура рефакторинга

Рефакторинг разделил монолитный класс на логические компоненты:

1. **Роутеры** - делегируют запросы соответствующим обработчикам:
   - `CommandRouter` - для команд (начинающихся с `/`)
   - `MenuRouter` - для callback'ов меню
   - `StateRouter` - для обработки состояний диалогов

2. **Обработчики** - содержат бизнес-логику:
   - `CommandHandler` - обработка команд
   - `MenuHandler` - обработка callback'ов
   - `StateHandler` - обработка состояний

3. **Билдеры** - формируют сообщения и клавиатуры:
   - `MessageBuilder` - формирование текста сообщений
   - `KeyboardBuilder` - формирование inline-клавиатур

4. **Менеджеры состояний** - управление состояниями диалогов:
   - `StateManager` - унифицированный интерфейс
   - `AbstractStateManager` - базовая реализация
   - Специализированные менеджеры для каждого типа состояний

5. **Отправители уведомлений** - отправка различных типов уведомлений:
   - `GroupNotificationSender` - групповые уведомления
   - `PersonalNotificationSender` - персональные уведомления
   - `GameNotificationSender` - уведомления об играх

### Решение циклических зависимостей

Использована аннотация `@Lazy` для разрыва циклов:
- Spring создает прокси для `AbsSender` вместо реального бина
- Реальный бин создается только при первом использовании
- Это позволяет избежать циклических зависимостей при инициализации

## Известные ограничения

- Некоторые компоненты все еще создают экземпляры утилит через `new` вместо инжекции (для обратной совместимости)
- Рекомендуется добавить unit-тесты для каждого компонента

## Благодарности

Спасибо за использование Game Planner! Если у вас есть предложения или вы нашли ошибку, пожалуйста, создайте issue в репозитории.

