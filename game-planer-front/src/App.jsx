import { useState, useMemo, useEffect, useCallback } from 'react'
import Login from './components/Login'
import Register from './components/Register'
import PasswordResetRequest from './components/PasswordResetRequest'
import PasswordResetConfirm from './components/PasswordResetConfirm'
import GameScheduler from './components/GameScheduler'
import GameDetails from './components/GameDetails'
import LanguageSwitcher from './components/LanguageSwitcher'
import TabNavigation from './components/TabNavigation'
import CalendarTab from './components/CalendarTab'
import UpcomingGamesTab from './components/UpcomingGamesTab'
import TimeMarkingFlow from './components/TimeMarkingFlow'
import ProfileTab from './components/ProfileTab'
import CampaignTab from './components/CampaignTab'
import AdminPanel from './components/AdminPanel'
import { playerApi, authApi, adminApi } from './services/api'
import { gameApi } from './services/gameApi'
import { useLanguage } from './i18n/LanguageContext'
import { getMergedGamesFetchRange } from './utils/gamesDateRange'
import './App.css'

function App() {
  const { t } = useLanguage()
  const [isAuthenticated, setIsAuthenticated] = useState(false)
  const [showRegister, setShowRegister] = useState(false)
  const [user, setUser] = useState(null)
  const [currentPlayer, setCurrentPlayer] = useState(null)
  const [allPlayers, setAllPlayers] = useState([])
  const [daysToShow, setDaysToShow] = useState(14)
  const [showPasswordReset, setShowPasswordReset] = useState(false)
  const [showPasswordResetConfirm, setShowPasswordResetConfirm] = useState(false)
  const [currentStartDate, setCurrentStartDate] = useState(() => {
    const today = new Date()
    today.setHours(0, 0, 0, 0)
    return today
  })
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [showGameScheduler, setShowGameScheduler] = useState(false)
  const [showTimeMarking, setShowTimeMarking] = useState(false)
  const [games, setGames] = useState([])
  const [selectedGame, setSelectedGame] = useState(null)
  const [activeTab, setActiveTab] = useState('games')
  const [isAdmin, setIsAdmin] = useState(false)

  const fetchRange = useMemo(
    () => getMergedGamesFetchRange(currentStartDate, daysToShow),
    [currentStartDate, daysToShow]
  )

  useEffect(() => {
    const calculateDaysToShow = () => {
      const appPadding = 32
      const hoursColumnWidth = 80
      const dayColumnWidth = 160
      const reservedSpace = 20

      const availableWidth = window.innerWidth - appPadding - hoursColumnWidth - reservedSpace
      const calculatedDays = Math.floor(availableWidth / dayColumnWidth)

      const days = Math.max(7, Math.min(90, calculatedDays))
      setDaysToShow(days)
    }

    calculateDaysToShow()
    window.addEventListener('resize', calculateDaysToShow)

    return () => window.removeEventListener('resize', calculateDaysToShow)
  }, [])

  useEffect(() => {
    if (authApi.isAuthenticated()) {
      setIsAuthenticated(true)
      checkAdminStatus()
      loadData()
    } else {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    if (!isAuthenticated || loading) return
    const params = new URLSearchParams(window.location.search)
    const gid = params.get('gameId')
    if (!gid) return
    const id = parseInt(gid, 10)
    if (!Number.isFinite(id) || id <= 0) return

    let cancelled = false
    gameApi
      .getGameById(id)
      .then(game => {
        if (cancelled) return
        setSelectedGame(game)
        const path = window.location.pathname || '/'
        window.history.replaceState({}, document.title, path)
      })
      .catch(() => {
        if (!cancelled) setError(t('errorOpenGame'))
      })

    return () => {
      cancelled = true
    }
  }, [isAuthenticated, loading, t])

  const checkAdminStatus = async () => {
    try {
      const response = await adminApi.isAdmin()
      setIsAdmin(response.isAdmin || false)
    } catch (err) {
      console.error('Failed to check admin status:', err)
      setIsAdmin(false)
    }
  }

  useEffect(() => {
    if (isAuthenticated && !loading) {
      const loadGamesAndPlayers = async () => {
        try {
          const { start, end } = fetchRange

          const [gamesData, current, all] = await Promise.all([
            gameApi.getGames(start, end),
            playerApi.getCurrentPlayer(start, end),
            playerApi.getAllPlayers(start, end)
          ])

          setGames(gamesData)
          setCurrentPlayer(current)
          setAllPlayers(all)
        } catch (err) {
          console.error('Failed to load games and players:', err)
          setError(t('errorLoadData'))
        }
      }
      loadGamesAndPlayers()
    }
  }, [currentStartDate, daysToShow, isAuthenticated, loading, fetchRange, t])

  const handleLogin = async (username, password) => {
    const response = await authApi.login(username, password)
    setUser(response)
    setIsAuthenticated(true)
    await checkAdminStatus()
    await loadData()
  }

  const handleRegister = async (username, password, email, inviteCode, name) => {
    const response = await authApi.register(username, password, email, inviteCode, name)
    setUser(response)
    setIsAuthenticated(true)
    await checkAdminStatus()
    await loadData()
  }

  const handleLogout = () => {
    authApi.logout()
    setIsAuthenticated(false)
    setUser(null)
    setCurrentPlayer(null)
    setAllPlayers([])
  }

  const loadData = async () => {
    try {
      setLoading(true)
      setError(null)

      const { start, end } = getMergedGamesFetchRange(currentStartDate, daysToShow)

      const [current, all, gamesData] = await Promise.all([
        playerApi.getCurrentPlayer(start, end),
        playerApi.getAllPlayers(start, end),
        gameApi.getGames(start, end)
      ])
      setCurrentPlayer(current)
      setAllPlayers(all)
      setGames(gamesData)
    } catch (err) {
      console.error('Failed to load data:', err)
      setError(t('errorLoadData'))
    } finally {
      setLoading(false)
    }
  }

  const dates = useMemo(() => {
    const datesArray = []
    for (let i = 0; i < daysToShow; i++) {
      const date = new Date(currentStartDate)
      date.setDate(currentStartDate.getDate() + i)
      datesArray.push(date)
    }
    return datesArray
  }, [currentStartDate, daysToShow])

  const hours = useMemo(() => {
    const hoursArray = []
    for (let i = 0; i < 24; i++) {
      hoursArray.push(i)
    }
    return hoursArray
  }, [])

  const handleUpdateProfile = async (name, color, timezone) => {
    try {
      setError(null)
      const updatedPlayer = await playerApi.updateCurrentPlayer(name, color, timezone)
      setCurrentPlayer(updatedPlayer)
      setAllPlayers(allPlayers.map(p =>
        p.id === updatedPlayer.id ? updatedPlayer : p
      ))
    } catch (err) {
      console.error('Failed to update profile:', err)
      setError(t('errorUpdateProfile'))
    }
  }

  const handleTimeSlotsSelect = async (slots, options = {}) => {
    if (!slots || slots.length === 0) return

    const mode = options.mode === 'remove' ? 'remove' : 'add'
    const duration = options.duration ?? 1

    try {
      setError(null)

      const updatedPlayer =
        mode === 'remove'
          ? await playerApi.removeTimeSlots(slots, duration)
          : await playerApi.addTimeSlots(slots, duration)

      setCurrentPlayer(updatedPlayer)
      setAllPlayers(allPlayers.map(p =>
        p.id === updatedPlayer.id ? updatedPlayer : p
      ))
    } catch (err) {
      console.error('Failed to update time slots:', err)
      setError(t('errorToggleSlot'))
    }
  }

  const handleTimeMarkingSaved = useCallback((updatedPlayer) => {
    setCurrentPlayer(updatedPlayer)
    setAllPlayers(prev =>
      prev.map(p => (p.id === updatedPlayer.id ? updatedPlayer : p))
    )
  }, [])

  const handleScheduleGame = async (startTime, endTime, title, description, participantIds, autoAddPlayers, campaignId, maxParticipants) => {
    try {
      setError(null)
      const game = await gameApi.createGame(startTime, endTime, title, description, participantIds, autoAddPlayers, maxParticipants, campaignId)
      setGames(prev => {
        if (prev.some(g => g.id === game.id)) return prev
        return [...prev, game]
      })
      setShowGameScheduler(false)
    } catch (err) {
      console.error('Failed to schedule game:', err)
      setError(t('errorScheduleGame'))
    }
  }

  const handleDeleteGame = async (gameId, cancellationReason) => {
    try {
      setError(null)
      await gameApi.deleteGame(gameId, cancellationReason)
      setGames(games.filter(g => g.id !== gameId))
      setSelectedGame(null)
    } catch (err) {
      console.error('Failed to delete game:', err)
      setError(t('errorDeleteGame'))
    }
  }

  const handleJoinGame = async (gameId) => {
    try {
      setError(null)
      const updatedGame = await gameApi.joinGame(gameId)
      setGames(games.map(g => g.id === gameId ? updatedGame : g))
      setSelectedGame(updatedGame)
      await loadData()
    } catch (err) {
      console.error('Failed to join game:', err)
      const errorMessage = err.message || ''
      if (errorMessage.includes('full') || errorMessage.includes('maximum') || errorMessage.includes('participants')) {
        setError(t('errorJoinGame'))
      } else {
        setError(t('errorJoinGame'))
      }
    }
  }

  const handleLeaveGame = async (gameId) => {
    try {
      setError(null)
      const updatedGame = await gameApi.leaveGame(gameId)
      setGames(games.map(g => g.id === gameId ? updatedGame : g))
      setSelectedGame(updatedGame)
      await loadData()
    } catch (err) {
      console.error('Failed to leave game:', err)
      setError(t('errorLeaveGame'))
    }
  }

  const handleUpdateGame = (updatedGame) => {
    setGames(games.map(g => g.id === updatedGame.id ? updatedGame : g))
    setSelectedGame(updatedGame)
  }

  if (!isAuthenticated) {
    if (showPasswordResetConfirm) {
      return (
        <PasswordResetConfirm
          onBackToLogin={() => {
            setShowPasswordResetConfirm(false)
            setShowPasswordReset(false)
          }}
          onPasswordReset={() => {
            setShowPasswordResetConfirm(false)
            setShowPasswordReset(false)
            alert('Пароль успешно изменен! Вы можете войти с новым паролем.')
          }}
        />
      )
    }
    if (showPasswordReset) {
      return (
        <PasswordResetRequest
          onBackToLogin={() => {
            setShowPasswordReset(false)
          }}
          onProceedToConfirm={() => {
            setShowPasswordReset(false)
            setShowPasswordResetConfirm(true)
          }}
        />
      )
    }
    return showRegister ? (
      <Register
        onRegister={handleRegister}
        onSwitchToLogin={() => setShowRegister(false)}
      />
    ) : (
      <Login
        onLogin={handleLogin}
        onSwitchToRegister={() => setShowRegister(true)}
        onForgotPassword={() => setShowPasswordReset(true)}
      />
    )
  }

  if (loading) {
    return (
      <div className="app-container">
        <div style={{ textAlign: 'center', padding: '2rem', color: '#fff' }}>
          <p>{t('loading')}</p>
        </div>
      </div>
    )
  }

  /** На вкладке «Игры» запланировать игру уже в нижней панели — в шапке только для «Календарь». */
  const showScheduleInHeader = activeTab === 'calendar'

  return (
    <div className="app-container">
      <header className="app-header">
        <div className="app-header-inner">
          <div className="app-header-titles">
            <h1>🎲 {t('appTitle')}</h1>
            <p className="app-subtitle">
              {t('appSubtitle')}
            </p>
          </div>
          <div className="app-header-actions">
            <LanguageSwitcher />
            {showScheduleInHeader && (
              <button
                type="button"
                className="app-header-btn app-header-btn-primary"
                onClick={() => setShowGameScheduler(true)}
              >
                🎲 {t('scheduleGame')}
              </button>
            )}
            {user && (
              <span className="app-header-user">
                {user.username}
              </span>
            )}
            <button
              type="button"
              className="app-header-btn app-header-btn-ghost"
              onClick={handleLogout}
            >
              {t('logout')}
            </button>
          </div>
        </div>
        {error && (
          <div className="app-error-banner">
            {error}
            <button
              type="button"
              className="app-error-dismiss"
              onClick={() => setError(null)}
            >
              ×
            </button>
          </div>
        )}
      </header>

      <div className="app-content">
        <TabNavigation activeTab={activeTab} onTabChange={setActiveTab} isAdmin={isAdmin} />

        {activeTab === 'games' && (
          <UpcomingGamesTab
            games={games}
            onSelectGame={setSelectedGame}
          />
        )}

        {activeTab === 'calendar' && (
          <CalendarTab
            currentStartDate={currentStartDate}
            daysToShow={daysToShow}
            allPlayers={allPlayers}
            currentPlayer={currentPlayer}
            games={games}
            dates={dates}
            hours={hours}
            onTimeSlotsSelect={handleTimeSlotsSelect}
            onEventClick={(event) => {
              if (event.game) {
                setSelectedGame(event.game)
              }
            }}
            onDateChange={setCurrentStartDate}
          />
        )}

        {activeTab === 'campaigns' && (
          <CampaignTab currentUserId={currentPlayer?.id} />
        )}

        {activeTab === 'profile' && (
          <ProfileTab
            currentPlayer={currentPlayer}
            onUpdateProfile={handleUpdateProfile}
          />
        )}

        {activeTab === 'admin' && isAdmin && (
          <AdminPanel currentUserId={currentPlayer?.id} />
        )}
      </div>

      {activeTab === 'games' && (
        <div className="app-home-bottom-actions" role="toolbar" aria-label={t('tabNavAria')}>
          <button
            type="button"
            className="app-home-bottom-btn secondary"
            onClick={() => setShowTimeMarking(true)}
          >
            📅 {t('markTime')}
          </button>
          <button
            type="button"
            className="app-home-bottom-btn primary"
            onClick={() => setShowGameScheduler(true)}
          >
            🎲 {t('scheduleGame')}
          </button>
        </div>
      )}

      {showGameScheduler && (
        <GameScheduler
          players={allPlayers}
          onSchedule={handleScheduleGame}
          onClose={() => setShowGameScheduler(false)}
        />
      )}

      {showTimeMarking && (
        <TimeMarkingFlow
          onClose={() => setShowTimeMarking(false)}
          onSaved={handleTimeMarkingSaved}
        />
      )}

      {selectedGame && (
        <GameDetails
          game={selectedGame}
          currentUserId={currentPlayer?.id}
          onJoin={handleJoinGame}
          onLeave={handleLeaveGame}
          onDelete={handleDeleteGame}
          onUpdate={handleUpdateGame}
          onClose={() => setSelectedGame(null)}
        />
      )}
    </div>
  )
}

export default App
