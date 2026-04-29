<template>
  <div class="home-page">
    <div class="header">
      <div class="user-info">
        <div class="avatar-wrapper">
          <van-icon name="user-circle-o" size="48" color="var(--text-inverse)" />
        </div>
        <div class="user-detail">
          <div class="name">{{ driverStore.driverInfo?.name }}</div>
          <div class="greeting">驾驶员工作台</div>
        </div>
      </div>
    </div>

    <div class="stats-wrapper">
      <div class="stats">
        <div class="stat-item">
          <div class="stat-value">{{ stats.available }}</div>
          <div class="stat-label">可借车辆</div>
        </div>
        <div class="stat-divider"></div>
        <div class="stat-item">
          <div class="stat-value">{{ stats.monthlyTrips }}</div>
          <div class="stat-label">本月用车次数</div>
        </div>
        <div class="stat-divider"></div>
        <div class="stat-item">
          <div class="stat-value">{{ stats.monthlyMileage }}</div>
          <div class="stat-label">本月用车公里数</div>
        </div>
      </div>
    </div>

    <button type="button" class="activity-entry-card" @click="$router.push('/vehicle-activity')">
      <div class="activity-entry-card__head">
        <div>
          <div class="activity-entry-card__title">车辆动态总览</div>
          <div class="activity-entry-card__subtitle">低频也能一眼看清每辆车现在在忙什么</div>
        </div>
        <van-icon name="arrow" size="20" color="var(--text-inverse)" />
      </div>
    </button>

    <div v-if="currentBorrowRecord" class="borrow-banner">
      <div class="borrow-banner__eyebrow">当前状态</div>
      <div class="borrow-banner__title">借车中</div>
      <div class="borrow-banner__detail">
        {{ currentBorrowRecord.plateNumber }} · 取车时间 {{ formatDate(currentBorrowRecord.takeTime) }}
      </div>
      <div class="borrow-banner__detail">
        取车里程 {{ currentBorrowRecord.takeMileage }} km
      </div>
      <div v-if="currentBorrowRecord.destination" class="borrow-banner__detail">
        去向 {{ currentBorrowRecord.destination }}
      </div>
      <div v-if="currentBorrowRecord.expectedReturnTime" class="borrow-banner__detail">
        预计还车 {{ formatDate(currentBorrowRecord.expectedReturnTime) }}
      </div>
      <van-button type="danger" block @click="$router.push('/borrow')">
        去还车
      </van-button>
    </div>

    <div class="content">
      <section class="mobile-card status-card">
        <div class="card-head">
          <div>
            <div class="card-title">当前状态</div>
            <div class="card-subtitle">优先展示你现在最需要处理的操作</div>
          </div>
        </div>

        <div v-if="currentBorrowRecord" class="status-panel">
          <div class="status-panel__plate">{{ currentBorrowRecord.plateNumber }}</div>
          <div v-if="currentBorrowRecord.usageReason" class="status-panel__line">
            事由：{{ currentBorrowRecord.usageReason }}
          </div>
          <div class="status-panel__line">
            取车里程：{{ currentBorrowRecord.takeMileage }} km
          </div>
          <div v-if="currentBorrowRecord.expectedReturnTime" class="status-panel__line">
            预计还车：{{ formatDate(currentBorrowRecord.expectedReturnTime) }}
          </div>
          <div class="status-actions status-actions--stack">
            <van-button type="warning" block @click="$router.push('/borrow')">立即还车</van-button>
            <van-button plain block @click="$router.push('/fuel')">去加油登记</van-button>
          </div>
        </div>

        <div v-else class="status-panel status-panel--idle">
          <div class="status-panel__plate">当前没有借车</div>
          <div class="status-panel__line">系统内未被借走的车辆可直接取车。</div>
          <div class="status-actions">
            <van-button type="primary" block @click="$router.push('/borrow')">去借车</van-button>
          </div>
        </div>
      </section>

      <section class="mobile-card quick-card">
        <div class="card-head">
          <div>
            <div class="card-title">快捷操作</div>
            <div class="card-subtitle">常用功能做成大按钮，手机上更容易点按</div>
          </div>
        </div>

        <div class="quick-actions">
          <button type="button" class="action-tile action-tile--blue" @click="$router.push('/borrow')">
            <div class="action-tile__icon">
              <van-icon name="logistics" size="26" color="var(--text-inverse)" />
            </div>
            <div class="action-tile__title">{{ currentBorrowRecord ? '还车登记' : '借车登记' }}</div>
            <div class="action-tile__desc">{{ currentBorrowRecord ? '当前车辆在借用中，点此直接归还' : '选择空闲车辆后直接取车' }}</div>
          </button>
          <button type="button" class="action-tile action-tile--green" @click="$router.push('/fuel')">
            <div class="action-tile__icon">
              <van-icon name="gold-coin" size="26" color="var(--text-inverse)" />
            </div>
            <div class="action-tile__title">加油登记</div>
            <div class="action-tile__desc">登记小票、油表和加油金额</div>
          </button>
        </div>
      </section>

      <section class="mobile-card records-card">
        <div class="card-head">
          <div>
            <div class="card-title">我的记录</div>
            <div class="card-subtitle">直接进入用车记录或加油记录，不再经过中转页</div>
          </div>
        </div>

        <div class="records-grid">
          <button type="button" class="record-link record-link--blue" @click="$router.push('/my/borrow-records')">
            <div class="record-link__icon">
              <van-icon name="notes-o" size="22" color="var(--text-inverse)" />
            </div>
            <div class="record-link__content">
              <div class="record-link__title">用车记录</div>
              <div class="record-link__desc">查看借车、还车信息和详情照片</div>
            </div>
            <van-icon name="arrow" size="18" color="var(--text-inverse)" />
          </button>

          <button type="button" class="record-link record-link--green" @click="$router.push('/my/fuel-records')">
            <div class="record-link__icon">
              <van-icon name="coupon-o" size="22" color="var(--text-inverse)" />
            </div>
            <div class="record-link__content">
              <div class="record-link__title">加油记录</div>
              <div class="record-link__desc">查看金额、公里数和上传照片</div>
            </div>
            <van-icon name="arrow" size="18" color="var(--text-inverse)" />
          </button>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showDialog, showToast } from 'vant'
