import CalendarTimeline from './CalendarTimeline'
import BestTimeSlots from './BestTimeSlots'

const CalendarTab = ({
    currentStartDate,
    daysToShow,
    allPlayers,
    currentPlayer,
    games,
    dates,
    hours,
    onTimeSlotClick,
    onTimeSlotsSelect,
    onEventClick,
    onDateChange
}) => {
    return (
        <div className="calendar-tab">
            <CalendarTimeline
                startDate={currentStartDate}
                daysToShow={daysToShow}
                players={allPlayers}
                selectedPlayerId={currentPlayer?.id}
                events={games.map(game => {
                    // Подсчет участников без создателя
                    const participantCount = game.participants 
                        ? game.participants.filter(p => p.id !== game.creatorId).length 
                        : 0
                    const maxParticipants = game.maxParticipants
                    // Проверяем, что maxParticipants задан (не null и не undefined)
                    const hasMaxParticipants = maxParticipants != null && maxParticipants !== undefined
                    return {
                        id: game.id,
                        start: game.startTime,
                        end: game.endTime,
                        title: game.title || `🎲 Игра`,
                        description: game.description || null,
                        color: '#646cff',
                        duration: Math.ceil((game.endTime - game.startTime) / (1000 * 60 * 60)),
                        game: game
                    }
                })}
                onTimeSlotClick={onTimeSlotClick}
                onTimeSlotsSelect={onTimeSlotsSelect}
                onEventClick={onEventClick}
                showAvailabilityOverlap={true}
                onDateChange={onDateChange}
            />

            {allPlayers.length >= 2 && (
                <div className="best-slots-section">
                    <BestTimeSlots
                        players={allPlayers}
                        dates={dates}
                        hours={hours}
                        minPlayers={2}
                    />
                </div>
            )}
        </div>
    )
}

export default CalendarTab
