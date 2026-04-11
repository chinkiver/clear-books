<template>
  <div class="statistics">
    <!-- 筛选栏 -->
    <el-card class="filter-card">
      <el-form :inline="true" :model="queryForm">
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            @change="handleDateChange"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 关键指标卡片 -->
    <el-row :gutter="20" class="kpi-row">
      <el-col :span="6">
        <el-card class="kpi-card expense">
          <div class="kpi-label">总支出</div>
          <div class="kpi-value">¥ {{ formatNumber(summary.expense) }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="kpi-card income">
          <div class="kpi-label">总收入</div>
          <div class="kpi-value">¥ {{ formatNumber(summary.income) }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="kpi-card balance">
          <div class="kpi-label">净结余</div>
          <div class="kpi-value" :class="{ negative: summary.balance < 0 }">¥ {{ formatNumber(summary.balance) }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="kpi-card avg">
          <div class="kpi-label">日均支出</div>
          <div class="kpi-value">¥ {{ formatNumber(summary.avgDailyExpense) }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 本月支出预测 -->
    <el-card class="forecast-card" v-if="expenseForecast.currentMonth">
      <template #header>
        <div class="chart-header">
          <span>
            <el-icon><TrendCharts /></el-icon>
            {{ expenseForecast.currentMonth }} 支出预测
          </span>
          <el-tag :type="getForecastStatusType(expenseForecast.status)" size="small">
            {{ expenseForecast.statusText }}
          </el-tag>
        </div>
      </template>
      <el-row :gutter="20">
        <el-col :span="16">
          <div class="chart-container" style="height: 280px;">
            <v-chart class="chart" :option="forecastChartOption" autoresize />
          </div>
        </el-col>
        <el-col :span="8">
          <div class="forecast-summary">
            <div class="forecast-item">
              <div class="forecast-label">本月已过</div>
              <div class="forecast-value">{{ expenseForecast.passedDays }}/{{ expenseForecast.totalDays }} 天</div>
              <el-progress :percentage="Math.round(expenseForecast.passedDays / expenseForecast.totalDays * 100)" :show-text="false" :stroke-width="8" color="#409EFF" />
            </div>
            <div class="forecast-item">
              <div class="forecast-label">当前支出</div>
              <div class="forecast-value expense">¥ {{ formatNumber(expenseForecast.actualExpense) }}</div>
            </div>
            <div class="forecast-item">
              <div class="forecast-label">日均支出</div>
              <div class="forecast-value">¥ {{ formatNumber(expenseForecast.dailyAverage) }}</div>
            </div>
            <div class="forecast-item">
              <div class="forecast-label">预计总支出</div>
              <div class="forecast-value warning">¥ {{ formatNumber(expenseForecast.forecastExpense) }}</div>
              <div class="forecast-range">
                区间: ¥{{ formatNumber(expenseForecast.forecastRangeLow) }} - ¥{{ formatNumber(expenseForecast.forecastRangeHigh) }}
              </div>
            </div>
            <div class="forecast-item" v-if="expenseForecast.lastMonthTotal > 0">
              <div class="forecast-label">上月总支出</div>
              <div class="forecast-value">¥ {{ formatNumber(expenseForecast.lastMonthTotal) }}</div>
              <div class="forecast-compare" :class="getForecastCompareClass()">
                {{ getForecastCompareText() }}
              </div>
            </div>
          </div>
        </el-col>
      </el-row>
      <el-divider />
      <div class="forecast-suggestion">
        <el-icon class="suggestion-icon"><InfoFilled /></el-icon>
        <span>{{ expenseForecast.suggestion }}</span>
      </div>
    </el-card>

    <!-- 收支趋势 -->
    <el-card class="chart-card">
      <template #header>
        <div class="chart-header">
          <span>收支趋势对比</span>
          <el-radio-group v-model="trendGroupBy" size="small" @change="loadTrendData">
            <el-radio-button label="DAY">按日</el-radio-button>
            <el-radio-button label="WEEK">按周</el-radio-button>
            <el-radio-button label="MONTH">按月</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <div class="chart-container">
        <v-chart class="chart" :option="trendOption" autoresize />
      </div>
    </el-card>

    <!-- 分类排行柱状图 -->
    <el-row :gutter="20" class="chart-row">
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="chart-header">
              <span>支出分类 TOP10</span>
              <el-tag type="danger" size="small">支出</el-tag>
            </div>
          </template>
          <div class="chart-container">
            <v-chart class="chart" :option="expenseCategoryOption" autoresize />
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="chart-header">
              <span>收入分类 TOP10</span>
              <el-tag type="success" size="small">收入</el-tag>
            </div>
          </template>
          <div class="chart-container">
            <v-chart class="chart" :option="incomeCategoryOption" autoresize />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 工作日 vs 周末分析 -->
    <el-row :gutter="20" class="chart-row">
      <el-col :span="16">
        <el-card>
          <template #header>
            <div class="chart-header">
              <span>工作日 vs 周末消费分析</span>
              <el-tag type="warning" size="small">时间维度</el-tag>
            </div>
          </template>
          <div class="weekday-analysis-container">
            <div class="weekday-chart">
              <v-chart class="chart" :option="weekdayOption" autoresize />
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header>
            <span>时间维度洞察</span>
          </template>
          <div class="weekday-insight">
            <div class="insight-summary">
              <div class="insight-item">
                <div class="insight-label">工作日日均</div>
                <div class="insight-value expense">¥ {{ formatNumber(weekdayAnalysis.weekdayAvgExpense) }}</div>
              </div>
              <div class="insight-item">
                <div class="insight-label">周末日均</div>
                <div class="insight-value danger">¥ {{ formatNumber(weekdayAnalysis.weekendAvgExpense) }}</div>
              </div>
              <div class="insight-item">
                <div class="insight-label">周末/工作日</div>
                <div class="insight-value" :class="getWeekendRatioClass(weekdayAnalysis.weekendAvgExpense, weekdayAnalysis.weekdayAvgExpense)">
                  {{ calculateWeekendRatio(weekdayAnalysis.weekendAvgExpense, weekdayAnalysis.weekdayAvgExpense) }}
                </div>
              </div>
            </div>
            <el-divider />
            <div class="insight-text">
              <el-icon class="insight-icon"><InfoFilled /></el-icon>
              <span>{{ weekdayAnalysis.insight || '暂无分析数据' }}</span>
            </div>
            <el-divider />
            <div class="weekday-top-categories">
              <div class="top-title">工作日 TOP3</div>
              <div v-for="(cat, index) in weekdayAnalysis.weekdayTopCategories.slice(0, 3)" :key="index" class="top-item">
                <span class="top-rank">{{ index + 1 }}</span>
                <span class="top-name">{{ cat.categoryName }}</span>
                <span class="top-percent">{{ cat.percentage }}%</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 结构分析环形图 -->
    <el-row :gutter="20" class="chart-row">
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="chart-header">
              <span>支出结构分析</span>
              <el-tag type="info" size="small">占比</el-tag>
            </div>
          </template>
          <div class="chart-container">
            <v-chart class="chart" :option="expensePieOption" autoresize />
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>支出趋势分析</span>
          </template>
          <div class="trend-analysis">
            <div class="analysis-item">
              <div class="analysis-label">最大支出日</div>
              <div class="analysis-value">
                <span class="date">{{ trendAnalysis.maxExpenseDate || '-' }}</span>
                <span class="amount expense">¥ {{ formatNumber(trendAnalysis.maxExpenseAmount) }}</span>
              </div>
            </div>
            <div class="analysis-item">
              <div class="analysis-label">最大支出分类</div>
              <div class="analysis-value">
                <span class="category">{{ trendAnalysis.topCategory || '-' }}</span>
                <span class="percent">{{ trendAnalysis.topCategoryPercent }}%</span>
              </div>
            </div>
            <div class="analysis-item">
              <div class="analysis-label">支出波动系数</div>
              <div class="analysis-value">
                <span :class="['stability', getStabilityClass(trendAnalysis.volatility)]">
                  {{ getStabilityText(trendAnalysis.volatility) }}
                </span>
                <el-tooltip content="波动系数越小说明支出越稳定">
                  <el-icon><InfoFilled /></el-icon>
                </el-tooltip>
              </div>
            </div>
            <div class="analysis-item">
              <div class="analysis-label">日均消费评级</div>
              <div class="analysis-value">
                <el-rate 
                  :model-value="getSpendingLevel(summary.avgDailyExpense)" 
                  disabled 
                  :colors="['#67C23A', '#E6A23C', '#F56C6C']"
                />
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import * as echarts from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, PieChart, BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent, TitleComponent, GraphicComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import dayjs from 'dayjs'
import { getTrend, getByCategory, getBalance, getWeekdayAnalysis, getExpenseForecast } from '@/api/statistics'

echarts.use([CanvasRenderer, LineChart, PieChart, BarChart, GridComponent, TooltipComponent, LegendComponent, TitleComponent, GraphicComponent])

const dateRange = ref([])
const trendGroupBy = ref('DAY')
const trendData = ref({ expense: [], income: [] })
const expenseCategories = ref([])
const incomeCategories = ref([])
const expenseForecast = ref({
  currentMonth: '',
  totalDays: 0,
  passedDays: 0,
  remainingDays: 0,
  actualExpense: 0,
  dailyAverage: 0,
  forecastExpense: 0,
  forecastRangeLow: 0,
  forecastRangeHigh: 0,
  lastMonthExpense: 0,
  lastMonthTotal: 0,
  avgMonthlyExpense: 0,
  status: 'NORMAL',
  statusText: '正常',
  suggestion: '',
  dailyForecasts: []
})
const weekdayAnalysis = ref({
  weekdayExpense: 0,
  weekendExpense: 0,
  weekdayIncome: 0,
  weekendIncome: 0,
  weekdayCount: 0,
  weekendCount: 0,
  weekdayAvgExpense: 0,
  weekendAvgExpense: 0,
  dayDetails: [],
  weekdayTopCategories: [],
  weekendTopCategories: [],
  insight: ''
})
const summary = reactive({
  expense: 0,
  income: 0,
  balance: 0,
  avgDailyExpense: 0
})

const queryForm = reactive({
  startDate: '',
  endDate: ''
})

// 格式化数字
const formatNumber = (num) => {
  if (!num || isNaN(num)) return '0.00'
  return Number(num).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// 趋势分析数据
const trendAnalysis = computed(() => {
  const expenseData = trendData.value.expense || []
  const expenseCategories = expenseCategories.value || []
  
  // 最大支出日
  let maxExpenseDate = ''
  let maxExpenseAmount = 0
  expenseData.forEach(d => {
    if (d.amount > maxExpenseAmount) {
      maxExpenseAmount = d.amount
      maxExpenseDate = d.date
    }
  })
  
  // 最大支出分类
  let topCategory = ''
  let topCategoryPercent = 0
  const totalExpense = expenseCategories.reduce((sum, c) => sum + Number(c.amount), 0)
  if (expenseCategories.length > 0) {
    const top = expenseCategories[0]
    topCategory = top.categoryName
    topCategoryPercent = totalExpense > 0 ? ((top.amount / totalExpense) * 100).toFixed(1) : 0
  }
  
  // 波动系数 (标准差/平均值)
  let volatility = 0
  if (expenseData.length > 1) {
    const amounts = expenseData.map(d => Number(d.amount))
    const avg = amounts.reduce((a, b) => a + b, 0) / amounts.length
    const variance = amounts.reduce((sum, val) => sum + Math.pow(val - avg, 2), 0) / amounts.length
    const stdDev = Math.sqrt(variance)
    volatility = avg > 0 ? (stdDev / avg).toFixed(2) : 0
  }
  
  return { maxExpenseDate, maxExpenseAmount, topCategory, topCategoryPercent, volatility }
})

// 获取稳定性文字
const getStabilityText = (volatility) => {
  const v = parseFloat(volatility)
  if (v < 0.5) return '非常稳定'
  if (v < 1.0) return '比较稳定'
  if (v < 1.5) return '波动一般'
  return '波动较大'
}

// 获取稳定性样式
const getStabilityClass = (volatility) => {
  const v = parseFloat(volatility)
  if (v < 0.5) return 'stable-good'
  if (v < 1.0) return 'stable-normal'
  if (v < 1.5) return 'stable-warning'
  return 'stable-bad'
}

// 获取消费评级
const getSpendingLevel = (avgDaily) => {
  if (!avgDaily || avgDaily <= 0) return 0
  if (avgDaily < 100) return 1
  if (avgDaily < 300) return 2
  if (avgDaily < 500) return 3
  if (avgDaily < 1000) return 4
  return 5
}

// 收支趋势对比（柱状图）
const trendOption = computed(() => {
  const dates = trendData.value.expense.map(d => d.date)
  const expenseAmounts = trendData.value.expense.map(d => d.amount)
  const incomeAmounts = trendData.value.income.map(d => {
    const found = trendData.value.expense.find(e => e.date === d.date)
    return found ? d.amount : 0
  })
  
  // 对齐收入数据
  const alignedIncome = dates.map(date => {
    const found = trendData.value.income.find(d => d.date === date)
    return found ? found.amount : 0
  })

  return {
    tooltip: { 
      trigger: 'axis',
      axisPointer: { type: 'shadow' }
    },
    legend: { 
      data: ['支出', '收入'],
      top: 0
    },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '15%', containLabel: true },
    xAxis: { 
      type: 'category', 
      data: dates,
      axisLabel: { rotate: dates.length > 10 ? 45 : 0 }
    },
    yAxis: { type: 'value' },
    series: [
      {
        name: '支出',
        type: 'bar',
        data: expenseAmounts,
        itemStyle: { color: '#F56C6C' },
        barMaxWidth: 30
      },
      {
        name: '收入',
        type: 'bar',
        data: alignedIncome,
        itemStyle: { color: '#67C23A' },
        barMaxWidth: 30
      }
    ]
  }
})

// 支出分类排行（横向柱状图）
const expenseCategoryOption = computed(() => {
  const sorted = [...expenseCategories.value].sort((a, b) => a.amount - b.amount).slice(0, 10)
  const names = sorted.map(item => item.categoryName)
  const values = sorted.map(item => item.amount)

  return {
    tooltip: { 
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: '{b}: ¥{c}'
    },
    grid: { left: '3%', right: '10%', bottom: '3%', top: '5%', containLabel: true },
    xAxis: { 
      type: 'value',
      axisLabel: { formatter: '¥{value}' }
    },
    yAxis: { 
      type: 'category', 
      data: names,
      axisLabel: { width: 80, overflow: 'truncate' }
    },
    series: [{
      type: 'bar',
      data: values,
      itemStyle: { 
        color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
          { offset: 0, color: '#F56C6C' },
          { offset: 1, color: '#FF9F7F' }
        ]),
        borderRadius: [0, 4, 4, 0]
      },
      barMaxWidth: 20,
      label: {
        show: true,
        position: 'right',
        formatter: '¥{c}'
      }
    }]
  }
})

// 收入分类排行（横向柱状图）
const incomeCategoryOption = computed(() => {
  const sorted = [...incomeCategories.value].sort((a, b) => a.amount - b.amount).slice(0, 10)
  const names = sorted.map(item => item.categoryName)
  const values = sorted.map(item => item.amount)

  return {
    tooltip: { 
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: '{b}: ¥{c}'
    },
    grid: { left: '3%', right: '10%', bottom: '3%', top: '5%', containLabel: true },
    xAxis: { 
      type: 'value',
      axisLabel: { formatter: '¥{value}' }
    },
    yAxis: { 
      type: 'category', 
      data: names,
      axisLabel: { width: 80, overflow: 'truncate' }
    },
    series: [{
      type: 'bar',
      data: values,
      itemStyle: { 
        color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
          { offset: 0, color: '#67C23A' },
          { offset: 1, color: '#95D475' }
        ]),
        borderRadius: [0, 4, 4, 0]
      },
      barMaxWidth: 20,
      label: {
        show: true,
        position: 'right',
        formatter: '¥{c}'
      }
    }]
  }
})