import { useDriverStore } from '@/store/driver'
import { getAvailableVehicles, getCurrentBorrowRecord, getMyMileageStats } from '@/api'

const route = useRoute()
const router = useRouter()
const driverStore = useDriverStore()

const stats = ref({
  available: 0,
  monthlyTrips: 0,
  monthlyMileage: 0
})

const currentBorrowRecord = ref(null)

const formatDate = (date) => {
  if (!date) return ''
  return String(date).substring(0, 16).replace('T', ' ')
}

onMounted(async () => {
  const daysThisMonth = new Date().getDate()
  const results = await Promise.allSettled([
    getAvailableVehicles(),
    getCurrentBorrowRecord(),
    getMyMileageStats(daysThisMonth)
  ])

  const [availableRes, currentRes, recordsRes] = results
  let hasFailedRequest = false

  if (availableRes.status === 'fulfilled') {
    stats.value.available = (availableRes.value || []).length
  } else {
    hasFailedRequest = true
  }

  if (currentRes.status === 'fulfilled') {
    currentBorrowRecord.value = currentRes.value || null
    driverStore.setActiveBorrow(!!currentBorrowRecord.value)
  } else {
    currentBorrowRecord.value = null
    driverStore.setActiveBorrow(false)
    hasFailedRequest = true
  }

  if (recordsRes.status === 'fulfilled') {
    stats.value.monthlyTrips = recordsRes.value.tripCount || 0
    stats.value.monthlyMileage = Number(recordsRes.value.totalMileage || 0)
  } else {
    stats.value.monthlyTrips = 0
    stats.value.monthlyMileage = 0
    hasFailedRequest = true
  }

  if (currentBorrowRecord.value && (driverStore.borrowNoticePending || route.query.borrowed === '1')) {
    await showDialog({
      title: '借车成功',
      message: `当前状态：借车中\n车辆：${currentBorrowRecord.value.plateNumber}\n可直接点击首页“去还车”按钮进行归还。`
    })
    driverStore.clearBorrowNotice()
    if (route.query.borrowed === '1') {
      router.replace('/home')
    }
  }

  if (hasFailedRequest) {
    showToast('登录状态已失效，请重新登录')
  }
})
</script>

<style scoped>
.home-page {
  min-height: 100dvh;
  background: transparent;
  padding-bottom: calc(88px + var(--safe-area-bottom));
}

.header {
  position: relative;
  overflow: hidden;
  background: var(--home-header-bg);
  padding: calc(34px + var(--safe-area-top)) 18px 62px;
  color: var(--text-inverse);
}

.header::after {
  content: "";
  position: absolute;
  right: -20px;
  top: 10px;
  width: 160px;
  height: 160px;
  border-radius: 50%;
  background: var(--home-header-glow);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 16px;
  position: relative;
  z-index: 1;
}

.avatar-wrapper {
  width: 68px;
  height: 68px;
  background: var(--glass-strong);
  border-radius: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: inset 0 0 0 1px var(--glass-line);
}

.user-detail .name {
  font-size: 24px;
  font-weight: 800;
  margin-bottom: 4px;
}

