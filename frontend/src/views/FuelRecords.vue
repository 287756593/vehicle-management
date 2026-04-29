<template>
  <div class="fuel-records page-shell">
    <section class="fuel-workbench">
      <div class="fuel-workbench__main">
        <div class="fuel-workbench__label">当前视图</div>
        <div class="fuel-workbench__desc">
          {{ queryForm.year }} 年 {{ selectedVehicleLabel }} 当前筛选共 {{ pagination.total }} 条记录，范围内待审批 {{ fuelOverview.currentPendingApproval }} 条，未报销 {{ fuelOverview.currentUnreimbursed }} 条。
        </div>
        <div class="fuel-workbench__chips">
          <span class="hero-chip">年度 {{ queryForm.year }}</span>
          <span class="hero-chip hero-chip--accent">{{ selectedVehicleLabel }}</span>
          <span class="hero-chip hero-chip--warning">待审批 {{ fuelOverview.currentPendingApproval }}</span>
          <span class="hero-chip hero-chip--success">未报销 {{ fuelOverview.currentUnreimbursed }}</span>
        </div>

        <div v-if="budgetStats" class="fuel-budget-strip">
          <div class="fuel-budget-strip__item">
            <span>年度预算</span>
            <strong>¥{{ formatMoney(budgetStats.budget) }}</strong>
          </div>
          <div class="fuel-budget-strip__item">
            <span>已计入预算</span>
            <strong>¥{{ formatMoney(budgetStats.used) }}</strong>
          </div>
          <div class="fuel-budget-strip__item">
            <span>剩余额度</span>
            <strong>¥{{ formatMoney(budgetStats.remaining) }}</strong>
          </div>
          <div class="fuel-budget-strip__item">
            <span>预算使用率</span>
            <strong>{{ formatPercent(budgetStats.usageRate) }}%</strong>
          </div>
        </div>
      </div>

      <div class="fuel-workbench__side">
        <div class="fuel-filter-toolbar">
          <label class="fuel-filter-field">
            <span>车辆</span>
            <el-select v-model="queryForm.vehicleId" placeholder="全部车辆" clearable class="fuel-filter-field__control">
              <el-option v-for="v in vehicles" :key="v.id" :label="v.plateNumber" :value="v.id" />
            </el-select>
          </label>

          <label class="fuel-filter-field">
            <span>年份</span>
            <el-input-number v-model="queryForm.year" :min="2024" :max="2099" :step="1" class="fuel-filter-field__control fuel-filter-year" />
          </label>

          <label class="fuel-filter-field">
            <span>审批状态</span>
            <el-select v-model="queryForm.status" placeholder="全部状态" clearable class="fuel-filter-field__control">
              <el-option label="无需审批（历史）" value="NONE" />
              <el-option label="待审批" value="PENDING" />
              <el-option label="已批准" value="APPROVED" />
              <el-option label="已驳回" value="REJECTED" />
            </el-select>
          </label>

          <div class="fuel-filter-actions">
            <el-button @click="handleReset">重置</el-button>
            <el-button type="primary" @click="handleSearch">查询</el-button>
          </div>
        </div>

        <div class="fuel-filter-summary">
          <span>车辆：{{ selectedVehicleLabel }}</span>
          <span>年份：{{ queryForm.year }}</span>
          <span>状态：{{ selectedStatusLabel }}</span>
        </div>

        <div class="fuel-spotlight">
          <div class="fuel-spotlight__label">{{ yearFuelCostLabel }}</div>
          <div class="fuel-spotlight__value">¥{{ formatMoney(yearFuelCostValue) }}</div>
          <div class="fuel-spotlight__desc">{{ yearlyInsightDescription }}</div>
          <div class="fuel-spotlight__meta">{{ fuelSpotlight.title }}：{{ fuelSpotlight.value }}</div>
        </div>
      </div>
    </section>

    <el-card class="panel-card">
      <template #header>
        <div class="panel-header">
          <div>
            <div class="panel-header__title">加油流水主表</div>
            <div class="panel-header__desc">按车辆、年份和审批状态筛选，统一核对金额、公里数、加油后油量和报销流转。</div>
          </div>
          <div class="panel-header__note">单车年度油费放在下方查看</div>
        </div>
      </template>

      <div class="quick-filters">
        <button
          type="button"
          class="quick-filter"
          :class="{ 'quick-filter--active': !queryForm.status }"
          @click="queryForm.status = ''; handleSearch()"
        >
          <span>全部状态</span>
          <strong>{{ fuelOverview.total }}</strong>
        </button>
        <button
          type="button"
          class="quick-filter"
          :class="{ 'quick-filter--active': queryForm.status === 'NONE' }"
          @click="queryForm.status = 'NONE'; handleSearch()"
        >
          <span>无需审批（历史）</span>
          <strong>{{ fuelOverview.noApproval }}</strong>
        </button>
        <button
          type="button"
          class="quick-filter"
          :class="{ 'quick-filter--active': queryForm.status === 'PENDING' }"
          @click="queryForm.status = 'PENDING'; handleSearch()"
        >
          <span>待审批</span>
          <strong>{{ fuelOverview.pendingApproval }}</strong>
        </button>
        <button
          type="button"
          class="quick-filter"
          :class="{ 'quick-filter--active': queryForm.status === 'APPROVED' }"
          @click="queryForm.status = 'APPROVED'; handleSearch()"
        >
          <span>已批准</span>
          <strong>{{ fuelOverview.approved }}</strong>
        </button>
      </div>

      <el-table :data="tableData" border class="fuel-table">
        <el-table-column label="加油摘要" min-width="250">
          <template #default="{ row }">
            <div class="record-cell">
              <div class="record-cell__title">{{ row.plateNumber }}</div>
              <div class="record-cell__meta">{{ row.driverName || '未记录驾驶员' }}</div>
              <div class="record-cell__sub">{{ formatDate(row.fuelDate) }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="金额 / 公里数" min-width="180">
          <template #default="{ row }">
            <div class="amount-cell">
              <div class="amount-cell__value">¥{{ formatMoney(row.totalAmount) }}</div>
              <div class="amount-cell__sub">公里数 {{ row.fuelMileage ?? '-' }} km</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="油品 / 登记内容" width="150">
          <template #default="{ row }">
            <div class="simple-stack">
              <span>{{ row.fuelType || '-' }}</span>
              <small>{{ formatFuelMeasure(row) }}</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="加油后油量" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.isFuelEnoughAfterFuel === 1" type="success">已满油</el-tag>
            <el-tag v-else-if="row.isFuelEnoughAfterFuel === 0" type="warning">未满油</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="审批状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getReportStatusType(row.cashReportStatus)">{{ getReportStatusName(row.cashReportStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="报销状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getReimbursementStatusType(row.reimbursementStatus)">{{ getReimbursementStatusName(row.reimbursementStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="380" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="showDetail(row)">查看</el-button>
            <el-button
              v-if="canEditFuelAmount(row)"
              link
              type="primary"
              @click="handleEditFuelAmount(row)"
            >
              {{ hasFuelAmount(row) ? '修改加油量' : '补录加油量' }}
            </el-button>
            <el-button
              v-if="canApprove(row)"
              link
              type="warning"
              @click="handleApproveCash(row)"
            >
              审批
            </el-button>
            <el-button
              v-if="canToggleReimbursement(row)"
              link
              :type="row.reimbursementStatus === 'REIMBURSED' ? 'warning' : 'success'"
              @click="handleToggleReimbursement(row)"
            >
              {{ row.reimbursementStatus === 'REIMBURSED' ? '标记未报销' : '标记已报销' }}
            </el-button>
            <el-button
              v-if="userStore.userInfo.role === 'OFFICE_ADMIN' || userStore.userInfo.role === 'SUPER_ADMIN'"
              link
              type="danger"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="当前没有匹配的加油记录" :image-size="90" />
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

    <el-card class="panel-card year-fuel-panel">
      <template #header>
        <div class="panel-header">
          <div>
            <div class="panel-header__title">车辆年度油费</div>
            <div class="panel-header__desc">点车牌可以切到对应车辆视图，也能直接看每台车 {{ queryForm.year }} 年用了多少油费。</div>
          </div>
          <div class="panel-header__note">当前选中：{{ selectedVehicleLabel }}</div>
        </div>
      </template>

      <div class="year-fuel-grid">
        <button
          type="button"
          class="year-fuel-card"
          :class="{ 'year-fuel-card--active': !queryForm.vehicleId }"
          @click="applyVehicleFilter(null)"
        >
          <span class="year-fuel-card__plate">全部车辆</span>
          <strong class="year-fuel-card__amount">¥{{ formatMoney(totalYearFuelCost) }}</strong>
          <small class="year-fuel-card__meta">{{ queryForm.year }} 年汇总</small>
        </button>

        <button
          v-for="item in yearlyVehicleSummary"
          :key="item.vehicleId"
          type="button"
          class="year-fuel-card"
          :class="{ 'year-fuel-card--active': queryForm.vehicleId === item.vehicleId }"
          @click="applyVehicleFilter(item.vehicleId)"
        >
          <div class="year-fuel-card__head">
            <span class="year-fuel-card__plate">{{ item.plateNumber }}</span>
            <span class="year-fuel-card__usage">{{ formatPercent(item.usageRate) }}%</span>
          </div>
          <strong class="year-fuel-card__amount">¥{{ formatMoney(item.usedAmount) }}</strong>
          <small class="year-fuel-card__meta">{{ item.recordCount }} 条记录 · 剩余预算 ¥{{ formatMoney(item.remaining) }}</small>
        </button>
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" title="加油记录详情" width="860px">
      <template v-if="currentRecord">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="车牌号">{{ currentRecord.plateNumber }}</el-descriptions-item>
          <el-descriptions-item label="驾驶员">{{ currentRecord.driverName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="加油时间">{{ formatDate(currentRecord.fuelDate) }}</el-descriptions-item>
          <el-descriptions-item label="加油站">{{ currentRecord.fuelLocation }}</el-descriptions-item>
          <el-descriptions-item label="油品类型">{{ currentRecord.fuelType || '-' }}</el-descriptions-item>
          <el-descriptions-item label="登记内容">{{ formatFuelMeasure(currentRecord) }}</el-descriptions-item>
          <el-descriptions-item label="单价">{{ formatFuelUnitPrice(currentRecord) }}</el-descriptions-item>
          <el-descriptions-item label="加油时公里数">{{ currentRecord.fuelMileage == null ? '-' : `${currentRecord.fuelMileage} km` }}</el-descriptions-item>
          <el-descriptions-item label="总金额">¥{{ formatMoney(currentRecord.totalAmount) }}</el-descriptions-item>
          <el-descriptions-item label="加油后油量">
            <span v-if="currentRecord.isFuelEnoughAfterFuel === 1">已满油</span>
            <span v-else-if="currentRecord.isFuelEnoughAfterFuel === 0">未满油</span>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="审批状态">
            {{ getReportStatusName(currentRecord.cashReportStatus) }}
          </el-descriptions-item>
          <el-descriptions-item label="报销状态">
            {{ getReimbursementStatusName(currentRecord.reimbursementStatus) }}
          </el-descriptions-item>
          <el-descriptions-item label="报销时间">
            {{ formatDate(currentRecord.reimbursedTime) || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ currentRecord.remark || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-divider>加油凭证照片</el-divider>
        <div class="photo-grid">
          <el-image
            v-for="(photo, index) in getPhotoList(currentRecord.invoicePhoto)"
            :key="`invoice-${index}`"
            :src="photo"
            :preview-src-list="getPhotoList(currentRecord.invoicePhoto)"
            fit="cover"
            class="photo-item"
          />
          <span v-if="getPhotoList(currentRecord.invoicePhoto).length === 0" class="empty-text">无</span>
        </div>

        <template v-if="getPhotoList(currentRecord.fuelGaugePhoto).length > 0">
          <el-divider>加油后油表照片（历史）</el-divider>
          <div class="photo-grid">
            <el-image
              v-for="(photo, index) in getPhotoList(currentRecord.fuelGaugePhoto)"
              :key="`gauge-${index}`"
              :src="photo"
              :preview-src-list="getPhotoList(currentRecord.fuelGaugePhoto)"
              fit="cover"
              class="photo-item"
            />
          </div>
        </template>

        <template v-if="currentRecord.isCash === 1">
          <template v-if="getPhotoList(currentRecord.leaderApprovalPhoto).length > 0">
            <el-divider>领导同意截图（历史数据）</el-divider>
            <div class="photo-grid">
              <el-image
                v-for="(photo, index) in getPhotoList(currentRecord.leaderApprovalPhoto)"
                :key="`approval-${index}`"
                :src="photo"
                :preview-src-list="getPhotoList(currentRecord.leaderApprovalPhoto)"
                fit="cover"
                class="photo-item"
              />
            </div>
          </template>

          <el-divider>现金小票或发票</el-divider>
          <div class="photo-grid">
            <el-image
              v-for="(photo, index) in getPhotoList(currentRecord.cashPhoto)"
              :key="`cash-${index}`"
              :src="photo"
              :preview-src-list="getPhotoList(currentRecord.cashPhoto)"
              fit="cover"
              class="photo-item"
            />
            <span v-if="getPhotoList(currentRecord.cashPhoto).length === 0" class="empty-text">无</span>
          </div>
        </template>
      </template>

      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button v-if="canApprove(currentRecord)" type="warning" @click="handleApproveCash(currentRecord)">审批旧记录</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="approveDialogVisible" title="历史现金记录审批" width="420px">
      <el-form :model="approveForm" label-width="80px">
        <el-form-item label="审批结果">
          <el-radio-group v-model="approveForm.approved">
            <el-radio :label="true">通过</el-radio>
            <el-radio :label="false">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="approveForm.comment" type="textarea" :rows="3" placeholder="可填写审批意见" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approveDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleApproveSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="fuelAmountDialogVisible"
      :title="fuelAmountDialogTitle"
      width="560px"
      @closed="resetFuelAmountForm"
    >
      <div class="fuel-amount-dialog">
        <div class="fuel-amount-dialog__summary">
          <div class="fuel-amount-dialog__card">
            <span>车辆</span>
            <strong>{{ fuelAmountForm.plateNumber || '-' }}</strong>
          </div>
          <div class="fuel-amount-dialog__card">
            <span>驾驶员</span>
            <strong>{{ fuelAmountForm.driverName || '未记录驾驶员' }}</strong>
          </div>
          <div class="fuel-amount-dialog__card">
            <span>加油时间</span>
            <strong>{{ formatDate(fuelAmountForm.fuelDate) || '-' }}</strong>
          </div>
          <div class="fuel-amount-dialog__card">
            <span>总金额</span>
            <strong>¥{{ formatMoney(fuelAmountForm.totalAmount) }}</strong>
          </div>
        </div>

        <el-form :model="fuelAmountForm" label-width="92px" class="fuel-amount-form">
          <el-form-item label="加油量">
            <div class="fuel-amount-form__editor">
              <el-input-number
                v-model="fuelAmountForm.fuelAmount"
                :min="0.01"
                :step="0.1"
                :precision="2"
                controls-position="right"
                class="fuel-amount-form__input"
                placeholder="请输入加油量"
              />
              <span class="fuel-amount-form__unit">L</span>
            </div>
          </el-form-item>
        </el-form>

        <div class="fuel-amount-dialog__preview">
          <span>按当前金额估算油价</span>
          <strong>{{ fuelAmountPreviewPrice }}</strong>
          <small>保存后列表会直接显示加油量，后续做油耗和报销核对会更直观。</small>
        </div>
      </div>

      <template #footer>
        <el-button @click="fuelAmountDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="fuelAmountSubmitting" @click="handleFuelAmountSubmit">
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  approveCashReport,
  deleteFuelRecord,
  getFuelRecords,
  getFuelStatistics,
  getVehicleYearlyFuelSummary,
  updateFuelAmount,
  updateReimbursementStatus
} from '@/api/fuelRecord'
import { getAllVehicles } from '@/api/vehicle'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()

const tableData = ref([])
const vehicles = ref([])
const budgetStats = ref(null)
const yearlyVehicleSummary = ref([])
const detailVisible = ref(false)
const currentRecord = ref(null)
const fuelAmountDialogVisible = ref(false)
const fuelAmountSubmitting = ref(false)
const createFuelSummary = () => ({
  total: 0,
  noApproval: 0,
  pendingApproval: 0,
  approved: 0,
  rejected: 0,
  unreimbursed: 0,
  reimbursed: 0
})
const fuelSummary = ref(createFuelSummary())
const filteredFuelSummary = ref(createFuelSummary())

const queryForm = reactive({
  vehicleId: null,
  year: new Date().getFullYear(),
  status: ''
})

const pagination = reactive({ current: 1, size: 10, total: 0 })

const selectedVehicleLabel = computed(() => {
  const matched = vehicles.value.find(item => item.id === queryForm.vehicleId)
  return matched ? matched.plateNumber : '全部车辆'
})

const selectedVehicleSummary = computed(() => {
  if (!queryForm.vehicleId) {
    return null
  }
  return yearlyVehicleSummary.value.find(item => item.vehicleId === queryForm.vehicleId) || null
})

const totalYearFuelCost = computed(() => (
  yearlyVehicleSummary.value.reduce((sum, item) => sum + Number(item.usedAmount || 0), 0)
))

const yearFuelCostLabel = computed(() => (
  queryForm.vehicleId ? `${selectedVehicleLabel.value}今年油费` : '今年油费'
))

const yearFuelCostValue = computed(() => {
  if (selectedVehicleSummary.value) {
    return Number(selectedVehicleSummary.value.usedAmount || 0)
  }
  return totalYearFuelCost.value
})

const selectedStatusLabel = computed(() => (
  queryForm.status ? getReportStatusName(queryForm.status) : '全部状态'
))

const yearlyInsightDescription = computed(() => {
  if (selectedVehicleSummary.value) {
    return `${selectedVehicleLabel.value} 在 ${queryForm.year} 年共有 ${selectedVehicleSummary.value.recordCount} 条加油记录，当前剩余预算 ¥${formatMoney(selectedVehicleSummary.value.remaining)}。`
  }
  return `当前展示的是 ${queryForm.year} 年全部车辆加油金额汇总，点击右侧车牌卡片可以切到单车视图。`
})

const fuelOverview = computed(() => ({
  total: Number(fuelSummary.value.total || 0),
  pageAmount: tableData.value.reduce((sum, item) => sum + Number(item.totalAmount || 0), 0),
  noApproval: Number(fuelSummary.value.noApproval || 0),
  pendingApproval: Number(fuelSummary.value.pendingApproval || 0),
  approved: Number(fuelSummary.value.approved || 0),
  unreimbursed: Number(fuelSummary.value.unreimbursed || 0),
  currentPendingApproval: Number(filteredFuelSummary.value.pendingApproval || 0),
  currentUnreimbursed: Number(filteredFuelSummary.value.unreimbursed || 0),
  notFull: tableData.value.filter(item => item.isFuelEnoughAfterFuel === 0).length
}))

const fuelSpotlight = computed(() => {
  if (selectedVehicleSummary.value) {
    return {
      title: '当前车辆年度油费',
      value: `¥${formatMoney(selectedVehicleSummary.value.usedAmount)}`,
      desc: `${selectedVehicleLabel.value} 在 ${queryForm.year} 年共有 ${selectedVehicleSummary.value.recordCount} 条记录，预算使用率 ${formatPercent(selectedVehicleSummary.value.usageRate)}%。`
    }
  }
  if (fuelOverview.value.currentPendingApproval > 0) {
    return {
      title: '现金审批待处理',
      value: fuelOverview.value.currentPendingApproval,
      desc: '当前筛选范围仍有记录需要人工审批，建议优先处理。'
    }
  }
  if (fuelOverview.value.currentUnreimbursed > 0) {
    return {
      title: '未报销记录待跟进',
      value: fuelOverview.value.currentUnreimbursed,
      desc: '可继续推进财务报销状态，避免账实脱节。'
    }
  }
  if (budgetStats.value) {
    return {
      title: '预算使用率',
      value: `${budgetStats.value.usageRate}%`,
      desc: `当前选中 ${selectedVehicleLabel.value}，年度剩余额度 ¥${formatMoney(budgetStats.value.remaining)}。`
    }
  }
  return {
    title: '本页加油金额',
    value: `¥${formatMoney(fuelOverview.value.pageAmount)}`,
    desc: '当前未限定具体车辆时，先看本页金额和报销进度。'
  }
})

const approveDialogVisible = ref(false)
const approveForm = reactive({ id: null, approved: true, comment: '' })
const fuelAmountForm = reactive({
  id: null,
  plateNumber: '',
  driverName: '',
  fuelDate: '',
  totalAmount: null,
  fuelAmount: null,
  originalFuelAmount: null
})

const canManageFuelAmount = computed(() => ['OFFICE_ADMIN', 'SUPER_ADMIN'].includes(userStore.userInfo.role))

const fuelAmountDialogTitle = computed(() => (
  hasFuelAmount({ fuelAmount: fuelAmountForm.originalFuelAmount }) ? '修改加油量' : '补录加油量'
))

const fuelAmountPreviewPrice = computed(() => {
  const totalAmount = Number(fuelAmountForm.totalAmount || 0)
  const fuelAmount = Number(fuelAmountForm.fuelAmount || 0)
  if (!totalAmount || !fuelAmount) {
    return '录入加油量后自动计算'
  }
  return `约 ¥${formatMoney(totalAmount / fuelAmount)}/L`
})

const buildAssetUrl = (path) => {
  if (!path) return ''
  if (/^https?:\/\//i.test(path)) {
    return path
  }
  return String(path).startsWith('/') ? path : `/${path}`
}

const getPhotoList = (photos) => {
  if (!photos) return []
  return String(photos).split(',').map(item => item.trim()).filter(Boolean).map(buildAssetUrl)
}

const hasFuelAmount = (record) => (
  !(record?.fuelAmount === null || record?.fuelAmount === undefined || record?.fuelAmount === '')
)

const canApprove = (row) => {
  if (!row) return false
  return (userStore.userInfo.role === 'OFFICE_ADMIN' || userStore.userInfo.role === 'SUPER_ADMIN')
    && row.isCash === 1
    && row.cashReportStatus === 'PENDING'
}

const canEditFuelAmount = (row) => Boolean(row) && canManageFuelAmount.value

const canToggleReimbursement = (row) => {
  if (!row) return false
  if (!['OFFICE_ADMIN', 'SUPER_ADMIN', 'FINANCE'].includes(userStore.userInfo.role)) {
    return false
  }
  if (row.isCash === 1) {
    return ['APPROVED', 'NONE', '', null, undefined].includes(row.cashReportStatus)
  }
  return true
}

const getReportStatusType = (status) => {
  const map = { NONE: 'info', PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger' }
  return map[status] || 'info'
}

const getReportStatusName = (status) => {
  const map = { NONE: '无需审批（历史）', PENDING: '待审批', APPROVED: '已批准', REJECTED: '已驳回' }
  return map[status] || status || '-'
}

const getReimbursementStatusType = (status) => {
  const map = { NONE: 'warning', UNREIMBURSED: 'warning', REIMBURSED: 'success' }
  return map[status] || 'warning'
}

const getReimbursementStatusName = (status) => {
  const map = { NONE: '未报销', UNREIMBURSED: '未报销', REIMBURSED: '已报销' }
  return map[status] || '未报销'
}

const formatDate = (date) => {
  if (!date) return ''
  return String(date).substring(0, 19).replace('T', ' ')
}

const formatMoney = (value) => {
  if (value === null || value === undefined || value === '') return '0.00'
  return Number(value).toFixed(2)
}

const formatFuelMeasure = (record) => {
  if (!hasFuelAmount(record)) {
    return '待管理员补录'
  }
  return `${record.fuelAmount} L`
}

const formatFuelUnitPrice = (record) => {
  if (!record) {
    return '未登记'
  }
  if (record.fuelPrice !== null && record.fuelPrice !== undefined && record.fuelPrice !== '') {
    return `¥${formatMoney(record.fuelPrice)}/L`
  }
  if (!hasFuelAmount(record) || record.totalAmount === null || record.totalAmount === undefined || record.totalAmount === '') {
    return '未登记'
  }
  return `约 ¥${formatMoney(Number(record.totalAmount) / Number(record.fuelAmount))}/L`
}

const formatPercent = (value) => {
  if (value === null || value === undefined || value === '') return '0.00'
  return Number(value).toFixed(2)
}

const fetchData = async () => {
  try {
    const res = await getFuelRecords({
      current: pagination.current,
      size: pagination.size,
      vehicleId: queryForm.vehicleId,
      year: queryForm.year,
      status: queryForm.status
    })
    tableData.value = res.data || []
    pagination.total = res.total || 0
    fuelSummary.value = { ...createFuelSummary(), ...(res.summary || {}) }
    filteredFuelSummary.value = { ...createFuelSummary(), ...(res.filteredSummary || res.summary || {}) }
  } catch (error) {
    tableData.value = []
    pagination.total = 0
    fuelSummary.value = createFuelSummary()
    filteredFuelSummary.value = createFuelSummary()
    ElMessage.error(error.response?.data?.message || '获取加油记录失败')
  }
}

const fetchVehicles = async () => {
  try {
    vehicles.value = await getAllVehicles()
  } catch (error) {
    ElMessage.error('获取车辆列表失败')
  }
}

const fetchYearlyVehicleSummary = async () => {
  try {
    const res = await getVehicleYearlyFuelSummary(queryForm.year)
    yearlyVehicleSummary.value = Array.isArray(res) ? res : []
  } catch (error) {
    yearlyVehicleSummary.value = []
    ElMessage.error(error.response?.data?.message || '获取车辆年度油费失败')
  }
}

const fetchBudgetStats = async () => {
  if (!queryForm.vehicleId) {
    budgetStats.value = null
    return
  }
  try {
    budgetStats.value = await getFuelStatistics(queryForm.vehicleId, queryForm.year)
  } catch (error) {
    budgetStats.value = null
    ElMessage.error(error.response?.data?.message || '获取预算统计失败')
  }
}

const applyVehicleFilter = async (vehicleId) => {
  queryForm.vehicleId = vehicleId
  await handleSearch()
}

const handleSearch = async () => {
  pagination.current = 1
  await Promise.all([fetchData(), fetchBudgetStats(), fetchYearlyVehicleSummary()])
}

const handleReset = async () => {
  queryForm.vehicleId = null
  queryForm.year = new Date().getFullYear()
  queryForm.status = ''
  await handleSearch()
}

const showDetail = (row) => {
  currentRecord.value = row
  detailVisible.value = true
}

const handleApproveCash = (row) => {
  currentRecord.value = row
  approveForm.id = row.id
  approveForm.approved = true
  approveForm.comment = ''
  approveDialogVisible.value = true
}

const resetFuelAmountForm = () => {
  fuelAmountForm.id = null
  fuelAmountForm.plateNumber = ''
  fuelAmountForm.driverName = ''
  fuelAmountForm.fuelDate = ''
  fuelAmountForm.totalAmount = null
  fuelAmountForm.fuelAmount = null
  fuelAmountForm.originalFuelAmount = null
  fuelAmountSubmitting.value = false
}

const handleEditFuelAmount = (row) => {
  fuelAmountForm.id = row.id
  fuelAmountForm.plateNumber = row.plateNumber || ''
  fuelAmountForm.driverName = row.driverName || ''
  fuelAmountForm.fuelDate = row.fuelDate || ''
  fuelAmountForm.totalAmount = row.totalAmount ?? null
  fuelAmountForm.originalFuelAmount = row.fuelAmount ?? null
  fuelAmountForm.fuelAmount = hasFuelAmount(row) ? Number(row.fuelAmount) : null
  fuelAmountDialogVisible.value = true
}

const handleApproveSubmit = async () => {
  try {
    await approveCashReport(approveForm.id, {
      approved: approveForm.approved,
      comment: approveForm.comment
    })
    ElMessage.success('审批成功')
    approveDialogVisible.value = false
    await Promise.all([fetchData(), fetchBudgetStats(), fetchYearlyVehicleSummary()])
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '审批失败')
  }
}

const handleFuelAmountSubmit = async () => {
  if (!fuelAmountForm.id) {
    ElMessage.error('未找到要更新的加油记录')
    return
  }
  if (fuelAmountForm.fuelAmount === null || fuelAmountForm.fuelAmount === undefined || Number(fuelAmountForm.fuelAmount) <= 0) {
    ElMessage.error('请输入大于 0 的加油量')
    return
  }

  fuelAmountSubmitting.value = true
  try {
    const updatedRecord = await updateFuelAmount(fuelAmountForm.id, {
      fuelAmount: fuelAmountForm.fuelAmount
    })
    if (currentRecord.value?.id === updatedRecord.id) {
      currentRecord.value = updatedRecord
    }
    ElMessage.success(hasFuelAmount({ fuelAmount: fuelAmountForm.originalFuelAmount }) ? '加油量更新成功' : '加油量补录成功')
    fuelAmountDialogVisible.value = false
    await Promise.all([fetchData(), fetchBudgetStats(), fetchYearlyVehicleSummary()])
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '保存加油量失败')
  } finally {
    fuelAmountSubmitting.value = false
  }
}

const handleToggleReimbursement = async (row) => {
  const reimbursed = row.reimbursementStatus !== 'REIMBURSED'
  const actionText = reimbursed ? '标记为已报销' : '标记为未报销'
  try {
    await ElMessageBox.confirm(`确定要${actionText}吗？`, '提示', { type: 'warning' })
    await updateReimbursementStatus(row.id, { reimbursed })
    ElMessage.success('报销状态更新成功')
    await Promise.all([fetchData(), fetchBudgetStats(), fetchYearlyVehicleSummary()])
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || '更新报销状态失败')
    }
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除这条加油记录吗？删除后可在回收站恢复', '提示', { type: 'warning' })
    await deleteFuelRecord(row.id)
    ElMessage.success('删除成功')
    await Promise.all([fetchData(), fetchBudgetStats(), fetchYearlyVehicleSummary()])
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || '删除失败')
    }
  }
}

onMounted(async () => {
  await Promise.all([fetchVehicles(), fetchData(), fetchYearlyVehicleSummary()])
})
</script>

<style scoped>
.fuel-records {
  padding: 4px 4px 18px;
}

.fuel-workbench {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(340px, 0.8fr);
  gap: 16px;
  margin-bottom: 18px;
  padding: 18px 20px;
  border-radius: 18px;
  background: #ffffff;
  border: 1px solid rgba(218, 225, 235, 0.92);
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.04);
}

.fuel-workbench__main {
  min-width: 0;
}

.fuel-workbench__label {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: #6c7a90;
}

.fuel-workbench__desc {
  margin-top: 8px;
  max-width: 760px;
  color: #66758d;
  font-size: 14px;
  line-height: 1.7;
}

.fuel-workbench__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 14px;
}

.hero-chip {
  display: inline-flex;
  align-items: center;
  padding: 8px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  color: #50617c;
  background: #f5f7fb;
  border: 1px solid rgba(214, 221, 233, 0.92);
}

.hero-chip--accent {
  background: rgba(43, 105, 217, 0.08);
  border-color: rgba(43, 105, 217, 0.14);
  color: #215fd9;
}

.hero-chip--warning {
  background: rgba(201, 123, 25, 0.08);
  border-color: rgba(201, 123, 25, 0.14);
  color: #925b16;
}

.hero-chip--success {
  background: rgba(22, 121, 95, 0.08);
  border-color: rgba(22, 121, 95, 0.14);
  color: #146b56;
}

.fuel-workbench__side {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.fuel-filter-toolbar {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr)) auto;
  gap: 12px;
  align-items: end;
}

.fuel-filter-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.fuel-filter-field span {
  color: #66758c;
  font-size: 12px;
  font-weight: 700;
}

.fuel-filter-field__control {
  width: 100%;
}

.fuel-filter-year {
  width: 100%;
}

.fuel-filter-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.fuel-filter-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.fuel-spotlight {
  padding: 16px 18px;
  border-radius: 16px;
  background: #f8fafc;
  border: 1px solid rgba(220, 227, 238, 0.92);
}

.fuel-spotlight__label {
  color: #6c7a90;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.fuel-spotlight__value {
  margin-top: 10px;
  color: #145bdf;
  font-size: 34px;
  font-weight: 800;
  line-height: 1;
}

.fuel-spotlight__desc {
  margin-top: 8px;
  color: #66758d;
  font-size: 13px;
  line-height: 1.6;
}

.fuel-spotlight__meta {
  margin-top: 10px;
  color: #41536f;
  font-size: 12px;
  font-weight: 700;
}

.fuel-filter-summary span {
  display: inline-flex;
  align-items: center;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(23, 37, 84, 0.05);
  color: #4e607d;
  font-size: 12px;
  font-weight: 700;
}

.fuel-budget-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin-top: 18px;
}

.fuel-budget-strip__item {
  padding: 14px 16px;
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(250, 252, 255, 0.98), rgba(243, 247, 252, 0.96));
  border: 1px solid rgba(224, 231, 241, 0.92);
}

