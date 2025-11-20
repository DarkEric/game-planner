import { useState, useEffect } from 'react'
import { useLanguage } from '../i18n/LanguageContext'
import './PlayerTooltip.css'

const PlayerTooltip = ({ players, visible, position, title }) => {
  const { t } = useLanguage()
  const displayTitle = title || t('available')
  if (!visible || !players || players.length === 0) return null

  return (
    <div 
      className="player-tooltip"
      style={{
        left: `${position.x}px`,
        top: `${position.y}px`
      }}
    >
      <div className="player-tooltip-header">
        {displayTitle} ({players.length})
      </div>
      <div className="player-tooltip-list">
        {players.map(player => (
          <div key={player.id} className="player-tooltip-item">
            <div 
              className="player-tooltip-color"
              style={{ backgroundColor: player.color }}
            />
            <span className="player-tooltip-name">{player.name}</span>
          </div>
        ))}
      </div>
    </div>
  )
}

export default PlayerTooltip
