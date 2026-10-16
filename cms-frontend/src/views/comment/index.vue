<template>
  <div class="page-container">
    <h2 class="page-title">评论管理</h2>
    <p class="page-subtitle">AI 机审流转：正常自动通过 · 疑似转人工复核 · 明显违规自动拒绝（文档 §7.5）</p>

    <!-- 工具栏 -->
    <div class="toolbar">
      <el-input
        v-model="keyword"
        placeholder="搜索评论内容 / 用户昵称"
        :prefix-icon="Search"
        clearable
        style="width: 220px"
        @keyup.enter="search"
        @clear="search"
      />
      <el-select v-model="statusFilter" placeholder="审核状态" clearable style="width: 140px" @change="search">
        <el-option label="待审核" :value="0" />
        <el-option label="已通过" :value="1" />
        <el-option label="已拒绝" :value="2" />
      </el-select>
      <el-select v-model="aiFilter" placeholder="AI 机审状态" clearable style="width: 160px" @change="search">
        <el-option label="未机审" :value="0" />
        <el-option label="机审通过" :value="1" />
        <el-option label="转人工" :value="2" />
        <el-option label="机审拒绝" :value="3" />
      </el-select>
      <div class="toolbar-spacer" />
      <el-button :loading="loading" @click="load">刷新</el-button>
    </div>

    <!-- 评论表格 -->
    <div class="table-card">
      <el-table :data="list" v-loading="loading" row-key="id">
        <el-table-column label="评论内容" min-width="300">
          <template #default="{ row }">
            <div class="comment-text">{{ row.content }}</div>
            <div class="comment-article">
              《{{ row.articleTitle || `文章 #${row.articleId}` }}》
            </div>
          </template>
        </el-table-column>
        <el-table-column label="作者" width="90">
          <template #default="{ row }">
            <span :class="row.userNickname ? '' : 'text-muted'">{{ row.userNickname || `#${row.userId}` }}</span>
            <span v-if="row.replyNickname" class="reply-hint">→ {{ row.replyNickname }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="ipAddress" label="IP" width="110">
          <template #default="{ row }">
            <span :class="row.ipAddress ? '' : 'text-muted'">{{ row.ipAddress || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="150">
          <template #default="{ row }">
            <span :class="row.createTime ? '' : 'text-muted'">{{ row.createTime?.replace('T', ' ').slice(0, 16) || '—' }}</span>
          </template>
        </el-table-column>

        <!-- AI 机审结果：分类 + 置信度 + 原因 -->
        <el-table-column label="AI 机审" width="190">
          <template #default="{ row }">
            <div class="ai-cell">
              <el-tag :type="aiTagType(row.aiStatus)" size="small" effect="light">
                {{ aiStatusText(row.aiStatus) }}
              </el-tag>
              <span v-if="row.aiScore != null" class="ai-score">{{ row.aiScore }}%</span>
            </div>
            <div v-if="row.aiReason" class="ai-reason" :title="row.aiReason">{{ row.aiReason }}</div>
          </template>
        </el-table-column>

        <el-table-column label="审核状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small" effect="light">
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 0">
              <el-button v-if="userStore.hasPermission('comment:audit')" link type="success" size="small" @click="onAudit(row, 1)">通过</el-button>
              <el-button v-if="userStore.hasPermission('comment:audit')" link type="danger" size="small" @click="onAudit(row, 2)">拒绝</el-button>
            </template>
            <el-button v-if="userStore.hasPermission('comment:audit')" link type="danger" size="small" @click="onDelete(row)">删除</el-button>
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
import { useUserStore } from '@/store/user'
const userStore = useUserStore()
import { Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { auditCommentApi, deleteCommentApi, pageCommentsApi, type CommentItem } from '@/api/comment'

const list = ref<CommentItem[]>([])
const loading = ref(false)
const keyword = ref('')
const statusFilter = ref<number | undefined>(undefined)
const aiFilter = ref<number | undefined>(undefined)
const page = ref(1)
const size = 10
const total = ref(0)

async function load() {
  loading.value = true
  try {
    const res = await pageCommentsApi({
      page: page.value,
      size,
      keyword: keyword.value || undefined,
      status: statusFilter.value,
      aiStatus: aiFilter.value
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

/* AI 机审状态文案与标签色 */
const aiStatusMap: Record<number, { text: string; tag: 'success' | 'danger' | 'warning' | 'info' }> = {
  0: { text: '未机审', tag: 'info' },
  1: { text: '机审通过', tag: 'success' },
  2: { text: '转人工', tag: 'warning' },
  3: { text: '机审拒绝', tag: 'danger' }
}

function aiStatusText(s: number): string {
  return aiStatusMap[s]?.text ?? '未知'
}

function aiTagType(s: number): 'success' | 'danger' | 'warning' | 'info' {
  return aiStatusMap[s]?.tag ?? 'info'
}

/* 审核状态 */
const statusMap: Record<number, { text: string; tag: 'warning' | 'success' | 'danger' }> = {
  0: { text: '待审核', tag: 'warning' },
  1: { text: '已通过', tag: 'success' },
  2: { text: '已拒绝', tag: 'danger' }
}

function statusText(s: number): string {
  return statusMap[s]?.text ?? '未知'
}

function statusTagType(s: number): 'warning' | 'success' | 'danger' {
  return statusMap[s]?.tag ?? 'warning'
}

/** 人工复核：PUT /api/admin/comments/{id}/audit（覆盖机审结果，文档 7.5 第 5 条） */
async function onAudit(row: CommentItem, next: number) {
  await auditCommentApi(row.id, next)
  ElMessage.success(`评论 #${row.id} 人工复核：${statusText(next)}`)
  load()
}

/** 删除评论：DELETE /api/comments/{id} */
async function onDelete(row: CommentItem) {
  await ElMessageBox.confirm(`确定删除评论 #${row.id}？删除后不可恢复。`, '删除评论', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  })
  await deleteCommentApi(row.id)
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

.comment-text {
  font-size: 13px;
  color: $text-primary;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.comment-article {
  font-size: 12px;
  color: $text-tertiary;
  margin-top: 2px;
}

.reply-hint {
  display: block;
  font-size: 11.5px;
  color: $text-tertiary;
}

.ai-cell {
  display: flex;
  align-items: center;
  gap: 6px;
}

.ai-score {
  font-family: $font-mono;
  font-size: 12px;
  color: $text-secondary;
}

.ai-reason {
  font-size: 11.5px;
  color: $text-tertiary;
  margin-top: 3px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 170px;
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
