import { useState, useMemo, useEffect } from 'react'
import { useLanguage } from '../i18n/LanguageContext'
import { campaignApi } from '../services/campaignApi'
import { localTimeInputValueForMoscowWall } from '../utils/moscowDefaults'
import './GameScheduler.css'

function stripTime(d) {
  const x = new Date(d)
  x.setHours(0, 0, 0, 0)
  return x
}

function isSameBestSlot(a, b) {
  if (!a || !b) return false
  return a.date.getTime() === b.date.getTime() && a.hour === b.hour && a.count === b.count
}

const GameScheduler = ({ players, onSchedule, onClose }) => {
  const { t, language } = useLanguage()
  const [viewMonth, setViewMonth] = useState(() => {
    const d = new Date()
    d.setDate(1)
    d.setHours(12, 0, 0, 0)
    return d
  })
  const [selectedDay, setSelectedDay] = useState(() => stripTime(new Date()))
  const [gameTime, setGameTime] = useState(() =>
    localTimeInputValueForMoscowWall(stripTime(new Date()))
  )
  const [durationHours, setDurationHours] = useState(4)
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [autoAddPlayers, setAutoAddPlayers] = useState(true)
  const [maxParticipants, setMaxParticipants] = useState('')
  const [selectedSlot, setSelectedSlot] = useState(null)
  const [campaigns, setCampaigns] = useState([])
  const [selectedCampaignId, setSelectedCampaignId] = useState('')

  useEffect(() => {
    loadCampaigns()
  }, [])

  const loadCampaigns = async () => {
    try {
      const data = await campaignApi.getUserCampaigns()
      setCampaigns(data.filter(c => c.status === 'ACTIVE'))
    } catch (error) {
      console.error('Failed to load campaigns:', error)
    }
  }

  const calendarDays = useMemo(() => {
    const year = viewMonth.getFullYear()
    const month = viewMonth.getMonth()
    const firstDay = new Date(year, month, 1)
    const lastDay = new Date(year, month + 1, 0)
    const daysInMonth = lastDay.getDate()
    const startDayOfWeek = firstDay.getDay()

    const days = []
    for (let i = 0; i < (startDayOfWeek === 0 ? 6 : startDayOfWeek - 1); i++) {
      days.push(null)
    }
    for (let day = 1; day <= daysInMonth; day++) {
      days.push(new Date(year, month, day))
    }
    return days
  }, [viewMonth])

  const bestSlots = useMemo(() => {
    const slots = []
    const today = new Date()
    today.setHours(0, 0, 0, 0)

    for (let dayOffset = 0; dayOffset < 30; dayOffset++) {
      const date = new Date(today)
      date.setDate(today.getDate() + dayOffset)

      for (let hour = 0; hour < 24; hour++) {
        const availablePlayers = players.filter(player =>
          player.availableTimes.some(timeSlot => {
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
        )

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
    const startStr = `${slot.hour.toString().padStart(2, '0')}:00`
    const endStr = `${slot.endHour.toString().padStart(2, '0')}:00`

    if (slot.duration > 1) {
      return `${dateStr}, ${startStr} – ${endStr}`
    }
    return `${dateStr}, ${startStr}`
  }

  const handleSlotSelect = (slot) => {
    setSelectedSlot(slot)
    setSelectedDay(stripTime(slot.date))
    setGameTime(`${slot.hour.toString().padStart(2, '0')}:00`)
    setDurationHours(Math.max(1, Math.min(24, slot.duration || 1)))
  }

  const handlePickDay = (day) => {
    if (!day) return
    const stripped = stripTime(day)
    setSelectedDay(stripped)
    setSelectedSlot(null)
    setGameTime(localTimeInputValueForMoscowWall(stripped))
  }

  const buildStartEnd = () => {
    if (!selectedDay) return { start: null, end: null }
    const parts = gameTime.split(':')
    const h = parseInt(parts[0], 10)
    const m = parseInt(parts[1] || '0', 10)
    if (Number.isNaN(h) || Number.isNaN(m)) return { start: null, end: null }

    const start = new Date(selectedDay)
    start.setHours(h, m, 0, 0)
    const dh = Math.max(1, Math.min(24, Number(durationHours) || 1))
    const end = new Date(start.getTime() + dh * 60 * 60 * 1000)
    return { start, end }
  }

  const handleSchedule = () => {
    const { start, end } = buildStartEnd()
    if (!start || !end || start >= end) return

    let participantIds = []

    if (selectedSlot) {
      participantIds = selectedSlot.availablePlayers.map(p => p.id)
    } else {
      const gameDurationHours = (end - start) / (1000 * 60 * 60)

      participantIds = players.filter(player => {
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

    const maxParticipantsValue = maxParticipants.trim() ? parseInt(maxParticipants, 10) : null
    onSchedule(start, end, title, description, participantIds, autoAddPlayers, selectedCampaignId || null, maxParticipantsValue)
  }

  const changeMonth = (offset) => {
    const newDate = new Date(viewMonth)
    newDate.setMonth(viewMonth.getMonth() + offset)
    setViewMonth(newDate)
  }

  const { start: computedStart, end: computedEnd } = buildStartEnd()
  const isScheduleDisabled =
    !selectedDay ||
    !computedStart ||
    !computedEnd ||
    computedStart >= computedEnd

  const todayStrip = stripTime(new Date())

  return (
    <div className="game-scheduler-overlay" onClick={onClose}>
      <div className="game-scheduler-modal" onClick={(e) => e.stopPropagation()}>
        <div className="game-scheduler-header">
          <h2>🎲 {t('scheduleGameTitle')}</h2>
          <button type="button" className="close-button" onClick={onClose} aria-label={t('close')}>×</button>
        </div>

        <div className="game-scheduler-content">
          <div className="calendar-section">
            <h3>{t('selectDateTime')}</h3>

            <div className="scheduler-calendar-wrap">
              <div className="scheduler-month-nav">
                <button type="button" className="scheduler-month-btn" onClick={() => changeMonth(-1)} aria-label={t('previous')}>
                  ‹
                </button>
                <span className="scheduler-month-label">
                  {viewMonth.toLocaleDateString(language === 'en' ? 'en-US' : 'ru-RU', { month: 'long', year: 'numeric' })}
                </span>
                <button type="button" className="scheduler-month-btn" onClick={() => changeMonth(1)} aria-label={t('next')}>
                  ›
                </button>
              </div>

              <div className="scheduler-weekday-row">
                {[t('mon'), t('tue'), t('wed'), t('thu'), t('fri'), t('sat'), t('sun')].map(day => (
                  <div key={day} className="scheduler-weekday-cell">{day}</div>
                ))}
              </div>

              <div className="scheduler-day-grid">
                {calendarDays.map((day, index) => {
                  const isToday = day && stripTime(day).getTime() === todayStrip.getTime()
                  const isSelected =
                    day &&
                    selectedDay &&
                    stripTime(day).getTime() === selectedDay.getTime()

                  return (
                    <div key={index} className="scheduler-day-cell">
                      {day ? (
                        <button
                          type="button"
                          className={`scheduler-day-btn${isSelected ? ' selected' : ''}${isToday ? ' today' : ''}`}
                          onClick={() => handlePickDay(day)}
                        >
                          {day.getDate()}
                        </button>
                      ) : (
                        <span className="scheduler-day-empty" />
                      )}
                    </div>
                  )
                })}
              </div>
            </div>

            <div className="scheduler-datetime-row">
              <div className="scheduler-field">
                <label htmlFor="scheduler-game-time">{t('schedulerGameTime')}</label>
                <input
                  id="scheduler-game-time"
                  type="time"
                  value={gameTime}
                  onChange={(e) => {
                    setGameTime(e.target.value)
                    setSelectedSlot(null)
                  }}
                  className="scheduler-input scheduler-input-time"
                />
              </div>
              <div className="scheduler-field">
                <label htmlFor="scheduler-duration">{t('schedulerDuration')}</label>
                <input
                  id="scheduler-duration"
                  type="number"
                  min={1}
                  max={24}
                  step={1}
                  value={durationHours}
                  onChange={(e) => {
                    const v = parseInt(e.target.value, 10)
                    if (!Number.isNaN(v)) {
                      setDurationHours(Math.min(24, Math.max(1, v)))
                    } else {
                      setDurationHours(1)
                    }
                    setSelectedSlot(null)
                  }}
                  className="scheduler-input"
                />
              </div>
            </div>

            <div className="game-info-inputs">
              <div className="scheduler-field">
                <label htmlFor="scheduler-title">{t('gameTitleOptional')}</label>
                <input
                  id="scheduler-title"
                  type="text"
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  placeholder={t('gameTitlePlaceholder')}
                  maxLength="255"
                  className="scheduler-input"
                />
              </div>
              <div className="scheduler-field">
                <label htmlFor="scheduler-desc">{t('descriptionOptional')}</label>
                <textarea
                  id="scheduler-desc"
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  placeholder={t('descriptionPlaceholder')}
                  maxLength="1000"
                  rows={3}
                  className="scheduler-textarea"
                />
              </div>
              <div className="scheduler-field scheduler-checkbox-row">
                <label className="scheduler-checkbox-label">
                  <input
                    type="checkbox"
                    checked={autoAddPlayers}
                    onChange={(e) => setAutoAddPlayers(e.target.checked)}
                  />
                  <span>{t('autoAddPlayersLabel')}</span>
                </label>
              </div>
              <div className="scheduler-field">
                <label htmlFor="scheduler-max-p">{t('maxParticipantsLabel')}</label>
                <input
                  id="scheduler-max-p"
                  type="number"
                  value={maxParticipants}
                  onChange={(e) => setMaxParticipants(e.target.value)}
                  placeholder={t('maxParticipantsPlaceholder')}
                  min={1}
                  className="scheduler-input"
                />
                <span className="scheduler-field-hint">{t('maxParticipantsHint')}</span>
              </div>
              {campaigns.length > 0 && (
                <div className="scheduler-field">
                  <label htmlFor="scheduler-campaign">{t('campaignOptional')}</label>
                  <select
                    id="scheduler-campaign"
                    value={selectedCampaignId}
                    onChange={(e) => setSelectedCampaignId(e.target.value)}
                    className="scheduler-input scheduler-select"
                  >
                    <option value="">{t('campaignNone')}</option>
                    {campaigns.map(campaign => (
                      <option key={campaign.id} value={campaign.id}>
                        {campaign.name}
                      </option>
                    ))}
                  </select>
                </div>
              )}
            </div>
          </div>

          <div className="slots-section">
            <h3>{t('topSlots')}</h3>
            {bestSlots.length > 0 ? (
              <div className="best-slots-list">
                {bestSlots.map((slot, index) => (
                  <button
                    type="button"
                    key={index}
                    className={`best-slot-item${isSameBestSlot(selectedSlot, slot) ? ' selected' : ''}`}
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
                  </button>
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
          <button type="button" className="cancel-button" onClick={onClose}>
            {t('cancel')}
          </button>
          <button
            type="button"
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
