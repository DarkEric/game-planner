/**
 * Wall-clock time in Europe/Moscow as UTC Date.
 * Moscow uses UTC+3 year-round (no DST since 2014).
 */
const MSK_OFFSET_MS = 3 * 60 * 60 * 1000

/**
 * @param {number} year - full year
 * @param {number} monthIndex0 - 0-11
 * @param {number} day - 1-31
 * @param {number} hour - 0-23
 * @param {number} [minute=0]
 * @param {number} [second=0]
 * @returns {Date} UTC instant matching this Moscow wall time
 */
export function utcDateFromMoscowWallClock(year, monthIndex0, day, hour, minute = 0, second = 0) {
  const utcMs = Date.UTC(year, monthIndex0, day, hour, minute, second) - MSK_OFFSET_MS
  return new Date(utcMs)
}
