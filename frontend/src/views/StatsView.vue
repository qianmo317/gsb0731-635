<template>
  <div class="stats-page">
    <div class="stats-overview">
      <div class="stat-card">
        <div class="stat-card-icon" style="background: rgba(99, 102, 241, 0.15);">📊</div>
        <div class="stat-card-value">{{ weeklyStats.count || 0 }}</div>
        <div class="stat-card-label">本周运动次数</div>
      </div>
      <div class="stat-card">
        <div class="stat-card-icon" style="background: rgba(16, 185, 129, 0.15);">⏱️</div>
        <div class="stat-card-value">{{ weeklyStats.duration || 0 }}</div>
        <div class="stat-card-label">本周运动时长(分钟)</div>
      </div>
      <div class="stat-card">
        <div class="stat-card-icon" style="background: rgba(245, 158, 11, 0.15);">🔥</div>
        <div class="stat-card-value">{{ formatNumber(weeklyStats.calories || 0) }}</div>
        <div class="stat-card-label">本周消耗卡路里</div>
      </div>
      <div class="stat-card">
        <div class="stat-card-icon" style="background: rgba(139, 92, 246, 0.15);">📅</div>
        <div class="stat-card-value">{{ monthlyStats.count || 0 }}</div>
        <div class="stat-card-label">本月运动次数</div>
      </div>
    </div>
    
    <div class="charts-grid">
      <div class="card chart-card">
        <div class="card-header">
          <h3>卡路里消耗趋势（30天）</h3>
        </div>
        <div class="chart-container">
          <Line v-if="trendData.length" :data="trendChartData" :options="lineChartOptions" />
          <div v-else class="empty-state">
            <div class="empty-state-icon">📈</div>
            <p>暂无数据</p>
          </div>
        </div>
      </div>
      
      <div class="card chart-card">
        <div class="card-header">
          <h3>运动类型分布</h3>
        </div>
        <div class="chart-container">
          <Doughnut v-if="distributionData.length" :data="distributionChartData" :options="doughnutOptions" />
          <div v-else class="empty-state">
            <div class="empty-state-icon">🥧</div>
            <p>暂无数据</p>
          </div>
        </div>
      </div>
    </div>
    
    <div class="card distribution-table">
      <div class="card-header">
        <h3>运动类型统计</h3>
      </div>
      <table v-if="distributionData.length" class="table">
        <thead>
          <tr>
            <th>运动类型</th>
            <th>次数</th>
            <th>占比</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in distributionData" :key="item.type">
            <td>{{ item.type }}</td>
            <td>{{ item.count }}</td>
            <td>{{ getPercentage(item.count) }}%</td>
          </tr>
        </tbody>
      </table>
      <div v-else class="empty-state">暂无数据</div>
    </div>
    
    <div class="card goal-summary-card">
      <div class="card-header">
        <h3>目标达成汇总</h3>
      </div>
      <div class="goal-summary-grid">
        <div class="goal-summary-group">
          <div class="summary-group-header">
            <span class="summary-group-icon">🎯</span>
            <span class="summary-group-title">不限运动项目</span>
          </div>
          <div class="summary-group-body">
            <div class="summary-stat">
              <span class="summary-stat-value">{{ goalSummary.unrestricted?.completed || 0 }}</span>
              <span class="summary-stat-divider">/</span>
              <span class="summary-stat-total">{{ goalSummary.unrestricted?.total || 0 }}</span>
              <span class="summary-stat-label">已达成 / 总数</span>
            </div>
            <div class="summary-rate-row">
              <div class="summary-rate-bar">
                <div class="summary-rate-fill" :style="{ width: (goalSummary.unrestricted?.rate || 0) + '%' }"></div>
              </div>
              <span class="summary-rate-text">{{ goalSummary.unrestricted?.rate || 0 }}%</span>
            </div>
          </div>
        </div>
        
        <div class="goal-summary-group">
          <div class="summary-group-header">
            <span class="summary-group-icon">🏃</span>
            <span class="summary-group-title">限定运动项目</span>
          </div>
          <div class="summary-group-body">
            <div class="summary-stat">
              <span class="summary-stat-value">{{ goalSummary.specificSport?.completed || 0 }}</span>
              <span class="summary-stat-divider">/</span>
              <span class="summary-stat-total">{{ goalSummary.specificSport?.total || 0 }}</span>
              <span class="summary-stat-label">已达成 / 总数</span>
            </div>
            <div class="summary-rate-row">
              <div class="summary-rate-bar">
                <div class="summary-rate-fill specific" :style="{ width: (goalSummary.specificSport?.rate || 0) + '%' }"></div>
              </div>
              <span class="summary-rate-text">{{ goalSummary.specificSport?.rate || 0 }}%</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { Line, Doughnut } from 'vue-chartjs'
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  ArcElement,
  Title,
  Tooltip,
  Legend,
  Filler
} from 'chart.js'
import api from '../services/api'

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, ArcElement, Title, Tooltip, Legend, Filler)

const weeklyStats = reactive({ count: 0, duration: 0, calories: 0 })
const monthlyStats = reactive({ count: 0, duration: 0, calories: 0 })
const trendData = ref([])
const distributionData = ref([])
const goalSummary = reactive({
  unrestricted: { total: 0, completed: 0, rate: 0 },
  specificSport: { total: 0, completed: 0, rate: 0 }
})

