<template>
  <div class="fixed inset-0 z-[100000] flex items-center justify-center bg-black/40 p-4"
       @click.self="$emit('close')"
       @keydown.esc.prevent.stop="$emit('close')">
    <div class="w-full max-w-[680px] rounded-2xl bg-white dark:bg-gray-900 shadow-xl border border-gray-200 dark:border-gray-800">
      <!-- 헤더 -->
      <div class="px-5 py-4 border-b border-gray-200 dark:border-gray-800 flex items-center justify-between">
        <h3 class="text-base font-semibold text-gray-800 dark:text-gray-100">{{ title }}</h3>
        <button class="text-gray-400 hover:text-gray-600 dark:hover:text-gray-300" @click="$emit('close')">✕</button>
      </div>

      <!-- 바디 -->
      <div class="px-5 py-4 space-y-3">
        <p class="text-sm text-gray-500 dark:text-gray-400">
          선택된 대상: <b class="text-gray-700 dark:text-gray-200">{{ rows.length }}</b>명
          <span class="text-gray-400 dark:text-gray-500"> · 해제할 대상을 골라 제거하세요.</span>
        </p>

        <div v-if="rows.length"
             class="max-h-[60vh] overflow-auto rounded-lg border border-gray-200 dark:border-gray-700">
          <table class="w-full border-collapse text-sm">
            <thead class="sticky top-0 z-10 bg-gray-100 dark:bg-gray-800">
              <tr>
                <th v-for="col in columns" :key="col.key"
                    class="border-b border-r border-gray-200 dark:border-gray-700 py-2.5 text-xs font-semibold text-gray-600 dark:text-gray-300 whitespace-nowrap"
                    :class="col.type === 'badge' ? 'px-5 text-center' : 'px-3 text-left'">
                  {{ col.label }}
                </th>
                <th class="border-b border-gray-200 dark:border-gray-700 px-3 py-2.5 text-right text-xs font-semibold text-gray-600 dark:text-gray-300 w-px whitespace-nowrap">
                  해제
                </th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, idx) in rows"
                  :key="keyOf(row, idx)"
                  class="odd:bg-white even:bg-gray-50 dark:odd:bg-gray-900 dark:even:bg-gray-800/40
                         hover:bg-blue-50/60 dark:hover:bg-gray-800/70">
                <td v-for="(col, ci) in columns"
                    :key="col.key"
                    class="border-b border-r border-gray-100 dark:border-gray-800 py-2.5 align-middle whitespace-nowrap"
                    :class="col.type === 'badge' ? 'px-5 text-center' : 'px-3'"
                    :title="String(rawVal(row, col) ?? '')">
                  <!-- 상태/직전상태: 색 배지 (가로 폭 통일 + 가운데 정렬) -->
                  <span v-if="col.type === 'badge' && !isEmpty(row, col)"
                        class="inline-flex h-[26px] w-[60px] items-center justify-center rounded-full text-xs font-medium"
                        :class="statusBadgeColor(rawVal(row, col))">
                    {{ display(row, col) }}
                  </span>
                  <!-- 텍스트(이름/그 외): 항상 말줄임. 전체 내용은 hover 툴팁(title)으로 -->
                  <span v-else
                        class="block truncate"
                        :class="ci === 0 ? 'max-w-[12rem] text-sm font-semibold text-gray-800 dark:text-gray-100' : 'max-w-[15rem] text-xs text-gray-500 dark:text-gray-400'">
                    {{ display(row, col) }}
                  </span>
                </td>
                <td class="border-b border-gray-100 dark:border-gray-800 px-3 py-2.5 text-right whitespace-nowrap">
                  <button
                      type="button"
                      @click="$emit('deselect', row)"
                      class="rounded-md border border-gray-300 px-2.5 py-1 text-xs text-gray-600 transition
                             hover:bg-gray-100 hover:text-gray-800
                             dark:border-gray-700 dark:text-gray-300 dark:hover:bg-gray-800/70 dark:hover:text-gray-100">
                    해제
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <p v-else class="py-10 text-center text-sm text-gray-400 dark:text-gray-500">
          선택된 대상이 없습니다.
        </p>
      </div>

      <!-- 푸터 -->
      <div class="px-5 py-3 border-t border-gray-200 dark:border-gray-800 flex items-center justify-between gap-2">
        <button
            class="h-10 rounded-lg border border-gray-200 px-4 text-sm text-gray-600 transition
                   hover:bg-gray-50 disabled:opacity-50
                   dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100 dark:hover:bg-gray-700/60"
            :disabled="!rows.length"
            @click="$emit('clearAll')">
          전체 해제
        </button>
        <button
            class="h-10 rounded-lg bg-blue-600 px-4 text-sm text-white transition hover:bg-blue-500"
            @click="$emit('close')">
          닫기
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { statusBadgeColor } from '@/composables/memoFormat.js'

const props = defineProps({
  rows:    { type: Array,  default: () => [] },         // 선택된 행 객체 배열
  columns: { type: Array,  required: true },            // [{ key, label, type?: 'badge'|'date' }]
  title:   { type: String, default: '선택된 대상' },
  rowKey:  { type: [String, Function], default: 'id' }, // :key 용 (식별은 부모가 행 객체로 처리)
})
defineEmits(['close', 'deselect', 'clearAll'])

function keyOf(row, idx) {
  if (typeof props.rowKey === 'function') return props.rowKey(row, idx)
  if (row && row[props.rowKey] != null) return row[props.rowKey]
  return idx
}
function rawVal(row, col) {
  return col ? row?.[col.key] : undefined
}
function isEmpty(row, col) {
  const v = rawVal(row, col)
  return v === null || v === undefined || v === ''
}
function display(row, col) {
  if (!col) return ''
  if (isEmpty(row, col)) return '-'
  const v = rawVal(row, col)
  if (col.type === 'date') return String(v).slice(0, 10) // YYYY-MM-DD 만
  return v
}
</script>
