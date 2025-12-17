import './TabNavigation.css'

const TabNavigation = ({ activeTab, onTabChange, isAdmin = false }) => {
    const tabs = [
        { id: 'calendar', label: 'Календарь игр', icon: '📅' },
        { id: 'campaigns', label: 'Кампании', icon: '📚' },
        { id: 'profile', label: 'Личный кабинет', icon: '👤' }
    ]

    if (isAdmin) {
        tabs.push({ id: 'admin', label: 'Админ-панель', icon: '⚙️' })
    }

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
