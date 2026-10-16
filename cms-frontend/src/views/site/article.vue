<template>
  <div class="detail" v-loading="loading">
    <template v-if="article">
      <!-- 文章主体 -->
      <article class="article">
        <header class="article-head">
          <div class="head-meta">
            <span class="meta-cat">{{ article.categoryName || '未分类' }}</span>
            <span v-if="article.isTop" class="meta-pin">置顶</span>
            <span v-if="article.isRecommend" class="meta-rec">推荐</span>
          </div>
          <h1 class="article-title">{{ article.title }}</h1>
          <div class="article-info">
            <span class="info-author">{{ article.authorName || '佚名' }}</span>
            <span class="info-dot">·</span>
            <span>{{ fmtTime(article.publishTime) }}</span>
            <span class="info-dot">·</span>
            <span>阅读 {{ article.viewCount }}</span>
            <span v-for="t in article.tags" :key="t" class="tag-chip">{{ t }}</span>
          </div>
        </header>

        <!-- 封面 -->
        <div v-if="article.coverImage" class="cover">
          <img :src="article.coverImage" :alt="article.title" />
        </div>

        <!-- 正文 -->
        <div class="article-body" v-html="article.content"></div>

        <!-- 互动条 -->
        <div class="interact-bar">
          <button class="like-btn" :class="{ liked: article.liked }" @click="toggleLike">
            <span class="like-icon">{{ article.liked ? '♥' : '♡' }}</span>
            <span>{{ article.liked ? '已点赞' : '点赞' }}</span>
            <span v-if="article.likeCount" class="like-count">{{ article.likeCount }}</span>
          </button>
          <span class="comment-anchor">评论 {{ comments.length }}</span>
        </div>
      </article>

      <!-- 评论区 -->
      <section class="comment-section">
        <h3 class="section-title">评论 <span class="section-count">({{ comments.length }})</span></h3>

        <!-- 发表框 -->
        <div class="comment-editor">
          <textarea
            v-model="commentText"
            class="comment-textarea"
            :placeholder="userStore.isLoggedIn ? '写下你的评论……' : '登录后即可发表评论'"
            :disabled="!userStore.isLoggedIn"
            rows="3"
            maxlength="500"
          />
          <div class="editor-foot">
            <span class="editor-hint">{{ commentText.length }}/500 · 评论将经 AI 初审后公开展示</span>
            <el-button type="primary" :loading="submitting" :disabled="!canSubmit" @click="submitComment">
              {{ userStore.isLoggedIn ? '发表评论' : '去登录' }}
            </el-button>
          </div>
        </div>

        <!-- 评论列表 -->
        <div v-loading="commentLoading" class="comment-list">
          <div v-for="c in comments" :key="c.id" class="comment-item">
            <div class="comment-head">
              <span class="avatar">{{ (c.userNickname || '匿')[0] }}</span>
              <span class="c-name">{{ c.userNickname || '匿名用户' }}</span>
              <span class="c-time">{{ fmtTime(c.createTime) }}</span>
              <button
                v-if="canDelete(c)"
                class="c-del"
                @click="onDeleteComment(c.id)"
              >
                删除
              </button>
            </div>
            <p class="c-content">{{ c.content }}</p>
          </div>
          <el-empty v-if="!commentLoading && comments.length === 0" description="暂无评论，来抢沙发" :image-size="64" />
        </div>
      </section>
    </template>

    <el-result v-else-if="!loading && !article" icon="warning" title="文章不存在或已下架">
      <template #extra>
        <el-button type="primary" @click="router.push('/site')">返回首页</el-button>
      </template>
    </el-result>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getArticleApi,
  likeArticleApi,
  unlikeArticleApi,
  type ArticleDetail
} from '@/api/article'
import {
  createCommentApi,
  deleteCommentApi,
  listArticleCommentsApi,
  type CommentItem
} from '@/api/comment'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const articleId = Number(route.params.id)
const loading = ref(false)
const article = ref<ArticleDetail | null>(null)

const comments = ref<CommentItem[]>([])
const commentLoading = ref(false)
const commentText = ref('')
const submitting = ref(false)

const canSubmit = computed(() => userStore.isLoggedIn && commentText.value.trim().length > 0)

function fmtTime(v?: string): string {
  if (!v) return ''
  return v.replace('T', ' ').slice(0, 16)
}

