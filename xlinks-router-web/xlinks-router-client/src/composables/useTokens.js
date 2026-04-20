import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { deleteApi, getApi, getPageRecords, postApi, putApi } from '@/utils/request'
import { toast } from '@/utils/toast'

function createDefaultSettingsForm() {
  return {
    id: '',
    tokenName: '',
    expireEnabled: false,
    expireTime: '',
    status: 'enabled',
    quotaEnabled: false,
    dailyQuota: '',
    totalQuota: '',
    usedQuota: 0,
    totalUsedQuota: 0,
    modelRestrictionEnabled: false,
    allowedModels: [],
  }
}

export function useTokens() {
  const { t } = useI18n()

  const tokens = ref([])
  const isCreateModalOpen = ref(false)
  const isEditModalOpen = ref(false)
  const newTokenName = ref('')
  const selectedToken = ref(null)
  const settingsForm = ref(createDefaultSettingsForm())
  const availableModels = ref([])
  const searchQuery = ref('')
  const selectedStatus = ref('all')
  const loading = ref(false)
  const submitting = ref(false)
  const savingSettings = ref(false)
  const detailLoading = ref(false)
  const loadingAvailableModels = ref(false)
  const actionMessage = ref('')

  const formatExpireDisplay = (value) => {
    if (!value) {
      return t('tokens.permanent')
    }
    const text = String(value).trim()
    if (!text) {
      return t('tokens.permanent')
    }
    return text.includes(' ') ? text.split(' ')[0] : text.split('T')[0]
  }

  const filteredTokens = computed(() => {
    const query = searchQuery.value.trim().toLowerCase()
    const status = selectedStatus.value

    return tokens.value.filter(token => {
      const matchesSearch =
        token.name.toLowerCase().includes(query) ||
        token.key.toLowerCase().includes(query)

      const matchesStatus = status === 'all' || token.status === status
      return matchesSearch && matchesStatus
    })
  })

  const normalizeToken = (item) => ({
    id: String(item.id),
    name: item.tokenName,
    key: item.tokenValue,
    lastUsed: item.lastUsedAt || t('tokens.neverUsed'),
    requests: item.totalRequests || 0,
    status: item.expireTime && new Date(item.expireTime) < new Date()
      ? 'expired'
      : (item.status === 1 ? 'enabled' : 'disabled'),
    expiresAt: formatExpireDisplay(item.expireTime),
    rawExpireTime: item.expireTime || '',
    dailyQuota: item.dailyQuota == null ? null : Number(item.dailyQuota),
    usedQuota: item.usedQuota == null ? 0 : Number(item.usedQuota),
    totalQuota: item.totalQuota == null ? null : Number(item.totalQuota),
    totalUsedQuota: item.totalUsedQuota == null ? 0 : Number(item.totalUsedQuota),
    allowedModels: Array.isArray(item.allowedModels) ? item.allowedModels : [],
    modelCodes: item.modelCodes || [],
  })

  const formatQuotaValue = (value) => {
    const num = Number(value || 0)
    if (num >= 1000000) {
      return `${(num / 1000000).toFixed(1)}M$`
    }
    if (num >= 1000) {
      return `${(num / 1000).toFixed(1)}K$`
    }
    return Number.isInteger(num) ? `${num}$` : `${num.toFixed(1)}$`
  }

  const getDailyQuotaText = (token) => {
    if (token.dailyQuota == null) {
      return {
        label: t('tokens.dailyQuotaUsed'),
        value: `${formatQuotaValue(token.usedQuota)}/${t('tokens.unlimitedText')}`,
      }
    }
    return {
      label: t('tokens.dailyQuotaUsed'),
      value: `${formatQuotaValue(token.usedQuota)}/${formatQuotaValue(token.dailyQuota)}`,
    }
  }

  const getTotalQuotaText = (token) => {
    if (token.totalQuota == null) {
      return {
        label: t('tokens.totalQuotaUsed'),
        value: `${formatQuotaValue(token.totalUsedQuota)}/${t('tokens.unlimitedText')}`,
      }
    }
    return {
      label: t('tokens.totalQuotaUsed'),
      value: `${formatQuotaValue(token.totalUsedQuota)}/${formatQuotaValue(token.totalQuota)}`,
    }
  }

  const getTotalQuotaPercent = (token) => {
    if (token.totalQuota == null) {
      return 100
    }
    return getQuotaProgress(token.totalUsedQuota, token.totalQuota)
  }

  const getDailyQuotaPercent = (token) => {
    if (token.dailyQuota == null) {
      return 100
    }
    if (token.dailyQuota <= 0) {
      return 0
    }
    return Math.max(0, Math.min(100, (token.usedQuota / token.dailyQuota) * 100))
  }

  const getDailyQuotaBarClass = (token) => {
    if (token.status === 'expired') {
      return 'bg-gradient-to-r from-rose-500 to-red-500'
    }
    if (token.dailyQuota != null && token.usedQuota >= token.dailyQuota) {
      return 'bg-gradient-to-r from-rose-500 to-red-500'
    }
    return 'bg-gradient-to-r from-emerald-500 to-green-500'
  }

  const getTotalQuotaBarClass = (token) => {
    if (token.status === 'expired') {
      return 'bg-gradient-to-r from-rose-500 to-red-500'
    }
    if (token.totalQuota == null || token.totalQuota <= 0) {
      return 'bg-gradient-to-r from-emerald-500 to-green-500'
    }
    if (token.totalUsedQuota >= token.totalQuota) {
      return 'bg-gradient-to-r from-rose-500 to-red-500'
    }
    return 'bg-gradient-to-r from-emerald-500 to-green-500'
  }

  const getQuotaProgress = (used, total) => {
    const normalizedTotal = Number(total || 0)
    if (normalizedTotal <= 0) {
      return 0
    }
    return Math.max(0, Math.min(100, (Number(used || 0) / normalizedTotal) * 100))
  }

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

  const loadAvailableModels = async () => {
    if (availableModels.value.length > 0 || loadingAvailableModels.value) {
      return
    }

    loadingAvailableModels.value = true
    try {
      const payload = await getApi('/v1/models/available')
      availableModels.value = Array.isArray(payload)
        ? payload
          .map(item => item?.name)
          .filter(Boolean)
          .sort((a, b) => a.localeCompare(b))
        : []
    } catch (error) {
      toast.error(t('tokens.modelsLoadFailed'), error.message)
    } finally {
      loadingAvailableModels.value = false
    }
  }

  const copyToClipboard = async (text, name) => {
    const rawText = String(text ?? '')
    try {
      await navigator.clipboard.writeText(rawText)
      toast.success(t('common.copySuccess'), t('tokens.copySuccessDetail', { name }))
      actionMessage.value = ''
    } catch {
      const textArea = document.createElement('textarea')
      textArea.value = rawText
      textArea.style.position = 'fixed'
      textArea.style.left = '-999999px'
      textArea.style.top = '-999999px'
      document.body.appendChild(textArea)
      textArea.focus()
      textArea.select()
      textArea.setSelectionRange(0, rawText.length)
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
      tokens.value = tokens.value.filter(token => token.id !== id)
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
      await putApi(`/v1/customer-tokens/${id}/status`, { status: newStatus })
      tokens.value = tokens.value.map(token =>
        token.id === id
          ? { ...token, status: newStatus === 1 ? 'enabled' : 'disabled' }
          : token,
      )
      toast.success(t('tokens.statusUpdated'), t('tokens.statusUpdatedDetail', { status: statusText }))
    } catch (error) {
      toast.error(t('tokens.statusUpdateFailed'), error.message)
    }
  }

  const handleImportToCCSwitch = (token) => {
    try {
      const origin = window.location.origin.replace(/\/$/, '')
      const params = new URLSearchParams({
        app: 'codex',
        name: `xlinks-${token.name}`,
        endpoint: `${origin}/v1/`,
        apiKey: token.key,
        homepage: origin,
      })
      window.location.href = `ccswitch://v1/import?${params.toString()}`
      toast.info(
        t('tokens.ccswitchImportTriggered'),
        t('tokens.ccswitchImportTriggeredDetail', { name: token.name }),
      )
    } catch (error) {
      toast.error(t('tokens.ccswitchImportFailed'), error?.message || t('tokens.ccswitchImportFailedDetail'))
    }
  }

  const toggleAllowedModel = (model) => {
    const current = new Set(settingsForm.value.allowedModels)
    if (current.has(model)) {
      current.delete(model)
    } else {
      current.add(model)
    }
    settingsForm.value.allowedModels = Array.from(current)
  }

  const selectAllAllowedModels = () => {
    settingsForm.value.allowedModels = [...availableModels.value]
  }

  const clearAllowedModels = () => {
    settingsForm.value.allowedModels = []
  }

  const resetQuotaUsage = () => {
    settingsForm.value.usedQuota = 0
    settingsForm.value.totalUsedQuota = 0
  }

  const openEditModal = async (token) => {
    isEditModalOpen.value = true
    detailLoading.value = true

    try {
      await loadAvailableModels()

      const detail = await getApi(`/v1/customer-tokens/${token.id}`)
      const normalized = normalizeToken(detail)
      selectedToken.value = normalized
      availableModels.value = Array.from(new Set([
        ...availableModels.value,
        ...normalized.allowedModels,
      ])).sort((a, b) => a.localeCompare(b))

      settingsForm.value = {
        id: normalized.id,
        tokenName: normalized.name,
        expireEnabled: Boolean(normalized.rawExpireTime),
        expireTime: normalized.rawExpireTime ? normalized.rawExpireTime.slice(0, 16) : '',
        status: normalized.status === 'disabled' ? 'disabled' : 'enabled',
        quotaEnabled: normalized.dailyQuota != null || normalized.totalQuota != null,
        dailyQuota: normalized.dailyQuota == null ? '' : String(normalized.dailyQuota),
        totalQuota: normalized.totalQuota == null ? '' : String(normalized.totalQuota),
        usedQuota: normalized.usedQuota,
        totalUsedQuota: normalized.totalUsedQuota,
        modelRestrictionEnabled: normalized.allowedModels.length > 0,
        allowedModels: [...normalized.allowedModels],
      }
    } catch (error) {
      toast.error(t('tokens.editLoadFailed'), error.message)
      isEditModalOpen.value = false
    } finally {
      detailLoading.value = false
    }
  }

  const closeEditModal = () => {
    isEditModalOpen.value = false
    selectedToken.value = null
    settingsForm.value = createDefaultSettingsForm()
  }

  const handleSaveSettings = async () => {
    if (!settingsForm.value.id) {
      return
    }

    if (!settingsForm.value.tokenName.trim()) {
      toast.warning(t('tokens.inputNameError'), t('tokens.inputNameErrorDetail'))
      return
    }

    const dailyQuota = settingsForm.value.quotaEnabled && settingsForm.value.dailyQuota !== ''
      ? Number(settingsForm.value.dailyQuota)
      : null
    const totalQuota = settingsForm.value.quotaEnabled && settingsForm.value.totalQuota !== ''
      ? Number(settingsForm.value.totalQuota)
      : null

    if (dailyQuota != null && (Number.isNaN(dailyQuota) || dailyQuota <= 0)) {
      toast.warning(t('tokens.invalidDailyQuota'), t('tokens.invalidDailyQuotaDetail'))
      return
    }

    if (totalQuota != null && (Number.isNaN(totalQuota) || totalQuota <= 0)) {
      toast.warning(t('tokens.invalidTotalQuota'), t('tokens.invalidTotalQuotaDetail'))
      return
    }

    const payload = {
      tokenName: settingsForm.value.tokenName.trim(),
      status: settingsForm.value.status === 'enabled' ? 1 : 0,
      expireTime: settingsForm.value.expireEnabled ? (settingsForm.value.expireTime || null) : null,
      dailyQuota: settingsForm.value.quotaEnabled ? dailyQuota : null,
      totalQuota: settingsForm.value.quotaEnabled ? totalQuota : null,
      allowedModels: settingsForm.value.modelRestrictionEnabled
        ? [...settingsForm.value.allowedModels]
        : null,
    }

    savingSettings.value = true
    try {
      await putApi(`/v1/customer-tokens/${settingsForm.value.id}`, payload)
      await loadTokens()
      closeEditModal()
      toast.success(t('tokens.editSuccess'), t('tokens.editSuccessDetail'))
    } catch (error) {
      toast.error(t('tokens.editFailed'), error.message)
    } finally {
      savingSettings.value = false
    }
  }

  return {
    tokens,
    isCreateModalOpen,
    isEditModalOpen,
    newTokenName,
    selectedToken,
    settingsForm,
    availableModels,
    searchQuery,
    selectedStatus,
    loading,
    submitting,
    savingSettings,
    detailLoading,
    loadingAvailableModels,
    actionMessage,
    filteredTokens,
    loadTokens,
    loadAvailableModels,
    copyToClipboard,
    handleImportToCCSwitch,
    handleCreateToken,
    handleDeleteToken,
    handleToggleStatus,
    toggleAllowedModel,
    selectAllAllowedModels,
    clearAllowedModels,
    resetQuotaUsage,
    openEditModal,
    closeEditModal,
    handleSaveSettings,
    formatQuotaValue,
    getQuotaProgress,
    getDailyQuotaText,
    getTotalQuotaText,
    getTotalQuotaPercent,
    getDailyQuotaPercent,
    getDailyQuotaBarClass,
    getTotalQuotaBarClass,
  }
}
