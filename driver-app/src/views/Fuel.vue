<template>
  <div class="fuel-page">
    <van-nav-bar title="加油登记" left-arrow @click-left="router.back()" />

    <div class="form-container">
      <div class="task-tip-card">
        <div class="task-tip-card__title">登记前提醒</div>
        <div class="task-tip-card__line">请准备加油凭证，按实际加油情况登记即可。</div>
      </div>

      <div class="page-hero page-hero--cash">
        <div class="page-hero__eyebrow">加油流程</div>
        <div class="page-hero__title">加油登记</div>
        <div class="page-hero__desc">{{ borrowContextDescription }}</div>
        <div class="page-hero__meta">
          <span class="hero-pill">{{ selectedVehicle?.plateNumber || '待选择记录' }}</span>
          <span class="hero-pill">{{ selectedBorrowRecord?.status === 'RETURNED' ? '补登记' : (selectedBorrowRecord ? '当前借用' : '待选择') }}</span>
          <span class="hero-pill hero-pill--soft">¥{{ form.totalAmount || '0.00' }}</span>
        </div>
      </div>

      <div v-if="selectedVehicle" class="vehicle-info-card">
        <div class="vehicle-info-card__row">
          <span class="vehicle-info-card__label">当前车辆</span>
          <span class="vehicle-info-card__value">{{ selectedVehicle.plateNumber }}</span>
        </div>
        <div class="vehicle-info-card__row">
          <span class="vehicle-info-card__label">车辆型号</span>
          <span class="vehicle-info-card__value">{{ selectedVehicle.model || '未登记' }}</span>
        </div>
        <div v-if="selectedBorrowRecord" class="vehicle-info-card__row">
          <span class="vehicle-info-card__label">借车记录</span>
          <span class="vehicle-info-card__value">{{ selectedBorrowRecordLabel }}</span>
        </div>
        <div v-if="selectedBorrowRecord" class="vehicle-info-card__row">
          <span class="vehicle-info-card__label">借车时间</span>
          <span class="vehicle-info-card__value">{{ formatBorrowWindow(selectedBorrowRecord) }}</span>
        </div>
        <div v-if="selectedVehicle.fuelReminderStatus === 'PENDING'" class="vehicle-info-card__warning">
          {{ selectedVehicle.fuelReminderNote || '该车上次还车时油量不足半箱，请本次加油后尽量补至半箱以上。' }}
        </div>
      </div>

      <van-cell-group inset>
        <van-field
          v-model="form.borrowRecordLabel"
          required
          is-link
          readonly
          label="借车记录"
          placeholder="请选择当前借车或近一周已归还记录"
          @click="handleBorrowRecordFieldClick"
        />
        <van-field
          v-model="form.fuelType"
          required
          label="油品"
          placeholder="如92#、95#"
        />
        <van-field
          v-model="form.totalAmount"
          required
          type="number"
          label="加油金额"
          placeholder="请输入本次加油金额"
        />
        <van-field
          v-model="form.fuelMileage"
          required
          type="number"
          label="加油里程"
          placeholder="请输入加油时公里数"
        />
        <van-field
          v-model="form.fuelDate"
          required
          type="datetime-local"
          label="加油时间"
          placeholder="请选择时间"
        />
      </van-cell-group>

      <div class="hint-card hint-card--orange">
        <div>补登记时仅显示近一周借车记录，且加油时间需落在本次借车期间内。</div>
        <div>提交时需附：加油凭证。</div>
      </div>

      <section class="upload-block">
        <div class="upload-block__header">
          <span>凭证照</span>
          <span class="required-badge">必传</span>
        </div>
        <van-cell-group inset>
          <van-cell>
            <van-uploader v-model="form.fuelPhotos" :before-read="handleBeforeRead" :max-count="2" accept="image/*" />
            <div class="upload-tip">上传小票或支付凭证，金额和时间要能看清。</div>
            <div v-if="getPreviewList(form.fuelPhotos).length" class="preview-strip">
              <van-image
                v-for="(photo, index) in getPreviewList(form.fuelPhotos)"
                :key="`fuel-photo-${index}`"
                :src="photo"
                fit="cover"
                class="preview-thumb"
                @click="previewImages(form.fuelPhotos, index)"
              />
            </div>
          </van-cell>
        </van-cell-group>
      </section>


      <div class="submit-btn">
        <van-button type="primary" block :loading="loading" :disabled="!hasAvailableBorrowContext" @click="handleSubmit">
          提交登记
        </van-button>
      </div>
    </div>

    <van-popup v-model:show="showBorrowRecordPicker" position="bottom">
      <van-picker
        title="选择借车记录"
        :columns="borrowRecordOptions"
        @confirm="onBorrowRecordConfirm"
        @cancel="showBorrowRecordPicker = false"
      />
    </van-popup>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showImagePreview, showToast } from 'vant'
