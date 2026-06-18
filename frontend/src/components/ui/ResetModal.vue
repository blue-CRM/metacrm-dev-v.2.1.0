<template>
  <Teleport to="body">
    <div
        v-if="isOpen"
        class="fixed inset-0 z-[100000] flex items-center justify-center bg-black/40 p-4"
        role="dialog"
        aria-modal="true"
        @click.self="close"
    >
      <div
          class="w-full max-w-[600px] rounded-2xl bg-white dark:bg-[#0e1626] shadow-xl
             border border-gray-200 dark:border-[#1f2a47]"
      >
        <div class="px-5 py-4 border-b border-gray-200 dark:border-[#1f2a47] flex items-center justify-between">
          <div>
            <h3 class="text-base font-semibold text-gray-800 dark:text-gray-100">담당자 이력 관리</h3>
            <p class="text-xs text-gray-500 mt-1">현재 담당자는 삭제할 수 없으며, 이력만 삭제됩니다.</p>
          </div>
          <button
              class="text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"
              @click="close"
          >
            ✕
          </button>
        </div>

        <div class="px-5 py-4 space-y-4">
          <div class="flex justify-between items-center">
            <div class="text-sm text-gray-500 dark:text-gray-400">
              <span v-if="loading">불러오는 중...</span>
              <span v-else>총 {{ list.length }}건 / 삭제 선택 {{ selectedIds.size }}건</span>
            </div>
            <div class="flex gap-2">
              <button
                  class="inline-flex h-9 items-center justify-center rounded-lg border border-gray-200 px-3 text-xs
                       hover:bg-gray-50 dark:border-[#28365a] dark:bg-[#16213b] dark:text-gray-100"
                  @click="toggleAll"
                  :disabled="loading || list.length === 0"
              >
                전체 선택/해제
              </button>
            </div>
          </div>

          <div class="max-h-[50vh] overflow-auto border border-gray-200 dark:border-[#1f2a47] rounded-md bg-white dark:bg-[#0e1626]">
            <div v-if="loading" class="p-8 text-center">
              <svg class="animate-spin h-6 w-6 text-gray-400 mx-auto" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" fill="none"/><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z"/></svg>
            </div>

            <ul v-else-if="list.length > 0">
              <li
                  v-for="item in list"
                  :key="item.logId"
                  @click="!item.isCurrent && toggleItem(item)"
                  class="flex items-center justify-between px-4 py-3 border-b last:border-0 border-gray-100 dark:border-[#1f2a47] transition-colors"
                  :class="[
                    // 선택된 이력(현재 담당자 제외) → 회색 하이라이트
                    selectedIds.has(item.logId) && !item.isCurrent
                      ? 'bg-gray-100 dark:bg-[#16213b]/70'
                      : '',
                    // 클릭 가능/불가 상태
                    !item.isCurrent
                      ? 'cursor-pointer hover:bg-gray-50 dark:hover:bg-[#16213b]'
                      : 'opacity-70 bg-gray-50 dark:bg-[#16213b]/70'
                  ]"
              >
                <div class="flex flex-col">
                  <div class="flex items-center gap-2">
                    <span class="text-sm font-medium text-gray-900 dark:text-gray-100">
                        {{ item.userName }}
                    </span>
                    <!-- 팀장 풀 / 현재 담당자 뱃지 -->
                    <span
                        v-if="item.isManagerPool"
                        class="inline-flex items-center gap-1.5 px-1 py-0.5 text-xs font-medium text-gray-600 dark:text-gray-300"
                    >
                      <span class="relative flex h-2 w-2">
                        <span class="absolute inline-flex h-full w-full rounded-full bg-green-600 opacity-75"></span>
                        <span class="relative inline-flex rounded-full h-2 w-2 bg-green-700"></span>
                      </span>
                      팀장 풀
                    </span>

                    <span
                        v-else-if="item.isCurrent"
                        class="inline-flex items-center gap-1.5 px-1 py-0.5 text-xs font-medium text-gray-600 dark:text-gray-300"
                    >
                      <span class="relative flex h-2 w-2">
                        <span class="absolute inline-flex h-full w-full rounded-full bg-blue-500 opacity-75"></span>
                        <span class="relative inline-flex rounded-full h-2 w-2 bg-blue-600"></span>
                      </span>
                      현재 담당자
                    </span>
                  </div>
                  <span v-if="item.isCurrent"
                        class="mt-1 text-xs text-gray-500">{{ formatDate(item.assignedAt) }} · {{ item.customerName }}
                  </span>
                </div>

                <div v-if="!item.isCurrent">
                  <button
                      class="px-2 py-1 text-xs rounded-md border transition pointer-events-none"
                      :class="selectedIds.has(item.logId)
                      ? 'bg-gray-900 text-white border-gray-900 hover:bg-gray-800 dark:bg-white dark:text-gray-900 dark:border-white dark:hover:bg-gray-200'
                      : 'text-gray-700 border-gray-300 hover:bg-gray-50 dark:text-gray-300 dark:border-[#28365a] dark:hover:bg-[#16213b]/70'"
                  >
                    {{ selectedIds.has(item.logId) ? '삭제 대상' : '선택' }}
                  </button>
                </div>
                <div v-else>
                  <span class="text-xs text-gray-400 select-none">삭제 불가</span>
                </div>
              </li>
            </ul>

            <div v-else class="p-8 text-center text-gray-500 text-sm">
              이력이 존재하지 않습니다.
            </div>
          </div>
        </div>

        <div class="px-5 py-3 border-t border-gray-200 dark:border-[#1f2a47] flex items-center justify-end gap-2">
          <button
              class="inline-flex h-10 items-center justify-center rounded-lg border border-gray-200 px-4 text-sm
                   hover:bg-gray-50
                   dark:border-[#28365a] dark:bg-[#16213b] dark:text-gray-100 dark:hover:bg-[#233152]/60"
              @click="close"
          >
            취소
          </button>
          <button
              class="inline-flex h-10 items-center justify-center rounded-lg bg-blue-600 text-white px-4 text-sm hover:bg-blue-700 disabled:opacity-60 disabled:cursor-not-allowed"
              :disabled="selectedIds.size === 0"
              @click="confirmDelete"
          >
            {{ selectedIds.size }}건 삭제하기
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import axios from '@/plugins/axios'

const props = defineProps<{
  isOpen: boolean
  customerId: number | null
}>()

const emit = defineEmits(['close', 'refresh'])

const list = ref<any[]>([])
const loading = ref(false)
const selectedIds = ref<Set<number>>(new Set())

// 이력 조회
const fetchHistory = async () => {
  if (!props.customerId) return
  loading.value = true
  list.value = []
  selectedIds.value = new Set()

  try {
    // API 경로는 백엔드 구현에 맞게 조정 필요
    const res = await axios.get(`/api/work/history/${props.customerId}`)
    // 예상 응답 포맷: [{ logId, userName, assignedAt, isCurrent }, ...]
    list.value = res.data
  } catch (e) {
    console.error(e)
    alert('이력을 불러오는데 실패했습니다.')
  } finally {
    loading.value = false
  }
}

// 아이템 토글
const toggleItem = (item: any) => {
  if (item.isCurrent) return // 현재 담당자 클릭 무시

  if (selectedIds.value.has(item.logId)) {
    selectedIds.value.delete(item.logId)
  } else {
    selectedIds.value.add(item.logId)
  }
}

// 전체 선택/해제
const toggleAll = () => {
  const deletableItems = list.value.filter(i => !i.isCurrent)
  const allSelected = deletableItems.every(i => selectedIds.value.has(i.logId))

  if (allSelected) {
    selectedIds.value.clear()
  } else {
    deletableItems.forEach(i => selectedIds.value.add(i.logId))
  }
}

// 삭제 실행
const confirmDelete = async () => {
  if (selectedIds.value.size === 0) return
  if (!confirm(`선택한 ${selectedIds.value.size}건의 담당자 이력을 삭제하시겠습니까?`)) return

  try {
    await axios.post('/api/work/history/delete', {
      logIds: Array.from(selectedIds.value)
    })
    emit('refresh') // 부모에게 새로고침 요청
    close()
  } catch (e) {
    console.error(e)
    alert('삭제 중 오류가 발생했습니다.')
  }
}

const close = () => {
  emit('close')
}

// 모달 열릴 때 데이터 로드
watch(() => props.isOpen, (newVal) => {
  if (newVal && props.customerId) {
    fetchHistory()
  }
})

// 날짜 포맷 함수
const formatDate = (isoString: string) => {
  if (!isoString) return ''
  return isoString.replace('T', ' ')
}
</script>