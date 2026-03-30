<script setup>
import { useI18n } from 'vue-i18n'
import { ref } from 'vue'
import { useToastStore } from '@/stores/toast'

const { t } = useI18n()
const toastStore = useToastStore()

const keyword = ref('')
const status = ref('')
const isCreateOpen = ref(false)
const createForm = ref({
  name: '',
  code: '',
  baseUrl: '',
  remark: '',
})

const resetCreateForm = () => {
  createForm.value = { name: '', code: '', baseUrl: '', remark: '' }
}

const openCreate = () => {
  resetCreateForm()
  isCreateOpen.value = true
}

const closeCreate = () => {
  isCreateOpen.value = false
}

const submitCreate = () => {
  if (!createForm.value.name || !createForm.value.code || !createForm.value.baseUrl) {
    toastStore.push('请填写服务商名称、编码与 Base URL', 'warning')
    return
  }
  toastStore.push('已提交新增服务商（静态示例）', 'success')
  isCreateOpen.value = false
}

const rows = [
  { id: 'P2001', name: 'OpenAI', endpoint: 'https://api.openai.com', status: 'active', updatedAt: '2026-03-24 09:20' },
  { id: 'P2002', name: 'Anthropic', endpoint: 'https://api.anthropic.com', status: 'active', updatedAt: '2026-03-22 11:00' },
  { id: 'P2003', name: 'Azure OpenAI', endpoint: 'https://azure.openai.com', status: 'inactive', updatedAt: '2026-03-18 15:10' },
]
</script>

<template>
  <div>
    <div class="p-6 space-y-6">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-2xl font-bold text-slate-900">{{ t('providers.title') }}</h1>
          <p class="text-slate-500 mt-1">管理服务商接入与状态（静态示例）</p>
        </div>
        <button class="btn-primary" @click="openCreate">新增服务商</button>
      </div>

      <div class="card">
        <div class="card-body grid gap-4 md:grid-cols-4">
          <div>
            <input v-model.trim="keyword" class="input mt-2" placeholder="服务商名称/ID" />
          </div>
          <div>
            <select v-model="status" class="input mt-2">
              <option value="">全部</option>
              <option value="active">启用</option>
              <option value="inactive">停用</option>
            </select>
          </div>
          <div class="md:col-span-2 flex items-end justify-end gap-3">
            <button class="btn-primary h-11">{{ t('common.search') }}</button>
            <button class="btn-outline h-11">{{ t('common.reset') }}</button>
          </div>
        </div>
      </div>

      <div class="card">
        <div class="card-header">
          <h2 class="card-title">服务商列表</h2>
          <span class="text-sm text-slate-400">共 {{ rows.length }} 条</span>
        </div>
        <div class="card-body">
          <table class="table">
            <thead>
              <tr>
                <th>服务商 ID</th>
                <th>名称</th>
                <th>API Endpoint</th>
                <th>状态</th>
                <th>更新时间</th>
                <th class="text-right">{{ t('common.action') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in rows" :key="row.id">
                <td>{{ row.id }}</td>
                <td>{{ row.name }}</td>
                <td>{{ row.endpoint }}</td>
                <td>
                  <span class="badge" :class="row.status === 'active' ? 'badge-success' : 'badge-danger'">
                    {{ row.status === 'active' ? '启用' : '停用' }}
                  </span>
                </td>
                <td>{{ row.updatedAt }}</td>
                <td>
                  <div class="flex items-center justify-end gap-2">
                    <button class="btn-outline">查看</button>
                    <button class="btn-primary">编辑</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <div v-if="isCreateOpen" class="fixed inset-0 z-50 flex items-center justify-center">
      <div class="absolute inset-0 bg-slate-900/50" @click="closeCreate"></div>
      <div class="relative w-full max-w-xl card p-6">
        <div class="flex items-center justify-between">
          <h3 class="text-lg font-semibold text-slate-800">新增服务商</h3>
          <button class="text-slate-400 hover:text-slate-600" @click="closeCreate">关闭</button>
        </div>
        <div class="mt-5 grid gap-4">
          <div>
            <label class="text-sm text-slate-500">服务商名称</label>
            <input v-model.trim="createForm.name" class="input mt-2" placeholder="例如：OpenAI" />
          </div>
          <div>
            <label class="text-sm text-slate-500">服务商编码</label>
            <input v-model.trim="createForm.code" class="input mt-2" placeholder="例如：openai" />
          </div>
          <div>
            <label class="text-sm text-slate-500">Base URL</label>
            <input v-model.trim="createForm.baseUrl" class="input mt-2" placeholder="https://api.provider.com" />
          </div>
          <div>
            <label class="text-sm text-slate-500">备注</label>
            <textarea v-model.trim="createForm.remark" class="input mt-2 h-24" placeholder="可选"></textarea>
          </div>
        </div>
        <div class="mt-6 flex justify-end gap-3">
          <button class="btn-outline h-11" @click="closeCreate">取消</button>
          <button class="btn-primary h-11" @click="submitCreate">确认新增</button>
        </div>
      </div>
    </div>
  </div>
</template>