import { useDriverStore } from '@/store/driver'
import { createFuelRecord, getMyBorrowRecords } from '@/api'
import { compressVantFiles } from '@/utils/imageUpload'

const router = useRouter()
const driverStore = useDriverStore()

const showBorrowRecordPicker = ref(false)
const loading = ref(false)
const borrowRecordOptions = ref([])

const getTodayDate = (date = new Date()) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day}T${hour}:${minute}`
}

const parseDateValue = (value) => {
  if (!value) {
    return null
  }
  const normalized = String(value).trim().replace(' ', 'T')
  const date = new Date(normalized)
  return Number.isNaN(date.getTime()) ? null : date
}

const formatDate = (value) => {
  const date = parseDateValue(value)
  if (!date) {
    return '—'
  }
  return getTodayDate(date).replace('T', ' ')
}

const formatBorrowWindow = (record) => {
  if (!record) {
    return '—'
  }
  return `${formatDate(record.takeTime)} 至 ${record.returnTime ? formatDate(record.returnTime) : '当前未还车'}`
}

const buildBorrowRecordLabel = (record) => {
  if (!record) {
    return ''
  }
  return `${record.plateNumber || '未登记车牌'} · ${record.status === 'RETURNED' ? '已还车' : '当前借用中'}`
}

const HISTORY_SELECTABLE_DAYS = 7
const HISTORY_SELECTABLE_MS = HISTORY_SELECTABLE_DAYS * 24 * 60 * 60 * 1000

const buildBorrowRecordOption = (record) => {
  const statusText = record.status === 'RETURNED'
    ? `已还车 ${formatDate(record.returnTime)}`
    : '当前借用中'
  return {
    ...record,
    text: `${record.plateNumber || '未登记车牌'} · ${statusText}`,
    value: record.id
  }
}

const isSelectableBorrowRecord = (record) => {
  if (!record || !['TAKEN', 'RETURNED'].includes(record.status)) {
    return false
  }
  if (record.status === 'TAKEN') {
    return true
  }
  const referenceTime = parseDateValue(record.returnTime || record.takeTime)
  const now = parseDateValue(getTodayDate())
  if (!referenceTime || !now) {
    return false
  }
  const diffMs = now.getTime() - referenceTime.getTime()
  return diffMs >= 0 && diffMs <= HISTORY_SELECTABLE_MS
}

const form = reactive({
  borrowRecordId: null,
  borrowRecordLabel: '',
  vehicleId: null,
  vehicleName: '',
  vehicleModel: '',
  fuelType: '92#',
  totalAmount: '',
  fuelMileage: '',
  fuelDate: getTodayDate(),
  isCash: 1,
  isFuelEnoughAfterFuel: 1,
  fuelPhotos: [],
})

const hasAvailableBorrowContext = computed(() => borrowRecordOptions.value.length > 0)

const selectedBorrowRecord = computed(() => {
  return borrowRecordOptions.value.find(item => item.id === form.borrowRecordId) || null
})

const selectedVehicle = computed(() => {
  const record = selectedBorrowRecord.value
  if (!record) {
    return null
  }
  return {
    id: record.vehicleId,
    plateNumber: record.plateNumber,
    model: record.vehicleModel || '',
    fuelReminderStatus: 'NONE',
    fuelReminderNote: ''
  }
})

const selectedBorrowRecordLabel = computed(() => {
  return selectedBorrowRecord.value ? buildBorrowRecordLabel(selectedBorrowRecord.value) : '未选择'
})

const borrowContextDescription = computed(() => {
  const record = selectedBorrowRecord.value
  if (!record) {
    return '当前没有可用于登记的借车记录。'
  }
  if (record.status === 'RETURNED') {
    return `${record.plateNumber} 已归还，可补登记本次借车期间的加油记录，请填写实际加油时间。`
  }
  return `${record.plateNumber} 当前借用中，可直接填写金额与照片提交登记。`
})

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

const appendFiles = (formData, field, files) => {
  files.forEach(item => {
    if (item?.file) {
      formData.append(field, item.file)
    }
  })
}

const updateFuelType = (vehicleModel) => {
  const model = String(vehicleModel || '').toLowerCase()
  if (
    model.includes('帕萨特') || model.includes('passat') ||
    model.includes('奥迪') || model.includes('a4') || model.includes('a6') ||
    model.includes('奔驰') || model.includes('宝马') || model.includes('bmw')
  ) {
    form.fuelType = '95#'
  } else {
    form.fuelType = '92#'
  }
}

const resolveDefaultMileage = (record) => {
  if (!record) {
    return ''
  }
  const fallbackMileage = record.returnMileage ?? record.takeMileage
  return fallbackMileage === null || fallbackMileage === undefined ? '' : String(fallbackMileage)
}

const pickSuggestedFuelDate = (record) => {
  if (!record) {
    return getTodayDate()
  }
  const returnTime = parseDateValue(record.returnTime)
  if (returnTime) {
    return getTodayDate(returnTime)
  }
  const takeTime = parseDateValue(record.takeTime)
  if (takeTime) {
    const now = parseDateValue(getTodayDate())
    if (now && now.getTime() < takeTime.getTime()) {
      return getTodayDate(takeTime)
    }
  }
  return getTodayDate()
}

const normalizeFuelDateForBorrowRecord = (currentValue, record) => {
  const currentDate = parseDateValue(currentValue)
  const takeTime = parseDateValue(record?.takeTime)
  const returnTime = parseDateValue(record?.returnTime)
  if (!currentDate) {
    return pickSuggestedFuelDate(record)
  }
  if (takeTime && currentDate.getTime() < takeTime.getTime()) {
    return getTodayDate(takeTime)
  }
  if (returnTime && currentDate.getTime() > returnTime.getTime()) {
    return getTodayDate(returnTime)
  }
  return getTodayDate(currentDate)
}

const applySelectedBorrowRecord = (record) => {
  if (!record) {
    return
  }
  const vehicleModel = record.vehicleModel || ''
  form.borrowRecordId = record.id
  form.borrowRecordLabel = buildBorrowRecordLabel(record)
  form.vehicleId = record.vehicleId
  form.vehicleName = record.plateNumber || ''
  form.vehicleModel = vehicleModel
  form.fuelMileage = resolveDefaultMileage(record)
  form.fuelDate = normalizeFuelDateForBorrowRecord(form.fuelDate, record)
  updateFuelType(vehicleModel)
}

const handleBorrowRecordFieldClick = () => {
  if (!hasAvailableBorrowContext.value) {
    showToast('当前没有可登记的借车记录')
    return
  }
  showBorrowRecordPicker.value = true
}

const validateFuelDateWithinBorrowPeriod = () => {
  const record = selectedBorrowRecord.value
  if (!record) {
    showToast('请选择借车记录')
    return false
  }
  const fuelDate = parseDateValue(form.fuelDate)
  const takeTime = parseDateValue(record.takeTime)
  const returnTime = parseDateValue(record.returnTime)
  if (!fuelDate) {
    showToast('请选择加油时间')
    return false
  }
  if (takeTime && fuelDate.getTime() < takeTime.getTime()) {
    showToast('加油时间不能早于本次借车时间')
    return false
  }
  if (returnTime && fuelDate.getTime() > returnTime.getTime()) {
    showToast('补登记请填写本次借车期间的实际加油时间')
    return false
  }
  return true
}

onMounted(async () => {
  try {
    const myBorrowRecordsRes = await getMyBorrowRecords({ current: 1, size: 50 })

    const recordData = myBorrowRecordsRes.data || []
    borrowRecordOptions.value = recordData
      .filter(isSelectableBorrowRecord)
      .map(buildBorrowRecordOption)

    if (borrowRecordOptions.value.length > 0) {
      applySelectedBorrowRecord(borrowRecordOptions.value[0])
    } else {
      form.borrowRecordId = null
      form.borrowRecordLabel = ''
      form.vehicleId = null
      form.vehicleName = ''
      form.vehicleModel = ''
      form.fuelMileage = ''
      showToast('当前没有可登记的借车记录')
    }
  } catch (error) {
    console.error('Failed to load fuel form context:', error)
    showToast('加油页面初始化失败')
  }
})


const onBorrowRecordConfirm = ({ selectedOptions }) => {
  const selected = selectedOptions[0]
  const record = borrowRecordOptions.value.find(item => item.id === selected?.value)
  applySelectedBorrowRecord(record)
  showBorrowRecordPicker.value = false
}

const handleSubmit = async () => {
  if (!hasAvailableBorrowContext.value) {
    showToast('当前没有可登记的借车记录')
    return
  }
  if (!selectedBorrowRecord.value || !form.vehicleId) {
    showToast('请选择借车记录')
    return
  }
  if (!form.totalAmount || Number(form.totalAmount) <= 0) {
    showToast('请填写加油金额')
    return
  }
  if (!form.fuelMileage) {
    showToast('请填写加油时公里数')
    return
  }
  if (!form.fuelDate) {
    showToast('请选择加油时间')
    return
  }
  if (!validateFuelDateWithinBorrowPeriod()) {
    return
  }
  if (form.fuelPhotos.length === 0) {
    showToast('请上传加油凭证照片')
    return
  }

  loading.value = true
  try {
    const formData = new FormData()
    formData.append('vehicleId', String(form.vehicleId))
    formData.append('driverId', String(driverStore.driverInfo.id))
    formData.append('fuelType', form.fuelType)
    formData.append('totalAmount', String(form.totalAmount || 0))
    formData.append('fuelMileage', String(form.fuelMileage))
    formData.append('fuelDate', form.fuelDate)
    formData.append('isCash', '1')
    formData.append('isFuelEnoughAfterFuel', String(form.isFuelEnoughAfterFuel))

    appendFiles(formData, 'fuelPhotos', form.fuelPhotos)

    await createFuelRecord(formData)
    showToast('加油记录已提交')
    router.replace('/home')
  } catch (error) {
    console.error('Submit error:', error)
    showToast(error.response?.data?.message || error.message || '提交失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.fuel-page {
  min-height: 100dvh;
  background: transparent;
  padding-bottom: calc(72px + var(--safe-area-bottom));
}

.form-container {
  padding: 8px 14px 0;
}

.task-tip-card {
  padding: 10px 12px;
  border-radius: 12px;
  background: var(--panel-bg-soft);
  border: 1px solid var(--brand-teal-soft-border);
  box-shadow: none;
  margin-bottom: 8px;
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

.page-hero {
  position: relative;
  overflow: hidden;
  margin-bottom: 8px;
  padding: 12px 14px;
  border-radius: 14px;
  color: var(--text-main);
  border: 1px solid var(--panel-border);
  box-shadow: none;
}

.page-hero::after {
  display: none;
}

.page-hero--card {
  background: var(--blue-notice-bg);
  border-color: var(--blue-notice-border);
}

.page-hero--cash {
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

.hero-pill--soft {
  background: var(--brand-blue-soft-bg);
  border-color: var(--brand-blue-soft-border);
  color: var(--brand-blue-soft-text);
}

.vehicle-info-card {
  margin-bottom: 8px;
  padding: 10px 12px;
  border-radius: 12px;
  background: var(--blue-notice-bg);
  border: 1px solid var(--blue-notice-border);
  box-shadow: none;
}

.vehicle-info-card__row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  font-size: 12px;
  line-height: 1.65;
}

.vehicle-info-card__label {
  color: var(--text-soft);
}

.vehicle-info-card__value {
  color: var(--text-strong);
  font-weight: 600;
  text-align: right;
}

.vehicle-info-card__warning {
  margin-top: 8px;
  padding: 8px 10px;
  border-radius: 10px;
  background: var(--brand-warm-soft-bg);
  color: var(--brand-warm-soft-text);
  font-size: 11px;
  line-height: 1.45;
}

.fuel-page :deep(.van-cell-group) {
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid var(--panel-border-strong);
  box-shadow: none;
  background: var(--panel-bg-solid);
}

.fuel-page :deep(.van-cell) {
  padding: 11px 14px;
  background: transparent;
}

.fuel-page :deep(.van-cell::after) {
  left: 14px;
  right: 14px;
  border-color: var(--panel-divider);
}

.fuel-page :deep(.van-field__label),
.fuel-page :deep(.van-cell__title) {
  flex: 0 0 84px;
  width: 84px;
  color: var(--text-soft-2);
  font-size: 12px;
  font-weight: 600;
  line-height: 1.35;
  white-space: nowrap;
}

.fuel-page :deep(.van-field__body) {
  align-items: center;
  gap: 8px;
}

.fuel-page :deep(.van-field__value),
.fuel-page :deep(.van-cell__value) {
  color: var(--text-main);
}

.fuel-page :deep(.van-field .van-field__value) {
  display: flex;
  align-items: stretch;
  min-height: 24px;
}

.fuel-page :deep(.van-field .van-field__control) {
  min-height: 24px;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--text-main);
  font-size: 14px;
}

.fuel-page :deep(.van-field textarea.van-field__control) {
  min-height: 78px;
  padding: 10px 12px;
  line-height: 1.6;
  border: 1px solid var(--panel-border);
  border-radius: 10px;
  background: var(--surface-muted);
}

.fuel-page :deep(.van-field--textarea .van-field__body) {
  align-items: flex-start;
}

.fuel-page :deep(.van-field__control::placeholder) {
  color: var(--text-muted-2);
}

.fuel-page :deep(.van-field--readonly .van-field__control) {
  color: var(--text-link);
  font-weight: 600;
}

.fuel-page :deep(.van-field__right-icon),
.fuel-page :deep(.van-cell__right-icon) {
  color: var(--text-muted);
}

.fuel-page :deep(.van-radio-group) {
  gap: 8px;
  justify-content: flex-end;
  flex-wrap: wrap;
}

.fuel-page :deep(.van-radio) {
  min-height: 30px;
  padding: 0 10px;
  border-radius: 999px;
  background: var(--surface-muted);
  border: 1px solid var(--panel-border);
}

.fuel-page :deep(.van-radio__label) {
  margin-left: 6px;
  font-size: 12px;
  white-space: nowrap;
}

.hint-card {
  margin: 8px 2px 0;
  padding: 10px 12px;
  border-radius: 12px;
  font-size: 11px;
  line-height: 1.5;
  box-shadow: none;
}

.hint-card--green {
  background: var(--hint-green-bg);
  color: var(--hint-green-text);
  border: 1px solid var(--hint-green-border);
}

.hint-card--orange {
  background: var(--hint-orange-bg);
  color: var(--hint-orange-text);
  border: 1px solid var(--hint-orange-border);
}

.upload-block {
  margin-top: 10px;
}

.upload-block__header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 6px 8px;
  font-size: 13px;
  font-weight: 800;
  color: var(--text-strong);
}

.required-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 34px;
  height: 20px;
  padding: 0 7px;
  border-radius: 999px;
  font-size: 10px;
  font-weight: 600;
  background: var(--danger-soft-bg);
  color: var(--danger-soft-text);
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

.submit-btn {
  padding: 12px 2px calc(10px + var(--safe-area-bottom));
}

.submit-btn :deep(.van-button) {
  height: 44px;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 800;
  box-shadow: none;
}

@media (max-width: 375px) {
  .preview-thumb {
    width: 60px;
    height: 60px;
  }

  .upload-block :deep(.van-uploader__upload),
  .upload-block :deep(.van-uploader__preview-image) {
    width: 60px;
    height: 60px;
    border-radius: 12px;
  }
}
</style>
