<template>
  <div class="drivers page-shell">
    <section class="drivers-summary">
      <div class="drivers-summary__label">当前概况</div>
      <div class="drivers-summary__desc">
        当前筛选结果共 {{ pagination.total }} 位驾驶人，当前页在职 {{ driverOverview.active }} 位，离职 {{ driverOverview.leaved }} 位，登录页按排序号从小到大显示。
      </div>
      <div class="drivers-summary__chips">
        <span class="drivers-chip">当前页 {{ tableData.length }}</span>
        <span class="drivers-chip drivers-chip--success">在职 {{ driverOverview.active }}</span>
        <span class="drivers-chip drivers-chip--neutral">离职 {{ driverOverview.leaved }}</span>
      </div>
    </section>

    <el-card class="panel-card">
      <template #header>
        <div class="card-header">
          <span>驾驶人管理</span>
          <div class="card-header__actions">
            <el-button type="success" @click="handleAddDept" v-if="canEdit">管理部门</el-button>
            <el-button type="primary" @click="handleAdd" v-if="canEdit">新增驾驶人</el-button>
          </div>
        </div>
      </template>

      <div class="toolbar-row">
        <div class="toolbar-row__filters">
          <el-input
            v-model="queryForm.driverName"
            class="toolbar-search"
            placeholder="输入姓名查询"
            clearable
            @keyup.enter="handleSearch"
          />
          <el-select v-model="queryForm.status" placeholder="全部状态" clearable class="toolbar-select">
            <el-option label="在职" value="ACTIVE" />
            <el-option label="离职" value="LEAVED" />
          </el-select>
        </div>
        <div class="toolbar-row__actions">
          <el-button @click="handleReset">重置</el-button>
          <el-button type="primary" @click="handleSearch">查询</el-button>
        </div>
      </div>

      <div class="quick-filters">
        <button
          type="button"
          class="quick-filter"
          :class="{ 'quick-filter--active': !queryForm.status }"
          @click="queryForm.status = ''; handleSearch()"
        >
          <span>全部驾驶人</span>
          <strong>{{ pagination.total }}</strong>
        </button>
        <button
          type="button"
          class="quick-filter"
          :class="{ 'quick-filter--active': queryForm.status === 'ACTIVE' }"
          @click="queryForm.status = 'ACTIVE'; handleSearch()"
        >
          <span>在职</span>
          <strong>{{ driverOverview.active }}</strong>
        </button>
        <button
          type="button"
          class="quick-filter"
          :class="{ 'quick-filter--active': queryForm.status === 'LEAVED' }"
          @click="queryForm.status = 'LEAVED'; handleSearch()"
        >
          <span>离职</span>
          <strong>{{ driverOverview.leaved }}</strong>
        </button>
      </div>

        <el-table :data="tableData" border class="drivers-table">
          <el-table-column prop="driverName" label="姓名" width="100" />
          <el-table-column label="登录排序" width="180">
            <template #default="{ row }">
              <div v-if="canEdit" class="sort-order-cell">
                <el-input-number
                  v-model="row.sortOrder"
                  :min="1"
                  :step="1"
                  :precision="0"
                  controls-position="right"
                  class="sort-order-input"
                />
                <el-button
                  link
                  type="primary"
                  :loading="sortSavingId === row.id"
                  @click="handleSortOrderSave(row)"
                >
                  保存
                </el-button>
              </div>
              <span v-else>{{ row.sortOrder || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="licenseNumber" label="驾驶证号" width="180" />
          <el-table-column prop="licenseType" label="准驾车型" width="100" />
        <el-table-column prop="driveAge" label="驾龄(年)" width="80" />
        <el-table-column prop="phone" label="联系电话" width="120" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">
              {{ row.status === 'ACTIVE' ? '在职' : '离职' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="180" fixed="right" v-if="canEdit">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button v-if="canDelete" link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="120px">
        <el-form-item label="关联部门" prop="deptId">
          <el-select v-model="form.deptId" placeholder="请选择部门">
            <el-option
              v-for="dept in deptList"
              :key="dept.id"
              :label="dept.deptName"
              :value="dept.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="姓名" prop="driverName">
          <el-input v-model="form.driverName" />
        </el-form-item>
        <el-form-item label="驾驶证号">
          <el-input v-model="form.licenseNumber" />
        </el-form-item>
        <el-form-item label="准驾车型" prop="licenseType">
          <el-select v-model="form.licenseType" placeholder="请选择">
            <el-option label="C1" value="C1" />
            <el-option label="C2" value="C2" />
            <el-option label="B1" value="B1" />
            <el-option label="B2" value="B2" />
            <el-option label="A1" value="A1" />
            <el-option label="A2" value="A2" />
          </el-select>
        </el-form-item>
        <el-form-item label="驾龄" prop="driveAge">
          <el-input-number v-model="form.driveAge" :min="0" :max="50" />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio label="ACTIVE">在职</el-radio>
            <el-radio label="LEAVED">离职</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="deptDialogVisible" title="管理部门" width="500px">
      <el-button type="primary" @click="showAddDeptForm" style="margin-bottom: 15px">新增部门</el-button>
      <el-table :data="deptList" border>
        <el-table-column prop="deptName" label="部门名称" />
        <el-table-column prop="deptCode" label="部门编码" />
        <el-table-column label="操作" width="100" v-if="canEdit">
          <template #default="{ row }">
            <el-button link type="danger" @click="handleDeleteDept(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-dialog v-model="addDeptDialogVisible" title="新增部门" width="400px" append-to-body>
        <el-form :model="deptForm" label-width="100px">
          <el-form-item label="部门名称">
            <el-input v-model="deptForm.deptName" placeholder="请输入部门名称" />
          </el-form-item>
          <el-form-item label="部门编码">
            <el-input v-model="deptForm.deptCode" placeholder="请输入部门编码" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="addDeptDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleAddDeptSubmit">确定</el-button>
        </template>
      </el-dialog>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getDrivers, createDriver, updateDriver, updateDriverSortOrder, deleteDriver } from '@/api/driver'
import { getDepts, createDept, deleteDept } from '@/api/dept'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const canEdit = computed(() => ['SUPER_ADMIN', 'OFFICE_ADMIN'].includes(userStore.userInfo.role))
const canDelete = computed(() => userStore.userInfo.role === 'SUPER_ADMIN')

const tableData = ref([])
const deptList = ref([])
const sortSavingId = ref(null)
const queryForm = reactive({
  driverName: '',
  status: ''
})

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const driverOverview = computed(() => ({
  active: tableData.value.filter(item => item.status === 'ACTIVE').length,
  leaved: tableData.value.filter(item => item.status === 'LEAVED').length
}))

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref()
const form = reactive({
  id: null,
  deptId: null,
  driverName: '',
  licenseNumber: '',
  licenseType: 'C1',
  driveAge: 0,
  phone: '',
  status: 'ACTIVE',
  sortOrder: null
})

const deptDialogVisible = ref(false)
const addDeptDialogVisible = ref(false)
const deptForm = reactive({
  deptName: '',
  deptCode: ''
})

const handleAddDept = () => {
  deptDialogVisible.value = true
}

const showAddDeptForm = () => {
  deptForm.deptName = ''
  deptForm.deptCode = ''
  addDeptDialogVisible.value = true
}

const handleAddDeptSubmit = async () => {
  if (!deptForm.deptName || !deptForm.deptCode) {
    ElMessage.warning('请填写部门名称和编码')
    return
  }
  try {
    await createDept(deptForm)
    ElMessage.success('添加成功')
    addDeptDialogVisible.value = false
    fetchDepts()
  } catch (error) {
    ElMessage.error('添加失败')
  }
}

const handleDeleteDept = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除该部门吗？', '提示', { type: 'warning' })
    await deleteDept(id)
    ElMessage.success('删除成功')
    fetchDepts()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || error.message || '删除失败')
    }
  }
}

