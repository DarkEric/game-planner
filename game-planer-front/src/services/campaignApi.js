import axios from 'axios'

const API_URL = 'http://localhost:8080/api/campaigns'

export const campaignApi = {
  // Get all campaigns for current user
  getUserCampaigns: async () => {
    const response = await axios.get(API_URL)
    return response.data
  },

  // Get campaign details
  getCampaignDetails: async (campaignId) => {
    const response = await axios.get(`${API_URL}/${campaignId}`)
    return response.data
  },

  // Create new campaign
  createCampaign: async (campaignData) => {
    const response = await axios.post(API_URL, campaignData)
    return response.data
  },

  // Update campaign status
  updateCampaignStatus: async (campaignId, status) => {
    const response = await axios.put(`${API_URL}/${campaignId}/status`, { status })
    return response.data
  },

  // Update milestones (master only)
  updateMilestones: async (campaignId, completedMilestones) => {
    const response = await axios.put(`${API_URL}/${campaignId}/milestones`, { completedMilestones })
    return response.data
  },

  // Add game to campaign
  addGameToCampaign: async (campaignId, gameId) => {
    const response = await axios.post(`${API_URL}/${campaignId}/games/${gameId}`)
    return response.data
  },

  // Remove game from campaign
  removeGameFromCampaign: async (campaignId, gameId) => {
    const response = await axios.delete(`${API_URL}/${campaignId}/games/${gameId}`)
    return response.data
  },

  // Add player to campaign
  addPlayerToCampaign: async (campaignId, playerData) => {
    const response = await axios.post(`${API_URL}/${campaignId}/players`, playerData)
    return response.data
  },

  // Update character
  updateCharacter: async (campaignId, playerId, characterData) => {
    const response = await axios.put(`${API_URL}/${campaignId}/players/${playerId}`, characterData)
    return response.data
  },

  // Remove player from campaign
  removePlayerFromCampaign: async (campaignId, playerId) => {
    const response = await axios.delete(`${API_URL}/${campaignId}/players/${playerId}`)
    return response.data
  }
}
