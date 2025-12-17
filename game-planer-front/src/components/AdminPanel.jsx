import { useState, useEffect } from 'react'
import AdminUserList from './AdminUserList'
import { adminApi } from '../services/api'
import './AdminPanel.css'

const AdminPanel = ({ currentUserId }) => {
  const [users, setUsers] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    loadUsers()
  }, [])

  const loadUsers = async () => {
    try {
      setLoading(true)
      setError(null)
      const usersData = await adminApi.getAllUsers()
      setUsers(usersData)
    } catch (err) {
      setError('Не удалось загрузить список пользователей')
      console.error('Failed to load users:', err)
    } finally {
      setLoading(false)
    }
  }

  const handlePasswordReset = async () => {
    // Перезагружаем список после сброса пароля
    await loadUsers()
  }

  const handleAdminRightsChange = async () => {
    // Перезагружаем список после изменения прав
    await loadUsers()
  }

  if (loading) {
    return (
      <div className="admin-panel">
        <div className="admin-loading">Загрузка...</div>
      </div>
    )
  }

  return (
    <div className="admin-panel">
      <div className="admin-header">
        <h2>Админ-панель</h2>
      </div>
      {error && (
        <div className="admin-error">
          {error}
        </div>
      )}
      <AdminUserList
        users={users}
        currentUserId={currentUserId}
        onPasswordReset={handlePasswordReset}
        onAdminRightsChange={handleAdminRightsChange}
      />
    </div>
  )
}

export default AdminPanel
