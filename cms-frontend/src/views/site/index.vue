<template>
  <div class="home">
    <!-- 首页题头 -->
    <section class="hero">
      <h1 class="hero-title">编辑部 · 稿件精选</h1>
      <p class="hero-sub">收录技术实践与创作沉淀，已发布内容在此公开展示</p>
    </section>

    <!-- 分类筛选 -->
    <div class="cat-bar">
      <button
        class="cat-chip"
        :class="{ active: currentCategory === undefined }"
        @click="pickCategory(undefined)"
      >
        全部
      </button>
      <button
        v-for="c in categories"
        :key="c.id"
        class="cat-chip"
        :class="{ active: currentCategory === c.id }"
        @click="pickCategory(c.id)"
      >
        {{ c.name }}
      </button>
    </div>

    <!-- 搜索 -->
    <div class="search-row">
      <div class="search-box">
        <el-input v-model="keyword" placeholder="搜索标题 / 摘要关键词" clearable @keyup.enter="onSearch" @clear="onSearch">
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button type="primary" class="search-btn" @click="onSearch">搜索</el-button>
      </div>
      <span v-if="total" class="count-hint">共 {{ total }} 篇</span>
    </div>

    <!-- 文章卡片流 -->
    <div v-loading="loading" class="card-list">
      <article v-for="a in list" :key="a.id" class="article-card" @click="goDetail(a.id)">
        <div class="cover" :class="{ placeholder: !a.coverImage }">
          <img v-if="a.coverImage" :src="a.coverImage" :alt="a.title" loading="lazy" />
          <span v-else class="cover-placeholder">{{ (a.categoryName || '编')[0] }}</span>
        </div>
        <div class="card-body">
          <div class="card-meta">
            <span class="meta-cat">{{ a.categoryName || '未分类' }}</span>
            <span v-if="a.isTop" class="meta-pin">置顶</span>
            <span v-if="a.isRecommend" class="meta-rec">推荐</span>
            <span class="meta-time">{{ fmtTime(a.publishTime) }}</span>
          </div>
          <h2 class="card-title">{{ a.title }}</h2>
          <p class="card-summary">{{ a.summary || '（无摘要）' }}</p>
          <div class="card-foot">
            <span class="foot-author">{{ a.authorName || '佚名' }}</span>
            <span class="foot-stat">阅读 {{ a.viewCount }}</span>
            <span class="foot-stat">点赞 {{ a.likeCount }}</span>
            <span v-for="t in a.tags" :key="t" class="tag-chip">{{ t }}</span>
          </div>
        </div>
      </article>

      <el-empty v-if="!loading && list.length === 0" description="暂无已发布文章" :image-size="72" />
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
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { listCategoriesApi, pageArticlesApi, type ArticleItem, type CategoryItem } from '@/api/article'

const router = useRouter()

const list = ref<ArticleItem[]>([])
const categories = ref<CategoryItem[]>([])
const loading = ref(false)
const keyword = ref('')
const currentCategory = ref<number | undefined>(undefined)
const page = ref(1)
const size = 8
const total = ref(0)

function fmtTime(v?: string): string {
  if (!v) return ''
  return v.slice(0, 10)
}

function pickCategory(id: number | undefined) {
  currentCategory.value = id
  page.value = 1
  load()
}

function onSearch() {
  page.value = 1
  load()
}

async function load() {
  loading.value = true
  try {
    const res = await pageArticlesApi({
      page: page.value,
      size,
      keyword: keyword.value || undefined,
      categoryId: currentCategory.value,
      status: 1
    })
    list.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function goDetail(id: number) {
  router.push(`/site/article/${id}`)
}

onMounted(async () => {
  const [catRes] = await Promise.all([listCategoriesApi()])
  categories.value = catRes.filter((c) => c.status === 1)
  load()
})
</script>

<style scoped lang="scss">
@use '@/assets/styles/variables.scss' as *;

/* ===== 题头 ===== */
.hero {
  padding: 10px 0 22px;

  .hero-title {
    font-family: $font-display;
    font-size: 30px;
    font-weight: 700;
    color: $text-primary;
    margin: 0 0 8px;
    letter-spacing: 0.5px;
  }

  .hero-sub {
    font-size: 13.5px;
    color: $text-secondary;
    margin: 0;
  }
}

/* ===== 分类条 ===== */
.cat-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.cat-chip {
  font-size: 13px;
  color: $text-secondary;
  background-color: $bg-surface;
  border: 1px solid $border-default;
  border-radius: 999px;
  padding: 6px 16px;
  cursor: pointer;
  transition: all 0.15s;

  &:hover {
    border-color: $brand-primary;
    color: $brand-primary;
  }

  &.active {
    background-color: $brand-primary;
    border-color: $brand-primary;
    color: #fff;
    font-weight: 600;
  }
}

/* ===== 搜索 ===== */
.search-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}

.search-box {
  display: flex;
  gap: 8px;
  width: 420px;
  max-width: 100%;
}

.search-btn {
  background-color: $brand-primary;
  border-color: $brand-primary;

  &:hover {
    background-color: $brand-hover;
    border-color: $brand-hover;
  }
}

.count-hint {
  font-size: 12.5px;
  color: $text-tertiary;
}

/* ===== 卡片流 ===== */
.card-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: 240px;
}

.article-card {
  display: flex;
  gap: 18px;
  background-color: $bg-surface;
  border: 1px solid $border-default;
  border-radius: $radius-lg;
  padding: 16px;
  cursor: pointer;
  transition: border-color 0.15s, box-shadow 0.15s, transform 0.15s;

  &:hover {
    border-color: $brand-primary;
    box-shadow: 0 6px 20px rgba(31, 107, 90, 0.10);
    transform: translateY(-1px);
  }
}

.cover {
  width: 180px;
  height: 120px;
  flex-shrink: 0;
  border-radius: $radius-md;
  overflow: hidden;
  background-color: $bg-subtle;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
  }

  &.placeholder {
    display: flex;
    align-items: center;
    justify-content: center;
  }
}

.cover-placeholder {
  font-family: $font-display;
  font-size: 40px;
  color: $brand-primary;
  opacity: 0.45;
}

.card-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.card-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;

  .meta-cat {
    font-size: 12px;
    color: $brand-primary;
    background-color: $brand-light;
    border-radius: 3px;
    padding: 1px 8px;
    font-weight: 600;
  }

  .meta-pin,
  .meta-rec {
    font-size: 11px;
    color: $accent-red;
    border: 1px solid rgba(180, 84, 59, 0.4);
    border-radius: 3px;
    padding: 0 6px;
  }

  .meta-rec {
    color: $warning;
    border-color: rgba(201, 138, 45, 0.4);
  }

  .meta-time {
    font-size: 12px;
    color: $text-tertiary;
    margin-left: auto;
  }
}

.card-title {
  font-family: $font-display;
  font-size: 19px;
  font-weight: 700;
  color: $text-primary;
  margin: 0 0 8px;
  line-height: 1.45;
}

.card-summary {
  font-size: 13.5px;
  color: $text-secondary;
  line-height: 1.7;
  margin: 0 0 12px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-foot {
  margin-top: auto;
  display: flex;
  align-items: center;
  gap: 14px;
  font-size: 12.5px;
  color: $text-tertiary;

  .foot-author {
    color: $text-secondary;
    font-weight: 600;
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

.pager {
  display: flex;
  justify-content: center;
  padding-top: 24px;
}
</style>
