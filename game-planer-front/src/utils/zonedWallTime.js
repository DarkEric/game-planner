/**
 * UTC instant for a calendar date and clock time interpreted in an IANA timezone
 * (настенные часы в зоне пользователя, без привязки к МСК).
 *
 * @param {number} year - full year
 * @param {number} monthIndex0 - 0-11
 * @param {number} day - 1-31
 * @param {number} hour - 0-23
 * @param {number} [minute=0]
 * @param {string} [timeZone] - IANA, по умолчанию часовой пояс браузера
 * @returns {Date}
 */
export function utcDateFromZonedWallClock(
  year,
  monthIndex0,
  day,
  hour,
  minute = 0,
  timeZone = Intl.DateTimeFormat().resolvedOptions().timeZone
) {
  const formatter = new Intl.DateTimeFormat('en-GB', {
    timeZone,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false
  })

  const wallParts = (utcMs) => {
    const parts = formatter.formatToParts(new Date(utcMs))
    const m = {}
    for (const p of parts) {
      if (p.type !== 'literal') m[p.type] = p.value
    }
    return {
      y: parseInt(m.year, 10),
      mo: parseInt(m.month, 10),
      d: parseInt(m.day, 10),
      h: parseInt(m.hour, 10),
      min: parseInt(m.minute, 10)
    }
  }

  const wantMonth = monthIndex0 + 1
  const naive = Date.UTC(year, monthIndex0, day, hour, minute, 0)
  const from = naive - 40 * 60 * 60 * 1000
  const to = naive + 40 * 60 * 60 * 1000

  for (let t = from; t <= to; t += 60 * 1000) {
    const p = wallParts(t)
    if (p.y === year && p.mo === wantMonth && p.d === day && p.h === hour && p.min === minute) {
      return new Date(t)
    }
  }

  console.warn('utcDateFromZonedWallClock: no exact match, using naive UTC', {
    year,
    monthIndex0,
    day,
    hour,
    minute,
    timeZone
  })
  return new Date(naive)
}
