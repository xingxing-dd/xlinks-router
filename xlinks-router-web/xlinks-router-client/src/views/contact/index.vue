<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { 
  Mail, 
  MessageCircle, 
  Phone, 
  Send, 
  MapPin, 
  Clock, 
  MessageCircleMore
} from 'lucide-vue-next'
import { getApi, postApi } from '@/utils/request'
import { toast } from '@/utils/toast'

const { t } = useI18n()
const channelConfigs = ref([])
const faqs = ref([])

const channelMetaMap = {
  email: {
    icon: Mail,
    wrapperClass: 'bg-gradient-to-br from-blue-500 to-cyan-500',
    actionClass: 'text-primary hover:text-primary',
  },
  online: {
    icon: MessageCircle,
    wrapperClass: 'bg-gradient-to-br from-green-500 to-emerald-500',
    actionClass: 'text-green-600 hover:text-green-700',
  },
  phone: {
    icon: Phone,
    wrapperClass: 'bg-gradient-icon',
    actionClass: 'text-primary hover:text-primary',
  },
}

const formData = reactive({
  name: '',
  email: '',
  subject: '',
  message: '',
})

const submitting = ref(false)
const successMessage = ref('')
const errorMessage = ref('')
const subjectOptions = ref([])
const historyItems = ref([])
const selectedHistory = ref(null)
const historyRecords = ref([])
const historyLoading = ref(false)
const recordsLoading = ref(false)
const isHistoryModalOpen = ref(false)

const loadChannelConfigs = async () => {
  try {
    channelConfigs.value = await getApi('/v1/contact/channels')
  } catch (error) {
    toast.error(error.message || t('contact.channelLoadFailed'))
  }
}

const loadFaqs = async () => {
  try {
    faqs.value = await getApi('/v1/contact/faqs')
  } catch (error) {
    toast.error(error.message || '常见问题加载失败')
  }
}

const getChannelMeta = (channelType) => channelMetaMap[channelType] || channelMetaMap.email

const resolveActionLink = (item) => {
  if (item.actionLink) return item.actionLink
  if (item.channelType === 'email' && item.contactValue) return `mailto:${item.contactValue}`
  if (item.channelType === 'phone' && item.contactValue) return `tel:${item.contactValue.replace(/[^\d+]/g, '')}`
  return '#'
}

const isButtonAction = (item) => item.channelType === 'online' && !item.contactValue

const loadSubjectOptions = async () => {
  try {
    subjectOptions.value = await getApi('/v1/contact/subjects')
  } catch (error) {
    toast.error(error.message || t('contact.subjectLoadFailed'))
  }
}

const loadHistory = async () => {
  historyLoading.value = true
  try {
    historyItems.value = await getApi('/v1/contact/history')
  } catch (error) {
    toast.error(error.message || t('contact.historyLoadFailed'))
  } finally {
    historyLoading.value = false
  }
}

const openHistoryRecords = async (item) => {
  selectedHistory.value = item
  isHistoryModalOpen.value = true
  recordsLoading.value = true
  historyRecords.value = []
  try {
    historyRecords.value = await getApi(`/v1/contact/history/${item.id}/records`)
  } catch (error) {
    toast.error(error.message || t('contact.recordLoadFailed'))
  } finally {
    recordsLoading.value = false
  }
}

const getStatusLabel = (status) => {
  return status === 1 ? t('contact.statusProcessed') : t('contact.statusPending')
}

const getStatusClass = (status) => {
  return status === 1 ? 'bg-emerald-100 text-emerald-700' : 'bg-amber-100 text-amber-700'
}

