import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { getApi, postApi } from '@/utils/request'
import { toast } from '@/utils/toast'

const DEFAULT_PLANS = [
  {
    id: 'small',
    name: 'Codex小包套餐',
    price: 45,
    dailyLimit: 30,
    monthlyQuota: 900,
    durationDays: 30,
    allowedModels: ['Codex'],
    carryOverDailyQuota: true,
    stackQuotaOnly: true,
  },
  {
    id: 'medium',
    name: 'Codex中包套餐',
    price: 60,
    dailyLimit: 60,
    monthlyQuota: 1800,
    durationDays: 30,
    allowedModels: ['Codex'],
    carryOverDailyQuota: true,
    stackQuotaOnly: true,
  },
  {
    id: 'large',
    name: 'Codex大包套餐',
    price: 75,
    dailyLimit: 90,
    monthlyQuota: 2700,
    durationDays: 30,
    allowedModels: ['Codex'],
    carryOverDailyQuota: true,
    stackQuotaOnly: true,
  },
]

const ALIPAY_ENABLED_PLAN_IDS = new Set(['10005'])

export function usePlans() {
  const { t } = useI18n()

  const formatAllowedModels = (models) => {
    if (!Array.isArray(models) || models.length === 0) {
      return t('plans.features.defaultModel')
    }
    return models.join(', ')
  }

  const buildPlanFeatures = (plan) => {
    const features = []

    features.push(
      t('plans.features.allowedModels', {
        models: formatAllowedModels(plan.allowedModels),
      }),
    )

    features.push(
      t('plans.features.totalQuota', {
        quota: plan.monthlyQuota ?? 0,
      }),
    )

    if (plan.carryOverDailyQuota !== false) {
      features.push(
        t('plans.features.dailyQuotaCarryOver', {
          quota: plan.dailyLimit ?? 0,
        }),
      )
    } else {
      features.push(
        t('plans.features.dailyQuota', {
          quota: plan.dailyLimit ?? 0,
        }),
      )
    }

    if (plan.stackQuotaOnly !== false) {
      features.push(t('plans.features.stackQuotaOnly'))
    }

    return features
  }

  const normalizePlan = (plan) => ({
    ...plan,
    id: String(plan.id),
    allowedModels: Array.isArray(plan.allowedModels) ? plan.allowedModels : [],
  })

  const enrichPlan = (plan) => {
    const normalized = normalizePlan(plan)
    return {
      ...normalized,
      features: buildPlanFeatures(normalized),
    }
  }

  const codexPlans = ref(DEFAULT_PLANS.map(enrichPlan))

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
  const isAlipayAvailableForSelectedPlan = computed(() => ALIPAY_ENABLED_PLAN_IDS.has(String(selectedPlan.value)))

  const loadPlans = async () => {
    loading.value = true

    try {
      const [remotePlans, remoteActiveSubs, remoteHistorySubs] = await Promise.all([
        getApi('/v1/plans'),
        getApi('/v1/subscriptions/active'),
        getApi('/v1/subscriptions/history'),
      ])

      if (Array.isArray(remotePlans) && remotePlans.length > 0) {
        codexPlans.value = remotePlans.map(enrichPlan)
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
    if (!ALIPAY_ENABLED_PLAN_IDS.has(String(planId)) && selectedPayment.value === 'alipay') {
      selectedPayment.value = 'third-party'
    }
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
        // Alipay pagePay returns a full HTML form, not a URL.
        if (payUrl.includes('<form') && payUrl.includes('submit')) {
          const payWindow = window.open('', '_blank')
          if (!payWindow) {
            throw new Error('支付窗口被浏览器拦截，请允许弹窗后重试')
          }
          payWindow.document.open()
          payWindow.document.write(payUrl)
          payWindow.document.close()
          return
        }

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
      activationCode.value = ''
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
    isAlipayAvailableForSelectedPlan,
    loadPlans,
    handlePurchasePlan,
    createOrder,
    handleActivateCode,
  }
}
