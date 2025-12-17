import './AdminPanel.css'

const GrantAdminModal = ({ user, action, onClose, onConfirm, loading, isLastAdmin, isCurrentUser }) => {
  const isGrant = action === 'grant-admin'
  const isRevoke = action === 'revoke-admin'

  const getWarningMessage = () => {
    if (isCurrentUser) {
      return 'Вы не можете отозвать права администратора у самого себя'
    }
    if (isLastAdmin && isRevoke) {
      return 'Невозможно отозвать права: в системе должен остаться минимум один администратор'
    }
    return null
  }

  const warningMessage = getWarningMessage()

  return (
    <div className="admin-modal-overlay" onClick={onClose}>
      <div className="admin-modal" onClick={(e) => e.stopPropagation()}>
        <h3>
          {isGrant ? 'Назначить администратором' : 'Отозвать права администратора'}
        </h3>
        <div className="admin-modal-message">
          {isGrant ? (
            <>
              Вы уверены, что хотите назначить пользователя <strong>{user.username}</strong> администратором?
            </>
          ) : (
            <>
              Вы уверены, что хотите отозвать права администратора у пользователя <strong>{user.username}</strong>?
            </>
          )}
        </div>
        {warningMessage && (
          <div className="admin-error">
            {warningMessage}
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
            className={isRevoke ? 'admin-button-danger' : 'admin-button-confirm'}
            onClick={onConfirm}
            disabled={loading || !!warningMessage}
            style={isRevoke ? {
              background: warningMessage ? '#555' : '#ff6b6b',
              color: '#fff'
            } : {}}
          >
            {loading ? 'Обработка...' : (isGrant ? 'Назначить' : 'Отозвать права')}
          </button>
        </div>
      </div>
    </div>
  )
}

export default GrantAdminModal
