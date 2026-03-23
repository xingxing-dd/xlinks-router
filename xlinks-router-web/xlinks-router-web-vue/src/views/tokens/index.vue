<script setup>
import { onMounted, ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  Plus,
  Copy,
  Eye,
  EyeOff,
  Trash2,
  Search,
  AlertCircle,
  TrendingUp,
  ChevronDown,
  Key,
} from 'lucide-vue-next'
import { useTokens } from '@/composables/useTokens'
import { cn } from '@/utils/cn'

const { t } = useI18n()

const isStatusDropdownOpen = ref(false)
const statusOptions = [
  { label: t('tokens.status.all'), value: 'all' },
  { label: t('tokens.status.enabled'), value: 'enabled' },
  { label: t('tokens.status.disabled'), value: 'disabled' },
  { label: t('tokens.status.expired'), value: 'expired' },
]

const {
  visibleKeys,
  isCreateModalOpen,
  newTokenName,
  searchQuery,
  selectedStatus,
  loading,
  submitting,
  filteredTokens,
  loadTokens,
  toggleKeyVisibility,
  copyToClipboard,
  handleCreateToken,
  handleDeleteToken,
  handleToggleStatus,
  maskKey,
} = useTokens()

const currentStatusLabel = computed(() => {
  return statusOptions.find(opt => opt.value === selectedStatus.value)?.label || t('tokens.status.all')
})

onMounted(loadTokens)
</script>

