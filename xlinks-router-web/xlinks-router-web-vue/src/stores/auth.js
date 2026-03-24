import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

const ACCESS_TOKEN_KEY = 'xlinks-access-token'

function readToken() {
  return localStorage.getItem(ACCESS_TOKEN_KEY) || sessionStorage.getItem(ACCESS_TOKEN_KEY) || ''
}

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref(readToken())

  const isAuthenticated = computed(() => Boolean(accessToken.value))

  function setAccessToken(token, options = {}) {
    const remember = Boolean(options.remember)
    accessToken.value = token || ''

    if (accessToken.value) {
      if (remember) {
        localStorage.setItem(ACCESS_TOKEN_KEY, accessToken.value)
        sessionStorage.removeItem(ACCESS_TOKEN_KEY)
      } else {
        sessionStorage.setItem(ACCESS_TOKEN_KEY, accessToken.value)
        localStorage.removeItem(ACCESS_TOKEN_KEY)
      }
      return
    }

    localStorage.removeItem(ACCESS_TOKEN_KEY)
    sessionStorage.removeItem(ACCESS_TOKEN_KEY)
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