// 支出结构环形图
const expensePieOption = computed(() => {
  const data = expenseCategories.value.slice(0, 8).map(item => ({
    name: item.categoryName,
    value: item.amount
  }))

  return {
    tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['50%', '50%'],
      avoidLabelOverlap: false,
      itemStyle: {
        borderRadius: 5,
        borderColor: '#fff',
        borderWidth: 2
      },
      label: { 
        show: true,
        formatter: '{b}\n{d}%'
      },
      emphasis: {
        label: { show: true, fontSize: 14, fontWeight: 'bold' }
      },
      data: data.length > 0 ? data : [{ name: '暂无数据', value: 0 }]
    }]
  }
})

const handleDateChange = (val) => {
  if (val) {
    queryForm.startDate = val[0]
    queryForm.endDate = val[1]
  } else {
    queryForm.startDate = ''
    queryForm.endDate = ''
  }
}

const loadTrendData = async () => {
  const [expenseRes, incomeRes] = await Promise.all([
    getTrend({
      type: 'EXPENSE',
      startDate: queryForm.startDate,
      endDate: queryForm.endDate,
      groupBy: trendGroupBy.value
    }),
    getTrend({
      type: 'INCOME',
      startDate: queryForm.startDate,
      endDate: queryForm.endDate,
      groupBy: trendGroupBy.value
    })
  ])
  trendData.value = {
    expense: expenseRes.data || [],
    income: incomeRes.data || []
  }
}

