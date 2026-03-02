<template>
  <AdminLayout>
    <PageBreadcrumb pageTitle="매출현황" />

    <div class="grid grid-cols-12 gap-4 items-stretch min-w-0">
      <div class="col-span-12 space-y-4 min-w-0">
        <div class="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03]">
          <div class="px-6 py-5 space-y-4">
            <!-- 보기 버튼 + 퇴사자 포함 -->
            <div class="flex flex-wrap items-center justify-between gap-3">
              <!-- 왼쪽: 보기 모드 -->
              <div class="flex flex-wrap items-center gap-2">
                <button :class="modeBtnClass(viewMode === 'BRANCH')" @click="viewMode = 'BRANCH'">
                  <BuildingStorefrontIcon class="h-4 w-4" />
                  <span>지점별 보기</span>
                </button>

                <button :class="modeBtnClass(viewMode === 'TEAM')" @click="viewMode = 'TEAM'">
                  <UsersIcon class="h-4 w-4" />
                  <span>팀별 보기</span>
                </button>

                <button :class="modeBtnClass(viewMode === 'PERSON')" @click="viewMode = 'PERSON'">
                  <UserIcon class="h-4 w-4" />
                  <span>개인별 보기</span>
                </button>
              </div>

              <!-- 오른쪽: 옵션 버튼 -->
              <div class="flex items-center gap-2">
                <button
                    class="inline-flex h-11 items-center gap-2 rounded-lg border border-gray-200 px-4 text-sm font-medium text-gray-600
             hover:bg-gray-50 dark:border-gray-700 dark:text-gray-200 dark:hover:bg-gray-800"
                    @click="includeResigned = !includeResigned"
                >
                  퇴사자 포함
                  <span class="text-gray-400">{{ includeResigned ? 'ON' : 'OFF' }}</span>
                </button>

                <button
                    class="inline-flex h-11 items-center gap-2 rounded-lg border border-gray-200 px-4 text-sm font-medium text-gray-600
             hover:bg-gray-50 dark:border-gray-700 dark:text-gray-200 dark:hover:bg-gray-800"
                >
                  스냅샷 촬영
                </button>
              </div>
            </div>

            <hr />

            <!-- 섹션 3개 -->
            <div v-for="sec in sections" :key="sec.key" class="space-y-3">
              <div class="mt-5 text-sm font-semibold text-gray-700 dark:text-gray-200">
                {{ sec.title }}
              </div>

              <div class="grid grid-cols-12 gap-4 min-w-0">
                <!-- 차트 카드 -->
                <div class="col-span-12 lg:col-span-8 min-w-0">
                  <div class="rounded-2xl border border-gray-200 p-4 h-full dark:border-gray-800">
                    <div data-chart-bounds class="relative overflow-hidden rounded-xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-gray-900/30">
                      <div class="relative p-4 min-w-0">
                        <!-- 스크롤 영역 -->
                        <div
                            class="relative h-[260px] overflow-y-auto pr-2"
                            style="scrollbar-gutter: stable;"
                        >
                          <!-- rows -->
                          <div class="space-y-5 pb-5">
                            <div
                                v-for="row in secRowsSorted(sec.key)"
                                :key="sec.key + '-' + row.key"
                                class="grid items-center gap-3 min-w-0"
                                :style="{ gridTemplateColumns: `${labelColWidth}px 1fr` }"
                            >
                              <!-- 라벨 -->
                              <div
                                  class="shrink-0 truncate text-sm text-gray-700 dark:text-gray-200"
                                  :title="row.label"
                              >
                                {{ shortLabel(row.label) }}
                              </div>

                              <!-- 바 영역 -->
                              <div
                                  class="relative min-w-0"
                                  @pointerdown.stop.prevent="onBarPointerDown($event, row)"
                                  @click.stop="onBarClick($event, row)"
                              >
                                <!-- 트랙 -->
                                <div
                                    class="relative w-full overflow-hidden rounded-md border border-gray-200 bg-gray-100
                                         dark:border-gray-700 dark:bg-gray-800"
                                >
                                  <!-- 보조선: 트랙 내부 레이어 (배경 위, 색막대 아래) -->
                                  <div class="pointer-events-none absolute inset-[1px] z-10" aria-hidden="true">
                                    <div
                                        v-for="tick in ticksOf(sec.key)"
                                        :key="sec.key + '-' + row.key + '-guide-' + tick"
                                        class="absolute top-0 bottom-0 bg-gray-200 dark:bg-gray-700"
                                        :style="{ left: tickInnerLeftPx(sec.key, tick) + 'px', width: '1px', transform: 'translateZ(0)' }"
                                    />
                                  </div>

                                  <!-- 막대는 보조선 위 -->
                                  <div class="relative z-20 flex flex-col">
