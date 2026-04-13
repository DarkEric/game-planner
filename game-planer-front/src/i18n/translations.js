export const translations = {
  ru: {
    // Header
    appTitle: 'Планируем игры вместе',
    appSubtitle: 'Отмечайте время, когда вы можете играть, и найдите общее время с друзьями',
    logout: 'Выйти',
    
    // Profile
    yourProfile: 'Ваш профиль',
    name: 'Имя',
    color: 'Цвет',
    timezone: 'Часовой пояс',
    scheduleGame: 'Запланировать игру',
    markTime: 'Разметить время',
    tabGames: 'Игры',
    tabCalendar: 'Календарь',
    tabCampaigns: 'Кампании',
    tabProfile: 'Профиль',
    tabAdmin: 'Админ',
    tabGamesAria: 'Предстоящие игры',
    tabCalendarAria: 'Календарь доступности',
    tabCampaignsAria: 'Кампании',
    tabProfileAria: 'Личный кабинет',
    tabAdminAria: 'Админ-панель',
    tabNavAria: 'Основные разделы',
    upcomingGamesEmpty: 'Нет запланированных игр в ближайшие дни.',
    timeMarkingTitle: 'Разметить время',
    timeMarkingHint:
      'На календаре: протяните выделение, начиная с пустой ячейки, чтобы добавить часы; начиная с уже отмеченной — чтобы убрать.',
    timeMarkingClearOthersQuestion: 'Удалить все другие размеченные слоты перед добавлением?',
    timeMarkingClearOthersYes: 'Да, очистить всё и оставить только выбранное ниже',
    timeMarkingClearOthersNo: 'Нет, только добавить к текущей разметке',
    timeMarkingStep1: 'Выберите дни',
    timeMarkingStep2: 'Время по дням',
    timeMarkingMoscowNote:
      'Время начала — в вашем часовом поясе из профиля (если не задан — как в браузере). По умолчанию 19:00.',
    timeMarkingNext: 'Далее',
    timeMarkingBack: 'Назад',
    timeMarkingSave: 'Сохранить',
    timeMarkingAnyTime: 'Без разницы',
    timeMarkingWholeDayAll: 'Без разницы для всех выбранных дней',
    timeMarkingStart: 'Начало',
    timeMarkingDurationHours: 'Часов',
    timeMarkingSelectOneDay: 'Выберите хотя бы один день.',
    timeMarkingSaveError: 'Не удалось сохранить. Попробуйте снова.',
    
    // Calendar
    previous: 'Предыдущие',
    today: 'Сегодня',
    next: 'Следующие',
    clickToMark:
      'Кликните или протяните мышь по часам: начало жеста на пустой ячейке — добавить выделенные часы, на уже отмеченной — убрать',
    availablePlayers: 'Доступно игроков',
    available: 'Доступны',
    gameParticipants: 'Участники',
    of: 'из',
    
    // Days of week
    mon: 'Пн',
    tue: 'Вт',
    wed: 'Ср',
    thu: 'Чт',
    fri: 'Пт',
    sat: 'Сб',
    sun: 'Вс',
    
    // Game Scheduler
    scheduleGameTitle: 'Запланировать игру',
    selectDateTime: 'Выберите дату и время',
    start: 'Начало',
    end: 'Конец',
    gameTitle: 'Название игры',
    gameTitlePlaceholder: 'Например: D&D сессия',
    gameTitleOptional: 'Название игры (необязательно)',
    description: 'Описание',
    descriptionPlaceholder: 'Дополнительная информация об игре...',
    descriptionOptional: 'Описание (необязательно)',
    optional: 'необязательно',
    topSlots: 'Топ-10 лучших слотов',
    noSlotsAvailable: 'Нет доступных временных слотов с 2+ игроками',
    cancel: 'Отмена',
    scheduleButton: 'Запланировать игру',
    schedulerGameTime: 'Время начала',
    schedulerDuration: 'Продолжительность, ч',
    autoAddPlayersLabel: 'Автоматически добавить доступных игроков',
    maxParticipantsLabel: 'Максимум участников (опционально)',
    maxParticipantsHint: 'Создатель игры не учитывается в этом лимите',
    maxParticipantsPlaceholder: 'Без ограничений',
    campaignOptional: 'Кампания (опционально)',
    campaignNone: 'Не привязана к кампании',
    
    // Game Details
    game: 'Игра',
    participants: 'Участники',
    join: 'Записаться',
    leave: 'Покинуть',
    deleteGame: 'Удалить игру',
    close: 'Закрыть',
    
    // Invites
    myInvites: 'Мои инвайт-коды',
    createInvite: 'Создать инвайт',
    active: 'Активен',
    inactive: 'Неактивен',
    uses: 'Использований',
    usedBy: 'Использован',
    createInviteQuestion: 'Создать одноразовый инвайт-код?',
    create: 'Создать',
    noInvites: 'У вас пока нет инвайтов. Создайте первый!',
    
    // Best Time Slots
    bestTimeSlots: 'Лучшие временные слоты',
    noSlots: 'Нет доступных слотов',
    
    // Auth
    login: 'Вход',
    register: 'Регистрация',
    username: 'Имя пользователя',
    password: 'Пароль',
    email: 'Email',
    inviteCode: 'Инвайт-код',
    loginButton: 'Войти',
    registerButton: 'Зарегистрироваться',
    switchToRegister: 'Нет аккаунта? Зарегистрируйтесь',
    switchToLogin: 'Уже есть аккаунт? Войдите',
    
    // Timezone Selector
    selectTimezone: 'Выберите часовой пояс',
    autoDetect: 'Определить автоматически',
    searchTimezone: 'Поиск часового пояса...',
    noResults: 'Ничего не найдено',
    
    // Loading
    loading: 'Загрузка игроков...',
    
    // Errors
    errorLoadData: 'Не удалось загрузить данные. Проверьте подключение к серверу.',
    errorUpdateProfile: 'Не удалось обновить профиль. Проверьте подключение к серверу.',
    errorUpdateTimezone: 'Не удалось обновить часовой пояс.',
    errorToggleSlot: 'Не удалось сохранить изменение времени. Проверьте подключение к серверу.',
    errorScheduleGame: 'Не удалось запланировать игру. Проверьте подключение к серверу.',
    errorDeleteGame: 'Не удалось удалить игру.',
    errorJoinGame: 'Не удалось записаться на игру.',
    errorLeaveGame: 'Не удалось покинуть игру.',
    errorOpenGame: 'Не удалось открыть игру по ссылке.',
  },
  
  en: {
    // Header
    appTitle: 'Plan Games Together',
    appSubtitle: 'Mark your available time and find common slots with friends',
    logout: 'Logout',
    
    // Profile
    yourProfile: 'Your Profile',
    name: 'Name',
    color: 'Color',
    timezone: 'Timezone',
    scheduleGame: 'Schedule Game',
    markTime: 'Mark availability',
    tabGames: 'Games',
    tabCalendar: 'Calendar',
    tabCampaigns: 'Campaigns',
    tabProfile: 'Profile',
    tabAdmin: 'Admin',
    tabGamesAria: 'Upcoming games',
    tabCalendarAria: 'Availability calendar',
    tabCampaignsAria: 'Campaigns',
    tabProfileAria: 'Profile',
    tabAdminAria: 'Admin panel',
    tabNavAria: 'Main sections',
    upcomingGamesEmpty: 'No scheduled games in the coming period.',
    timeMarkingTitle: 'Mark availability',
    timeMarkingHint:
      'On the calendar: drag starting from an empty cell to add hours; starting from an already marked cell to remove them.',
    timeMarkingClearOthersQuestion: 'Remove all other marked slots before adding these?',
    timeMarkingClearOthersYes: 'Yes, clear everything and keep only what you set below',
    timeMarkingClearOthersNo: 'No, only add to my current availability',
    timeMarkingStep1: 'Choose days',
    timeMarkingStep2: 'Time per day',
    timeMarkingMoscowNote:
      'Start time is in your profile timezone (browser timezone if unset). Default 19:00.',
    timeMarkingNext: 'Next',
    timeMarkingBack: 'Back',
    timeMarkingSave: 'Save',
    timeMarkingAnyTime: 'Any time',
    timeMarkingWholeDayAll: 'Any time for all selected days',
    timeMarkingStart: 'Start',
    timeMarkingDurationHours: 'Hours',
    timeMarkingSelectOneDay: 'Select at least one day.',
    timeMarkingSaveError: 'Could not save. Try again.',
    
    // Calendar
    previous: 'Previous',
    today: 'Today',
    next: 'Next',
    clickToMark:
      'Click or drag across hours: start on an empty cell to add the selection, on a marked cell to remove it',
    availablePlayers: 'Available players',
    available: 'Available',
    gameParticipants: 'Participants',
    of: 'of',
    
    // Days of week
    mon: 'Mon',
    tue: 'Tue',
    wed: 'Wed',
    thu: 'Thu',
    fri: 'Fri',
    sat: 'Sat',
    sun: 'Sun',
    
    // Game Scheduler
    scheduleGameTitle: 'Schedule Game',
    selectDateTime: 'Select Date and Time',
    start: 'Start',
    end: 'End',
    gameTitle: 'Game Title',
    gameTitlePlaceholder: 'e.g., D&D Session',
    gameTitleOptional: 'Game Title (optional)',
    description: 'Description',
    descriptionPlaceholder: 'Additional information about the game...',
    descriptionOptional: 'Description (optional)',
    optional: 'optional',
    topSlots: 'Top-10 Best Slots',
    noSlotsAvailable: 'No available time slots with 2+ players',
    cancel: 'Cancel',
    scheduleButton: 'Schedule Game',
    schedulerGameTime: 'Start time',
    schedulerDuration: 'Duration (hours)',
    autoAddPlayersLabel: 'Automatically add available players',
    maxParticipantsLabel: 'Max participants (optional)',
    maxParticipantsHint: 'The game host does not count toward this limit',
    maxParticipantsPlaceholder: 'No limit',
    campaignOptional: 'Campaign (optional)',
    campaignNone: 'No campaign',
    
    // Game Details
    game: 'Game',
    participants: 'Participants',
    join: 'Join',
    leave: 'Leave',
    deleteGame: 'Delete Game',
    close: 'Close',
    
    // Invites
    myInvites: 'My Invite Codes',
    createInvite: 'Create Invite',
    active: 'Active',
    inactive: 'Inactive',
    uses: 'Uses',
    usedBy: 'Used by',
    createInviteQuestion: 'Create a single-use invite code?',
    create: 'Create',
    noInvites: 'You don\'t have any invites yet. Create your first one!',
    
    // Best Time Slots
    bestTimeSlots: 'Best Time Slots',
    noSlots: 'No available slots',
    
    // Auth
    login: 'Login',
    register: 'Register',
    username: 'Username',
    password: 'Password',
    email: 'Email',
    inviteCode: 'Invite Code',
    loginButton: 'Log In',
    registerButton: 'Register',
    switchToRegister: 'No account? Register',
    switchToLogin: 'Already have an account? Log in',
    
    // Timezone Selector
    selectTimezone: 'Select Timezone',
    autoDetect: 'Auto-detect',
    searchTimezone: 'Search timezone...',
    noResults: 'No results found',
    
    // Loading
    loading: 'Loading players...',
    
    // Errors
    errorLoadData: 'Failed to load data. Check server connection.',
    errorUpdateProfile: 'Failed to update profile. Check server connection.',
    errorUpdateTimezone: 'Failed to update timezone.',
    errorToggleSlot: 'Failed to save time change. Check server connection.',
    errorScheduleGame: 'Failed to schedule game. Check server connection.',
    errorDeleteGame: 'Failed to delete game.',
    errorJoinGame: 'Failed to join game.',
    errorLeaveGame: 'Failed to leave game.',
    errorOpenGame: 'Could not open game from link.',
  }
}

export const getTranslation = (lang, key) => {
  return translations[lang]?.[key] || translations['en'][key] || key
}
