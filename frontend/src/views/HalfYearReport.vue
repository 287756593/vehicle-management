<template>
  <div class="half-year-report page-shell">
    <section class="report-hero">
      <div class="report-hero__copy">
        <div class="report-hero__eyebrow">半年经营视图</div>
        <h1 class="report-hero__title">公务车辆半年用车报告</h1>
        <p class="report-hero__desc">
          把近 6 个月的出车、油费、里程、异常闭环与车辆活跃度整理成正式汇报稿，适合办公室直接导出 PDF 留档。
        </p>
        <div class="report-hero__chips" v-if="report">
          <span class="hero-chip">{{ report.period?.label }}</span>
          <span class="hero-chip hero-chip--light">生成时间 {{ report.generatedAt }}</span>
          <span class="hero-chip hero-chip--light">生成人 {{ generatedBy }}</span>
        </div>
      </div>

      <div class="report-hero__actions">
        <el-date-picker
          v-model="endMonth"
          type="month"
          value-format="YYYY-MM"
          format="YYYY 年 MM 月"
          placeholder="选择截止月份"
          :clearable="false"
          class="report-hero__picker"
        />
        <el-button :loading="loading" @click="loadReport">刷新报告</el-button>
        <el-button type="primary" :disabled="!report" @click="exportPdf">导出 PDF</el-button>
      </div>
    </section>

    <div v-if="report" class="report-content">
      <section id="half-year-report-printable" class="report-sheet">
        <section class="sheet-cover">
          <div class="sheet-cover__main">
            <div class="sheet-cover__eyebrow">公务车辆管理系统</div>
            <div class="sheet-cover__title">半年用车与燃油费用汇报</div>
            <div class="sheet-cover__meta">
              统计区间：{{ report.period?.label }} ｜ 生成时间：{{ report.generatedAt }} ｜ 生成人：{{ generatedBy }}
            </div>
            <div class="sheet-cover__summary">
              <article v-for="item in summaryParagraphs" :key="item" class="summary-callout">
                {{ item }}
              </article>
            </div>
          </div>

          <div class="sheet-cover__focus">
            <div class="sheet-cover__focus-label">汇报核心指标</div>
            <div class="sheet-cover__focus-value">{{ formatNumber(overview.totalMileage, 1) }}</div>
            <div class="sheet-cover__focus-unit">公里</div>
            <div class="sheet-cover__focus-caption">半年累计行驶里程</div>
          </div>
        </section>

        <div class="report-grid report-grid--metrics">
          <article v-for="card in overviewCards" :key="card.label" class="metric-card">
            <span class="metric-card__label">{{ card.label }}</span>
            <strong class="metric-card__value">{{ card.value }}</strong>
            <small class="metric-card__hint">{{ card.hint }}</small>
          </article>
        </div>

        <div class="report-grid report-grid--metrics">
          <article v-for="card in fuelCards" :key="card.label" class="metric-card metric-card--fuel">
            <span class="metric-card__label">{{ card.label }}</span>
            <strong class="metric-card__value">{{ card.value }}</strong>
            <small class="metric-card__hint">{{ card.hint }}</small>
          </article>
        </div>

        <section class="briefing-grid">
          <article class="report-panel">
            <div class="report-panel__header">
              <div>
                <div class="report-panel__title">汇报摘要</div>
                <div class="report-panel__desc">把这半年最适合口头汇报的内容，压成几条清晰结论。</div>
              </div>
            </div>

            <div class="summary-list">
              <div v-for="item in summaryParagraphs" :key="item" class="summary-list__item">
                {{ item }}
              </div>
            </div>
          </article>

          <article class="report-panel report-panel--focus">
            <div class="report-panel__header">
              <div>
                <div class="report-panel__title">管理关注点</div>
                <div class="report-panel__desc">聚焦陕U-W873P 与陕A-83GR6 两辆车的单车油耗和燃油费用，便于单车对比汇报。</div>
              </div>
            </div>

            <div class="focus-grid">
              <article v-for="card in managementCards" :key="card.label" class="focus-card">
                <span class="focus-card__label">{{ card.label }}</span>
                <strong class="focus-card__value">{{ card.value }}</strong>
                <small class="focus-card__hint">{{ card.hint }}</small>
              </article>
            </div>
          </article>
        </section>

        <section class="report-panel report-panel--charts">
          <div class="report-panel__header">
            <div>
              <div class="report-panel__title">图表总览</div>
              <div class="report-panel__desc">用更适合正式汇报的趋势图和排行图，把半年业务变化、费用结构和重点对象集中呈现。</div>
            </div>
          </div>

          <div class="chart-matrix">
            <article class="chart-card">
              <div class="chart-card__head">
                <strong>出车与里程趋势</strong>
                <span>观察月度出车强度和行驶里程变化。</span>
              </div>
              <div ref="operationTrendChartRef" class="chart-surface chart-surface--lg"></div>
            </article>

            <article class="chart-card">
              <div class="chart-card__head">
                <strong>油费与加油量趋势</strong>
                <span>适合在汇报时直接说明费用波动来源。</span>
              </div>
              <div ref="fuelTrendChartRef" class="chart-surface chart-surface--lg"></div>
            </article>

            <article class="chart-card">
              <div class="chart-card__head">
                <strong>驾驶员活跃度排行</strong>
                <span>按半年累计里程展示主要使用人。</span>
              </div>
              <div ref="driverRankChartRef" class="chart-surface"></div>
            </article>

            <article class="chart-card">
              <div class="chart-card__head">
                <strong>高频去向排行</strong>
                <span>看清半年内最集中的用车去向。</span>
              </div>
              <div ref="destinationRankChartRef" class="chart-surface"></div>
            </article>
          </div>
        </section>

        <section class="report-panels">
          <article class="report-panel report-panel--trend">
            <div class="report-panel__header">
              <div>
                <div class="report-panel__title">趋势解读</div>
                <div class="report-panel__desc">把图表里的关键月份再拆成文字卡，便于打印阅读和口头汇报时快速抓重点。</div>
              </div>
            </div>

            <div class="trend-cards">
              <article v-for="item in monthlyTrendView" :key="item.month" class="trend-card">
                <div class="trend-card__head">
                  <strong>{{ formatMonthLabel(item.month) }}</strong>
                  <span>{{ item.tripCount || 0 }} 次出车</span>
                </div>

                <div class="trend-card__bar-row">
                  <span>里程</span>
                  <div class="trend-card__bar"><i :style="{ width: item.mileageWidth }"></i></div>
                  <strong>{{ formatNumber(item.mileage, 1) }} km</strong>
                </div>

                <div class="trend-card__bar-row">
                  <span>油费</span>
                  <div class="trend-card__bar trend-card__bar--warm"><i :style="{ width: item.fuelCostWidth }"></i></div>
                  <strong>{{ formatCurrency(item.fuelCost) }}</strong>
                </div>

                <div class="trend-card__meta">
                  <span>车辆 {{ item.vehicleCount || 0 }} 台</span>
                  <span>异常 {{ item.issueCount || 0 }} 条</span>
                </div>

                <div v-if="item.vehicleDetails?.length" class="trend-card__vehicles">
                  <div class="trend-card__vehicles-label">当月车辆</div>
                  <div class="trend-vehicle-list">
                    <article
                      v-for="vehicle in item.vehicleDetails.slice(0, 4)"
                      :key="`${item.month}-${vehicle.vehicleId || vehicle.plateNumber}`"
                      class="trend-vehicle"
                    >
                      <strong>{{ vehicle.plateNumber || '-' }}</strong>
                      <span>{{ vehicle.tripCount || 0 }} 次 ｜ {{ formatNumber(vehicle.mileage, 1) }} km</span>
                      <small>油费 {{ formatCurrency(vehicle.fuelCost) }}</small>
                    </article>
                  </div>
                  <div v-if="item.vehicleDetails.length > 4" class="trend-card__more">
                    其余 {{ item.vehicleDetails.length - 4 }} 台车辆已折叠
                  </div>
                </div>
                <div v-else class="trend-card__empty">本月暂无车辆出车</div>
              </article>
            </div>
          </article>

          <article class="report-panel report-panel--aside">
            <div class="mini-panel">
              <div class="report-panel__title">图表解读</div>
              <div class="highlight-list" v-if="chartHighlights.length">
                <article v-for="item in chartHighlights" :key="item.label" class="highlight-card">
                  <span class="highlight-card__label">{{ item.label }}</span>
                  <strong class="highlight-card__value">{{ item.value }}</strong>
                  <small class="highlight-card__hint">{{ item.hint }}</small>
                </article>
              </div>
              <el-empty v-else description="暂无图表解读数据" :image-size="70" />
            </div>

            <div class="mini-panel">
              <div class="report-panel__title">车辆聚焦</div>
              <div class="vehicle-spotlight-list" v-if="vehicleSpotlights.length">
                <div v-for="item in vehicleSpotlights" :key="item.vehicleId" class="vehicle-spotlight">
                  <div class="vehicle-spotlight__top">
                    <strong>{{ item.plateNumber }}</strong>
                    <span>{{ formatVehicleStatus(item.status) }}</span>
                  </div>
                  <div class="vehicle-spotlight__line">
                    半年出车 {{ item.tripCount }} 次 ｜ 里程 {{ formatNumber(item.totalMileage, 1) }} km
                  </div>
                  <div class="vehicle-spotlight__line">
                    油费 {{ formatCurrency(item.fuelCost) }} ｜ 最近 {{ item.latestDriver || '-' }}
                  </div>
                </div>
              </div>
              <el-empty v-else description="暂无车辆数据" :image-size="70" />
            </div>
          </article>
        </section>

        <section class="report-panel">
          <div class="report-panel__header">
            <div>
              <div class="report-panel__title">车辆明细</div>
              <div class="report-panel__desc">把原来分散字段压缩成更适合 A4 汇报阅读的结构，兼顾里程、油费和最近任务。</div>
            </div>
          </div>

          <div class="report-table-wrapper">
            <table class="report-table report-table--vehicles">
              <thead>
                <tr>
                  <th>车牌号</th>
                  <th>车型 / 状态</th>
                  <th>当前里程</th>
                  <th>半年出车</th>
                  <th>半年里程</th>
                  <th>加油量 / 金额</th>
                  <th>最近任务</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in report.vehicleSummaries || []" :key="item.vehicleId">
                  <td>
                    <div class="table-plate">{{ item.plateNumber }}</div>
                  </td>
                  <td>
                    <div class="table-stack">
                      <strong>{{ item.model }}</strong>
                      <span>{{ formatVehicleStatus(item.status) }}</span>
                    </div>
                  </td>
                  <td>{{ formatNumber(item.currentMileage, 1) }} km</td>
                  <td>{{ item.tripCount }}</td>
                  <td>{{ formatNumber(item.totalMileage, 1) }} km</td>
                  <td>
                    <div class="table-stack">
                      <strong>{{ formatNumber(item.fuelVolume, 1) }} L</strong>
                      <span>{{ formatCurrency(item.fuelCost) }}</span>
                    </div>
                  </td>
                  <td>
                    <div class="table-stack">
                      <strong>{{ item.latestDriver || '-' }}</strong>
                      <span>{{ item.latestDestination || '-' }}</span>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <section class="report-panel report-panel--insights">
          <div class="report-panel__header">
            <div>
              <div class="report-panel__title">报告摘要与建议</div>
              <div class="report-panel__desc">这一页保留管理层最关心的结论与建议，导出后适合直接进入汇报材料末页。</div>
            </div>
          </div>

          <div class="insight-section">
            <ul class="insight-list">
              <li v-for="(item, index) in report.insights || []" :key="index">{{ item }}</li>
            </ul>
            <div class="report-note">
              本报告由系统根据借还车、加油和车辆台账自动汇总，仅用于内部经营分析与阶段汇报。
            </div>
          </div>
        </section>
      </section>
    </div>

    <el-empty v-else-if="!loading" description="暂无报告数据" :image-size="90" />
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts/core'
import { BarChart, LineChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { getHalfYearReport } from '@/api/statistics'
import { useUserStore } from '@/store/user'

echarts.use([BarChart, LineChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer])

const userStore = useUserStore()

function getCurrentMonthValue() {
  const now = new Date()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  return `${now.getFullYear()}-${month}`
}

function normalizePlateNumber(value) {
  return String(value || '')
    .trim()
    .toUpperCase()
    .replace(/\s+/g, '')
}

const loading = ref(false)
const report = ref(null)
const endMonth = ref(getCurrentMonthValue())
const operationTrendChartRef = ref(null)
const fuelTrendChartRef = ref(null)
const driverRankChartRef = ref(null)
const destinationRankChartRef = ref(null)
const chartInstances = {}

const generatedBy = computed(() => userStore.userInfo?.realName || userStore.userInfo?.username || '管理员')
const overview = computed(() => report.value?.overview || {})
const vehicleSpotlights = computed(() => (report.value?.vehicleSummaries || [])
  .filter(item => toNumber(item.tripCount) > 0 || toNumber(item.totalMileage) > 0 || toNumber(item.fuelCost) > 0)
  .slice(0, 4))

const busiestMonth = computed(() => {
  const list = report.value?.monthlyTrend || []
  if (!list.length) {
    return null
  }
  return [...list].sort((left, right) => {
    const tripDiff = toNumber(right.tripCount) - toNumber(left.tripCount)
    if (tripDiff !== 0) {
      return tripDiff
    }
    return toNumber(right.mileage) - toNumber(left.mileage)
  })[0]
})

const fuelPeakMonth = computed(() => pickPeakMonth('fuelCost'))
const issuePeakMonth = computed(() => pickPeakMonth('issueCount'))
const topDriver = computed(() => report.value?.topDrivers?.[0] || null)
const topDestination = computed(() => report.value?.topDestinations?.[0] || null)

const chartHighlights = computed(() => [
  busiestMonth.value
    ? {
        label: '出车高峰',
        value: `${formatMonthLabel(busiestMonth.value.month)} · ${busiestMonth.value.tripCount || 0} 次`,
        hint: `对应里程 ${formatNumber(busiestMonth.value.mileage, 1)} km`
      }
    : null,
  fuelPeakMonth.value
    ? {
        label: '油费峰值',
        value: `${formatMonthLabel(fuelPeakMonth.value.month)} · ${formatCurrency(fuelPeakMonth.value.fuelCost)}`,
        hint: `同期加油 ${formatNumber(fuelPeakMonth.value.fuelVolume, 1)} L`
      }
    : null,
  issuePeakMonth.value && toNumber(issuePeakMonth.value.issueCount) > 0
    ? {
        label: '异常集中',
        value: `${formatMonthLabel(issuePeakMonth.value.month)} · ${issuePeakMonth.value.issueCount || 0} 条`,
        hint: '建议同步核对异常闭环责任与时点'
      }
    : null,
  topDriver.value
    ? {
        label: '活跃驾驶员',
        value: `${topDriver.value.driverName}`,
        hint: `${topDriver.value.tripCount || 0} 次用车 ｜ ${formatNumber(topDriver.value.mileage, 1)} km`
      }
    : null,
  topDestination.value
    ? {
        label: '高频去向',
        value: `${topDestination.value.destination}`,
        hint: `半年出现 ${topDestination.value.count || 0} 次`
      }
    : null
].filter(Boolean).slice(0, 4))

const overviewCards = computed(() => [
  {
    label: '半年出车次数',
    value: `${overview.value.totalTrips || 0} 次`,
    hint: `已还车 ${overview.value.returnedTrips || 0} 次`
  },
  {
    label: '覆盖车辆',
    value: `${overview.value.uniqueVehicles || 0} 台`,
    hint: `涉及驾驶员 ${overview.value.uniqueDrivers || 0} 人`
  },
  {
    label: '累计行驶里程',
    value: `${formatNumber(overview.value.totalMileage, 1)} km`,
    hint: `单次平均 ${formatNumber(overview.value.avgTripMileage, 1)} km`
  },
  {
    label: '异常与闭环',
    value: `${overview.value.issueCount || 0} / ${overview.value.pendingFollowUpCount || 0}`,
    hint: '异常上报 / 待闭环'
  }
])

const fuelCards = computed(() => [
  {
    label: '加油记录',
    value: `${overview.value.fuelRecordCount || 0} 条`,
    hint: `现金加油 ${overview.value.cashFuelCount || 0} 条`
  },
  {
    label: '累计加油量',
    value: `${formatNumber(overview.value.totalFuelAmount, 1)} L`,
    hint: `平均油价 ${formatCurrencyNumber(overview.value.avgFuelPrice)}/L`
  },
  {
    label: '累计加油金额',
    value: formatCurrency(overview.value.totalFuelCost),
    hint: `单笔平均 ${formatCurrency(overview.value.avgFuelCostPerRecord)}`
  },
  {
    label: '平均用车时长',
    value: `${formatNumber(overview.value.avgTripDurationHours, 1)} 小时`,
    hint: '仅统计已还车记录'
  }
])

const managementFocusPlates = ['陕U-W873P', '陕A-83GR6']

const managementCards = computed(() => {
  const summaries = report.value?.vehicleSummaries || []
  const fuelConsumptionCards = managementFocusPlates.map(plateNumber => {
    const vehicle = summaries.find(item => normalizePlateNumber(item.plateNumber) === normalizePlateNumber(plateNumber))
    const totalMileage = toNumber(vehicle?.totalMileage)
    const fuelVolume = toNumber(vehicle?.fuelVolume)

    return {
      label: `${plateNumber} 百公里油耗`,
      value: `${formatNumber(calculateHundredKmFuelVolume(fuelVolume, totalMileage), 2)} L`,
      hint: `半年里程 ${formatNumber(totalMileage, 1)} km ｜ 加油量 ${formatNumber(fuelVolume, 1)} L`
    }
  })

  const fuelCostCards = managementFocusPlates.map(plateNumber => {
    const vehicle = summaries.find(item => normalizePlateNumber(item.plateNumber) === normalizePlateNumber(plateNumber))
    const totalMileage = toNumber(vehicle?.totalMileage)
    const fuelCost = toNumber(vehicle?.fuelCost)

    return {
      label: `${plateNumber} 百公里燃油费`,
      value: `${formatCurrencyNumber(calculateFuelCostPerHundredKm(fuelCost, totalMileage))} 元`,
      hint: `半年里程 ${formatNumber(totalMileage, 1)} km ｜ 油费 ${formatCurrency(vehicle?.fuelCost)}`
    }
  })

  return [...fuelConsumptionCards, ...fuelCostCards]
})

const summaryParagraphs = computed(() => {
  const topVehicle = report.value?.vehicleSummaries?.[0]
  const topDriver = report.value?.topDrivers?.[0]
  const topDestination = report.value?.topDestinations?.[0]
  const lines = [
    `统计期内共完成 ${overview.value.totalTrips || 0} 次用车，累计行驶 ${formatNumber(overview.value.totalMileage, 1)} 公里，累计油费 ${formatCurrency(overview.value.totalFuelCost)}。`,
    busiestMonth.value
      ? `${formatMonthLabel(busiestMonth.value.month)} 为阶段高峰，完成 ${busiestMonth.value.tripCount || 0} 次出车，产生 ${formatCurrency(busiestMonth.value.fuelCost)} 油费。`
      : '',
    topVehicle
      ? `${topVehicle.plateNumber} 是最活跃车辆，半年内出车 ${topVehicle.tripCount || 0} 次，累计 ${formatNumber(topVehicle.totalMileage, 1)} 公里。`
      : '',
    topDriver
      ? `${topDriver.driverName} 是最活跃驾驶员，完成 ${topDriver.tripCount || 0} 次用车，累计 ${formatNumber(topDriver.mileage, 1)} 公里。`
      : '',
    topDestination
      ? `高频去向集中在“${topDestination.destination}”，半年内出现 ${topDestination.count || 0} 次。`
      : '',
    toNumber(overview.value.pendingFollowUpCount) > 0
      ? `当前仍有 ${overview.value.pendingFollowUpCount} 条闭环事项待处理，建议在导出汇报时同步附上责任分工和处理时点。`
      : ''
  ]
  return lines.filter(Boolean).slice(0, 4)
})

const monthlyTrendView = computed(() => {
  const list = report.value?.monthlyTrend || []
  const maxMileage = getMaxValue(list, item => toNumber(item.mileage))
  const maxFuelCost = getMaxValue(list, item => toNumber(item.fuelCost))
  return list.map(item => ({
    ...item,
    mileageWidth: buildBarWidth(item.mileage, maxMileage),
    fuelCostWidth: buildBarWidth(item.fuelCost, maxFuelCost)
  }))
})

const loadReport = async () => {
  loading.value = true
  try {
    report.value = await getHalfYearReport({ endMonth: endMonth.value })
    await nextTick()
    renderAllCharts()
  } catch (error) {
    report.value = null
    disposeCharts()
    ElMessage.error(error.message || '加载半年报告失败')
  } finally {
    loading.value = false
  }
}

const exportPdf = async () => {
  if (!report.value) {
    ElMessage.warning('请先加载报告数据')
    return
  }

  const printable = window.open('', '_blank', 'width=1280,height=960')
  if (!printable) {
    ElMessage.error('浏览器拦截了新窗口，请允许弹窗后重试')
    return
  }

  await nextTick()
  renderAllCharts()
  await wait(120)

  printable.onload = () => {
    printable.focus()
    window.setTimeout(() => {
      printable.print()
    }, 180)
  }
  printable.document.write(buildPrintHtml(collectChartImages()))
  printable.document.close()
}

const buildPrintHtml = (chartImages) => {
  const vehicleChunks = chunkArray(report.value?.vehicleSummaries || [], 12)
  const totalPages = vehicleChunks.length + 3

  const coverPage = buildPrintPage(
    1,
    totalPages,
    `
      <section class="print-cover">
        <div class="print-cover__main">
          <div class="print-cover__eyebrow">公务车辆管理系统</div>
          <div class="print-cover__title">半年用车与燃油费用汇报</div>
          <div class="print-cover__meta">
            统计区间：${escapeHtml(report.value?.period?.label || '-')} ｜ 生成时间：${escapeHtml(report.value?.generatedAt || '-')} ｜ 生成人：${escapeHtml(generatedBy.value)}
          </div>
          <div class="print-callouts">
            ${summaryParagraphs.value.map(item => `<div class="print-callout">${escapeHtml(item)}</div>`).join('')}
          </div>
        </div>
        <div class="print-cover__focus">
          <span>半年总里程</span>
          <strong>${escapeHtml(formatNumber(overview.value.totalMileage, 1))}</strong>
          <small>公里</small>
        </div>
      </section>

      <section class="print-grid print-grid--metrics">
        ${overviewCards.value.map(buildMetricCardHtml).join('')}
      </section>
      <section class="print-grid print-grid--metrics">
        ${fuelCards.value.map(card => buildMetricCardHtml(card, true)).join('')}
      </section>

      <section class="print-focus-grid">
        ${managementCards.value.map(card => `
          <article class="print-focus-card">
            <span>${escapeHtml(card.label)}</span>
            <strong>${escapeHtml(card.value)}</strong>
            <small>${escapeHtml(card.hint)}</small>
          </article>
        `).join('')}
      </section>
    `
  )

  const trendPage = buildPrintPage(
    2,
    totalPages,
    `
      <section class="print-section">
        <div class="print-section__head">
          <div class="print-section__title">图表总览与重点解读</div>
          <div class="print-section__desc">把趋势变化、重点排行和汇报关注点放到同一页，导出后更接近正式汇报材料。</div>
        </div>

        <div class="print-chart-grid">
          ${buildPrintChartCard('出车与里程趋势', '观察月度出车强度和里程变化', chartImages.operation)}
          ${buildPrintChartCard('油费与加油量趋势', '说明费用与补能波动来源', chartImages.fuel)}
          ${buildPrintChartCard('驾驶员活跃度排行', '按半年累计里程排序', chartImages.driver)}
          ${buildPrintChartCard('高频去向排行', '按半年出现次数排序', chartImages.destination)}
        </div>

        <div class="print-highlight-grid">
          ${chartHighlights.value.map(item => `
            <article class="print-highlight-card">
              <span>${escapeHtml(item.label)}</span>
              <strong>${escapeHtml(item.value)}</strong>
              <small>${escapeHtml(item.hint)}</small>
            </article>
          `).join('')}
        </div>
      </section>
    `
  )

  const vehiclePages = vehicleChunks.map((chunk, index) => buildPrintPage(
    index + 3,
    totalPages,
    `
      <section class="print-section">
        <div class="print-section__head">
          <div class="print-section__title">车辆明细（第 ${index + 1} 组）</div>
          <div class="print-section__desc">按车辆展示半年使用频次、里程、油费与最近任务。</div>
        </div>
        <table class="print-table">
          <thead>
            <tr>
              <th>车牌号</th>
              <th>车型 / 状态</th>
              <th>当前里程</th>
              <th>半年出车</th>
              <th>半年里程</th>
              <th>加油量 / 金额</th>
              <th>最近任务</th>
            </tr>
          </thead>
          <tbody>
            ${chunk.map(item => `
              <tr>
                <td>${escapeHtml(item.plateNumber || '-')}</td>
                <td>${escapeHtml(item.model || '-')}<br/><span class="muted">${escapeHtml(formatVehicleStatus(item.status))}</span></td>
                <td>${escapeHtml(formatNumber(item.currentMileage, 1))} km</td>
                <td>${item.tripCount || 0}</td>
                <td>${escapeHtml(formatNumber(item.totalMileage, 1))} km</td>
                <td>${escapeHtml(formatNumber(item.fuelVolume, 1))} L<br/><span class="muted">${escapeHtml(formatCurrency(item.fuelCost))}</span></td>
                <td>${escapeHtml(item.latestDriver || '-')}<br/><span class="muted">${escapeHtml(item.latestDestination || '-')}</span></td>
              </tr>
            `).join('')}
          </tbody>
        </table>
      </section>
    `
  ))

  const insightPage = buildPrintPage(
    totalPages,
    totalPages,
    `
      <section class="print-section">
        <div class="print-section__head">
          <div class="print-section__title">报告摘要与建议</div>
          <div class="print-section__desc">这一页保留最适合在正式汇报中收尾的结论与行动建议。</div>
        </div>

        <div class="print-insight-wrap">
          <ul class="print-insight-list">
            ${(report.value?.insights || []).map(item => `<li>${escapeHtml(item)}</li>`).join('')}
          </ul>
          <div class="print-note">
            本报告由系统根据借还车记录、加油记录和车辆台账自动汇总。建议导出时同步附上责任部门、闭环时点与后续改进计划。
          </div>
        </div>
      </section>
    `
  )

  return `
    <!doctype html>
    <html lang="zh-CN">
      <head>
        <meta charset="UTF-8" />
        <title>半年用车报告</title>
        <style>
          * { box-sizing: border-box; }
          html, body { margin: 0; padding: 0; font-family: "PingFang SC", "Microsoft YaHei", sans-serif; color: #14283d; background: #eef3f7; }
          body { padding: 10mm; }
          @page { size: A4 landscape; margin: 10mm; }
          .print-page { position: relative; min-height: 178mm; padding: 10mm 11mm 13mm; background: #ffffff; border-radius: 7mm; box-shadow: 0 4mm 12mm rgba(15, 23, 42, 0.10); }
          .print-page + .print-page { page-break-before: always; margin-top: 8mm; }
          .print-page__head { display: flex; align-items: center; justify-content: space-between; gap: 8mm; padding-bottom: 4mm; border-bottom: 0.4mm solid #d8e1e8; }
          .print-page__title { font-size: 5.6mm; font-weight: 800; color: #10263f; }
          .print-page__meta { font-size: 3.1mm; color: #6a7c8f; }
          .print-page__foot { position: absolute; left: 11mm; right: 11mm; bottom: 5mm; display: flex; justify-content: space-between; font-size: 2.9mm; color: #708094; }
          .print-cover { display: grid; grid-template-columns: 1.45fr 0.55fr; gap: 6mm; align-items: stretch; margin-top: 4mm; }
          .print-cover__main { padding: 5mm; border-radius: 5mm; background: linear-gradient(135deg, #153150 0%, #1f577e 100%); color: #ffffff; }
          .print-cover__eyebrow { font-size: 3mm; letter-spacing: 0.18em; opacity: 0.74; text-transform: uppercase; }
          .print-cover__title { margin-top: 3mm; font-size: 8.4mm; font-weight: 800; line-height: 1.2; }
          .print-cover__meta { margin-top: 3mm; font-size: 3.1mm; line-height: 1.8; color: rgba(239, 245, 249, 0.9); }
          .print-cover__focus { padding: 5mm; border-radius: 5mm; background: linear-gradient(180deg, #f6fafc 0%, #edf3f7 100%); border: 0.4mm solid #d8e1e8; text-align: left; }
          .print-cover__focus span { display: block; font-size: 3mm; color: #65778b; }
          .print-cover__focus strong { display: block; margin-top: 4mm; font-size: 10mm; line-height: 1; color: #10263f; }
          .print-cover__focus small { display: block; margin-top: 2mm; font-size: 3mm; color: #6e7e90; }
          .print-callouts { display: grid; gap: 2.5mm; margin-top: 4mm; }
          .print-callout { padding: 3mm 3.2mm; border-radius: 3.5mm; background: rgba(255, 255, 255, 0.12); font-size: 3.2mm; line-height: 1.75; }
          .print-grid { display: grid; gap: 3mm; margin-top: 4mm; }
          .print-grid--metrics { grid-template-columns: repeat(4, minmax(0, 1fr)); }
          .print-metric-card { padding: 4mm; border-radius: 4mm; background: #f8fbfd; border: 0.4mm solid #dbe5ec; }
          .print-metric-card--fuel { background: #f7faf4; }
          .print-metric-card span { display: block; font-size: 2.9mm; color: #66788d; }
          .print-metric-card strong { display: block; margin-top: 2.5mm; font-size: 6.4mm; color: #10263f; line-height: 1.15; }
          .print-metric-card small { display: block; margin-top: 2mm; font-size: 2.8mm; color: #7b8b9d; line-height: 1.6; }
          .print-focus-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 3mm; margin-top: 4mm; }
          .print-focus-card { padding: 3.6mm; border-radius: 4mm; border: 0.4mm solid #dbe5ec; background: #ffffff; }
          .print-focus-card span { display: block; font-size: 2.9mm; color: #66788d; }
          .print-focus-card strong { display: block; margin-top: 2.2mm; font-size: 5.8mm; color: #10263f; }
          .print-focus-card small { display: block; margin-top: 1.8mm; font-size: 2.7mm; color: #7b8b9d; line-height: 1.55; }
          .print-section { margin-top: 4mm; }
          .print-section__head { display: flex; align-items: flex-start; justify-content: space-between; gap: 5mm; margin-bottom: 3mm; }
          .print-section__title { font-size: 5mm; font-weight: 800; color: #10263f; }
          .print-section__desc { max-width: 98mm; font-size: 2.9mm; line-height: 1.7; color: #66778a; }
          .print-chart-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 3mm; }
          .print-chart-card { padding: 3.4mm; border-radius: 4mm; background: #f8fbfd; border: 0.4mm solid #dbe5ec; }
          .print-chart-card__head strong { display: block; font-size: 3.5mm; color: #10263f; }
          .print-chart-card__head span { display: block; margin-top: 1.1mm; font-size: 2.7mm; line-height: 1.6; color: #75869a; }
          .print-chart-card__body { display: flex; align-items: center; justify-content: center; margin-top: 2.4mm; height: 38mm; border-radius: 3mm; background: #ffffff; border: 0.35mm solid #e3ebf2; overflow: hidden; }
          .print-chart-card__body img { width: 100%; height: 100%; object-fit: contain; }
          .print-chart-card__empty { font-size: 2.8mm; color: #8d9bad; }
          .print-highlight-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 3mm; margin-top: 4mm; }
          .print-highlight-card { padding: 3.4mm; border-radius: 4mm; border: 0.4mm solid #dbe5ec; background: #ffffff; }
          .print-highlight-card span { display: block; font-size: 2.7mm; color: #66788d; }
          .print-highlight-card strong { display: block; margin-top: 1.6mm; font-size: 4.2mm; color: #10263f; line-height: 1.25; }
          .print-highlight-card small { display: block; margin-top: 1.4mm; font-size: 2.6mm; line-height: 1.5; color: #78889c; }
          .print-trend-cards { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 3mm; }
          .print-trend-card { padding: 3.8mm; border-radius: 4mm; background: #f8fbfd; border: 0.4mm solid #dbe5ec; }
          .print-trend-card__head { display: flex; justify-content: space-between; gap: 2mm; font-size: 3mm; color: #637488; }
          .print-trend-card__head strong { font-size: 3.5mm; color: #10263f; }
          .print-bar-row { display: grid; grid-template-columns: 10mm 1fr auto; gap: 2mm; align-items: center; margin-top: 2.6mm; font-size: 2.8mm; color: #64768a; }
          .print-bar { height: 2.5mm; border-radius: 999px; background: #e7eef5; overflow: hidden; }
          .print-bar i { display: block; height: 100%; border-radius: inherit; background: linear-gradient(90deg, #2459b0 0%, #4d89df 100%); }
          .print-bar--warm i { background: linear-gradient(90deg, #b76a15 0%, #db9a38 100%); }
          .print-trend-card__meta { margin-top: 2.6mm; font-size: 2.7mm; color: #728296; }
          .print-aside-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 3mm; margin-top: 4mm; }
          .print-mini-panel { padding: 4mm; border-radius: 4mm; border: 0.4mm solid #dbe5ec; background: #ffffff; }
          .print-mini-panel__title { font-size: 4.1mm; font-weight: 800; color: #10263f; }
          .print-list { margin: 3mm 0 0; padding-left: 4.8mm; font-size: 2.9mm; line-height: 1.85; color: #314356; }
          .print-list li + li { margin-top: 1.3mm; }
          .print-table { width: 100%; border-collapse: collapse; margin-top: 3mm; font-size: 2.8mm; }
          .print-table th, .print-table td { padding: 2.3mm 2mm; text-align: left; border-bottom: 0.35mm solid #e4ebf1; vertical-align: top; }
          .print-table thead th { font-weight: 700; color: #516478; background: #f6fafc; }
          .muted { color: #7a8b9d; }
          .print-insight-wrap { display: grid; gap: 4mm; margin-top: 2mm; }
          .print-insight-list { margin: 0; padding-left: 4.8mm; font-size: 3.1mm; line-height: 1.9; color: #314356; }
          .print-insight-list li + li { margin-top: 1.6mm; }
          .print-note { padding: 4mm; border-radius: 4mm; background: #f8fbfd; border: 0.4mm solid #dbe5ec; font-size: 2.9mm; line-height: 1.8; color: #607085; }
        </style>
      </head>
      <body>
        ${coverPage}
        ${trendPage}
        ${vehiclePages.join('')}
        ${insightPage}
      </body>
    </html>
  `
}

const buildMetricCardHtml = (card, isFuel = false) => `
  <article class="print-metric-card ${isFuel ? 'print-metric-card--fuel' : ''}">
    <span>${escapeHtml(card.label)}</span>
    <strong>${escapeHtml(card.value)}</strong>
    <small>${escapeHtml(card.hint)}</small>
  </article>
`

const buildPrintChartCard = (title, desc, imageUrl) => `
  <article class="print-chart-card">
    <div class="print-chart-card__head">
      <strong>${escapeHtml(title)}</strong>
      <span>${escapeHtml(desc)}</span>
    </div>
    <div class="print-chart-card__body">
      ${imageUrl
        ? `<img src="${imageUrl}" alt="${escapeHtml(title)}" />`
        : `<div class="print-chart-card__empty">暂无图表数据</div>`}
    </div>
  </article>
`

const buildPrintPage = (pageNumber, totalPages, bodyHtml) => `
  <section class="print-page">
    <div class="print-page__head">
      <div>
        <div class="print-page__title">公务车辆半年汇报</div>
        <div class="print-page__meta">${escapeHtml(report.value?.period?.label || '-')} ｜ ${escapeHtml(report.value?.generatedAt || '-')}</div>
      </div>
      <div class="print-page__meta">第 ${pageNumber} / ${totalPages} 页</div>
    </div>
    ${bodyHtml}
    <div class="print-page__foot">
      <span>公务车辆管理系统</span>
      <span>内部汇报使用</span>
    </div>
  </section>
`

const wait = (ms) => new Promise(resolve => window.setTimeout(resolve, ms))

const pickPeakMonth = (field) => {
  const list = report.value?.monthlyTrend || []
  if (!list.length) {
    return null
  }
  return [...list].sort((left, right) => {
    const diff = toNumber(right[field]) - toNumber(left[field])
    if (diff !== 0) {
      return diff
    }
    return String(right.month || '').localeCompare(String(left.month || ''))
  })[0]
}

const disposeCharts = () => {
  Object.keys(chartInstances).forEach((key) => {
    chartInstances[key]?.dispose()
    delete chartInstances[key]
  })
}

const resizeCharts = () => {
  Object.values(chartInstances).forEach(chart => chart?.resize())
}

const ensureChart = (key, element) => {
  if (!element) {
    return null
  }
  const existed = chartInstances[key]
  if (existed) {
    return existed
  }
  const chart = echarts.init(element, null, { renderer: 'canvas' })
  chartInstances[key] = chart
  return chart
}

const buildEmptyChartOption = (text) => ({
  title: {
    text,
    left: 'center',
    top: 'middle',
    textStyle: {
      color: '#94a3b8',
      fontSize: 14,
      fontWeight: 500
    }
  },
  xAxis: { show: false },
  yAxis: { show: false },
  series: []
})

const getChartTextStyle = () => ({
  color: '#66788d',
  fontSize: 12
})

const renderAllCharts = () => {
  renderOperationTrendChart()
  renderFuelTrendChart()
  renderDriverRankChart()
  renderDestinationRankChart()
}

const renderOperationTrendChart = () => {
  const chart = ensureChart('operation', operationTrendChartRef.value)
  const list = report.value?.monthlyTrend || []
  if (!chart) {
    return
  }
  if (!list.length) {
    chart.setOption(buildEmptyChartOption('暂无月度出车趋势数据'), true)
    return
  }

  chart.setOption({
    color: ['#2f6fb8', '#1f8a70'],
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(15, 23, 42, 0.92)',
      borderWidth: 0,
      textStyle: { color: '#ffffff' },
      axisPointer: { type: 'shadow' },
      formatter: (params) => {
        const [trip, mileage] = params
        return [
          `${trip.axisValue}`,
          `出车次数：${trip?.data ?? 0} 次`,
          `行驶里程：${formatNumber(mileage?.data, 1)} km`
        ].join('<br/>')
      }
    },
    legend: {
      top: 0,
      icon: 'roundRect',
      textStyle: getChartTextStyle()
    },
    grid: {
      left: 56,
      right: 48,
      top: 42,
      bottom: 34
    },
    xAxis: {
      type: 'category',
      data: list.map(item => formatMonthLabel(item.month)),
      axisLine: { lineStyle: { color: '#d7e3ee' } },
      axisLabel: getChartTextStyle(),
      axisTick: { show: false }
    },
    yAxis: [
      {
        type: 'value',
        name: '次',
        nameTextStyle: getChartTextStyle(),
        axisLabel: getChartTextStyle(),
        splitLine: { lineStyle: { color: '#edf2f7' } }
      },
      {
        type: 'value',
        name: 'km',
        nameTextStyle: getChartTextStyle(),
        axisLabel: getChartTextStyle(),
        splitLine: { show: false }
      }
    ],
    series: [
      {
        name: '出车次数',
        type: 'bar',
        barWidth: 18,
        data: list.map(item => toNumber(item.tripCount)),
        itemStyle: {
          borderRadius: [8, 8, 0, 0]
        }
      },
      {
        name: '行驶里程',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        symbolSize: 8,
        data: list.map(item => toNumber(item.mileage)),
        lineStyle: {
          width: 3
        },
        areaStyle: {
          color: 'rgba(31, 138, 112, 0.14)'
        }
      }
    ]
  }, true)
}

