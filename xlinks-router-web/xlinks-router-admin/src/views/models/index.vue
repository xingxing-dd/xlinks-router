<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import {
  createModel,
  createProviderModel,
  deleteModel,
  deleteProviderModel,
  listModels,
  listProviderModels,
  listProviders,
  updateModel,
  updateModelStatus,
  updateProviderModel,
  updateProviderModelStatus,
} from '@/api/admin'
import { useToastStore } from '@/stores/toast'
import { formatDateTime, formatNullable, formatStatus } from '@/utils/format'

const toastStore = useToastStore()

const activeTab = ref('models')
const pageSize = 50
const providers = ref([])
const modelOptions = ref([])
const modelProviderOptions = ['OPENAI', 'ANTHROPIC', 'GOOGLE', 'DEEPSEEK', 'MISTRAL', 'META', 'QWEN', 'XAI', 'OTHER']

const modelLoading = ref(false)
const modelSubmitting = ref(false)
const modelRecords = ref([])
const modelTotal = ref(0)
const modelFilters = reactive({ modelCode: '', status: '' })
const modelDialogVisible = ref(false)
const modelDialogMode = ref('create')
const currentModelId = ref(null)
const modelForm = reactive({
  modelName: '',
  modelCode: '',
  modelProvider: 'OPENAI',
  modelDesc: '',
  inputPrice: '',
  outputPrice: '',
  cacheHitPrice: '',
  contextSize: '',
  status: 1,
  remark: '',
})

const mappingLoading = ref(false)
const mappingSubmitting = ref(false)
const mappingRecords = ref([])
const mappingTotal = ref(0)
const mappingFilters = reactive({ providerId: '', modelId: '', providerModelCode: '', status: '' })
const mappingDialogVisible = ref(false)
const mappingDialogMode = ref('create')
const currentMappingId = ref(null)
const mappingForm = reactive({
  providerId: '',
  modelId: '',
  providerModelCode: '',
  providerModelName: '',
  status: 1,
  remark: '',
})

const modelCodeMap = computed(() => Object.fromEntries(modelOptions.value.map((item) => [item.id, item.modelCode])))
const providerNameMap = computed(() => Object.fromEntries(providers.value.map((item) => [item.id, item.providerName])))

const loadProviders = async () => {
  const data = await listProviders({ page: 1, pageSize: 200 })
  providers.value = data.records || []
}

const loadModelOptions = async () => {
  const data = await listModels({ page: 1, pageSize: 200 })
  modelOptions.value = data.records || []
}

const loadBaseOptions = async () => {
  try {
    await Promise.all([loadProviders(), loadModelOptions()])
  } catch (error) {
    toastStore.push(error.message || '加载基础选项失败', 'error')
  }
}

const loadModels = async () => {
  modelLoading.value = true
  try {
    const data = await listModels({ page: 1, pageSize, ...modelFilters })
    modelRecords.value = data.records || []
    modelTotal.value = data.total || 0
  } catch (error) {
    toastStore.push(error.message || '加载模型失败', 'error')
  } finally {
    modelLoading.value = false
  }
}

const loadMappings = async () => {
  mappingLoading.value = true
  try {
    const data = await listProviderModels({ page: 1, pageSize, ...mappingFilters })
    mappingRecords.value = data.records || []
    mappingTotal.value = data.total || 0
  } catch (error) {
    toastStore.push(error.message || '加载服务商模型映射失败', 'error')
  } finally {
    mappingLoading.value = false
  }
}

const resetModelForm = () => Object.assign(modelForm, {
  modelName: '',
  modelCode: '',
  modelProvider: 'OPENAI',
  modelDesc: '',
  inputPrice: '',
  outputPrice: '',
  cacheHitPrice: '',
  contextSize: '',
  status: 1,
  remark: '',
})

const resetMappingForm = () => Object.assign(mappingForm, {
  providerId: '',
  modelId: '',
  providerModelCode: '',
  providerModelName: '',
  status: 1,
  remark: '',
})

