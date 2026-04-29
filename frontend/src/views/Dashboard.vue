<template>
  <div class="dashboard page-shell">
    <section class="dashboard-summary" :class="{ 'dashboard-summary--single': !canLoadBorrowDashboard() }">
      <div class="dashboard-summary__copy">
        <div class="dashboard-summary__label">当前概况</div>
        <div class="dashboard-summary__desc">
          当前共有 {{ statistics.vehicleCount }} 台车辆，可调度 {{ statistics.availableCount }} 台，待处理提醒 {{ reminders.length }} 条，借还车待闭环 {{ statistics.pendingFollowUpCount }} 条。
        </div>

        <div class="dashboard-summary__chips">
          <span class="summary-chip">在库车辆 {{ statistics.vehicleCount }}</span>
          <span class="summary-chip summary-chip--success">可调度 {{ statistics.availableCount }}</span>
          <span class="summary-chip summary-chip--warning">待闭环 {{ statistics.pendingFollowUpCount }}</span>
        </div>
      </div>

      <div class="dashboard-summary__focus" v-if="canLoadBorrowDashboard()">
        <div class="dashboard-summary__focus-label">待处理提醒</div>
        <div class="dashboard-summary__focus-value">{{ reminders.length }}</div>
        <div class="dashboard-summary__focus-desc">优先处理补油、闭环和临近到期事项。</div>
        <div class="dashboard-summary__focus-actions">
          <el-button type="primary" @click="goTo('/borrow-records')">查看借还车</el-button>
          <el-button plain v-if="canShowHalfYearReport()" @click="goTo('/half-year-report')">半年报告</el-button>
          <el-button plain @click="goTo('/vehicles')">查看车辆</el-button>
        </div>
      </div>
    </section>

    <div class="stats-strip">
      <article class="stats-strip__item">
        <span>车辆总数</span>
        <strong>{{ statistics.vehicleCount }}</strong>
        <small>在库档案</small>
      </article>

      <article class="stats-strip__item stats-strip__item--success">
        <span>可用车辆</span>
        <strong>{{ statistics.availableCount }}</strong>
        <small>可直接调度</small>
      </article>

      <article class="stats-strip__item stats-strip__item--warning">
        <span>驾驶人数</span>
        <strong>{{ statistics.driverCount }}</strong>
        <small>当前已登记</small>
      </article>

      <article class="stats-strip__item stats-strip__item--danger">
        <span>待闭环</span>
        <strong>{{ statistics.pendingFollowUpCount }}</strong>
        <small>需办公室跟进</small>
      </article>
    </div>

    <div class="dashboard-grid">
      <el-card class="panel-card table-card">
        <template #header>
          <div class="panel-header">
            <div>
              <div class="panel-header__title">待处理提醒</div>
              <div class="panel-header__desc">待补油、待闭环和临近到期项目统一展示。</div>
            </div>
            <el-tag type="warning" effect="plain">{{ reminders.length }} 条</el-tag>
          </div>
        </template>

        <el-table :data="reminders" :stripe="true" class="dashboard-table">
          <el-table-column prop="title" label="提醒事项" />
          <el-table-column prop="type" label="类型" width="120">
            <template #default="{ row }">
              <el-tag :type="getTypeColor(row.type)" effect="dark" size="small">{{ getTypeName(row.type) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="180" />
          <template #empty>
            <el-empty description="当前没有待处理提醒" :image-size="88" />
          </template>
        </el-table>
      </el-card>

      <el-card class="panel-card table-card">
        <template #header>
          <div class="panel-header">
            <div>
              <div class="panel-header__title">最近借还车记录</div>
              <div class="panel-header__desc">快速查看最近的车辆流转情况和当前状态。</div>
            </div>
            <el-tag type="primary" effect="plain">{{ recentBorrowRecords.length }} 条</el-tag>
          </div>
        </template>

        <el-table :data="recentBorrowRecords" :stripe="true" class="dashboard-table">
          <el-table-column prop="recordNo" label="记录单号" width="210" />
          <el-table-column prop="plateNumber" label="车牌号" width="120" />
          <el-table-column prop="driverName" label="驾驶员" width="120" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)" effect="dark" size="small">{{ getStatusName(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty description="最近暂无借还车记录" :image-size="88" />
          </template>
        </el-table>
      </el-card>
    </div>

  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

const statistics = ref({
  vehicleCount: 0,
  availableCount: 0,
  driverCount: 0,
  pendingFollowUpCount: 0
})

const reminders = ref([])
const recentBorrowRecords = ref([])

const canLoadBorrowDashboard = () => ['SUPER_ADMIN', 'OFFICE_ADMIN'].includes(userStore.userInfo.role)
const canShowHalfYearReport = () => ['SUPER_ADMIN', 'OFFICE_ADMIN', 'FINANCE'].includes(userStore.userInfo.role)

const goTo = (path) => {
  router.push(path)
}

const fetchStatistics = async () => {
  try {
    const [summaryRes, todoSummaryRes] = await Promise.all([
      request({ url: '/statistics', method: 'get' }),
      canLoadBorrowDashboard()
        ? request({ url: '/todo-center/summary', method: 'get' })
        : Promise.resolve({ typeCounts: {} })
    ])

    const typeCounts = todoSummaryRes.typeCounts || {}
    statistics.value = {
      vehicleCount: summaryRes.vehicleCount || 0,
      availableCount: summaryRes.availableCount || 0,
      driverCount: summaryRes.driverCount || 0,
      pendingFollowUpCount: Number(typeCounts.BORROW_FOLLOW_UP || 0)
    }
  } catch (error) {
    console.error('Failed to fetch statistics:', error)
  }
}

const fetchRecentBorrowRecords = async () => {
  if (!canLoadBorrowDashboard()) {
    recentBorrowRecords.value = []
    return
  }

  try {
    const res = await request({
      url: '/vehicle-borrow/records',
      method: 'get',
      params: {
        current: 1,
        size: 5
      }
    })
    recentBorrowRecords.value = res.data || []
  } catch (error) {
    recentBorrowRecords.value = []
    console.error('Failed to fetch borrow records:', error)
  }
}

const formatDate = (value) => {
  if (!value) return '-'
  return String(value).substring(0, 19).replace('T', ' ')
}

const getDaysUntil = (dateValue) => {
  if (!dateValue) return null
  const target = new Date(`${dateValue}T00:00:00`)
  if (Number.isNaN(target.getTime())) return null

  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  return Math.ceil((target.getTime() - today.getTime()) / (1000 * 60 * 60 * 24))
}

const fetchReminders = async () => {
  if (!canLoadBorrowDashboard()) {
    reminders.value = []
    return
  }

  const todoTypeMap = {
    VEHICLE_FUEL_REMINDER: 'FUEL_REMINDER',
    VEHICLE_INSURANCE_EXPIRING: 'INSURANCE_EXPIRE',
    VEHICLE_INSPECTION_EXPIRING: 'INSPECTION_EXPIRE',
    BORROW_FOLLOW_UP: 'FOLLOW_UP',
    BORROW_OVERDUE: 'FOLLOW_UP'
  }

  try {
    const res = await request({
      url: '/todo-center/items',
      method: 'get',
      params: { current: 1, size: 8 }
    })
    reminders.value = (res.data || [])
      .filter(item => todoTypeMap[item.todoType])
      .map(item => ({
        title: item.title,
        type: todoTypeMap[item.todoType],
        createTime: formatDate(item.dueTime)
      }))
  } catch (error) {
    reminders.value = []
    console.error('Failed to fetch reminders:', error)
  }
}

const getTypeColor = (type) => {
  const map = {
    INSURANCE_EXPIRE: 'danger',
    INSPECTION_EXPIRE: 'warning',
    MAINTENANCE_MILEAGE: 'info',
    FUEL_REMINDER: 'warning',
    FOLLOW_UP: 'primary'
  }
  return map[type] || ''
}

const getTypeName = (type) => {
  const map = {
    INSURANCE_EXPIRE: '保险到期',
    INSPECTION_EXPIRE: '年检到期',
    MAINTENANCE_MILEAGE: '保养提醒',
    FUEL_REMINDER: '待补油',
    FOLLOW_UP: '待闭环'
  }
  return map[type] || type
}

const getStatusType = (status) => {
  const map = { TAKEN: 'warning', RETURNED: 'success' }
  return map[status] || ''
}

const getStatusName = (status) => {
  const map = {
    TAKEN: '使用中',
    RETURNED: '已还车'
  }
  return map[status] || status
}

onMounted(() => {
  fetchStatistics()
  fetchRecentBorrowRecords()
  fetchReminders()
})
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 2px 2px 20px;
}

