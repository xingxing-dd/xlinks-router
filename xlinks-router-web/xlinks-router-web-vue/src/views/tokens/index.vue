<script setup>
import { ref, computed } from 'vue'
import {
  Plus,
  Copy,
  Eye,
  EyeOff,
  Trash2,
  RefreshCw,
  Search,
  X
} from 'lucide-vue-next'

const mockTokens = [
  {
    id: '1',
    name: '生产环境主Key',
    key: 'sk-abc123def456ghi789jkl012mno345pqr678stu901vwx234yz',
    created: '2026-02-15',
    lastUsed: '2 分钟前',
    requests: 12453,
    status: 'active',
  },
  {
    id: '2',
    name: '测试环境Key',
    key: 'sk-test789xyz456abc123def890ghi567jkl234mno901pqr678st',
    created: '2026-03-01',
    lastUsed: '1 小时前',
    requests: 3241,
    status: 'active',
  },
  {
    id: '3',
    name: '开发环境Key',
    key: 'sk-dev456mno123pqr789stu345vwx901yza567bcd123efg789hi',
    created: '2026-03-05',
    lastUsed: '5 天前',
    requests: 856,
    status: 'inactive',
  },
]

const tokens = ref([...mockTokens])
const visibleKeys = ref(new Set())
const isCreateModalOpen = ref(false)
const newTokenName = ref('')
const searchQuery = ref('')

const filteredTokens = computed(() => {
  const query = searchQuery.value.toLowerCase()
  return tokens.value.filter(
    token => 
      token.name.toLowerCase().includes(query) || 
      token.key.toLowerCase().includes(query)
  )
})

const toggleKeyVisibility = (id) => {
  if (visibleKeys.value.has(id)) {
    visibleKeys.value.delete(id)
  } else {
    visibleKeys.value.add(id)
  }
}

const copyToClipboard = async (text) => {
  try {
    await navigator.clipboard.writeText(text)
    alert('API Key 已复制到剪贴板')
  } catch (err) {
    const textArea = document.createElement('textarea')
    textArea.value = text
    textArea.style.position = 'fixed'
    textArea.style.left = '-999999px'
    textArea.style.top = '-999999px'
    document.body.appendChild(textArea)
    textArea.focus()
    textArea.select()
    try {
      document.execCommand('copy')
      alert('API Key 已复制到剪贴板')
    } catch (err) {
      alert('复制失败，请手动复制')
    } finally {
      document.body.removeChild(textArea)
    }
  }
}

const handleCreateToken = () => {
  if (!newTokenName.value.trim()) return

  const newToken = {
    id: String(Date.now()),
    name: newTokenName.value,
    key: `sk-new${Math.random().toString(36).substring(2, 15)}${Math.random().toString(36).substring(2, 15)}`,
    created: new Date().toISOString().split('T')[0],
    lastUsed: '从未使用',
    requests: 0,
    status: 'active',
  }

  tokens.value.unshift(newToken)
  newTokenName.value = ''
  isCreateModalOpen.value = false
}

const handleDeleteToken = (id) => {
  if (confirm('确定要删除这个 Token 吗？此操作不可撤销。')) {
    tokens.value = tokens.value.filter(t => t.id !== id)
  }
}

const maskKey = (key) => {
  return `${key.substring(0, 10)}${'*'.repeat(20)}${key.substring(key.length - 10)}`
}
</script>