const renderFuelTrendChart = () => {
  const chart = ensureChart('fuel', fuelTrendChartRef.value)
  const list = report.value?.monthlyTrend || []
  if (!chart) {
    return
  }
  if (!list.length) {
    chart.setOption(buildEmptyChartOption('暂无油费趋势数据'), true)
    return
  }

  chart.setOption({
    color: ['#c57a1d', '#2563eb'],
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(15, 23, 42, 0.92)',
      borderWidth: 0,
      textStyle: { color: '#ffffff' },
      axisPointer: { type: 'shadow' },
      formatter: (params) => {
        const [fuelCost, fuelVolume] = params
        return [
          `${fuelCost.axisValue}`,
          `加油金额：${formatCurrency(fuelCost?.data)}`,
          `加油量：${formatNumber(fuelVolume?.data, 1)} L`
        ].join('<br/>')
      }
    },
    legend: {
      top: 0,
      icon: 'roundRect',
      textStyle: getChartTextStyle()
    },
    grid: {
      left: 56,
      right: 48,
      top: 42,
      bottom: 34
    },
    xAxis: {
      type: 'category',
      data: list.map(item => formatMonthLabel(item.month)),
      axisLine: { lineStyle: { color: '#d7e3ee' } },
      axisLabel: getChartTextStyle(),
      axisTick: { show: false }
    },
    yAxis: [
      {
        type: 'value',
        name: '元',
        nameTextStyle: getChartTextStyle(),
        axisLabel: {
          ...getChartTextStyle(),
          formatter: value => Number(value).toFixed(0)
        },
        splitLine: { lineStyle: { color: '#edf2f7' } }
      },
      {
        type: 'value',
        name: 'L',
        nameTextStyle: getChartTextStyle(),
        axisLabel: getChartTextStyle(),
        splitLine: { show: false }
      }
    ],
    series: [
      {
        name: '加油金额',
        type: 'bar',
        barWidth: 18,
        data: list.map(item => toNumber(item.fuelCost)),
        itemStyle: {
          color: '#d5963e',
          borderRadius: [8, 8, 0, 0]
        }
      },
      {
        name: '加油量',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        symbolSize: 8,
        data: list.map(item => toNumber(item.fuelVolume)),
        lineStyle: {
          width: 3
        },
        areaStyle: {
          color: 'rgba(37, 99, 235, 0.12)'
        }
      }
    ]
  }, true)
}

