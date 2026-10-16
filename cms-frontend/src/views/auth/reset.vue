<template>
  <div class="auth-page">
    <BrandPanel />

    <!-- 右侧表单区 -->
    <div class="form-panel">
      <div class="auth-card">
        <template v-if="hasToken">
          <h2 class="form-title">重置密码</h2>
          <p class="form-desc">设置新密码，重置链接仅可使用一次</p>

          <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="onSubmit">
            <el-form-item prop="password">
              <el-input
                v-model="form.password"
                type="password"
                placeholder="新密码（6-64 位）"
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
                placeholder="确认新密码"
                :prefix-icon="Lock"
                show-password
                maxlength="64"
                autocomplete="new-password"
              />
            </el-form-item>

            <el-button type="primary" class="submit-btn" :loading="loading" @click="onSubmit">
              确认重置
            </el-button>
          </el-form>
        </template>

        <template v-else>
          <h2 class="form-title">链接无效</h2>
          <p class="form-desc">重置链接缺失或已失效，请重新发起找回密码</p>
          <el-button type="primary" class="submit-btn" @click="router.push('/forgot-password')">
            重新找回
          </el-button>
        </template>

        <div class="form-footer">
          <el-link type="primary" :underline="false" @click="router.push('/login')">返回登录</el-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Lock } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { resetPasswordApi } from '@/api/auth'
import BrandPanel from './BrandPanel.vue'

const route = useRoute()
const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)

/** 重置令牌来自邮件链接：/reset-password?token=xxx */
const token = (route.query.token as string) || ''
const hasToken = computed(() => token.length > 0)

const form = reactive({
  password: '',
  confirmPassword: ''
})

const rules: FormRules = {
  password: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 64, message: '密码长度 6-64 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
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
  ]
}

async function onSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await resetPasswordApi({ token, password: form.password })
    ElMessage.success('密码已重置，请使用新密码登录')
    router.push('/login')
  } catch {
    // 错误提示已由请求层统一处理（令牌无效/过期会提示）
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
