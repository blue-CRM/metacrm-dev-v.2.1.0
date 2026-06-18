<template>
  <div class="rounded-xl border border-gray-200 bg-gray-50/60 dark:border-[#1f2a47] dark:bg-[#16213b]/30">
    <!-- 1) 최상위 아코디언: 처음엔 접힘 -->
    <button
        type="button"
        class="flex w-full items-center justify-between gap-3 px-4 py-3 text-left hover:bg-gray-50/70 dark:hover:bg-[#16213b]/40"
        @click="toggleOuter"
    >
      <div class="min-w-0 flex-1">
        <h4 class="text-sm font-semibold text-gray-800 dark:text-gray-100">
          선택 DB 날짜별 전문가 집계
        </h4>
      </div>

      <div class="flex shrink-0 items-center gap-3 text-xs text-gray-500 dark:text-gray-400">
        <template v-if="hasLoaded && !loading && groupedRows.length">
          <span class="whitespace-nowrap">총 {{ totalCount }}건</span>
          <span class="whitespace-nowrap">날짜 {{ groupedRows.length }}일치</span>
          <span class="whitespace-nowrap">전문가 {{ uniqueExpertCount }}명</span>
        </template>

        <ChevronUpIcon
            v-if="outerOpen"
            class="h-4 w-4 text-gray-500 dark:text-gray-400"
        />
        <ChevronDownIcon
            v-else
            class="h-4 w-4 text-gray-500 dark:text-gray-400"
        />
      </div>
    </button>

    <!-- 최상위 아코디언 내용 -->
    <div v-if="outerOpen" class="border-t border-gray-100 px-4 py-3 dark:border-[#1f2a47]">
      <div v-if="loading" class="text-sm text-gray-500 dark:text-gray-400">
        불러오는 중...
      </div>

      <div v-else-if="hasLoaded && !groupedRows.length" class="text-sm text-gray-500 dark:text-gray-400">
        집계 데이터가 없습니다.
      </div>

      <div v-else-if="groupedRows.length" class="space-y-3">
        <!-- 2) 전체 전문가 합계 아코디언: 처음엔 접힘 -->
        <div class="rounded-lg border border-gray-200 bg-white dark:border-[#28365a] dark:bg-[#0e1626]/40">
          <button
              type="button"
              class="flex w-full items-center justify-between gap-3 px-4 py-3 text-left hover:bg-gray-50 dark:hover:bg-[#16213b]/60"
              @click="overallOpen = !overallOpen"
          >
            <div class="min-w-0 flex-1">
              <div class="text-sm font-semibold text-gray-800 dark:text-gray-100">
                전체 전문가 합계
              </div>
            </div>

            <div class="flex shrink-0 items-center gap-3 text-xs text-gray-500 dark:text-gray-400">
              <span class="whitespace-nowrap">전문가 {{ overallExpertTotals.length }}명</span>
              <span class="whitespace-nowrap font-semibold text-gray-700 dark:text-gray-200">
                합계 {{ totalCount }}개
              </span>

              <ChevronUpIcon
                  v-if="overallOpen"
                  class="h-4 w-4 text-gray-500 dark:text-gray-400"
              />
              <ChevronDownIcon
                  v-else
                  class="h-4 w-4 text-gray-500 dark:text-gray-400"
              />
            </div>
          </button>

          <div v-if="overallOpen" class="border-t border-gray-100 px-4 py-3 dark:border-[#1f2a47]">
            <div class="max-h-28 overflow-y-auto rounded-md bg-gray-50 dark:bg-[#16213b]/50">
              <div
                  v-for="expert in overallExpertTotals"
                  :key="expert.expertName"
                  class="flex items-center justify-between gap-3 border-b border-gray-100 px-3 py-2 text-sm last:border-b-0 dark:border-[#28365a]"
              >
                <span class="min-w-0 flex-1 truncate text-gray-700 dark:text-gray-200">
                  {{ expert.expertName }}
                </span>
                <span class="shrink-0 font-medium text-gray-800 dark:text-gray-100">
                  {{ expert.count }}개
                </span>
              </div>
            </div>
          </div>
        </div>

        <!-- 3) 날짜별 집계 아코디언: 처음엔 접힘 -->
        <div class="rounded-lg border border-gray-200 bg-white dark:border-[#28365a] dark:bg-[#0e1626]/40">
          <button
              type="button"
              class="flex w-full items-center justify-between gap-3 px-4 py-3 text-left hover:bg-gray-50 dark:hover:bg-[#16213b]/60"
              @click="datesSectionOpen = !datesSectionOpen"
          >
            <div class="min-w-0 flex-1">
              <div class="text-sm font-semibold text-gray-800 dark:text-gray-100">
                날짜별 집계
              </div>
            </div>

            <div class="flex shrink-0 items-center gap-3 text-xs text-gray-500 dark:text-gray-400">
              <span class="whitespace-nowrap">날짜 {{ groupedRows.length }}일치</span>

              <ChevronUpIcon
                  v-if="datesSectionOpen"
                  class="h-4 w-4 text-gray-500 dark:text-gray-400"
              />
              <ChevronDownIcon
                  v-else
                  class="h-4 w-4 text-gray-500 dark:text-gray-400"
              />
            </div>
          </button>

          <div v-if="datesSectionOpen" class="border-t border-gray-100 px-4 py-3 dark:border-[#1f2a47]">
            <!-- 세로 길이 줄임 -->
            <div class="max-h-52 overflow-y-auto rounded-md bg-gray-50/60 dark:bg-[#16213b]/30">
              <div
                  v-for="row in groupedRows"
                  :key="row.statDate"
                  class="border-b border-gray-100 last:border-b-0 dark:border-[#1f2a47]"
              >
                <!-- 날짜별 아코디언: 처음엔 전부 접힘 -->
                <button
                    type="button"
                    class="flex w-full items-center justify-between gap-3 px-3 py-3 text-left hover:bg-gray-50 dark:hover:bg-[#16213b]/60"
                    @click="toggleDate(row.statDate)"
                >
                  <div class="min-w-0 flex-1">
                    <div class="text-sm font-semibold text-gray-800 dark:text-gray-100">
                      {{ row.statDate }}
                    </div>
                  </div>

                  <div class="flex shrink-0 items-center gap-3 text-xs text-gray-500 dark:text-gray-400">
                    <span class="whitespace-nowrap">전문가 {{ row.expertCount }}명</span>
                    <span class="whitespace-nowrap font-semibold text-gray-700 dark:text-gray-200">
                      합계 {{ row.totalCount }}개
                    </span>

                    <ChevronUpIcon
                        v-if="isOpen(row.statDate)"
                        class="h-4 w-4 text-gray-500 dark:text-gray-400"
                    />
                    <ChevronDownIcon
                        v-else
                        class="h-4 w-4 text-gray-500 dark:text-gray-400"
                    />
                  </div>
                </button>

                <div v-if="isOpen(row.statDate)" class="px-3 pb-3">
                  <div class="rounded-md bg-white dark:bg-[#0e1626]/40">
                    <div
                        v-for="expert in row.experts"
                        :key="`${row.statDate}-${expert.expertName}`"
                        class="flex items-center justify-between gap-3 border-b border-gray-100 px-3 py-2 text-sm last:border-b-0 dark:border-[#28365a]"
                    >
                      <span class="min-w-0 flex-1 truncate text-gray-700 dark:text-gray-200">
                        {{ expert.expertName }}
                      </span>
                      <span class="shrink-0 font-medium text-gray-800 dark:text-gray-100">
                        {{ expert.count }}개
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-else class="text-sm text-gray-500 dark:text-gray-400">
        집계 데이터가 없습니다.
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { ChevronDownIcon, ChevronUpIcon } from '@heroicons/vue/24/solid'
import axios from '@/plugins/axios.js'

