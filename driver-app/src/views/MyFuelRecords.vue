<template>
  <div class="records-page">
    <van-nav-bar title="加油记录" left-arrow @click-left="router.back()" />

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
            is-link
            class="fuel-cell"
            @click="openRecord(item)"
          >
            <template #title>
              <div class="fuel-info">
                <div class="fuel-plate">{{ item.plateNumber || '未知车牌' }}</div>
                <div class="fuel-detail">{{ formatDate(item.fuelDate) }} · {{ formatFuelSummary(item) }}</div>
              </div>
            </template>
            <template #value>
              <div class="fuel-value">
                <div class="fuel-amount">¥{{ formatMoney(item.totalAmount) }}</div>
                <van-tag :type="getReimbursementStatusType(item.reimbursementStatus)" size="small">
                  {{ getReimbursementStatusText(item.reimbursementStatus) }}
                </van-tag>
              </div>
            </template>
          </van-cell>
        </van-cell-group>
      </van-list>

      <div v-else-if="initialLoaded" class="empty-wrap">
        <van-empty description="暂无加油记录" />
      </div>
    </div>

    <van-popup v-model:show="detailVisible" position="bottom" round class="record-popup">
      <div v-if="selectedRecord" class="record-popup__content">
        <div class="record-popup__header">
          <div>
            <div class="record-popup__title">{{ selectedRecord.plateNumber || '加油记录' }}</div>
            <div class="record-popup__subtitle">{{ formatDate(selectedRecord.fuelDate) }}</div>
          </div>
          <van-tag :type="getReimbursementStatusType(selectedRecord.reimbursementStatus)" size="medium">
            {{ getReimbursementStatusText(selectedRecord.reimbursementStatus) }}
          </van-tag>
        </div>

        <van-cell-group inset class="record-popup__group">
          <van-cell title="车牌号" :value="selectedRecord.plateNumber || '-'" />
          <van-cell title="加油时间" :value="formatDate(selectedRecord.fuelDate) || '-'" />
          <van-cell title="油品" :value="selectedRecord.fuelType || '-'" />
          <van-cell title="金额" :value="`¥${formatMoney(selectedRecord.totalAmount)}`" />
          <van-cell title="登记内容" :value="formatFuelAmount(selectedRecord.fuelAmount)" />
          <van-cell title="加油里程" :value="formatMileage(selectedRecord.fuelMileage)" />
          <van-cell title="报销状态" :value="getReimbursementStatusText(selectedRecord.reimbursementStatus)" />
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
import { getMyFuelRecords } from '@/api'

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
    { key: 'invoice', title: '加油凭证照片', photos: getPhotoList(selectedRecord.value.invoicePhoto) },
    { key: 'gauge', title: '加油后油表照片（历史）', photos: getPhotoList(selectedRecord.value.fuelGaugePhoto) },
    { key: 'cash', title: '现金票据照片', photos: getPhotoList(selectedRecord.value.cashPhoto) },
  ].filter(section => section.photos.length > 0)
})

const formatDate = (date) => {
  if (!date) return ''
  return String(date).substring(0, 16).replace('T', ' ')
}

const formatMoney = (value) => {
  if (value === null || value === undefined || value === '') {
    return '0.00'
  }
  return Number(value).toFixed(2)
}

const formatFuelAmount = (value) => {
  if (value === null || value === undefined || value === '') return '仅登记金额'
  return `${value} L`
}

const formatMileage = (value) => {
  if (value === null || value === undefined || value === '') return '-'
  return `${value} km`
}

const formatFuelSummary = (item) => {
  if (!item) {
    return '仅登记金额'
  }
  const fuelType = item.fuelType || '未标注油品'
  if (item.fuelAmount === null || item.fuelAmount === undefined || item.fuelAmount === '') {
    return `${fuelType} · 仅登记金额`
  }
  return `${fuelType} · ${item.fuelAmount}L`
}


const getReimbursementStatusType = (status) => {
  const map = { NONE: 'primary', UNREIMBURSED: 'warning', REIMBURSED: 'success' }
  return map[status] || 'primary'
}

const getReimbursementStatusText = (status) => {
  const map = { NONE: '未报销', UNREIMBURSED: '未报销', REIMBURSED: '已报销' }
  return map[status] || status || '-'
}

const previewPhotos = (photos, startPosition = 0) => {
  if (!photos.length) {
    return
  }
  showImagePreview({ images: photos, startPosition })
}

const openRecord = (item) => {
  selectedRecord.value = item
  detailVisible.value = true
}

const loadMore = async () => {
  if (loading.value || finished.value) {
    return
  }
  loading.value = true
  try {
    const res = await getMyFuelRecords({ current: page.value, size: PAGE_SIZE })
    const rows = res.data || []
    total.value = res.total || 0
    records.value = [...records.value, ...rows]
    initialLoaded.value = true
    page.value += 1
    finished.value = records.value.length >= total.value || rows.length < PAGE_SIZE
  } catch (error) {
    finished.value = true
    initialLoaded.value = true
    showToast(error.response?.data?.message || error.message || '加油记录加载失败')
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

.fuel-cell {
  padding: 12px 16px;
}

.fuel-info .fuel-plate {
  font-weight: 600;
  color: var(--text-main);
  margin-bottom: 4px;
}

.fuel-info .fuel-detail {
  font-size: 12px;
  color: var(--text-muted);
}

.fuel-value {
  text-align: right;
}

.fuel-amount {
  font-weight: 600;
  color: var(--text-danger);
  margin-bottom: 6px;
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
</style>
