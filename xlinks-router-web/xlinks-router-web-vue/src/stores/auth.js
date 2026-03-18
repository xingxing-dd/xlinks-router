import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

const ACCESS_TOKEN_KEY = 'xlinks-access-token'

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref(localStorage.getItem(ACCESS_TOKEN_KEY) || '')

  const isAuthenticated = computed(() => Boolean(accessToken.value))

  function setAccessToken(token) {
    accessToken.value = token || ''

    if (accessToken.value) {
      localStorage.setItem(ACCESS_TOKEN_KEY, accessToken.value)
      return
    }

    localStorage.removeItem(ACCESS_TOKEN_KEY)
  }

  function clearAuth() {
    setAccessToken('')
  }

  return {
    accessToken,
    isAuthenticated,
    setAccessToken,
    clearAuth,
  }
})