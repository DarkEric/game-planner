import './GameDetails.css'
import { useState } from 'react'
import DOMPurify from 'dompurify'
import { gameApi } from '../services/gameApi'

function toDatetimeLocalValue(date) {
  const d = new Date(date)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

const GameDetails = ({ game, currentUserId, onJoin, onLeave, onDelete, onClose, onUpdate }) => {
  const [showHeldModal, setShowHeldModal] = useState(false)
  const [showEditModal, setShowEditModal] = useState(false)
  const [editTitle, setEditTitle] = useState('')
  const [editDescription, setEditDescription] = useState('')
  const [editStart, setEditStart] = useState('')
  const [editEnd, setEditEnd] = useState('')
  const [editMaxParticipants, setEditMaxParticipants] = useState('')
  const [editSubmitting, setEditSubmitting] = useState(false)
  const [keyEvents, setKeyEvents] = useState('')
  const [activeTab, setActiveTab] = useState('write') // 'write' or 'preview'
  const [isSubmitting, setIsSubmitting] = useState(false)

  const isCreator = game.creatorId === currentUserId
  const isParticipant = game.participants.some(p => p.id === currentUserId)
  const canEditGame =
    isCreator &&
    !game.isHeld &&
    new Date(game.endTime).getTime() > Date.now()
  
  // Подсчет участников без создателя
  const participantCount = game.participants ? game.participants.filter(p => p.id !== game.creatorId).length : 0
  const maxParticipants = game.maxParticipants
  
  // Проверяем, что maxParticipants задан (не null и не undefined)
  const hasMaxParticipants = maxParticipants != null && maxParticipants !== undefined
  
  // Проверка заполненности игры
  const isFull = hasMaxParticipants && participantCount >= maxParticipants
  
  // Формат отображения: "Участники (X/Y)" или "Участники (X)"
  const participantsLabel = hasMaxParticipants
    ? `Участники (${participantCount}/${maxParticipants})`
    : `Участники (${participantCount})`

  const formatDateTime = (date) => {
    return new Date(date).toLocaleString('ru-RU', {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
      hour: '2-digit',
      minute: '2-digit'
    })
  }

  const getDuration = () => {
    const start = new Date(game.startTime)
    const end = new Date(game.endTime)
    const hours = Math.round((end - start) / (1000 * 60 * 60))
    return `${hours} ч`
  }

  const handleMarkAsHeld = async () => {
    try {
      setIsSubmitting(true)
      const updatedGame = await gameApi.markGameAsHeld(game.id, keyEvents)
      if (onUpdate) {
        onUpdate(updatedGame)
      }
      setShowHeldModal(false)
    } catch (error) {
      console.error('Failed to mark game as held:', error)
      alert('Не удалось отметить игру как проведенную')
    } finally {
      setIsSubmitting(false)
    }
  }

  const openEditModal = () => {
    setEditTitle(game.title || '')
    setEditDescription(game.description || '')
    setEditStart(toDatetimeLocalValue(game.startTime))
    setEditEnd(toDatetimeLocalValue(game.endTime))
    setEditMaxParticipants(game.maxParticipants != null ? String(game.maxParticipants) : '')
    setShowEditModal(true)
  }

  const handleSaveEdit = async () => {
    const start = new Date(editStart)
    const end = new Date(editEnd)
    if (!(end > start)) {
      alert('Время окончания должно быть позже времени начала')
      return
    }

    let maxP = null
    const maxTrim = editMaxParticipants.trim()
    if (maxTrim !== '') {
      maxP = parseInt(maxTrim, 10)
      if (Number.isNaN(maxP) || maxP < 0) {
        alert('Укажите корректный лимит участников или оставьте поле пустым')
        return
      }
    }

    try {
      setEditSubmitting(true)
      const updated = await gameApi.updateGame(game.id, {
        startTime: start,
        endTime: end,
        title: editTitle.trim() || null,
        description: editDescription.trim() || null,
        maxParticipants: maxP
      })

      const sameCampaign =
        updated.campaignId != null &&
        game.campaignId != null &&
        String(updated.campaignId) === String(game.campaignId)
      const campaign = !updated.campaignId
        ? undefined
        : sameCampaign && game.campaign
          ? game.campaign
          : { name: updated.campaignName, sessionNumber: game.campaign?.sessionNumber }

      if (onUpdate) {
        onUpdate({ ...updated, campaign })
      }
      setShowEditModal(false)
    } catch (error) {
      console.error('Failed to update game:', error)
      alert('Не удалось сохранить изменения')
    } finally {
      setEditSubmitting(false)
    }
  }

  const handleRemovePlayer = async (playerId) => {
    if (!confirm('Удалить этого игрока из игры?')) {
      return
    }

    try {
      const updatedGame = await gameApi.removePlayerFromGame(game.id, playerId)
      if (onUpdate) {
        onUpdate(updatedGame)
      }
    } catch (error) {
      console.error('Failed to remove player:', error)
      alert('Не удалось удалить игрока из игры')
    }
  }

  return (
    <>
      <div className="game-details-overlay" onClick={onClose}>
        <div className="game-details-modal" onClick={(e) => e.stopPropagation()}>
          <div className="game-details-header">
            <h2>🎲 Детали игры</h2>
            <button className="close-button" onClick={onClose}>×</button>
          </div>

          <div className="game-details-content">
            {game.title && (
              <div className="game-title-section">
                <h3 style={{ margin: '0 0 0.5rem 0', color: '#fff', fontSize: '1.2rem' }}>
                  {game.title}
                  {game.isHeld && <span className="held-badge" style={{
                    marginLeft: '10px',
                    fontSize: '0.8rem',
                    background: '#4caf50',
                    padding: '2px 8px',
                    borderRadius: '4px',
                    verticalAlign: 'middle'
                  }}>Проведена</span>}
                </h3>
              </div>
            )}

            {game.description && (
              <div className="game-description-section" style={{
                marginBottom: '1rem',
                padding: '0.75rem',
                background: '#2a2a2a',
                borderRadius: '6px',
                color: '#ccc',
                fontSize: '0.95rem',
                lineHeight: '1.5'
              }}>
                {game.description}
              </div>
            )}

            {game.isHeld && game.keyEvents && (
              <div className="game-key-events-section" style={{
                marginBottom: '1rem',
                padding: '0.75rem',
                background: '#1e3a2a',
                borderRadius: '6px',
                color: '#e0e0e0',
                fontSize: '0.95rem',
                lineHeight: '1.5'
              }}>
                <h4 style={{ marginTop: 0, marginBottom: '0.5rem', color: '#81c784' }}>Ключевые события:</h4>
                <div className="markdown-content" dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(game.keyEvents) }} />
              </div>
            )}

            {game.campaign && (
              <div className="game-campaign-section" style={{
                marginBottom: '1rem',
                padding: '0.75rem',
                background: 'rgba(100, 108, 255, 0.1)',
                borderRadius: '6px',
                border: '1px solid rgba(100, 108, 255, 0.3)'
              }}>
                <h4 style={{ marginTop: 0, marginBottom: '0.5rem', color: '#646cff' }}>
                  📚 Часть кампании
                </h4>
                <div style={{ color: '#ccc' }}>
                  <strong>{game.campaign.name}</strong>
                  {game.campaign.sessionNumber && (
                    <span style={{ marginLeft: '0.5rem', color: '#888', fontSize: '0.9rem' }}>
                      (Сессия {game.campaign.sessionNumber})
                    </span>
                  )}
                </div>
              </div>
            )}

            <div className="game-info-section">
              <div className="game-info-item">
                <div className="game-info-label">Начало</div>
                <div className="game-info-value">{formatDateTime(game.startTime)}</div>
              </div>

              <div className="game-info-item">
                <div className="game-info-label">Окончание</div>
                <div className="game-info-value">{formatDateTime(game.endTime)}</div>
              </div>

              <div className="game-info-item">
                <div className="game-info-label">Длительность</div>
                <div className="game-info-value">{getDuration()}</div>
              </div>

              <div className="game-info-item">
                <div className="game-info-label">Организатор</div>
                <div className="game-info-value">{game.creatorName}</div>
              </div>

              <div className="game-info-item">
                <div className="game-info-label">Игроки</div>
                <div className="game-info-value">
                  {participantsLabel}
                  {isFull && hasMaxParticipants && (
                    <span style={{ 
                      marginLeft: '0.5rem', 
                      color: '#ff6b6b', 
                      fontSize: '0.9rem'
                    }}>
                      (Заполнена)
                    </span>
                  )}
                </div>
              </div>

              {hasMaxParticipants && (
                <div className="game-info-item">
                  <div className="game-info-label">Максимум участников</div>
                  <div className="game-info-value">{maxParticipants}</div>
                </div>
              )}
            </div>

            <div className="game-participants">
              <div className="game-info-label">
                {participantsLabel}
                {isFull && maxParticipants != null && (
                  <span style={{ 
                    marginLeft: '0.5rem', 
                    color: '#ff6b6b', 
                    fontSize: '0.9rem',
                    fontWeight: 'normal'
                  }}>
                    (Игра заполнена)
                  </span>
                )}
              </div>
              {game.participants.length > 0 ? (
                <div className="participants-list">
                  {game.participants.map(participant => (
                    <div
                      key={participant.id}
                      className={`participant-badge ${participant.id === game.creatorId ? 'creator' : ''}`}
                      style={{ 
                        backgroundColor: participant.color,
                        display: 'flex',
                        alignItems: 'center',
                        gap: '8px'
                      }}
                    >
                      <span>{participant.name}</span>
                      {isCreator && participant.id !== game.creatorId && (
                        <button
                          onClick={(e) => {
                            e.stopPropagation()
                            handleRemovePlayer(participant.id)
                          }}
                          style={{
                            background: 'rgba(255, 255, 255, 0.2)',
                            border: 'none',
                            borderRadius: '50%',
                            width: '20px',
                            height: '20px',
                            color: '#fff',
                            cursor: 'pointer',
                            fontSize: '14px',
                            lineHeight: '1',
                            padding: '0',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center'
                          }}
                          title="Удалить игрока"
                        >
                          ×
                        </button>
                      )}
                    </div>
                  ))}
                </div>
              ) : (
                <div className="no-participants">Пока нет участников</div>
              )}
            </div>

            <div className="game-details-actions">
              {isCreator ? (
                <>
                  {canEditGame && (
                    <button
                      type="button"
                      className="game-action-button"
                      style={{ backgroundColor: '#646cff' }}
                      onClick={openEditModal}
                    >
                      Редактировать
                    </button>
                  )}
                  {!game.isHeld && (
                    <button
                      className="game-action-button held-button"
                      style={{ backgroundColor: '#4caf50' }}
                      onClick={() => setShowHeldModal(true)}
                    >
                      Игра состоялась
                    </button>
                  )}
                  <button
                    className="game-action-button delete-button"
                    onClick={() => {
                      const reason = prompt('Причина отмены игры (опционально):')
                      if (reason !== null) { // null если нажали Cancel
                        onDelete(game.id, reason)
                      }
                    }}
                  >
                    Удалить игру
                  </button>
                  <button
                    className="game-action-button close-button-game"
                    onClick={onClose}
                  >
                    Закрыть
                  </button>
                </>
              ) : isParticipant ? (
                <>
                  <button
                    className="game-action-button leave-button"
                    onClick={() => onLeave(game.id)}
                  >
                    Покинуть игру
                  </button>
                  <button
                    className="game-action-button close-button-game"
                    onClick={onClose}
                  >
                    Закрыть
                  </button>
                </>
              ) : (
                <>
                  <button
                    className="game-action-button join-button"
                    onClick={() => onJoin(game.id)}
                    disabled={isFull}
                    style={isFull ? { opacity: 0.5, cursor: 'not-allowed' } : {}}
                  >
                    {isFull ? 'Игра заполнена' : 'Записаться на игру'}
                  </button>
                  <button
                    className="game-action-button close-button-game"
                    onClick={onClose}
                  >
                    Закрыть
                  </button>
                </>
              )}
            </div>
          </div>
        </div>
      </div>

      {showEditModal && (
        <div className="modal-overlay" onClick={() => !editSubmitting && setShowEditModal(false)}>
          <div
            className="modal-content"
            onClick={e => e.stopPropagation()}
            style={{ width: '520px', maxWidth: '90%' }}
          >
            <h3>Редактирование игры</h3>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', marginTop: '12px' }}>
              <label style={{ color: '#ccc', fontSize: '0.9rem' }}>
                Название
                <input
                  type="text"
                  value={editTitle}
                  onChange={e => setEditTitle(e.target.value)}
                  placeholder="Необязательно"
                  style={{
                    display: 'block',
                    width: '100%',
                    marginTop: '4px',
                    padding: '8px',
                    background: '#222',
                    border: '1px solid #444',
                    borderRadius: '4px',
                    color: '#fff'
                  }}
                />
              </label>
              <label style={{ color: '#ccc', fontSize: '0.9rem' }}>
                Описание
                <textarea
                  value={editDescription}
                  onChange={e => setEditDescription(e.target.value)}
                  rows={3}
                  style={{
                    display: 'block',
                    width: '100%',
                    marginTop: '4px',
                    padding: '8px',
                    background: '#222',
                    border: '1px solid #444',
                    borderRadius: '4px',
                    color: '#fff',
                    resize: 'vertical'
                  }}
                />
              </label>
              <label style={{ color: '#ccc', fontSize: '0.9rem' }}>
                Начало
                <input
                  type="datetime-local"
                  value={editStart}
                  onChange={e => setEditStart(e.target.value)}
                  style={{
                    display: 'block',
                    width: '100%',
                    marginTop: '4px',
                    padding: '8px',
                    background: '#222',
                    border: '1px solid #444',
                    borderRadius: '4px',
                    color: '#fff'
                  }}
                />
              </label>
              <label style={{ color: '#ccc', fontSize: '0.9rem' }}>
                Окончание
                <input
                  type="datetime-local"
                  value={editEnd}
                  onChange={e => setEditEnd(e.target.value)}
                  style={{
                    display: 'block',
                    width: '100%',
                    marginTop: '4px',
                    padding: '8px',
                    background: '#222',
                    border: '1px solid #444',
                    borderRadius: '4px',
                    color: '#fff'
                  }}
                />
              </label>
              <label style={{ color: '#ccc', fontSize: '0.9rem' }}>
                Макс. участников (без учёта организатора), пусто — без лимита
                <input
                  type="number"
                  min={0}
                  value={editMaxParticipants}
                  onChange={e => setEditMaxParticipants(e.target.value)}
                  placeholder=""
                  style={{
                    display: 'block',
                    width: '100%',
                    marginTop: '4px',
                    padding: '8px',
                    background: '#222',
                    border: '1px solid #444',
                    borderRadius: '4px',
                    color: '#fff'
                  }}
                />
              </label>
            </div>
            <div className="modal-actions" style={{ marginTop: '20px', display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
              <button
                type="button"
                onClick={() => setShowEditModal(false)}
                disabled={editSubmitting}
                style={{
                  padding: '8px 16px',
                  background: 'transparent',
                  border: '1px solid #666',
                  color: '#fff',
                  borderRadius: '4px',
                  cursor: editSubmitting ? 'default' : 'pointer'
                }}
              >
                Отмена
              </button>
              <button
                type="button"
                onClick={handleSaveEdit}
                disabled={editSubmitting}
                style={{
                  padding: '8px 16px',
                  background: '#646cff',
                  border: 'none',
                  color: '#fff',
                  borderRadius: '4px',
                  cursor: editSubmitting ? 'default' : 'pointer',
                  opacity: editSubmitting ? 0.7 : 1
                }}
              >
                {editSubmitting ? 'Сохранение...' : 'Сохранить'}
              </button>
            </div>
          </div>
        </div>
      )}

      {
        showHeldModal && (
          <div className="modal-overlay" onClick={() => setShowHeldModal(false)}>
            <div className="modal-content" onClick={e => e.stopPropagation()} style={{ width: '600px', maxWidth: '90%' }}>
              <h3>Завершение игры</h3>
              <p>Опишите ключевые события игры (поддерживается HTML):</p>

              <div className="tabs" style={{ display: 'flex', marginBottom: '10px', borderBottom: '1px solid #444' }}>
                <button
                  style={{
                    padding: '8px 16px',
                    background: activeTab === 'write' ? '#444' : 'transparent',
                    border: 'none',
                    color: activeTab === 'write' ? '#fff' : '#aaa',
                    cursor: 'pointer'
                  }}
                  onClick={() => setActiveTab('write')}
                >
                  Редактор
                </button>
                <button
                  style={{
                    padding: '8px 16px',
                    background: activeTab === 'preview' ? '#444' : 'transparent',
                    border: 'none',
                    color: activeTab === 'preview' ? '#fff' : '#aaa',
                    cursor: 'pointer'
                  }}
                  onClick={() => setActiveTab('preview')}
                >
                  Предпросмотр
                </button>
              </div>

              {activeTab === 'write' ? (
                <textarea
                  value={keyEvents}
                  onChange={e => setKeyEvents(e.target.value)}
                  placeholder="Что интересного произошло? Кто победил? Какие были смешные моменты?"
                  style={{
                    width: '100%',
                    height: '200px',
                    padding: '10px',
                    background: '#222',
                    color: '#fff',
                    border: '1px solid #444',
                    borderRadius: '4px',
                    resize: 'vertical',
                    fontFamily: 'monospace'
                  }}
                />
              ) : (
                <div style={{
                  height: '200px',
                  overflowY: 'auto',
                  padding: '10px',
                  background: '#222',
                  border: '1px solid #444',
                  borderRadius: '4px'
                }} dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(keyEvents || '<i>Ничего не написано</i>') }} />
              )}

              <div className="modal-actions" style={{ marginTop: '20px', display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
                <button
                  onClick={() => setShowHeldModal(false)}
                  style={{ padding: '8px 16px', background: 'transparent', border: '1px solid #666', color: '#fff', borderRadius: '4px', cursor: 'pointer' }}
                >
                  Отмена
                </button>
                <button
                  onClick={handleMarkAsHeld}
                  disabled={isSubmitting}
                  style={{ padding: '8px 16px', background: '#4caf50', border: 'none', color: '#fff', borderRadius: '4px', cursor: 'pointer', opacity: isSubmitting ? 0.7 : 1 }}
                >
                  {isSubmitting ? 'Сохранение...' : 'Сохранить и завершить'}
                </button>
              </div>
            </div >
          </div >
        )}
    </>
  )
}

export default GameDetails