const rules = {
  driverName: [{ required: true, message: '请输入姓名', trigger: 'blur' }]
}

const fetchData = async () => {
  try {
    const res = await getDrivers({
      current: pagination.current,
      size: pagination.size,
      ...queryForm
    })
    tableData.value = (res.data || []).map(item => ({
      ...item,
      sortOrder: item.sortOrder ?? item.id
    }))
    pagination.total = res.total
  } catch (error) {
    ElMessage.error('获取数据失败')
  }
}

const handleSearch = () => {
  pagination.current = 1
  fetchData()
}

const handleReset = () => {
  queryForm.driverName = ''
  queryForm.status = ''
  handleSearch()
}

const handleAdd = () => {
  dialogTitle.value = '新增驾驶人'
  Object.assign(form, {
    id: null,
    userId: null,
    deptId: null,
    driverName: '',
    licenseNumber: '',
    licenseType: 'C1',
    driveAge: 0,
    phone: '',
    status: 'ACTIVE',
    sortOrder: null
  })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑驾驶人'
  Object.assign(form, row)
  dialogVisible.value = true
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该驾驶人吗?', '提示', { type: 'warning' })
    await deleteDriver(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || error.message || '删除失败')
    }
  }
}

const handleSortOrderSave = async (row) => {
  const nextSortOrder = Number(row.sortOrder)
  if (!Number.isInteger(nextSortOrder) || nextSortOrder < 1) {
    ElMessage.warning('登录排序必须填写正整数')
    row.sortOrder = row.id
    return
  }

  sortSavingId.value = row.id
  try {
    await updateDriverSortOrder(row.id, { sortOrder: nextSortOrder })
    ElMessage.success('登录排序已更新')
    await fetchData()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '更新登录排序失败')
    await fetchData()
  } finally {
    sortSavingId.value = null
  }
}

