<template>
  <div class="login-page">
    <div class="login-bg">
      <div class="bg-circle bg-circle-1"></div>
      <div class="bg-circle bg-circle-2"></div>
      <div class="bg-circle bg-circle-3"></div>
    </div>

    <div class="login-header">
      <div class="logo-wrapper">
        <van-icon name="car-o" size="50" color="var(--text-inverse)" />
      </div>
      <h1>公务车辆管理</h1>
      <p>公务车驾驶员端</p>
    </div>

    <div class="login-form">
      <div class="form-title">欢迎登录公务车辆管理系统</div>
      <van-cell-group inset class="custom-group">
        <van-field
          v-model="selectedDriverName"
          is-link
          readonly
          label="驾驶员"
          placeholder="请选择驾驶员"
          @click="showDriverPicker = true"
          class="custom-field"
        />
      </van-cell-group>

      <div class="login-btn">
        <van-button type="primary" block size="large" :loading="loading" @click="handleLogin" class="custom-button">
          {{ loading ? '登录中...' : '登 录' }}
        </van-button>
      </div>

    </div>

    <van-popup v-model:show="showDriverPicker" position="bottom" round>
      <van-picker
        title="选择驾驶员"
        :columns="driverList"
        @confirm="onDriverConfirm"
        @cancel="showDriverPicker = false"
        confirm-button-text="确定"
        cancel-button-text="取消"
      />
    </van-popup>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { useDriverStore } from '@/store/driver'
import { getDrivers, driverLogin } from '@/api'

const router = useRouter()
const driverStore = useDriverStore()

const loading = ref(false)
const driverList = ref([])
const selectedDriverName = ref('')
const selectedDriverId = ref(null)
const showDriverPicker = ref(false)

const loadDrivers = async () => {
  try {
    const res = await getDrivers()
    driverList.value = (res || []).map(driver => ({
      text: driver.driverName + (driver.phone ? ' - ' + driver.phone : ''),
      value: driver.id,
      driverName: driver.driverName
    }))
  } catch (error) {
    console.error('Failed to load drivers:', error)
    showToast('加载驾驶员列表失败')
  }
}

onMounted(() => {
  loadDrivers()
})

const onDriverConfirm = (payload) => {
  const selectedIndex = payload?.selectedIndexes?.[0]
  const selectedValue = payload?.selectedValues?.[0]
  const selected = payload?.selectedOptions?.[0]
    || (typeof selectedIndex === 'number' ? driverList.value[selectedIndex] : null)
    || (selectedValue != null ? driverList.value.find(driver => driver.value === selectedValue) : null)
    || null

  if (!selected?.value) {
    showToast('请选择有效的驾驶员')
    return
  }

  selectedDriverId.value = selected.value
  selectedDriverName.value = selected.driverName || selected.text
  showDriverPicker.value = false
}

const handleLogin = async () => {
  if (!selectedDriverId.value) {
    showToast('请选择驾驶员')
    return
  }

  loading.value = true
  try {
    const res = await driverLogin({ driverId: selectedDriverId.value })
    // Decode JWT exp to store token expiry time
    let expiresAt = null
    try {
      const payload = JSON.parse(atob(res.token.split('.')[1]))
      if (payload.exp) {
        expiresAt = payload.exp * 1000 // convert to ms
      }
    } catch (e) {
      // If decode fails, leave expiresAt as null
    }
    driverStore.login({
      id: res.driverId,
      userId: res.userId,
      username: res.username,
      name: res.driverName || selectedDriverName.value,
      deptName: res.deptName || ''
    }, res.token, expiresAt)
    showToast('登录成功')
    router.replace('/home')
  } catch (e) {
    showToast(e?.message || e.response?.data?.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100dvh;
  background: var(--auth-bg);
  padding: calc(36px + var(--safe-area-top)) 16px calc(24px + var(--safe-area-bottom));
  position: relative;
  overflow: hidden;
}

.login-bg {
  position: absolute;
  width: 100%;
  height: 100%;
  top: 0;
  left: 0;
  pointer-events: none;
}

.bg-circle {
  position: absolute;
  border-radius: 50%;
  opacity: 0.1;
}

.bg-circle-1 {
  width: 300px;
  height: 300px;
  background: var(--auth-circle-blue);
  top: -100px;
  right: -50px;
}

.bg-circle-2 {
  width: 200px;
  height: 200px;
  background: var(--auth-circle-green);
  bottom: 100px;
  left: -80px;
}

.bg-circle-3 {
  width: 150px;
  height: 150px;
  background: var(--auth-circle-warm);
  bottom: -50px;
  right: 30%;
}

.login-header {
  text-align: center;
  color: var(--text-inverse);
  margin-bottom: 40px;
  position: relative;
  z-index: 1;
}

.logo-wrapper {
  width: 100px;
  height: 100px;
  background: var(--auth-logo-bg);
  border-radius: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 24px;
  box-shadow: var(--auth-logo-shadow);
}

.login-header h1 {
  font-size: 28px;
  margin-bottom: 8px;
  font-weight: 600;
  letter-spacing: 2px;
}

.login-header p {
  font-size: 14px;
  opacity: 0.8;
  letter-spacing: 1px;
}

.login-form {
  background: var(--panel-bg-solid);
  border-radius: 24px;
  padding: 28px 18px 22px;
  position: relative;
  z-index: 1;
  box-shadow: var(--stats-shadow);
}

.form-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-strong);
  text-align: center;
  margin-bottom: 24px;
}

.custom-group {
  border-radius: 16px;
  overflow: hidden;
}

.custom-field {
  padding: 16px;
}

.custom-field :deep(.van-field__label) {
  font-weight: 500;
  color: var(--text-main);
}

.login-btn {
  margin-top: 32px;
}

.custom-button {
  height: 48px;
  border-radius: 24px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 4px;
  background: var(--button-primary-bg);
  border: none;
  box-shadow: var(--button-primary-shadow);
}

.custom-button:active {
  transform: scale(0.98);
}

@media (max-width: 375px) {
  .logo-wrapper {
    width: 84px;
    height: 84px;
    border-radius: 24px;
  }

  .login-header h1 {
    font-size: 24px;
    letter-spacing: 1px;
  }
}
</style>
