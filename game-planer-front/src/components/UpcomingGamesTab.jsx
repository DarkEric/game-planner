import { useMemo, useState, useEffect } from 'react'
import { useLanguage } from '../i18n/LanguageContext'
import './UpcomingGamesTab.css'

/** Одна строка: время и дата в локальном часовом поясе устройства */
function formatWhenLine(date, language) {
  const locale = language === 'en' ? 'en-US' : 'ru-RU'
  return new Intl.DateTimeFormat(locale, {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    hour: '2-digit',
    minute: '2-digit'
  }).format(date)
}

function formatDateBox(date, language) {
  const locale = language === 'en' ? 'en-US' : 'ru-RU'
  const day = new Intl.DateTimeFormat(locale, { day: 'numeric' }).format(date)
  const month = new Intl.DateTimeFormat(locale, { month: 'short' }).format(date)
  return { day, month }
}

const UpcomingGamesTab = ({ games, onSelectGame }) => {
  const { t, language } = useLanguage()
  const [now, setNow] = useState(() => new Date())

  useEffect(() => {
    const id = setInterval(() => setNow(new Date()), 60_000)
    return () => clearInterval(id)
  }, [])

  const upcoming = useMemo(() => {
    const t0 = now.getTime()
    return (games || [])
      .filter(g => g.startTime && new Date(g.startTime).getTime() >= t0)
      .sort((a, b) => new Date(a.startTime) - new Date(b.startTime))
  }, [games, now])

  const participantCountExcludingCreator = (game) => {
    if (!game.participants) return 0
    return game.participants.filter(p => p.id !== game.creatorId).length
  }

  if (upcoming.length === 0) {
    return (
      <div className="upcoming-games-tab">
        <p className="upcoming-games-empty">{t('upcomingGamesEmpty')}</p>
      </div>
    )
  }

  return (
    <div className="upcoming-games-tab">
      <ul className="upcoming-games-list">
        {upcoming.map(game => {
          const count = participantCountExcludingCreator(game)
          const max = game.maxParticipants
          const countLabel = max != null
            ? `${count}/${max}`
            : `${count}`
          const start = new Date(game.startTime)
          const { day, month } = formatDateBox(start, language)

          return (
            <li key={game.id}>
              <button
                type="button"
                className="upcoming-game-card"
                onClick={() => onSelectGame(game)}
              >
                <div className="upcoming-game-card-datebox" aria-hidden>
                  <span className="upcoming-game-card-day">{day}</span>
                  <span className="upcoming-game-card-month">{month}</span>
                </div>
                <div className="upcoming-game-card-body">
                  <span className="upcoming-game-card-title">
                    {game.title?.trim() ? game.title : t('game')}
                  </span>
                  <span className="upcoming-game-card-when">
                    {formatWhenLine(start, language)}
                  </span>
                  {game.campaignName && (
                    <span className="upcoming-game-card-campaign">{game.campaignName}</span>
                  )}
                  <span className="upcoming-game-card-participants">
                    {t('participants')} <strong>{countLabel}</strong>
                  </span>
                </div>
                <span className="upcoming-game-card-arrow" aria-hidden>›</span>
              </button>
            </li>
          )
        })}
      </ul>
    </div>
  )
}

export default UpcomingGamesTab
