<template>
  <div class="categories">
    <el-card>
      <template #header>
        <div class="header">
          <span>分类管理</span>
          <div>
            <el-text type="info" class="drag-hint">
              <el-icon><Rank /></el-icon>
              拖动行可调整排序
            </el-text>
            <el-button type="primary" @click="handleAdd" style="margin-left: 15px;">
              <el-icon><Plus /></el-icon>新增分类
            </el-button>
          </div>
        </div>
      </template>

      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="支出分类" name="EXPENSE">
          <el-table
            ref="expenseTableRef"
            :data="flattenedExpenseCategories"
            stripe
            row-key="id"
            class="drag-table"
          >
            <el-table-column type="index" label="序号" width="60" align="center" />
            <el-table-column label="拖拽排序" width="80" align="center">
              <template #default="{ row }">
                <el-icon v-if="row.level === 0" class="drag-handle"><Rank /></el-icon>
                <span v-else class="drag-disabled">-</span>
              </template>
            </el-table-column>
            <el-table-column prop="name" label="名称" min-width="200">
              <template #default="{ row }">
                <div class="category-item">
                  <span v-if="row.level > 0" class="indent" :style="{ width: row.level * 24 + 'px' }"></span>
                  <span>{{ row.name }}</span>
                  <el-tag v-if="row.level > 0" size="small" type="info" class="sub-tag">子</el-tag>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="sortOrder" label="排序" width="60" align="center" />
            <el-table-column label="操作" width="280" align="center">
              <template #default="{ row }">
                <el-button type="primary" link @click="handleAddSub(row)" v-if="row.level === 0">添加子分类</el-button>
                <el-button type="primary" link @click="handleMove(row)" v-if="row.level === 1">调整层级</el-button>
                <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
                <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="收入分类" name="INCOME">
          <el-table
            ref="incomeTableRef"
            :data="flattenedIncomeCategories"
            stripe
            row-key="id"
            class="drag-table"
          >
            <el-table-column type="index" label="序号" width="60" align="center" />
            <el-table-column label="拖拽排序" width="80" align="center">
              <template #default="{ row }">
                <el-icon v-if="row.level === 0" class="drag-handle"><Rank /></el-icon>
                <span v-else class="drag-disabled">-</span>
              </template>
            </el-table-column>
            <el-table-column prop="name" label="名称" min-width="200">
              <template #default="{ row }">
                <div class="category-item">
                  <span v-if="row.level > 0" class="indent" :style="{ width: row.level * 24 + 'px' }"></span>
                  <span>{{ row.name }}</span>
                  <el-tag v-if="row.level > 0" size="small" type="info" class="sub-tag">子</el-tag>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="sortOrder" label="排序" width="60" align="center" />
            <el-table-column label="操作" width="280" align="center">
              <template #default="{ row }">
                <el-button type="primary" link @click="handleAddSub(row)" v-if="row.level === 0">添加子分类</el-button>
                <el-button type="primary" link @click="handleMove(row)" v-if="row.level === 1">调整层级</el-button>
                <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
                <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="520px"
      :close-on-click-modal="false"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px" @submit.native.prevent="handleSubmit">
        <el-form-item label="分类名称" prop="name">
          <el-input
            ref="formInput"
            v-model="form.name"
            placeholder="请输入分类名称"
            @keyup.enter="handleSubmit"
          />
        </el-form-item>
        
        <el-form-item label="分类类型" prop="type">
          <el-radio-group v-model="form.type" disabled>
            <el-radio-button value="EXPENSE">支出</el-radio-button>
            <el-radio-button value="INCOME">收入</el-radio-button>
          </el-radio-group>
        </el-form-item>
        
        <el-form-item label="所属父分类" prop="parentId" v-if="isSubCategory || isMove">
          <el-select v-model="form.parentId" placeholder="请选择父分类（不选则为一级分类）" clearable style="width: 100%">
            <el-option
              v-for="cat in availableParents"
              :key="cat.id"
              :label="cat.name"
              :value="cat.id"
              :disabled="cat.id === form.id"
            />
          </el-select>
          <div class="form-tip">不选择则为一级分类</div>
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
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import Sortable from 'sortablejs'
import { getCategories, createCategory, updateCategory, deleteCategory } from '@/api/category'

