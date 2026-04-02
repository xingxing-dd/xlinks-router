const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const phoneRegex = /^1\d{10}$/

export function resolveAccountTargetType(value) {
  const input = (value || '').trim()
  if (emailRegex.test(input)) {
    return 'email'
  }
  if (phoneRegex.test(input)) {
    return 'phone'
  }
  return ''
}
