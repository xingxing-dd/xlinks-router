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

  const activeSubscriptions = ref([
    {
      id: "sub-1",
      planName: "Codex中包套餐",
      planType: "medium",
      daysRemaining: 24,
      purchaseDate: "2026-03-11 16:15",
      expiryDate: "2026-04-10 16:15",
      dailyReset: true,
      remainingQuota: 58.049198,
      totalQuota: 60,
      usedPercentage: 3,
    },
    {
      id: "sub-2",
      planName: "Codex大包套餐",
      planType: "large",
      daysRemaining: 15,
      purchaseDate: "2026-02-28 10:30",
      expiryDate: "2026-03-30 10:30",
      dailyReset: false,
      remainingQuota: 72.5,
      totalQuota: 90,
      usedPercentage: 19,
    },
    {
      id: "sub-3",
      planName: "Codex小包套餐",
      planType: "small",
      daysRemaining: 8,
      purchaseDate: "2026-03-05 14:20",
      expiryDate: "2026-03-25 14:20",
      dailyReset: true,
      remainingQuota: 28.3,
      totalQuota: 30,
      usedPercentage: 5,
    },
  ])

  const currentSubscriptionIndex = ref(0)
  const isDropdownOpen = ref(false)
  const selectedPlan = ref(null)
  const selectedPayment = ref('alipay')
  const isCheckoutModalOpen = ref(false)
  const isActivationCodeModalOpen = ref(false)
  const activationCode = ref('')
  const loading = ref(false)
  const submitting = ref(false)
  const actionMessage = ref('')

  const currentSubscription = computed(() => activeSubscriptions.value[currentSubscriptionIndex.value])
  const selectedPlanData = computed(() => codexPlans.value.find((p) => p.id === selectedPlan.value))

  const loadPlans = async () => {
    loading.value = true

    try {
      const remotePlans = await getApi('/v1/plans')
      if (remotePlans && remotePlans.length > 0) {
        codexPlans.value = remotePlans
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
      actionMessage.value = `订单已创建，支付链接：${result.payUrl}`
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
      // 模拟 API 调用
      if (activationCode.value.trim() === "TEST2024") {
        toast.success(t('plans.activationSuccess'), t('plans.activationSuccessDetail'))
        isActivationCodeModalOpen.value = false
        activationCode.value = ""
      } else {
        toast.error(t('plans.activationFailed'), t('plans.activationFailedDetail'))
      }
    } catch (error) {
      toast.error(t('plans.activationFailed'), error.message)
    } finally {
      submitting.value = false
    }
  }

  return {
    codexPlans,
    activeSubscriptions,
    currentSubscriptionIndex,
    isDropdownOpen,
    selectedPlan,
    selectedPayment,
    isCheckoutModalOpen,
    isActivationCodeModalOpen,
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
