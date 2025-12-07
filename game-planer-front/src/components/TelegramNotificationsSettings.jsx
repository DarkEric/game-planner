import { useState, useEffect } from 'react'
import { notificationApi } from '../services/notificationApi'
import './TelegramNotificationsSettings.css'

/**
 * Конвертирует UTC дату (ISO строка от сервера) в локальное время пользователя
 * для отображения в datetime-local input
 */
const convertUTCToLocalDateTimeString = (utcDateString, userTimezone) => {
  if (!utcDateString) return ''
  
  try {
    const utcDate = new Date(utcDateString)
    if (isNaN(utcDate.getTime())) return ''
    
    // Форматируем UTC дату в timezone пользователя
    // Используем формат 'sv-SE' который дает YYYY-MM-DD HH:mm
    const formatter = new Intl.DateTimeFormat('sv-SE', {
      timeZone: userTimezone,
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    })
    
    // Формат 'sv-SE' дает YYYY-MM-DD HH:mm, заменяем пробел на T
    const result = formatter.format(utcDate).replace(' ', 'T')
    
    // Отладочный вывод
    console.log('convertUTCToLocalDateTimeString:', {
      input: utcDateString,
      userTimezone,
      output: result,
      utcDate: utcDate.toISOString()
    })
    
    return result
  } catch (error) {
    console.error('Error converting UTC to local datetime:', error)
    return ''
  }
}

/**
 * Конвертирует локальное время пользователя (из datetime-local input) в UTC
 * для отправки на сервер
 * 
 * datetime-local возвращает строку YYYY-MM-DDTHH:mm, которую браузер интерпретирует
 * как время в своем timezone. Но мы хотим интерпретировать её как время в userTimezone.
 */
const convertLocalDateTimeStringToUTC = (localDateTimeString, userTimezone) => {
  if (!localDateTimeString) return null
  
  try {
    const browserTz = Intl.DateTimeFormat().resolvedOptions().timeZone
    
    // Если timezone пользователя совпадает с timezone браузера, просто конвертируем
    if (userTimezone === browserTz) {
      const date = new Date(localDateTimeString)
      return date.toISOString()
    }
    
    // Парсим компоненты из строки
    const [datePart, timePart] = localDateTimeString.split('T')
    const [year, month, day] = datePart.split('-').map(Number)
    const [hours, minutes] = timePart.split(':').map(Number)
    
    // Создаем дату, как будто эти компоненты относятся к userTimezone
    // Для этого создаем временную дату и вычисляем offset
    
    // Создаем временную дату в UTC с этими компонентами
    const tempUTC = new Date(Date.UTC(year, month - 1, day, hours, minutes))
    
    // Вычисляем, какое время в userTimezone соответствует tempUTC
    const userFormatter = new Intl.DateTimeFormat('en-US', {
      timeZone: userTimezone,
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: false
    })
    
    const userParts = userFormatter.formatToParts(tempUTC)
    const userComponents = {}
    userParts.forEach(part => {
      if (part.type !== 'literal') {
        userComponents[part.type] = parseInt(part.value)
      }
    })
    
    // Создаем дату в UTC с компонентами из userTimezone
    const userTzDate = new Date(Date.UTC(
      userComponents.year,
      userComponents.month - 1,
      userComponents.day,
      userComponents.hour,
      userComponents.minute,
      userComponents.second
    ))
    
    // Вычисляем offset: разница между tempUTC и userTzDate
    const offset = userTzDate.getTime() - tempUTC.getTime()
    
    // Если компоненты (year, month, day, hours, minutes) интерпретируются
    // как время в userTimezone, то UTC = tempUTC - offset
    const resultUTC = new Date(tempUTC.getTime() - offset)
    
    const result = resultUTC.toISOString()
    
    // Отладочный вывод
    console.log('convertLocalDateTimeStringToUTC:', {
      input: localDateTimeString,
      userTimezone,
      browserTimezone: browserTz,
      tempUTC: tempUTC.toISOString(),
      userTzDate: userTzDate.toISOString(),
      offset: offset / (1000 * 60), // в минутах
      result
    })
    
    return result
  } catch (error) {
    console.error('Error converting local datetime to UTC:', error)
    return null
  }
}

