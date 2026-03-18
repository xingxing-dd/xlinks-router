import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { deleteApi, getApi, getPageRecords, postApi } from '@/utils/request'
import { toast } from '@/utils/toast'

export function useTokens() {
  const { t } = useI18n()

  const tokens = ref([])
  const visibleKeys = ref(new Set())
  const isCreateModalOpen = ref(false)
  const newTokenName = ref('')
  const searchQuery = ref('')
  const loading = ref(false)
  const submitting = ref(false)
  const actionMessage = ref('')

  const filteredTokens = computed(() => {
    const query = searchQuery.value.trim().toLowerCase()
    return tokens.value.filter(
      token =>
        token.name.toLowerCase().includes(query) ||
        token.key.toLowerCase().includes(query)
    )
  })

  const normalizeToken = (item) => ({
    id: String(item.id),
    name: item.tokenName,
    key: item.tokenValue,
    created: item.createdAt,
    lastUsed: item.lastUsedAt || t('tokens.neverUsed'),
    requests: item.totalRequests || 0,
    status: item.expireTime && new Date(item.expireTime) < new Date() 
      ? 'expired' 
      : (item.status === 1 ? 'enabled' : 'disabled'),
    expiresAt: item.expireTime || t('tokens.permanent'),
    modelCodes: item.modelCodes || [],
  })

  const loadTokens = async () => {
    loading.value = true

    try {
      const payload = await getApi('/v1/customer-tokens?page=1&pageSize=100')
      tokens.value = getPageRecords(payload).map(normalizeToken)
    } catch (error) {
      toast.error(t('tokens.loadFailed'), error.message)
    } finally {
      loading.value = false
    }
  }

  const toggleKeyVisibility = (id) => {
    if (visibleKeys.value.has(id)) {
      visibleKeys.value.delete(id)
    } else {
      visibleKeys.value.add(id)
    }
  }

  const copyToClipboard = async (text, name) => {
    try {
      await navigator.clipboard.writeText(text)
      toast.success(t('common.copySuccess'), t('tokens.copySuccessDetail', { name }))
      actionMessage.value = ''
    } catch {
      const textArea = document.createElement('textarea')
      textArea.value = text
      textArea.style.position = 'fixed'
      textArea.style.left = '-999999px'
      textArea.style.top = '-999999px'
      document.body.appendChild(textArea)
      textArea.focus()
      textArea.select()
      try {
        document.execCommand('copy')
        toast.success(t('common.copySuccess'), t('tokens.copySuccessDetail', { name }))
        actionMessage.value = ''
      } catch {
        toast.error(t('tokens.copyFailed'), t('tokens.copyFailedDetail'))
      } finally {
        document.body.removeChild(textArea)
      }
    }
  }

  const handleCreateToken = async () => {
    if (!newTokenName.value.trim()) {
      toast.warning(t('tokens.inputNameError'), t('tokens.inputNameErrorDetail'))
      return
    }

    submitting.value = true
    actionMessage.value = ''

    try {
      const created = await postApi('/v1/customer-tokens', {
        tokenName: newTokenName.value.trim(),
      })

      tokens.value.unshift(normalizeToken({
        ...created,
        status: 1,
        totalRequests: 0,
        lastUsedAt: null,
        modelCodes: [],
      }))
      const name = newTokenName.value.trim()
      newTokenName.value = ''
      isCreateModalOpen.value = false
      toast.success(t('tokens.createSuccess'), t('tokens.createSuccessDetail', { name }))
    } catch (error) {
      toast.error(t('tokens.createFailed'), error.message)
    } finally {
      submitting.value = false
    }
  }

  const handleDeleteToken = async (id, name) => {
    if (!confirm(t('tokens.deleteConfirm'))) {
      return
    }

    try {
      await deleteApi(`/v1/customer-tokens/${id}`)
      tokens.value = tokens.value.filter(t => t.id !== id)
      toast.success(t('tokens.deleteSuccess'), t('tokens.deleteSuccessDetail', { name }))
      actionMessage.value = ''
    } catch (error) {
      toast.error(t('tokens.deleteFailed'), error.message)
    }
  }

  const handleToggleStatus = async (id, currentStatus) => {
    if (currentStatus === 'expired') return

    const newStatus = currentStatus === 'enabled' ? 0 : 1
    const statusText = newStatus === 1 ? t('tokens.status.enabled') : t('tokens.status.disabled')

    try {
      await postApi(`/v1/customer-tokens/${id}/status`, { status: newStatus })
      tokens.value = tokens.value.map((token) => 
        token.id === id 
          ? { ...token, status: newStatus === 1 ? 'enabled' : 'disabled' } 
          : token
      )
      toast.success(t('tokens.statusUpdated'), t('tokens.statusUpdatedDetail', { status: statusText }))
    } catch (error) {
      toast.error(t('tokens.statusUpdateFailed'), error.message)
    }
  }

  const maskKey = (key) => {
    if (!key || key.length <= 20) {
      return key
    }

    return `${key.substring(0, 10)}${'\u2022'.repeat(20)}${key.substring(key.length - 10)}`
  }

  return {
    tokens,
    visibleKeys,
    isCreateModalOpen,
    newTokenName,
    searchQuery,
    loading,
    submitting,
    actionMessage,
    filteredTokens,
    loadTokens,
    toggleKeyVisibility,
    copyToClipboard,
    handleCreateToken,
    handleDeleteToken,
    handleToggleStatus,
    maskKey,
  }
}
