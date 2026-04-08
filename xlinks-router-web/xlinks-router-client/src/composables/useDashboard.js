import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { getApi } from '@/utils/request'
import { toast } from '@/utils/toast'

export function useDashboard() {
  const { t } = useI18n()
  const DEFAULT_USAGE_PAGE_SIZE = 20

  const usageData = ref([])
  const modelUsage = ref([])
  const dashboardStats = ref({
    todayRequests: 0,
    todayRequestsChange: 0,
    todayTokens: 0,
    todayTokensChange: 0,
    todayCost: 0,
    todayCostChange: 0,
    balance: 0,
  })

  const activities = ref([])
  const usageRecords = ref([])
  const usageLoading = ref(false)
  const usageCurrentPage = ref(1)
  const usagePageSize = ref(DEFAULT_USAGE_PAGE_SIZE)
  const usageTotal = ref(0)
  const loading = ref(false)
  const isRechargeModalOpen = ref(false)
  const usdAmount = ref('')
  const selectedPayment = ref('alipay')

  const calculateCnyAmount = computed(() => {
    const amount = parseFloat(usdAmount.value)
    if (isNaN(amount) || amount <= 0) return 0
    return amount * 0.2
  })

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
    const cost = Number(item?.cost ?? item?.amount ?? 0) || 0

    const time = item?.time ?? item?.createdAt ?? item?.timestamp ?? ''
    const model = item?.model ?? item?.modelName ?? item?.providerModel ?? item?.name ?? '-'
    const token = item?.token ?? item?.tokenName ?? item?.tokenId ?? item?.apiKeyName ?? '-'
    const channel = item?.channel ?? item?.route ?? item?.provider ?? item?.gateway ?? '-'

    return {
      time,
      token,
      channel,
      model,
      inputTokens,
      cacheHitTokens,
      outputTokens,
      totalTokens,
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
      Math.max(1, Number(payload?.pageSize ?? payload?.size ?? DEFAULT_USAGE_PAGE_SIZE) || DEFAULT_USAGE_PAGE_SIZE)
    )

    return { records, total, page, pageSize }
  }

  const loadRecentActivities = async (page = usageCurrentPage.value) => {
    const targetPage = Math.max(1, Number(page) || 1)
    usageLoading.value = true

    try {
      const payload = await getApi(`/v1/dashboard/recent-activities?page=${targetPage}&pageSize=${DEFAULT_USAGE_PAGE_SIZE}`)
      const { records, total, page: currentPage, pageSize } = parseRecentActivitiesPayload(payload, targetPage)
      activities.value = records
      usageRecords.value = records.map(mapUsageRecord)
      usageTotal.value = total
      usageCurrentPage.value = currentPage
      usagePageSize.value = pageSize
    } catch (error) {
      toast.error(t('common.error'), error.message)
      usageRecords.value = []
      usageTotal.value = 0
    } finally {
      usageLoading.value = false
    }
  }

  const handleUsagePageChange = async (nextPage) => {
    const page = Math.max(1, Number(nextPage) || 1)
    if (page === usageCurrentPage.value || page > usageTotalPages.value) {
      return
    }
    await loadRecentActivities(page)
  }

  const handleUsageRefresh = async () => {
    await loadRecentActivities(usageCurrentPage.value)
  }

  const loadDashboard = async () => {
    loading.value = true

    try {
      const [stats, trend, modelData] = await Promise.all([
        getApi('/v1/dashboard/stats'),
        getApi('/v1/dashboard/usage-trend?days=7'),
        getApi('/v1/dashboard/model-usage'),
      ])

      dashboardStats.value = stats
      usageData.value = trend
      modelUsage.value = modelData.map(item => ({
        model: item.model,
        requests: item.requests,
      }))
    } catch (error) {
      toast.error(t('common.error'), error.message)
    } finally {
      loading.value = false
    }

    await loadRecentActivities(usageCurrentPage.value)
  }

  const handleConfirmRecharge = () => {
    const amount = parseFloat(usdAmount.value)
    if (isNaN(amount) || amount <= 0) {
      toast.error(t('common.error'), t('dashboard.inputAmountPlaceholder'))
      return
    }
    toast.info(t('common.loading'))
    isRechargeModalOpen.value = false
    usdAmount.value = ''
  }

  const formatChange = (value) => `${Math.abs(Number(value || 0)).toFixed(1)}%`

  return {
    usageData,
    modelUsage,
    dashboardStats,
    activities,
    usageRecords,
    usageLoading,
    usageCurrentPage,
    usagePageSize,
    usageTotal,
    usageTotalPages,
    loading,
    isRechargeModalOpen,
    usdAmount,
    selectedPayment,
    calculateCnyAmount,
    loadDashboard,
    handleUsagePageChange,
    handleUsageRefresh,
    handleConfirmRecharge,
    formatChange,
  }
}
