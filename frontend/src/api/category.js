import request from '@/utils/request'

export const getCategories = (type) => request.get('/categories', { params: { type, tree: true } })
export const createCategory = (data) => request.post('/categories', data)
export const updateCategory = (id, data) => request.put(`/categories/${id}`, data)
export const deleteCategory = (id) => request.delete(`/categories/${id}`)
