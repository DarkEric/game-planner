/**
 * Локальные часы (для input type="time"), соответствующие заданному времени по стенкам МСК
 * в указанную календарную дату (год/месяц/день в локальной зоне — как у выбранного дня в форме).
 * МСК = UTC+3 (без летнего времени).
 */
export function localTimeInputValueForMoscowWall(dateLocalMidnight, moscowHour = 19, moscowMinute = 0) {
  const y = dateLocalMidnight.getFullYear()
  const mo = dateLocalMidnight.getMonth()
  const d = dateLocalMidnight.getDate()
  const utcH = moscowHour - 3
  const instant = new Date(Date.UTC(y, mo, d, utcH, moscowMinute, 0))
  const hh = String(instant.getHours()).padStart(2, '0')
  const mm = String(instant.getMinutes()).padStart(2, '0')
  return `${hh}:${mm}`
}