const renderDriverRankChart = () => {
  const chart = ensureChart('driver', driverRankChartRef.value)
  const list = (report.value?.topDrivers || []).slice(0, 5)
  if (!chart) {
    return
  }
  if (!list.length) {
    chart.setOption(buildEmptyChartOption('暂无驾驶员排行数据'), true)
    return
  }

  chart.setOption({
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(15, 23, 42, 0.92)',
      borderWidth: 0,
      textStyle: { color: '#ffffff' },
      formatter: (params) => [
        `${params.name}`,
        `累计里程：${formatNumber(params.value, 1)} km`,
        `用车次数：${params.data.tripCount || 0} 次`
      ].join('<br/>')
    },
    grid: {
      left: 82,
      right: 26,
      top: 12,
      bottom: 20
    },
    xAxis: {
      type: 'value',
      axisLabel: getChartTextStyle(),
      splitLine: { lineStyle: { color: '#edf2f7' } }
    },
    yAxis: {
      type: 'category',
      inverse: true,
      data: list.map(item => item.driverName),
      axisLabel: getChartTextStyle(),
      axisLine: { show: false },
      axisTick: { show: false }
    },
    series: [
      {
        type: 'bar',
        barWidth: 16,
        data: list.map(item => ({
          value: toNumber(item.mileage),
          tripCount: item.tripCount || 0,
          name: item.driverName
        })),
        label: {
          show: true,
          position: 'right',
          color: '#4b5d72',
          formatter: params => `${formatNumber(params.value, 1)} km`
        },
        itemStyle: {
          color: '#1f8a70',
          borderRadius: [0, 8, 8, 0]
        }
      }
    ]
  }, true)
}

