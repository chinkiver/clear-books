<template>
  <div class="accounts">
    <el-card>
      <template #header>
        <div class="header">
          <span>账户管理</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>新增账户
          </el-button>
        </div>
      </template>

      <el-row :gutter="20">
        <el-col :span="6" v-for="item in accountList" :key="item.id">
          <el-card class="account-card" :body-style="{ padding: '20px' }">
            <div class="account-header">
              <el-icon size="32" :color="item.color || '#409EFF'"><Wallet /></el-icon>
              <el-dropdown @command="(cmd) => handleCommand(cmd, item)">
                <el-icon class="more-icon"><More /></el-icon>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="edit">编辑</el-dropdown-item>
                    <el-dropdown-item command="adjust">调整余额</el-dropdown-item>
                    <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
            <div class="account-name">{{ item.name }}</div>
            <div class="account-type">{{ getTypeText(item.type) }}</div>
            <div class="account-balance" :style="{ color: item.color || '#409EFF' }">
              {{ formatAmount(item.balance) }}
            </div>
          </el-card>
        </el-col>
      </el-row>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑账户' : '新增账户'"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px" @submit.native.prevent="handleSubmit">
        <el-form-item label="账户名称" prop="name">
          <el-input
            v-model="form.name"
            placeholder="请输入账户名称"
            @keyup.enter="handleSubmit"
          />
        </el-form-item>
        <el-form-item label="账户类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择账户类型" style="width: 100%">
            <el-option label="现金" value="CASH" />
            <el-option label="银行卡" value="BANK" />
            <el-option label="支付宝" value="ALIPAY" />
            <el-option label="微信支付" value="WECHAT" />
            <el-option label="信用卡" value="CREDIT" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="初始余额" prop="balance" v-if="!isEdit">
          <el-input-number v-model="form.balance" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="颜色标识" prop="color">
          <el-color-picker v-model="form.color" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>

    <!-- 调整余额弹窗 -->
    <el-dialog
      v-model="adjustVisible"
      title="调整余额"
      width="400px"
      :close-on-click-modal="false"
    >
      <el-form :model="adjustForm" label-width="100px" @submit.native.prevent="handleAdjustSubmit">
        <el-form-item label="当前余额">
          <span>{{ formatAmount(currentAccount?.balance) }}</span>
        </el-form-item>
        <el-form-item label="新余额">
          <el-input-number
            v-model="adjustForm.amount"
            :precision="2"
            style="width: 100%"
            @keyup.enter="handleAdjustSubmit"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adjustVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAdjustSubmit" :loading="adjustLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAccounts, createAccount, updateAccount, deleteAccount, adjustBalance } from '@/api/account'

const accountList = ref([])
const dialogVisible = ref(false)
const adjustVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const adjustLoading = ref(false)
const currentAccount = ref(null)
const formRef = ref()

const form = reactive({
  id: null,
  name: '',
  type: '',
  balance: 0,
  color: '#409EFF'
})

const adjustForm = reactive({
  accountId: null,
  amount: 0
})

const rules = {
  name: [{ required: true, message: '请输入账户名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择账户类型', trigger: 'change' }]
}

const typeMap = {
  CASH: '现金',
  BANK: '银行卡',
  ALIPAY: '支付宝',
  WECHAT: '微信支付',
  CREDIT: '信用卡',
  OTHER: '其他'
}

const formatAmount = (amount) => {
  return '¥ ' + Number(amount || 0).toFixed(2)
}

const getTypeText = (type) => {
  return typeMap[type] || type
}

const resetForm = () => {
  form.id = null
  form.name = ''
  form.type = ''
  form.balance = 0
  form.color = '#409EFF'
}

const handleAdd = () => {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

const handleCommand = (cmd, item) => {
  if (cmd === 'edit') {
    isEdit.value = true
    Object.assign(form, item)
    dialogVisible.value = true
  } else if (cmd === 'adjust') {
    currentAccount.value = item
    adjustForm.accountId = item.id
    adjustForm.amount = Number(item.balance)
    adjustVisible.value = true
  } else if (cmd === 'delete') {
    ElMessageBox.confirm('确定要删除该账户吗？', '提示', { type: 'warning' }).then(async () => {
      await deleteAccount(item.id)
      ElMessage.success('删除成功')
      loadData()
    })
  }
}

const handleSubmit = async () => {
  await formRef.value.validate()
  submitLoading.value = true
  try {
    if (isEdit.value) {
      await updateAccount(form.id, form)
      ElMessage.success('更新成功')
    } else {
      await createAccount(form)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    submitLoading.value = false
  }
}

const handleAdjustSubmit = async () => {
  adjustLoading.value = true
  try {
    await adjustBalance(adjustForm.accountId, adjustForm.amount)
    ElMessage.success('余额调整成功')
    adjustVisible.value = false
    loadData()
  } finally {
    adjustLoading.value = false
  }
}

const loadData = async () => {
  accountList.value = await getAccounts()
}

onMounted(loadData)
</script>

<style scoped>
.accounts {
  padding-bottom: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.account-card {
  margin-bottom: 20px;
}

.account-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.more-icon {
  cursor: pointer;
  color: #909399;
}

.account-name {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 5px;
}

.account-type {
  font-size: 12px;
  color: #909399;
  margin-bottom: 10px;
}

.account-balance {
  font-size: 24px;
  font-weight: bold;
}
</style>
