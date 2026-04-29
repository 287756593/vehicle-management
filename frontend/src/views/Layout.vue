<template>
  <el-container class="layout-container">
    <el-aside width="248px" class="aside">
      <div class="aside-orb aside-orb--top"></div>
      <div class="aside-orb aside-orb--bottom"></div>

      <div class="logo">
        <div class="logo-mark">
          <img :src="sacLogo" alt="Logo" class="logo-image" />
        </div>
        <div class="logo-copy">
          <span class="logo-text">公务车辆</span>
          <span class="logo-subtitle">公务车辆管理平台</span>
        </div>
      </div>

      <div class="menu-caption">功能菜单</div>

      <el-menu
        :default-active="activeMenu"
        class="menu"
        router
        :background-color="'transparent'"
        text-color="#dbe7f7"
        active-text-color="#ffffff"
      >
        <el-menu-item index="/dashboard">
          <el-icon><HomeFilled /></el-icon>
          <span>首页</span>
        </el-menu-item>

        <el-menu-item index="/half-year-report" v-if="canShow(['SUPER_ADMIN', 'OFFICE_ADMIN', 'FINANCE'])">
          <el-icon><Document /></el-icon>
          <span>半年用车报告</span>
        </el-menu-item>

        <el-menu-item index="/vehicles" v-if="canShow(['SUPER_ADMIN', 'OFFICE_ADMIN'])">
          <el-icon><Van /></el-icon>
          <span>车辆管理</span>
        </el-menu-item>

        <el-menu-item index="/drivers" v-if="canShow(['SUPER_ADMIN', 'OFFICE_ADMIN'])">
          <el-icon><UserFilled /></el-icon>
          <span>驾驶人管理</span>
        </el-menu-item>

        <el-menu-item index="/borrow-records" v-if="canShow(['SUPER_ADMIN', 'OFFICE_ADMIN'])">
          <el-icon><Tickets /></el-icon>
          <span>借还车记录</span>
        </el-menu-item>

        <el-menu-item index="/todo-center" v-if="canShow(['SUPER_ADMIN', 'OFFICE_ADMIN', 'FINANCE'])">
          <el-icon><Bell /></el-icon>
          <span>待办中心</span>
        </el-menu-item>

        <el-menu-item index="/fuel-records" v-if="canShow(['SUPER_ADMIN', 'OFFICE_ADMIN', 'FINANCE'])">
          <el-icon><OilField /></el-icon>
          <span>加油管理</span>
        </el-menu-item>

        <el-menu-item index="/maintenance-work-orders" v-if="canShow(['SUPER_ADMIN', 'OFFICE_ADMIN', 'FINANCE'])">
          <el-icon><Tools /></el-icon>
          <span>维修工单</span>
        </el-menu-item>

      </el-menu>

      <div class="aside-footer">
        <div class="aside-footer__label">当前角色 / 版本</div>
        <div class="aside-footer__value-row">
          <div class="aside-footer__value">{{ roleLabel }}</div>
          <el-tooltip content="点击查看当前版本更新内容" placement="top" v-if="currentVersionNo">
            <button
              type="button"
              class="aside-footer__version aside-footer__version-button"
              @click="openVersionDialog"
            >
              v{{ currentVersionNo }}
            </button>
          </el-tooltip>
        </div>
        <div class="aside-footer__meta">{{ todayLabel }}</div>
      </div>
    </el-aside>

    <el-container class="content-shell">
      <el-header class="header">
        <div class="header-copy">
          <div class="page-title">{{ pageTitle }}</div>
          <div class="page-meta-row">
            <span class="page-status">在线运行中</span>
            <span class="page-desc">{{ pageDescription }}</span>
          </div>
        </div>

        <div class="header-actions">
          <span class="header-runtime">{{ todayLabel }}</span>

          <el-dropdown @command="handleCommand" trigger="click">
            <div class="user-info">
              <el-avatar :size="36" class="user-avatar">
                {{ userStore.userInfo.realName?.charAt(0) || '管' }}
              </el-avatar>
              <div class="user-copy">
                <span class="user-name">{{ userStore.userInfo.realName || userStore.userInfo.username }}</span>
                <span class="user-role">{{ roleLabel }}</span>
              </div>
              <el-icon class="user-arrow"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>

  <el-dialog
    v-model="versionDialogVisible"
    width="520px"
    class="version-dialog"
    destroy-on-close
  >
    <template #header>
      <div class="version-dialog__header">
        <div class="version-dialog__eyebrow">当前版本</div>
        <div class="version-dialog__title">v{{ currentVersionNo || '-' }}</div>
      </div>
    </template>

    <div class="version-dialog__body">
      <div class="version-dialog__section">更新内容</div>
      <ul class="version-dialog__logs" v-if="currentVersionLogs.length">
        <li v-for="(line, index) in currentVersionLogs" :key="index">{{ line }}</li>
      </ul>
      <el-empty v-else description="当前版本暂无更新日志" :image-size="80" />
    </div>
  </el-dialog>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import sacLogo from '@/assets/sac-logo.svg'
