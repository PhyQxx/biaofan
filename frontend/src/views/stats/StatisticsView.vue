<template>

        <div class="page-header">
          <h1>数据统计</h1>
          <div class="header-actions">
            <el-select v-model="trendDays" size="default" style="width:120px" @change="loadTrend">
              <el-option label="近7天" :value="7" />
              <el-option label="近14天" :value="14" />
              <el-option label="近30天" :value="30" />
            </el-select>
          </div>
        </div>

        <!-- KPI Cards -->
        <div class="kpi-row">
          <div class="kpi-card kpi-primary">
            <div class="kpi-icon">📋</div>
            <div class="kpi-body">
              <div class="kpi-value">{{ dash.todayTotal }}</div>
              <div class="kpi-label">今日待执行</div>
            </div>
          </div>
          <div class="kpi-card kpi-success">
            <div class="kpi-icon">✅</div>
            <div class="kpi-body">
              <div class="kpi-value">{{ dash.todayCompleted }}</div>
              <div class="kpi-label">今日已完成</div>
            </div>
          </div>
          <div class="kpi-card kpi-danger">
            <div class="kpi-icon">⚠️</div>
            <div class="kpi-body">
              <div class="kpi-value">{{ dash.todayOverdue }}</div>
              <div class="kpi-label">今日超时</div>
            </div>
          </div>
          <div class="kpi-card kpi-purple">
            <div class="kpi-icon">📊</div>
            <div class="kpi-body">
              <div class="kpi-value">{{ dash.completionRate }}%</div>
              <div class="kpi-label">总完成率</div>
            </div>
          </div>
          <div class="kpi-card kpi-orange">
            <div class="kpi-icon">👥</div>
            <div class="kpi-body">
              <div class="kpi-value">{{ dash.totalMembers }}</div>
              <div class="kpi-label">参与成员</div>
            </div>
          </div>
          <div class="kpi-card kpi-blue">
            <div class="kpi-icon">📄</div>
            <div class="kpi-body">
              <div class="kpi-value">{{ dash.totalSOPs }}</div>
              <div class="kpi-label">活跃 SOP</div>
            </div>
          </div>
        </div>

        <!-- Charts Row -->
        <div class="charts-row">
          <!-- Trend Chart -->
          <div class="chart-card chart-wide">
            <div class="chart-title">📈 完成率趋势（近{{ trendDays }}天）</div>
            <div ref="trendChartRef" class="chart-container"></div>
          </div>
          <!-- Leaderboard -->
          <div class="chart-card chart-narrow">
            <div class="chart-title">🏆 执行排行榜 TOP{{ dash.topMembers?.length || 0 }}</div>
            <div class="leaderboard">
              <div
                v-for="m in dash.topMembers"
                :key="m.rank"
                class="leader-item"
              >
                <span class="rank" :class="rankClass(m.rank)">{{ m.rank }}</span>
                <span class="member-name">{{ m.username }}</span>
                <span class="member-count">{{ m.completedCount }}次</span>
                <span class="member-rate">{{ m.completionRate }}%</span>
              </div>
              <div v-if="!dash.topMembers?.length" class="empty-chart">暂无数据</div>
            </div>
          </div>
        </div>

        <!-- Overall Stats -->
        <div class="overall-row">
          <div class="overall-card">
            <div class="overall-title">📊 总体概况</div>
            <div class="overall-grid">
              <div class="overall-item">
                <span class="oi-label">待执行</span>
                <span class="oi-value pending">{{ dash.pendingCount }}</span>
              </div>
              <div class="overall-item">
                <span class="oi-label">已完成</span>
                <span class="oi-value success">{{ dash.completedCount }}</span>
              </div>
              <div class="overall-item">
                <span class="oi-label">总完成率</span>
                <span class="oi-value primary">{{ dash.completionRate }}%</span>
              </div>
              <div class="overall-item">
                <span class="oi-label">参与成员</span>
                <span class="oi-value">{{ dash.totalMembers }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- SOP individual stats -->
        <div class="section-title">📋 各 SOP 执行详情</div>
        <div class="sop-stats-grid" v-if="sopStats.length">
          <div v-for="s in sopStats" :key="s.id" class="sop-stat-card">
            <div class="sop-stat-header">
              <span class="sop-stat-title">{{ s.sopTitle }}</span>
              <span class="sop-stat-rate" :class="rateClass(completionRateOf(s))">
                {{ completionRateOf(s) }}%
              </span>
            </div>
            <div class="sop-stat-bar">
              <div class="sop-stat-bar-fill" :style="{ width: completionRateOf(s) + '%', background: barColor(completionRateOf(s)) }"></div>
            </div>
            <div class="sop-stat-meta">
              <span>总执行 {{ s.totalCount || 0 }} 次</span>
              <span>完成 {{ s.completedCount || 0 }} 次</span>
              <span v-if="s.lastExecutedAt">最近 {{ formatDate(s.lastExecutedAt) }}</span>
            </div>
          </div>
        </div>
        <div v-else class="empty-state">
          <p>暂无 SOP 统计数据</p>
          <p class="empty-sub">开始分发 SOP 后这里会显示统计</p>
        </div>
</template>

<script setup lang="ts">


/**
 * PC 端统计分析页
 * - ECharts 图表：执行完成率、趋势图
 * - 各 SOP 执行数据统计
 */
import { ref, reactive, onMounted, nextTick, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import { getDashboardStats, getTrend, type DashboardStats } from '@/api/stats'
import request from '@/api'


const dash = reactive<DashboardStats>({
  todayTotal: 0, todayCompleted: 0, todayOverdue: 0,
  pendingCount: 0, completedCount: 0, completionRate: 0,
  totalMembers: 0, totalSOPs: 0, trend: [], topMembers: []
})

const trendDays = ref(7)
const trendChartRef = ref<HTMLElement>()
let trendChart: echarts.ECharts | null = null

const sopStats = ref<any[]>([])

const loadDashboard = async () => {
  try {
    const res = await getDashboardStats()
    Object.assign(dash, res.data)
  } catch (e) {
    console.error('Dashboard load error', e)
  }
}

const loadTrend = async () => {
  try {
    const res = await getTrend(trendDays.value)
    dash.trend = res.data
    await nextTick()
    renderTrendChart()
  } catch (e) {
    console.error('Trend load error', e)
  }
}

const loadSopStats = async () => {
  try {
    const res = await request.get('/stats/my') as any
    if (res.success && res.data) {
      sopStats.value = res.data
    }
  } catch (e) {
    console.error('Sop stats error', e)
  }
}

const renderTrendChart = () => {
  if (!trendChartRef.value || !dash.trend.length) return
  if (trendChart) trendChart.dispose()
  trendChart = echarts.init(trendChartRef.value)
  const dates = dash.trend.map(t => t.date.slice(5))
  const rates = dash.trend.map(t => t.rate)
  const completed = dash.trend.map(t => t.completed)
  const option = {
    backgroundColor: 'transparent',
    grid: { top: 20, right: 20, bottom: 30, left: 50 },
    tooltip: { trigger: 'axis', formatter: (params: any) => {
      const p = params[0]
      return `${p.name}<br/>完成率: <b>${p.value}%</b><br/>完成: ${completed[p.dataIndex]} 次`
    }},
    xAxis: { type: 'category', data: dates, axisLine: { lineStyle: { color: '#E8E8E8' } }, axisLabel: { color: '#666' } },
    yAxis: { type: 'value', max: 100, axisLabel: { formatter: '{value}%', color: '#666' }, splitLine: { lineStyle: { color: '#F0F0F0' } } },
    series: [{
      name: '完成率',
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 8,
      lineStyle: { color: '#5B7FFF', width: 3 },
      itemStyle: { color: '#5B7FFF' },
      areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
        { offset: 0, color: 'rgba(91,127,255,0.25)' },
        { offset: 1, color: 'rgba(91,127,255,0.02)' }
      ]) },
      data: rates
    }]
  }
  trendChart.setOption(option)
}