<!--                                    &lt;!&ndash; 위: PartitionBar &ndash;&gt;-->
<!--                                    <div class="h-4 w-full">-->
<!--                                      <div class="flex h-full">-->
<!--                                        <div-->
<!--                                            v-for="seg in partitionSegs(row.total)"-->
<!--                                            :key="sec.key + '-' + row.key + '-p-' + seg.from"-->
<!--                                            class="h-full"-->
<!--                                            :style="{ width: pctX(seg.value) + '%', backgroundColor: seg.color }"-->
<!--                                        />-->
<!--                                        <div class="flex-1" />-->
<!--                                      </div>-->
<!--                                    </div>-->
                                    <!-- 위: PartitionBar (단색 버킷) -->
                                    <div class="h-4 w-full">
                                      <div class="flex h-full">
                                        <div
                                            class="h-full"
                                            :style="{
                                              width: pctX(sec.key, row.total) + '%',
                                              backgroundColor: bucketColor(row.total),
                                            }"
                                        />
                                        <div class="flex-1" />
                                      </div>
                                    </div>

                                    <!-- 아래: ABStackBar -->
                                    <div class="h-4 w-full">
                                      <div class="flex h-full">
                                        <!-- A(파란색) -->
                                        <div
                                            class="relative h-full overflow-hidden"
                                            :style="{ width: pctX(sec.key, row.a) + '%', backgroundColor: '#3B82F6' }"
                                        >
                                          <div
                                              v-if="showBluePct(sec.key, row)"
                                              class="absolute inset-0 flex items-center justify-center text-[10px] font-semibold text-white/90"
                                          >
                                            {{ percent(row.a, row.total) }}
                                          </div>
                                        </div>

                                        <!-- B(초록색) = b+c -->
                                        <div
                                            class="relative h-full overflow-hidden"
                                            :style="{ width: pctX(sec.key, Number(row.b||0)+Number(row.c||0)) + '%', backgroundColor: '#22C55E' }"
                                        >
                                          <div
                                              v-if="showGreenPct(sec.key, row)"
                                              class="absolute inset-0 flex items-center justify-center text-[10px] font-semibold text-white/90"
                                          >
                                            {{ percent(Number(row.b || 0) + Number(row.c || 0), row.total) }}
                                          </div>
                                        </div>

                                        <div class="flex-1" />
                                      </div>
                                    </div>
                                  </div>

                                  <!-- 총액 라벨 -->
                                  <div
                                      class="absolute top-1/2 z-30 -translate-y-1/2 text-xs font-semibold text-gray-700 dark:text-gray-200"
                                      :style="endLabelStyle(sec.key, row.total)"
                                  >
                                    {{ money(row.total) }}
                                  </div>
                                </div>
                              </div>
                            </div>
                          </div>

                          <!-- x축: sticky + 윗쪽 회색 선 -->
                          <div
                              class="sticky bottom-0 z-50 border-t border-gray-200 bg-white pt-2 backdrop-blur-sm
                                   dark:border-gray-800 dark:bg-gray-900"
                          >
                            <div
                                class="grid items-center gap-3"
                                :style="{ gridTemplateColumns: `${labelColWidth}px 1fr` }"
                            >
                              <div />
                              <div class="relative h-6" :ref="el => setAxisTrackEl(sec.key, el)">
                                <div
                                    v-for="tick in ticksOf(sec.key)"
                                    :key="sec.key + '-tickLabel-' + tick"
                                    class="absolute -translate-x-1/2 text-xs text-gray-400"
                                    :style="{ left: tickAxisLeftPx(sec.key, tick) + 'px' }"
                                >
                                  {{ tick }}
                                </div>
                              </div>
                            </div>
                          </div>
                        </div>
                        <!-- /스크롤 -->
                      </div>
                    </div>
                  </div>
                </div>

                <!-- 순위표 카드 -->
                <div class="col-span-12 lg:col-span-4 min-w-0">
                  <div class="rounded-2xl border border-gray-200 p-4 h-full flex flex-col min-h-0 dark:border-gray-800">
                    <div class="flex items-center justify-between gap-2">
                      <div class="text-sm font-semibold text-gray-700 dark:text-gray-200">
                        {{ rankTitle(sec.key) }}
                      </div>
                    </div>

                    <div class="mt-3 flex-1 min-h-0 overflow-hidden rounded-xl border border-gray-200 dark:border-gray-800">
                      <table class="w-full text-sm">
                        <thead class="bg-gray-50 dark:bg-gray-900/40">
                        <tr class="text-gray-600 dark:text-gray-200">
                          <th class="w-16 px-3 py-2 text-left">순위</th>
                          <th class="px-3 py-2 text-left">명칭</th>
                          <th class="px-3 py-2 text-right">누적매출액</th>
                        </tr>
                        </thead>

                        <tbody>
                        <tr
                            v-for="(r, idx) in secTopRanks(sec.key)"
                            :key="sec.key + '-rank-' + r.key"
                            :class="idx === 0 ? 'bg-sky-100/70 dark:bg-sky-900/25' : 'bg-white dark:bg-gray-900/10'"
                            class="border-t border-gray-200 dark:border-gray-800"
                        >
                          <td class="px-3 py-3 font-semibold text-gray-700 dark:text-gray-100">
                            {{ idx + 1 }}
                          </td>
                          <td class="px-3 py-3 text-gray-700 dark:text-gray-100">
                            {{ r.label }}
                            <span v-if="r.resigned" class="ml-2 text-xs text-gray-400">(퇴사)</span>
                          </td>
                          <td class="px-3 py-3 text-right font-semibold text-gray-900 dark:text-white">
                            {{ money(r.total) }}
                          </td>
                        </tr>

                        <tr v-if="secTopRanks(sec.key).length === 0" class="border-t border-gray-200 dark:border-gray-800">
                          <td colspan="3" class="px-3 py-6 text-center text-sm text-gray-400">
                            데이터 없음
                          </td>
                        </tr>
                        </tbody>
                      </table>
                    </div>

                    <div class="mt-3 text-xs text-gray-400">
                      더미데이터 기준, 필터/검색은 UI만 연결
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 툴팁: 클릭 즉시 표시, 이후 마우스/스크롤 움직이면 닫힘 (툴팁 위에서는 유지) -->
            <Teleport to="body">
              <div v-show="tooltip.open" class="pointer-events-none fixed inset-0 z-[9999]" aria-hidden="true">
                <div ref="tipTranslateEl" class="absolute left-0 top-0" style="transform: translate3d(0px,0px,0);">
                  <div
                      ref="tipInnerEl"
                      class="pointer-events-auto rounded-lg border border-gray-200 bg-white px-3 py-2 text-xs text-gray-700 shadow-lg
                           dark:border-gray-700 dark:bg-gray-900 dark:text-gray-200"
                      style="will-change: transform;"
                      @mouseenter="isTipHover = true; cancelClose()"
                      @mouseleave="isTipHover = false; closeTipDelayed()"
                      @click.stop
                  >
                    <div class="space-y-1">
                      <!-- 지점명 -->
                      <div class="font-semibold text-gray-800 dark:text-gray-100">
                        {{ tooltip.title }}
                      </div>

                      <!-- 누적 -->
                      <div class="text-gray-700 dark:text-gray-300">
                        누적매출액 : {{ money(tooltip.total) }}
                      </div>

                      <!-- 최초 -->
                      <div class="text-gray-600 dark:text-gray-400">
                        최초 : {{ money(tooltip.a) }} ({{ percent(tooltip.a, tooltip.total) }})
                      </div>

                      <!-- 업셀 -->
                      <div class="text-gray-600 dark:text-gray-400">
                        업셀 : {{ money(tooltip.b) }} ({{ percent(tooltip.b, tooltip.total) }})
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </Teleport>
          </div>
        </div>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { computed, ref, onMounted, watch, onBeforeUnmount } from 'vue'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import { UsersIcon, UserIcon } from '@heroicons/vue/24/outline'
