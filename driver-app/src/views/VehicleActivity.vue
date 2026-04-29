<template>
  <div class="vehicle-activity-page">
    <van-nav-bar title="车辆动态总览" left-arrow @click-left="router.back()" />

    <div v-if="loading && vehicles.length === 0" class="activity-loading">
      <div class="activity-hero">
        <div class="activity-hero__copy">
          <div class="activity-hero__label">车辆一页看全</div>
          <div class="activity-hero__title">正在整理车辆动态</div>
          <div class="activity-hero__desc">把可借、出车、跟进和送修信息统一拉到这里。</div>
        </div>
      </div>

      <section v-for="index in 3" :key="index" class="activity-card activity-card--skeleton">
        <van-skeleton title :row="4" />
      </section>
    </div>

    <van-pull-refresh v-else v-model="refreshing" @refresh="handleRefresh">
      <div class="activity-scroll">
        <section class="activity-hero">
          <div class="activity-hero__copy">
            <div class="activity-hero__label">车辆一页看全</div>
            <div class="activity-hero__title">今天每辆车现在都在忙什么</div>
            <div class="activity-hero__desc">
              需要借车就直接去借，其他车辆先看动态，不需要再来回翻页面。
            </div>
            <div class="activity-hero__chips">
              <span class="hero-chip">共 {{ heroSummary.total }} 辆</span>
              <span class="hero-chip hero-chip--danger">超期 {{ heroSummary.overdue }}</span>
            </div>
          </div>

          <div class="activity-focus">
            <div class="activity-focus__label">{{ spotlight.title }}</div>
            <div class="activity-focus__value">{{ spotlight.value }}</div>
            <div class="activity-focus__desc">{{ spotlight.desc }}</div>
          </div>
        </section>

        <section class="activity-toolbar">
          <van-search
            v-model="keyword"
            shape="round"
            placeholder="搜索车牌、司机、去向或维修厂"
            class="activity-search"
          />
        </section>

        <div v-if="filteredVehicles.length" class="activity-list">
          <article
            v-for="vehicle in filteredVehicles"
            :key="vehicle.vehicleId || vehicle.id || vehicle.plateNumber"
            class="activity-card"
            :class="resolveCardToneClass(vehicle)"
          >
            <div class="activity-card__head">
              <div class="activity-card__identity">
                <div class="activity-card__plate">{{ vehicle.plateNumber }}</div>
                <div class="activity-card__spec">{{ buildVehicleSpec(vehicle) }}</div>
              </div>
              <van-tag plain :type="resolveStatusTagType(vehicle)">
                {{ resolveStatusText(vehicle) }}
              </van-tag>
            </div>

            <div class="activity-card__title">{{ resolveActivityTitle(vehicle) }}</div>
            <div class="activity-card__subtitle">{{ resolveActivitySubtitle(vehicle) }}</div>

            <div class="activity-meta">
              <div v-for="row in buildMetaRows(vehicle)" :key="`${vehicle.plateNumber}-${row.label}`" class="activity-meta__item">
                <span class="activity-meta__label">{{ row.label }}</span>
                <strong class="activity-meta__value">{{ row.value }}</strong>
              </div>
            </div>

            <div v-if="buildBadges(vehicle).length" class="activity-badges">
              <span
                v-for="badge in buildBadges(vehicle)"
                :key="`${vehicle.plateNumber}-${badge.label}`"
                class="activity-pill"
                :class="badge.className"
              >
                {{ badge.label }}
              </span>
            </div>

            <div class="activity-actions">
              <van-button
                v-if="isAvailableForBorrow(vehicle)"
                type="primary"
                size="small"
                round
                @click="goBorrow()"
              >
                去借车
              </van-button>
              <van-button
                v-if="!isAvailableForBorrow(vehicle)"
                plain
                size="small"
                round
                @click="openDetail(vehicle)"
              >
                查看详情
              </van-button>
              <van-button
                v-if="isAvailableForBorrow(vehicle)"
                plain
                size="small"
                round
                @click="openDetail(vehicle)"
              >
                看详情
              </van-button>
            </div>
          </article>
        </div>

        <div v-else class="activity-empty">
          <van-empty description="当前筛选下没有匹配的车辆动态" />
        </div>
      </div>
    </van-pull-refresh>

    <van-popup v-model:show="detailVisible" position="bottom" round class="activity-detail-popup">
      <div v-if="selectedVehicle" class="activity-detail">
        <div class="activity-detail__handle"></div>
        <div class="activity-detail__header">
          <div>
            <div class="activity-detail__plate">{{ selectedVehicle.plateNumber }}</div>
            <div class="activity-detail__spec">{{ buildVehicleSpec(selectedVehicle) }}</div>
          </div>
          <van-tag plain :type="resolveStatusTagType(selectedVehicle)">
            {{ resolveStatusText(selectedVehicle) }}
          </van-tag>
        </div>

        <section class="activity-detail__section">
          <div class="activity-detail__section-title">当前动态</div>
          <div class="activity-detail__summary">{{ resolveActivityTitle(selectedVehicle) }}</div>
          <div class="activity-detail__desc">{{ resolveActivitySubtitle(selectedVehicle) }}</div>
        </section>

        <section class="activity-detail__section">
          <div class="activity-detail__section-title">关键信息</div>
          <div class="activity-detail__grid">
            <div
              v-for="row in buildDetailRows(selectedVehicle)"
              :key="`${selectedVehicle.plateNumber}-${row.label}`"
              class="activity-detail__cell"
            >
              <span>{{ row.label }}</span>
              <strong>{{ row.value }}</strong>
            </div>
          </div>
        </section>

        <section v-if="buildNotes(selectedVehicle).length" class="activity-detail__section">
          <div class="activity-detail__section-title">补充说明</div>
          <div class="activity-detail__notes">
            <div
              v-for="note in buildNotes(selectedVehicle)"
              :key="`${selectedVehicle.plateNumber}-${note}`"
              class="activity-detail__note"
            >
              {{ note }}
            </div>
          </div>
        </section>

        <div class="activity-detail__actions">
          <van-button
            v-if="isAvailableForBorrow(selectedVehicle)"
            type="primary"
            block
            round
            @click="goBorrow()"
          >
            去借车
          </van-button>
          <van-button v-else plain block round @click="detailVisible = false">
            关闭
          </van-button>
        </div>
      </div>
    </van-popup>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getVehicleActivities } from '@/api'

