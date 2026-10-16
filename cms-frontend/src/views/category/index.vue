<template>
  <div class="page-container">
    <h2 class="page-title">分类管理</h2>
    <p class="page-subtitle">栏目结构 · 新增与编辑需管理员权限</p>

    <div class="toolbar">
      <div class="toolbar-spacer" />
      <el-button v-if="userStore.hasPermission('category:manage')" type="primary" @click="openCreate">
        <el-icon><Plus /></el-icon>
        新建分类
      </el-button>
    </div>

    <div class="table-card">
      <el-table :data="list" v-loading="loading" row-key="id">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="名称" min-width="160">
          <template #default="{ row }">
            <span class="cat-name">{{ row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="slug" label="别名" width="140">
          <template #default="{ row }">
            <span :class="row.slug ? '' : 'text-muted'">{{ row.slug || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span :class="row.description ? '' : 'text-muted'">{{ row.description || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="70" align="right" />
        <el-table-column prop="articleCount" label="文章数" width="80" align="right" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small" effect="light">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button v-if="userStore.hasPermission('category:manage')" link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="userStore.hasPermission('category:manage')" link type="danger" size="small" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑分类' : '新建分类'" width="480px">
      <el-form :model="form" label-width="70px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" placeholder="分类名称" maxlength="30" />
        </el-form-item>
        <el-form-item label="别名">
          <el-input v-model="form.slug" placeholder="slug（URL 友好，选填）" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="选填" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
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
  createCategoryApi,
  deleteCategoryApi,
  listCategoriesApi,
  updateCategoryApi,
  type CategoryForm,
  type CategoryItem
} from '@/api/article'

const list = ref<CategoryItem[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const editing = ref<CategoryItem | null>(null)

const form = reactive<CategoryForm>({
  name: '',
  slug: '',
  description: '',
  sortOrder: 0,
  status: 1
})

async function load() {
  loading.value = true
  try {
    list.value = await listCategoriesApi()
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = null
  form.name = ''
  form.slug = ''
  form.description = ''
  form.sortOrder = 0
  form.status = 1
  dialogVisible.value = true
}

function openEdit(row: CategoryItem) {
  editing.value = row
  form.name = row.name
  form.slug = row.slug || ''
  form.description = row.description || ''
  form.sortOrder = row.sortOrder
  form.status = row.status
  dialogVisible.value = true
}

async function onSave() {
  if (!form.name.trim()) {
    ElMessage.warning('请填写分类名称')
    return
  }
  saving.value = true
  try {
    if (editing.value) {
      await updateCategoryApi(editing.value.id, { ...form })
      ElMessage.success('分类已更新')
    } else {
      await createCategoryApi({ ...form })
      ElMessage.success('分类已创建')
    }
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function onDelete(row: CategoryItem) {
  await ElMessageBox.confirm(
    `确定删除分类「${row.name}」？该分类下仍有文章时将无法删除。`,
    '删除分类',
    { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
  )
  await deleteCategoryApi(row.id)
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

.cat-name {
  font-weight: 500;
}

.text-muted {
  color: $text-tertiary;
}
</style>
