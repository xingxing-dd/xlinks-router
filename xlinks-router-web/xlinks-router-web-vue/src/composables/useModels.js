import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { getApi } from '@/utils/request'
import { toast } from '@/utils/toast'

export function useModels() {
  const { t } = useI18n()

  const models = ref([
    {
      id: "1",
      name: "claude-3-7-sonnet",
      provider: "Anthropic",
      description: "高性能对话模型，适合复杂推理任务",
      inputPrice: "$3.00/M",
      outputPrice: "$15.00/M",
      contextWindow: "200K",
      status: "available",
    },
    {
      id: "2",
      name: "claude-opus-4",
      provider: "Anthropic",
      description: "最强大的推理模型，适合复杂任务",
      inputPrice: "$15.00/M",
      outputPrice: "$75.00/M",
      contextWindow: "200K",
      status: "available",
    },
    {
      id: "3",
      name: "claude-haiku-4-5",
      provider: "Anthropic",
      description: "快速响应，高性价比的轻量级模型",
      inputPrice: "$1.00/M",
      outputPrice: "$5.00/M",
      contextWindow: "200K",
      status: "available",
    },
    {
      id: "4",
      name: "claude-sonnet-4",
      provider: "Anthropic",
      description: "平衡性能与成本的智能模型",
      inputPrice: "$3.00/M",
      outputPrice: "$15.00/M",
      contextWindow: "200K",
      status: "available",
    },
    {
      id: "5",
      name: "gpt-4o",
      provider: "OpenAI",
      description: "最新旗舰模型，多模态能力强大",
      inputPrice: "$5.00/M",
      outputPrice: "$15.00/M",
      contextWindow: "128K",
      status: "available",
    },
    {
      id: "6",
      name: "gpt-4-turbo",
      provider: "OpenAI",
      description: "优化版GPT-4，速度更快成本更低",
      inputPrice: "$10.00/M",
      outputPrice: "$30.00/M",
      contextWindow: "128K",
      status: "available",
    },
    {
      id: "7",
      name: "gpt-4",
      provider: "OpenAI",
      description: "强大的推理和创作能力",
      inputPrice: "$30.00/M",
      outputPrice: "$60.00/M",
      contextWindow: "8K",
      status: "available",
    },
    {
      id: "8",
      name: "gpt-3.5-turbo",
      provider: "OpenAI",
      description: "性价比之选，适合大量对话场景",
      inputPrice: "$0.50/M",
      outputPrice: "$1.50/M",
      contextWindow: "16K",
      status: "available",
    },
    {
      id: "9",
      name: "gemini-2.0-flash-exp",
      provider: "Google",
      description: "实验性超快响应模型",
      inputPrice: "$0.00/M",
      outputPrice: "$0.00/M",
      contextWindow: "1M",
      status: "available",
    },
    {
      id: "10",
      name: "gemini-1.5-pro",
      provider: "Google",
      description: "长文本处理专家，支持百万token",
      inputPrice: "$1.25/M",
      outputPrice: "$5.00/M",
      contextWindow: "2M",
      status: "available",
    },
    {
      id: "11",
      name: "gemini-1.5-flash",
      provider: "Google",
      description: "快速高效的轻量级模型",
      inputPrice: "$0.075/M",
      outputPrice: "$0.30/M",
      contextWindow: "1M",
      status: "available",
    },
    {
      id: "12",
      name: "deepseek-chat",
      provider: "DeepSeek",
      description: "中文优化的对话模型",
      inputPrice: "$0.14/M",
      outputPrice: "$0.28/M",
      contextWindow: "64K",
      status: "available",
    },
    {
      id: "13",
      name: "deepseek-coder",
      provider: "DeepSeek",
      description: "专业代码生成模型",
      inputPrice: "$0.14/M",
      outputPrice: "$0.28/M",
      contextWindow: "64K",
      status: "available",
    },
    {
      id: "14",
      name: "mistral-large",
      provider: "Mistral",
      description: "欧洲顶级开源模型",
      inputPrice: "$2.00/M",
      outputPrice: "$6.00/M",
      contextWindow: "128K",
      status: "available",
    },
    {
      id: "15",
      name: "mistral-medium",
      provider: "Mistral",
      description: "中等规模高性价比模型",
      inputPrice: "$0.65/M",
      outputPrice: "$2.00/M",
      contextWindow: "32K",
      status: "available",
    },
    {
      id: "16",
      name: "mistral-small",
      provider: "Mistral",
      description: "快速轻量级模型",
      inputPrice: "$0.20/M",
      outputPrice: "$0.60/M",
      contextWindow: "32K",
      status: "available",
    },
    {
      id: "17",
      name: "llama-3.1-405b",
      provider: "Meta",
      description: "最大参数量开源模型",
      inputPrice: "$3.00/M",
      outputPrice: "$3.00/M",
      contextWindow: "128K",
      status: "available",
    },
    {
      id: "18",
      name: "llama-3.1-70b",
      provider: "Meta",
      description: "高性能开源模型",
      inputPrice: "$0.50/M",
      outputPrice: "$0.75/M",
      contextWindow: "128K",
      status: "available",
    },
    {
      id: "19",
      name: "llama-3.1-8b",
      provider: "Meta",
      description: "轻量级开源模型",
      inputPrice: "$0.10/M",
      outputPrice: "$0.10/M",
      contextWindow: "128K",
      status: "available",
    },
    {
      id: "20",
      name: "qwen-max",
      provider: "阿里云",
      description: "通义千问旗舰模型",
      inputPrice: "$2.00/M",
      outputPrice: "$6.00/M",
      contextWindow: "32K",
      status: "available",
    },
    {
      id: "21",
      name: "qwen-plus",
      provider: "阿里云",
      description: "性能均衡的中文模型",
      inputPrice: "$0.50/M",
      outputPrice: "$1.50/M",
      contextWindow: "32K",
      status: "available",
    },
  ])
  const searchQuery = ref('')
  const selectedProvider = ref(t('models.allProviders'))
  const isProviderDropdownOpen = ref(false)
  const loading = ref(false)

  const providers = computed(() => [
    t('models.allProviders'),
    ...Array.from(new Set(models.value.map(m => m.provider)))
  ])

  const filteredModels = computed(() => {
    const query = searchQuery.value.toLowerCase()
    return models.value.filter(model => {
      const matchesSearch = 
        model.name.toLowerCase().includes(query) || 
        model.description.toLowerCase().includes(query)
      
      const matchesProvider = 
        selectedProvider.value === t('models.allProviders') || 
        model.provider === selectedProvider.value
      
      return matchesSearch && matchesProvider
    })
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
      case 'available': return t('models.status.available')
      case 'limited': return t('models.status.limited')
      case 'unavailable': return t('models.status.unavailable')
      default: return t('models.status.unknown')
    }
  }

  const copyModelName = async (name) => {
    try {
      if (navigator.clipboard && navigator.clipboard.writeText) {
        await navigator.clipboard.writeText(name)
        toast.success(t('models.copySuccess'))
      } else {
        const textArea = document.createElement('textarea')
        textArea.value = name
        textArea.style.position = 'fixed'
        textArea.style.left = '-999999px'
        textArea.style.top = '-999999px'
        document.body.appendChild(textArea)
        textArea.focus()
        textArea.select()
        const successful = document.execCommand('copy')
        document.body.removeChild(textArea)
        if (successful) {
          toast.success(t('models.copySuccess'))
        } else {
          toast.error(t('models.copyFailed'))
        }
      }
    } catch (err) {
      toast.error(t('models.copyFailed'))
    }
  }

  const loadModels = async () => {
    loading.value = true

    try {
      const remoteModels = await getApi('/v1/models/available')
      if (remoteModels && remoteModels.length > 0) {
        models.value = remoteModels
      }
    } catch (error) {
      toast.error(t('models.loadFailed'), error.message)
    } finally {
      loading.value = false
    }
  }

  return {
    models,
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
  }
}
