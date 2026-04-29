<template>
  <div class="vehicles-page page-shell">
    <section class="vehicles-summary">
      <div class="vehicles-summary__copy">
        <div class="vehicles-summary__label">当前概况</div>
        <div class="vehicles-summary__desc">
          当前共 {{ overview.total }} 台车辆，可直接借用 {{ overview.available }} 台，使用中 {{ overview.inUse }} 台，今日受限 {{ overview.restrictedToday }} 台，需重点关注 {{ overview.needsAttention }} 台。
        </div>
      </div>
      <div class="vehicles-hero__meta">
        <span class="hero-chip hero-chip--warning">今日限行 {{ overview.restrictedToday }}</span>
        <span class="hero-chip hero-chip--neutral">待补油 {{ overview.pendingFuel }}</span>
        <span class="hero-chip hero-chip--warning">待复核 {{ overview.pendingCheck }}</span>
        <span class="hero-chip hero-chip--danger">临期证照 {{ overview.expiringSoon }}</span>
      </div>
    </section>

    <div class="overview-grid" v-loading="overviewLoading">
      <article class="overview-card overview-card--blue">
        <div class="overview-card__label">车辆总数</div>
        <div class="overview-card__value">{{ overview.total }}</div>
        <div class="overview-card__meta">当前系统内全部车辆</div>
      </article>
      <article class="overview-card overview-card--green">
        <div class="overview-card__label">可用车辆</div>
        <div class="overview-card__value">{{ overview.available }}</div>
        <div class="overview-card__meta">可直接借用</div>
      </article>
      <article class="overview-card overview-card--amber">
        <div class="overview-card__label">使用中</div>
        <div class="overview-card__value">{{ overview.inUse }}</div>
        <div class="overview-card__meta">当前有驾驶员在用</div>
      </article>
      <article class="overview-card overview-card--red">
        <div class="overview-card__label">需关注车辆</div>
        <div class="overview-card__value">{{ overview.needsAttention }}</div>
        <div class="overview-card__meta">维修、待复核、限行、补油提醒或证照临期</div>
      </article>
    </div>

    <el-card class="panel-card">
      <template #header>
        <div class="panel-header">
          <div>
            <div class="panel-header__title">车辆清单</div>
            <div class="panel-header__desc">主表优先展示调度、限行和管理最常用的信息，档案明细放到右侧详情里。</div>
          </div>
          <el-button v-if="canEdit" type="primary" @click="handleAdd">新增车辆</el-button>
        </div>
      </template>

      <div class="toolbar-row">
        <div class="toolbar-row__filters">
          <el-input
            v-model="queryForm.plateNumber"
            class="toolbar-search"
            placeholder="输入车牌号查询"
            clearable
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-select v-model="queryForm.status" placeholder="全部状态" clearable class="toolbar-select">
            <el-option label="正常" value="NORMAL" />
            <el-option label="使用中" value="IN_USE" />
            <el-option label="待复核" value="PENDING_CHECK" />
            <el-option label="维修中" value="MAINTENANCE" />
            <el-option label="报废" value="SCRAP" />
          </el-select>
        </div>
        <div class="toolbar-row__actions">
          <el-button @click="handleReset">重置</el-button>
          <el-button type="primary" @click="handleSearch">查询</el-button>
        </div>
      </div>

      <div class="quick-filters">
        <button
          v-for="item in statusQuickFilters"
          :key="item.value"
          type="button"
          class="quick-filter"
          :class="{ 'quick-filter--active': queryForm.status === item.value || (!queryForm.status && !item.value) }"
          @click="applyQuickFilter(item.value)"
        >
          <span>{{ item.label }}</span>
          <strong>{{ item.count }}</strong>
        </button>
      </div>

      <el-table v-loading="loading" :data="tableData" border class="vehicle-table">
        <el-table-column label="车辆信息" min-width="240">
          <template #default="{ row }">
            <div class="vehicle-cell">
              <div class="vehicle-cell__plate">{{ row.plateNumber }}</div>
              <div class="vehicle-cell__meta">
                {{ [row.brand, row.model].filter(Boolean).join(' / ') || '未填写品牌型号' }}
              </div>
              <div class="vehicle-cell__sub">{{ row.vehicleType || '未填写车型' }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="当前状态" width="230">
          <template #default="{ row }">
            <div class="status-cell">
              <div class="status-cell__tags">
                <el-tag :type="getStatusType(row.status)">{{ getStatusName(row.status) }}</el-tag>
                <el-tag
                  v-if="isRestrictedToday(row)"
                  :type="isRestrictionReleasedToday(row) ? 'success' : 'danger'"
                  effect="plain"
                  size="small"
                >
                  {{ isRestrictionReleasedToday(row) ? '今日限行已放行' : '今日限行' }}
                </el-tag>
              </div>
              <el-tooltip
                v-if="getRestrictionHint(row)"
                :content="row.trafficRestrictionMessage || getRestrictionHint(row)"
                placement="top"
              >
                <div
                  class="status-cell__hint"
                  :class="{ 'status-cell__hint--released': isRestrictionReleasedToday(row) }"
                >
                  {{ getRestrictionHint(row) }}
                </div>
              </el-tooltip>
              <div class="status-cell__text">
                {{ getLocationLabel(row) }}：{{ getLocationText(row) }}
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="里程 / 燃油预算" min-width="220">
          <template #default="{ row }">
            <div class="metric-cell">
              <div class="metric-row">
                <span>当前里程</span>
                <strong>{{ formatMileage(row.currentMileage) }} km</strong>
              </div>
              <div class="metric-row">
                <span>预算执行</span>
                <strong>¥{{ formatMoney(row.annualFuelUsed) }} / ¥{{ formatMoney(row.annualFuelBudget) }}</strong>
              </div>
              <el-progress
                :percentage="getBudgetPercentage(row)"
                :show-text="false"
                :stroke-width="8"
                :color="getBudgetProgressColor(row)"
              />
            </div>
          </template>
        </el-table-column>

        <el-table-column label="当前借用信息" min-width="210">
          <template #default="{ row }">
            <div class="borrow-cell">
              <div class="borrow-cell__driver">{{ row.currentDriverName || '当前无人借用' }}</div>
              <div class="borrow-cell__time">{{ row.currentDestination || '当前无借用去向' }}</div>
              <div class="borrow-cell__time">{{ formatDateTime(row.borrowTime) || '借车时间未记录' }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="证照到期" min-width="220">
          <template #default="{ row }">
            <div class="expiry-cell">
              <div class="expiry-cell__item">
                <span>保险</span>
                <el-tag :type="getDueTagType(row.insuranceExpireDate)" effect="plain">{{ getDueText(row.insuranceExpireDate) }}</el-tag>
              </div>
              <div class="expiry-cell__item">
                <span>年检</span>
                <el-tag :type="getDueTagType(row.inspectionExpireDate)" effect="plain">{{ getDueText(row.inspectionExpireDate) }}</el-tag>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="提醒" min-width="220">
          <template #default="{ row }">
            <div class="reminder-tags">
              <el-tag
                v-for="tag in getReminderTags(row)"
                :key="`${row.id}-${tag.label}`"
                :type="tag.type"
                effect="plain"
                size="small"
              >
                {{ tag.label }}
              </el-tag>
              <span v-if="!getReminderTags(row).length" class="reminder-tags__empty">当前无提醒</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <div class="action-group">
              <el-button
                v-if="canEdit && isRestrictedToday(row)"
                :class="[
                  'action-button',
                  isRestrictionReleasedToday(row) ? 'action-button--release-cancel' : 'action-button--release'
                ]"
                size="small"
                @click="handleTrafficRestrictionRelease(row, !isRestrictionReleasedToday(row))"
              >
                {{ isRestrictionReleasedToday(row) ? '取消放行' : '今日放行' }}
              </el-button>
              <el-button
                v-if="canEdit"
                class="action-button action-button--edit"
                size="small"
                @click="handleEdit(row)"
              >
                编辑
              </el-button>
              <el-button
                v-if="canDelete"
                class="action-button action-button--delete"
                size="small"
                @click="handleDelete(row)"
              >
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="当前没有匹配的车辆数据" :image-size="90" />
        </template>
      </el-table>

      <el-pagination
        v-model:current-page="pagination.current"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        class="table-pagination"
        @size-change="fetchData"
        @current-change="fetchData"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="760px" class="vehicle-editor-dialog">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="vehicle-editor-form">
        <section class="editor-section">
          <div class="editor-section__title">基础信息</div>
          <div class="editor-grid">
            <el-form-item label="车牌号" prop="plateNumber">
              <el-input v-model="form.plateNumber" placeholder="如：陕A12345" />
            </el-form-item>
            <el-form-item label="车型">
              <el-select v-model="form.vehicleType" placeholder="请选择车型">
                <el-option label="轿车" value="轿车" />
                <el-option label="SUV" value="SUV" />
                <el-option label="商务车" value="商务车" />
                <el-option label="客车" value="客车" />
              </el-select>
            </el-form-item>
            <el-form-item label="品牌">
              <el-input v-model="form.brand" placeholder="如：大众" />
            </el-form-item>
            <el-form-item label="型号">
              <el-input v-model="form.model" placeholder="如：Passat" />
            </el-form-item>
            <el-form-item label="颜色">
              <el-input v-model="form.color" placeholder="如：黑色" />
            </el-form-item>
            <el-form-item label="注册日期">
              <el-date-picker v-model="form.registerDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
            <el-form-item label="车架号">
              <el-input v-model="form.vin" placeholder="请输入车架号" />
            </el-form-item>
            <el-form-item label="发动机号">
              <el-input v-model="form.engineNumber" placeholder="请输入发动机号" />
            </el-form-item>
          </div>
        </section>

        <section class="editor-section">
          <div class="editor-section__title">运营信息</div>
          <div class="editor-grid">
            <el-form-item label="当前里程">
              <el-input-number v-model="form.currentMileage" :min="0" :step="100" style="width: 100%" />
            </el-form-item>
            <el-form-item label="年燃油预算">
              <el-input-number v-model="form.annualFuelBudget" :min="0" :step="1000" style="width: 100%" />
            </el-form-item>
            <el-form-item label="停放位置" class="editor-grid__span-2">
              <el-input v-model="form.parkingLocation" placeholder="请输入常用停放位置" />
            </el-form-item>
          </div>
        </section>

        <section class="editor-section">
          <div class="editor-section__title">保险与年检</div>
          <div class="editor-grid">
            <el-form-item label="保险公司">
              <el-input v-model="form.insuranceCompany" placeholder="请输入保险公司" />
            </el-form-item>
            <el-form-item label="保险到期日期">
              <el-date-picker v-model="form.insuranceExpireDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
            <el-form-item label="年检到期日期">
              <el-date-picker v-model="form.inspectionExpireDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </div>
        </section>

      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import {
  createVehicle,
  deleteVehicle,
  getVehicleOverview,
  getVehicles,
  updateVehicle,
  updateVehicleTrafficRestrictionRelease
} from '@/api/vehicle'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const canEdit = computed(() => ['SUPER_ADMIN', 'OFFICE_ADMIN'].includes(userStore.userInfo.role))
const canDelete = computed(() => userStore.userInfo.role === 'SUPER_ADMIN')

const loading = ref(false)
const overviewLoading = ref(false)
const tableData = ref([])

const queryForm = reactive({
  plateNumber: '',
  status: ''
})

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const overview = reactive({
  total: 0,
  available: 0,
  inUse: 0,
  restrictedToday: 0,
  needsAttention: 0,
  pendingFuel: 0,
  pendingCheck: 0,
  expiringSoon: 0
})

const dialogVisible = ref(false)
const dialogTitle = ref('新增车辆')
const formRef = ref()

const createEmptyForm = () => ({
  id: null,
  plateNumber: '',
  vehicleType: '',
  brand: '',
  model: '',
  color: '',
  vin: '',
  engineNumber: '',
  registerDate: '',
  currentMileage: 0,
  annualFuelBudget: 12000,
  parkingLocation: '西京酒店停车场',
  insuranceCompany: '',
  insuranceExpireDate: '',
  inspectionExpireDate: ''
})

const form = reactive(createEmptyForm())

const rules = {
  plateNumber: [{ required: true, message: '请输入车牌号', trigger: 'blur' }]
}

const statusMeta = {
  NORMAL: { label: '正常', type: 'success' },
  IN_USE: { label: '使用中', type: 'warning' },
  PENDING_CHECK: { label: '待复核', type: 'warning' },
  MAINTENANCE: { label: '维修中', type: 'danger' },
  SCRAP: { label: '报废', type: 'info' }
}

const fuelReminderMeta = {
  NONE: { label: '无提醒', type: 'info' },
  PENDING: { label: '待补油', type: 'warning' },
  COMPLETED: { label: '已补油', type: 'success' }
}

const statusQuickFilters = computed(() => {
  const list = tableData.value
  const countBy = (status) => list.filter(item => item.status === status).length
  return [
    { label: '全部', value: '', count: overview.total },
    { label: '正常', value: 'NORMAL', count: countBy('NORMAL') },
    { label: '使用中', value: 'IN_USE', count: countBy('IN_USE') },
    { label: '待复核', value: 'PENDING_CHECK', count: countBy('PENDING_CHECK') },
    { label: '维修中', value: 'MAINTENANCE', count: countBy('MAINTENANCE') }
  ]
})

const formatMoney = (value) => {
  if (value === null || value === undefined || value === '') return '0.00'
  return Number(value).toFixed(2)
}

const formatMileage = (value) => {
  if (value === null || value === undefined || value === '') return '0'
  return Number(value).toFixed(0)
}

const formatDateTime = (value) => {
  if (!value) return ''
  return String(value).substring(0, 19).replace('T', ' ')
}

const getStatusName = (status) => statusMeta[status]?.label || status || '-'
const getStatusType = (status) => statusMeta[status]?.type || 'info'
const getFuelReminderName = (status) => fuelReminderMeta[status]?.label || '无提醒'
const getFuelReminderType = (status) => fuelReminderMeta[status]?.type || 'info'

const isRestrictedToday = (row) => Number(row?.trafficRestrictedToday || 0) === 1
const isRestrictionReleasedToday = (row) => Number(row?.trafficRestrictionReleasedToday || 0) === 1
const canBorrowToday = (row) => row?.status === 'NORMAL' && (!isRestrictedToday(row) || isRestrictionReleasedToday(row))

const getLocationText = (row) => {
  if (row.status === 'IN_USE') {
    return row.currentDestination || '已借出，未填写去向'
  }
  return row.parkingLocation || '未填写停放位置'
}

const getLocationLabel = (row) => (row?.status === 'IN_USE' ? '去向' : '停放')

const getRestrictionHint = (row) => {
  if (!isRestrictedToday(row)) return ''
  const matchedRule = row?.trafficRestrictionMessage?.match(/（(.+?)）/)
  const ruleText = matchedRule?.[1] || '今日尾号限行'
  return isRestrictionReleasedToday(row)
    ? `${ruleText}，已手动放行`
    : `${ruleText}，需管理员放行`
}

const getBudgetPercentage = (row) => {
  const budget = Number(row.annualFuelBudget || 0)
  const used = Number(row.annualFuelUsed || 0)
  if (!budget) return 0
  return Math.max(0, Math.min(100, Number(((used / budget) * 100).toFixed(2))))
}

const getBudgetProgressColor = (row) => {
  const percentage = getBudgetPercentage(row)
  if (percentage >= 100) return '#d84c4c'
  if (percentage >= 80) return '#e98b2a'
  return '#1f6feb'
}

const getDaysUntil = (dateValue) => {
  if (!dateValue) return null
  const target = new Date(`${dateValue}T00:00:00`)
  if (Number.isNaN(target.getTime())) return null
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  return Math.ceil((target.getTime() - today.getTime()) / (1000 * 60 * 60 * 24))
}

const getDueText = (dateValue) => {
  if (!dateValue) return '未设置'
  const days = getDaysUntil(dateValue)
  if (days === null) return dateValue
  if (days < 0) return `已过期 ${Math.abs(days)} 天`
  if (days === 0) return '今天到期'
  if (days <= 30) return `${days} 天内到期`
  return dateValue
}

const getDueTagType = (dateValue) => {
  const days = getDaysUntil(dateValue)
  if (days === null) return 'info'
  if (days < 0) return 'danger'
  if (days <= 30) return 'warning'
  return 'success'
}

const getReminderTags = (row) => {
  const items = []
  if (isRestrictedToday(row)) {
    items.push({
      label: isRestrictionReleasedToday(row) ? '今日限行已放行' : '今日限行',
      type: isRestrictionReleasedToday(row) ? 'success' : 'danger'
    })
  }
  if (row.fuelReminderStatus === 'PENDING') {
    items.push({ label: '待补油', type: 'warning' })
  }
  if (row.status === 'PENDING_CHECK') {
    items.push({ label: '待复核', type: 'warning' })
  }
  if (row.status === 'MAINTENANCE') {
    items.push({ label: '维修中', type: 'danger' })
  }
  if (getDaysUntil(row.insuranceExpireDate) !== null && getDaysUntil(row.insuranceExpireDate) <= 30) {
    items.push({ label: '保险临期', type: 'warning' })
  }
  if (getDaysUntil(row.inspectionExpireDate) !== null && getDaysUntil(row.inspectionExpireDate) <= 30) {
    items.push({ label: '年检临期', type: 'danger' })
  }
  return items
}

const updateOverview = (vehicles) => {
  overview.total = vehicles.length
  overview.available = vehicles.filter(item => canBorrowToday(item)).length
  overview.inUse = vehicles.filter(item => item.status === 'IN_USE').length
  overview.restrictedToday = vehicles.filter(item => isRestrictedToday(item) && !isRestrictionReleasedToday(item)).length
  overview.pendingFuel = vehicles.filter(item => item.fuelReminderStatus === 'PENDING').length
  overview.pendingCheck = vehicles.filter(item => item.status === 'PENDING_CHECK').length
  overview.expiringSoon = vehicles.filter(item => {
    const insuranceDays = getDaysUntil(item.insuranceExpireDate)
    const inspectionDays = getDaysUntil(item.inspectionExpireDate)
    return (insuranceDays !== null && insuranceDays <= 30) || (inspectionDays !== null && inspectionDays <= 30)
  }).length
  overview.needsAttention = vehicles.filter(item => {
    return (isRestrictedToday(item) && !isRestrictionReleasedToday(item))
      || item.status === 'PENDING_CHECK'
      || item.status === 'MAINTENANCE'
      || item.fuelReminderStatus === 'PENDING'
      || getReminderTags(item).some(tag => tag.label === '保险临期' || tag.label === '年检临期')
  }).length
}

const fetchOverview = async () => {
  overviewLoading.value = true
  try {
    const res = await getVehicleOverview()
    overview.total = res.total || 0
    overview.available = res.available || 0
    overview.inUse = res.inUse || 0
    overview.restrictedToday = res.restrictedToday || 0
    overview.needsAttention = res.needsAttention || 0
    overview.pendingFuel = res.pendingFuel || 0
    overview.pendingCheck = res.pendingCheck || 0
  } catch (error) {
    ElMessage.error('获取车辆概览失败')
  } finally {
    overviewLoading.value = false
  }
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getVehicles({
      current: pagination.current,
      size: pagination.size,
      ...queryForm
    })
    tableData.value = res.data || []
    pagination.total = Number(res.total || 0)
  } catch (error) {
    ElMessage.error('获取车辆列表失败')
  } finally {
    loading.value = false
  }
}

