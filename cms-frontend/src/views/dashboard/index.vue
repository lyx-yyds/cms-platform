<template>
  <div class="page-container">
    <h2 class="page-title">仪表盘</h2>
    <p class="page-subtitle">编辑部今日概览 · 数据来自 /api/stats/dashboard（文档 5.1）</p>

    <!-- 统计卡：大数字 font-display + tabular（无阴影无渐变） -->
    <div class="stat-grid">
      <div class="stat-card">
        <div class="stat-label">文章总数</div>
        <div class="stat-number">{{ d?.articleTotal ?? 0 }}</div>
        <div class="stat-trend">已发布 {{ d?.articlePublished ?? 0 }} · 草稿 {{ d?.articleDraft ?? 0 }} · 下架 {{ d?.articleOffline ?? 0 }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">待审稿件</div>
        <div class="stat-number" :class="{ 'stat-warning': (d?.articleDraft ?? 0) > 0 }">{{ d?.articleDraft ?? 0 }}</div>
        <div class="stat-trend">草稿状态 · 待编辑发布</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">评论待审</div>
        <div class="stat-number" :class="{ 'stat-warning': (d?.commentPending ?? 0) > 0 }">{{ d?.commentPending ?? 0 }}</div>
        <div class="stat-trend">AI 初审后待人工复核</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">今日访问</div>
        <div class="stat-number">{{ (d?.todayPv ?? 0).toLocaleString() }}</div>
        <div class="stat-trend">PV {{ (d?.todayPv ?? 0).toLocaleString() }} · UV {{ (d?.todayUv ?? 0).toLocaleString() }}</div>
      </div>
    </div>

    <!-- 近 7 日访问趋势 -->
    <div class="panel trend-panel">
      <div class="panel-head">
        <h3 class="panel-title">近 7 日访问趋势</h3>
        <el-link type="primary" :underline="false" @click="goStats">查看完整统计</el-link>
      </div>
      <div ref="trendRef" class="trend-chart"></div>
    </div>

    <div class="dashboard-grid">
      <!-- 待审稿件：签名元素稿件台 -->
      <div class="panel">
        <div class="panel-head">
          <h3 class="panel-title">待审稿件</h3>
          <el-link type="primary" :underline="false" @click="goArticleList">查看全部</el-link>
        </div>
        <div v-for="a in d?.pendingArticles || []" :key="a.id" class="article-row pending-item">
          <span class="article-id">#{{ a.id }}</span>
          <span class="pending-title">{{ a.title }}</span>
          <span class="status-ribbon status-draft">草稿</span>
          <span class="pending-meta">{{ a.time }}</span>
        </div>
        <div v-if="!loading && !d?.pendingArticles?.length" class="empty-inline">暂无待审稿件</div>
      </div>

      <!-- 最近发布 -->
      <div class="panel">
        <div class="panel-head">
          <h3 class="panel-title">最近发布</h3>
          <el-link type="primary" :underline="false" @click="goArticleList">查看文章</el-link>
        </div>
        <div v-for="a in d?.recentPublished || []" :key="a.id" class="article-row status-published pending-item">
          <span class="article-id">#{{ a.id }}</span>
          <span class="pending-title">{{ a.title }}</span>
          <span class="status-ribbon status-published">已发布</span>
          <span class="pending-meta">{{ a.views }} 阅读 · {{ a.time }}</span>
        </div>
        <div v-if="!loading && !d?.recentPublished?.length" class="empty-inline">暂无已发布文章</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { getDashboardApi, type DashboardData } from '@/api/stats'

const router = useRouter()
const d = ref<DashboardData | null>(null)
const loading = ref(true)
const trendRef = ref<HTMLElement>()
let trendChart: echarts.ECharts | null = null

const C_PRIMARY = '#1F6B5A'
const C_AMBER = '#C98A2D'
const C_TEXT = '#23262B'
const C_SUB = '#5E6570'
const C_GRID = '#E4E0D8'

function goArticleList() {
  router.push('/article/list')
}
function goStats() {
  router.push('/stats')
}

function renderTrend() {
  if (!trendChart || !d.value) return
  trendChart.setOption({
    color: [C_PRIMARY, C_AMBER],
    tooltip: {
      trigger: 'axis',
      confine: true,
      backgroundColor: 'rgba(255,255,255,0.96)',
      borderColor: C_GRID,
      textStyle: { color: C_TEXT, fontSize: 12 },
      formatter: (ps: unknown) => {
        const arr = ps as Array<{ axisValue: string; seriesName: string; value: number; marker: string }>
        let html = `${arr[0].axisValue}<br/>`
        arr.forEach((p) => {
          html += `${p.marker}${p.seriesName}：<b>${p.value.toLocaleString()}</b><br/>`
        })
        return html
      }
    },
    legend: { data: ['PV', 'UV'], textStyle: { color: C_SUB, fontSize: 12 }, itemWidth: 12, itemHeight: 12 },
    grid: { left: 44, right: 20, top: 38, bottom: 30 },
    xAxis: {
      type: 'category',
      data: d.value.weekTrend.map((t) => t.time.slice(5)),
      axisLine: { lineStyle: { color: C_GRID } },
      axisLabel: { color: C_SUB, fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      axisLine: { show: false },
      axisLabel: { color: C_SUB, fontSize: 11 },
      splitLine: { lineStyle: { color: 'rgba(228,224,216,0.55)' } }
    },
    series: [
      {
        name: 'PV',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 5,
        data: d.value.weekTrend.map((t) => t.pv),
        lineStyle: { width: 2 },
        itemStyle: { color: C_PRIMARY },
        areaStyle: { color: 'rgba(31,107,90,0.08)' }
      },
      {
        name: 'UV',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 5,
        data: d.value.weekTrend.map((t) => t.uv),
        lineStyle: { width: 2 },
        itemStyle: { color: C_AMBER }
      }
    ]
  })
}

function onResize() {
  trendChart?.resize()
}

onMounted(async () => {
  if (trendRef.value) trendChart = echarts.init(trendRef.value)
  window.addEventListener('resize', onResize)
  try {
    d.value = await getDashboardApi()
    await nextTick()
    renderTrend()
  } finally {
    loading.value = false
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  trendChart?.dispose()
})
</script>

<style scoped lang="scss">
@use '@/assets/styles/variables.scss' as *;

.stat-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  flex: 1 1 220px;
  min-width: 0;
  background-color: $bg-surface;
  border: 1px solid $border-default;
  border-radius: $radius-md;
  padding: 20px;
  box-sizing: border-box;
}

.stat-label {
  font-size: 13px;
  color: $text-secondary;
  margin-bottom: 8px;
}

.stat-number {
  font-size: 32px;
  font-weight: 600;
  line-height: 1.3;
  color: $text-primary;
  font-variant-numeric: tabular-nums;
}

.stat-warning {
  color: $warning;
}

.stat-trend {
  margin-top: 8px;
  font-size: 12px;
  color: $text-tertiary;
}

.dashboard-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.panel {
  flex: 1 1 420px;
  min-width: 0;
  background-color: $bg-surface;
  border: 1px solid $border-default;
  border-radius: $radius-md;
  padding: 20px;
  box-sizing: border-box;
}

.trend-panel {
  flex: 1 1 100%;
  margin-bottom: 16px;
}

.trend-chart {
  width: 100%;
  height: 220px;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid $border-default;
}

.panel-title {
  font-size: 16px;
  font-weight: 600;
  margin: 0;
}

.pending-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 0 10px 14px;
  border-bottom: 1px solid #edeae3;
  cursor: pointer;
}

.pending-item:last-child {
  border-bottom: none;
}

.pending-title {
  flex: 1;
  font-size: 13px;
  color: $text-primary;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.pending-meta {
  font-size: 12px;
  color: $text-tertiary;
  flex-shrink: 0;
}

.empty-inline {
  padding: 16px 0;
  font-size: 13px;
  color: $text-tertiary;
}

/* 签名元素：状态色带（设计系统附录 E） */
.status-ribbon {
  flex: none;
  padding: 2px 8px;
  border-radius: 3px;
  font-size: 11px;
  line-height: 1.5;
}

.status-draft {
  background-color: rgba(154, 161, 171, 0.14);
  color: #6b7280;
}

.status-published {
  background-color: rgba(62, 124, 79, 0.12);
  color: #3e7c4f;
}
</style>