.user-detail .greeting {
  font-size: 13px;
  opacity: 0.82;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.stats-wrapper {
  margin: -38px 14px 18px;
}

.stats {
  background: var(--stats-card-bg);
  backdrop-filter: blur(16px);
  border-radius: 24px;
  display: flex;
  align-items: center;
  justify-content: space-around;
  padding: 20px 14px;
  border: 1px solid var(--stats-card-border);
  box-shadow: var(--stats-shadow);
}

.stat-item {
  text-align: center;
  flex: 1;
}

.stat-value {
  font-size: clamp(24px, 7vw, 28px);
  font-weight: 800;
  color: var(--text-strong);
  line-height: 1.2;
}

.stat-label {
  font-size: 12px;
  color: var(--text-soft);
  margin-top: 4px;
}

.stat-divider {
  width: 1px;
  height: 40px;
  background: var(--stats-divider);
}

.content {
  padding: 0 14px;
}

.activity-entry-card {
  width: calc(100% - 28px);
  margin: 0 14px 18px;
  border: 0;
  border-radius: 24px;
  text-align: left;
  color: var(--text-inverse);
  background: linear-gradient(135deg, rgba(30, 87, 199, 0.98), rgba(23, 51, 116, 0.96));
  box-shadow: var(--action-shadow);
  padding: 16px 16px 14px;
}

.activity-entry-card__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.activity-entry-card__title {
  font-size: 18px;
  font-weight: 800;
  line-height: 1.3;
}

.activity-entry-card__subtitle {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.6;
  opacity: 0.9;
}

.borrow-banner {
  margin: 0 14px 18px;
  padding: 20px 18px 18px;
  border-radius: 24px;
  background: var(--banner-warm-bg);
  color: var(--text-inverse);
  box-shadow: var(--banner-warm-shadow);
}

.borrow-banner__eyebrow {
  font-size: 12px;
  opacity: 0.85;
  margin-bottom: 6px;
}

.borrow-banner__title {
  font-size: 30px;
  font-weight: 800;
  line-height: 1.2;
  margin-bottom: 10px;
}

.borrow-banner__detail {
  font-size: 13px;
  line-height: 1.7;
  margin-bottom: 4px;
}

.mobile-card {
  background: var(--panel-bg);
  border-radius: 24px;
  padding: 18px 16px;
  border: 1px solid var(--panel-border);
  box-shadow: var(--shadow-soft);
}

.mobile-card + .mobile-card {
  margin-top: 15px;
}

.card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.card-title {
  font-size: 16px;
  font-weight: 800;
  color: var(--text-strong);
}

.card-subtitle {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.5;
  color: var(--text-soft);
}

.status-panel {
  border-radius: 18px;
  background: var(--warm-notice-bg);
  padding: 16px;
  border: 1px solid var(--warm-notice-border);
}

.status-panel--idle {
  background: var(--blue-notice-bg);
  border-color: var(--blue-notice-border);
}

.status-panel__plate {
  font-size: 18px;
  font-weight: 800;
  color: var(--text-strong);
  margin-bottom: 8px;
}

.status-panel__line {
  font-size: 13px;
  color: var(--text-soft-2);
  line-height: 1.7;
}

.status-actions {
  margin-top: 14px;
}

.status-actions--stack {
  display: grid;
  gap: 10px;
}

.quick-actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.action-tile {
  border: 0;
  text-align: left;
  width: 100%;
  padding: 18px 16px;
  border-radius: 22px;
  color: var(--text-inverse);
  box-shadow: var(--action-shadow);
}

.action-tile--blue {
  background: var(--action-blue-bg);
}

.action-tile--green {
  background: var(--action-green-bg);
}

.action-tile__icon {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--action-icon-bg);
  margin-bottom: 14px;
}

.action-tile__title {
  font-size: 16px;
  font-weight: 700;
  line-height: 1.3;
}

.action-tile__desc {
  margin-top: 8px;
  font-size: 12px;
  line-height: 1.6;
  opacity: 0.9;
}

.records-grid {
  display: grid;
  gap: 12px;
}

.record-link {
  width: 100%;
  border: 0;
  border-radius: 22px;
  padding: 18px 16px;
  display: flex;
  align-items: center;
  gap: 14px;
  text-align: left;
  color: var(--text-inverse);
  box-shadow: var(--action-shadow);
}

.record-link--blue {
  background: linear-gradient(135deg, rgba(30, 87, 199, 0.96), rgba(23, 51, 116, 0.94));
}

.record-link--green {
  background: linear-gradient(135deg, rgba(36, 155, 117, 0.96), rgba(18, 84, 76, 0.94));
}

.record-link__icon {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.16);
  flex: none;
}

.record-link__content {
  min-width: 0;
  flex: 1;
}

.record-link__title {
  font-size: 16px;
  font-weight: 700;
  line-height: 1.3;
}

.record-link__desc {
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.6;
  opacity: 0.9;
}

@media (max-width: 375px) {
  .user-info {
    gap: 12px;
  }

  .avatar-wrapper {
    width: 56px;
    height: 56px;
    border-radius: 18px;
  }

  .user-detail .name {
    font-size: 20px;
  }

  .borrow-banner__title {
    font-size: 24px;
  }

  .quick-actions {
    grid-template-columns: 1fr;
  }

  .record-link {
    align-items: flex-start;
  }
}
</style>