const handleResize = () => trendChart?.resize()
window.addEventListener('resize', handleResize)

const completionRateOf = (s: any) => s.totalCount ? Math.round((s.completedCount / s.totalCount) * 100) : 0
const rateClass = (r: number) => r >= 80 ? 'rate-high' : r >= 50 ? 'rate-mid' : 'rate-low'
const barColor = (r: number) => r >= 80 ? '#52C41A' : r >= 50 ? '#FAAD14' : '#FF4D4F'
const rankClass = (rank: number) => rank === 1 ? 'gold' : rank === 2 ? 'silver' : rank === 3 ? 'bronze' : ''

const formatDate = (d: string) => {
  if (!d) return ''
  return d.slice(0, 10)
}

onMounted(async () => {
  await loadDashboard()
  await loadTrend()
  await loadSopStats()
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
})
</script>

<style scoped lang="scss">
.stats-page { min-height: 100vh; background: #F5F7FA; }

.topbar {
  height: 56px; background: #fff; border-bottom: 1px solid #E8E8E8;
  display: flex; align-items: center; padding: 0 24px; justify-content: space-between;
  .logo { display: flex; align-items: center; gap: 8px; font-weight: 700; font-size: 18px; color: #5B7FFF; }
  .topbar-right { display: flex; align-items: center; gap: 12px; }
  .btn-new { background: #5B7FFF; color: #fff; border: none; border-radius: 6px; padding: 8px 16px; font-size: 14px; cursor: pointer; &:hover { background: #7994FF; } }
  .avatar { width: 36px; height: 36px; border-radius: 50%; background: #5B7FFF; color: #fff; display: flex; align-items: center; justify-content: center; font-weight: 600; cursor: pointer; }
}

.main-layout { display: flex; }
.sidebar {
  width: 200px; background: #fff; border-right: 1px solid #E8E8E8; min-height: calc(100vh - 56px); padding: 16px 0;
  .sidebar-item { display: flex; align-items: center; gap: 10px; padding: 12px 20px; cursor: pointer; font-size: 14px; color: #666; border-radius: 0; transition: all .15s; &:hover { background: #F0F3FF; color: #5B7FFF; } &.active { background: #E8ECFF; color: #5B7FFF; font-weight: 600; } }
  .sidebar-divider { height: 1px; background: #F0F0F0; margin: 8px 0; }
}
.main-content { flex: 1; padding: 24px; overflow-y: auto; min-height: calc(100vh - 56px); }

.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px;
  h1 { font-size: 22px; font-weight: 700; color: #212121; margin: 0; }
}

.kpi-row { display: grid; grid-template-columns: repeat(6, 1fr); gap: 16px; margin-bottom: 20px; }
.kpi-card { background: #fff; border-radius: 12px; padding: 20px; display: flex; align-items: center; gap: 14px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); transition: transform .2s, box-shadow .2s;
  &:hover { transform: translateY(-2px); box-shadow: 0 6px 16px rgba(91,127,255,0.12); }
  .kpi-icon { font-size: 32px; }
  .kpi-body { flex: 1; }
  .kpi-value { font-size: 26px; font-weight: 700; color: #212121; line-height: 1.1; }
  .kpi-label { font-size: 13px; color: #999; margin-top: 4px; }
  &.kpi-primary { border-left: 4px solid #5B7FFF; }
  &.kpi-success { border-left: 4px solid #52C41A; }
  &.kpi-danger { border-left: 4px solid #FF4D4F; }
  &.kpi-purple { border-left: 4px solid #8B5CF6; }
  &.kpi-orange { border-left: 4px solid #FAAD14; }
  &.kpi-blue { border-left: 4px solid #1677FF; }
}

.charts-row { display: flex; gap: 16px; margin-bottom: 20px; }
.chart-card { background: #fff; border-radius: 12px; padding: 20px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
.chart-wide { flex: 2; }
.chart-narrow { flex: 1; min-width: 280px; }
.chart-title { font-size: 15px; font-weight: 600; color: #333; margin-bottom: 16px; }
.chart-container { height: 200px; width: 100%; }

.leaderboard { display: flex; flex-direction: column; gap: 10px; }
.leader-item { display: flex; align-items: center; gap: 10px; padding: 8px 12px; border-radius: 8px; background: #F5F7FA;
  .rank { width: 22px; height: 22px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 12px; font-weight: 700; color: #fff; background: #BDBDBD; flex-shrink: 0;
    &.gold { background: linear-gradient(135deg,#FFD700,#FFA500); }
    &.silver { background: linear-gradient(135deg,#C0C0C0,#999); }
    &.bronze { background: linear-gradient(135deg,#CD7F32,#A0522D); }
  }
  .member-name { flex: 1; font-size: 14px; color: #333; font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  .member-count { font-size: 13px; color: #666; }
  .member-rate { font-size: 13px; font-weight: 600; color: #52C41A; min-width: 45px; text-align: right; }
}

.overall-row { margin-bottom: 24px; }
.overall-card { background: #fff; border-radius: 12px; padding: 20px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
.overall-title { font-size: 15px; font-weight: 600; color: #333; margin-bottom: 16px; }
.overall-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.overall-item { display: flex; flex-direction: column; align-items: center; gap: 4px;
  .oi-label { font-size: 13px; color: #999; }
  .oi-value { font-size: 24px; font-weight: 700; &.pending { color: #FAAD14; } &.success { color: #52C41A; } &.primary { color: #5B7FFF; } }
}

.section-title { font-size: 16px; font-weight: 600; color: #333; margin-bottom: 16px; }
.sop-stats-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; }
.sop-stat-card { background: #fff; border-radius: 10px; padding: 16px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
.sop-stat-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.sop-stat-title { font-size: 15px; font-weight: 600; color: #333; }
.sop-stat-rate { font-size: 16px; font-weight: 700; &.rate-high { color: #52C41A; } &.rate-mid { color: #FAAD14; } &.rate-low { color: #FF4D4F; } }
.sop-stat-bar { height: 8px; background: #F0F0F0; border-radius: 4px; overflow: hidden; margin-bottom: 8px; }
.sop-stat-bar-fill { height: 100%; border-radius: 4px; transition: width .3s; }
.sop-stat-meta { display: flex; justify-content: space-between; font-size: 12px; color: #999; }

.empty-state { text-align: center; padding: 40px; color: #999; background: #fff; border-radius: 12px;
  p { margin: 0 0 8px; }
  .empty-sub { font-size: 13px; }
}
.empty-chart { text-align: center; padding: 40px; color: #ccc; font-size: 14px; }
</style>
