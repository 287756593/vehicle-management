<template>
  <div class="borrow-page">
    <van-nav-bar :title="activeRecord ? '还车登记' : '借车登记'" left-arrow @click-left="router.back()" />

    <div v-if="activeRecord" class="content">
      <div class="page-hero page-hero--return">
        <div class="page-hero__eyebrow">还车流程</div>
        <div class="page-hero__title">归还 {{ activeRecord.plateNumber }}</div>
        <div class="page-hero__desc">填里程，上传停车照和仪表照后即可提交。</div>
        <div class="page-hero__meta">
          <span class="hero-pill">{{ formatDate(activeRecord.takeTime) }}</span>
          <span class="hero-pill">{{ activeRecord.takeMileage }} km</span>
          <span class="hero-pill hero-pill--warm">{{ activeRecord.destination || '本次行程' }}</span>
        </div>
      </div>

      <div class="task-tip-card">
        <div class="task-tip-card__title">还车前检查</div>
        <div class="task-tip-card__line">填里程并上传停车照、仪表照；有异常再补说明。</div>
      </div>

      <van-cell-group inset title="当前借用车辆">
        <van-field :model-value="activeRecord.plateNumber" label="车牌号" readonly />
        <van-field :model-value="activeRecord.destination || '-'" label="行程去向" readonly />
        <van-field :model-value="formatDate(activeRecord.takeTime)" label="取车时间" readonly />
        <van-field :model-value="activeRecord.takeMileage" label="取车里程" readonly />
        <van-field
          v-model="returnForm.returnMileage"
          required
          type="digit"
          label="还车总里程"
          placeholder="请输入里程表读数"
        />
      </van-cell-group>

      <section class="upload-block">
        <div class="upload-block__header">
          <span>停车照</span>
          <span class="required-badge">必传</span>
        </div>
        <van-cell-group inset>
          <van-cell>
            <van-uploader v-model="returnForm.vehiclePhotos" :before-read="handleBeforeRead" :max-count="3" accept="image/*" />
            <div class="upload-tip">拍到车辆整体外观，建议带上车头或车尾。</div>
            <div v-if="getPreviewList(returnForm.vehiclePhotos).length" class="preview-strip">
              <van-image
                v-for="(photo, index) in getPreviewList(returnForm.vehiclePhotos)"
                :key="`return-vehicle-${index}`"
                :src="photo"
                fit="cover"
                class="preview-thumb"
                @click="previewImages(returnForm.vehiclePhotos, index)"
              />
            </div>
          </van-cell>
        </van-cell-group>
      </section>

      <section class="upload-block">
        <div class="upload-block__header">
          <span>仪表照</span>
          <span class="required-badge">必传</span>
        </div>
        <van-cell-group inset>
          <van-cell>
            <van-uploader v-model="returnForm.mileagePhotos" :before-read="handleBeforeRead" :max-count="1" accept="image/*" />
            <div class="upload-tip">拍清仪表盘里程和油量，尽量避免反光。</div>
            <div v-if="getPreviewList(returnForm.mileagePhotos).length" class="preview-strip">
              <van-image
                v-for="(photo, index) in getPreviewList(returnForm.mileagePhotos)"
                :key="`return-mileage-${index}`"
                :src="photo"
                fit="cover"
                class="preview-thumb"
                @click="previewImages(returnForm.mileagePhotos, index)"
              />
            </div>
          </van-cell>
        </van-cell-group>
      </section>

      <van-cell-group inset style="margin-top: 15px;">
        <van-cell title="车辆整洁" required>
          <template #value>
            <van-radio-group v-model="returnForm.isClean" direction="horizontal">
              <van-radio :name="1">是</van-radio>
              <van-radio :name="0">否</van-radio>
            </van-radio-group>
          </template>
        </van-cell>
        <van-cell title="半箱以上" required>
          <template #value>
            <van-radio-group v-model="returnForm.isFuelEnough" direction="horizontal">
              <van-radio :name="1">是</van-radio>
              <van-radio :name="0">否</van-radio>
            </van-radio-group>
          </template>
        </van-cell>
      </van-cell-group>

      <van-cell-group inset style="margin-top: 15px;">
        <van-field
          v-model="returnForm.issueDescription"
          label="异常说明"
          type="textarea"
          autosize
          placeholder="有剐蹭、故障灯、异响时再填"
        />
      </van-cell-group>

      <section class="upload-block">
        <div class="upload-block__header">
          <span>异常照片</span>
          <span class="optional-badge">选传</span>
        </div>
        <van-cell-group inset>
          <van-cell>
            <van-uploader v-model="returnForm.issuePhotos" :before-read="handleBeforeRead" :max-count="3" accept="image/*" />
            <div class="upload-tip">有剐蹭或故障时再上传，尽量拍近一点。</div>
            <div v-if="getPreviewList(returnForm.issuePhotos).length" class="preview-strip">
              <van-image
                v-for="(photo, index) in getPreviewList(returnForm.issuePhotos)"
                :key="`return-issue-${index}`"
                :src="photo"
                fit="cover"
                class="preview-thumb"
                @click="previewImages(returnForm.issuePhotos, index)"
              />
            </div>
          </van-cell>
        </van-cell-group>
      </section>

      <div v-if="hasPendingFollowUp" class="notice-card">
        <div v-if="returnForm.isClean === 0">已标记为需要洗车。</div>
        <div v-if="returnForm.isFuelEnough === 0">已标记为需要加油。</div>
        <div v-if="hasIssueFilled">已标记为车辆异常待处理。</div>
      </div>

      <div class="submit-btn">
        <van-button type="primary" block :loading="loading" @click="handleReturn">
          确认还车
        </van-button>
      </div>
    </div>

    <div v-else class="content">
      <div class="page-hero page-hero--take">
        <div class="page-hero__eyebrow">借车流程</div>
        <div class="page-hero__title">直接借车</div>
        <div class="page-hero__desc">选车后填写里程和去向，再上传两张照片即可取车。</div>
        <div class="page-hero__meta">
          <span class="hero-pill">{{ availableVehicles.length }} 辆可借</span>
          <span class="hero-pill">{{ selectedVehicle?.plateNumber || '待选择车辆' }}</span>
          <span class="hero-pill hero-pill--teal">{{ selectedVehicle?.fuelReminderStatus === 'PENDING' ? '待补油提醒' : '借车前检查' }}</span>
        </div>
      </div>

      <div class="task-tip-card">
        <div class="task-tip-card__title">取车前检查</div>
        <div class="task-tip-card__line">选车，填里程和去向，再上传车辆照与里程照。</div>
      </div>

      <van-cell-group inset title="选择车辆">
        <van-field
          v-model="takeForm.vehiclePlate"
          required
          label="车牌号"
          placeholder="请选择要借用的车辆"
          is-link
          readonly
          @click="showVehiclePicker = true"
        />
        <van-field
          v-model="takeForm.initialMileage"
          required
          type="digit"
          label="出发里程"
          placeholder="请输入里程表读数"
        />
        <van-field
          v-model="takeForm.destination"
          required
          label="行程去向"
          placeholder="请输入去向"
        />
      </van-cell-group>

      <div v-if="selectedVehicle" class="notice-card notice-card--info">
        <div class="notice-card__title">已选车辆信息</div>
        <div>当前里程：{{ selectedVehicle.currentMileage || 0 }} km</div>
        <div v-if="selectedVehicle.fuelReminderStatus === 'PENDING'">
          待补油提醒：{{ selectedVehicle.fuelReminderNote || '上次还车油量不足半箱，请优先补油。' }}
        </div>
      </div>

      <section class="upload-block">
        <div class="upload-block__header">
          <span>车辆照</span>
          <span class="required-badge">必传</span>
        </div>
        <van-cell-group inset>
          <van-cell>
            <van-uploader v-model="takeForm.vehiclePhotos" :before-read="handleBeforeRead" :max-count="3" accept="image/*" />
            <div class="upload-tip">拍到出发前车辆整体外观，车身要完整。</div>
            <div v-if="getPreviewList(takeForm.vehiclePhotos).length" class="preview-strip">
              <van-image
                v-for="(photo, index) in getPreviewList(takeForm.vehiclePhotos)"
                :key="`take-vehicle-${index}`"
                :src="photo"
                fit="cover"
                class="preview-thumb"
                @click="previewImages(takeForm.vehiclePhotos, index)"
              />
            </div>
          </van-cell>
        </van-cell-group>
      </section>

      <section class="upload-block">
        <div class="upload-block__header">
          <span>里程照</span>
          <span class="required-badge">必传</span>
        </div>
        <van-cell-group inset>
          <van-cell>
            <van-uploader v-model="takeForm.mileagePhotos" :before-read="handleBeforeRead" :max-count="1" accept="image/*" />
            <div class="upload-tip">拍清出发前里程数字，确保读数可辨认。</div>
            <div v-if="getPreviewList(takeForm.mileagePhotos).length" class="preview-strip">
              <van-image
                v-for="(photo, index) in getPreviewList(takeForm.mileagePhotos)"
                :key="`take-mileage-${index}`"
                :src="photo"
                fit="cover"
                class="preview-thumb"
                @click="previewImages(takeForm.mileagePhotos, index)"
              />
            </div>
          </van-cell>
        </van-cell-group>
      </section>

      <div v-if="availableVehicles.length === 0" class="empty-wrap">
        <van-empty description="当前没有可直接借用的车辆" />
      </div>

      <div class="submit-btn">
        <van-button type="primary" block :loading="loading" :disabled="availableVehicles.length === 0" @click="handleTake">
          确认取车
        </van-button>
      </div>
    </div>

    <van-popup v-model:show="showVehiclePicker" position="bottom">
      <van-picker title="选择车辆" :columns="vehicleColumns" @confirm="onVehicleConfirm" @cancel="showVehiclePicker = false" />
    </van-popup>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showDialog, showImagePreview, showToast } from 'vant'
