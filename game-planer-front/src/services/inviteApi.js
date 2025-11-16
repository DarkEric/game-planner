const API_BASE_URL = import.meta.env.VITE_API_URL || '/api'

const getToken = () => localStorage.getItem('authToken')

const getAuthHeaders = () => {
  const token = getToken()
  return {
    'Content-Type': 'application/json',
    ...(token && { 'Authorization': `Bearer ${token}` })
  }
}

export const inviteApi = {
  async createInvite(expiresAt = null, maxUses = null) {
    const response = await fetch(`${API_BASE_URL}/invites`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify({
        expiresAt,
        maxUses
      })
    })
    
    if (!response.ok) {
      throw new Error('Failed to create invite')
    }
    
    return await response.json()
  },

  async getInviteByCode(code) {
    const response = await fetch(`${API_BASE_URL}/invites/${code}`)
    
    if (!response.ok) {
      throw new Error('Invalid invite code')
    }
    
    return await response.json()
  },

  async getMyInvites() {
    const response = await fetch(`${API_BASE_URL}/invites/my`, {
      headers: getAuthHeaders()
    })
    
    if (!response.ok) {
      throw new Error('Failed to fetch invites')
    }
    
    return await response.json()
  },

  async deleteInvite(inviteId) {
    const response = await fetch(`${API_BASE_URL}/invites/${inviteId}`, {
      method: 'DELETE',
      headers: getAuthHeaders()
    })
    
    if (!response.ok) {
      throw new Error('Failed to delete invite')
    }
  }
}
