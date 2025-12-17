import { useState } from 'react'
import { passwordResetApi } from '../services/api'
import './Login.css'

const PasswordResetRequest = ({ onBackToLogin, onProceedToConfirm }) => {
  const [username, setUsername] = useState('')
  const [error, setError] = useState('')
  const [success, setSuccess] = useState(false)
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setSuccess(false)
    setLoading(true)

    try {
      await passwordResetApi.requestPasswordReset(username)
      setSuccess(true)
    } catch (err) {
      setError(err.message || 'Ошибка при запросе сброса пароля')
    } finally {
      setLoading(false)
    }
  }

  if (success) {
    return (
      <div className="auth-container">
        <div className="auth-card">
          <h2>Запрос отправлен</h2>
          <div style={{ color: '#aaa', marginBottom: '1.5rem', lineHeight: '1.6' }}>
            <p>Если ваш аккаунт связан с Telegram ботом, код для сброса пароля будет отправлен в Telegram.</p>
            <p style={{ marginTop: '1rem' }}>Проверьте сообщения от бота и используйте полученный код для сброса пароля.</p>
          </div>
          {onProceedToConfirm && (
            <button
              type="button"
              onClick={onProceedToConfirm}
              className="auth-button"
              style={{ width: '100%', marginBottom: '0.5rem' }}
            >
              У меня есть код, сбросить пароль
            </button>
          )}
          <button
            type="button"
            onClick={onBackToLogin}
            className="link-button"
            style={{ width: '100%', textAlign: 'center', display: 'block' }}
          >
            Вернуться к входу
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className="auth-container">
      <div className="auth-card">
        <h2>Восстановление пароля</h2>
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="reset-username">Имя пользователя</label>
            <input
              id="reset-username"
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              required
              disabled={loading}
              placeholder="Введите ваше имя пользователя"
            />
          </div>
          {error && <div className="error-message">{error}</div>}
          <div style={{ color: '#aaa', fontSize: '0.85rem', marginBottom: '1rem', lineHeight: '1.5' }}>
            Если ваш аккаунт связан с Telegram ботом, код для сброса будет отправлен в Telegram.
          </div>
          <button type="submit" disabled={loading} className="auth-button">
            {loading ? 'Отправка...' : 'Запросить сброс пароля'}
          </button>
        </form>
        <p className="auth-switch">
          <button type="button" onClick={onBackToLogin} className="link-button">
            Вернуться к входу
          </button>
        </p>
      </div>
    </div>
  )
}

export default PasswordResetRequest
