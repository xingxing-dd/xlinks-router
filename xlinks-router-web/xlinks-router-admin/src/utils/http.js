import { clearStoredAuth, loadStoredAuth } from '@/utils/auth'

const API_BASE_URL = (import.meta.env.VITE_ADMIN_API_BASE_URL || '').replace(/\/$/, '')

const buildUrl = (path, query) => {
  const base = API_BASE_URL ? `${API_BASE_URL}${path}` : path
  const url = new URL(base, window.location.origin)

  if (query) {
    Object.entries(query).forEach(([key, value]) => {
      if (value === undefined || value === null || value === '') {
        return
      }
      url.searchParams.set(key, value)
    })
  }

  return API_BASE_URL ? url.toString() : `${url.pathname}${url.search}`
}

export async function request(path, options = {}) {
  const { method = 'GET', query, body, headers = {} } = options
  const stored = loadStoredAuth()
  const response = await fetch(buildUrl(path, query), {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(stored.token ? { Authorization: `Bearer ${stored.token}` } : {}),
      ...headers,
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  })

  const payload = await response.json().catch(() => null)

  if (response.status === 401) {
    clearStoredAuth()
    if (window.location.pathname !== '/login') {
      window.location.href = '/login'
    }
  }

  if (!response.ok) {
    throw new Error(payload?.message || `请求失败：${response.status}`)
  }

  if (!payload) {
    return null
  }

  if (payload.code !== 0) {
    throw new Error(payload.message || '请求失败')
  }

  return payload.data
}
