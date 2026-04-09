import request from '@/utils/request'

export const getProfile = () => request.get('/user/profile')
export const changePassword = (params) => request.put('/user/password', null, { params })
