import './ProfileTab.css'
import TimezoneSelector from './TimezoneSelector'
import InviteManager from './InviteManager'
import TelegramNotificationsSettings from './TelegramNotificationsSettings'
import { useLanguage } from '../i18n/LanguageContext'

const ProfileTab = ({ currentPlayer, onUpdateProfile }) => {
    const { t } = useLanguage()

    if (!currentPlayer) {
        return (
            <div className="profile-tab">
                <div className="profile-loading">Загрузка профиля...</div>
            </div>
        )
    }

    return (
        <div className="profile-tab">
            <div className="profile-container">
                <div className="profile-header">
                    <div className="profile-avatar" style={{ backgroundColor: currentPlayer.color }}>
                        {currentPlayer.name.charAt(0).toUpperCase()}
                    </div>
                    <h2 className="profile-title">Личный кабинет</h2>
                </div>

                <div className="profile-sections">
                    {/* Основные настройки */}
                    <section className="profile-section">
                        <h3 className="section-title">
                            <span className="section-icon">⚙️</span>
                            Основные настройки
                        </h3>
                        <div className="profile-fields">
                            <div className="profile-field">
                                <label className="field-label">{t('name')}:</label>
                                <input
                                    type="text"
                                    value={currentPlayer.name}
                                    onChange={(e) => {
                                        const newName = e.target.value
                                        onUpdateProfile(newName, currentPlayer.color, currentPlayer.timezone)
                                    }}
                                    className="field-input"
                                    placeholder="Введите ваше имя"
                                />
                            </div>

                            <div className="profile-field">
                                <label className="field-label">{t('color')}:</label>
                                <div className="color-picker-wrapper">
                                    <input
                                        type="color"
                                        value={currentPlayer.color}
                                        onChange={(e) => {
                                            const newColor = e.target.value
                                            onUpdateProfile(currentPlayer.name, newColor, currentPlayer.timezone)
                                        }}
                                        className="field-color-input"
                                    />
                                    <span className="color-preview" style={{ backgroundColor: currentPlayer.color }}>
                                        {currentPlayer.color}
                                    </span>
                                </div>
                            </div>
                        </div>
                    </section>

                    {/* Часовой пояс */}
                    <section className="profile-section">
                        <h3 className="section-title">
                            <span className="section-icon">🌍</span>
                            Часовой пояс
                        </h3>
                        <div className="timezone-wrapper">
                            <TimezoneSelector
                                currentTimezone={currentPlayer.timezone}
                                onTimezoneChange={(timezone) =>
                                    onUpdateProfile(currentPlayer.name, currentPlayer.color, timezone)
                                }
                            />
                        </div>
                    </section>

                    {/* Приглашения */}
                    <section className="profile-section">
                        <h3 className="section-title">
                            <span className="section-icon">✉️</span>
                            Управление приглашениями
                        </h3>
                        <div className="invite-wrapper">
                            <InviteManager />
                        </div>
                    </section>

                    {/* Telegram уведомления */}
                    <section className="profile-section">
                        <h3 className="section-title">
                            <span className="section-icon">📱</span>
                            Telegram уведомления
                        </h3>
                        <TelegramNotificationsSettings userTimezone={currentPlayer.timezone} />
                    </section>
                </div>
            </div>
        </div>
    )
}

export default ProfileTab