const router = useRouter()

const loading = ref(false)
const refreshing = ref(false)
const keyword = ref('')
const detailVisible = ref(false)
const selectedVehicle = ref(null)
const vehicles = ref([])
const summary = ref({
  total: 0,
  available: 0,
  inUse: 0,
  pending: 0,
  maintenance: 0,
  overdue: 0
})

const normalizeVehicleList = (response) => {
  const candidates = [
    response?.vehicles,
    response?.data,
    response?.list,
    response?.records,
    response?.items
  ]
  return candidates.find(item => Array.isArray(item)) || []
}

const isRestricted = (vehicle) => Number(vehicle?.trafficRestrictedToday || 0) === 1
  && Number(vehicle?.trafficRestrictionReleasedToday || 0) !== 1

const isAvailableForBorrow = (vehicle) => {
  if (typeof vehicle?.availableForBorrow === 'boolean') {
    return vehicle.availableForBorrow
  }
  return vehicle?.status === 'NORMAL' && !isRestricted(vehicle)
}

const isInUse = (vehicle) => ['IN_USE', 'OVERDUE_RETURN', 'OVERDUE'].includes(vehicle?.activityType) || vehicle?.status === 'IN_USE'
const isMaintenance = (vehicle) => String(vehicle?.activityType || '').startsWith('MAINTENANCE') || vehicle?.status === 'MAINTENANCE'
const isPending = (vehicle) => vehicle?.activityType === 'FOLLOW_UP_PENDING' || vehicle?.status === 'PENDING_CHECK'
const isOverdue = (vehicle) => ['OVERDUE_RETURN', 'OVERDUE'].includes(vehicle?.activityType)

const deriveSummary = (list) => ({
  total: list.length,
  available: list.filter(item => isAvailableForBorrow(item)).length,
  inUse: list.filter(item => isInUse(item)).length,
  pending: list.filter(item => isPending(item)).length,
  maintenance: list.filter(item => isMaintenance(item)).length,
  overdue: list.filter(item => isOverdue(item)).length
})

