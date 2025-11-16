/**
 * Утилиты для работы с часовыми поясами и UTC
 * 
 * Архитектура:
 * - Сервер хранит время в UTC (как LocalDateTime без timezone)
 * - Фронтенд конвертирует между локальным временем пользователя и UTC
 * - Каждый пользователь видит время в своем часовом поясе
 */

/**
 * Конвертирует "наивное" локальное время в UTC
 * 
 * ВАЖНО: localDate - это Date объект, компоненты которого (год, месяц, день, час)
 * нужно интерпретировать как время в userTimezone (не в timezone браузера!)
 * 
 * Пример: 
 * - localDate имеет компоненты: 2024-01-15 01:00
 * - userTimezone = "Europe/Moscow" (UTC+3)
 * - Результат: 2024-01-14T22:00:00Z (UTC)
 * 
 * @param {Date} localDate - Date объект с "наивными" компонентами времени
 * @param {string} userTimezone - IANA timezone (например, "Europe/Moscow")
 * @returns {Date} - Дата в UTC
 */
export const convertLocalToUTC = (localDate, userTimezone) => {
  if (!userTimezone) {
    userTimezone = Intl.DateTimeFormat().resolvedOptions().timeZone
  }
  
  // Получаем "наивные" компоненты
  const year = localDate.getFullYear()
  const month = localDate.getMonth()
  const day = localDate.getDate()
  const hours = localDate.getHours()
  const minutes = localDate.getMinutes()
  const seconds = localDate.getSeconds()
  
  // Используем Intl.DateTimeFormat для получения offset
  // Создаем дату в UTC с этими компонентами
  const utcDate = new Date(Date.UTC(year, month, day, hours, minutes, seconds))
  
  // Форматируем эту дату в целевом timezone
  const formatter = new Intl.DateTimeFormat('en-US', {
    timeZone: userTimezone,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false
  })
  
  const parts = formatter.formatToParts(utcDate)
  const tzComponents = {}
  parts.forEach(part => {
    if (part.type !== 'literal') {
      tzComponents[part.type] = parseInt(part.value)
    }
  })
  
  // Создаем дату с компонентами из timezone (в UTC)
  const tzDate = new Date(Date.UTC(
    tzComponents.year,
    tzComponents.month - 1,
    tzComponents.day,
    tzComponents.hour,
    tzComponents.minute,
    tzComponents.second
  ))
  
  // Вычисляем offset
  const offset = tzDate.getTime() - utcDate.getTime()
  
  // Применяем offset
  // Если мы хотим чтобы компоненты (year, month, day, hours) представляли время в userTimezone,
  // то нужно вычесть offset из UTC даты
  return new Date(utcDate.getTime() - offset)
}

/**
 * Конвертирует UTC время в локальное время пользователя
 * 
 * ВАЖНО: Создает Date объект, где компоненты (getHours(), getDate() и т.д.)
 * соответствуют времени в userTimezone, НО интерпретируются как время в timezone браузера.
 * 
 * Это означает, что если userTimezone != timezone браузера, то внутренний UTC timestamp
 * будет неправильным. Но для отображения в календаре это работает, потому что
 * календарь использует getHours(), getDate() и т.д.
 * 
 * Пример: utcDate = 2024-01-14 22:00 UTC, userTimezone = "Europe/Moscow" (UTC+3)
 * Результат: Date с компонентами 2024-01-15 01:00 (но timestamp может быть неправильным)
 * 
 * @param {Date} utcDate - Дата в UTC
 * @param {string} userTimezone - IANA timezone пользователя
 * @returns {Date} - Дата с компонентами в локальном времени пользователя
 */
export const convertUTCToLocal = (utcDate, userTimezone) => {
  if (!userTimezone) {
    userTimezone = Intl.DateTimeFormat().resolvedOptions().timeZone
  }
  
  // Форматируем UTC дату в указанном timezone
  const formatter = new Intl.DateTimeFormat('en-US', {
    timeZone: userTimezone,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false
  })
  
  const parts = formatter.formatToParts(utcDate)
  const components = {}
  parts.forEach(part => {
    if (part.type !== 'literal') {
      components[part.type] = parseInt(part.value)
    }
  })
  
  // Создаем новую дату с этими компонентами
  // ВАЖНО: JavaScript Date интерпретирует эти компоненты как время в timezone браузера!
  // Если timezone браузера != userTimezone, то внутренний timestamp будет неправильным
  return new Date(
    components.year,
    components.month - 1,
    components.day,
    components.hour,
    components.minute,
    components.second
  )
}

/**
 * Получает offset в миллисекундах для указанного timezone
 * 
 * @param {string} timezone - IANA timezone
 * @param {Date} date - Дата для которой нужен offset (учитывает DST)
 * @returns {number} - Offset в миллисекундах
 */
export const getTimezoneOffset = (timezone, date = new Date()) => {
  // Получаем дату в UTC
  const utcDate = new Date(date.toLocaleString('en-US', { timeZone: 'UTC' }))
  
  // Получаем дату в указанном timezone
  const tzDate = new Date(date.toLocaleString('en-US', { timeZone: timezone }))
  
  // Разница и есть offset
  return tzDate.getTime() - utcDate.getTime()
}

