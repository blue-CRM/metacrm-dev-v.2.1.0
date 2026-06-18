<template>
  <div
    class="flex flex-col h-full bg-white dark:bg-[#0e1626] overflow-hidden"
    style="font-family: 'Pretendard', 'Pretendard Variable', -apple-system, BlinkMacSystemFont, system-ui, 'Apple SD Gothic Neo', 'Malgun Gothic', sans-serif"
  >
    <!-- ───────── 헤더(H1): 이름 · 전화 · 담당자 · 우측 상태배지+약속칩 + 새로고침 ───────── -->
    <header
      class="flex items-end gap-3 px-[18px] py-3.5 border-b border-gray-200 dark:border-[#28365a]
             bg-white dark:bg-[#0e1626] shrink-0"
    >
      <!-- 좌측 정보: 평소 1줄(이름·전화·프로). 좁거나 이름이 길면 전화·프로 묶음이 다음 줄로 wrap.
           이름은 8자 이상이면 말줄임(max-w + truncate). -->
      <div class="flex-1 min-w-0 flex flex-wrap items-baseline gap-x-2.5 gap-y-0.5">
        <span class="text-[17px] font-bold text-gray-900 dark:text-gray-100 tracking-tight truncate max-w-[150px]">
          {{ customer.name || '—' }}
        </span>
        <span class="flex items-baseline gap-2.5 min-w-0">
          <span class="text-[13px] text-gray-500 dark:text-gray-400 tabular-nums tracking-wide whitespace-nowrap shrink-0">
            {{ customer.phone }}
          </span>
          <span class="text-[12px] text-gray-400 dark:text-gray-500 truncate min-w-0">
            프로: {{ customer.assignedStaffName || '미배정' }}
          </span>
        </span>
      </div>

      <!-- 상태배지(읽기전용) + 약속칩 -->
      <span class="inline-flex items-center gap-1.5 shrink-0">
        <!-- 표(PsnsTable) 배지와 동일: text-theme-xs(12px) · font-medium · px-2 py-0.5 · rounded-full -->
        <span
          v-if="customer.status"
          :class="[
            'inline-flex items-center justify-center min-w-[70px] h-[28px] px-2 rounded-full text-theme-xs font-medium whitespace-nowrap',
            statusBadgeColor(customer.status),
          ]"
        >
          {{ customer.status }}
        </span>
        <span
          v-if="appointmentText"
          class="inline-flex items-center gap-1 text-[11px] font-medium text-gray-500 dark:text-gray-400 tabular-nums whitespace-nowrap"
        >
          <svg width="11" height="11" viewBox="0 0 14 14" fill="none" class="opacity-70">
            <circle cx="7" cy="7" r="5.5" stroke="currentColor" stroke-width="1.2" />
            <path d="M7 4v3l2 1.5" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" />
          </svg>
          {{ appointmentText }}
        </span>
      </span>

      <!-- 새로고침 -->
      <button
        title="새로고침"
        class="w-7 h-7 rounded-md flex items-center justify-center shrink-0
               text-gray-400 hover:text-gray-700 hover:bg-gray-100
               dark:hover:text-gray-200 dark:hover:bg-[#233152] transition"
        @click="refresh"
      >
        <svg width="15" height="15" viewBox="0 0 14 14" fill="none">
          <path
            d="M12 7a5 5 0 1 1-1.5-3.5M12 2v2.5H9.5"
            stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round" fill="none"
          />
        </svg>
      </button>
    </header>

    <!-- ───────── 검색(Split): 텍스트 + 날짜 픽커 (프런트 필터) ───────── -->
    <div
      class="flex items-center gap-1.5 px-3.5 py-2 border-b border-gray-200 dark:border-[#28365a]
             bg-gray-50 dark:bg-[#16213b]/50 shrink-0"
    >
      <div
        class="flex-1 flex items-center gap-1.5 min-w-0 bg-white dark:bg-[#16213b] rounded-md
               border border-gray-200 dark:border-[#28365a] px-2.5 py-1.5"
      >
        <svg width="12" height="12" viewBox="0 0 12 12" fill="none" class="text-gray-400 shrink-0">
          <circle cx="5" cy="5" r="3.5" stroke="currentColor" stroke-width="1.3" />
          <path d="M8 8l2.5 2.5" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" />
        </svg>
        <input
          v-model="search"
          placeholder="내용·이름으로 검색"
          class="flex-1 min-w-0 border-none outline-none bg-transparent text-[12px]
                 text-gray-900 dark:text-gray-100 placeholder:text-gray-400"
        />
        <button
          v-if="search"
          class="text-gray-400 text-sm leading-none"
          @click="search = ''"
        >
          ×
        </button>
      </div>

      <div class="relative shrink-0">
        <button
          :class="[
            'flex items-center gap-1.5 rounded-md px-2.5 h-7 text-[12px] font-semibold border transition',
            dateSearch
              ? 'bg-blue-50 dark:bg-blue-500/[0.12] border-blue-300 text-blue-600 dark:text-blue-400'
              : 'bg-white dark:bg-[#16213b] border-gray-200 dark:border-[#28365a] text-gray-500 dark:text-gray-400',
          ]"
          @click="openDatePicker"
        >
          <svg width="13" height="13" viewBox="0 0 14 14" fill="none">
            <rect x="2" y="3" width="10" height="9" rx="1" stroke="currentColor" stroke-width="1.3" />
            <path d="M2 6h10M5 2v2M9 2v2" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" />
          </svg>
          {{ dateSearch ? fmtMonthDay(dateSearch) : '날짜' }}
        </button>
        <button
          v-if="dateSearch"
          title="날짜 해제"
          class="absolute -top-1 -right-1 w-4 h-4 rounded-full bg-gray-400 text-white text-[10px] leading-none flex items-center justify-center"
          @click="dateSearch = ''"
        >
          ×
        </button>
        <input
          ref="dateRef"
          v-model="dateSearch"
          type="date"
          class="absolute left-0 top-full opacity-0 pointer-events-none w-px h-px"
        />
      </div>
    </div>

    <!-- ───────── 바디 ───────── -->
    <div ref="bodyRef" class="flex-1 overflow-y-auto px-4 pt-3.5 pb-4 bg-gray-50 dark:bg-[#16213b]/50">
      <!-- 로딩 -->
      <div v-if="loading" class="h-full flex items-center justify-center text-center px-6">
        <p class="text-[13px] text-gray-400 dark:text-gray-500">불러오는 중…</p>
      </div>

      <!-- 권한없음/없는 고객 -->
      <div v-else-if="error === 'GONE'" class="h-full flex items-center justify-center text-center px-6">
        <p class="text-[13px] text-gray-400 dark:text-gray-500">
          권한이 없거나 존재하지 않는 고객입니다.
        </p>
      </div>

      <!-- 네트워크/서버 오류 -->
      <div v-else-if="error === 'NETWORK'" class="h-full flex flex-col items-center justify-center gap-2 text-center px-6">
        <p class="text-[13px] text-gray-400 dark:text-gray-500">불러오지 못했습니다.</p>
        <button
          class="text-[12px] text-blue-600 dark:text-blue-400 hover:underline"
          @click="refresh"
        >다시 시도</button>
      </div>

      <!-- 빈 상태 -->
      <div
        v-else-if="!groups.length"
        class="h-full flex items-center justify-center text-center px-6"
      >
        <p class="text-[13px] text-gray-400 dark:text-gray-500">
          {{ search || dateSearch ? '검색 결과가 없습니다.' : '아직 메모가 없습니다.' }}
        </p>
      </div>

      <!-- 채팅 그룹 -->
      <template v-else>
        <div v-for="(g, gi) in groups" :key="gi" class="mb-3.5">
          <!-- 작성자 그룹헤더: 아바타 + 이름 (타임스탬프는 여기 아님) -->
          <div class="flex items-center gap-2 mb-1.5">
            <!-- 시스템 아바타 -->
            <div
              v-if="g.isSystem"
              class="w-[26px] h-[26px] rounded-full flex items-center justify-center shrink-0
                     bg-gray-200 dark:bg-[#233152] text-gray-500 dark:text-gray-400"
            >
              <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
                <circle cx="7" cy="7" r="5.5" stroke="currentColor" stroke-width="1.3" />
                <path d="M7 6.5v3M7 4.5v.01" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" />
              </svg>
            </div>
            <!-- 사용자 이니셜 아바타 -->
            <div
              v-else
              class="w-[26px] h-[26px] rounded-full flex items-center justify-center shrink-0 text-white text-[11px] font-bold"
              :style="{ background: avatarColor(g.authorName) }"
            >
              {{ avatarInitial(g.authorName) }}
            </div>
            <span
              :class="[
                'text-[12.5px] font-semibold',
                g.isSystem ? 'text-gray-500 dark:text-gray-400' : 'text-gray-900 dark:text-gray-100',
              ]"
            >
              {{ g.isSystem ? '시스템' : g.authorName }}
            </span>
          </div>

          <!-- 말풍선 리스트 (이름 아래로 들여쓰기) -->
          <div class="flex flex-col gap-1.5 pl-[34px]">
            <MemoChatItem
              v-for="m in g.items"
              :key="m.chatId"
              :chat="m"
              :readonly="view"
              :editing="editingId === m.chatId"
              :edit-text="editText"
              @update:edit-text="editText = $event"
              @start-edit="startEdit"
              @remove="remove"
              @save="saveEdit"
              @cancel="cancelEdit"
            />
          </div>
        </div>
      </template>
    </div>

    <!-- ───────── 입력창 (canWrite && !view 일 때만) ───────── -->
    <div
      v-if="canWrite && !view"
      class="flex items-end gap-2 p-3 border-t border-gray-200 dark:border-[#28365a] bg-white dark:bg-[#0e1626] shrink-0"
    >
      <div
        class="flex-1 flex items-end bg-gray-50 dark:bg-[#16213b] rounded-2xl px-3.5 py-2
               border border-gray-200 dark:border-[#28365a]"
      >
        <textarea
          ref="composerRef"
          v-model="draft"
          rows="1"
          placeholder="메모 입력 후 Enter"
          class="flex-1 border-none outline-none resize-none bg-transparent text-[13px] leading-normal
                 text-gray-900 dark:text-gray-100 max-h-[120px] placeholder:text-gray-400"
          @keydown="onComposerKey"
          @input="autoGrow"
        ></textarea>
      </div>
      <button
        :disabled="!draft.trim()"
        :class="[
          'h-9 px-4 rounded-2xl text-[13px] font-semibold shrink-0 transition',
          draft.trim()
            ? 'bg-blue-500 hover:bg-blue-600 text-white cursor-pointer'
            : 'bg-gray-100 dark:bg-[#16213b] text-gray-400 cursor-not-allowed',
        ]"
        @click="submit"
      >
        전송
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'
import MemoChatItem from '@/components/memo/MemoChatItem.vue'
import { useMemoChats } from '@/composables/useMemoChats'
import { MEMO_SYNC_CHANNEL } from '@/composables/useMemoWindow'
import {
  fmtMonthDay,
  fmtAppointment,
  groupChats,
  applySearch,
  statusBadgeColor,
  avatarColor,
  avatarInitial,
  NEEDS_APPOINTMENT,
} from '@/composables/memoFormat'

const props = defineProps({
  customerId: { type: [Number, String], default: null },
  /** 읽기전용(?view=1): 입력창·수정·삭제 숨김 (분배창 등). 그 외 권한은 서버가 판정 */
  view: { type: Boolean, default: false },
})

const {
  customer,
  chats,
  loading,
  error,
  view,
  canWrite,
  editingId,
  editText,
  send,
  startEdit,
  cancelEdit,
  saveEdit,
  remove,
  refresh,
} = useMemoChats(props.customerId, {
  view: props.view,
})

/* 검색(프런트 필터) */
const search = ref('')
const dateSearch = ref('')
const dateRef = ref(null)
function openDatePicker() {
  dateRef.value?.showPicker?.() || dateRef.value?.focus()
}

const groups = computed(() =>
  groupChats(applySearch(chats.value, { text: search.value, dateSearch: dateSearch.value })),
)

/* 약속칩: 재콜·내방일 때. 시간 있으면 일시(내방이면 회의실명), 없으면 "일정없음" */
const appointmentText = computed(() => {
  if (!NEEDS_APPOINTMENT.has(customer.status)) return ''
  if (!customer.appointmentAt) return '일정없음'
  const base = fmtAppointment(customer.appointmentAt)
  if (customer.status === '내방' && customer.roomName) {
    return `${base} · ${customer.roomName}`
  }
  return base
})

/* 입력창 */
const draft = ref('')
const composerRef = ref(null)
function autoGrow() {
  const el = composerRef.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = Math.min(el.scrollHeight, 120) + 'px'
}
function submit() {
  if (!draft.value.trim()) return
  send(draft.value)
  draft.value = ''
  nextTick(autoGrow)
}
function onComposerKey(e) {
  // IME 가드: 한글 조합 중 Enter는 전송 안 함(코드베이스 선례 없어 직접 구현)
  if (e.key === 'Enter' && !e.shiftKey && !e.isComposing) {
    e.preventDefault()
    submit()
  }
}

/* 새 채팅 추가 시 하단 자동 스크롤 */
const bodyRef = ref(null)
watch(
  () => chats.value.length,
  () => {
    nextTick(() => {
      const el = bodyRef.value
      if (el) el.scrollTop = el.scrollHeight
    })
  },
  { immediate: true },
)

/* 본창 표에서 이 고객 정보(이름/상태/재콜시간/내방일정/소유권) 변경 시 → 헤더+채팅 재조회
 * (같은 브라우저 BroadcastChannel. 다른 사용자 실시간은 3차 WebSocket.) */
let bc = null
onMounted(() => {
  try {
    bc = new BroadcastChannel(MEMO_SYNC_CHANNEL)
    bc.onmessage = (e) => {
      if (
        e.data?.type === 'customer-updated' &&
        String(e.data.customerId) === String(props.customerId)
      ) {
        refresh()
      }
    }
  } catch {
    /* 미지원 환경 무시 */
  }
})
onUnmounted(() => {
  try {
    bc?.close()
  } catch {
    /* noop */
  }
})
</script>
