<template>
  <div class="page">
    <!-- 页头 -->
    <div class="page-head">
      <h1 class="page-title">用户管理</h1>
      <p class="page-sub">管理账号、角色与账号状态 · 稿件台编号 USER-{id}</p>
    </div>

    <!-- 工具栏 -->
    <div class="card toolbar">
      <el-input
        v-model="query.keyword"
        placeholder="搜索用户名 / 昵称"
        clearable
        class="search-input"
        @keyup.enter="handleSearch"
        @clear="handleSearch"
      />
      <el-select v-model="query.status" placeholder="全部状态" clearable class="status-select" @change="handleSearch">
        <el-option label="正常" :value="1" />
        <el-option label="禁用" :value="0" />
      </el-select>
      <el-button class="btn-brand" @click="handleSearch">查询</el-button>
      <el-button v-if="userStore.hasPermission('user:manage')" type="primary" @click="openCreate">新建用户</el-button>
    </div>

    <!-- 用户表格 -->
    <div class="card">
      <el-table :data="rows" v-loading="loading" class="cms-table" row-key="id">
        <el-table-column label="用户" min-width="200">
          <template #default="{ row }">
            <div class="user-cell">
              <span class="status-bar" :class="row.status === 1 ? 'is-active' : 'is-disabled'" />
              <div class="user-main">
                <div class="user-name">
                  <span class="mono-id">USER-{{ String(row.id).padStart(3, '0') }}</span>
                  <span class="nickname">{{ row.nickname || '—' }}</span>
                </div>
                <div class="user-sub">@{{ row.username }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="邮箱" min-width="180" prop="email">
          <template #default="{ row }">{{ row.email || '—' }}</template>
        </el-table-column>
        <el-table-column label="角色" min-width="160">
          <template #default="{ row }">
            <span v-if="row.roles && row.roles.length" class="role-tags">
              <el-tag v-for="r in row.roles" :key="r" size="small" class="role-tag">{{ r }}</el-tag>
            </span>
            <span v-else class="text-muted">—</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <span class="status-label" :class="row.status === 1 ? 'ok' : 'off'">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="最后登录" min-width="160">
          <template #default="{ row }">{{ row.lastLoginTime ? formatTime(row.lastLoginTime) : '—' }}</template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="160">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-if="userStore.hasPermission('user:manage')" link type="primary" @click="openEdit(row)">编辑</el-button>
            <template v-if="userStore.hasPermission('user:manage') && !(row.roles && row.roles.includes('ADMIN'))">
              <el-button link :type="row.status === 1 ? 'warning' : 'success'" @click="toggleStatus(row)">
                {{ row.status === 1 ? '禁用' : '启用' }}
              </el-button>
              <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        class="pager"
        @current-change="load"
        @size-change="load"
      />
    </div>

    <!-- 新建 / 编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑用户' : '新建用户'"
      width="520px"
      class="cms-dialog"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px">
        <el-form-item label="用户名" prop="username" v-if="!editingId">
          <el-input v-model="form.username" placeholder="3-50 位" maxlength="50" autocomplete="new-username" />
        </el-form-item>
        <el-form-item :label="editingId ? '新密码' : '密码'" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            :placeholder="editingId ? '留空则不修改密码' : '6-64 位'"
            maxlength="64"
            autocomplete="new-password"
          />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="显示名称" maxlength="50" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="user@example.com" maxlength="100" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="选填" maxlength="20" />
        </el-form-item>
        <el-form-item label="角色" prop="roleIds">
          <el-select v-model="form.roleIds" multiple placeholder="不选则默认为普通用户" class="role-select">
            <el-option v-for="r in roleOptions" :key="r.id" :label="`${r.name}（${r.code}）`" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useUserStore } from '@/store/user'
const userStore = useUserStore()
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  pageUsersApi,
  createUserApi,
  updateUserApi,
  deleteUserApi,
  listRolesApi,
  type UserItem,
  type RoleOption
} from '@/api/admin'

const rows = ref<UserItem[]>([])
const total = ref(0)
const loading = ref(false)
const roleOptions = ref<RoleOption[]>([])
const dialogVisible = ref(false)
const submitting = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()

const query = reactive({ page: 1, size: 10, keyword: '', status: undefined as number | undefined })

const form = reactive<{
  username?: string
  nickname?: string
  email?: string
  phone?: string
  password?: string
  status: number
  roleIds?: number[]
}>({ status: 1 })

const formRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '长度 3-50 位', trigger: 'blur' }
  ],
  password: [
    { min: 6, max: 64, message: '密码长度 6-64 位', trigger: 'blur' },
    {
      validator: (_r, value, cb) => {
        if (!editingId.value && !value) cb(new Error('新建用户必须设置密码'))
        else cb()
      },
      trigger: 'blur'
    }
  ],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
}

function formatTime(t?: string): string {
  if (!t) return '—'
  return t.replace('T', ' ').slice(0, 19)
}

async function load() {
  loading.value = true
  try {
    const res = await pageUsersApi({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
      status: query.status
    })
    rows.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.page = 1
  load()
}

async function loadRoles() {
  try {
    roleOptions.value = await listRolesApi()
  } catch {
    roleOptions.value = []
  }
}

function openCreate() {
  editingId.value = null
  Object.assign(form, { username: '', nickname: '', email: '', phone: '', password: '', status: 1, roleIds: [] })
  dialogVisible.value = true
}

function openEdit(row: UserItem) {
  editingId.value = row.id
  Object.assign(form, {
    username: row.username,
    nickname: row.nickname,
    email: row.email || '',
    phone: row.phone || '',
    password: '',
    status: row.status,
    roleIds: []
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate()
  submitting.value = true
  try {
    const payload = {
      nickname: form.nickname || undefined,
      email: form.email || undefined,
      phone: form.phone || undefined,
      password: form.password || undefined,
      status: form.status,
      roleIds: form.roleIds && form.roleIds.length ? form.roleIds : undefined
    }
    if (editingId.value) {
      await updateUserApi(editingId.value, payload)
      ElMessage.success('用户已更新')
    } else {
      await createUserApi({ ...payload, username: form.username })
      ElMessage.success('用户已创建')
    }
    dialogVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

async function toggleStatus(row: UserItem) {
  const next = row.status === 1 ? 0 : 1
  await updateUserApi(row.id, { status: next })
  ElMessage.success(next === 1 ? '已启用' : '已禁用')
  load()
}

async function handleDelete(row: UserItem) {
  await ElMessageBox.confirm(`确认删除用户 @${row.username}？该操作不可恢复。`, '删除用户', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  })
  await deleteUserApi(row.id)
  ElMessage.success('用户已删除')
  load()
}

onMounted(() => {
  load()
  loadRoles()
})
</script>

<style scoped lang="scss">
@use '@/assets/styles/variables.scss' as *;

.page {
  padding: $space-5;
  max-width: $content-max-width;
}

.page-head {
  margin-bottom: $space-5;
}

.page-title {
  font-family: $font-display;
  font-size: 24px;
  font-weight: 600;
  color: $text-primary;
  margin: 0 0 4px;
}

.page-sub {
  font-size: 13px;
  color: $text-secondary;
  margin: 0;
}

.toolbar {
  display: flex;
  gap: $space-3;
  align-items: center;
  margin-bottom: $space-4;
  padding: $space-4;
}

.search-input {
  width: 240px;
}

.status-select {
  width: 130px;
}

.user-cell {
  display: flex;
  align-items: stretch;
  gap: $space-3;
}

.status-bar {
  width: 3px;
  border-radius: 2px;
  flex-shrink: 0;
  &.is-active {
    background-color: $status-published;
  }
  &.is-disabled {
    background-color: $status-draft;
  }
}

.user-main {
  min-width: 0;
}

.user-name {
  display: flex;
  align-items: baseline;
  gap: $space-2;
}

.mono-id {
  font-family: $font-mono;
  font-size: 12px;
  color: $text-tertiary;
}

.nickname {
  font-size: 14px;
  font-weight: 500;
  color: $text-primary;
}

.user-sub {
  font-size: 12px;
  color: $text-secondary;
  margin-top: 2px;
}

.role-tags {
  display: inline-flex;
  gap: 4px;
  flex-wrap: wrap;
}

.role-tag {
  font-family: $font-mono;
}

.status-label {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 12px;
  &.ok {
    background-color: rgba(62, 124, 79, 0.12);
    color: $status-published;
  }
  &.off {
    background-color: rgba(154, 161, 171, 0.16);
    color: $text-secondary;
  }
}

.text-muted {
  color: $text-tertiary;
}

.pager {
  margin-top: $space-4;
  justify-content: flex-end;
}

.role-select {
  width: 100%;
}
</style>
