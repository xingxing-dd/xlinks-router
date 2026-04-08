export const formatDateTime = (value) => {
  if (!value) {
    return '-'
  }
  return value.replace('T', ' ').slice(0, 19)
}

export const toDateTimeLocalValue = (value) => {
  if (!value) {
    return ''
  }
  return value.slice(0, 16)
}

export const normalizeDateTimeInput = (value) => {
  if (!value) {
    return null
  }
  return value.length === 16 ? `${value}:00` : value
}

export const formatStatus = (value) => (Number(value) === 1 ? '启用' : '停用')

export const formatNullable = (value) => value || '-'

export const summarizeJsonArray = (value) => {
  if (!value) {
    return '-'
  }
  try {
    const parsed = JSON.parse(value)
    return Array.isArray(parsed) ? parsed.join(', ') : value
  } catch (error) {
    return value
  }
}

export const formatBooleanFlag = (value, enabled = '是', disabled = '否') => (Number(value) === 1 ? enabled : disabled)