const activeTab = ref('EXPENSE')
const categories = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const isSubCategory = ref(false)
const isMove = ref(false)
const submitLoading = ref(false)
const formRef = ref()
const formInput = ref()
const expenseTableRef = ref()
const incomeTableRef = ref()
const sortableInstances = ref([])

const form = reactive({
  id: null,
  name: '',
  type: 'EXPENSE',
  sortOrder: 0,
  parentId: null,
  icon: ''  // emoji图标
})

const rules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择分类类型', trigger: 'change' }],
  icon: [{ required: true, message: '请选择图标', trigger: 'change' }]
}

const dialogTitle = computed(() => {
  if (isMove.value) return '调整层级'
  if (isEdit.value) return '编辑分类'
  if (isSubCategory.value) return '添加子分类'
  return '新增分类'
})

// 获取可作为父分类的选项（一级分类，且不能是自己）
const availableParents = computed(() => {
  return categories.value.filter(c => 
    c.type === form.type && 
    c.parentId === null && 
    c.id !== form.id
  )
})

// 将树形数据扁平化，并标记层级
const flattenCategories = (tree, level = 0, parent = null) => {
  const result = []
  for (const node of tree) {
    // 解构出 children，避免 Element Plus 表格将其识别为树形数据导致重复显示
    const { children, ...nodeWithoutChildren } = node
    // 用 _children 保留引用供拖拽逻辑使用
    const item = { ...nodeWithoutChildren, level, parent, _children: children }
    result.push(item)
    if (children && children.length > 0) {
      result.push(...flattenCategories(children, level + 1, node))
    }
  }
  return result
}

const flattenedExpenseCategories = computed(() => {
  const tree = categories.value.filter(c => c.type === 'EXPENSE')
  return flattenCategories(tree)
})

const flattenedIncomeCategories = computed(() => {
  const tree = categories.value.filter(c => c.type === 'INCOME')
  return flattenCategories(tree)
})

