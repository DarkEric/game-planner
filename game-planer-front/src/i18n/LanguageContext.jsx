import { createContext, useContext, useState, useEffect } from 'react'
import { getTranslation } from './translations'

const LanguageContext = createContext()

export const LanguageProvider = ({ children }) => {
  const [language, setLanguage] = useState(() => {
    // Загружаем язык из localStorage или используем браузерный
    const saved = localStorage.getItem('language')
    if (saved) return saved
    
    // Определяем язык браузера
    const browserLang = navigator.language.split('-')[0]
    return browserLang === 'ru' ? 'ru' : 'en'
  })

  useEffect(() => {
    // Сохраняем выбранный язык
    localStorage.setItem('language', language)
  }, [language])

  const t = (key) => getTranslation(language, key)

  const value = {
    language,
    setLanguage,
    t
  }

  return (
    <LanguageContext.Provider value={value}>
      {children}
    </LanguageContext.Provider>
  )
}

export const useLanguage = () => {
  const context = useContext(LanguageContext)
  if (!context) {
    throw new Error('useLanguage must be used within LanguageProvider')
  }
  return context
}
