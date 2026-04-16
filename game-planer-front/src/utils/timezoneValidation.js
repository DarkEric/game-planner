export function isValidIanaTimezone(timeZone) {
  try {
    if (typeof timeZone !== 'string' || !timeZone.trim()) {
      return false
    }
    new Intl.DateTimeFormat('en-US', { timeZone })
    return true
  } catch {
    return false
  }
}

export function resolveWallClockTimezone(profileTimezone, fallbackTimezone) {
  if (isValidIanaTimezone(profileTimezone)) {
    return profileTimezone
  }
  if (isValidIanaTimezone(fallbackTimezone)) {
    return fallbackTimezone
  }
  return Intl.DateTimeFormat().resolvedOptions().timeZone
}