const openModelCreate = () => {
  modelDialogMode.value = 'create'
  currentModelId.value = null
  resetModelForm()
  modelDialogVisible.value = true
}

const openModelEdit = (record) => {
  modelDialogMode.value = 'edit'
  currentModelId.value = record.id
  Object.assign(modelForm, {
    modelName: record.modelName || '',
    modelCode: record.modelCode || '',
    modelProvider: record.modelProvider || 'OPENAI',
    modelDesc: record.modelDesc || '',
    inputPrice: record.inputPrice ?? '',
    outputPrice: record.outputPrice ?? '',
    cacheHitPrice: record.cacheHitPrice ?? '',
    contextSize: record.contextSize ?? '',
    status: record.status ?? 1,
    remark: record.remark || '',
  })
  modelDialogVisible.value = true
}

const submitModel = async () => {
  if (!modelForm.modelName || (modelDialogMode.value === 'create' && !modelForm.modelCode) || !modelForm.modelProvider) {
    toastStore.push('请完整填写模型名称、模型编码和模型服务商', 'warning')
    return
  }
  modelSubmitting.value = true
  try {
    const payload = {
      modelName: modelForm.modelName.trim(),
      modelCode: modelForm.modelCode.trim(),
      modelProvider: modelForm.modelProvider,
      modelDesc: modelForm.modelDesc,
      inputPrice: modelForm.inputPrice === '' ? null : Number(modelForm.inputPrice),
      outputPrice: modelForm.outputPrice === '' ? null : Number(modelForm.outputPrice),
      cacheHitPrice: modelForm.cacheHitPrice === '' ? null : Number(modelForm.cacheHitPrice),
      contextSize: modelForm.contextSize === '' ? null : Number(modelForm.contextSize),
      status: Number(modelForm.status || 1),
      remark: modelForm.remark,
    }
    if (modelDialogMode.value === 'create') {
      await createModel(payload)
      toastStore.push('标准模型创建成功', 'success')
    } else {
      await updateModel(currentModelId.value, {
        modelName: payload.modelName,
        modelProvider: payload.modelProvider,
        modelDesc: payload.modelDesc,
        inputPrice: payload.inputPrice,
        outputPrice: payload.outputPrice,
        cacheHitPrice: payload.cacheHitPrice,
        contextSize: payload.contextSize,
        remark: payload.remark,
      })
      toastStore.push('标准模型更新成功', 'success')
    }
    modelDialogVisible.value = false
    await loadModels()
    await loadModelOptions()
  } catch (error) {
    toastStore.push(error.message || '保存标准模型失败', 'error')
  } finally {
    modelSubmitting.value = false
  }
}

const toggleModelStatus = async (record) => {
  try {
    await updateModelStatus(record.id, Number(record.status) === 1 ? 0 : 1)
    toastStore.push('标准模型状态已更新', 'success')
    await loadModels()
    await loadModelOptions()
  } catch (error) {
    toastStore.push(error.message || '更新标准模型状态失败', 'error')
  }
}

const removeModel = async (record) => {
  if (!window.confirm(`确认删除标准模型“${record.modelName}”吗？`)) {
    return
  }
  try {
    await deleteModel(record.id)
    toastStore.push('标准模型已删除', 'success')
    await loadModels()
    await loadModelOptions()
  } catch (error) {
    toastStore.push(error.message || '删除标准模型失败', 'error')
  }
}

const handleMappingModelChange = () => {
  if (mappingDialogMode.value !== 'create' || !mappingForm.modelId) {
    return
  }
  const selectedModel = modelOptions.value.find((item) => Number(item.id) === Number(mappingForm.modelId))
  if (!selectedModel) {
    return
  }
  const defaultName = selectedModel.modelName || selectedModel.modelCode || ''
  mappingForm.providerModelCode = defaultName
  mappingForm.providerModelName = defaultName
}