const loadCategoryData = async () => {
  expenseCategories.value = await getByCategory({
    type: 'EXPENSE',
    startDate: queryForm.startDate,
    endDate: queryForm.endDate
  })
  incomeCategories.value = await getByCategory({
    type: 'INCOME',
    startDate: queryForm.startDate,
    endDate: queryForm.endDate
  })
}

const loadSummaryData = async () => {
  const res = await getBalance({
    startDate: queryForm.startDate,
    endDate: queryForm.endDate
  })
  summary.expense = res.expense || 0
  summary.income = res.income || 0
  summary.balance = res.balance || 0
  
  // 计算日均支出
  if (queryForm.startDate && queryForm.endDate) {
    const days = dayjs(queryForm.endDate).diff(dayjs(queryForm.startDate), 'day') + 1
    summary.avgDailyExpense = days > 0 ? (summary.expense / days) : 0
  }
}

// 加载工作日/周末分析数据
const loadWeekdayAnalysis = async () => {
  const res = await getWeekdayAnalysis({
    startDate: queryForm.startDate,
    endDate: queryForm.endDate
  })
  weekdayAnalysis.value = res
}

// 计算周末/工作日比例
const calculateWeekendRatio = (weekendAvg, weekdayAvg) => {
  if (!weekdayAvg || weekdayAvg <= 0) return '-'
  const ratio = weekendAvg / weekdayAvg
  return ratio.toFixed(2) + 'x'
}

