import request from '@/utils/request'

const API_PREFIX = '/maintenance/work-orders'

const isEmpty = (value) => value === undefined || value === null || value === ''

const cleanParams = (params = {}) => {
  const cleaned = {}
  Object.entries(params).forEach(([key, value]) => {
    if (isEmpty(value)) {
      return
    }
    cleaned[key] = value
  })
  return cleaned
}

const hasFileLikeValue = (value) => {
  if (!value) {
    return false
  }
  if (Array.isArray(value)) {
    return value.some(item => item instanceof File || item instanceof Blob)
  }
  return value instanceof File || value instanceof Blob
}

const toFormData = (payload = {}) => {
  const formData = new FormData()
  Object.entries(payload).forEach(([key, value]) => {
    if (isEmpty(value)) {
      return
    }
    if (Array.isArray(value)) {
      value.forEach(item => {
        if (!isEmpty(item)) {
          formData.append(key, item)
        }
      })
      return
    }
    formData.append(key, value)
  })
  return formData
}

const requestWithFallbacks = async (candidates) => {
  let lastError = null
  for (const candidate of candidates) {
    try {
      // eslint-disable-next-line no-await-in-loop
      return await request(candidate)
    } catch (error) {
      lastError = error
      if (![404, 405].includes(error.response?.status)) {
        throw error
      }
    }
  }
  throw lastError || new Error('请求失败')
}

export function getMaintenanceWorkOrders(params) {
  return request({
    url: API_PREFIX,
    method: 'get',
    params: cleanParams(params)
  })
}

export function getMaintenanceWorkOrder(id) {
  return request({
    url: `${API_PREFIX}/${id}`,
    method: 'get'
  })
}

export function createMaintenanceWorkOrder(data) {
  const payload = cleanParams(data)
  const hasFiles = hasFileLikeValue(payload.issuePhotos)
  if (hasFiles) {
    return request({
      url: API_PREFIX,
      method: 'post',
      data: toFormData(payload),
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }
  return request({
    url: API_PREFIX,
    method: 'post',
    data: payload
  })
}

export function createMaintenanceFromBorrow(recordId, data) {
  const payload = cleanParams(data)
  const hasFiles = hasFileLikeValue(payload.issuePhotos)
  if (hasFiles) {
    return request({
      url: `${API_PREFIX}/from-borrow-record/${recordId}`,
      method: 'post',
      data: toFormData(payload),
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }
  return request({
    url: `${API_PREFIX}/from-borrow-record/${recordId}`,
    method: 'post',
    data: payload
  })
}

export function submitMaintenanceWorkOrder(id, remark) {
  return requestWithFallbacks([
    {
      url: `${API_PREFIX}/${id}/submit`,
      method: 'put',
      params: cleanParams({ remark })
    },
    {
      url: `${API_PREFIX}/${id}/submit-approval`,
      method: 'put',
      params: cleanParams({ remark })
    }
  ])
}

export function approveMaintenanceWorkOrder(id, approved, comment) {
  if (approved) {
    return request({
      url: `${API_PREFIX}/${id}/approve`,
      method: 'put',
      params: cleanParams({ comment })
    })
  }
  return requestWithFallbacks([
    {
      url: `${API_PREFIX}/${id}/reject`,
      method: 'put',
      params: cleanParams({ comment, remark: comment })
    },
    {
      url: `${API_PREFIX}/${id}/approve`,
      method: 'put',
      params: cleanParams({ approved: false, comment, remark: comment })
    }
  ])
}

export function startRepair(id, payload = {}) {
  return request({
    url: `${API_PREFIX}/${id}/start-repair`,
    method: 'put',
    data: cleanParams(payload)
  })
}

export function finishRepair(id, payload = {}) {
  const cleaned = cleanParams(payload)
  const hasFiles = hasFileLikeValue(cleaned.repairPhotos)
  if (hasFiles) {
    return request({
      url: `${API_PREFIX}/${id}/finish-repair`,
      method: 'put',
      data: toFormData(cleaned),
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }
  return request({
    url: `${API_PREFIX}/${id}/finish-repair`,
    method: 'put',
    data: cleaned
  })
}

export function acceptRepair(id, payload = {}) {
  const cleaned = cleanParams(payload)
  const hasFiles = hasFileLikeValue(cleaned.invoicePhotos) || hasFileLikeValue(cleaned.acceptancePhotos)
  if (hasFiles) {
    return request({
      url: `${API_PREFIX}/${id}/accept`,
      method: 'put',
      data: toFormData(cleaned),
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }
  return request({
    url: `${API_PREFIX}/${id}/accept`,
    method: 'put',
    data: cleaned
  })
}

export function cancelMaintenance(id, reason) {
  return request({
    url: `${API_PREFIX}/${id}/cancel`,
    method: 'put',
    params: cleanParams({
      reason,
      remark: reason
    })
  })
}
