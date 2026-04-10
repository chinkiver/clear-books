import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getPublicInfo, getAllSettings, updateSetting, updateSettings } from '@/api/settings'

export const useSystemStore = defineStore('system', () => {
  // State
  const systemName = ref('Clear Books')
  const logo = ref('')
  const icon = ref('')
  const settings = ref([])
  const loaded = ref(false)

  // Getters
  const hasLogo = computed(() => !!logo.value)
  const hasIcon = computed(() => !!icon.value)

  // Actions
  /**
   * 加载公开的系统信息
   */
  const loadPublicInfo = async () => {
    try {
      const res = await getPublicInfo()
      if (res.data) {
        systemName.value = res.data.systemName || 'Clear Books'
        logo.value = res.data.logo || ''
        icon.value = res.data.icon || ''
        
        // 更新页面标题
        document.title = systemName.value
        
        // 更新 favicon
        if (icon.value) {
          updateFavicon(icon.value)
        }
        
        loaded.value = true
      }
    } catch (error) {
      console.error('Failed to load system info:', error)
    }
  }

  /**
   * 加载所有设置（管理员）
   */
  const loadAllSettings = async () => {
    const res = await getAllSettings()
    settings.value = res.data || []
    return settings.value
  }

  /**
   * 更新单个设置
   */
  const updateSystemSetting = async (data) => {
    const res = await updateSetting(data)
    
    // 更新本地状态
    if (data.key === 'system.name') {
      systemName.value = data.value || 'Clear Books'
      document.title = systemName.value
    } else if (data.key === 'system.logo') {
      logo.value = data.value || ''
    } else if (data.key === 'system.icon') {
      icon.value = data.value || ''
      if (icon.value) {
        updateFavicon(icon.value)
      }
    }
    
    return res.data
  }

  /**
   * 批量更新设置
   */
  const updateSystemSettings = async (dataList) => {
    await updateSettings(dataList)
    
    // 更新本地状态
    for (const data of dataList) {
      if (data.key === 'system.name') {
        systemName.value = data.value || 'Clear Books'
        document.title = systemName.value
      } else if (data.key === 'system.logo') {
        logo.value = data.value || ''
      } else if (data.key === 'system.icon') {
        icon.value = data.value || ''
        if (icon.value) {
          updateFavicon(icon.value)
        }
      }
    }
  }

  /**
   * 更新 favicon
   */
  const updateFavicon = (iconUrl) => {
    let link = document.querySelector("link[rel*='icon']")
    if (!link) {
      link = document.createElement('link')
      link.rel = 'icon'
      document.head.appendChild(link)
    }
    link.href = iconUrl
  }

  return {
    systemName,
    logo,
    icon,
    settings,
    loaded,
    hasLogo,
    hasIcon,
    loadPublicInfo,
    loadAllSettings,
    updateSystemSetting,
    updateSystemSettings
  }
})
