<template>
  <div class="site-stats-page">
    <VChart v-if="option" :option="option" :autoresize="true" style="height:400px" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, GridComponent, DataZoomComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { API_BASE_URL } from '../main'
import { getToken } from '../utils/auth'

use([LineChart, TitleComponent, TooltipComponent, GridComponent, DataZoomComponent, CanvasRenderer])

const option = ref(null)

async function loadData() {
  const token = getToken()
  const res = await fetch(`${API_BASE_URL}/api/stats/dau-range?days=30`, {
    headers: { Authorization: `Bearer ${token}` }
  })
  if (res.ok) {
    const data = await res.json()
    data.sort((a, b) => new Date(a.date) - new Date(b.date))
    const dates = data.map(d => d.date)
    const values = data.map(d => d.value)
    option.value = {
      title: { text: '站点 DAU' },
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: dates },
      yAxis: { type: 'value' },
      dataZoom: [{ type: 'slider', start: 80 }, { type: 'inside' }],
      series: [{ type: 'line', areaStyle: {}, smooth: true, data: values }]
    }
  }
}

onMounted(loadData)
</script>

<style scoped>
.site-stats-page {
  padding: 20px;
  max-width: var(--page-max-width);
  background-color: var(--background-color);
  margin: 0 auto;
  height: calc(100vh - var(--header-height) - 40px);
  padding-top: calc(var(--header-height) + 20px);
}
</style>
