<template>
  <el-dialog v-model="visible" title="管理员补登记" width="700px">
    <el-alert
      type="info"
      :closable="false"
      style="margin-bottom: 16px"
      title="适用于驾驶员借车时忘记登记的场景。管理员补登记不要求上传任何照片，保存后车辆会进入使用中。"
    />

    <el-form :model="form" label-width="110px">
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="车辆" required>
            <el-select
              v-model="form.vehicleId"
              placeholder="请选择车辆"
              filterable
              style="width: 100%"
            >
              <el-option
                v-for="item in vehicleOptions"
                :key="item.id"
                :label="formatVehicleLabel(item)"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="驾驶员" required>
            <el-select
              v-model="form.driverId"
              placeholder="请选择驾驶员"
              filterable
              style="width: 100%"
            >
              <el-option
                v-for="item in driverOptions"
                :key="item.id"
                :label="item.driverName"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="用车事由">
        <el-input v-model="form.usageReason" placeholder="如无可不填" />
      </el-form-item>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="目的地/去向" required>
            <el-input v-model="form.destination" placeholder="请输入目的地/去向" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="取车里程(km)" required>
            <el-input-number
              v-model="form.takeMileage"
              :min="0"
              :precision="2"
              :step="1"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="取车时间" required>
            <el-date-picker
              v-model="form.takeTime"
              type="datetime"
              value-format="YYYY-MM-DD HH:mm:ss"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="预计还车">
            <el-date-picker
              v-model="form.expectedReturnTime"
              type="datetime"
              value-format="YYYY-MM-DD HH:mm:ss"
              style="width: 100%"
              clearable
            />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="loading" @click="handleCreate">保存补登记</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { createAdminSupplementBorrowRecord } from '@/api/vehicleBorrow'
import { getActiveDrivers } from '@/api/driver'
import { getAllVehicles } from '@/api/vehicle'

const emit = defineEmits(['created'])

const visible = ref(false)
const loading = ref(false)
const driverOptions = ref([])
const vehicleOptions = ref([])

const form = reactive({
  vehicleId: null,
  driverId: null,
  usageReason: '',
  destination: '',
  takeTime: '',
  expectedReturnTime: '',
  takeMileage: null
})

const padNumber = (value) => String(value).padStart(2, '0')

const formatDateTimeValue = (date) => {
  return [
    date.getFullYear(),
    padNumber(date.getMonth() + 1),
    padNumber(date.getDate())
  ].join('-') + ` ${padNumber(date.getHours())}:${padNumber(date.getMinutes())}:${padNumber(date.getSeconds())}`
}

const parseDateTimeValue = (value) => {
  if (!value) return null
  const parsed = new Date(String(value).replace(' ', 'T'))
  return Number.isNaN(parsed.getTime()) ? null : parsed
}

const formatVehicleLabel = (vehicle) => {
  if (!vehicle) return ''
  const parts = [
    vehicle.plateNumber,
    vehicle.brand,
    vehicle.model,
    vehicle.currentMileage === null || vehicle.currentMileage === undefined ? null : `${vehicle.currentMileage} km`
  ].filter(Boolean)
  return parts.join(' ｜ ')
}

const resetForm = () => {
  form.vehicleId = null
  form.driverId = null
  form.usageReason = ''
  form.destination = ''
  form.takeTime = formatDateTimeValue(new Date())
  form.expectedReturnTime = ''
  form.takeMileage = null
}

const open = async () => {
  try {
    const [drivers, vehicles] = await Promise.all([
      getActiveDrivers(),
      getAllVehicles()
    ])
    driverOptions.value = (Array.isArray(drivers) ? drivers : [])
      .slice()
      .sort((a, b) => String(a.driverName || '').localeCompare(String(b.driverName || ''), 'zh-CN'))
    vehicleOptions.value = (Array.isArray(vehicles) ? vehicles : [])
      .filter(item => item.status === 'NORMAL')
      .sort((a, b) => String(a.plateNumber || '').localeCompare(String(b.plateNumber || ''), 'zh-CN'))
    resetForm()
    visible.value = true
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '加载补登记选项失败')
  }
}

const handleCreate = async () => {
  if (!form.vehicleId) { ElMessage.warning('请选择车辆'); return }
  if (!form.driverId) { ElMessage.warning('请选择驾驶员'); return }
  if (!form.destination.trim()) { ElMessage.warning('请填写目的地/去向'); return }
  if (!form.takeTime) { ElMessage.warning('请选择取车时间'); return }
  const takeTimeValue = parseDateTimeValue(form.takeTime)
  if (!takeTimeValue) { ElMessage.warning('取车时间格式不正确'); return }
  if (takeTimeValue.getTime() > Date.now()) { ElMessage.warning('取车时间不能晚于当前时间'); return }
  if (form.expectedReturnTime) {
    const expectedReturnTimeValue = parseDateTimeValue(form.expectedReturnTime)
    if (!expectedReturnTimeValue) { ElMessage.warning('预计还车时间格式不正确'); return }
    if (expectedReturnTimeValue.getTime() <= takeTimeValue.getTime()) {
      ElMessage.warning('预计还车时间必须晚于取车时间'); return
    }
  }
  if (form.takeMileage === null || form.takeMileage === undefined) {
    ElMessage.warning('请填写取车里程'); return
  }

  loading.value = true
  try {
    const record = await createAdminSupplementBorrowRecord({
      vehicleId: form.vehicleId,
      driverId: form.driverId,
      usageReason: form.usageReason.trim(),
      destination: form.destination.trim(),
      takeTime: form.takeTime,
      expectedReturnTime: form.expectedReturnTime || null,
      takeMileage: form.takeMileage
    })
    visible.value = false
    resetForm()
    ElMessage.success('补登记成功，车辆已同步为使用中')
    emit('created', record)
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '补登记失败')
  } finally {
    loading.value = false
  }
}

defineExpose({ open })
</script>
