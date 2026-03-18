import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { getApi } from '@/utils/request'
import { toast } from '@/utils/toast'

export function useDashboard() {
  const { t } = useI18n()

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
  const loading = ref(false)
  const isRechargeModalOpen = ref(false)
  const usdAmount = ref('')
  const selectedPayment = ref('alipay')

  const calculateCnyAmount = computed(() => {
    const amount = parseFloat(usdAmount.value)
    if (isNaN(amount) || amount <= 0) return 0
    return amount * 0.2
  })

  const loadDashboard = async () => {
    loading.value = true

    try {
      const [stats, trend, modelData, recent] = await Promise.all([
        getApi('/v1/dashboard/stats'),
        getApi('/v1/dashboard/usage-trend?days=7'),
        getApi('/v1/dashboard/model-usage'),
        getApi('/v1/dashboard/recent-activities?limit=5'),
      ])

      dashboardStats.value = stats
      usageData.value = trend
      modelUsage.value = modelData.map(item => ({
        model: item.model,
        requests: item.requests,
      }))
      activities.value = recent
    } catch (error) {
      toast.error(t('common.error'), error.message)
    } finally {
      loading.value = false
    }
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
    loading,
    isRechargeModalOpen,
    usdAmount,
    selectedPayment,
    calculateCnyAmount,
    loadDashboard,
    handleConfirmRecharge,
    formatChange,
  }
}
