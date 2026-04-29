<template>
  <div class="records-page">
    <van-nav-bar title="用车记录" left-arrow @click-left="router.back()" />

    <div class="records-body">
      <van-list
        v-if="!initialLoaded || records.length > 0"
        v-model:loading="loading"
        :finished="finished"
        finished-text="没有更多了"
        @load="loadMore"
      >
        <van-cell-group inset class="records-group">
          <van-cell
            v-for="item in records"
            :key="item.id"
            :title="`${item.plateNumber || '未知车牌'} · ${item.recordNo || ''}`"
            :label="formatRecordLabel(item)"
            is-link
            class="record-cell"
            @click="openRecord(item)"
          >
            <template #icon>
              <span class="status-badge" :class="`status-badge--${getStatusVariant(item.status)}`">
                <span class="status-badge__dot"></span>
                <span>{{ getStatusText(item.status) }}</span>
              </span>
            </template>
          </van-cell>
        </van-cell-group>
      </van-list>

      <div v-else-if="initialLoaded" class="empty-wrap">
        <van-empty description="暂无用车记录" />
      </div>
    </div>

    <van-popup v-model:show="detailVisible" position="bottom" round class="record-popup">
      <div v-if="selectedRecord" class="record-popup__content">
        <div class="record-popup__header">
          <div>
            <div class="record-popup__title">{{ selectedRecord.plateNumber }}</div>
            <div class="record-popup__subtitle">{{ selectedRecord.recordNo }}</div>
          </div>
          <span class="status-badge status-badge--popup" :class="`status-badge--${getStatusVariant(selectedRecord.status)}`">
            <span class="status-badge__dot"></span>
            <span>{{ getStatusText(selectedRecord.status) }}</span>
          </span>
        </div>

        <van-cell-group inset class="record-popup__group">
          <van-cell title="车牌号" :value="selectedRecord.plateNumber || '-'" />
          <van-cell title="取车时间" :value="formatDate(selectedRecord.takeTime) || '-'" />
          <van-cell title="还车时间" :value="formatDate(selectedRecord.returnTime) || '-'" />
          <van-cell title="目的地/去向" :value="selectedRecord.destination || '-'" />
          <van-cell title="取车里程" :value="formatMileage(selectedRecord.takeMileage)" />
          <van-cell title="还车里程" :value="formatMileage(selectedRecord.returnMileage)" />
          <van-cell title="本次行驶" :value="formatMileage(selectedRecord.tripMileage)" />
          <van-cell title="闭环状态">
            <template #value>
              <van-tag :type="getFollowUpType(selectedRecord.followUpStatus)" size="small">
                {{ getFollowUpText(selectedRecord.followUpStatus) }}
              </van-tag>
            </template>
          </van-cell>
          <van-cell title="需处理事项">
            <template #value>
              <span class="record-popup__value record-popup__value--multiline">
                {{ selectedRecord.actionRequired || '无' }}
              </span>
            </template>
          </van-cell>
          <van-cell title="车辆异常">
            <template #value>
              <span class="record-popup__value record-popup__value--multiline">
                {{ selectedRecord.issueDescription || '无' }}
              </span>
            </template>
          </van-cell>
        </van-cell-group>

        <div
          v-for="section in photoSections"
          :key="section.key"
          class="record-photo-block"
        >
          <div class="record-photo-block__title">{{ section.title }}</div>
          <div v-if="section.photos.length" class="record-photo-strip">
            <van-image
              v-for="(photo, index) in section.photos"
              :key="`${section.key}-${index}`"
              :src="photo"
              fit="cover"
              class="record-photo-thumb"
              @click="previewPhotos(section.photos, index)"
            />
          </div>
          <div v-else class="record-photo-empty">暂无照片</div>
        </div>

        <div class="record-detail-tip">点击照片可放大查看。</div>

        <div class="record-popup__footer">
          <van-button block plain type="primary" @click="detailVisible = false">
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
import { showImagePreview, showToast } from 'vant'
import { getMyBorrowRecords } from '@/api'

const router = useRouter()

const PAGE_SIZE = 10
const records = ref([])
const page = ref(1)
const total = ref(0)
const loading = ref(false)
const finished = ref(false)
const initialLoaded = ref(false)
const detailVisible = ref(false)
const selectedRecord = ref(null)

const buildAssetUrl = (photo) => {
  if (!photo) {
    return ''
  }
  if (/^(https?:)?\/\//.test(photo) || photo.startsWith('data:') || photo.startsWith('blob:')) {
    return photo
  }
  if (photo.startsWith('/uploads/') || photo.startsWith('uploads/')) {
    const normalized = photo.startsWith('/') ? photo : `/${photo}`
    return `/api/uploads/preview?path=${encodeURIComponent(normalized)}`
  }
  if (photo.startsWith('/')) {
    return `${window.location.origin}${photo}`
  }
  return photo
}

const getPhotoList = (photos) => {
  if (!photos) {
    return []
  }
  if (Array.isArray(photos)) {
    return photos
      .map(item => {
        if (typeof item === 'string') return item
        return item?.url || item?.content || ''
      })
      .filter(Boolean)
      .map(buildAssetUrl)
  }
  return String(photos).split(',').map(item => item.trim()).filter(Boolean).map(buildAssetUrl)
}

