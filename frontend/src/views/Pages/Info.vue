<template>
  <AdminLayout>
    <PageBreadcrumb pageTitle="메타CRM 조직도" />

    <div class="relative overflow-hidden rounded-2xl border border-gray-200 dark:border-[#1f2a47] bg-white dark:bg-[#14203a] p-0
                dark:border-[#1f2a47] dark:bg-[#14203a]">
      <div class="relative z-10 p-6">

        <ul class="space-y-3">
          <UserNode
              v-for="node in treeData"
              :key="nodeKey(node)"
              :node="node"
              :current-user="currentUser"
              :expand-keys="expandKeys"
              @toggle="onToggle"
              @edit="openEdit"
          />
        </ul>
      </div>
    </div>

    <!-- 간단 수정 모달 -->
    <Teleport to="body">
      <div
          v-if="edit.open"
          class="fixed inset-0 z-[100000] flex items-center justify-center"
          role="dialog" aria-modal="true"
      >
        <!-- 배경 오버레이 (오타 제거, z 보장) -->
        <div
            class="fixed inset-0 bg-black/40 dark:bg-black/60 z-[99999]"
            @click="closeEdit"
        ></div>

        <!-- 모달 박스 -->
        <div
            class="relative z-[100000] w-[440px] rounded-xl bg-white dark:bg-[#0e1626] p-5 shadow-xl ring-1 ring-black/5 dark:ring-white/10
             dark:bg-[#0e1626] dark:text-gray-100"
        >
          <h3 class="mb-3 text-base font-semibold">정보 수정</h3>

          <div class="space-y-3">
            <div>
              <label class="mb-1 block text-sm text-gray-600 dark:text-gray-300">이름</label>
              <input
                  v-model.trim="edit.name"
                  @blur="validateName"
                  class="h-11 w-full rounded-lg border px-3
                   bg-white dark:bg-[#16213b] text-gray-800 dark:text-gray-100 focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10
                   dark:border-[#28365a] dark:bg-[#16213b] dark:text-gray-100"
              />
              <p v-if="editErr.name" class="mt-1 text-xs text-error-500">{{ editErr.name }}</p>
            </div>

            <div>
              <label class="mb-1 block text-sm text-gray-600 dark:text-gray-300">전화번호</label>
              <input
                  v-model="edit.phone"
                  @input="onPhoneInput"
                  @blur="validatePhone"
                  placeholder="010-1234-5678"
                  class="h-11 w-full rounded-lg border px-3
                   bg-white dark:bg-[#16213b] text-gray-800 dark:text-gray-100 focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10
                   dark:border-[#28365a] dark:bg-[#16213b] dark:text-gray-100"
              />
              <p v-if="editErr.phone" class="mt-1 text-xs text-error-500">{{ editErr.phone }}</p>
            </div>
          </div>

          <div class="mt-4 flex justify-end gap-2">
            <button
                class="h-10 rounded-lg bg-gray-200 px-4 text-gray-800 hover:bg-gray-100
                 dark:bg-[#233152] dark:text-gray-100"
                @click="closeEdit"
            >
              취소
            </button>
            <button
                class="h-10 rounded-lg bg-brand-500 px-4 text-white hover:bg-brand-600"
                @click="saveEdit"
            >
              저장
            </button>
          </div>
        </div>
      </div>
    </Teleport>

  </AdminLayout>
</template>

<script setup>
import { ref, onMounted, nextTick, watch } from "vue"
import AdminLayout from "@/components/layout/AdminLayout.vue"
import PageBreadcrumb from "@/components/common/PageBreadcrumb.vue"
import UserNode from "@/components/profile/UserNode.vue"
import axios from "@/plugins/axios.js"
import { globalFilters } from "@/composables/globalFilters.js" // 헤더 검색 상태

const treeData    = ref([])
const currentUser = ref(null)

/** 펼침 제어: Set<string> (예: "HQ:메타CRM", "GROUP:본사", "CENTER:센터A") */
const expandKeys  = ref(new Set(["HQ:메타CRM"])) // 기본: 본사만 열기

const nodeKey = (n) => {
  if (n.userId) return `U:${n.userId}`

  if (n.userRole === "HQ") return "HQ:메타CRM"

  // 지점 노드 (GROUP)
  if (n.userRole === "GROUP") return `B:${n.branchId ?? n.userName}`

  // 팀 노드 (CENTER)
  if (n.userRole === "CENTER") return `C:${n.centerId ?? n.userName}`

  return `${n.userRole}:${n.userName}`
}

function onToggle({ key, open }) {
  const next = new Set(expandKeys.value)
  if (open) next.add(key); else next.delete(key)
  expandKeys.value = next
}

/** 데이터 로드 */
const didInitExpand = ref(false)
async function loadTree({ restoreExpandedKeys = null, focusUserId = null } = {}) {
  const { data } = await axios.get("/api/info/tree", { withCredentials: true })
  treeData.value = data.nodes
  currentUser.value = data.currentUser

  // 1) 저장/검색 등으로 복원 요청이 있으면 그걸 우선
  if (restoreExpandedKeys) {
    expandKeys.value = new Set(restoreExpandedKeys)
  }

  // 2) 최초 로그인 1회만: 내 경로(HQ -> 지점 -> 팀) 자동 오픈
  if (!didInitExpand.value && !restoreExpandedKeys && !(globalFilters.keyword || "").trim()) {
    if (currentUser.value?.userId) {
      forceOpenPathToUserId(currentUser.value.userId, { exclusive: true })
    } else {
      expandKeys.value = new Set(["HQ:메타CRM"])
    }
    didInitExpand.value = true
  }

  // 3) 특정 유저 포커스(저장 후 재조회)
  if (focusUserId) {
    forceOpenPathToUserId(focusUserId, { exclusive: false })
    await nextTick()
    const el = document.getElementById("user-" + focusUserId)
    if (el) el.scrollIntoView({ behavior: "smooth", block: "center" })
  }

  // 4) 검색어 있으면 검색 펼침
  if ((globalFilters.keyword || "").trim()) applySearch(globalFilters.keyword || "")
}

onMounted(loadTree)

function forceOpenPathToUserId(userId, { exclusive = false } = {}) {
  // exclusive=true 이면 기존 펼침 무시하고 "내 경로만" 세팅
  const open = exclusive ? new Set() : new Set(expandKeys.value)

  function dfs(list, ancestors = []) {
    for (const n of list) {
      const k = nodeKey(n)
      const nextAnc = ['HQ','GROUP','CENTER'].includes(n.userRole) ? [...ancestors, k] : ancestors
      if (n.userId === userId) { nextAnc.forEach(ak => open.add(ak)); return true }
      if (n.children && dfs(n.children, nextAnc)) return true
    }
    return false
  }
  dfs(treeData.value)
  expandKeys.value = open
}

/** 헤더 검색 → 펼침 제어 (동명이인이면 전부 펼침) */
watch(() => globalFilters.keyword, (term) => applySearch(term || ""))

function applySearch(term) {
  term = (term || "").toLowerCase().trim()
  if (!term) return // 빈 검색어면 펼침 상태 유지

  const parents = new Map() // childKey -> parentKey
  const matches = []

  function walk(list, parentKey = null) {
    for (const n of list) {
      const k = nodeKey(n)
      if (parentKey) parents.set(k, parentKey)
      if (n.children && n.children.length) walk(n.children, k)
      if (n.userId) {
        const hay = `${n.userName||""} ${n.userEmail||""} ${n.userPhone||""}`.toLowerCase()
        if (hay.includes(term)) matches.push(k)
      }
    }
  }
  walk(treeData.value)

  // if (matches.length === 0) { expandKeys.value = new Set(["HQ:메타CRM"]); return }
  if (matches.length === 0) return // 매칭 없으면 현 상태 유지

  const open = new Set()
  for (const mk of matches) {
    let p = parents.get(mk)
    while (p) { open.add(p); p = parents.get(p) }
  }
  expandKeys.value = open

  // 첫 매칭 위치로 스크롤
  nextTick(() => {
    const first = matches[0]
    if (first?.startsWith("U:")) {
      const el = document.getElementById(`user-${first.slice(2)}`)
      if (el) el.scrollIntoView({ behavior: "smooth", block: "center" })
    }
  })
}

/* ===== 수정 모달 ===== */
const edit = ref({ open:false, userId:null, name:"", phone:"" })
const editErr = ref({ name: '', phone: '' })

function openEdit(user) {
  edit.value = {
    open: true,
    userId: user.userId,
    name:  user.userName || "",
    phone: user.userPhone && user.userPhone.startsWith('010')
        ? user.userPhone : ''
  }
  editErr.value = { name: '', phone: '' }
}
function closeEdit(){ edit.value.open = false }

// 자동 하이픈
function hyphenatePhone(str) {
  const raw = (str || '').replace(/\D/g, '')
  if (raw.length <= 3) return raw
  if (raw.length <= 7) return `${raw.slice(0,3)}-${raw.slice(3)}`
  return `${raw.slice(0,3)}-${raw.slice(3,7)}-${raw.slice(7,11)}`
}
function onPhoneInput() {
  edit.value.phone = hyphenatePhone(edit.value.phone)
  editErr.value.phone = '' // 입력 중에는 에러 지움
}

// 유효성 검사
function validateName() {
  const n = (edit.value.name || '').trim()
  editErr.value.name = n.length >= 2 ? '' : '이름은 최소 2글자 이상이어야 합니다.'
}
function validatePhone() {
  const ok = /^010-\d{4}-\d{4}$/.test(edit.value.phone || '')
  editErr.value.phone = ok ? '' : '전화번호는 010-1234-5678 형식이어야 합니다.'
}

async function saveEdit() {
  // 유효성 검사
  validateName()
  validatePhone()
  if (editErr.value.name || editErr.value.phone) return

  const u = edit.value
  try {
    await axios.patch(`/api/info/users/update/${u.userId}`, { field:"name",  value: u.name  }, { withCredentials:true })
    await axios.patch(`/api/info/users/update/${u.userId}`, { field:"phone", value: u.phone }, { withCredentials:true })
    const keep = new Set(expandKeys.value) // 펼침 상태 보관
    closeEdit()
    await loadTree({ restoreExpandedKeys: keep, focusUserId: u.userId })
  } catch (e) {
    alert(e?.response?.data || "저장 실패")
  }
}
</script>