import { getAvailableVehicles, getCurrentBorrowRecord, returnVehicle, takeVehicle } from '@/api'
import { useDriverStore } from '@/store/driver'
import { compressVantFiles } from '@/utils/imageUpload'

const router = useRouter()
const driverStore = useDriverStore()

const loading = ref(false)
const showVehiclePicker = ref(false)
const availableVehicles = ref([])
const vehicleColumns = ref([])
const activeRecord = ref(null)
const selectedVehicleId = ref(null)
const selectedVehicle = computed(() => {
  return availableVehicles.value.find(vehicle => vehicle.id === selectedVehicleId.value) || null
})

const takeForm = reactive({
  vehiclePlate: '',
  initialMileage: '',
  destination: '',
  vehiclePhotos: [],
  mileagePhotos: []
})

const returnForm = reactive({
  returnMileage: '',
  vehiclePhotos: [],
  mileagePhotos: [],
  issueDescription: '',
  issuePhotos: [],
  isClean: 1,
  isFuelEnough: 1
})

const hasIssueFilled = computed(() => {
  return !!returnForm.issueDescription.trim() || returnForm.issuePhotos.length > 0
})

const hasPendingFollowUp = computed(() => {
  return returnForm.isClean === 0 || returnForm.isFuelEnough === 0 || hasIssueFilled.value
})

