import axios from 'axios'
import { useUserStore } from '@/store/user'
import router from '@/router'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

const isDefaultForbidden = (error) => {
  return error.response?.status === 403
    && !error.response?.data?.message
    && error.response?.data?.error === 'Forbidden'
}

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
    if (isDefaultForbidden(error)) {
      return '登录状态已失效，请重新登录'
    }
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
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
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
    if (error.response?.status === 401 || isDefaultForbidden(error)) {
      const userStore = useUserStore()
      userStore.logout()
      router.push('/login')
    }
    error.message = getChineseErrorMessage(error)
    return Promise.reject(error)
  }
)

export default request