const openMappingCreate = () => {
  mappingDialogMode.value = 'create'
  currentMappingId.value = null
  resetMappingForm()
  mappingDialogVisible.value = true
}

const openMappingEdit = (record) => {
  mappingDialogMode.value = 'edit'
  currentMappingId.value = record.id
  Object.assign(mappingForm, {
    providerId: record.providerId || '',
    modelId: record.modelId || '',
    providerModelCode: record.providerModelCode || '',
    providerModelName: record.providerModelName || '',
    status: record.status ?? 1,
    remark: record.remark || '',
  })
  mappingDialogVisible.value = true
}

const submitMapping = async () => {
  if (!mappingForm.providerId || !mappingForm.modelId || !mappingForm.providerModelCode) {
    toastStore.push('请完整填写服务商、标准模型和模型编码', 'warning')
    return
  }
  mappingSubmitting.value = true
  try {
    const payload = {
      providerId: Number(mappingForm.providerId),
      modelId: Number(mappingForm.modelId),
      providerModelCode: mappingForm.providerModelCode,
      providerModelName: mappingForm.providerModelName,
      status: Number(mappingForm.status || 1),
      remark: mappingForm.remark,
    }
    if (mappingDialogMode.value === 'create') {
      await createProviderModel(payload)
      toastStore.push('服务商映射创建成功', 'success')
    } else {
      await updateProviderModel(currentMappingId.value, payload)
      toastStore.push('服务商映射更新成功', 'success')
    }
    mappingDialogVisible.value = false
    await loadMappings()
  } catch (error) {
    toastStore.push(error.message || '保存服务商映射失败', 'error')
  } finally {
    mappingSubmitting.value = false
  }
}

const toggleMappingStatus = async (record) => {
  try {
    await updateProviderModelStatus(record.id, Number(record.status) === 1 ? 0 : 1)
    toastStore.push('服务商映射状态已更新', 'success')
    await loadMappings()
  } catch (error) {
    toastStore.push(error.message || '更新服务商映射状态失败', 'error')
  }
}

const removeMapping = async (record) => {
  if (!window.confirm(`确认删除映射“${record.providerModelCode}”吗？`)) {
    return
  }
  try {
    await deleteProviderModel(record.id)
    toastStore.push('服务商映射已删除', 'success')
    await loadMappings()
  } catch (error) {
    toastStore.push(error.message || '删除服务商映射失败', 'error')
  }
}

onMounted(async () => {
  await loadBaseOptions()
  await Promise.all([loadModels(), loadMappings()])
})
</script>

