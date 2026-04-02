export const STORAGE_KEY = 'xlinks-admin-auth'

export const loadStoredAuth = () => {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY)) || {}
  } catch (error) {
    return {}
  }
}

export const saveStoredAuth = (user, token) => {
  localStorage.setItem(STORAGE_KEY, JSON.stringify({ user, token }))
}

export const clearStoredAuth = () => {
  localStorage.removeItem(STORAGE_KEY)
}