const heroSummary = computed(() => ({
  ...deriveSummary(vehicles.value),
  ...summary.value
}))

const spotlight = computed(() => {
  if (heroSummary.value.overdue > 0) {
    return {
      title: '需要优先关注',
      value: heroSummary.value.overdue,
      desc: '当前有车辆超过预计还车时间，建议先联系确认。'
    }
  }
  if (heroSummary.value.pending > 0) {
    return {
      title: '需跟进车辆',
      value: heroSummary.value.pending,
      desc: '已还车但仍有待闭环事项，办公室处理后才能恢复正常。'
    }
  }
  return {
    title: '可直接调度',
    value: heroSummary.value.available,
    desc: '当前空闲车辆可直接进入借车页取车。'
  }
})

const keywordFilteredVehicles = computed(() => {
  const normalized = keyword.value.trim().toLowerCase()
  if (!normalized) {
    return vehicles.value
  }
  return vehicles.value.filter((vehicle) => {
    const haystack = [
      vehicle.plateNumber,
      vehicle.vehicleType,
      vehicle.brand,
      vehicle.model,
      vehicle.statusLabel,
      vehicle.activityTitle,
      vehicle.activitySubtitle,
      vehicle.currentDriverName,
      vehicle.currentDestination,
      vehicle.usageReason,
      vehicle.parkingLocation,
      vehicle.repairVendor,
      vehicle.maintenanceIssueDescription,
      vehicle.actionRequired
    ]
      .filter(Boolean)
      .join(' ')
      .toLowerCase()
    return haystack.includes(normalized)
  })
})

const filteredVehicles = computed(() => keywordFilteredVehicles.value)

const buildVehicleSpec = (vehicle) => {
  const parts = [vehicle?.vehicleType, vehicle?.brand, vehicle?.model].filter(Boolean)
  return parts.length ? parts.join(' · ') : '公务车辆'
}

const formatDate = (value) => {
  if (!value) return '-'
  return String(value).replace('T', ' ').substring(0, 16)
}

const resolveStatusText = (vehicle) => vehicle?.statusLabel || {
  NORMAL: '可借',
  IN_USE: '出车',
  PENDING_CHECK: '跟进',
  MAINTENANCE: '送修'
}[vehicle?.status] || '状态未知'

const resolveStatusTagType = (vehicle) => {
  if (isOverdue(vehicle)) return 'danger'
  if (isPending(vehicle)) return 'warning'
  if (isMaintenance(vehicle)) return 'danger'
  if (isInUse(vehicle)) return 'primary'
  return 'success'
}

const resolveActivityTitle = (vehicle) => {
  if (vehicle?.activityTitle) {
    return vehicle.activityTitle
  }
  if (isAvailableForBorrow(vehicle)) {
    return '当前空闲，可直接借用'
  }
  if (isOverdue(vehicle)) {
    return `${vehicle?.currentDriverName || '当前驾驶员'} 正在使用，已超过预计还车时间`
  }
  if (isInUse(vehicle)) {
    return `${vehicle?.currentDriverName || '当前驾驶员'} 正在用车`
  }
  if (isMaintenance(vehicle)) {
    return '车辆已送修'
  }
  if (isPending(vehicle)) {
    return '本车已归还，等待闭环跟进'
  }
  return '当前状态待确认'
}

const resolveActivitySubtitle = (vehicle) => {
  if (vehicle?.activitySubtitle) {
    return vehicle.activitySubtitle
  }
  if (isAvailableForBorrow(vehicle)) {
    return vehicle?.parkingLocation ? `停放位置：${vehicle.parkingLocation}` : '当前没有异常提醒'
  }
  if (isInUse(vehicle)) {
    return [
      vehicle?.currentDestination ? `去向 ${vehicle.currentDestination}` : '',
      vehicle?.expectedReturnTime ? `预计还车 ${formatDate(vehicle.expectedReturnTime)}` : ''
    ].filter(Boolean).join('，') || '借车进行中'
  }
  if (isMaintenance(vehicle)) {
    return [
      vehicle?.maintenanceIssueDescription ? `送修原因：${vehicle.maintenanceIssueDescription}` : '',
      vehicle?.repairVendor ? `维修厂：${vehicle.repairVendor}` : '',
      vehicle?.expectedFinishTime ? `预计完成：${formatDate(vehicle.expectedFinishTime)}` : ''
    ].filter(Boolean).join('；') || '维修处理中'
  }
  if (isPending(vehicle)) {
    return vehicle?.actionRequired || '等待办公室处理闭环'
  }
  return '暂无更多说明'
}