import { getCurrentVersionInfo } from '@/api/versionInfo'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)
const pageTitle = computed(() => route.meta.title || '首页')
const currentVersionInfo = ref(null)
const versionDialogVisible = ref(false)
const currentVersionNo = computed(() => currentVersionInfo.value?.versionNo || '')
const currentVersionLogs = computed(() => {
  return String(currentVersionInfo.value?.changeLog || '')
    .split(/\n+/)
    .map(item => item.trim())
    .filter(Boolean)
})

const pageDescriptionMap = {
  '/dashboard': '集中查看车辆运行、提醒事项和最近借还车动态。',
  '/half-year-report': '导出近 6 个月的中文用车与加油分析报告，适合打印归档或汇报。',
  '/vehicles': '统一维护车辆档案、当前状态、证照到期和预算执行情况。',
  '/drivers': '查看驾驶员账号、联系方式和借车主体信息。',
  '/borrow-records': '处理借还车过程、闭环事项和现场照片记录。',
  '/todo-center': '集中查看借还车、加油、车辆和维修相关待办事项。',
  '/fuel-records': '跟踪现金加油报备、审批状态和补油提醒。',
  '/maintenance-work-orders': '统一管理送修、维修进度、验收结果和费用记录。',
}

const roleLabelMap = {
  SUPER_ADMIN: '超级管理员',
  OFFICE_ADMIN: '办公室管理员',
  DEPT_APPROVER: '部门审批人',
  DRIVER: '驾驶员',
  FINANCE: '财务'
}

const pageDescription = computed(() => pageDescriptionMap[route.path] || '当前页面已进入管理工作台。')
const roleLabel = computed(() => roleLabelMap[userStore.userInfo.role] || '系统用户')
const todayLabel = computed(() => new Intl.DateTimeFormat('zh-CN', {
  year: 'numeric',
  month: 'long',
  day: 'numeric',
  weekday: 'short'
}).format(new Date()))

const canShow = (roles) => roles.includes(userStore.userInfo.role)

const loadCurrentVersion = async () => {
  try {
    const res = await getCurrentVersionInfo()
    currentVersionInfo.value = res || null
  } catch (error) {
    currentVersionInfo.value = null
  }
}

const openVersionDialog = () => {
  if (!currentVersionNo.value) return
  versionDialogVisible.value = true
}

const handleCommand = (command) => {
  if (command === 'logout') {
    userStore.logout()
    router.push('/login')
  }
}

onMounted(() => {
  loadCurrentVersion()
})
</script>

<style scoped>
.layout-container {
  min-height: 100vh;
  background: transparent;
}

.aside {
  position: relative;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, #17304f 0%, #1b3a5d 100%);
  border-right: 1px solid rgba(255, 255, 255, 0.08);
  overflow: hidden;
}

.aside::before {
  content: "";
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.05), transparent 24%);
  pointer-events: none;
}

.aside-orb {
  display: none;
}

.aside-orb--top {
  width: 200px;
  height: 200px;
  top: -50px;
  right: -40px;
  background: radial-gradient(circle, rgba(66, 153, 225, 0.75), transparent 68%);
}

.aside-orb--bottom {
  width: 170px;
  height: 170px;
  bottom: 60px;
  left: -60px;
  background: radial-gradient(circle, rgba(22, 163, 74, 0.48), transparent 72%);
}

