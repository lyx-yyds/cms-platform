<template>
  <div class="page-container">
    <h2 class="page-title">标签管理</h2>
    <p class="page-subtitle">稿件标签库 · 新增与编辑需管理员权限</p>

    <div class="toolbar">
      <div class="toolbar-spacer" />
      <el-button v-if="userStore.hasPermission('tag:manage')" type="primary" @click="openCreate">
        <el-icon><Plus /></el-icon>
        新建标签
      </el-button>
    </div>

    <div class="table-card">
      <el-table :data="list" v-loading="loading" row-key="id">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="标签名" min-width="160">
          <template #default="{ row }">
            <span class="tag-name">{{ row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="slug" label="别名" width="180">
          <template #default="{ row }">
            <span :class="row.slug ? '' : 'text-muted'">{{ row.slug || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="articleCount" label="文章数" width="100" align="right" />
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button v-if="userStore.hasPermission('tag:manage')" link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="userStore.hasPermission('tag:manage')" link type="danger" size="small" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑标签' : '新建标签'" width="440px">
      <el-form :model="form" label-width="70px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" placeholder="标签名称" maxlength="20" />
        </el-form-item>
        <el-form-item label="别名">
          <el-input v-model="form.slug" placeholder="slug（选填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useUserStore } from '@/store/user'
const userStore = useUserStore()
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  createTagApi,
  deleteTagApi,
  listTagsApi,
  updateTagApi,
  type TagForm,
  type TagItem
} from '@/api/article'

const list = ref<TagItem[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const editing = ref<TagItem | null>(null)

const form = reactive<TagForm>({ name: '', slug: '' })

async function load() {
  loading.value = true
  try {
    list.value = await listTagsApi()
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = null
  form.name = ''
  form.slug = ''
  dialogVisible.value = true
}

function openEdit(row: TagItem) {
  editing.value = row
  form.name = row.name
  form.slug = row.slug || ''
  dialogVisible.value = true
}

async function onSave() {
  if (!form.name.trim()) {
    ElMessage.warning('请填写标签名称')
    return
  }
  saving.value = true
  try {
    if (editing.value) {
      await updateTagApi(editing.value.id, { ...form })
      ElMessage.success('标签已更新')
    } else {
      await createTagApi({ ...form })
      ElMessage.success('标签已创建')
    }
    dialogVisible.value = false
    load()
  } catch {
    // 错误提示已由请求拦截器统一弹出，这里仅保持对话框打开供用户修改
  } finally {
    saving.value = false
  }
}

async function onDelete(row: TagItem) {
  await ElMessageBox.confirm(
    `确定删除标签「${row.name}」？已被文章使用的标签将无法删除。`,
    '删除标签',
    { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
  )
  await deleteTagApi(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<style scoped lang="scss">
@use '@/assets/styles/variables.scss' as *;

.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.toolbar-spacer {
  flex: 1;
}

.table-card {
  background-color: $bg-surface;
  border: 1px solid $border-default;
  border-radius: $radius-md;
  padding: 12px;
  box-sizing: border-box;
}

.tag-name {
  font-weight: 500;
  color: $brand-primary;
}

.text-muted {
  color: $text-tertiary;
}
</style>