const buildMetaRows = (vehicle) => {
  const rows = []
  if (vehicle?.currentDriverName && isInUse(vehicle)) {
    rows.push({ label: '驾驶员', value: vehicle.currentDriverName })
  }
  if (vehicle?.currentDestination && isInUse(vehicle)) {
    rows.push({ label: '去向', value: vehicle.currentDestination })
  }
  if (vehicle?.expectedReturnTime && isInUse(vehicle)) {
    rows.push({ label: '预计还车', value: formatDate(vehicle.expectedReturnTime) })
  }
  if (vehicle?.parkingLocation && isAvailableForBorrow(vehicle)) {
    rows.push({ label: '停放位置', value: vehicle.parkingLocation })
  }
  if (vehicle?.repairVendor && isMaintenance(vehicle)) {
    rows.push({ label: '维修厂', value: vehicle.repairVendor })
  }
  if (vehicle?.expectedFinishTime && isMaintenance(vehicle)) {
    rows.push({ label: '预计完成', value: formatDate(vehicle.expectedFinishTime) })
  }
  if (!rows.length && vehicle?.updateTime) {
    rows.push({ label: '最近更新', value: formatDate(vehicle.updateTime) })
  }
  return rows.slice(0, 3)
}

const buildBadges = (vehicle) => {
  const badges = []
  if (isOverdue(vehicle)) {
    badges.push({ label: '超期未还', className: 'activity-pill--danger' })
  }
  if (vehicle?.followUpStatus === 'PENDING' || isPending(vehicle)) {
    badges.push({ label: '待闭环', className: 'activity-pill--warning' })
  }
  if (vehicle?.fuelReminderStatus === 'PENDING') {
    badges.push({ label: '待补油', className: 'activity-pill--warm' })
  }
  if (isRestricted(vehicle)) {
    badges.push({ label: '今日限行', className: 'activity-pill--danger' })
  }
  if (Number(vehicle?.trafficRestrictionReleasedToday || 0) === 1) {
    badges.push({ label: '限行已放行', className: 'activity-pill--success' })
  }
  if (vehicle?.maintenanceStatusLabel && isMaintenance(vehicle)) {
    badges.push({ label: vehicle.maintenanceStatusLabel, className: 'activity-pill--blue' })
  }
  return badges
}

const buildDetailRows = (vehicle) => {
  const rows = [
    { label: '车辆状态', value: resolveStatusText(vehicle) },
    vehicle?.parkingLocation ? { label: '停放位置', value: vehicle.parkingLocation } : null,
    vehicle?.borrowTime ? { label: '取车时间', value: formatDate(vehicle.borrowTime) } : null,
    vehicle?.expectedReturnTime ? { label: '预计还车', value: formatDate(vehicle.expectedReturnTime) } : null,
    vehicle?.currentDriverName ? { label: '当前驾驶员', value: vehicle.currentDriverName } : null,
    vehicle?.currentDestination ? { label: '当前去向', value: vehicle.currentDestination } : null,
    vehicle?.usageReason ? { label: '用车事由', value: vehicle.usageReason } : null,
    vehicle?.repairVendor ? { label: '维修厂', value: vehicle.repairVendor } : null,
    vehicle?.maintenanceStatusLabel ? { label: '维修状态', value: vehicle.maintenanceStatusLabel } : null,
    vehicle?.expectedFinishTime ? { label: '预计完成', value: formatDate(vehicle.expectedFinishTime) } : null
  ]
  return rows.filter(Boolean)
}

const buildNotes = (vehicle) => [
  vehicle?.actionRequired,
  vehicle?.maintenanceIssueDescription,
  vehicle?.fuelReminderNote,
  vehicle?.trafficRestrictionMessage
].filter(Boolean)