const handleSubmit = async () => {
  await formRef.value.validate()
  try {
    if (form.id) {
      await updateDriver(form.id, form)
      ElMessage.success('更新成功')
    } else {
      await createDriver(form)
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || error.message || '操作失败')
  }
}

onMounted(() => {
  fetchData()
  fetchDepts()
})

const fetchDepts = async () => {
  try {
    const res = await getDepts()
    deptList.value = res || []
  } catch (error) {
    console.error('Failed to fetch depts:', error)
  }
}
</script>

<style scoped>
.drivers {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.drivers-summary {
  padding: 18px 20px;
  border-radius: 18px;
  background: #ffffff;
  border: 1px solid rgba(218, 225, 235, 0.92);
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.04);
}

.drivers-summary__label {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: #6c7a90;
}

.drivers-summary__desc {
  margin-top: 8px;
  color: #66758d;
  font-size: 14px;
  line-height: 1.7;
}

.drivers-summary__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 14px;
}

.drivers-chip {
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

.drivers-chip--success {
  background: rgba(22, 121, 95, 0.08);
  border-color: rgba(22, 121, 95, 0.14);
  color: #146b56;
}

.drivers-chip--neutral {
  background: rgba(100, 116, 139, 0.08);
  border-color: rgba(100, 116, 139, 0.14);
  color: #5b6b85;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header__actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.toolbar-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 20px;
}

.toolbar-row__filters {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  flex-wrap: wrap;
}

.toolbar-row__actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.toolbar-search {
  width: 240px;
}

.toolbar-select {
  width: 180px;
}

.quick-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 18px;
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

.sort-order-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.sort-order-input {
  width: 96px;
}

.drivers-table {
  width: 100%;
}

.table-pagination {
  margin-top: 20px;
  justify-content: flex-end;
}

@media (max-width: 960px) {
  .toolbar-row {
    flex-direction: column;
    align-items: stretch;
  }

  .toolbar-search,
  .toolbar-select {
    width: 100%;
  }
}
</style>
