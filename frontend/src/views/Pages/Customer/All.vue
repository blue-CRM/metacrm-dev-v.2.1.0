<template>
  <AdminLayout>
    <PageBreadcrumb :pageTitle="currentPageTitle" />
    <div class="grid grid-cols-12 gap-4 min-w-0">
      <div class="col-span-12 space-y-6 min-w-0">

        <!-- SUPERADMIN (본사) -->
        <ComponentCard
            v-if="isAdminView"
            :selects="adminSelects"
            :buttons="adminButtons"
            :active="activeLabels"
            :showRefresh="true"
            :refreshing="isRefreshing"
            @refresh="onRefresh"
            @changeSize="setSize"
            @selectChange="onAdminSelectChange"
            @buttonClick="onCommonButtonClick"
        >
          <PsnsTable
              :key="tableKey"
              ref="tableRef"
              :columns="adminColumns"
              :data="items"
              :showCheckbox="true"
              :rowSelectable="isRowSelectable"
              :page="page"
              :totalPages="totalPages"
              :page-size="size"
              :loading="busy"
              @rowSelect="onRowSelect"
              @badgeUpdate="onBadgeUpdate"
              @DateUpdate="onDateUpdate"
              @salesUpdate="onSalesUpdate"
              @textUpdate="onTextUpdate"
              @visitSchedule="onVisitSchedule"
              @buttonClick="onTableButtonClick"
              @changePage="changePage"
          />
        </ComponentCard>

        <!-- MANAGER / STAFF -->
        <ComponentCard
            v-else
            :selects="managerSelects"
            :buttons="managerButtons"
            :active="activeLabels"
            :showRefresh="true"
            :refreshing="isRefreshing"
            @refresh="onRefresh"
            @changeSize="setSize"
            @selectChange="onManagerStatusSelect"
            @buttonClick="onCommonButtonClick"
        >
          <PsnsTable
              :key="tableKey"
              ref="tableRef"
              :columns="commonColumns"
              :data="items"
              :showCheckbox="true"
              :rowSelectable="isRowSelectable"
              :page="page"
              :totalPages="totalPages"
              :page-size="size"
              :loading="busy"
              @rowSelect="onRowSelect"
              @badgeUpdate="onBadgeUpdate"
              @DateUpdate="onDateUpdate"
              @salesUpdate="onSalesUpdate"
              @visitSchedule="onVisitSchedule"
              @buttonClick="onTableButtonClick"
              @changePage="changePage"
          />
        </ComponentCard>

        <Teleport to="body">
          <Memo
              v-if="memoOpen"
              :row="memoRow"
              :readOnly="memoReadOnly"
              @close="closeMemo"
              @saved="onMemoSaved"
          />
        </Teleport>

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

<script setup>
import {ref, computed, onUnmounted, watch, onMounted} from "vue";
import { useAuthStore } from "@/stores/auth.js";
import PageBreadcrumb from "@/components/common/PageBreadcrumb.vue";
import AdminLayout from "@/components/layout/AdminLayout.vue";
import ComponentCard from "@/components/common/ComponentCard.vue";
import PsnsTable from "@/components/tables/basic-tables/PsnsTable.vue";
import Memo from "@/components/ui/MEMO.vue";
import { EyeIcon } from "@heroicons/vue/24/outline";
import { useTableQuery } from "@/composables/useTableQuery.js";
import { globalFilters } from "@/composables/globalFilters.js";
import axios from "@/plugins/axios.js"

const auth = useAuthStore();
const role = computed(() => auth.grants.role)
const isManager = computed(() => role.value === 'MANAGER');
const memoReadOnly = computed(() => ['CENTERHEAD', 'EXPERT'].includes(role.value))
const mineOnly = ref(false);

const currentPageTitle = ref("전체 고객DB관리");
const tableKey = ref(0); // 선택 초기화 강제 리렌더용
const tableRef = ref(null); // PsnsTable 메서드 접근
const selectedRows = ref([]); // 선택된 행 캐시
const memoOpen = ref(false); // 메모 모달 상태
const memoRow = ref(null); // 메모 모달에 넘길 행
const isRefreshing = ref(false); // 새로고침 스피너/비활성

// 수동 새로고침
async function onRefresh() {
  if (isRefreshing.value) return;
  isRefreshing.value = true;
  try {
    // 수동 새로고침 (sid 기본값 1)
    const { data } = await axios.post('/api/sheets/refresh?sid=1');
    // 선택 초기화는 유지하고 싶으면 주석 해제
    // selectedRows.value = [];
    // tableRef.value?.clearSelection?.();

    // 테이블 데이터 재조회
    // 페이지 검사하며 새로고침
    await refetchAndClamp();

    // (선택) 서버 reason에 따라 토스트/알림
    // if (data?.reason === 'debounced') alert('조금 뒤에 다시 시도해주세요.');
  } catch (err) {
    console.error('수동 새로고침 실패', err);
    alert('새로고침 중 오류가 발생했습니다.');
  } finally {
    isRefreshing.value = false;
  }
}

