<template>
  <AdminLayout>
    <PageBreadcrumb :pageTitle="pageTitle" />
    <div class="grid grid-cols-12 gap-4 min-w-0">
      <div class="col-span-12 space-y-6 min-w-0">

        <!-- SUPERADMIN (본사) + CENTERHEAD, EXPERT -->
        <ComponentCard
            v-if="role === 'SUPERADMIN' || role === 'CENTERHEAD' || role === 'EXPERT'"
            :selects="hqSelects"
            :buttons="hqButtons"
            :disabledButtons="hqDisabledButtons"
            :showRefresh="true"
            :refreshing="isRefreshing"
            @refresh="onRefresh"
            @changeSize="setSize"
            @selectChange="onSelectChange"
            @buttonClick="onHqButton"
        >
          <PsnsTable
              ref="tableRef"
              :columns="hqColumns"
              :data="items"
              :showCheckbox="true"
              :page="page"
              :totalPages="totalPages"
              :page-size="size"
              :loading="busy"
              @rowSelect="onRowSelect"
              @changePage="changePage"
          />
        </ComponentCard>

        <!-- MANAGER (팀장) -->
        <ComponentCard
            v-else-if="role === 'MANAGER'"
            :selects="[
                ['상태 전체', '부재1', '부재2', '부재3', '부재4', '부재5', '기타', '결번',
                  '재콜', '내방', '내방취소', '신규', '가망', '자연풀', '카피', '거절', '없음', '회수'],
                ['직전상태 전체', '부재1','부재2','부재3','부재4','부재5','기타','결번',
                   '재콜','내방','내방취소','가망','자연풀','카피','거절']
                   ]"
            :buttons="mgrButtons"
            :showRefresh="true"
            :refreshing="isRefreshing"
            @refresh="onRefresh"
            @changeSize="setSize"
            @selectChange="onSelectChange"
            @buttonClick="onMgrButton"
        >
          <PsnsTable
              ref="tableRef"
              :columns="mgrColumns"
              :data="items"
              :showCheckbox="true"
              :page="page"
              :totalPages="totalPages"
              :page-size="size"
              :loading="busy"
              @rowSelect="onRowSelect"
              @changePage="changePage"
          />
        </ComponentCard>

        <!-- STAFF 접근 시 경고 안내를 노출하려면 주석 해제
        <div v-else class="p-6 rounded-xl border border-amber-200 bg-amber-50 text-amber-800 dark:border-amber-900/60 dark:bg-amber-950/30 dark:text-amber-200">
          이 페이지는 본사 또는 팀장만 사용할 수 있습니다.
        </div>
        -->

      </div>
    </div>
  </AdminLayout>

  <!-- 전역 로딩 오버레이 (메모 모달과 동일하게 body로 텔레포트) -->
  <Teleport to="body">
    <transition name="fade">
      <div
          v-if="showTableSpinner"
          class="fixed inset-0 z-[2147483646]"
          aria-live="polite" aria-busy="true" role="status"
      >
        <!-- 배경 -->
        <div class="absolute inset-0 bg-black/5 dark:bg-black/60"></div>

        <!-- 스피너 -->
        <div class="absolute inset-0 z-[2147483647] flex items-center justify-center">
          <div class="flex flex-col items-center gap-3">
            <svg class="animate-spin h-8 w-8 text-blue-500" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10"
                      stroke="currentColor" stroke-width="4" fill="none"/>
              <path class="opacity-75" fill="currentColor"
                    d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z"/>
            </svg>
            <p class="text-sm text-gray-900 dark:text-white/90">불러오는 중…</p>
          </div>
        </div>
      </div>
    </transition>
  </Teleport>

  <AllocateModal
      v-if="transferModal.open"
      mode="HQ"
      :selected-count="selectedRows.length"
      :exclude-center-ids="excludedCenterIds"
      api-base="/api/work/revoke/transfer"
      title="DB 일괄 이관"
      primary-label="이관하기"
      manager-pool-label="팀 공용 이관"
      manager-personal-label="팀장 개인 이관"
      @close="closeTransferModal"
      @confirm="onConfirmTransfer"
  />

</template>

<script setup lang="ts">
import {computed, onUnmounted, ref, watch} from 'vue'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import ComponentCard from '@/components/common/ComponentCard.vue'
import PsnsTable from '@/components/tables/basic-tables/PsnsTable.vue'
import AllocateModal from '@/components/ui/AllocateModal.vue'
import { useAuthStore } from '@/stores/auth.js'
import { useTableQuery } from '@/composables/useTableQuery.js'
import { globalFilters } from '@/composables/globalFilters.js'
import axios from '@/plugins/axios.js'

