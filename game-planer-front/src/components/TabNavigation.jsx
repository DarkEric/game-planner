import { useLanguage } from '../i18n/LanguageContext'
import './TabNavigation.css'

const TabNavigation = ({ activeTab, onTabChange, isAdmin = false }) => {
  const { t } = useLanguage()
  const tabs = [
    { id: 'games', label: t('tabGames'), shortLabel: t('tabGames'), icon: '🎮', aria: t('tabGamesAria') },
    { id: 'calendar', label: t('tabCalendar'), shortLabel: t('tabCalendar'), icon: '📅', aria: t('tabCalendarAria') },
    { id: 'campaigns', label: t('tabCampaigns'), shortLabel: t('tabCampaigns'), icon: '📚', aria: t('tabCampaignsAria') },
    { id: 'profile', label: t('tabProfile'), shortLabel: t('tabProfile'), icon: '👤', aria: t('tabProfileAria') }
  ]

  if (isAdmin) {
    tabs.push({
      id: 'admin',
      label: t('tabAdmin'),
      shortLabel: t('tabAdmin'),
      icon: '⚙️',
      aria: t('tabAdminAria')
    })
  }

  return (
    <nav className="tab-navigation" aria-label={t('tabNavAria')}>
      {tabs.map(tab => (
        <button
          key={tab.id}
          type="button"
          className={`tab-button ${activeTab === tab.id ? 'active' : ''}`}
          onClick={() => onTabChange(tab.id)}
          aria-label={tab.aria}
          title={tab.label}
        >
          <span className="tab-icon" aria-hidden>{tab.icon}</span>
          <span className="tab-label">{tab.label}</span>
        </button>
      ))}
    </nav>
  )
}

export default TabNavigation
