<template>
  <div class="page">
    <!-- 页头 -->
    <div class="page-head">
      <h1 class="page-title">个人信息</h1>
      <p class="page-sub">账号资料与头像 · 修改后全局生效</p>
    </div>

    <div class="profile-grid">
      <!-- 左：头像卡 -->
      <div class="card avatar-card">
        <div class="avatar-wrap">
          <el-avatar :size="88" :src="form.avatar || undefined" class="big-avatar">
            {{ userStore.displayName.slice(0, 1) }}
          </el-avatar>
        </div>
        <p class="avatar-tip">支持上传图片作为头像</p>
        <el-upload
          :show-file-list="false"
          :before-upload="beforeAvatarUpload"
          :http-request="uploadAvatar"
          accept="image/*"
        >
          <el-button class="btn-brand" :loading="uploading">更换头像</el-button>
        </el-upload>
        <el-button v-if="form.avatar" link type="info" class="clear-avatar" @click="form.avatar = ''">
          移除头像
        </el-button>
      </div>

      <!-- 右：资料表单 -->
      <div class="card form-card">
        <el-form ref="formRef" :model="form" :rules="rules" label-width="80px" class="profile-form">
          <el-form-item label="用户名">
            <span class="username-readonly">@{{ userStore.userInfo?.username }}</span>
            <span class="text-muted username-note">用户名不可修改</span>
          </el-form-item>
          <el-form-item label="昵称" prop="nickname">
            <el-input v-model="form.nickname" placeholder="显示名称" maxlength="50" />
          </el-form-item>
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="form.email" placeholder="user@example.com（用于找回密码）" maxlength="100" />
          </el-form-item>
          <el-form-item label="角色">
            <span v-if="userStore.roles.length" class="role-tags">
              <el-tag v-for="r in userStore.roles" :key="r" size="small" class="role-tag">{{ r }}</el-tag>
            </span>
            <span v-else class="text-muted">—</span>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" class="save-btn" :loading="saving" @click="onSave">保存修改</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>

    <!-- 修改密码 -->
    <div class="card password-card">
      <div class="password-card-head">
        <h3 class="password-title">修改密码</h3>
        <p class="password-tip">校验原密码后更新，无需邮箱验证</p>
      </div>
      <el-form
        ref="pwdRef"
        :model="pwdForm"
        :rules="pwdRules"
        label-width="80px"
        class="profile-form password-form"
      >
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="当前登录密码" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="6-64 位" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="再次输入新密码" />
        </el-form-item>
        <el-form-item>
          <el-button class="btn-brand" :loading="pwdSaving" @click="onChangePassword">更新密码</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { changePasswordApi, updateUserInfoApi } from '@/api/auth'
import { uploadMediaApi } from '@/api/media'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const formRef = ref<FormInstance>()
const saving = ref(false)
const uploading = ref(false)
const pwdRef = ref<FormInstance>()
const pwdSaving = ref(false)

const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const pwdRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 64, message: '新密码长度需在 6-64 位之间', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value: string, callback) => {
        if (value !== pwdForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const form = reactive({
  nickname: '',
  email: '',
  avatar: ''
})

const rules: FormRules = {
  nickname: [{ max: 50, message: '昵称长度不超过 50 位', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
}

onMounted(() => {
  const info = userStore.userInfo
  if (info) {
    form.nickname = info.nickname || ''
    form.email = info.email || ''
    form.avatar = info.avatar || ''
  }
})

/** 上传前校验：仅图片、≤2MB */
function beforeAvatarUpload(file: File): boolean {
  if (!file.type.startsWith('image/')) {
    ElMessage.error('头像仅支持图片格式')
    return false
  }
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.error('头像图片不能超过 2MB')
    return false
  }
  return true
}

/** 上传头像：走媒体库接口拿 URL（随机文件名，防覆盖） */
async function uploadAvatar(option: { file: File }) {
  uploading.value = true
  try {
    const media = await uploadMediaApi(option.file)
    form.avatar = media.url
    ElMessage.success('头像已上传，保存后生效')
  } catch {
    // 错误提示已由请求层统一处理
  } finally {
    uploading.value = false
  }
}

async function onSave() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    await updateUserInfoApi({
      nickname: form.nickname,
      email: form.email,
      avatar: form.avatar
    })
    // 刷新全局用户信息（顶栏昵称/头像即时生效）
    await userStore.fetchUserInfo()
    ElMessage.success('个人信息已保存')
  } catch {
    // 错误提示已由请求层统一处理
  } finally {
    saving.value = false
  }
}

/** 修改密码：校验旧密码 → 更新（无需邮件） */
async function onChangePassword() {
  if (!pwdRef.value) return
  const valid = await pwdRef.value.validate().catch(() => false)
  if (!valid) return

  pwdSaving.value = true
  try {
    await changePasswordApi({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword
    })
    ElMessage.success('密码已修改，下次登录请使用新密码')
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirmPassword = ''
    pwdRef.value.clearValidate()
  } catch {
    // 错误提示已由请求层统一处理（原密码错误 → 1004）
  } finally {
    pwdSaving.value = false
  }
}
</script>

<style scoped lang="scss">
@use '@/assets/styles/variables.scss' as *;

.profile-grid {
  display: flex;
  gap: $space-5;
  flex-wrap: wrap;
  align-items: flex-start;
}

.avatar-card {
  flex: 0 0 260px;
  padding: $space-6 $space-5;
  text-align: center;
}

.avatar-wrap {
  display: flex;
  justify-content: center;
  margin-bottom: $space-3;
}

.big-avatar {
  background-color: $brand-primary;
  font-size: 32px;
  color: #fff;
}

.avatar-tip {
  font-size: 12px;
  color: $text-tertiary;
  margin: 0 0 $space-4;
}

.clear-avatar {
  display: block;
  margin: $space-2 auto 0;
}

.form-card {
  flex: 1 1 460px;
  min-width: 320px;
  padding: $space-6;
}

.profile-form {
  max-width: 460px;
}

.username-readonly {
  font-family: $font-mono;
  font-size: 13px;
  color: $text-primary;
  margin-right: $space-2;
}

.username-note {
  font-size: 12px;
}

.role-tags {
  display: flex;
  gap: $space-2;
  flex-wrap: wrap;
}

.save-btn {
  min-width: 120px;
}

.password-card {
  margin-top: $space-5;
  max-width: 640px;
  padding: $space-6;
}

.password-card-head {
  margin-bottom: $space-4;
}

.password-title {
  margin: 0 0 $space-1;
  font-size: 15px;
  font-weight: 600;
  color: $text-primary;
}

.password-tip {
  margin: 0;
  font-size: 12px;
  color: $text-tertiary;
}

.password-form {
  max-width: 460px;
}
</style>