/**
 * Форматирует дату для отправки на сервер (UTC в формате ISO-8601)
 * 
 * @param {Date} localDate - Дата в локальном времени пользователя
 * @param {string} userTimezone - IANA timezone пользователя
 * @returns {string} - Строка в формате ISO-8601 UTC (например, "2024-01-15T12:00:00Z")
 */
export const formatForServer = (localDate, userTimezone) => {
  console.log('formatForServer input:', {
    localDate: localDate.toString(),
    components: {
      year: localDate.getFullYear(),
      month: localDate.getMonth() + 1,
      day: localDate.getDate(),
      hours: localDate.getHours(),
      minutes: localDate.getMinutes()
    },
    userTimezone
  })
  
  const utcDate = convertLocalToUTC(localDate, userTimezone)
  const result = utcDate.toISOString()
  
  console.log('formatForServer output:', result)
  
  return result
}

/**
 * Парсит дату от сервера (UTC в формате ISO-8601) в локальное время пользователя
 * 
 * ВАЖНО: Теперь просто возвращаем Date объект с правильным UTC timestamp.
 * JavaScript Date автоматически конвертирует в локальное время браузера при вызове getHours() и т.д.
 * 
 * @param {string} serverDateStr - Строка даты от сервера (ISO-8601 UTC, например "2024-01-15T12:00:00Z")
 * @param {string} userTimezone - IANA timezone пользователя (не используется, т.к. используем timezone браузера)
 * @returns {Date} - Дата с правильным UTC timestamp
 */
export const parseFromServer = (serverDateStr, userTimezone) => {
  if (!serverDateStr) {
    console.warn('parseFromServer: пустая строка даты')
    return new Date()
  }
  
  try {
    // Парсим ISO-8601 строку как UTC дату
    // Date объект автоматически хранит правильный UTC timestamp
    // и конвертирует в локальное время браузера при вызове getHours(), getDate() и т.д.
    const utcDate = new Date(serverDateStr)
    
    if (isNaN(utcDate.getTime())) {
      throw new Error('Invalid date')
    }
    
    // Отладочный вывод (раскомментируйте при необходимости)
    // console.log('parseFromServer:', {
    //   input: serverDateStr,
    //   output: utcDate.toString(),
    //   outputISO: utcDate.toISOString(),
    //   hours: utcDate.getHours(),
    //   browserTimezone: Intl.DateTimeFormat().resolvedOptions().timeZone
    // })
    
    // Просто возвращаем Date с правильным UTC timestamp
    // Браузер автоматически покажет время в своем timezone
    return utcDate
  } catch (error) {
    console.error('parseFromServer: ошибка парсинга', serverDateStr, error)
    return new Date()
  }
}

/**
 * Получает информацию о timezone пользователя
 * 
 * @param {string} timezone - IANA timezone (опционально)
 * @returns {object} - Информация о timezone
 */
export const getTimezoneInfo = (timezone = null) => {
  const tz = timezone || Intl.DateTimeFormat().resolvedOptions().timeZone
  const now = new Date()
  const offset = getTimezoneOffset(tz, now) / (1000 * 60 * 60) // В часах
  const offsetStr = offset >= 0 ? `+${offset}` : `${offset}`
  
  return {
    timezone: tz,
    offset,
    offsetStr: `UTC${offsetStr}`,
    isDST: isDaylightSavingTime(now, tz)
  }
}

/**
 * Проверяет, действует ли летнее время
 * 
 * @param {Date} date - Дата для проверки
 * @param {string} timezone - IANA timezone
 * @returns {boolean} - true если летнее время
 */
export const isDaylightSavingTime = (date, timezone) => {
  const january = new Date(date.getFullYear(), 0, 1)
  const july = new Date(date.getFullYear(), 6, 1)
  
  const janOffset = getTimezoneOffset(timezone, january)
  const julOffset = getTimezoneOffset(timezone, july)
  const currentOffset = getTimezoneOffset(timezone, date)
  
  return currentOffset !== Math.max(janOffset, julOffset)
}

/**
 * Форматирует дату для отображения с учетом timezone
 * 
 * @param {Date} date - Дата
 * @param {string} timezone - IANA timezone
 * @param {object} options - Опции форматирования
 * @returns {string} - Отформатированная строка
 */
export const formatWithTimezone = (date, timezone, options = {}) => {
  const defaultOptions = {
    timeZone: timezone,
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    ...options
  }
  
  return date.toLocaleString('ru-RU', defaultOptions)
}

/**
 * Отладочная функция для проверки конвертации
 * 
 * @param {Date} date - Дата для проверки
 * @param {string} timezone - IANA timezone
 */
export const debugTimezoneConversion = (date, timezone) => {
  console.group('🌍 Timezone Conversion Debug')
  console.log('Input date:', date.toString())
  console.log('User timezone:', timezone)
  
  const utcDate = convertLocalToUTC(date, timezone)
  console.log('Converted to UTC:', utcDate.toISOString())
  
  const formatted = formatForServer(date, timezone)
  console.log('Formatted for server:', formatted)
  
  const parsed = parseFromServer(formatted, timezone)
  console.log('Parsed back:', parsed.toString())
  
  const info = getTimezoneInfo(timezone)
  console.log('Timezone info:', info)
  
  console.groupEnd()
}
