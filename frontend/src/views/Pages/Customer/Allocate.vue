<template>
  <AdminLayout>
    <PageBreadcrumb :pageTitle="pageTitle" />
    <div class="grid grid-cols-12 gap-4 min-w-0">
      <div class="col-span-12 space-y-6 min-w-0">

        <!-- SUPERADMIN (본사) -->
        <ComponentCard
            v-if="['SUPERADMIN', 'CENTERHEAD', 'EXPERT'].includes(role)"
            :selects="[[ '전체','최초','유효' ],
                       [ '직전상태 전체', '모든 부재', '부재1', '부재2', '부재3', '부재4', '부재5',
                            '기타', '결번', '재콜', '내방', '내방취소', '가망', '자연풀', '카피', '거절' ]]"
            :buttons="allocatorButtons"
            :showRefresh="true"
            :showBulkSelect="true"
            :selectionCount="selectionCount"
            :refreshing="isRefreshing"
            @refresh="onRefresh"
            @bulkSelect="onBulkSelect"
            @clearSelection="onClearSelection"
            @openSelection="selectionModalOpen = true"
            @changeSize="setSize"
            @selectChange="onHqSelectChange"
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
              @selectionCount="onSelectionCount"
              @buttonClick="onTableButtonClick"
              @changePage="changePage"
          />
        </ComponentCard>

        <!-- MANAGER (팀장) -->
        <ComponentCard
            v-else-if="role === 'MANAGER'"
            :selects="[[ '직전상태 전체', '모든 부재', '부재1', '부재2', '부재3', '부재4', '부재5',
                            '기타', '결번', '재콜', '내방', '내방취소', '가망', '자연풀', '카피', '거절' ]]"
            :buttons="mgrButtons"
            :showRefresh="true"
            :showBulkSelect="true"
            :selectionCount="selectionCount"
            :refreshing="isRefreshing"
            @refresh="onRefresh"
            @bulkSelect="onBulkSelect"
            @clearSelection="onClearSelection"
            @openSelection="selectionModalOpen = true"
            @changeSize="setSize"
            @selectChange="onMgrSelectChange"
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
              @selectionCount="onSelectionCount"
              @buttonClick="onTableButtonClick"
              @changePage="changePage"
          />
        </ComponentCard>

        <!--        &lt;!&ndash; STAFF 접근 X: 보안상 라우팅에서 막히겠지만 방어적으로 안내 &ndash;&gt;-->
        <!--        <div v-else class="p-6 rounded-xl border border-amber-200 bg-amber-50 text-amber-800 dark:border-amber-900/60 dark:bg-amber-950/30 dark:text-amber-200">-->
        <!--          이 페이지는 본사 또는 팀장만 사용할 수 있습니다.-->
        <!--        </div>-->

        <AllocateModal
            v-if="modal.open"
            :mode="modal.mode"
            :selected-count="selectionCount"
            :allow-multi="role === 'SUPERADMIN'"
            @close="closeModal"
            @confirm="onConfirmAllocate"
        >
          <template
              v-if="role === 'SUPERADMIN' && auth.grants.isSuper === true"
              #extra
          >
            <AllocateExpertDailySummary
                :customer-ids="selectedRows.map(row => row.id)"
            />
          </template>
        </AllocateModal>

        <ResetModal
          :is-open="resetModal.open"
          :customer-id="resetModal.targetId"
          @close="closeResetModal"
          @refresh="fetchData"
        />

        <!-- 선택한 대상 보기/개별 해제 모달 -->
        <SelectedRowsModal
            v-if="selectionModalOpen"
            title="선택한 고객"
            :rows="selectedRows"
            :columns="selectionColumns"
            @deselect="onDeselectRow"
            @clearAll="onClearSelection"
            @close="selectionModalOpen = false"
        />

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

</template>