const formatDate = (date) => {
  if (!date) return ''
  return String(date).substring(0, 16).replace('T', ' ')
}

const getPreviewList = (files) => {
  if (!files || files.length === 0) {
    return []
  }
  return files.map(item => item.url || item.content).filter(Boolean)
}

const previewImages = (files, startPosition = 0) => {
  const images = getPreviewList(files)
  if (!images.length) {
    return
  }
  showImagePreview({ images, startPosition })
}

const handleBeforeRead = async (files) => {
  try {
    return await compressVantFiles(files)
  } catch (error) {
    showToast(error.message || '图片处理失败，请重试')
    return false
  }
}

const resetTakeForm = () => {
  takeForm.vehiclePlate = ''
  takeForm.initialMileage = ''
  takeForm.destination = ''
  takeForm.vehiclePhotos = []
  takeForm.mileagePhotos = []
  selectedVehicleId.value = null
}

const resetReturnForm = () => {
  returnForm.returnMileage = ''
  returnForm.vehiclePhotos = []
  returnForm.mileagePhotos = []
  returnForm.issueDescription = ''
  returnForm.issuePhotos = []
  returnForm.isClean = 1
  returnForm.isFuelEnough = 1
}

const loadData = async () => {
  const [vehicleRes, currentRes] = await Promise.allSettled([
    getAvailableVehicles(),
    getCurrentBorrowRecord()
  ])

  let hasFailedRequest = false

  if (vehicleRes.status === 'fulfilled') {
    availableVehicles.value = vehicleRes.value || []
    vehicleColumns.value = availableVehicles.value.map(vehicle => ({
      text: `${vehicle.plateNumber} (${vehicle.currentMileage || 0}km)${vehicle.fuelReminderStatus === 'PENDING' ? ' · 待补油' : ''}`,
      value: vehicle.id,
      plateNumber: vehicle.plateNumber,
      currentMileage: vehicle.currentMileage || 0,
      fuelReminderStatus: vehicle.fuelReminderStatus,
      fuelReminderNote: vehicle.fuelReminderNote
    }))
  } else {
    availableVehicles.value = []
    vehicleColumns.value = []
    hasFailedRequest = true
  }

  if (currentRes.status === 'fulfilled') {
    activeRecord.value = currentRes.value || null
    driverStore.setActiveBorrow(!!activeRecord.value)
  } else {
    activeRecord.value = null
    driverStore.setActiveBorrow(false)
    hasFailedRequest = true
  }

  if (activeRecord.value) {
    resetReturnForm()
  } else {
    resetTakeForm()
  }

  if (hasFailedRequest) {
    showToast('借还车页面部分数据加载失败')
  }
}