// 获取比例样式类
const getWeekendRatioClass = (weekendAvg, weekdayAvg) => {
  if (!weekdayAvg || weekdayAvg <= 0) return ''
  const ratio = weekendAvg / weekdayAvg
  if (ratio > 2) return 'danger'
  if (ratio > 1.5) return 'warning'
  if (ratio < 0.8) return 'good'
  return 'normal'
}

// 工作日/周末柱状图配置
const weekdayOption = computed(() => {
  const dayDetails = weekdayAnalysis.value.dayDetails || []
  const days = dayDetails.map(d => d.dayOfWeek)
  const expenses = dayDetails.map(d => d.expense)
  const incomes = dayDetails.map(d => d.income)
  
  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' }
    },
    legend: {
      data: ['支出', '收入'],
      top: 0
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '15%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: days,
      axisLabel: {
        color: (value, index) => {
          // 周末高亮显示
          return index >= 5 ? '#F56C6C' : '#606266'
        }
      }
    },
    yAxis: {
      type: 'value',
      axisLabel: { formatter: '¥{value}' }
    },
    series: [
      {
        name: '支出',
        type: 'bar',
        data: expenses,
        itemStyle: {
          color: (params) => {
            // 周末使用红色，工作日使用蓝色
            return params.dataIndex >= 5 ? '#F56C6C' : '#409EFF'
          },
          borderRadius: [4, 4, 0, 0]
        },
        barMaxWidth: 30
      },
      {
        name: '收入',
        type: 'bar',
        data: incomes,
        itemStyle: {
          color: '#67C23A',
          borderRadius: [4, 4, 0, 0]
        },
        barMaxWidth: 30
      }
    ]
  }
})

