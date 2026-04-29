import request from '@/utils/request'

export function getBorrowRecords(params) {
  return request({
    url: '/vehicle-borrow/records',
    method: 'get',
    params
  })
}

export function completeBorrowFollowUp(id, params) {
  return request({
    url: `/vehicle-borrow/records/${id}/complete-follow-up`,
    method: 'put',
    params
  })
}

export function updateBorrowRecord(id, data) {
  return request({
    url: `/vehicle-borrow/records/${id}`,
    method: 'put',
    data
  })
}

export function createAdminSupplementBorrowRecord(data) {
  return request({
    url: '/vehicle-borrow/records/admin-supplement',
    method: 'post',
    data
  })
}

export function getBorrowRecordEditLogs(id) {
  return request({
    url: `/vehicle-borrow/records/${id}/edit-logs`,
    method: 'get'
  })
}

export function deleteBorrowRecord(id, params) {
  return request({
    url: `/vehicle-borrow/records/${id}`,
    method: 'delete',
    params
  })
}

export function getBorrowRecycleBin(params) {
  return request({
    url: '/vehicle-borrow/records/recycle-bin',
    method: 'get',
    params
  })
}

export function restoreBorrowRecord(id) {
  return request({
    url: `/vehicle-borrow/records/${id}/restore`,
    method: 'put'
  })
}

export function permanentDeleteBorrowRecord(id) {
  return request({
    url: `/vehicle-borrow/records/${id}/permanent`,
    method: 'delete'
  })
}

export function clearBorrowRecycleBin() {
  return request({
    url: '/vehicle-borrow/records/recycle-bin/clear',
    method: 'delete'
  })
}
