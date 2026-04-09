import request from '@/utils/request'

export const getPaymentMethods = () => request.get('/payment-methods')
export const createPaymentMethod = (data) => request.post('/payment-methods', data)
export const updatePaymentMethod = (id, data) => request.put(`/payment-methods/${id}`, data)
export const deletePaymentMethod = (id) => request.delete(`/payment-methods/${id}`)
