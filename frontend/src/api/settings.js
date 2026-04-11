import request from '@/utils/request'

// 获取公开的系统信息（无需登录）
export const getPublicInfo = () => request.get('/settings/public')

// 获取系统名称（无需登录）
export const getSystemName = () => request.get('/settings/system-name')

// 获取所有设置（需要管理员权限）
export const getAllSettings = () => request.get('/settings')

// 更新单个设置
export const updateSetting = (data) => request.put('/settings', data)

// 批量更新设置
export const updateSettings = (data) => request.put('/settings/batch', data)

// 上传 Logo 图片
export const uploadLogo = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/files/logo', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 上传 Favicon 图标
export const uploadIcon = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/files/icon', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
