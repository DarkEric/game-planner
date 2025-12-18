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
                    const participantCount = game.participants ? game.participants.length : 0
                    const maxParticipants = game.maxParticipants
                    const playersInfo = maxParticipants != null 
                        ? `${participantCount}/${maxParticipants} игроков`
                        : `${participantCount} игроков`
                    return {
                        id: game.id,
                        start: game.startTime,
                        end: game.endTime,
                        title: game.title || `🎲 Игра`,
                        description: game.description ? `${game.description} | ${playersInfo}` : playersInfo,
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
