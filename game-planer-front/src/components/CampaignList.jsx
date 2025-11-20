import { useState, useEffect } from 'react'
import { campaignApi } from '../services/campaignApi'
import './CampaignList.css'

const CampaignList = ({ onSelectCampaign, onCreateCampaign }) => {
    const [campaigns, setCampaigns] = useState([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState(null)

    useEffect(() => {
        loadCampaigns()
    }, [])

    const loadCampaigns = async () => {
        try {
            setLoading(true)
            const data = await campaignApi.getUserCampaigns()
            setCampaigns(data)
            setError(null)
        } catch (err) {
            setError('Не удалось загрузить кампании')
            console.error('Error loading campaigns:', err)
        } finally {
            setLoading(false)
        }
    }

    const getStatusIcon = (status) => {
        switch (status) {
            case 'ACTIVE':
                return '🟢'
            case 'COMPLETED':
                return '✅'
            case 'ON_HOLD':
                return '⏸️'
            default:
                return '❓'
        }
    }

    const getStatusText = (status) => {
        switch (status) {
            case 'ACTIVE':
                return 'Активна'
            case 'COMPLETED':
                return 'Завершена'
            case 'ON_HOLD':
                return 'На паузе'
            default:
                return status
        }
    }

    if (loading) {
        return (
            <div className="campaign-list">
                <div className="campaign-loading">Загрузка кампаний...</div>
            </div>
        )
    }

    if (error) {
        return (
            <div className="campaign-list">
                <div className="campaign-error">{error}</div>
            </div>
        )
    }

    return (
        <div className="campaign-list">
            <div className="campaign-list-header">
                <h2>📚 Кампании</h2>
                <button onClick={onCreateCampaign} className="create-campaign-btn">
                    + Создать кампанию
                </button>
            </div>

            {campaigns.length === 0 ? (
                <div className="no-campaigns">
                    <p>У вас пока нет кампаний</p>
                    <button onClick={onCreateCampaign} className="create-first-campaign-btn">
                        Создать первую кампанию
                    </button>
                </div>
            ) : (
                <div className="campaigns-grid">
                    {campaigns.map(campaign => (
                        <div
                            key={campaign.id}
                            className="campaign-card"
                            onClick={() => onSelectCampaign(campaign.id)}
                            style={{
                                border: campaign.hasInvite ? '2px solid #646cff' : undefined,
                                boxShadow: campaign.hasInvite ? '0 0 10px rgba(100, 108, 255, 0.3)' : undefined
                            }}
                        >
                            <div className="campaign-card-header">
                                <h3>
                                    {campaign.name}
                                    {campaign.isCreator && <span style={{ marginLeft: '8px', fontSize: '0.8rem' }}>👑</span>}
                                    {campaign.isPlayer && <span style={{ marginLeft: '8px', fontSize: '0.8rem' }}>🎭</span>}
                                    {campaign.hasInvite && <span style={{ marginLeft: '8px', fontSize: '0.8rem' }}>✉️</span>}
                                </h3>
                                <span className="campaign-status">
                                    {getStatusIcon(campaign.status)} {getStatusText(campaign.status)}
                                </span>
                            </div>

                            {campaign.description && (
                                <p className="campaign-description">{campaign.description}</p>
                            )}

                            <div className="campaign-stats">
                                <div className="stat">
                                    <span className="stat-label">Завершено:</span>
                                    <span className="stat-value">{campaign.completedGamesCount || 0}</span>
                                </div>

                                <div className="stat">
                                    <span className="stat-label">Запланировано:</span>
                                    <span className="stat-value">{campaign.upcomingGamesCount || 0}</span>
                                </div>

                                {campaign.totalMilestones && (
                                    <div className="stat">
                                        <span className="stat-label">Прогресс:</span>
                                        <span className="stat-value">{campaign.getProgressPercentage || 0}%</span>
                                    </div>
                                )}

                                {campaign.players && campaign.players.length > 0 && (
                                    <div className="stat">
                                        <span className="stat-label">Игроки:</span>
                                        <span className="stat-value">
                                            {campaign.players.map(p => p.playerName).join(', ')}
                                        </span>
                                    </div>
                                )}
                            </div>

                            {campaign.totalMilestones && (
                                <div className="progress-bar">
                                    <div
                                        className="progress-fill"
                                        style={{ width: `${campaign.getProgressPercentage || 0}%` }}
                                    />
                                </div>
                            )}

                            <div className="campaign-creator">
                                Мастер: {campaign.creator?.name}
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    )
}

export default CampaignList
