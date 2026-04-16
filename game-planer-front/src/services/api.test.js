import test from 'node:test'
import assert from 'node:assert/strict'
import { resolveWallClockTimezone } from '../utils/timezoneValidation.js'
import { utcDateFromZonedWallClock } from '../utils/zonedWallTime.js'

test('resolveWallClockTimezone falls back when profile timezone is invalid', () => {
  const result = resolveWallClockTimezone('0', 'Europe/Moscow')
  assert.equal(result, 'Europe/Moscow')
})

test('resolveWallClockTimezone keeps valid IANA timezone', () => {
  const result = resolveWallClockTimezone('Asia/Yekaterinburg', 'Europe/Moscow')
  assert.equal(result, 'Asia/Yekaterinburg')
})

test('utcDateFromZonedWallClock: whole-day call passes timeZone as 6th arg (not a stray 0)', () => {
  const d = utcDateFromZonedWallClock(2026, 3, 25, 0, 0, 'Europe/Moscow')
  assert.ok(!Number.isNaN(d.getTime()))
})

test('utcDateFromZonedWallClock: extra 0 before timeZone used to set timeZone=0 and throw', () => {
  assert.throws(
    () => utcDateFromZonedWallClock(2026, 3, 25, 0, 0, 0, 'Europe/Moscow'),
    (e) => e instanceof RangeError && /time zone/i.test(String(e.message))
  )
})
