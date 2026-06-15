<template>
  <AdminLayout>
    <PageBreadcrumb :pageTitle="currentPageTitle" />
    <div class="grid grid-cols-12 gap-4 min-w-0">
      <div class="col-span-12 space-y-6 min-w-0">

        <!-- 센터DB는 SUPERADMIN / CENTERHEAD / EXPERT만 (조회 전용) -->
        <ComponentCard
            v-if="isAllowed"
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
              :columns="columns"
              :data="items"
              :showCheckbox="false"
              :page="page"
              :totalPages="totalPages"
              :page-size="size"
              :loading="busy"
              @changePage="changePage"
          />
        </ComponentCard>

      </div>
    </div>
  </AdminLayout>

  <!-- 전역 로딩 오버레이 -->
  <Teleport to="body">
    <transition name="fade">
      <div
          v-if="showTableSpinner"
          class="fixed inset-0 z-[2147483646]"
          aria-live="polite" aria-busy="true" role="status"
      >
        <div class="absolute inset-0 bg-black/5 dark:bg-black/60"></div>

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
import { ref, computed, onMounted, onUnmounted, watch } from "vue";
import { useRouter } from "vue-router";
import { useAuthStore } from "@/stores/auth.js";
import PageBreadcrumb from "@/components/common/PageBreadcrumb.vue";
import AdminLayout from "@/components/layout/AdminLayout.vue";
import ComponentCard from "@/components/common/ComponentCard.vue";
import PsnsTable from "@/components/tables/basic-tables/PsnsTable.vue";
import { useTableQuery } from "@/composables/useTableQuery.js";
import { globalFilters } from "@/composables/globalFilters.js";
import api from "@/plugins/axios.js";

const router = useRouter();
const auth = useAuthStore();
const role = computed(() => auth.grants.role);

const currentPageTitle = ref("센터DB 조회");
const tableKey = ref(0);
const tableRef = ref(null);
const isRefreshing = ref(false);

const isAllowed = computed(() =>
    ["SUPERADMIN", "CENTERHEAD", "EXPERT"].includes(role.value)
);

/* =============================
   서버 응답 row 전처리
============================= */
const MONEY_ZERO_STATUSES = new Set(["자연풀", "카피"]);
const USD = new Intl.NumberFormat("en-US");
const isNil = (v) => v === null || v === undefined;

const toNumber = (v) => {
  if (typeof v === "number") return v;
  const s = String(v ?? "").replace(/[^\d.-]/g, ""); // "$1,200" 같은 것도 대응
  return s ? Number(s) : NaN;
};

const formatUSD = (v) => {
  if (isNil(v)) return v;
  const n = toNumber(v);
  if (!Number.isFinite(n)) return v; // 혹시 이상값이면 원본 유지
  return `$ ${USD.format(n)}`;
};

function preprocessRows(rows) {
  const list = Array.isArray(rows) ? rows : [];
  return list.map((row) => {
    const r = { ...row };

    // 1) 재콜 + 내방 + reservation null => 없음
    if ((r.status === "재콜" || r.status === "내방") && isNil(r.reservation)) {
      r.reservation = "없음";
    }

    // 2) 자연풀/카피 + (최초/업셀) null => 없음
    if (MONEY_ZERO_STATUSES.has(r.status)) {
      r.initialPrice = isNil(r.initialPrice) ? "없음" : formatUSD(r.initialPrice);
      r.upsellPrice  = isNil(r.upsellPrice)  ? "없음" : formatUSD(r.upsellPrice);
    }

    // 3) 최초/업셀 달러 표기 + 천단위 콤마
    if (!isNil(r.initialPrice)) r.initialPrice = formatUSD(r.initialPrice);
    if (!isNil(r.upsellPrice))  r.upsellPrice  = formatUSD(r.upsellPrice);

    return r;
  });
}

/* =============================
   목록 조회 (조회 전용)
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
  url: "/api/work/center/db",
  externalFilters: globalFilters,
  useExternalKeys: {
    from: "dateFrom",
    to: "dateTo",
    category: "category",
    keyword: "keyword",
    sort: "sort",
    status: "status",
    division: "division",
    expertName: "expertName",
    branchId: "branchId",
    centerId: "centerId",
  },
  mapper: (res) => ({
    items: preprocessRows(res.data.items),
    totalPages: res.data.totalPages,
    totalCount: res.data.totalCount
  })
});

/* =============================
   로딩 오버레이
============================= */
const uiLoading = ref(false);
const busy = computed(() => tableLoading.value || isRefreshing.value || uiLoading.value);
const showTableSpinner = ref(false);
let delayTimer = null;

