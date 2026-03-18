import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useToastStore = defineStore('toast', () => {
  const toasts = ref([])

  const addToast = (type, title, message, duration) => {
    const id = Math.random().toString(36).substring(2, 9)
    const finalDuration = duration || 3000
    toasts.value.push({ id, type, title, message, duration: finalDuration })
  }

  const removeToast = (id) => {
    toasts.value = toasts.value.filter((t) => t.id !== id)
  }

  const success = (title, message, duration) => addToast('success', title, message, duration)
  const error = (title, message, duration) => addToast('error', title, message, duration)
  const warning = (title, message, duration) => addToast('warning', title, message, duration)
  const info = (title, message, duration) => addToast('info', title, message, duration)

  return {
    toasts,
    addToast,
    removeToast,
    success,
    error,
    warning,
    info
  }
})
