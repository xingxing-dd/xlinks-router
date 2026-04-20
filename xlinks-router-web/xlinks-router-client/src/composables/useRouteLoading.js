import { readonly, ref } from 'vue'

const isRouteLoading = ref(false)

const SHOW_DELAY = 120
const MIN_VISIBLE = 220

let pendingCount = 0
let showTimer = null
let hideTimer = null
let visibleAt = 0

const clearTimer = (timerRef) => {
  if (timerRef) {
    clearTimeout(timerRef)
  }
  return null
}

const showLoading = () => {
  if (!isRouteLoading.value) {
    isRouteLoading.value = true
    visibleAt = Date.now()
  }
}

const hideLoading = () => {
  isRouteLoading.value = false
  visibleAt = 0
}

export const startRouteLoading = () => {
  pendingCount += 1
  hideTimer = clearTimer(hideTimer)

  if (isRouteLoading.value || showTimer) {
    return
  }

  showTimer = setTimeout(() => {
    showTimer = null
    if (pendingCount > 0) {
      showLoading()
    }
  }, SHOW_DELAY)
}

export const finishRouteLoading = () => {
  pendingCount = Math.max(0, pendingCount - 1)
  if (pendingCount > 0) {
    return
  }

  showTimer = clearTimer(showTimer)
  if (!isRouteLoading.value) {
    return
  }

  const elapsed = Date.now() - visibleAt
  const remain = Math.max(0, MIN_VISIBLE - elapsed)
  hideTimer = setTimeout(() => {
    hideTimer = null
    if (pendingCount === 0) {
      hideLoading()
    }
  }, remain)
}

export const useRouteLoading = () => ({
  isRouteLoading: readonly(isRouteLoading),
})

