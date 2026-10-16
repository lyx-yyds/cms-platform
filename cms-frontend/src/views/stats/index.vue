<template>
  <div class="page-container">
    <h2 class="page-title">数据统计</h2>
    <p class="page-subtitle">
      编辑部访问分析 · 口径：PV=原始请求计数，UV=按（用户ID，IP）去重（文档 7.11）
    </p>

    <!-- 工具栏 -->
    <div class="toolbar">
      <el-radio-group v-model="granularity" size="default" @change="loadTimeDim">
        <el-radio-button value="day">按日</el-radio-button>
        <el-radio-button value="month">按月</el-radio-button>
      </el-radio-group>
      <el-radio-group v-model="days" size="default" @change="loadTimeDim">
        <el-radio-button :value="7">近 7 天</el-radio-button>
        <el-radio-button :value="30">近 30 天</el-radio-button>
        <el-radio-button :value="90">近 90 天</el-radio-button>
      </el-radio-group>
      <div class="spacer" />
      <el-button :loading="loading" @click="loadAll">刷新</el-button>
    </div>

    <!-- 访问趋势 -->
    <div class="panel">
      <div class="panel-head">
        <h3 class="panel-title">访问趋势</h3>
        <span class="panel-hint">PV / UV 时间序列</span>
      </div>
      <div ref="trendRef" class="chart"></div>
    </div>

    <div class="two-col">
      <!-- 文章状态分布 -->
      <div class="panel">
        <div class="panel-head">
          <h3 class="panel-title">文章状态分布</h3>
          <span class="panel-hint">按 status 分组</span>
        </div>
        <div ref="statusRef" class="chart chart-sm"></div>
        <div v-if="articleStats?.statusCount?.length" class="status-legend">
          <span v-for="s in articleStats.statusCount" :key="s.status" class="legend-item">
            <i :style="{ backgroundColor: statusColor(s.status) }" />
            {{ statusLabel(s.status) }} · {{ s.count }}
          </span>
        </div>
      </div>

      <!-- 热门文章 TopN -->
      <div class="panel">
        <div class="panel-head">
          <h3 class="panel-title">热门文章 Top{{ topN }}</h3>
          <span class="panel-hint">按浏览量 · 含 Redis 未落库增量</span>
        </div>
        <div v-loading="loading" class="top-list">
          <div v-for="(a, i) in articleStats?.topArticles || []" :key="a.id" class="top-row">
            <span class="rank" :class="{ 'rank-top': i < 3 }">{{ i + 1 }}</span>
            <a class="top-title" :href="`/article/edit/${a.id}`" target="_blank">{{ a.title }}</a>
            <span class="top-views">{{ a.views }}</span>
          </div>
          <el-empty v-if="!loading && !articleStats?.topArticles?.length" description="暂无已发布文章" :image-size="70" />
        </div>
      </div>
    </div>

    <div class="two-col">
      <!-- 用户增长 -->
      <div class="panel">
        <div class="panel-head">
          <h3 class="panel-title">用户增长</h3>
          <span class="panel-hint">新增用户 · 共 {{ userStats?.total ?? 0 }} 人</span>
        </div>
        <div ref="growthRef" class="chart chart-sm"></div>
      </div>

      <!-- 角色分布 -->
      <div class="panel">
        <div class="panel-head">
          <h3 class="panel-title">角色分布</h3>
          <span class="panel-hint">sys_user_role 联查</span>
        </div>
        <div ref="roleRef" class="chart chart-sm"></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import {
  getArticleStatsApi,
  getVisitsApi,
  getUserStatsApi,
  STATUS_LABEL,
  type ArticleStats,
  type TrendPoint,
  type UserStats
} from '@/api/stats'

/* 编辑部 Editorial 色板 */
const C_PRIMARY = '#1F6B5A'
const C_PRIMARY_DARK = '#1A5A4C'
const C_AMBER = '#C98A2D'
const C_TEXT = '#23262B'
const C_SUB = '#5E6570'
const C_GRID = '#E4E0D8'
const STATUS_COLOR: Record<number, string> = { 0: '#9AA1AB', 1: '#3E7C4F', 2: '#C98A2D' }

const granularity = ref<'day' | 'month'>('day')
const days = ref(30)
const topN = 10
const loading = ref(false)
const trendData = ref<TrendPoint[]>([])
const articleStats = ref<ArticleStats | null>(null)
const userStats = ref<UserStats | null>(null)

