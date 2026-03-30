<script setup>
import { storeToRefs } from 'pinia'
import Toast from './Toast.vue'
import { useToastStore } from '@/stores/toast'

const store = useToastStore()
const { toasts } = storeToRefs(store)
</script>

<template>
  <div class="fixed right-6 top-6 z-[999] space-y-3">
    <Toast
      v-for="toast in toasts"
      :key="toast.id"
      :message="toast.message"
      :type="toast.type"
      :duration="toast.duration"
      @close="store.removeToast(toast.id)"
    />
  </div>
</template>
