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

    <!-- 趋势图表 -->
    <el-card class="chart-card">
      <template #header>
        <div class="chart-header">
          <span>收支趋势</span>
          <el-radio-group v-model="trendType" size="small" @change="loadTrendData">
            <el-radio-button label="EXPENSE">支出</el-radio-button>
            <el-radio-button label="INCOME">收入</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <div class="chart-container">
        <v-chart class="chart" :option="trendOption" autoresize />
      </div>
    </el-card>

    <!-- 分类统计 -->
    <el-row :gutter="20" class="chart-row">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>支出分类占比</span>
          </template>
          <div class="chart-container">
            <v-chart class="chart" :option="expenseCategoryOption" autoresize />
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>收入分类占比</span>
          </template>
          <div class="chart-container">
            <v-chart class="chart" :option="incomeCategoryOption" autoresize />
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, PieChart, BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent, TitleComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import dayjs from 'dayjs'
import { getTrend, getByCategory, getBalance } from '@/api/statistics'

use([CanvasRenderer, LineChart, PieChart, BarChart, GridComponent, TooltipComponent, LegendComponent, TitleComponent])

const dateRange = ref([])
const trendType = ref('EXPENSE')
const trendData = ref([])
const expenseCategories = ref([])
const incomeCategories = ref([])

const queryForm = reactive({
  startDate: '',
  endDate: ''
})

const trendOption = computed(() => {
  const dates = trendData.value.map(d => d.date)
  const amounts = trendData.value.map(d => d.amount)

  return {
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: dates },
    yAxis: { type: 'value' },
    series: [{
      type: 'line',
      data: amounts,
      smooth: true,
      areaStyle: {
        color: trendType.value === 'INCOME' ? 'rgba(82, 196, 26, 0.3)' : 'rgba(245, 34, 45, 0.3)'
      },
      itemStyle: {
        color: trendType.value === 'INCOME' ? '#F5222D' : '#52C41A'
      }
    }]
  }
})

const expenseCategoryOption = computed(() => {
  const data = expenseCategories.value.map(item => ({
    name: item.categoryName,
    value: item.amount
  }))

  return {
    tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
    legend: { orient: 'vertical', right: 5, top: 'center', itemWidth: 10, itemHeight: 10, textStyle: { fontSize: 12 } },
    series: [{
      type: 'pie',
      radius: ['35%', '60%'],
      center: ['35%', '50%'],
      data: data.length > 0 ? data : [{ name: '暂无数据', value: 0 }],
      itemStyle: {
        color: (params) => expenseCategories.value[params.dataIndex]?.color || undefined
      }
    }]
  }
})

const incomeCategoryOption = computed(() => {
  const data = incomeCategories.value.map(item => ({
    name: item.categoryName,
    value: item.amount
  }))

  return {
    tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
    legend: { orient: 'vertical', right: 5, top: 'center', itemWidth: 10, itemHeight: 10, textStyle: { fontSize: 12 } },
    series: [{
      type: 'pie',
      radius: ['35%', '60%'],
      center: ['35%', '50%'],
      data: data.length > 0 ? data : [{ name: '暂无数据', value: 0 }],
      itemStyle: {
        color: (params) => incomeCategories.value[params.dataIndex]?.color || undefined
      }
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
  const res = await getTrend({
    type: trendType.value,
    startDate: queryForm.startDate,
    endDate: queryForm.endDate,
    groupBy: 'DAY'
  })
  trendData.value = res.data
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

const loadData = async () => {
  await Promise.all([loadTrendData(), loadCategoryData()])
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
</style>