const onVehicleConfirm = ({ selectedOptions }) => {
  const selected = selectedOptions[0]
  if (!selected) return
  selectedVehicleId.value = selected.value
  takeForm.vehiclePlate = selected.plateNumber
  takeForm.initialMileage = String(selected.currentMileage || '')
  showVehiclePicker.value = false
}

const handleTake = async () => {
  if (!selectedVehicleId.value) {
    showToast('请选择车辆')
    return
  }
  if (!takeForm.initialMileage) {
    showToast('请输入当前里程')
    return
  }
  if (!takeForm.destination.trim()) {
    showToast('请填写目的地/去向')
    return
  }
  if (takeForm.vehiclePhotos.length === 0) {
    showToast('请上传出发前车辆照片')
    return
  }
  if (takeForm.mileagePhotos.length === 0) {
    showToast('请上传出发前公里数照片')
    return
  }

  loading.value = true
  try {
    const formData = new FormData()
    formData.append('vehicleId', selectedVehicleId.value)
    formData.append('initialMileage', takeForm.initialMileage)
    formData.append('destination', takeForm.destination.trim())
    takeForm.vehiclePhotos.forEach(item => {
      if (item?.file) formData.append('vehiclePhotos', item.file)
    })
    takeForm.mileagePhotos.forEach(item => {
      if (item?.file) formData.append('mileagePhotos', item.file)
    })
    await takeVehicle(formData)
    driverStore.setActiveBorrow(true)
    driverStore.triggerBorrowNotice()
    await loadData()
    router.replace('/home?borrowed=1')
  } catch (error) {
    showToast(error.response?.data?.message || '取车失败')
  } finally {
    loading.value = false
  }
}