/** 권한/페이지 타이틀 */
const auth = useAuthStore()
const role = computed(() => auth.grants.role)
const pageTitle = ref('DB 회수하기')

const isSuper = computed(() => auth.grants.isSuper)
const canTransfer = computed(() => role.value === 'SUPERADMIN' && isSuper.value)

/** 목록 훅 (공용 필터: 날짜/카테고리/키워드) */
const {
  items, page, size, totalPages, fetchData, changePage, setSize, setFilter, loading: tableLoading,
} = useTableQuery({
  url: '/api/work/revoke/list',
  externalFilters: globalFilters,
  useExternalKeys: {
    from: 'dateFrom',
    to: 'dateTo',
    category: 'category',
    keyword: 'keyword',
    status: 'status',
    prevStatus: 'prevStatus',
    sort: 'sort',
    centerId: 'centerId',
  },
  mapper: (res: any) => ({
    items: res.data.items,
    totalPages: res.data.totalPages,
    totalCount: res.data.totalCount,
  }),
})

// 로딩 오버레이 설정
const isRefreshing = ref(false)
const uiLoading = ref(false)
const busy = computed(() => tableLoading.value || isRefreshing.value || uiLoading.value)
const showTableSpinner = ref(false)
let delayTimer = null

async function runBusy(task) {
  if (uiLoading.value) return
  uiLoading.value = true
  try { await task() } finally { uiLoading.value = false }
}

watch(busy, (v) => {
  if (v) {
    // 짧은 로딩은 스피너 숨김
    delayTimer = setTimeout(() => { showTableSpinner.value = true }, 200)
  } else {
    if (delayTimer) { clearTimeout(delayTimer); delayTimer = null }
    showTableSpinner.value = false
  }
})

onUnmounted(() => {
  if (delayTimer) { clearTimeout(delayTimer); delayTimer = null }
})

/** 테이블 선택 */
const tableRef = ref<any>(null)
const selectedRows = ref<any[]>([])
const excludedCenterIds = computed(() => {
  return [...new Set(
      selectedRows.value
          .map((r: any) => r.centerId)
          .filter((v: any) => v != null)
  )]
})
const onRowSelect = (rows: any[]) => { selectedRows.value = rows }
function needSelection(action: '회수' | '이관' = '회수'): number[] {
  const ids = selectedRows.value.map((r: any) => r.id)
  if (!ids.length) alert(`${action}할 고객을 선택해주세요.`)
  return ids
}

/** 컬럼 정의 */
const hqColumns = [
  { key: 'createdAt', label: 'DB생성일', type: 'text' },
  { key: 'division',  label: '구분',     type: 'badge', options: ['최초','유효'] },
  // { key: 'category',  label: '카테고리', type: 'badge', options: ['주식','코인'] },
  { key: 'name',      label: '이름',     type: 'text' },
  { key: 'phone',     label: '전화번호', type: 'text', ellipsis: { width: 150 } },
  { key: 'source',    label: 'DB출처',   type: 'text' },
  { key: 'content',   label: '내용',     type: 'text', ellipsis: { width: 150 } },
  { key: "status",    label: "상태",     type: "badge" },
  { key: 'prevStatus', label: '직전상태', type: 'badge', hideIfEmpty: true },
  { key: "",  label: "",   type: "text", ellipsis: { width: 10 } },
  {
    key: "centerName",
    label: "팀명",
    type: "text",
  },
  { key: 'staff',     label: '프로',    type: 'text' },
]

const mgrColumns = [
  { key: 'createdAt', label: 'DB생성일', type: 'text' },
  { key: "",  label: "",   type: "text", ellipsis: { width: 10 } },
  // { key: 'category',  label: '카테고리', type: 'badge', options: ['주식','코인'] },
  { key: 'name',      label: '이름',     type: 'text' },
  { key: 'phone',     label: '전화번호', type: 'text', ellipsis: { width: 150 } },
  { key: 'source',    label: 'DB출처',   type: 'text' },
  { key: 'content',   label: '내용',     type: 'text', ellipsis: { width: 150 } },
  { key: "status",    label: "상태",     type: "badge" },
  { key: 'prevStatus', label: '직전상태', type: 'badge', hideIfEmpty: true },
  { key: "",  label: "",   type: "text", ellipsis: { width: 10 } },
  {
    key: "centerName",
    label: "팀명",
    type: "text",
  },
  { key: 'staff',     label: '프로',    type: 'text' },
]

/* =============================
   드롭다운 필터 관련
============================= */

const teamSelectOptions = ref<string[]>(['팀 전체'])
const teamNameToId = ref<Record<string, number>>({})