<script setup lang="ts">
import {computed, onUnmounted, ref, watch} from 'vue'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import ComponentCard from '@/components/common/ComponentCard.vue'
import PsnsTable from '@/components/tables/basic-tables/PsnsTable.vue'
import AllocateModal from '@/components/ui/AllocateModal.vue'
import SelectedRowsModal from '@/components/ui/SelectedRowsModal.vue'
import AllocateExpertDailySummary from "@/components/ui/AllocateExpertDailySummary.vue";
import ResetModal from '@/components/ui/ResetModal.vue'
import { openMemoWindow, broadcastCustomerUpdated } from '@/composables/useMemoWindow.js'
import { EyeIcon } from '@heroicons/vue/24/outline'
import { ClipboardDocumentListIcon } from "@heroicons/vue/24/outline";
import { useAuthStore } from '@/stores/auth.js'
import { useTableQuery } from '@/composables/useTableQuery.js'
import { globalFilters } from '@/composables/globalFilters.js'
import axios from '@/plugins/axios.js'

const auth = useAuthStore()
const role = computed(() => auth.grants.role)
const pageTitle = ref('DB 분배하기')

// ===== 테이블 & 페이징 =====
const { items, page, size, totalPages, fetchData, changePage, setSize, setFilter, fetchTopN, loading: tableLoading } = useTableQuery({
  url: '/api/work/allocate/list',
  externalFilters: globalFilters,
  useExternalKeys: { from: 'dateFrom', to: 'dateTo', category: 'category', keyword: 'keyword', sort: 'sort', prevStatus: 'prevStatus', division: 'division' },
  mapper: (res:any) => ({ items: res.data.items, totalPages: res.data.totalPages, totalCount: res.data.totalCount })
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

// 헤더 공통 필터(검색어/카테고리/기간) 변경 시 선택 초기화 (맥락 변경)
// ※ 상태/직전상태/구분/정렬은 globalFilters가 아니라 내부 setFilter로 처리 → 여기서 감시하지 않음
watch(
  () => [globalFilters.keyword, globalFilters.category,
         globalFilters.dateFrom, globalFilters.dateTo],
  () => { onClearSelection() }
)

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

const tableRef = ref<any>(null)
const selectedRows = ref<any[]>([])
const onRowSelect = (rows:any[]) => { selectedRows.value = rows }

// 1~N 일괄선택 + 선택 카운터 (id 기반, 페이지 무관)
const selectionCount = ref(0)
function onSelectionCount(c:number) { selectionCount.value = c }
async function onBulkSelect(n:number) {
  if (!Number.isInteger(n) || n < 1) return  // 빈/유효하지 않은 입력 → 기존 선택 보존
  const rows = await fetchTopN(n)
  tableRef.value?.applyBulkSelect(rows)
}
function onClearSelection() { tableRef.value?.clearSelection() }

// 선택한 대상 보기/개별 해제 모달
const selectionModalOpen = ref(false)
const selectionColumns = [
  { key: 'name', label: '이름' },
  { key: 'phone', label: '전화번호' },
  { key: 'createdAt', label: '생성일', type: 'date' },
  { key: 'status', label: '상태', type: 'badge' },
  { key: 'prevStatus', label: '직전상태', type: 'badge' },
]
function onDeselectRow(row:any) { tableRef.value?.deselect(row) }

// '과거 이력' 파싱: 신형 'nameSTATUS'(레코드 구분 ) / 구형 ', ' 둘 다 처리.
// STATUS: 'C'=현재 담당자, 'P'=팀장풀, ''=과거
function parsePastStaff(v:any): { name:string, status:string }[] {
  const s = String(v ?? '')
  if (!s) return []
  const RS = '\n', US = '\t'
  const records = s.includes(RS) ? s.split(RS) : s.split(',')
  return records.map(r => {
    const t = r.trim()
    if (!t) return null
    if (t.includes(US)) {
      const [name, status] = t.split(US)
      return { name: (name ?? '').trim(), status: (status ?? '').trim() }
    }
    return { name: t, status: '' }
  }).filter(Boolean) as { name:string, status:string }[]
}

// HTML 이스케이프 (이름은 DB값이라 안전 처리)
function escapeHtml(s:any): string {
  return String(s).replace(/[&<>"']/g, (c:string) => (
    { '&':'&amp;', '<':'&lt;', '>':'&gt;', '"':'&quot;', "'":'&#39;' }[c] as string
  ))
}

// 과거 이력 셀 렌더: 현재 담당자=파랑, 팀장풀=초록, 그 외 과거=회색.
// PsnsTable 기본 text 렌더 안에서 내용만 교체 → 클릭 시 펼침/접힘 동작은 그대로 유지됨.
function renderPastStaff(value:any): string {
  const list = parsePastStaff(value)
  if (!list.length) return '<span class="text-gray-400 dark:text-gray-500">-</span>'
  const sep = '<span class="text-gray-400 dark:text-gray-600">, </span>'
  return list.map(it => {
    const cls = it.status === 'C' ? 'text-blue-400 dark:text-blue-300'
              : it.status === 'P' ? 'text-emerald-400 dark:text-emerald-300'
              : 'text-gray-500 dark:text-gray-400'
    return `<span class="${cls}">${escapeHtml(it.name)}</span>`
  }).join(sep)
}

// ===== 컬럼 (읽기 전용!) =====
const baseCols = [
  { key: 'createdAt', label: 'DB생성일', type: 'text' },
  // { key: 'category',  label: '카테고리', type: 'badge', options: ['주식','코인'] },
  { key: 'name',      label: '이름',     type: 'text' },
  { key: 'phone',     label: '전화번호', type: 'text', ellipsis: { width: 150 } },
  { key: 'source',    label: 'DB출처',   type: 'text' },
  { key: 'content',   label: '내용',     type: 'text', ellipsis: { width: 150 } },
]

// 본사(HQ)만 과거 이력 + 구분 노출
const hqColumns = [
  { key: 'createdAt', label: 'DB생성일', type: 'text' },
  { key: 'division',  label: '구분',     type: 'badge', options: ['최초','유효'] },
  // { key: 'category',  label: '카테고리', type: 'badge', options: ['주식','코인'] },
  { key: 'name',      label: '이름',     type: 'text' },
  { key: 'phone',     label: '전화번호', type: 'text', ellipsis: { width: 150 } },
  { key: 'source',    label: 'DB출처',   type: 'text' },
  { key: 'content',   label: '내용',     type: 'text', ellipsis: { width: 150 } },
  { key: "status", label: "상태", type: "badge",
    // 회수와 신규 상태는 수동으로 줄 수 없음
    // 회수 : DB회수하기 메뉴에서
    // 신규 : 한번도 분배가 되지 않은 항목만
    options: ["부재1","부재2","부재3","부재4","부재5","기타","결번","재콜","내방","내방취소","가망","자연풀","카피","거절"] },
  { key: 'prevStatus', label: '직전상태', type: 'badge', editable: false },
  { key: 'memo',    label: '메모', type: 'iconButton', icon: EyeIcon },
  { key: 'actions', label: '관리', type: 'iconButton', icon: ClipboardDocumentListIcon }, // HQ 전용
  { key: 'paststaff',     label: '과거 이력', type: 'text', ellipsis: { width: 200 }, render: renderPastStaff }, // HQ 전용
]

// 매니저는 과거 이력/구분 X (분배만)
const mgrColumns = [ ...baseCols,
  { key: 'prevStatus', label: '직전상태', type: 'badge', editable: false },
  { key: 'memo', label: '메모', type: 'iconButton', icon: EyeIcon },
  { key: 'paststaff', label: '과거 이력', type: 'text', ellipsis: { width: 200 }, render: renderPastStaff },
]

// ===== HQ 전용 구분 필터 =====
function onHqSelectChange({ idx, value }:{ idx:number, value:string }){
  return runBusy(async () => {if (idx === 0) {
    // 구분 필터
    setFilter('division', value === '전체' ? null : value)
  } else if (idx === 1) {
    // 직전상태 필터
    setFilter('prevStatus', value === '직전상태 전체' ? null : value)
  }
    await fetchData()
  })
}
// ===== MANAGER 전용 구분 필터 =====
function onMgrSelectChange({ idx, value }:{ idx:number, value:string }) {
  return runBusy(async () => {
    if (idx === 0) {
      // 직전상태 필터
      setFilter('prevStatus', value === '직전상태 전체' ? null : value)
      await fetchData()
    }
  })
}

function needSelection(): number[] {
  const ids = selectedRows.value.map((r:any) => r.id)
  if (!ids.length) alert('분배할 고객을 선택해주세요.')
  return ids
}

function needSelection_Reset(): number[] {
  const ids = selectedRows.value.map((r:any) => r.id)
  if (!ids.length) alert('초기화할 고객을 선택해주세요.')
  return ids
}

/* =============================
   정렬 버튼 관련
============================= */

// 정렬 상태 관리
// 1. 상태 변수 선언 (가장 먼저)
const viewOptions = ref({ oldest: false })

// 2. 버튼 라벨 동적 계산 (viewOptions 사용)
const sortLabel = computed(() => viewOptions.value.oldest ? '최신순 보기' : '과거순 보기')

// 3. 버튼 배열 생성 (sortLabel 사용)
const allocatorButtons = computed(() => {
  const btns = [sortLabel.value, '분배하기']
  // 이력초기화는 오직 본사(SUPERADMIN)만 가능
  if (role.value === 'SUPERADMIN') {
    btns.push('이력초기화')
  }
  return btns
})
const mgrButtons = computed(() => [sortLabel.value, '분배하기'])

// 공통 정렬 토글 로직
async function toggleSort() {
  await runBusy(async () => {
    // 1. 상태 반전
    viewOptions.value.oldest = !viewOptions.value.oldest

    // 2. 필터 설정 (백엔드는 'oldest' 값이 오면 과거순, 없으면 최신순으로 처리)
    setFilter('sort', viewOptions.value.oldest ? 'oldest' : null)

    // 정렬은 같은 데이터의 순서만 바뀜 → id 기반 선택은 그대로 유지(초기화 안 함)
  })
}

// ===== 메모 모달 관련 =====
// 분배 화면 메모는 읽기전용 → 별도 창을 view 모드(?view=1)로 연다
function openMemo(row: any) {
  // ⚠️ 클릭 핸들러에서 동기 호출(팝업 차단 회피)
  openMemoWindow(row.customer_id || row.id, { view: true })
}

// ===== 모달 열기/닫기 =====
const modal = ref<{ open:boolean, mode:'HQ'|'MANAGER' }>({ open:false, mode: 'HQ' })
const resetModal = ref<{ open: boolean, targetId: number|null }>({ open: false, targetId: null })
function onHqButton(btn:string){
  // 현재 떠있는 정렬 버튼 텍스트와 같으면 토글 실행
  if (btn === sortLabel.value) {
    toggleSort();
    return;
  }
  if (btn==='분배하기'){
    if (!needSelection().length) return;
    modal.value={open:true, mode:'HQ'}
  }
  if (btn === '이력초기화') {
    resetHistoryBulk();
    return;
  }
}
function onMgrButton(btn:string){
  // 현재 떠있는 정렬 버튼 텍스트와 같으면 토글 실행
  if (btn === sortLabel.value) {
    toggleSort();
    return;
  }
  if (btn==='분배하기'){
    if (!needSelection().length) return;
    modal.value={open:true, mode:'MANAGER'}
  }
}

// 개별 모달 열기
function onTableButtonClick(row: any, key: string) {
  if (key === 'memo') {
    openMemo(row)
    return
  }

  // 컬럼 정의에서 설정한 key가 'actions'일 때 동작
  if (key === 'actions') {
    if (role.value !== 'SUPERADMIN') {
      alert('권한이 없습니다.')
      return
    }

    const id = row.customer_id || row.id
    resetModal.value = { open: true, targetId: id }
  }
}
function closeResetModal() {
  resetModal.value.open = false
  resetModal.value.targetId = null
}
function closeModal(){
  modal.value.open = false
}

// ===== 실제 분배 호출 =====
async function onConfirmAllocate(payload: {
  centerId?:number|null,
  userIds:number[],
  assignToManagerAsStaff?: boolean
}) {
  const ids = needSelection();
  if (!ids.length) return

  await runBusy(async () => {
    const userIds = payload.userIds || []

    // 1. 유효성 검사
    // 상위 분배자는 팀/직원 모두 필수
    if (modal.value.mode === 'HQ') {
      if (!payload.centerId) { alert('팀을 선택하세요.'); return; }
      if (!userIds.length) { alert('직원을 선택하세요.'); return; }
    } else {
      // 매니저는 직원 필수
      if (!userIds.length) { alert('직원을 선택하세요.'); return; }
    }

    // 2. API 호출 분기
    if (role.value === 'SUPERADMIN') {
      // [본사] 여러 명 선택 시 랜덤 균등분배, 한 명이면 기존 단일 분배
      if (userIds.length > 1) {
        await axios.post('/api/work/allocate/hq/random', {
          customerIds: ids,
          targetCenterId: payload.centerId,
          targetUserIds: userIds,
          assignToManagerAsStaff: payload.assignToManagerAsStaff || false
        })
      } else {
        await axios.post('/api/work/allocate/hq', {
          customerIds: ids,
          targetCenterId: payload.centerId,
          targetUserId: userIds[0],
          assignToManagerAsStaff: payload.assignToManagerAsStaff || false
        })
      }
    } else if (role.value === 'CENTERHEAD' || role.value === 'EXPERT') {
      // [센터장/전문가]
      await axios.post('/api/work/allocate/experthead', {
        customerIds: ids,
        targetCenterId: payload.centerId,
        targetUserId: userIds[0],
        assignToManagerAsStaff: payload.assignToManagerAsStaff || false
      })
    } else {
      // [팀장]
      await axios.post('/api/work/allocate/manager', {
        customerIds: ids,
        targetUserId: userIds[0]
      })
    }

    // alert(`분배 완료: ${ids.length}건`)
    modal.value.open = false
    selectedRows.value = []
    tableRef.value?.clearSelection?.()

    // 페이지 검사하며 새로고침
    await refetchAndClamp()

    // 분배된 고객의 메모 팝업(열려있다면) 담당자·권한 즉시 갱신(같은 브라우저)
    ids.forEach((id:any) => broadcastCustomerUpdated(id))
  }).catch((e:any) => {
    console.error(e)
    alert(e?.response?.data || '분배 중 오류가 발생했습니다.')
  })
}

// 일괄 이력 초기화 (본사 전용)
async function resetHistoryBulk() {
  const ids = needSelection_Reset();
  if (!ids.length) return;

  if (!confirm(`선택한 ${ids.length}건에 대해 현재 담당자를 제외한 모든 이력을 초기화하시겠습니까?`)) return;

  await runBusy(async () => {
    try {
      await axios.post('/api/work/history/reset/bulk', ids)
      await fetchData()
      selectedRows.value = []
      tableRef.value?.clearSelection?.()
    } catch (e: any) {
      console.error(e)
      alert(e?.response?.data || '오류 발생')
    }
  })
}

// 페이지 검사하며 새로고침
async function refetchAndClamp() {
  await fetchData();

  // 총 페이지 수가 줄어 현재 page가 범위를 넘으면 마지막 페이지로
  if (page.value > totalPages.value) {
    changePage(Math.max(1, totalPages.value)); // changePage가 알아서 fetch 호출
    return;
  }

  // 총 페이지 값은 맞지만 현 페이지 데이터가 0개면 한 페이지 앞으로
  if ((items.value?.length ?? 0) === 0 && page.value > 1) {
    changePage(page.value - 1);
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