// 加载支出预测
const loadExpenseForecast = async () => {
  const res = await getExpenseForecast()
  expenseForecast.value = res
}

// 获取预测状态样式
const getForecastStatusType = (status) => {
  const typeMap = {
    'NORMAL': 'info',
    'GOOD': 'success',
    'WARNING': 'warning',
    'DANGER': 'danger'
  }
  return typeMap[status] || 'info'
}

// 获取预测对比样式
const getForecastCompareClass = () => {
  const forecast = expenseForecast.value.forecastExpense
  const lastMonth = expenseForecast.value.lastMonthTotal
  if (!forecast || !lastMonth) return ''
  const ratio = forecast / lastMonth
  if (ratio > 1.1) return 'danger'
  if (ratio < 0.9) return 'good'
  return 'normal'
}

// 获取预测对比文字
const getForecastCompareText = () => {
  const forecast = expenseForecast.value.forecastExpense
  const lastMonth = expenseForecast.value.lastMonthTotal
  if (!forecast || !lastMonth) return ''
  const diff = ((forecast - lastMonth) / lastMonth * 100).toFixed(0)
  if (diff > 0) return `比上月预计多 ${diff}%`
  if (diff < 0) return `比上月预计少 ${Math.abs(diff)}%`
  return '与上月持平'
}

