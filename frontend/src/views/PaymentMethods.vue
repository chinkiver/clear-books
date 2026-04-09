<template>
  <div class="payment-methods">
    <el-card>
      <template #header>
        <div class="header">
          <span>支付方式管理</span>
          <div>
            <el-text type="info" class="drag-hint">
              <el-icon><Rank /></el-icon>
              拖动行可调整排序
            </el-text>
            <el-button type="primary" @click="handleAdd" style="margin-left: 15px;">
              <el-icon><Plus /></el-icon>新增支付方式
            </el-button>
          </div>
        </div>
      </template>

      <el-table
        ref="tableRef"
        :data="paymentMethodList"
        stripe
        row-key="id"
        class="drag-table"
      >
        <el-table-column type="index" label="序号" width="80" align="center">
          <template #default="{ $index }">
            <span class="index-num">{{ $index + 1 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="拖拽排序" width="100" align="center">
          <template #default>
            <el-icon class="drag-handle"><Rank /></el-icon>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="支付方式" />
        <el-table-column label="操作" width="150" align="center">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑支付方式' : '新增支付方式'"
      width="500px"
      :close-on-click-modal="false"
      @opened="$refs.formInput?.focus()"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="120px" @submit.native.prevent="handleSubmit">
        <el-form-item label="支付方式" prop="name">
          <el-input
            ref="formInput"
            v-model="form.name"
            placeholder="例如：微信支付、支付宝、京东支付等"
            @keyup.enter="handleSubmit"
          />
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
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import Sortable from 'sortablejs'
import { getPaymentMethods, createPaymentMethod, updatePaymentMethod, deletePaymentMethod } from '@/api/paymentMethod'
import request from '@/utils/request'

const tableRef = ref()
const paymentMethodList = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref()
const sortable = ref(null)

const form = reactive({
  id: null,
  name: ''
})

const rules = {
  name: [{ required: true, message: '请输入支付方式', trigger: 'blur' }]
}

// 初始化拖拽排序
const initSortable = () => {
  const tbody = tableRef.value.$el.querySelector('.el-table__body-wrapper tbody')
  if (!tbody) return

  sortable.value = new Sortable(tbody, {
    handle: '.drag-handle',
    animation: 150,
    ghostClass: 'sortable-ghost',
    chosenClass: 'sortable-chosen',
    dragClass: 'sortable-drag',
    onEnd: async ({ newIndex, oldIndex }) => {
      if (newIndex === oldIndex) return

      // 更新本地数据顺序
      const movedItem = paymentMethodList.value.splice(oldIndex, 1)[0]
      paymentMethodList.value.splice(newIndex, 0, movedItem)

      // 发送排序请求到后端
      const ids = paymentMethodList.value.map(item => item.id)
      try {
        await request.post('/payment-methods/sort', { ids })
        ElMessage.success('排序已保存')
      } catch (error) {
        // 失败则重新加载数据
        loadData()
      }
    }
  })
}

const resetForm = () => {
  form.id = null
  form.name = ''
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
  ElMessageBox.confirm('确定要删除该支付方式吗？', '提示', { type: 'warning' }).then(async () => {
    await deletePaymentMethod(row.id)
    ElMessage.success('删除成功')
    loadData()
  })
}

const handleSubmit = async () => {
  await formRef.value.validate()
  submitLoading.value = true
  try {
    if (isEdit.value) {
      await updatePaymentMethod(form.id, form)
      ElMessage.success('更新成功')
    } else {
      await createPaymentMethod(form)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    submitLoading.value = false
  }
}

const loadData = async () => {
  paymentMethodList.value = await getPaymentMethods()
}

onMounted(async () => {
  await loadData()
  nextTick(() => {
    initSortable()
  })
})
</script>

<style scoped>
.payment-methods {
  padding-bottom: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.drag-hint {
  font-size: 13px;
  color: #909399;
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.drag-handle {
  cursor: move;
  color: #909399;
  font-size: 18px;
}

.drag-handle:hover {
  color: #409EFF;
}

.index-num {
  color: #909399;
  font-size: 13px;
}

.drag-table :deep(.sortable-ghost) {
  opacity: 0.5;
  background: #f5f7fa;
}

.drag-table :deep(.sortable-chosen) {
  background: #ecf5ff;
}

.drag-table :deep(.sortable-drag) {
  opacity: 0.9;
  background: #fff;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}
</style>