// 初始化拖拽排序
const initSortable = () => {
  // 销毁旧实例
  sortableInstances.value.forEach(s => s.destroy())
  sortableInstances.value = []

  const tableRef = activeTab.value === 'EXPENSE' ? expenseTableRef.value : incomeTableRef.value
  const flatData = activeTab.value === 'EXPENSE' ? flattenedExpenseCategories.value : flattenedIncomeCategories.value
  
  if (!tableRef) return

  const tbody = tableRef.$el.querySelector('.el-table__body-wrapper tbody')
  if (!tbody) return

  const sortable = new Sortable(tbody, {
    handle: '.drag-handle',
    animation: 150,
    ghostClass: 'sortable-ghost',
    chosenClass: 'sortable-chosen',
    dragClass: 'sortable-drag',
    onEnd: async ({ newIndex, oldIndex }) => {
      if (newIndex === oldIndex) return

      // 获取当前扁平数据
      const currentFlatList = [...flatData]
      const movedItem = currentFlatList[oldIndex]
      
      if (!movedItem) return

      // 如果拖到不同层级，提示用户使用"调整层级"功能
      const targetItem = currentFlatList[newIndex]
      
      if (targetItem && targetItem.level !== movedItem.level) {
        ElMessage.warning('跨层级移动请使用"调整层级"功能')
        return
      }

      // 获取所有同级项目
      const siblings = currentFlatList
        .filter(item => item.level === movedItem.level && item.parentId === movedItem.parentId)
      
      // 从原位置移除
      const movedIndex = siblings.findIndex(item => item.id === movedItem.id)
      const [moved] = siblings.splice(movedIndex, 1)
      
      // 计算在新顺序中的位置
      const targetIndex = targetItem 
        ? siblings.findIndex(item => item.id === targetItem.id)
        : siblings.length
      
      // 插入到新位置
      if (targetIndex >= 0 && targetIndex < siblings.length) {
        siblings.splice(targetIndex, 0, moved)
      } else {
        siblings.push(moved)
      }

      // 如果是移动一级分类，需要同时移动其子分类
      // 构建完整的排序列表（包含子分类）
      const fullSortedList = []
      siblings.forEach((sibling, index) => {
        // 添加父分类
        fullSortedList.push({ id: sibling.id, sortOrder: index })
        
        // 如果是一级分类，同时添加其子分类
        if (sibling.level === 0 && sibling._children) {
          sibling._children.forEach((child, childIndex) => {
            fullSortedList.push({ 
              id: child.id, 
              sortOrder: index,  // 子分类使用父分类的 sortOrder
              parentId: sibling.id 
            })
          })
        }
      })

      // 批量更新排序
      try {
        for (const item of fullSortedList) {
          const cat = categories.value.find(c => c.id === item.id)
          if (cat) {
            const updateData = { 
              ...cat, 
              sortOrder: item.sortOrder 
            }
            // 如果有 parentId，说明是子分类，更新其父ID
            if (item.parentId !== undefined && cat.parentId !== item.parentId) {
              updateData.parentId = item.parentId
            }
            await updateCategory(cat.id, updateData)
          }
        }
        ElMessage.success('排序已更新')
        loadData()
      } catch (error) {
        console.error('排序更新失败:', error)
        loadData()
      }
    }
  })

  sortableInstances.value.push(sortable)
}

const handleTabChange = () => {
  // 切换标签时重新加载对应类型的数据
  loadData()
  nextTick(() => {
    initSortable()
  })
}

const resetForm = () => {
  form.id = null
  form.name = ''
  form.type = activeTab.value
  form.sortOrder = 0
  form.parentId = null
  isSubCategory.value = false
  isMove.value = false
}

const handleAdd = () => {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
  nextTick(() => {
    formInput.value?.focus()
  })
}

const handleAddSub = (row) => {
  isEdit.value = false
  isSubCategory.value = true
  resetForm()
  form.parentId = row.id
  dialogVisible.value = true
  nextTick(() => {
    formInput.value?.focus()
  })
}

// 调整层级
const handleMove = (row) => {
  isEdit.value = true
  isMove.value = true
  Object.assign(form, row)
  activeTab.value = row.type
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  isMove.value = false
  Object.assign(form, row)
  activeTab.value = row.type
  dialogVisible.value = true
}

const handleDelete = (row) => {
  const msg = row._children && row._children.length > 0 
    ? '该分类下有子分类，删除将同时删除所有子分类，确定要删除吗？'
    : '确定要删除该分类吗？'
  
  ElMessageBox.confirm(msg, '提示', { type: 'warning' }).then(async () => {
    await deleteCategory(row.id)
    ElMessage.success('删除成功')
    loadData()
  })
}

const handleSubmit = async () => {
  await formRef.value.validate()
  submitLoading.value = true
  try {
    if (isEdit.value) {
      await updateCategory(form.id, form)
      ElMessage.success(isMove.value ? '层级调整成功' : '更新成功')
    } else {
      await createCategory(form)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    submitLoading.value = false
  }
}

const loadData = async () => {
  categories.value = await getCategories(activeTab.value)
  nextTick(() => {
    initSortable()
  })
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.categories {
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

.drag-disabled {
  color: #c0c4cc;
  font-size: 14px;
}

.category-item {
  display: flex;
  align-items: center;
}

.indent {
  display: inline-block;
}

.sub-tag {
  margin-left: 8px;
  font-size: 11px;
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

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
}
</style>
