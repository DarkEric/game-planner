import { getUserTimezone } from '../utils/dateUtils'
import { parseFromServer, formatForServer } from '../utils/timezoneUtils'
import { utcDateFromZonedWallClock } from '../utils/zonedWallTime'

const API_BASE_URL = import.meta.env.VITE_API_URL || '/api'

// Получаем timezone пользователя (будет обновляться при изменении профиля)
let currentUserTimezone = null

export const setUserTimezone = (timezone) => {
  currentUserTimezone = timezone
}

/** @returns {string|null} последний TZ из профиля (если setUserTimezone вызывали) */
export const getLastSetUserTimezone = () => currentUserTimezone

export const getUserTimezoneForAPI = () => {
  // ВАЖНО: Всегда используем timezone браузера для корректной работы Date объектов
  // currentUserTimezone используется только для отображения в UI
  return Intl.DateTimeFormat().resolvedOptions().timeZone
}

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

export const authApi = {
  // Регистрация
  async register(username, password, email, inviteCode, name = null) {
    const response = await fetch(`${API_BASE_URL}/auth/register`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ username, password, email, inviteCode, name })
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

export const passwordResetApi = {
  // Запросить сброс пароля
  async requestPasswordReset(username) {
    const response = await fetch(`${API_BASE_URL}/auth/password-reset/request`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ username })
    })
    if (!response.ok) {
      const errorData = await response.json().catch(() => ({ error: 'Failed to request password reset' }))
      throw new Error(errorData.error || 'Ошибка при запросе сброса пароля')
    }
    return await response.json()
  },

  // Подтвердить сброс пароля
  async confirmPasswordReset(token, newPassword) {
    const response = await fetch(`${API_BASE_URL}/auth/password-reset/confirm`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ token, newPassword })
    })
    if (!response.ok) {
      const errorData = await response.json().catch(() => ({ error: 'Failed to reset password' }))
      throw new Error(errorData.error || 'Ошибка при сбросе пароля')
    }
    return await response.json()
  }
}

export const adminApi = {
  // Проверить, является ли текущий пользователь администратором
  async isAdmin() {
    const response = await fetch(`${API_BASE_URL}/admin/users/me/is-admin`, {
      headers: getAuthHeaders()
    })
    if (!response.ok) {
      return { isAdmin: false }
    }
    return await response.json()
  },

  // Получить список всех пользователей
  async getAllUsers() {
    const response = await fetch(`${API_BASE_URL}/admin/users`, {
      headers: getAuthHeaders()
    })
    if (!response.ok) {
      handleAuthError(response)
      throw new Error('Failed to fetch users')
    }
    return await response.json()
  },

  // Сбросить пароль пользователя
  async resetUserPassword(userId) {
    const response = await fetch(`${API_BASE_URL}/admin/users/${userId}/reset-password`, {
      method: 'POST',
      headers: getAuthHeaders()
    })
    if (!response.ok) {
      handleAuthError(response)
      const errorData = await response.json().catch(() => ({ error: 'Failed to reset password' }))
      throw new Error(errorData.error || 'Ошибка при сбросе пароля')
    }
    return await response.json()
  },

  // Назначить права администратора
  async grantAdminRights(userId) {
    const response = await fetch(`${API_BASE_URL}/admin/users/${userId}/grant-admin`, {
      method: 'POST',
      headers: getAuthHeaders()
    })
    if (!response.ok) {
      handleAuthError(response)
      const errorData = await response.json().catch(() => ({ error: 'Failed to grant admin rights' }))
      throw new Error(errorData.error || 'Ошибка при назначении прав')
    }
    return await response.json()
  },

  // Отозвать права администратора
  async revokeAdminRights(userId) {
    const response = await fetch(`${API_BASE_URL}/admin/users/${userId}/revoke-admin`, {
      method: 'POST',
      headers: getAuthHeaders()
    })
    if (!response.ok) {
      handleAuthError(response)
      const errorData = await response.json().catch(() => ({ error: 'Failed to revoke admin rights' }))
      throw new Error(errorData.error || 'Ошибка при отзыве прав')
    }
    return await response.json()
  },

  // Удалить пользователя (требует подтверждения паролем)
  async deleteUser(userId, password) {
    const response = await fetch(`${API_BASE_URL}/admin/users/${userId}`, {
      method: 'DELETE',
      headers: getAuthHeaders(),
      body: JSON.stringify({ password })
    })
    if (!response.ok) {
      handleAuthError(response)
      const errorData = await response.json().catch(() => ({ error: 'Failed to delete user' }))
      throw new Error(errorData.error || 'Ошибка при удалении пользователя')
    }
    return await response.json()
  }
}

