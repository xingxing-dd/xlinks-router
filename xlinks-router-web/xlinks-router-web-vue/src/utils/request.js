const API_PREFIX = '/api'
const AUTH_PREFIX = '/auth'

function redirectToLogin() {
  if (typeof window === 'undefined') {
    return
  }

  const currentPath = `${window.location.pathname}${window.location.search}`
  const loginUrl = currentPath.startsWith('/login')
    ? '/login'
    : `/login?redirect=${encodeURIComponent(currentPath)}`

  localStorage.removeItem('xlinks-access-token')
  window.location.href = loginUrl
}

function buildUrl(path) {
  if (path.startsWith('http://') || path.startsWith('https://')) {
    return path
  }

  if (path.startsWith('/')) {
    return path
  }

  return `/${path}`
}

async function request(path, options = {}) {
  const token = localStorage.getItem('xlinks-access-token')
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {}),
  }

  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  const response = await fetch(buildUrl(path), {
    ...options,
    headers,
  })

  const payload = await response.json().catch(() => null)

  if (response.status === 401) {
    redirectToLogin()
    throw new Error(payload?.message || '登录已失效，请重新登录')
  }

  if (!response.ok) {
    const message = payload?.message || '请求失败'
    throw new Error(message)
  }

  if (payload?.code !== 0) {
    throw new Error(payload?.message || '接口返回异常')
  }

  return payload?.data
}

export function postAuth(path, body) {
  return request(`${AUTH_PREFIX}${path}`, {
    method: 'POST',
    body: JSON.stringify(body ?? {}),
  })
}

export function getApi(path) {
  return request(`${API_PREFIX}${path}`, {
    method: 'GET',
  })
}

export function postApi(path, body) {
  return request(`${API_PREFIX}${path}`, {
    method: 'POST',
    body: JSON.stringify(body ?? {}),
  })
}

export function putApi(path, body) {
  return request(`${API_PREFIX}${path}`, {
    method: 'PUT',
    body: JSON.stringify(body ?? {}),
  })
}

export function deleteApi(path) {
  return request(`${API_PREFIX}${path}`, {
    method: 'DELETE',
  })
}

export function getPageRecords(payload) {
  if (Array.isArray(payload)) {
    return payload
  }

  return payload?.records || []
}