async function loadDetail() {
  loading.value = true
  try {
    article.value = await getArticleApi(articleId)
    document.title = `${article.value.title} · CMS 编辑部`
  } catch (e) {
    const err = e as { message?: string }
    if (err?.message?.includes('404')) {
      article.value = null
    } else {
      ElMessage.error(err?.message || '加载失败')
    }
  } finally {
    loading.value = false
  }
}

async function loadComments() {
  commentLoading.value = true
  try {
    comments.value = await listArticleCommentsApi(articleId)
  } finally {
    commentLoading.value = false
  }
}

// ===== 点赞 =====
async function toggleLike() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后再点赞')
    return
  }
  try {
    if (article.value?.liked) {
      await unlikeArticleApi(articleId)
      article.value!.liked = false
      article.value!.likeCount = Math.max(0, (article.value!.likeCount || 0) - 1)
    } else {
      await likeArticleApi(articleId)
      article.value!.liked = true
      article.value!.likeCount = (article.value!.likeCount || 0) + 1
    }
  } catch (e) {
    const err = e as { message?: string }
    ElMessage.error(err?.message || '操作失败')
  }
}

// ===== 评论 =====
async function submitComment() {
  if (!userStore.isLoggedIn) {
    router.push(`/login?redirect=${route.fullPath}`)
    return
  }
  const content = commentText.value.trim()
  if (!content) return
  submitting.value = true
  try {
    await createCommentApi(articleId, { content })
    ElMessage.success('评论已提交，审核通过后公开展示')
    commentText.value = ''
    // 延迟刷新：AI 初审通常数秒内完成，通过后即可见
    setTimeout(loadComments, 2500)
  } catch (e) {
    const err = e as { message?: string }
    ElMessage.error(err?.message || '发表失败')
  } finally {
    submitting.value = false
  }
}

function canDelete(c: CommentItem): boolean {
  if (!userStore.isLoggedIn) return false
  if (userStore.isAdmin) return true
  return c.userId === (userStore.userInfo?.id as number)
}

async function onDeleteComment(id: number) {
  await ElMessageBox.confirm('确定删除这条评论？', '删除评论', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  })
  await deleteCommentApi(id)
  ElMessage.success('已删除')
  loadComments()
}

onMounted(() => {
  loadDetail()
  loadComments()
})
</script>

<style scoped lang="scss">
@use '@/assets/styles/variables.scss' as *;

/* ===== 文章主体 ===== */
.article {
  background-color: $bg-surface;
  border: 1px solid $border-default;
  border-radius: $radius-lg;
  padding: 36px 44px;
}

.article-head {
  margin-bottom: 20px;
}

.head-meta {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;

  .meta-cat {
    font-size: 12px;
    color: $brand-primary;
    background-color: $brand-light;
    border-radius: 3px;
    padding: 2px 10px;
    font-weight: 600;
  }

  .meta-pin,
  .meta-rec {
    font-size: 11px;
    color: $accent-red;
    border: 1px solid rgba(180, 84, 59, 0.4);
    border-radius: 3px;
    padding: 1px 7px;
  }

  .meta-rec {
    color: $warning;
    border-color: rgba(201, 138, 45, 0.4);
  }
}

.article-title {
  font-family: $font-display;
  font-size: 30px;
  font-weight: 700;
  color: $text-primary;
  line-height: 1.4;
  margin: 0 0 14px;
}

.article-info {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  font-size: 13px;
  color: $text-tertiary;

  .info-author {
    color: $text-secondary;
    font-weight: 600;
  }

  .info-dot {
    color: $border-strong;
  }

  .tag-chip {
    margin-left: 2px;
  }
}

.tag-chip {
  font-size: 11.5px;
  color: $text-tertiary;
  background-color: $bg-subtle;
  border: 1px solid $border-default;
  border-radius: 999px;
  padding: 1px 10px;
}

.cover {
  margin: 0 0 22px;
  border-radius: $radius-md;
  overflow: hidden;
  border: 1px solid $border-default;

  img {
    width: 100%;
    max-height: 420px;
    object-fit: cover;
    display: block;
  }
}

