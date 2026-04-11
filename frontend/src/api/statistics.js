import request from '@/utils/request'

export const getSummary = (params) => request.get('/statistics/summary', { params })
export const getTrend = (params) => request.get('/statistics/trend', { params })
export const getByCategory = (params) => request.get('/statistics/by-category', { params })
export const getBalance = (params) => request.get('/statistics/balance', { params })
export const getWeekdayAnalysis = (params) => request.get('/statistics/weekday-analysis', { params })
export const getExpenseForecast = () => request.get('/statistics/expense-forecast')