.logo {
  position: relative;
  display: flex;
  align-items: center;
  gap: 14px;
  margin: 18px 16px 12px;
  padding: 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.logo-mark {
  width: 54px;
  height: 54px;
  border-radius: 14px;
  background: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.logo-image {
  width: 38px;
  height: auto;
  display: block;
}

.logo-copy {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.logo-text {
  color: #ffffff;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.logo-subtitle {
  color: rgba(218, 229, 247, 0.72);
  font-size: 12px;
}

.menu-caption {
  position: relative;
  margin: 8px 24px 6px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: rgba(196, 209, 230, 0.56);
}

.menu {
  position: relative;
  flex: 1;
  border-right: none;
  background: transparent;
  padding: 2px 0 18px;
}

.menu :deep(.el-menu-item) {
  margin: 6px 12px;
  height: 48px;
  border-radius: 12px;
  transition: background 0.2s ease, color 0.2s ease;
}

.menu :deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.08) !important;
}

.menu :deep(.el-menu-item.is-active) {
  background: rgba(69, 135, 240, 0.22) !important;
  box-shadow: inset 0 0 0 1px rgba(148, 192, 255, 0.18);
}

.menu :deep(.el-menu-item .el-icon) {
  font-size: 18px;
}

.aside-footer {
  position: relative;
  margin: 0 16px 16px;
  padding: 16px 18px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.aside-footer__label {
  font-size: 12px;
  letter-spacing: 0.08em;
  color: rgba(206, 218, 235, 0.58);
}

.aside-footer__value {
  color: #ffffff;
  font-size: 16px;
  font-weight: 700;
}

.aside-footer__value-row {
  margin-top: 8px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.aside-footer__version {
  flex: none;
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  color: #eff7ff;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.12);
}

.aside-footer__version-button {
  cursor: pointer;
  transition: transform 0.2s ease, background 0.2s ease, box-shadow 0.2s ease;
}

.aside-footer__version-button:hover {
  background: rgba(255, 255, 255, 0.14);
}

.aside-footer__meta {
  margin-top: 4px;
  color: rgba(221, 229, 241, 0.7);
  font-size: 12px;
}

.version-dialog :deep(.el-dialog) {
  border-radius: 18px;
  padding: 4px 4px 8px;
  background: #ffffff;
}

.version-dialog :deep(.el-dialog__header) {
  margin-right: 0;
  padding: 10px 12px 0;
}

.version-dialog :deep(.el-dialog__body) {
  padding: 8px 12px 12px;
}

.version-dialog__header {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.version-dialog__eyebrow {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: #7c8798;
}

.version-dialog__title {
  font-size: 28px;
  font-weight: 800;
  color: #172033;
}

.version-dialog__body {
  border-radius: 14px;
  padding: 18px 18px 8px;
  background: #f8fafc;
  border: 1px solid rgba(221, 228, 238, 0.9);
}

.version-dialog__section {
  font-size: 13px;
  font-weight: 700;
  color: #526278;
}

.version-dialog__logs {
  margin: 14px 0 0;
  padding-left: 18px;
  color: #26364d;
  line-height: 1.8;
}

.version-dialog__logs li + li {
  margin-top: 8px;
}

.content-shell {
  min-width: 0;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 14px;
  padding: 14px 18px;
  margin: 12px 18px 0;
  border-radius: 16px;
  background: #ffffff;
  box-shadow: 0 6px 16px rgba(15, 23, 42, 0.04);
  border: 1px solid rgba(226, 232, 240, 0.8);
}

.header-copy {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.page-meta-row {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  flex-wrap: wrap;
}

.page-status {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(22, 121, 95, 0.08);
  border: 1px solid rgba(22, 121, 95, 0.14);
  color: #126c57;
  font-size: 12px;
  font-weight: 700;
}

.page-title {
  font-size: 20px;
  font-weight: 800;
  color: #172033;
  letter-spacing: 0.01em;
}

.page-desc {
  color: #667489;
  font-size: 13px;
  line-height: 1.5;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.header-runtime {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  color: #49607f;
  background: #f8fafc;
  border: 1px solid rgba(210, 219, 232, 0.9);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  padding: 6px 10px;
  border-radius: 12px;
  transition: background 0.2s ease, border-color 0.2s ease;
  border: 1px solid rgba(214, 222, 235, 0.88);
  background: #ffffff;
}

.user-info:hover {
  background: #f8fafc;
}

.user-avatar {
  background: linear-gradient(135deg, #2b69d9 0%, #2f7be7 100%);
  font-weight: 700;
}

.user-copy {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.user-name {
  font-size: 14px;
  color: #31435f;
  font-weight: 700;
}

.user-role {
  font-size: 12px;
  color: #7b879a;
}

.user-arrow {
  color: #73829a;
}

.main {
  padding: 20px 20px 28px;
  background: transparent;
  min-height: calc(100vh - 144px);
}

@media (max-width: 1180px) {
  .header {
    flex-direction: column;
    align-items: flex-start;
  }

  .header-actions {
    width: 100%;
    justify-content: flex-start;
  }
}

@media (max-width: 860px) {
  .layout-container {
    flex-direction: column;
  }

  .aside {
    width: 100% !important;
  }

  .menu-caption {
    margin-bottom: 0;
  }

  .menu :deep(.el-menu) {
    display: flex;
    flex-wrap: wrap;
    padding: 0 10px 10px;
  }

  .menu :deep(.el-menu-item) {
    width: calc(50% - 12px);
    margin: 8px 6px 0;
  }

  .aside-footer {
    margin-top: 4px;
  }
}

@media (max-width: 640px) {
  .header {
    margin: 12px 12px 0;
    padding: 14px 14px 12px;
  }

  .page-title {
    font-size: 18px;
  }

  .page-meta-row,
  .header-actions {
    width: 100%;
  }

  .user-info {
    width: 100%;
    justify-content: space-between;
  }

  .main {
    padding: 16px 12px 24px;
  }
}
</style>
