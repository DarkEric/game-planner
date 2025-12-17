import { useState } from 'react'
import { passwordResetApi } from '../services/api'
import './Login.css'

const PasswordResetConfirm = ({ onBackToLogin, onPasswordReset }) => {
  const [token, setToken] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')

    if (newPassword !== confirmPassword) {
      setError('Пароли не совпадают')
      return
    }

    if (newPassword.length < 6) {
      setError('Пароль должен содержать минимум 6 символов')
      return
    }

    setLoading(true)

    try {
      await passwordResetApi.confirmPasswordReset(token, newPassword)
      if (onPasswordReset) {
        onPasswordReset()
      }
    } catch (err) {
      setError(err.message || 'Ошибка при сбросе пароля')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-container">
      <div className="auth-card">
        <h2>Сброс пароля</h2>
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="reset-token">Код для сброса</label>
            <input
              id="reset-token"
              type="text"
              value={token}
              onChange={(e) => setToken(e.target.value)}
              required
              disabled={loading}
              placeholder="Введите код из Telegram"
            />
          </div>
          <div className="form-group">
            <label htmlFor="new-password">Новый пароль</label>
            <input
              id="new-password"
              type="password"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              required
              minLength={6}
              disabled={loading}
              placeholder="Минимум 6 символов"
            />
          </div>
          <div className="form-group">
            <label htmlFor="confirm-password">Подтвердите пароль</label>
            <input
              id="confirm-password"
              type="password"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              required
              minLength={6}
              disabled={loading}
              placeholder="Повторите новый пароль"
            />
          </div>
          {error && <div className="error-message">{error}</div>}
          <button type="submit" disabled={loading} className="auth-button">
            {loading ? 'Сброс пароля...' : 'Сбросить пароль'}
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

export default PasswordResetConfirm
