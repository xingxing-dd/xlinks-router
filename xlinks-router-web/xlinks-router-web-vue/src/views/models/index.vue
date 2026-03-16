<script setup>
import { ref, computed } from 'vue'
import { Search, Cpu, DollarSign, Check } from 'lucide-vue-next'

const claudeModels = [
  { id: '1', name: 'claude-3-7-sonnet', provider: 'Anthropic', description: '高性能对话模型，适合复杂推理任务', inputPrice: '$3.00/M', outputPrice: '$15.00/M', contextWindow: '200K', status: 'available' },
  { id: '2', name: 'claude-3-7-sonnet-20250219', provider: 'Anthropic', description: 'Sonnet 3.7 特定版本，稳定可靠', inputPrice: '$3.00/M', outputPrice: '$15.00/M', contextWindow: '200K', status: 'available' },
  { id: '3', name: 'claude-3-7-sonnet-20250219-thinking', provider: 'Anthropic', description: '增强思考能力的 Sonnet 版本', inputPrice: '$3.00/M', outputPrice: '$15.00/M', contextWindow: '200K', status: 'available' },
  { id: '4', name: 'claude-haiku-4-5', provider: 'Anthropic', description: '快速响应，高性价比的轻量级模型', inputPrice: '$1.00/M', outputPrice: '$5.00/M', contextWindow: '200K', status: 'available' },
  { id: '5', name: 'claude-haiku-4-5-20251001', provider: 'Anthropic', description: 'Haiku 4.5 稳定版本', inputPrice: '$1.00/M', outputPrice: '$5.00/M', contextWindow: '200K', status: 'available' },
  { id: '6', name: 'claude-opus-4', provider: 'Anthropic', description: '最强大的推理模型，适合复杂任务', inputPrice: '$15.00/M', outputPrice: '$75.00/M', contextWindow: '200K', status: 'available' },
  { id: '7', name: 'claude-opus-4-1', provider: 'Anthropic', description: 'Opus 4.1 增强版本', inputPrice: '$15.00/M', outputPrice: '$75.00/M', contextWindow: '200K', status: 'available' },
  { id: '8', name: 'claude-opus-4-1-20250805', provider: 'Anthropic', description: 'Opus 4.1 特定日期版本', inputPrice: '$15.00/M', outputPrice: '$75.00/M', contextWindow: '200K', status: 'available' },
  { id: '9', name: 'claude-opus-4-1-20250805-thinking', provider: 'Anthropic', description: '增强思考能力的 Opus 版本', inputPrice: '$15.00/M', outputPrice: '$75.00/M', contextWindow: '200K', status: 'available' },
  { id: '10', name: 'claude-opus-4-20250514', provider: 'Anthropic', description: 'Opus 4 稳定版本', inputPrice: '$15.00/M', outputPrice: '$75.00/M', contextWindow: '200K', status: 'available' },
]

const models = ref([...claudeModels])
const searchQuery = ref('')

const filteredModels = computed(() => {
  const query = searchQuery.value.toLowerCase()
  return models.value.filter(model => 
    model.name.toLowerCase().includes(query) || 
    model.description.toLowerCase().includes(query)
  )
})

const getStatusColor = (status) => {
  switch (status) {
    case 'available': return 'text-green-700 bg-green-100'
    case 'limited': return 'text-yellow-700 bg-yellow-100'
    case 'unavailable': return 'text-red-700 bg-red-100'
    default: return 'text-gray-700 bg-gray-100'
  }
}

const getStatusText = (status) => {
  switch (status) {
    case 'available': return '可用'
    case 'limited': return '限流'
    case 'unavailable': return '不可用'
    default: return '未知'
  }
}
</script>

<template>
  <div class="p-4 md:p-8 max-w-7xl mx-auto">
    <!-- 搜索和筛选 -->
    <div class="mb-6">
      <div class="relative">
        <Search class="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400" />
        <input
          v-model="searchQuery"
          type="text"
          placeholder="搜索模型..."
          class="w-full pl-11 pr-4 py-3 border border-slate-300 rounded-xl focus:ring-2 focus:ring-violet-500 focus:border-transparent outline-none bg-white text-slate-900"
        />
      </div>
    </div>

    <!-- 模型网格 -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <div
        v-for="model in filteredModels"
        :key="model.id"
        class="bg-white rounded-2xl border border-slate-200 shadow-sm p-6 hover:shadow-lg hover:border-violet-200 transition-all duration-200 group"
      >
        <div class="flex items-start justify-between mb-4">
          <div class="flex-1">
            <div class="flex items-center gap-2 mb-1">
              <h3 class="text-lg font-bold text-slate-900">{{ model.name }}</h3>
              <span 
                class="px-2 py-1 rounded-full text-xs font-medium"
                :class="getStatusColor(model.status)"
              >
                {{ getStatusText(model.status) }}
              </span>
            </div>
            <p class="text-sm text-slate-500">{{ model.provider }}</p>
          </div>
          <div class="w-12 h-12 bg-gradient-to-br from-violet-500 to-fuchsia-500 rounded-xl flex items-center justify-center shadow-lg flex-shrink-0 group-hover:scale-110 transition-transform">
            <Cpu class="w-6 h-6 text-white" />
          </div>
        </div>

        <p class="text-slate-600 mb-4 text-sm">{{ model.description }}</p>

        <div class="grid grid-cols-2 gap-4 mb-4">
          <div class="bg-slate-50 rounded-xl p-3">
            <div class="flex items-center gap-2 mb-1">
              <DollarSign class="w-4 h-4 text-slate-500" />
              <span class="text-xs text-slate-500">输入价格</span>
            </div>
            <p class="text-sm font-semibold text-slate-900 tabular-nums">
              {{ model.inputPrice }}
            </p>
          </div>
          <div class="bg-slate-50 rounded-xl p-3">
            <div class="flex items-center gap-2 mb-1">
              <DollarSign class="w-4 h-4 text-slate-500" />
              <span class="text-xs text-slate-500">输出价格</span>
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
              上下文: {{ model.contextWindow }}
            </span>
          </div>
          <div class="text-xs text-slate-500 font-medium">
            Tokens
          </div>
        </div>
      </div>
    </div>

    <div v-if="filteredModels.length === 0" class="text-center py-12">
      <p class="text-slate-500">未找到匹配的模型</p>
    </div>
  </div>
</template>
