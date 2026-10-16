<template>
  <div class="auth-page">
    <BrandPanel />

    <!-- 右侧表单区 -->
    <div class="form-panel">
      <div class="auth-card">
        <h2 class="form-title">注册账号</h2>
        <p class="form-desc">加入编辑部，默认普通用户权限</p>

        <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="onSubmit">
          <el-form-item prop="username">
            <el-input
              v-model="form.username"
              placeholder="用户名（3-50 位）"
              :prefix-icon="User"
              clearable
              maxlength="50"
              autocomplete="new-username"
            />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="密码（6-64 位）"
              :prefix-icon="Lock"
              show-password
              maxlength="64"
              autocomplete="new-password"
            />
          </el-form-item>
          <el-form-item prop="confirmPassword">
            <el-input
              v-model="form.confirmPassword"
              type="password"
              placeholder="确认密码"
              :prefix-icon="Lock"
              show-password
              maxlength="64"
              autocomplete="new-password"
            />
          </el-form-item>
          <el-form-item prop="email">
            <el-input
              v-model="form.email"
              placeholder="邮箱（选填，用于找回密码）"
              :prefix-icon="Message"
              clearable
              maxlength="100"
            />
          </el-form-item>

          <el-button type="primary" class="submit-btn" :loading="loading" @click="onSubmit">
            注 册
          </el-button>
        </el-form>

        <div class="form-footer">
          已有账号？
          <el-link type="primary" :underline="false" @click="router.push('/login')">直接登录</el-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock, Message } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { registerApi } from '@/api/auth'
import BrandPanel from './BrandPanel.vue'

const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  email: ''
})

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度 3-50 位', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 64, message: '密码长度 6-64 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== form.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
}

async function onSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await registerApi({
      username: form.username,
      password: form.password,
      email: form.email || undefined
    })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch {
    // 错误提示已由请求层统一处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
@use '@/assets/styles/variables.scss' as *;

.auth-page {
  display: flex;
  min-height: 100vh;
  background-color: $bg-page;
}

.form-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px;
  box-sizing: border-box;
}

.auth-card {
  width: 100%;
  max-width: 400px;
  background-color: $bg-surface;
  border: 1px solid $border-default;
  border-radius: $radius-md;
  box-shadow: none;
  padding: 40px 36px;
  box-sizing: border-box;
}

.form-title {
  font-size: 24px;
  font-weight: 600;
  margin: 0 0 4px;
}

.form-desc {
  font-size: 13px;
  color: $text-secondary;
  margin: 0 0 28px;
}

.submit-btn {
  width: 100%;
  height: 42px;
  font-size: 14px;
}

.form-footer {
  margin-top: 20px;
  font-size: 13px;
  color: $text-secondary;
  text-align: center;
}

@media (max-width: 900px) {
  .auth-page {
    flex-direction: column;
  }
}
</style>
