<template>
  <div class="borrow-records page-shell">
    <section class="borrow-workbench">
      <div class="borrow-workbench__main">
        <div class="borrow-workbench__label">当前范围</div>
        <div v-if="viewMode === 'ACTIVE'" class="borrow-workbench__desc">
          {{ activeRangeLabel }}，共 {{ statusOverview.total }} 条记录，其中使用中 {{ statusOverview.taken }} 条，已还车 {{ statusOverview.returned }} 条，待闭环 {{ statusOverview.pendingFollowUp }} 条。
        </div>
        <div v-else class="borrow-workbench__desc">
          当前位于借还车回收站，共 {{ recycleSummary.total }} 条记录。支持恢复记录，或做永久删除。
        </div>
        <div v-if="viewMode === 'ACTIVE'" class="borrow-workbench__chips">
          <span class="hero-chip">{{ selectedRangeTitle }} {{ statusOverview.total }}</span>
          <span class="hero-chip hero-chip--warning">使用中 {{ statusOverview.taken }}</span>
          <span class="hero-chip hero-chip--success">已还车 {{ statusOverview.returned }}</span>
          <span class="hero-chip hero-chip--danger">待闭环 {{ statusOverview.pendingFollowUp }}</span>
        </div>
        <div v-else class="borrow-workbench__chips">
          <span class="hero-chip">回收站 {{ recycleSummary.total }}</span>
        </div>
      </div>

      <div class="borrow-workbench__side">
        <div v-if="viewMode === 'ACTIVE'" class="borrow-workbench__presets">
          <button
            v-for="item in datePresetOptions"
            :key="item.value"
            type="button"
            class="overview-pill"
            :class="{ 'overview-pill--active': queryForm.datePreset === item.value }"
            @click="applyDatePreset(item.value)"
          >
            {{ item.label }}
          </button>
        </div>

        <div class="borrow-workbench__spotlight">
          <div class="borrow-workbench__spotlight-label">{{ borrowSpotlight.title }}</div>
          <div class="borrow-workbench__spotlight-value">{{ borrowSpotlight.value }}</div>
          <div class="borrow-workbench__spotlight-desc">{{ borrowSpotlight.desc }}</div>
        </div>
      </div>
    </section>

    <el-card class="panel-card">
      <template #header>
        <div class="panel-header">
          <div>
            <div class="panel-header__title">{{ viewMode === 'ACTIVE' ? '借还车主表' : '借还车回收站' }}</div>
            <div class="panel-header__desc">
              {{ viewMode === 'ACTIVE'
                ? '把记录单号、车牌、驾驶员、状态和待处理事项放在同一张表里，方便办公室连续处理。'
                : '回收站保留误删记录，可恢复到主表，或做永久删除。'
              }}
            </div>
          </div>
          <div v-if="viewMode === 'ACTIVE'" class="panel-header__actions">
            <el-button type="primary" @click="openSupplementDialog">补登记</el-button>
            <el-button type="primary" plain @click="openHalfYearReport">半年报告</el-button>
          </div>
        </div>
      </template>

      <div class="view-mode-tabs">
        <el-button :type="viewMode === 'ACTIVE' ? 'primary' : 'default'" @click="switchViewMode('ACTIVE')">主表</el-button>
        <el-button :type="viewMode === 'RECYCLE_BIN' ? 'primary' : 'default'" @click="switchViewMode('RECYCLE_BIN')">
          回收站（{{ recycleSummary.total }}）
        </el-button>
        <div class="view-mode-tabs__tools">
          <el-button v-if="viewMode === 'RECYCLE_BIN'" :icon="Refresh" @click="fetchRecycleBin">刷新</el-button>
          <el-button
            v-if="viewMode === 'RECYCLE_BIN'"
            type="danger"
            plain
            :icon="Delete"
            @click="handleClearRecycleBin"
          >
            清空回收站
          </el-button>
        </div>
      </div>

      <div v-if="viewMode === 'ACTIVE'" class="toolbar-row">
        <div class="toolbar-row__filters">
          <el-input
            v-model="queryForm.plateNumber"
            class="toolbar-search"
            placeholder="输入车牌号查询"
            clearable
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-input
            v-model="queryForm.driverName"
            class="toolbar-search"
            placeholder="输入驾驶员姓名"
            clearable
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon><User /></el-icon>
            </template>
          </el-input>
          <el-select v-model="queryForm.status" placeholder="借还状态" clearable class="toolbar-select">
            <el-option label="使用中" value="TAKEN" />
            <el-option label="已还车" value="RETURNED" />
          </el-select>
          <el-select v-model="queryForm.followUpStatus" placeholder="闭环状态" clearable class="toolbar-select">
            <el-option label="无需处理" value="NONE" />
            <el-option label="待处理" value="PENDING" />
            <el-option label="已处理" value="COMPLETED" />
          </el-select>
        </div>
        <div class="toolbar-row__actions">
          <el-button @click="handleReset">重置</el-button>
          <el-button type="primary" @click="handleSearch">查询</el-button>
        </div>
      </div>

      <div v-if="viewMode === 'ACTIVE'" class="quick-filters">
        <button
          type="button"
          class="quick-filter"
          :class="{ 'quick-filter--active': !queryForm.status && !queryForm.followUpStatus }"
          @click="queryForm.status = ''; queryForm.followUpStatus = ''; handleSearch()"
        >
          <span>全部记录</span>
          <strong>{{ pagination.total }}</strong>
        </button>
        <button
          type="button"
          class="quick-filter"
          :class="{ 'quick-filter--active': queryForm.status === 'TAKEN' && !queryForm.followUpStatus }"
          @click="queryForm.status = 'TAKEN'; queryForm.followUpStatus = ''; handleSearch()"
        >
          <span>使用中</span>
          <strong>{{ statusOverview.taken }}</strong>
        </button>
        <button
          type="button"
          class="quick-filter"
          :class="{ 'quick-filter--active': queryForm.status === 'RETURNED' && !queryForm.followUpStatus }"
          @click="queryForm.status = 'RETURNED'; queryForm.followUpStatus = ''; handleSearch()"
        >
          <span>已还车</span>
          <strong>{{ statusOverview.returned }}</strong>
        </button>
        <button
          type="button"
          class="quick-filter"
          :class="{ 'quick-filter--active': queryForm.followUpStatus === 'PENDING' && !queryForm.status }"
          @click="queryForm.status = ''; queryForm.followUpStatus = 'PENDING'; handleSearch()"
        >
          <span>待闭环</span>
          <strong>{{ statusOverview.pendingFollowUp }}</strong>
        </button>
      </div>

      <template v-if="viewMode === 'ACTIVE'">
        <el-table :data="tableData" border class="borrow-table" :row-class-name="getActiveRowClassName">
          <el-table-column label="任务摘要" min-width="260">
            <template #default="{ row }">
              <div class="record-cell">
                <div class="record-cell__title">{{ row.recordNo }}</div>
                <div class="record-cell__meta">{{ row.destination || '未填写目的地/去向' }}</div>
                <div v-if="row.usageReason" class="record-cell__sub">{{ row.usageReason }}</div>
              </div>
            </template>
          </el-table-column>
        <el-table-column prop="plateNumber" label="车牌号" width="120" />
        <el-table-column prop="driverName" label="驾驶员" width="120" />
        <el-table-column label="取车时间" width="170">
          <template #default="{ row }">
            {{ formatDate(row.takeTime) }}
          </template>
        </el-table-column>
        <el-table-column label="还车时间" width="170">
          <template #default="{ row }">
            {{ formatDate(row.returnTime) || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="借还状态" width="110">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusName(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="闭环状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getFollowUpStatusType(row.followUpStatus)">{{ getFollowUpStatusName(row.followUpStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="需处理事项" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="action-required-cell">
              <span>{{ row.actionRequired || '当前无待处理事项' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="showDetail(row)">查看详情</el-button>
            <el-button link type="success" @click="openEditDialog(row)">修改</el-button>
            <el-button
              v-if="row.followUpStatus === 'PENDING'"
              link
              type="warning"
              @click="openFollowUpDialog(row)"
            >
              完成闭环
            </el-button>
            <el-button
              v-if="row.status === 'RETURNED'"
              link
              type="danger"
              @click="handleDelete(row)"
            >
              移入回收站
            </el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="当前没有匹配的借还车记录" :image-size="90" />
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
      </template>

      <template v-else>
        <el-table :data="recycleData" border class="borrow-table">
          <el-table-column label="任务摘要" min-width="260">
            <template #default="{ row }">
              <div class="record-cell">
                <div class="record-cell__title">{{ row.recordNo }}</div>
                <div class="record-cell__meta">{{ row.destination || '未填写目的地/去向' }}</div>
                <div v-if="row.usageReason" class="record-cell__sub">{{ row.usageReason }}</div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="plateNumber" label="车牌号" width="120" />
          <el-table-column prop="driverName" label="驾驶员" width="120" />
          <el-table-column label="取车时间" width="170">
            <template #default="{ row }">
              {{ formatDate(row.takeTime) }}
            </template>
          </el-table-column>
          <el-table-column label="还车时间" width="170">
            <template #default="{ row }">
              {{ formatDate(row.returnTime) || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="删除时间" width="170">
            <template #default="{ row }">
              {{ formatDate(row.deletedTime) || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="删除原因" min-width="180" show-overflow-tooltip>
            <template #default="{ row }">
              {{ row.deleteReason || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="280" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="showDetail(row)">查看详情</el-button>
              <el-button link type="success" @click="handleRestore(row)">恢复</el-button>
              <el-button link type="danger" @click="handlePermanentDelete(row)">永久删除</el-button>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty description="回收站为空" :image-size="90" />
          </template>
        </el-table>

        <el-pagination
          v-model:current-page="recyclePagination.current"
          v-model:page-size="recyclePagination.size"
          :total="recyclePagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          class="table-pagination"
          @size-change="fetchRecycleBin"
          @current-change="fetchRecycleBin"
        />
      </template>
    </el-card>

    <el-dialog v-model="detailVisible" title="借还车详情" width="960px">
      <template v-if="currentRecord">
        <el-alert
          v-if="currentRecord.followUpStatus === 'PENDING'"
          type="warning"
          :closable="false"
          style="margin-bottom: 16px"
          :title="`当前待处理：${currentRecord.actionRequired || '车辆异常待处理'}`"
        />

        <el-descriptions :column="2" border>
          <el-descriptions-item label="记录单号">{{ currentRecord.recordNo }}</el-descriptions-item>
          <el-descriptions-item label="借还状态">
            <el-tag :type="getStatusType(currentRecord.status)">
              {{ getStatusName(currentRecord.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="车牌号">{{ currentRecord.plateNumber }}</el-descriptions-item>
          <el-descriptions-item label="驾驶员">{{ currentRecord.driverName }}</el-descriptions-item>
          <el-descriptions-item label="用车事由">{{ currentRecord.usageReason || '-' }}</el-descriptions-item>
          <el-descriptions-item label="目的地/去向">{{ currentRecord.destination || '-' }}</el-descriptions-item>
          <el-descriptions-item label="取车时间">{{ formatDate(currentRecord.takeTime) }}</el-descriptions-item>
          <el-descriptions-item label="预计还车">{{ formatDate(currentRecord.expectedReturnTime) || '-' }}</el-descriptions-item>
          <el-descriptions-item label="取车里程">{{ formatMileage(currentRecord.takeMileage) }}</el-descriptions-item>
          <el-descriptions-item label="还车时间">{{ formatDate(currentRecord.returnTime) || '-' }}</el-descriptions-item>
          <el-descriptions-item label="还车里程">{{ formatMileage(currentRecord.returnMileage) }}</el-descriptions-item>
          <el-descriptions-item label="本次行驶里程">{{ formatMileage(currentRecord.tripMileage) }}</el-descriptions-item>
          <el-descriptions-item label="车辆是否干净">
            <el-tag v-if="currentRecord.isClean === 1" type="success">干净</el-tag>
            <el-tag v-else-if="currentRecord.isClean === 0" type="warning">需洗车</el-tag>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="油量是否不少于半箱">
            <el-tag v-if="currentRecord.isFuelEnough === 1" type="success">是</el-tag>
            <el-tag v-else-if="currentRecord.isFuelEnough === 0" type="danger">需加油</el-tag>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="车辆异常说明">{{ currentRecord.issueDescription || '无' }}</el-descriptions-item>
          <el-descriptions-item label="需处理事项">{{ currentRecord.actionRequired || '无' }}</el-descriptions-item>
          <el-descriptions-item label="闭环状态">
            <el-tag :type="getFollowUpStatusType(currentRecord.followUpStatus)">
              {{ getFollowUpStatusName(currentRecord.followUpStatus) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="闭环结果">
            <span v-if="currentRecord.followUpStatus === 'COMPLETED'">
              {{ getVehicleStatusName(currentRecord.followUpResultStatus) }}
            </span>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="处理人">
            {{ currentRecord.followUpHandledByName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="处理时间">
            {{ formatDate(currentRecord.followUpHandledTime) || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="处理说明" :span="2">
            {{ currentRecord.followUpRemark || '-' }}
          </el-descriptions-item>
        </el-descriptions>

        <el-divider>取车车辆照片</el-divider>
        <div class="photo-grid">
          <el-image
            v-for="(photo, index) in getPhotoList(currentRecord.takeVehiclePhotos)"
            :key="'take-vehicle-' + index"
            :src="photo"
            :preview-src-list="getPhotoList(currentRecord.takeVehiclePhotos)"
            fit="cover"
            class="photo-item"
          />
          <span v-if="getPhotoList(currentRecord.takeVehiclePhotos).length === 0" class="empty-text">无</span>
        </div>

        <el-divider>取车公里数照片</el-divider>
        <div class="photo-grid">
          <el-image
            v-for="(photo, index) in getPhotoList(currentRecord.takeMileagePhoto)"
            :key="'take-mileage-' + index"
            :src="photo"
            :preview-src-list="getPhotoList(currentRecord.takeMileagePhoto)"
            fit="cover"
            class="photo-item"
          />
          <span v-if="getPhotoList(currentRecord.takeMileagePhoto).length === 0" class="empty-text">无</span>
        </div>

        <el-divider>还车停车照片</el-divider>
        <div class="photo-grid">
          <el-image
            v-for="(photo, index) in getPhotoList(currentRecord.returnVehiclePhotos)"
            :key="'return-vehicle-' + index"
            :src="photo"
            :preview-src-list="getPhotoList(currentRecord.returnVehiclePhotos)"
            fit="cover"
            class="photo-item"
          />
          <span v-if="getPhotoList(currentRecord.returnVehiclePhotos).length === 0" class="empty-text">无</span>
        </div>

        <el-divider>{{ getReturnMileageSectionTitle(currentRecord) }}</el-divider>
        <div class="photo-grid">
          <el-image
            v-for="(photo, index) in getReturnMileageSectionPhotos(currentRecord)"
            :key="'return-mileage-' + index"
            :src="photo"
            :preview-src-list="getReturnMileageSectionPhotos(currentRecord)"
            fit="cover"
            class="photo-item"
          />
          <span v-if="getReturnMileageSectionPhotos(currentRecord).length === 0" class="empty-text">无</span>
        </div>

        <template v-if="!hasSharedReturnDashboardPhotos(currentRecord)">
          <el-divider>还车油表照片</el-divider>
          <div class="photo-grid">
            <el-image
              v-for="(photo, index) in getReturnFuelPhotos(currentRecord)"
              :key="'return-fuel-' + index"
              :src="photo"
              :preview-src-list="getReturnFuelPhotos(currentRecord)"
              fit="cover"
              class="photo-item"
            />
            <span v-if="getReturnFuelPhotos(currentRecord).length === 0" class="empty-text">无</span>
          </div>
        </template>

        <el-divider>车辆异常照片</el-divider>
        <div class="photo-grid">
          <el-image
            v-for="(photo, index) in getPhotoList(currentRecord.issuePhotos)"
            :key="'issue-' + index"
            :src="photo"
            :preview-src-list="getPhotoList(currentRecord.issuePhotos)"
            fit="cover"
            class="photo-item"
          />
          <span v-if="getPhotoList(currentRecord.issuePhotos).length === 0" class="empty-text">无</span>
        </div>

        <el-divider>修改日志</el-divider>
        <el-skeleton v-if="editLogsLoading" :rows="3" animated />
        <el-empty v-else-if="editLogs.length === 0" description="暂无修改日志" />
        <el-timeline v-else>
          <el-timeline-item
            v-for="log in editLogs"
            :key="log.id"
            :timestamp="formatDate(log.createTime)"
            placement="top"
          >
            <div class="log-card">
              <div class="log-operator">{{ log.operatorName || '系统' }}</div>
              <div class="log-summary">{{ log.changeSummary }}</div>
            </div>
          </el-timeline-item>
        </el-timeline>
      </template>

      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button v-if="currentRecord?.deleted !== 1" type="success" @click="openEditDialog(currentRecord)">修改记录</el-button>
        <el-button
          v-if="currentRecord?.deleted !== 1 && currentRecord?.followUpStatus === 'PENDING'"
          type="warning"
          @click="openFollowUpDialog(currentRecord)"
        >
          完成闭环
        </el-button>
        <el-button
          v-if="currentRecord?.deleted === 1"
          type="success"
          @click="handleRestore(currentRecord)"
        >
          恢复记录
        </el-button>
        <el-button
          v-if="currentRecord?.deleted === 1"
          type="danger"
          @click="handlePermanentDelete(currentRecord)"
        >
          永久删除
        </el-button>
        <el-button
          v-if="currentRecord?.deleted !== 1 && currentRecord?.status === 'RETURNED'"
          type="danger"
          @click="handleDelete(currentRecord)"
        >
          移入回收站
        </el-button>
      </template>
    </el-dialog>

    <SupplementDialog ref="supplementDialogRef" @created="onSupplementCreated" />

    <el-dialog v-model="editDialogVisible" title="修改借还车记录" width="920px">
      <el-alert
        type="info"
        :closable="false"
        style="margin-bottom: 16px"
        title="管理员可以直接修正借还车字段并替换照片。每次保存都会生成修改日志。留空不上传，表示保留原照片。"
      />

      <el-form :model="editForm" label-width="120px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="车牌号">
              <el-input v-model="editForm.plateNumber" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="借还状态">
              <el-input :model-value="getStatusName(editRecordStatus)" disabled />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="用车事由">
          <el-input
            v-model="editForm.usageReason"
            type="textarea"
            :rows="2"
            placeholder="可为空"
          />
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="目的地/去向" required>
              <el-input v-model="editForm.destination" placeholder="请输入目的地/去向" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="取车里程(km)" required>
              <el-input-number v-model="editForm.takeMileage" :min="0" :precision="2" :step="1" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="取车时间" required>
              <el-date-picker
                v-model="editForm.takeTime"
                type="datetime"
                value-format="YYYY-MM-DD HH:mm:ss"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="预计还车">
              <el-date-picker
                v-model="editForm.expectedReturnTime"
                type="datetime"
                value-format="YYYY-MM-DD HH:mm:ss"
                style="width: 100%"
                clearable
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider>取车照片</el-divider>
        <div class="edit-photo-group">
          <div class="edit-photo-title">取车车辆照片</div>
          <div class="upload-note">上传后将替换当前整组照片</div>
          <div class="photo-grid compact-grid">
            <el-image
              v-for="(photo, index) in getPhotoList(editExistingPhotos.takeVehiclePhotos)"
              :key="'edit-take-vehicle-' + index"
              :src="photo"
              :preview-src-list="getPhotoList(editExistingPhotos.takeVehiclePhotos)"
              fit="cover"
              class="photo-item small-photo"
            />
            <span v-if="getPhotoList(editExistingPhotos.takeVehiclePhotos).length === 0" class="empty-text">无</span>
          </div>
          <el-upload
            v-model:file-list="editUploadLists.takeVehiclePhotos"
            class="upload-block"
            list-type="picture-card"
            accept="image/*"
            :auto-upload="false"
            :multiple="true"
            :limit="6"
            :on-preview="handleUploadPreview"
            :on-exceed="() => handleUploadExceed('取车车辆照片', 6)"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
        </div>

        <div class="edit-photo-group">
          <div class="edit-photo-title">取车公里数照片</div>
          <div class="upload-note">上传后将替换当前照片</div>
          <div class="photo-grid compact-grid">
            <el-image
              v-for="(photo, index) in getPhotoList(editExistingPhotos.takeMileagePhoto)"
              :key="'edit-take-mileage-' + index"
              :src="photo"
              :preview-src-list="getPhotoList(editExistingPhotos.takeMileagePhoto)"
              fit="cover"
              class="photo-item small-photo"
            />
            <span v-if="getPhotoList(editExistingPhotos.takeMileagePhoto).length === 0" class="empty-text">无</span>
          </div>
          <el-upload
            v-model:file-list="editUploadLists.takeMileagePhotos"
            class="upload-block"
            list-type="picture-card"
            accept="image/*"
            :auto-upload="false"
            :multiple="false"
            :limit="1"
            :on-preview="handleUploadPreview"
            :on-exceed="() => handleUploadExceed('取车公里数照片', 1)"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
        </div>

        <template v-if="editRecordStatus === 'RETURNED'">
          <el-divider>还车信息</el-divider>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="还车时间" required>
                <el-date-picker
                  v-model="editForm.returnTime"
                  type="datetime"
                  value-format="YYYY-MM-DD HH:mm:ss"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="还车里程(km)" required>
                <el-input-number v-model="editForm.returnMileage" :min="0" :precision="2" :step="1" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="车辆是否干净" required>
                <el-select v-model="editForm.isClean" style="width: 100%">
                  <el-option label="干净" :value="1" />
                  <el-option label="需洗车" :value="0" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="油量是否半箱" required>
                <el-select v-model="editForm.isFuelEnough" style="width: 100%">
                  <el-option label="是" :value="1" />
                  <el-option label="否，需加油" :value="0" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="车辆异常说明">
            <el-input
              v-model="editForm.issueDescription"
              type="textarea"
              :rows="3"
              placeholder="如无异常可不填"
            />
          </el-form-item>

          <el-divider>还车照片</el-divider>
          <div class="edit-photo-group">
            <div class="edit-photo-title">还车停车照片</div>
            <div class="upload-note">上传后将替换当前整组照片</div>
            <div class="photo-grid compact-grid">
              <el-image
                v-for="(photo, index) in getPhotoList(editExistingPhotos.returnVehiclePhotos)"
                :key="'edit-return-vehicle-' + index"
                :src="photo"
                :preview-src-list="getPhotoList(editExistingPhotos.returnVehiclePhotos)"
                fit="cover"
                class="photo-item small-photo"
              />
              <span v-if="getPhotoList(editExistingPhotos.returnVehiclePhotos).length === 0" class="empty-text">无</span>
            </div>
            <el-upload
              v-model:file-list="editUploadLists.returnVehiclePhotos"
              class="upload-block"
              list-type="picture-card"
              accept="image/*"
              :auto-upload="false"
              :multiple="true"
              :limit="6"
              :on-preview="handleUploadPreview"
              :on-exceed="() => handleUploadExceed('还车停车照片', 6)"
            >
              <el-icon><Plus /></el-icon>
            </el-upload>
          </div>

          <div class="edit-photo-group">
            <div class="edit-photo-title">还车公里数照片</div>
            <div class="upload-note">上传后将替换当前照片</div>
            <div class="photo-grid compact-grid">
              <el-image
                v-for="(photo, index) in getPhotoList(editExistingPhotos.returnMileagePhoto)"
                :key="'edit-return-mileage-' + index"
                :src="photo"
                :preview-src-list="getPhotoList(editExistingPhotos.returnMileagePhoto)"
                fit="cover"
                class="photo-item small-photo"
              />
              <span v-if="getPhotoList(editExistingPhotos.returnMileagePhoto).length === 0" class="empty-text">无</span>
            </div>
            <el-upload
              v-model:file-list="editUploadLists.returnMileagePhotos"
              class="upload-block"
              list-type="picture-card"
              accept="image/*"
              :auto-upload="false"
              :multiple="false"
              :limit="1"
              :on-preview="handleUploadPreview"
              :on-exceed="() => handleUploadExceed('还车公里数照片', 1)"
            >
              <el-icon><Plus /></el-icon>
            </el-upload>
          </div>

          <div class="edit-photo-group">
            <div class="edit-photo-title">还车油表照片</div>
            <div class="upload-note">上传后将替换当前照片</div>
            <div class="photo-grid compact-grid">
              <el-image
                v-for="(photo, index) in getPhotoList(editExistingPhotos.returnFuelPhoto)"
                :key="'edit-return-fuel-' + index"
                :src="photo"
                :preview-src-list="getPhotoList(editExistingPhotos.returnFuelPhoto)"
                fit="cover"
                class="photo-item small-photo"
              />
              <span v-if="getPhotoList(editExistingPhotos.returnFuelPhoto).length === 0" class="empty-text">无</span>
            </div>
            <el-upload
              v-model:file-list="editUploadLists.returnFuelPhotos"
              class="upload-block"
              list-type="picture-card"
              accept="image/*"
              :auto-upload="false"
              :multiple="false"
              :limit="1"
              :on-preview="handleUploadPreview"
              :on-exceed="() => handleUploadExceed('还车油表照片', 1)"
            >
              <el-icon><Plus /></el-icon>
            </el-upload>
          </div>

          <div class="edit-photo-group">
            <div class="edit-photo-title">车辆异常照片</div>
            <div class="upload-note">如填写异常说明，建议同步上传异常照片；留空表示保留原照片</div>
            <div class="photo-grid compact-grid">
              <el-image
                v-for="(photo, index) in getPhotoList(editExistingPhotos.issuePhotos)"
                :key="'edit-issue-' + index"
                :src="photo"
                :preview-src-list="getPhotoList(editExistingPhotos.issuePhotos)"
                fit="cover"
                class="photo-item small-photo"
              />
              <span v-if="getPhotoList(editExistingPhotos.issuePhotos).length === 0" class="empty-text">无</span>
            </div>
            <el-upload
              v-model:file-list="editUploadLists.issuePhotos"
              class="upload-block"
              list-type="picture-card"
              accept="image/*"
              :auto-upload="false"
              :multiple="true"
              :limit="6"
              :on-preview="handleUploadPreview"
              :on-exceed="() => handleUploadExceed('车辆异常照片', 6)"
            >
              <el-icon><Plus /></el-icon>
            </el-upload>
          </div>
        </template>
      </el-form>

      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="editLoading" @click="handleUpdateRecord">保存修改</el-button>
      </template>
    </el-dialog>

    <FollowUpDialog ref="followUpDialogRef" @completed="onFollowUpCompleted" />

    <el-dialog v-model="uploadPreviewVisible" title="照片预览" width="720px">
      <img :src="uploadPreviewUrl" alt="预览图片" class="preview-image" />
    </el-dialog>

  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search, User, Delete, Refresh, Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  deleteBorrowRecord,
  getBorrowRecycleBin,
  getBorrowRecordEditLogs,
  getBorrowRecords,
  clearBorrowRecycleBin,
  permanentDeleteBorrowRecord,
  restoreBorrowRecord,
  updateBorrowRecord
} from '@/api/vehicleBorrow'
import SupplementDialog from '@/components/SupplementDialog.vue'
import FollowUpDialog from '@/components/FollowUpDialog.vue'

const route = useRoute()
const router = useRouter()
const viewMode = ref('ACTIVE')
const tableData = ref([])
const recycleData = ref([])
const supplementDialogRef = ref(null)
const followUpDialogRef = ref(null)
const detailVisible = ref(false)
const editDialogVisible = ref(false)
const editLoading = ref(false)
const editLogsLoading = ref(false)
const currentRecord = ref(null)
const editTargetId = ref(null)
const editRecordStatus = ref('TAKEN')
const editLogs = ref([])
const uploadPreviewVisible = ref(false)
const uploadPreviewUrl = ref('')
const highlightRecordId = ref(null)
const datePresetOptions = [
  { label: '本月', value: 'month' },
  { label: '30天', value: '30d' },
  { label: '90天', value: '90d' },
  { label: '180天', value: '180d' },
  { label: '全年', value: 'year' }
]

const queryForm = reactive({
  plateNumber: '',
  driverName: '',
  status: '',
  followUpStatus: '',
  startDate: '',
  endDate: '',
  datePreset: 'month'
})

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const recyclePagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const recycleSummary = reactive({
  total: 0
})

const statusOverview = reactive({
  total: 0,
  taken: 0,
  returned: 0,
  pendingFollowUp: 0,
  actionRequired: 0
})

const rangeTitleMap = {
  month: '本月记录',
  '30d': '30天记录',
  '90d': '90天记录',
  '180d': '180天记录',
  year: '全年记录'
}

const borrowSpotlight = computed(() => {
  if (viewMode.value === 'RECYCLE_BIN') {
    return {
      title: '回收站',
      value: recycleSummary.total,
      desc: '已移入回收站的历史记录，可恢复或永久删除。'
    }
  }
  if (statusOverview.pendingFollowUp > 0) {
    return {
      title: '闭环事项待处理',
      value: statusOverview.pendingFollowUp,
      desc: '优先完成闭环，避免车辆当前状态和历史记录不一致。'
    }
  }
  if (statusOverview.taken > 0) {
    return {
      title: '仍在使用中的车辆',
      value: statusOverview.taken,
      desc: '当前仍有借车未还的记录，适合先检查预计还车和实际状态。'
    }
  }
  return {
    title: '流转状态平稳',
    value: statusOverview.returned,
    desc: '当前范围内没有明显堵点，可以继续核对照片、里程和历史细节。'
  }
})

const selectedRangeTitle = computed(() => rangeTitleMap[queryForm.datePreset] || '本月记录')

const activeRangeLabel = computed(() => {
  if (!queryForm.startDate || !queryForm.endDate) {
    return '当前未设置时间范围'
  }
  return `按取车时间筛选：${queryForm.startDate.slice(0, 10)} 至 ${queryForm.endDate.slice(0, 10)}`
})

const editForm = reactive({
  plateNumber: '',
  usageReason: '',
  destination: '',
  takeTime: '',
  expectedReturnTime: '',
  takeMileage: null,
  returnTime: '',
  returnMileage: null,
  isClean: 1,
  isFuelEnough: 1,
  issueDescription: ''
})

const editExistingPhotos = reactive({
  takeVehiclePhotos: '',
  takeMileagePhoto: '',
  returnVehiclePhotos: '',
  returnMileagePhoto: '',
  returnFuelPhoto: '',
  issuePhotos: ''
})

const createUploadLists = () => ({
  takeVehiclePhotos: [],
  takeMileagePhotos: [],
  returnVehiclePhotos: [],
  returnMileagePhotos: [],
  returnFuelPhotos: [],
  issuePhotos: []
})

const editUploadLists = reactive(createUploadLists())

const getStatusType = (status) => {
  const map = { TAKEN: 'warning', RETURNED: 'success' }
  return map[status] || 'info'
}

const getStatusName = (status) => {
  const map = { TAKEN: '使用中', RETURNED: '已还车' }
  return map[status] || status
}

const getFollowUpStatusType = (status) => {
  const map = { NONE: 'info', PENDING: 'warning', COMPLETED: 'success' }
  return map[status] || 'info'
}

const getFollowUpStatusName = (status) => {
  const map = { NONE: '无需处理', PENDING: '待处理', COMPLETED: '已处理' }
  return map[status] || status || '-'
}

const getVehicleStatusName = (status) => {
  const map = { NORMAL: '正常', MAINTENANCE: '维修中', PENDING_CHECK: '待复核' }
  return map[status] || status || '-'
}

const formatDate = (date) => {
  if (!date) return ''
  return String(date).substring(0, 19).replace('T', ' ')
}

const formatMileage = (value) => {
  if (value === null || value === undefined || value === '') return '-'
  return `${value} km`
}

const formatDateTimeValue = (date) => {
  const pad = (n) => String(n).padStart(2, '0')
  return [
    date.getFullYear(),
    pad(date.getMonth() + 1),
    pad(date.getDate())
  ].join('-') + ` ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

const getMonthRange = () => {
  const now = new Date()
  const start = new Date(now.getFullYear(), now.getMonth(), 1, 0, 0, 0)
  const end = new Date(now.getFullYear(), now.getMonth() + 1, 0, 23, 59, 59)
  return {
    startDate: formatDateTimeValue(start),
    endDate: formatDateTimeValue(end)
  }
}

const getRecentDaysRange = (days) => {
  const now = new Date()
  const start = new Date(now)
  start.setDate(now.getDate() - (days - 1))
  start.setHours(0, 0, 0, 0)
  const end = new Date(now)
  end.setHours(23, 59, 59, 0)
  return {
    startDate: formatDateTimeValue(start),
    endDate: formatDateTimeValue(end)
  }
}

const getYearRange = () => {
  const now = new Date()
  const start = new Date(now.getFullYear(), 0, 1, 0, 0, 0)
  const end = new Date(now.getFullYear(), 11, 31, 23, 59, 59)
  return {
    startDate: formatDateTimeValue(start),
    endDate: formatDateTimeValue(end)
  }
}

const getRangeByPreset = (preset) => {
  switch (preset) {
    case '30d':
      return getRecentDaysRange(30)
    case '90d':
      return getRecentDaysRange(90)
    case '180d':
      return getRecentDaysRange(180)
    case 'year':
      return getYearRange()
    case 'month':
    default:
      return getMonthRange()
  }
}

const applyDatePreset = (preset, shouldSearch = true) => {
  const nextPreset = preset || 'month'
  const range = getRangeByPreset(nextPreset)
  queryForm.datePreset = nextPreset
  queryForm.startDate = range.startDate
  queryForm.endDate = range.endDate
  if (shouldSearch && viewMode.value === 'ACTIVE') {
    handleSearch()
  }
}

const getTableSummary = (records) => ({
  total: records.length,
  taken: records.filter(item => item.status === 'TAKEN').length,
  returned: records.filter(item => item.status === 'RETURNED').length,
  pendingFollowUp: records.filter(item => item.followUpStatus === 'PENDING').length,
  actionRequired: records.filter(item => !!item.actionRequired).length
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
  return String(photos)
    .split(',')
    .map(item => item.trim())
    .filter(Boolean)
    .map(buildAssetUrl)
}

const isSamePhotoList = (left, right) => {
  if (!left.length || left.length !== right.length) {
    return false
  }
  return left.every((photo, index) => photo === right[index])
}

const getReturnMileagePhotos = (record) => getPhotoList(record?.returnMileagePhoto)
const getReturnFuelPhotos = (record) => getPhotoList(record?.returnFuelPhoto)
const hasSharedReturnDashboardPhotos = (record) => isSamePhotoList(getReturnMileagePhotos(record), getReturnFuelPhotos(record))
const getReturnDashboardPhotos = (record) => {
  const mileagePhotos = getReturnMileagePhotos(record)
  if (mileagePhotos.length > 0) {
    return mileagePhotos
  }
  return getReturnFuelPhotos(record)
}
const getReturnMileageSectionTitle = (record) => hasSharedReturnDashboardPhotos(record) ? '还车仪表照片' : '还车公里数照片'
const getReturnMileageSectionPhotos = (record) => hasSharedReturnDashboardPhotos(record)
  ? getReturnDashboardPhotos(record)
  : getReturnMileagePhotos(record)

const resetUploadLists = () => {
  Object.assign(editUploadLists, createUploadLists())
}

const normalizeText = (value) => {
  if (value === null || value === undefined) {
    return ''
  }
  return String(value).trim()
}

const parsePositiveInt = (value) => {
  const text = normalizeText(value)
  if (!text) {
    return null
  }
  const number = Number(text)
  if (!Number.isInteger(number) || number <= 0) {
    return null
  }
  return number
}

const getActiveRowClassName = ({ row }) => {
  if (!highlightRecordId.value) {
    return ''
  }
  return Number(row.id) === Number(highlightRecordId.value) ? 'borrow-table__row--highlight' : ''
}

const resetRecordContextById = (recordId) => {
  if (!recordId) {
    return
  }
  if (currentRecord.value?.id === recordId) {
    detailVisible.value = false
    currentRecord.value = null
    editLogs.value = []
  }
  if (editTargetId.value === recordId) {
    editDialogVisible.value = false
    editTargetId.value = null
  }
}

const fetchRecycleBin = async () => {
  try {
    const res = await getBorrowRecycleBin({
      current: recyclePagination.current,
      size: recyclePagination.size
    })
    recycleData.value = res.data || []
    recyclePagination.total = Number(res.total || 0)
    recycleSummary.total = Number(res.total || recycleData.value.length || 0)
  } catch (error) {
    recycleData.value = []
    recyclePagination.total = 0
    recycleSummary.total = 0
    ElMessage.error(error.response?.data?.message || '获取回收站失败')
  }
}

const switchViewMode = (nextMode) => {
  if (viewMode.value === nextMode) {
    return
  }
  viewMode.value = nextMode
  if (nextMode === 'ACTIVE') {
    pagination.current = 1
    fetchData()
    return
  }
  recyclePagination.current = 1
  fetchRecycleBin()
}

const fetchData = async () => {
  if (viewMode.value === 'RECYCLE_BIN') {
    return fetchRecycleBin()
  }
  try {
    const { datePreset, ...requestQuery } = queryForm
    const res = await getBorrowRecords({
      current: pagination.current,
      size: pagination.size,
      ...requestQuery
    })
    tableData.value = res.data || []
    pagination.total = res.total || 0
    const summary = res.summary || getTableSummary(tableData.value)
    statusOverview.total = Number(summary.total || 0)
    statusOverview.taken = Number(summary.taken || 0)
    statusOverview.returned = Number(summary.returned || 0)
    statusOverview.pendingFollowUp = Number(summary.pendingFollowUp || 0)
    statusOverview.actionRequired = Number(summary.actionRequired || 0)
  } catch (error) {
    tableData.value = []
    pagination.total = 0
    statusOverview.total = 0
    statusOverview.taken = 0
    statusOverview.returned = 0
    statusOverview.pendingFollowUp = 0
    statusOverview.actionRequired = 0
  }
}

const loadEditLogs = async (recordId) => {
  if (!recordId) {
    editLogs.value = []
    return
  }
  editLogsLoading.value = true
  try {
    editLogs.value = await getBorrowRecordEditLogs(recordId)
  } catch (error) {
    editLogs.value = []
    ElMessage.error(error.response?.data?.message || '获取修改日志失败')
  } finally {
    editLogsLoading.value = false
  }
}

const handleSearch = () => {
  pagination.current = 1
  fetchData()
}

const handleReset = () => {
  queryForm.plateNumber = ''
  queryForm.driverName = ''
  queryForm.status = ''
  queryForm.followUpStatus = ''
  highlightRecordId.value = null
  applyDatePreset('month', false)
  handleSearch()
}

const openHalfYearReport = () => {
  router.push('/half-year-report')
}

const openSupplementDialog = () => supplementDialogRef.value.open()

const showDetail = (row) => {
  currentRecord.value = row
  detailVisible.value = true
  loadEditLogs(row?.id)
}

const openEditDialog = (row) => {
  if (!row) {
    return
  }
  editTargetId.value = row.id
  editRecordStatus.value = row.status
  editForm.plateNumber = row.plateNumber || ''
  editForm.usageReason = row.usageReason || ''
  editForm.destination = row.destination || ''
  editForm.takeTime = formatDate(row.takeTime) || ''
  editForm.expectedReturnTime = formatDate(row.expectedReturnTime) || ''
  editForm.takeMileage = row.takeMileage === null || row.takeMileage === undefined ? null : Number(row.takeMileage)
  editForm.returnTime = formatDate(row.returnTime) || ''
  editForm.returnMileage = row.returnMileage === null || row.returnMileage === undefined ? null : Number(row.returnMileage)
  editForm.isClean = row.isClean === null || row.isClean === undefined ? 1 : Number(row.isClean)
  editForm.isFuelEnough = row.isFuelEnough === null || row.isFuelEnough === undefined ? 1 : Number(row.isFuelEnough)
  editForm.issueDescription = row.issueDescription || ''
  editExistingPhotos.takeVehiclePhotos = row.takeVehiclePhotos || ''
  editExistingPhotos.takeMileagePhoto = row.takeMileagePhoto || ''
  editExistingPhotos.returnVehiclePhotos = row.returnVehiclePhotos || ''
  editExistingPhotos.returnMileagePhoto = row.returnMileagePhoto || ''
  editExistingPhotos.returnFuelPhoto = row.returnFuelPhoto || ''
  editExistingPhotos.issuePhotos = row.issuePhotos || ''
  resetUploadLists()
  editDialogVisible.value = true
}

const handleUploadPreview = (file) => {
  uploadPreviewUrl.value = file.url || ''
  uploadPreviewVisible.value = true
}

const handleUploadExceed = (label, limit) => {
  ElMessage.warning(`${label}最多上传${limit}张`)
}

const appendFiles = (formData, field, files) => {
  if (!Array.isArray(files)) {
    return
  }
  files.forEach(file => {
    if (file.raw) {
      formData.append(field, file.raw)
    }
  })
}

const parseDateTimeValue = (value) => {
  if (!value) return null
  const parsed = new Date(String(value).replace(/ /g, 'T'))
  return Number.isNaN(parsed.getTime()) ? null : parsed
}
const handleUpdateRecord = async (record) => {
  if (!editTargetId.value) {
    ElMessage.error('未找到需要修改的记录')
    return
  }
  if (!editForm.destination.trim()) {
    ElMessage.warning('请填写目的地/去向')
    return
  }
  if (editForm.takeMileage === null || editForm.takeMileage === undefined) {
    ElMessage.warning('请填写取车里程')
    return
  }
  if (!editForm.takeTime) {
    ElMessage.warning('请选择取车时间')
    return
  }

  const takeTimeValue = parseDateTimeValue(editForm.takeTime)
  if (!takeTimeValue) {
    ElMessage.warning('取车时间格式不正确')
    return
  }

  if (editForm.expectedReturnTime) {
    const expectedReturnTimeValue = parseDateTimeValue(editForm.expectedReturnTime)
    if (!expectedReturnTimeValue) {
      ElMessage.warning('预计还车时间格式不正确')
      return
    }
    if (expectedReturnTimeValue.getTime() <= takeTimeValue.getTime()) {
      ElMessage.warning('预计还车时间必须晚于取车时间')
      return
    }
  }
  if (editRecordStatus.value === 'RETURNED') {
    if (editForm.returnMileage === null || editForm.returnMileage === undefined) {
      ElMessage.warning('请填写还车里程')
      return
    }
    if (!editForm.returnTime) {
      ElMessage.warning('请选择还车时间')
      return
    }
    const returnTimeValue = parseDateTimeValue(editForm.returnTime)
    if (!returnTimeValue) {
      ElMessage.warning('还车时间格式不正确')
      return
    }
    if (returnTimeValue.getTime() < takeTimeValue.getTime()) {
      ElMessage.warning('还车时间不能早于取车时间')
      return
    }
    if (Number(editForm.returnMileage) < Number(editForm.takeMileage)) {
      ElMessage.warning('还车里程不能小于取车里程')
      return
    }
  }

  editLoading.value = true
  try {
    const formData = new FormData()
    formData.append('usageReason', editForm.usageReason.trim())
    formData.append('destination', editForm.destination.trim())
    formData.append('takeTime', editForm.takeTime)
    if (editForm.expectedReturnTime) {
      formData.append('expectedReturnTime', editForm.expectedReturnTime)
    }
    formData.append('takeMileage', String(editForm.takeMileage))
    if (editRecordStatus.value === 'RETURNED') {
      formData.append('returnTime', editForm.returnTime)
      formData.append('returnMileage', String(editForm.returnMileage))
      formData.append('isClean', String(editForm.isClean))
      formData.append('isFuelEnough', String(editForm.isFuelEnough))
      formData.append('issueDescription', editForm.issueDescription.trim())
    }
    appendFiles(formData, 'takeVehiclePhotos', editUploadLists.takeVehiclePhotos)
    appendFiles(formData, 'takeMileagePhotos', editUploadLists.takeMileagePhotos)
    appendFiles(formData, 'returnVehiclePhotos', editUploadLists.returnVehiclePhotos)
    appendFiles(formData, 'returnMileagePhotos', editUploadLists.returnMileagePhotos)
    appendFiles(formData, 'returnFuelPhotos', editUploadLists.returnFuelPhotos)
    appendFiles(formData, 'issuePhotos', editUploadLists.issuePhotos)

    const record = await updateBorrowRecord(editTargetId.value, formData)
    editDialogVisible.value = false
    resetUploadLists()
    currentRecord.value = record
    detailVisible.value = true
    ElMessage.success('借还车记录已更新')
    await Promise.all([fetchData(), loadEditLogs(record.id)])
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '更新借还车记录失败')
  } finally {
    editLoading.value = false
  }
}

const handleDelete = async (row) => {
  if (!row?.id) {
    ElMessage.error('未找到需要删除的记录')
    return
  }
  const confirmText = row.followUpStatus === 'PENDING'
    ? '确定要将这条借还车记录移入回收站吗？待闭环事项会一起移入，可在回收站恢复。'
    : '确定要将这条借还车记录移入回收站吗？后续可在回收站恢复。'
  try {
    const { value } = await ElMessageBox.prompt(confirmText, '移入回收站', {
      type: 'warning',
      confirmButtonText: '移入回收站',
      cancelButtonText: '取消',
      inputPlaceholder: '可选：填写删除原因，便于后续回溯',
      inputValidator: (inputValue) => {
        if (normalizeText(inputValue).length > 500) {
          return '删除原因不能超过500个字符'
        }
        return true
      }
    })
    await deleteBorrowRecord(row.id, {
      reason: normalizeText(value) || undefined
    })
    if (pagination.current > 1 && tableData.value.length === 1) {
      pagination.current -= 1
    }
    resetRecordContextById(row.id)
    ElMessage.success('借还车记录已移入回收站')
    await Promise.all([fetchData(), fetchRecycleBin()])
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || '删除失败')
    }
  }
}

const handleRestore = async (row) => {
  if (!row?.id) {
    ElMessage.error('未找到需要恢复的记录')
    return
  }
  try {
    await restoreBorrowRecord(row.id)
    if (recyclePagination.current > 1 && recycleData.value.length === 1) {
      recyclePagination.current -= 1
    }
    resetRecordContextById(row.id)
    ElMessage.success('借还车记录已恢复')
    await Promise.all([fetchRecycleBin(), fetchData()])
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '恢复失败')
  }
}

const handlePermanentDelete = async (row) => {
  if (!row?.id) {
    ElMessage.error('未找到需要永久删除的记录')
    return
  }
  try {
    await ElMessageBox.confirm('确定永久删除这条记录吗？该操作不可恢复，照片和修改日志也会清除。', '高风险操作', {
      type: 'error'
    })
    await permanentDeleteBorrowRecord(row.id)
    if (recyclePagination.current > 1 && recycleData.value.length === 1) {
      recyclePagination.current -= 1
    }
    resetRecordContextById(row.id)
    ElMessage.success('记录已永久删除')
    await Promise.all([fetchRecycleBin(), fetchData()])
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || '永久删除失败')
    }
  }
}

const handleClearRecycleBin = async () => {
  try {
    await ElMessageBox.confirm('确定清空回收站吗？所有记录将被永久删除且不可恢复。', '高风险操作', {
      type: 'error'
    })
    await clearBorrowRecycleBin()
    recyclePagination.current = 1
    detailVisible.value = false
    currentRecord.value = null
    editLogs.value = []
    ElMessage.success('回收站已清空')
    await Promise.all([fetchRecycleBin(), fetchData()])
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || '清空回收站失败')
    }
  }
}

const openFollowUpDialog = (row) => followUpDialogRef.value.open(row)

const syncRouteQueryToFilters = async (query = {}) => {
  viewMode.value = 'ACTIVE'
  if (query.status !== undefined) queryForm.status = normalizeText(query.status)
  if (query.followUpStatus !== undefined) queryForm.followUpStatus = normalizeText(query.followUpStatus)
  if (query.plateNumber !== undefined) queryForm.plateNumber = normalizeText(query.plateNumber)
  highlightRecordId.value = parsePositiveInt(query.highlightId)
  const hasRouteParams = !!(query.status || query.followUpStatus || query.plateNumber)
  if (hasRouteParams) {
    pagination.current = 1
    await fetchData()
  }
}

watch(
  () => route.query,
  (query) => {
    syncRouteQueryToFilters(query)
  }
)

const onSupplementCreated = async (record) => {
  highlightRecordId.value = record.id
  currentRecord.value = record
  detailVisible.value = true
  await Promise.all([fetchData(), loadEditLogs(record.id)])
}

const onFollowUpCompleted = async (record) => {
  currentRecord.value = record
  detailVisible.value = true
  await Promise.all([fetchData(), loadEditLogs(record.id)])
}

onMounted(() => {
  viewMode.value = 'ACTIVE'
  applyDatePreset('month', false)
  Promise.all([fetchData(), fetchRecycleBin()])
})
</script>

<style scoped>
.borrow-records {
  padding: 4px 4px 18px;
}

.borrow-table :deep(.borrow-table__row--highlight > td.el-table__cell) {
  background: #fff7e6;
}

.borrow-workbench {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(320px, 0.75fr);
  gap: 16px;
  margin-bottom: 18px;
  padding: 18px 20px;
  border-radius: 18px;
  background: #ffffff;
  border: 1px solid rgba(218, 225, 235, 0.92);
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.04);
}

.borrow-workbench__main {
  min-width: 0;
}

.borrow-workbench__label {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: #6c7a90;
}

.borrow-workbench__desc {
  margin-top: 8px;
  max-width: 760px;
  color: #66758d;
  font-size: 14px;
  line-height: 1.7;
}

.borrow-workbench__chips {
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

.hero-chip--danger {
  background: rgba(201, 76, 76, 0.08);
  border-color: rgba(201, 76, 76, 0.14);
  color: #993f4a;
}

.borrow-workbench__side {
  display: flex;
  flex-direction: column;
  gap: 12px;
  align-items: stretch;
}

.borrow-workbench__presets {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

.borrow-workbench__spotlight {
  padding: 16px 18px;
  border-radius: 16px;
  background: #f8fafc;
  border: 1px solid rgba(220, 227, 238, 0.92);
}

.borrow-workbench__spotlight-label {
  color: #6c7a90;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.borrow-workbench__spotlight-value {
  margin-top: 10px;
  font-size: 34px;
  font-weight: 800;
  line-height: 1;
  color: #182335;
}

.borrow-workbench__spotlight-desc {
  margin-top: 8px;
  color: #66758d;
  font-size: 13px;
  line-height: 1.6;
}

.overview-pill {
  border: 0;
  outline: none;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 999px;
  background: #f5f7fb;
  color: #4c607e;
  font-size: 12px;
  font-weight: 700;
  box-shadow: inset 0 0 0 1px rgba(205, 214, 228, 0.95);
  transition: background 0.2s ease, color 0.2s ease, box-shadow 0.2s ease;
}

.overview-pill:hover {
  box-shadow: inset 0 0 0 1px rgba(113, 157, 226, 0.45);
}

.overview-pill--active {
  background: rgba(43, 105, 217, 0.1);
  color: #215fd9;
  box-shadow: inset 0 0 0 1px rgba(43, 105, 217, 0.16);
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

.view-mode-tabs {
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.panel-header__actions {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.view-mode-tabs__tools {
  margin-left: auto;
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.toolbar-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 16px;
}

.toolbar-row__filters {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  flex-wrap: wrap;
}

.toolbar-search {
  width: 220px;
}

.toolbar-select {
  width: 180px;
}

.toolbar-row__actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: none;
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
  transition: border-color 0.2s ease, background 0.2s ease;
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

.borrow-table {
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
  line-height: 1.6;
}

.record-cell__sub {
  margin-top: 6px;
  color: #7c8799;
  font-size: 12px;
}

.action-required-cell {
  color: #5d6b84;
  line-height: 1.7;
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

.compact-grid {
  margin-bottom: 12px;
}

.photo-item {
  width: 140px;
  height: 140px;
}

.small-photo {
  width: 96px;
  height: 96px;
}

.empty-text {
  color: #999;
  line-height: 32px;
}

.edit-photo-group {
  margin-bottom: 20px;
  padding: 16px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fafafa;
}

.edit-photo-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 8px;
}

.upload-note {
  color: #909399;
  font-size: 12px;
  margin-bottom: 10px;
}

.upload-block :deep(.el-upload--picture-card),
.upload-block :deep(.el-upload-list__item) {
  width: 104px;
  height: 104px;
}

.log-card {
  padding: 12px 14px;
  border-radius: 8px;
  background: #f8fafc;
  border: 1px solid #ebeef5;
}

.log-operator {
  font-weight: 600;
  color: #303133;
  margin-bottom: 6px;
}

.log-summary {
  color: #606266;
  line-height: 1.6;
  word-break: break-all;
}

.preview-image {
  display: block;
  width: 100%;
  max-height: 70vh;
  object-fit: contain;
}

@media (max-width: 1440px) {
  .borrow-workbench {
    grid-template-columns: 1fr;
  }

  .borrow-workbench__presets {
    justify-content: flex-start;
  }
}

@media (max-width: 1100px) {
  .view-mode-tabs__tools {
    margin-left: 0;
  }

  .toolbar-row {
    flex-direction: column;
    align-items: stretch;
  }

  .toolbar-search,
  .toolbar-select {
    width: 100%;
  }

  .quick-filters {
    gap: 10px;
  }
}
</style>
