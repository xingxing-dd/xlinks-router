import { onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { postAuth } from '@/utils/request'
import { resolveAccountTargetType } from '@/utils/auth'
import { toast } from '@/utils/toast'

const DEFAULT_COUNTDOWN = 60

export function useForgotPassword() {
  const { t } = useI18n()
  const router = useRouter()

  const formData = reactive({
    account: '',
    verificationCode: '',
    newPassword: '',
    confirmPassword: '',
  })

  const isSubmitting = ref(false)
  const isSendingCode = ref(false)
  const countdown = ref(0)

  let timer = null

  const startCountdown = (seconds = DEFAULT_COUNTDOWN) => {
    countdown.value = seconds
    if (timer) {
      clearInterval(timer)
    }
    timer = setInterval(() => {
      if (countdown.value <= 1) {
        countdown.value = 0
        clearInterval(timer)
        timer = null
        return
      }
      countdown.value -= 1
    }, 1000)
  }

  onUnmounted(() => {
    if (timer) {
      clearInterval(timer)
      timer = null
    }
  })

  const handleSendCode = async () => {
    const targetType = resolveAccountTargetType(formData.account)
    if (!targetType) {
      toast.error(t('common.error'), t('forgotPassword.invalidAccount'))
      return
    }

    isSendingCode.value = true
    try {
      const data = await postAuth('/verify-code', {
        codeType: targetType,
        target: formData.account,
        scene: 'resetpwd',
      })

      toast.success(t('common.success'), data?.message || t('forgotPassword.codeSent'))
      startCountdown(data?.expireSeconds || DEFAULT_COUNTDOWN)
    } catch (error) {
      toast.error(t('common.error'), error.message)
    } finally {
      isSendingCode.value = false
    }
  }

  const handleSubmit = async () => {
    const targetType = resolveAccountTargetType(formData.account)
    if (!targetType) {
      toast.error(t('common.error'), t('forgotPassword.invalidAccount'))
      return
    }

    if (!formData.newPassword || !formData.confirmPassword) {
      toast.error(t('common.error'), t('forgotPassword.passwordRequired'))
      return
    }

    if (formData.newPassword !== formData.confirmPassword) {
      toast.error(t('common.error'), t('forgotPassword.passwordMismatch'))
      return
    }

    isSubmitting.value = true
    try {
      await postAuth('/reset-password', {
        target: formData.account,
        targetType,
        password: formData.newPassword,
        code: formData.verificationCode,
      })

      toast.success(t('common.success'), t('forgotPassword.resetSuccess'))
      router.push('/login')
    } catch (error) {
      toast.error(t('common.error'), error.message)
    } finally {
      isSubmitting.value = false
    }
  }

  return {
    formData,
    isSubmitting,
    isSendingCode,
    countdown,
    handleSendCode,
    handleSubmit,
  }
}