import { BuildingStorefrontIcon } from '@heroicons/vue/24/solid'
import { useAuthStore } from '@/stores/auth.js'
import { globalFilters } from '@/composables/globalFilters.js'
import api from '@/plugins/axios.js'

/** 섹션 */
const sections = [
  { key: 'month', title: '이번달 매출현황' },
  { key: 'range', title: '기간내 매출현황' },
  { key: 'all', title: '전체기간내 매출현황' },
]

function rankTitle(secKey) {
  if (secKey === 'month') return '이번달 누적매출액 순위표'
  if (secKey === 'range') return '기간내 누적매출액 순위표'
  if (secKey === 'all') return '전체기간 누적매출액 순위표'
  return '누적매출액 순위표'
}

/** 보기 모드 */
const viewMode = ref('BRANCH')
const includeResigned = ref(false)

// PartitionBar 구간(서버 값으로 덮어씀)
const partitionStops = ref([])
const partitionColors = ref([])

function bucketIndex(total) {
  const t = Number(total || 0)
  const stops = [...(partitionStops.value || [])].sort((a, b) => a - b)

  for (let i = 0; i < stops.length; i++) {
    if (t <= stops[i]) return i
  }
  return stops.length // 마지막 구간(최대 초과)
}
function bucketColor(total) {
  const colors = partitionColors.value || []
  const i = bucketIndex(total)

  // 이상 케이스 대비: i번째 없으면 마지막 색, 그것도 없으면 회색
  return colors[i] || colors[colors.length - 1] || '#D1D5DB'
}
// function partitionSegs(secKey, total) {
//   const t = Number(total || 0)
//   const stops = [...(partitionStops.value || [])].sort((a, b) => a - b)
//   const bounds = [0, ...stops, Infinity]
//   const colors = partitionColors.value || []
//
//   const out = []
//   for (let i = 0; i < bounds.length - 1; i++) {
//     const from = bounds[i]
//     const to = bounds[i + 1]
//     const val = Math.max(0, Math.min(t, to) - from)
//     if (val <= 0) continue
//     out.push({ from, to, value: val, color: colors[i] || '#D1D5DB' })
//   }
//   return out
// }

