import request from '@/utils/request'

export function getTodoCenterSummary() {
  return request({
    url: '/todo-center/summary',
    method: 'get'
  })
}

export function getTodoCenterItems(params) {
  return request({
    url: '/todo-center/items',
    method: 'get',
    params
  })
}
