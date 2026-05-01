import { getUserTimezoneForAPI } from './api'
import { parseFromServer, formatForServer } from '../utils/timezoneUtils'

const API_BASE_URL = import.meta.env.VITE_API_URL || '/api'

const getToken = () => localStorage.getItem('authToken')

const getAuthHeaders = () => {
  const token = getToken()
  return {
    'Content-Type': 'application/json',
    ...(token && { 'Authorization': `Bearer ${token}` })
  }
}

export const gameApi = {
  async createGame(startTime, endTime, title, description, participantIds, autoAddPlayers = true, maxParticipants = null, campaignId = null) {
    const userTimezone = getUserTimezoneForAPI()

    const response = await fetch(`${API_BASE_URL}/games`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify({
        startTime: formatForServer(startTime, userTimezone),
        endTime: formatForServer(endTime, userTimezone),
        title: title || null,
        description: description || null,
        participantIds,
        autoAddPlayers,
        maxParticipants: maxParticipants || null,
        campaignId: campaignId || null
      })
    })

    if (!response.ok) {
      throw new Error('Failed to create game')
    }

    const data = await response.json()
    return {
      id: data.id,
      startTime: parseFromServer(data.startTime, userTimezone),
      endTime: parseFromServer(data.endTime, userTimezone),
      title: data.title,
      description: data.description,
      creatorId: data.creatorId,
      creatorName: data.creatorName,
      participants: data.participants,
      createdAt: parseFromServer(data.createdAt, userTimezone),
      isHeld: data.isHeld !== undefined ? data.isHeld : data.held,
      keyEvents: data.keyEvents,
      campaignId: data.campaignId,
      campaignName: data.campaignName,
      maxParticipants: data.maxParticipants
    }
  },

  async getGames(startDate, endDate) {
    const userTimezone = getUserTimezoneForAPI()
    const params = new URLSearchParams()

    if (startDate) {
      params.append('startDate', formatForServer(startDate, userTimezone))
    }
    if (endDate) {
      params.append('endDate', formatForServer(endDate, userTimezone))
    }

    const response = await fetch(`${API_BASE_URL}/games?${params}`, {
      headers: getAuthHeaders()
    })

    if (!response.ok) {
      throw new Error('Failed to fetch games')
    }

    const data = await response.json()
    return data.map(game => ({
      id: game.id,
      startTime: parseFromServer(game.startTime, userTimezone),
      endTime: parseFromServer(game.endTime, userTimezone),
      title: game.title,
      description: game.description,
      creatorId: game.creatorId,
      creatorName: game.creatorName,
      participants: game.participants,
      createdAt: parseFromServer(game.createdAt, userTimezone),
      isHeld: game.isHeld !== undefined ? game.isHeld : game.held,
      keyEvents: game.keyEvents,
      campaignId: game.campaignId,
      campaignName: game.campaignName,
      maxParticipants: game.maxParticipants
    }))
  },

  async getMyGames() {
    const userTimezone = getUserTimezoneForAPI()

    const response = await fetch(`${API_BASE_URL}/games/my`, {
      headers: getAuthHeaders()
    })

    if (!response.ok) {
      throw new Error('Failed to fetch my games')
    }

    const data = await response.json()
    return data.map(game => ({
      id: game.id,
      startTime: parseFromServer(game.startTime, userTimezone),
      endTime: parseFromServer(game.endTime, userTimezone),
      title: game.title,
      description: game.description,
      creatorId: game.creatorId,
      creatorName: game.creatorName,
      participants: game.participants,
      createdAt: parseFromServer(game.createdAt, userTimezone),
      isHeld: game.isHeld !== undefined ? game.isHeld : game.held,
      keyEvents: game.keyEvents,
      campaignId: game.campaignId,
      campaignName: game.campaignName,
      maxParticipants: game.maxParticipants
    }))
  },

  async getMyCreatedGames() {
    const userTimezone = getUserTimezoneForAPI()

    const response = await fetch(`${API_BASE_URL}/games/my/created`, {
      headers: getAuthHeaders()
    })

    if (!response.ok) {
      throw new Error('Failed to fetch my created games')
    }

    const data = await response.json()
    return data.map(game => ({
      id: game.id,
      startTime: parseFromServer(game.startTime, userTimezone),
      endTime: parseFromServer(game.endTime, userTimezone),
      title: game.title,
      description: game.description,
      creatorId: game.creatorId,
      creatorName: game.creatorName,
      participants: game.participants,
      createdAt: parseFromServer(game.createdAt, userTimezone),
      isHeld: game.isHeld !== undefined ? game.isHeld : game.held,
      keyEvents: game.keyEvents,
      campaignId: game.campaignId,
      campaignName: game.campaignName,
      maxParticipants: game.maxParticipants
    }))
  },

  async updateGame(gameId, { startTime, endTime, title, description, maxParticipants }) {
    const userTimezone = getUserTimezoneForAPI()

    const response = await fetch(`${API_BASE_URL}/games/${gameId}`, {
      method: 'PUT',
      headers: getAuthHeaders(),
      body: JSON.stringify({
        startTime: formatForServer(startTime, userTimezone),
        endTime: formatForServer(endTime, userTimezone),
        title: title || null,
        description: description || null,
        maxParticipants: maxParticipants != null ? maxParticipants : null
      })
    })

    if (!response.ok) {
      throw new Error('Failed to update game')
    }

    const data = await response.json()
    return {
      id: data.id,
      startTime: parseFromServer(data.startTime, userTimezone),
      endTime: parseFromServer(data.endTime, userTimezone),
      title: data.title,
      description: data.description,
      creatorId: data.creatorId,
      creatorName: data.creatorName,
      participants: data.participants,
      createdAt: parseFromServer(data.createdAt, userTimezone),
      isHeld: data.isHeld !== undefined ? data.isHeld : data.held,
      keyEvents: data.keyEvents,
      campaignId: data.campaignId,
      campaignName: data.campaignName,
      maxParticipants: data.maxParticipants
    }
  },

  async deleteGame(gameId, cancellationReason) {
    const params = new URLSearchParams()
    if (cancellationReason && cancellationReason.trim()) {
      params.append('cancellationReason', cancellationReason.trim())
    }

    const url = `${API_BASE_URL}/games/${gameId}${params.toString() ? '?' + params.toString() : ''}`
    const response = await fetch(url, {
      method: 'DELETE',
      headers: getAuthHeaders()
    })

    if (!response.ok) {
      throw new Error('Failed to delete game')
    }
  },

  async joinGame(gameId) {
    const userTimezone = getUserTimezoneForAPI()

    const response = await fetch(`${API_BASE_URL}/games/${gameId}/join`, {
      method: 'POST',
      headers: getAuthHeaders()
    })

    if (!response.ok) {
      throw new Error('Failed to join game')
    }

    const data = await response.json()
    return {
      id: data.id,
      startTime: parseFromServer(data.startTime, userTimezone),
      endTime: parseFromServer(data.endTime, userTimezone),
      title: data.title,
      description: data.description,
      creatorId: data.creatorId,
      creatorName: data.creatorName,
      participants: data.participants,
      createdAt: parseFromServer(data.createdAt, userTimezone),
      isHeld: data.isHeld !== undefined ? data.isHeld : data.held,
      keyEvents: data.keyEvents,
      maxParticipants: data.maxParticipants
    }
  },

  async leaveGame(gameId) {
    const userTimezone = getUserTimezoneForAPI()

    const response = await fetch(`${API_BASE_URL}/games/${gameId}/leave`, {
      method: 'POST',
      headers: getAuthHeaders()
    })

    if (!response.ok) {
      throw new Error('Failed to leave game')
    }

    const data = await response.json()
    return {
      id: data.id,
      startTime: parseFromServer(data.startTime, userTimezone),
      endTime: parseFromServer(data.endTime, userTimezone),
      title: data.title,
      description: data.description,
      creatorId: data.creatorId,
      creatorName: data.creatorName,
      participants: data.participants,
      createdAt: parseFromServer(data.createdAt, userTimezone),
      isHeld: data.isHeld !== undefined ? data.isHeld : data.held,
      keyEvents: data.keyEvents,
      maxParticipants: data.maxParticipants
    }
  },

  async removePlayerFromGame(gameId, playerId) {
    const userTimezone = getUserTimezoneForAPI()

    const response = await fetch(`${API_BASE_URL}/games/${gameId}/players/${playerId}`, {
      method: 'DELETE',
      headers: getAuthHeaders()
    })

    if (!response.ok) {
      throw new Error('Failed to remove player from game')
    }

    const data = await response.json()
    return {
      id: data.id,
      startTime: parseFromServer(data.startTime, userTimezone),
      endTime: parseFromServer(data.endTime, userTimezone),
      title: data.title,
      description: data.description,
      creatorId: data.creatorId,
      creatorName: data.creatorName,
      participants: data.participants,
      createdAt: parseFromServer(data.createdAt, userTimezone),
      isHeld: data.isHeld !== undefined ? data.isHeld : data.held,
      keyEvents: data.keyEvents,
      maxParticipants: data.maxParticipants
    }
  },

  async markGameAsHeld(gameId, keyEvents) {
    const userTimezone = getUserTimezoneForAPI()

    const response = await fetch(`${API_BASE_URL}/games/${gameId}/hold`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify({ keyEvents })
    })

    if (!response.ok) {
      throw new Error('Failed to mark game as held')
    }

    const data = await response.json()
    return {
      id: data.id,
      startTime: parseFromServer(data.startTime, userTimezone),
      endTime: parseFromServer(data.endTime, userTimezone),
      title: data.title,
      description: data.description,
      creatorId: data.creatorId,
      creatorName: data.creatorName,
      participants: data.participants,
      createdAt: parseFromServer(data.createdAt, userTimezone),
      isHeld: data.isHeld !== undefined ? data.isHeld : data.held,
      keyEvents: data.keyEvents,
      maxParticipants: data.maxParticipants
    }
  },

  async getGameById(gameId) {
    const userTimezone = getUserTimezoneForAPI()

    const response = await fetch(`${API_BASE_URL}/games/${gameId}`, {
      method: 'GET',
      headers: getAuthHeaders()
    })

    if (!response.ok) {
      throw new Error('Failed to get game')
    }

    const data = await response.json()
    return {
      id: data.id,
      startTime: parseFromServer(data.startTime, userTimezone),
      endTime: parseFromServer(data.endTime, userTimezone),
      title: data.title,
      description: data.description,
      creatorId: data.creatorId,
      creatorName: data.creatorName,
      participants: data.participants,
      createdAt: parseFromServer(data.createdAt, userTimezone),
      isHeld: data.isHeld !== undefined ? data.isHeld : (data.held !== undefined ? data.held : false),
      keyEvents: data.keyEvents,
      campaignId: data.campaignId,
      campaignName: data.campaignName,
      maxParticipants: data.maxParticipants
    }
  }
}
