import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { getApi, postApi } from '@/utils/request'
import { toast } from '@/utils/toast'

export function usePlans() {
  const { t } = useI18n()

  const codexPlans = ref([
    {
      id: "small",
      name: "Codex小包套餐",
      price: 45,
      dailyLimit: 30,
      monthlyQuota: 900,
      concurrency: 8,
      features: [
        "有效期 30 天",
        "仅可用 Codex",
        "月度可用 $900 额度",
        "每日可用 $30 + 昨日未用完额度",
        "单套餐并发量为 8",
        "套餐多买只叠加额度，不叠加时间",
      ],
    },
    {
      id: "medium",
      name: "Codex中包套餐",
      price: 60,
      dailyLimit: 60,
      monthlyQuota: 1800,
      concurrency: 12,
      features: [
        "有效期 30 天",
        "仅可用 Codex",
        "月度可用 $1800 额度",
        "每日可用 $60 + 昨日未用完额度",
        "单套餐并发量为 12",
        "套餐多买只叠加额度，不叠加时间",
      ],
    },
    {
      id: "large",
      name: "Codex大包套餐",
      price: 75,
      dailyLimit: 90,
      monthlyQuota: 2700,
      concurrency: 16,
      features: [
        "有效期 30 天",
        "仅可用 Codex",
        "月度可用 $2700 额度",
        "每日可用 $90 + 昨日未用完额度",
        "单套餐并发量为 16",
        "套餐多买只叠加额度，不叠加时间",
      ],
    },
  ])

  const activeSubscriptions = ref([])

  const historicalSubscriptions = ref([])

  const currentSubscriptionIndex = ref(0)
  const isDropdownOpen = ref(false)
  const selectedPlan = ref(null)
  const selectedPayment = ref('alipay')
  const isCheckoutModalOpen = ref(false)
  const isActivationCodeModalOpen = ref(false)
  const isHistoryDetailModalOpen = ref(false)
  const selectedHistoricalSub = ref(null)
  const activationCode = ref('')
  const loading = ref(false)
  const submitting = ref(false)
  const actionMessage = ref('')

  const emptySubscription = {
    planName: '',
    daysRemaining: 0,
    purchaseDate: '-',
    expiryDate: '-',
    dailyReset: false,
    remainingQuota: 0,
    totalQuota: 0,
    usedPercentage: 0,
  }

  const currentSubscription = computed(() => activeSubscriptions.value[currentSubscriptionIndex.value] || emptySubscription)
  const selectedPlanData = computed(() => codexPlans.value.find((p) => p.id === selectedPlan.value))

  const loadPlans = async () => {
    loading.value = true

    try {
      const [remotePlans, remoteActiveSubs, remoteHistorySubs] = await Promise.all([
        getApi('/v1/plans'),
        getApi('/v1/subscriptions/active'),
        getApi('/v1/subscriptions/history'),
      ])

      if (remotePlans && remotePlans.length > 0) {
        codexPlans.value = remotePlans
      }

      if (Array.isArray(remoteActiveSubs)) {
        activeSubscriptions.value = remoteActiveSubs.map((item) => ({
          ...item,
          planType: item.planId,
        }))
      }

      if (Array.isArray(remoteHistorySubs)) {
        historicalSubscriptions.value = remoteHistorySubs.map((item) => ({
          ...item,
          planType: item.planId,
        }))
      }
    } catch (error) {
      // toast.error(t('plans.loadFailed'), error.message)
    } finally {
      loading.value = false
    }
  }

  const handlePurchasePlan = (planId) => {
    selectedPlan.value = planId
    isCheckoutModalOpen.value = true
  }

  const createOrder = async () => {
    if (!selectedPlan.value) return

    submitting.value = true

    try {
      const result = await postApi('/v1/orders', {
        planId: selectedPlan.value,
        paymentMethod: selectedPayment.value,
      })
      isCheckoutModalOpen.value = false
      const payUrl = String(result?.payUrl || '')
        .trim()
        .replace(/`/g, '')
        .replace(/^"+|"+$/g, '')
        .replace(/^'+|'+$/g, '')

      actionMessage.value = `订单已创建：${result?.orderId || ''}`

      if (payUrl) {
        window.open(payUrl, '_blank', 'noopener,noreferrer')
        return
      }
    } catch (error) {
      toast.error(t('plans.orderFailed'), error.message)
    } finally {
      submitting.value = false
    }
  }

  const handleActivateCode = async () => {
    if (!activationCode.value.trim()) {
      toast.error(t('plans.activationCodeEmpty'), t('plans.activationCodeEmptyDetail'))
      return
    }

    if (activationCode.value.trim().length < 8) {
      toast.error(t('plans.activationCodeFormatError'), t('plans.activationCodeFormatErrorDetail'))
      return
    }

    submitting.value = true
    try {
      const result = await postApi('/v1/activation-codes/consume', {
        code: activationCode.value.trim(),
      })
      toast.success(t('plans.activationSuccess'), result?.message || t('plans.activationSuccessDetail'))
      isActivationCodeModalOpen.value = false
      activationCode.value = ""
      await loadPlans()
    } catch (error) {
      toast.error(t('plans.activationFailed'), error.message)
    } finally {
      submitting.value = false
    }
  }

  return {
    codexPlans,
    activeSubscriptions,
    historicalSubscriptions,
    currentSubscriptionIndex,
    isDropdownOpen,
    selectedPlan,
    selectedPayment,
    isCheckoutModalOpen,
    isActivationCodeModalOpen,
    isHistoryDetailModalOpen,
    selectedHistoricalSub,
    activationCode,
    loading,
    submitting,
    actionMessage,
    currentSubscription,
    selectedPlanData,
    loadPlans,
    handlePurchasePlan,
    createOrder,
    handleActivateCode,
  }
}
