<template>
  <el-dialog v-model="visible" title="完成闭环处理" width="520px">
    <el-form :model="form" label-width="110px">
      <el-alert
        v-if="currentRecord?.issueDescription"
        type="info"
        :closable="false"
        style="margin-bottom: 16px"
        title="当前记录包含车辆异常。若需要转入维修，可直接创建维修工单并同步完成闭环。"
      />
      <el-form-item label="处理后状态">
        <el-select v-model="form.nextVehicleStatus" style="width: 100%">
          <el-option label="恢复正常" value="NORMAL" />
          <el-option label="转入维修" value="MAINTENANCE" />
        </el-select>
      </el-form-item>
      <el-form-item label="处理说明">
        <el-input
          v-model="form.remark"
          type="textarea"
          :rows="4"
          placeholder="可填写已洗车、已加油、异常已排查等说明"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button
        v-if="currentRecord?.issueDescription && form.nextVehicleStatus === 'MAINTENANCE'"
        type="success"
        plain
        :loading="maintenanceCreating"
        @click="handleCreateMaintenanceAndClose"
      >
        建维修工单并闭环
      </el-button>
      <el-button type="primary" :loading="loading" @click="handleComplete">确定完成</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { completeBorrowFollowUp } from '@/api/vehicleBorrow'
import { createMaintenanceFromBorrow } from '@/api/maintenanceWorkOrder'

const emit = defineEmits(['completed'])

const visible = ref(false)
const loading = ref(false)
const maintenanceCreating = ref(false)
const targetId = ref(null)
const currentRecord = ref(null)

const form = reactive({
  nextVehicleStatus: 'NORMAL',
  remark: ''
})

const normalizeText = (value) => {
  if (value === null || value === undefined) return ''
  return String(value).trim()
}

const buildRemark = (remark, order) => {
  const r = normalizeText(remark)
  const no = normalizeText(order?.orderNo)
  if (!no) return r
  return r ? `${r}；已创建维修工单 ${no}` : `已创建维修工单 ${no}`
}

const open = (row) => {
  targetId.value = row.id
  currentRecord.value = row
  form.nextVehicleStatus = row.issueDescription ? 'MAINTENANCE' : 'NORMAL'
  form.remark = ''
  visible.value = true
}

const handleComplete = async () => {
  if (!targetId.value) { ElMessage.error('未找到需要闭环的记录'); return }
  loading.value = true
  try {
    const record = await completeBorrowFollowUp(targetId.value, {
      nextVehicleStatus: form.nextVehicleStatus,
      remark: form.remark.trim()
    })
    visible.value = false
    ElMessage.success('闭环处理完成')
    emit('completed', record)
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '闭环处理失败')
  } finally {
    loading.value = false
  }
}

const handleCreateMaintenanceAndClose = async () => {
  if (!targetId.value) { ElMessage.error('未找到需要处理的闭环记录'); return }
  maintenanceCreating.value = true
  try {
    const order = await createMaintenanceFromBorrow(targetId.value)
    const record = await completeBorrowFollowUp(targetId.value, {
      nextVehicleStatus: 'MAINTENANCE',
      remark: buildRemark(form.remark, order)
    })
    form.nextVehicleStatus = 'MAINTENANCE'
    visible.value = false
    ElMessage.success(order?.orderNo ? `已创建维修工单 ${order.orderNo} 并完成闭环` : '已创建维修工单并完成闭环')
    emit('completed', record)
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '创建维修工单失败')
  } finally {
    maintenanceCreating.value = false
  }
}

defineExpose({ open })
</script>