.fuel-budget-strip__item span,
.fuel-budget-strip__item strong {
  display: block;
}

.fuel-budget-strip__item span {
  color: #708096;
  font-size: 12px;
}

.fuel-budget-strip__item strong {
  margin-top: 8px;
  color: #172033;
  font-size: 22px;
  font-weight: 800;
}

.year-fuel-panel {
  margin-top: 18px;
}

.year-fuel-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.year-fuel-card {
  border: 0;
  text-align: left;
  cursor: pointer;
  padding: 18px;
  border-radius: 20px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(246, 249, 252, 0.96));
  border: 1px solid rgba(224, 231, 241, 0.92);
  box-shadow: 0 14px 24px rgba(15, 23, 42, 0.05);
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease, background 0.2s ease;
}

.year-fuel-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 18px 32px rgba(15, 23, 42, 0.08);
}

.year-fuel-card--active {
  background: linear-gradient(135deg, rgba(55, 110, 220, 0.12), rgba(27, 188, 155, 0.1));
  border-color: rgba(67, 115, 236, 0.22);
}

.year-fuel-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.year-fuel-card__plate {
  display: block;
  color: #55637c;
  font-size: 13px;
  font-weight: 700;
}

.year-fuel-card__usage {
  color: #215fd9;
  font-size: 12px;
  font-weight: 800;
}

