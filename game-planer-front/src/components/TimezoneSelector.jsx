import { useState, useEffect } from 'react'
import { getUserTimezone } from '../utils/dateUtils'
import './TimezoneSelector.css'

/**
 * Компонент для выбора часового пояса пользователя
 */
const TimezoneSelector = ({ currentTimezone, onTimezoneChange }) => {
  const [isOpen, setIsOpen] = useState(false)
  const [selectedTimezone, setSelectedTimezone] = useState(currentTimezone)
  const [searchQuery, setSearchQuery] = useState('')

  // Популярные часовые пояса (российские города)
  const popularTimezones = [
    { value: 'Europe/Moscow', label: 'Москва (UTC+3)', offset: 3 },
    { value: 'Europe/Samara', label: 'Самара (UTC+4)', offset: 4 },
    { value: 'Asia/Yekaterinburg', label: 'Екатеринбург (UTC+5)', offset: 5 },
    { value: 'Asia/Omsk', label: 'Омск (UTC+6)', offset: 6 },
    { value: 'Asia/Novosibirsk', label: 'Новосибирск (UTC+7)', offset: 7 },
    { value: 'Asia/Krasnoyarsk', label: 'Красноярск (UTC+7)', offset: 7 },
    { value: 'Asia/Irkutsk', label: 'Иркутск (UTC+8)', offset: 8 },
    { value: 'Asia/Yakutsk', label: 'Якутск (UTC+9)', offset: 9 },
    { value: 'Asia/Vladivostok', label: 'Владивосток (UTC+10)', offset: 10 },
    { value: 'Asia/Magadan', label: 'Магадан (UTC+11)', offset: 11 },
    { value: 'Asia/Kamchatka', label: 'Камчатка (UTC+12)', offset: 12 },
    { value: 'Europe/Kaliningrad', label: 'Калининград (UTC+2)', offset: 2 }
  ]

  // Все доступные часовые пояса
  const allTimezones = [
    ...popularTimezones,
    // Другие города России и СНГ
    { value: 'Europe/Minsk', label: 'Минск (UTC+3)', offset: 3 },
    { value: 'Europe/Kirov', label: 'Киров (UTC+3)', offset: 3 },
    { value: 'Europe/Astrakhan', label: 'Астрахань (UTC+4)', offset: 4 },
    { value: 'Europe/Saratov', label: 'Саратов (UTC+4)', offset: 4 },
    { value: 'Europe/Ulyanovsk', label: 'Ульяновск (UTC+4)', offset: 4 },
    { value: 'Asia/Barnaul', label: 'Барнаул (UTC+7)', offset: 7 },
    { value: 'Asia/Tomsk', label: 'Томск (UTC+7)', offset: 7 },
    { value: 'Asia/Chita', label: 'Чита (UTC+9)', offset: 9 },
    { value: 'Asia/Khandyga', label: 'Хандыга (UTC+9)', offset: 9 },
    { value: 'Asia/Sakhalin', label: 'Сахалин (UTC+11)', offset: 11 },
    { value: 'Asia/Srednekolymsk', label: 'Среднеколымск (UTC+11)', offset: 11 },
    { value: 'Asia/Anadyr', label: 'Анадырь (UTC+12)', offset: 12 },
    // Международные
    { value: 'UTC', label: 'UTC (UTC+0)', offset: 0 },
    { value: 'Europe/London', label: 'Лондон (UTC+0)', offset: 0 },
    { value: 'Europe/Paris', label: 'Париж (UTC+1)', offset: 1 },
    { value: 'Europe/Berlin', label: 'Берлин (UTC+1)', offset: 1 },
    { value: 'Europe/Istanbul', label: 'Стамбул (UTC+3)', offset: 3 },
    { value: 'Asia/Dubai', label: 'Дубай (UTC+4)', offset: 4 },
    { value: 'Asia/Shanghai', label: 'Шанхай (UTC+8)', offset: 8 },
    { value: 'Asia/Singapore', label: 'Сингапур (UTC+8)', offset: 8 },
    { value: 'Asia/Tokyo', label: 'Токио (UTC+9)', offset: 9 },
    { value: 'Asia/Seoul', label: 'Сеул (UTC+9)', offset: 9 },
    { value: 'Australia/Sydney', label: 'Сидней (UTC+11)', offset: 11 },
    { value: 'Pacific/Auckland', label: 'Окленд (UTC+13)', offset: 13 },
    { value: 'America/New_York', label: 'Нью-Йорк (UTC-5)', offset: -5 },
    { value: 'America/Toronto', label: 'Торонто (UTC-5)', offset: -5 },
    { value: 'America/Chicago', label: 'Чикаго (UTC-6)', offset: -6 },
    { value: 'America/Mexico_City', label: 'Мехико (UTC-6)', offset: -6 },
    { value: 'America/Los_Angeles', label: 'Лос-Анджелес (UTC-8)', offset: -8 },
    { value: 'America/Sao_Paulo', label: 'Сан-Паулу (UTC-3)', offset: -3 }
  ]

  useEffect(() => {
    setSelectedTimezone(currentTimezone)
  }, [currentTimezone])

  const handleDetectTimezone = () => {
    const detected = getUserTimezone()
    setSelectedTimezone(detected.timezone)
    handleSave(detected.timezone)
  }

  const handleSave = (timezone) => {
    if (onTimezoneChange) {
      onTimezoneChange(timezone || selectedTimezone)
    }
    setIsOpen(false)
  }

  const filteredTimezones = allTimezones.filter(tz =>
    tz.label.toLowerCase().includes(searchQuery.toLowerCase()) ||
    tz.value.toLowerCase().includes(searchQuery.toLowerCase())
  )

  const getCurrentTimezoneLabel = () => {
    if (!currentTimezone) return 'Не указан'
    const tz = allTimezones.find(t => t.value === currentTimezone)
    return tz ? tz.label : currentTimezone
  }

  return (
    <div className="timezone-selector">
      <div className="timezone-display" onClick={() => setIsOpen(!isOpen)}>
        <span className="timezone-icon">🌍</span>
        <div className="timezone-info">
          <div className="timezone-label">Часовой пояс</div>
          <div className="timezone-value">{getCurrentTimezoneLabel()}</div>
        </div>
        <span className="timezone-arrow">{isOpen ? '▲' : '▼'}</span>
      </div>

      {isOpen && (
        <div className="timezone-dropdown">
          <div className="timezone-dropdown-header">
            <h3>Выберите часовой пояс</h3>
            <button 
              className="timezone-close"
              onClick={() => setIsOpen(false)}
            >
              ×
            </button>
          </div>

          <div className="timezone-search">
            <input
              type="text"
              placeholder="Поиск города или часового пояса..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="timezone-search-input"
            />
          </div>

          <button
            className="timezone-detect-btn"
            onClick={handleDetectTimezone}
          >
            🎯 Определить автоматически
          </button>

          <div className="timezone-list">
            {searchQuery === '' && (
              <>
                <div className="timezone-section-title">Россия</div>
                {popularTimezones.map(tz => (
                  <div
                    key={tz.value}
                    className={`timezone-item ${selectedTimezone === tz.value ? 'selected' : ''}`}
                    onClick={() => {
                      setSelectedTimezone(tz.value)
                      handleSave(tz.value)
                    }}
                  >
                    <span className="timezone-item-label">{tz.label}</span>
                    {selectedTimezone === tz.value && (
                      <span className="timezone-check">✓</span>
                    )}
                  </div>
                ))}
                <div className="timezone-section-title">Все часовые пояса</div>
              </>
            )}
            
            {filteredTimezones
              .filter(tz => searchQuery !== '' || !popularTimezones.find(p => p.value === tz.value))
              .map(tz => (
                <div
                  key={tz.value}
                  className={`timezone-item ${selectedTimezone === tz.value ? 'selected' : ''}`}
                  onClick={() => {
                    setSelectedTimezone(tz.value)
                    handleSave(tz.value)
                  }}
                >
                  <span className="timezone-item-label">{tz.label}</span>
                  {selectedTimezone === tz.value && (
                    <span className="timezone-check">✓</span>
                  )}
                </div>
              ))}
            
            {filteredTimezones.length === 0 && (
              <div className="timezone-no-results">
                Ничего не найдено
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  )
}

export default TimezoneSelector
