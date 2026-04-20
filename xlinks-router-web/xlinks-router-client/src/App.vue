<script setup>
import ToastContainer from "@/components/common/ToastContainer.vue";
import { useRouteLoading } from "@/composables/useRouteLoading";

const { isRouteLoading } = useRouteLoading();
</script>

<template>
  <div class="route-loading-indicator" :class="{ 'is-active': isRouteLoading }" aria-hidden="true" />
  <router-view />
  <ToastContainer />
</template>

<style>
.route-loading-indicator {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 3px;
  opacity: 0;
  pointer-events: none;
  z-index: 10000;
  transition: opacity 0.2s ease;
}

.route-loading-indicator::before {
  content: "";
  display: block;
  width: 36%;
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #f97316 0%, #ec4899 100%);
  transform: translateX(-130%);
}

.route-loading-indicator.is-active {
  opacity: 1;
}

.route-loading-indicator.is-active::before {
  animation: route-loading-slide 1s ease-in-out infinite;
}

@keyframes route-loading-slide {
  0% {
    transform: translateX(-130%);
  }
  100% {
    transform: translateX(380%);
  }
}
</style>
