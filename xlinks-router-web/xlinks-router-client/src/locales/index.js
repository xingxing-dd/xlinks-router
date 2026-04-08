import { createI18n } from 'vue-i18n'
import zhCN from './zh-CN'
import enUS from './en-US'

const LOCALE_STORAGE_KEY = 'xlinks-locale'
const SUPPORTED_LOCALES = ['zh-CN', 'en-US']

export function normalizeLocale(value) {
  if (!value || typeof value !== 'string') {
    return 'zh-CN'
  }

  const lower = value.toLowerCase()
  if (lower.startsWith('zh')) return 'zh-CN'
  if (lower.startsWith('en')) return 'en-US'
  return 'zh-CN'
}

function resolveInitialLocale() {
  if (typeof window === 'undefined') {
    return 'zh-CN'
  }

  const saved = window.localStorage.getItem(LOCALE_STORAGE_KEY)
  if (saved) {
    return normalizeLocale(saved)
  }

  return normalizeLocale(window.navigator.language)
}

const i18n = createI18n({
  legacy: false,
  locale: resolveInitialLocale(),
  fallbackLocale: 'zh-CN',
  messages: {
    'zh-CN': zhCN,
    'en-US': enUS,
  }
})

export function setLocale(locale) {
  const normalized = normalizeLocale(locale)
  if (!SUPPORTED_LOCALES.includes(normalized)) {
    return
  }

  i18n.global.locale.value = normalized
  if (typeof window !== 'undefined') {
    window.localStorage.setItem(LOCALE_STORAGE_KEY, normalized)
    if (window.document?.documentElement) {
      window.document.documentElement.lang = normalized
    }
  }
}

export function getLocale() {
  return normalizeLocale(i18n.global.locale.value)
}

export default i18n