/** 통계 데이터 */
const dashboard = ref(null)

function normalizeRows(rows) {
  const arr = Array.isArray(rows) ? rows : []
  return arr.map(r => {
    const a = Number(r.a || 0)
    const b = Number(r.b || 0)
    const c = Number(r.c || 0)
    const total = Number(r.total ?? (a + b + c))
    return { ...r, a, b, c, total, resigned: !!r.resigned }
  })
}

let dateWatchTimer = null

watch(
    () => [globalFilters.dateFrom, globalFilters.dateTo],
    () => {
      if (dateWatchTimer) clearTimeout(dateWatchTimer)
      dateWatchTimer = setTimeout(() => fetchDashboard(), 150)
    }
)

async function fetchDashboard() {
  const params = {
    viewMode: viewMode.value, // BRANCH | TEAM | PERSON
    includeResigned: includeResigned.value, // true/false
    dateFrom: globalFilters.dateFrom || undefined,
    dateTo: globalFilters.dateTo || undefined,
  }

  const { data } = await api.get('/api/work/sales/dashboard', { params })

  dashboard.value = {
    ...data,
    month: { ...data.month, rows: normalizeRows(data?.month?.rows), topRanks: data?.month?.topRanks || [] },
    range: { ...data.range, rows: normalizeRows(data?.range?.rows), topRanks: data?.range?.topRanks || [] },
    all:   { ...data.all,   rows: normalizeRows(data?.all?.rows),   topRanks: data?.all?.topRanks   || [] },
  }

  partitionStops.value = data?.partitionStops || []
  partitionColors.value = data?.partitionColors || []
}

