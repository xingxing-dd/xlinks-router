import { defineStore } from 'pinia'
import { clearStoredAuth, loadStoredAuth, saveStoredAuth } from '@/utils/auth'

export const useAuthStore = defineStore('admin-auth', {
  state: () => ({
    user: loadStoredAuth().user || null,
    token: loadStoredAuth().token || '',
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.token),
  },
  actions: {
    setAuth(user, token) {
      this.user = user
      this.token = token
      saveStoredAuth(user, token)
    },
    clearAuth() {
      this.user = null
      this.token = ''
      clearStoredAuth()
    },
  },
})