/* =============================
   공통 useTableQuery
============================= */
const {
  items,
  page,
  size,
  totalPages,
  loading: tableLoading,
  fetchData,
  changePage,
  setSize,
  setFilter
} = useTableQuery({
  url: "/api/work/db", // 공통 API
  externalFilters: globalFilters,
  useExternalKeys: {
    from: "dateFrom",
    to: "dateTo",
    category: "category",
    keyword: "keyword",
    sort: "sort",
    mine: "mine",
    staffUserId: "staffUserId",
    status: "status",
    prevStatus: "prevStatus",
    division: "division",
    expertName: "expertName"
  },
  mapper: (res) => {
    const raw = res?.data?.items ?? [];

    return {
      items: raw.map(r => ({
        ...r,
        staffView: r.status === '없음' ? '' : (r.staff ?? ''),
        reservation: (r.status === '내방' && typeof r.reservation === 'string')
            ? r.reservation.replace(/\n+/g, ' / ')
            : r.reservation,
      })),
      totalPages: res?.data?.totalPages ?? 1,
      totalCount: res?.data?.totalCount ?? raw.length
    };
  }
});

// 본사, 센터장, 전문가는 '관리자형 뷰' (구분 칼럼 O, 매출 칼럼 O)
const isAdminView = computed(() => {
  return ['SUPERADMIN', 'CENTERHEAD', 'EXPERT'].includes(role.value);
});

// 로딩 오버레이 설정
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

/** 체크박스 활성화 조건: '중복'만 선택 가능 (관리자 전용 화면), 유효/최초는 비활성 */
function isRowSelectable(row) {
  return row.origin === 'DUPLICATE';
}

/** 중복이면 모든 편집 비활성 (배지/날짜 등) */
function notDuplicate(row) {
  return row.origin !== 'DUPLICATE';
}

// 매출(최초/업셀) 칼럼 편집 가능 여부 (자연풀 or 카피 상태만)
function isSalesEditable(row) {
  return row.origin !== 'DUPLICATE' && ['자연풀', '카피'].includes(row.status);
}

function isNameEditable(row) {
  return role.value === 'SUPERADMIN' && row.origin !== 'DUPLICATE';
}

/* =============================
   SUPERADMIN 전용 컬럼
============================= */
const adminColumns = [
  { key: "createdAt", label: "DB생성일", type: "text" },
  { key: "staffView", label: "프로", type: "text" },
  { key: "division", label: "구분", type: "badge", options: ["최초", "중복", "유효"] },
  { key: "",  label: "",   type: "text", ellipsis: { width: 5 } },
  // { key: "category", label: "카테고리", type: "badge", options: ["주식", "코인"] },
  { key: "name", label: "이름", type: "text", textEditable: isNameEditable },
  { key: "phone", label: "전화번호", type: "text", ellipsis: { width: 150 } },
  { key: "source", label: "DB출처", type: "text", ellipsis: { width: 100 } },
  { key: "content", label: "내용", type: "text", ellipsis: { width: 150 } },
  { key: "memo", label: "메모", type: "iconButton", icon: EyeIcon, disabled: (row)=> row.origin==='DUPLICATE' },
  { key: "status", label: "상태", type: "badge",
      editable: notDuplicate,
      // 회수와 신규 상태는 수동으로 줄 수 없음
      // 회수 : 팀장풀 혹은 개인에게 분배된 이후 회수된 데이터
      // 신규 : 최초의 확정분배 시에만 신규
      options: ["부재1","부재2","부재3","부재4","부재5","기타","결번","재콜","내방","내방취소","가망","자연풀","카피","거절"] },
  { key: "prevStatus", label: "직전상태", type: "badge", editable: false},
  { key: "reservation", label: "예약", type: "date", editable: notDuplicate },
  { key: "initialPrice", label: "최초(달러)", type: "money", editable: isSalesEditable },
  { key: "upsellPrice",  label: "업셀(달러)", type: "money", editable: isSalesEditable },
];