const TelegramNotificationsSettings = ({ userTimezone }) => {
  const [settings, setSettings] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [linkToken, setLinkToken] = useState(null)
  const [telegramSubscribed, setTelegramSubscribed] = useState(false)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    loadSettings()
  }, [])

  const loadSettings = async () => {
    try {
      setLoading(true)
      setError(null)
      const data = await notificationApi.getNotificationSettings()
      
      // Преобразуем минуты в удобные единицы для отображения
      if (data.upcomingGameReminders) {
        data.upcomingGameReminders = data.upcomingGameReminders.map(reminder => {
          const display = convertMinutesToDisplay(reminder.minutesBefore || 0)
          return {
            ...reminder,
            displayValue: display.value,
            displayUnit: display.unit
          }
        })
      }
      
      setSettings(data)
      // Получаем статус подписки из настроек
      setTelegramSubscribed(data.telegramSubscribed || false)
    } catch (err) {
      setError('Не удалось загрузить настройки')
      console.error(err)
    } finally {
      setLoading(false)
    }
  }

  const handleGetLinkToken = async () => {
    try {
      setError(null)
      const token = await notificationApi.getTelegramLinkToken()
      setLinkToken(token)
    } catch (err) {
      setError('Не удалось получить токен')
      console.error(err)
    }
  }

  const handleUnlink = async () => {
    if (!confirm('Отвязать Telegram аккаунт?')) return
    
    try {
      setError(null)
      await notificationApi.unlinkTelegramAccount()
      setTelegramSubscribed(false)
      setLinkToken(null)
      // Перезагружаем настройки, чтобы получить обновленный статус
      await loadSettings()
    } catch (err) {
      setError('Не удалось отвязать аккаунт')
      console.error(err)
    }
  }

  const handleSave = async () => {
    if (!settings) return
    
    try {
      setSaving(true)
      setError(null)
      
      // Валидация: до 5 напоминаний
      if (settings.upcomingGameReminders && settings.upcomingGameReminders.length > 5) {
        setError('Максимум 5 напоминаний о предстоящих играх')
        setSaving(false)
        return
      }
      
      // Подготавливаем данные для отправки: убираем display-поля, оставляем только minutesBefore
      const settingsToSave = {
        ...settings,
        upcomingGameReminders: settings.upcomingGameReminders?.map(reminder => ({
          minutesBefore: reminder.minutesBefore,
          enabled: reminder.enabled
        })) || []
      }
      
      await notificationApi.updateNotificationSettings(settingsToSave)
      setError(null)
      alert('Настройки сохранены')
    } catch (err) {
      setError('Не удалось сохранить настройки')
      console.error(err)
    } finally {
      setSaving(false)
    }
  }

  // Преобразование минут в удобную единицу для отображения
  const convertMinutesToDisplay = (minutes) => {
    if (minutes % (24 * 60) === 0 && minutes >= 24 * 60) {
      return { value: minutes / (24 * 60), unit: 'days' }
    } else if (minutes % 60 === 0 && minutes >= 60) {
      return { value: minutes / 60, unit: 'hours' }
    } else {
      return { value: minutes, unit: 'minutes' }
    }
  }

  // Преобразование значения в минуты для отправки на API
  const convertToMinutes = (value, unit) => {
    switch (unit) {
      case 'days':
        return value * 24 * 60
      case 'hours':
        return value * 60
      case 'minutes':
      default:
        return value
    }
  }

  const handleReminderChange = (index, field, value) => {
    const reminders = [...(settings.upcomingGameReminders || [])]
    if (!reminders[index]) {
      reminders[index] = { minutesBefore: 0, enabled: false }
    }
    reminders[index][field] = value
    setSettings({ ...settings, upcomingGameReminders: reminders })
  }

  const handleReminderValueChange = (index, value, unit) => {
    const reminders = [...(settings.upcomingGameReminders || [])]
    if (!reminders[index]) {
      reminders[index] = { minutesBefore: 0, enabled: false }
    }
    // Сохраняем в минутах для API
    reminders[index].minutesBefore = convertToMinutes(value || 0, unit)
    // Сохраняем единицу для отображения
    reminders[index].displayUnit = unit
    reminders[index].displayValue = value || 0
    setSettings({ ...settings, upcomingGameReminders: reminders })
  }

  const handleReminderUnitChange = (index, unit) => {
    const reminders = [...(settings.upcomingGameReminders || [])]
    if (!reminders[index]) {
      reminders[index] = { minutesBefore: 0, enabled: false }
    }
    // Получаем текущее значение в минутах
    const currentMinutes = reminders[index].minutesBefore || 0
    // Преобразуем минуты в новую единицу
    let newValue
    switch (unit) {
      case 'days':
        newValue = Math.round((currentMinutes / (24 * 60)) * 100) / 100 // Округляем до 2 знаков
        break
      case 'hours':
        newValue = Math.round((currentMinutes / 60) * 100) / 100 // Округляем до 2 знаков
        break
      case 'minutes':
      default:
        newValue = currentMinutes
        break
    }
    // Если значение получилось 0, устанавливаем 1
    if (newValue === 0) {
      newValue = 1
    }
    // Обновляем minutesBefore на основе нового значения
    reminders[index].minutesBefore = convertToMinutes(newValue, unit)
    reminders[index].displayUnit = unit
    reminders[index].displayValue = newValue
    setSettings({ ...settings, upcomingGameReminders: reminders })
  }

  const addReminder = () => {
    const reminders = [...(settings.upcomingGameReminders || [])]
    if (reminders.length >= 5) {
      setError('Максимум 5 напоминаний')
      return
    }
    reminders.push({ 
      minutesBefore: 60, 
      enabled: true,
      displayValue: 1,
      displayUnit: 'hours'
    })
    setSettings({ ...settings, upcomingGameReminders: reminders })
  }

  const removeReminder = (index) => {
    const reminders = [...(settings.upcomingGameReminders || [])]
    reminders.splice(index, 1)
    setSettings({ ...settings, upcomingGameReminders: reminders })
  }

  const copyToken = () => {
    if (linkToken) {
      navigator.clipboard.writeText(linkToken)
      alert('Токен скопирован в буфер обмена')
    }
  }

  if (loading) {
    return <div className="telegram-settings-loading">Загрузка настроек...</div>
  }

  if (!settings) {
    return <div className="telegram-settings-error">Ошибка загрузки настроек</div>
  }

  return (
    <div className="telegram-notifications-settings">
      {error && <div className="telegram-settings-error">{error}</div>}

      {/* Статус подписки */}
      <div className="telegram-subscription-status">
        <div className="status-header">
          <span className="status-label">Статус подписки:</span>
          <span className={`status-value ${telegramSubscribed ? 'subscribed' : 'not-subscribed'}`}>
            {telegramSubscribed ? '✓ Подписан' : '✗ Не подписан'}
          </span>
        </div>
        
        {!telegramSubscribed && (
          <div className="link-section">
            <button onClick={handleGetLinkToken} className="btn-get-token">
              Получить токен для связывания
            </button>
            {linkToken && (
              <div className="token-display">
                <p>Отправьте боту команду:</p>
                <code className="token-code">/link {linkToken}</code>
                <button onClick={copyToken} className="btn-copy-token">📋 Копировать</button>
              </div>
            )}
          </div>
        )}
        
        {telegramSubscribed && (
          <button onClick={handleUnlink} className="btn-unlink">
            Отвязать Telegram аккаунт
          </button>
        )}
      </div>

      {/* Настройки уведомлений */}
      <div className="notification-settings-grid">
        {/* Игра создана */}
        <div className="setting-group">
          <label className="setting-label">Игра создана:</label>
          <div className="radio-group">
            <label>
              <input
                type="radio"
                value="ALL"
                checked={settings.gameCreated === 'ALL'}
                onChange={(e) => setSettings({ ...settings, gameCreated: e.target.value })}
              />
              Все игры
            </label>
            <label>
              <input
                type="radio"
                value="MY_GAMES"
                checked={settings.gameCreated === 'MY_GAMES'}
                onChange={(e) => setSettings({ ...settings, gameCreated: e.target.value })}
              />
              Только мои игры
            </label>
            <label>
              <input
                type="radio"
                value="NONE"
                checked={settings.gameCreated === 'NONE'}
                onChange={(e) => setSettings({ ...settings, gameCreated: e.target.value })}
              />
              Не получать
            </label>
          </div>
        </div>

        {/* Игра отменена */}
        <div className="setting-group">
          <label className="setting-label">Игра отменена:</label>
          <div className="radio-group">
            <label>
              <input
                type="radio"
                value="ALL"
                checked={settings.gameCancelled === 'ALL'}
                onChange={(e) => setSettings({ ...settings, gameCancelled: e.target.value })}
              />
              Все игры
            </label>
            <label>
              <input
                type="radio"
                value="MY_GAMES"
                checked={settings.gameCancelled === 'MY_GAMES'}
                onChange={(e) => setSettings({ ...settings, gameCancelled: e.target.value })}
              />
              Только мои игры
            </label>
            <label>
              <input
                type="radio"
                value="NONE"
                checked={settings.gameCancelled === 'NONE'}
                onChange={(e) => setSettings({ ...settings, gameCancelled: e.target.value })}
              />
              Не получать
            </label>
          </div>
        </div>

        {/* Игра проведена */}
        <div className="setting-group">
          <label className="setting-label">Игра проведена:</label>
          <div className="radio-group">
            <label>
              <input
                type="radio"
                value="ALL"
                checked={settings.gameHeld === 'ALL'}
                onChange={(e) => setSettings({ ...settings, gameHeld: e.target.value })}
              />
              Все игры
            </label>
            <label>
              <input
                type="radio"
                value="MY_GAMES"
                checked={settings.gameHeld === 'MY_GAMES'}
                onChange={(e) => setSettings({ ...settings, gameHeld: e.target.value })}
              />
              Только мои игры
            </label>
            <label>
              <input
                type="radio"
                value="NONE"
                checked={settings.gameHeld === 'NONE'}
                onChange={(e) => setSettings({ ...settings, gameHeld: e.target.value })}
              />
              Не получать
            </label>
          </div>
        </div>

        {/* Добавили на игру */}
        <div className="setting-group">
          <label className="setting-label">
            <input
              type="checkbox"
              checked={settings.gameAddedToGame || false}
              onChange={(e) => setSettings({ ...settings, gameAddedToGame: e.target.checked })}
            />
            Добавили на игру
          </label>
        </div>

        {/* Напоминания о предстоящих играх */}
        <div className="setting-group reminders-group">
          <label className="setting-label">Напоминания о предстоящих играх:</label>
          <div className="reminders-list">
            {(settings.upcomingGameReminders || []).map((reminder, index) => {
              const displayValue = reminder.displayValue !== undefined 
                ? reminder.displayValue 
                : convertMinutesToDisplay(reminder.minutesBefore || 0).value
              const displayUnit = reminder.displayUnit || convertMinutesToDisplay(reminder.minutesBefore || 0).unit
              
              return (
                <div key={index} className="reminder-item">
                  <div className="reminder-input-group">
                    <input
                      type="number"
                      min="1"
                      placeholder="Значение"
                      value={displayValue || ''}
                      onChange={(e) => handleReminderValueChange(index, parseInt(e.target.value) || 0, displayUnit)}
                      className="reminder-input"
                    />
                    <select
                      value={displayUnit}
                      onChange={(e) => handleReminderUnitChange(index, e.target.value)}
                      className="reminder-unit-select"
                    >
                      <option value="minutes">минут</option>
                      <option value="hours">часов</option>
                      <option value="days">дней</option>
                    </select>
                  </div>
                  <label className="reminder-toggle">
                    <input
                      type="checkbox"
                      checked={reminder.enabled || false}
                      onChange={(e) => handleReminderChange(index, 'enabled', e.target.checked)}
                    />
                    Включено
                  </label>
                  <button onClick={() => removeReminder(index)} className="btn-remove-reminder">
                    ✕
                  </button>
                </div>
              )
            })}
            {(settings.upcomingGameReminders || []).length < 5 && (
              <button onClick={addReminder} className="btn-add-reminder">
                + Добавить напоминание
              </button>
            )}
          </div>
        </div>

        {/* Напоминание разметить время */}
        <div className="setting-group">
          <label className="setting-label">
            <input
              type="checkbox"
              checked={settings.timeSlotReminderEnabled || false}
              onChange={(e) => setSettings({ ...settings, timeSlotReminderEnabled: e.target.checked })}
            />
            Напоминание разметить время
          </label>
          {settings.timeSlotReminderEnabled && (
            <input
              type="datetime-local"
              value={settings.timeSlotReminderDateTime 
                ? convertUTCToLocalDateTimeString(settings.timeSlotReminderDateTime, userTimezone || Intl.DateTimeFormat().resolvedOptions().timeZone)
                : ''}
              onChange={(e) => {
                const effectiveTimezone = userTimezone || Intl.DateTimeFormat().resolvedOptions().timeZone
                const utcString = convertLocalDateTimeStringToUTC(e.target.value, effectiveTimezone)
                
                // Отладочный вывод
                console.log('DateTime change:', {
                  input: e.target.value,
                  userTimezone: effectiveTimezone,
                  browserTimezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
                  utcResult: utcString
                })
                
                setSettings({ ...settings, timeSlotReminderDateTime: utcString })
              }}
              className="datetime-input"
            />
          )}
        </div>

        {/* Напоминание завершить игру */}
        <div className="setting-group">
          <label className="setting-label">
            <input
              type="checkbox"
              checked={settings.gameCompletionReminderEnabled || false}
              onChange={(e) => setSettings({ ...settings, gameCompletionReminderEnabled: e.target.checked })}
            />
            Напоминание завершить игру (для создателя)
          </label>
        </div>
      </div>

      <div className="settings-actions">
        <button onClick={handleSave} disabled={saving} className="btn-save">
          {saving ? 'Сохранение...' : 'Сохранить настройки'}
        </button>
      </div>
    </div>
  )
}

export default TelegramNotificationsSettings
