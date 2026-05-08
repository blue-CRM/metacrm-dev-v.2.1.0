<template>
  <AdminLayout>
    <PageBreadcrumb :pageTitle="currentPageTitle" />
    <div class="grid grid-cols-12 gap-4 min-w-0">
      <div class="col-span-12 space-y-6 min-w-0">
        <ComponentCard
            :buttons="['일괄승인']"
            :showRefresh="true"
            :refreshing="isRefreshing"
            @refresh="onRefresh"
            @changeSize="val => setSize(val)"
            @buttonClick="onButtonClick">
          <PsnsTable
              ref="tableRef"
              :columns="columns"
              :data="items"
              :showCheckbox="true"
              :rowSelectable="isRowSelectable"
              :page="page"
              :totalPages="totalPages"
              :page-size="size"
              :loading="busy"
              @rowSelect="onRowSelect"
              @badgeUpdate="onBadgeUpdate"
              @changePage="changePage"
          />
        </ComponentCard>
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
import {ref, computed, onMounted, watch, onUnmounted} from "vue";
import PageBreadcrumb from "@/components/common/PageBreadcrumb.vue";
import AdminLayout from "@/components/layout/AdminLayout.vue";
import ComponentCard from "@/components/common/ComponentCard.vue";
import PsnsTable from "@/components/tables/basic-tables/PsnsTable.vue";
import axios from "@/plugins/axios.js";
import {useAuthStore} from "@/stores/auth.js";

// 로그인 관리
const auth = useAuthStore()

// 공용 쿼리 컴포저블
import { useTableQuery } from "@/composables/useTableQuery.js";
import { globalFilters } from "@/composables/globalFilters.js";

const tableRef = ref(null)
const currentPageTitle = ref("직원관리")
const selectedRows = ref([])
const isRefreshing = ref(false); // 새로고침 스피너/비활성

async function onRefresh() {
  if (isRefreshing.value) return;
  isRefreshing.value = true;
  try {
    // 단순히 테이블 데이터만 다시 조회
    await fetchData();
  } catch (err) {
    console.error('테이블 새로고침 실패', err);
    alert('새로고침 중 오류가 발생했습니다.');
  } finally {
    isRefreshing.value = false;
  }
}

// DB에서 전문가 목록 불러오기
const expertOptions = ref([]) // [{ expertId, expertName }]
onMounted(async () => {
  try {
    if (!auth.grants.isSuper) {
      expertOptions.value = []
      return
    }

    const res = await axios.get("/api/me/experts")
    // 기대 응답: [{ expertId, expertName, ... }, ...]
    expertOptions.value = res.data.map(e => ({
      expertId: e.expertId,
      expertName: e.expertName
    }))
  } catch (err) {
    console.error("전문가 목록 불러오기 실패:", err.response?.data || err.message)
    expertOptions.value = []
  }
})

// DB에서 centers 불러오기
const fullCenterList = ref([]); // { centerId, centerName, branchId } 객체 배열

// 드롭다운용 분리된 목록
const centerUnitOptions = ref([]); // 센터장/전문가용 (지점)
const teamUnitOptions = ref([]);   // 팀장/프로용 (팀)
onMounted(async () => {
  try {
    const res = await axios.get("/api/super/users/centers");

    // 로직 판단을 위해 전체 데이터 저장
    // console.log(res)
    fullCenterList.value = res.data;

    // 테이블 드롭다운에는 이름만 전달
    // 1. 센터(지점) 목록: 본사(branch_id=1) 소속이면서, ID가 1(본사)이 아닌 것
    centerUnitOptions.value = res.data
        .filter(c => c.branchId === 1 && c.centerId !== 1)
        .map(c => c.centerName);
    // console.log(centerUnitOptions.value);

    // 2. 팀 목록: 본사(branch_id=1) 소속이 아닌 것
    teamUnitOptions.value = res.data
        .filter(c => c.branchId !== 1)
        .map(c => c.centerName);
    // console.log(teamUnitOptions.value);
  } catch (err) {
    console.error("센터 목록 불러오기 실패:", err.response?.data || err.message);
    fullCenterList.value = []
    centerUnitOptions.value = []
    teamUnitOptions.value = []
  }
});

// 팀/센터 구분 헬퍼 함수
// 팀: branch_id != 1
function isTeamUnit(centerName) {
  const target = fullCenterList.value.find(c => c.centerName === centerName);
  // target이 없거나, branchId가 1이 아니면 팀으로 간주 (본사 예외 처리 필요 시 추가)
  return target && target.branchId !== 1;
}
// 센터: branch_id == 1 && center_id != 1
function isCenterUnit(centerName) {
  const target = fullCenterList.value.find(c => c.centerName === centerName);
  return target && target.branchId === 1 && target.centerId !== 1;
}

