<template>
  <div class="page-container">
    <h2 class="page-title">文章管理</h2>
    <p class="page-subtitle">稿件台 · 状态色带标识每篇稿件的流转位置（草稿 / 已发布 / 已下架）</p>

    <!-- 工具栏 -->
    <div class="toolbar">
      <el-input
        v-model="keyword"
        placeholder="搜索标题 / 摘要关键词"
        clearable
        style="width: 220px"
        @keyup.enter="onSearch"
        @clear="onSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-select v-model="statusFilter" placeholder="状态筛选" clearable style="width: 140px" @change="onStatusChange">
        <el-option label="草稿" :value="0" />
        <el-option label="已发布" :value="1" />
        <el-option label="已下架" :value="2" />
      </el-select>
      <div class="toolbar-spacer" />
      <el-button v-if="userStore.hasPermission('article:create')" type="primary" @click="goCreate">
        <el-icon><Plus /></el-icon>
        新建文章
      </el-button>
    </div>

    <!-- 稿件台表格：行左侧 3px 状态色带（签名元素） -->
    <div class="table-card">
      <el-table :data="list" v-loading="loading" row-key="id" :row-class-name="rowClassName">
        <el-table-column label="稿件编号" width="80">
          <template #default="{ row }">
            <span class="article-id">#{{ row.id }}</span>
          </template>
        </el-table-column>
        <el-table-column label="标题" min-width="260" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="article-title">
              {{ row.title }}
              <el-tag v-if="row.isTop" size="small" type="warning" effect="plain" class="flag-tag">置顶</el-tag>
              <el-tag v-if="row.isRecommend" size="small" type="success" effect="plain" class="flag-tag">推荐</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="categoryName" label="分类" width="90">
          <template #default="{ row }">
            <span :class="row.categoryName ? '' : 'text-muted'">{{ row.categoryName || '未分类' }}</span>
          </template>
        </el-table-column>
        <el-table-column v-if="userStore.isAdmin" prop="authorName" label="作者" width="90" />
        <el-table-column label="标签" width="160">
          <template #default="{ row }">
            <span v-for="t in row.tags" :key="t" class="tag-chip">{{ t }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="tagType(row.status)" size="small" effect="light">
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="viewCount" label="阅读" width="70" align="right" />
        <el-table-column prop="likeCount" label="点赞" width="60" align="right" />
        <el-table-column label="发布时间" width="150">
          <template #default="{ row }">
            <span :class="row.publishTime ? '' : 'text-muted'">{{ row.publishTime?.slice(0, 16).replace('T', ' ') || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button v-if="userStore.hasPermission('article:update')" link type="primary" size="small" @click="goEdit(row)">编辑</el-button>
            <el-button
              v-if="row.status === 0 && userStore.hasPermission('article:update')"
              link
              type="success"
              size="small"
              @click="onPublish(row)"
            >
              发布
            </el-button>
            <el-button
              v-if="row.status === 1 && userStore.hasPermission('article:update')"
              link
              type="warning"
              size="small"
              @click="onOffline(row)"
            >
              下架
            </el-button>
            <el-button v-if="userStore.hasPermission('article:delete')" link type="danger" size="small" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="page"
          :page-size="size"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="load"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import {
  deleteArticleApi,
  pageMineArticlesApi,
  updateArticleApi,
  type ArticleItem
} from '@/api/article'

const router = useRouter()
const userStore = useUserStore()

const list = ref<ArticleItem[]>([])
const loading = ref(false)
const statusFilter = ref<number | undefined>(undefined)
const keyword = ref('')
const page = ref(1)
const size = 10
const total = ref(0)

async function load() {
  loading.value = true
  try {
    const res = await pageMineArticlesApi({ page: page.value, size, keyword: keyword.value || undefined, status: statusFilter.value })
    list.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function onSearch() {
  page.value = 1
  load()
}

function onStatusChange() {
  page.value = 1
  load()
}

function rowClassName({ row }: { row: ArticleItem }): string {
  const statusClass = {
    0: 'status-draft',
    1: 'status-published',
    2: 'status-reviewing'
  }[row.status]
  return `article-row ${statusClass}`
}

const statusMap: Record<number, string> = { 0: '草稿', 1: '已发布', 2: '已下架' }
function statusText(s: number): string {
  return statusMap[s] ?? '未知'
}

type TagType = 'info' | 'warning' | 'success' | 'danger'
const tagTypeMap: Record<number, TagType> = { 0: 'info', 1: 'success', 2: 'warning' }
function tagType(s: number): TagType {
  return tagTypeMap[s] ?? 'info'
}

function goCreate() {
  router.push('/article/edit')
}

function goEdit(row: ArticleItem) {
  router.push(`/article/edit/${row.id}`)
}

async function onPublish(row: ArticleItem) {
  await updateArticleApi(row.id, { title: row.title, status: 1 })
  ElMessage.success(`《${row.title}》已发布`)
  load()
}

async function onOffline(row: ArticleItem) {
  await updateArticleApi(row.id, { title: row.title, status: 2 })
  ElMessage.success(`《${row.title}》已下架`)
  load()
}

async function onDelete(row: ArticleItem) {
  await ElMessageBox.confirm(`确定删除稿件 #${row.id}《${row.title}》？删除后不可恢复。`, '删除稿件', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  })
  await deleteArticleApi(row.id)
  ElMessage.success('已删除（逻辑删除，见文档 7.10）')
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

.article-id {
  font-family: $font-mono;
  color: $text-tertiary;
  font-size: 13px;
}

.article-title {
  font-weight: 500;
  color: $text-primary;
}

.flag-tag {
  margin-left: 6px;
  vertical-align: 1px;
}

.tag-chip {
  display: inline-block;
  margin-right: 6px;
  padding: 1px 8px;
  font-size: 12px;
  color: $brand-primary;
  background-color: $brand-light;
  border-radius: 999px;
}

.text-muted {
  color: $text-tertiary;
}

.pager {
  display: flex;
  justify-content: flex-end;
  padding: 14px 4px 4px;
}
</style>