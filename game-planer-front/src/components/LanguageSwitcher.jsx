import { useLanguage } from '../i18n/LanguageContext'
import './LanguageSwitcher.css'

const LanguageSwitcher = () => {
  const { language, setLanguage } = useLanguage()

  return (
    <div className="language-switcher">
      <button
        className={`lang-button ${language === 'ru' ? 'active' : ''}`}
        onClick={() => setLanguage('ru')}
        title="Русский"
      >
        RU
      </button>
      <button
        className={`lang-button ${language === 'en' ? 'active' : ''}`}
        onClick={() => setLanguage('en')}
        title="English"
      >
        EN
      </button>
    </div>
  )
}

export default LanguageSwitcher
