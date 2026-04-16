import test from 'node:test'
import assert from 'node:assert/strict'
import { resolveWallClockTimezone } from '../utils/timezoneValidation.js'

test('resolveWallClockTimezone falls back when profile timezone is invalid', () => {
  const result = resolveWallClockTimezone('0', 'Europe/Moscow')
  assert.equal(result, 'Europe/Moscow')
})

test('resolveWallClockTimezone keeps valid IANA timezone', () => {
  const result = resolveWallClockTimezone('Asia/Yekaterinburg', 'Europe/Moscow')
  assert.equal(result, 'Asia/Yekaterinburg')
})
