import request from '@/utils/request'

export function getVersionList() {
  return request({
    url: '/system-versions',
    method: 'get'
  })
}

export function getCurrentVersionInfo() {
  return request({
    url: '/system-versions/current',
    method: 'get'
  })
}
