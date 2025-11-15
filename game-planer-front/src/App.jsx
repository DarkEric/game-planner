import { useState, useMemo, useEffect } from 'react'
import CalendarTimeline from './components/CalendarTimeline'
import PlayerManager from './components/PlayerManager'
import BestTimeSlots from './components/BestTimeSlots'
import Login from './components/Login'
import Register from './components/Register'
import { playerApi, authApi } from './services/api'
import './App.css'

function App() {
  const [isAuthenticated, setIsAuthenticated] = useState(false)
  const [showRegister, setShowRegister] = useState(false)
  const [user, setUser] = useState(null)
  const [currentPlayer, setCurrentPlayer] = useState(null)
  const [allPlayers, setAllPlayers] = useState([])
  const [daysToShow] = useState(7)
  const [currentStartDate, setCurrentStartDate] = useState(new Date())
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  // Проверка аутентификации при монтировании
  useEffect(() => {
    if (authApi.isAuthenticated()) {
      setIsAuthenticated(true)
      loadData()
    } else {
      setLoading(false)
    }
  }, [])

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

  const handleRegister = async (username, password, email) => {
    try {
      const response = await authApi.register(username, password, email)
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
      // Загружаем текущего пользователя и всех игроков
      const [current, all] = await Promise.all([
        playerApi.getCurrentPlayer(),
        playerApi.getAllPlayers()
      ])
      setCurrentPlayer(current)
      setAllPlayers(all)
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

  const handleUpdateProfile = async (name, color) => {
    try {
      setError(null)
      const updatedPlayer = await playerApi.updateCurrentPlayer(name, color)
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

  const handleTimeSlotClick = async (date, hour) => {
    try {
      setError(null)
      const slotDate = new Date(date)
      slotDate.setHours(hour, 0, 0, 0)

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

  const handleTimeSlotsSelect = async (slots) => {
    if (!slots || slots.length === 0) return

    try {
      setError(null)
      
      // Отправляем запрос на сервер для массового переключения
      const updatedPlayer = await playerApi.toggleTimeSlots(slots)
      
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
          <p>Загрузка игроков...</p>
        </div>
      </div>
    )
  }

  return (
    <div className="app-container">
      <header className="app-header">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', width: '100%' }}>
          <div>
            <h1>🎲 Планировщик D&D Игр</h1>
            <p className="app-subtitle">
              Отмечайте время, когда вы можете играть, и найдите общее время с друзьями
            </p>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
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
              Выйти
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
        <div className="left-panel">
          {currentPlayer && (
            <div className="player-profile">
              <h2>Ваш профиль</h2>
              <div className="profile-info">
                <div className="profile-field">
                  <label>Имя:</label>
                  <input
                    type="text"
                    value={currentPlayer.name}
                    onChange={(e) => {
                      const newName = e.target.value
                      setCurrentPlayer({ ...currentPlayer, name: newName })
                    }}
                    onBlur={() => handleUpdateProfile(currentPlayer.name, currentPlayer.color)}
                    className="profile-input"
                  />
                </div>
                <div className="profile-field">
                  <label>Цвет:</label>
                  <input
                    type="color"
                    value={currentPlayer.color}
                    onChange={(e) => {
                      const newColor = e.target.value
                      setCurrentPlayer({ ...currentPlayer, color: newColor })
                    }}
                    onBlur={() => handleUpdateProfile(currentPlayer.name, currentPlayer.color)}
                    className="profile-color-input"
                  />
                </div>
              </div>
              <p className="selector-hint">
                Кликайте по ячейкам времени или протяните мышь, чтобы отметить свою доступность
              </p>
            </div>
          )}
        </div>

        <div className="right-panel">
          <div className="calendar-wrapper">
            <CalendarTimeline
              startDate={currentStartDate}
              daysToShow={daysToShow}
              players={allPlayers}
              selectedPlayerId={currentPlayer?.id}
              onTimeSlotClick={(date, hour) => handleTimeSlotClick(date, hour)}
              onTimeSlotsSelect={(slots) => handleTimeSlotsSelect(slots)}
              showAvailabilityOverlap={true}
              onDateChange={setCurrentStartDate}
            />
          </div>

          {allPlayers.length >= 2 && (
            <BestTimeSlots
              players={allPlayers}
              dates={dates}
              hours={hours}
              minPlayers={2}
            />
          )}
        </div>
      </div>
    </div>
  )
}

export default App
