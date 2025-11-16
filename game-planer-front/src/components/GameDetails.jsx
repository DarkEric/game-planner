import './GameDetails.css'

const GameDetails = ({ game, currentUserId, onJoin, onLeave, onDelete, onClose }) => {
  const isCreator = game.creatorId === currentUserId
  const isParticipant = game.participants.some(p => p.id === currentUserId)
  
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

  return (
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
          </div>

          <div className="game-participants">
            <div className="game-info-label">
              Участники ({game.participants.length})
            </div>
            {game.participants.length > 0 ? (
              <div className="participants-list">
                {game.participants.map(participant => (
                  <div
                    key={participant.id}
                    className={`participant-badge ${participant.id === game.creatorId ? 'creator' : ''}`}
                    style={{ backgroundColor: participant.color }}
                  >
                    {participant.name}
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
                <button 
                  className="game-action-button delete-button"
                  onClick={() => {
                    if (confirm('Удалить эту игру?')) {
                      onDelete(game.id)
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
                >
                  Записаться на игру
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
  )
}

export default GameDetails
