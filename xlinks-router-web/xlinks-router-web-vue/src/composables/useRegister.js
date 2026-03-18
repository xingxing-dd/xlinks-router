import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { postAuth } from '@/utils/request'
import { toast } from '@/utils/toast'

export function useRegister() {
  const { t } = useI18n()
  const router = useRouter()

  const formData = reactive({
    email: '',
    password: '',
    verificationCode: '',
    inviteCode: '',
  })

  const isSubmitting = ref(false)
  const isSendingCode = ref(false)
  const feedback = ref('')

  const handleSubmit = async () => {
    feedback.value = ''
    isSubmitting.value = true

    try {
      await postAuth('/register', {
        target: formData.email,
        targetType: 'email',
        password: formData.password,
        code: formData.verificationCode,
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
    if (!formData.email) {
      toast.error(t('common.error'), `${t('register.email')} ${t('common.error')}`)
      return
    }

    feedback.value = ''
    isSendingCode.value = true

    try {
      const data = await postAuth('/verify-code', {
        codeType: 'email',
        target: formData.email,
        scene: 'register',
      })

      feedback.value = `${data?.message || t('common.success')}`
      toast.success(t('common.success'), feedback.value)
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
    feedback,
    handleSubmit,
    handleSendCode,
  }
}