const trendRef = ref<HTMLElement>()
const statusRef = ref<HTMLElement>()
const growthRef = ref<HTMLElement>()
const roleRef = ref<HTMLElement>()

let trendChart: echarts.ECharts | null = null
let statusChart: echarts.ECharts | null = null
let growthChart: echarts.ECharts | null = null
let roleChart: echarts.ECharts | null = null

function statusLabel(s: number): string {
  return STATUS_LABEL[s] ?? `未知(${s})`
}
function statusColor(s: number): string {
  return STATUS_COLOR[s] ?? '#9AA1AB'
}

/* ---------- 渲染 ---------- */
function renderTrend() {
  if (!trendChart) return
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
    grid: { left: 46, right: 20, top: 40, bottom: 32, containLabel: false },
    xAxis: {
      type: 'category',
      data: trendData.value.map((t) => t.time),
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
        symbolSize: 6,
        data: trendData.value.map((t) => t.pv),
        lineStyle: { width: 2 },
        itemStyle: { color: C_PRIMARY },
        areaStyle: { color: 'rgba(31,107,90,0.08)' }
      },
      {
        name: 'UV',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        data: trendData.value.map((t) => t.uv),
        lineStyle: { width: 2 },
        itemStyle: { color: C_AMBER }
      }
    ]
  })
}

function renderStatus() {
  if (!statusChart) return
  const list = articleStats.value?.statusCount || []
  const pieData = list.map((s) => ({
    name: statusLabel(s.status),
    value: s.count,
    itemStyle: { color: statusColor(s.status) }
  }))
  statusChart.setOption({
    color: [C_PRIMARY, C_AMBER, '#9AA1AB'],
    tooltip: {
      confine: true,
      backgroundColor: 'rgba(255,255,255,0.96)',
      borderColor: C_GRID,
      textStyle: { color: C_TEXT, fontSize: 12 },
      formatter: '{b}：{c} 篇（{d}%）'
    },
    series: [
      {
        type: 'pie',
        radius: ['52%', '74%'],
        center: ['50%', '50%'],
        avoidLabelOverlap: true,
        itemStyle: { borderColor: '#FFFFFF', borderWidth: 2 },
        label: { color: C_SUB, fontSize: 11, formatter: '{b} {c}' },
        labelLine: { lineStyle: { color: C_GRID } },
        data: pieData.length ? pieData : [{ name: '暂无', value: 1, itemStyle: { color: '#E4E0D8' } }]
      }
    ]
  })
}

function renderGrowth() {
  if (!growthChart) return
  growthChart.setOption({
    color: [C_PRIMARY],
    tooltip: {
      trigger: 'axis',
      confine: true,
      backgroundColor: 'rgba(255,255,255,0.96)',
      borderColor: C_GRID,
      textStyle: { color: C_TEXT, fontSize: 12 },
      formatter: (ps: unknown) => {
        const arr = ps as Array<{ axisValue: string; value: number; marker: string }>
        return `${arr[0].axisValue}<br/>新增 ${arr[0].value} 人`
      }
    },
    grid: { left: 40, right: 16, top: 24, bottom: 32 },
    xAxis: {
      type: 'category',
      data: userStats.value?.growth.map((g) => g.time) || [],
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
        type: 'bar',
        barWidth: '55%',
        data: userStats.value?.growth.map((g) => g.count) || [],
        itemStyle: { color: C_PRIMARY, borderRadius: [4, 4, 0, 0] }
      }
    ]
  })
}

function renderRole() {
  if (!roleChart) return
  const list = userStats.value?.roleDistribution || []
  const names = list.map((r) => r.roleName)
  const vals = list.map((r) => r.count)
  roleChart.setOption({
    color: ['#1F6B5A', '#3E7C4F', '#C98A2D', '#9AA1AB', '#5E6570'],
    tooltip: {
      confine: true,
      backgroundColor: 'rgba(255,255,255,0.96)',
      borderColor: C_GRID,
      textStyle: { color: C_TEXT, fontSize: 12 },
      formatter: '{b}：{c} 人'
    },
    grid: { left: 8, right: 30, top: 12, bottom: 28, containLabel: true },
    xAxis: {
      type: 'value',
      minInterval: 1,
      axisLine: { show: false },
      axisLabel: { color: C_SUB, fontSize: 11 },
      splitLine: { lineStyle: { color: 'rgba(228,224,216,0.55)' } }
    },
    yAxis: {
      type: 'category',
      data: names,
      axisLine: { lineStyle: { color: C_GRID } },
      axisLabel: { color: C_SUB, fontSize: 11 }
    },
    series: [
      {
        type: 'bar',
        barWidth: 14,
        data: vals.map((v, i) => ({ value: v, itemStyle: { color: ['#1F6B5A', '#3E7C4F', '#C98A2D', '#9AA1AB', '#5E6570'][i % 5] } })),
        label: { show: true, position: 'right', color: C_TEXT, fontSize: 11 }
      }
    ]
  })
}