function endLabelStyle(secKey, total) {
  const p = pctX(secKey, total)
  if (p > 92) return { right: '0px', paddingRight: '6px' }
  return { left: `calc(${p}% + 8px)` }
}

function secRows(secKey) {
  const base = dashboard.value?.[secKey]?.rows || []
  // includeResigned는 서버에 이미 반영되지만, 방어로 한 번 더 걸어도 됨
  return includeResigned.value ? base : base.filter(r => !r.resigned)
}

function secRowsSorted(secKey) {
  return [...secRows(secKey)].sort((a, b) => b.total - a.total)
}

function secTopRanks(secKey) {
  return dashboard.value?.[secKey]?.topRanks || []
}

function maxValueOf(secKey) {
  const rows = secRowsSorted(secKey)
  const rowMax = Math.max(1, ...rows.map(r => r.total))
  const stopMax = Math.max(0, ...(partitionStops.value || []))
  const m = Math.max(rowMax, stopMax)
  return Math.ceil(m * 1.15)
}

function ticksOf(secKey) {
  const m = maxValueOf(secKey)
  return (partitionStops.value || []).filter(v => v > 0 && v < m)
}

function pctX(secKey, v) {
  const m = maxValueOf(secKey)
  return Math.max(0, Math.min(100, (Number(v || 0) / m) * 100))
}

function money(n) {
  const num = Number(n || 0)
  return `$${num.toLocaleString()}`
}

function percent(part, total) {
  const t = Number(total || 0)
  if (!t) return '0%'
  return `${Math.round((Number(part || 0) / t) * 100)}%`
}

/** 버튼 스타일 */
function modeBtnClass(active) {
  return [
    'inline-flex h-11 items-center gap-2 rounded-lg border px-4 text-sm font-medium transition',
    active
        ? 'border-gray-300 bg-gray-100 text-gray-800 dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100'
        : 'border-gray-200 bg-white text-gray-600 hover:bg-gray-50 dark:border-gray-800 dark:bg-white/[0.02] dark:text-gray-300 dark:hover:bg-gray-800',
  ].join(' ')
}

/** 라벨 말줄임 */
function shortLabel(s) {
  const str = String(s ?? '')
  return str.length > 8 ? str.slice(0, 7) + '…' : str
}

/** 레이아웃 */
const labelColWidth = 92
const TRACK_BORDER_PX = 1

/** 섹션별 x축 트랙 폭 측정 */
const axisTrackElByKey = new Map()
const axisTrackWidthByKey = ref({})
const axisRos = new Map()