const renderDestinationRankChart = () => {
  const chart = ensureChart('destination', destinationRankChartRef.value)
  const list = (report.value?.topDestinations || []).slice(0, 5)
  if (!chart) {
    return
  }
  if (!list.length) {
    chart.setOption(buildEmptyChartOption('暂无去向排行数据'), true)
    return
  }

  chart.setOption({
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(15, 23, 42, 0.92)',
      borderWidth: 0,
      textStyle: { color: '#ffffff' },
      formatter: (params) => `${params.name}<br/>出现次数：${params.value || 0} 次`
    },
    grid: {
      left: 102,
      right: 26,
      top: 12,
      bottom: 20
    },
    xAxis: {
      type: 'value',
      axisLabel: getChartTextStyle(),
      splitLine: { lineStyle: { color: '#edf2f7' } }
    },
    yAxis: {
      type: 'category',
      inverse: true,
      data: list.map(item => item.destination),
      axisLabel: {
        ...getChartTextStyle(),
        width: 84,
        overflow: 'truncate'
      },
      axisLine: { show: false },
      axisTick: { show: false }
    },
    series: [
      {
        type: 'bar',
        barWidth: 16,
        data: list.map(item => ({
          value: toNumber(item.count),
          name: item.destination
        })),
        label: {
          show: true,
          position: 'right',
          color: '#4b5d72',
          formatter: params => `${params.value || 0} 次`
        },
        itemStyle: {
          color: '#2f6fb8',
          borderRadius: [0, 8, 8, 0]
        }
      }
    ]
  }, true)
}

