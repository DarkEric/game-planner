import { useState } from 'react'
import { adminApi } from '../services/api'
import './AdminPanel.css'

const DeleteUserModal = ({ user, onClose, onConfirm, loading: externalLoading }) => {
  const [password, setPassword] = useState('')
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(false)

  const handleConfirm = async () => {
    if (!user) return
    
    if (!password.trim()) {
      setError('Введите пароль для подтверждения')
      return
    }
    
    setLoading(true)
    setError(null)

    try {
      await adminApi.deleteUser(user.id, password)
      if (onConfirm) {
        onConfirm()
      }
      onClose()
    } catch (err) {
      setError(err.message || 'Произошла ошибка при удалении пользователя')
    } finally {
      setLoading(false)
    }
  }

  const handleClose = () => {
    setPassword('')
    setError(null)
    onClose()
  }

  return (
    <div className="admin-modal-overlay" onClick={handleClose}>
      <div className="admin-modal" onClick={(e) => e.stopPropagation()}>
        <h3>Удаление пользователя</h3>
        <div className="admin-modal-message">
          <p style={{ color: '#ff6b6b', fontWeight: 'bold', marginBottom: '1rem' }}>
            ⚠️ ВНИМАНИЕ: Это действие необратимо!
          </p>
          <p>
            Вы собираетесь удалить пользователя <strong>{user?.username}</strong>.
          </p>
          <p style={{ marginTop: '0.5rem' }}>
            Все данные пользователя будут удалены, а игры и кампании будут переданы системному пользователю.
          </p>
        </div>
        <div style={{ marginTop: '1rem' }}>
          <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 'bold' }}>
            Введите ваш пароль для подтверждения:
          </label>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="Пароль администратора"
            style={{
              width: '100%',
              padding: '0.75rem',
              borderRadius: '6px',
              border: '1px solid #555',
              background: '#2a2a2a',
              color: '#fff',
              fontSize: '1rem',
              boxSizing: 'border-box'
            }}
            onKeyPress={(e) => {
              if (e.key === 'Enter' && !loading && !externalLoading) {
                handleConfirm()
              }
            }}
            autoFocus
          />
        </div>
        {error && (
          <div className="admin-error" style={{ marginTop: '1rem' }}>
            {error}
          </div>
        )}
        <div className="admin-modal-actions" style={{ marginTop: '1.5rem' }}>
          <button
            className="admin-button-cancel"
            onClick={handleClose}
            disabled={loading || externalLoading}
          >
            Отмена
          </button>
          <button
            className="admin-button-confirm"
            onClick={handleConfirm}
            disabled={loading || externalLoading || !password.trim()}
            style={{
              backgroundColor: '#ff6b6b',
              borderColor: '#ff6b6b'
            }}
          >
            {loading || externalLoading ? 'Удаление...' : 'Удалить пользователя'}
          </button>
        </div>
      </div>
    </div>
  )
}

export default DeleteUserModal
