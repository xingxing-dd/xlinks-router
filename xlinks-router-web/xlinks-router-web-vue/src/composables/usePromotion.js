import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { getApi, getPageRecords } from '@/utils/request'
import { toast } from '@/utils/toast'

export function usePromotion() {
  const { t } = useI18n()

  const stats = ref({
    totalReferrals: 0,
    activeReferrals: 0,
    totalEarnings: 0,
    pendingEarnings: 0,
  })

  const records = ref([])
  const referralCode = ref('')
  const referralLink = ref('')
  const rules = ref(null)
  const loading = ref(false)

  const copyToClipboard = async (text) => {
    try {
      await navigator.clipboard.writeText(text)
      toast.success(t('promotion.copySuccess'))
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
        toast.success(t('promotion.copySuccess'))
      } catch {
        toast.error(t('promotion.copyFailed'))
      } finally {
        document.body.removeChild(textArea)
      }
    }
  }

  const getStatusColor = (status) => {
    switch (status) {
      case 'active': return 'bg-green-100 text-green-700'
      case 'pending': return 'bg-yellow-100 text-yellow-700'
      case 'inactive': return 'bg-gray-100 text-gray-700'
      default: return 'bg-gray-100 text-gray-700'
    }
  }

  const getStatusText = (status) => {
    switch (status) {
      case 'active': return t('promotion.status.active')
      case 'pending': return t('promotion.status.pending')
      case 'inactive': return t('promotion.status.inactive')
      default: return t('promotion.status.unknown')
    }
  }

  const loadPromotionData = async () => {
    loading.value = true

    try {
      const [info, recordPage, ruleData] = await Promise.all([
        getApi('/v1/promotion/info'),
        getApi('/v1/promotion/records?page=1&pageSize=20'),
        getApi('/v1/promotion/rules'),
      ])

      stats.value = {
        totalReferrals: info.totalReferrals,
        activeReferrals: info.activeReferrals,
        totalEarnings: info.totalEarnings,
        pendingEarnings: info.pendingEarnings,
      }
      referralCode.value = info.referralCode
      referralLink.value = info.referralLink
      records.value = getPageRecords(recordPage)
      rules.value = ruleData
    } catch (error) {
      toast.error(t('promotion.loadFailed'), error.message)
    } finally {
      loading.value = false
    }
  }

  return {
    stats,
    records,
    referralCode,
    referralLink,
    rules,
    loading,
    copyToClipboard,
    getStatusColor,
    getStatusText,
    loadPromotionData,
  }
}