<template>
  <div class="p-4 md:p-8 max-w-7xl mx-auto">
    <!-- 搜索框和筛选下拉框 -->
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

        <!-- 状态筛选下拉框 -->
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

          <!-- 下拉菜单 -->
          <template v-if="isStatusDropdownOpen">
            <!-- 背景遮罩 -->
            <div
              class="fixed inset-0 z-10"
              @click="isStatusDropdownOpen = false"
            />

            <!-- 下拉选项 -->
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

    <!-- Token 列表 -->
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
        <!-- 加载状态 -->
        <div v-if="loading" class="text-center py-12">
          <div class="w-12 h-12 border-4 border-primary/20 border-t-primary rounded-full animate-spin mx-auto mb-4" />
          <p class="text-slate-500">{{ t('common.loading') }}</p>
        </div>

        <!-- 空状态 -->
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
          <button
            v-if="!searchQuery"
            @click="isCreateModalOpen = true"
            class="inline-flex items-center gap-2 bg-gradient-button text-white px-6 py-3 rounded-xl hover:shadow-lg hover:shadow-primary/25 transition-all duration-200 font-medium"
          >
            <Plus class="w-5 h-5" />
            <span>{{ t('tokens.create') }}</span>
          </button>
        </div>

        <template v-else>
          <!-- 桌面端表格视图 -->
          <div class="hidden md:block overflow-x-auto">
            <table class="w-full">
              <thead>
                <tr class="border-b-2 border-slate-200">
                  <th class="text-left py-3 px-4 text-sm font-semibold text-slate-700">
                    {{ t('tokens.table.name') }}
                  </th>
                  <th class="text-left py-3 px-4 text-sm font-semibold text-slate-700">
                    {{ t('tokens.table.key') }}
                  </th>
                  <th class="text-left py-3 px-4 text-sm font-semibold text-slate-700">
                    {{ t('tokens.table.created') }}
                  </th>
                  <th class="text-left py-3 px-4 text-sm font-semibold text-slate-700">
                    {{ t('tokens.table.expiresAt') }}
                  </th>
                  <th class="text-left py-3 px-4 text-sm font-semibold text-slate-700">
                    {{ t('tokens.table.requests') }}
                  </th>
                  <th class="text-left py-3 px-4 text-sm font-semibold text-slate-700">
                    {{ t('tokens.table.status') }}
                  </th>
                  <th class="text-right py-3 px-4 text-sm font-semibold text-slate-700">
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
                  <td class="py-4 px-4">
                    <div class="flex items-center gap-2">
                      <div class="w-8 h-8 bg-gradient-icon rounded-lg flex items-center justify-center">
                        <Key class="w-4 h-4 text-white" />
                      </div>
                      <span class="font-medium text-slate-900">
                        {{ token.name }}
                      </span>
                    </div>
                  </td>
                  <td class="py-4 px-4">
                    <div class="flex items-center gap-2 max-w-xs">
                      <code
                        :class="cn(
                          'text-xs font-mono text-slate-700 bg-slate-100 px-2 py-1 rounded flex-1',
                          visibleKeys.has(token.id) ? 'overflow-x-auto whitespace-nowrap' : 'truncate'
                        )"
                      >
                        {{ visibleKeys.has(token.id) ? token.key : maskKey(token.key) }}
                      </code>
                      <button
                        @click="toggleKeyVisibility(token.id)"
                        class="p-1.5 hover:bg-slate-200 rounded transition-colors opacity-0 group-hover:opacity-100"
                        :title="visibleKeys.has(token.id) ? t('common.hide') : t('common.show')"
                      >
                        <component :is="visibleKeys.has(token.id) ? EyeOff : Eye" class="w-3.5 h-3.5 text-slate-600" />
                      </button>
                      <button
                        @click="copyToClipboard(token.key, token.name)"
                        class="p-1.5 hover:bg-primary/10 rounded transition-colors opacity-0 group-hover:opacity-100"
                        :title="t('common.copy')"
                      >
                        <Copy class="w-3.5 h-3.5 text-primary" />
                      </button>
                    </div>
                  </td>
                  <td class="py-4 px-4 text-sm text-slate-600">
                    {{ token.created }}
                  </td>
                  <td class="py-4 px-4 text-sm text-slate-600">
                    {{ token.expiresAt }}
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
                    <button
                      @click="handleDeleteToken(token.id, token.name)"
                      class="inline-flex items-center gap-1 text-sm text-red-600 hover:text-red-700 font-medium transition-colors"
                      :title="t('common.delete')"
                    >
                      <Trash2 class="w-4 h-4" />
                      {{ t('common.delete') }}
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <!-- 移动端卡片视图 -->
          <div class="md:hidden space-y-4">
            <div
              v-for="token in filteredTokens"
              :key="token.id"
              class="bg-slate-50 rounded-2xl p-4 border border-slate-200"
            >
              <div class="flex items-center justify-between mb-3">
                <div class="flex items-center gap-2">
                  <div class="w-8 h-8 bg-gradient-icon rounded-lg flex items-center justify-center">
                    <Key class="w-4 h-4 text-white" />
                  </div>
                  <span class="font-semibold text-slate-900">
                    {{ token.name }}
                  </span>
                </div>
                <span
                  v-if="token.status === 'expired'"
                  class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-red-100 text-red-700"
                >
                  {{ t('tokens.status.expired') }}
                </span>
                <button
                  v-else
                  @click="handleToggleStatus(token.id, token.status)"
                  class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium transition-all"
                  :class="[
                    token.status === 'enabled'
                      ? 'bg-green-100 text-green-700'
                      : 'bg-slate-100 text-slate-700'
                  ]"
                >
                  {{ token.status === 'enabled' ? t('tokens.status.enabled') : t('tokens.status.disabled') }}
                </button>
              </div>

              <div class="mb-3">
                <div class="flex items-center gap-2">
                  <code
                    :class="cn(
                      'text-xs font-mono text-slate-700 bg-white px-2 py-1.5 rounded flex-1 border border-slate-200',
                      visibleKeys.has(token.id) ? 'overflow-x-auto whitespace-nowrap' : 'truncate'
                    )"
                  >
                    {{ visibleKeys.has(token.id) ? token.key : maskKey(token.key) }}
                  </code>
                  <button
                    @click="toggleKeyVisibility(token.id)"
                    class="p-1.5 hover:bg-slate-200 rounded transition-colors"
                  >
                    <component :is="visibleKeys.has(token.id) ? EyeOff : Eye" class="w-4 h-4 text-slate-600" />
                  </button>
                  <button
                    @click="copyToClipboard(token.key, token.name)"
                    class="p-1.5 hover:bg-primary/10 rounded transition-colors"
                  >
                    <Copy class="w-4 h-4 text-primary" />
                  </button>
                </div>
              </div>

              <div class="space-y-2 mb-3">
                <div class="flex items-center justify-between text-sm">
                  <span class="text-slate-600">
                    {{ t('tokens.table.created') }}
                  </span>
                  <span class="text-slate-900">
                    {{ token.created }}
                  </span>
                </div>
                <div class="flex items-center justify-between text-sm">
                  <span class="text-slate-600">
                    {{ t('tokens.table.expiresAt') }}
                  </span>
                  <span class="text-slate-900">
                    {{ token.expiresAt }}
                  </span>
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

              <button
                @click="handleDeleteToken(token.id, token.name)"
                class="w-full flex items-center justify-center gap-2 text-sm text-red-600 hover:text-red-700 font-medium transition-colors py-2 border-t border-slate-200 mt-2"
              >
                <Trash2 class="w-4 h-4" />
                {{ t('common.delete') }}
              </button>
            </div>
          </div>
        </template>
      </div>
    </div>

    <!-- 创建 Token 模态框 -->
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
  </div>
</template>
