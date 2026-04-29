import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')

  // 修复: 添加异常处理防止 JSON 解析失败导致应用崩溃
  let parsedUserInfo = {}
  try {
    const storedUserInfo = localStorage.getItem('userInfo')
    if (storedUserInfo) {
      parsedUserInfo = JSON.parse(storedUserInfo)
    }
  } catch (error) {
    console.error('Failed to parse userInfo from localStorage:', error)
    localStorage.removeItem('userInfo')
  }
  const userInfo = ref(parsedUserInfo)

  function setToken(newToken) {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  function setUserInfo(info) {
    userInfo.value = info
    localStorage.setItem('userInfo', JSON.stringify(info))
  }

  function logout() {
    token.value = ''
    userInfo.value = {}
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  return {
    token,
    userInfo,
    setToken,
    setUserInfo,
    logout
  }
})
