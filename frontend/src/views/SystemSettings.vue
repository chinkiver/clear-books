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
              <img v-if="logoPreview" :src="logoPreview" class="logo-preview" />
              <el-icon v-else class="logo-uploader-icon"><Plus /></el-icon>
            </el-upload>
            <div class="upload-tips">
              <p>建议尺寸: 32x32 或 64x64 像素</p>
              <p>支持格式: PNG, JPG, SVG</p>
              <p>最大大小: 100KB</p>
              <el-button v-if="logoPreview" type="danger" link @click="clearLogo">清除</el-button>
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
              <img v-if="iconPreview" :src="iconPreview" class="icon-preview" />
              <el-icon v-else class="icon-uploader-icon"><Plus /></el-icon>
            </el-upload>
            <div class="upload-tips">
              <p>建议尺寸: 16x16 或 32x32 像素</p>
              <p>支持格式: PNG, ICO</p>
              <p>最大大小: 50KB</p>
              <el-button v-if="iconPreview" type="danger" link @click="clearIcon">清除</el-button>
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
          <img v-if="logoPreview" :src="logoPreview" class="preview-logo" />
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
import { updateSettings, uploadLogo, uploadIcon } from '@/api/settings'

const systemStore = useSystemStore()
const saving = ref(false)
const uploadingLogo = ref(false)
const uploadingIcon = ref(false)

const form = reactive({
  systemName: ''
})

// 图片预览（从 store 加载的 URL 或新上传的预览）
const logoPreview = ref('')
const iconPreview = ref('')

// 是否有新选择的文件待上传
const newLogoFile = ref(null)
const newIconFile = ref(null)

// 从服务器加载的设置（用于判断是否变更）
const serverLogoUrl = ref('')
const serverIconUrl = ref('')

// 加载现有设置
onMounted(async () => {
  // 如果 store 未加载，先从服务器获取
  if (!systemStore.loaded) {
    await systemStore.loadPublicInfo()
  }
  
  form.systemName = systemStore.systemName
  logoPreview.value = systemStore.logo || ''
  iconPreview.value = systemStore.icon || ''
  
  // 保存服务器返回的 URL
  serverLogoUrl.value = systemStore.logo || ''
  serverIconUrl.value = systemStore.icon || ''
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

// 处理 Logo 选择（不上传，只预览）
const handleLogoChange = (file) => {
  if (!beforeLogoUpload(file.raw)) {
    return
  }
  
  // 创建本地预览
  const reader = new FileReader()
  reader.onload = (e) => {
    logoPreview.value = e.target.result
  }
  reader.readAsDataURL(file.raw)
  
  // 保存文件，等待保存时上传
  newLogoFile.value = file.raw
}

// 处理 Icon 选择（不上传，只预览）
const handleIconChange = (file) => {
  if (!beforeIconUpload(file.raw)) {
    return
  }
  
  // 创建本地预览
  const reader = new FileReader()
  reader.onload = (e) => {
    iconPreview.value = e.target.result
  }
  reader.readAsDataURL(file.raw)
  
  // 保存文件，等待保存时上传
  newIconFile.value = file.raw
}

// 清除 Logo
const clearLogo = () => {
  logoPreview.value = ''
  newLogoFile.value = null
  serverLogoUrl.value = ''
}

// 清除 Icon
const clearIcon = () => {
  iconPreview.value = ''
  newIconFile.value = null
  serverIconUrl.value = ''
}

// 保存设置
const handleSave = async () => {
  if (!form.systemName.trim()) {
    ElMessage.warning('请输入系统名称')
    return
  }

  saving.value = true
  let logoUrl = serverLogoUrl.value
  let iconUrl = serverIconUrl.value
  
  try {
    // 1. 如果有新 Logo，先上传
    if (newLogoFile.value) {
      uploadingLogo.value = true
      const logoResult = await uploadLogo(newLogoFile.value)
      logoUrl = logoResult.url
      uploadingLogo.value = false
    }
    
    // 2. 如果有新 Icon，先上传
    if (newIconFile.value) {
      uploadingIcon.value = true
      const iconResult = await uploadIcon(newIconFile.value)
      iconUrl = iconResult.url
      uploadingIcon.value = false
    }
    
    // 3. 保存设置到数据库（只存 URL）
    const settings = [
      { key: 'system.name', value: form.systemName.trim(), description: '系统名称' },
      { key: 'system.logo', value: logoUrl, description: '系统 Logo URL' },
      { key: 'system.icon', value: iconUrl, description: '网站图标 URL' }
    ]
    
    await updateSettings(settings)
    
    // 4. 更新本地状态
    systemStore.systemName = form.systemName.trim()
    systemStore.logo = logoUrl
    systemStore.icon = iconUrl
    
    // 更新预览为服务器 URL（这样刷新后也是同样的 URL）
    if (logoUrl) {
      logoPreview.value = logoUrl
    }
    if (iconUrl) {
      iconPreview.value = iconUrl
    }
    
    // 清空待上传文件标记
    newLogoFile.value = null
    newIconFile.value = null
    serverLogoUrl.value = logoUrl
    serverIconUrl.value = iconUrl
    
    // 更新页面标题和图标
    document.title = form.systemName.trim()
    if (iconUrl && systemStore.updateFavicon) {
      systemStore.updateFavicon(iconUrl)
    }
    
    ElMessage.success('设置保存成功')
  } catch (error) {
    console.error('Save failed:', error)
    ElMessage.error('保存失败: ' + (error.message || '未知错误'))
  } finally {
    saving.value = false
    uploadingLogo.value = false
    uploadingIcon.value = false
  }
}

// 重置
const handleReset = () => {
  form.systemName = systemStore.systemName
  logoPreview.value = systemStore.logo || ''
  iconPreview.value = systemStore.icon || ''
  newLogoFile.value = null
  newIconFile.value = null
  serverLogoUrl.value = systemStore.logo || ''
  serverIconUrl.value = systemStore.icon || ''
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
