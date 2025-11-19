import './TabNavigation.css'

const TabNavigation = ({ activeTab, onTabChange }) => {
    const tabs = [
        { id: 'calendar', label: 'Календарь игр', icon: '📅' },
        { id: 'profile', label: 'Личный кабинет', icon: '👤' }
    ]

    return (
        <nav className="tab-navigation">
            {tabs.map(tab => (
                <button
                    key={tab.id}
                    className={`tab-button ${activeTab === tab.id ? 'active' : ''}`}
                    onClick={() => onTabChange(tab.id)}
                >
                    <span className="tab-icon">{tab.icon}</span>
                    <span className="tab-label">{tab.label}</span>
                </button>
            ))}
        </nav>
    )
}

export default TabNavigation
