import { useState } from 'react'
import './Login.css'

const Login = ({ onLogin, onSwitchToRegister, onForgotPassword }) => {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)

    try {
      await onLogin(username, password)
    } catch (err) {
      setError('Неверное имя пользователя или пароль')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-container">
      <div className="auth-card">
        <h2>Вход</h2>
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="username">Имя пользователя</label>
            <input
              id="username"
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              required
              disabled={loading}
            />
          </div>
          <div className="form-group">
            <label htmlFor="password">Пароль</label>
            <input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              disabled={loading}
            />
          </div>
          {error && <div className="error-message">{error}</div>}
          <button type="submit" disabled={loading} className="auth-button">
            {loading ? 'Вход...' : 'Войти'}
          </button>
        </form>
        {onForgotPassword && (
          <p style={{ textAlign: 'center', marginTop: '0.5rem', marginBottom: '0' }}>
            <button
              type="button"
              onClick={onForgotPassword}
              className="link-button"
              style={{ fontSize: '0.85rem' }}
            >
              Забыли пароль?
            </button>
          </p>
        )}
        <p className="auth-switch">
          Нет аккаунта?{' '}
          <button type="button" onClick={onSwitchToRegister} className="link-button">
            Зарегистрироваться
          </button>
        </p>
      </div>
    </div>
  )
}

export default Login

