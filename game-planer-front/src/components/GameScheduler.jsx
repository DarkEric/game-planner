import { useState, useMemo } from 'react'
import { useLanguage } from '../i18n/LanguageContext'
import './GameScheduler.css'

const GameScheduler = ({ players, onSchedule, onClose }) => {
  const { t, language } = useLanguage()
  const [selectedDate, setSelectedDate] = useState(new Date())
  const [startTime, setStartTime] = useState('')
  const [endTime, setEndTime] = useState('')
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [selectedSlot, setSelectedSlot] = useState(null)

  // Генерируем календарь на текущий месяц
  const calendarDays = useMemo(() => {
    const year = selectedDate.getFullYear()
    const month = selectedDate.getMonth()
    const firstDay = new Date(year, month, 1)
    const lastDay = new Date(year, month + 1, 0)
    const daysInMonth = lastDay.getDate()
    const startDayOfWeek = firstDay.getDay()
    
    const days = []
    
    // Добавляем пустые ячейки для выравнивания
    for (let i = 0; i < (startDayOfWeek === 0 ? 6 : startDayOfWeek - 1); i++) {
      days.push(null)
    }
    
    // Добавляем дни месяца
    for (let day = 1; day <= daysInMonth; day++) {
      days.push(new Date(year, month, day))
    }
    
    return days
  }, [selectedDate])

  // Вычисляем лучшие временные слоты
  const bestSlots = useMemo(() => {
    const slots = []
    const today = new Date()
    today.setHours(0, 0, 0, 0)
    
    // Проверяем следующие 30 дней
    for (let dayOffset = 0; dayOffset < 30; dayOffset++) {
      const date = new Date(today)
      date.setDate(today.getDate() + dayOffset)
      
      // Проверяем каждый час
      for (let hour = 0; hour < 24; hour++) {
        const availablePlayers = players.filter(player => {
          return player.availableTimes.some(timeSlot => {
            const slotDate = new Date(timeSlot.start)
            const slotEnd = new Date(slotDate)
            slotEnd.setHours(slotDate.getHours() + (timeSlot.duration || 1))
            
            const checkDate = new Date(date)
            checkDate.setHours(hour, 0, 0, 0)
            
            return (
              checkDate >= slotDate &&
              checkDate < slotEnd &&
              slotDate.toDateString() === date.toDateString()
            )
          })
        })
        
        if (availablePlayers.length >= 2) {
          slots.push({
            date,
            hour,
            availablePlayers,
            count: availablePlayers.length
          })
        }
      }
    }
    
    // Объединяем последовательные слоты
    const mergedSlots = []
    const sortedSlots = slots.sort((a, b) => {
      const dateCompare = a.date.getTime() - b.date.getTime()
      return dateCompare !== 0 ? dateCompare : a.hour - b.hour
    })
    
    for (let i = 0; i < sortedSlots.length; i++) {
      const currentSlot = sortedSlots[i]
      
      if (mergedSlots.length > 0) {
        const lastMerged = mergedSlots[mergedSlots.length - 1]
        const isSameDate = lastMerged.date.toDateString() === currentSlot.date.toDateString()
        const isConsecutive = lastMerged.endHour === currentSlot.hour
        const hasSamePlayers = lastMerged.count === currentSlot.count &&
          lastMerged.availablePlayers.every(p => 
            currentSlot.availablePlayers.some(cp => cp.id === p.id)
          )
        
        if (isSameDate && isConsecutive && hasSamePlayers) {
          lastMerged.endHour = currentSlot.hour + 1
          lastMerged.duration = lastMerged.endHour - lastMerged.hour
          continue
        }
      }
      
      mergedSlots.push({
        ...currentSlot,
        endHour: currentSlot.hour + 1,
        duration: 1
      })
    }
    
    return mergedSlots
      .sort((a, b) => b.count - a.count)
      .slice(0, 10)
  }, [players])

  const formatSlotTime = (slot) => {
    const locale = language === 'en' ? 'en-US' : 'ru-RU'
    const dateStr = slot.date.toLocaleDateString(locale, {
      day: 'numeric',
      month: 'long',
      weekday: 'short'
    })
    const startTime = `${slot.hour.toString().padStart(2, '0')}:00`
    const endTime = `${slot.endHour.toString().padStart(2, '0')}:00`
    
    if (slot.duration > 1) {
      return `${dateStr}, ${startTime} - ${endTime}`
    }
    return `${dateStr}, ${startTime}`
  }

  const handleSlotSelect = (slot) => {
    setSelectedSlot(slot)
    
    // Устанавливаем дату и время
    const start = new Date(slot.date)
    start.setHours(slot.hour, 0, 0, 0)
    const end = new Date(start)
    end.setHours(slot.endHour, 0, 0, 0)
    
    // Форматируем для datetime-local input (нужно локальное время, не UTC)
    // Формат: YYYY-MM-DDTHH:mm
    const formatForInput = (date) => {
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      const hours = String(date.getHours()).padStart(2, '0')
      const minutes = String(date.getMinutes()).padStart(2, '0')
      return `${year}-${month}-${day}T${hours}:${minutes}`
    }
    
    setStartTime(formatForInput(start))
    setEndTime(formatForInput(end))
  }

  const handleSchedule = () => {
    if (!startTime || !endTime) return
    
    // datetime-local input возвращает строку в формате "YYYY-MM-DDTHH:mm"
    // new Date() интерпретирует это как локальное время браузера
    const start = new Date(startTime)
    const end = new Date(endTime)
    
    // Получаем ID участников из выбранного слота
    // Если слот выбран из топ-10, используем игроков из слота
    // Если время выбрано вручную, находим доступных игроков на это время
    let participantIds = []
    
    if (selectedSlot) {
      participantIds = selectedSlot.availablePlayers.map(p => p.id)
    } else {
      // Находим игроков, доступных на выбранное время
      const gameDurationHours = (end - start) / (1000 * 60 * 60)
      
      participantIds = players.filter(player => {
        // Проверяем, доступен ли игрок на всё время игры
        for (let hourOffset = 0; hourOffset < gameDurationHours; hourOffset++) {
          const checkTime = new Date(start)
          checkTime.setHours(start.getHours() + hourOffset)
          
          const isAvailable = player.availableTimes.some(timeSlot => {
            const slotDate = new Date(timeSlot.start)
            const slotEnd = new Date(slotDate)
            slotEnd.setHours(slotDate.getHours() + (timeSlot.duration || 1))
            
            return (
              checkTime >= slotDate &&
              checkTime < slotEnd &&
              slotDate.toDateString() === checkTime.toDateString()
            )
          })
          
          if (!isAvailable) {
            return false
          }
        }
        
        return true
      }).map(p => p.id)
    }
    
    onSchedule(start, end, title, description, participantIds)
  }

  const changeMonth = (offset) => {
    const newDate = new Date(selectedDate)
    newDate.setMonth(newDate.getMonth() + offset)
    setSelectedDate(newDate)
  }

  const isScheduleDisabled = !startTime || !endTime || new Date(startTime) >= new Date(endTime)

  return (
    <div className="game-scheduler-overlay" onClick={onClose}>
      <div className="game-scheduler-modal" onClick={(e) => e.stopPropagation()}>
        <div className="game-scheduler-header">
          <h2>🎲 {t('scheduleGameTitle')}</h2>
          <button className="close-button" onClick={onClose}>×</button>
        </div>

        <div className="game-scheduler-content">
          <div className="calendar-section">
            <h3>{t('selectDateTime')}</h3>
            
            <div style={{ marginBottom: '1rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
                <button 
                  onClick={() => changeMonth(-1)}
                  style={{ background: 'none', border: 'none', color: '#fff', fontSize: '1.5rem', cursor: 'pointer' }}
                >
                  ‹
                </button>
                <span style={{ color: '#fff', fontSize: '1.1rem' }}>
                  {selectedDate.toLocaleDateString(language === 'en' ? 'en-US' : 'ru-RU', { month: 'long', year: 'numeric' })}
                </span>
                <button 
                  onClick={() => changeMonth(1)}
                  style={{ background: 'none', border: 'none', color: '#fff', fontSize: '1.5rem', cursor: 'pointer' }}
                >
                  ›
                </button>
              </div>
              
              <div style={{ 
                display: 'grid', 
                gridTemplateColumns: 'repeat(7, 1fr)', 
                gap: '0.5rem',
                marginBottom: '0.5rem'
              }}>
                {[t('mon'), t('tue'), t('wed'), t('thu'), t('fri'), t('sat'), t('sun')].map(day => (
                  <div key={day} style={{ textAlign: 'center', color: '#aaa', fontSize: '0.85rem' }}>
                    {day}
                  </div>
                ))}
              </div>
              
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(7, 1fr)', gap: '0.5rem' }}>
                {calendarDays.map((day, index) => (
                  <div
                    key={index}
                    style={{
                      padding: '0.75rem',
                      textAlign: 'center',
                      background: day ? '#2a2a2a' : 'transparent',
                      color: day ? '#fff' : 'transparent',
                      borderRadius: '6px',
                      cursor: day ? 'pointer' : 'default',
                      border: day && day.toDateString() === new Date().toDateString() ? '2px solid #646cff' : 'none'
                    }}
                  >
                    {day ? day.getDate() : ''}
                  </div>
                ))}
              </div>
            </div>

            <div className="time-inputs">
              <div className="time-input-group">
                <label>{t('start')}</label>
                <input
                  type="datetime-local"
                  value={startTime}
                  onChange={(e) => setStartTime(e.target.value)}
                />
              </div>
              <div className="time-input-group">
                <label>{t('end')}</label>
                <input
                  type="datetime-local"
                  value={endTime}
                  onChange={(e) => setEndTime(e.target.value)}
                />
              </div>
            </div>

            <div className="game-info-inputs">
              <div className="time-input-group">
                <label>{t('gameTitleOptional')}</label>
                <input
                  type="text"
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  placeholder={t('gameTitlePlaceholder')}
                  maxLength="255"
                />
              </div>
              <div className="time-input-group">
                <label>{t('descriptionOptional')}</label>
                <textarea
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  placeholder={t('descriptionPlaceholder')}
                  maxLength="1000"
                  rows="3"
                  style={{
                    width: '100%',
                    padding: '0.75rem',
                    background: '#2a2a2a',
                    border: '1px solid #444',
                    borderRadius: '6px',
                    color: '#fff',
                    fontSize: '1rem',
                    resize: 'vertical',
                    fontFamily: 'inherit'
                  }}
                />
              </div>
            </div>
          </div>

          <div className="slots-section">
            <h3>{t('topSlots')}</h3>
            {bestSlots.length > 0 ? (
              <div className="best-slots-list">
                {bestSlots.map((slot, index) => (
                  <div
                    key={index}
                    className={`best-slot-item ${selectedSlot === slot ? 'selected' : ''}`}
                    onClick={() => handleSlotSelect(slot)}
                  >
                    <div className="best-slot-header">
                      <div className="best-slot-time">
                        #{index + 1} {formatSlotTime(slot)}
                      </div>
                      <div className="best-slot-count">
                        {slot.count}/{players.length}
                      </div>
                    </div>
                    <div className="best-slot-players">
                      {slot.availablePlayers.map(player => (
                        <span
                          key={player.id}
                          className="player-badge-small"
                          style={{ backgroundColor: player.color }}
                        >
                          {player.name}
                        </span>
                      ))}
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="no-slots-message">
                {t('noSlotsAvailable')}
              </div>
            )}
          </div>
        </div>

        <div className="game-scheduler-actions">
          <button className="cancel-button" onClick={onClose}>
            {t('cancel')}
          </button>
          <button 
            className="schedule-button" 
            onClick={handleSchedule}
            disabled={isScheduleDisabled}
          >
            {t('scheduleButton')}
          </button>
        </div>
      </div>
    </div>
  )
}

export default GameScheduler
