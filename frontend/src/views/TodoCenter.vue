<template>
  <div class="todo-center page-shell">
    <section class="todo-hero">
      <div class="todo-hero__copy">
        <div class="todo-hero__label">统一待办中心</div>
        <div class="todo-hero__desc">
          把借还车、加油、车辆和维修事项放到同一张清单里，先看优先级，再跳回对应页面处理。
        </div>
        <div class="todo-hero__chips">
          <span class="hero-chip">总待办 {{ summary.total || 0 }}</span>
          <span class="hero-chip hero-chip--danger">高优先级 {{ summary.highPriority || 0 }}</span>
          <span class="hero-chip hero-chip--warning">中优先级 {{ summary.mediumPriority || 0 }}</span>
          <span class="hero-chip hero-chip--neutral">低优先级 {{ summary.lowPriority || 0 }}</span>
        </div>
      </div>

      <div class="todo-hero__focus">
        <div class="todo-hero__focus-label">{{ spotlight.title }}</div>
        <div class="todo-hero__focus-value">{{ spotlight.value }}</div>
        <div class="todo-hero__focus-desc">{{ spotlight.desc }}</div>
      </div>
    </section>

    <div class="todo-summary-grid">
      <button
        v-for="card in summaryCards"
        :key="card.type"
        type="button"
        class="todo-summary-card"
        :class="{ 'todo-summary-card--active': queryForm.type === card.type }"
        @click="applyTypeFilter(card.type)"
      >
        <span class="todo-summary-card__label">{{ card.label }}</span>
        <strong class="todo-summary-card__value">{{ card.count }}</strong>
      </button>
    </div>

    <el-card class="panel-card">
      <template #header>
        <div class="panel-header">
          <div>
            <div class="panel-header__title">待办清单</div>
            <div class="panel-header__desc">支持按类型、优先级和关键字筛选，点“去处理”直接跳回对应业务页面。</div>
          </div>
        </div>
      </template>

      <div class="toolbar-row">
        <div class="toolbar-row__filters">
          <el-input
            v-model="queryForm.keyword"
            class="toolbar-search"
            placeholder="输入车牌号或标题关键词"
            clearable
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>

          <el-select v-model="queryForm.type" clearable placeholder="待办类型" class="toolbar-select">
            <el-option
              v-for="item in typeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>

          <el-select v-model="queryForm.priority" clearable placeholder="优先级" class="toolbar-select">
            <el-option label="高优先级" value="HIGH" />
            <el-option label="中优先级" value="MEDIUM" />
            <el-option label="低优先级" value="LOW" />
          </el-select>
        </div>

        <div class="toolbar-row__actions">
          <el-button @click="handleReset">重置</el-button>
          <el-button type="primary" @click="handleSearch">查询</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="tableData" border class="todo-table">
        <el-table-column label="待办事项" min-width="320">
          <template #default="{ row }">
            <div class="todo-item">
              <div class="todo-item__title">{{ row.title }}</div>
              <div class="todo-item__desc">{{ row.description || '当前待办暂无补充说明' }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="类型" width="160">
          <template #default="{ row }">
            <el-tag :type="getTypeTagType(row.todoType)" effect="plain">
              {{ getTypeLabel(row.todoType) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="优先级" width="110">
          <template #default="{ row }">
            <el-tag :type="getPriorityTagType(row.priority)">
              {{ getPriorityLabel(row.priority) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="车牌号" width="130">
          <template #default="{ row }">
            {{ row.plateNumber || '-' }}
          </template>
        </el-table-column>

        <el-table-column label="截止/时间点" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.dueTime) || '-' }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="goToAction(row)">去处理</el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="当前筛选下没有待办事项" :image-size="90" />
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
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getTodoCenterItems, getTodoCenterSummary } from '@/api/todoCenter'

const router = useRouter()

const loading = ref(false)
const tableData = ref([])
const summary = reactive({
  total: 0,
  highPriority: 0,
  mediumPriority: 0,
  lowPriority: 0,
  typeCounts: {}
})

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const queryForm = reactive({
  type: '',
  priority: '',
  keyword: ''
})

const typeOptions = [
  { label: '借还车待闭环', value: 'BORROW_FOLLOW_UP' },
  { label: '超期未还', value: 'BORROW_OVERDUE' },
  { label: '加油待审批', value: 'FUEL_APPROVAL' },
  { label: '待补油', value: 'VEHICLE_FUEL_REMINDER' },
  { label: '保险临期', value: 'VEHICLE_INSURANCE_EXPIRING' },
  { label: '年检临期', value: 'VEHICLE_INSPECTION_EXPIRING' },
  { label: '限行已放行', value: 'VEHICLE_RESTRICTION_RELEASED' },
  { label: '维修待审批', value: 'MAINTENANCE_PENDING_APPROVAL' },
  { label: '维修待验收', value: 'MAINTENANCE_WAIT_ACCEPTANCE' },
  { label: '维修超期', value: 'MAINTENANCE_OVERDUE' }
]

const summaryCards = computed(() => ([
  { type: 'BORROW_FOLLOW_UP', label: '待闭环', count: Number(summary.typeCounts?.BORROW_FOLLOW_UP || 0) },
  { type: 'BORROW_OVERDUE', label: '超期未还', count: Number(summary.typeCounts?.BORROW_OVERDUE || 0) },
  { type: 'FUEL_APPROVAL', label: '待审批加油', count: Number(summary.typeCounts?.FUEL_APPROVAL || 0) },
  { type: 'VEHICLE_FUEL_REMINDER', label: '待补油', count: Number(summary.typeCounts?.VEHICLE_FUEL_REMINDER || 0) },
  { type: 'VEHICLE_INSURANCE_EXPIRING', label: '保险临期', count: Number(summary.typeCounts?.VEHICLE_INSURANCE_EXPIRING || 0) },
  { type: 'VEHICLE_INSPECTION_EXPIRING', label: '年检临期', count: Number(summary.typeCounts?.VEHICLE_INSPECTION_EXPIRING || 0) },
  { type: 'MAINTENANCE_PENDING_APPROVAL', label: '维修待审批', count: Number(summary.typeCounts?.MAINTENANCE_PENDING_APPROVAL || 0) },
  { type: 'MAINTENANCE_WAIT_ACCEPTANCE', label: '维修待验收', count: Number(summary.typeCounts?.MAINTENANCE_WAIT_ACCEPTANCE || 0) },
  { type: 'MAINTENANCE_OVERDUE', label: '维修超期', count: Number(summary.typeCounts?.MAINTENANCE_OVERDUE || 0) }
]))

const spotlight = computed(() => {
  if (summary.highPriority > 0) {
    return {
      title: '高优先级待办',
      value: summary.highPriority,
      desc: '优先处理超期未还、待闭环和维修待验收这些会影响车辆流转的事项。'
    }
  }
  return {
    title: '当前待办总量',
    value: summary.total,
    desc: '待办中心会把借还车、加油、车辆和维修事项聚合到同一张清单里。'
  }
})

const fetchSummary = async () => {
  try {
    const res = await getTodoCenterSummary()
    summary.total = Number(res.total || 0)
    summary.highPriority = Number(res.highPriority || 0)
    summary.mediumPriority = Number(res.mediumPriority || 0)
    summary.lowPriority = Number(res.lowPriority || 0)
    summary.typeCounts = res.typeCounts || {}
  } catch (error) {
    ElMessage.error(error.message || '获取待办汇总失败')
  }
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getTodoCenterItems({
      current: pagination.current,
      size: pagination.size,
      type: queryForm.type || undefined,
      priority: queryForm.priority || undefined,
      keyword: queryForm.keyword || undefined
    })
    tableData.value = res.data || []
    pagination.total = Number(res.total || 0)
  } catch (error) {
    tableData.value = []
    pagination.total = 0
    ElMessage.error(error.message || '获取待办列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.current = 1
  fetchData()
}

const handleReset = () => {
  queryForm.type = ''
  queryForm.priority = ''
  queryForm.keyword = ''
  pagination.current = 1
  fetchData()
}

const applyTypeFilter = (type) => {
  queryForm.type = queryForm.type === type ? '' : type
  pagination.current = 1
  fetchData()
}

const goToAction = (row) => {
  router.push({
    path: row.actionRoute || '/dashboard',
    query: row.actionQuery || {}
  })
}

const getTypeLabel = (type) => {
  return typeOptions.find(item => item.value === type)?.label || type || '-'
}

const getTypeTagType = (type) => {
  const map = {
    BORROW_FOLLOW_UP: 'warning',
    BORROW_OVERDUE: 'danger',
    FUEL_APPROVAL: 'warning',
    VEHICLE_FUEL_REMINDER: 'info',
    VEHICLE_INSURANCE_EXPIRING: 'danger',
    VEHICLE_INSPECTION_EXPIRING: 'warning',
    VEHICLE_RESTRICTION_RELEASED: 'success',
    MAINTENANCE_PENDING_APPROVAL: 'warning',
    MAINTENANCE_WAIT_ACCEPTANCE: 'danger',
    MAINTENANCE_OVERDUE: 'danger'
  }
  return map[type] || 'info'
}

const getPriorityLabel = (priority) => {
  const map = { HIGH: '高', MEDIUM: '中', LOW: '低' }
  return map[priority] || priority || '-'
}

const getPriorityTagType = (priority) => {
  const map = { HIGH: 'danger', MEDIUM: 'warning', LOW: 'info' }
  return map[priority] || 'info'
}

const formatDateTime = (value) => {
  if (!value) return ''
  return String(value).substring(0, 19).replace('T', ' ')
}

onMounted(async () => {
  await fetchSummary()
  await fetchData()
})
</script>

<style scoped>
.todo-center {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.todo-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(280px, 0.65fr);
  gap: 16px;
}

.todo-hero__copy,
.todo-hero__focus {
  border-radius: 18px;
  background: #ffffff;
  border: 1px solid rgba(218, 225, 235, 0.92);
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.04);
}

.todo-hero__copy {
  padding: 18px 20px;
}

.todo-hero__focus {
  padding: 18px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.todo-hero__label,
.todo-hero__focus-label {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: #6b7a90;
}

.todo-hero__desc,
.todo-hero__focus-desc {
  margin-top: 10px;
  color: #617089;
  font-size: 14px;
  line-height: 1.7;
}

.todo-hero__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 14px;
}

.todo-hero__focus-value {
  margin-top: 10px;
  font-size: 36px;
  font-weight: 800;
  color: #182335;
}

.todo-summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.todo-summary-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 14px 16px;
  border: 1px solid rgba(218, 225, 235, 0.92);
  border-radius: 16px;
  background: #ffffff;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.2s ease, transform 0.2s ease, box-shadow 0.2s ease;
}

.todo-summary-card:hover,
.todo-summary-card--active {
  border-color: rgba(37, 99, 235, 0.42);
  box-shadow: 0 12px 26px rgba(37, 99, 235, 0.10);
  transform: translateY(-1px);
}

.todo-summary-card__label {
  font-size: 13px;
  font-weight: 700;
  color: #5f6f86;
}

.todo-summary-card__value {
  font-size: 28px;
  font-weight: 800;
  color: #182335;
}

.todo-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.todo-item__title {
  font-size: 14px;
  font-weight: 700;
  color: #182335;
}

.todo-item__desc {
  font-size: 12px;
  line-height: 1.6;
  color: #6d7b90;
}

.todo-table :deep(.el-table__cell) {
  vertical-align: middle;
}

@media (max-width: 1100px) {
  .todo-hero,
  .todo-summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
