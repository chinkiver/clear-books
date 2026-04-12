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
        <el-form-item label="分类">
          <el-select v-model="queryForm.categoryId" placeholder="全部分类" clearable style="width: 160px">
            <el-option
              v-for="cat in allCategories"
              :key="cat.id"
              :label="cat.parentId ? '　└ ' + cat.name : cat.name"
              :value="cat.id"
            >
              <span v-if="cat.parentId" style="padding-left: 20px; color: #909399;">└ {{ cat.name }}</span>
              <span v-else>{{ cat.name }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="标签">
          <el-select v-model="queryForm.tag" placeholder="全部标签" clearable filterable style="width: 150px">
            <el-option v-for="tag in allTags" :key="tag" :label="tag" :value="tag" />
          </el-select>
        </el-form-item>
        <el-form-item label="金额">
          <el-input-number 
            v-model="queryForm.amount" 
            :precision="2" 
            :min="0" 
            placeholder="输入金额查询" 
            style="width: 150px"
            controls-position="right"
            clearable
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 数据表格 -->
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column type="index" label="序号" width="60" align="center" :index="(index) => (queryForm.page - 1) * queryForm.size + index + 1" />
        <el-table-column prop="transactionDate" label="日期" width="110" />
        <el-table-column prop="type" label="类型" width="70">
          <template #default="{ row }">
            <el-tag :type="getTypeTag(row.type)" size="small">{{ getTypeText(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="categoryName" label="分类" width="100" show-overflow-tooltip />
        <el-table-column prop="amount" label="金额" width="110">
          <template #default="{ row }">
            <span :class="row.type === 'INCOME' ? 'income-text' : 'expense-text'">
              {{ row.type === 'INCOME' ? '+' : '-' }}{{ formatAmount(row.amount) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="accountName" label="账户" width="160" show-overflow-tooltip />
        <el-table-column prop="paymentMethodName" label="支付方式" width="100" show-overflow-tooltip />
        <el-table-column label="标签" width="160">
          <template #default="{ row }">
            <el-tag
              v-for="tag in (row.tags || [])"
              :key="tag"
              size="small"
              class="tag-item"
              :color="tagColorMap[tag] || '#409EFF'"
              effect="dark"
            >
              {{ tag }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="备注" min-width="150" show-overflow-tooltip />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="info" link @click="handleQuickTag(row)">标签</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 页面合计 -->
      <div class="page-summary" v-if="tableData.length > 0">
        <span class="summary-item">
          本页收入：<span class="income-text">+{{ formatAmount(pageTotals.income) }}</span>
        </span>
        <span class="summary-item">
          本页支出：<span class="expense-text">-{{ formatAmount(pageTotals.expense) }}</span>
        </span>
        <span class="summary-item" v-if="pageTotals.transfer > 0">
          本页转账：<span>{{ formatAmount(pageTotals.transfer) }}</span>
        </span>
        <span class="summary-item summary-balance">
          本页结余：<span :class="pageTotals.balance >= 0 ? 'income-text' : 'expense-text'">
            {{ pageTotals.balance >= 0 ? '+' : '' }}{{ formatAmount(pageTotals.balance) }}
          </span>
        </span>
      </div>

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
          <el-input-number v-model="form.amount" :precision="2" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="日期" prop="transactionDate">
          <el-date-picker v-model="form.transactionDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="账户" prop="accountId">
          <div style="display: flex; gap: 8px; width: 100%">
            <el-select v-model="form.accountId" placeholder="请选择账户" style="flex: 1">
              <el-option v-for="acc in accounts" :key="acc.id" :label="acc.name" :value="acc.id" />
            </el-select>
            <el-button type="primary" plain @click="openQuickAddAccount" title="新增账户">
              <el-icon><Plus /></el-icon>
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="目标账户" prop="toAccountId" v-if="form.type === 'TRANSFER'">
          <div style="display: flex; gap: 8px; width: 100%">
            <el-select v-model="form.toAccountId" placeholder="请选择目标账户" style="flex: 1">
              <el-option v-for="acc in accounts" :key="acc.id" :label="acc.name" :value="acc.id" />
            </el-select>
            <el-button type="primary" plain @click="openQuickAddAccount" title="新增账户">
              <el-icon><Plus /></el-icon>
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label=" " prop="countAsExpense" v-if="form.type === 'TRANSFER'">
          <el-checkbox v-model="form.countAsExpense">计入支出（如转账给他人）</el-checkbox>
        </el-form-item>
        <el-form-item label="分类" prop="categoryId" v-if="form.type !== 'TRANSFER'">
          <div style="display: flex; gap: 8px; width: 100%">
            <el-select v-model="form.categoryId" placeholder="请选择分类" style="flex: 1">
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
            <el-button type="primary" plain @click="openQuickAddCategory" title="新增分类">
              <el-icon><Plus /></el-icon>
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="支付方式" prop="paymentMethodId">
          <div style="display: flex; gap: 8px; width: 100%">
            <el-select v-model="form.paymentMethodId" placeholder="请选择支付方式" clearable style="flex: 1">
              <el-option v-for="pm in paymentMethods" :key="pm.id" :label="pm.name" :value="pm.id" />
            </el-select>
            <el-button type="primary" plain @click="openQuickAddPaymentMethod" title="新增支付方式">
              <el-icon><Plus /></el-icon>
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="标签">
          <el-select
            v-model="form.tags"
            multiple
            filterable
            allow-create
            default-first-option
            placeholder="请输入或选择标签，按回车确认新标签"
            style="width: 100%"
          >
            <el-option v-for="tag in allTags" :key="tag" :label="tag" :value="tag">
              <span class="tag-option-dot" :style="{ backgroundColor: tagColorMap[tag] || '#409EFF' }"></span>
              {{ tag }}
            </el-option>
          </el-select>
          <div style="color: #909399; font-size: 12px; margin-top: 4px;">
            输入新标签后按回车键即可创建，一条流水可添加多个标签
          </div>
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

    <!-- 快速新增账户弹窗 -->
    <el-dialog v-model="quickAddAccountVisible" title="新增账户" width="400px" destroy-on-close>
      <el-form ref="quickAccountRef" :model="quickAccountForm" :rules="quickAccountRules" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="quickAccountForm.name" placeholder="请输入账户名称" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="quickAccountForm.type" placeholder="请选择类型" style="width: 100%">
            <el-option label="现金" value="CASH" />
            <el-option label="借记卡" value="DEBIT" />
            <el-option label="信用卡" value="CREDIT" />
            <el-option label="储蓄卡" value="SAVINGS" />
            <el-option label="银行卡" value="BANK" />
            <el-option label="电子钱包" value="EWALLET" />
            <el-option label="投资账户" value="INVESTMENT" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="初始余额">
          <el-input-number v-model="quickAccountForm.balance" :precision="2" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="颜色">
          <el-color-picker v-model="quickAccountForm.color" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="quickAddAccountVisible = false">取消</el-button>
        <el-button type="primary" @click="submitQuickAccount" :loading="quickAddLoading">确定</el-button>
      </template>
    </el-dialog>

    <!-- 快速新增分类弹窗 -->
    <el-dialog v-model="quickAddCategoryVisible" title="新增分类" width="400px" destroy-on-close>
      <el-form ref="quickCategoryRef" :model="quickCategoryForm" :rules="quickCategoryRules" label-width="80px">
        <el-form-item label="类型" prop="type">
          <el-radio-group v-model="quickCategoryForm.type" @change="onQuickCategoryTypeChange">
            <el-radio-button value="EXPENSE">支出</el-radio-button>
            <el-radio-button value="INCOME">收入</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="quickCategoryForm.name" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="父分类">
          <el-select 
            v-model="quickCategoryForm.parentId" 
            placeholder="不选则为一级分类" 
            clearable 
            style="width: 100%"
          >
            <el-option
              v-for="cat in parentCategoriesForQuickAdd"
              :key="cat.id"
              :label="cat.name"
              :value="cat.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="quickAddCategoryVisible = false">取消</el-button>
        <el-button type="primary" @click="submitQuickCategory" :loading="quickAddLoading">确定</el-button>
      </template>
    </el-dialog>

    <!-- 快速新增支付方式弹窗 -->
    <el-dialog v-model="quickAddPaymentMethodVisible" title="新增支付方式" width="400px" destroy-on-close>
      <el-form ref="quickPaymentMethodRef" :model="quickPaymentMethodForm" :rules="quickPaymentMethodRules" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="quickPaymentMethodForm.name" placeholder="请输入支付方式名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="quickAddPaymentMethodVisible = false">取消</el-button>
        <el-button type="primary" @click="submitQuickPaymentMethod" :loading="quickAddLoading">确定</el-button>
      </template>
    </el-dialog>

    <!-- 快速编辑标签弹窗 -->
    <el-dialog v-model="quickTagVisible" title="编辑标签" width="400px" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item label="标签">
          <el-select
            v-model="quickTagValues"
            multiple
            filterable
            allow-create
            default-first-option
            placeholder="请输入或选择标签"
            style="width: 100%"
          >
            <el-option v-for="tag in allTags" :key="tag" :label="tag" :value="tag">
              <span class="tag-option-dot" :style="{ backgroundColor: tagColorMap[tag] || '#409EFF' }"></span>
              {{ tag }}
            </el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="quickTagVisible = false">取消</el-button>
        <el-button type="primary" @click="submitQuickTag" :loading="quickTagLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import { getTransactions, createTransaction, updateTransaction, deleteTransaction, getTagsWithCount } from '@/api/transaction'
import { getAccounts, createAccount } from '@/api/account'
import { getCategories, createCategory } from '@/api/category'
import { getPaymentMethods, createPaymentMethod } from '@/api/paymentMethod'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)

// 快速新增弹窗控制
const quickAddAccountVisible = ref(false)
const quickAddCategoryVisible = ref(false)
const quickAddPaymentMethodVisible = ref(false)
const quickAddLoading = ref(false)

// 快速新增表单
const quickAccountForm = reactive({ name: '', type: 'BANK', balance: 0, color: '#409EFF' })
const quickCategoryForm = reactive({ name: '', type: 'EXPENSE', parentId: null, sortOrder: 0 })
const quickPaymentMethodForm = reactive({ name: '', sortOrder: 0 })

const quickAccountRef = ref()
const quickCategoryRef = ref()
const quickPaymentMethodRef = ref()

const quickAccountRules = {
  name: [{ required: true, message: '请输入账户名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择账户类型', trigger: 'change' }]
}

const quickCategoryRules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择分类类型', trigger: 'change' }]
}

const quickPaymentMethodRules = {
  name: [{ required: true, message: '请输入支付方式名称', trigger: 'blur' }]
}

// 快速编辑标签
const quickTagVisible = ref(false)
const quickTagLoading = ref(false)
const quickTagRow = ref(null)
const quickTagValues = ref([])

const tableData = ref([])
const total = ref(0)
const accounts = ref([])
const categories = ref([])
const paymentMethods = ref([])
const allTags = ref([])
const tagsWithColor = ref([])
const dateRange = ref([])
const dateRangeType = ref('month')  // month: 本月, all: 全部, custom: 自定义
const formRef = ref()

const queryForm = reactive({
  startDate: '',
  endDate: '',
  type: '',
  accountId: '',
  categoryId: '',
  tag: '',
  amount: null,
  page: 1,  // 前端从1开始，传给后端时减1
  size: 10
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
  tags: [],
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

// 查询表单中使用的全部分类（平铺展示）
const allCategories = computed(() => {
  return flattenCategories(categories.value)
})

// 标签颜色映射
const tagColorMap = computed(() => {
  const map = {}
  tagsWithColor.value.forEach(tag => {
    if (tag.color) {
      map[tag.name] = tag.color
    }
  })
  return map
})

// 当前页金额合计
const pageTotals = computed(() => {
  let income = 0
  let expense = 0
  let transfer = 0
  tableData.value.forEach(row => {
    const amount = Number(row.amount) || 0
    if (row.type === 'INCOME') income += amount
    else if (row.type === 'EXPENSE') expense += amount
    else if (row.type === 'TRANSFER') transfer += amount
  })
  return { income, expense, transfer, balance: income - expense }
})

// 快速新增分类时可用的一级分类选项
const parentCategoriesForQuickAdd = computed(() => {
  return categories.value.filter(c => 
    c.type === quickCategoryForm.type && 
    c.parentId === null
  )
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
  queryForm.categoryId = ''
  queryForm.tag = ''
  queryForm.amount = null
  queryForm.page = 1
  handleDateRangeTypeChange('month')
}

const resetForm = () => {
  form.id = null
  form.type = 'EXPENSE'
  form.amount = null
  form.transactionDate = dayjs().format('YYYY-MM-DD')
  form.accountId = ''
  form.toAccountId = ''
  form.categoryId = ''
  form.paymentMethodId = ''
  form.tags = []
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

// 快速新增账户
const openQuickAddAccount = () => {
  quickAccountForm.name = ''
  quickAccountForm.type = 'BANK'
  quickAccountForm.balance = 0
  quickAccountForm.color = '#409EFF'
  quickAddAccountVisible.value = true
}

const submitQuickAccount = async () => {
  await quickAccountRef.value.validate()
  quickAddLoading.value = true
  try {
    // 创建普通对象提交
    const submitData = {
      name: quickAccountForm.name,
      type: quickAccountForm.type,
      balance: quickAccountForm.balance,
      color: quickAccountForm.color
    }
    const res = await createAccount(submitData)
    ElMessage.success('账户创建成功')
    quickAddAccountVisible.value = false
    // 刷新账户列表并选中新账户
    accounts.value = await getAccounts()
    form.accountId = res.id
  } finally {
    quickAddLoading.value = false
  }
}

// 快速新增分类
const openQuickAddCategory = () => {
  quickCategoryForm.name = ''
  quickCategoryForm.type = form.type === 'INCOME' ? 'INCOME' : 'EXPENSE'
  quickCategoryForm.parentId = null
  quickAddCategoryVisible.value = true
}

// 当快速新增分类的类型改变时，清空已选择的父分类
const onQuickCategoryTypeChange = () => {
  quickCategoryForm.parentId = null
}

const submitQuickCategory = async () => {
  await quickCategoryRef.value.validate()
  quickAddLoading.value = true
  try {
    // 创建普通对象提交，避免 reactive 解包问题
    const submitData = {
      name: quickCategoryForm.name,
      type: quickCategoryForm.type,
      parentId: quickCategoryForm.parentId,
      sortOrder: quickCategoryForm.sortOrder
    }
    const res = await createCategory(submitData)
    ElMessage.success('分类创建成功')
    quickAddCategoryVisible.value = false
    // 刷新分类列表并选中新分类
    categories.value = await getCategories()
    form.categoryId = res.id
  } finally {
    quickAddLoading.value = false
  }
}

// 快速新增支付方式
const openQuickAddPaymentMethod = () => {
  quickPaymentMethodForm.name = ''
  quickAddPaymentMethodVisible.value = true
}

const submitQuickPaymentMethod = async () => {
  await quickPaymentMethodRef.value.validate()
  quickAddLoading.value = true
  try {
    // 创建普通对象提交
    const submitData = {
      name: quickPaymentMethodForm.name,
      sortOrder: quickPaymentMethodForm.sortOrder
    }
    const res = await createPaymentMethod(submitData)
    ElMessage.success('支付方式创建成功')
    quickAddPaymentMethodVisible.value = false
    // 刷新支付方式列表并选中新支付方式
    paymentMethods.value = await getPaymentMethods()
    form.paymentMethodId = res.id
  } finally {
    quickAddLoading.value = false
  }
}

const handleQuickTag = (row) => {
  quickTagRow.value = row
  quickTagValues.value = row.tags ? [...row.tags] : []
  quickTagVisible.value = true
}

const submitQuickTag = async () => {
  if (!quickTagRow.value) return
  quickTagLoading.value = true
  try {
    const submitData = {
      type: quickTagRow.value.type,
      amount: quickTagRow.value.amount,
      transactionDate: quickTagRow.value.transactionDate,
      accountId: quickTagRow.value.accountId,
      toAccountId: quickTagRow.value.toAccountId,
      categoryId: quickTagRow.value.categoryId,
      paymentMethodId: quickTagRow.value.paymentMethodId,
      description: quickTagRow.value.description,
      tags: quickTagValues.value,
      countAsExpense: quickTagRow.value.countAsExpense
    }
    await updateTransaction(quickTagRow.value.id, submitData)
    ElMessage.success('标签更新成功')
    quickTagVisible.value = false
    loadData()
  } finally {
    quickTagLoading.value = false
  }
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
    if (queryForm.page !== undefined) params.page = queryForm.page - 1  // 后端从0开始
    if (queryForm.size !== undefined) params.size = queryForm.size
    if (queryForm.startDate) params.startDate = queryForm.startDate
    if (queryForm.endDate) params.endDate = queryForm.endDate
    if (queryForm.type) params.type = queryForm.type
    if (queryForm.accountId) params.accountId = queryForm.accountId
    if (queryForm.categoryId) params.categoryId = queryForm.categoryId
    if (queryForm.tag) params.tag = queryForm.tag
    if (queryForm.amount !== null && queryForm.amount !== undefined && queryForm.amount !== '') params.amount = queryForm.amount
    
    const res = await getTransactions(params)
    tableData.value = res.content
    total.value = res.totalElements
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  accounts.value = await getAccounts()
  categories.value = await getCategories()
  paymentMethods.value = await getPaymentMethods()
  const tagRes = await getTagsWithCount()
  tagsWithColor.value = tagRes || []
  allTags.value = (tagRes || []).map(t => t.name)

  // 从 URL 读取标签参数
  if (route.query.tag) {
    queryForm.tag = route.query.tag
  }

  // 默认显示本月数据（如果有 URL 参数，先应用参数再加载）
  if (!queryForm.tag && !route.query.type && !route.query.accountId && !route.query.categoryId) {
    handleDateRangeTypeChange('month')
  } else {
    // 保持日期为当月（因为标签页跳转没有带日期参数）
    const start = dayjs().startOf('month').format('YYYY-MM-DD')
    const end = dayjs().endOf('month').format('YYYY-MM-DD')
    dateRange.value = [start, end]
    queryForm.startDate = start
    queryForm.endDate = end
    loadData()
  }
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
  color: #F5222D;
}

.expense-text {
  color: #52C41A;
}

.pagination {
  margin-top: 20px;
  justify-content: flex-end;
}

.tag-item {
  margin-right: 4px;
  margin-bottom: 2px;
}

.tag-option-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 6px;
  vertical-align: middle;
}

.page-summary {
  margin-top: 16px;
  padding: 12px 16px;
  background-color: #f6f8fa;
  border-radius: 4px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 24px;
  font-size: 14px;
  color: #606266;
}

.summary-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.summary-balance {
  font-weight: 600;
  margin-left: auto;
}
</style>
