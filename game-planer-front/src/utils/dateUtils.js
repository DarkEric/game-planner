/**
 * Утилиты для работы с датами и временем
 * Все функции работают с локальным временем пользователя
 */

/**
 * Парсит строку LocalDateTime из бэкенда в объект Date
 * Бэкенд отправляет даты без timezone (например: "2025-11-15T15:00:00")
 * Мы интерпретируем это как локальное время пользователя
 * 
 * @param {string} dateStr - Строка даты в формате ISO без timezone
 * @returns {Date} - Date объект в локальном времени
 */
export const parseLocalDateTime = (dateStr) => {
  if (!dateStr) {
    console.warn('parseLocalDateTime: пустая строка даты')
    return new Date()
  }
  
  if (typeof dateStr === 'string' && dateStr.includes('T')) {
    try {
      // Парсим строку вручную, чтобы избежать проблем с timezone
      const [datePart, timePart] = dateStr.split('T')
      const [year, month, day] = datePart.split('-').map(Number)
      
      // Парсим время, обрабатывая возможные форматы
      const timeComponents = (timePart || '00:00:00').split(':')
      const hours = Number(timeComponents[0] || 0)
      const minutes = Number(timeComponents[1] || 0)
      // Убираем миллисекунды если есть
      const seconds = Number((timeComponents[2] || '0').split('.')[0])
      
      // Создаем Date в локальном времени пользователя
      const date = new Date(year, month - 1, day, hours, minutes, seconds, 0)
      
      // Проверяем валидность
      if (isNaN(date.getTime())) {
        console.error('parseLocalDateTime: невалидная дата', dateStr)
        return new Date()
      }
      
      return date
    } catch (error) {
      console.error('parseLocalDateTime: ошибка парсинга', dateStr, error)
      return new Date()
    }
  }
  
  // Fallback на стандартный парсинг
  const date = new Date(dateStr)
  if (isNaN(date.getTime())) {
    console.error('parseLocalDateTime: невалидная дата (fallback)', dateStr)
    return new Date()
  }
  return date
}

/**
 * Форматирует Date объект в строку LocalDateTime для отправки на бэкенд
 * Отправляем в формате без timezone (например: "2025-11-15T15:00:00")
 * 
 * @param {Date} date - Date объект
 * @returns {string} - Строка в формате ISO без timezone
 */
export const formatLocalDateTime = (date) => {
  const d = date instanceof Date ? date : new Date(date)
  
  if (isNaN(d.getTime())) {
    console.error('formatLocalDateTime: невалидная дата', date)
    return new Date().toISOString().split('.')[0]
  }
  
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hours = String(d.getHours()).padStart(2, '0')
  const minutes = String(d.getMinutes()).padStart(2, '0')
  const seconds = String(d.getSeconds()).padStart(2, '0')
  
  return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}`
}

/**
 * Получает информацию о часовом поясе пользователя
 * @returns {object} - Информация о timezone
 */
export const getUserTimezone = () => {
  const timezone = Intl.DateTimeFormat().resolvedOptions().timeZone
  const offset = -new Date().getTimezoneOffset() / 60
  const offsetStr = offset >= 0 ? `+${offset}` : `${offset}`
  
  return {
    timezone,
    offset,
    offsetStr: `UTC${offsetStr}`
  }
}

/**
 * Форматирует дату для отображения пользователю
 * @param {Date} date - Date объект
 * @param {object} options - Опции форматирования
 * @returns {string} - Отформатированная строка
 */
export const formatDisplayDate = (date, options = {}) => {
  const d = date instanceof Date ? date : new Date(date)
  
  const defaultOptions = {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    ...options
  }
  
  return d.toLocaleString('ru-RU', defaultOptions)
}

/**
 * Проверяет, находятся ли две даты в одном дне
 * @param {Date} date1 
 * @param {Date} date2 
 * @returns {boolean}
 */
export const isSameDay = (date1, date2) => {
  return date1.toDateString() === date2.toDateString()
}

/**
 * Логирует информацию о дате для отладки
 * @param {string} label - Метка для лога
 * @param {Date} date - Date объект
 */
export const debugDate = (label, date) => {
  const tz = getUserTimezone()
  console.log(`[${label}]`, {
    date: date.toString(),
    iso: date.toISOString(),
    local: formatLocalDateTime(date),
    display: formatDisplayDate(date),
    timezone: tz
  })
}
