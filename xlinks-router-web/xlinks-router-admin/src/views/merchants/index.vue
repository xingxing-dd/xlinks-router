<script setup>
import { useI18n } from 'vue-i18n'
import { ref } from 'vue'

const { t } = useI18n()

const keyword = ref('')
const status = ref('')

const rows = [
  { id: 'M1001', name: '未来科技', contact: 'li@future.com', status: 'active', createdAt: '2026-03-21 10:30' },
  { id: 'M1002', name: '星云智能', contact: 'ops@nebula.com', status: 'pending', createdAt: '2026-03-22 09:10' },
  { id: 'M1003', name: '蓝鲸数据', contact: 'finance@whale.com', status: 'inactive', createdAt: '2026-03-20 16:40' },
]
</script>

<template>
  <div class="p-6 space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-slate-900">{{ t('merchants.title') }}</h1>
        <p class="text-slate-500 mt-1">维护商户账号、实名与权限（静态示例）</p>
      </div>
      <button class="btn-primary">新增商户</button>
    </div>

    <div class="card">
      <div class="card-body grid gap-4 md:grid-cols-4">
        <div>
          <input v-model.trim="keyword" class="input mt-2" placeholder="商户名称/ID" />
        </div>
        <div>
          <select v-model="status" class="input mt-2">
            <option value="">全部</option>
            <option value="active">启用</option>
            <option value="pending">待审核</option>
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
        <h2 class="card-title">商户列表</h2>
        <span class="text-sm text-slate-400">共 {{ rows.length }} 条</span>
      </div>
      <div class="card-body">
        <table class="table">
          <thead>
            <tr>
              <th>商户 ID</th>
              <th>商户名称</th>
              <th>联系人</th>
              <th>状态</th>
              <th>创建时间</th>
              <th class="text-right">{{ t('common.action') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in rows" :key="row.id">
              <td>{{ row.id }}</td>
              <td>{{ row.name }}</td>
              <td>{{ row.contact }}</td>
              <td>
                <span
                  class="badge"
                  :class="row.status === 'active'
                    ? 'badge-success'
                    : row.status === 'pending'
                      ? 'badge-warning'
                      : 'badge-danger'"
                >
                  {{ row.status === 'active' ? '启用' : row.status === 'pending' ? '待审核' : '停用' }}
                </span>
              </td>
              <td>{{ row.createdAt }}</td>
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
</template>