const collectChartImages = () => ({
  operation: getChartImage('operation'),
  fuel: getChartImage('fuel'),
  driver: getChartImage('driver'),
  destination: getChartImage('destination')
})

const getChartImage = (key) => {
  const chart = chartInstances[key]
  if (!chart) {
    return ''
  }
  return chart.getDataURL({
    type: 'png',
    pixelRatio: 2,
    backgroundColor: '#ffffff'
  })
}

const chunkArray = (list, size) => {
  const result = []
  for (let index = 0; index < list.length; index += size) {
    result.push(list.slice(index, index + size))
  }
  return result
}

const getMaxValue = (list, getter) => {
  if (!list.length) {
    return 1
  }
  const max = Math.max(...list.map(item => getter(item)))
  return max > 0 ? max : 1
}

const buildBarWidth = (value, max) => {
  const current = toNumber(value)
  if (current <= 0 || max <= 0) {
    return '0%'
  }
  return `${Math.max(12, Math.round((current / max) * 100))}%`
}

const calculateFuelCostPerHundredKm = (fuelCost, mileage) => {
  if (!mileage) {
    return 0
  }
  return (fuelCost / mileage) * 100
}

const calculateHundredKmFuelVolume = (fuelVolume, mileage) => {
  if (!mileage) {
    return 0
  }
  return (fuelVolume / mileage) * 100
}

