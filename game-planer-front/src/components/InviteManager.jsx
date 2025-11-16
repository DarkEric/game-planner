import { useState, useEffect } from 'react'
import { inviteApi } from '../services/inviteApi'
import { useLanguage } from '../i18n/LanguageContext'
import './InviteManager.css'

const InviteManager = () => {
  const { t } = useLanguage()
  const [invites, setInvites] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [showCreateForm, setShowCreateForm] = useState(false)
  const [copiedCode, setCopiedCode] = useState(null)

  useEffect(() => {
    loadInvites()
  }, [])

  const loadInvites = async () => {
    try {
      setLoading(true)
      const data = await inviteApi.getMyInvites()
      setInvites(data)
    } catch (err) {
      setError(t('errorLoadData'))
    } finally {
      setLoading(false)
    }
  }

  const handleCreateInvite = async () => {
    try {
      setError(null)
      const newInvite = await inviteApi.createInvite(null, 1) // Одноразовый бессрочный
      setInvites([newInvite, ...invites])
      setShowCreateForm(false)
    } catch (err) {
      setError(t('errorLoadData'))
    }
  }

  const handleDeleteInvite = async (inviteId) => {
    if (!confirm(t('deleteGame') + '?')) return

    try {
      setError(null)
      await inviteApi.deleteInvite(inviteId)
      setInvites(invites.filter(inv => inv.id !== inviteId))
    } catch (err) {
      setError(t('errorLoadData'))
    }
  }

  const copyToClipboard = (code) => {
    navigator.clipboard.writeText(code)
    setCopiedCode(code)
    setTimeout(() => setCopiedCode(null), 2000)
  }

  const getInviteUrl = (code) => {
    return `${window.location.origin}?invite=${code}`
  }

  if (loading) {
    return <div className="invite-manager">{t('loading')}</div>
  }

  return (
    <div className="invite-manager">
      <div className="invite-header">
        <h3>{t('myInvites')}</h3>
        <button onClick={() => setShowCreateForm(!showCreateForm)} className="create-invite-btn">
          + {t('createInvite')}
        </button>
      </div>

      {error && (
        <div className="invite-error">{error}</div>
      )}

      {showCreateForm && (
        <div className="create-invite-form">
          <p>{t('createInviteQuestion')}</p>
          <div className="form-actions">
            <button onClick={handleCreateInvite} className="btn-primary">{t('create')}</button>
            <button onClick={() => setShowCreateForm(false)} className="btn-secondary">{t('cancel')}</button>
          </div>
        </div>
      )}

      <div className="invites-list">
        {invites.length === 0 ? (
          <p className="no-invites">{t('noInvites')}</p>
        ) : (
          invites.map(invite => (
            <div key={invite.id} className={`invite-item ${!invite.isValid ? 'invalid' : ''}`}>
              <div className="invite-code-section">
                <code className="invite-code">{invite.code}</code>
                <button 
                  onClick={() => copyToClipboard(invite.code)}
                  className="copy-btn"
                  title={t('create')}
                >
                  {copiedCode === invite.code ? '✓' : '📋'}
                </button>
              </div>
              <div className="invite-stats">
                <div className="invite-stats-left">
                  <span className={`invite-status ${invite.isValid ? 'valid' : 'invalid'}`}>
                    {invite.isValid ? `✓ ${t('active')}` : `✗ ${t('inactive')}`}
                  </span>
                  {invite.maxUses && (
                    <span className="invite-uses">
                      {t('uses')}: {invite.usesCount}/{invite.maxUses}
                    </span>
                  )}
                  {invite.usedByName && (
                    <span className="invite-used-by">
                      {t('usedBy')}: {invite.usedByName}
                    </span>
                  )}
                </div>
                <button 
                  onClick={() => handleDeleteInvite(invite.id)}
                  className="delete-invite-btn"
                  title={t('deleteGame')}
                >
                  🗑️
                </button>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  )
}

export default InviteManager
