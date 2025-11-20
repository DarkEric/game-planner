const API_BASE_URL = import.meta.env.VITE_API_URL || '/api'
const API_URL = `${API_BASE_URL}/campaigns`

const getToken = () => localStorage.getItem('authToken')

const getAuthHeaders = () => {
  const token = getToken()
  return {
    'Content-Type': 'application/json',
    ...(token && { 'Authorization': `Bearer ${token}` })
  }
}

export const campaignApi = {
  // Get all campaigns for current user
  getUserCampaigns: async () => {
    const response = await fetch(API_URL, {
      method: 'GET',
      headers: getAuthHeaders()
    })
    if (!response.ok) throw new Error('Failed to get campaigns')
    return response.json()
  },

  // Get campaign details
  getCampaignDetails: async (campaignId) => {
    const response = await fetch(`${API_URL}/${campaignId}`, {
      method: 'GET',
      headers: getAuthHeaders()
    })
    if (!response.ok) throw new Error('Failed to get campaign details')
    return response.json()
  },

  // Create new campaign
  createCampaign: async (campaignData) => {
    const response = await fetch(API_URL, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify(campaignData)
    })
    if (!response.ok) throw new Error('Failed to create campaign')
    return response.json()
  },

  // Update campaign status
  updateCampaignStatus: async (campaignId, status) => {
    const response = await fetch(`${API_URL}/${campaignId}/status`, {
      method: 'PUT',
      headers: getAuthHeaders(),
      body: JSON.stringify({ status })
    })
    if (!response.ok) throw new Error('Failed to update campaign status')
    return response.json()
  },

  // Update milestones (master only)
  updateMilestones: async (campaignId, completedMilestones, totalMilestones) => {
    const response = await fetch(`${API_URL}/${campaignId}/milestones`, {
      method: 'PUT',
      headers: getAuthHeaders(),
      body: JSON.stringify({ 
        completedMilestones: completedMilestones !== undefined ? completedMilestones : null,
        totalMilestones: totalMilestones !== undefined ? totalMilestones : null
      })
    })
    if (!response.ok) throw new Error('Failed to update milestones')
    return response.json()
  },

  // Add game to campaign
  addGameToCampaign: async (campaignId, gameId) => {
    const response = await fetch(`${API_URL}/${campaignId}/games/${gameId}`, {
      method: 'POST',
      headers: getAuthHeaders()
    })
    if (!response.ok) throw new Error('Failed to add game to campaign')
    return response.json()
  },

  // Remove game from campaign
  removeGameFromCampaign: async (campaignId, gameId) => {
    const response = await fetch(`${API_URL}/${campaignId}/games/${gameId}`, {
      method: 'DELETE',
      headers: getAuthHeaders()
    })
    if (!response.ok) throw new Error('Failed to remove game from campaign')
    return response.json()
  },

  // Add player to campaign
  addPlayerToCampaign: async (campaignId, playerData) => {
    const response = await fetch(`${API_URL}/${campaignId}/players`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify(playerData)
    })
    if (!response.ok) throw new Error('Failed to add player to campaign')
    return response.json()
  },

  // Update character
  updateCharacter: async (campaignId, playerId, characterData) => {
    const response = await fetch(`${API_URL}/${campaignId}/players/${playerId}`, {
      method: 'PUT',
      headers: getAuthHeaders(),
      body: JSON.stringify(characterData)
    })
    if (!response.ok) throw new Error('Failed to update character')
    return response.json()
  },

  // Remove player from campaign
  removePlayerFromCampaign: async (campaignId, playerId) => {
    const response = await fetch(`${API_URL}/${campaignId}/players/${playerId}`, {
      method: 'DELETE',
      headers: getAuthHeaders()
    })
    if (!response.ok) throw new Error('Failed to remove player from campaign')
    return response.json()
  }
}