.year-fuel-card__amount {
  display: block;
  margin-top: 12px;
  color: #172033;
  font-size: 24px;
  font-weight: 800;
}

.year-fuel-card__meta {
  display: block;
  margin-top: 10px;
  color: #7a879a;
  font-size: 12px;
}

.panel-card {
  border-radius: 26px;
}

.panel-card :deep(.el-card__body) {
  padding: 22px;
}

.panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.panel-header__title {
  color: #182133;
  font-size: 18px;
  font-weight: 800;
}

.panel-header__desc {
  margin-top: 8px;
  color: #75839a;
  font-size: 13px;
  line-height: 1.7;
}

.panel-header__note {
  color: #7a879a;
  font-size: 12px;
  line-height: 1.6;
  text-align: right;
}

.quick-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}

.quick-filter {
  border: 1px solid rgba(214, 223, 236, 0.92);
  border-radius: 999px;
  padding: 10px 14px;
  text-align: left;
  background: #f8fafc;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}

.quick-filter--active {
  background: rgba(43, 105, 217, 0.08);
  border-color: rgba(67, 115, 236, 0.18);
}

.quick-filter span {
  color: #66758e;
  font-size: 13px;
}

.quick-filter strong {
  color: #172033;
  font-size: 14px;
  font-weight: 700;
}

.fuel-table {
  width: 100%;
}

.record-cell__title {
  color: #182133;
  font-size: 14px;
  font-weight: 700;
}

.record-cell__meta {
  margin-top: 6px;
  color: #55637c;
  font-size: 13px;
}

.record-cell__sub {
  margin-top: 6px;
  color: #7c8799;
  font-size: 12px;
}

.amount-cell__value {
  color: #182133;
  font-size: 15px;
  font-weight: 700;
}

.amount-cell__sub {
  margin-top: 6px;
  color: #708096;
  font-size: 12px;
}

.simple-stack {
  display: flex;
  flex-direction: column;
  gap: 6px;
  color: #243147;
}

.simple-stack small {
  color: #7b879a;
  font-size: 12px;
}

.fuel-amount-dialog {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.fuel-amount-dialog__summary {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.fuel-amount-dialog__card {
  padding: 14px 16px;
  border-radius: 16px;
  background: linear-gradient(180deg, #f9fbff 0%, #f3f6fb 100%);
  border: 1px solid rgba(214, 223, 236, 0.92);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.fuel-amount-dialog__card span {
  color: #6b7890;
  font-size: 12px;
  font-weight: 700;
}

.fuel-amount-dialog__card strong {
  color: #182133;
  font-size: 16px;
  font-weight: 800;
}

.fuel-amount-form {
  padding: 18px 18px 4px;
  border-radius: 18px;
  background: #fffdf8;
  border: 1px solid rgba(235, 220, 182, 0.78);
}

.fuel-amount-form__editor {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.fuel-amount-form__input {
  width: 220px;
}

.fuel-amount-form__unit {
  color: #5c6a82;
  font-size: 13px;
  font-weight: 700;
}

.fuel-amount-dialog__preview {
  padding: 14px 16px;
  border-radius: 16px;
  background: #f3f8ff;
  border: 1px solid rgba(170, 201, 248, 0.82);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.fuel-amount-dialog__preview span {
  color: #607089;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.06em;
}

.fuel-amount-dialog__preview strong {
  color: #145bdf;
  font-size: 22px;
  font-weight: 800;
}

.fuel-amount-dialog__preview small {
  color: #6b7890;
  font-size: 12px;
  line-height: 1.6;
}

.table-pagination {
  margin-top: 20px;
  justify-content: flex-end;
}

.photo-grid {
  min-height: 60px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.photo-item {
  width: 120px;
  height: 120px;
}

.empty-text {
  color: #999;
}

@media (max-width: 1440px) {
  .fuel-workbench,
  .fuel-budget-strip,
  .year-fuel-grid,
  .quick-filters {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 1100px) {
  .fuel-workbench,
  .fuel-filter-toolbar,
  .fuel-budget-strip,
  .year-fuel-grid,
  .quick-filters {
    grid-template-columns: 1fr;
  }

  .fuel-filter-actions {
    justify-content: stretch;
  }

  .fuel-filter-actions :deep(.el-button) {
    flex: 1;
  }

  .fuel-amount-dialog__summary {
    grid-template-columns: 1fr;
  }
}
</style>