const colors = ['#6366f1', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#06b6d4', '#ec4899', '#84cc16']

const trendChartData = computed(() => ({
  labels: trendData.value.map(d => d.date.slice(5)),
  datasets: [{
    label: '卡路里',
    data: trendData.value.map(d => d.calories),
    borderColor: '#6366f1',
    backgroundColor: 'rgba(99, 102, 241, 0.1)',
    fill: true,
    tension: 0.4
  }]
}))

const distributionChartData = computed(() => ({
  labels: distributionData.value.map(d => d.type),
  datasets: [{
    data: distributionData.value.map(d => d.count),
    backgroundColor: colors.slice(0, distributionData.value.length),
    borderWidth: 0
  }]
}))

const lineChartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { display: false }
  },
  scales: {
    x: { grid: { display: false }, ticks: { color: '#64748b' } },
    y: { grid: { color: 'rgba(148, 163, 184, 0.1)' }, ticks: { color: '#64748b' }, beginAtZero: true }
  }
}

const doughnutOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: {
      position: 'bottom',
      labels: { color: '#94a3b8', padding: 20, usePointStyle: true }
    }
  }
}

const totalCount = computed(() => distributionData.value.reduce((sum, item) => sum + item.count, 0))

function getPercentage(count) {
  if (totalCount.value === 0) return 0
  return ((count / totalCount.value) * 100).toFixed(1)
}

function formatNumber(num) {
  return num >= 1000 ? (num / 1000).toFixed(1) + 'k' : num
}

async function fetchWeeklyStats() {
  try {
    const response = await api.get('/api/stats/weekly')
    if (response.data.success) {
      Object.assign(weeklyStats, response.data.data)
    }
  } catch (error) {
    console.error('获取周统计失败:', error)
  }
}

async function fetchMonthlyStats() {
  try {
    const response = await api.get('/api/stats/monthly')
    if (response.data.success) {
      Object.assign(monthlyStats, response.data.data)
    }
  } catch (error) {
    console.error('获取月统计失败:', error)
  }
}

async function fetchTrend() {
  try {
    const response = await api.get('/api/stats/trend?days=30')
    if (response.data.success) {
      trendData.value = response.data.data
    }
  } catch (error) {
    console.error('获取趋势数据失败:', error)
  }
}

async function fetchDistribution() {
  try {
    const response = await api.get('/api/stats/distribution')
    if (response.data.success) {
      distributionData.value = response.data.data
    }
  } catch (error) {
    console.error('获取分布数据失败:', error)
  }
}

async function fetchGoalSummary() {
  try {
    const response = await api.get('/api/stats/goals-summary')
    if (response.data.success) {
      const data = response.data.data
      if (data.unrestricted) {
        Object.assign(goalSummary.unrestricted, data.unrestricted)
      }
      if (data.specificSport) {
        Object.assign(goalSummary.specificSport, data.specificSport)
      }
    }
  } catch (error) {
    console.error('获取目标达成汇总失败:', error)
  }
}

onMounted(() => {
  fetchWeeklyStats()
  fetchMonthlyStats()
  fetchTrend()
  fetchDistribution()
  fetchGoalSummary()
})
</script>

<style scoped>
.stats-overview {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1.5rem;
  margin-bottom: 1.5rem;
}

.charts-grid {
  display: grid;
  grid-template-columns: 3fr 2fr;
  gap: 1.5rem;
  margin-bottom: 1.5rem;
}

.chart-card {
  min-height: 350px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1.5rem;
}

.card-header h3 {
  font-size: 1.125rem;
}

.chart-container {
  height: 280px;
}

.distribution-table {
  padding: 1.5rem;
}

.goal-summary-card {
  padding: 1.5rem;
}

.goal-summary-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.5rem;
}

.goal-summary-group {
  padding: 1.25rem;
  background: rgba(15, 23, 42, 0.5);
  border: 1px solid rgba(148, 163, 184, 0.12);
  border-radius: 0.75rem;
}

.summary-group-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 1rem;
}

.summary-group-icon {
  font-size: 1.25rem;
}

.summary-group-title {
  font-size: 0.9375rem;
  font-weight: 600;
  color: #e2e8f0;
}

.summary-group-body {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.summary-stat {
  display: flex;
  align-items: baseline;
  gap: 0.25rem;
}

.summary-stat-value {
  font-size: 1.75rem;
  font-weight: 700;
  color: #10b981;
}

.summary-stat-divider {
  font-size: 1.25rem;
  color: #64748b;
}

.summary-stat-total {
  font-size: 1.25rem;
  font-weight: 600;
  color: #94a3b8;
}

.summary-stat-label {
  margin-left: 0.5rem;
  font-size: 0.8125rem;
  color: #64748b;
}

.summary-rate-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.summary-rate-bar {
  flex: 1;
  height: 0.5rem;
  background: rgba(71, 85, 105, 0.4);
  border-radius: 1rem;
  overflow: hidden;
}

.summary-rate-fill {
  height: 100%;
  border-radius: 1rem;
  background: linear-gradient(90deg, #6366f1, #10b981);
  transition: width 0.5s ease;
}

.summary-rate-fill.specific {
  background: linear-gradient(90deg, #8b5cf6, #06b6d4);
}

.summary-rate-text {
  font-size: 0.875rem;
  font-weight: 700;
  color: #a5b4fc;
  min-width: 3rem;
  text-align: right;
}

@media (max-width: 1024px) {
  .stats-overview {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .charts-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .stats-overview {
    grid-template-columns: 1fr;
  }
  
  .goal-summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