const resolveCardToneClass = (vehicle) => {
  if (isOverdue(vehicle)) return 'activity-card--danger'
  if (isMaintenance(vehicle)) return 'activity-card--ink'
  if (isPending(vehicle)) return 'activity-card--warm'
  if (isInUse(vehicle)) return 'activity-card--blue'
  return 'activity-card--idle'
}

const fetchData = async ({ silent = false } = {}) => {
  if (!silent) {
    loading.value = true
  }
  try {
    const response = await getVehicleActivities()
    const list = normalizeVehicleList(response)
    vehicles.value = list
    summary.value = {
      ...deriveSummary(list),
      ...(response?.summary || {})
    }
  } catch (error) {
    showToast(error.message || '车辆动态加载失败')
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

const handleRefresh = async () => {
  await fetchData({ silent: true })
}

const openDetail = (vehicle) => {
  selectedVehicle.value = vehicle
  detailVisible.value = true
}

const goBorrow = () => {
  detailVisible.value = false
  router.push('/borrow')
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.vehicle-activity-page {
  min-height: 100dvh;
  background: transparent;
}

.activity-scroll,
.activity-loading {
  padding: 14px 14px calc(26px + var(--safe-area-bottom));
}

.activity-hero {
  position: relative;
  overflow: hidden;
  padding: 22px 18px;
  border-radius: 28px;
  color: var(--text-inverse);
  background:
    radial-gradient(circle at top right, rgba(255, 255, 255, 0.18), transparent 28%),
    radial-gradient(circle at bottom left, rgba(24, 168, 137, 0.24), transparent 30%),
    linear-gradient(135deg, #091a34 0%, #15407b 52%, #169c85 100%);
  box-shadow: 0 20px 38px rgba(12, 26, 52, 0.18);
}

.activity-hero__label {
  font-size: 12px;
  letter-spacing: 0.1em;
  opacity: 0.82;
  text-transform: uppercase;
}

.activity-hero__title {
  margin-top: 8px;
  font-size: 28px;
  font-weight: 800;
  line-height: 1.2;
}

.activity-hero__desc {
  margin-top: 10px;
  max-width: 340px;
  font-size: 13px;
  line-height: 1.8;
  opacity: 0.92;
}

.activity-hero__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 16px;
}

.hero-chip {
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  font-size: 12px;
  line-height: 1;
  backdrop-filter: blur(10px);
}

.hero-chip--success {
  background: rgba(25, 189, 150, 0.18);
}

.hero-chip--warning {
  background: rgba(241, 155, 47, 0.18);
}

.hero-chip--danger {
  background: rgba(232, 91, 77, 0.18);
}

.activity-focus {
  margin-top: 20px;
  padding: 16px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(14px);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.activity-focus__label {
  font-size: 12px;
  opacity: 0.8;
}

.activity-focus__value {
  margin-top: 8px;
  font-size: 34px;
  font-weight: 800;
  line-height: 1.1;
}

.activity-focus__desc {
  margin-top: 8px;
  font-size: 12px;
  line-height: 1.7;
  opacity: 0.92;
}

.activity-toolbar {
  margin-top: 16px;
  padding: 16px;
  border-radius: 24px;
  background: var(--panel-bg);
  border: 1px solid var(--panel-border);
  box-shadow: var(--shadow-soft);
}

.activity-search {
  padding: 0;
  background: transparent;
}

.activity-toolbar :deep(.van-search__content) {
  background: var(--surface-muted-2);
  border-radius: 16px;
}

.activity-list {
  margin-top: 16px;
}

.activity-card {
  padding: 18px 16px;
  border-radius: 24px;
  background: var(--panel-bg);
  border: 1px solid var(--panel-border);
  box-shadow: var(--shadow-soft);
}

.activity-card + .activity-card {
  margin-top: 12px;
}

.activity-card--idle {
  background: var(--activity-card-idle-bg);
}

.activity-card--blue {
  background: var(--activity-card-blue-bg);
}

.activity-card--warm {
  background: var(--activity-card-warm-bg);
}

.activity-card--ink {
  background: var(--activity-card-ink-bg);
}

.activity-card--danger {
  background: var(--activity-card-danger-bg);
}

.activity-card__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.activity-card__identity {
  min-width: 0;
}

.activity-card__plate {
  font-size: 22px;
  font-weight: 800;
  color: var(--text-strong);
  line-height: 1.2;
}

.activity-card__spec {
  margin-top: 6px;
  font-size: 12px;
  color: var(--text-soft);
  line-height: 1.5;
}

.activity-card__title {
  margin-top: 16px;
  font-size: 16px;
  font-weight: 800;
  color: var(--text-strong);
  line-height: 1.5;
}

.activity-card__subtitle {
  margin-top: 8px;
  font-size: 13px;
  color: var(--text-soft-2);
  line-height: 1.7;
}

.activity-meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-top: 14px;
}

.activity-meta__item {
  padding: 12px;
  border-radius: 18px;
  background: var(--activity-meta-item-bg);
}

.activity-meta__label {
  display: block;
  font-size: 11px;
  color: var(--text-muted);
}

.activity-meta__value {
  display: block;
  margin-top: 6px;
  font-size: 13px;
  color: var(--text-strong);
  line-height: 1.5;
}

.activity-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.activity-pill {
  padding: 7px 10px;
  border-radius: 999px;
  font-size: 12px;
  line-height: 1;
}

.activity-pill--blue {
  background: var(--brand-blue-soft-bg);
  color: var(--brand-blue-soft-text);
}

.activity-pill--warning {
  background: var(--brand-warm-soft-bg);
  color: var(--brand-warm-soft-text);
}

.activity-pill--warm {
  background: var(--brand-warm-soft-bg);
  color: var(--brand-warm-soft-text);
}

.activity-pill--danger {
  background: var(--danger-soft-bg);
  color: var(--danger-soft-text);
}

.activity-pill--success {
  background: var(--brand-teal-soft-bg);
  color: var(--brand-teal-soft-text);
}

.activity-actions {
  display: flex;
  gap: 10px;
  margin-top: 16px;
}

.activity-empty {
  margin-top: 18px;
  padding: 26px 0;
  border-radius: 24px;
  background: var(--empty-card-bg);
  border: 1px solid var(--panel-border);
}

.activity-card--skeleton {
  margin-top: 14px;
}

.activity-detail-popup {
  max-height: 86vh;
  overflow: auto;
  background: var(--panel-bg-solid);
}

.activity-detail {
  padding: 10px 16px calc(22px + var(--safe-area-bottom));
}

.activity-detail__handle {
  width: 48px;
  height: 5px;
  border-radius: 999px;
  background: var(--panel-divider);
  margin: 0 auto 14px;
}

.activity-detail__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.activity-detail__plate {
  font-size: 24px;
  font-weight: 800;
  color: var(--text-strong);
}

.activity-detail__spec {
  margin-top: 6px;
  font-size: 12px;
  color: var(--text-soft);
}

.activity-detail__section {
  margin-top: 18px;
}

.activity-detail__section-title {
  font-size: 14px;
  font-weight: 800;
  color: var(--text-strong);
}

.activity-detail__summary {
  margin-top: 12px;
  font-size: 16px;
  font-weight: 800;
  line-height: 1.6;
  color: var(--text-strong);
}

.activity-detail__desc {
  margin-top: 8px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--text-soft-2);
}

.activity-detail__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-top: 12px;
}

.activity-detail__cell {
  padding: 13px 12px;
  border-radius: 18px;
  background: var(--surface-muted);
}

.activity-detail__cell span {
  display: block;
  font-size: 11px;
  color: var(--text-muted);
}

.activity-detail__cell strong {
  display: block;
  margin-top: 6px;
  font-size: 13px;
  line-height: 1.6;
  color: var(--text-strong);
}

.activity-detail__notes {
  display: grid;
  gap: 10px;
  margin-top: 12px;
}

.activity-detail__note {
  padding: 13px 12px;
  border-radius: 18px;
  background: var(--blue-notice-bg);
  border: 1px solid var(--blue-notice-border);
  font-size: 13px;
  line-height: 1.7;
  color: var(--blue-notice-text);
}

.activity-detail__actions {
  margin-top: 22px;
}

@media (max-width: 375px) {
  .activity-hero__title {
    font-size: 24px;
  }

  .activity-meta,
  .activity-detail__grid {
    grid-template-columns: 1fr;
  }

  .activity-actions {
    flex-direction: column;
  }
}
</style>
