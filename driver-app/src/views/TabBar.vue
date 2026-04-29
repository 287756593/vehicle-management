<template>
  <div class="tabbar-page">
    <router-view />
    <van-tabbar v-model="active" route class="custom-tabbar">
      <van-tabbar-item to="/home" icon="wap-home" class="tab-item">首页</van-tabbar-item>
      <van-tabbar-item to="/fuel" icon="gold-coin" class="tab-item">加油</van-tabbar-item>
      <van-tabbar-item to="/borrow" icon="logistics" class="tab-item">{{ borrowTabText }}</van-tabbar-item>
      <van-tabbar-item to="/profile" icon="user-o" class="tab-item">我的</van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useDriverStore } from '@/store/driver'

const active = ref(0)
const driverStore = useDriverStore()
const borrowTabText = computed(() => driverStore.hasActiveBorrow ? '还车' : '借车')
</script>

<style scoped>
.tabbar-page {
  min-height: 100dvh;
  padding-bottom: calc(54px + var(--safe-area-bottom));
  background: transparent;
}

.custom-tabbar {
  height: calc(58px + var(--safe-area-bottom));
  padding: 4px 8px calc(6px + var(--safe-area-bottom));
  margin: 0 8px 8px;
  border-radius: 18px;
  background: var(--tabbar-bg);
  backdrop-filter: blur(16px);
  box-shadow: var(--tabbar-shadow);
}

.custom-tabbar :deep(.van-tabbar-item) {
  font-size: 11px;
  min-height: 42px;
  border-radius: 12px;
  transition: all 0.22s ease;
}

.custom-tabbar :deep(.van-tabbar-item__icon) {
  font-size: 19px;
  margin-bottom: 2px;
}

.custom-tabbar :deep(.van-tabbar-item--active) {
  color: var(--brand-blue);
  background: var(--tabbar-active-bg);
}

.custom-tabbar :deep(.van-tabbar-item__icon + span) {
  font-weight: 700;
}

@media (min-width: 640px) {
  .custom-tabbar {
    max-width: var(--app-max-width);
    left: 50%;
    transform: translateX(-50%);
    margin-bottom: 14px;
  }
}
</style>