const formatCurrency = (value) => `${formatCurrencyNumber(value)} 元`

const formatCurrencyNumber = (value) => {
  const number = toNumber(value)
  return number.toFixed(2)
}

const formatNumber = (value, digits = 0) => {
  const number = toNumber(value)
  return number.toFixed(digits)
}

const formatPercent = (value, digits = 1) => `${(toNumber(value) * 100).toFixed(digits)}%`

const toNumber = (value) => {
  const number = Number(value || 0)
  return Number.isFinite(number) ? number : 0
}

const formatVehicleStatus = (status) => {
  const map = {
    NORMAL: '正常',
    IN_USE: '使用中',
    MAINTENANCE: '维修中',
    PENDING_CHECK: '待复核',
    SCRAPPED: '停用'
  }
  return map[status] || status || '-'
}

const formatMonthLabel = (value) => {
  if (!value) {
    return '-'
  }
  const [year, month] = String(value).split('-')
  if (!year || !month) {
    return String(value)
  }
  return `${year}.${month}`
}

const escapeHtml = (value) => String(value ?? '')
  .replaceAll('&', '&amp;')
  .replaceAll('<', '&lt;')
  .replaceAll('>', '&gt;')
  .replaceAll('"', '&quot;')
  .replaceAll("'", '&#39;')