async function runBusy(task) {
  if (uiLoading.value) return;
  uiLoading.value = true;
  try { await task(); } finally { uiLoading.value = false; }
}

watch(busy, (v) => {
  if (v) {
    delayTimer = setTimeout(() => { showTableSpinner.value = true; }, 200);
  } else {
    if (delayTimer) { clearTimeout(delayTimer); delayTimer = null; }
    showTableSpinner.value = false;
  }
});

onUnmounted(() => {
  if (delayTimer) { clearTimeout(delayTimer); delayTimer = null; }
});

/* =============================
   컬럼 (조회 전용)
   - badge/date/money 편집 이벤트 제거
============================= */

const columns = [
  { key: "createdAt", label: "DB생성일", type: "text" },
  { key: "branchName", label: "지점", type: "text" },
  { key: "centerName", label: "팀",   type: "text" },
  { key: "staff", label: "프로", type: "text" },
  { key: "division", label: "구분", type: "badge", options: ["최초", "중복", "유효"] },
  { key: "",  label: "",   type: "text", ellipsis: { width: 5 } },
  { key: "name", label: "이름", type: "text" },
  { key: "phone", label: "전화번호", type: "text", ellipsis: { width: 150 } },
  { key: "source", label: "DB출처", type: "text", ellipsis: { width: 100 } },
  { key: "content", label: "내용", type: "text", ellipsis: { width: 150 } },
  { key: "memo", label: "메모", type: "memoRead", ellipsis: { width: 220 } },
  { key: "status", label: "상태", type: "badge",
    options: ["부재1","부재2","부재3","부재4","부재5","기타","결번","재콜","내방","내방취소","가망","자연풀","카피","거절"] },
  { key: "reservation", label: "예약", type: "text", ellipsis: { width: 120 } },
  { key: "initialPrice", label: "최초(달러)", type: "text", ellipsis: { width: 110 } },
  { key: "upsellPrice",  label: "업셀(달러)", type: "text", ellipsis: { width: 110 } },
];

/* =============================
   지점/팀 종속 드롭박스 (필터)
============================= */
const branches = ref([]);      // { branchId, branchName }
const branchMap = ref({});     // name -> id
const branchId = ref(null);

const centers = ref([]);       // { centerId, centerName }
const centerMap = ref({});     // name -> id
const centerId = ref(null);

const branchOptions = computed(() => ["지점 전체", ...branches.value.map(b => b.branchName)]);
const teamOptions = computed(() => {
  if (!branchId.value) return ["팀 전체"];
  return ["팀 전체", ...centers.value.map(c => c.centerName)];
});

async function loadBranches() {
  const { data } = await api.get("/api/work/center/branches");
  branches.value = data || [];
  branchMap.value = Object.fromEntries(branches.value.map(b => [b.branchName, b.branchId]));
}

async function loadCentersByBranch(bid) {
  centers.value = [];
  centerMap.value = {};
  if (!bid) return;

  const { data } = await api.get("/api/work/center/teams", { params: { branchId: bid } });
  centers.value = data || [];
  centerMap.value = Object.fromEntries(centers.value.map(c => [c.centerName, c.centerId]));
}

/* =============================
   드롭박스: 지점/팀/구분/상태/전문가(본사/센터장만)
============================= */
const divisionOptions = ["구분 전체", "최초", "유효", "중복"];
const statusOptions = [
  "상태 전체", "모든 부재", "부재1", "부재2", "부재3", "부재4", "부재5",
  "기타", "결번", "재콜", "내방", "내방취소", "신규", "가망", "자연풀", "카피", "거절", "없음", "회수"
];
const expertOptions = ref(["전문가 전체"]);

const adminSelects = computed(() => {
  // idx: 0=지점, 1=팀, 2=구분, 3=상태, 4=전문가(옵션)
  const base = [branchOptions.value, teamOptions.value, divisionOptions, statusOptions];
  if (role.value !== "EXPERT") base.push(expertOptions.value);
  return base;
});

function onAdminSelectChange({ idx, value }) {
  return runBusy(async () => {
    // 0=지점
    if (idx === 0) {
      branchId.value = (value === "지점 전체") ? null : (branchMap.value[value] ?? null);

      // 지점 바뀌면 팀 초기화 + 팀 로드
      centerId.value = null;
      setFilter("centerId", null);

      setFilter("branchId", branchId.value);
      await loadCentersByBranch(branchId.value);
      return;
    }

    // 1=팀
    if (idx === 1) {
      if (!branchId.value) {
        centerId.value = null;
        setFilter("centerId", null);
        return;
      }
      centerId.value = (value === "팀 전체") ? null : (centerMap.value[value] ?? null);
      setFilter("centerId", centerId.value);
      return;
    }

    // 2=구분
    if (idx === 2) {
      const v = (value === "구분 전체") ? null : value;
      setFilter("division", v);
      globalFilters.division = v;
      return;
    }

    // 3=상태
    if (idx === 3) {
      const v = (value === "상태 전체") ? null : value;
      setFilter("status", v);
      globalFilters.status = v;
      return;
    }

    // 4=전문가
    if (idx === 4) {
      const v = (value === "전문가 전체") ? null : value;
      setFilter("expertName", v);
      globalFilters.expertName = v;
    }
  });
}

