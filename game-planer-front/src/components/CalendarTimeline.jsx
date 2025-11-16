import { useState, useMemo, useEffect, useRef, useCallback } from 'react'
import { useLanguage } from '../i18n/LanguageContext'
import './CalendarTimeline.css'

const CalendarTimeline = ({ 
  startDate = new Date(), 
  daysToShow = 7,
  events = [],
  players = [],
  selectedPlayerId = null,
  onDateClick,
  onEventClick,
  onTimeSlotClick,
  onTimeSlotsSelect,
  showAvailabilityOverlap = true,
  onDateChange
}) => {
  const [currentStartDate, setCurrentStartDate] = useState(startDate)
  const [isDragging, setIsDragging] = useState(false)
  const [dragStart, setDragStart] = useState(null)
  const [dragEnd, setDragEnd] = useState(null)
  const gridRef = useRef(null)
  const containerRef = useRef(null)
  const { t } = useLanguage()

  useEffect(() => {
    setCurrentStartDate(startDate)
  }, [startDate])

  // Автоматическая прокрутка к 12 часам при монтировании
  useEffect(() => {
    // Небольшая задержка для корректной прокрутки после рендера
    setTimeout(scrollToNoon, 100)
  }, [])

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

  const formatDate = (date) => {
    return date.toLocaleDateString('ru-RU', { 
      weekday: 'short', 
      day: 'numeric', 
      month: 'short' 
    })
  }

  const formatHour = (hour) => {
    return `${hour.toString().padStart(2, '0')}:00`
  }

  const getEventsForDateAndHour = (date, hour) => {
    return events.filter(event => {
      const eventStart = event.start instanceof Date ? event.start : new Date(event.start)
      const eventEnd = new Date(eventStart)
      eventEnd.setHours(eventStart.getHours() + (event.duration || 1))
      
      // Создаем Date для проверяемого часа
      const checkDate = new Date(date)
      checkDate.setHours(hour, 0, 0, 0)
      const checkDateEnd = new Date(checkDate)
      checkDateEnd.setHours(hour + 1, 0, 0, 0)
      
      // Проверяем, начинается ли событие в этой ячейке
      const startsInThisCell = eventStart >= checkDate && eventStart < checkDateEnd
      
      // Если событие начинается в этой ячейке, показываем его
      if (startsInThisCell) {
        return true
      }
      
      // Если событие переходит через полночь и продолжается на следующий день,
      // показываем его продолжение в 00:00 следующего дня
      if (hour === 0) {
        const eventStartDate = new Date(eventStart)
        eventStartDate.setHours(0, 0, 0, 0)
        const currentDate = new Date(date)
        currentDate.setHours(0, 0, 0, 0)
        
        // Событие началось раньше и продолжается в этот день
        if (eventStartDate < currentDate && eventEnd > checkDate) {
          return true
        }
      }
      
      return false
    })
  }
  
  const getEventDisplayHeight = (event, date, hour) => {
    const eventStart = event.start instanceof Date ? event.start : new Date(event.start)
    const eventEnd = new Date(eventStart)
    eventEnd.setHours(eventStart.getHours() + (event.duration || 1))
    
    const checkDate = new Date(date)
    checkDate.setHours(hour, 0, 0, 0)
    const checkDateEnd = new Date(checkDate)
    checkDateEnd.setHours(hour + 1, 0, 0, 0)
    
    // Если событие начинается в этой ячейке
    if (eventStart >= checkDate && eventStart < checkDateEnd) {
      // Конец дня (полночь следующего дня)
      const endOfDay = new Date(date)
      endOfDay.setHours(24, 0, 0, 0)
      
      // Если событие заканчивается до конца дня - показываем полную высоту
      if (eventEnd <= endOfDay) {
        const durationHours = (eventEnd - eventStart) / (1000 * 60 * 60)
        return durationHours
      } else {
        // Событие переходит на следующий день - показываем только до полуночи
        const hoursUntilMidnight = (endOfDay - eventStart) / (1000 * 60 * 60)
        return hoursUntilMidnight
      }
    }
    
    // Если это продолжение события с предыдущего дня (hour === 0)
    if (hour === 0) {
      const currentDayStart = new Date(date)
      currentDayStart.setHours(0, 0, 0, 0)
      
      if (eventStart < currentDayStart && eventEnd > currentDayStart) {
        // Показываем оставшуюся часть события
        const remainingHours = (eventEnd - currentDayStart) / (1000 * 60 * 60)
        return remainingHours
      }
    }
    
    return event.duration || 1
  }

  const getAvailabilityForDateAndHour = (date, hour) => {
    const availablePlayers = players.filter(player => {
      return player.availableTimes.some(timeSlot => {
        const slotDate = new Date(timeSlot.start)
        const slotEnd = new Date(slotDate)
        slotEnd.setHours(slotDate.getHours() + (timeSlot.duration || 1))
        
        const checkDate = new Date(date)
        checkDate.setHours(hour, 0, 0, 0)
        const checkDateEnd = new Date(checkDate)
        checkDateEnd.setHours(hour + 1, 0, 0, 0)
        
        // Проверяем пересечение временных интервалов
        // Слот доступен если он начинается до конца проверяемого часа
        // И заканчивается после начала проверяемого часа
        return slotDate < checkDateEnd && slotEnd > checkDate
      })
    })
    return availablePlayers
  }

  const getAvailabilityOverlap = (date, hour) => {
    const availablePlayers = getAvailabilityForDateAndHour(date, hour)
    return availablePlayers.length
  }

  const isTimeSlotSelected = (date, hour) => {
    if (!selectedPlayerId) return false
    const player = players.find(p => p.id === selectedPlayerId)
    if (!player) return false
    
    return player.availableTimes.some(timeSlot => {
      const slotDate = new Date(timeSlot.start)
      const slotEnd = new Date(slotDate)
      slotEnd.setHours(slotDate.getHours() + (timeSlot.duration || 1))
      
      const checkDate = new Date(date)
      checkDate.setHours(hour, 0, 0, 0)
      const checkDateEnd = new Date(checkDate)
      checkDateEnd.setHours(hour + 1, 0, 0, 0)
      
      // Проверяем пересечение временных интервалов
      return slotDate < checkDateEnd && slotEnd > checkDate
    })
  }

  const isTimeSlotInDragSelection = (date, hour) => {
    if (!isDragging || !dragStart || !dragEnd) return false
    
    const dateIndex = dates.findIndex(d => d.toDateString() === date.toDateString())
    const startDateIndex = dates.findIndex(d => d.toDateString() === dragStart.date.toDateString())
    const endDateIndex = dates.findIndex(d => d.toDateString() === dragEnd.date.toDateString())
    
    const minDateIndex = Math.min(startDateIndex, endDateIndex)
    const maxDateIndex = Math.max(startDateIndex, endDateIndex)
    const minHour = Math.min(dragStart.hour, dragEnd.hour)
    const maxHour = Math.max(dragStart.hour, dragEnd.hour)
    
    return (
      dateIndex >= minDateIndex &&
      dateIndex <= maxDateIndex &&
      hour >= minHour &&
      hour <= maxHour
    )
  }

  const getCellFromEvent = useCallback((e) => {
    const cell = e.target.closest('.time-cell')
    if (!cell) return null
    
    const dateColumn = cell.closest('.date-column')
    const grid = dateColumn?.parentElement
    if (!grid || !gridRef.current) return null
    
    const dateIndex = Array.from(grid.children).indexOf(dateColumn)
    const hourIndex = Array.from(dateColumn.children).indexOf(cell)
    
    if (dateIndex < 0 || hourIndex < 0 || dateIndex >= dates.length || hourIndex >= hours.length) return null
    
    return {
      date: dates[dateIndex],
      hour: hours[hourIndex],
      dateIndex,
      hourIndex
    }
  }, [dates, hours])

  const handleMouseDown = (e) => {
    if (!selectedPlayerId || e.button !== 0) return // Только левая кнопка мыши
    
    const cell = getCellFromEvent(e)
    if (!cell) return
    
    // Предотвращаем выделение при клике на событие
    if (e.target.closest('.timeline-event')) return
    
    setIsDragging(true)
    setDragStart(cell)
    setDragEnd(cell)
    e.preventDefault()
  }

  const handleMouseMove = useCallback((e) => {
    if (!isDragging || !dragStart) return
    
    const cell = getCellFromEvent(e)
    if (cell) {
      setDragEnd(cell)
    }
  }, [isDragging, dragStart, getCellFromEvent])

  const handleMouseUp = useCallback((e) => {
    if (!isDragging || !dragStart || !dragEnd) {
      setIsDragging(false)
      setDragStart(null)
      setDragEnd(null)
      return
    }
    
    // Определяем все ячейки в выделенной области
    const selectedSlots = []
    const startDateIndex = dates.findIndex(d => d.toDateString() === dragStart.date.toDateString())
    const endDateIndex = dates.findIndex(d => d.toDateString() === dragEnd.date.toDateString())
    const minDateIndex = Math.min(startDateIndex, endDateIndex)
    const maxDateIndex = Math.max(startDateIndex, endDateIndex)
    const minHour = Math.min(dragStart.hour, dragEnd.hour)
    const maxHour = Math.max(dragStart.hour, dragEnd.hour)
    
    for (let dateIdx = minDateIndex; dateIdx <= maxDateIndex; dateIdx++) {
      for (let hourIdx = minHour; hourIdx <= maxHour; hourIdx++) {
        selectedSlots.push({
          date: dates[dateIdx],
          hour: hours[hourIdx]
        })
      }
    }
    
    // Если есть обработчик для массового выбора, используем его
    if (onTimeSlotsSelect && selectedSlots.length > 0) {
      onTimeSlotsSelect(selectedSlots)
    } else if (onTimeSlotClick && selectedSlots.length === 1) {
      // Если только одна ячейка, используем обычный обработчик
      onTimeSlotClick(selectedSlots[0].date, selectedSlots[0].hour)
    }
    
    setIsDragging(false)
    setDragStart(null)
    setDragEnd(null)
  }, [isDragging, dragStart, dragEnd, dates, hours, onTimeSlotsSelect, onTimeSlotClick, selectedPlayerId])

  useEffect(() => {
    if (isDragging) {
      document.addEventListener('mousemove', handleMouseMove)
      document.addEventListener('mouseup', handleMouseUp)
      
      return () => {
        document.removeEventListener('mousemove', handleMouseMove)
        document.removeEventListener('mouseup', handleMouseUp)
      }
    }
  }, [isDragging, handleMouseMove, handleMouseUp])

  const scrollToNoon = () => {
    if (containerRef.current) {
      containerRef.current.scrollTop = 12 * 40
    }
  }

  const navigateDays = (direction) => {
    const newDate = new Date(currentStartDate)
    newDate.setDate(currentStartDate.getDate() + (direction * daysToShow))
    setCurrentStartDate(newDate)
    if (onDateChange) {
      onDateChange(newDate)
    }
    // Прокручиваем к 12 часам после небольшой задержки
    setTimeout(scrollToNoon, 100)
  }

  const goToToday = () => {
    const today = new Date()
    today.setHours(0, 0, 0, 0)
    setCurrentStartDate(today)
    if (onDateChange) {
      onDateChange(today)
    }
    // Прокручиваем к 12 часам после небольшой задержки
    setTimeout(scrollToNoon, 100)
  }

  return (
    <div className="calendar-timeline">
      <div className="calendar-timeline-header">
        <button onClick={() => navigateDays(-1)} className="nav-button">
          ← {t('previous')}
        </button>
        <button onClick={goToToday} className="today-button">
          {t('today')}
        </button>
        <button onClick={() => navigateDays(1)} className="nav-button">
          {t('next')} →
        </button>
      </div>

      <div className="calendar-timeline-container" ref={containerRef}>
        <div className="timeline-hours-column">
          <div className="hours-header"></div>
          <div className="hours-list">
            {hours.map(hour => (
              <div key={hour} className="hour-cell">
                {formatHour(hour)}
              </div>
            ))}
          </div>
        </div>

        <div className="timeline-dates-container">
          <div className="dates-header">
            {dates.map((date, index) => (
              <div 
                key={index} 
                className="date-header-cell"
                onClick={() => onDateClick && onDateClick(date)}
              >
                <div className="date-header-day">{date.getDate()}</div>
                <div className="date-header-weekday">
                  {date.toLocaleDateString('ru-RU', { weekday: 'short' })}
                </div>
              </div>
            ))}
          </div>

          <div 
            className="timeline-grid"
            ref={gridRef}
            onMouseDown={handleMouseDown}
            style={{ userSelect: 'none' }}
          >
            {dates.map((date, dateIndex) => (
              <div key={dateIndex} className="date-column">
                {hours.map((hour, hourIndex) => {
                  const hourEvents = getEventsForDateAndHour(date, hour)
                  const availablePlayers = getAvailabilityForDateAndHour(date, hour)
                  const overlapCount = getAvailabilityOverlap(date, hour)
                  const isSelected = isTimeSlotSelected(date, hour)
                  const isInDragSelection = isTimeSlotInDragSelection(date, hour)
                  const maxPlayers = players.length
                  const overlapPercentage = maxPlayers > 0 ? (overlapCount / maxPlayers) * 100 : 0
                  
                  return (
                    <div 
                      key={hourIndex} 
                      className={`time-cell ${isSelected ? 'time-cell-selected' : ''} ${isInDragSelection ? 'time-cell-dragging' : ''}`}
                      style={{
                        backgroundColor: isInDragSelection
                          ? 'rgba(100, 108, 255, 0.4)'
                          : showAvailabilityOverlap && overlapCount > 0
                          ? `rgba(100, 108, 255, ${Math.min(overlapPercentage / 100, 0.3)})`
                          : undefined
                      }}
                      title={
                        overlapCount > 0
                          ? `Доступно игроков: ${overlapCount} из ${maxPlayers}`
                          : 'Нажмите или протяните, чтобы отметить время'
                      }
                    >
                      {showAvailabilityOverlap && overlapCount > 0 && (
                        <div className="availability-indicator">
                          {overlapCount}/{maxPlayers}
                        </div>
                      )}
                      
                      {availablePlayers.map((player, playerIndex) => (
                        <div
                          key={player.id}
                          className="player-availability-marker"
                          style={{
                            backgroundColor: player.color,
                            left: `${playerIndex * 4}px`,
                            width: '3px',
                            height: '100%',
                            position: 'absolute',
                            top: 0
                          }}
                          title={player.name}
                        />
                      ))}
                      
                      {hourEvents.map((event, eventIndex) => {
                        const displayHeight = getEventDisplayHeight(event, date, hour)
                        return (
                          <div
                            key={eventIndex}
                            className="timeline-event"
                            onClick={(e) => {
                              e.stopPropagation()
                              onEventClick && onEventClick(event)
                            }}
                            style={{
                              backgroundColor: event.color || '#646cff',
                              height: `${displayHeight * 40}px`
                            }}
                          >
                            <div className="event-title">{event.title}</div>
                            {event.description && (
                              <div className="event-description">{event.description}</div>
                            )}
                          </div>
                        )
                      })}
                    </div>
                  )
                })}
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  )
}

export default CalendarTimeline