const hqSelects = computed(() => [
  ['상태 전체', '부재1', '부재2', '부재3', '부재4', '부재5', '기타', '결번',
    '재콜', '내방', '내방취소', '신규', '가망', '자연풀', '카피', '거절', '없음', '회수'],
  ['직전상태 전체', '부재1','부재2','부재3','부재4','부재5','기타','결번',
    '재콜','내방','내방취소','가망','자연풀','카피','거절'],
  teamSelectOptions.value,
])

async function loadRevokeTeams() {
  if (!['SUPERADMIN', 'CENTERHEAD', 'EXPERT'].includes(role.value)) return

  try {
    const { data } = await axios.get('/api/work/revoke/teams')
    const rows = Array.isArray(data) ? data : []

    teamSelectOptions.value = ['팀 전체', ...rows.map((r: any) => r.centerName)]
    teamNameToId.value = Object.fromEntries(
        rows.map((r: any) => [r.centerName, r.centerId])
    )
  } catch (e) {
    console.error(e)
    teamSelectOptions.value = ['팀 전체']
    teamNameToId.value = {}
  }
}

// 상단 드롭다운(Select) 변경 시 호출
function onSelectChange({ idx, value }: { idx: number; value: string }) {
  // 첫 번째 드롭다운(index 0) : '상태(status)' 필터
  if (idx === 0) {
    // '상태 전체'를 선택하면 필터를 해제(null)하고, 그 외에는 해당 값을 전송
    const filterValue = value === '상태 전체' ? null : value

    // useTableQuery의 setFilter를 호출하여 API 재요청
    setFilter('status', filterValue)
  }

  // 두 번째 드롭다운(index 1) : '직전상태(prevStatus)' 필터
  if (idx === 1) {
    const filterValue = value === '직전상태 전체' ? null : value
    setFilter('prevStatus', filterValue)
  }

  // 세 번째 드롭다운(index 2) : '팀선택(centerId)' 필터
  if (idx === 2) {
    const filterValue = value === '팀 전체' ? null : teamNameToId.value[value] ?? null
    setFilter('centerId', filterValue)
  }
}

/* =============================
   정렬 버튼 관련
============================= */

// 1. 정렬 상태 변수 선언 (가장 먼저)
const viewOptions = ref({ oldest: false })  // false: 최신순, true: 과거순(오래된순)

// 2. 버튼 라벨 동적 계산 (viewOptions 사용)
const sortLabel = computed(() =>
    viewOptions.value.oldest ? '최신순 보기' : '과거순 보기'
)

// 3. 버튼 배열 생성 (sortLabel 사용)
const hqButtons = computed(() => {
  const btns = [sortLabel.value, '회수하기']
  if (canTransfer.value) btns.push('이관하기')
  return btns
})
const mgrButtons = computed(() => [sortLabel.value, '회수하기'])

// 이관 모달 상태 추가
const transferModal = ref<{ open: boolean }>({ open: false })
function closeTransferModal() {
  transferModal.value.open = false
}

// 공통 정렬 토글 로직
async function toggleSort() {
  await runBusy(async () => {
    // 1. 상태 반전
    viewOptions.value.oldest = !viewOptions.value.oldest

    // 2. 필터 설정 (백엔드는 'oldest' 값이 오면 과거순, 없으면 최신순으로 처리)
    setFilter('sort', viewOptions.value.oldest ? 'oldest' : null)

    // 3. UX 편의상 선택 초기화 (정렬 바뀌면 선택된 행들의 위치가 섞이므로)
    selectedRows.value = []
    tableRef.value?.clearSelection?.()
  })
}

// ===== 모달 및 버튼 핸들러 =====
function onHqButton(btn: string) {
  if (btn === sortLabel.value) {
    toggleSort()
    return
  }

  if (btn === '회수하기') {
    const ids = needSelection()
    if (!ids.length) return
    if (!confirm(ids.length + '개 DB를 회수하시겠습니까?')) return
    onConfirmRevoke(ids)
    return
  }

  if (btn === '이관하기') {
    if (transferDisabled.value) return

    const ids = needSelection('이관')
    if (!ids.length) return
    transferModal.value.open = true
  }
}
function onMgrButton(btn: string) {
  if (btn === sortLabel.value) {
    toggleSort();
    return;
  }
  if (btn === '회수하기') {
    const ids = needSelection()

    if (!ids.length) return
    if (!confirm(ids.length + "개 DB를 회수하시겠습니까?")) return

    onConfirmRevokeByMgr(ids)
  }
}

