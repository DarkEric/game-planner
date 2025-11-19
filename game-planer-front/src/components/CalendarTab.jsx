import CalendarTimeline from './CalendarTimeline'
import BestTimeSlots from './BestTimeSlots'
import { useLanguage } from '../i18n/LanguageContext'

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
    onDateChange,
    onScheduleGame
}) => {
    const { t } = useLanguage()

    return (
        <div className="calendar-tab">
            <div className="calendar-header">
                <button
                    onClick={onScheduleGame}
                    className="schedule-game-button"
                >
                    🎲 {t('scheduleGame')}
                </button>
            </div>

            <div className="calendar-content">
                <CalendarTimeline
                    startDate={currentStartDate}
                    daysToShow={daysToShow}
                    players={allPlayers}
                    selectedPlayerId={currentPlayer?.id}
                    events={games.map(game => ({
                        id: game.id,
                        start: game.startTime,
                        end: game.endTime,
                        title: game.title || `🎲 Игра`,
                        description: game.description || `${game.participants.length} игроков`,
                        color: '#646cff',
                        duration: Math.ceil((game.endTime - game.startTime) / (1000 * 60 * 60)),
                        game: game
                    }))}
                    onTimeSlotClick={onTimeSlotClick}
                    onTimeSlotsSelect={onTimeSlotsSelect}
                    onEventClick={onEventClick}
                    showAvailabilityOverlap={true}
                    onDateChange={onDateChange}
                />
            </div>

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
