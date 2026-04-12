import request from '@/utils/request'

export const getTransactions = (params) => request.get('/transactions', { params })
export const getTransaction = (id) => request.get(`/transactions/${id}`)
export const createTransaction = (data) => request.post('/transactions', data)
export const updateTransaction = (id, data) => request.put(`/transactions/${id}`, data)
export const deleteTransaction = (id) => request.delete(`/transactions/${id}`)
export const getTags = () => request.get('/transactions/tags')
export const getTagsWithCount = () => request.get('/transactions/tags/with-count')
export const addPresetTag = (tagName) => request.post('/transactions/tags/preset', null, { params: { tagName } })
export const updateTagColor = (tagName, color) => request.put('/transactions/tags/color', null, { params: { tagName, color } })
export const renameTag = (oldName, newName) => request.put('/transactions/tags/rename', null, { params: { oldName, newName } })
export const deleteTag = (tagName) => request.delete(`/transactions/tags/${encodeURIComponent(tagName)}`)
