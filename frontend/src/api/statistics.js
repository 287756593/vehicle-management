import request from '@/utils/request'

export function getHalfYearReport(params) {
  return request({
    url: '/statistics/half-year-report',
    method: 'get',
    params
  })
}
