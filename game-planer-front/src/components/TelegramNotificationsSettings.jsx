import { useState, useEffect } from 'react'
import { notificationApi } from '../services/notificationApi'
import './TelegramNotificationsSettings.css'

const TelegramNotificationsSettings = () => {
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
      setSettings(data)
      // TODO: Get telegram subscription status from user profile
      // For now, we'll assume it's false if settings exist
      setTelegramSubscribed(false)
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
      
      await notificationApi.updateNotificationSettings(settings)
      setError(null)
      alert('Настройки сохранены')
    } catch (err) {
      setError('Не удалось сохранить настройки')
      console.error(err)
    } finally {
      setSaving(false)
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

  const addReminder = () => {
    const reminders = [...(settings.upcomingGameReminders || [])]
    if (reminders.length >= 5) {
      setError('Максимум 5 напоминаний')
      return
    }
    reminders.push({ minutesBefore: 60, enabled: true })
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
            {(settings.upcomingGameReminders || []).map((reminder, index) => (
              <div key={index} className="reminder-item">
                <input
                  type="number"
                  min="1"
                  placeholder="Минут до начала"
                  value={reminder.minutesBefore || ''}
                  onChange={(e) => handleReminderChange(index, 'minutesBefore', parseInt(e.target.value) || 0)}
                  className="reminder-input"
                />
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
            ))}
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
              value={settings.timeSlotReminderDateTime ? new Date(settings.timeSlotReminderDateTime).toISOString().slice(0, 16) : ''}
              onChange={(e) => setSettings({ ...settings, timeSlotReminderDateTime: e.target.value ? new Date(e.target.value).toISOString() : null })}
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
