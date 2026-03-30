export function formatDateTime(value) {
  if (!value) {
    return '--'
  }

  const normalized = value.includes('T') ? value : value.replace(' ', 'T')
  const date = new Date(normalized)

  if (Number.isNaN(date.getTime())) {
    return value
  }

  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export function formatRelativeTime(value) {
  if (!value) {
    return '从未使用'
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

  if (diff < hour) {
    return `${Math.max(1, Math.floor(diff / minute))} 分钟前`
  }

  if (diff < day) {
    return `${Math.floor(diff / hour)} 小时前`
  }

  return `${Math.floor(diff / day)} 天前`
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

export function formatNumber(value) {
  return Number(value || 0).toLocaleString('zh-CN')
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
