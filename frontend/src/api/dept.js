import request from '@/utils/request'

export function getDepts() {
  return request({
    url: '/depts',
    method: 'get'
  })
}

export function createDept(data) {
  return request({
    url: '/depts',
    method: 'post',
    data
  })
}

export function deleteDept(id) {
  return request({
    url: `/depts/${id}`,
    method: 'delete'
  })
}
