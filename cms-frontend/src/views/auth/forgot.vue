<template>
  <div class="auth-page">
    <BrandPanel />

    <!-- 右侧表单区 -->
    <div class="form-panel">
      <div class="auth-card">
        <h2 class="form-title">找回密码</h2>
        <p class="form-desc">输入注册邮箱，重置链接将在 30 分钟内有效</p>

        <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="onSubmit">
          <el-form-item prop="email">
            <el-input
              v-model="form.email"
              placeholder="注册邮箱"
              :prefix-icon="Message"
              clearable
              maxlength="100"
            />
          </el-form-item>

          <el-button type="primary" class="submit-btn" :loading="loading" @click="onSubmit">
            发送重置邮件
          </el-button>
        </el-form>

        <div class="form-footer">
          <el-link type="primary" :underline="false" @click="router.push('/login')">返回登录</el-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Message } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { forgotPasswordApi } from '@/api/auth'
import BrandPanel from './BrandPanel.vue'

const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({ email: '' })

const rules: FormRules = {
  email: [
    { required: true, message: '请输入注册邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ]
}

async function onSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await forgotPasswordApi({ email: form.email })
    ElMessage.success('如果该邮箱已注册，重置邮件已发送，请查收')
    // 开发环境未配置 SMTP 时，后端仅记日志；提示用户可查看后端日志中的重置链接
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