/* =============================
   정렬/버튼 (조회 전용)
============================= */
const viewOptions = ref({
  status: false,
  oldest: false
});

const activeLabels = computed(() => {
  const arr = [];
  if (viewOptions.value.status) arr.push("상태별 보기");
  if (viewOptions.value.oldest) arr.push("과거순 보기");
  return arr;
});

const adminButtons = computed(() => ["상태별 보기", "과거순 보기", "엑셀 다운로드"]);

async function onCommonButtonClick(btn) {
  if (uiLoading.value) return;
  uiLoading.value = true;

  try {
    if (btn === "엑셀 다운로드") {
      await downloadExcel();
      return;
    }

    if (btn === "상태별 보기") viewOptions.value.status = !viewOptions.value.status;
    if (btn === "과거순 보기") viewOptions.value.oldest = !viewOptions.value.oldest;

    const sortParts = [];
    if (viewOptions.value.status) sortParts.push("status");
    if (viewOptions.value.oldest) sortParts.push("oldest");
    const sort = sortParts.length ? sortParts.join(",") : null;

    setFilter("sort", sort);
    globalFilters.sort = sort;

    tableKey.value++; // 선택 초기화/리렌더 목적
  } finally {
    uiLoading.value = false;
  }
}

/* =============================
   엑셀 다운로드 (센터DB 기존 로직 유지)
============================= */
function todayStr() {
  const d = new Date();
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const dd = String(d.getDate()).padStart(2, "0");
  return `${y}-${m}-${dd}`;
}
function sanitize(name) {
  return name.replace(/[\\/:*?"<>|]/g, "_");
}

const currentBranchName = computed(() => {
  const id = branchId.value;
  if (!id) return "";
  const found = branches.value.find(b => b.branchId === id);
  return found?.branchName ?? "";
});

const currentCenterName = computed(() => {
  const id = centerId.value;
  if (!id) return "";
  const found = centers.value.find(c => c.centerId === id);
  return found?.centerName ?? "";
});

async function downloadExcel() {
  const params = {
    dateFrom: globalFilters.dateFrom || undefined,
    dateTo: globalFilters.dateTo || undefined,
    keyword: (globalFilters.keyword || "").trim() || undefined,

    category: globalFilters.category || undefined,
    division: globalFilters.division || undefined,
    status: globalFilters.status || undefined,
    sort: globalFilters.sort || undefined,
    expertName: globalFilters.expertName || undefined,

    branchId: branchId.value ?? undefined,
    centerId: centerId.value ?? undefined,
  };

  const res = await api.get("/api/work/center/db/export", { params, responseType: "blob" });
  const blob = new Blob([res.data], { type: res.headers["content-type"] });

  const a = document.createElement("a");
  a.href = URL.createObjectURL(blob);

  const namePart = [currentBranchName.value, currentCenterName.value].filter(Boolean).join("-");
  a.download = sanitize(namePart ? `${namePart} (${todayStr()}).xlsx` : `${todayStr()}.xlsx`);

  a.click();
  URL.revokeObjectURL(a.href);
}

/* =============================
   수동 새로고침
============================= */
async function onRefresh() {
  if (isRefreshing.value) return;
  isRefreshing.value = true;
  try {
    await api.post("/api/sheets/refresh?sid=1");
    await fetchData();
  } catch (err) {
    console.error(err);
    alert("새로고침 중 오류가 발생했습니다.");
  } finally {
    isRefreshing.value = false;
  }
}

/* =============================
   마운트
============================= */
onMounted(async () => {
  await loadBranches();

  if (role.value !== "EXPERT") {
    try {
      const { data } = await api.get("/api/work/db/experts");
      const names = (data || []).map(e => e.expertName);
      expertOptions.value = ["전문가 전체", ...names];
    } catch (err) {
      console.error("전문가 리스트 로딩 실패", err);
    }
  }
});

onUnmounted(() => {
  // 혹시 남아있을 값 방어
  globalFilters.mine = null;
  globalFilters.staffUserId = null;

  globalFilters.division = null;
  globalFilters.status = null;
  globalFilters.expertName = null;
  globalFilters.sort = null;
});
</script>
