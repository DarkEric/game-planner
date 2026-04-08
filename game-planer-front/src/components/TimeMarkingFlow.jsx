import { useState, useMemo, useCallback } from 'react'
import { useLanguage } from '../i18n/LanguageContext'
import { playerApi } from '../services/api'
import './TimeMarkingFlow.css'

const DAY_MS = 24 * 60 * 60 * 1000
const MARKING_DAYS = 28
const DURATION_HOUR_OPTIONS = Array.from({ length: 24 }, (_, i) => i + 1)

function pad(n) {
  return String(n).padStart(2, '0')
}

function dateKeyFromDate(d) {
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

function parseDateKey(key) {
  const [y, m, d] = key.split('-').map(Number)
  return { year: y, month: m, day: d }
}

function addDays(base, n) {
  const x = new Date(base)
  x.setDate(x.getDate() + n)
  x.setHours(0, 0, 0, 0)
  return x
}

const defaultDayConfig = () => ({
  wholeDay: false,
  hour: 19,
  minute: 0,
  duration: 4
})

const TimeMarkingFlow = ({ onClose, onSaved }) => {
  const { t, language } = useLanguage()
  const [step, setStep] = useState(1)
  const [selectedKeys, setSelectedKeys] = useState(() => new Set())
  const [dayConfigs, setDayConfigs] = useState({})
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState(null)

  const dayOptions = useMemo(() => {
    const today = new Date()
    today.setHours(0, 0, 0, 0)
    const out = []
    for (let i = 0; i < MARKING_DAYS; i++) {
      const d = addDays(today, i)
      out.push({ key: dateKeyFromDate(d), date: d })
    }
    return out
  }, [])

  const locale = language === 'en' ? 'en-US' : 'ru-RU'

  const toggleDay = useCallback((key) => {
    setSelectedKeys(prev => {
      const next = new Set(prev)
      if (next.has(key)) {
        next.delete(key)
      } else {
        next.add(key)
        setDayConfigs(dc => ({
          ...dc,
          [key]: dc[key] || defaultDayConfig()
        }))
      }
      return next
    })
  }, [])

  const setWholeDayAll = useCallback(() => {
    setDayConfigs(dc => {
      const next = { ...dc }
      selectedKeys.forEach(k => {
        next[k] = { ...defaultDayConfig(), ...(next[k] || {}), wholeDay: true }
      })
      return next
    })
  }, [selectedKeys])

  const updateConfig = (key, patch) => {
    setDayConfigs(dc => ({
      ...dc,
      [key]: { ...defaultDayConfig(), ...dc[key], ...patch }
    }))
  }

  const handleSubmit = async () => {
    if (selectedKeys.size === 0) {
      setError(t('timeMarkingSelectOneDay'))
      return
    }
    setError(null)
    setSaving(true)
    try {
      const slots = []
      for (const key of selectedKeys) {
        const { year, month, day } = parseDateKey(key)
        const cfg = dayConfigs[key] || defaultDayConfig()
        if (cfg.wholeDay) {
          slots.push({ year, month, day, wholeDay: true })
        } else {
          slots.push({
            year,
            month,
            day,
            hour: cfg.hour,
            minute: cfg.minute,
            duration: cfg.duration
          })
        }
      }
      const updated = await playerApi.toggleTimeSlotsMoscowBatch(slots)
      onSaved(updated)
      onClose()
    } catch (e) {
      console.error(e)
      setError(t('timeMarkingSaveError'))
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="time-marking-overlay" onClick={onClose}>
      <div className="time-marking-modal" onClick={e => e.stopPropagation()}>
        <div className="time-marking-header">
          <h2>{t('timeMarkingTitle')}</h2>
          <button type="button" className="time-marking-close" onClick={onClose} aria-label={t('close')}>×</button>
        </div>

        <p className="time-marking-hint">{t('timeMarkingToggleHint')}</p>

        {step === 1 && (
          <div className="time-marking-step">
            <h3 className="time-marking-subtitle">{t('timeMarkingStep1')}</h3>
            <div className="time-marking-day-grid">
              {dayOptions.map(({ key, date }) => (
                <label
                  key={key}
                  className={`time-marking-day-chip${selectedKeys.has(key) ? ' selected' : ''}`}
                  title={date.toLocaleDateString(locale, { dateStyle: 'medium' })}
                >
                  <input
                    type="checkbox"
                    checked={selectedKeys.has(key)}
                    onChange={() => toggleDay(key)}
                  />
                  <span className="time-marking-day-chip-inner">
                    <span className="time-marking-day-num">{date.getDate()}</span>
                    <span className="time-marking-day-meta">
                      {date.toLocaleDateString(locale, { weekday: 'short' })}
                    </span>
                  </span>
                </label>
              ))}
            </div>
            <div className="time-marking-actions">
              <button
                type="button"
                className="time-marking-btn primary"
                disabled={selectedKeys.size === 0}
                onClick={() => setStep(2)}
              >
                {t('timeMarkingNext')}
              </button>
            </div>
          </div>
        )}

        {step === 2 && (
          <div className="time-marking-step">
            <h3 className="time-marking-subtitle">{t('timeMarkingStep2')}</h3>
            <p className="time-marking-moscow-note">{t('timeMarkingMoscowNote')}</p>
            <div className="time-marking-bulk">
              <button type="button" className="time-marking-btn secondary" onClick={setWholeDayAll}>
                {t('timeMarkingWholeDayAll')}
              </button>
            </div>
            <div className="time-marking-day-rows">
              {[...selectedKeys].sort().map(key => {
                const cfg = dayConfigs[key] || defaultDayConfig()
                const { date } = dayOptions.find(o => o.key === key) || {}
                const label = date
                  ? date.toLocaleDateString(locale, { weekday: 'short', day: 'numeric', month: 'short' })
                  : key
                return (
                  <div key={key} className="time-marking-day-row">
                    <div className="time-marking-day-row-title">{label}</div>
                    <label className="time-marking-whole-toggle">
                      <input
                        type="checkbox"
                        checked={cfg.wholeDay}
                        onChange={e => updateConfig(key, { wholeDay: e.target.checked })}
                      />
                      {t('timeMarkingAnyTime')}
                    </label>
                    {!cfg.wholeDay && (
                      <div className="time-marking-inputs">
                        <label>
                          {t('timeMarkingStart')}
                          <input
                            type="time"
                            value={`${pad(cfg.hour)}:${pad(cfg.minute)}`}
                            onChange={e => {
                              const [h, m] = e.target.value.split(':').map(Number)
                              updateConfig(key, { hour: h || 0, minute: m || 0 })
                            }}
                          />
                        </label>
                        <label className="time-marking-duration-label">
                          {t('timeMarkingDurationHours')}
                          <select
                            className="time-marking-duration-select"
                            value={cfg.duration}
                            onChange={e =>
                              updateConfig(key, {
                                duration: Math.min(24, Math.max(1, parseInt(e.target.value, 10) || 4))
                              })
                            }
                            aria-label={t('timeMarkingDurationHours')}
                          >
                            {DURATION_HOUR_OPTIONS.map(h => (
                              <option key={h} value={h}>
                                {h}
                              </option>
                            ))}
                          </select>
                        </label>
                      </div>
                    )}
                  </div>
                )
              })}
            </div>
            <div className="time-marking-actions">
              <button type="button" className="time-marking-btn secondary" onClick={() => setStep(1)}>
                {t('timeMarkingBack')}
              </button>
              <button
                type="button"
                className="time-marking-btn primary"
                disabled={saving}
                onClick={handleSubmit}
              >
                {saving ? '…' : t('timeMarkingSave')}
              </button>
            </div>
          </div>
        )}

        {error && <div className="time-marking-error" role="alert">{error}</div>}
      </div>
    </div>
  )
}

export default TimeMarkingFlow
