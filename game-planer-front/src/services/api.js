const API_BASE_URL = import.meta.env.VITE_API_URL || '/api'

// Получить токен из localStorage
const getToken = () => {
  return localStorage.getItem('authToken')
}

// Сохранить токен в localStorage
const setToken = (token) => {
  localStorage.setItem('authToken', token)
}

// Удалить токен из localStorage
const removeToken = () => {
  localStorage.removeItem('authToken')
}

// Получить заголовки с авторизацией
const getAuthHeaders = () => {
  const token = getToken()
  return {
    'Content-Type': 'application/json',
    ...(token && { 'Authorization': `Bearer ${token}` })
  }
}

// Обработка ошибок авторизации
const handleAuthError = (response) => {
  if (response.status === 401 || response.status === 403) {
    removeToken()
    window.location.reload()
  }
}

// Вспомогательная функция для парсинга даты как локального времени
const parseLocalDateTime = (dateStr) => {
  if (dateStr && typeof dateStr === 'string' && dateStr.includes('T')) {
    const [datePart, timePart] = dateStr.split('T')
    const [year, month, day] = datePart.split('-').map(Number)
    const [hours, minutes, seconds = 0] = (timePart || '00:00:00').split(':').map(Number)
    // Создаем Date в локальном времени (без конвертации часового пояса)
    return new Date(year, month - 1, day, hours, minutes, seconds)
  }
  return new Date(dateStr)
}

// Вспомогательная функция для форматирования даты в локальное время без часового пояса
const formatLocalDateTime = (date) => {
  const d = date instanceof Date ? date : new Date(date)
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hours = String(d.getHours()).padStart(2, '0')
  const minutes = String(d.getMinutes()).padStart(2, '0')
  const seconds = String(d.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}`
}

export const authApi = {
  // Регистрация
  async register(username, password, email) {
    const response = await fetch(`${API_BASE_URL}/auth/register`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ username, password, email })
    })
    if (!response.ok) {
      const errorText = await response.text()
      throw new Error(errorText || 'Failed to register')
    }
    const data = await response.json()
    setToken(data.token)
    return data
  },

  // Вход
  async login(username, password) {
    const response = await fetch(`${API_BASE_URL}/auth/login`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ username, password })
    })
    if (!response.ok) {
      throw new Error('Invalid username or password')
    }
    const data = await response.json()
    setToken(data.token)
    return data
  },

  // Выход
  logout() {
    removeToken()
  },

  // Проверить, авторизован ли пользователь
  isAuthenticated() {
    return !!getToken()
  }
}

export const playerApi = {
  // Получить всех пользователей (игроков)
  async getAllPlayers() {
    const response = await fetch(`${API_BASE_URL}/players`, {
      headers: getAuthHeaders()
    })
    if (!response.ok) {
      handleAuthError(response)
      throw new Error('Failed to fetch players')
    }
    const data = await response.json()
    // Преобразуем данные из формата бэкенда в формат фронтенда
    return data.map(player => ({
      id: player.id,
      name: player.name,
      color: player.color,
      availableTimes: (player.availableTimes || []).map(ts => ({
        start: parseLocalDateTime(ts.start),
        duration: ts.duration || 1
      }))
    }))
  },

  // Получить текущего пользователя
  async getCurrentPlayer() {
    const response = await fetch(`${API_BASE_URL}/players/me`, {
      headers: getAuthHeaders()
    })
    if (!response.ok) {
      handleAuthError(response)
      throw new Error('Failed to fetch current player')
    }
    const data = await response.json()
    return {
      id: data.id,
      name: data.name,
      color: data.color,
      availableTimes: (data.availableTimes || []).map(ts => ({
        start: parseLocalDateTime(ts.start),
        duration: ts.duration || 1
      }))
    }
  },

  // Обновить профиль текущего пользователя
  async updateCurrentPlayer(name, color) {
    const response = await fetch(`${API_BASE_URL}/players/me`, {
      method: 'PUT',
      headers: getAuthHeaders(),
      body: JSON.stringify({ name, color })
    })
    if (!response.ok) {
      handleAuthError(response)
      throw new Error('Failed to update player')
    }
    const data = await response.json()
    return {
      id: data.id,
      name: data.name,
      color: data.color,
      availableTimes: (data.availableTimes || []).map(ts => ({
        start: parseLocalDateTime(ts.start),
        duration: ts.duration || 1
      }))
    }
  },

  // Переключить временной слот (добавить/удалить) для текущего пользователя
  async toggleTimeSlot(start, duration = 1) {
    const startLocal = formatLocalDateTime(start)
    
    const response = await fetch(`${API_BASE_URL}/players/me/time-slots/toggle`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify({
        start: startLocal,
        duration: duration
      })
    })
    if (!response.ok) {
      handleAuthError(response)
      throw new Error('Failed to toggle time slot')
    }
    const data = await response.json()
    return {
      id: data.id,
      name: data.name,
      color: data.color,
      availableTimes: (data.availableTimes || []).map(ts => ({
        start: parseLocalDateTime(ts.start),
        duration: ts.duration || 1
      }))
    }
  },

  // Переключить несколько временных слотов (для drag selection)
  async toggleTimeSlots(slots) {
    const slotsData = slots.map(slot => {
      const slotDate = slot.date instanceof Date ? slot.date : new Date(slot.date)
      slotDate.setHours(slot.hour, 0, 0, 0)
      
      return {
        start: formatLocalDateTime(slotDate),
        duration: 1
      }
    })
    
    const response = await fetch(`${API_BASE_URL}/players/me/time-slots/toggle-batch`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify({
        slots: slotsData
      })
    })
    if (!response.ok) {
      handleAuthError(response)
      throw new Error('Failed to toggle time slots')
    }
    const data = await response.json()
    return {
      id: data.id,
      name: data.name,
      color: data.color,
      availableTimes: (data.availableTimes || []).map(ts => ({
        start: parseLocalDateTime(ts.start),
        duration: ts.duration || 1
      }))
    }
  }
}

