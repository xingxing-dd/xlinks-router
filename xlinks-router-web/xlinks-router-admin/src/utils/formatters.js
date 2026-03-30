export const formatDateTime = (value) => {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return date.toLocaleString('zh-CN', {
    hour12: false,
  })
}

export const formatStatus = (status) => {
  const map = {
    active: '启用',
    inactive: '停用',
    pending: '待审核',
  }
  return map[status] || status || '-'
}

export const formatCurrency = (value) => {
  if (value === null || value === undefined || value === '') return '-'
  const number = Number(value)
  if (Number.isNaN(number)) return value
  return `¥${number.toFixed(2)}`
}
