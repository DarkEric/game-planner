import { useState } from 'react'
import './PlayerManager.css'

const PlayerManager = ({ players, onAddPlayer, onRemovePlayer, onUpdatePlayer }) => {
  const [newPlayerName, setNewPlayerName] = useState('')
  const [newPlayerColor, setNewPlayerColor] = useState('#646cff')

  const defaultColors = [
    '#646cff', '#42b883', '#ff6b6b', '#ffa500', 
    '#9b59b6', '#3498db', '#e74c3c', '#1abc9c'
  ]

  const handleAddPlayer = () => {
    if (newPlayerName.trim()) {
      onAddPlayer({
        id: Date.now(),
        name: newPlayerName.trim(),
        color: newPlayerColor,
        availableTimes: []
      })
      setNewPlayerName('')
      setNewPlayerColor(defaultColors[players.length % defaultColors.length])
    }
  }

  const handleKeyPress = (e) => {
    if (e.key === 'Enter') {
      handleAddPlayer()
    }
  }

  return (
    <div className="player-manager">
      <h2>Игроки</h2>
      
      <div className="add-player-section">
        <input
          type="text"
          placeholder="Имя игрока"
          value={newPlayerName}
          onChange={(e) => setNewPlayerName(e.target.value)}
          onKeyPress={handleKeyPress}
          className="player-name-input"
        />
        <input
          type="color"
          value={newPlayerColor}
          onChange={(e) => setNewPlayerColor(e.target.value)}
          className="player-color-input"
          title="Выберите цвет игрока"
        />
        <button onClick={handleAddPlayer} className="add-player-button">
          + Добавить
        </button>
      </div>

      <div className="players-list">
        {players.length === 0 ? (
          <p className="no-players">Добавьте игроков для начала планирования</p>
        ) : (
          players.map(player => (
            <div key={player.id} className="player-item">
              <div className="player-info">
                <div 
                  className="player-color-indicator"
                  style={{ backgroundColor: player.color }}
                />
                <span className="player-name">{player.name}</span>
              </div>
              <button
                onClick={() => onRemovePlayer(player.id)}
                className="remove-player-button"
                title="Удалить игрока"
              >
                ×
              </button>
            </div>
          ))
        )}
      </div>
    </div>
  )
}

export default PlayerManager

