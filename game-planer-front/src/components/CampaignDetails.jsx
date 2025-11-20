import { useState, useEffect } from 'react'
import { campaignApi } from '../services/campaignApi'
import { gameApi } from '../services/gameApi'
import DOMPurify from 'dompurify'
import './CampaignDetails.css'

const CampaignDetails = ({ campaignId, currentUserId, onBack }) => {
    const [campaign, setCampaign] = useState(null)
    const [games, setGames] = useState([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState(null)

    useEffect(() => {
        loadCampaignDetails()
    }, [campaignId])

    const loadCampaignDetails = async () => {
        try {
            setLoading(true)
            const data = await campaignApi.getCampaignDetails(campaignId)
            setCampaign(data)

            // Load games for this campaign
            if (data.gameIds && data.gameIds.length > 0) {
                const gamesData = await Promise.all(
                    data.gameIds.map(id => gameApi.getGameById(id))
                )
                setGames(gamesData.sort((a, b) => new Date(a.startTime) - new Date(b.startTime)))
            }

            setError(null)
        } catch (err) {
            setError('Не удалось загрузить детали кампании')
            console.error('Error loading campaign details:', err)
        } finally {
            setLoading(false)
        }
    }

    const getStatusIcon = (status) => {
        switch (status) {
            case 'ACTIVE': return '🟢'
            case 'COMPLETED': return '✅'
            case 'ON_HOLD': return '⏸️'
            default: return '❓'
        }
    }

    const getStatusText = (status) => {
        switch (status) {
            case 'ACTIVE': return 'Активна'
            case 'COMPLETED': return 'Завершена'
            case 'ON_HOLD': return 'На паузе'
            default: return status
        }
    }

    const formatDate = (dateString) => {
        const date = new Date(dateString)
        return date.toLocaleDateString('ru-RU', { day: 'numeric', month: 'long', year: 'numeric' })
    }

    const isCreator = campaign?.creator?.id === currentUserId

    const handleStatusChange = async (newStatus) => {
        try {
            await campaignApi.updateCampaignStatus(campaignId, newStatus)
            loadCampaignDetails()
        } catch (error) {
            console.error('Failed to update status:', error)
            alert('Не удалось изменить статус кампании')
        }
    }

    if (loading) {
        return (
            <div className="campaign-details">
                <div className="loading">Загрузка...</div>
            </div>
        )
    }

    if (error || !campaign) {
        return (
            <div className="campaign-details">
                <button onClick={onBack} className="back-btn">← Назад</button>
                <div className="error">{error || 'Кампания не найдена'}</div>
            </div>
        )
    }

    const heldGames = games.filter(g => g.isHeld || g.held)
    const upcomingGames = games.filter(g => !(g.isHeld || g.held))

    return (
        <div className="campaign-details">
            <button onClick={onBack} className="back-btn">← Назад к списку</button>

            <div className="campaign-header">
                <div className="campaign-title-section">
                    <h1>{campaign.name}</h1>
                    <span className="campaign-status-badge">
                        {getStatusIcon(campaign.status)} {getStatusText(campaign.status)}
                    </span>
                </div>

                {campaign.description && (
                    <p className="campaign-description">{campaign.description}</p>
                )}

                <div className="campaign-meta">
                    <div className="meta-item">
                        <span className="meta-label">Мастер:</span>
                        <span className="meta-value">{campaign.creator?.name}</span>
                    </div>
                    <div className="meta-item">
                        <span className="meta-label">Создана:</span>
                        <span className="meta-value">{formatDate(campaign.createdAt)}</span>
                    </div>
                </div>

                {isCreator && (
                    <div className="status-controls" style={{
                        marginTop: '1rem',
                        display: 'flex',
                        gap: '0.5rem',
                        flexWrap: 'wrap'
                    }}>
                        <button
                            onClick={() => handleStatusChange('ACTIVE')}
                            disabled={campaign.status === 'ACTIVE'}
                            style={{
                                padding: '0.5rem 1rem',
                                background: campaign.status === 'ACTIVE' ? '#4caf50' : '#2a2a2a',
                                border: '1px solid #4caf50',
                                borderRadius: '6px',
                                color: '#fff',
                                cursor: campaign.status === 'ACTIVE' ? 'default' : 'pointer',
                                opacity: campaign.status === 'ACTIVE' ? 0.7 : 1
                            }}
                        >
                            🟢 Активна
                        </button>
                        <button
                            onClick={() => handleStatusChange('ON_HOLD')}
                            disabled={campaign.status === 'ON_HOLD'}
                            style={{
                                padding: '0.5rem 1rem',
                                background: campaign.status === 'ON_HOLD' ? '#ff9800' : '#2a2a2a',
                                border: '1px solid #ff9800',
                                borderRadius: '6px',
                                color: '#fff',
                                cursor: campaign.status === 'ON_HOLD' ? 'default' : 'pointer',
                                opacity: campaign.status === 'ON_HOLD' ? 0.7 : 1
                            }}
                        >
                            ⏸️ На паузе
                        </button>
                        <button
                            onClick={() => handleStatusChange('COMPLETED')}
                            disabled={campaign.status === 'COMPLETED'}
                            style={{
                                padding: '0.5rem 1rem',
                                background: campaign.status === 'COMPLETED' ? '#646cff' : '#2a2a2a',
                                border: '1px solid #646cff',
                                borderRadius: '6px',
                                color: '#fff',
                                cursor: campaign.status === 'COMPLETED' ? 'default' : 'pointer',
                                opacity: campaign.status === 'COMPLETED' ? 0.7 : 1
                            }}
                        >
                            ✅ Завершена
                        </button>
                    </div>
                )}
            </div>

            {campaign.totalMilestones && (
                <div className="milestones-section">
                    <h2>📊 Прогресс кампании</h2>
                    {isCreator ? (
                        <div className="milestone-details">
                            <p>Вехи: {campaign.completedMilestones} / {campaign.totalMilestones}</p>
                            <div className="progress-bar-large">
                                <div
                                    className="progress-fill"
                                    style={{ width: `${(campaign.completedMilestones / campaign.totalMilestones) * 100}%` }}
                                />
                            </div>
                            <p className="progress-percentage">
                                {Math.round((campaign.completedMilestones / campaign.totalMilestones) * 100)}% завершено
                            </p>
                        </div>
                    ) : (
                        <div className="milestone-details">
                            <div className="progress-bar-large">
                                <div
                                    className="progress-fill"
                                    style={{ width: `${(campaign.completedMilestones / campaign.totalMilestones) * 100}%` }}
                                />
                            </div>
                            <p className="progress-percentage">
                                {Math.round((campaign.completedMilestones / campaign.totalMilestones) * 100)}% завершено
                            </p>
                        </div>
                    )}
                </div>
            )}

            {campaign.players && campaign.players.length > 0 && (
                <div className="players-section">
                    <h2>👥 Постоянные игроки</h2>
                    <div className="players-list">
                        {campaign.players.map(player => (
                            <div key={player.id} className="player-card">
                                <div className="player-name">{player.playerName}</div>
                                {player.characterName && (
                                    <div className="character-info">
                                        <strong>{player.characterName}</strong>
                                        {player.characterClass && ` - ${player.characterClass}`}
                                    </div>
                                )}
                                {player.sessionNumber && (
                                    <div className="player-joined">
                                        Присоединился с {player.sessionNumber} сессии
                                    </div>
                                )}
                                {player.characterNotes && (
                                    <div className="character-notes">{player.characterNotes}</div>
                                )}
                            </div>
                        ))}
                    </div>
                </div>
            )}

            <div className="history-section">
                <h2>📖 История кампании</h2>

                {heldGames.length === 0 && upcomingGames.length === 0 ? (
                    <p className="no-games">Пока нет игр в этой кампании</p>
                ) : (
                    <div className="games-timeline">
                        {heldGames.map((game, index) => (
                            <div key={game.id} className="game-entry held">
                                <div className="game-header">
                                    <span className="session-number">✅ Сессия {index + 1}</span>
                                    <span className="game-date">{formatDate(game.startTime)}</span>
                                </div>
                                <h3 className="game-title">{game.title || 'Без названия'}</h3>
                                {game.keyEvents && (
                                    <div
                                        className="key-events"
                                        dangerouslySetInnerHTML={{
                                            __html: DOMPurify.sanitize(game.keyEvents)
                                        }}
                                    />
                                )}
                            </div>
                        ))}

                        {upcomingGames.map((game, index) => (
                            <div key={game.id} className="game-entry upcoming">
                                <div className="game-header">
                                    <span className="session-number">
                                        📅 Сессия {heldGames.length + index + 1}
                                    </span>
                                    <span className="game-date">{formatDate(game.startTime)}</span>
                                </div>
                                <h3 className="game-title">{game.title || 'Без названия'}</h3>
                                <p className="upcoming-note">Еще не проведена</p>
                            </div>
                        ))}
                    </div>
                )}
            </div>
        </div>
    )
}

export default CampaignDetails
