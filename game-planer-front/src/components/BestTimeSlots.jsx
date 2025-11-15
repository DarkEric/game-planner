import { useMemo } from 'react'
import './BestTimeSlots.css'

const BestTimeSlots = ({ players, dates, hours, minPlayers = 2 }) => {
  const bestSlots = useMemo(() => {
    const slots = []
    
    dates.forEach(date => {
      hours.forEach(hour => {
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
        
        if (availablePlayers.length >= minPlayers) {
          slots.push({
            date,
            hour,
            availablePlayers,
            count: availablePlayers.length,
            playerNames: availablePlayers.map(p => p.name).join(', ')
          })
        }
      })
    })
    
    // Объединяем последовательные слоты
    const mergedSlots = []
    const sortedSlots = slots.sort((a, b) => {
      const dateCompare = a.date.getTime() - b.date.getTime()
      return dateCompare !== 0 ? dateCompare : a.hour - b.hour
    })
    
    for (let i = 0; i < sortedSlots.length; i++) {
      const currentSlot = sortedSlots[i]
      
      // Проверяем, можно ли объединить с предыдущим слотом
      if (mergedSlots.length > 0) {
        const lastMerged = mergedSlots[mergedSlots.length - 1]
        const isSameDate = lastMerged.date.toDateString() === currentSlot.date.toDateString()
        const isConsecutive = lastMerged.endHour === currentSlot.hour
        const hasSamePlayers = lastMerged.count === currentSlot.count &&
          lastMerged.availablePlayers.every(p => 
            currentSlot.availablePlayers.some(cp => cp.id === p.id)
          )
        
        if (isSameDate && isConsecutive && hasSamePlayers) {
          // Объединяем слоты
          lastMerged.endHour = currentSlot.hour + 1
          lastMerged.duration = lastMerged.endHour - lastMerged.hour
          continue
        }
      }
      
      // Добавляем новый слот
      mergedSlots.push({
        ...currentSlot,
        endHour: currentSlot.hour + 1,
        duration: 1
      })
    }
    
    return mergedSlots
      .sort((a, b) => b.count - a.count)
      .slice(0, 10)
  }, [players, dates, hours, minPlayers])

  const formatDateTime = (date, hour, endHour) => {
    const dateStr = date.toLocaleDateString('ru-RU', {
      weekday: 'long',
      day: 'numeric',
      month: 'long'
    })
    const startTime = `${hour.toString().padStart(2, '0')}:00`
    
    if (endHour && endHour > hour + 1) {
      const endTime = `${endHour.toString().padStart(2, '0')}:00`
      return `${dateStr}, ${startTime} - ${endTime}`
    }
    
    return `${dateStr}, ${startTime}`
  }

  if (bestSlots.length === 0) {
    return (
      <div className="best-time-slots">
        <h2>Лучшие временные слоты</h2>
        <p className="no-slots">
          Нет временных слотов, когда доступно минимум {minPlayers} игроков
        </p>
      </div>
    )
  }

  return (
    <div className="best-time-slots">
      <h2>Лучшие временные слоты</h2>
      <div className="slots-list">
        {bestSlots.map((slot, index) => (
          <div key={index} className="slot-item">
            <div className="slot-header">
              <div className="slot-rank">#{index + 1}</div>
              <div className="slot-info">
                <div className="slot-datetime">
                  {formatDateTime(slot.date, slot.hour, slot.endHour)}
                  {slot.duration > 1 && (
                    <span className="slot-duration"> ({slot.duration}ч)</span>
                  )}
                </div>
                <div className="slot-players-count">
                  {slot.count} из {players.length} игроков
                </div>
              </div>
            </div>
            <div className="slot-players">
              {slot.availablePlayers.map(player => (
                <span
                  key={player.id}
                  className="player-badge"
                  style={{ backgroundColor: player.color }}
                >
                  {player.name}
                </span>
              ))}
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}

export default BestTimeSlots