/*이관 실행*/
async function onConfirmTransfer(payload: {
  centerId?: number | null,
  userId?: number | null,
  assignToManagerAsStaff?: boolean
}) {
  const ids = needSelection('이관')
  if (!ids.length) return

  await runBusy(async () => {
    if (!payload.centerId) {
      alert('팀을 선택하세요.')
      return
    }
    if (!payload.userId) {
      alert('직원을 선택하세요.')
      return
    }

    await axios.post('/api/work/revoke/transfer', {
      customerIds: ids,
      targetCenterId: payload.centerId,
      targetUserId: payload.userId,
      assignToManagerAsStaff: payload.assignToManagerAsStaff || false,
    })

    transferModal.value.open = false
    selectedRows.value = []
    tableRef.value?.clearSelection?.()
    await refetchAndClamp()
  }).catch((e: any) => {
    console.error(e)
    alert(e?.response?.data || '이관 중 오류가 발생했습니다.')
  })
}

const remainingTransferCenterCount = ref(0)

async function loadRemainingTransferCenters() {
  if (!canTransfer.value) {
    remainingTransferCenterCount.value = 0
    return
  }

  try {
    const params = new URLSearchParams()
    excludedCenterIds.value.forEach((id) => {
      params.append('excludeCenterIds', String(id))
    })

    const qs = params.toString()
    const url = qs
        ? `/api/work/revoke/transfer/centers?${qs}`
        : '/api/work/revoke/transfer/centers'

    const { data } = await axios.get(url)
    remainingTransferCenterCount.value = Array.isArray(data) ? data.length : 0
  } catch (e) {
    console.error(e)
    remainingTransferCenterCount.value = 0
  }
}

watch(
    [excludedCenterIds, canTransfer],
    async () => {
      if (!canTransfer.value) {
        remainingTransferCenterCount.value = 0
        return
      }
      await loadRemainingTransferCenters()
    },
    { deep: true, immediate: true }
)

watch(
    role,
    async (r) => {
      if (['SUPERADMIN', 'CENTERHEAD', 'EXPERT'].includes(r)) {
        await loadRevokeTeams()
      } else {
        teamSelectOptions.value = ['팀 전체']
        teamNameToId.value = {}
      }
    },
    { immediate: true }
)

const transferDisabled = computed(() =>
    canTransfer.value &&
    selectedRows.value.length > 0 &&
    remainingTransferCenterCount.value === 0
)

const hqDisabledButtons = computed(() =>
    transferDisabled.value ? ['이관하기'] : []
)

/** 회수 실행 */
// 역할별 회수 엔드포인트만 추가
function getRevokeUrlByRole() {
  if (role.value === 'SUPERADMIN') return '/api/work/revoke/hq'
  if (role.value === 'CENTERHEAD') return '/api/work/revoke/centerhead'
  if (role.value === 'EXPERT') return '/api/work/revoke/expert'
  return '/api/work/revoke/hq'
}
async function onConfirmRevoke(ids: number[]) {
  return runBusy(async () => {
    try {
      await axios.post(getRevokeUrlByRole(), { customerIds: ids })

      // 선택 초기화
      selectedRows.value = []
      tableRef.value?.clearSelection?.()

      // 페이지 검사 후 새로고침
      await refetchAndClamp()
    } catch (e: any) {
      console.error(e)
      alert(e?.response?.data || '회수 중 오류가 발생했습니다.')
    }
  })
}

async function onConfirmRevokeByMgr(ids: number[]) {
  return runBusy(async () => {
    try {
      await axios.post('/api/work/revoke/manager', { customerIds: ids })

      // 선택 초기화
      selectedRows.value = []
      tableRef.value?.clearSelection?.()

      // 페이지 검사 후 새로고침
      await refetchAndClamp()
    } catch (e: any) {
      console.error(e)
      alert(e?.response?.data || '회수 중 오류가 발생했습니다.')
    }
  })
}

// 페이지 검사 후 새로고침
async function refetchAndClamp() {
  await fetchData();

  // 총 페이지 수가 줄어 현재 page가 범위를 넘으면 마지막 페이지로
  if (page.value > totalPages.value) {
    changePage(Math.max(1, totalPages.value));
    await fetchData();        // <- 바로 로드
    return;
  }

  // 총 페이지는 같지만 현 페이지 데이터가 0이면 한 페이지 앞으로
  if ((items.value?.length ?? 0) === 0 && page.value > 1) {
    changePage(page.value - 1);
    await fetchData();        // <- 바로 로드
  }
}

// 수동 새로고침
async function onRefresh() {
  if (isRefreshing.value) return
  isRefreshing.value = true
  try {
    await axios.post('/api/sheets/refresh?sid=1')
    await refetchAndClamp()   // 중복 fetch 방지 + 페이지 클램핑 일원화
  } catch (e) {
    console.error(e)
    alert('새로고침 중 오류가 발생했습니다.')
  } finally {
    isRefreshing.value = false
  }
}
</script>