onMounted(() => {
  window.addEventListener('resize', resizeCharts)
  loadReport()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  disposeCharts()
})
</script>

<style scoped>
.half-year-report {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.report-hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  padding: 28px 30px;
  border-radius: 28px;
  background:
    radial-gradient(circle at top right, rgba(21, 128, 61, 0.12), transparent 28%),
    radial-gradient(circle at left bottom, rgba(37, 99, 235, 0.12), transparent 30%),
    linear-gradient(135deg, #f7fbff 0%, #eef6fb 100%);
  border: 1px solid rgba(207, 221, 232, 0.95);
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.05);
}

.report-hero__copy {
  max-width: 760px;
}

.report-hero__eyebrow {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.18em;
  color: #55718b;
  text-transform: uppercase;
}

.report-hero__title {
  margin: 12px 0 0;
  font-size: 34px;
  line-height: 1.2;
  color: #10263f;
}

.report-hero__desc {
  margin: 12px 0 0;
  max-width: 700px;
  font-size: 14px;
  line-height: 1.8;
  color: #5e7188;
}

.report-hero__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 18px;
}

.hero-chip {
  display: inline-flex;
  align-items: center;
  padding: 7px 14px;
  border-radius: 999px;
  background: #17324f;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
}

.hero-chip--light {
  background: rgba(255, 255, 255, 0.9);
  color: #28435d;
  border: 1px solid rgba(191, 207, 220, 0.9);
}

.report-hero__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: flex-end;
}

.report-hero__picker {
  width: 180px;
}

.report-content,
.report-sheet {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.sheet-cover {
  display: grid;
  grid-template-columns: 1.45fr 0.55fr;
  gap: 18px;
  padding: 28px;
  border-radius: 28px;
  background: linear-gradient(135deg, #17324f 0%, #20567c 100%);
  color: #fff;
  box-shadow: 0 18px 36px rgba(17, 38, 63, 0.16);
}

.sheet-cover__eyebrow {
  font-size: 12px;
  letter-spacing: 0.18em;
  color: rgba(230, 240, 248, 0.72);
  text-transform: uppercase;
}

.sheet-cover__title {
  margin-top: 12px;
  font-size: 34px;
  font-weight: 800;
  line-height: 1.2;
}

.sheet-cover__meta {
  margin-top: 12px;
  font-size: 13px;
  line-height: 1.8;
  color: rgba(233, 241, 247, 0.82);
}

.sheet-cover__summary {
  display: grid;
  gap: 10px;
  margin-top: 18px;
}

.summary-callout {
  padding: 14px 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.1);
  font-size: 13px;
  line-height: 1.8;
}

.sheet-cover__focus {
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-height: 220px;
  padding: 24px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(12px);
}

.sheet-cover__focus-label {
  font-size: 12px;
  color: rgba(244, 248, 251, 0.72);
}

.sheet-cover__focus-value {
  margin-top: 18px;
  font-size: 52px;
  line-height: 1;
  font-weight: 800;
}

.sheet-cover__focus-unit,
.sheet-cover__focus-caption {
  margin-top: 10px;
  font-size: 13px;
  color: rgba(244, 248, 251, 0.78);
}

.report-grid {
  display: grid;
  gap: 14px;
}

.report-grid--metrics {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.metric-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 136px;
  padding: 20px;
  border-radius: 22px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbfd 100%);
  border: 1px solid rgba(214, 226, 236, 0.95);
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.04);
}

.metric-card--fuel {
  background: linear-gradient(180deg, #fdfefb 0%, #f7faf4 100%);
}

.metric-card__label {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.06em;
  color: #617488;
}

.metric-card__value {
  font-size: 28px;
  line-height: 1.15;
  color: #112941;
}

.metric-card__hint {
  font-size: 13px;
  line-height: 1.6;
  color: #7a8b9d;
}

.chart-matrix {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-top: 16px;
}

.chart-card {
  padding: 18px;
  border-radius: 22px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbfd 100%);
  border: 1px solid rgba(214, 226, 236, 0.95);
}

.chart-card__head {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.chart-card__head strong {
  font-size: 16px;
  color: #10263f;
}

.chart-card__head span {
  font-size: 12px;
  line-height: 1.7;
  color: #748599;
}

.chart-surface {
  height: 240px;
  margin-top: 14px;
}

.chart-surface--lg {
  height: 280px;
}

.briefing-grid {
  display: grid;
  grid-template-columns: 1.05fr 0.95fr;
  gap: 18px;
}

.report-panels {
  display: grid;
  grid-template-columns: 1.35fr 0.85fr;
  gap: 18px;
}

.report-panel {
  padding: 22px 24px;
  border-radius: 24px;
  background: #fff;
  border: 1px solid rgba(214, 226, 236, 0.95);
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.04);
}