.article-body {
  font-size: 15px;
  line-height: 1.85;
  color: $text-primary;

  :deep(h1),
  :deep(h2),
  :deep(h3) {
    font-family: $font-display;
    font-weight: 700;
    color: $text-primary;
    margin: 26px 0 12px;
    line-height: 1.4;
  }

  :deep(h1) {
    font-size: 24px;
  }

  :deep(h2) {
    font-size: 21px;
    border-left: 3px solid $brand-primary;
    padding-left: 12px;
  }

  :deep(h3) {
    font-size: 18px;
  }

  :deep(p) {
    margin: 0 0 14px;
  }

  :deep(img) {
    max-width: 100%;
    border-radius: $radius-md;
  }

  :deep(pre) {
    background-color: $bg-subtle;
    border: 1px solid $border-default;
    border-radius: $radius-md;
    padding: 14px 16px;
    overflow-x: auto;
    font-size: 13px;
    font-family: $font-mono;
  }

  :deep(code) {
    font-family: $font-mono;
    font-size: 13px;
    background-color: $bg-subtle;
    border-radius: 3px;
    padding: 1px 5px;
  }

  :deep(blockquote) {
    margin: 0 0 14px;
    padding: 10px 16px;
    background-color: $brand-light;
    border-left: 3px solid $brand-primary;
    border-radius: $radius-sm;
    color: $text-secondary;
  }

  :deep(ul),
  :deep(ol) {
    padding-left: 24px;
    margin: 0 0 14px;
  }
}

/* ===== 互动条 ===== */
.interact-bar {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-top: 28px;
  padding-top: 18px;
  border-top: 1px solid $border-default;
}

.like-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13.5px;
  color: $text-secondary;
  background-color: $bg-subtle;
  border: 1px solid $border-default;
  border-radius: 999px;
  padding: 8px 20px;
  cursor: pointer;
  transition: all 0.15s;

  &:hover {
    border-color: $brand-primary;
    color: $brand-primary;
  }

  &.liked {
    color: $accent-red;
    border-color: rgba(180, 84, 59, 0.5);
    background-color: rgba(180, 84, 59, 0.06);

    .like-icon {
      color: $accent-red;
    }
  }

  .like-icon {
    font-size: 16px;
    color: $text-tertiary;
  }

  .like-count {
    font-size: 12px;
    color: $text-tertiary;
    margin-left: 2px;
  }
}

.comment-anchor {
  font-size: 13px;
  color: $text-tertiary;
}

/* ===== 评论区 ===== */
.comment-section {
  margin-top: 22px;
  background-color: $bg-surface;
  border: 1px solid $border-default;
  border-radius: $radius-lg;
  padding: 24px 28px;
}

.section-title {
  font-family: $font-display;
  font-size: 19px;
  font-weight: 700;
  color: $text-primary;
  margin: 0 0 16px;

  .section-count {
    font-size: 13px;
    color: $text-tertiary;
    font-weight: 400;
  }
}

.comment-editor {
  border: 1px solid $border-strong;
  border-radius: $radius-md;
  overflow: hidden;
  margin-bottom: 20px;

  &:focus-within {
    border-color: $brand-primary;
  }
}

.comment-textarea {
  width: 100%;
  border: none;
  outline: none;
  resize: vertical;
  padding: 12px 14px;
  font-size: 13.5px;
  font-family: $font-ui;
  line-height: 1.7;
  color: $text-primary;
  background-color: $bg-surface;
  box-sizing: border-box;
  min-height: 74px;

  &::placeholder {
    color: $text-tertiary;
  }

  &:disabled {
    background-color: $bg-subtle;
  }
}

.editor-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background-color: $bg-subtle;
  border-top: 1px solid $border-default;

  .editor-hint {
    font-size: 12px;
    color: $text-tertiary;
  }
}

.comment-list {
  min-height: 60px;
}

.comment-item {
  padding: 14px 0;
  border-top: 1px solid $border-default;

  &:first-child {
    border-top: none;
  }
}

.comment-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;

  .avatar {
    width: 26px;
    height: 26px;
    border-radius: 50%;
    background-color: $brand-light;
    color: $brand-primary;
    font-size: 13px;
    font-weight: 600;
    display: inline-flex;
    align-items: center;
    justify-content: center;
  }

  .c-name {
    font-size: 13px;
    font-weight: 600;
    color: $text-primary;
  }

  .c-time {
    font-size: 12px;
    color: $text-tertiary;
  }

  .c-del {
    margin-left: auto;
    font-size: 12px;
    color: $text-tertiary;
    background: none;
    border: none;
    cursor: pointer;

    &:hover {
      color: $danger;
    }
  }
}

.c-content {
  font-size: 13.5px;
  line-height: 1.75;
  color: $text-secondary;
  margin: 0;
  word-break: break-word;
}
</style>
