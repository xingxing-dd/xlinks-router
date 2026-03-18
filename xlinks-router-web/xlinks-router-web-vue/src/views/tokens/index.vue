<script setup>
import { onMounted } from 'vue'
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
} from 'lucide-vue-next'
import { useTokens } from '@/composables/useTokens'

const { t } = useI18n()

const {
  visibleKeys,
  isCreateModalOpen,
  newTokenName,
  searchQuery,
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

onMounted(loadTokens)
</script>

<template>
  <div class="p-6 space-y-6 max-w-7xl mx-auto">
    <!-- 搜索框和创建按钮 -->
    <div class="bg-white rounded-2xl shadow-sm border border-slate-200 p-4">
      <div class="flex flex-col sm:flex-row gap-4">
        <div class="relative flex-1">
          <Search class="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400" />
          <input
            v-model="searchQuery"
            type="text"
            :placeholder="t('tokens.searchPlaceholder')"
            class="w-full pl-11 pr-4 py-3 border border-slate-300 rounded-xl focus:ring-2 focus:ring-violet-500 focus:border-transparent outline-none bg-white transition-all text-slate-900"
          />
        </div>
        <button
          @click="isCreateModalOpen = true"
          class="flex items-center justify-center gap-2 bg-gradient-to-r from-violet-600 to-fuchsia-600 text-white px-6 py-3 rounded-xl hover:shadow-lg hover:shadow-violet-500/50 transition-all duration-200 whitespace-nowrap font-medium"
        >
          <Plus class="w-5 h-5" />
          <span>{{ t('tokens.create') }}</span>
        </button>
      </div>
    </div>

    <!-- Token 列表 -->
    <div class="bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden">
      <div v-if="!loading && filteredTokens.length === 0" class="p-12 text-center">
        <AlertCircle class="w-12 h-12 text-slate-400 mx-auto mb-4" />
        <h3 class="text-lg font-semibold text-slate-900 mb-2">
          {{ t('tokens.noTokens') }}
        </h3>
        <p class="text-slate-600 text-sm">
          {{ searchQuery ? t('tokens.noTokensSearch') : t('tokens.noTokensEmpty') }}
        </p>
      </div>
      <div v-else class="overflow-x-auto">
        <table class="w-full">
          <thead class="bg-gradient-to-r from-slate-50 to-slate-100 border-b border-slate-200">
            <tr>
              <th class="px-6 py-4 text-left text-sm font-semibold text-slate-900">{{ t('tokens.table.name') }}</th>
              <th class="px-6 py-4 text-left text-sm font-semibold text-slate-900">{{ t('tokens.table.key') }}</th>
              <th class="px-6 py-4 text-left text-sm font-semibold text-slate-900">{{ t('tokens.table.created') }}</th>
              <th class="px-6 py-4 text-left text-sm font-semibold text-slate-900">{{ t('tokens.table.expiresAt') }}</th>
              <th class="px-6 py-4 text-left text-sm font-semibold text-slate-900">{{ t('tokens.table.requests') }}</th>
              <th class="px-6 py-4 text-left text-sm font-semibold text-slate-900">{{ t('tokens.table.status') }}</th>
              <th class="px-6 py-4 text-left text-sm font-semibold text-slate-900">{{ t('tokens.table.actions') }}</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-200">
            <tr v-if="loading">
              <td colspan="7" class="px-6 py-12 text-center text-slate-500">
                {{ t('common.loading') }}
              </td>
            </tr>
            <tr
              v-for="token in filteredTokens"
              v-else
              :key="token.id"
              class="hover:bg-slate-50 transition-colors group"
            >
              <td class="px-6 py-4">
                <p class="font-semibold text-slate-900">{{ token.name }}</p>
              </td>
              <td class="px-6 py-4">
                <div class="flex items-center gap-2 max-w-xs">
                  <code class="text-xs font-mono text-slate-700 bg-slate-100 px-2 py-1 rounded flex-1 truncate">
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
                    class="p-1.5 hover:bg-violet-100 rounded transition-colors opacity-0 group-hover:opacity-100"
                    :title="t('common.copy')"
                  >
                    <Copy class="w-3.5 h-3.5 text-violet-600" />
                  </button>
                </div>
              </td>
              <td class="px-6 py-4 text-sm text-slate-900">{{ token.created }}</td>
              <td class="px-6 py-4 text-sm text-slate-900">{{ token.expiresAt }}</td>
              <td class="px-6 py-4">
                <div class="flex items-center gap-2">
                  <TrendingUp class="w-4 h-4 text-violet-600" />
                  <span class="text-sm font-semibold text-slate-900">
                    {{ token.requests.toLocaleString() }}
                  </span>
                </div>
              </td>
              <td class="px-6 py-4">
                <span
                  v-if="token.status === 'expired'"
                  class="inline-flex items-center px-3 py-1 rounded-full text-xs font-medium bg-red-100 text-red-700 cursor-not-allowed"
                  :title="t('tokens.status.expiredHint')"
                >
                  {{ t('tokens.status.expired') }}
                </span>
                <button
                  v-else
                  @click="handleToggleStatus(token.id, token.status)"
                  class="inline-flex items-center px-3 py-1 rounded-full text-xs font-medium transition-all cursor-pointer hover:shadow-md"
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
              <td class="px-6 py-4">
                <button
                  @click="handleDeleteToken(token.id, token.name)"
                  class="p-2 hover:bg-red-50 rounded-lg transition-colors group/delete"
                  :title="t('common.delete')"
                >
                  <Trash2 class="w-4 h-4 text-red-600 group-hover/delete:scale-110 transition-transform" />
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 创建 Token 模态框 -->
    <div v-if="isCreateModalOpen" class="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center z-50 p-4 animate-in fade-in duration-200">
      <div class="bg-white rounded-2xl max-w-md w-full shadow-2xl overflow-hidden animate-in zoom-in duration-200">
        <div class="bg-gradient-to-r from-violet-600 to-fuchsia-600 p-6 text-white">
          <h2 class="text-2xl font-bold mb-1">{{ t('tokens.modal.title') }}</h2>
          <p class="text-white/80 text-sm">{{ t('tokens.modal.subtitle') }}</p>
        </div>
        <div class="p-6">
          <div class="mb-6">
            <label class="block text-sm font-semibold text-slate-900 mb-2">{{ t('tokens.modal.nameLabel') }}</label>
            <input
              v-model="newTokenName"
              type="text"
              class="w-full px-4 py-3 border-2 border-slate-300 rounded-xl focus:ring-2 focus:ring-violet-500 focus:border-violet-500 outline-none transition-all text-slate-900"
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
              class="flex-1 px-6 py-3 bg-gradient-to-r from-violet-600 to-fuchsia-600 text-white rounded-xl hover:shadow-lg hover:shadow-violet-500/50 transition-all duration-200 font-medium"
            >
              {{ submitting ? t('tokens.creating') : t('common.confirm') }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
