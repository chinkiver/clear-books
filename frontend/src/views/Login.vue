<template>
  <div class="login-container">
    <el-card class="login-card">
      <template #header>
        <div class="login-header">
          <img v-if="systemStore.logo" :src="systemStore.logo" class="login-logo" alt="logo" />
          <h2 class="login-title">{{ systemStore.systemName }}</h2>
        </div>
      </template>
      
      <el-tabs v-model="activeTab">
        <el-tab-pane label="登录" name="login">
          <el-form :model="loginForm" :rules="rules" ref="loginRef" label-position="top">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="loginForm.username" placeholder="请输入用户名" prefix-icon="User" />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" prefix-icon="Lock" show-password />
            </el-form-item>
            <el-form-item label="验证码" prop="captchaCode">
              <div class="captcha-wrapper">
                <el-input 
                  v-model="loginForm.captchaCode" 
                  placeholder="请输入验证码" 
                  prefix-icon="Key"
                  maxlength="4"
                  @keyup.enter="handleLogin"
                />
                <div class="captcha-image" @click="refreshCaptcha" :class="{ loading: captchaLoading }">
                  <img v-if="captchaImage" :src="captchaImage" alt="验证码" @load="captchaLoading = false" />
                  <el-icon v-else-if="captchaLoading" class="loading-icon"><Loading /></el-icon>
                  <span v-else class="captcha-placeholder">点击刷新</span>
                </div>
              </div>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" class="submit-btn" @click="handleLogin" :loading="loading">登录</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        
        <el-tab-pane label="注册" name="register">
          <el-form :model="registerForm" :rules="registerRules" ref="registerRef" label-position="top">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="registerForm.username" placeholder="请输入用户名" prefix-icon="User" />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input v-model="registerForm.password" type="password" placeholder="请输入密码" prefix-icon="Lock" show-password />
            </el-form-item>
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="registerForm.nickname" placeholder="请输入昵称（可选）" prefix-icon="UserFilled" />
            </el-form-item>
            <el-form-item label="验证码" prop="captchaCode">
              <div class="captcha-wrapper">
                <el-input 
                  v-model="registerForm.captchaCode" 
                  placeholder="请输入验证码" 
                  prefix-icon="Key"
                  maxlength="4"
                  @keyup.enter="handleRegister"
                />
                <div class="captcha-image" @click="refreshCaptcha" :class="{ loading: captchaLoading }">
                  <img v-if="captchaImage" :src="captchaImage" alt="验证码" @load="captchaLoading = false" />
                  <el-icon v-else-if="captchaLoading" class="loading-icon"><Loading /></el-icon>
                  <span v-else class="captcha-placeholder">点击刷新</span>
                </div>
              </div>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" class="submit-btn" @click="handleRegister" :loading="loading">注册</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login, register, getCaptcha } from '@/api/auth'
import { useUserStore } from '@/stores/user'
import { useSystemStore } from '@/stores/system'

const router = useRouter()
const userStore = useUserStore()
const systemStore = useSystemStore()

const activeTab = ref('login')
const loading = ref(false)
const loginRef = ref()
const registerRef = ref()
const captchaImage = ref('')
const captchaKey = ref('')
const captchaLoading = ref(false)

const loginForm = reactive({
  username: '',
  password: '',
  captchaCode: '',
  captchaKey: ''
})

const registerForm = reactive({
  username: '',
  password: '',
  nickname: '',
  captchaCode: '',
  captchaKey: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captchaCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

const registerRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captchaCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

// 加载验证码
const refreshCaptcha = async () => {
  if (captchaLoading.value) return
  
  captchaLoading.value = true
  captchaImage.value = ''
  
  try {
    const res = await getCaptcha()
    
    // 响应拦截器已解包 data，直接使用 res
    if (res && res.captchaImage) {
      captchaImage.value = res.captchaImage
      captchaKey.value = res.captchaKey
      loginForm.captchaKey = res.captchaKey
      registerForm.captchaKey = res.captchaKey
    }
  } catch (error) {
    console.error('获取验证码失败:', error)
    ElMessage.error('获取验证码失败')
  } finally {
    captchaLoading.value = false
  }
}

// 切换标签页时刷新验证码
watch(activeTab, () => {
  loginForm.captchaCode = ''
  registerForm.captchaCode = ''
  refreshCaptcha()
})

// 加载系统信息
onMounted(() => {
  systemStore.loadPublicInfo()
  refreshCaptcha()
})

const handleLogin = async () => {
  await loginRef.value.validate()
  loading.value = true
  try {
    const res = await login(loginForm)
    userStore.setToken(res.token)
    userStore.setUserInfo(res.user)
    ElMessage.success('登录成功')
    router.push('/')
  } finally {
    loading.value = false
  }
}

const handleRegister = async () => {
  await registerRef.value.validate()
  loading.value = true
  try {
    const res = await register(registerForm)
    userStore.setToken(res.token)
    userStore.setUserInfo(res.user)
    ElMessage.success('注册成功')
    router.push('/')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: #f5f7fa;
}

.login-card {
  width: 420px;
}

.login-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.login-logo {
  width: 48px;
  height: 48px;
  object-fit: contain;
}

.login-title {
  text-align: center;
  margin: 0;
  color: #303133;
}

.captcha-wrapper {
  display: flex;
  gap: 10px;
  align-items: center;
}

.captcha-wrapper .el-input {
  flex: 1;
}

.captcha-image {
  width: 120px;
  height: 40px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  cursor: pointer;
  overflow: hidden;
  display: flex;
  justify-content: center;
  align-items: center;
  background: #f5f7fa;
  transition: all 0.3s;
}

.captcha-image:hover {
  border-color: #409eff;
}

.captcha-image.loading {
  opacity: 0.6;
  cursor: not-allowed;
}

.captcha-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.loading-icon {
  font-size: 20px;
  color: #909399;
  animation: rotating 2s linear infinite;
}

@keyframes rotating {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.captcha-placeholder {
  font-size: 12px;
  color: #909399;
}

.submit-btn {
  width: 100%;
}
</style>
