<template>
  <div class="dashboard">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stat-cards">
      <el-col :span="6">
        <el-card class="stat-card income">
          <div class="stat-label">本月收入</div>
          <div class="stat-value income-text">{{ formatAmount(summary.totalIncome) }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card expense">
          <div class="stat-label">本月支出</div>
          <div class="stat-value expense-text">{{ formatAmount(summary.totalExpense) }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card balance">
          <div class="stat-label">本月结余</div>
          <div class="stat-value" :class="summary.balance >= 0 ? 'income-text' : 'expense-text'">
            {{ formatAmount(summary.balance) }}
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-label">账户总资产</div>
          <div class="stat-value primary-text">{{ formatAmount(totalAssets) }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" class="chart-row">
      <el-col :span="24">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>{{ trendTitle }}</span>
              <div class="header-actions">
                <el-radio-group v-model="trendViewType" size="small" @change="loadTrendData">
                  <el-radio-button value="month">按月</el-radio-button>
                  <el-radio-button value="day">按日</el-radio-button>
                </el-radio-group>
                <el-text type="info" size="small" style="margin-left: 12px;">{{ trendDateRange }}</el-text>
              </div>
            </div>
          </template>
          <div class="chart-container">
            <v-chart class="chart" :option="trendOption" autoresize />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="chart-row">
      <el-col :span="24">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>{{ currentMonthText }}支出分类</span>
              <el-text type="info" size="small">{{ monthDateRange }}</el-text>
            </div>
          </template>
          <div class="chart-container">
            <v-chart class="chart" :option="categoryOption" autoresize />
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent, TitleComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import dayjs from 'dayjs'
import { getSummary, getTrend } from '@/api/statistics'
import { getAccounts } from '@/api/account'

use([CanvasRenderer, LineChart, BarChart, GridComponent, TooltipComponent, LegendComponent, TitleComponent])

const summary = ref({
  totalIncome: 0,
  totalExpense: 0,
  balance: 0,
  expenseByCategory: []
})
const trendData = ref([])
const trendViewType = ref('month')
const accounts = ref([])

const totalAssets = computed(() => {
  return accounts.value.reduce((sum, acc) => sum + Number(acc.balance || 0), 0)
})

const formatAmount = (amount) => {
  return '¥ ' + Number(amount || 0).toFixed(2)
}

const trendTitle = computed(() => {
  return trendViewType.value === 'month' ? '近6个月收支趋势' : '近30天收支趋势'
})

const trendDateRange = computed(() => {
  if (trendViewType.value === 'month') {
    const start = dayjs().subtract(5, 'month').startOf('month').format('YYYY-MM')
    const end = dayjs().format('YYYY-MM')
    return `${start} ~ ${end}`
  } else {
    const start = dayjs().subtract(29, 'day').format('MM-DD')
    const end = dayjs().format('MM-DD')
    return `${start} ~ ${end}`
  }
})

const trendOption = computed(() => {
  const labels = trendData.value.map(d => d.label)
  const incomeData = trendData.value.map(d => d.income)
  const expenseData = trendData.value.map(d => d.expense)

  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['收入', '支出'] },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { 
      type: 'category', 
      data: labels,
      axisLabel: { rotate: trendViewType.value === 'day' ? 45 : 0 }
    },
    yAxis: { type: 'value' },
    series: [
      { name: '收入', type: 'line', data: incomeData, smooth: true, itemStyle: { color: '#F5222D' } },
      { name: '支出', type: 'line', data: expenseData, smooth: true, itemStyle: { color: '#52C41A' } }
    ]
  }
})

const currentMonthText = computed(() => {
  return dayjs().format('YYYY年M月')
})

const monthDateRange = computed(() => {
  const start = dayjs().startOf('month').format('MM-DD')
  const end = dayjs().endOf('month').format('MM-DD')
  return `${start} ~ ${end}`
})

const categoryOption = computed(() => {
  // 按金额降序排列，取前15个分类
  const sortedData = (summary.value.expenseByCategory || [])
    .slice()
    .sort((a, b) => b.amount - a.amount)
    .slice(0, 15)
  
  const categories = sortedData.map(item => item.categoryName)
  const values = sortedData.map(item => Number(item.amount || 0))

  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, formatter: '{b}: ¥{c}' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { 
      type: 'category', 
      data: categories,
      axisLabel: { fontSize: 12, rotate: 30 }
    },
    yAxis: { type: 'value' },
    series: [{
      type: 'bar',
      data: values,
      itemStyle: { 
        color: (params) => {
          const val = params.value
          if (val > 1000) return '#F56C6C'
          if (val > 500) return '#E6A23C'
          return '#52C41A'
        },
        borderRadius: [4, 4, 0, 0]
      },
      label: { show: true, position: 'top', formatter: '¥{c}' }
    }]
  }
})

const loadTrendData = async () => {
  let startDate, endDate, groupBy
  if (trendViewType.value === 'month') {
    startDate = dayjs().subtract(5, 'month').startOf('month').format('YYYY-MM-DD')
    endDate = dayjs().endOf('month').format('YYYY-MM-DD')
    groupBy = 'MONTH'
  } else {
    startDate = dayjs().subtract(29, 'day').format('YYYY-MM-DD')
    endDate = dayjs().format('YYYY-MM-DD')
    groupBy = 'DAY'
  }

  const [incomeRes, expenseRes] = await Promise.all([
    getTrend({ type: 'INCOME', startDate, endDate, groupBy }),
    getTrend({ type: 'EXPENSE', startDate, endDate, groupBy })
  ])

  const incomeMap = new Map((incomeRes.data || []).map(d => [d.date, Number(d.amount || 0)]))
  const expenseMap = new Map((expenseRes.data || []).map(d => [d.date, Number(d.amount || 0)]))

  // 生成完整的日期/月份列表，补齐无数据的日期
  const filledKeys = []
  if (trendViewType.value === 'month') {
    for (let i = 5; i >= 0; i--) {
      filledKeys.push(dayjs().subtract(i, 'month').format('YYYY-MM'))
    }
  } else {
    let current = dayjs(startDate)
    const last = dayjs(endDate)
    while (!current.isAfter(last)) {
      filledKeys.push(current.format('YYYY-MM-DD'))
      current = current.add(1, 'day')
    }
  }

  trendData.value = filledKeys.map(key => ({
    label: trendViewType.value === 'month' ? dayjs(key + '-01').format('YYYY年M月') : dayjs(key).format('MM-DD'),
    income: incomeMap.get(key) || 0,
    expense: expenseMap.get(key) || 0
  }))
}

const loadData = async () => {
  const currentMonth = dayjs().format('YYYY-MM')
  const res = await getSummary({ period: 'MONTH', date: currentMonth })
  summary.value = res

  const accRes = await getAccounts()
  accounts.value = accRes

  await loadTrendData()
}

onMounted(loadData)
</script>

<style scoped>
.dashboard {
  padding-bottom: 20px;
}

.stat-cards {
  margin-bottom: 20px;
}

.stat-card {
  text-align: center;
}

.stat-label {
  color: #909399;
  font-size: 14px;
  margin-bottom: 10px;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
}

.income-text {
  color: #F5222D;
}

.expense-text {
  color: #52C41A;
}

.primary-text {
  color: #409EFF;
}

.chart-row {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  align-items: center;
}

.chart-container {
  height: 300px;
}

.chart {
  width: 100%;
  height: 100%;
}
</style>
