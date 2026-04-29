<template>
  <div class="maintenance-page page-shell">
    <section class="maintenance-toolbar">
      <div class="toolbar-filters">
        <el-select v-model="queryForm.status" placeholder="工单状态" clearable class="toolbar-select" @change="handleSearch">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-input
          v-model="queryForm.vehicleId"
          placeholder="车辆ID"
          class="toolbar-search toolbar-search--compact"
          clearable
          @keyup.enter="handleSearch"
        />
        <el-input
          v-model="queryForm.plateNumber"
          placeholder="车牌号"
          class="toolbar-search"
          clearable
          @keyup.enter="handleSearch"
        />
        <el-input
          v-model="queryForm.keyword"
          placeholder="问题描述 / 维修厂关键词"
          class="toolbar-search"
          clearable
          @keyup.enter="handleSearch"
        />
        <el-button @click="handleReset">重置</el-button>
        <el-button type="primary" @click="handleSearch">查询</el-button>
      </div>

      <div class="toolbar-actions">
        <el-tag v-if="highlightOrderId" type="warning" effect="dark">
          高亮工单 #{{ highlightOrderId }}
        </el-tag>
        <el-button v-if="canWrite" type="primary" @click="openCreateDialog">新建工单</el-button>
      </div>
    </section>

    <el-card class="panel-card">
      <template #header>
        <div class="panel-header">
          <div>
            <div class="panel-header__title">维修工单</div>
            <div class="panel-header__desc">统一追踪报修、审批、开工、完工和验收闭环。</div>
          </div>
        </div>
      </template>

      <el-table
        :data="tableData"
        border
        class="maintenance-table"
        v-loading="loading"
        :row-class-name="getRowClassName"
      >
        <el-table-column label="工单号" min-width="170">
          <template #default="{ row }">
            <div class="cell-title">{{ row.orderNo || '-' }}</div>
            <div class="cell-sub">{{ row.workType || '-' }} · {{ getSourceTypeLabel(row.sourceType) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="车辆" min-width="150">
          <template #default="{ row }">
            <div class="cell-title">{{ row.plateNumberDisplay || '-' }}</div>
            <div class="cell-sub">ID {{ row.vehicleId || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="130">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusName(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="问题 / 维修厂" min-width="260" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="cell-title">{{ row.issueDescription || '未填写问题描述' }}</div>
            <div class="cell-sub">{{ row.repairVendor || '维修厂待填写' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="时间轴" min-width="210">
          <template #default="{ row }">
            <div class="cell-title">{{ formatDate(row.reportedTime) }}</div>
            <div class="cell-sub">{{ formatTimeline(row) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="费用" width="150">
          <template #default="{ row }">
            <div class="cell-title">预估 ¥{{ formatMoney(row.estimatedCost) }}</div>
            <div class="cell-sub">实际 ¥{{ formatMoney(row.actualCost) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">查看</el-button>
            <el-button
              v-if="canAdvance(row.status)"
              link
              type="success"
              @click="openAdvanceDialog(row)"
            >
              处理
            </el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="当前没有匹配的工单" :image-size="90" />
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

    <el-drawer v-model="detailVisible" title="工单详情" size="760px">
      <template v-if="currentRecord">
        <div class="detail-section">
          <div class="detail-title">基本信息</div>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="工单号">{{ currentRecord.orderNo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="getStatusType(currentRecord.status)">{{ getStatusName(currentRecord.status) }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="车辆">{{ currentRecord.plateNumberDisplay || '-' }}</el-descriptions-item>
            <el-descriptions-item label="车辆ID">{{ currentRecord.vehicleId || '-' }}</el-descriptions-item>
            <el-descriptions-item label="来源">{{ getSourceTypeLabel(currentRecord.sourceType) }}</el-descriptions-item>
            <el-descriptions-item label="来源记录ID">{{ currentRecord.sourceRecordId || '-' }}</el-descriptions-item>
            <el-descriptions-item label="工单类型">{{ currentRecord.workType || '-' }}</el-descriptions-item>
            <el-descriptions-item label="上报时间">{{ formatDate(currentRecord.reportedTime) }}</el-descriptions-item>
            <el-descriptions-item label="问题描述" :span="2">{{ currentRecord.issueDescription || '-' }}</el-descriptions-item>
            <el-descriptions-item label="维修厂">{{ currentRecord.repairVendor || '-' }}</el-descriptions-item>
            <el-descriptions-item label="联系人">{{ currentRecord.repairContact || '-' }}</el-descriptions-item>
            <el-descriptions-item label="预估费用">¥{{ formatMoney(currentRecord.estimatedCost) }}</el-descriptions-item>
            <el-descriptions-item label="实际费用">¥{{ formatMoney(currentRecord.actualCost) }}</el-descriptions-item>
            <el-descriptions-item label="计划开始">{{ formatDate(currentRecord.plannedStartTime) }}</el-descriptions-item>
            <el-descriptions-item label="开工时间">{{ formatDate(currentRecord.repairStartTime) }}</el-descriptions-item>
            <el-descriptions-item label="完工时间">{{ formatDate(currentRecord.repairFinishTime) }}</el-descriptions-item>
            <el-descriptions-item label="验收时间">{{ formatDate(currentRecord.acceptedTime) }}</el-descriptions-item>
            <el-descriptions-item label="验收结果" :span="2">{{ currentRecord.acceptanceResult || '-' }}</el-descriptions-item>
            <el-descriptions-item label="备注" :span="2">{{ currentRecord.remark || '-' }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <div class="detail-actions">
          <el-button
            type="primary"
            @click="openAdvanceDialog(currentRecord)"
            v-if="canAdvance(currentRecord.status)"
          >
            处理工单
          </el-button>
          <el-button @click="detailVisible = false">关闭</el-button>
        </div>
      </template>
    </el-drawer>

    <el-dialog v-model="createDialogVisible" title="新建维修工单" width="560px">
      <el-form :model="createForm" label-width="120px">
        <el-form-item label="车辆ID">
          <el-input v-model="createForm.vehicleId" placeholder="例如：12" />
        </el-form-item>
        <el-form-item label="车牌号快照">
          <el-input v-model="createForm.plateNumber" placeholder="例如：陕A12345（可选）" />
        </el-form-item>
        <el-form-item label="工单类型">
          <el-select v-model="createForm.workType" style="width: 100%">
            <el-option label="维修" value="REPAIR" />
            <el-option label="保养" value="MAINTENANCE" />
            <el-option label="检测" value="CHECK" />
          </el-select>
        </el-form-item>
        <el-form-item label="问题描述">
          <el-input v-model="createForm.issueDescription" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="维修厂">
          <el-input v-model="createForm.repairVendor" />
        </el-form-item>
        <el-form-item label="维修联系人">
          <el-input v-model="createForm.repairContact" />
        </el-form-item>
        <el-form-item label="预估费用">
          <el-input v-model="createForm.estimatedCost" placeholder="例如：800.00" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleCreate">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="advanceDialogVisible" title="工单处理" width="560px">
      <el-form :model="advanceForm" label-width="120px">
        <el-form-item label="动作">
          <el-select v-model="advanceForm.action" placeholder="选择下一步" style="width: 100%">
            <el-option
              v-for="item in currentAdvanceActions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item v-if="showCommentInput" :label="commentLabel">
          <el-input v-model="advanceForm.comment" type="textarea" :rows="3" />
        </el-form-item>

        <template v-if="advanceForm.action === 'START'">
          <el-form-item label="维修厂">
            <el-input v-model="advanceForm.repairVendor" />
          </el-form-item>
          <el-form-item label="维修联系人">
            <el-input v-model="advanceForm.repairContact" />
          </el-form-item>
          <el-form-item label="开工时间">
            <el-date-picker
              v-model="advanceForm.startTime"
              type="datetime"
              value-format="YYYY-MM-DD HH:mm:ss"
              placeholder="可选，默认当前时间"
              style="width: 100%"
            />
          </el-form-item>
        </template>

        <template v-if="advanceForm.action === 'FINISH'">
          <el-form-item label="完工时间">
            <el-date-picker
              v-model="advanceForm.finishTime"
              type="datetime"
              value-format="YYYY-MM-DD HH:mm:ss"
              placeholder="可选，默认当前时间"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="实际费用">
            <el-input v-model="advanceForm.actualCost" placeholder="例如：1250.00" />
          </el-form-item>
        </template>

        <el-form-item v-if="advanceForm.action === 'ACCEPT'" label="验收结果">
          <el-input v-model="advanceForm.acceptanceResult" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="advanceDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleAdvanceSubmit">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import {
  acceptRepair,
  approveMaintenanceWorkOrder,
  cancelMaintenance,
  createMaintenanceWorkOrder,
  finishRepair,
  getMaintenanceWorkOrder,
  getMaintenanceWorkOrders,
  startRepair,
  submitMaintenanceWorkOrder
} from '@/api/maintenanceWorkOrder'

const route = useRoute()
const userStore = useUserStore()

const tableData = ref([])
const loading = ref(false)
const detailVisible = ref(false)
const createDialogVisible = ref(false)
const advanceDialogVisible = ref(false)
const submitLoading = ref(false)
const currentRecord = ref(null)
const highlightOrderId = ref(null)

const queryForm = reactive({
  status: '',
  vehicleId: '',
  plateNumber: '',
  keyword: ''
})

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const statusOptions = [
  { label: '全部状态', value: '' },
  { label: '草稿', value: 'DRAFT' },
  { label: '待审批', value: 'PENDING_APPROVAL' },
  { label: '已批准', value: 'APPROVED' },
  { label: '维修中', value: 'IN_REPAIR' },
  { label: '待验收', value: 'WAIT_ACCEPTANCE' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '已驳回', value: 'REJECTED' },
  { label: '已取消', value: 'CANCELED' }
]

const createForm = reactive({
  vehicleId: '',
  plateNumber: '',
  workType: 'REPAIR',
  issueDescription: '',
  repairVendor: '',
  repairContact: '',
  estimatedCost: ''
})

const advanceForm = reactive({
  action: '',
  comment: '',
  repairVendor: '',
  repairContact: '',
  startTime: '',
  finishTime: '',
  actualCost: '',
  acceptanceResult: ''
})

const normalizeText = (value) => {
  if (value === null || value === undefined) {
    return ''
  }
  return String(value).trim()
}

const parsePositiveInt = (value) => {
  const text = normalizeText(value)
  if (!text) {
    return undefined
  }
  const number = Number(text)
  if (!Number.isInteger(number) || number <= 0) {
    return undefined
  }
  return number
}

const parseOptionalAmount = (value) => {
  const text = normalizeText(value)
  if (!text) {
    return undefined
  }
  const number = Number(text)
  if (!Number.isFinite(number) || number < 0) {
    return undefined
  }
  return number
}

const normalizeOrder = (raw) => {
  if (!raw) {
    return raw
  }
  return {
    ...raw,
    plateNumberDisplay: raw.plateNumberSnapshot || raw.plateNumber || '-',
    issueDescription: raw.issueDescription || raw.faultDescription || '',
    repairVendor: raw.repairVendor || raw.repairFactory || '',
    repairStartTime: raw.repairStartTime || raw.sendRepairTime || null,
    repairFinishTime: raw.repairFinishTime || raw.finishTime || null,
    acceptedTime: raw.acceptedTime || raw.acceptanceTime || null
  }
}

const formatDate = (value) => {
  if (!value) return '-'
  return String(value).substring(0, 19).replace('T', ' ')
}

const formatMoney = (value) => {
  if (value === null || value === undefined || value === '') return '0.00'
  const number = Number(value)
  if (!Number.isFinite(number)) return '0.00'
  return number.toFixed(2)
}

const getStatusName = (status) => {
  const map = {
    DRAFT: '草稿',
    PENDING_APPROVAL: '待审批',
    APPROVED: '已批准',
    IN_REPAIR: '维修中',
    WAIT_ACCEPTANCE: '待验收',
    COMPLETED: '已完成',
    REJECTED: '已驳回',
    CANCELED: '已取消'
  }
  return map[status] || status || '-'
}

const getStatusType = (status) => {
  const map = {
    DRAFT: 'info',
    PENDING_APPROVAL: 'warning',
    APPROVED: 'success',
    IN_REPAIR: 'primary',
    WAIT_ACCEPTANCE: 'warning',
    COMPLETED: 'success',
    REJECTED: 'danger',
    CANCELED: 'info'
  }
  return map[status] || 'info'
}

const getSourceTypeLabel = (sourceType) => {
  const map = {
    MANUAL: '手动创建',
    BORROW_RECORD: '借还车异常'
  }
  return map[sourceType] || sourceType || '-'
}

const formatTimeline = (row) => {
  if (row.status === 'WAIT_ACCEPTANCE') {
    return `完工 ${formatDate(row.repairFinishTime)}，待验收`
  }
  if (row.status === 'IN_REPAIR') {
    return `已开工 ${formatDate(row.repairStartTime)}`
  }
  if (row.status === 'COMPLETED') {
    return `验收完成 ${formatDate(row.acceptedTime)}`
  }
  return row.remark || '等待处理'
}

const getAdvanceActions = (status) => {
  switch (status) {
    case 'DRAFT':
      return [
        { label: '提交审批', value: 'SUBMIT' },
        { label: '取消工单', value: 'CANCEL' }
      ]
    case 'PENDING_APPROVAL':
      return [
        { label: '审批通过', value: 'APPROVE' },
        { label: '审批驳回', value: 'REJECT' },
        { label: '取消工单', value: 'CANCEL' }
      ]
    case 'APPROVED':
      return [
        { label: '开始维修', value: 'START' },
        { label: '取消工单', value: 'CANCEL' }
      ]
    case 'IN_REPAIR':
      return [
        { label: '完工待验收', value: 'FINISH' },
        { label: '取消工单', value: 'CANCEL' }
      ]
    case 'WAIT_ACCEPTANCE':
      return [
        { label: '验收完成', value: 'ACCEPT' },
        { label: '取消工单', value: 'CANCEL' }
      ]
    default:
      return []
  }
}

const canWrite = computed(() => ['SUPER_ADMIN', 'OFFICE_ADMIN'].includes(userStore.userInfo.role))

const currentAdvanceActions = computed(() => {
  if (!canWrite.value) {
    return []
  }
  return getAdvanceActions(currentRecord.value?.status)
})

const canAdvance = (status) => canWrite.value && getAdvanceActions(status).length > 0

const showCommentInput = computed(() => {
  return ['SUBMIT', 'APPROVE', 'REJECT', 'CANCEL'].includes(advanceForm.action)
})

const commentLabel = computed(() => {
  if (advanceForm.action === 'APPROVE') return '审批意见'
  if (advanceForm.action === 'REJECT') return '驳回原因'
  if (advanceForm.action === 'CANCEL') return '取消原因'
  return '说明'
})

const getRowClassName = ({ row }) => {
  if (!highlightOrderId.value) {
    return ''
  }
  return Number(row.id) === Number(highlightOrderId.value) ? 'maintenance-table__row--highlight' : ''
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getMaintenanceWorkOrders({
      current: pagination.current,
      size: pagination.size,
      status: normalizeText(queryForm.status) || undefined,
      vehicleId: parsePositiveInt(queryForm.vehicleId),
      plateNumber: normalizeText(queryForm.plateNumber) || undefined,
      keyword: normalizeText(queryForm.keyword) || undefined
    })
    tableData.value = (res.data || []).map(item => normalizeOrder(item))
    pagination.total = Number(res.total || 0)
    pagination.current = Number(res.current || pagination.current)
    pagination.size = Number(res.size || pagination.size)
  } catch (error) {
    tableData.value = []
    pagination.total = 0
    ElMessage.error(error.response?.data?.message || error.message || '获取工单失败')
  } finally {
    loading.value = false
  }
}

const openDetail = async (row) => {
  try {
    const res = await getMaintenanceWorkOrder(row.id)
    currentRecord.value = normalizeOrder(res)
    detailVisible.value = true
  } catch (error) {
    ElMessage.error(error.response?.data?.message || error.message || '获取工单详情失败')
  }
}

const resetCreateForm = () => {
  createForm.vehicleId = ''
  createForm.plateNumber = ''
  createForm.workType = 'REPAIR'
  createForm.issueDescription = ''
  createForm.repairVendor = ''
  createForm.repairContact = ''
  createForm.estimatedCost = ''
}

const openCreateDialog = () => {
  resetCreateForm()
  createDialogVisible.value = true
}

const handleCreate = async () => {
  const vehicleId = parsePositiveInt(createForm.vehicleId)
  const plateNumber = normalizeText(createForm.plateNumber)
  if (!vehicleId && !plateNumber) {
    ElMessage.warning('请至少填写车辆ID或车牌号快照')
    return
  }

  submitLoading.value = true
  try {
    await createMaintenanceWorkOrder({
      vehicleId,
      plateNumber,
      workType: normalizeText(createForm.workType) || 'REPAIR',
      issueDescription: normalizeText(createForm.issueDescription) || undefined,
      faultDescription: normalizeText(createForm.issueDescription) || undefined,
      repairVendor: normalizeText(createForm.repairVendor) || undefined,
      repairFactory: normalizeText(createForm.repairVendor) || undefined,
      repairContact: normalizeText(createForm.repairContact) || undefined,
      estimatedCost: parseOptionalAmount(createForm.estimatedCost)
    })
    createDialogVisible.value = false
    ElMessage.success('工单创建成功')
    pagination.current = 1
    await fetchData()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || error.message || '创建失败')
  } finally {
    submitLoading.value = false
  }
}

const resetAdvanceForm = (row) => {
  advanceForm.action = ''
  advanceForm.comment = ''
  advanceForm.repairVendor = row?.repairVendor || ''
  advanceForm.repairContact = row?.repairContact || ''
  advanceForm.startTime = ''
  advanceForm.finishTime = ''
  advanceForm.actualCost = ''
  advanceForm.acceptanceResult = ''
}

const openAdvanceDialog = (row) => {
  if (!canAdvance(row?.status)) {
    return
  }
  currentRecord.value = normalizeOrder(row)
  resetAdvanceForm(currentRecord.value)
  advanceDialogVisible.value = true
}

const handleAdvanceSubmit = async () => {
  if (!advanceForm.action) {
    ElMessage.warning('请选择动作')
    return
  }
  const id = currentRecord.value?.id
  if (!id) {
    ElMessage.warning('未找到工单ID')
    return
  }

  submitLoading.value = true
  try {
    switch (advanceForm.action) {
      case 'SUBMIT':
        await submitMaintenanceWorkOrder(id, normalizeText(advanceForm.comment) || undefined)
        break
      case 'APPROVE':
        await approveMaintenanceWorkOrder(id, true, normalizeText(advanceForm.comment) || undefined)
        break
      case 'REJECT':
        await approveMaintenanceWorkOrder(id, false, normalizeText(advanceForm.comment) || undefined)
        break
      case 'START':
        await startRepair(id, {
          repairVendor: normalizeText(advanceForm.repairVendor) || undefined,
          repairContact: normalizeText(advanceForm.repairContact) || undefined,
          startTime: normalizeText(advanceForm.startTime) || undefined
        })
        break
      case 'FINISH':
        await finishRepair(id, {
          finishTime: normalizeText(advanceForm.finishTime) || undefined,
          actualCost: parseOptionalAmount(advanceForm.actualCost)
        })
        break
      case 'ACCEPT':
        await acceptRepair(id, {
          acceptanceResult: normalizeText(advanceForm.acceptanceResult) || undefined
        })
        break
      case 'CANCEL':
        await ElMessageBox.confirm('确定要取消该工单吗？', '取消确认', { type: 'warning' })
        await cancelMaintenance(id, normalizeText(advanceForm.comment) || '手动取消')
        break
      default:
        break
    }

    advanceDialogVisible.value = false
    ElMessage.success('操作成功')
    await fetchData()
    if (detailVisible.value && currentRecord.value?.id) {
      const latest = await getMaintenanceWorkOrder(currentRecord.value.id)
      currentRecord.value = normalizeOrder(latest)
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || error.message || '操作失败')
    }
  } finally {
    submitLoading.value = false
  }
}

const handleSearch = () => {
  pagination.current = 1
  fetchData()
}

const handleReset = () => {
  queryForm.status = ''
  queryForm.vehicleId = ''
  queryForm.plateNumber = ''
  queryForm.keyword = ''
  highlightOrderId.value = null
  pagination.current = 1
  fetchData()
}

const syncQueryToFilters = async (query) => {
  queryForm.status = normalizeText(query.status)
  queryForm.vehicleId = normalizeText(query.vehicleId)
  queryForm.plateNumber = normalizeText(query.plateNumber)
  queryForm.keyword = normalizeText(query.keyword)
  highlightOrderId.value = parsePositiveInt(query.highlightOrderId) || null
  pagination.current = 1
  await fetchData()
}

watch(
  () => route.query,
  (query) => {
    syncQueryToFilters(query)
  },
  { immediate: true }
)
</script>

<style scoped>
.maintenance-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.maintenance-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
}

.toolbar-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.toolbar-select {
  width: 180px;
}

.toolbar-search {
  width: 220px;
}

.toolbar-search--compact {
  width: 140px;
}

.panel-header__title {
  font-weight: 700;
}

.maintenance-table :deep(.el-table__cell) {
  vertical-align: middle;
}

.maintenance-table :deep(.maintenance-table__row--highlight > td.el-table__cell) {
  background: #fff7e6;
}

.cell-title {
  font-weight: 700;
  color: #1f2d3d;
}

.cell-sub {
  color: #7a8a9a;
  font-size: 12px;
}

.detail-section {
  margin-bottom: 16px;
}

.detail-title {
  font-weight: 700;
  margin-bottom: 8px;
}

.detail-actions {
  display: flex;
  gap: 10px;
  margin-top: 12px;
}

.page-shell {
  padding-bottom: 16px;
}

.table-pagination {
  margin-top: 12px;
  justify-content: flex-end;
}
</style>
