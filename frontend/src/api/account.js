import request from '@/utils/request'

export const getAccounts = () => request.get('/accounts')
export const getAccount = (id) => request.get(`/accounts/${id}`)
export const createAccount = (data) => request.post('/accounts', data)
export const updateAccount = (id, data) => request.put(`/accounts/${id}`, data)
export const deleteAccount = (id) => request.delete(`/accounts/${id}`)
export const adjustBalance = (id, amount) => request.post(`/accounts/${id}/adjust`, null, { params: { amount } })
