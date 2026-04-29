import request from '@/utils/request'

export function getDrivers(params) {
  return request({
    url: '/drivers',
    method: 'get',
    params
  })
}

export function getDriver(id) {
  return request({
    url: `/drivers/${id}`,
    method: 'get'
  })
}

export function getActiveDrivers() {
  return request({
    url: '/drivers/active',
    method: 'get'
  })
}

export function createDriver(data) {
  return request({
    url: '/drivers',
    method: 'post',
    data
  })
}

export function updateDriver(id, data) {
  return request({
    url: `/drivers/${id}`,
    method: 'put',
    data
  })
}

export function updateDriverSortOrder(id, data) {
  return request({
    url: `/drivers/${id}/sort-order`,
    method: 'put',
    data
  })
}

export function deleteDriver(id) {
  return request({
    url: `/drivers/${id}`,
    method: 'delete'
  })
}
