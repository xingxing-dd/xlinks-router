<script setup>
import { onMounted, ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  Plus,
  Copy,
  Trash2,
  Settings,
  Search,
  AlertCircle,
  TrendingUp,
  ChevronDown,
  Key,
  Check,
  RotateCcw,
  Download,
} from 'lucide-vue-next'
import { useTokens } from '@/composables/useTokens'
import { cn } from '@/utils/cn'
import { toast } from '@/utils/toast'

const { t } = useI18n()

const isStatusDropdownOpen = ref(false)
const statusOptions = [
  { label: t('tokens.status.all'), value: 'all' },
  { label: t('tokens.status.enabled'), value: 'enabled' },
  { label: t('tokens.status.disabled'), value: 'disabled' },
  { label: t('tokens.status.expired'), value: 'expired' },
]

const {
  isCreateModalOpen,
  isEditModalOpen,
  newTokenName,
  selectedToken,
  settingsForm,
  availableModels,
  searchQuery,
  selectedStatus,
  loading,
  submitting,
  savingSettings,
  detailLoading,
  loadingAvailableModels,
  filteredTokens,
  loadTokens,
  loadAvailableModels,
  copyToClipboard,
  handleCreateToken,
  handleDeleteToken,
  handleToggleStatus,
  openEditModal,
  closeEditModal,
  handleSaveSettings,
  toggleAllowedModel,
  selectAllAllowedModels,
  clearAllowedModels,
  resetQuotaUsage,
  formatQuotaValue,
  getQuotaProgress,
  getDailyQuotaText,
  getTotalQuotaText,
  getTotalQuotaPercent,
  getDailyQuotaPercent,
  getDailyQuotaBarClass,
  getTotalQuotaBarClass,
} = useTokens()

const currentStatusLabel = computed(() => {
  return statusOptions.find(opt => opt.value === selectedStatus.value)?.label || t('tokens.status.all')
})

const isModelSelected = (model) => settingsForm.value.allowedModels.includes(model)
const isAllModelsSelected = computed(() => {
  return availableModels.value.length > 0 && settingsForm.value.allowedModels.length === availableModels.value.length
})
const isImportModalOpen = ref(false)
const importToken = ref(null)
const importApp = ref('codex')
const importName = ref('')
const importPrimaryModel = ref('')
const importSonnetModel = ref('')
const importOpusModel = ref('')
const importDropdownOpen = ref('')

const importModelOptions = computed(() => {
  const tokenModels = Array.isArray(importToken.value?.allowedModels) ? importToken.value.allowedModels : []
  const base = tokenModels.length > 0 ? tokenModels : availableModels.value
  return Array.from(new Set((base || []).filter(Boolean)))
})

const getImportModelLabel = (value) => {
  return value || t('tokens.ccswitchSelectModel')
}

const syncImportModels = () => {
  const options = importModelOptions.value
  const firstModel = options[0] || ''

  if (!options.includes(importPrimaryModel.value)) {
    importPrimaryModel.value = firstModel
  }
  if (!options.includes(importSonnetModel.value)) {
    importSonnetModel.value = firstModel
  }
  if (!options.includes(importOpusModel.value)) {
    importOpusModel.value = firstModel
  }
}

const openImportModal = async (token) => {
  importToken.value = token
  importApp.value = 'codex'
  importName.value = token.name
  importPrimaryModel.value = ''
  importSonnetModel.value = ''
  importOpusModel.value = ''

  await loadAvailableModels()
  syncImportModels()
  importDropdownOpen.value = ''
  isImportModalOpen.value = true
}

const closeImportModal = () => {
  isImportModalOpen.value = false
  importToken.value = null
  importDropdownOpen.value = ''
}

const toggleImportDropdown = (key) => {
  importDropdownOpen.value = importDropdownOpen.value === key ? '' : key
}

const selectImportModel = (key, model) => {
  if (key === 'primary') {
    importPrimaryModel.value = model
  } else if (key === 'sonnet') {
    importSonnetModel.value = model
  } else if (key === 'opus') {
    importOpusModel.value = model
  }
  importDropdownOpen.value = ''
}

const handleImportToCCSwitch = () => {
  if (!importToken.value) {
    return
  }
  if (!importName.value.trim()) {
    toast.warning(t('tokens.ccswitchNameRequired'))
    return
  }
  if (!importPrimaryModel.value) {
    toast.warning(t('tokens.ccswitchPrimaryRequired'))
    return
  }

  try {
    const origin = window.location.origin.replace(/\/$/, '')
    const params = new URLSearchParams({
      resource: 'provider',
      app: importApp.value,
      name: importName.value.trim(),
      endpoint: importApp.value === 'codex' ? `${origin}/v1` : origin,
      apiKey: importToken.value.key,
      model: importPrimaryModel.value,
      homepage: origin,
      enabled: 'true',
    })

    if (importApp.value === 'claude') {
      params.set('haikuModel', importPrimaryModel.value)
      if (importSonnetModel.value) {
        params.set('sonnetModel', importSonnetModel.value)
      }
      if (importOpusModel.value) {
        params.set('opusModel', importOpusModel.value)
      }
    }

    window.location.href = `ccswitch://v1/import?${params.toString()}`
    toast.info(
      t('tokens.ccswitchImportTriggered'),
      t('tokens.ccswitchImportTriggeredDetail', { name: importName.value.trim() }),
    )
    closeImportModal()
  } catch (error) {
    toast.error(t('tokens.ccswitchImportFailed'), error?.message || t('tokens.ccswitchImportFailedDetail'))
  }
}

const getCurrentDateTimeLocal = () => {
  const now = new Date()
  const offset = now.getTimezoneOffset()
  return new Date(now.getTime() - offset * 60000).toISOString().slice(0, 16)
}

onMounted(loadTokens)
</script>

