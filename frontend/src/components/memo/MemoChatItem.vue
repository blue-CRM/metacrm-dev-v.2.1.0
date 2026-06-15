<template>
  <!-- 인라인 수정 모드 -->
  <div
    v-if="editing"
    class="rounded-lg border-[1.5px] border-blue-500 bg-white dark:bg-gray-800 p-1.5"
  >
    <textarea
      ref="editRef"
      :value="editText"
      rows="1"
      class="w-full resize-none border-none outline-none bg-transparent
             text-[13px] leading-relaxed text-gray-900 dark:text-gray-100 min-h-[20px]"
      @input="onEditInput"
      @keydown="onEditKey"
    ></textarea>
    <div class="flex justify-end gap-1.5 mt-1">
      <button
        class="text-[11px] px-2.5 py-[3px] rounded-2xl border border-gray-200 dark:border-gray-600
               bg-white dark:bg-gray-700 text-gray-500 dark:text-gray-300"
        @click="$emit('cancel')"
      >
        취소
      </button>
      <button
        class="text-[11px] px-2.5 py-[3px] rounded-2xl border-none bg-blue-500 hover:bg-blue-600 text-white font-semibold"
        @click="$emit('save')"
      >
        저장
      </button>
    </div>
  </div>

  <!-- 보기 모드: [말풍선] [시간영역] [hover ✏️✕] -->
  <div v-else class="group/item flex items-end gap-1">
    <!-- 말풍선 -->
    <div
      :class="[
        'rounded-xl px-3.5 py-2.5 text-[13px] leading-relaxed whitespace-pre-wrap break-words min-w-0 [overflow-wrap:anywhere]',
        isSystem
          ? 'bg-gray-100 dark:bg-gray-800 border border-gray-200 dark:border-gray-700 text-gray-500 dark:text-gray-400'
          : 'bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 text-gray-900 dark:text-gray-100',
      ]"
    >
      {{ chat.content }}
    </div>

    <!-- 시간영역: 항상 작성시각(createdAt) 표시. 수정시각(updatedAt)은 DB 기록만 하고 UI 미표시 -->
    <div class="flex flex-col items-start leading-tight pb-0.5 shrink-0 select-none">
      <span class="text-[10px] text-gray-400 dark:text-gray-500 whitespace-nowrap">
        {{ fmtDateTime(chat.createdAt) }}
      </span>
    </div>

    <!-- hover 수정/삭제 (editable && 읽기전용 아닐 때만) -->
    <div
      v-if="chat.editable && !readonly"
      class="flex gap-0.5 pb-0.5 shrink-0 opacity-0 group-hover/item:opacity-100 transition-opacity"
    >
      <button
        title="수정"
        class="w-6 h-6 rounded-md flex items-center justify-center
               text-gray-400 hover:text-gray-700 hover:bg-gray-100
               dark:hover:text-gray-200 dark:hover:bg-gray-700 transition"
        @click="$emit('startEdit', chat)"
      >
        <svg width="13" height="13" viewBox="0 0 14 14" fill="none">
          <path
            d="M9 2.5l2.5 2.5L4.5 12 2 12l0-2.5L9 2.5z"
            stroke="currentColor" stroke-width="1.3" stroke-linejoin="round" fill="none"
          />
        </svg>
      </button>
      <button
        title="삭제"
        class="w-6 h-6 rounded-md flex items-center justify-center
               text-gray-400 hover:text-red-600 hover:bg-red-50
               dark:hover:bg-red-900/30 transition"
        @click="onDelete"
      >
        <svg width="13" height="13" viewBox="0 0 14 14" fill="none">
          <path
            d="M3 4h8M5.5 4V2.5h3V4M4 4l.5 8h5l.5-8M6 6.5v3M8 6.5v3"
            stroke="currentColor" stroke-width="1.3" stroke-linecap="round"
            stroke-linejoin="round" fill="none"
          />
        </svg>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'
import { fmtDateTime, isSystemType } from '@/composables/memoFormat'

const props = defineProps({
  /** 채팅 1건. .editable(서버/클라 산출), .content, .createdAt, .updatedAt, .chatType */
  chat: { type: Object, required: true },
  /** 이 항목이 인라인 수정 모드인지 */
  editing: { type: Boolean, default: false },
  /** 수정 중 텍스트(부모 상태) */
  editText: { type: String, default: '' },
  /** 읽기 전용(view) 모드 — true면 hover ✏️✕ 숨김 */
  readonly: { type: Boolean, default: false },
})

const emit = defineEmits(['startEdit', 'remove', 'save', 'cancel', 'update:editText'])

const isSystem = ref(isSystemType(props.chat.chatType))
watch(
  () => props.chat.chatType,
  (v) => (isSystem.value = isSystemType(v)),
)

const editRef = ref(null)
watch(
  () => props.editing,
  async (on) => {
    if (on) {
      await nextTick()
      const el = editRef.value
      if (el) {
        el.focus()
        el.style.height = 'auto'
        el.style.height = el.scrollHeight + 'px'
      }
    }
  },
)

function onEditInput(e) {
  emit('update:editText', e.target.value)
  e.target.style.height = 'auto'
  e.target.style.height = e.target.scrollHeight + 'px'
}

function onEditKey(e) {
  if (e.key === 'Escape') {
    e.preventDefault()
    emit('cancel')
    return
  }
  // Enter=저장, Shift+Enter=줄바꿈, IME(한글 조합) 중엔 무시 — 입력창과 동일
  if (e.key === 'Enter' && !e.shiftKey && !e.isComposing) {
    e.preventDefault()
    emit('save')
  }
}

function onDelete() {
  if (confirm('이 메모를 삭제할까요?')) emit('remove', props.chat.chatId)
}
</script>
