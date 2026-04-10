import request from '@/utils/request'

export const login = (data) => request.post('/auth/login', data)
export const register = (data) => request.post('/auth/register', data)
export const refreshToken = () => request.post('/auth/refresh')

// 获取验证码
export const getCaptcha = () => request.get('/captcha')