const props = defineProps({
  customerIds: {
    type: Array,
    default: () => [],
  },
})

const loading = ref(false)
const rows = ref([])

/* 처음엔 전부 접힘 */
const outerOpen = ref(false)
const overallOpen = ref(false)
const datesSectionOpen = ref(false)
const openedDates = ref([])
const hasLoaded = ref(false)

/* 같은 선택이면 재로딩 방지 */
const customerKey = computed(() => {
  const ids = [...(props.customerIds || [])].sort((a, b) => a - b)
  return ids.join(',')
})

const cache = new Map()

async function fetchSummary(force = false) {
  const key = customerKey.value

  if (!key) {
    rows.value = []
    openedDates.value = []
    overallOpen.value = false
    datesSectionOpen.value = false
    hasLoaded.value = false
    return
  }

  if (!force && cache.has(key)) {
    rows.value = cache.get(key) || []
    hasLoaded.value = true
    return
  }

  loading.value = true
  try {
    const { data } = await axios.post('/api/work/allocate/expert-daily-summary', {
      customerIds: props.customerIds,
    })

    const result = data || []
    rows.value = result
    cache.set(key, result)

    hasLoaded.value = true

    overallOpen.value = false
    datesSectionOpen.value = false
    openedDates.value = []
  } catch (e) {
    console.error(e)
    rows.value = []
    overallOpen.value = false
    datesSectionOpen.value = false
    openedDates.value = []
    hasLoaded.value = false
  } finally {
    loading.value = false
  }
}

