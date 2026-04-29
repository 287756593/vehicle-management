import request from '@/utils/request'

export function getVehicles(params) {
  return request({
    url: '/vehicles',
    method: 'get',
    params
  })
}

export async function getAllVehicles() {
  const res = await request({
    url: '/vehicles',
    method: 'get',
    params: {
      current: 1,
      size: 500
    }
  })
  return res.data || []
}

export function getVehicle(id) {
  return request({
    url: `/vehicles/${id}`,
    method: 'get'
  })
}

export function getVehicleOverview() {
  return request({
    url: '/vehicles/overview',
    method: 'get'
  })
}

export function getAvailableVehicles() {
  return request({
    url: '/vehicles/available',
    method: 'get'
  })
}

export function createVehicle(data) {
  return request({
    url: '/vehicles',
    method: 'post',
    data
  })
}

export function updateVehicle(id, data) {
  return request({
    url: `/vehicles/${id}`,
    method: 'put',
    data
  })
}

export function deleteVehicle(id) {
  return request({
    url: `/vehicles/${id}`,
    method: 'delete'
  })
}

export function updateVehicleTrafficRestrictionRelease(id, released) {
  return request({
    url: `/vehicles/${id}/traffic-restriction-release`,
    method: 'put',
    params: { released }
  })
}
