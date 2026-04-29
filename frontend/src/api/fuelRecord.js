import request from '@/utils/request'

export function getFuelRecords(params) {
  return request({
    url: '/fuel-records',
    method: 'get',
    params
  })
}

export function getFuelRecord(id) {
  return request({
    url: `/fuel-records/${id}`,
    method: 'get'
  })
}

export function createFuelRecord(data) {
  return request({
    url: '/fuel-records',
    method: 'post',
    data
  })
}

export function deleteFuelRecord(id) {
  return request({
    url: `/fuel-records/${id}`,
    method: 'delete'
  })
}

export function restoreFuelRecord(id) {
  return request({
    url: `/fuel-records/${id}/restore`,
    method: 'put'
  })
}

export function permanentDeleteFuelRecord(id) {
  return request({
    url: `/fuel-records/${id}/permanent`,
    method: 'delete'
  })
}

export function getRecycleBin(params) {
  return request({
    url: '/fuel-records/recycle-bin',
    method: 'get',
    params
  })
}

export function clearRecycleBin() {
  return request({
    url: '/fuel-records/recycle-bin/clear',
    method: 'delete'
  })
}

export function approveCashReport(id, data) {
  return request({
    url: `/fuel-records/${id}/approve-cash-report`,
    method: 'put',
    params: data
  })
}

export function updateReimbursementStatus(id, data) {
  return request({
    url: `/fuel-records/${id}/reimbursement-status`,
    method: 'put',
    params: data
  })
}

export function updateFuelAmount(id, data) {
  return request({
    url: `/fuel-records/${id}/fuel-amount`,
    method: 'put',
    data
  })
}

export function getFuelStatistics(vehicleId, year) {
  return request({
    url: `/fuel-records/statistics/vehicle/${vehicleId}`,
    method: 'get',
    params: { year }
  })
}

export function getVehicleYearlyFuelSummary(year) {
  return request({
    url: '/fuel-records/statistics/vehicles',
    method: 'get',
    params: { year }
  })
}
