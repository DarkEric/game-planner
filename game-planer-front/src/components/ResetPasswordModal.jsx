import { useState, useEffect } from 'react'
import { adminApi } from '../services/api'
import './AdminPanel.css'

const ResetPasswordModal = ({ user, onClose, onConfirm, loading: externalLoading }) => {
  const [result, setResult] = useState(null)
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    // Сбрасываем состояние при открытии модального окна
    setResult(null)
    setError(null)
  }, [user])

  const handleConfirm = async () => {
    if (!user) return
    
    setLoading(true)
    setError(null)

    try {
      const response = await adminApi.resetUserPassword(user.id)
      setResult(response)
      if (onConfirm) {
        onConfirm()
      }
    } catch (err) {
      setError(err.message || 'Произошла ошибка при сбросе пароля')
    } finally {
      setLoading(false)
    }
  }

  if (result) {
    return (
      <div className="admin-modal-overlay" onClick={onClose}>
        <div className="admin-modal" onClick={(e) => e.stopPropagation()}>
          <h3>Результат сброса пароля</h3>
          {result.sentViaTelegram ? (
            <div className="admin-success-message">
              Новый пароль отправлен пользователю в Telegram
            </div>
          ) : (
            <div>
              <div className="admin-modal-message">
                Новый пароль для пользователя <strong>{user.username}</strong>:
              </div>
              <div className="admin-password-display">
                <span>{result.temporaryPassword}</span>
                <button
                  className="admin-copy-button"
                  onClick={() => {
                    navigator.clipboard.writeText(result.temporaryPassword)
                    alert('Пароль скопирован в буфер обмена')
                  }}
                >
                  Копировать
                </button>
              </div>
              <div className="admin-modal-message" style={{ fontSize: '0.85rem', color: '#ff6b6b' }}>
                ⚠️ Сохраните этот пароль! Он больше не будет показан.
              </div>
            </div>
          )}
          <div className="admin-modal-actions">
            <button
              className="admin-button-confirm"
              onClick={onClose}
            >
              Закрыть
            </button>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="admin-modal-overlay" onClick={onClose}>
      <div className="admin-modal" onClick={(e) => e.stopPropagation()}>
        <h3>Сброс пароля</h3>
        <div className="admin-modal-message">
          Вы уверены, что хотите сбросить пароль для пользователя <strong>{user.username}</strong>?
        </div>
        {error && (
          <div className="admin-error">
            {error}
          </div>
        )}
        <div className="admin-modal-actions">
          <button
            className="admin-button-cancel"
            onClick={onClose}
            disabled={loading}
          >
            Отмена
          </button>
          <button
            className="admin-button-confirm"
            onClick={handleConfirm}
            disabled={loading || externalLoading}
          >
            {loading || externalLoading ? 'Сброс...' : 'Сбросить пароль'}
          </button>
        </div>
      </div>
    </div>
  )
}

export default ResetPasswordModal
