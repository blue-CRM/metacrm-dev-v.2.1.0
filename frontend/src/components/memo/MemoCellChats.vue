<template>
  <!-- 회수·센터 메모 셀 펼침: 전체 채팅(STATUS 포함) 최신순 스크롤(읽기전용) -->
  <div
    class="w-[230px] max-h-32 overflow-auto rounded-md border border-gray-200 dark:border-gray-700 bg-gray-50/60 dark:bg-gray-900"
  >
    <div v-if="loading" class="px-2 py-3 text-xs text-gray-400 text-center">불러오는 중…</div>
    <div v-else-if="error" class="px-2 py-3 text-xs text-gray-400 text-center">{{ error }}</div>
    <div v-else-if="!chats.length" class="px-2 py-3 text-xs text-gray-400 text-center">메모 없음</div>

    <ul v-else class="divide-y divide-gray-100 dark:divide-gray-800">
      <li v-for="c in chats" :key="c.chatId" class="px-2 py-1">
        <div class="flex items-center gap-1 text-[9px] leading-tight text-gray-400 dark:text-gray-500">
          <span class="font-semibold">
            {{ isSystemType(c.chatType) ? '시스템' : (c.authorName || '—') }}
          </span>
          <span class="tabular-nums">{{ fmtDateTime(c.createdAt) }}</span>
        </div>
        <div class="text-[11px] leading-snug text-gray-700 dark:text-gray-300 whitespace-pre-wrap break-words">
          {{ c.content }}
        </div>
      </li>
    </ul>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import api from '@/plugins/axios'
import { fmtDateTime, isSystemType } from '@/composables/memoFormat'

const props = defineProps({
  customerId: { type: [Number, String], required: true },
})

const chats = ref([])
const loading = ref(false)
const error = ref('')

async function load() {
  if (props.customerId == null) return
  loading.value = true
  error.value = ''
  try {
    const { data } = await api.get(`/api/work/db/memo/${props.customerId}`)
    // 서버는 created_at 오름차순(오래된→최신) → 최신순으로 뒤집기
    chats.value = [...(data.chats ?? [])].reverse()
  } catch (e) {
    const st = e?.response?.status
    error.value = st === 410 || st === 403 ? '조회 권한 없음' : '불러오지 못했습니다'
    chats.value = []
  } finally {
    loading.value = false
  }
}

// 펼칠 때(마운트) 1회 로드. customerId 바뀌면 재로드.
watch(() => props.customerId, load, { immediate: true })
</script>
