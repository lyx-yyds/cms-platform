<template>
  <div class="page">
    <!-- 页头 -->
    <div class="page-head">
      <h1 class="page-title">角色管理</h1>
      <p class="page-sub">RBAC 角色与权限分配 · 内置角色（ADMIN/EDITOR/AUTHOR/USER）不可删除</p>
    </div>

    <!-- 工具栏 -->
    <div class="card toolbar">
      <div class="toolbar-left">
        <el-button type="primary" @click="openCreate">新建角色</el-button>
      </div>
      <span class="text-muted">共 {{ roles.length }} 个角色</span>
    </div>

    <!-- 角色表格 -->
    <div class="card">
      <el-table :data="roles" v-loading="loading" class="cms-table" row-key="id">
        <el-table-column label="角色" min-width="220">
          <template #default="{ row }">
            <div class="role-cell">
              <span class="mono-id">ROLE-{{ String(row.id).padStart(2, '0') }}</span>
              <span class="role-name">{{ row.name }}</span>
              <el-tag v-if="row.id <= 4" size="small" class="builtin-tag" type="info">内置</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="编码" min-width="120">
          <template #default="{ row }">
            <span class="mono-code">{{ row.code }}</span>
          </template>
        </el-table-column>
        <el-table-column label="描述" min-width="200" prop="description">
          <template #default="{ row }">{{ row.description || '—' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <span class="status-label" :class="row.status === 1 ? 'ok' : 'off'">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="权限" min-width="260">
          <template #default="{ row }">
            <span v-if="row.permissions && row.permissions.length" class="perm-tags">
              <el-tag v-for="p in row.permissions.slice(0, 6)" :key="p" size="small" class="perm-tag">
                {{ permName(p) }}
              </el-tag>
              <span v-if="row.permissions.length > 6" class="perm-more">+{{ row.permissions.length - 6 }}</span>
            </span>
            <span v-else class="text-muted">—</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" :disabled="row.id <= 4" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 新建 / 编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑角色' : '新建角色'"
      width="560px"
      class="cms-dialog"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px">
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="form.name" placeholder="如：内容编辑" maxlength="50" />
        </el-form-item>
        <el-form-item label="角色编码" prop="code">
          <el-input v-model="form.code" placeholder="如：EDITOR（唯一，一般用大写）" maxlength="50" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" placeholder="角色职责说明（选填）" maxlength="200" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="权限">
          <div class="perm-picker">
            <el-checkbox
              v-for="p in permissions"
              :key="p.id"
              :model-value="selectedPermIds.includes(p.id)"
              class="perm-check"
              @change="(v: boolean | string | number) => togglePerm(p.id, v)"
            >
              {{ p.name }}<span class="perm-code">{{ p.code }}</span>
            </el-checkbox>
            <span v-if="!permissions.length" class="text-muted">权限列表加载中…</span>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  listRolesApi,
  listPermissionsApi,
  createRoleApi,
  updateRoleApi,
  deleteRoleApi,
  type RoleOption,
  type PermissionItem
} from '@/api/admin'

const loading = ref(false)
const saving = ref(false)
const roles = ref<RoleOption[]>([])
const permissions = ref<PermissionItem[]>([])

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()

const form = reactive({
  name: '',
  code: '',
  description: '',
  status: 1
})

/** 弹窗内已勾选权限 ID */
const selectedPermIds = ref<number[]>([])

const formRules: FormRules = {
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  code: [
    { required: true, message: '请输入角色编码', trigger: 'blur' },
    { pattern: /^[A-Za-z][A-Za-z0-9_]{1,49}$/, message: '编码需以字母开头，仅含字母/数字/下划线', trigger: 'blur' }
  ]
}

async function load() {
  loading.value = true
  try {
    roles.value = await listRolesApi()
  } finally {
    loading.value = false
  }
}

/** 权限编码 → 中文名（无匹配则回退编码） */
function permName(code: string): string {
  const hit = permissions.value.find((p) => p.code === code)
  return hit ? hit.name : code
}

function togglePerm(id: number, checked: boolean | string | number) {
  if (checked) {
    if (!selectedPermIds.value.includes(id)) selectedPermIds.value.push(id)
  } else {
    selectedPermIds.value = selectedPermIds.value.filter((x) => x !== id)
  }
}

function openCreate() {
  editingId.value = null
  form.name = ''
  form.code = ''
  form.description = ''
  form.status = 1
  selectedPermIds.value = []
  dialogVisible.value = true
}

function openEdit(row: RoleOption) {
  editingId.value = row.id
  form.name = row.name
  form.code = row.code
  form.description = row.description || ''
  form.status = row.status ?? 1
  // 由权限编码反查 ID 回显（RoleResp 只返回编码列表）
  const codes = row.permissions || []
  selectedPermIds.value = permissions.value
    .filter((p) => codes.includes(p.code))
    .map((p) => p.id)
  dialogVisible.value = true
}

async function onSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    const payload = {
      name: form.name,
      code: form.code,
      description: form.description,
      status: form.status,
      permissionIds: selectedPermIds.value
    }
    if (editingId.value) {
      await updateRoleApi(editingId.value, payload)
      ElMessage.success('角色已更新')
    } else {
      await createRoleApi(payload)
      ElMessage.success('角色已创建')
    }
    dialogVisible.value = false
    await load()
  } catch {
    // 错误提示已由请求层统一处理
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: RoleOption) {
  try {
    await ElMessageBox.confirm(
      `确定删除角色「${row.name}」吗？已分配该角色的用户将失去对应权限。`,
      '删除确认',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  try {
    await deleteRoleApi(row.id)
    ElMessage.success('角色已删除')
    await load()
  } catch {
    // 错误提示已由请求层统一处理
  }
}

onMounted(async () => {
  await load()
  permissions.value = await listPermissionsApi().catch(() => [])
})
</script>

<style scoped lang="scss">
@use '@/assets/styles/variables.scss' as *;

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.role-cell {
  display: flex;
  align-items: center;
  gap: $space-2;
}

.role-name {
  font-size: 14px;
  font-weight: 600;
  color: $text-primary;
}

.mono-id,
.mono-code {
  font-family: $font-mono;
  font-size: 12px;
  color: $text-tertiary;
}

.mono-code {
  color: $text-secondary;
  font-size: 13px;
}

.builtin-tag {
  font-size: 11px;
}

.perm-tags {
  display: flex;
  gap: $space-2;
  flex-wrap: wrap;
  align-items: center;
}

.perm-tag {
  font-size: 11px;
}

.perm-more {
  font-size: 12px;
  color: $text-tertiary;
}

.perm-picker {
  display: flex;
  flex-wrap: wrap;
  gap: $space-2 $space-4;
  padding: $space-3;
  background-color: $bg-subtle;
  border: 1px solid $border-default;
  border-radius: $radius-md;
  width: 100%;
  box-sizing: border-box;
}

.perm-check {
  width: calc(50% - $space-2);
  margin-right: 0;
  font-size: 13px;
}

.perm-code {
  font-family: $font-mono;
  font-size: 11px;
  color: $text-tertiary;
  margin-left: 6px;
}
</style>
