import { onUnmounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { postAuth } from '@/utils/request'
import { resolveAccountTargetType } from '@/utils/auth'
import { toast } from '@/utils/toast'

const DEFAULT_COUNTDOWN = 60

export function useRegister() {
  const { t } = useI18n()
  const route = useRoute()
  const router = useRouter()

  const formData = reactive({
    phone: '',
    password: '',
    verificationCode: '',
    verificationToken: '',
    inviteCode: String(route.query.ref || '').trim().toUpperCase(),
  })

  const isSubmitting = ref(false)
  const isSendingCode = ref(false)
  const countdown = ref(0)
  const feedback = ref('')

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

  const handleSubmit = async () => {
    feedback.value = ''
    isSubmitting.value = true

    const targetType = resolveAccountTargetType(formData.phone)
    if (!targetType) {
      toast.error(t('common.error'), t('register.phone'))
      isSubmitting.value = false
      return
    }

    try {
      await postAuth('/register', {
        target: formData.phone,
        targetType,
        password: formData.password,
        code: formData.verificationCode,
        token: formData.verificationToken,
        inviteCode: formData.inviteCode || undefined,
      })

      toast.success(t('common.success'))
      router.push('/login')
    } catch (error) {
      toast.error(t('common.error'), error.message)
    } finally {
      isSubmitting.value = false
    }
  }

  const handleSendCode = async () => {
    if (!formData.phone) {
      toast.error(t('common.error'), `${t('register.phone')} ${t('common.error')}`)
      return
    }

    const targetType = resolveAccountTargetType(formData.phone)
    if (!targetType) {
      toast.error(t('common.error'), t('register.phone'))
      return
    }

    feedback.value = ''
    isSendingCode.value = true

    try {
      const data = await postAuth('/verify-code', {
        codeType: targetType,
        target: formData.phone,
        scene: 'register',
      })

      formData.verificationToken = data?.token || ''
      feedback.value = `${data?.message || t('common.success')}`
      toast.success(t('common.success'), feedback.value)
      startCountdown()
    } catch (error) {
      toast.error(t('common.error'), error.message)
    } finally {
      isSendingCode.value = false
    }
  }

  return {
    formData,
    isSubmitting,
    isSendingCode,
    countdown,
    feedback,
    handleSubmit,
    handleSendCode,
  }
}
