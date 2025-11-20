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
    const [editedCompletedMilestones, setEditedCompletedMilestones] = useState(0)
    const [editedTotalMilestones, setEditedTotalMilestones] = useState(0)
    const [showInviteModal, setShowInviteModal] = useState(false)
    const [availablePlayers, setAvailablePlayers] = useState([])
    const [showAcceptInviteModal, setShowAcceptInviteModal] = useState(false)
    const [characterName, setCharacterName] = useState('')
    const [characterClass, setCharacterClass] = useState('')
    const [characterNotes, setCharacterNotes] = useState('')
    const [expandedPlayers, setExpandedPlayers] = useState(new Set())
    const [isEditingCampaign, setIsEditingCampaign] = useState(false)
    const [editedName, setEditedName] = useState('')
    const [editedDescription, setEditedDescription] = useState('')

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
            const completed = parseInt(editedCompletedMilestones)
            const total = parseInt(editedTotalMilestones)
            
            if (completed > total) {
                alert('Количество пройденных вех не может быть больше общего количества')
                return
            }
            
            await campaignApi.updateMilestones(campaignId, completed, total)
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

    const handleLoadAvailablePlayers = async () => {
        try {
            // Load all users except those already in campaign
            const response = await fetch('/api/players', {
                headers: {
                    'Authorization': `Bearer ${localStorage.getItem('authToken')}`
                }
            })
            const allPlayers = await response.json()
            
            // Filter out players already in campaign and the creator
            const playerIds = campaign.players.map(p => p.playerId)
            const available = allPlayers.filter(p => 
                p.id !== campaign.creator.id && !playerIds.includes(p.id)
            )
            setAvailablePlayers(available)
            setShowInviteModal(true)
        } catch (error) {
            console.error('Failed to load players:', error)
            alert('Не удалось загрузить список игроков')
        }
    }

    const handleInvitePlayer = async (playerId) => {
        try {
            await campaignApi.invitePlayerToCampaign(campaignId, playerId)
            setShowInviteModal(false)
            alert('Приглашение отправлено!')
        } catch (error) {
            console.error('Failed to invite player:', error)
            alert('Не удалось отправить приглашение')
        }
    }

    const handleAcceptInvite = async () => {
        try {
            // Find the invite for this campaign
            const invites = await campaignApi.getPendingInvites()
            const invite = invites.find(inv => inv.campaignId === campaignId)
            
            if (!invite) {
                alert('Приглашение не найдено')
                return
            }

            await campaignApi.acceptCampaignInvite(invite.id, {
                characterName,
                characterClass,
                characterNotes
            })
            setShowAcceptInviteModal(false)
            loadCampaignDetails()
        } catch (error) {
            console.error('Failed to accept invite:', error)
            alert('Не удалось принять приглашение')
        }
    }

    const handleDeclineInvite = async () => {
        try {
            const invites = await campaignApi.getPendingInvites()
            const invite = invites.find(inv => inv.campaignId === campaignId)
            
            if (!invite) {
                alert('Приглашение не найдено')
                return
            }

            await campaignApi.declineCampaignInvite(invite.id)
            loadCampaignDetails()
        } catch (error) {
            console.error('Failed to decline invite:', error)
            alert('Не удалось отклонить приглашение')
        }
    }

    const handleUpdateCampaign = async () => {
        try {
            if (!editedName.trim()) {
                alert('Название кампании не может быть пустым')
                return
            }
            await campaignApi.updateCampaign(campaignId, editedName, editedDescription)
            setIsEditingCampaign(false)
            loadCampaignDetails()
        } catch (error) {
            console.error('Failed to update campaign:', error)
            alert('Не удалось обновить кампанию')
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

            {campaign.hasInvite && (
                <div style={{
                    background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                    padding: '1.5rem',
                    borderRadius: '8px',
                    marginBottom: '1rem',
                    color: '#fff'
                }}>
                    <h3 style={{ margin: '0 0 0.5rem 0' }}>✉️ Вы приглашены в эту кампанию!</h3>
                    <p style={{ margin: '0 0 1rem 0' }}>Мастер {campaign.creator?.name} приглашает вас присоединиться к кампании.</p>
                    <div style={{ display: 'flex', gap: '0.5rem' }}>
                        <button
                            onClick={() => setShowAcceptInviteModal(true)}
                            style={{
                                padding: '0.5rem 1rem',
                                background: '#4caf50',
                                border: 'none',
                                borderRadius: '6px',
                                color: '#fff',
                                cursor: 'pointer',
                                fontWeight: 'bold'
                            }}
                        >
                            Принять приглашение
                        </button>
                        <button
                            onClick={handleDeclineInvite}
                            style={{
                                padding: '0.5rem 1rem',
                                background: 'transparent',
                                border: '1px solid #fff',
                                borderRadius: '6px',
                                color: '#fff',
                                cursor: 'pointer'
                            }}
                        >
                            Отклонить
                        </button>
                    </div>
                </div>
            )}

            <div className="campaign-header">
                <div className="campaign-title-section">
                    {isEditingCampaign ? (
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem', flex: 1 }}>
                            <input
                                type="text"
                                value={editedName}
                                onChange={(e) => setEditedName(e.target.value)}
                                placeholder="Название кампании"
                                style={{
                                    fontSize: '1.5rem',
                                    fontWeight: 'bold',
                                    padding: '0.5rem',
                                    background: '#2a2a2a',
                                    color: '#fff',
                                    border: '1px solid #444',
                                    borderRadius: '4px'
                                }}
                            />
                            <textarea
                                value={editedDescription}
                                onChange={(e) => setEditedDescription(e.target.value)}
                                placeholder="Описание кампании (необязательно)"
                                style={{
                                    padding: '0.5rem',
                                    background: '#2a2a2a',
                                    color: '#fff',
                                    border: '1px solid #444',
                                    borderRadius: '4px',
                                    minHeight: '80px',
                                    resize: 'vertical'
                                }}
                            />
                            <div style={{ display: 'flex', gap: '0.5rem' }}>
                                <button
                                    onClick={handleUpdateCampaign}
                                    style={{
                                        padding: '0.5rem 1rem',
                                        background: '#4caf50',
                                        color: '#fff',
                                        border: 'none',
                                        borderRadius: '4px',
                                        cursor: 'pointer'
                                    }}
                                >
                                    💾 Сохранить
                                </button>
                                <button
                                    onClick={() => setIsEditingCampaign(false)}
                                    style={{
                                        padding: '0.5rem 1rem',
                                        background: '#f44336',
                                        color: '#fff',
                                        border: 'none',
                                        borderRadius: '4px',
                                        cursor: 'pointer'
                                    }}
                                >
                                    ❌ Отмена
                                </button>
                            </div>
                        </div>
                    ) : (
                        <>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                                <h1>{campaign.name}</h1>
                                {isCreator && (
                                    <button
                                        onClick={() => {
                                            setEditedName(campaign.name)
                                            setEditedDescription(campaign.description || '')
                                            setIsEditingCampaign(true)
                                        }}
                                        style={{
                                            background: 'none',
                                            border: 'none',
                                            cursor: 'pointer',
                                            fontSize: '1.2rem',
                                            padding: '0.25rem'
                                        }}
                                        title="Редактировать кампанию"
                                    >
                                        ✏️
                                    </button>
                                )}
                            </div>
                            <span className="campaign-status-badge">
                                {getStatusIcon(campaign.status)} {getStatusText(campaign.status)}
                            </span>
                        </>
                    )}
                </div>

                {!isEditingCampaign && campaign.description && (
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

            {(campaign.totalMilestones || isCreator) && (
                <div className="milestones-section">
                    <h2>📊 Прогресс кампании</h2>
                    {isCreator && !campaign.totalMilestones && !isEditingMilestones ? (
                        <div className="milestone-details">
                            <p style={{ color: '#888', marginBottom: '1rem' }}>
                                Вехи не настроены. Установите количество вех для отслеживания прогресса кампании.
                            </p>
                            <button
                                onClick={() => {
                                    setEditedCompletedMilestones(0)
                                    setEditedTotalMilestones(10)
                                    setIsEditingMilestones(true)
                                }}
                                style={{
                                    padding: '0.5rem 1rem',
                                    background: '#646cff',
                                    color: '#fff',
                                    border: 'none',
                                    borderRadius: '6px',
                                    cursor: 'pointer'
                                }}
                            >
                                Настроить вехи
                            </button>
                        </div>
                    ) : isCreator ? (
                        <div className="milestone-details">
                            <div className="milestone-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                                {isEditingMilestones ? (
                                    <div className="milestone-edit-controls" style={{ display: 'flex', gap: '0.5rem', alignItems: 'center', flexWrap: 'wrap' }}>
                                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
                                            <span style={{ fontSize: '0.9rem', color: '#aaa' }}>Пройдено:</span>
                                            <input
                                                type="number"
                                                min="0"
                                                max={editedTotalMilestones}
                                                value={editedCompletedMilestones}
                                                onChange={(e) => setEditedCompletedMilestones(e.target.value)}
                                                style={{ width: '60px', padding: '0.25rem', background: '#2a2a2a', color: '#fff', border: '1px solid #444', borderRadius: '4px' }}
                                            />
                                        </div>
                                        <span style={{ color: '#666' }}>/</span>
                                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
                                            <span style={{ fontSize: '0.9rem', color: '#aaa' }}>Всего:</span>
                                            <input
                                                type="number"
                                                min="1"
                                                value={editedTotalMilestones}
                                                onChange={(e) => setEditedTotalMilestones(e.target.value)}
                                                style={{ width: '60px', padding: '0.25rem', background: '#2a2a2a', color: '#fff', border: '1px solid #444', borderRadius: '4px' }}
                                            />
                                        </div>
                                        <button onClick={handleUpdateMilestones} className="save-btn" style={{ padding: '0.25rem 0.5rem', background: '#4caf50', border: 'none', borderRadius: '4px', cursor: 'pointer', color: '#fff' }}>💾</button>
                                        <button onClick={() => setIsEditingMilestones(false)} className="cancel-btn" style={{ padding: '0.25rem 0.5rem', background: '#f44336', border: 'none', borderRadius: '4px', cursor: 'pointer', color: '#fff' }}>❌</button>
                                    </div>
                                ) : (
                                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                                        <p style={{ margin: 0 }}>Вехи: {campaign.completedMilestones} / {campaign.totalMilestones}</p>
                                        <button
                                            onClick={() => {
                                                setEditedCompletedMilestones(campaign.completedMilestones)
                                                setEditedTotalMilestones(campaign.totalMilestones)
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
                            <div className="milestone-header" style={{ marginBottom: '0.5rem' }}>
                                <p style={{ margin: 0 }}>Вехи: {campaign.completedMilestones} / {campaign.totalMilestones}</p>
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
                    )}
                </div>
            )}

            {((campaign.players && campaign.players.length > 0) || isCreator) && (
                <div className="players-section">
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
                        <h2 style={{ margin: 0 }}>👥 Постоянные игроки</h2>
                        {isCreator && (
                            <button
                                onClick={handleLoadAvailablePlayers}
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
                                + Пригласить игрока
                            </button>
                        )}
                    </div>
                    {campaign.players && campaign.players.length > 0 ? (
                        <div className="players-list">
                        {campaign.players.map(player => {
                            const isExpanded = expandedPlayers.has(player.id)
                            const toggleExpanded = () => {
                                const newExpanded = new Set(expandedPlayers)
                                if (isExpanded) {
                                    newExpanded.delete(player.id)
                                } else {
                                    newExpanded.add(player.id)
                                }
                                setExpandedPlayers(newExpanded)
                            }

                            return (
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
                                        <div style={{ marginTop: '0.5rem' }}>
                                            <button
                                                onClick={toggleExpanded}
                                                style={{
                                                    background: 'none',
                                                    border: 'none',
                                                    color: '#646cff',
                                                    cursor: 'pointer',
                                                    padding: '0',
                                                    fontSize: '0.9rem',
                                                    textDecoration: 'underline',
                                                    marginBottom: '0.5rem'
                                                }}
                                            >
                                                {isExpanded ? '▼ Скрыть предысторию' : '▶ Показать предысторию'}
                                            </button>
                                            {isExpanded && (
                                                <div className="character-notes" style={{
                                                    marginTop: '0.5rem',
                                                    padding: '0.75rem',
                                                    background: '#2a2a2a',
                                                    borderRadius: '4px',
                                                    color: '#ccc',
                                                    fontSize: '0.9rem',
                                                    lineHeight: '1.5',
                                                    whiteSpace: 'pre-wrap'
                                                }}>
                                                    {player.characterNotes}
                                                </div>
                                            )}
                                        </div>
                                    )}
                                </div>
                            )
                        })}
                        </div>
                    ) : (
                        <p style={{ color: '#888' }}>Пока нет постоянных игроков</p>
                    )}
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

            {/* Invite Player Modal */}
            {showInviteModal && (
                <div className="modal-overlay" style={{
                    position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
                    background: 'rgba(0,0,0,0.7)', display: 'flex', justifyContent: 'center', alignItems: 'center', zIndex: 1000
                }}>
                    <div className="modal-content" style={{
                        background: '#1a1a1a', padding: '2rem', borderRadius: '8px', width: '90%', maxWidth: '500px', maxHeight: '80vh', overflowY: 'auto'
                    }}>
                        <h3>Пригласить игрока в кампанию</h3>
                        {availablePlayers.length === 0 ? (
                            <p>Нет доступных игроков для приглашения.</p>
                        ) : (
                            <div className="players-list" style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem', marginTop: '1rem' }}>
                                {availablePlayers.map(player => (
                                    <div key={player.id} style={{
                                        padding: '1rem', background: '#2a2a2a', borderRadius: '6px',
                                        display: 'flex', justifyContent: 'space-between', alignItems: 'center'
                                    }}>
                                        <div>
                                            <div style={{ fontWeight: 'bold' }}>{player.name}</div>
                                        </div>
                                        <button
                                            onClick={() => handleInvitePlayer(player.id)}
                                            style={{
                                                padding: '0.25rem 0.75rem', background: '#646cff', color: 'white',
                                                border: 'none', borderRadius: '4px', cursor: 'pointer'
                                            }}
                                        >
                                            Пригласить
                                        </button>
                                    </div>
                                ))}
                            </div>
                        )}
                        <button
                            onClick={() => setShowInviteModal(false)}
                            style={{ marginTop: '1rem', padding: '0.5rem 1rem', background: 'transparent', border: '1px solid #666', color: '#fff', borderRadius: '4px', cursor: 'pointer', width: '100%' }}
                        >
                            Отмена
                        </button>
                    </div>
                </div>
            )}

            {/* Accept Invite Modal */}
            {showAcceptInviteModal && (
                <div className="modal-overlay" style={{
                    position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
                    background: 'rgba(0,0,0,0.7)', display: 'flex', justifyContent: 'center', alignItems: 'center', zIndex: 1000
                }}>
                    <div className="modal-content" style={{
                        background: '#1a1a1a', padding: '2rem', borderRadius: '8px', width: '90%', maxWidth: '500px'
                    }}>
                        <h3>Присоединиться к кампании</h3>
                        <p style={{ color: '#aaa', marginBottom: '1rem' }}>Расскажите о своем персонаже:</p>
                        
                        <div style={{ marginBottom: '1rem' }}>
                            <label style={{ display: 'block', marginBottom: '0.5rem', color: '#ccc' }}>
                                Имя персонажа *
                            </label>
                            <input
                                type="text"
                                value={characterName}
                                onChange={(e) => setCharacterName(e.target.value)}
                                placeholder="Например: Арагорн"
                                style={{
                                    width: '100%', padding: '0.5rem', background: '#2a2a2a', color: '#fff',
                                    border: '1px solid #444', borderRadius: '4px'
                                }}
                            />
                        </div>

                        <div style={{ marginBottom: '1rem' }}>
                            <label style={{ display: 'block', marginBottom: '0.5rem', color: '#ccc' }}>
                                Класс/Роль (необязательно)
                            </label>
                            <input
                                type="text"
                                value={characterClass}
                                onChange={(e) => setCharacterClass(e.target.value)}
                                placeholder="Например: Воин, Маг"
                                style={{
                                    width: '100%', padding: '0.5rem', background: '#2a2a2a', color: '#fff',
                                    border: '1px solid #444', borderRadius: '4px'
                                }}
                            />
                        </div>

                        <div style={{ marginBottom: '1rem' }}>
                            <label style={{ display: 'block', marginBottom: '0.5rem', color: '#ccc' }}>
                                Заметки о персонаже (необязательно)
                            </label>
                            <textarea
                                value={characterNotes}
                                onChange={(e) => setCharacterNotes(e.target.value)}
                                placeholder="Краткая предыстория, особенности..."
                                style={{
                                    width: '100%', padding: '0.5rem', background: '#2a2a2a', color: '#fff',
                                    border: '1px solid #444', borderRadius: '4px', minHeight: '80px', resize: 'vertical'
                                }}
                            />
                        </div>

                        <div style={{ display: 'flex', gap: '0.5rem', marginTop: '1.5rem' }}>
                            <button
                                onClick={handleAcceptInvite}
                                disabled={!characterName}
                                style={{
                                    flex: 1, padding: '0.75rem', background: characterName ? '#4caf50' : '#555',
                                    color: '#fff', border: 'none', borderRadius: '4px',
                                    cursor: characterName ? 'pointer' : 'not-allowed', fontWeight: 'bold'
                                }}
                            >
                                Присоединиться
                            </button>
                            <button
                                onClick={() => setShowAcceptInviteModal(false)}
                                style={{
                                    flex: 1, padding: '0.75rem', background: 'transparent',
                                    border: '1px solid #666', color: '#fff', borderRadius: '4px', cursor: 'pointer'
                                }}
                            >
                                Отмена
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    )
}

export default CampaignDetails
