import { useState } from 'react'
import ResetPasswordModal from './ResetPasswordModal'
import GrantAdminModal from './GrantAdminModal'
import DeleteUserModal from './DeleteUserModal'
import { adminApi } from '../services/api'
import './AdminPanel.css'

const AdminUserList = ({ users, currentUserId, onPasswordReset, onAdminRightsChange }) => {
  const [searchTerm, setSearchTerm] = useState('')
  const [selectedUser, setSelectedUser] = useState(null)
  const [modalType, setModalType] = useState(null) // 'reset-password', 'grant-admin', 'revoke-admin', 'delete-user'
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  const filteredUsers = users.filter(user => {
    const search = searchTerm.toLowerCase()
    return user.username.toLowerCase().includes(search) ||
           (user.email && user.email.toLowerCase().includes(search))
  })

  const handleResetPassword = (user) => {
    setSelectedUser(user)
    setModalType('reset-password')
    setError(null)
  }

  const handleGrantAdmin = (user) => {
    setSelectedUser(user)
    setModalType('grant-admin')
    setError(null)
  }

  const handleRevokeAdmin = (user) => {
    setSelectedUser(user)
    setModalType('revoke-admin')
    setError(null)
  }

  const handleDeleteUser = (user) => {
    setSelectedUser(user)
    setModalType('delete-user')
    setError(null)
  }

  const handleCloseModal = () => {
    setSelectedUser(null)
    setModalType(null)
    setError(null)
  }

  const handleConfirmAction = async () => {
    if (!selectedUser) return

    setLoading(true)
    setError(null)

    try {
      if (modalType === 'grant-admin') {
        await adminApi.grantAdminRights(selectedUser.id)
        onAdminRightsChange()
        handleCloseModal()
      } else if (modalType === 'revoke-admin') {
        await adminApi.revokeAdminRights(selectedUser.id)
        onAdminRightsChange()
        handleCloseModal()
      }
      // Для reset-password модальное окно само вызывает API
    } catch (err) {
      setError(err.message || 'Произошла ошибка')
      setLoading(false)
    }
  }

  const handlePasswordResetComplete = () => {
    onPasswordReset()
  }

  const handleUserDeleted = async () => {
    // Перезагружаем список после удаления
    if (onPasswordReset) {
      await onPasswordReset()
    }
    handleCloseModal()
  }

  const isCurrentUser = (user) => user.id === currentUserId
  const isLastAdmin = users.filter(u => u.isAdmin).length === 1

  return (
    <div className="admin-user-list">
      <div className="admin-search">
        <input
          type="text"
          placeholder="Поиск по имени пользователя или email..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
        />
      </div>

      {error && (
        <div className="admin-error">
          {error}
        </div>
      )}

      <table className="admin-table">
        <thead>
          <tr>
            <th>Имя пользователя</th>
            <th>Email</th>
            <th>Telegram</th>
            <th>Администратор</th>
            <th>Действия</th>
          </tr>
        </thead>
        <tbody>
          {filteredUsers.map(user => (
            <tr key={user.id}>
              <td>{user.username}</td>
              <td>{user.email}</td>
              <td>
                {user.telegramSubscribed ? (
                  <span className="admin-badge admin-badge-telegram">✓</span>
                ) : (
                  <span style={{ color: '#666' }}>—</span>
                )}
              </td>
              <td>
                {user.isAdmin ? (
                  <span className="admin-badge admin-badge-admin">Админ</span>
                ) : (
                  <span style={{ color: '#666' }}>—</span>
                )}
              </td>
              <td>
                <div className="admin-actions">
                  <button
                    className="admin-button admin-button-primary"
                    onClick={() => handleResetPassword(user)}
                    disabled={loading}
                  >
                    Сбросить пароль
                  </button>
                  {user.isAdmin ? (
                    <button
                      className="admin-button admin-button-danger"
                      onClick={() => handleRevokeAdmin(user)}
                      disabled={loading || isCurrentUser(user) || (isLastAdmin && user.isAdmin)}
                      title={
                        isCurrentUser(user)
                          ? 'Вы не можете отозвать права у самого себя'
                          : isLastAdmin && user.isAdmin
                          ? 'Нельзя отозвать права у последнего администратора'
                          : 'Отозвать права администратора'
                      }
                    >
                      Отозвать права
                    </button>
                  ) : (
                    <button
                      className="admin-button admin-button-primary"
                      onClick={() => handleGrantAdmin(user)}
                      disabled={loading}
                    >
                      Назначить админом
                    </button>
                  )}
                  <button
                    className="admin-button admin-button-danger"
                    onClick={() => handleDeleteUser(user)}
                    disabled={loading || isCurrentUser(user) || (user.isAdmin && isLastAdmin)}
                    title={
                      isCurrentUser(user)
                        ? 'Вы не можете удалить самого себя'
                        : user.isAdmin && isLastAdmin
                        ? 'Нельзя удалить последнего администратора'
                        : 'Удалить пользователя'
                    }
                    style={{ marginLeft: '0.5rem' }}
                  >
                    Удалить
                  </button>
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {filteredUsers.length === 0 && (
        <div style={{ textAlign: 'center', padding: '2rem', color: '#666' }}>
          {searchTerm ? 'Пользователи не найдены' : 'Нет пользователей'}
        </div>
      )}

      {modalType === 'reset-password' && selectedUser && (
        <ResetPasswordModal
          user={selectedUser}
          onClose={handleCloseModal}
          onConfirm={handlePasswordResetComplete}
          loading={loading}
        />
      )}

      {(modalType === 'grant-admin' || modalType === 'revoke-admin') && selectedUser && (
        <GrantAdminModal
          user={selectedUser}
          action={modalType}
          onClose={handleCloseModal}
          onConfirm={handleConfirmAction}
          loading={loading}
          isLastAdmin={isLastAdmin && selectedUser.isAdmin}
          isCurrentUser={isCurrentUser(selectedUser)}
        />
      )}

      {modalType === 'delete-user' && selectedUser && (
        <DeleteUserModal
          user={selectedUser}
          onClose={handleCloseModal}
          onConfirm={handleUserDeleted}
          loading={loading}
        />
      )}
    </div>
  )
}

export default AdminUserList