/* ---------- 数据 ---------- */
async function loadVisits() {
  loading.value = true
  try {
    trendData.value = await getVisitsApi(days.value, granularity.value)
    await nextTick()
    renderTrend()
  } finally {
    loading.value = false
  }
}

/** 时间维度切换：趋势图与用户增长图同步刷新，保证口径一致 */
async function loadTimeDim() {
  loading.value = true
  try {
    await Promise.all([loadVisits(), loadUsers()])
  } catch {
    ElMessage.error('统计数据加载失败')
  } finally {
    loading.value = false
  }
}

async function loadArticles() {
  articleStats.value = await getArticleStatsApi(topN)
  await nextTick()
  renderStatus()
}

async function loadUsers() {
  userStats.value = await getUserStatsApi(days.value, granularity.value)
  await nextTick()
  renderGrowth()
  renderRole()
}

async function loadAll() {
  loading.value = true
  try {
    await Promise.all([loadVisits(), loadArticles(), loadUsers()])
  } catch {
    ElMessage.error('统计数据加载失败')
  } finally {
    loading.value = false
  }
}

function onResize() {
  trendChart?.resize()
  statusChart?.resize()
  growthChart?.resize()
  roleChart?.resize()
}

onMounted(() => {
  if (trendRef.value) trendChart = echarts.init(trendRef.value)
  if (statusRef.value) statusChart = echarts.init(statusRef.value)
  if (growthRef.value) growthChart = echarts.init(growthRef.value)
  if (roleRef.value) roleChart = echarts.init(roleRef.value)
  window.addEventListener('resize', onResize)
  loadAll()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  trendChart?.dispose()
  statusChart?.dispose()
  growthChart?.dispose()
  roleChart?.dispose()
})
</script>

<style scoped lang="scss">
@use '@/assets/styles/variables.scss' as *;

.toolbar {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.spacer {
  flex: 1;
}

.panel {
  background-color: $bg-surface;
  border: 1px solid $border-default;
  border-radius: $radius-md;
  padding: 16px;
  margin-bottom: 16px;
}

.panel-head {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 12px;
}

.panel-title {
  font-size: 14px;
  font-weight: 600;
  color: $text-primary;
  margin: 0;
}

.panel-hint {
  font-size: 11.5px;
  color: $text-tertiary;
}

.two-col {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(340px, 1fr));
  gap: 16px;

  .panel {
    margin-bottom: 0;
  }
}

.chart {
  width: 100%;
  height: 300px;
}

.chart-sm {
  height: 240px;
}

.status-legend {
  display: flex;
  gap: 16px;
  justify-content: center;
  font-size: 12px;
  color: $text-tertiary;
  margin-top: 4px;

  .legend-item {
    display: inline-flex;
    align-items: center;
    gap: 5px;
  }

  i {
    display: inline-block;
    width: 9px;
    height: 9px;
    border-radius: 2px;
  }
}

.top-list {
  min-height: 200px;
}

.top-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 7px 2px;
  border-bottom: 1px dashed $border-default;

  &:last-child {
    border-bottom: none;
  }
}

.rank {
  flex: none;
  width: 22px;
  height: 22px;
  border-radius: 5px;
  background-color: $bg-subtle;
  color: $text-tertiary;
  font-size: 12px;
  font-family: $font-mono;
  display: flex;
  align-items: center;
  justify-content: center;
}

.rank-top {
  background-color: $brand-primary;
  color: #fff;
}

.top-title {
  flex: 1;
  min-width: 0;
  font-size: 12.5px;
  color: $text-primary;
  text-decoration: none;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;

  &:hover {
    color: $brand-primary;
  }
}

.top-views {
  flex: none;
  font-size: 12.5px;
  font-family: $font-mono;
  color: $text-primary;
}
</style>