.dashboard-summary {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(280px, 0.65fr);
  gap: 16px;
  padding: 16px 18px;
  border-radius: 18px;
  background: #ffffff;
  border: 1px solid rgba(218, 225, 235, 0.92);
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.04);
}

.dashboard-summary--single {
  grid-template-columns: 1fr;
}

.dashboard-summary__copy {
  min-width: 0;
}

.dashboard-summary__label {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: #6b7a90;
}

.dashboard-summary__desc {
  margin-top: 8px;
  color: #617089;
  font-size: 14px;
  line-height: 1.7;
}

.dashboard-summary__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 14px;
}

.summary-chip {
  display: inline-flex;
  align-items: center;
  padding: 8px 12px;
  border-radius: 999px;
  background: #f5f7fb;
  border: 1px solid rgba(214, 221, 233, 0.92);
  color: #50617c;
  font-size: 12px;
  font-weight: 700;
}

.summary-chip--success {
  background: rgba(22, 121, 95, 0.08);
  border-color: rgba(22, 121, 95, 0.14);
  color: #146b56;
}

.summary-chip--warning {
  background: rgba(201, 123, 25, 0.08);
  border-color: rgba(201, 123, 25, 0.14);
  color: #925b16;
}

.dashboard-summary__focus {
  min-width: 0;
  padding: 16px 18px;
  border-radius: 16px;
  background: #f8fafc;
  border: 1px solid rgba(220, 227, 238, 0.92);
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.dashboard-summary__focus-label {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: #6c7a90;
}

.dashboard-summary__focus-value {
  margin-top: 10px;
  font-size: 34px;
  line-height: 1;
  font-weight: 800;
  color: #182335;
}

.dashboard-summary__focus-desc {
  margin-top: 8px;
  font-size: 13px;
  line-height: 1.6;
  color: #66758d;
}

.dashboard-summary__focus-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 16px;
}

