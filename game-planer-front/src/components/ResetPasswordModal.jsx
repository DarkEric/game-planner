import { useState, useEffect } from 'react'
import { adminApi } from '../services/api'
import './AdminPanel.css'

const ResetPasswordModal = ({ user, onClose, onConfirm, loading: externalLoading }) => {
  const [result, setResult] = useState(null)
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    // Сбрасываем состояние при открытии модального окна для нового пользователя
    if (user) {
      // Сбрасываем только если это другой пользователь или модальное окно только что открылось
      setResult(null)
      setError(null)
    }
  }, [user?.id])

  const handleConfirm = async () => {
    if (!user) return
    
    setLoading(true)
    setError(null)

    try {
      const response = await adminApi.resetUserPassword(user.id)
      console.log('Reset password response:', response)
      // Нормализуем boolean значение для sentViaTelegram
      const normalizedResponse = {
        ...response,
        sentViaTelegram: response.sentViaTelegram === true || response.sentViaTelegram === 'true'
      }
      console.log('Normalized response:', normalizedResponse)
      setResult(normalizedResponse)
      // НЕ вызываем onConfirm здесь - вызовем при закрытии модального окна
    } catch (err) {
      setError(err.message || 'Произошла ошибка при сбросе пароля')
    } finally {
      setLoading(false)
    }
  }

  const handleClose = () => {
    // Если есть результат (пароль был сброшен), вызываем onConfirm для обновления списка
    if (result && onConfirm) {
      onConfirm()
    }
    // Сбрасываем состояние и закрываем модальное окно
    setResult(null)
    setError(null)
    onClose()
  }

  if (result) {
    const wasSentViaTelegram = result.sentViaTelegram === true || result.sentViaTelegram === 'true'
    const password = result.temporaryPassword
    const hasPassword = password != null && password !== '' && String(password).trim() !== ''
    
    console.log('Rendering result:', { 
      wasSentViaTelegram, 
      hasPassword, 
      temporaryPassword: password,
      passwordType: typeof password,
      passwordLength: password ? String(password).length : 0,
      fullResult: result
    })
    
    return (
      <div className="admin-modal-overlay" onClick={handleClose}>
        <div className="admin-modal" onClick={(e) => e.stopPropagation()}>
          <h3>Результат сброса пароля</h3>
          {wasSentViaTelegram && (
            <div className="admin-success-message">
              Новый пароль отправлен пользователю в Telegram
            </div>
          )}
          {hasPassword ? (
            <div>
              <div className="admin-modal-message">
                Новый пароль для пользователя <strong>{user?.username || 'пользователя'}</strong>:
              </div>
              <div className="admin-password-display">
                <span style={{ color: '#646cff', fontWeight: 'bold', fontSize: '1.1rem' }}>
                  {String(password)}
                </span>
                <button
                  className="admin-copy-button"
                  onClick={() => {
                    navigator.clipboard.writeText(String(password))
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
          ) : (
            <div className="admin-error">
              Ошибка: пароль не был получен. 
              <div style={{ marginTop: '0.5rem', fontSize: '0.85rem' }}>
                Ответ сервера: {JSON.stringify(result, null, 2)}
              </div>
            </div>
          )}
          <div className="admin-modal-actions">
            <button
              className="admin-button-confirm"
              onClick={handleClose}
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
