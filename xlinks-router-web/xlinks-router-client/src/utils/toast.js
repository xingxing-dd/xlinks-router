import { useToastStore } from '@/stores/toast'

export const toast = {
  success: (title, message, duration) => {
    const toastStore = useToastStore()
    if (!message && title) {
      toastStore.success(title, '', duration)
    } else {
      toastStore.success(title, message, duration)
    }
  },
  error: (title, message, duration) => {
    const toastStore = useToastStore()
    if (!message && title) {
      toastStore.error(title, '', duration)
    } else {
      toastStore.error(title, message, duration)
    }
  },
  warning: (title, message, duration) => {
    const toastStore = useToastStore()
    if (!message && title) {
      toastStore.warning(title, '', duration)
    } else {
      toastStore.warning(title, message, duration)
    }
  },
  info: (title, message, duration) => {
    const toastStore = useToastStore()
    if (!message && title) {
      toastStore.info(title, '', duration)
    } else {
      toastStore.info(title, message, duration)
    }
  },
};
