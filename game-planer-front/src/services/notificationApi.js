const API_BASE_URL = import.meta.env.VITE_API_URL || '/api'

const getToken = () => localStorage.getItem('authToken')

const getAuthHeaders = () => {
  const token = getToken()
  return {
    'Content-Type': 'application/json',
    ...(token && { 'Authorization': `Bearer ${token}` })
  }
}

const handleAuthError = (response) => {
  if (response.status === 401 || response.status === 403) {
    localStorage.removeItem('authToken')
    window.location.reload()
  }
}

export const notificationApi = {
  async getNotificationSettings() {
    const response = await fetch(`${API_BASE_URL}/notification-settings`, {
      headers: getAuthHeaders()
    })
    
    if (!response.ok) {
      handleAuthError(response)
      throw new Error('Failed to fetch notification settings')
    }
    
    return await response.json()
  },
  
  async updateNotificationSettings(settings) {
    const response = await fetch(`${API_BASE_URL}/notification-settings`, {
      method: 'PUT',
      headers: getAuthHeaders(),
      body: JSON.stringify(settings)
    })
    
    if (!response.ok) {
      handleAuthError(response)
      throw new Error('Failed to update notification settings')
    }
    
    return await response.json()
  },
  
  async getTelegramLinkToken() {
    const response = await fetch(`${API_BASE_URL}/notification-settings/telegram/link-token`, {
      headers: getAuthHeaders()
    })
    
    if (!response.ok) {
      handleAuthError(response)
      throw new Error('Failed to get link token')
    }
    
    return await response.text()
  },
  
  async unlinkTelegramAccount() {
    const response = await fetch(`${API_BASE_URL}/notification-settings/telegram/unlink`, {
      method: 'POST',
      headers: getAuthHeaders()
    })
    
    if (!response.ok) {
      handleAuthError(response)
      throw new Error('Failed to unlink Telegram account')
    }
  }
}