/* =============================
   MANAGER / STAFF 전용 컬럼
============================= */
const commonColumns = [
  { key: "createdAt", label: "DB생성일", type: "text" },
  { key: "",  label: "",   type: "text", ellipsis: { width: 5 } },
  { key: "staffView", label: "프로", type: "text" },
  // { key: "category", label: "카테고리", type: "badge", options: ["주식", "코인"] },
  { key: "name", label: "이름", type: "text"},
  { key: "phone", label: "전화번호", type: "text", ellipsis: { width: 150 } },
  { key: "source", label: "DB출처", type: "text", ellipsis: { width: 100 } },
  { key: "content", label: "내용", type: "text", ellipsis: { width: 150 } },
  { key: "memo", label: "메모", type: "iconButton", icon: EyeIcon, disabled: (row)=> row.origin==='DUPLICATE' },
  { key: "status", label: "상태", type: "badge",
      editable: notDuplicate,
      options: ["부재1","부재2","부재3","부재4","부재5","기타","결번","재콜","내방","내방취소","가망","자연풀","카피","거절"] },
  { key: "prevStatus", label: "직전상태", type: "badge", editable: false},
  { key: "reservation", label: "예약", type: "date", editable: notDuplicate },
  { key: "initialPrice", label: "최초(달러)", type: "money", editable: isSalesEditable },
  { key: "upsellPrice",  label: "업셀(달러)", type: "money", editable: isSalesEditable },
];

/* =============================
   이벤트 핸들러
============================= */
function onRowSelect(rows) {
  selectedRows.value = rows;
  // console.log("선택 행:", rows);
}

function onBadgeUpdate(row, key, newValue) {
  if (row.origin === 'DUPLICATE') {
    alert('중복 DB는 수정할 수 없습니다.');
    return;
  }

  // 상태 배지만 서버 반영 (필요시 division 등 확장)
  if (key === 'status') {
    axios.patch(`/api/work/db/all/update/${row.id}`, {
      field: key,
      value: newValue
    }).then(() => {
      // 다른상태 -> 내방으로 바뀌는 순간 예약시간 null 처리와 UI 동기화
      if (newValue === '내방') {
        row.reservation = null;
      }
    }).catch(err => {
      const msg =
          err?.response?.data?.message
          || err?.response?.data
          || '상태 변경 중 오류가 발생했습니다.'

      alert(msg)
    });
  }

  // console.log("배지 수정:", row, key, newValue);
}

// 재콜 : 예약일 저장
async function onDateUpdate(row, key, newValue) {
  // 중복DB는 클릭 불가
  if (row.origin === 'DUPLICATE') {
    return;
  }

  try {
    await axios.patch(`/api/work/db/all/update/${row.id}`, {
      field: key,       // "reservation"
      value: newValue   // 날짜 값
    })
    // console.log("예약일 저장 성공:", row.name, newValue)
  } catch (err) {
    console.error("예약일 저장 실패", err)
    alert("예약일 저장 중 오류가 발생했습니다.")
  }
}

// 내방 : 예약일을 새탭에서 달력 통해서 저장
function onVisitSchedule(row) {
  if (row.origin === 'DUPLICATE') {
    alert('중복 DB는 내방일정을 등록할 수 없습니다.');
    return;
  }

  const query = new URLSearchParams({
    customerId: row.id
  }).toString();

  window.open(`/visit-calendar?${query}`, '_blank');
}

// 최초/업셀 매출 금액 저장 핸들러
async function onSalesUpdate(row, key, newValue) {
  // 1. 편집 가능 상태인지 재확인
  if (!isSalesEditable(row)) return;

// 2. 유효성 검사: 값이 없거나 숫자가 아니면 저장하지 않고 return
  if (newValue === '' || newValue === null || isNaN(newValue)) {
    // alert("숫자만 입력해주세요.");
    return;
  }

  // 3. 정수로 변환
  const amount = parseInt(newValue);

  // 4. API 호출 타입 결정
  const type = key === 'initialPrice' ? 'INITIAL' : 'UPSELL';

  try {
    await axios.post(`/api/work/db/sales`, {
      customerId: row.id,
      type: type,
      amount: amount
    });

    // 성공 시 로컬 데이터 업데이트
    row[key] = amount;
    console.log("매출 저장 성공:", type, amount);

  } catch (err) {
    console.error("매출 저장 실패", err);
    alert("매출 금액 저장 중 오류가 발생했습니다.");
    // 에러나면 원래 값으로 되돌리거나 새로고침 하는 로직 필요시 추가
  }
}

