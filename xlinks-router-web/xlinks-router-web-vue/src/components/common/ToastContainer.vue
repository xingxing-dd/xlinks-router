<script setup>
import { useToastStore } from '@/stores/toast'
import Toast from './Toast.vue'

const toastStore = useToastStore()
</script>

<template>
  <div class="fixed top-4 right-4 z-[9999] flex flex-col gap-3 pointer-events-none">
    <div class="pointer-events-auto space-y-3">
      <TransitionGroup name="list">
        <Toast
          v-for="toast in toastStore.toasts"
          :key="toast.id"
          v-bind="toast"
          @close="toastStore.removeToast"
        />
      </TransitionGroup>
    </div>
  </div>
</template>

<style scoped>
.list-enter-active,
.list-leave-active {
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}
.list-enter-from {
  opacity: 0;
  transform: translateX(100%);
}
.list-leave-to {
  opacity: 0;
  transform: scale(0.95);
  filter: blur(4px);
}
</style>