async function toggleOuter() {
  outerOpen.value = !outerOpen.value

  if (outerOpen.value && !hasLoaded.value) {
    await fetchSummary()
  }
}

watch(
    customerKey,
    (newKey, oldKey) => {
      if (newKey === oldKey) return

      rows.value = []
      hasLoaded.value = false

      outerOpen.value = false
      overallOpen.value = false
      datesSectionOpen.value = false
      openedDates.value = []
    },
    { immediate: true }
)

const groupedRows = computed(() => {
  const map = new Map()

  for (const r of rows.value) {
    if (!map.has(r.statDate)) {
      map.set(r.statDate, {
        statDate: r.statDate,
        expertCount: 0,
        totalCount: 0,
        experts: [],
      })
    }

    const group = map.get(r.statDate)
    group.experts.push({
      expertName: r.expertName,
      count: r.count,
    })
    group.totalCount += r.count
  }

  return [...map.values()]
      .map((group) => {
        group.experts.sort((a, b) => {
          if (b.count !== a.count) return b.count - a.count
          return a.expertName.localeCompare(b.expertName, 'ko')
        })
        group.expertCount = group.experts.length
        return group
      })
      .sort((a, b) => b.statDate.localeCompare(a.statDate))
})

const totalCount = computed(() => {
  return rows.value.reduce((sum, r) => sum + r.count, 0)
})

const uniqueExpertCount = computed(() => {
  return new Set(rows.value.map((r) => r.expertName)).size
})

const overallExpertTotals = computed(() => {
  const map = new Map()

  for (const r of rows.value) {
    map.set(r.expertName, (map.get(r.expertName) || 0) + r.count)
  }

  return [...map.entries()]
      .map(([expertName, count]) => ({ expertName, count }))
      .sort((a, b) => {
        if (b.count !== a.count) return b.count - a.count
        return a.expertName.localeCompare(b.expertName, 'ko')
      })
})

function isOpen(statDate) {
  return openedDates.value.includes(statDate)
}

function toggleDate(statDate) {
  if (openedDates.value.includes(statDate)) {
    openedDates.value = openedDates.value.filter((d) => d !== statDate)
  } else {
    openedDates.value = [...openedDates.value, statDate]
  }
}
</script>