.dashboard-summary__focus-actions :deep(.el-button.is-plain) {
  background: #ffffff;
  border-color: rgba(211, 220, 233, 0.92);
  color: #50617c;
}

.stats-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.stats-strip__item {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 14px 16px;
  border-radius: 16px;
  background: #ffffff;
  border: 1px solid rgba(217, 224, 235, 0.92);
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.04);
  border-left: 4px solid #2b69d9;
}

.stats-strip__item span {
  color: #687890;
  font-size: 12px;
  font-weight: 700;
}

.stats-strip__item strong {
  color: #182335;
  font-size: 28px;
  line-height: 1.1;
  font-weight: 800;
}

.stats-strip__item small {
  color: #74839a;
  font-size: 12px;
}

.stats-strip__item--success {
  border-left-color: #16795f;
}

.stats-strip__item--warning {
  border-left-color: #c97b19;
}

.stats-strip__item--danger {
  border-left-color: #c94c4c;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;
}

.table-card :deep(.el-card__header) {
  padding: 18px 22px;
}

.dashboard-table :deep(.el-table__cell) {
  vertical-align: middle;
}

@media (max-width: 1200px) {
  .dashboard-summary,
  .stats-strip,
  .dashboard-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .dashboard-summary,
  .stats-strip,
  .dashboard-grid {
    grid-template-columns: 1fr;
  }

  .dashboard-summary__focus-value {
    font-size: 40px;
  }
}
</style>