const photoSections = computed(() => {
  if (!selectedRecord.value) {
    return []
  }
  return [
    { key: 'take-vehicle', title: '取车车辆照片', photos: getPhotoList(selectedRecord.value.takeVehiclePhotos) },
    { key: 'take-mileage', title: '取车公里数照片', photos: getPhotoList(selectedRecord.value.takeMileagePhoto) },
    { key: 'return-vehicle', title: '还车停车照片', photos: getPhotoList(selectedRecord.value.returnVehiclePhotos) },
    { key: 'return-mileage', title: '还车公里数照片', photos: getPhotoList(selectedRecord.value.returnMileagePhoto) },
    { key: 'return-fuel', title: '加油小票照片', photos: getPhotoList(selectedRecord.value.returnFuelPhoto) },
    { key: 'issue', title: '车辆异常照片', photos: getPhotoList(selectedRecord.value.issuePhotos) }
  ]
})

const formatDate = (date) => {
  if (!date) return ''
  return String(date).substring(0, 16).replace('T', ' ')
}

const formatMileage = (value) => {
  if (value === null || value === undefined || value === '') return '-'
  return `${value} km`
}

const getStatusVariant = (status) => {
  const map = { TAKEN: 'active', RETURNED: 'returned' }
  return map[status] || 'default'
}

const getStatusText = (status) => {
  const map = { TAKEN: '使用中', RETURNED: '已还车' }
  return map[status] || status
}

const getFollowUpType = (status) => {
  const map = { NONE: 'success', PENDING: 'warning', COMPLETED: 'primary' }
  return map[status] || 'primary'
}

const getFollowUpText = (status) => {
  const map = { NONE: '无需处理', PENDING: '待管理员处理', COMPLETED: '已处理' }
  return map[status] || status || '-'
}

const formatRecordLabel = (item) => {
  const time = item.status === 'TAKEN' ? item.takeTime : item.returnTime
  const followUp = item.followUpStatus === 'PENDING' ? ' · 待管理员处理' : ''
  return `${getStatusText(item.status)} · ${formatDate(time)}${followUp}`
}

const previewPhotos = (photos, startPosition = 0) => {
  if (!photos.length) {
    return
  }
  showImagePreview({ images: photos, startPosition })
}

const openRecord = (item) => {
  if (item.status === 'TAKEN') {
    router.push('/borrow')
    return
  }
  selectedRecord.value = item
  detailVisible.value = true
}

const loadMore = async () => {
  if (loading.value || finished.value) {
    return
  }
  loading.value = true
  try {
    const res = await getMyBorrowRecords({ current: page.value, size: PAGE_SIZE })
    const rows = res.data || []
    total.value = res.total || 0
    records.value = [...records.value, ...rows]
    initialLoaded.value = true
    page.value += 1
    finished.value = records.value.length >= total.value || rows.length < PAGE_SIZE
  } catch (error) {
    finished.value = true
    initialLoaded.value = true
    showToast(error.response?.data?.message || error.message || '用车记录加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadMore()
})
</script>

<style scoped>
.records-page {
  min-height: 100dvh;
  background: var(--app-bg);
  padding-bottom: calc(24px + var(--safe-area-bottom));
}

.records-body {
  padding: 14px;
}

.records-group {
  border-radius: 20px;
  overflow: hidden;
}

.record-cell :deep(.van-cell__title) {
  margin-left: 10px;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  line-height: 1;
  font-weight: 600;
}

.status-badge__dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: currentColor;
}

.status-badge--active {
  color: var(--brand-blue);
  background: var(--tabbar-active-bg);
}

.status-badge--returned {
  color: var(--brand-teal);
  background: var(--brand-teal-soft-bg);
}

.status-badge--default {
  color: var(--text-soft);
  background: var(--neutral-soft-bg);
}

.status-badge--popup {
  flex: none;
}

.empty-wrap {
  margin-top: 48px;
}

.record-popup {
  max-height: calc(88dvh - var(--safe-area-top));
}

.record-popup__content {
  max-height: calc(88dvh - var(--safe-area-top));
  overflow-y: auto;
  padding: 18px 14px calc(24px + var(--safe-area-bottom));
  background: transparent;
}

.record-popup__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.record-popup__title {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-strong);
  line-height: 1.3;
}

.record-popup__subtitle {
  margin-top: 4px;
  font-size: 12px;
  color: var(--text-muted);
  word-break: break-all;
}

.record-popup__group {
  border-radius: 16px;
  overflow: hidden;
}

.record-popup__value {
  color: var(--text-main);
}

.record-popup__value--multiline {
  display: inline-block;
  max-width: 180px;
  text-align: right;
  white-space: normal;
  line-height: 1.5;
}

.record-photo-block {
  margin-top: 14px;
  background: var(--sheet-card-bg);
  border-radius: 18px;
  padding: 14px;
  border: 1px solid var(--sheet-card-border);
  box-shadow: var(--sheet-card-shadow);
}

.record-photo-block__title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-strong);
  margin-bottom: 10px;
}

.record-photo-strip {
  display: flex;
  gap: 10px;
  overflow-x: auto;
  padding-bottom: 2px;
}

.record-photo-thumb {
  width: 88px;
  height: 88px;
  flex: none;
  overflow: hidden;
  border-radius: 14px;
  background: var(--surface-muted-4);
}

.record-photo-empty {
  font-size: 12px;
  color: var(--text-muted);
  line-height: 1.6;
}

.record-detail-tip {
  margin: 14px 4px 0;
  font-size: 12px;
  line-height: 1.6;
  color: var(--text-muted);
}

.record-popup__footer {
  margin-top: 16px;
}

@media (max-width: 375px) {
  .record-popup__value--multiline {
    max-width: 140px;
  }
}
</style>
