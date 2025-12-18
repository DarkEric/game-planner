import { useState, useMemo, useEffect, useRef, useCallback } from 'react'
import { useLanguage } from '../i18n/LanguageContext'
import PlayerTooltip from './PlayerTooltip'
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
  const [tooltip, setTooltip] = useState({ visible: false, players: [], position: { x: 0, y: 0 }, title: '' })
  const gridRef = useRef(null)
  const containerRef = useRef(null)
  // Хранилище для колонок многочасовых игр: gameId -> column
  const gameColumnMapRef = useRef(new Map())
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

  // Очищаем маппинг колонок при изменении событий или дат
  useEffect(() => {
    gameColumnMapRef.current.clear()
  }, [events, currentStartDate, daysToShow])

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

  // Проверка перекрытия двух событий по времени (частичного или полного)
  const eventsOverlap = useCallback((event1, event2) => {
    const start1 = event1.start instanceof Date ? event1.start : new Date(event1.start)
    let end1
    if (event1.end) {
      end1 = event1.end instanceof Date ? event1.end : new Date(event1.end)
    } else {
      end1 = new Date(start1)
      end1.setHours(start1.getHours() + (event1.duration || 1))
    }
    
    const start2 = event2.start instanceof Date ? event2.start : new Date(event2.start)
    let end2
    if (event2.end) {
      end2 = event2.end instanceof Date ? event2.end : new Date(event2.end)
    } else {
      end2 = new Date(start2)
      end2.setHours(start2.getHours() + (event2.duration || 1))
    }
    
    // Перекрытие если: start1 < end2 && start2 < end1
    return start1 < end2 && start2 < end1
  }, [])

  // Распределение событий по колонкам с учетом перекрытий
  const calculateEventColumns = useCallback((events, date, hour) => {
    if (events.length === 0) {
      return []
    }

    // Убираем дубликаты событий по gameId/id
    const uniqueEventsMap = new Map()
    events.forEach(event => {
      const eventKey = event.game?.id || event.id || `event-${event.start}-${event.title}`
      if (!uniqueEventsMap.has(eventKey)) {
        uniqueEventsMap.set(eventKey, event)
      }
    })
    const uniqueEvents = Array.from(uniqueEventsMap.values())

    // Если только одно событие, оно занимает полную ширину
    if (uniqueEvents.length === 1) {
      return [{ event: uniqueEvents[0], column: 0, totalColumns: 1 }]
    }

    // Сортируем события по времени начала
    const sortedEvents = [...uniqueEvents].sort((a, b) => {
      const startA = a.start instanceof Date ? a.start : new Date(a.start)
      const startB = b.start instanceof Date ? b.start : new Date(b.start)
      return startA - startB
    })

    // Массив колонок: каждая колонка содержит события, которые в ней размещены
    const columns = []
    const result = []

    for (const event of sortedEvents) {
      // Проверяем, есть ли уже назначенная колонка для этой игры (для многочасовых игр)
      const gameId = event.game?.id || event.id || `event-${event.start}-${event.title}`
      let assignedColumn = gameColumnMapRef.current.get(gameId)

      // Если колонка уже назначена, проверяем, свободна ли она
      if (assignedColumn !== undefined && columns[assignedColumn]) {
        // Проверяем, не перекрывается ли событие с другими в этой колонке
        const hasOverlap = columns[assignedColumn].some(existingEvent => 
          eventsOverlap(event, existingEvent)
        )
        
        if (!hasOverlap) {
          // Колонка свободна, используем её
          columns[assignedColumn].push(event)
          result.push({ event, column: assignedColumn, totalColumns: Math.max(columns.length, assignedColumn + 1) })
          continue
        } else {
          // Колонка занята, нужно найти новую
          assignedColumn = undefined
        }
      }

      // Ищем первую свободную колонку
      let foundColumn = -1
      for (let colIndex = 0; colIndex < columns.length; colIndex++) {
        const columnEvents = columns[colIndex]
        // Проверяем, есть ли перекрытие с событиями в этой колонке
        const hasOverlap = columnEvents.some(existingEvent => 
          eventsOverlap(event, existingEvent)
        )
        
        if (!hasOverlap) {
          foundColumn = colIndex
          break
        }
      }

      // Если не нашли свободную колонку, создаем новую
      if (foundColumn === -1) {
        foundColumn = columns.length
        columns.push([])
      }

      // Размещаем событие в найденной колонке
      columns[foundColumn].push(event)
      
      // Сохраняем колонку для многочасовых игр
      if (gameId) {
        gameColumnMapRef.current.set(gameId, foundColumn)
      }

      result.push({ 
        event, 
        column: foundColumn, 
        totalColumns: columns.length 
      })
    }

    return result
  }, [eventsOverlap])

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
      
      // Событие должно отображаться только в ячейке, где оно начинается
      // Проверяем, начинается ли событие в этой ячейке
      const startsInThisCell = eventStart >= checkDate && eventStart < checkDateEnd
      
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
    // Используем event.end если доступен, иначе вычисляем из duration
    const eventStart = event.start instanceof Date ? event.start : new Date(event.start)
    let eventEnd
    if (event.end) {
      eventEnd = event.end instanceof Date ? event.end : new Date(event.end)
    } else {
      eventEnd = new Date(eventStart)
      eventEnd.setHours(eventStart.getHours() + (event.duration || 1))
    }
    
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
        return Math.max(durationHours, 0.5) // Минимум 0.5 часа для видимости
      } else {
        // Событие переходит на следующий день - показываем только до полуночи
        const hoursUntilMidnight = (endOfDay - eventStart) / (1000 * 60 * 60)
        return Math.max(hoursUntilMidnight, 0.5)
      }
    }
    
    // Если это продолжение события с предыдущего дня (hour === 0)
    if (hour === 0) {
      const currentDayStart = new Date(date)
      currentDayStart.setHours(0, 0, 0, 0)
      
      if (eventStart < currentDayStart && eventEnd > currentDayStart) {
        // Показываем оставшуюся часть события
        const remainingHours = (eventEnd - currentDayStart) / (1000 * 60 * 60)
        return Math.max(remainingHours, 0.5)
      }
    }
    
    // Fallback: используем duration если доступен
    return Math.max(event.duration || 1, 0.5)
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

  const handleCellMouseEnter = (e, availablePlayers) => {
    if (availablePlayers.length === 0 || isDragging) return
    
    const rect = e.currentTarget.getBoundingClientRect()
    setTooltip({
      visible: true,
      players: availablePlayers,
      position: {
        x: rect.right + 10,
        y: rect.top
      },
      title: t('available')
    })
  }

  const handleCellMouseLeave = () => {
    setTooltip({ visible: false, players: [], position: { x: 0, y: 0 }, title: t('available') })
  }

  const handleEventMouseEnter = (e, event) => {
    e.stopPropagation() // Предотвращаем всплытие к ячейке
    if (isDragging) return
    
    // Получаем участников из объекта игры
    const participants = event.game?.participants || []
    if (participants.length === 0) return
    
    const rect = e.currentTarget.getBoundingClientRect()
    setTooltip({
      visible: true,
      players: participants,
      position: {
        x: rect.right + 10,
        y: rect.top
      },
      title: t('gameParticipants')
    })
  }

  const handleEventMouseLeave = (e) => {
    e.stopPropagation() // Предотвращаем всплытие к ячейке
    setTooltip({ visible: false, players: [], position: { x: 0, y: 0 }, title: t('available') })
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
            {dates.map((date, dateIndex) => {
              // Собираем все события, которые пересекаются с этой датой
              const dateEvents = events.filter(event => {
                const eventStart = event.start instanceof Date ? event.start : new Date(event.start)
                let eventEnd
                if (event.end) {
                  eventEnd = event.end instanceof Date ? event.end : new Date(event.end)
                } else {
                  eventEnd = new Date(eventStart)
                  eventEnd.setHours(eventStart.getHours() + (event.duration || 1))
                }
                
                const eventDate = new Date(eventStart)
                eventDate.setHours(0, 0, 0, 0)
                const currentDate = new Date(date)
                currentDate.setHours(0, 0, 0, 0)
                const nextDate = new Date(currentDate)
                nextDate.setDate(currentDate.getDate() + 1)
                
                // Событие пересекается с этой датой, если оно начинается в этот день или продолжается в этот день
                return (eventDate.getTime() === currentDate.getTime()) || 
                       (eventStart < nextDate && eventEnd > currentDate)
              })
              
              // Вычисляем колонки для всех событий этой даты (учитывая все перекрытия)
              let allEventsColumns = []
              if (dateEvents.length > 0) {
                // Сортируем события по времени начала
                const sortedEvents = [...dateEvents].sort((a, b) => {
                  const startA = a.start instanceof Date ? a.start : new Date(a.start)
                  const startB = b.start instanceof Date ? b.start : new Date(b.start)
                  return startA - startB
                })
                
                const columns = []
                
                for (const event of sortedEvents) {
                  const gameId = event.game?.id || event.id || `event-${event.start}-${event.title}`
                  let assignedColumn = gameColumnMapRef.current.get(gameId)
                  
                  // Если колонка уже назначена, проверяем, свободна ли она
                  if (assignedColumn !== undefined && columns[assignedColumn]) {
                    const hasOverlap = columns[assignedColumn].some(existingEvent => 
                      eventsOverlap(event, existingEvent)
                    )
                    
                    if (!hasOverlap) {
                      columns[assignedColumn].push(event)
                      // totalColumns должен быть одинаковым для всех событий
                      allEventsColumns.push({ event, column: assignedColumn, totalColumns: columns.length })
                      continue
                    } else {
                      assignedColumn = undefined
                    }
                  }
                  
                  // Ищем первую свободную колонку
                  let foundColumn = -1
                  for (let colIndex = 0; colIndex < columns.length; colIndex++) {
                    const columnEvents = columns[colIndex]
                    const hasOverlap = columnEvents.some(existingEvent => 
                      eventsOverlap(event, existingEvent)
                    )
                    
                    if (!hasOverlap) {
                      foundColumn = colIndex
                      break
                    }
                  }
                  
                  if (foundColumn === -1) {
                    foundColumn = columns.length
                    columns.push([])
                  }
                  
                  columns[foundColumn].push(event)
                  
                  if (gameId) {
                    gameColumnMapRef.current.set(gameId, foundColumn)
                  }
                  
                  // totalColumns должен быть одинаковым для всех событий - используем текущее количество колонок
                  allEventsColumns.push({ 
                    event, 
                    column: foundColumn, 
                    totalColumns: columns.length 
                  })
                }
                
                // Обновляем totalColumns для всех событий, чтобы он был одинаковым
                const maxColumns = columns.length
                allEventsColumns = allEventsColumns.map(({ event, column }) => ({
                  event,
                  column,
                  totalColumns: maxColumns
                }))
              }
              
              return (
                <div key={dateIndex} className="date-column">
                  {hours.map((hour, hourIndex) => {
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
                        onMouseEnter={(e) => handleCellMouseEnter(e, availablePlayers)}
                        onMouseLeave={handleCellMouseLeave}
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
                      </div>
                    )
                  })}
                  
                  {/* Рендерим события на уровне колонки */}
                  {allEventsColumns.map(({ event, column, totalColumns }) => {
                    const eventStart = event.start instanceof Date ? event.start : new Date(event.start)
                    let eventEnd
                    if (event.end) {
                      eventEnd = event.end instanceof Date ? event.end : new Date(event.end)
                    } else {
                      eventEnd = new Date(eventStart)
                      eventEnd.setHours(eventStart.getHours() + (event.duration || 1))
                    }
                    
                    const eventDate = new Date(eventStart)
                    eventDate.setHours(0, 0, 0, 0)
                    const currentDate = new Date(date)
                    currentDate.setHours(0, 0, 0, 0)
                    const nextDate = new Date(currentDate)
                    nextDate.setDate(currentDate.getDate() + 1)
                    
                    // Определяем, в какой час начинается событие для этой даты
                    let startHour
                    let displayHeight
                    
                    if (eventDate.getTime() === currentDate.getTime()) {
                      // Событие начинается в этот день
                      startHour = eventStart.getHours()
                      displayHeight = getEventDisplayHeight(event, date, startHour)
                    } else if (eventStart < currentDate && eventEnd > currentDate) {
                      // Событие продолжается с предыдущего дня - начинаем с 0
                      startHour = 0
                      // Вычисляем высоту от начала дня до конца события или конца дня
                      const dayEnd = new Date(nextDate)
                      const remainingHours = (Math.min(eventEnd, dayEnd) - currentDate) / (1000 * 60 * 60)
                      displayHeight = Math.max(remainingHours, 0.5)
                    } else {
                      // Событие не должно отображаться в этот день
                      return null
                    }
                    
                    // Вычисляем позицию и ширину для колонки
                    let left, width
                    if (totalColumns === 1) {
                      left = '2px'
                      width = 'calc(100% - 4px)'
                    } else {
                      const columnWidth = 100 / totalColumns
                      const gap = 1
                      left = `${column * columnWidth + gap}%`
                      width = `${columnWidth - gap * 2}%`
                    }
                    
                    // Вычисляем top относительно начала колонки
                    const top = `${startHour * 40 + 2}px`
                    
                    const eventKey = event.game?.id || event.id || `${event.start}-${event.title}`
                    return (
                      <div
                        key={`${eventKey}-${date.getTime()}`}
                        className="timeline-event"
                        onClick={(e) => {
                          e.stopPropagation()
                          onEventClick && onEventClick(event)
                        }}
                        onMouseEnter={(e) => handleEventMouseEnter(e, event)}
                        onMouseLeave={handleEventMouseLeave}
                        style={{
                          backgroundColor: event.color || '#646cff',
                          height: `${displayHeight * 40}px`,
                          left: left,
                          width: width,
                          top: top,
                          minWidth: '50px',
                          position: 'absolute'
                        }}
                      >
                        <div className="event-title">{event.title}</div>
                        {event.game && (() => {
                          const participantCount = event.game.participants 
                            ? event.game.participants.filter(p => p.id !== event.game.creatorId).length 
                            : 0
                          const maxParticipants = event.game.maxParticipants
                          const hasMaxParticipants = maxParticipants != null && maxParticipants !== undefined
                          const playersInfo = hasMaxParticipants
                            ? `${participantCount}/${maxParticipants} игроков`
                            : `${participantCount} игроков`
                          return (
                            <div className="event-players-info" style={{
                              fontSize: '0.85rem',
                              opacity: 0.9,
                              marginTop: '2px'
                            }}>
                              👥 {playersInfo}
                            </div>
                          )
                        })()}
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
        </div>
      </div>

      <PlayerTooltip 
        players={tooltip.players}
        visible={tooltip.visible}
        position={tooltip.position}
        title={tooltip.title}
      />
    </div>
  )
}

export default CalendarTimeline

