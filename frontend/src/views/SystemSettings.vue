<template>
  <div class="system-settings">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>系统设置</span>
        </div>
      </template>

      <el-form :model="form" label-width="120px" style="max-width: 600px">
        <!-- 系统名称 -->
        <el-form-item label="系统名称">
          <el-input v-model="form.systemName" placeholder="请输入系统名称" maxlength="50" show-word-limit />
        </el-form-item>

        <!-- Logo -->
        <el-form-item label="系统 Logo">
          <div class="upload-wrapper">
            <el-upload
              class="logo-uploader"
              action="#"
              :auto-upload="false"
              :on-change="handleLogoChange"
              :before-upload="beforeLogoUpload"
              :show-file-list="false"
              accept="image/png,image/jpeg,image/jpg,image/svg+xml"
            >
              <img v-if="form.logoPreview" :src="form.logoPreview" class="logo-preview" />
              <el-icon v-else class="logo-uploader-icon"><Plus /></el-icon>
            </el-upload>
            <div class="upload-tips">
              <p>建议尺寸: 32x32 或 64x64 像素</p>
              <p>支持格式: PNG, JPG, SVG</p>
              <p>最大大小: 100KB</p>
              <el-button v-if="form.logoPreview" type="danger" link @click="clearLogo">清除</el-button>
            </div>
          </div>
        </el-form-item>

        <!-- Icon -->
        <el-form-item label="网站图标">
          <div class="upload-wrapper">
            <el-upload
              class="icon-uploader"
              action="#"
              :auto-upload="false"
              :on-change="handleIconChange"
              :before-upload="beforeIconUpload"
              :show-file-list="false"
              accept="image/png,image/x-icon,image/vnd.microsoft.icon"
            >
              <img v-if="form.iconPreview" :src="form.iconPreview" class="icon-preview" />
              <el-icon v-else class="icon-uploader-icon"><Plus /></el-icon>
            </el-upload>
            <div class="upload-tips">
              <p>建议尺寸: 16x16 或 32x32 像素</p>
              <p>支持格式: PNG, ICO</p>
              <p>最大大小: 50KB</p>
              <el-button v-if="form.iconPreview" type="danger" link @click="clearIcon">清除</el-button>
            </div>
          </div>
        </el-form-item>

        <!-- 操作按钮 -->
        <el-form-item>
          <el-button type="primary" @click="handleSave" :loading="saving">保存设置</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 预览卡片 -->
    <el-card class="preview-card">
      <template #header>
        <span>效果预览</span>
      </template>
      <div class="preview-content">
        <div class="preview-header">
          <img v-if="form.logoPreview" :src="form.logoPreview" class="preview-logo" />
          <el-icon v-else size="24"><Wallet /></el-icon>
          <span class="preview-title">{{ form.systemName || 'Clear Books' }}</span>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useSystemStore } from '@/stores/system'
import { updateSettings } from '@/api/settings'

const systemStore = useSystemStore()
const saving = ref(false)

const form = reactive({
  systemName: '',
  logo: '',
  logoPreview: '',
  icon: '',
  iconPreview: ''
})

// 加载现有设置
onMounted(async () => {
  // 如果 store 未加载，先从服务器获取
  if (!systemStore.loaded) {
    await systemStore.loadPublicInfo()
  }
  
  form.systemName = systemStore.systemName
  form.logoPreview = systemStore.logo
  form.iconPreview = systemStore.icon
  form.logo = systemStore.logo
  form.icon = systemStore.icon
})

// 上传前检查 Logo
const beforeLogoUpload = (rawFile) => {
  const maxSize = 100 * 1024 // 100KB
  if (rawFile.size > maxSize) {
    ElMessage.error('Logo 大小不能超过 100KB，请压缩后再试')
    return false
  }
  return true
}