// 预测趋势图配置
const forecastChartOption = computed(() => {
  const dailyForecasts = expenseForecast.value.dailyForecasts || []
  const dates = dailyForecasts.map(d => d.date.substring(5)) // 只显示 MM-DD
  const actualData = dailyForecasts.map(d => d.actual > 0 ? d.actual : null)
  const forecastData = dailyForecasts.map(d => d.forecast > 0 ? d.forecast : null)
  const cumulativeData = dailyForecasts.map(d => d.cumulative)
  
  // 找到今天的索引
  const todayIndex = dailyForecasts.findIndex(d => d.isToday)
  
  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' }
    },
    legend: {
      data: ['实际支出', '预测支出', '累计支出'],
      top: 0
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '15%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: dates,
      axisLabel: {
        interval: Math.floor(dates.length / 6),
        formatter: (value) => value
      }
    },
    yAxis: [
      {
        type: 'value',
        name: '日支出',
        position: 'left',
        axisLabel: { formatter: '¥{value}' }
      },
      {
        type: 'value',
        name: '累计',
        position: 'right',
        axisLabel: { formatter: '¥{value}' }
      }
    ],
    series: [
      {
        name: '实际支出',
        type: 'bar',
        data: actualData,
        itemStyle: { color: '#409EFF' },
        barMaxWidth: 12
      },
      {
        name: '预测支出',
        type: 'bar',
        data: forecastData,
        itemStyle: { color: 'rgba(64, 158, 255, 0.3)' },
        barMaxWidth: 12
      },
      {
        name: '累计支出',
        type: 'line',
        yAxisIndex: 1,
        data: cumulativeData,
        smooth: true,
        itemStyle: { color: '#F56C6C' },
        lineStyle: { width: 2 }
      }
    ],
    // 标记今天
    markLine: todayIndex >= 0 ? {
      silent: true,
      data: [{
        xAxis: todayIndex,
        lineStyle: { color: '#E6A23C', type: 'dashed' },
        label: { formatter: '今天', position: 'end' }
      }]
    } : undefined
  }
})

const loadData = async () => {
  await Promise.all([loadTrendData(), loadCategoryData(), loadSummaryData(), loadWeekdayAnalysis(), loadExpenseForecast()])
}

onMounted(() => {
  // Default to current month
  const start = dayjs().startOf('month').format('YYYY-MM-DD')
  const end = dayjs().endOf('month').format('YYYY-MM-DD')
  dateRange.value = [start, end]
  queryForm.startDate = start
  queryForm.endDate = end
  loadData()
})
</script>

