<template>
  <div class="profile-page">
    <div class="header">
      <van-icon name="user-circle-o" size="60" color="var(--text-inverse)"/>
      <div class="name">{{ driverStore.driverInfo?.name }}</div>
    </div>

    <van-cell-group inset style="margin-top: 15px;">
      <van-cell title="所在部门" :value="driverStore.driverInfo?.deptName || '-'" />
      <van-cell title="驾驶员姓名" :value="driverStore.driverInfo?.name || '-'" />
    </van-cell-group>

    <div class="logout-btn">
      <van-button type="danger" block @click="handleLogout">
        退出登录
      </van-button>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { showConfirmDialog } from 'vant'
import { useDriverStore } from '@/store/driver'

const router = useRouter()
const driverStore = useDriverStore()

const handleLogout = async () => {
  try {
    await showConfirmDialog({
      title: '提示',
      message: '确定要退出登录吗？'
    })
    driverStore.logout()
    await router.replace('/login')
  } catch {
    // User cancelled the logout action.
  }
}
</script>

<style scoped>
.profile-page {
  min-height: 100dvh;
  background: transparent;
  padding-bottom: calc(72px + var(--safe-area-bottom));
}

.header {
  background: var(--action-blue-bg);
  padding: calc(30px + var(--safe-area-top)) 20px 34px;
  color: var(--text-inverse);
  text-align: center;
}

.name {
  font-size: 20px;
  font-weight: bold;
  margin-top: 10px;
}

.logout-btn {
  margin: 30px 15px 0;
}

.logout-btn :deep(.van-button) {
  height: 46px;
  border-radius: 14px;
}
</style>