// 上传前检查 Icon
const beforeIconUpload = (rawFile) => {
  const maxSize = 50 * 1024 // 50KB
  if (rawFile.size > maxSize) {
    ElMessage.error('图标大小不能超过 50KB，请压缩后再试')
    return false
  }
  return true
}

// 处理 Logo 上传
const handleLogoChange = (file) => {
  // 再次检查文件大小
  if (!beforeLogoUpload(file.raw)) {
    return
  }
  const reader = new FileReader()
  reader.onload = (e) => {
    form.logo = e.target.result
    form.logoPreview = e.target.result
  }
  reader.readAsDataURL(file.raw)
}

// 处理 Icon 上传
const handleIconChange = (file) => {
  // 再次检查文件大小
  if (!beforeIconUpload(file.raw)) {
    return
  }
  const reader = new FileReader()
  reader.onload = (e) => {
    form.icon = e.target.result
    form.iconPreview = e.target.result
  }
  reader.readAsDataURL(file.raw)
}

// 清除 Logo
const clearLogo = () => {
  form.logo = ''
  form.logoPreview = ''
}

// 清除 Icon
const clearIcon = () => {
  form.icon = ''
  form.iconPreview = ''
}

// 保存设置
const handleSave = async () => {
  if (!form.systemName.trim()) {
    ElMessage.warning('请输入系统名称')
    return
  }

  saving.value = true
  try {
    const settings = [
      { key: 'system.name', value: form.systemName.trim(), description: '系统名称' },
      { key: 'system.logo', value: form.logo, description: '系统 Logo' },
      { key: 'system.icon', value: form.icon, description: '网站图标' }
    ]
    
    await updateSettings(settings)
    
    // 更新本地状态
    systemStore.systemName = form.systemName.trim()
    systemStore.logo = form.logo
    systemStore.icon = form.icon
    
    // 更新页面标题和图标
    document.title = form.systemName.trim()
    if (form.icon && systemStore.updateFavicon) {
      systemStore.updateFavicon(form.icon)
    }
    
    ElMessage.success('设置保存成功')
  } catch (error) {
    ElMessage.error('保存失败: ' + (error.message || '未知错误'))
  } finally {
    saving.value = false
  }
}

// 重置
const handleReset = () => {
  form.systemName = systemStore.systemName
  form.logoPreview = systemStore.logo
  form.iconPreview = systemStore.icon
  form.logo = systemStore.logo
  form.icon = systemStore.icon
  ElMessage.info('已重置为当前设置')
}
</script>

<style scoped>
.system-settings {
  padding-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.upload-wrapper {
  display: flex;
  align-items: flex-start;
  gap: 20px;
}

.logo-uploader,
.icon-uploader {
  border: 1px dashed var(--el-border-color);
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: var(--el-transition-duration-fast);
}

.logo-uploader:hover,
.icon-uploader:hover {
  border-color: var(--el-color-primary);
}

.logo-uploader {
  width: 100px;
  height: 100px;
}

.icon-uploader {
  width: 64px;
  height: 64px;
}

/* Fix: Make el-upload content center */
.logo-uploader :deep(.el-upload),
.icon-uploader :deep(.el-upload) {
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
}

.logo-uploader-icon,
.icon-uploader-icon {
  font-size: 28px;
  color: #8c939d;
}

.logo-preview {
  width: 100px;
  height: 100px;
  object-fit: contain;
}

.icon-preview {
  width: 64px;
  height: 64px;
  object-fit: contain;
}

.upload-tips {
  color: #909399;
  font-size: 12px;
  line-height: 1.5;
}

.upload-tips p {
  margin: 4px 0;
}

.preview-card {
  margin-top: 20px;
}

.preview-content {
  padding: 20px;
  background: #f5f7fa;
  border-radius: 4px;
}

.preview-header {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #409EFF;
}

.preview-logo {
  width: 32px;
  height: 32px;
  object-fit: contain;
}

.preview-title {
  font-size: 18px;
  font-weight: bold;
}
</style>
