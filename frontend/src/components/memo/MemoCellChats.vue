<template>
  <!-- 회수·센터·전체 메모 셀 펼침: 전체 채팅(STATUS 포함) 최신순 스크롤(읽기전용) -->
  <!-- 박스(테두리·스크롤·리사이즈)는 부모 팝업이 담당. 여기는 내용만. -->
  <div class="w-full">
    <div v-if="loading" class="px-2 py-3 text-theme-xs text-gray-400 text-center">불러오는 중…</div>
    <div v-else-if="error" class="px-2 py-3 text-theme-xs text-gray-400 text-center">{{ error }}</div>
    <div v-else-if="!chats.length" class="px-2 py-3 text-theme-xs text-gray-400 text-center">메모 없음</div>

    <ul v-else class="divide-y divide-gray-100 dark:divide-gray-800">
      <li v-for="c in chats" :key="c.chatId" class="px-2.5 py-1.5">
        <div class="flex items-center gap-1 text-theme-xs leading-tight text-gray-400 dark:text-gray-500">
          <span class="font-semibold">
            {{ isSystemType(c.chatType) ? '시스템' : (c.authorName || '—') }}
          </span>
          <span class="tabular-nums">{{ fmtDateTime(c.createdAt) }}</span>
        </div>
        <div class="text-theme-sm leading-normal text-gray-700 dark:text-gray-300 whitespace-pre-wrap break-words">
          {{ c.content }}
        </div>
      </li>
    </ul>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onBeforeUnmount } from 'vue'
import api from '@/plugins/axios'
import { fmtDateTime, isSystemType } from '@/composables/memoFormat'
import { MEMO_SYNC_CHANNEL } from '@/composables/useMemoWindow'

const props = defineProps({
  customerId: { type: [Number, String], required: true },
})

// 모듈 단위 캐시: 같은 고객 셀을 다시 펼칠 때 즉시 표시(네트워크 0회).
// 메모가 수정되면(BroadcastChannel) 해당 고객 캐시를 무효화한다.
const cache = new Map() // key(string) -> chats(최신순 배열)

const chats = ref([])
const loading = ref(false)
const error = ref('')

async function load(force = false) {
  const id = props.customerId
  if (id == null) return
  const key = String(id)

  // 캐시 적중 시 즉시 표시
  if (!force && cache.has(key)) {
    chats.value = cache.get(key)
    return
  }

  loading.value = true
  error.value = ''
  try {
    // 읽기전용 경량 엔드포인트: 채팅 목록만 내려온다(헤더/권한 계산 없음)
    const { data } = await api.get(`/api/work/db/memo/${id}/chats`)
    // 서버는 created_at 오름차순(오래된→최신) → 최신순으로 뒤집기
    const list = [...(Array.isArray(data) ? data : [])].reverse()
    cache.set(key, list)
    chats.value = list
  } catch (e) {
    const st = e?.response?.status
    error.value = st === 410 || st === 403 ? '조회 권한 없음' : '불러오지 못했습니다'
    chats.value = []
  } finally {
    loading.value = false
  }
}

// 펼칠 때(마운트) 1회 로드. customerId 바뀌면 재로드.
watch(() => props.customerId, () => load(), { immediate: true })

// 메모 작성/수정 → 캐시 무효화 + 현재 보고 있는 셀이면 즉시 새로고침
let ch = null
onMounted(() => {
  try {
    ch = new BroadcastChannel(MEMO_SYNC_CHANNEL)
    ch.onmessage = (ev) => {
      const msg = ev?.data
      // 채팅 작성/수정/삭제(memo-latest) 또는 고객정보 변경(customer-updated) → 캐시 무효화
      if ((msg?.type === 'customer-updated' || msg?.type === 'memo-latest') && msg.customerId != null) {
        cache.delete(String(msg.customerId))
        if (String(msg.customerId) === String(props.customerId)) load(true)
      }
    }
  } catch {
    /* BroadcastChannel 미지원 환경은 무시 */
  }
})
onBeforeUnmount(() => { try { ch?.close() } catch {} })
</script>
