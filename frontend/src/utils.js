export const PRIORITIES = ['HIGH', 'MEDIUM', 'LOW']
export const PRIORITY_LABEL = { HIGH: 'High', MEDIUM: 'Medium', LOW: 'Low' }

export function nextPriority(current) {
  return PRIORITIES[(PRIORITIES.indexOf(current) + 1) % PRIORITIES.length]
}

export const TRADE_TYPES = [
  { value: 'IMMEDIATE', label: 'Now', days: 0 },
  { value: 'DAYS_7', label: '7 days', days: 7 },
  { value: 'DAYS_30', label: '30 days', days: 30 },
  { value: 'CUSTOM', label: 'Custom', days: null },
]
export const TRADE_LABEL = {
  IMMEDIATE: 'Immediate trade',
  DAYS_7: '7-day trade',
  DAYS_30: '30-day trade',
  CUSTOM: 'Custom plan',
}

export const PLATFORMS = ['WhatsApp', 'Discord', 'Telegram', 'Reddit', 'Other']

export const STATUS_LABEL = { PLANNED: 'Planned', DONE: 'Done', CANCELLED: 'Cancelled' }

/** yyyy-MM-dd for the device's local calendar day. */
export function todayIso() {
  return new Date().toLocaleDateString('en-CA')
}

export function addDays(iso, n) {
  const [y, m, d] = iso.split('-').map(Number)
  return new Date(Date.UTC(y, m - 1, d + n)).toISOString().slice(0, 10)
}

export function formatDate(iso) {
  if (!iso) return ''
  const [y, m, d] = iso.split('-').map(Number)
  return new Date(Date.UTC(y, m - 1, d)).toLocaleDateString(undefined, {
    weekday: 'short',
    day: 'numeric',
    month: 'short',
    timeZone: 'UTC',
  })
}

/** What the ticket stub on a deal card shows. */
export function stubFor(deal) {
  if (deal.status === 'DONE') return { big: 'Done', small: '', tone: 'done' }
  if (deal.status === 'CANCELLED') return { big: 'Off', small: 'cancelled', tone: 'off' }
  if (deal.overdue) {
    const n = Math.abs(deal.daysLeft)
    return { big: String(n), small: n === 1 ? 'day late' : 'days late', tone: 'late' }
  }
  if (deal.daysLeft === 0) return { big: 'Today', small: '', tone: 'today' }
  return {
    big: String(deal.daysLeft),
    small: deal.daysLeft === 1 ? 'day left' : 'days left',
    tone: deal.daysLeft <= 3 ? 'soon' : 'later',
  }
}
