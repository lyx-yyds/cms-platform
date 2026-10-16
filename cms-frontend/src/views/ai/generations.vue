<template>
  <div class="page-container">
    <h2 class="page-title">AI 生成记录</h2>
    <p class="page-subtitle">写作助手产出明细 · 含模型与 Token 消耗（成本审计，文档 7.4）</p>

    <div class="panel">
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column label="ID" prop="id" width="70" />
        <el-table-column label="类型" width="110">
          <template #default="{ row }">
            <span class="type-ribbon" :class="typeClass(row.type)">{{ typeLabel(row.type) }}</span>
          </template>
        </el-table-column>
        <el-table-column v-if="userStore.isAdmin" prop="username" label="用户" width="100" />
        <el-table-column label="提示词摘要" prop="prompt" min-width="220" show-overflow-tooltip />
        <el-table-column label="结果摘要" min-width="220">
          <template #default="{ row }">{{ resultBrief(row) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <span class="type-ribbon" :class="statusClass(row.status)">{{ statusLabel(row.status) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="模型" prop="model" width="130" />
        <el-table-column label="Tokens" prop="tokensUsed" width="90" align="right">
          <template #default="{ row }">{{ row.tokensUsed ?? 0 }}</template>
        </el-table-column>
        <el-table-column label="时间" width="160">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button v-if="canEdit(row)" link type="primary" size="small" @click="goEdit(row)">编辑</el-button>
            <el-button link type="primary" size="small" @click="viewResult(row)">查看</el-button>
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

    <!-- 结果详情 -->
    <el-dialog v-model="detailVisible" title="生成结果" width="640px">
      <div class="detail-meta">
        <span class="type-ribbon" :class="typeClass(current?.type)">{{ typeLabel(current?.type) }}</span>
        <span class="detail-sub">模型 {{ current?.model }} · {{ current?.tokensUsed ?? 0 }} tokens</span>
      </div>
      <div class="detail-body">
        <div v-if="current?.result" class="detail-markdown" v-html="renderMd(current.result)" />
        <el-empty v-else-if="current?.status === 2" description="生成失败" :image-size="60">
          <span class="err-msg">{{ current?.errorMsg }}</span>
        </el-empty>
        <el-empty v-else description="暂无结果" :image-size="60" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { marked } from 'marked'
import { useUserStore } from '@/store/user'
import { AI_TYPE_LABEL, generationsApi, type AiGenerationRecord } from '@/api/ai'

const router = useRouter()
const userStore = useUserStore()

const list = ref<AiGenerationRecord[]>([])
const loading = ref(false)
const page = ref(1)
const size = 10
const total = ref(0)

const detailVisible = ref(false)
const current = ref<AiGenerationRecord | null>(null)

function typeLabel(t?: string): string {
  return (t && AI_TYPE_LABEL[t]) || t || '-'
}
function formatTime(v?: string): string {
  if (!v) return '-'
  return v.replace('T', ' ').slice(0, 19)
}
function typeClass(t?: string): string {
  return t === 'seo' ? 'ribbon-seo' : 'ribbon-write'
}
function statusLabel(s: number): string {
  return s === 1 ? '成功' : s === 2 ? '失败' : '处理中'
}
function statusClass(s: number): string {
  return s === 1 ? 'ribbon-ok' : s === 2 ? 'ribbon-fail' : 'ribbon-pending'
}
function resultBrief(row: AiGenerationRecord): string {
  if (row.status === 2) return row.errorMsg || '生成失败'
  const r = (row.result || '').replace(/<[^>]+>/g, '').replace(/\s+/g, ' ').trim()
  return r.length > 60 ? r.slice(0, 60) + '…' : (r || '-')
}
function renderMd(src: string): string {
  return marked.parse(src) as string
}
function viewResult(row: AiGenerationRecord) {
  current.value = row
  detailVisible.value = true
}
/** 可续编保存：正文类生成（草稿/续写/润色）且成功且有结果 */
function canEdit(row: AiGenerationRecord): boolean {
  return (row.type === 'draft' || row.type === 'continue' || row.type === 'polish') && row.status === 1 && !!row.result
}
function goEdit(row: AiGenerationRecord) {
  router.push(`/article/edit?aiRecordId=${row.id}`)
}

async function load() {
  loading.value = true
  try {
    const res = await generationsApi({ page: page.value, size })
    list.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped lang="scss">
@use '@/assets/styles/variables.scss' as *;

.panel {
  background-color: $bg-surface;
  border: 1px solid $border-default;
  border-radius: $radius-md;
  padding: 16px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  padding-top: 14px;
}

.type-ribbon {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 3px;
  font-size: 11px;
  line-height: 1.5;
}

.ribbon-write {
  background-color: rgba(31, 107, 90, 0.12);
  color: #1f6b5a;
}

.ribbon-seo {
  background-color: rgba(201, 138, 45, 0.14);
  color: #a06f1d;
}

.ribbon-ok {
  background-color: rgba(62, 124, 79, 0.12);
  color: #3e7c4f;
}

.ribbon-fail {
  background-color: rgba(192, 69, 60, 0.12);
  color: #c0453c;
}

.ribbon-pending {
  background-color: rgba(154, 161, 171, 0.14);
  color: #6b7280;
}

.detail-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.detail-sub {
  font-size: 12px;
  color: $text-tertiary;
}

.detail-body {
  max-height: 420px;
  overflow-y: auto;
  background-color: $bg-subtle;
  border: 1px solid $border-default;
  border-radius: $radius-sm;
  padding: 14px;
}

.detail-markdown {
  font-size: 13px;
  line-height: 1.7;
  color: $text-primary;

  :deep(h1), :deep(h2), :deep(h3) {
    font-weight: 600;
    margin: 10px 0 6px;
  }

  :deep(pre) {
    background-color: $bg-surface;
    border: 1px solid $border-default;
    border-radius: $radius-sm;
    padding: 8px 10px;
    overflow-x: auto;
    font-size: 12px;
    font-family: $font-mono;
  }
}

.err-msg {
  font-size: 12.5px;
  color: $text-tertiary;
}
</style>
