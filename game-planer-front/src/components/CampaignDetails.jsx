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
    const [showLinkGameModal, setShowLinkGameModal] = useState(false)
    const [availableGames, setAvailableGames] = useState([])
    const [isEditingMilestones, setIsEditingMilestones] = useState(false)
    const [editedMilestones, setEditedMilestones] = useState(0)

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

    console.log('CampaignDetails Debug:', {
        campaignCreatorId: campaign?.creator?.id,
        currentUserId: currentUserId,
        isCreatorCheck: String(campaign?.creator?.id) === String(currentUserId)
    })

    const isCreator = String(campaign?.creator?.id) === String(currentUserId)

    const handleStatusChange = async (newStatus) => {
        try {
            await campaignApi.updateCampaignStatus(campaignId, newStatus)
            loadCampaignDetails()
        } catch (error) {
            console.error('Failed to update status:', error)
            alert('Не удалось изменить статус кампании')
        }
    }

    const handleUpdateMilestones = async () => {
        try {
            await campaignApi.updateMilestones(campaignId, parseInt(editedMilestones))
            setIsEditingMilestones(false)
            loadCampaignDetails()
        } catch (error) {
            console.error('Failed to update milestones:', error)
            alert('Не удалось обновить прогресс')
        }
    }

    const handleLoadAvailableGames = async () => {
        try {
            const myGames = await gameApi.getMyGames()
            // Filter games that are not in any campaign
            const available = myGames.filter(g => !g.campaignId)
            setAvailableGames(available)
            setShowLinkGameModal(true)
        } catch (error) {
            console.error('Failed to load games:', error)
            alert('Не удалось загрузить список игр')
        }
    }

    const handleLinkGame = async (gameId) => {
        try {
            await campaignApi.addGameToCampaign(campaignId, gameId)
            setShowLinkGameModal(false)
            loadCampaignDetails()
        } catch (error) {
            console.error('Failed to link game:', error)
            alert('Не удалось привязать игру')
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
                    <p className="campaign-details-description">{campaign.description}</p>
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
                            <div className="milestone-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                                {isEditingMilestones ? (
                                    <div className="milestone-edit-controls" style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                                        <input
                                            type="number"
                                            min="0"
                                            max={campaign.totalMilestones}
                                            value={editedMilestones}
                                            onChange={(e) => setEditedMilestones(e.target.value)}
                                            style={{ width: '60px', padding: '0.25rem' }}
                                        />
                                        <span>/ {campaign.totalMilestones}</span>
                                        <button onClick={handleUpdateMilestones} className="save-btn" style={{ padding: '0.25rem 0.5rem', background: '#4caf50', border: 'none', borderRadius: '4px', cursor: 'pointer' }}>💾</button>
                                        <button onClick={() => setIsEditingMilestones(false)} className="cancel-btn" style={{ padding: '0.25rem 0.5rem', background: '#f44336', border: 'none', borderRadius: '4px', cursor: 'pointer' }}>❌</button>
                                    </div>
                                ) : (
                                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                                        <p style={{ margin: 0 }}>Вехи: {campaign.completedMilestones} / {campaign.totalMilestones}</p>
                                        <button
                                            onClick={() => {
                                                setEditedMilestones(campaign.completedMilestones)
                                                setIsEditingMilestones(true)
                                            }}
                                            style={{ background: 'none', border: 'none', cursor: 'pointer', fontSize: '1.2rem' }}
                                            title="Изменить прогресс"
                                        >
                                            ✏️
                                        </button>
                                    </div>
                                )}
                            </div>
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
                <div className="history-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
                    <h2>📖 История кампании</h2>
                    {isCreator && (
                        <button
                            onClick={handleLoadAvailableGames}
                            className="link-game-btn"
                            style={{
                                padding: '0.5rem 1rem',
                                background: '#646cff',
                                color: 'white',
                                border: 'none',
                                borderRadius: '6px',
                                cursor: 'pointer',
                                fontSize: '0.9rem'
                            }}
                        >
                            🔗 Привязать игру
                        </button>
                    )}
                </div>

                {showLinkGameModal && (
                    <div className="modal-overlay" style={{
                        position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
                        background: 'rgba(0,0,0,0.7)', display: 'flex', justifyContent: 'center', alignItems: 'center', zIndex: 1000
                    }}>
                        <div className="modal-content" style={{
                            background: '#1a1a1a', padding: '2rem', borderRadius: '8px', width: '90%', maxWidth: '500px', maxHeight: '80vh', overflowY: 'auto'
                        }}>
                            <h3>Привязать существующую игру</h3>
                            {availableGames.length === 0 ? (
                                <p>Нет доступных игр для привязки.</p>
                            ) : (
                                <div className="games-list" style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem', marginTop: '1rem' }}>
                                    {availableGames.map(game => (
                                        <div key={game.id} style={{
                                            padding: '1rem', background: '#2a2a2a', borderRadius: '6px',
                                            display: 'flex', justifyContent: 'space-between', alignItems: 'center'
                                        }}>
                                            <div>
                                                <div style={{ fontWeight: 'bold' }}>{game.title || 'Без названия'}</div>
                                                <div style={{ fontSize: '0.8rem', color: '#888' }}>{formatDate(game.startTime)}</div>
                                            </div>
                                            <button
                                                onClick={() => handleLinkGame(game.id)}
                                                style={{
                                                    padding: '0.25rem 0.75rem', background: '#4caf50', color: 'white',
                                                    border: 'none', borderRadius: '4px', cursor: 'pointer'
                                                }}
                                            >
                                                Выбрать
                                            </button>
                                        </div>
                                    ))}
                                </div>
                            )}
                            <button
                                onClick={() => setShowLinkGameModal(false)}
                                style={{ marginTop: '1rem', padding: '0.5rem 1rem', background: 'transparent', border: '1px solid #666', color: '#fff', borderRadius: '4px', cursor: 'pointer', width: '100%' }}
                            >
                                Отмена
                            </button>
                        </div>
                    </div>
                )}

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
