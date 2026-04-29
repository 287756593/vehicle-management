import axios from 'axios'
import { useDriverStore } from '@/store/driver'
import router from '@/router'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

const getChineseErrorMessage = (error) => {
  const status = error.response?.status
  const backendMessage = error.response?.data?.message
  if (backendMessage) {
    return backendMessage
  }
  if (status === 401) {
    return '登录状态已失效，请重新登录'
  }
  if (status === 403) {
    return '没有权限执行该操作'
  }
  if (status === 404) {
    return '请求的接口不存在'
  }
  if (status >= 500) {
    return '系统处理失败，请稍后重试'
  }
  if (error.code === 'ECONNABORTED') {
    return '请求超时，请稍后重试'
  }
  if (error.message === 'Network Error') {
    return '网络异常，请检查连接后重试'
  }
  return '请求失败，请稍后重试'
}

request.interceptors.request.use(
  config => {
    const driverStore = useDriverStore()
    const isPublicDriverEndpoint = config.url === '/drivers/list' || config.url === '/auth/driver-login'

    if (!isPublicDriverEndpoint) {
      // Check if token is expired before sending request
      if (driverStore.token && driverStore.isTokenExpired()) {
        const error = new Error('登录状态已过期，请重新登录')
        error.isTokenExpired = true
        driverStore.logout()
        router.push('/login').catch(() => {})
        return Promise.reject(error)
      }
      if (driverStore.token) {
        config.headers.Authorization = `Bearer ${driverStore.token}`
      }
    }

    return config
  },
  error => {
    return Promise.reject(error)
  }
)

request.interceptors.response.use(
  response => {
    return response.data
  },
  error => {
    const status = error.response?.status
    if (status === 401) {
      const driverStore = useDriverStore()
      driverStore.logout()
      router.push('/login').catch(() => {})
    }
    if (status === 403 || status === 401) {
      const driverStore = useDriverStore()
      const url = error.config?.url || 'unknown'
      const method = (error.config?.method || 'unknown').toUpperCase()
      const data = error.response?.data
      console.error(`[${status}] ${method} /api${url}`, data)
      window.__last403 = { method, url, data, status }
      // Treat 401/403 as session expired, force re-login
      driverStore.logout()
      router.push('/login').catch(() => {})
      const msg = data?.message || (status === 401 ? '登录状态已失效，请重新登录' : '登录状态已失效，请重新登录')
      error.message = msg
      return Promise.reject(error)
    }
    error.message = getChineseErrorMessage(error)
    return Promise.reject(error)
  }
)

export default request

export function getDrivers() {
  return request({
    url: '/drivers/list',
    method: 'get'
  })
}

export function driverLogin(data) {
  return request({
    url: '/auth/driver-login',
    method: 'post',
    data
  })
}

export function getAvailableVehicles() {
  return request({
    url: '/vehicles/available',
    method: 'get'
  })
}

export function getCurrentBorrowRecord() {
  return request({
    url: '/vehicle-borrow/current',
    method: 'get'
  })
}

export function getMyBorrowRecords(params = {}) {
  return request({
    url: '/vehicle-borrow/my-records',
    method: 'get',
    params
  })
}

export function getBorrowRecords(params = {}) {
  return request({
    url: '/vehicle-borrow/records',
    method: 'get',
    params
  })
}

export function getAllVehicles() {
  return request({
    url: '/vehicles',
    method: 'get',
    params: { current: 1, size: 100 }
  })
}

export function getVehicleActivities(params = {}) {
  return request({
    url: '/driver/vehicle-activity',
    method: 'get',
    params
  })
}

export function createFuelRecord(data) {
  return request({
    url: '/fuel-records/with-photos',
    method: 'post',
    data,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export function takeVehicle(formData) {
  return request({
    url: '/vehicle-borrow/take',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export function returnVehicle(formData) {
  return request({
    url: '/vehicle-borrow/return',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export function getMyMileageStats(days = 30) {
  return request({
    url: '/vehicle-borrow/statistics/mileage',
    method: 'get',
    params: { days }
  })
}

export function getMyFuelRecords(params = {}) {
  return request({
    url: '/fuel-records',
    method: 'get',
    params: {
      current: 1,
      size: 100,
      ...params
    }
  })
}
