import { useState, useMemo, useEffect } from 'react'
import Login from './components/Login'
import Register from './components/Register'
import GameScheduler from './components/GameScheduler'
import GameDetails from './components/GameDetails'
import LanguageSwitcher from './components/LanguageSwitcher'
import TabNavigation from './components/TabNavigation'
import CalendarTab from './components/CalendarTab'
import ProfileTab from './components/ProfileTab'
import { playerApi, authApi, setUserTimezone } from './services/api'
import { gameApi } from './services/gameApi'
import { useLanguage } from './i18n/LanguageContext'
import './App.css'

function App() {
  const { t } = useLanguage()
  const [isAuthenticated, setIsAuthenticated] = useState(false)
  const [showRegister, setShowRegister] = useState(false)
  const [user, setUser] = useState(null)
  const [currentPlayer, setCurrentPlayer] = useState(null)
  const [allPlayers, setAllPlayers] = useState([])
  const [daysToShow, setDaysToShow] = useState(14)
  // Инициализируем с сегодняшней датой (без времени)
  const [currentStartDate, setCurrentStartDate] = useState(() => {
    const today = new Date()
    today.setHours(0, 0, 0, 0)
    return today
  })
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [showGameScheduler, setShowGameScheduler] = useState(false)
  const [games, setGames] = useState([])
  const [selectedGame, setSelectedGame] = useState(null)
  const [activeTab, setActiveTab] = useState('calendar')

  // Вычисление количества дней на основе ширины экрана
  useEffect(() => {
    const calculateDaysToShow = () => {
      // Ширина левой панели (300px) + gap (1rem = 16px) + отступы (2rem = 32px)
      // Ширина колонки часов (80px) + ширина одной колонки дня (80px)
      const leftPanelWidth = 300
      const gap = 16
      const padding = 32
      const hoursColumnWidth = 80
      const dayColumnWidth = 80

      const availableWidth = window.innerWidth - leftPanelWidth - gap - padding - hoursColumnWidth
      const calculatedDays = Math.floor(availableWidth / dayColumnWidth)

      // Минимум 7 дней, максимум 60 день, на FullHD (1920px) будет ~14 дней
      const days = Math.max(7, Math.min(60, calculatedDays))
      setDaysToShow(days)
    }

    calculateDaysToShow()
    window.addEventListener('resize', calculateDaysToShow)

    return () => window.removeEventListener('resize', calculateDaysToShow)
  }, [])

  // Проверка аутентификации при монтировании
  useEffect(() => {
    if (authApi.isAuthenticated()) {
      setIsAuthenticated(true)
      loadData()
    } else {
      setLoading(false)
    }
  }, [])

  // Перезагружаем игры при изменении даты
  useEffect(() => {
    if (isAuthenticated && !loading) {
      const loadGames = async () => {
        try {
          const endDate = new Date(currentStartDate)
          endDate.setDate(endDate.getDate() + daysToShow)
          const gamesData = await gameApi.getGames(currentStartDate, endDate)
          setGames(gamesData)
        } catch (err) {
          console.error('Failed to load games:', err)
        }
      }
      loadGames()
    }
  }, [currentStartDate, daysToShow, isAuthenticated, loading])

  const handleLogin = async (username, password) => {
    try {
      const response = await authApi.login(username, password)
      setUser(response)
      setIsAuthenticated(true)
      await loadData()
    } catch (err) {
      throw err
    }
  }

  const handleRegister = async (username, password, email, inviteCode) => {
    try {
      const response = await authApi.register(username, password, email, inviteCode)
      setUser(response)
      setIsAuthenticated(true)
      await loadData()
    } catch (err) {
      throw err
    }
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

      // Вычисляем диапазон дат для фильтрации (текущая неделя + 1 день назад и вперед для запаса)
      const startDate = new Date(currentStartDate)
      startDate.setDate(startDate.getDate() - 1)
      const endDate = new Date(currentStartDate)
      endDate.setDate(endDate.getDate() + daysToShow + 1)

      // Загружаем текущего пользователя, всех игроков и игры с фильтрацией
      const [current, all, gamesData] = await Promise.all([
        playerApi.getCurrentPlayer(startDate, endDate),
        playerApi.getAllPlayers(startDate, endDate),
        gameApi.getGames(startDate, endDate)
      ])
      setCurrentPlayer(current)
      setAllPlayers(all)
      setGames(gamesData)
    } catch (err) {
      console.error('Failed to load data:', err)
      setError('Не удалось загрузить данные. Проверьте подключение к серверу.')
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
      // Обновляем в списке всех игроков
      setAllPlayers(allPlayers.map(p =>
        p.id === updatedPlayer.id ? updatedPlayer : p
      ))
    } catch (err) {
      console.error('Failed to update profile:', err)
      setError('Не удалось обновить профиль. Проверьте подключение к серверу.')
    }
  }

  const handleTimezoneChange = async (timezone) => {
    if (!currentPlayer) return

    try {
      setError(null)
      // Обновляем timezone в API сразу
      setUserTimezone(timezone)
      await handleUpdateProfile(currentPlayer.name, currentPlayer.color, timezone)
    } catch (err) {
      console.error('Failed to update timezone:', err)
      setError('Не удалось обновить часовой пояс.')
    }
  }

  const handleTimeSlotClick = async (date, hour) => {
    try {
      setError(null)

      // Создаем "наивную" дату с компонентами из date и указанным часом
      // Эти компоненты будут интерпретированы как время в timezone пользователя
      const slotDate = new Date(
        date.getFullYear(),
        date.getMonth(),
        date.getDate(),
        hour,
        0,
        0,
        0
      )

      console.log('handleTimeSlotClick:', {
        date: date.toString(),
        hour,
        slotDate: slotDate.toString(),
        components: {
          year: slotDate.getFullYear(),
          month: slotDate.getMonth() + 1,
          day: slotDate.getDate(),
          hour: slotDate.getHours()
        }
      })

      // Отправляем запрос на сервер для текущего пользователя
      const updatedPlayer = await playerApi.toggleTimeSlot(slotDate, 1)

      // Обновляем локальное состояние
      setCurrentPlayer(updatedPlayer)
      setAllPlayers(allPlayers.map(p =>
        p.id === updatedPlayer.id ? updatedPlayer : p
      ))
    } catch (err) {
      console.error('Failed to toggle time slot:', err)
      setError('Не удалось сохранить изменение времени. Проверьте подключение к серверу.')
    }
  }

  const handleTimeSlotsSelect = async (slots, duration = 1) => {
    if (!slots || slots.length === 0) return

    try {
      setError(null)

      // Отправляем запрос на сервер для массового переключения
      // duration можно настроить (по умолчанию 1 час)
      const updatedPlayer = await playerApi.toggleTimeSlots(slots, duration)

      // Обновляем локальное состояние
      setCurrentPlayer(updatedPlayer)
      setAllPlayers(allPlayers.map(p =>
        p.id === updatedPlayer.id ? updatedPlayer : p
      ))
    } catch (err) {
      console.error('Failed to toggle time slots:', err)
      setError('Не удалось сохранить изменения времени. Проверьте подключение к серверу.')
    }
  }

  const handleScheduleGame = async (startTime, endTime, title, description, participantIds) => {
    try {
      setError(null)
      const game = await gameApi.createGame(startTime, endTime, title, description, participantIds)
      setGames([...games, game])
      setShowGameScheduler(false)
    } catch (err) {
      console.error('Failed to schedule game:', err)
      setError('Не удалось запланировать игру. Проверьте подключение к серверу.')
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
      setError('Не удалось удалить игру.')
    }
  }

  const handleJoinGame = async (gameId) => {
    try {
      setError(null)
      const updatedGame = await gameApi.joinGame(gameId)
      setGames(games.map(g => g.id === gameId ? updatedGame : g))
      setSelectedGame(updatedGame)
      // Перезагружаем данные игроков для обновления доступности
      await loadData()
    } catch (err) {
      console.error('Failed to join game:', err)
      setError('Не удалось записаться на игру.')
    }
  }

  const handleLeaveGame = async (gameId) => {
    try {
      setError(null)
      const updatedGame = await gameApi.leaveGame(gameId)
      setGames(games.map(g => g.id === gameId ? updatedGame : g))
      setSelectedGame(updatedGame)
      // Перезагружаем данные игроков для обновления доступности
      await loadData()
    } catch (err) {
      console.error('Failed to leave game:', err)
      setError('Не удалось покинуть игру.')
    }
  }

  const handleUpdateGame = (updatedGame) => {
    setGames(games.map(g => g.id === updatedGame.id ? updatedGame : g))
    setSelectedGame(updatedGame)
  }

  if (!isAuthenticated) {
    return showRegister ? (
      <Register
        onRegister={handleRegister}
        onSwitchToLogin={() => setShowRegister(false)}
      />
    ) : (
      <Login
        onLogin={handleLogin}
        onSwitchToRegister={() => setShowRegister(true)}
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

  return (
    <div className="app-container">
      <header className="app-header">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', width: '100%' }}>
          <div>
            <h1>🎲 {t('appTitle')}</h1>
            <p className="app-subtitle">
              {t('appSubtitle')}
            </p>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
            <LanguageSwitcher />
            {user && (
              <span style={{ color: '#aaa', fontSize: '0.9rem' }}>
                {user.username}
              </span>
            )}
            <button
              onClick={handleLogout}
              style={{
                padding: '0.5rem 1rem',
                borderRadius: '6px',
                border: '1px solid #555',
                background: 'transparent',
                color: '#fff',
                cursor: 'pointer',
                fontSize: '0.9rem'
              }}
            >
              {t('logout')}
            </button>
          </div>
        </div>
        {error && (
          <div style={{
            marginTop: '1rem',
            padding: '0.75rem',
            backgroundColor: '#ff6b6b',
            color: '#fff',
            borderRadius: '6px',
            fontSize: '0.9rem'
          }}>
            {error}
            <button
              onClick={() => setError(null)}
              style={{
                marginLeft: '1rem',
                background: 'none',
                border: '1px solid #fff',
                color: '#fff',
                padding: '0.25rem 0.5rem',
                borderRadius: '4px',
                cursor: 'pointer'
              }}
            >
              ×
            </button>
          </div>
        )}
      </header>

      <div className="app-content">
        <TabNavigation activeTab={activeTab} onTabChange={setActiveTab} />

        {activeTab === 'calendar' ? (
          <CalendarTab
            currentStartDate={currentStartDate}
            daysToShow={daysToShow}
            allPlayers={allPlayers}
            currentPlayer={currentPlayer}
            games={games}
            dates={dates}
            hours={hours}
            onTimeSlotClick={handleTimeSlotClick}
            onTimeSlotsSelect={handleTimeSlotsSelect}
            onEventClick={(event) => {
              if (event.game) {
                setSelectedGame(event.game)
              }
            }}
            onDateChange={setCurrentStartDate}
            onScheduleGame={() => setShowGameScheduler(true)}
          />
        ) : (
          <ProfileTab
            currentPlayer={currentPlayer}
            onUpdateProfile={handleUpdateProfile}
          />
        )}
      </div>

      {showGameScheduler && (
        <GameScheduler
          players={allPlayers}
          onSchedule={handleScheduleGame}
          onClose={() => setShowGameScheduler(false)}
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
