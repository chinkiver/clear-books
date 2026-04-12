<template>
  <div class="tags-page">
    <el-card>
      <template #header>
        <div class="header">
          <span>标签管理</span>
          <el-text type="info">为流水添加标签，方便按项目、场景等维度统计花费</el-text>
        </div>
      </template>

      <div class="tag-actions">
        <el-input
          v-model="newTagName"
          placeholder="输入新标签名称"
          style="width: 200px"
          @keyup.enter="handleAddTag"
        />
        <el-button type="primary" @click="handleAddTag" :loading="addLoading">新增标签</el-button>
        <el-text type="info">标签用于标记流水，方便按项目、出差等场景统计花费</el-text>
      </div>

      <div v-if="tags.length === 0" class="empty-state">
        <el-empty description="暂无标签" />
        <el-text type="info">在上方输入标签名称添加，或在新增流水时直接创建标签</el-text>
      </div>

      <el-row v-else :gutter="20">
        <el-col :span="6" v-for="tag in tags" :key="tag.name">
          <el-card class="tag-card" shadow="hover">
            <div class="tag-header">
              <el-tag size="large" :color="tag.color || '#409EFF'" effect="dark">{{ tag.name }}</el-tag>
              <el-dropdown @command="handleCommand($event, tag.name)">
                <el-icon class="more-icon"><MoreFilled /></el-icon>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="rename">重命名</el-dropdown-item>
                    <el-dropdown-item command="delete" divided style="color: #F56C6C">删除</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
            <div class="tag-stats">
              <div class="tag-stat-item">
                <span class="stat-value">{{ tag.count }}</span>
                <span class="stat-label">条流水</span>
              </div>
              <div class="tag-stat-item">
                <span class="stat-value amount">{{ formatAmount(tag.totalAmount) }}</span>
                <span class="stat-label">金额合计</span>
              </div>
            </div>
            <div class="tag-meta">
              <div class="tag-color-setting">
                <span class="color-label">颜色</span>
                <el-color-picker v-model="tag.color" size="small" :predefine="predefineColors" @change="(color) => handleColorChange(tag.name, color)" />
              </div>
            </div>
            <div class="tag-actions-card">
              <el-button type="primary" link @click="viewTransactions(tag.name)">
                查看流水
              </el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </el-card>

    <!-- 重命名弹窗 -->
    <el-dialog v-model="renameVisible" title="重命名标签" width="400px">
      <el-form ref="renameRef" :model="renameForm" :rules="renameRules" label-width="100px">
        <el-form-item label="原标签">
          <el-input v-model="renameForm.oldName" disabled />
        </el-form-item>
        <el-form-item label="新标签" prop="newName">
          <el-input v-model="renameForm.newName" placeholder="请输入新标签名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="renameVisible = false">取消</el-button>
        <el-button type="primary" @click="submitRename" :loading="renameLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTagsWithCount, addPresetTag, updateTagColor, renameTag, deleteTag } from '@/api/transaction'

const router = useRouter()

const tags = ref([])
const newTagName = ref('')
const addLoading = ref(false)
const renameVisible = ref(false)
const renameLoading = ref(false)
const renameRef = ref()

const predefineColors = [
  '#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399',
  '#9254de', '#ff4d4f', '#ffa940', '#36cfc9', '#597ef7',
  '#eb2f96', '#7cb305', '#cf1322', '#1890ff', '#52c41a'
]

const renameForm = ref({
  oldName: '',
  newName: ''
})

const renameRules = {
  newName: [
    { required: true, message: '请输入新标签名称', trigger: 'blur' },
    { min: 1, max: 20, message: '长度在 1 到 20 个字符', trigger: 'blur' }
  ]
}

const formatAmount = (amount) => {
  return '¥ ' + Number(amount || 0).toFixed(2)
}

const loadTags = async () => {
  tags.value = await getTagsWithCount()
}

const viewTransactions = (tagName) => {
  router.push({
    path: '/transactions',
    query: { tag: tagName }
  })
}

const handleAddTag = async () => {
  const name = newTagName.value.trim()
  if (!name) {
    ElMessage.warning('请输入标签名称')
    return
  }
  addLoading.value = true
  try {
    await addPresetTag(name)
    ElMessage.success('标签添加成功')
    newTagName.value = ''
    loadTags()
  } finally {
    addLoading.value = false
  }
}

const handleColorChange = async (tagName, color) => {
  await updateTagColor(tagName, color)
  ElMessage.success('颜色更新成功')
  loadTags()
}

const handleCommand = (command, tagName) => {
  if (command === 'rename') {
    renameForm.value.oldName = tagName
    renameForm.value.newName = tagName
    renameVisible.value = true
  } else if (command === 'delete') {
    ElMessageBox.confirm(`确定要删除标签 "${tagName}" 吗？所有流水中的该标签都会被移除。`, '提示', {
      type: 'warning'
    }).then(async () => {
      await deleteTag(tagName)
      ElMessage.success('删除成功')
      loadTags()
    })
  }
}

const submitRename = async () => {
  await renameRef.value.validate()
  if (renameForm.value.newName === renameForm.value.oldName) {
    renameVisible.value = false
    return
  }
  renameLoading.value = true
  try {
    await renameTag(renameForm.value.oldName, renameForm.value.newName)
    ElMessage.success('重命名成功')
    renameVisible.value = false
    loadTags()
  } finally {
    renameLoading.value = false
  }
}

onMounted(loadTags)
</script>

<style scoped>
.tags-page {
  padding-bottom: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.tag-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  padding: 16px;
  background-color: #f6f8fa;
  border-radius: 4px;
}

.empty-state {
  text-align: center;
  padding: 40px 0;
}

.tag-card {
  margin-bottom: 20px;
}

.tag-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.more-icon {
  cursor: pointer;
  color: #909399;
  font-size: 16px;
}

.more-icon:hover {
  color: #409EFF;
}

.tag-stats {
  display: flex;
  justify-content: space-around;
  margin-bottom: 16px;
  padding: 12px;
  background-color: #f6f8fa;
  border-radius: 4px;
}

.tag-stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.stat-value {
  font-size: 20px;
  font-weight: bold;
  color: #303133;
}

.stat-value.amount {
  color: #F56C6C;
}

.stat-label {
  font-size: 12px;
  color: #909399;
}

.tag-meta {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  margin-bottom: 12px;
}

.tag-color-setting {
  display: flex;
  align-items: center;
  gap: 8px;
}

.color-label {
  font-size: 12px;
  color: #909399;
}

.tag-actions-card {
  text-align: right;
  border-top: 1px solid #ebeef5;
  padding-top: 12px;
}
</style>
