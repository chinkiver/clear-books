<template>
  <div class="transactions">
    <el-card>
      <template #header>
        <div class="header">
          <span>流水管理</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>新增流水
          </el-button>
        </div>
      </template>

      <!-- 筛选栏 -->
      <el-form :inline="true" :model="queryForm" class="filter-form">
        <el-form-item label="日期范围">
          <el-radio-group v-model="dateRangeType" size="small" @change="handleDateRangeTypeChange" style="margin-right: 10px;">
            <el-radio-button value="month">本月</el-radio-button>
            <el-radio-button value="all">全部</el-radio-button>
            <el-radio-button value="custom">自定义</el-radio-button>
          </el-radio-group>
          <el-date-picker
            v-if="dateRangeType === 'custom'"
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            @change="handleDateChange"
          />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="queryForm.type" placeholder="全部类型" clearable style="width: 120px">
            <el-option label="收入" value="INCOME" />
            <el-option label="支出" value="EXPENSE" />
            <el-option label="转账" value="TRANSFER" />
          </el-select>
        </el-form-item>
        <el-form-item label="账户">
          <el-select v-model="queryForm.accountId" placeholder="全部账户" clearable style="width: 150px">
            <el-option v-for="acc in accounts" :key="acc.id" :label="acc.name" :value="acc.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 数据表格 -->
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="transactionDate" label="日期" width="120" />
        <el-table-column prop="type" label="类型" width="80">
          <template #default="{ row }">
            <el-tag :type="getTypeTag(row.type)">{{ getTypeText(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="categoryName" label="分类" width="120" />
        <el-table-column prop="amount" label="金额" width="120">
          <template #default="{ row }">
            <span :class="row.type === 'INCOME' ? 'income-text' : 'expense-text'">
              {{ row.type === 'INCOME' ? '+' : '-' }}{{ formatAmount(row.amount) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="accountName" label="账户" width="120" />
        <el-table-column prop="paymentMethodName" label="支付方式" width="120" />
        <el-table-column prop="description" label="备注" show-overflow-tooltip />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="queryForm.page"
        v-model:page-size="queryForm.size"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next"
        class="pagination"
        @size-change="loadData"
        @current-change="loadData"
      />
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑流水' : '新增流水'" width="600px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio-button value="EXPENSE">支出</el-radio-button>
            <el-radio-button value="INCOME">收入</el-radio-button>
            <el-radio-button value="TRANSFER">转账</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="金额" prop="amount">
          <el-input-number v-model="form.amount" :precision="2" :min="0.01" style="width: 100%" />
        </el-form-item>
        <el-form-item label="日期" prop="transactionDate">
          <el-date-picker v-model="form.transactionDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="账户" prop="accountId">
          <el-select v-model="form.accountId" placeholder="请选择账户" style="width: 100%">
            <el-option v-for="acc in accounts" :key="acc.id" :label="acc.name" :value="acc.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标账户" prop="toAccountId" v-if="form.type === 'TRANSFER'">
          <el-select v-model="form.toAccountId" placeholder="请选择目标账户" style="width: 100%">
            <el-option v-for="acc in accounts" :key="acc.id" :label="acc.name" :value="acc.id" />
          </el-select>
        </el-form-item>
        <el-form-item label=" " prop="countAsExpense" v-if="form.type === 'TRANSFER'">
          <el-checkbox v-model="form.countAsExpense">计入支出（如转账给他人）</el-checkbox>
        </el-form-item>
        <el-form-item label="分类" prop="categoryId" v-if="form.type !== 'TRANSFER'">
          <el-select v-model="form.categoryId" placeholder="请选择分类" style="width: 100%">
            <el-option
              v-for="cat in filteredCategories"
              :key="cat.id"
              :label="cat.parentId ? '　└ ' + cat.name : cat.name"
              :value="cat.id"
            >
              <span v-if="cat.parentId" style="padding-left: 20px; color: #909399;">└ {{ cat.name }}</span>
              <span v-else>{{ cat.name }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="支付方式" prop="paymentMethodId">
          <el-select v-model="form.paymentMethodId" placeholder="请选择支付方式" clearable style="width: 100%">
            <el-option v-for="pm in paymentMethods" :key="pm.id" :label="pm.name" :value="pm.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import { getTransactions, createTransaction, updateTransaction, deleteTransaction } from '@/api/transaction'
import { getAccounts } from '@/api/account'
import { getCategories } from '@/api/category'
import { getPaymentMethods } from '@/api/paymentMethod'

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const tableData = ref([])
const total = ref(0)
const accounts = ref([])
const categories = ref([])
const paymentMethods = ref([])
const dateRange = ref([])
const dateRangeType = ref('month')  // month: 本月, all: 全部, custom: 自定义
const formRef = ref()

const queryForm = reactive({
  startDate: '',
  endDate: '',
  type: '',
  accountId: '',
  page: 0,
  size: 20
})

const form = reactive({
  id: null,
  type: 'EXPENSE',
  amount: null,  // 初始为空，不预设0.01
  transactionDate: dayjs().format('YYYY-MM-DD'),
  accountId: '',
  toAccountId: '',
  categoryId: '',
  paymentMethodId: '',
  description: '',
  countAsExpense: false
})

const rules = {
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  amount: [{ required: true, message: '请输入金额', trigger: 'blur' }],
  transactionDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
  accountId: [{ required: true, message: '请选择账户', trigger: 'change' }],
  toAccountId: [{ required: true, message: '请选择目标账户', trigger: 'change' }]
}

// 将树形分类扁平化，包含所有子分类
const flattenCategories = (tree) => {
  const result = []
  for (const node of tree) {
    result.push(node)
    if (node.children && node.children.length > 0) {
      result.push(...flattenCategories(node.children))
    }
  }
  return result
}

const filteredCategories = computed(() => {
  const flatList = flattenCategories(categories.value)
  return flatList.filter(c => c.type === form.type)
})

const formatAmount = (amount) => {
  return '¥ ' + Number(amount).toFixed(2)
}

const getTypeTag = (type) => {
  const map = { INCOME: 'success', EXPENSE: 'danger', TRANSFER: 'info' }
  return map[type] || ''
}

const getTypeText = (type) => {
  const map = { INCOME: '收入', EXPENSE: '支出', TRANSFER: '转账' }
  return map[type] || type
}

const handleDateRangeTypeChange = (type) => {
  if (type === 'month') {
    // 本月
    const start = dayjs().startOf('month').format('YYYY-MM-DD')
    const end = dayjs().endOf('month').format('YYYY-MM-DD')
    dateRange.value = [start, end]
    queryForm.startDate = start
    queryForm.endDate = end
  } else if (type === 'all') {
    // 全部 - 不限制日期
    dateRange.value = []
    queryForm.startDate = ''
    queryForm.endDate = ''
  } else {
    // 自定义 - 保持当前选择或清空
    if (!dateRange.value || dateRange.value.length === 0) {
      const start = dayjs().startOf('month').format('YYYY-MM-DD')
      const end = dayjs().endOf('month').format('YYYY-MM-DD')
      dateRange.value = [start, end]
    }
    queryForm.startDate = dateRange.value[0]
    queryForm.endDate = dateRange.value[1]
  }
  loadData()
}

const handleDateChange = (val) => {
  if (val) {
    queryForm.startDate = val[0]
    queryForm.endDate = val[1]
  } else {
    queryForm.startDate = ''
    queryForm.endDate = ''
  }
}

const resetQuery = () => {
  dateRangeType.value = 'month'
  dateRange.value = []
  queryForm.startDate = ''
  queryForm.endDate = ''
  queryForm.type = ''
  queryForm.accountId = ''
  queryForm.page = 0
  handleDateRangeTypeChange('month')
}

const resetForm = () => {
  form.id = null
  form.type = 'EXPENSE'
  form.amount = 0
  form.transactionDate = dayjs().format('YYYY-MM-DD')
  form.accountId = ''
  form.toAccountId = ''
  form.categoryId = ''
  form.paymentMethodId = ''
  form.description = ''
  form.countAsExpense = false
}

const handleAdd = () => {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  Object.assign(form, row)
  dialogVisible.value = true
}

const handleDelete = (row) => {
  ElMessageBox.confirm('确定要删除这条流水吗？', '提示', {
    type: 'warning'
  }).then(async () => {
    await deleteTransaction(row.id)
    ElMessage.success('删除成功')
    loadData()
  })
}

const handleSubmit = async () => {
  await formRef.value.validate()
  submitLoading.value = true
  try {
    if (isEdit.value) {
      await updateTransaction(form.id, form)
      ElMessage.success('更新成功')
    } else {
      await createTransaction(form)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    submitLoading.value = false
  }
}

const loadData = async () => {
  loading.value = true
  try {
    // 构建查询参数，过滤掉空值
    const params = {}
    if (queryForm.page !== undefined) params.page = queryForm.page
    if (queryForm.size !== undefined) params.size = queryForm.size
    if (queryForm.startDate) params.startDate = queryForm.startDate
    if (queryForm.endDate) params.endDate = queryForm.endDate
    if (queryForm.type) params.type = queryForm.type
    if (queryForm.accountId) params.accountId = queryForm.accountId
    
    const res = await getTransactions(params)
    tableData.value = res.content
    total.value = res.totalElements
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  // 默认显示本月数据
  handleDateRangeTypeChange('month')
  
  accounts.value = await getAccounts()
  categories.value = await getCategories()
  paymentMethods.value = await getPaymentMethods()
})
</script>

<style scoped>
.transactions {
  padding-bottom: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.filter-form {
  margin-bottom: 20px;
}

.income-text {
  color: #52C41A;
}

.expense-text {
  color: #F5222D;
}

.pagination {
  margin-top: 20px;
  justify-content: flex-end;
}
</style>