// 고객 이름 변경
async function onTextUpdate(row, key, newValue) {
  if (key !== 'name') return;

  if (role.value !== 'SUPERADMIN') {
    alert('권한이 없습니다.');
    return;
  }

  if (row.origin === 'DUPLICATE') {
    alert('중복 DB는 이름 수정이 불가합니다.');
    return;
  }

  const nextName = (newValue ?? '').trim();
  if (!nextName) {
    alert('이름을 입력해주세요.');
    return;
  }

  try {
    await axios.patch(`/api/work/db/all/update/${row.id}`, {
      field: key,
      value: nextName
    });

    row.name = nextName;
  } catch (err) {
    console.error("이름 수정 실패", err);

    const msg =
        err?.response?.data?.message
        || err?.response?.data
        || '이름 수정 중 오류가 발생했습니다.';

    alert(msg);
  }
}

// 메모 아이콘 클릭 -> 모달 오픈
function onTableButtonClick(row, key) {
  if (key !== 'memo') return;
  if (row.origin === 'DUPLICATE') {
    alert('중복 DB는 메모 수정이 불가합니다.');
    return;
  }

  memoRow.value = row;
  memoOpen.value = true;
}

function closeMemo() {
  memoOpen.value = false;
  memoRow.value = null;
}

/* =============================
   드롭박스 필터 관련
============================= */

// 1. 정적 옵션 정의 (변하지 않는 값)
const divisionOptions = ['구분 전체', '최초', '유효', '중복'];
const statusOptions = [
  '상태 전체', '모든 부재', '부재1', '부재2', '부재3', '부재4', '부재5',
  '기타', '결번', '재콜', '내방', '내방취소', '신규', '가망', '자연풀', '카피', '거절', '없음', '회수'
];
const prevStatusOptions = [
  '직전상태 전체', '모든 부재', '부재1', '부재2', '부재3', '부재4', '부재5',
  '기타', '결번', '재콜', '내방', '내방취소', '가망', '자연풀', '카피', '거절',
];

// 2. 동적 옵션 정의 (서버에서 가져올 전문가 리스트)
const expertOptions = ref(['전문가 전체']); // 기본값 설정

// 3. onMounted에서 전문가 리스트 로드

// 4. 권한별 최종 Select 배열 생성 (Computed)
const adminSelects = computed(() => {
  // 기본: [0:구분, 1:상태]
  const base = [divisionOptions, statusOptions, prevStatusOptions];

  // 전문가(EXPERT)가 아닐 때만 '전문가 선택' 드롭박스 추가
  if (role.value !== 'EXPERT') {
    base.push(expertOptions.value); // 2:전문가
  }

  return base;
});
const managerSelects = computed(() => {
  // [0:상태, 1:전문가] (매니저는 구분 없음)
  return [statusOptions, prevStatusOptions, expertOptions.value];
});

// 필터 동작
function onAdminSelectChange({ idx, value }) {
  return runBusy(async () => {
    // idx 순서: 0=구분, 1=상태, 2=전문가
    if (idx === 0) {
      // '구분 전체'면 해제
      setFilter("division", value === "구분 전체" ? null : value);
    } else if (idx === 1) {
      // '상태 전체'면 해제
      setFilter("status", value === "상태 전체" ? null : value);
    }  else if (idx === 2) {
      // '직전상태 전체'면 해제
      setFilter("prevStatus", value === "직전상태 전체" ? null : value);
    } else if (idx === 3) {
      // '전문가 전체'면 해제 (EXPERT/전문가 권한은 해당사항 없음)
      setFilter("expertName", value === "전문가 전체" ? null : value);
    }
  })
}
function onManagerStatusSelect({ idx, value }) {
  return runBusy(async () => {
    // idx 순서: 0=상태, 1=전문가 (매니저는 구분 필터 없음)
    if (idx === 0) {
      // '상태 전체'면 해제
      setFilter("status", value === "상태 전체" ? null : value);
    } else if (idx === 1) {
      // '직전상태 전체'면 해제
      setFilter("prevStatus", value === "직전상태 전체" ? null : value);
    } else if (idx === 2) {
      // '전문가 전체'면 해제
      setFilter("expertName", value === "전문가 전체" ? null : value);
    }
  })
}

/* =============================
   정렬 버튼 관련
============================= */

// 뷰 옵션 상태 관리
const viewOptions = ref({
  status: false, // 상태별 보기
  oldest: false, // 과거순 보기
  mine: false    // 팀 DB 보기 (Manager 전용)
});

// 활성화된 버튼 라벨 계산
const activeLabels = computed(() => {
  const arr = [];
  if (viewOptions.value.status) arr.push('상태별 보기');
  if (viewOptions.value.oldest) arr.push('과거순 보기');
  if (!viewOptions.value.mine)   arr.push('팀 DB 보기');
  return arr;
});

