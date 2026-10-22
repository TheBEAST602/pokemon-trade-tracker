const BASE = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080').replace(/\/+$/, '')
const API_KEY = import.meta.env.VITE_API_KEY || 'dev-key'
const TIMEOUT_MS = 90_000
const SLOW_AFTER_MS = 3_000

export class ApiError extends Error {
  constructor(message, status) {
    super(message)
    this.status = status
  }
}

// --- "server is waking up" state, shared with <ServerBanner /> -------------
let pending = 0
let slow = false
const listeners = new Set()

function setSlow(next) {
  if (slow === next) return
  slow = next
  listeners.forEach((l) => l())
}

export const serverStatus = {
  subscribe(listener) {
    listeners.add(listener)
    return () => listeners.delete(listener)
  },
  getSnapshot: () => slow,
}

async function request(path, { method = 'GET', body } = {}) {
  const controller = new AbortController()
  const abortTimer = setTimeout(() => controller.abort(), TIMEOUT_MS)
  const slowTimer = setTimeout(() => setSlow(true), SLOW_AFTER_MS)
  pending += 1

  try {
    let response
    try {
      response = await fetch(`${BASE}/api/v1${path}`, {
        method,
        headers: {
          'X-API-Key': API_KEY,
          ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}),
        },
        body: body !== undefined ? JSON.stringify(body) : undefined,
        signal: controller.signal,
      })
    } catch (err) {
      throw new ApiError(
        err.name === 'AbortError'
          ? 'The server took too long to answer. Try again in a moment.'
          : 'Cannot reach the server. Check your connection and the API address.',
        0,
      )
    }

    if (response.status === 204) return null

    const data = await response.json().catch(() => null)
    if (!response.ok) {
      const fallback = response.status === 401 ? 'The API key was rejected.' : 'Request failed.'
      throw new ApiError(data?.message || fallback, response.status)
    }
    return data
  } finally {
    clearTimeout(abortTimer)
    clearTimeout(slowTimer)
    pending -= 1
    if (pending === 0) setSlow(false)
  }
}

function toQuery(filters = {}) {
  const params = new URLSearchParams()
  Object.entries(filters).forEach(([key, value]) => {
    if (value !== '' && value !== null && value !== undefined && value !== false) {
      params.set(key, String(value))
    }
  })
  const q = params.toString()
  return q ? `?${q}` : ''
}

export const api = {
  getToday: () => request('/dashboard/today'),
  listDeals: (filters) => request(`/deals${toQuery(filters)}`),
  getDeal: (id) => request(`/deals/${id}`),
  createDeal: (deal) => request('/deals', { method: 'POST', body: deal }),
  updateDeal: (id, deal) => request(`/deals/${id}`, { method: 'PUT', body: deal }),
  updatePriority: (id, priority) => request(`/deals/${id}/priority`, { method: 'PATCH', body: { priority } }),
  updateStatus: (id, status) => request(`/deals/${id}/status`, { method: 'PATCH', body: { status } }),
  deleteDeal: (id) => request(`/deals/${id}`, { method: 'DELETE' }),
}