function setAxisTrackEl(secKey, el) {
  if (!el) return

  // 같은 엘리먼트면 아무 것도 안 함 (재귀 루프 핵심 차단)
  const prevEl = axisTrackElByKey.get(secKey)
  if (prevEl === el) return
  axisTrackElByKey.set(secKey, el)

  const prev = axisRos.get(secKey)
  if (prev) prev.disconnect()

  const ro = new ResizeObserver(() => {
    const w = el.clientWidth || 0
    const cur = Number(axisTrackWidthByKey.value[secKey] || 0)
    // 폭이 바뀔 때만 반응형 업데이트
    if (cur === w) return
    axisTrackWidthByKey.value = { ...axisTrackWidthByKey.value, [secKey]: w }
  })
  ro.observe(el)
  axisRos.set(secKey, ro)

  // 초기값도 폭이 바뀔 때만 반영
  const w0 = el.clientWidth || 0
  const cur0 = Number(axisTrackWidthByKey.value[secKey] || 0)
  if (cur0 !== w0) {
    axisTrackWidthByKey.value = { ...axisTrackWidthByKey.value, [secKey]: w0 }
  }
}

onBeforeUnmount(() => {
  axisRos.forEach(ro => ro.disconnect())
  axisRos.clear()
  axisTrackElByKey.clear()
})

function trackW(secKey) {
  return Number(axisTrackWidthByKey.value[secKey] || 0)
}
function innerW(secKey) {
  return Math.max(0, trackW(secKey) - TRACK_BORDER_PX * 2)
}
function valueToInnerPx(secKey, v) {
  const w = innerW(secKey)
  if (!w) return 0
  const ratio = Math.max(0, Math.min(1, Number(v || 0) / maxValueOf(secKey)))
  return Math.round(ratio * w)
}
function tickInnerLeftPx(secKey, tick) {
  return valueToInnerPx(secKey, tick)
}
function tickAxisLeftPx(secKey, tick) {
  return TRACK_BORDER_PX + valueToInnerPx(secKey, tick)
}
function showBluePct(secKey, row) {
  const a = Number(row.a || 0)
  return valueToInnerPx(secKey, a) >= 36
}
function showGreenPct(secKey, row) {
  const g = Number(row.b || 0) + Number(row.c || 0)
  return valueToInnerPx(secKey, g) >= 36
}

/** 클릭 툴팁 */
const tooltip = ref({
  open: false,
  rowKey: '',
  title: '',
  total: 0,
  a: 0,
  b: 0,
})

const tipTranslateEl = ref(null)
const tipInnerEl = ref(null)

let tipW = 240
let tipH = 64
let tipBoundsRect = null

function clamp(n, min, max) {
  return Math.max(min, Math.min(max, n))
}

function measureTip() {
  const el = tipInnerEl.value
  if (!el) return
  const w = el.offsetWidth
  const h = el.offsetHeight
  if (w > 0) tipW = w
  if (h > 0) tipH = h
}

function calcTipXY(clientX, clientY) {
  const margin = 12
  const rawX = clientX + 12
  const rawY = clientY - 48

  // 기본은 뷰포트
  let minX = margin
  let minY = margin
  let maxX = window.innerWidth - tipW - margin
  let maxY = window.innerHeight - tipH - margin

  // 그래프 영역이 있으면 그 안에서만 clamp
  if (tipBoundsRect) {
    minX = tipBoundsRect.left + margin
    minY = tipBoundsRect.top + margin
    maxX = tipBoundsRect.right - tipW - margin
    maxY = tipBoundsRect.bottom - tipH - margin
  }

  // 영역이 너무 작을 때 깨지지 않게
  maxX = Math.max(minX, maxX)
  maxY = Math.max(minY, maxY)

  return {
    x: clamp(rawX, minX, maxX),
    y: clamp(rawY, minY, maxY),
  }
}

function applyTransform(x, y) {
  const el = tipTranslateEl.value
  if (!el) return
  el.style.transform = `translate3d(${x}px, ${y}px, 0)`
}

/** 툴팁 호버 상태 + 클릭 직후 미세 움직임 무시(즉각적으로 보이게) */
const isTipHover = ref(false)
const OPEN_GRACE_MS = 200
let ignoreMoveUntil = 0

