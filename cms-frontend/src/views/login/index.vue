<template>
  <div class="login-page">
    <!-- 左侧品牌区（编辑部气质，无渐变） -->
    <div class="brand-panel">
      <div class="brand-logo">
        <span class="logo-mark">编</span>
        <span class="brand-name">CMS 内容管理系统</span>
      </div>

      <h1 class="font-display slogan">
        内容管理，<br />像编辑一份刊物。
      </h1>
      <p class="slogan-sub">
        稿件台 · 审核流 · AI 辅助写作，一个安静、专业的内容工作台。
      </p>

      <!-- 签名元素微示：稿件状态色带 -->
      <div class="brand-samples">
        <div class="sample-row status-draft">
          <span class="sample-id">#0241</span><span>草稿 · 等待落笔</span>
        </div>
        <div class="sample-row status-reviewing">
          <span class="sample-id">#0242</span><span>审核中 · 2 条批注</span>
        </div>
        <div class="sample-row status-published">
          <span class="sample-id">#0243</span><span>已发布 · 阅读 1.2k</span>
        </div>
      </div>
    </div>

    <!-- 右侧登录表单（白卡 8px 圆角无阴影） -->
    <div class="form-panel">
      <div class="login-card">
        <h2 class="form-title">登录</h2>
        <p class="form-desc">使用账号进入编辑部</p>

        <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="onSubmit">
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" clearable />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="密码"
              :prefix-icon="Lock"
              show-password
            />
          </el-form-item>

          <div class="form-options">
            <el-checkbox v-model="remember">记住我</el-checkbox>
            <el-link type="primary" :underline="false" @click="onForgot">忘记密码</el-link>
          </div>

          <el-button
            type="primary"
            class="submit-btn"
            :loading="loading"
            @click="onSubmit"
          >
            登 录
          </el-button>
        </el-form>

        <div class="form-footer">
          还没有账号？
          <el-link type="primary" :underline="false" @click="onRegister">注册</el-link>
        </div>

        <p v-if="demoMode" class="demo-hint">骨架演示模式（VITE_DEMO_MODE）：输入任意账号密码即可进入</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const remember = ref(true)

/** 联调/演示开关：VITE_DEMO_MODE=true 时跳过真实请求 */
const demoMode = import.meta.env.VITE_DEMO_MODE === 'true'

const form = reactive({
  username: '',
  password: ''
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' }
  ]
}

/** 骨架演示模式：后端未就绪时用假 token 进入（生产移除） */
function demoLogin(): boolean {
  const demo = import.meta.env.VITE_DEMO_MODE === 'true'
  if (!demo) return false
  userStore.setUserInfo({ id: 1, username: form.username, nickname: '演示编辑', roles: ['ADMIN'] })
  ElMessage.success('演示模式登录成功')
  return true
}

async function onSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    if (!demoLogin()) {
      await userStore.login({ username: form.username, password: form.password })
      // 拉取用户信息（含角色），用于按权限决定登录后去向
      await userStore.fetchUserInfo()
    }
    const isCreator = userStore.isCreator
    // 普通用户不进入后台：仅当明确目标是个人信息/门户时保留，否则一律去内容门户（文档 7.3）
    let redirect = route.query.redirect as string | undefined
    if (!isCreator && redirect !== '/site' && redirect !== '/profile') {
      redirect = '/site'
    }
    router.push(redirect || (isCreator ? '/dashboard' : '/site'))
  } catch {
    // 错误提示已在 request 层统一处理
  } finally {
    loading.value = false
  }
}

function onForgot() {
  router.push('/forgot-password')
}

function onRegister() {
  router.push('/register')
}
</script>

<style scoped lang="scss">
@use '@/assets/styles/variables.scss' as *;

.login-page {
  display: flex;
  min-height: 100vh;
  background-color: $bg-page;
}

/* 左：品牌区 56% */
.brand-panel {
  flex: 0 0 56%;
  padding: 48px 64px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  box-sizing: border-box;
}

.brand-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 48px;
}

.logo-mark {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  background-color: $brand-primary;
  color: #fff;
  font-family: $font-display;
  font-size: 17px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.brand-name {
  font-size: 15px;
  font-weight: 600;
  color: $text-primary;
}

.slogan {
  font-size: 40px;
  line-height: 1.3;
  font-weight: 600;
  color: $text-primary;
  margin: 0 0 16px;
}

.slogan-sub {
  font-size: 14px;
  color: $text-secondary;
  margin: 0 0 40px;
  max-width: 340px;
}

/* 签名元素微示 */
.brand-samples {
  max-width: 300px;
}

.sample-row {
  position: relative;
  padding: 8px 0 8px 16px;
  font-size: 13px;
  color: $text-secondary;
  display: flex;
  gap: 10px;
}

.sample-row::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 18px;
  border-radius: 1px;
  background: var(--status-color);
}

.sample-id {
  font-family: $font-mono;
  font-size: 12px;
  color: $text-tertiary;
}

/* 右：表单 44% */
.form-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px;
  box-sizing: border-box;
}

.login-card {
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

.form-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
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

.demo-hint {
  margin-top: 16px;
  font-size: 12px;
  color: $text-tertiary;
  text-align: center;
}

/* 响应式：窄屏单列 */
@media (max-width: 900px) {
  .login-page {
    flex-direction: column;
  }

  .brand-panel {
    flex: none;
    padding: 32px 24px;
  }

  .brand-samples {
    display: none;
  }
}
</style>
