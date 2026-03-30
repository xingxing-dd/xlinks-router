import { defineStore } from 'pinia'

let seed = 1

export const useToastStore = defineStore('toast', {
  state: () => ({
    toasts: [],
  }),
  actions: {
    push(message, type = 'info', duration = 2800) {
      const id = seed++
      this.toasts.push({ id, message, type, duration })
      return id
    },
    removeToast(id) {
      this.toasts = this.toasts.filter(item => item.id !== id)
    },
    clear() {
      this.toasts = []
    },
  },
})