<template>
  <div class="p-4 md:p-8 max-w-7xl mx-auto">
    <!-- 搜索框和创建按钮 -->
    <div class="mb-6 flex flex-col sm:flex-row gap-4">
      <div class="relative flex-1">
        <Search class="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400" />
        <input
          v-model="searchQuery"
          type="text"
          placeholder="搜索 Token 名称或 API Key..."
          class="w-full pl-11 pr-4 py-3 border border-slate-300 rounded-xl focus:ring-2 focus:ring-violet-500 focus:border-transparent outline-none bg-white text-slate-900"
        />
      </div>
      <button
        @click="isCreateModalOpen = true"
        class="flex items-center justify-center gap-2 bg-gradient-to-r from-violet-600 to-fuchsia-600 text-white px-4 py-3 rounded-xl hover:shadow-lg hover:shadow-violet-500/50 transition-all duration-200 whitespace-nowrap"
      >
        <Plus class="w-5 h-5" />
        <span>创建 Token</span>
      </button>
    </div>

    <!-- Token 列表 -->
    <div class="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden hover:shadow-lg transition-shadow">
      <div class="overflow-x-auto">
        <table class="w-full">
          <thead class="bg-slate-50 border-b border-slate-200">
            <tr>
              <th class="px-6 py-4 text-left text-sm font-semibold text-slate-900">名称</th>
              <th class="px-6 py-4 text-left text-sm font-semibold text-slate-900">API Key</th>
              <th class="px-6 py-4 text-left text-sm font-semibold text-slate-900">创建时间</th>
              <th class="px-6 py-4 text-left text-sm font-semibold text-slate-900">最后使用</th>
              <th class="px-6 py-4 text-left text-sm font-semibold text-slate-900">请求数</th>
              <th class="px-6 py-4 text-left text-sm font-semibold text-slate-900">状态</th>
              <th class="px-6 py-4 text-left text-sm font-semibold text-slate-900">操作</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-200">
            <tr
              v-for="token in filteredTokens"
              :key="token.id"
              class="hover:bg-slate-50 transition-colors group"
            >
              <td class="px-6 py-4">
                <span class="font-medium text-slate-900">{{ token.name }}</span>
              </td>
              <td class="px-6 py-4">
                <div class="flex items-center gap-2">
                  <code class="text-sm bg-slate-100 px-3 py-1 rounded-lg font-mono text-slate-600">
                    {{ visibleKeys.has(token.id) ? token.key : maskKey(token.key) }}
                  </code>
                  <button
                    @click="toggleKeyVisibility(token.id)"
                    class="p-1 hover:bg-slate-200 rounded-lg transition-colors"
                    :title="visibleKeys.has(token.id) ? '隐藏' : '显示'"
                  >
                    <component :is="visibleKeys.has(token.id) ? EyeOff : Eye" class="w-4 h-4 text-slate-600" />
                  </button>
                  <button
                    @click="copyToClipboard(token.key)"
                    class="p-1 hover:bg-slate-200 rounded-lg transition-colors"
                    title="复制"
                  >
                    <Copy class="w-4 h-4 text-slate-600" />
                  </button>
                </div>
              </td>
              <td class="px-6 py-4 text-sm text-slate-600">{{ token.created }}</td>
              <td class="px-6 py-4 text-sm text-slate-600">{{ token.lastUsed }}</td>
              <td class="px-6 py-4 text-sm text-slate-900 font-medium tabular-nums">
                {{ token.requests.toLocaleString() }}
              </td>
              <td class="px-6 py-4">
                <span
                  class="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium"
                  :class="[
                    token.status === 'active'
                      ? 'bg-green-100 text-green-700'
                      : 'bg-slate-100 text-slate-700'
                  ]"
                >
                  {{ token.status === 'active' ? '活跃' : '未激活' }}
                </span>
              </td>
              <td class="px-6 py-4">
                <div class="flex items-center gap-2">
                  <button
                    class="p-2 hover:bg-slate-100 rounded-lg transition-colors"
                    title="刷新"
                  >
                    <RefreshCw class="w-4 h-4 text-slate-600" />
                  </button>
                  <button
                    @click="handleDeleteToken(token.id)"
                    class="p-2 hover:bg-red-50 rounded-lg transition-colors"
                    title="删除"
                  >
                    <Trash2 class="w-4 h-4 text-red-600" />
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="filteredTokens.length === 0">
              <td colspan="7" class="px-6 py-12 text-center text-slate-500">
                未找到相关 Token
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 创建 Token 模态框 -->
    <div v-if="isCreateModalOpen" class="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center z-50 p-4">
      <div class="bg-white rounded-2xl max-w-md w-full p-6 shadow-2xl border border-white/20">
        <div class="flex items-center justify-between mb-4">
          <h2 class="text-xl font-bold text-slate-900">创建新 Token</h2>
          <button @click="isCreateModalOpen = false" class="p-1 hover:bg-slate-100 rounded-full transition-colors">
            <X class="w-5 h-5 text-slate-400" />
          </button>
        </div>
        
        <div class="mb-6">
          <label class="block text-sm font-medium text-slate-700 mb-2">Token 名称</label>
          <input
            v-model="newTokenName"
            type="text"
            class="w-full px-4 py-2 border border-slate-300 rounded-xl focus:ring-2 focus:ring-violet-500 focus:border-transparent outline-none text-slate-900"
            placeholder="例如：生产环境主Key"
            autoFocus
            @keyup.enter="handleCreateToken"
          />
        </div>
        
        <div class="flex gap-3">
          <button
            @click="isCreateModalOpen = false"
            class="flex-1 px-4 py-2 border border-slate-300 text-slate-700 rounded-xl hover:bg-slate-50 transition-colors font-medium"
          >
            取消
          </button>
          <button
            @click="handleCreateToken"
            class="flex-1 px-4 py-2 bg-gradient-to-r from-violet-600 to-fuchsia-600 text-white rounded-xl hover:shadow-lg hover:shadow-violet-500/50 transition-all duration-200 font-medium active:scale-[0.98]"
          >
            创建
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
