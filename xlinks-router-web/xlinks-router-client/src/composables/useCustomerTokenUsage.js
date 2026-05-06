import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { getApi } from '@/utils/request'
import { toast } from '@/utils/toast'

const DEFAULT_USAGE_PAGE_SIZE = 20
const TOKEN_STORAGE_KEY = 'xlinks-customer-token-usage-token'

export function useCustomerTokenUsage() {
  const { t } = useI18n()

  const tokenInput = ref('')
  const currentToken = ref('')
  const usageData = ref([])
  const modelUsage = ref([])
  const dashboardStats = ref({
    todayRequests: 0,
    todayRequestsChange: 0,
    todayTokens: 0,
    todayTokensChange: 0,
    todayCost: 0,
    totalCost: 0,
    todayCostChange: 0,
    balance: 0,
  })
  const usageRecords = ref([])
  const usageLoading = ref(false)
  const usageCurrentPage = ref(1)
  const usagePageSize = ref(DEFAULT_USAGE_PAGE_SIZE)
  const usageTotal = ref(0)
  const loading = ref(false)
  const searched = ref(false)
  const hasSavedToken = ref(false)
  const isTokenEditing = ref(true)

  const hasLoaded = computed(() => searched.value && currentToken.value)
  const canQuery = computed(() => tokenInput.value.trim().length > 0)
  const maskedTokenInput = computed(() => maskToken(tokenInput.value))
  const maskedCurrentToken = computed(() => maskToken(currentToken.value))
  const usageTotalPages = computed(() => {
    const size = Number(usagePageSize.value || DEFAULT_USAGE_PAGE_SIZE)
    const total = Number(usageTotal.value || 0)
    return Math.max(1, Math.ceil(total / size))
  })

  const mapUsageRecord = (item) => {
    const inputTokens = Number(item?.inputTokens ?? item?.promptTokens ?? 0) || 0
    const cacheHitTokens = Number(item?.cacheHitTokens ?? item?.cachedTokens ?? 0) || 0
    const outputTokens = Number(item?.outputTokens ?? item?.completionTokens ?? 0) || 0
    const totalTokens = Number(item?.totalTokens ?? inputTokens + outputTokens) || 0
    const responseMs = Number(item?.responseMs ?? item?.latencyMs ?? item?.firstResponseMs ?? 0) || 0
    const usageType = String(item?.usageType ?? '').trim().toLowerCase()
    const cost = Number(item?.cost ?? item?.amount ?? 0) || 0

    return {
      time: item?.time ?? item?.createdAt ?? item?.timestamp ?? '',
      token: item?.token ?? item?.tokenName ?? item?.tokenId ?? item?.apiKeyName ?? '-',
      channel: item?.channel ?? item?.route ?? item?.provider ?? item?.gateway ?? '-',
      model: item?.model ?? item?.modelName ?? item?.providerModel ?? item?.name ?? '-',
      inputTokens,
      cacheHitTokens,
      outputTokens,
      totalTokens,
      responseMs,
      usageType,
      cost,
    }
  }

  const parseRecentActivitiesPayload = (payload, fallbackPage = 1) => {
    if (Array.isArray(payload)) {
      return {
        records: payload,
        total: payload.length,
        page: 1,
        pageSize: DEFAULT_USAGE_PAGE_SIZE,
      }
    }

    const records = Array.isArray(payload?.records)
      ? payload.records
      : (Array.isArray(payload?.list) ? payload.list : [])
    const total = Number(payload?.total ?? records.length) || 0
    const page = Math.max(1, Number(payload?.page ?? payload?.current ?? fallbackPage) || fallbackPage)
    const pageSize = Math.min(
      DEFAULT_USAGE_PAGE_SIZE,
      Math.max(1, Number(payload?.pageSize ?? payload?.size ?? DEFAULT_USAGE_PAGE_SIZE) || DEFAULT_USAGE_PAGE_SIZE),
    )

    return { records, total, page, pageSize }
  }

  const createUsageQuery = (path, token = currentToken.value) => {
    const params = new URLSearchParams({ customerToken: token })
    return `/v1/usages/${path}?${params.toString()}`
  }

  const maskToken = (token) => {
    const normalized = String(token || '').trim()
    if (!normalized) {
      return ''
    }
    if (normalized.length <= 8) {
      return `${normalized.slice(0, 1)}****${normalized.slice(-1)}`
    }
    return `${normalized.slice(0, 3)}****${normalized.slice(-4)}`
  }

  const readSavedToken = () => {
    if (typeof window === 'undefined') {
      return ''
    }
    return localStorage.getItem(TOKEN_STORAGE_KEY) || ''
  }

  const writeSavedToken = (token) => {
    if (typeof window === 'undefined') {
      return
    }
    localStorage.setItem(TOKEN_STORAGE_KEY, token)
  }

  const resetUsageState = () => {
    usageData.value = []
    modelUsage.value = []
    dashboardStats.value = {
      todayRequests: 0,
      todayRequestsChange: 0,
      todayTokens: 0,
      todayTokensChange: 0,
      todayCost: 0,
      totalCost: 0,
      todayCostChange: 0,
      balance: 0,
    }
    usageRecords.value = []
    usageCurrentPage.value = 1
    usagePageSize.value = DEFAULT_USAGE_PAGE_SIZE
    usageTotal.value = 0
  }

  const loadRecentActivities = async (page = usageCurrentPage.value, token = currentToken.value) => {
    if (!token) {
      return
    }

    const targetPage = Math.max(1, Number(page) || 1)
    usageLoading.value = true

    try {
      const payload = await getApi(`${createUsageQuery('recent-activities', token)}&page=${targetPage}&pageSize=${DEFAULT_USAGE_PAGE_SIZE}`)
      const { records, total, page: currentPage, pageSize } = parseRecentActivitiesPayload(payload, targetPage)
      usageRecords.value = records.map(mapUsageRecord)
      usageTotal.value = total
      usageCurrentPage.value = currentPage
      usagePageSize.value = pageSize
    } catch (error) {
      usageRecords.value = []
      usageTotal.value = 0
      throw error
    } finally {
      usageLoading.value = false
    }
  }

  const loadDashboard = async (token = currentToken.value) => {
    if (!token) {
      return
    }

    loading.value = true

    try {
      const [stats, trend, modelData] = await Promise.all([
        getApi(createUsageQuery('stats', token)),
        getApi(`${createUsageQuery('usage-trend', token)}&days=7`),
        getApi(createUsageQuery('model-usage', token)),
      ])

      dashboardStats.value = {
        ...dashboardStats.value,
        ...stats,
        balance: 0,
      }
      usageData.value = Array.isArray(trend) ? trend : []
      modelUsage.value = Array.isArray(modelData)
        ? modelData.map(item => ({
            model: item.model,
            requests: item.requests,
          }))
        : []

      await loadRecentActivities(1, token)
    } catch (error) {
      resetUsageState()
      throw error
    } finally {
      loading.value = false
    }
  }

  const handleSaveToken = () => {
    const normalizedToken = tokenInput.value.trim()

    if (!normalizedToken) {
      toast.warning(t('common.error'), t('customerTokenUsage.validation.tokenRequired'))
      return ''
    }

    writeSavedToken(normalizedToken)
    hasSavedToken.value = true
    isTokenEditing.value = false
    tokenInput.value = normalizedToken

    return normalizedToken
  }

  const handleSearch = async () => {
    const normalizedToken = handleSaveToken()
    searched.value = true

    if (!normalizedToken) {
      currentToken.value = ''
      resetUsageState()
      return
    }

    currentToken.value = normalizedToken

    try {
      await loadDashboard(normalizedToken)
    } catch (error) {
      toast.error(t('common.error'), error.message || t('customerTokenUsage.loadFailed'))
    }
  }

  const handleUsagePageChange = async (nextPage) => {
    const page = Math.max(1, Number(nextPage) || 1)
    if (!currentToken.value || page === usageCurrentPage.value || page > usageTotalPages.value) {
      return
    }

    try {
      await loadRecentActivities(page)
    } catch (error) {
      toast.error(t('common.error'), error.message || t('customerTokenUsage.loadFailed'))
    }
  }

  const handleUsageRefresh = async () => {
    if (!currentToken.value) {
      return
    }

    try {
      await loadDashboard(currentToken.value)
    } catch (error) {
      toast.error(t('common.error'), error.message || t('customerTokenUsage.loadFailed'))
    }
  }

  const formatChange = (value) => `${Math.abs(Number(value || 0)).toFixed(1)}%`

  const handleEditToken = () => {
    isTokenEditing.value = true
  }

  onMounted(async () => {
    const savedToken = readSavedToken().trim()
    hasSavedToken.value = savedToken.length > 0

    if (!savedToken) {
      return
    }

    tokenInput.value = savedToken
    currentToken.value = savedToken
    searched.value = true
    isTokenEditing.value = false

    try {
      await loadDashboard(savedToken)
    } catch (error) {
      toast.error(t('common.error'), error.message || t('customerTokenUsage.loadFailed'))
    }
  })

  return {
    tokenInput,
    currentToken,
    usageData,
    modelUsage,
    dashboardStats,
    usageRecords,
    usageLoading,
    usageCurrentPage,
    usagePageSize,
    usageTotal,
    usageTotalPages,
    loading,
    searched,
    hasSavedToken,
    isTokenEditing,
    hasLoaded,
    canQuery,
    maskedTokenInput,
    maskedCurrentToken,
    handleEditToken,
    handleSaveToken,
    handleSearch,
    handleUsagePageChange,
    handleUsageRefresh,
    formatChange,
  }
}