.report-panel--focus,
.report-panel--aside {
  display: flex;
  flex-direction: column;
}

.report-panel__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.report-panel__title {
  font-size: 18px;
  font-weight: 800;
  color: #10263f;
}

.report-panel__desc {
  margin-top: 6px;
  color: #68798c;
  font-size: 13px;
  line-height: 1.7;
}

.summary-list {
  display: grid;
  gap: 12px;
  margin-top: 16px;
}

.summary-list__item {
  padding: 14px 16px;
  border-radius: 18px;
  background: #f8fbfd;
  border: 1px solid rgba(218, 228, 237, 0.92);
  font-size: 13px;
  line-height: 1.85;
  color: #314356;
}

.focus-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 16px;
}

.focus-card {
  padding: 18px 16px;
  border-radius: 18px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbfd 100%);
  border: 1px solid rgba(218, 228, 237, 0.92);
}

.focus-card__label {
  display: block;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.04em;
  color: #66778a;
}

.focus-card__value {
  display: block;
  margin-top: 10px;
  font-size: 26px;
  line-height: 1.15;
  color: #112941;
}

.focus-card__hint {
  display: block;
  margin-top: 8px;
  font-size: 12px;
  line-height: 1.6;
  color: #7a8b9d;
}

.highlight-list {
  display: grid;
  gap: 12px;
  margin-top: 16px;
}

.highlight-card {
  padding: 16px;
  border-radius: 18px;
  background: #f8fbfd;
  border: 1px solid rgba(218, 228, 237, 0.92);
}

.highlight-card__label {
  display: block;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.04em;
  color: #687a8d;
}

.highlight-card__value {
  display: block;
  margin-top: 10px;
  font-size: 18px;
  color: #10263f;
}

.highlight-card__hint {
  display: block;
  margin-top: 8px;
  font-size: 12px;
  line-height: 1.6;
  color: #7a8b9d;
}

.trend-cards {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 16px;
}

.trend-card {
  padding: 16px;
  border-radius: 18px;
  background: #f8fbfd;
  border: 1px solid rgba(218, 228, 237, 0.92);
}

.trend-card__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
}

.trend-card__head strong {
  font-size: 16px;
  color: #122a43;
}

.trend-card__head span {
  font-size: 12px;
  color: #6b7c90;
}

.trend-card__bar-row {
  display: grid;
  grid-template-columns: 34px 1fr auto;
  gap: 10px;
  align-items: center;
  margin-top: 12px;
}

.trend-card__bar-row span {
  font-size: 12px;
  color: #6a7b8f;
}

.trend-card__bar-row strong {
  font-size: 12px;
  color: #16324f;
}

.trend-card__bar {
  position: relative;
  height: 8px;
  border-radius: 999px;
  overflow: hidden;
  background: #e6edf5;
}

.trend-card__bar i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #2159b1 0%, #4f8be0 100%);
}

.trend-card__bar--warm i {
  background: linear-gradient(90deg, #be7617 0%, #dda141 100%);
}

.trend-card__meta {
  margin-top: 12px;
  font-size: 12px;
  color: #728296;
}

.trend-card__vehicles {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px dashed rgba(210, 221, 232, 0.95);
}

.trend-card__vehicles-label {
  font-size: 12px;
  font-weight: 700;
  color: #66778a;
}

.trend-vehicle-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-top: 10px;
}

.trend-vehicle {
  padding: 12px;
  border-radius: 16px;
  background: #ffffff;
  border: 1px solid rgba(220, 229, 237, 0.95);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.7);
}

.trend-vehicle strong {
  display: block;
  font-size: 13px;
  color: #14304f;
}

.trend-vehicle span,
.trend-vehicle small {
  display: block;
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.6;
  color: #708196;
}

.trend-card__more,
.trend-card__empty {
  margin-top: 10px;
  font-size: 12px;
  color: #7d8da0;
}

.mini-panel + .mini-panel {
  margin-top: 10px;
  padding-top: 16px;
  border-top: 1px solid rgba(221, 229, 237, 0.9);
}

.rank-list,
.destination-list,
.vehicle-spotlight-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 16px;
}

.rank-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 14px;
  border-radius: 18px;
  background: #f7fafc;
  border: 1px solid rgba(222, 230, 237, 0.94);
}

.rank-item__index {
  width: 30px;
  height: 30px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #17324f;
  color: #fff;
  font-weight: 700;
}

.rank-item__copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.rank-item__copy strong {
  color: #17304f;
}

.rank-item__copy small {
  color: #73859a;
}

.destination-pill {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 16px;
  background: #f5f9f6;
  border: 1px solid rgba(219, 231, 222, 0.94);
}

.destination-pill span {
  color: #17304f;
}

.destination-pill strong {
  color: #1a7a4b;
}

.vehicle-spotlight {
  padding: 14px 16px;
  border-radius: 16px;
  background: #f7fafc;
  border: 1px solid rgba(222, 230, 237, 0.94);
}

.vehicle-spotlight__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.vehicle-spotlight__top strong {
  color: #16324f;
}

.vehicle-spotlight__top span,
.vehicle-spotlight__line {
  font-size: 12px;
  color: #728296;
}

.vehicle-spotlight__line + .vehicle-spotlight__line {
  margin-top: 6px;
}

.report-table-wrapper {
  overflow-x: auto;
}

.report-table {
  width: 100%;
  margin-top: 16px;
  border-collapse: collapse;
  font-size: 13px;
}

.report-table th,
.report-table td {
  padding: 12px 10px;
  text-align: left;
  border-bottom: 1px solid rgba(227, 234, 240, 0.9);
  vertical-align: top;
}

.report-table thead th {
  color: #56697e;
  font-weight: 700;
  background: #f6fafc;
}

.table-plate {
  font-weight: 800;
  color: #10263f;
}

.table-stack {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.table-stack strong {
  color: #17304f;
  font-size: 13px;
}

.table-stack span {
  color: #73859a;
  font-size: 12px;
  line-height: 1.6;
}

.insight-section {
  display: grid;
  gap: 14px;
  margin-top: 16px;
}

.insight-list {
  margin: 0;
  padding-left: 20px;
  color: #314356;
  line-height: 1.9;
}

.insight-list li + li {
  margin-top: 8px;
}

.report-note {
  padding: 16px 18px;
  border-radius: 18px;
  background: #f8fbfd;
  border: 1px solid rgba(218, 228, 237, 0.92);
  color: #607085;
  font-size: 13px;
  line-height: 1.8;
}

@media (max-width: 1320px) {
  .briefing-grid,
  .report-panels {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 1180px) {
  .report-grid--metrics,
  .focus-grid,
  .trend-cards {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .chart-matrix {
    grid-template-columns: 1fr;
  }

  .trend-vehicle-list {
    grid-template-columns: 1fr;
  }

  .sheet-cover {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .report-hero {
    flex-direction: column;
    padding: 22px 20px;
  }

  .sheet-cover,
  .report-panel {
    padding: 18px;
  }

  .report-grid--metrics,
  .focus-grid,
  .trend-cards {
    grid-template-columns: 1fr;
  }

  .chart-surface,
  .chart-surface--lg {
    height: 240px;
  }

  .sheet-cover__title,
  .report-hero__title {
    font-size: 26px;
  }
}
</style>
