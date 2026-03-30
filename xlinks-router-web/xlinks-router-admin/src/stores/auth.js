import { defineStore } from 'pinia'

const STORAGE_KEY = 'xlinks-admin-auth'

const loadState = () => {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY)) || {}
  } catch (error) {
    return {}
  }
}

export const useAuthStore = defineStore('admin-auth', {
  state: () => ({
    user: loadState().user || null,
    token: loadState().token || '',
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.token),
  },
  actions: {
    setAuth(user, token) {
      this.user = user
      this.token = token
      localStorage.setItem(STORAGE_KEY, JSON.stringify({ user, token }))
    },
    clearAuth() {
      this.user = null
      this.token = ''
      localStorage.removeItem(STORAGE_KEY)
    },
  },
})