function openTipAt(e, row) {
  const a = Number(row.a || 0)
  const b = Number(row.b || 0) + Number(row.c || 0)

  tooltip.value.open = true
  tooltip.value.rowKey = String(row.key)
  tooltip.value.title = row.label
  tooltip.value.total = row.total
  tooltip.value.a = a
  tooltip.value.b = b

  const boundsEl = e?.currentTarget?.closest?.('[data-chart-bounds]')
  tipBoundsRect = boundsEl ? boundsEl.getBoundingClientRect() : null

  ignoreMoveUntil = performance.now() + OPEN_GRACE_MS

  // 즉시 표시 (기본 tipW/tipH로 바로 clamp)
  const p0 = calcTipXY(e.clientX, e.clientY)
  applyTransform(p0.x, p0.y)

  // 다음 프레임에 실제 크기 측정 후 clamp 재적용
  requestAnimationFrame(() => {
    if (!tooltip.value.open) return
    measureTip()
    const p1 = calcTipXY(e.clientX, e.clientY)
    applyTransform(p1.x, p1.y)
  })
}

let closeTimer = null
const CLOSE_DELAY_MS = 200

function closeTip() {
  if (closeTimer) {
    clearTimeout(closeTimer)
    closeTimer = null
  }
  tooltip.value.open = false
  tooltip.value.rowKey = ''
  tipBoundsRect = null
}

function closeTipDelayed() {
  if (closeTimer) return

  closeTimer = setTimeout(() => {
    tooltip.value.open = false
    tooltip.value.rowKey = ''
    tipBoundsRect = null
    closeTimer = null
  }, CLOSE_DELAY_MS)
}

function cancelClose() {
  if (closeTimer) {
    clearTimeout(closeTimer)
    closeTimer = null
  }
}

function toggleRowTip(e, row) {
  const key = String(row.key)
  if (tooltip.value.open && tooltip.value.rowKey === key) {
    closeTip()
    return
  }
  openTipAt(e, row)
}

/** pointerdown 즉시 오픈 + 뒤따르는 click 토글(닫힘) 방지 */
let suppressClickUntil = 0
let suppressClickRowKey = ''

function onBarPointerDown(e, row) {
  suppressClickUntil = performance.now() + 350
  suppressClickRowKey = String(row.key)
  toggleRowTip(e, row)
}

function onBarClick(e, row) {
  if (performance.now() < suppressClickUntil && String(row.key) === suppressClickRowKey) return
  toggleRowTip(e, row)
}

/** 열려있는 상태에서 스크롤 or 마우스 움직이면 닫기 (툴팁 위에서는 유지) */
function onAnyScroll() {
  if (!tooltip.value.open) return
  if (isTipHover.value) return
  closeTipDelayed()
}

function onMouseMove() {
  if (!tooltip.value.open) return
  if (isTipHover.value) return
  if (performance.now() < ignoreMoveUntil) return
  closeTipDelayed()
}

/** 바깥 클릭으로 닫기 / ESC로 닫기 */
function onDocClick() {
  if (!tooltip.value.open) return
  if (isTipHover.value) return
  closeTipDelayed()
}

function onKeyDown(ev) {
  if (ev.key === 'Escape' && tooltip.value.open) closeTip()
}

document.addEventListener('click', onDocClick)
document.addEventListener('keydown', onKeyDown)
document.addEventListener('mousemove', onMouseMove)
document.addEventListener('scroll', onAnyScroll, true)

onMounted(fetchDashboard)

watch([viewMode, includeResigned], () => {
  fetchDashboard()
})

onBeforeUnmount(() => {
  document.removeEventListener('click', onDocClick)
  document.removeEventListener('keydown', onKeyDown)
  document.removeEventListener('mousemove', onMouseMove)
  document.removeEventListener('scroll', onAnyScroll, true)
})
</script>