// 버튼 목록 정의
const adminButtons = ['상태별 보기', '과거순 보기', '중복DB로 이동'];
const managerButtons = computed(() => {
  const arr = ['상태별 보기', '과거순 보기'];
  if (isManager.value) arr.push('팀 DB 보기'); // 매니저만 추가
  return arr;
});

// 버튼 동작
async function onCommonButtonClick(btn) {
  // busy 가드
  if (uiLoading.value) return;
  uiLoading.value = true;

  try {
    // 1) 먼저 중복 이동 처리
    if (btn === "중복DB로 이동") {
      const dupIds = selectedRows.value
          .filter(r => r.origin === 'DUPLICATE')
          .map(r => r.id);

      if (dupIds.length === 0) {
        alert("중복 항목만 선택해서 이동할 수 있습니다.");
        return;
      }

      try {
        await axios.post("/api/work/db/duplicate/hide", { ids: dupIds });
        alert(`중복 ${dupIds.length}건을 중복DB 메뉴로 이동(숨김)했습니다.`);

        // 선택 초기화
        selectedRows.value = [];
        tableRef.value?.clearSelection?.();
        tableKey.value++;

        // 페이지 검사하며 새로고침
        await refetchAndClamp();
      } catch (err) {
        console.error("중복 이동 실패", err);
        alert("중복 이동 중 오류가 발생했습니다.");
      }
      return; // 조기 종료
    }

    // 2) 상태/구분 토글
    if (btn === "상태별 보기") viewOptions.value.status = !viewOptions.value.status;
    if (btn === "과거순 보기") viewOptions.value.oldest = !viewOptions.value.oldest;
    if (btn === "팀 DB 보기") viewOptions.value.mine = !viewOptions.value.mine;

    // 3) sort 조합
    const sortParts = [];
    if (viewOptions.value.status) sortParts.push("status");
    if (viewOptions.value.oldest) sortParts.push('oldest');
    setFilter("sort", sortParts.length ? sortParts.join(",") : null);

    // 3-1) Mine 파라미터 조합 (Manager만 해당)
    if (isManager.value) {
      setFilter("mine", viewOptions.value.mine ? "Y" : null);
      setFilter("staffUserId", null);
    }

    // 선택 초기화
    selectedRows.value = [];
    tableRef.value?.clearSelection?.();
    tableKey.value++; // 테이블 강제 리렌더

  } finally {
    uiLoading.value = false;
  }
}

// 모달 갱신
function onMemoSaved(patch) {
  const arr = items.value ?? [];
  const idx = arr.findIndex(r => r.id === patch.id);
  if (idx !== -1) {
    const cur = arr[idx];
    const next = { ...cur };

    if ('status' in patch) next.status = patch.status;
    if ('reservation' in patch) next.reservation = patch.reservation;
    if ('initialPrice' in patch) next.initialPrice = patch.initialPrice;
    if ('upsellPrice' in patch) next.upsellPrice = patch.upsellPrice;

    arr.splice(idx, 1, next);
  } else {
    // 현재 페이지에 행이 없을 때만 안전 재조회 (페이지 유지)
    const curPage = page.value;
    fetchData().then(() => { if (page.value !== curPage) changePage(curPage); });
  }
}

// 페이지 방어
async function refetchAndClamp() {
  await fetchData();

  // 총 페이지가 줄어들어 현재 페이지가 범위를 넘은 경우 → 마지막 페이지로 이동
  if (page.value > totalPages.value) {
    changePage(Math.max(1, totalPages.value)); // watch가 트리거되어 fetch 자동 호출
    return;
  }

  // 방어: 총 페이지 값은 맞지만, 현재 페이지에 레코드가 0개면 이전 페이지로 한 칸
  if ((items.value?.length ?? 0) === 0 && page.value > 1) {
    changePage(page.value - 1);
  }
}

onMounted(async () => {
  // 매니저 초기 세팅
  if (isManager.value) {
    viewOptions.value.mine = true;
    setFilter('mine', 'y');
    setFilter('staffUserId', null);
    if (page.value !== 1) changePage(1);
  }

  // 전문가 권한이 아닐 때만 전문가 리스트 API 호출
  if (role.value !== 'EXPERT') {
    try {
      // 전문가 리스트 조회
      const {data} = await axios.get('/api/work/db/experts');
      // console.log(data)

      // 이름만 추출하여 배열 생성
      const names = data.map(expert => expert.expertName);

      // '전문가 전체' 뒤에 붙이기
      expertOptions.value = ['전문가 전체', ...names];
    } catch (err) {
      console.error("전문가 리스트 로딩 실패", err);
    }
  }
});

onUnmounted(() => {
  globalFilters.mine = null;
  globalFilters.staffUserId = null;
});
</script>