<style scoped>
.statistics {
  padding-bottom: 20px;
}

.filter-card {
  margin-bottom: 20px;
}

.kpi-row {
  margin-bottom: 20px;
}

.kpi-card {
  text-align: center;
}

.kpi-card.expense .kpi-value {
  color: #F56C6C;
}

.kpi-card.income .kpi-value {
  color: #67C23A;
}

.kpi-card.balance .kpi-value {
  color: #409EFF;
}

.kpi-card.balance .kpi-value.negative {
  color: #F56C6C;
}

.kpi-card.avg .kpi-value {
  color: #E6A23C;
}

.kpi-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}

.kpi-value {
  font-size: 24px;
  font-weight: bold;
}

.chart-card {
  margin-bottom: 20px;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart-container {
  height: 350px;
}

.chart {
  width: 100%;
  height: 100%;
}

.chart-row {
  margin-bottom: 20px;
}

.trend-analysis {
  padding: 20px;
}

.analysis-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
  border-bottom: 1px solid #EBEEF5;
}

.analysis-item:last-child {
  border-bottom: none;
}

.analysis-label {
  font-size: 14px;
  color: #606266;
}

.analysis-value {
  font-size: 16px;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 8px;
}

.analysis-value .date {
  color: #909399;
  font-size: 14px;
}

.analysis-value .amount {
  font-weight: bold;
}

.analysis-value .amount.expense {
  color: #F56C6C;
}

.analysis-value .category {
  color: #606266;
}

.analysis-value .percent {
  color: #F56C6C;
  font-weight: bold;
}

.stable-good { color: #67C23A; }
.stable-normal { color: #E6A23C; }
.stable-warning { color: #F56C6C; }
.stable-bad { color: #F56C6C; font-weight: bold; }

.forecast-card {
  margin-bottom: 20px;
}

.forecast-summary {
  padding: 10px;
}

.forecast-item {
  margin-bottom: 16px;
}

.forecast-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}

.forecast-value {
  font-size: 20px;
  font-weight: bold;
  color: #303133;
}

.forecast-value.expense {
  color: #409EFF;
}

.forecast-value.warning {
  color: #E6A23C;
}

.forecast-range {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.forecast-compare {
  font-size: 12px;
  margin-top: 4px;
}

.forecast-compare.danger {
  color: #F56C6C;
}

.forecast-compare.good {
  color: #67C23A;
}

.forecast-compare.normal {
  color: #909399;
}

.forecast-suggestion {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 15px;
  background: #f5f7fa;
  border-radius: 4px;
  font-size: 13px;
  color: #606266;
}

.suggestion-icon {
  color: #E6A23C;
  font-size: 16px;
}

.weekday-analysis-container {
  height: 350px;
}

.weekday-chart {
  height: 100%;
}

.weekday-insight {
  padding: 10px;
}

.insight-summary {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.insight-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.insight-label {
  font-size: 14px;
  color: #606266;
}

.insight-value {
  font-size: 18px;
  font-weight: bold;
}

.insight-value.expense {
  color: #409EFF;
}

.insight-value.danger {
  color: #F56C6C;
}

.insight-value.good {
  color: #67C23A;
}

.insight-value.warning {
  color: #E6A23C;
}

.insight-value.normal {
  color: #909399;
}

.insight-text {
  display: flex;
  gap: 8px;
  align-items: flex-start;
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
}

.insight-icon {
  color: #E6A23C;
  margin-top: 2px;
  flex-shrink: 0;
}

.weekday-top-categories {
  margin-top: 10px;
}

.top-title {
  font-size: 13px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 10px;
}

.top-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  font-size: 13px;
}

.top-rank {
  width: 18px;
  height: 18px;
  background: #409EFF;
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: bold;
}

.top-item:nth-child(2) .top-rank {
  background: #67C23A;
}

.top-item:nth-child(3) .top-rank {
  background: #E6A23C;
}

.top-name {
  flex: 1;
  color: #606266;
}

.top-percent {
  color: #F56C6C;
  font-weight: 500;
}
</style>