// 테이블 데이터 훅 연결
const {
  items,       // 데이터 rows
  page,        // 현재 페이지 (ref)
  size,        // 페이지당 갯수 (ref)
  totalPages,  // 총 페이지 수 (ref)
  changePage,  // 페이지 변경 함수
  setSize,     // 표 사이즈 결정 함수
  fetchData,   // 표 새로고침
  loading: tableLoading,     // 로딩 상태
  error        // 에러 상태
} = useTableQuery({
  url: "/api/super/users", // 실제 API 엔드포인트
  externalFilters: globalFilters, // Header.vue에서 keyword 값 반영됨
  useExternalKeys: { keyword: "keyword" }, // 딱 검색어만 API 파라미터로 넘김
  mapper: (res) => {
    // console.log("서버에서 받은 원본 데이터(첫번째 줄):", res.data.items[0]);
    // API 응답 매핑 (백엔드 형식 맞게 조정 필요)
    return {
      items: res.data.items.map(u => ({
        userId: u.userId,
        type:
            u.userRole === "SUPERADMIN" ? "관리자" :
            u.userRole === "MANAGER"    ? "팀장" :
            u.userRole === "STAFF"      ? "프로" :
            u.userRole === "CENTERHEAD" ? "센터장" :
            u.userRole === "EXPERT"     ? "전문가" : u.userRole,
        name: u.userName,
        phone: u.userPhone,
        email: u.userEmail,
        center: u.centerName || '미할당',
        requestStatus:
            u.userApproved === 'Y' ? '승인' :
            u.userApproved === 'N' ? '대기' :
            u.userApproved === 'X' ? '탈퇴' : u.userApproved,
        visible: u.managerPhoneAccess === 'N' ? '차단' : '공개',
        allocate: u.canAllocate === 'Y' ? '허용' : '차단',
        expert: u.expertName || '미지정',
      })),
      totalPages: res.data.totalPages,
      totalCount: res.data.totalCount
    }
  }
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

// 컬럼 정의에 조건부로 super 전용 칼럼 삽입
const columns = computed(() => {
  const base = [
    { key: "",  label: "",   type: "text", ellipsis: { width: 10 } },
    { key: "type", label: "구분", type: "badge", editable: canEdit, options: ["관리자", "센터장", "전문가", "팀장", "프로"] },
    { key: "",  label: "",   type: "text", ellipsis: { width: 20 } },
    { key: "name", label: "이름", type: "text", ellipsis: { width: 150 } },
    { key: "phone", label: "전화번호", type: "text", ellipsis: { width: 180 } },
    { key: "email", label: "ID(Email)", type: "text", ellipsis: { width: 250 } },
    {
      key: "center",
      label: "소속",
      type: "badge",
      editable: (row) => row.center !== "본사",

      // 행(row)의 직책에 따라 옵션 목록을 갈아끼움
      options: (row) => {
        if (["센터장", "전문가"].includes(row.type)) {
          return centerUnitOptions.value; // 센터 목록만 리턴
        } else if (["팀장", "프로"].includes(row.type)) {
          return teamUnitOptions.value;   // 팀 목록만 리턴
        }
        return fullCenterList.value; // 혹은 전체 목록
      }
    },
    { key: "",  label: "",   type: "text", ellipsis: { width: 20 } },
    { key: "requestStatus", label: "요청상태", type: "badge", editable: canEdit, options: ["승인", "대기", "탈퇴"] },
    { key: "",  label: "",   type: "text", ellipsis: { width: 20 } },
  ];

  if (isSuper.value) {
    // super에게만 노출 + 수정 가능
    base.push({
        key: "visible",
        label: "가시권한",
        type: "badge",
        // super가 볼 때만 노출, 그리고 "행의 권한이 관리자, 센터장, 전문가"인 경우에만 편집 허용 (자기 자신은 수정 불가)
          editable: (row) =>
              isSuper.value &&
              ["관리자", "센터장", "전문가"].includes(row.type) &&
              row.email !== auth.email,
        options: ["공개", "차단"]
      },
      { key: "",  label: "",   type: "text", ellipsis: { width: 20 } },
      {
        key: "allocate",
        label: "분배권한",
        type: "badge",
        // super가 볼 때만 노출, 그리고 "행의 권한이 관리자/센터장/전문가"인 경우에만 편집 허용 (자기 자신은 수정 불가)
        editable: (row) =>
            isSuper.value &&
            ["관리자", "센터장", "전문가"].includes(row.type) &&
            row.email !== auth.email,
        options: ["허용", "차단"]
      },
      { key: "",  label: "",   type: "text", ellipsis: { width: 20 } },
      {
        key: "expert",
        label: "전문가",
        type: "badge",
        // 직책이 '전문가, 센터장'인 행에서만 수정 가능 + super만
        editable: (row) => isSuper.value && (row.type === "센터장" || row.type === "전문가"),
        options: expertOptions.value.map(e => e.expertName)  // 배지에 보일 텍스트
      },
      { key: "",  label: "",   type: "text", ellipsis: { width: 10 } },
    )
  }
  return base;
});

// 요청상태가 승인이면 체크박스 비활성화
function isRowSelectable(row) {
  const isApproved = row.requestStatus === '승인';
  return !isApproved;
}

// 배지 수정 가능 여부 검토
function canEdit(row) {
  const requesterEmail = auth.email
  // 1. 본인 계정이면 불가
  if (row.email === requesterEmail) return false
  // 2. super 계정 제외, 관리자 계정 수정 불가
  return !((row.type === "관리자" || row.center === "본사") && !auth.grants.isSuper);
}

// 프론트는 백엔드가 내려준 isSuper만 사용
const isSuper = computed(() => auth.grants.isSuper)

// 팀장 중복 확인 함수
async function hasManager(centerName, excludeUserId) {
  const { data } = await axios.get("/api/super/users/has-manager", {
    params: { centerName, excludeUserId }
  });
  // data = { exists: true | false }
  return !!data?.exists;
}

// 센터장 중복 확인 함수
async function hasCenterHead(centerName, excludeUserId) {
  console.log(centerName, excludeUserId)
  const { data } = await axios.get("/api/super/users/has-center-head", {
    params: { centerName, excludeUserId }
  });
  console.log(data);
  return !!data?.exists;
}

// 이벤트 핸들러들
// 1. 테이블에서 체크박스 선택 시
function onRowSelect(rows) {
  selectedRows.value = rows
  // console.log("현재 선택된 행들:", rows)
}

// 2. 버튼 클릭 시 (ComponentCard → User.vue)
async function onButtonClick(btn) {
  // console.log("Button clicked:", btn)
  if (btn === "일괄승인") {
    await onBulkApprove()
  }
}

// 2-1. 일괄승인 로직
async function onBulkApprove() {
  if (selectedRows.value.length === 0) {
    alert("선택된 사용자가 없습니다.")
    return
  }

  if (!confirm(`${selectedRows.value.length}명의 사용자를 승인하시겠습니까?`)) {
    return
  }

  return runBusy(async () => {
    try {
      const ids = selectedRows.value.map(r => r.userId)
      const res = await axios.patch("/api/super/users/bulk-approve", ids)
      const { approvedIds, skippedIds } = res.data

      // 결과 메시지 구성
      if (approvedIds.length > 0 && skippedIds.length === 0) {
        alert(`일괄 승인 성공`)
      } else if (approvedIds.length > 0 && skippedIds.length > 0) {
        alert(`관리자 권한인 ${skippedIds.length}명 제외, ${approvedIds.length}명 승인 성공`)
      } else if (approvedIds.length === 0 && skippedIds.length > 0) {
        alert(`관리자 권한인 ${skippedIds.length}명 제외, 승인된 사용자가 없습니다`)
      }

      tableRef.value?.clearSelection?.();

      // 테이블 새로고침 (DB 최신 상태 반영)
      await fetchData()

      // 선택 해제
      selectedRows.value = []
    } catch (err) {
      console.error("일괄 승인 실패", err)
      alert("일괄 승인 중 오류가 발생했습니다.")
    }
  })
}

// 3. 배지를 수정할 경우
async function onBadgeUpdate(row, key, newValue) {
  // console.log("Badge updated:", row, key, newValue)
  let fieldLabel = key
  let displayValue = newValue

  // key → 한글
  if (key === "type") fieldLabel = "구분"
  else if (key === "center") fieldLabel = "소속"
  else if (key === "requestStatus") fieldLabel = "요청상태"
  else if (key === "visible") fieldLabel = "가시권한"
  else if (key === "allocate") fieldLabel = "분배권한"
  else if (key === "expert") fieldLabel = "전문가";

  // value → 한글
  if (newValue === "SUPERADMIN") displayValue = "관리자"
  else if (newValue === "MANAGER") displayValue = "팀장"
  else if (newValue === "STAFF") displayValue = "프로"

  // 가시권한 → Y/N
  if (key === "visible") {
    newValue = newValue === "공개" ? "Y" : "N";
  }

  // 분배권한 → Y/N
  if (key === "allocate") {
    newValue = newValue === "허용" ? "Y" : "N";
  }

  // === 센터/팀 선택 제한 가드 ===
  if (key === "center" && newValue !== "본사") {
    const userRole = row.type; // "센터장", "전문가", "팀장", "프로"

    // 1. 센터장/전문가 그룹: '팀' 선택 시 차단
    if (userRole === "센터장" || userRole === "전문가") {
      // 선택한 곳이 '센터'가 아니면 경고 (즉, 팀을 선택했으면)
      if (!isCenterUnit(newValue)) {
        alert("센터장 및 전문가는 '센터(지점)'만 선택할 수 있습니다.");
        await fetchData(); // UI 원복
        return;
      }
    }

    // 2. 팀장/프로 그룹: '센터' 선택 시 차단
    if (userRole === "팀장" || userRole === "프로") {
      // 선택한 곳이 '팀'이 아니면 경고 (즉, 센터를 선택했으면)
      if (!isTeamUnit(newValue)) {
        alert("팀장 및 프로는 '팀'만 선택할 수 있습니다.");
        await fetchData(); // UI 원복
        return;
      }
    }
  }

  // --- [가드 로직] 권한 및 유효성 검사 ---

  // 가시권한 가드
  if (key === "visible" && !["관리자", "센터장", "전문가"].includes(row.type)) {
    alert("가시권한은 '관리자', '센터장', '전문가' 권한에만 수정할 수 있습니다.");
    await fetchData();
    return;
  }

  // 분배권한 가드
  if (key === "allocate" && !["관리자", "센터장", "전문가"].includes(row.type)) {
    alert("'관리자', '센터장' 또는 '전문가'의 분배권한만 수정가능합니다.");
    await fetchData();
    return;
  }

  // 전문가 가드
  if (key === "expert" && !(row.type === "센터장" || row.type === "전문가")) {
    alert("권한이 '센터장' 또는 '전문가'인 직원만 전문가를 설정할 수 있습니다.")
    await fetchData()
    return
  }

  // === 승인된 직원은 super계정 만 '관리자'로 변경 가능 ===
  if (key === "type" && newValue === "관리자" && row.requestStatus === "승인" && !auth.grants.isSuper) {
    alert("수정 권한이 없습니다.");
    await fetchData();
    return;
  }

  // 팀장 중복 확인 가드
  try {
    // 1) 구분을 '팀장'으로 바꾸려는 경우
    if (key === "type" && newValue === "팀장") {
      const targetCenter = row.center; // 현재 소속
      if (targetCenter && targetCenter !== "본사") {
        const exists = await hasManager(targetCenter, row.userId);
        if (exists) {
          alert(`'${targetCenter}'에는 이미 팀장이 있습니다. 기존 팀장을 해제한 뒤 진행하세요.`);
          await fetchData();
          return;
        }
      }
    }

    // 2) 소속을 변경하는데, 현재 계정이 '팀장'인 경우
    if (key === "center" && row.type === "팀장" && newValue && newValue !== "본사") {
      const exists = await hasManager(newValue, row.userId);
      if (exists) {
        alert(`'${newValue}'에는 이미 팀장이 있습니다. 기존 팀장을 해제한 뒤 진행하세요.`);
        await fetchData();
        return;
      }
    }
  } catch (e) {
    console.error("팀장 중복 확인 실패", e);
    alert("팀장 중복 확인 중 오류가 발생했습니다.");
    return;
  }

  // 센터장 중복 확인 가드
  try {
    // 1) 직책을 '센터장'으로 변경 시
    if (key === "type" && newValue === "센터장") {
      const targetCenter = row.center;
      // 본사나 미배정 상태가 아니라면 체크
      if (targetCenter && targetCenter !== "본사" && targetCenter !== "미할당") {
        const exists = await hasCenterHead(targetCenter, row.userId);
        if (exists) {
          alert(`'${targetCenter}'에는 이미 센터장이 있습니다.`);
          await fetchData();
          return;
        }
      }
    }

    // 2) 센터장이 소속(센터)을 옮길 시
    if (key === "center" && row.type === "센터장" && newValue && newValue !== "본사") {
      const exists = await hasCenterHead(newValue, row.userId);
      if (exists) {
        alert(`'${newValue}'에는 이미 센터장이 있습니다.`);
        await fetchData();
        return;
      }
    }
  } catch (e) {
    console.error("중복 확인 실패", e);
    alert("중복 확인 중 오류가 발생했습니다.");
    await fetchData();
    return;
  }

  if (!confirm(`${row.name}의 ${fieldLabel}을(를) "${displayValue}"(으)로 변경하시겠습니까?`)) {
    await fetchData()
    return
  }

  try {
    // key: 'type' | 'center' | 'requestStatus' ...
    const payload = { field: key, value: newValue }

    // row 에 userId 가 있어야 함 (없으면 매퍼쪽에서 내려줘야 함)
    await axios.patch(`/api/super/users/update/${row.userId}`, payload)

    // 성공 시 UI 반영
    row[key] = newValue
    await fetchData()

    // console.log(newValue, "업데이트 성공")
  } catch (err) {
    console.error("업데이트 실패", err)
    alert("업데이트 중 오류가 발생했습니다.")
  }
}
// function onPageChange(newPage) {
//   page.value = newPage
//   console.log("Page changed:", newPage)
// }
</script>
