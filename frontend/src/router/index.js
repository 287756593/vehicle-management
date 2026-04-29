import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'

const APP_NAME = '公务车辆管理系统'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/views/Layout.vue'),
    children: [
      {
        path: '',
        redirect: '/dashboard'
      },
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '首页', roles: ['SUPER_ADMIN', 'OFFICE_ADMIN', 'DEPT_APPROVER', 'DRIVER', 'FINANCE'] }
      },
      {
        path: 'half-year-report',
        name: 'HalfYearReport',
        component: () => import('@/views/HalfYearReport.vue'),
        meta: { title: '半年用车报告', roles: ['SUPER_ADMIN', 'OFFICE_ADMIN', 'FINANCE'] }
      },

      {
        path: 'vehicles',
        name: 'Vehicles',
        component: () => import('@/views/Vehicles.vue'),
        meta: { title: '车辆管理', roles: ['SUPER_ADMIN', 'OFFICE_ADMIN'] }
      },
      {
        path: 'drivers',
        name: 'Drivers',
        component: () => import('@/views/Drivers.vue'),
        meta: { title: '驾驶人管理', roles: ['SUPER_ADMIN', 'OFFICE_ADMIN'] }
      },
      {
        path: 'borrow-records',
        name: 'BorrowRecords',
        component: () => import('@/views/BorrowRecords.vue'),
        meta: { title: '借还车记录', roles: ['SUPER_ADMIN', 'OFFICE_ADMIN'] }
      },
      {
        path: 'todo-center',
        name: 'TodoCenter',
        component: () => import('@/views/TodoCenter.vue'),
        meta: { title: '待办中心', roles: ['SUPER_ADMIN', 'OFFICE_ADMIN', 'FINANCE'] }
      },
      {
        path: 'fuel-records',
        name: 'FuelRecords',
        component: () => import('@/views/FuelRecords.vue'),
        meta: { title: '加油管理', roles: ['SUPER_ADMIN', 'OFFICE_ADMIN', 'FINANCE'] }
      },
      {
        path: 'maintenance-work-orders',
        name: 'MaintenanceWorkOrders',
        component: () => import('@/views/MaintenanceWorkOrders.vue'),
        meta: { title: '维修工单', roles: ['SUPER_ADMIN', 'OFFICE_ADMIN', 'FINANCE'] }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  const token = localStorage.getItem('token')
  const role = userStore.userInfo.role

  if (to.path !== '/login' && !token) {
    next('/login')
  } else if (to.path === '/login' && token) {
    next('/')
  } else if (to.meta.roles && !to.meta.roles.includes(role)) {
    // 修复: 当 role 为空或不在允许列表时,都重定向到 dashboard
    next('/dashboard')
  } else {
    next()
  }
})

router.afterEach((to) => {
  document.title = to.meta?.title ? `${to.meta.title} - ${APP_NAME}` : APP_NAME
})

export default router
