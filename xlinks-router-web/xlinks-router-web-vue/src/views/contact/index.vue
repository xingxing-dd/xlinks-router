<script setup>
import { reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { 
  Mail, 
  MessageCircle, 
  Phone, 
  Send, 
  MapPin, 
  Clock, 
  Github, 
  Twitter, 
  Linkedin 
} from 'lucide-vue-next'
import { postApi } from '@/utils/request'
import { toast } from '@/utils/toast'

const { t } = useI18n()

const formData = reactive({
  name: '',
  email: '',
  subject: '',
  message: '',
})

const submitting = ref(false)
const successMessage = ref('')
const errorMessage = ref('')

const handleSubmit = async () => {
  submitting.value = true
  successMessage.value = ''
  errorMessage.value = ''

  try {
    await postApi('/v1/contact', { ...formData })
    toast.success(t('contact.submitSuccess'))
    successMessage.value = ''
    formData.name = ''
    formData.email = ''
    formData.subject = ''
    formData.message = ''
  } catch (error) {
    toast.error(error.message || t('contact.submitFailed'))
    errorMessage.value = ''
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="p-4 md:p-8 max-w-7xl mx-auto">
    <div v-if="errorMessage" class="mb-4 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-600">
      {{ errorMessage }}
    </div>
    <div v-if="successMessage" class="mb-4 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">
      {{ successMessage }}
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-3 gap-8 mb-12">
      <!-- 联系方式卡片 -->
      <div class="bg-white rounded-2xl p-6 border border-slate-200 shadow-sm hover:shadow-lg transition-shadow">
        <div class="w-12 h-12 bg-gradient-to-br from-blue-500 to-cyan-500 rounded-xl flex items-center justify-center mb-4 shadow-lg">
          <Mail class="w-6 h-6 text-white" />
        </div>
        <h3 class="font-semibold text-slate-900 mb-2">
          {{ t('contact.emailSupport') }}
        </h3>
        <p class="text-sm text-slate-600 mb-3">
          {{ t('contact.emailSupportDesc') }}
        </p>
        <a
          href="mailto:support@token-hub.com"
          class="text-violet-600 hover:text-violet-700 font-medium"
        >
          support@token-hub.com
        </a>
      </div>

      <div class="bg-white rounded-2xl p-6 border border-slate-200 shadow-sm hover:shadow-lg transition-shadow">
        <div class="w-12 h-12 bg-gradient-to-br from-green-500 to-emerald-500 rounded-xl flex items-center justify-center mb-4 shadow-lg">
          <MessageCircle class="w-6 h-6 text-white" />
        </div>
        <h3 class="font-semibold text-slate-900 mb-2">
          {{ t('contact.onlineSupport') }}
        </h3>
        <p class="text-sm text-slate-600 mb-3">
          {{ t('contact.onlineSupportDesc') }}
        </p>
        <button class="text-green-600 hover:text-green-700 font-medium">
          {{ t('contact.onlineSupportBtn') }}
        </button>
      </div>

      <div class="bg-white rounded-2xl p-6 border border-slate-200 shadow-sm hover:shadow-lg transition-shadow">
        <div class="w-12 h-12 bg-gradient-to-br from-violet-500 to-purple-500 rounded-xl flex items-center justify-center mb-4 shadow-lg">
          <Phone class="w-6 h-6 text-white" />
        </div>
        <h3 class="font-semibold text-slate-900 mb-2">
          {{ t('contact.phoneSupport') }}
        </h3>
        <p class="text-sm text-slate-600 mb-3">
          {{ t('contact.phoneSupportDesc') }}
        </p>
        <a
          href="tel:+8640012345678"
          class="text-violet-600 hover:text-violet-700 font-medium"
        >
          400-123-4567
        </a>
      </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-2 gap-8">
      <!-- 联系表单 -->
      <div class="bg-white rounded-2xl p-8 border border-slate-200 shadow-sm hover:shadow-lg transition-shadow">
        <h2 class="text-xl font-bold text-slate-900 mb-6">
          {{ t('contact.formTitle') }}
        </h2>
        <form @submit.prevent="handleSubmit" class="space-y-5">
          <div>
            <label class="block text-sm font-medium text-slate-700 mb-2">
              {{ t('contact.name') }} <span class="text-red-500">*</span>
            </label>
            <input
              v-model="formData.name"
              type="text"
              class="w-full px-4 py-3 border border-slate-300 rounded-xl focus:ring-2 focus:ring-violet-500 focus:border-transparent outline-none"
              :placeholder="t('contact.namePlaceholder')"
              required
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-slate-700 mb-2">
              {{ t('contact.email') }} <span class="text-red-500">*</span>
            </label>
            <input
              v-model="formData.email"
              type="email"
              class="w-full px-4 py-3 border border-slate-300 rounded-xl focus:ring-2 focus:ring-violet-500 focus:border-transparent outline-none"
              :placeholder="t('contact.emailPlaceholder')"
              required
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-slate-700 mb-2">
              {{ t('contact.subject') }} <span class="text-red-500">*</span>
            </label>
            <select
              v-model="formData.subject"
              class="w-full px-4 py-3 border border-slate-300 rounded-xl focus:ring-2 focus:ring-violet-500 focus:border-transparent outline-none bg-white"
              required
            >
              <option value="">{{ t('contact.subjectPlaceholder') }}</option>
              <option value="technical">{{ t('contact.subjectTechnical') }}</option>
              <option value="billing">{{ t('contact.subjectBilling') }}</option>
              <option value="feature">{{ t('contact.subjectFeature') }}</option>
              <option value="bug">{{ t('contact.subjectBug') }}</option>
              <option value="other">{{ t('contact.subjectOther') }}</option>
            </select>
          </div>

          <div>
            <label class="block text-sm font-medium text-slate-700 mb-2">
              {{ t('contact.message') }} <span class="text-red-500">*</span>
            </label>
            <textarea
              v-model="formData.message"
              rows="6"
              class="w-full px-4 py-3 border border-slate-300 rounded-xl focus:ring-2 focus:ring-violet-500 focus:border-transparent outline-none resize-none"
              :placeholder="t('contact.messagePlaceholder')"
              required
            />
          </div>

          <button
            type="submit"
            :disabled="submitting"
            class="w-full bg-gradient-to-r from-violet-600 to-fuchsia-600 text-white py-3 rounded-xl hover:shadow-lg hover:shadow-violet-500/50 transition-all duration-200 font-medium flex items-center justify-center gap-2"
          >
            <Send class="w-5 h-5" />
            <span>{{ submitting ? t('contact.sendingBtn') : t('contact.sendBtn') }}</span>
          </button>
        </form>
      </div>

      <!-- 其他信息 -->
      <div class="space-y-6">
        <!-- 公司信息 -->
        <div class="bg-white rounded-2xl p-8 border border-slate-200 shadow-sm hover:shadow-lg transition-shadow">
          <h2 class="text-xl font-bold text-slate-900 mb-6">
            {{ t('contact.companyInfo') }}
          </h2>
          <div class="space-y-4">
            <div class="flex items-start gap-3">
              <MapPin class="w-5 h-5 text-slate-400 mt-1 flex-shrink-0" />
              <div>
                <p class="font-medium text-slate-900 mb-1">
                  {{ t('contact.address') }}
                </p>
                <p class="text-slate-600 whitespace-pre-line">
                  {{ t('contact.addressDetail') }}
                </p>
              </div>
            </div>

            <div class="flex items-start gap-3">
              <Clock class="w-5 h-5 text-slate-400 mt-1 flex-shrink-0" />
              <div>
                <p class="font-medium text-slate-900 mb-1">
                  {{ t('contact.workingHours') }}
                </p>
                <p class="text-slate-600 whitespace-pre-line">
                  {{ t('contact.workingHoursDetail') }}
                </p>
              </div>
            </div>

            <div class="flex items-start gap-3">
              <Mail class="w-5 h-5 text-slate-400 mt-1 flex-shrink-0" />
              <div>
                <p class="font-medium text-slate-900 mb-1">
                  {{ t('contact.businessCooperation') }}
                </p>
                <p class="text-slate-600">
                  business@token-hub.com
                </p>
              </div>
            </div>
          </div>
        </div>

        <!-- 社交媒体 -->
        <div class="bg-gradient-to-br from-violet-600 via-purple-600 to-fuchsia-600 rounded-2xl p-8 text-white shadow-2xl">
          <h2 class="text-xl font-bold mb-4 text-white">
            {{ t('contact.followUs') }}
          </h2>
          <p class="text-white/95 mb-6 font-medium">
            {{ t('contact.followUsDesc') }}
          </p>
          <div class="flex gap-4">
            <a
              href="#"
              class="w-12 h-12 bg-white/30 rounded-xl flex items-center justify-center hover:bg-white/40 transition-all backdrop-blur-sm shadow-lg border-2 border-white/30"
            >
              <Github class="w-6 h-6 text-white" />
            </a>
            <a
              href="#"
              class="w-12 h-12 bg-white/30 rounded-xl flex items-center justify-center hover:bg-white/40 transition-all backdrop-blur-sm shadow-lg border-2 border-white/30"
            >
              <Twitter class="w-6 h-6 text-white" />
            </a>
            <a
              href="#"
              class="w-12 h-12 bg-white/30 rounded-xl flex items-center justify-center hover:bg-white/40 transition-all backdrop-blur-sm shadow-lg border-2 border-white/30"
            >
              <Linkedin class="w-6 h-6 text-white" />
            </a>
          </div>
        </div>

        <!-- FAQ 快速链接 -->
        <div class="bg-white rounded-2xl p-8 border border-slate-200 shadow-sm hover:shadow-lg transition-shadow">
          <h2 class="text-xl font-bold text-slate-900 mb-4">
            {{ t('contact.faq') }}
          </h2>
          <div class="space-y-3">
            <a
              href="#"
              class="block text-violet-600 hover:text-violet-700 hover:underline font-medium"
            >
              {{ t('contact.faq1') }}
            </a>
            <a
              href="#"
              class="block text-violet-600 hover:text-violet-700 hover:underline font-medium"
            >
              {{ t('contact.faq2') }}
            </a>
            <a
              href="#"
              class="block text-violet-600 hover:text-violet-700 hover:underline font-medium"
            >
              {{ t('contact.faq3') }}
            </a>
            <a
              href="#"
              class="block text-violet-600 hover:text-violet-700 hover:underline font-medium"
            >
              {{ t('contact.faq4') }}
            </a>
            <a
              href="#"
              class="block text-violet-600 hover:text-violet-700 hover:underline font-medium"
            >
              {{ t('contact.faq5') }}
            </a>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
