import { getLocale } from '@/locales'

function resolveLocale(locale) {
  return locale || getLocale() || 'zh-CN'
}

export function formatDateTime(value, locale) {
  if (!value) {
    return '--'
  }

  const normalized = value.includes('T') ? value : value.replace(' ', 'T')
  const date = new Date(normalized)

  if (Number.isNaN(date.getTime())) {
    return value
  }

  return date.toLocaleString(resolveLocale(locale), {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export function formatRelativeTime(value, locale) {
  const resolvedLocale = resolveLocale(locale)

  if (!value) {
    return resolvedLocale.startsWith('zh') ? '从未使用' : 'Never used'
  }

  const normalized = value.includes('T') ? value : value.replace(' ', 'T')
  const date = new Date(normalized)
  if (Number.isNaN(date.getTime())) {
    return value
  }

  const diff = Date.now() - date.getTime()
  const minute = 60 * 1000
  const hour = 60 * minute
  const day = 24 * hour
  const formatter = new Intl.RelativeTimeFormat(resolvedLocale, { numeric: 'auto' })

  if (diff < hour) {
    return formatter.format(-Math.max(1, Math.floor(diff / minute)), 'minute')
  }
  if (diff < day) {
    return formatter.format(-Math.max(1, Math.floor(diff / hour)), 'hour')
  }
  return formatter.format(-Math.max(1, Math.floor(diff / day)), 'day')
}

export function formatCurrency(
  value,
  currency = 'USD',
  locale,
  fractionDigits = 2,
) {
  const amount = Number(value || 0)
  const resolvedLocale = locale || (currency === 'USD' ? 'en-US' : 'zh-CN')
  const digits = Number.isFinite(Number(fractionDigits)) ? Number(fractionDigits) : 2

  return new Intl.NumberFormat(resolvedLocale, {
    style: 'currency',
    currency,
    minimumFractionDigits: digits,
    maximumFractionDigits: digits,
  }).format(amount)
}

export function formatNumber(value, locale) {
  return Number(value || 0).toLocaleString(resolveLocale(locale))
}

export function formatCompactNumber(value) {
  const n = Number(value || 0)
  const abs = Math.abs(n)

  if (abs >= 1_000_000) {
    const scaled = abs / 1_000_000
    const text = scaled >= 10 ? scaled.toFixed(0) : scaled.toFixed(1)
    return `${n < 0 ? '-' : ''}${text.replace(/\.0$/, '')}M`
  }

  if (abs >= 1_000) {
    const scaled = abs / 1_000
    return `${n < 0 ? '-' : ''}${scaled.toFixed(1)}k`
  }

  return `${n}`
}
