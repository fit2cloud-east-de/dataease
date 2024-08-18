import request from '@/utils/request'

export function validate(data) {
  return request({
    url: '/about/license/validate',
    method: 'post',
    data
  })
}

export function haveLicense() {
  return request({
    url: '/about/haveLicense',
    method: 'get'
  })
}

export function buildVersion() {
  return request({
    url: '/about/build/version',
    method: 'get'
  })
}

export function updateInfo(data) {
  return request({
    url: '/about/license/update',
    method: 'post',
    data
  })
}
