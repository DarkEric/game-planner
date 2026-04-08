/**
 * Объединённый диапазон загрузки игр: окно календаря и список предстоящих (как в боте ~60 дней).
 */
export function getMergedGamesFetchRange(currentStartDate, daysToShow) {
  const homeStart = new Date()
  homeStart.setDate(homeStart.getDate() - 1)
  homeStart.setHours(0, 0, 0, 0)

  const homeEnd = new Date()
  homeEnd.setDate(homeEnd.getDate() + 60)

  const calStart = new Date(currentStartDate)
  calStart.setDate(calStart.getDate() - 1)

  const calEnd = new Date(currentStartDate)
  calEnd.setDate(calEnd.getDate() + daysToShow + 1)

  const start = calStart < homeStart ? calStart : homeStart
  const end = calEnd > homeEnd ? calEnd : homeEnd
  return { start, end }
}