export const playerApi = {
  // Получить всех пользователей (игроков)
  async getAllPlayers(startDate = null, endDate = null) {
    const userTimezone = getUserTimezoneForAPI()
    let url = `${API_BASE_URL}/players`
    
    // Добавляем параметры фильтрации если указаны
    if (startDate || endDate) {
      const params = new URLSearchParams()
      if (startDate) {
        params.append('startDate', formatForServer(startDate, userTimezone))
      }
      if (endDate) {
        params.append('endDate', formatForServer(endDate, userTimezone))
      }
      url += `?${params}`
    }
    
    const response = await fetch(url, {
      headers: getAuthHeaders()
    })
    if (!response.ok) {
      handleAuthError(response)
      throw new Error('Failed to fetch players')
    }
    const data = await response.json()
    
    // Преобразуем данные из формата бэкенда в формат фронтенда
    const currentUserTimezone = getUserTimezoneForAPI()
    
    return data.map(player => ({
      id: player.id,
      name: player.name,
      color: player.color,
      timezone: player.timezone,
      availableTimes: (player.availableTimes || []).map(ts => ({
        // Каждый игрок видит время других игроков в своем timezone
        start: parseFromServer(ts.start, currentUserTimezone),
        duration: ts.duration || 1
      }))
    }))
  },

  // Получить текущего пользователя
  async getCurrentPlayer(startDate = null, endDate = null) {
    const userTimezone = getUserTimezoneForAPI()
    let url = `${API_BASE_URL}/players/me`
    
    // Добавляем параметры фильтрации если указаны
    if (startDate || endDate) {
      const params = new URLSearchParams()
      if (startDate) {
        params.append('startDate', formatForServer(startDate, userTimezone))
      }
      if (endDate) {
        params.append('endDate', formatForServer(endDate, userTimezone))
      }
      url += `?${params}`
    }
    
    const response = await fetch(url, {
      headers: getAuthHeaders()
    })
    if (!response.ok) {
      handleAuthError(response)
      throw new Error('Failed to fetch current player')
    }
    const data = await response.json()
    
    // Устанавливаем timezone пользователя для API
    if (data.timezone) {
      setUserTimezone(data.timezone)
    }
    
    const playerTimezone = data.timezone || getUserTimezone().timezone
    return {
      id: data.id,
      name: data.name,
      color: data.color,
      timezone: data.timezone,
      availableTimes: (data.availableTimes || []).map(ts => ({
        start: parseFromServer(ts.start, playerTimezone), // Парсим из UTC
        duration: ts.duration || 1
      }))
    }
  },

  // Обновить профиль текущего пользователя
  async updateCurrentPlayer(name, color, timezone) {
    // Если timezone не передан, получаем текущий
    const tz = timezone || Intl.DateTimeFormat().resolvedOptions().timeZone
    
    const response = await fetch(`${API_BASE_URL}/players/me`, {
      method: 'PUT',
      headers: getAuthHeaders(),
      body: JSON.stringify({ name, color, timezone: tz })
    })
    if (!response.ok) {
      handleAuthError(response)
      throw new Error('Failed to update player')
    }
    const data = await response.json()
    
    const userTimezone = data.timezone || getUserTimezone().timezone
    return {
      id: data.id,
      name: data.name,
      color: data.color,
      timezone: data.timezone,
      availableTimes: (data.availableTimes || []).map(ts => ({
        start: parseFromServer(ts.start, userTimezone), // Парсим из UTC
        duration: ts.duration || 1
      }))
    }
  },

  // Переключить временной слот (добавить/удалить) для текущего пользователя
  async toggleTimeSlot(start, duration = 1) {
    const userTimezone = getUserTimezoneForAPI()
    
    const response = await fetch(`${API_BASE_URL}/players/me/time-slots/toggle`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify({
        start: formatForServer(start, userTimezone), // Конвертируем в UTC
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
      timezone: data.timezone,
      availableTimes: (data.availableTimes || []).map(ts => ({
        start: parseFromServer(ts.start, userTimezone), // Парсим из UTC
        duration: ts.duration || 1
      }))
    }
  },

  // Переключить несколько временных слотов (для drag selection)
  async toggleTimeSlots(slots, duration = 1) {
    const userTimezone = getUserTimezoneForAPI()
    
    const slotsData = slots.map(slot => {
      const date = slot.date instanceof Date ? slot.date : new Date(slot.date)
      
      // Создаем "наивную" дату с компонентами
      const slotDate = new Date(
        date.getFullYear(),
        date.getMonth(),
        date.getDate(),
        slot.hour,
        0,
        0,
        0
      )
      
      return {
        start: formatForServer(slotDate, userTimezone), // Конвертируем в UTC
        duration: slot.duration || duration
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
    const userTz = getUserTimezoneForAPI()
    return {
      id: data.id,
      name: data.name,
      color: data.color,
      timezone: data.timezone,
      availableTimes: (data.availableTimes || []).map(ts => ({
        start: parseFromServer(ts.start, userTz), // Парсим из UTC
        duration: ts.duration || 1
      }))
    }
  },

  /**
   * Массовый toggle слотов: время — настенные часы в часовом поясе профиля
   * (getLastSetUserTimezone / иначе часовой пояс браузера), не МСК.
   * @param {Array<{ year: number, month: number, day: number, hour: number, minute?: number, duration: number } | { year: number, month: number, day: number, wholeDay: true }>} slots
   */
  async toggleTimeSlotsMoscowBatch(slots) {
    const wallTz = getLastSetUserTimezone() || getUserTimezoneForAPI()
    const slotsData = slots.map(s => {
      if (s.wholeDay) {
        const start = utcDateFromZonedWallClock(s.year, s.month - 1, s.day, 0, 0, 0, wallTz)
        return {
          start: start.toISOString(),
          duration: 24
        }
      }
      const start = utcDateFromZonedWallClock(
        s.year,
        s.month - 1,
        s.day,
        s.hour,
        s.minute ?? 0,
        wallTz
      )
      return {
        start: start.toISOString(),
        duration: s.duration
      }
    })

    const response = await fetch(`${API_BASE_URL}/players/me/time-slots/toggle-batch`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify({ slots: slotsData })
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
      timezone: data.timezone,
      availableTimes: (data.availableTimes || []).map(ts => ({
        start: parseFromServer(ts.start, userTz),
        duration: ts.duration || 1
      }))
    }
  }
}