const refreshPage = async () => {
  await Promise.all([fetchData(), fetchOverview()])
}

const handleSearch = () => {
  pagination.current = 1
  fetchData()
}

const handleReset = () => {
  queryForm.plateNumber = ''
  queryForm.status = ''
  handleSearch()
}

const applyQuickFilter = (status) => {
  queryForm.status = status
  handleSearch()
}

const resetForm = () => {
  Object.assign(form, createEmptyForm())
}

const handleAdd = () => {
  dialogTitle.value = '新增车辆'
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑车辆'
  resetForm()
  Object.assign(form, {
    ...createEmptyForm(),
    ...row
  })
  dialogVisible.value = true
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定删除车辆 ${row.plateNumber} 吗？`, '删除确认', { type: 'warning' })
    await deleteVehicle(row.id)
    ElMessage.success('删除成功')
    await refreshPage()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || error.message || '删除失败')
    }
  }
}

const handleTrafficRestrictionRelease = async (row, released) => {
  const actionText = released ? '手动放行' : '取消放行'
  const confirmText = released
    ? `确认将 ${row.plateNumber} 设置为“今日限行已放行”吗？`
    : `确认取消 ${row.plateNumber} 的今日限行放行吗？`

  try {
    await ElMessageBox.confirm(confirmText, actionText, { type: released ? 'warning' : 'info' })
    await updateVehicleTrafficRestrictionRelease(row.id, released)
    ElMessage.success(released ? '已放行，今日可正常出车' : '已取消放行，今日重新受限')
    await refreshPage()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || error.message || `${actionText}失败`)
    }
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  try {
    const payload = {
      plateNumber: form.plateNumber,
      vehicleType: form.vehicleType,
      brand: form.brand,
      model: form.model,
      color: form.color,
      vin: form.vin,
      engineNumber: form.engineNumber,
      registerDate: form.registerDate,
      currentMileage: form.currentMileage,
      annualFuelBudget: form.annualFuelBudget,
      parkingLocation: form.parkingLocation,
      insuranceCompany: form.insuranceCompany,
      insuranceExpireDate: form.insuranceExpireDate,
      inspectionExpireDate: form.inspectionExpireDate
    }
    if (form.id) {
      await updateVehicle(form.id, payload)
      ElMessage.success('更新成功')
    } else {
      await createVehicle(payload)
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    await refreshPage()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || error.message || '保存失败')
  }
}

onMounted(() => {
  refreshPage()
})
</script>

<style scoped>
.vehicles-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.vehicles-summary {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: center;
  padding: 0 2px;
}

.vehicles-summary__label {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: #67778f;
}

.vehicles-summary__desc {
  margin-top: 8px;
  max-width: 760px;
  font-size: 14px;
  line-height: 1.6;
  color: #69788e;
}

.vehicles-hero__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}

.hero-chip {
  display: inline-flex;
  align-items: center;
  padding: 8px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  background: #ffffff;
  border: 1px solid rgba(215, 223, 234, 0.92);
  color: #49556d;
}

.hero-chip--warning {
  color: #9f5b08;
  background: rgba(244, 177, 63, 0.12);
  border-color: rgba(233, 139, 42, 0.24);
}

.hero-chip--danger {
  color: #b43c4e;
  background: rgba(216, 76, 76, 0.10);
  border-color: rgba(216, 76, 76, 0.2);
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.overview-card {
  padding: 18px 18px 16px;
  border-radius: 16px;
  border: 1px solid rgba(216, 224, 235, 0.92);
  background: #ffffff;
  box-shadow: 0 6px 18px rgba(15, 23, 42, 0.03);
}

.overview-card--blue { border-top: 4px solid #2b69d9; }
.overview-card--green { border-top: 4px solid #16795f; }
.overview-card--amber { border-top: 4px solid #c97b19; }
.overview-card--red { border-top: 4px solid #c94c4c; }

.overview-card__label {
  font-size: 13px;
  font-weight: 700;
  color: #6c7891;
}

.overview-card__value {
  margin-top: 10px;
  font-size: 34px;
  line-height: 1;
  font-weight: 800;
  color: #182236;
}

.overview-card__meta {
  margin-top: 10px;
  font-size: 13px;
  color: #73809a;
}

.panel-card {
  overflow: hidden;
  border-radius: 18px;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
}

.panel-header__title {
  font-size: 18px;
  font-weight: 800;
  color: #172033;
}

.panel-header__desc {
  margin-top: 6px;
  font-size: 13px;
  color: #6c7891;
  line-height: 1.6;
}

.toolbar-row {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 14px;
}

.toolbar-row__filters,
.toolbar-row__actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.toolbar-search {
  width: 300px;
}

.toolbar-select {
  width: 180px;
}

.quick-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 14px;
}

.quick-filter {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border-radius: 999px;
  border: 1px solid rgba(215, 223, 234, 0.92);
  background: #f7f9fc;
  color: #57647d;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: background 0.2s ease, border-color 0.2s ease, color 0.2s ease;
}

.quick-filter strong {
  font-size: 13px;
  color: #2b69d9;
}

.quick-filter:hover,
.quick-filter--active {
  color: #2b69d9;
  border-color: rgba(43, 105, 217, 0.22);
  background: rgba(43, 105, 217, 0.08);
}

.vehicle-table {
  width: 100%;
}

:deep(.vehicle-table .el-table__cell) {
  padding: 16px 0;
  vertical-align: top;
}

:deep(.vehicle-table .cell) {
  padding: 0 14px;
}

.vehicle-cell {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.vehicle-cell__plate {
  font-size: 16px;
  font-weight: 800;
  color: #172033;
}

.vehicle-cell__meta,
.vehicle-cell__sub,
.status-cell__text,
.status-cell__hint,
.borrow-cell__time,
.reminder-tags__empty {
  font-size: 13px;
  color: #6a7890;
}

.status-cell,
.borrow-cell,
.metric-cell,
.expiry-cell {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.status-cell__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.status-cell__hint {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  max-width: 100%;
  padding: 6px 10px;
  border-radius: 12px;
  border: 1px solid rgba(232, 112, 89, 0.18);
  background: rgba(255, 244, 241, 0.92);
  color: #ce5a49;
  line-height: 1.5;
}

.status-cell__hint--released {
  border-color: rgba(22, 121, 95, 0.18);
  background: rgba(235, 248, 243, 0.92);
  color: #16795f;
}

.status-cell__text {
  line-height: 1.6;
}

.metric-row,
.expiry-cell__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.metric-row span,
.expiry-cell__item span {
  font-size: 13px;
  color: #6e7a92;
}

.metric-row strong,
.borrow-cell__driver {
  font-size: 13px;
  color: #172033;
  font-weight: 700;
}

.reminder-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-content: flex-start;
  min-height: 32px;
}

.reminder-tags__empty {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  border: 1px dashed rgba(208, 217, 230, 0.9);
  background: #f8fafc;
}

.action-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

:deep(.action-group .action-button.el-button) {
  margin-left: 0;
  min-width: 84px;
  height: 34px;
  padding: 0 16px;
  border-radius: 999px;
  border: 1px solid transparent;
  box-shadow: none;
  font-weight: 700;
  letter-spacing: 0.01em;
  transition: transform 0.18s ease, background 0.18s ease, border-color 0.18s ease, color 0.18s ease;
}

:deep(.action-group .action-button.el-button:hover) {
  transform: translateY(-1px);
}

:deep(.action-group .action-button--edit.el-button) {
  color: #46658c;
  background: linear-gradient(180deg, #f4f8ff 0%, #e8f0fb 100%);
  border-color: rgba(106, 134, 170, 0.22);
}

:deep(.action-group .action-button--edit.el-button:hover) {
  color: #36557d;
  background: linear-gradient(180deg, #eef5ff 0%, #dfeaf8 100%);
  border-color: rgba(90, 120, 160, 0.3);
}

:deep(.action-group .action-button--release.el-button) {
  color: #256b5e;
  background: linear-gradient(180deg, #eef8f4 0%, #dff0e8 100%);
  border-color: rgba(56, 132, 114, 0.2);
}

:deep(.action-group .action-button--release.el-button:hover) {
  color: #1e5b51;
  background: linear-gradient(180deg, #e6f4ef 0%, #d4e9df 100%);
  border-color: rgba(44, 112, 98, 0.28);
}

:deep(.action-group .action-button--release-cancel.el-button) {
  color: #8a6531;
  background: linear-gradient(180deg, #fcf6ea 0%, #f6ecd8 100%);
  border-color: rgba(190, 148, 84, 0.24);
}

:deep(.action-group .action-button--release-cancel.el-button:hover) {
  color: #755526;
  background: linear-gradient(180deg, #f9f1e0 0%, #f1e4ca 100%);
  border-color: rgba(166, 124, 61, 0.3);
}

:deep(.action-group .action-button--delete.el-button) {
  color: #a55f6a;
  background: linear-gradient(180deg, #fff4f5 0%, #fbe8eb 100%);
  border-color: rgba(214, 138, 151, 0.24);
}

:deep(.action-group .action-button--delete.el-button:hover) {
  color: #8f4f5a;
  background: linear-gradient(180deg, #feeeef 0%, #f6dde2 100%);
  border-color: rgba(196, 118, 133, 0.3);
}

.table-pagination {
  margin-top: 20px;
  justify-content: flex-end;
}

.detail-shell {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.detail-hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  padding: 18px;
  border-radius: 14px;
  background: #f7f9fc;
  border: 1px solid rgba(221, 228, 238, 0.92);
}

.detail-hero__plate {
  font-size: 24px;
  font-weight: 800;
  color: #172033;
}

.detail-hero__meta {
  margin-top: 6px;
  font-size: 13px;
  color: #63708a;
}

.detail-hero__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}

.detail-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.detail-section__title,
.editor-section__title {
  font-size: 15px;
  font-weight: 800;
  color: #172033;
}

.detail-actions {
  display: flex;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 10px;
}

.vehicle-editor-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.editor-section {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.editor-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.editor-grid__span-2 {
  grid-column: span 2;
}

.vehicle-editor__alert {
  margin-top: 4px;
}

:deep(.vehicle-editor-dialog .el-dialog__body) {
  padding-top: 12px !important;
}

:deep(.detail-shell .el-descriptions__label) {
  width: 126px;
}

@media (max-width: 1280px) {
  .overview-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 960px) {
  .vehicles-summary,
  .panel-header,
  .toolbar-row {
    flex-direction: column;
    align-items: stretch;
  }

  .toolbar-search,
  .toolbar-select {
    width: 100%;
  }

  .editor-grid {
    grid-template-columns: 1fr;
  }

  .editor-grid__span-2 {
    grid-column: span 1;
  }
}

@media (max-width: 720px) {
  .overview-grid {
    grid-template-columns: 1fr;
  }
}
</style>