<template>
  <div class="w-full max-w-[90rem] mx-auto px-4 py-4 md:px-6 md:py-8 xl:px-8">
    <!-- 鎼滅储妗嗗拰绛涢€変笅鎷夋 -->
    <div class="bg-white rounded-2xl shadow-sm border border-slate-200 p-4 mb-6">
      <div class="flex flex-col sm:flex-row gap-4">
        <div class="relative flex-1">
          <Search class="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400" />
          <input
            v-model="searchQuery"
            type="text"
            :placeholder="t('tokens.searchPlaceholder')"
            class="w-full pl-11 pr-4 py-3 border border-slate-300 rounded-xl focus:ring-2 focus:ring-ring focus:border-transparent outline-none bg-white transition-all text-slate-900"
          />
        </div>

        <!-- 鐘舵€佺瓫閫変笅鎷夋 -->
        <div class="relative">
          <button
            @click="isStatusDropdownOpen = !isStatusDropdownOpen"
            class="flex items-center justify-between gap-3 bg-gradient-button text-white px-6 py-3 rounded-xl hover:shadow-lg hover:shadow-primary/25 transition-all duration-200 whitespace-nowrap font-medium min-w-[160px]"
          >
            <span>{{ currentStatusLabel }}</span>
            <ChevronDown
              class="w-5 h-5 transition-transform duration-200"
              :class="{ 'rotate-180': isStatusDropdownOpen }"
            />
          </button>

          <!-- 涓嬫媺鑿滃崟 -->
          <template v-if="isStatusDropdownOpen">
            <!-- 鑳屾櫙閬僵 -->
            <div
              class="fixed inset-0 z-10"
              @click="isStatusDropdownOpen = false"
            />

            <!-- 涓嬫媺閫夐」 -->
            <div class="absolute top-full right-0 mt-2 bg-white rounded-xl shadow-2xl border-2 border-primary/20 overflow-hidden z-20 min-w-[160px] animate-in fade-in zoom-in duration-200">
              <button
                v-for="option in statusOptions"
                :key="option.value"
                @click="() => {
                  selectedStatus = option.value;
                  isStatusDropdownOpen = false;
                }"
                class="w-full px-4 py-3 text-left hover:bg-primary/5 transition-all duration-150 flex items-center justify-between"
                :class="[
                  selectedStatus === option.value
                    ? 'bg-primary/10 text-primary font-semibold'
                    : 'text-slate-700'
                ]"
              >
                <span>{{ option.label }}</span>
                <span v-if="selectedStatus === option.value" class="text-primary">✓</span>
              </button>
            </div>
          </template>
        </div>
      </div>
    </div>

    <!-- Token 鍒楄〃 -->
    <div class="bg-white rounded-3xl border-2 border-slate-200 shadow-sm overflow-hidden">
      <div class="bg-gradient-hero p-6">
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-3">
            <div class="w-10 h-10 bg-white/25 rounded-xl flex items-center justify-center backdrop-blur-sm border border-white/30">
              <Key class="w-5 h-5 text-white" />
            </div>
            <h2 class="text-xl font-bold text-white">
              {{ t('tokens.listTitle') }}
            </h2>
          </div>
          <button
            @click="isCreateModalOpen = true"
            class="flex items-center gap-2 bg-white text-primary px-4 md:px-6 py-2.5 md:py-3 rounded-xl hover:bg-white/90 hover:shadow-lg transition-all duration-200 font-semibold"
          >
            <Plus class="w-4 h-4 md:w-5 md:h-5" />
            <span class="hidden md:inline">{{ t('tokens.create') }}</span>
          </button>
        </div>
      </div>

      <div class="p-6">
        <!-- 鍔犺浇鐘舵€?-->
        <div v-if="loading" class="text-center py-12">
          <div class="w-12 h-12 border-4 border-primary/20 border-t-primary rounded-full animate-spin mx-auto mb-4" />
          <p class="text-slate-500">{{ t('common.loading') }}</p>
        </div>

        <!-- 绌虹姸鎬?-->
        <div v-else-if="filteredTokens.length === 0" class="text-center py-12">
          <div class="w-16 h-16 bg-slate-100 rounded-2xl flex items-center justify-center mx-auto mb-4">
            <AlertCircle class="w-8 h-8 text-slate-400" />
          </div>
          <h3 class="text-lg font-semibold text-slate-900 mb-2">
            {{ t('tokens.noTokens') }}
          </h3>
          <p class="text-slate-500 text-sm mb-4">
            {{ searchQuery ? t('tokens.noTokensSearch') : t('tokens.noTokensEmpty') }}
          </p>
          <!-- <button
            v-if="!searchQuery"
            @click="isCreateModalOpen = true"
            class="inline-flex items-center gap-2 bg-gradient-button text-white px-6 py-3 rounded-xl hover:shadow-lg hover:shadow-primary/25 transition-all duration-200 font-medium"
          >
            <Plus class="w-5 h-5" />
            <span>{{ t('tokens.create') }}</span>
          </button> -->
        </div>

        <template v-else>
          <!-- 妗岄潰绔〃鏍艰鍥?-->
          <div class="hidden md:block overflow-x-auto">
            <table class="w-full">
              <thead>
                <tr class="border-b-2 border-slate-200">
                  <th class="w-[12%] text-left py-3 px-4 text-sm font-semibold text-slate-700">
                    {{ t('tokens.table.name') }}
                  </th>
                  <th class="w-[16%] text-left py-3 px-4 text-sm font-semibold text-slate-700">
                    {{ t('tokens.table.key') }}
                  </th>
                  <th class="w-[15%] text-left py-3 px-4 text-sm font-semibold text-slate-700">
                    {{ t('tokens.table.dailyQuota') }}
                  </th>
                  <th class="w-[15%] text-left py-3 px-4 text-sm font-semibold text-slate-700">
                    {{ t('tokens.totalQuotaLabel') }}
                  </th>
                  <th class="w-[12%] text-left py-3 px-4 text-sm font-semibold text-slate-700">
                    {{ t('tokens.table.expiresAt') }}
                  </th>
                  <th class="w-[8%] text-left py-3 px-4 text-sm font-semibold text-slate-700">
                    {{ t('tokens.table.requests') }}
                  </th>
                  <th class="w-[6%] text-left py-3 px-4 text-sm font-semibold text-slate-700">
                    {{ t('tokens.table.status') }}
                  </th>
                  <th class="w-[8%] text-right py-3 px-4 text-sm font-semibold text-slate-700">
                    {{ t('tokens.table.actions') }}
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="token in filteredTokens"
                  :key="token.id"
                  class="border-b border-slate-100 hover:bg-slate-50 transition-colors group"
                >
                  <td class="max-w-[9rem] py-4 px-4">
                    <div class="flex items-center gap-2 min-w-0">
                      <!-- <div class="w-8 h-8 bg-gradient-icon rounded-lg flex items-center justify-center">
                        <Key class="w-4 h-4 text-white" />
                      </div> -->
                      <span class="truncate font-medium text-slate-900" :title="token.name">
                        {{ token.name }}
                      </span>
                    </div>
                  </td>
                  <td class="w-[16%] py-4 px-4">
                    <div class="flex items-center gap-1.5 max-w-[12rem]">
                      <code
                        :title="token.key"
                        :class="cn(
                          'text-xs font-mono text-slate-700 bg-slate-100 px-2 py-1 rounded flex-1 whitespace-nowrap truncate'
                        )"
                      >
                        {{ token.key }}
                      </code>
                      <button
                        @click="copyToClipboard(token.key, token.name)"
                        class="p-1.5 hover:bg-primary/10 rounded transition-colors opacity-0 group-hover:opacity-100"
                        :title="t('common.copy')"
                      >
                        <Copy class="w-3.5 h-3.5 text-primary" />
                      </button>
                      <button
                        @click="openImportModal(token)"
                        class="p-1.5 hover:bg-sky-50 rounded transition-colors opacity-0 group-hover:opacity-100"
                        :title="t('tokens.importToCCSwitch')"
                      >
                        <Download class="w-3.5 h-3.5 text-sky-600" />
                      </button>
                    </div>
                  </td>
                  <td class="w-[15%] py-4 px-4 min-w-[180px]">
                    <div class="space-y-2">
                      <div class="flex items-center justify-between gap-3 text-xs">
                        <span class="truncate font-medium text-slate-500" :title="getDailyQuotaText(token).label">{{ getDailyQuotaText(token).label }}</span>
                        <span class="max-w-[7rem] truncate text-right font-semibold text-slate-900" :title="getDailyQuotaText(token).value">{{ getDailyQuotaText(token).value }}</span>
                      </div>
                      <div class="h-2 rounded-full bg-slate-100 overflow-hidden">
                        <div
                          class="h-full rounded-full transition-all duration-300"
                          :class="getDailyQuotaBarClass(token)"
                          :style="{ width: `${getDailyQuotaPercent(token)}%` }"
                        />
                      </div>
                    </div>
                  </td>
                  <td class="w-[15%] py-4 px-4 min-w-[180px]">
                    <div class="space-y-2">
                      <div class="flex items-center justify-between gap-3 text-xs">
                        <span class="truncate font-medium text-slate-500" :title="getTotalQuotaText(token).label">{{ getTotalQuotaText(token).label }}</span>
                        <span class="max-w-[7rem] truncate text-right font-semibold text-slate-900" :title="getTotalQuotaText(token).value">{{ getTotalQuotaText(token).value }}</span>
                      </div>
                      <div class="h-2 rounded-full bg-slate-100 overflow-hidden">
                        <div
                          class="h-full rounded-full transition-all duration-300"
                          :class="getTotalQuotaBarClass(token)"
                          :style="{ width: `${getTotalQuotaPercent(token)}%` }"
                        />
                      </div>
                    </div>
                  </td>
                  <td class="py-4 px-4 text-sm text-slate-600">
                    <div class="truncate whitespace-nowrap" :title="token.expiresAt">
                      {{ token.expiresAt }}
                    </div>
                  </td>
                  <td class="py-4 px-4">
                    <div class="flex items-center gap-2">
                      <TrendingUp class="w-4 h-4 text-primary" />
                      <span class="text-sm font-semibold text-slate-900">
                        {{ token.requests.toLocaleString() }}
                      </span>
                    </div>
                  </td>
                  <td class="py-4 px-4">
                    <span
                      v-if="token.status === 'expired'"
                      class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-red-100 text-red-700"
                    >
                      {{ t('tokens.status.expired') }}
                    </span>
                    <button
                      v-else
                      @click="handleToggleStatus(token.id, token.status)"
                      class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium transition-all cursor-pointer hover:shadow-md"
                      :class="[
                        token.status === 'enabled'
                          ? 'bg-green-100 text-green-700 hover:bg-green-200'
                          : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
                      ]"
                      :title="t('tokens.status.toggleHint')"
                    >
                      {{ token.status === 'enabled' ? t('tokens.status.enabled') : t('tokens.status.disabled') }}
                    </button>
                  </td>
                  <td class="py-4 px-4 text-right">
                    <div class="inline-flex items-center gap-2">
                      <button
                        @click="openEditModal(token)"
                        class="p-1.5 text-primary hover:text-primary/80 transition-colors"
                        :title="t('tokens.settings.action')"
                      >
                        <Settings class="w-4 h-4" />
                      </button>
                      <button
                        @click="handleDeleteToken(token.id, token.name)"
                        class="p-1.5 text-red-500 hover:text-red-600 transition-colors"
                        :title="t('common.delete')"
                      >
                        <Trash2 class="w-4 h-4" />
                      </button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <!-- 绉诲姩绔崱鐗囪鍥?-->
          <div class="md:hidden space-y-4">
            <div
              v-for="token in filteredTokens"
              :key="token.id"
              class="bg-slate-50 rounded-2xl p-4 border border-slate-200"
            >
              <div class="flex items-center justify-between mb-3">
                <div class="flex min-w-0 items-center gap-2">
                  <div class="w-8 h-8 bg-gradient-icon rounded-lg flex items-center justify-center">
                    <Key class="w-4 h-4 text-white" />
                  </div>
                  <span class="truncate font-semibold text-slate-900" :title="token.name">
                    {{ token.name }}
                  </span>
                </div>
              </div>

              <div class="mb-3">
                <div class="flex items-center gap-2">
                  <code
                    class="flex-1 truncate rounded border border-slate-200 bg-white px-2 py-1.5 text-xs font-mono text-slate-700"
                    :title="token.key"
                  >
                    {{ token.key }}
                  </code>
                  <button
                    @click="copyToClipboard(token.key, token.name)"
                    class="p-1.5 hover:bg-primary/10 rounded transition-colors"
                  >
                    <Copy class="w-4 h-4 text-primary" />
                  </button>
                  <button
                    @click="openImportModal(token)"
                    class="p-1.5 hover:bg-sky-50 rounded transition-colors"
                    :title="t('tokens.importToCCSwitch')"
                  >
                    <Download class="w-4 h-4 text-sky-600" />
                  </button>
                </div>
              </div>

              <div class="space-y-2 mb-3">
                <div class="flex items-center justify-between text-sm">
                  <span class="text-slate-600">
                    {{ t('tokens.table.expiresAt') }}
                  </span>
                  <div class="flex min-w-0 items-center gap-2">
                    <span class="truncate text-slate-900" :title="token.expiresAt">
                      {{ token.expiresAt }}
                    </span>
                    <span
                      v-if="token.status === 'expired'"
                      class="inline-flex shrink-0 items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-red-100 text-red-700"
                    >
                      {{ t('tokens.status.expired') }}
                    </span>
                    <button
                      v-else
                      @click="handleToggleStatus(token.id, token.status)"
                      class="inline-flex shrink-0 items-center px-2.5 py-0.5 rounded-full text-xs font-medium transition-all"
                      :class="[
                        token.status === 'enabled'
                          ? 'bg-green-100 text-green-700'
                          : 'bg-slate-100 text-slate-700'
                      ]"
                    >
                      {{ token.status === 'enabled' ? t('tokens.status.enabled') : t('tokens.status.disabled') }}
                    </button>
                  </div>
                </div>
                <div class="rounded-xl bg-white p-3 border border-slate-200">
                  <div class="flex items-center justify-between gap-3 text-xs">
                    <span class="font-medium text-slate-500">{{ getDailyQuotaText(token).label }}</span>
                    <span class="font-semibold text-slate-900">{{ getDailyQuotaText(token).value }}</span>
                  </div>
                  <div class="mt-2 h-2 rounded-full bg-slate-100 overflow-hidden">
                    <div
                      class="h-full rounded-full transition-all duration-300"
                      :class="getDailyQuotaBarClass(token)"
                      :style="{ width: `${getDailyQuotaPercent(token)}%` }"
                    />
                  </div>
                </div>
                <div class="rounded-xl bg-white p-3 border border-slate-200">
                  <div class="flex items-center justify-between gap-3 text-xs">
                    <span class="font-medium text-slate-500">{{ getTotalQuotaText(token).label }}</span>
                    <span class="font-semibold text-slate-900">{{ getTotalQuotaText(token).value }}</span>
                  </div>
                  <div class="mt-2 h-2 rounded-full bg-slate-100 overflow-hidden">
                    <div
                      class="h-full rounded-full transition-all duration-300"
                      :class="getTotalQuotaBarClass(token)"
                      :style="{ width: `${getTotalQuotaPercent(token)}%` }"
                    />
                  </div>
                </div>
                <div class="flex items-center justify-between text-sm">
                  <span class="text-slate-600">
                    {{ t('tokens.table.requests') }}
                  </span>
                  <div class="flex items-center gap-1">
                    <TrendingUp class="w-4 h-4 text-primary" />
                    <span class="font-semibold text-slate-900">
                      {{ token.requests.toLocaleString() }}
                    </span>
                  </div>
                </div>
              </div>

              <div class="w-full flex items-center justify-end gap-3 border-t border-slate-200 mt-2 pt-3">
                <button
                  @click="openEditModal(token)"
                  class="p-1.5 text-primary hover:text-primary/80 transition-colors"
                  :title="t('tokens.settings.action')"
                >
                  <Settings class="w-4 h-4" />
                </button>
                <button
                  @click="handleDeleteToken(token.id, token.name)"
                  class="p-1.5 text-red-500 hover:text-red-600 transition-colors"
                  :title="t('common.delete')"
                >
                  <Trash2 class="w-4 h-4" />
                </button>
              </div>
            </div>
          </div>
        </template>
      </div>
    </div>

    <!-- 鍒涘缓 Token 妯℃€佹 -->
    <div v-if="isCreateModalOpen" class="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center z-50 p-4 animate-in fade-in duration-200">
      <div class="bg-white rounded-2xl max-w-md w-full shadow-2xl overflow-hidden animate-in zoom-in duration-200">
        <div class="bg-gradient-button p-6 text-white">
          <h2 class="text-2xl font-bold mb-1">{{ t('tokens.modal.title') }}</h2>
          <p class="text-white/80 text-sm">{{ t('tokens.modal.subtitle') }}</p>
        </div>
        <div class="p-6">
          <div class="mb-6">
            <label class="block text-sm font-semibold text-slate-900 mb-2">{{ t('tokens.modal.nameLabel') }}</label>
            <input
              v-model="newTokenName"
              type="text"
              class="w-full px-4 py-3 border-2 border-slate-300 rounded-xl focus:ring-2 focus:ring-ring focus:border-primary outline-none transition-all text-slate-900"
              :placeholder="t('tokens.modal.namePlaceholder')"
              autoFocus
              @keyup.enter="handleCreateToken"
            />
            <p class="text-xs text-slate-500 mt-2">{{ t('tokens.modal.desc') }}</p>
          </div>
          <div class="flex gap-3">
            <button
              @click="() => { isCreateModalOpen = false; newTokenName = '' }"
              class="flex-1 px-6 py-3 border-2 border-slate-300 text-slate-700 rounded-xl hover:bg-slate-50 transition-all font-medium"
            >
              {{ t('common.cancel') }}
            </button>
            <button
              @click="handleCreateToken"
              :disabled="submitting"
              class="flex-1 px-6 py-3 bg-gradient-button text-white rounded-xl hover:shadow-lg hover:shadow-primary/25 transition-all duration-200 font-medium"
            >
              {{ submitting ? t('tokens.creating') : t('common.confirm') }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="isImportModalOpen" class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 backdrop-blur-sm">
      <div class="w-full max-w-lg overflow-hidden rounded-2xl bg-white shadow-2xl">
        <div class="bg-gradient-button p-6 text-white">
          <h2 class="text-2xl font-bold">{{ t('tokens.ccswitchModalTitle') }}</h2>
          <p class="mt-1 text-sm text-white/80">{{ importToken?.name }}</p>
        </div>
        <div class="space-y-4 p-6">
          <div>
            <label class="mb-2 block text-sm font-semibold text-slate-900">{{ t('tokens.ccswitchApp') }}</label>
            <div class="grid grid-cols-2 gap-3">
              <button
                type="button"
                @click="importApp = 'codex'"
                class="rounded-xl border px-4 py-3 text-sm font-semibold transition"
                :class="importApp === 'codex' ? 'border-transparent bg-gradient-button text-white shadow-lg shadow-primary/20' : 'border-slate-300 bg-white text-slate-700'"
              >
                {{ t('tokens.ccswitchCodex') }}
              </button>
              <button
                type="button"
                @click="importApp = 'claude'"
                class="rounded-xl border px-4 py-3 text-sm font-semibold transition"
                :class="importApp === 'claude' ? 'border-transparent bg-gradient-button text-white shadow-lg shadow-primary/20' : 'border-slate-300 bg-white text-slate-700'"
              >
                {{ t('tokens.ccswitchClaude') }}
              </button>
            </div>
          </div>

          <div>
            <label class="mb-2 block text-sm font-semibold text-slate-900">{{ t('tokens.ccswitchName') }}</label>
            <input
              v-model="importName"
              type="text"
              class="w-full rounded-xl border border-slate-300 bg-white px-4 py-3 text-sm text-slate-900 outline-none transition focus:border-primary/40 focus:ring-4 focus:ring-primary/10"
              :placeholder="t('tokens.modal.namePlaceholder')"
            />
          </div>

          <div>
            <label class="mb-2 block text-sm font-semibold text-slate-900">{{ t('tokens.ccswitchPrimaryModel') }}</label>
            <div class="relative">
              <button
                type="button"
                @click="toggleImportDropdown('primary')"
                class="flex w-full items-center justify-between rounded-xl border border-slate-300 bg-white px-4 py-3 text-left text-sm text-slate-900 shadow-sm transition hover:border-primary/40 focus:border-primary/40 focus:ring-4 focus:ring-primary/10"
              >
                <span class="truncate">{{ getImportModelLabel(importPrimaryModel) }}</span>
                <ChevronDown class="h-4 w-4 shrink-0 text-slate-400 transition-transform" :class="{ 'rotate-180': importDropdownOpen === 'primary' }" />
              </button>
              <div
                v-if="importDropdownOpen === 'primary'"
                class="absolute z-20 mt-2 max-h-60 w-full overflow-y-auto rounded-xl border-2 border-primary/20 bg-white shadow-2xl"
              >
                <button
                  v-for="model in importModelOptions"
                  :key="`primary-${model}`"
                  type="button"
                  @click="selectImportModel('primary', model)"
                  class="flex w-full items-center justify-between px-4 py-3 text-left text-sm transition hover:bg-primary/5"
                  :class="importPrimaryModel === model ? 'bg-primary/10 font-semibold text-primary' : 'text-slate-700'"
                >
                  <span class="truncate">{{ model }}</span>
                  <Check v-if="importPrimaryModel === model" class="h-4 w-4 shrink-0" />
                </button>
              </div>
            </div>
          </div>

          <template v-if="importApp === 'claude'">
            <div>
              <label class="mb-2 block text-sm font-semibold text-slate-900">{{ t('tokens.ccswitchSonnetModel') }}</label>
              <div class="relative">
                <button
                  type="button"
                  @click="toggleImportDropdown('sonnet')"
                  class="flex w-full items-center justify-between rounded-xl border border-slate-300 bg-white px-4 py-3 text-left text-sm text-slate-900 shadow-sm transition hover:border-primary/40 focus:border-primary/40 focus:ring-4 focus:ring-primary/10"
                >
                  <span class="truncate">{{ getImportModelLabel(importSonnetModel) }}</span>
                  <ChevronDown class="h-4 w-4 shrink-0 text-slate-400 transition-transform" :class="{ 'rotate-180': importDropdownOpen === 'sonnet' }" />
                </button>
                <div
                  v-if="importDropdownOpen === 'sonnet'"
                  class="absolute z-20 mt-2 max-h-60 w-full overflow-y-auto rounded-xl border-2 border-primary/20 bg-white shadow-2xl"
                >
                  <button
                    v-for="model in importModelOptions"
                    :key="`sonnet-${model}`"
                    type="button"
                    @click="selectImportModel('sonnet', model)"
                    class="flex w-full items-center justify-between px-4 py-3 text-left text-sm transition hover:bg-primary/5"
                    :class="importSonnetModel === model ? 'bg-primary/10 font-semibold text-primary' : 'text-slate-700'"
                  >
                    <span class="truncate">{{ model }}</span>
                    <Check v-if="importSonnetModel === model" class="h-4 w-4 shrink-0" />
                  </button>
                </div>
              </div>
            </div>

            <div>
              <label class="mb-2 block text-sm font-semibold text-slate-900">{{ t('tokens.ccswitchOpusModel') }}</label>
              <div class="relative">
                <button
                  type="button"
                  @click="toggleImportDropdown('opus')"
                  class="flex w-full items-center justify-between rounded-xl border border-slate-300 bg-white px-4 py-3 text-left text-sm text-slate-900 shadow-sm transition hover:border-primary/40 focus:border-primary/40 focus:ring-4 focus:ring-primary/10"
                >
                  <span class="truncate">{{ getImportModelLabel(importOpusModel) }}</span>
                  <ChevronDown class="h-4 w-4 shrink-0 text-slate-400 transition-transform" :class="{ 'rotate-180': importDropdownOpen === 'opus' }" />
                </button>
                <div
                  v-if="importDropdownOpen === 'opus'"
                  class="absolute z-20 mt-2 max-h-60 w-full overflow-y-auto rounded-xl border-2 border-primary/20 bg-white shadow-2xl"
                >
                  <button
                    v-for="model in importModelOptions"
                    :key="`opus-${model}`"
                    type="button"
                    @click="selectImportModel('opus', model)"
                    class="flex w-full items-center justify-between px-4 py-3 text-left text-sm transition hover:bg-primary/5"
                    :class="importOpusModel === model ? 'bg-primary/10 font-semibold text-primary' : 'text-slate-700'"
                  >
                    <span class="truncate">{{ model }}</span>
                    <Check v-if="importOpusModel === model" class="h-4 w-4 shrink-0" />
                  </button>
                </div>
              </div>
            </div>
          </template>

          <div class="flex gap-3 pt-2">
            <button
              type="button"
              @click="closeImportModal"
              class="flex-1 rounded-xl border-2 border-slate-300 px-6 py-3 font-medium text-slate-700 transition hover:bg-slate-50"
            >
              {{ t('common.cancel') }}
            </button>
            <button
              type="button"
              @click="handleImportToCCSwitch"
              class="flex-1 rounded-xl bg-gradient-button px-6 py-3 font-medium text-white transition hover:shadow-lg hover:shadow-primary/25"
            >
              {{ t('tokens.importToCCSwitch') }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="isEditModalOpen" class="fixed inset-0 z-50 flex items-center justify-center bg-black/45 p-3 backdrop-blur-sm md:p-5">
      <div class="token-settings-scroll max-h-[92vh] w-full max-w-[52rem] overflow-y-auto rounded-[2rem] bg-white p-4 shadow-[0_28px_90px_rgba(15,23,42,0.22)] md:p-5">
        <div class="mb-3 flex items-start gap-3">
          <div class="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-gradient-button shadow-lg shadow-primary/20">
            <Settings class="h-5 w-5 text-white" />
          </div>
          <div class="min-w-0 flex-1">
            <h2 class="truncate text-[1.5rem] font-bold leading-none text-slate-900">{{ t('tokens.editTitle') }}</h2>
            <p class="mt-1 truncate text-sm text-slate-500">{{ selectedToken?.name || t('tokens.settings.action') }}</p>
          </div>
        </div>

        <div v-if="detailLoading" class="py-12 text-center">
          <div class="mx-auto mb-4 h-10 w-10 animate-spin rounded-full border-4 border-primary/20 border-t-primary" />
          <p class="text-slate-500">{{ t('common.loading') }}</p>
        </div>

        <template v-else>
          <section class="rounded-[1.5rem] bg-slate-50/90 px-4 py-4 shadow-[inset_0_1px_0_rgba(255,255,255,0.8)]">
            <div class="mb-4 text-base font-bold text-slate-900">{{ t('tokens.sections.basic') }}</div>

            <div class="space-y-3.5">
              <div>
                <label class="mb-1.5 block text-sm font-semibold text-slate-900">{{ t('tokens.modal.nameLabel') }}</label>
                <input
                  v-model="settingsForm.tokenName"
                  type="text"
                  class="w-full rounded-2xl border border-slate-300 bg-white px-4 py-3.5 text-base text-slate-900 shadow-sm outline-none transition focus:border-primary/40 focus:ring-4 focus:ring-primary/10"
                  :placeholder="t('tokens.modal.namePlaceholder')"
                />
              </div>

              <div class="grid gap-3 md:grid-cols-2">
                <div>
                  <div class="mb-1.5 flex items-center justify-between gap-3">
                    <label class="block text-sm font-semibold text-slate-900">{{ t('tokens.table.expiresAt') }}</label>
                    <button
                      type="button"
                      @click="() => {
                        settingsForm.expireEnabled = !settingsForm.expireEnabled
                        if (settingsForm.expireEnabled && !settingsForm.expireTime) settingsForm.expireTime = getCurrentDateTimeLocal()
                        if (!settingsForm.expireEnabled) settingsForm.expireTime = ''
                      }"
                      class="relative inline-flex h-7 w-12 items-center rounded-full transition-colors"
                      :class="settingsForm.expireEnabled ? 'bg-gradient-button' : 'bg-slate-300'"
                    >
                      <span
                        class="absolute h-5 w-5 rounded-full bg-white shadow transition-transform"
                        :class="settingsForm.expireEnabled ? 'translate-x-6' : 'translate-x-1'"
                      />
                    </button>
                  </div>
                  <template v-if="settingsForm.expireEnabled">
                    <input
                      v-model="settingsForm.expireTime"
                      type="datetime-local"
                      class="w-full rounded-2xl border border-slate-300 bg-white px-4 py-3.5 text-base text-slate-900 shadow-sm outline-none transition focus:border-primary/40 focus:ring-4 focus:ring-primary/10"
                    />
                  </template>
                  <template v-else>
                    <div class="rounded-2xl border border-slate-300 bg-white px-4 py-3.5 text-base text-slate-400 shadow-sm">
                      {{ t('tokens.permanent') }}
                    </div>
                  </template>
                </div>

                <div>
                  <div class="mb-1.5 flex items-center justify-between gap-3">
                    <label class="block text-sm font-semibold text-slate-900">{{ t('tokens.table.status') }}</label>
                    <button
                      type="button"
                      @click="settingsForm.status = settingsForm.status === 'enabled' ? 'disabled' : 'enabled'"
                      class="relative inline-flex h-7 w-12 items-center rounded-full transition-colors"
                      :class="settingsForm.status === 'enabled' ? 'bg-gradient-button' : 'bg-slate-300'"
                    >
                      <span
                        class="absolute h-5 w-5 rounded-full bg-white shadow transition-transform"
                        :class="settingsForm.status === 'enabled' ? 'translate-x-6' : 'translate-x-1'"
                      />
                    </button>
                  </div>
                  <div class="rounded-2xl border border-slate-300 bg-white px-4 py-3.5 text-base shadow-sm"
                    :class="settingsForm.status === 'enabled' ? 'text-slate-900' : 'text-slate-400'">
                    {{ settingsForm.status === 'enabled' ? t('tokens.status.enabled') : t('tokens.status.disabled') }}
                  </div>
                </div>
              </div>
            </div>
          </section>

          <section class="mt-4 rounded-[1.5rem] bg-slate-50/90 px-4 py-4 shadow-[inset_0_1px_0_rgba(255,255,255,0.8)]">
            <div class="flex items-center justify-between gap-4">
              <div class="text-base font-bold text-slate-900">{{ t('tokens.sections.quota') }}</div>
              <button
                type="button"
                @click="settingsForm.quotaEnabled = !settingsForm.quotaEnabled"
                class="relative inline-flex h-7 w-12 items-center rounded-full transition-colors"
                :class="settingsForm.quotaEnabled ? 'bg-gradient-button' : 'bg-slate-300'"
              >
                <span
                  class="absolute h-5 w-5 rounded-full bg-white shadow transition-transform"
                  :class="settingsForm.quotaEnabled ? 'translate-x-6' : 'translate-x-1'"
                />
              </button>
            </div>

            <template v-if="settingsForm.quotaEnabled">
              <div class="mt-3 mb-3 text-xs font-medium text-slate-500">{{ t('tokens.sections.quotaHint') }}</div>

              <div class="grid gap-3 md:grid-cols-2">
                <div>
                  <div class="mb-1.5 flex items-center justify-between gap-3">
                    <label class="block text-sm font-semibold text-slate-900">{{ t('tokens.table.dailyQuota') }}</label>
                    <button
                      type="button"
                      @click="resetQuotaUsage"
                      class="inline-flex items-center gap-1 rounded-full px-2.5 py-1 text-xs font-semibold text-orange-500 transition hover:bg-orange-50"
                    >
                      <RotateCcw class="h-3.5 w-3.5" />
                      {{ t('tokens.resetUsage') }}
                    </button>
                  </div>
                  <input
                    v-model="settingsForm.dailyQuota"
                    type="number"
                    min="0"
                    step="0.01"
                    class="w-full rounded-2xl border border-slate-300 bg-white px-4 py-3 text-base text-slate-900 shadow-sm outline-none transition focus:border-primary/40 focus:ring-4 focus:ring-primary/10"
                  />
                  <div class="mt-2 flex items-center justify-between text-xs">
                    <span class="font-medium text-slate-500">{{ t('tokens.dailyQuotaUsed') }}:</span>
                    <span class="font-semibold text-slate-500">
                      {{ formatQuotaValue(settingsForm.usedQuota) }} / {{ settingsForm.dailyQuota ? formatQuotaValue(settingsForm.dailyQuota) : t('tokens.dailyQuotaUnlimitedShort') }}
                    </span>
                  </div>
                  <div class="mt-1.5 h-2 rounded-full bg-slate-200">
                    <div
                      class="h-full rounded-full bg-gradient-to-r from-emerald-500 to-green-500"
                      :style="{ width: `${getQuotaProgress(settingsForm.usedQuota, settingsForm.dailyQuota)}%` }"
                    />
                  </div>
                </div>

                <div>
                  <label class="mb-1.5 block pt-0.5 text-sm font-semibold text-slate-900">{{ t('tokens.totalQuotaLabel') }}</label>
                  <input
                    v-model="settingsForm.totalQuota"
                    type="number"
                    min="0"
                    step="0.01"
                    class="w-full rounded-2xl border border-slate-300 bg-white px-4 py-3 text-base text-slate-900 shadow-sm outline-none transition focus:border-primary/40 focus:ring-4 focus:ring-primary/10"
                  />
                  <div class="mt-2 flex items-center justify-between text-xs">
                    <span class="font-medium text-slate-500">{{ t('tokens.totalQuotaUsed') }}:</span>
                    <span class="font-semibold text-slate-500">
                      {{ formatQuotaValue(settingsForm.totalUsedQuota) }} / {{ settingsForm.totalQuota ? formatQuotaValue(settingsForm.totalQuota) : t('tokens.dailyQuotaUnlimitedShort') }}
                    </span>
                  </div>
                  <div class="mt-1.5 h-2 rounded-full bg-slate-200">
                    <div
                      class="h-full rounded-full bg-gradient-to-r from-emerald-500 to-green-500"
                      :style="{ width: `${getQuotaProgress(settingsForm.totalUsedQuota, settingsForm.totalQuota)}%` }"
                    />
                  </div>
                </div>
              </div>
            </template>
          </section>

          <section class="mt-4 rounded-[1.5rem] bg-slate-50/90 px-4 py-4 shadow-[inset_0_1px_0_rgba(255,255,255,0.8)]">
            <div class="flex items-center justify-between gap-4">
              <div class="text-base font-bold text-slate-900">{{ t('tokens.sections.models') }}</div>
              <button
                type="button"
                @click="settingsForm.modelRestrictionEnabled = !settingsForm.modelRestrictionEnabled"
                class="relative inline-flex h-7 w-12 items-center rounded-full transition-colors"
                :class="settingsForm.modelRestrictionEnabled ? 'bg-gradient-button' : 'bg-slate-300'"
              >
                <span
                  class="absolute h-5 w-5 rounded-full bg-white shadow transition-transform"
                  :class="settingsForm.modelRestrictionEnabled ? 'translate-x-6' : 'translate-x-1'"
                />
              </button>
            </div>

            <template v-if="settingsForm.modelRestrictionEnabled">
              <div v-if="loadingAvailableModels" class="py-8 text-center text-slate-500">
                {{ t('common.loading') }}
              </div>
              <template v-else>
                <div class="mt-3 mb-3 flex items-center justify-between gap-3">
                  <p class="text-xs font-medium text-slate-500">
                    {{ t('tokens.modelsSelectedCount', { count: settingsForm.allowedModels.length }) }}
                  </p>
                  <div class="flex items-center gap-2">
                    <button
                      type="button"
                      @click="selectAllAllowedModels"
                      class="rounded-full px-3 py-1 text-xs font-semibold transition"
                      :class="isAllModelsSelected ? 'bg-slate-200 text-slate-400 cursor-not-allowed' : 'bg-white text-primary shadow-sm hover:bg-primary/5'"
                      :disabled="isAllModelsSelected"
                    >
                      {{ t('tokens.selectAllModels') }}
                    </button>
                    <button
                      type="button"
                      @click="clearAllowedModels"
                      class="rounded-full bg-white px-3 py-1 text-xs font-semibold text-slate-600 shadow-sm transition hover:bg-slate-100"
                      :disabled="settingsForm.allowedModels.length === 0"
                    >
                      {{ t('tokens.clearAllModels') }}
                    </button>
                  </div>
                </div>
                <div class="grid grid-cols-2 gap-2 sm:grid-cols-3">
                  <button
                    v-for="model in availableModels"
                    :key="model"
                    type="button"
                    @click="toggleAllowedModel(model)"
                    class="flex min-w-0 items-center justify-between rounded-xl border px-3 py-2 text-left text-xs sm:text-sm transition-all"
                    :class="isModelSelected(model)
                      ? 'border-transparent bg-gradient-button text-white shadow-lg shadow-primary/20'
                      : 'border-slate-300 bg-white text-slate-700 shadow-sm'"
                  >
                    <span class="truncate pr-2">{{ model }}</span>
                    <span
                      class="flex h-4.5 w-4.5 shrink-0 items-center justify-center rounded-full border sm:h-5 sm:w-5"
                      :class="isModelSelected(model) ? 'border-white/80 bg-white/10 text-white' : 'border-slate-300 text-transparent'"
                    >
                      <Check class="h-3 w-3 sm:h-3.5 sm:w-3.5" />
                    </span>
                  </button>
                </div>
              </template>
            </template>
          </section>

          <div class="mt-4 rounded-[1.35rem] border border-orange-200 bg-orange-50/70 px-4 py-3.5">
            <div class="flex items-start gap-3">
              <AlertCircle class="mt-0.5 h-5 w-5 shrink-0 text-orange-500" />
              <div>
                <div class="text-base font-bold text-orange-600">{{ t('tokens.noticeTitle') }}</div>
                <p class="mt-1 text-xs leading-5 text-orange-600">
                  {{ t('tokens.noticeText') }}
                </p>
              </div>
            </div>
          </div>

          <div class="mt-5 grid grid-cols-2 gap-2.5">
            <button
              @click="closeEditModal"
              class="rounded-2xl border border-slate-300 bg-white px-5 py-3 text-base font-semibold text-slate-700 shadow-sm transition hover:bg-slate-50"
            >
              {{ t('common.cancel') }}
            </button>
            <button
              @click="handleSaveSettings"
              :disabled="savingSettings"
              class="rounded-2xl bg-gradient-button px-5 py-3 text-base font-semibold text-white shadow-lg shadow-primary/25 transition hover:opacity-95 disabled:cursor-not-allowed disabled:opacity-70"
            >
              {{ savingSettings ? t('common.submitting') : t('common.save') }}
            </button>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<style scoped>
.token-settings-scroll {
  scrollbar-width: thin;
  scrollbar-color: rgba(249, 115, 22, 0.3) transparent;
}

.token-settings-scroll::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.token-settings-scroll::-webkit-scrollbar-track {
  background: transparent;
  border-radius: 10px;
}

.token-settings-scroll::-webkit-scrollbar-thumb {
  border-radius: 10px;
  background: linear-gradient(135deg, rgba(249, 115, 22, 0.4) 0%, rgba(236, 72, 153, 0.4) 50%, rgba(168, 85, 247, 0.4) 100%);
  transition: all 0.3s ease;
}

.token-settings-scroll::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(135deg, rgba(249, 115, 22, 0.7) 0%, rgba(236, 72, 153, 0.7) 50%, rgba(168, 85, 247, 0.7) 100%);
  box-shadow: 0 0 6px rgba(249, 115, 22, 0.3);
}

.token-settings-scroll::-webkit-scrollbar-thumb:active {
  background: linear-gradient(135deg, #f97316 0%, #ec4899 50%, #a855f7 100%);
}

.token-settings-scroll::-webkit-scrollbar-corner {
  background: transparent;
}
</style>

