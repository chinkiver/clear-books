<template>
  <el-container class="layout-container">
    <!-- Header -->
    <el-header class="layout-header">
      <div class="logo">
        <img v-if="systemStore.hasLogo" :src="systemStore.logo" class="logo-img" alt="logo" />
        <el-icon v-else size="24"><Wallet /></el-icon>
        <span class="logo-text">{{ systemStore.systemName }}</span>
      </div>
      <div class="header-right">
        <el-dropdown @command="handleCommand">
          <span class="user-info">
            {{ userStore.nickname || userStore.username }}
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人信息</el-dropdown-item>
              <el-dropdown-item v-if="userStore.isAdmin" command="system">系统设置</el-dropdown-item>
              <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>

    <el-container class="main-container">
      <!-- Sidebar -->
      <el-aside width="200px" class="layout-aside">
        <el-menu
          :default-active="$route.path"
          router
          class="layout-menu"
          background-color="#ffffff"
          text-color="#606266"
          active-text-color="#409EFF"
        >
          <el-menu-item index="/dashboard">
            <el-icon><House /></el-icon>
            <span>首页</span>
          </el-menu-item>
          <el-menu-item index="/transactions">
            <el-icon><Document /></el-icon>
            <span>流水管理</span>
          </el-menu-item>
          <el-menu-item index="/accounts">
            <el-icon><Wallet /></el-icon>
            <span>账户管理</span>
          </el-menu-item>
          <el-menu-item index="/categories">
            <el-icon><Collection /></el-icon>
            <span>分类管理</span>
          </el-menu-item>
          <el-menu-item index="/payment-methods">
            <el-icon><CreditCard /></el-icon>
            <span>支付方式</span>
          </el-menu-item>
          <el-menu-item index="/statistics">
            <el-icon><TrendCharts /></el-icon>
            <span>统计报表</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <!-- Main Content -->
      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useSystemStore } from '@/stores/system'

const router = useRouter()
const userStore = useUserStore()
const systemStore = useSystemStore()

// 加载系统信息
systemStore.loadPublicInfo()

const handleCommand = (command) => {
  if (command === 'profile') {
    router.push('/settings')
  } else if (command === 'system') {
    router.push('/system-settings')
  } else if (command === 'logout') {
    ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      userStore.logout()
      ElMessage.success('已退出登录')
      router.push('/login')
    })
  }
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.layout-header {
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #409EFF;
}

.logo-img {
  width: 32px;
  height: 32px;
  object-fit: contain;
}

.logo-text {
  font-size: 18px;
  font-weight: bold;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 5px;
  color: #606266;
}

.main-container {
  flex: 1;
  overflow: hidden;
}

.layout-aside {
  background: #fff;
  border-right: 1px solid #e4e7ed;
}

.layout-menu {
  border-right: none;
  height: 100%;
}

.layout-main {
  background: #f5f7fa;
  padding: 20px;
  overflow-y: auto;
}
</style>
