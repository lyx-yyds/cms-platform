<template>
  <div class="page-container">
    <h2 class="page-title">媒体库</h2>
    <p class="page-subtitle">MinIO 对象存储 · 白名单 jpg/png/gif/webp/pdf/docx/xlsx · 图片 ≤5MB · 文档 ≤20MB（文档 §7.8）</p>

    <!-- 工具栏 -->
    <div class="toolbar">
      <el-upload
        :show-file-list="false"
        :http-request="customUpload"
        :accept="acceptTypes"
        multiple
        :disabled="uploading"
      >
        <el-button type="primary" :loading="uploading">
          <el-icon><Upload /></el-icon>
          上传文件
        </el-button>
      </el-upload>
      <el-input
        v-model="keyword"
        placeholder="搜索文件名"
        :prefix-icon="Search"
        clearable
        style="width: 220px"
        @keyup.enter="search"
        @clear="search"
      />
      <el-select v-model="typeFilter" placeholder="文件类型" clearable style="width: 150px" @change="search">
        <el-option label="图片" value="image" />
        <el-option label="文档" value="application" />
      </el-select>
      <div class="toolbar-spacer" />
      <el-button :loading="loading" @click="load">刷新</el-button>
    </div>

    <!-- 文件网格 -->
    <div v-loading="loading" class="media-grid">
      <div v-for="m in list" :key="m.id" class="media-card">
        <!-- 预览 -->
        <a :href="m.url" target="_blank" rel="noopener" class="media-preview" :title="m.originalName">
          <img v-if="isImage(m)" :src="m.url" :alt="m.originalName" loading="lazy" />
          <div v-else class="doc-thumb">
            <el-icon :size="34"><Document /></el-icon>
            <span class="doc-ext">{{ (m.extension || '').toUpperCase() }}</span>
          </div>
        </a>
        <!-- 信息 -->
        <div class="media-info">
          <div class="media-name" :title="m.originalName">{{ m.originalName }}</div>
          <div class="media-meta">{{ formatSize(m.size) }} · {{ m.uploadUserNickname || `#${m.uploadUserId}` }}</div>
        </div>
        <!-- 操作 -->
        <div class="media-actions">
          <el-button link size="small" @click="copyUrl(m)">复制链接</el-button>
          <el-button link type="danger" size="small" @click="onDelete(m)">删除</el-button>
        </div>
      </div>

      <el-empty v-if="!loading && list.length === 0" description="暂无媒体文件" :image-size="90" />
    </div>

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
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Document, Search, Upload } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { UploadRequestOptions } from 'element-plus'
import { deleteMediaApi, formatSize, pageMediaApi, uploadMediaApi, type MediaItem } from '@/api/media'

const list = ref<MediaItem[]>([])
const loading = ref(false)
const uploading = ref(false)
const keyword = ref('')
const typeFilter = ref<string | undefined>(undefined)
const page = ref(1)
const size = 12
const total = ref(0)

const acceptTypes = '.jpg,.jpeg,.png,.gif,.webp,.pdf,.docx,.xlsx'

function isImage(m: MediaItem): boolean {
  return !!m.type?.startsWith('image/')
}

async function load() {
  loading.value = true
  try {
    const res = await pageMediaApi({
      page: page.value,
      size,
      keyword: keyword.value || undefined,
      type: typeFilter.value
    })
    list.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  load()
}

/** 自定义上传：走 /api/media/upload（后端做白名单/MIME/大小校验） */
async function customUpload(options: UploadRequestOptions) {
  uploading.value = true
  try {
    const media = await uploadMediaApi(options.file as File)
    ElMessage.success(`已上传：${media.originalName}`)
    if (page.value === 1) {
      load()
    } else {
      page.value = 1
      load()
    }
  } finally {
    uploading.value = false
  }
}

async function copyUrl(m: MediaItem) {
  try {
    await navigator.clipboard.writeText(m.url)
    ElMessage.success('链接已复制')
  } catch {
    ElMessage.info(m.url)
  }
}

async function onDelete(m: MediaItem) {
  await ElMessageBox.confirm(`确定删除「${m.originalName}」？删除后链接立即失效。`, '删除文件', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  })
  await deleteMediaApi(m.id)
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

.media-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 14px;
  min-height: 160px;
}

.media-card {
  background-color: $bg-surface;
  border: 1px solid $border-default;
  border-radius: $radius-md;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.media-preview {
  display: block;
  height: 150px;
  background-color: $bg-subtle;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid $border-default;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.doc-thumb {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  color: $text-tertiary;
}

.doc-ext {
  font-size: 11px;
  color: $text-tertiary;
  font-family: $font-mono;
}

.media-info {
  padding: 10px 12px 6px;
}

.media-name {
  font-size: 12.5px;
  color: $text-primary;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.media-meta {
  font-size: 11.5px;
  color: $text-tertiary;
  margin-top: 3px;
}

.media-actions {
  display: flex;
  justify-content: flex-end;
  padding: 0 8px 6px;
  margin-top: auto;
}

.pager {
  display: flex;
  justify-content: flex-end;
  padding: 14px 4px 4px;
}
</style>