const getSenderLabel = (senderType) => {
  if (senderType === 'admin') return t('contact.senderAdmin')
  if (senderType === 'system') return t('contact.senderSystem')
  return t('contact.senderUser')
}

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
    loadHistory()
  } catch (error) {
    toast.error(error.message || t('contact.submitFailed'))
    errorMessage.value = ''
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadChannelConfigs()
  loadFaqs()
  loadSubjectOptions()
  loadHistory()
})
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
      <div
        v-for="item in channelConfigs"
        :key="item.id"
        class="bg-white rounded-2xl p-6 border border-slate-200 shadow-sm hover:shadow-lg transition-shadow"
      >
        <div :class="['w-12 h-12 rounded-xl flex items-center justify-center mb-4 shadow-lg', getChannelMeta(item.channelType).wrapperClass]">
          <component :is="getChannelMeta(item.channelType).icon" class="w-6 h-6 text-white" />
        </div>
        <h3 class="font-semibold text-slate-900 mb-2">
          {{ item.title }}
        </h3>
        <p class="text-sm text-slate-600 mb-3">
          {{ item.description }}
        </p>
        <button
          v-if="isButtonAction(item)"
          type="button"
          :class="['font-medium', getChannelMeta(item.channelType).actionClass]"
        >
          {{ item.actionLabel || item.contactValue }}
        </button>
        <a
          v-else
          :href="resolveActionLink(item)"
          :class="['font-medium break-all', getChannelMeta(item.channelType).actionClass]"
        >
          {{ item.actionLabel || item.contactValue }}
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
              class="w-full px-4 py-3 border border-slate-300 rounded-xl focus:ring-2 focus:ring-ring focus:border-transparent outline-none"
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
              class="w-full px-4 py-3 border border-slate-300 rounded-xl focus:ring-2 focus:ring-ring focus:border-transparent outline-none"
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
              class="w-full px-4 py-3 border border-slate-300 rounded-xl focus:ring-2 focus:ring-ring focus:border-transparent outline-none bg-white"
              required
            >
              <option value="">{{ t('contact.subjectPlaceholder') }}</option>
              <option
                v-for="option in subjectOptions"
                :key="option.code"
                :value="option.code"
              >
                {{ option.label }}
              </option>
            </select>
          </div>

          <div>
            <label class="block text-sm font-medium text-slate-700 mb-2">
              {{ t('contact.message') }} <span class="text-red-500">*</span>
            </label>
            <textarea
              v-model="formData.message"
              rows="6"
              class="w-full px-4 py-3 border border-slate-300 rounded-xl focus:ring-2 focus:ring-ring focus:border-transparent outline-none resize-none"
              :placeholder="t('contact.messagePlaceholder')"
              required
            />
          </div>

          <button
            type="submit"
            :disabled="submitting"
            class="w-full bg-gradient-button text-white py-3 rounded-xl hover:shadow-lg hover:shadow-primary/25 transition-all duration-200 font-medium flex items-center justify-center gap-2"
          >
            <Send class="w-5 h-5" />
            <span>{{ submitting ? t('contact.sendingBtn') : t('contact.sendBtn') }}</span>
          </button>
        </form>
      </div>

      <!-- 其他信息 -->
      <div class="space-y-6">
        <div class="bg-white rounded-2xl p-8 border border-slate-200 shadow-sm hover:shadow-lg transition-shadow">
          <div class="flex items-center justify-between mb-6 gap-4">
            <div>
              <h2 class="text-xl font-bold text-slate-900">{{ t('contact.historyTitle') }}</h2>
              <p class="text-sm text-slate-500 mt-1">{{ t('contact.historyDesc') }}</p>
            </div>
          </div>

          <div v-if="historyLoading" class="py-8 text-center text-slate-500">
            {{ t('common.loading') }}
          </div>

          <div v-else-if="!historyItems.length" class="py-8 text-center text-sm text-slate-500">
            {{ t('contact.noHistory') }}
          </div>

          <div v-else class="space-y-4">
            <div
              v-for="item in historyItems"
              :key="item.id"
              class="rounded-2xl border border-slate-200 p-4 bg-slate-50/60"
            >
              <div class="flex items-start justify-between gap-4 mb-3">
                <div>
                  <div class="flex items-center gap-2 flex-wrap">
                    <h3 class="font-semibold text-slate-900">{{ item.subjectLabel }}</h3>
                    <span class="px-2.5 py-1 rounded-full text-xs font-medium" :class="getStatusClass(item.status)">
                      {{ getStatusLabel(item.status) }}
                    </span>
                  </div>
                  <p class="text-xs text-slate-500 mt-1">{{ item.createdAt }}</p>
                </div>

                <button
                  type="button"
                  @click="openHistoryRecords(item)"
                  class="shrink-0 inline-flex items-center gap-2 rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm font-medium text-slate-700 hover:border-primary hover:text-primary transition-all"
                >
                  <MessageCircle class="w-4 h-4" />
                  {{ t('contact.viewRecords') }}
                </button>
              </div>

              <p class="text-sm text-slate-600 line-clamp-2 break-all">{{ item.message }}</p>
            </div>
          </div>
        </div>

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
        <div class="bg-gradient-hero rounded-2xl p-8 text-white shadow-2xl">
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
              aria-label="QQ"
            >
              <MessageCircleMore class="w-6 h-6 text-white" />
            </a>
          </div>
        </div>

        <!-- FAQ 快速链接 -->
        <div class="bg-white rounded-2xl p-8 border border-slate-200 shadow-sm hover:shadow-lg transition-shadow">
          <h2 class="text-xl font-bold text-slate-900 mb-4">
            {{ t('contact.faq') }}
          </h2>
          <div class="space-y-3">
            <details
              v-for="item in faqs"
              :key="item.id"
              class="group rounded-2xl border border-slate-200 bg-slate-50/80 px-4 py-3"
            >
              <summary class="cursor-pointer list-none font-medium text-primary transition-colors group-open:text-slate-900">
                {{ item.question }}
              </summary>
              <p class="mt-3 text-sm leading-6 text-slate-600 whitespace-pre-line">
                {{ item.answer }}
              </p>
            </details>
          </div>
        </div>
      </div>
    </div>
  </div>

  <div v-if="isHistoryModalOpen" class="fixed inset-0 z-[100] flex items-center justify-center bg-black/50 backdrop-blur-sm p-4">
    <div class="absolute inset-0" @click="isHistoryModalOpen = false" />
    <div class="relative w-full max-w-3xl overflow-hidden rounded-3xl bg-white shadow-2xl">
      <div class="bg-gradient-hero px-6 py-5 text-white">
        <h3 class="text-xl font-bold">{{ t('contact.recordDialogTitle') }}</h3>
        <p class="mt-1 text-sm text-white/85">{{ selectedHistory?.subjectLabel || '-' }}</p>
      </div>

      <div class="max-h-[70vh] overflow-y-auto p-6">
        <div v-if="recordsLoading" class="py-10 text-center text-slate-500">
          {{ t('common.loading') }}
        </div>

        <div v-else-if="!historyRecords.length" class="py-10 text-center text-slate-500">
          {{ t('contact.noRecords') }}
        </div>

        <div v-else class="space-y-4">
          <div
            v-for="record in historyRecords"
            :key="record.id"
            class="rounded-2xl border border-slate-200 p-4"
          >
            <div class="flex items-center justify-between gap-4 mb-2">
              <div class="flex items-center gap-2">
                <span class="inline-flex items-center rounded-full bg-slate-100 px-2.5 py-1 text-xs font-medium text-slate-700">
                  {{ getSenderLabel(record.senderType) }}
                </span>
                <span class="text-sm font-medium text-slate-900">{{ record.senderName || '-' }}</span>
              </div>
              <span class="text-xs text-slate-500">{{ record.createdAt }}</span>
            </div>
            <p class="text-sm leading-6 text-slate-700 whitespace-pre-line break-all">{{ record.content }}</p>
          </div>
        </div>
      </div>

      <div class="border-t border-slate-200 px-6 py-4 flex justify-end bg-slate-50">
        <button
          type="button"
          @click="isHistoryModalOpen = false"
          class="rounded-xl bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-800 transition-colors"
        >
          {{ t('common.confirm') }}
        </button>
      </div>
    </div>
  </div>
</template>
