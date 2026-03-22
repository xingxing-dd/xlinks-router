<script setup>
import { onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { Search, Cpu, DollarSign, Check, ChevronDown, Copy } from 'lucide-vue-next'
import { useModels } from '@/composables/useModels'

const { t } = useI18n()

const {
  searchQuery,
  selectedProvider,
  isProviderDropdownOpen,
  providers,
  loading,
  filteredModels,
  getStatusColor,
  getStatusText,
  copyModelName,
  loadModels,
} = useModels()

onMounted(loadModels)
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
            :placeholder="t('models.searchPlaceholder')"
            class="w-full pl-11 pr-4 py-3 border border-slate-300 rounded-xl focus:ring-2 focus:ring-ring focus:border-transparent outline-none bg-white transition-all text-slate-900"
          />
        </div>
        
        <!-- 服务商筛选下拉框 -->
        <div class="relative">
          <button
            @click="isProviderDropdownOpen = !isProviderDropdownOpen"
            class="flex items-center justify-between gap-3 bg-gradient-button text-white px-6 py-3 rounded-xl hover:shadow-lg hover:shadow-primary/25 transition-all duration-200 whitespace-nowrap font-medium min-w-[160px]"
          >
            <span>{{ selectedProvider }}</span>
            <ChevronDown class="w-5 h-5 transition-transform duration-200" :class="{ 'rotate-180': isProviderDropdownOpen }" />
          </button>
          
          <!-- 下拉菜单 -->
          <template v-if="isProviderDropdownOpen">
            <!-- 背景遮罩 -->
            <div
              class="fixed inset-0 z-10"
              @click="isProviderDropdownOpen = false"
            />
            
            <!-- 下拉选项 -->
            <div class="absolute top-full right-0 mt-2 bg-white rounded-xl shadow-2xl border-2 border-primary/20 overflow-hidden z-20 min-w-[160px] animate-in fade-in zoom-in duration-200">
              <button
                v-for="provider in providers"
                :key="provider"
                @click="() => { selectedProvider = provider; isProviderDropdownOpen = false }"
                class="w-full px-4 py-3 text-left hover:bg-primary/5 transition-all duration-150"
                :class="[
                  selectedProvider === provider
                    ? 'bg-primary/10 text-primary font-semibold'
                    : 'text-slate-700'
                ]"
              >
                {{ provider }}
                <span v-if="selectedProvider === provider" class="ml-2">✓</span>
              </button>
            </div>
          </template>
        </div>
      </div>
    </div>

    <!-- 模型网格 -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      <div v-if="loading" class="md:col-span-2 lg:col-span-3 rounded-2xl border border-slate-200 bg-white py-12 text-center text-slate-500">
        {{ t('common.loading') }}
      </div>
      <div
        v-for="model in filteredModels"
        :key="model.id"
        class="bg-white rounded-2xl border border-slate-200 shadow-sm p-6 hover:shadow-lg hover:border-primary/20 transition-all duration-200 group"
      >
        <div class="flex items-start justify-between mb-4">
          <div class="flex-1">
            <div class="flex items-center gap-2 mb-1">
              <h3 class="text-lg font-bold text-slate-900">{{ model.name }}</h3>
              <button
                @click="copyModelName(model.name)"
                class="p-1 hover:bg-primary/10 rounded-md transition-colors duration-200"
                :title="t('models.copyModelName')"
              >
                <Copy class="w-4 h-4 text-slate-400 hover:text-primary" />
              </button>
              <span 
                class="px-2 py-1 rounded-full text-xs font-medium"
                :class="getStatusColor(model.status)"
              >
                {{ getStatusText(model.status) }}
              </span>
            </div>
            <p class="text-sm text-slate-500">{{ model.provider }}</p>
          </div>
          <div class="w-12 h-12 bg-gradient-icon rounded-xl flex items-center justify-center shadow-lg flex-shrink-0 group-hover:scale-110 transition-transform">
            <Cpu class="w-6 h-6 text-white" />
          </div>
        </div>

        <p class="text-slate-600 mb-4 text-sm">{{ model.description }}</p>

        <div class="grid grid-cols-2 gap-4 mb-4">
          <div class="bg-slate-50 rounded-xl p-3">
            <div class="flex items-center gap-2 mb-1">
              <DollarSign class="w-4 h-4 text-slate-500" />
              <span class="text-xs text-slate-500">{{ t('models.inputPrice') }}</span>
            </div>
            <p class="text-sm font-semibold text-slate-900 tabular-nums">
              {{ model.inputPrice }}
            </p>
          </div>
          <div class="bg-slate-50 rounded-xl p-3">
            <div class="flex items-center gap-2 mb-1">
              <DollarSign class="w-4 h-4 text-slate-500" />
              <span class="text-xs text-slate-500">{{ t('models.outputPrice') }}</span>
            </div>
            <p class="text-sm font-semibold text-slate-900 tabular-nums">
              {{ model.outputPrice }}
            </p>
          </div>
        </div>

        <div class="flex items-center justify-between pt-4 border-t border-slate-200">
          <div class="flex items-center gap-2">
            <Check class="w-4 h-4 text-green-600" />
            <span class="text-sm text-slate-600 font-medium">
              {{ t('models.context') }}: {{ model.contextWindow }}
            </span>
          </div>
          <div class="text-xs text-slate-500 font-medium">
            Tokens
          </div>
        </div>
      </div>
    </div>

    <div v-if="!loading && filteredModels.length === 0" class="text-center py-12">
      <p class="text-slate-500">{{ t('models.noModels') }}</p>
    </div>
  </div>
</template>