<template>
  <div class="p-6 space-y-6">
    <div class="flex flex-col gap-3 lg:flex-row lg:items-end lg:justify-between">
      <div>
        <h1 class="text-2xl font-bold text-slate-900">模型管理</h1>
        <p class="text-slate-500">维护标准模型与服务商映射。</p>
      </div>
      <div class="section-tabs self-start lg:self-auto">
        <button class="tab-button" :class="activeTab === 'models' ? 'tab-button-active' : ''" @click="activeTab = 'models'">标准模型</button>
        <button class="tab-button" :class="activeTab === 'mappings' ? 'tab-button-active' : ''" @click="activeTab = 'mappings'">服务商映射</button>
      </div>
    </div>

    <div v-if="activeTab === 'models'" class="space-y-6">
      <div class="card">
        <div class="card-body grid gap-4 md:grid-cols-3">
          <div>
            <label class="text-sm text-slate-500">模型编码</label>
            <input v-model.trim="modelFilters.modelCode" class="input mt-2" placeholder="如 gpt-5.4" />
          </div>
          <div>
            <label class="text-sm text-slate-500">状态</label>
            <select v-model="modelFilters.status" class="input mt-2">
              <option value="">全部</option>
              <option :value="1">启用</option>
              <option :value="0">停用</option>
            </select>
          </div>
          <div class="flex items-end justify-end gap-3">
            <button class="btn-primary h-11" @click="loadModels">搜索</button>
            <button class="btn-outline h-11" @click="Object.assign(modelFilters, { modelCode: '', status: '' }); loadModels()">重置</button>
            <button class="btn-primary h-11" @click="openModelCreate">新增模型</button>
          </div>
        </div>
      </div>

      <div class="card">
        <div class="card-header">
          <div>
            <h2 class="card-title">标准模型列表</h2>
            <p class="text-sm text-slate-400 mt-1">共 {{ modelTotal }} 条记录</p>
          </div>
          <button class="btn-outline" :disabled="modelLoading" @click="loadModels">{{ modelLoading ? '刷新中...' : '刷新' }}</button>
        </div>
        <div class="card-body">
          <div class="table-wrap">
            <table class="table">
              <thead>
                <tr>
                  <th>模型编码</th>
                  <th>模型名称</th>
                  <th>模型服务商</th>
                  <th>价格</th>
                  <th>上下文</th>
                  <th>状态</th>
                  <th>更新时间</th>
                  <th class="text-right">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="!modelRecords.length && !modelLoading">
                  <td colspan="8" class="empty-state">暂无标准模型数据</td>
                </tr>
                <tr v-for="record in modelRecords" :key="record.id">
                  <td>{{ record.modelCode }}</td>
                  <td>
                    <div class="font-medium text-slate-800">{{ record.modelName }}</div>
                    <div class="text-xs text-slate-400 mt-1">{{ formatNullable(record.modelDesc) }}</div>
                  </td>
                  <td>{{ record.modelProvider || '-' }}</td>
                  <td>输入 {{ record.inputPrice ?? '-' }} / 输出 {{ record.outputPrice ?? '-' }} / 缓存命中 {{ record.cacheHitPrice ?? '-' }}</td>
                  <td>{{ record.contextSize ?? '-' }}</td>
                  <td>
                    <span class="badge" :class="Number(record.status) === 1 ? 'badge-success' : 'badge-danger'">
                      {{ formatStatus(record.status) }}
                    </span>
                  </td>
                  <td>{{ formatDateTime(record.updatedAt) }}</td>
                  <td>
                    <div class="flex items-center justify-end gap-2">
                      <button class="btn-outline" @click="openModelEdit(record)">编辑</button>
                      <button class="btn-outline" @click="toggleModelStatus(record)">{{ Number(record.status) === 1 ? '停用' : '启用' }}</button>
                      <button class="btn-danger" @click="removeModel(record)">删除</button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>

    <div v-if="activeTab === 'mappings'" class="space-y-6">
      <div class="card">
        <div class="card-body grid gap-4 md:grid-cols-5">
          <div>
            <label class="text-sm text-slate-500">服务商</label>
            <select v-model="mappingFilters.providerId" class="input mt-2">
              <option value="">全部</option>
              <option v-for="provider in providers" :key="provider.id" :value="provider.id">{{ provider.providerName }}</option>
            </select>
          </div>
          <div>
            <label class="text-sm text-slate-500">标准模型</label>
            <select v-model="mappingFilters.modelId" class="input mt-2">
              <option value="">全部</option>
              <option v-for="model in modelOptions" :key="model.id" :value="model.id">{{ model.modelCode }}</option>
            </select>
          </div>
          <div>
            <label class="text-sm text-slate-500">映射编码</label>
            <input v-model.trim="mappingFilters.providerModelCode" class="input mt-2" placeholder="支持模糊搜索" />
          </div>
          <div>
            <label class="text-sm text-slate-500">状态</label>
            <select v-model="mappingFilters.status" class="input mt-2">
              <option value="">全部</option>
              <option :value="1">启用</option>
              <option :value="0">停用</option>
            </select>
          </div>
          <div class="flex items-end justify-end gap-3">
            <button class="btn-primary h-11" @click="loadMappings">搜索</button>
            <button class="btn-outline h-11" @click="Object.assign(mappingFilters, { providerId: '', modelId: '', providerModelCode: '', status: '' }); loadMappings()">重置</button>
            <button class="btn-primary h-11" @click="openMappingCreate">新增映射</button>
          </div>
        </div>
      </div>

      <div class="card">
        <div class="card-header">
          <div>
            <h2 class="card-title">服务商映射列表</h2>
            <p class="text-sm text-slate-400 mt-1">共 {{ mappingTotal }} 条记录</p>
          </div>
          <button class="btn-outline" :disabled="mappingLoading" @click="loadMappings">{{ mappingLoading ? '刷新中...' : '刷新' }}</button>
        </div>
        <div class="card-body">
          <div class="table-wrap">
            <table class="table">
              <thead>
                <tr>
                  <th>服务商</th>
                  <th>标准模型</th>
                  <th>映射编码</th>
                  <th>映射名称</th>
                  <th>状态</th>
                  <th>更新时间</th>
                  <th class="text-right">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="!mappingRecords.length && !mappingLoading">
                  <td colspan="7" class="empty-state">暂无服务商映射数据</td>
                </tr>
                <tr v-for="record in mappingRecords" :key="record.id">
                  <td>{{ providerNameMap[record.providerId] || `#${record.providerId}` }}</td>
                  <td>{{ modelCodeMap[record.modelId] || `#${record.modelId}` }}</td>
                  <td>{{ record.providerModelCode }}</td>
                  <td>
                    <div class="font-medium text-slate-800">{{ formatNullable(record.providerModelName) }}</div>
                    <div class="text-xs text-slate-400 mt-1">{{ formatNullable(record.remark) }}</div>
                  </td>
                  <td>
                    <span class="badge" :class="Number(record.status) === 1 ? 'badge-success' : 'badge-danger'">
                      {{ formatStatus(record.status) }}
                    </span>
                  </td>
                  <td>{{ formatDateTime(record.updatedAt) }}</td>
                  <td>
                    <div class="flex items-center justify-end gap-2">
                      <button class="btn-outline" @click="openMappingEdit(record)">编辑</button>
                      <button class="btn-outline" @click="toggleMappingStatus(record)">{{ Number(record.status) === 1 ? '停用' : '启用' }}</button>
                      <button class="btn-danger" @click="removeMapping(record)">删除</button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>

    <div v-if="modelDialogVisible" class="fixed inset-0 z-50 flex items-center justify-center px-4">
      <div class="absolute inset-0 bg-slate-900/50" @click="modelDialogVisible = false"></div>
      <div class="modal-panel max-w-3xl">
        <div class="flex items-center justify-between gap-4">
          <h3 class="text-lg font-semibold text-slate-800">{{ modelDialogMode === 'create' ? '新增标准模型' : '编辑标准模型' }}</h3>
          <button class="btn-text" @click="modelDialogVisible = false">关闭</button>
        </div>
        <div class="mt-6 grid gap-4 md:grid-cols-2">
          <div>
            <label class="text-sm text-slate-500">模型名称</label>
            <input v-model.trim="modelForm.modelName" class="input mt-2" placeholder="GPT-5.4" />
          </div>
          <div>
            <label class="text-sm text-slate-500">模型编码</label>
            <input v-model.trim="modelForm.modelCode" class="input mt-2" :disabled="modelDialogMode === 'edit'" placeholder="gpt-5.4" />
          </div>
          <div>
            <label class="text-sm text-slate-500">模型服务商</label>
            <select v-model="modelForm.modelProvider" class="input mt-2">
              <option v-for="provider in modelProviderOptions" :key="provider" :value="provider">{{ provider }}</option>
            </select>
          </div>
          <div>
            <label class="text-sm text-slate-500">上下文长度</label>
            <input v-model="modelForm.contextSize" type="number" class="input mt-2" placeholder="200000" />
          </div>
          <div>
            <label class="text-sm text-slate-500">输入价格</label>
            <input v-model="modelForm.inputPrice" type="number" step="0.01" class="input mt-2" placeholder="5" />
          </div>
          <div>
            <label class="text-sm text-slate-500">输出价格</label>
            <input v-model="modelForm.outputPrice" type="number" step="0.01" class="input mt-2" placeholder="20" />
          </div>
          <div>
            <label class="text-sm text-slate-500">缓存命中价格</label>
            <input v-model="modelForm.cacheHitPrice" type="number" step="0.01" class="input mt-2" placeholder="可选" />
          </div>
          <div v-if="modelDialogMode === 'create'">
            <label class="text-sm text-slate-500">初始状态</label>
            <select v-model.number="modelForm.status" class="input mt-2">
              <option :value="1">启用</option>
              <option :value="0">停用</option>
            </select>
          </div>
          <div class="md:col-span-2">
            <label class="text-sm text-slate-500">模型说明</label>
            <textarea v-model.trim="modelForm.modelDesc" class="input mt-2 min-h-24" placeholder="可选"></textarea>
          </div>
          <div class="md:col-span-2">
            <label class="text-sm text-slate-500">备注</label>
            <textarea v-model.trim="modelForm.remark" class="input mt-2 min-h-24" placeholder="可选"></textarea>
          </div>
        </div>
        <div class="mt-6 flex justify-end gap-3">
          <button class="btn-outline" @click="modelDialogVisible = false">取消</button>
          <button class="btn-primary" :disabled="modelSubmitting" @click="submitModel">{{ modelSubmitting ? '提交中...' : '确认保存' }}</button>
        </div>
      </div>
    </div>

    <div v-if="mappingDialogVisible" class="fixed inset-0 z-50 flex items-center justify-center px-4">
      <div class="absolute inset-0 bg-slate-900/50" @click="mappingDialogVisible = false"></div>
      <div class="modal-panel max-w-3xl">
        <div class="flex items-center justify-between gap-4">
          <h3 class="text-lg font-semibold text-slate-800">{{ mappingDialogMode === 'create' ? '新增服务商映射' : '编辑服务商映射' }}</h3>
          <button class="btn-text" @click="mappingDialogVisible = false">关闭</button>
        </div>
        <div class="mt-6 grid gap-4 md:grid-cols-2">
          <div>
            <label class="text-sm text-slate-500">服务商</label>
            <select v-model="mappingForm.providerId" class="input mt-2">
              <option value="">请选择服务商</option>
              <option v-for="provider in providers" :key="provider.id" :value="provider.id">{{ provider.providerName }}</option>
            </select>
          </div>
          <div>
            <label class="text-sm text-slate-500">标准模型</label>
            <select v-model="mappingForm.modelId" class="input mt-2" @change="handleMappingModelChange">
              <option value="">请选择标准模型</option>
              <option v-for="model in modelOptions" :key="model.id" :value="model.id">{{ model.modelCode }}</option>
            </select>
          </div>
          <div>
            <label class="text-sm text-slate-500">模型编码</label>
            <input v-model.trim="mappingForm.providerModelCode" class="input mt-2" placeholder="上游模型编码" />
          </div>
          <div>
            <label class="text-sm text-slate-500">模型名称</label>
            <input v-model.trim="mappingForm.providerModelName" class="input mt-2" placeholder="上游模型展示名" />
          </div>
          <div v-if="mappingDialogMode === 'create'">
            <label class="text-sm text-slate-500">初始状态</label>
            <select v-model.number="mappingForm.status" class="input mt-2">
              <option :value="1">启用</option>
              <option :value="0">停用</option>
            </select>
          </div>
          <div class="md:col-span-2">
            <label class="text-sm text-slate-500">备注</label>
            <textarea v-model.trim="mappingForm.remark" class="input mt-2 min-h-24" placeholder="可选"></textarea>
          </div>
        </div>
        <div class="mt-6 flex justify-end gap-3">
          <button class="btn-outline" @click="mappingDialogVisible = false">取消</button>
          <button class="btn-primary" :disabled="mappingSubmitting" @click="submitMapping">{{ mappingSubmitting ? '提交中...' : '确认保存' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>