const handleReturn = async () => {
  if (!activeRecord.value) {
    showToast('未找到当前借车记录')
    return
  }
  if (!returnForm.returnMileage) {
    showToast('请输入当前累计里程')
    return
  }
  if (Number(returnForm.returnMileage) < Number(activeRecord.value.takeMileage || 0)) {
    showToast(`还车里程不能小于取车里程 ${activeRecord.value.takeMileage}km`)
    return
  }
  if (returnForm.vehiclePhotos.length === 0) {
    showToast('请上传停车后的车辆照片')
    return
  }
  if (returnForm.mileagePhotos.length === 0) {
    showToast('请上传还车仪表照片')
    return
  }
  if (returnForm.issueDescription.trim() && returnForm.issuePhotos.length === 0) {
    showToast('请上传车辆异常照片')
    return
  }
  if (!returnForm.issueDescription.trim() && returnForm.issuePhotos.length > 0) {
    showToast('请填写车辆异常说明')
    return
  }

  loading.value = true
  let result = null
  try {
    const formData = new FormData()
    formData.append('recordId', activeRecord.value.id)
    formData.append('returnMileage', returnForm.returnMileage)
    formData.append('isClean', String(returnForm.isClean))
    formData.append('isFuelEnough', String(returnForm.isFuelEnough))
    if (returnForm.issueDescription.trim()) {
      formData.append('issueDescription', returnForm.issueDescription.trim())
    }
    returnForm.vehiclePhotos.forEach(item => {
      if (item?.file) formData.append('vehiclePhotos', item.file)
    })
    returnForm.mileagePhotos.forEach(item => {
      if (item?.file) formData.append('mileagePhotos', item.file)
    })
    returnForm.issuePhotos.forEach(item => {
      if (item?.file) formData.append('issuePhotos', item.file)
    })
    result = await returnVehicle(formData)
  } catch (error) {
    showToast(error.response?.data?.message || '还车失败')
    loading.value = false
    return
  }

  try {
    driverStore.setActiveBorrow(false)
    driverStore.clearBorrowNotice()
    const actionRequired = result.record?.actionRequired
    if (actionRequired) {
      await showDialog({
        title: '还车已记录',
        message: `${actionRequired}。管理员端会继续处理闭环。`
      })
    } else {
      showToast('还车成功')
    }
    await loadData()
    await router.replace('/home')
  } catch (error) {
    console.error('Return success, but post-submit UI update failed:', error)
    showToast('还车已登记，请返回首页查看状态')
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.borrow-page {
  min-height: 100dvh;
  background: transparent;
}

.borrow-page :deep(.van-nav-bar) {
  position: sticky;
  top: 0;
  z-index: 10;
}

.content {
  padding: 8px 0 calc(20px + var(--safe-area-bottom));
}

.page-hero {
  position: relative;
  overflow: hidden;
  margin: 0 16px 8px;
  padding: 12px 14px;
  border-radius: 14px;
  color: var(--text-main);
  border: 1px solid var(--panel-border);
  box-shadow: none;
}

.page-hero::after {
  display: none;
}

.page-hero--take {
  background: var(--blue-notice-bg);
  border-color: var(--blue-notice-border);
}

.page-hero--return {
  background: var(--warm-notice-bg);
  border-color: var(--warm-notice-border);
}

.page-hero__eyebrow {
  font-size: 10px;
  letter-spacing: 0.08em;
  color: var(--text-soft);
}

.page-hero__title {
  margin-top: 4px;
  font-size: 20px;
  font-weight: 800;
  line-height: 1.2;
}

.page-hero__desc {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.5;
  color: var(--text-soft-2);
}

.page-hero__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}

.hero-pill {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 9px;
  border-radius: 999px;
  background: var(--surface-muted-2);
  border: 1px solid var(--panel-border-strong);
  font-size: 11px;
  font-weight: 600;
  color: var(--text-soft-2);
}

.hero-pill--teal {
  background: var(--brand-teal-soft-bg);
  border-color: var(--brand-teal-soft-border);
  color: var(--brand-teal-soft-text);
}

.hero-pill--warm {
  background: var(--brand-warm-soft-bg);
  border-color: var(--brand-warm-soft-border);
  color: var(--brand-warm-soft-text);
}

.task-tip-card {
  margin: 0 16px 8px;
  padding: 10px 12px;
  border-radius: 12px;
  background: var(--panel-bg-soft);
  border: 1px dashed var(--panel-border-strong);
  box-shadow: none;
}

.task-tip-card__title {
  font-size: 12px;
  font-weight: 700;
  color: var(--text-strong);
  margin-bottom: 2px;
}

.task-tip-card__line {
  font-size: 11px;
  line-height: 1.45;
  color: var(--text-soft-2);
}

.borrow-page :deep(.van-cell-group) {
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid var(--panel-border-strong);
  box-shadow: none;
  background: var(--panel-bg-solid);
}

.borrow-page :deep(.van-cell) {
  padding: 11px 14px;
  background: transparent;
}

.borrow-page :deep(.van-cell::after) {
  left: 14px;
  right: 14px;
  border-color: var(--panel-divider);
}

.borrow-page :deep(.van-cell-group__title) {
  padding: 0 18px 8px;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.06em;
  color: var(--text-soft);
  text-transform: uppercase;
}

.borrow-page :deep(.van-field__label),
.borrow-page :deep(.van-cell__title) {
  flex: 0 0 84px;
  width: 84px;
  color: var(--text-soft-2);
  font-size: 12px;
  font-weight: 600;
  line-height: 1.35;
  white-space: nowrap;
}

.borrow-page :deep(.van-field__body) {
  align-items: center;
  gap: 8px;
}

.borrow-page :deep(.van-field__value),
.borrow-page :deep(.van-cell__value) {
  color: var(--text-main);
}

.borrow-page :deep(.van-field .van-field__value) {
  display: flex;
  align-items: stretch;
  min-height: 24px;
}

.borrow-page :deep(.van-field .van-field__control) {
  min-height: 24px;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--text-main);
  font-size: 14px;
}

.borrow-page :deep(.van-field textarea.van-field__control) {
  min-height: 78px;
  padding: 10px 12px;
  line-height: 1.6;
  border: 1px solid var(--panel-border);
  border-radius: 10px;
  background: var(--surface-muted);
}

.borrow-page :deep(.van-field--textarea .van-field__body) {
  align-items: flex-start;
}

.borrow-page :deep(.van-field__control::placeholder) {
  color: var(--text-muted-2);
}

.borrow-page :deep(.van-field--readonly .van-field__control) {
  color: var(--text-link);
  font-weight: 600;
}

.borrow-page :deep(.van-field__right-icon),
.borrow-page :deep(.van-cell__right-icon) {
  color: var(--text-muted);
}

