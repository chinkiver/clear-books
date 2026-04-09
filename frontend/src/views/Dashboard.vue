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
      <el-col :span="16">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>近6个月收支趋势</span>
              <el-text type="info" size="small">{{ sixMonthsRange }}</el-text>
            </div>
          </template>
          <div class="chart-container">
            <v-chart class="chart" :option="trendOption" autoresize />
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
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

    <!-- 账户列表 -->
    <el-card class="account-card">
      <template #header>
        <span>账户余额</span>
      </template>
      <el-row :gutter="20">
        <el-col :span="6" v-for="account in accounts" :key="account.id">
          <div class="account-item">
            <div class="account-name">{{ account.name }}</div>
            <div class="account-balance">{{ formatAmount(account.balance) }}</div>
          </div>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, PieChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent, TitleComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import dayjs from 'dayjs'
import { getSummary } from '@/api/statistics'
import { getAccounts } from '@/api/account'

use([CanvasRenderer, LineChart, PieChart, GridComponent, TooltipComponent, LegendComponent, TitleComponent])

const summary = ref({
  totalIncome: 0,
  totalExpense: 0,
  balance: 0,
  expenseByCategory: []
})
const accounts = ref([])
const trendData = ref([])

const totalAssets = computed(() => {
  return accounts.value.reduce((sum, acc) => sum + Number(acc.balance || 0), 0)
})

const formatAmount = (amount) => {
  return '¥ ' + Number(amount || 0).toFixed(2)
}

const trendOption = computed(() => {
  const months = []
  const incomeData = []
  const expenseData = []
  
  for (let i = 5; i >= 0; i--) {
    const month = dayjs().subtract(i, 'month').format('YYYY-MM')
    months.push(dayjs(month).format('M月'))
    const data = trendData.value.find(d => d.month === month)
    incomeData.push(data ? data.income : 0)
    expenseData.push(data ? data.expense : 0)
  }

  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['收入', '支出'] },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: months },
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

const sixMonthsRange = computed(() => {
  const start = dayjs().subtract(5, 'month').startOf('month').format('YYYY-MM')
  const end = dayjs().format('YYYY-MM')
  return `${start} ~ ${end}`
})

const categoryOption = computed(() => {
  const data = summary.value.expenseByCategory?.map(item => ({
    name: item.categoryName,
    value: item.amount
  })) || []

  return {
    tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
    legend: { orient: 'vertical', right: 5, top: 'center', itemWidth: 10, itemHeight: 10, textStyle: { fontSize: 12 } },
    series: [{
      type: 'pie',
      radius: ['35%', '60%'],
      center: ['35%', '50%'],
      avoidLabelOverlap: false,
      label: { show: false },
      data: data.length > 0 ? data : [{ name: '暂无数据', value: 0 }]
    }]
  }
})

const loadData = async () => {
  const currentMonth = dayjs().format('YYYY-MM')
  const res = await getSummary({ period: 'MONTH', date: currentMonth })
  summary.value = res

  const accRes = await getAccounts()
  accounts.value = accRes

  // Load trend data for past 6 months
  const trendPromises = []
  for (let i = 5; i >= 0; i--) {
    const month = dayjs().subtract(i, 'month').format('YYYY-MM')
    trendPromises.push(
      getSummary({ period: 'MONTH', date: month }).then(data => ({
        month,
        income: Number(data.totalIncome || 0),
        expense: Number(data.totalExpense || 0)
      }))
    )
  }
  trendData.value = await Promise.all(trendPromises)
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

.chart-container {
  height: 300px;
}

.chart {
  width: 100%;
  height: 100%;
}

.account-card {
  margin-top: 20px;
}

.account-item {
  text-align: center;
  padding: 15px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 10px;
}

.account-name {
  color: #606266;
  font-size: 14px;
  margin-bottom: 8px;
}

.account-balance {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
}
</style>