.borrow-page :deep(.van-radio-group) {
  gap: 8px;
  justify-content: flex-end;
  flex-wrap: wrap;
}

.borrow-page :deep(.van-radio) {
  min-height: 30px;
  padding: 0 10px;
  border-radius: 999px;
  background: var(--surface-muted);
  border: 1px solid var(--panel-border);
}

.borrow-page :deep(.van-radio__label) {
  margin-left: 6px;
  font-size: 12px;
  white-space: nowrap;
}

.upload-block {
  margin-top: 10px;
}

.upload-block__header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 18px 8px;
  font-size: 13px;
  font-weight: 800;
  color: var(--text-strong);
}

.required-badge,
.optional-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 34px;
  height: 20px;
  padding: 0 7px;
  border-radius: 999px;
  font-size: 10px;
  font-weight: 600;
}

.required-badge {
  background: var(--danger-soft-bg);
  color: var(--danger-soft-text);
}

.optional-badge {
  background: var(--neutral-soft-bg);
  color: var(--neutral-soft-text);
}

.upload-tip {
  margin-top: 6px;
  font-size: 11px;
  line-height: 1.45;
  color: var(--text-subtle);
}

.preview-strip {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding: 8px 0 0;
}

.preview-thumb {
  flex: none;
  width: 68px;
  height: 68px;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: none;
  border: 1px solid var(--panel-border);
}

.upload-block :deep(.van-cell-group) {
  overflow: hidden;
  border-radius: 14px;
  background: var(--panel-bg-solid);
  border: 1px solid var(--panel-border);
  box-shadow: none;
}

.upload-block :deep(.van-cell) {
  background: transparent;
}

.upload-block :deep(.van-cell__value) {
  overflow: visible;
}

.upload-block :deep(.van-uploader) {
  width: 100%;
}

.upload-block :deep(.van-uploader__wrapper) {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.upload-block :deep(.van-uploader__preview),
.upload-block :deep(.van-uploader__upload) {
  margin: 0;
}

.upload-block :deep(.van-uploader__upload) {
  position: relative;
  width: 68px;
  height: 68px;
  border-radius: 12px;
  border: 1px dashed var(--uploader-border);
  background: var(--uploader-bg);
}

.upload-block :deep(.van-uploader__upload-icon) {
  font-size: 20px;
  color: var(--brand-blue);
}

.upload-block :deep(.van-uploader__upload)::after {
  content: none;
}

.upload-block :deep(.van-uploader__preview-image) {
  width: 68px;
  height: 68px;
  border-radius: 12px;
  overflow: hidden;
}

.upload-block :deep(.van-uploader__preview-delete) {
  top: 6px;
  right: 6px;
  background: var(--uploader-delete-bg);
}

.notice-card {
  margin: 10px 16px 0;
  padding: 10px 12px;
  border-radius: 12px;
  background: var(--warm-notice-bg);
  color: var(--warm-notice-text);
  line-height: 1.65;
  border: 1px solid var(--warm-notice-border);
  box-shadow: none;
}

.notice-card--info {
  background: var(--blue-notice-bg);
  color: var(--blue-notice-text);
  border-color: var(--blue-notice-border);
}

.notice-card__title {
  font-weight: 800;
  margin-bottom: 4px;
}

.empty-wrap {
  margin: 10px 16px 0;
  border-radius: 14px;
  overflow: hidden;
  background: var(--empty-card-bg);
  border: 1px solid var(--panel-border);
  box-shadow: none;
}

.submit-btn {
  position: sticky;
  bottom: 0;
  padding: 12px 16px calc(8px + var(--safe-area-bottom));
  background: var(--submit-fade);
}

.submit-btn :deep(.van-button) {
  height: 44px;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 800;
  box-shadow: none;
}

@media (max-width: 375px) {
  .page-hero {
    margin-left: 12px;
    margin-right: 12px;
    padding-inline: 14px;
  }

  .task-tip-card,
  .notice-card {
    margin-left: 12px;
    margin-right: 12px;
  }

  .upload-block__header {
    padding-left: 12px;
    padding-right: 12px;
  }

  .preview-thumb {
    width: 60px;
    height: 60px;
  }

  .submit-btn {
    padding-left: 12px;
    padding-right: 12px;
  }

  .upload-block :deep(.van-uploader__upload),
  .upload-block :deep(.van-uploader__preview-image) {
    width: 60px;
    height: 60px;
    border-radius: 12px;
  }
}
</style>
