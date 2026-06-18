<template>
  <div class="overflow-hidden rounded-xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03]">
    <div
        ref="tableScrollHost"
        class="relative max-w-full overflow-x-auto overflow-y-auto custom-scrollbar"
        :style="{ maxHeight: scrollMaxHeight }"
    >
      <table class="w-full table-fixed">
        <!-- table-fixed: 칼럼 너비를 colgroup으로 고정 → 셀이 세로로 커져도 표 전체 재측정(reflow) 없음 -->
        <colgroup>
          <col v-if="showCheckbox" style="width: 44px" />
          <col style="width: 52px" />
          <col v-for="(col, idx) in columns" :key="idx" :style="{ width: colWidth(col) + 'px' }" />
        </colgroup>
        <thead class="sticky top-0 z-20 bg-white dark:bg-gray-900
           shadow-[0_1px_0_0_rgba(229,231,235,0.4)]
           dark:shadow-[0_1px_0_0_rgba(31,41,55,0.7)]">
        <tr class="border-b border-gray-200 dark:border-gray-700" style="white-space: nowrap;">
          <th v-if="showCheckbox" class="px-5 py-3 w-10">
            <input type="checkbox" :checked="allSelected" @change="toggleAll" />
          </th>

          <th class="px-3 py-3 text-left" style="white-space: nowrap;">
            <p class="font-medium text-gray-500 text-theme-xs dark:text-gray-400">번호</p>
          </th>

          <th v-for="(col, idx) in columns" :key="idx" class="px-3 py-3 text-left" style="white-space: nowrap;">
            <p class="font-medium text-gray-500 text-theme-xs dark:text-gray-400">{{ col.label }}</p>
          </th>
        </tr>
        </thead>

        <tbody>
        <!-- 데이터 있을 때 -->
        <tr
            v-for="(row, rowIndex) in data"
            :key="getRowKey(row, rowIndex)"
            :class="[
              'border-y border-gray-100 dark:border-gray-800',
              rowIndex % 2 === 0
                ? 'bg-white dark:bg-gray-900' // 홀수행 흰색
                : 'bg-gray-50 dark:bg-gray-800/60' // 짝수행 회색
            ]"
        >
<!--        <tr v-for="(row, rowIndex) in data"-->
<!--            key="getRowKey(row, rowIndex)"-->
<!--            class="border-t border-gray-100 dark:border-gray-800">-->

          <!-- 체크박스 -->
          <td v-if="showCheckbox" class="px-5 py-4 sm:px-6">
            <input type="checkbox"
                   :checked="isSelected(row)" @change="toggleRow(row)"
                   :disabled="rowSelectable ? !rowSelectable(row) : false"/>
          </td>

          <!-- 행번호 -->
          <td class="px-3 py-4 text-center text-xs text-gray-400">
            {{ rowNumber(rowIndex) }}
          </td>

          <!-- 동적 컬럼 (select-none: 빈 여백 클릭 시 브라우저가 잘린 글자를 선택·스크롤해 드러내는 것 방지) -->
          <td v-for="(col, colIndex) in columns" :key="colIndex" class="px-2 py-4 min-w-0 select-none">
            <!-- 생성일 컬럼 -->
            <span
                v-if="col.key === 'createdAt'"
                class="text-gray-500 text-theme-sm dark:text-gray-400 block text-left leading-tight whitespace-nowrap"
            >
              {{ row[col.key].split(' ')[0] }} <br />
              {{ row[col.key].split(' ')[1] }}
            </span>

            <!-- 텍스트 -->
            <template v-else-if="col.type === 'text'">
              <!-- 이름 인라인 수정 -->
              <div
                  v-if="isTextEditable(col, row)"
                  class="relative h-9 min-w-[6rem]"
              >
                <span
                    v-if="!(editState.row === rowIndex && editState.col === col.key)"
                    class="absolute inset-0 flex items-center text-theme-sm leading-5
                           text-gray-500 dark:text-gray-400
                           cursor-pointer transition-colors
                           hover:text-blue-600 dark:hover:text-blue-400"
                    :title="row[col.key] ?? ''"
                    @click.stop="startEdit(rowIndex, col.key, row[col.key])"
                >
                  <span class="block w-full truncate">{{ row[col.key] ?? '' }}</span>
                </span>

                <div
                    v-else
                    class="text-edit-wrapper absolute inset-0 z-10 flex items-center"
                    @mousedown.stop
                    @click.stop
                >
                  <input
                      :ref="el => setFocusInput(rowIndex, col.key, el)"
                      v-model="editValue"
                      type="text"
                      class="w-full h-full min-w-0 px-2 text-xs border border-blue-500 rounded outline-none
                           focus:ring-1 focus:ring-blue-500
                           dark:bg-gray-800 dark:text-white dark:border-blue-400"
                      @mousedown.stop
                      @click.stop
                      @blur="updateText(row, col.key)"
                      @keyup.enter="$event.target.blur()"
                      @keyup.esc="cancelEdit()"
                  />
                </div>
              </div>

              <!-- 일반 텍스트: 클릭 시 '그 자리 팝업'으로 전체 표시(메모와 동일). 잘린 셀에서만 팝업 -->
              <TextPopupCell
                  v-else
                  :cell-key="getRowKey(row, rowIndex) + '::' + col.key"
                  :text="col.key === 'staff' ? (row[col.key]?.toString().trim() || '') : (row[col.key] ?? '')"
                  :render-html="typeof col.render === 'function' ? col.render(row[col.key], row) : null"
                  :width="ellipsisPx(col)"
              />
            </template>

            <!-- 배지 -->
            <!-- 기본: 70px 고정 단일줄 pill. badgeWrap 컬럼만: 내용폭(최대 badgeMaxWidth)으로 자동, 길면 2~3줄 줄바꿈 -->
            <div v-else-if="col.type === 'badge' && !(col.key === 'prevStatus' && row[col.key] === null)"
                 class="relative inline-block align-middle"
                 :class="badgeExpand(col, row) ? 'min-h-[28px] min-w-[70px]' : 'h-[28px] w-[70px]'"
                 :style="badgeExpand(col, row) ? { maxWidth: badgeMaxPx(col) + 'px' } : null">
                <span
                    v-if="!(editState.row === rowIndex && editState.col === col.key)"
                    class="text-theme-xs font-medium cursor-pointer transition rounded-full"
                    :class="[
                      badgeClass(row[col.key]),
                      col.editable ? 'hover:opacity-80' : '',
                      badgeExpand(col, row)
                        ? 'block px-3 py-1 text-center leading-tight break-words [overflow-wrap:anywhere]'
                        : 'absolute inset-0 flex items-center justify-center px-2 py-0.5'
                    ]"
                    :style="badgeExpand(col, row) ? clamp3 : null"
                    :title="typeof row[col.key] === 'string' ? row[col.key].trim() : row[col.key]"
                    @click.stop="onBadgeClick(row, rowIndex, col)"
                >
                  <!-- render 함수가 있으면 그것을 사용 -->
                  <span v-if="typeof col.render === 'function'" :class="badgeExpand(col, row) ? '' : 'block w-full truncate text-center'" v-html="col.render(row[col.key], row)"></span>

                                <!-- 없으면 빈문자열/공백 → '미배정' 폴백 -->
                  <span v-else :class="badgeExpand(col, row) ? '' : 'block w-full truncate text-center'">
                    {{
                      (typeof row[col.key] === 'string'
                          ? row[col.key].trim()
                          : row[col.key]) || (col.emptyText ?? '미배정')
                    }}
                  </span>
                </span>

              <select
                  v-if="editState.row === rowIndex && editState.col === col.key"
                  v-model="editValue"
                  class="px-1 py-1 text-xs rounded-md border border-gray-300 dark:border-gray-600 focus:border-blue-500 focus:ring-blue-500 dark:bg-gray-800 dark:text-white"
                  :class="col.badgeWrap ? 'block w-full min-w-[120px]' : 'absolute inset-0 w-full h-full'"
                  @change="updateBadge(row, col.key)"
                  @blur="cancelEdit"
              >
                <option v-for="opt in getOptions(col, row)" :key="opt" :value="opt">{{ opt }}</option>
              </select>
            </div>

            <!-- 읽기전용 메모(자체 펼침: 표 전체 리렌더 방지) -->
            <MemoReadCell
                v-else-if="col.type === 'memoRead'"
                :key="getRowKey(row, rowIndex)"
                :cell-key="getRowKey(row, rowIndex)"
                :customer-id="row.id"
                :preview="row[col.key]"
                :width="ellipsisPx(col)"
                :text-only="col.textOnly ? col.textOnly(row) : false"
            />

            <!-- 아이콘 버튼 -->
            <button
                v-else-if="col.type === 'iconButton'"
                class="p-2 transition rounded-md
                     text-gray-500 hover:text-blue-600
                     disabled:text-gray-400 disabled:hover:text-gray-300
                     disabled:opacity-50 disabled:cursor-not-allowed
                     disabled:pointer-events-none"
                :disabled="col.disabled && col.disabled(row)"
                @click="$emit('buttonClick', row, col.key)"
            >
              <component :is="col.icon" class="w-5 h-5" />
            </button>

            <!-- 날짜/시간 -->
            <div v-else-if="col.type === 'date'" class="relative h-9 min-w-[5.5rem] w-auto">
              <!-- 재콜 표시 모드 -->
              <span
                  v-if="props.data[rowIndex]?.status === '재콜'"
                  v-show="!(editState.row === rowIndex && editState.col === col.key)"
                  class="absolute inset-0 flex items-center px-2 text-xs leading-5
                       text-gray-500 dark:text-gray-400
                       rounded-md border border-transparent
                       overflow-hidden whitespace-nowrap text-ellipsis cursor-pointer bg-transparent
                       transition-colors hover:text-blue-600 dark:hover:text-blue-400"
                  @click.stop="startDateEdit(rowIndex, col.key, row[col.key])"
              >
                {{ row[col.key] || '없음' }}
              </span>

              <!-- 재콜 수정 모드(Flatpickr 대상 input) -->
              <input
                  v-if="props.data[rowIndex]?.status === '재콜'"
                  v-show="editState.row === rowIndex && editState.col === col.key"
                  :ref="el => bindDateInput(rowIndex, el)"
                  type="text"
                  class="absolute inset-0 w-full h-full
                       border rounded-md px-2 text-xs leading-5
                       dark:bg-gray-800 dark:text-white
                       focus:ring-1 focus:ring-blue-500 focus:border-blue-500"
              />

              <div
                  v-else-if="props.data[rowIndex]?.status === '내방'"
                  class="absolute inset-0 px-2 py-1 text-xs leading-tight
                     cursor-pointer rounded-md
                     text-gray-500 dark:text-gray-400
                     hover:text-blue-600 dark:hover:text-blue-400
                     group"
                  @click.stop="goVisitSchedule(row)"
              >
                <template v-if="row[col.key]">
                  <template v-if="typeof row[col.key] === 'string'">
                    <!-- time -->
                    <div class="whitespace-nowrap">
                      {{ row[col.key].split(/\s*\/\s*|\n/)[0] }}
                    </div>

                    <!-- room -->
                    <div class="text-[11px] text-gray-500 dark:text-gray-400 whitespace-nowrap
                  group-hover:text-blue-600 dark:group-hover:text-blue-400">
                      {{ row[col.key].split(/\s*\/\s*|\n/).slice(1).join(' / ') || '미등록' }}
                    </div>
                  </template>

                  <template v-else>
                    <!-- 객체로 오는 경우 -->
                    <div class="whitespace-nowrap">
                      {{ row[col.key]?.date }}
                    </div>
                    <div class="text-[11px] text-gray-500 dark:text-gray-400
                  group-hover:text-blue-600 dark:group-hover:text-blue-400">
                      {{ row[col.key]?.room || '미등록' }}
                    </div>
                  </template>
                </template>

                <template v-else>
                  <span class="text-gray-500 dark:text-gray-400
                               group-hover:text-blue-600 dark:group-hover:text-blue-400">
                    없음
                  </span>
                </template>
              </div>
            </div>

            <!-- 최초/업셀 매출 -->
            <div v-else-if="col.type === 'money'" class="relative h-9 min-w-[6rem]">
              <span
                  v-if="col.editable(row) && !(editState.row === rowIndex && editState.col === col.key)"
                  class="absolute inset-0 flex items-center justify-end px-2 text-xs leading-5
                         text-gray-500 dark:text-gray-400
                         cursor-pointer transition-colors hover:text-blue-600 dark:hover:text-blue-400"
                  @click.stop="startEdit(rowIndex, col.key, row[col.key])"
              >
                {{ (row[col.key] && Number(row[col.key]) > 0) ? '$' + Number(row[col.key]).toLocaleString() : '없음' }}
              </span>

              <div
                  v-else-if="editState.row === rowIndex && editState.col === col.key"
                  class="money-edit-wrapper absolute inset-0 z-10 flex items-center shadow-sm"
              >
                <input
                    :ref="el => setFocusInput(rowIndex, col.key, el)"
                    v-model="editValue"
                    type="number"
                    class="no-spinner w-full h-full min-w-0 px-2 text-right text-xs border border-blue-500 rounded-l outline-none focus:ring-1 focus:ring-blue-500 dark:bg-gray-800 dark:text-white dark:border-blue-400"
                    placeholder="0"
                    @blur="updateMoney(row, col.key)"
                    @keyup.enter="$event.target.blur()"
                />
                <span class="flex items-center justify-center h-full px-1.5 text-[10px] text-gray-500 bg-gray-100 border-y border-r border-gray-300 rounded-r dark:bg-gray-700 dark:border-gray-600 dark:text-gray-300 whitespace-nowrap">
                  $
                </span>
              </div>
            </div>
          </td>
        </tr>

        <!-- 데이터 없을 때 -->
        <tr v-if="!data || data.length === 0">
          <td :colspan="(columns.length + (showCheckbox ? 1 : 0)) + 1"
              class="px-4 py-6 text-center text-gray-500 dark:text-gray-400">
            조회된 데이터가 없습니다.
          </td>
        </tr>
        </tbody>
      </table>
    </div>

    <!-- 페이지네이션 -->
    <div class="grid grid-cols-3 items-center px-4 py-3 gap-x-35">
      <div class="flex justify-end">
        <button
            class="w-8 h-7 flex items-center justify-center text-gray-600 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-md disabled:opacity-40"
            :disabled="page === 1"
            @click="page > 1 && $emit('changePage', page - 1)"
        >
          <ChevronLeftIcon class="w-4 h-4" />
        </button>
      </div>

      <div class="flex justify-center space-x-1">
        <button
            v-for="n in visiblePages"
            :key="n"
            :disabled="n === '...'"
            @click="n !== '...' && $emit('changePage', n)"
            :class="[
            'px-3 py-1 text-sm transition',
            n === '...' ? 'text-gray-400 cursor-default'
            : page === n ? 'bg-blue-500 text-white rounded-md'
            : 'text-gray-600 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-md'
          ]"
        >
          {{ n }}
        </button>
      </div>

      <div class="flex justify-start">
        <button
            class="w-8 h-7 flex items-center justify-center text-gray-600 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-md disabled:opacity-40"
            :disabled="page === totalPages"
            @click="page < totalPages && $emit('changePage', page + 1)"
        >
          <ChevronRightIcon class="w-4 h-4" />
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, computed, onMounted, onBeforeUnmount, nextTick } from "vue"
import { ChevronLeftIcon, ChevronRightIcon } from '@heroicons/vue/24/solid'
import MemoReadCell from '@/components/memo/MemoReadCell.vue'
import TextPopupCell from '@/components/tables/basic-tables/TextPopupCell.vue'
import { resetMemo } from '@/composables/useMemoExpand'

function ellipsisPx(col) {
  const raw = (col && typeof col.ellipsis === 'object' && col.ellipsis !== null)
      ? col.ellipsis.width
      : col?.ellipsis;
  const n = (typeof raw === 'number') ? raw : Number.parseInt(raw, 10);
  return Number.isFinite(n) ? n : 100;
}

// table-fixed용 칼럼 너비(px). 기존 셀들이 span 너비(ellipsisPx)로 이미 폭이 잡혀 있어
// 그 값을 그대로 따르고, 셀 좌우 패딩(px-2=16px) 여유만 더한다. 비텍스트 타입은 실제 렌더 폭 기준.
// badgeWrap 배지의 최대 가로폭(px). 이 폭 안에서 줄바꿈, 넘치면 말줄임(3줄).
function badgeMaxPx(col) {
  const n = Number.parseInt(col?.badgeMaxWidth, 10)
  return Number.isFinite(n) ? n : 160
}
// 줄바꿈 배지: 최대 3줄까지만 보이고 넘치면 말줄임
const clamp3 = { display: '-webkit-box', WebkitLineClamp: 3, WebkitBoxOrient: 'vertical', overflow: 'hidden' }

// badgeWrap 확장 임계 글자수(기본 6자)
function badgeThreshold(col) {
  const n = Number.parseInt(col?.badgeWrapThreshold, 10)
  return Number.isFinite(n) ? n : 6
}
// 셀 값 길이
function badgeLen(col, row) {
  const v = row?.[col.key]
  return (typeof v === 'string' ? v.trim() : (v ?? '')).toString().length
}
// 이 배지를 '확장(둥근 모양 유지하며 넓어지고 줄바꿈)'할지 여부.
// badgeWrap 컬럼에서 글자수가 임계값을 넘을 때만 확장. 그 이하는 고정폭 70px 둥근 pill.
function badgeExpand(col, row) {
  if (!col?.badgeWrap) return false
  return badgeLen(col, row) > badgeThreshold(col)
}
// badgeWrap 컬럼의 동적 너비: 현재 데이터 중 가장 긴 값에 맞춰 계산(최대 badgeMaxWidth).
// 전부 짧으면 70px로 좁게, 긴 값이 있을 때만 그만큼만 넓어진다.
const CHAR_PX = 13
function badgeWrapColPx(col) {
  const cap = badgeMaxPx(col)
  let need = 70
  for (const r of (props.data || [])) {
    const len = badgeLen(col, r)
    const w = len <= badgeThreshold(col) ? 70 : Math.min(cap, len * CHAR_PX + 24)
    if (w > need) need = w
  }
  return Math.round(need)
}

const CELL_PAD = 20
function colWidth(col) {
  switch (col?.type) {
    case 'badge':      return (col?.badgeWrap ? badgeWrapColPx(col) : 70) + 26   // 배지 폭 + 좌우 여유
    case 'money':      return 120  // min-w-[6rem]
    case 'date':       return 124  // min-w-[5.5~6rem]
    case 'iconButton': return 60   // p-2 + 아이콘 w-5
    default: break
  }
  if (col?.key === 'createdAt') return 104 // 날짜+시간 2줄
  // text / memoRead — span 너비(ellipsisPx, 기본 100) + 패딩
  return ellipsisPx(col) + CELL_PAD
}

const props = defineProps({
  columns: { type: Array, required: true },
  data: { type: Array, required: true },
  showCheckbox: { type: Boolean, default: false },
  page: { type: Number, default: 1 },
  totalPages: { type: Number, default: 1 },
  rowSelectable: { type: Function, default: null },
  rowKey: { type: [String, Function], default: 'id' },
  pageSize: { type: Number, default: null },
  loading: { type: Boolean, default: false }
})
const emit = defineEmits([
  "rowSelect",
  "selectionCount",
  "badgeUpdate",
  "buttonClick",
  "changePage",
  "DateUpdate",
  "salesUpdate",
  "textUpdate",
  "visitSchedule"
])

// 옵션이 함수인 경우 row를 인자로 실행, 배열인 경우 그대로 반환
function getOptions(col, row) {
  if (typeof col.options === 'function') {
    return col.options(row) || []
  }
  return col.options || []
}

// 텍스트 변경 여부 확인하는 함수
function isTextEditable(col, row) {
  if (typeof col.textEditable === 'function') {
    return col.textEditable(row)
  }
  return !!col.textEditable
}

// 행번호 계산 함수
const displayPage = ref(props.page)
function rowNumber(rowIndex) {
  if (props.pageSize) {
    return (displayPage.value - 1) * props.pageSize + rowIndex + 1
  }
  return rowIndex + 1
}

// 행 고유 키(선택/팝업 단일오픈 식별용)
function getRowKey(row, idx) {
  if (typeof props.rowKey === 'function') return props.rowKey(row, idx)
  if (row && row[props.rowKey] != null) return row[props.rowKey]
  // id가 없으면 페이지-인덱스로라도 충돌 방지
  return `${props.page}-${idx}`
}
// 페이지 변경시 선택 초기화
watch(
    () => props.loading,
    async (loading, prev) => {
      // 페이지 로딩이 완료된 이후 초기화 진행
      if (prev && !loading) {
        displayPage.value = props.page

        // 펼침/에딧 상태만 초기화 (선택은 id 기반이라 페이지 이동에도 유지)
        resetMemo()   // 열린 메모/텍스트 팝업 닫기
        cancelEdit()

        // 스크롤 위치 초기화
        await nextTick()
        const host = tableScrollHost.value
        if (host) {
          host.scrollTop = 0
          host.scrollLeft = 0
        }
      }
    }
)

/* 체크박스 — 행 인덱스가 아니라 고유 id(customer_id) 기반.
   페이지 이동·보기개수 변경에도 선택이 유지되도록 Set(id) + Map(id→row 캐시)로 관리. */
const selectedIds = ref(new Set())     // 선택된 id 집합
const selectedCache = ref(new Map())   // id → row 객체(다른 페이지 선택분도 보관)

function isSelected(row) {
  return selectedIds.value.has(getRowKey(row))
}
// 현재 페이지에서 선택 가능한 행들
function selectableRows() {
  return props.data.filter(row => (props.rowSelectable ? props.rowSelectable(row) : true))
}
const allSelected = computed(() => {
  const rows = selectableRows()
  return rows.length > 0 && rows.every(row => selectedIds.value.has(getRowKey(row)))
})
function selectRow(row) {
  const id = getRowKey(row)
  selectedIds.value.add(id)
  selectedCache.value.set(id, row)
}
function deselectRow(row) {
  const id = getRowKey(row)
  selectedIds.value.delete(id)
  selectedCache.value.delete(id)
}
function commitSelection() {
  // Set/Map in-place 변경 → 새 참조로 교체해 반응성 트리거
  selectedIds.value = new Set(selectedIds.value)
  selectedCache.value = new Map(selectedCache.value)
  emitSelected()
}
function toggleRow(row) {
  if (selectedIds.value.has(getRowKey(row))) deselectRow(row)
  else selectRow(row)
  commitSelection()
}
function toggleAll(e) {
  const rows = selectableRows()
  if (e.target.checked) rows.forEach(selectRow)
  else rows.forEach(deselectRow)
  commitSelection()
}
function emitSelected() {
  emit("rowSelect", [...selectedCache.value.values()])
  emit("selectionCount", selectedIds.value.size)
}
/* 1~N 일괄선택: 부모(툴바)가 서버에서 받은 상위 N개 행을 넘겨 선택 설정(덮어쓰기). */
function applyBulkSelect(rows) {
  // 선택 불가 행(rowSelectable=false)은 제외 — 1~N이 disabled 행을 선택하지 않도록
  const arr = (Array.isArray(rows) ? rows : [])
      .filter(r => (props.rowSelectable ? props.rowSelectable(r) : true))
  selectedIds.value = new Set(arr.map(r => getRowKey(r)))
  selectedCache.value = new Map(arr.map(r => [getRowKey(r), r]))
  emitSelected()
}

/* 배지 수정 */
const editState = ref({ row: null, col: null })
const editValue = ref(null)
function onBadgeClick(row, rowIndex, col) {
  // status 배지만 대상
  if (col.key === 'status') {
    // 클릭 시점의 값은 아직 과거 상태다
    if (row.status === '내방' && row.reservation) {
      alert('내방 예약시간이 있어 상태 변경이 불가합니다.\n내방일정 메뉴에서 일정삭제 후 시도하세요.');
      return;
    }
  }

  // 기존 로직 그대로
  if (typeof col.editable === 'function') {
    if (col.editable(row)) {
      startEdit(rowIndex, col.key, row[col.key])
    }
  } else if (col.editable) {
    startEdit(rowIndex, col.key, row[col.key])
  }
}
function startEdit(rowIndex, colKey, currentValue) {
  // 다음 틱에서 편집 모드로
  setTimeout(() => {
    editState.value = { row: rowIndex, col: colKey }
    editValue.value = currentValue
  }, 0)
}
function cancelEdit() { editState.value = { row: null, col: null } }
function updateBadge(row, key) {
  // 낙관적 선반영 금지: 값은 부모가 확인/검증 통과 후 직접 반영한다.
  // (부모에서 confirm을 띄우는 경우, 확인 전에 배지가 먼저 바뀌는 문제 방지)
  const next = editValue.value
  cancelEdit()
  emit("badgeUpdate", row, key, next)
}

// 금액 저장 함수
function updateMoney(row, key) {
  const val = editValue.value;

  // 1. 빈 문자열, null, 숫자가 아닌 경우 -> 저장 안 하고 그냥 편집 종료
  if (val === '' || val === null || isNaN(val)) {
    cancelEdit();
    return;
  }

  // 2. 값이 유효할 때만 부모에게 이벤트 전송
  emit("salesUpdate", row, key, val);

  // 3. 편집 모드 종료
  cancelEdit();
}

// 이름 변경 함수
function updateText(row, key) {
  const val = (editValue.value ?? '').toString().trim()

  if (!val) {
    cancelEdit()
    return
  }

  emit("textUpdate", row, key, val)
  cancelEdit()
}

// 입력창 자동 포커스용
function setFocusInput(rowIndex, colKey, el) {
  if (editState.value.row === rowIndex && editState.value.col === colKey && el) {
    el.focus();
  }
}

/* --- 날짜/시간: flatpickr --- */
import flatpickr from "flatpickr"
import { Korean } from "flatpickr/dist/l10n/ko.js"
import "flatpickr/dist/flatpickr.css"

const dateInputs = ref({})   // rowIndex -> element
const fpInstances = ref({})  // rowIndex -> flatpickr

function startDateEdit(rowIndex, colKey, currentValue) {
  const row = props.data[rowIndex]
  if (row?.status !== "재콜") return

  // 상태가 재콜일 경우에만 수정할 수 있도록
  setTimeout(() => {
    editState.value = { row: rowIndex, col: colKey }
    editValue.value = currentValue
  }, 0)
}

// 안전한 ref 바인더 (인라인 함수 대신 사용)
function bindDateInput(idx, el) {
  if (!dateInputs.value) dateInputs.value = {}
  if (el) dateInputs.value[idx] = el
  else if (dateInputs.value[idx]) delete dateInputs.value[idx]
}

function fmtLocal(dt) {
  const y = dt.getFullYear()
  const m = String(dt.getMonth() + 1).padStart(2, '0')
  const d = String(dt.getDate()).padStart(2, '0')
  const hh = String(dt.getHours()).padStart(2, '0')
  const mm = String(dt.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${d} ${hh}:${mm}`
}

watch(
    () => editState.value,
    async (state) => {
      if (!state) return
      const { row, col } = state
      if ((props.columns.find(c => c.key === col)?.type) !== "date" || row == null) return;

      await nextTick()
      const el = dateInputs.value && dateInputs.value[row]
      if (!el) return

      // 기존 인스턴스 정리
      if (fpInstances.value[row]) {
        try { fpInstances.value[row].destroy() } catch {}
        delete fpInstances.value[row]
      }

      fpInstances.value[row] = flatpickr(el, {
        enableTime: true,
        time_24hr: true,
        dateFormat: "n월 j일 H:i",
        locale: Korean,
        allowInput: true,
        defaultDate: props.data[row][col] || null,

        // 실시간 반영
        onReady: (_d, _str, ins) => {
          ins.open()

          // ====== 1) 시간 인풋 실시간 커밋 유지 ======
          if (!ins._commitAttached) {
            const commit = () => {
              const hh = String(parseInt(ins.hourElement?.value || 0, 10)).padStart(2, "0")
              const mm = String(parseInt(ins.minuteElement?.value || 0, 10)).padStart(2, "0")

              let base = ins.selectedDates[0]
              // 만약 clear 후라서 날짜가 없다면 → 오늘 날짜 기준으로
              if (!base) {
                base = new Date()
                // 날짜만 오늘로 잡고, 선택 배열에도 넣어줌
                ins.selectedDates = [base]
              }

              // const base = ins.selectedDates[0] || new Date()
              const ymd  = ins.formatDate(base, "n월 j일")
              const val  = `${ymd} ${hh}:${mm}`
              ins.setDate(val, false, "n월 j일 H:i")   // 값만 업데이트 → emit은 onValueUpdate가 담당
            }
            ins.hourElement?.addEventListener("input", commit)
            ins.minuteElement?.addEventListener("input", commit)
            ins._commitAttached = true
          }

          // ====== 2) 달력 팝업 내부 X ======
          if (!ins._popupClearBtn) {
            const btn = document.createElement("button")
            btn.type = "button"
            btn.textContent = "✕"
            btn.title = "입력값 지우기"
            btn.className =
                "fp-clear-btn absolute top-1 right-1 w-6 h-6 flex items-center justify-center " +
                "text-xs text-gray-500 hover:text-gray-700 rounded z-50";

            btn.addEventListener("mousedown", (e) => {
              // 팝업 닫히는 버블링 방지
              e.preventDefault()
              e.stopPropagation()

              // flatpickr 비우기
              ins.clear()

              // 중복 방지
              ins._wasCleared = true

              // 곧바로 닫기
              ins.close()
            })

            // 팝업 컨테이너는 position이 이미 잡혀있으므로 별도 변경 불필요
            ins.calendarContainer.appendChild(btn)
            ins._popupClearBtn = btn
          }

          // ====== 3) 입력창 안의 X ======
          if (!ins._clearButton) {
            const btn = document.createElement("button")
            btn.type = "button"
            btn.textContent = "✕"
            btn.title = "입력값 지우기"
            btn.className =
                "absolute left-[75%] top-1/2 -translate-y-1/2 text-gray-400 hover:text-red-500"

            btn.addEventListener("mousedown", (e) => {
              e.preventDefault();
              e.stopPropagation();

              // 1) flatpickr 비우기
              ins.clear();

              // 3) onClose에서 중복 반영 방지 플래그
              ins._wasCleared = true;

              // 4) 곧바로 닫기
              ins.close();
            });

            // input wrapper에 버튼 넣기
            el.parentNode.style.position = "relative"
            el.parentNode.appendChild(btn)
            ins._clearButton = btn
          }
        },
        onClose: (dates, _str, ins) => {
          let displayStr = ""
          let apiStr = ""

          if (ins._wasCleared) {
            // X 눌러서 지운 경우
            displayStr = ""
            apiStr = ""
          } else if (dates?.length) {
            // 정상적으로 날짜가 선택된 경우
            displayStr = (ins.input.value || "").trim()
            apiStr = ins.formatDate(dates[0], "Y-m-d H:i")
          } else {
            // clear 이후, 날짜는 없지만 시/분 필드에 값이 있는 경우
            const hh = parseInt(ins.hourElement?.value || "0", 10)
            const mm = parseInt(ins.minuteElement?.value || "0", 10)
            if (!isNaN(hh) || !isNaN(mm)) {
              const base = new Date()
              base.setHours(hh)
              base.setMinutes(mm)
              displayStr = ins.formatDate(base, "n월 j일 H:i")
              apiStr = ins.formatDate(base, "Y-m-d H:i")
            }
          }
          ins._wasCleared = false;

          // 셀 반영 + 부모에 emit
          props.data[row][col] = displayStr;
          emit("DateUpdate", props.data[row], col, apiStr);

          cancelEdit()

          if (ins._clearButton) {
            ins._clearButton.remove()
            ins._clearButton = null
          }
        }
      })
    },
    { deep: true }
)

onBeforeUnmount(() => {
  Object.values(fpInstances.value || {}).forEach(ins => { try { ins.destroy() } catch {} })
})

// 내방일정 : 새탭 이동
function goVisitSchedule(row) {
  emit("visitSchedule", row)
}

/* 바깥 클릭 시 편집 종료 (달력/입력/select/배지/머니인풋은 예외) */
function onDocMouseDown(e) {
  if (editState.value.row === null) return
  const t = e.target
  if (
      t?.closest?.(".flatpickr-calendar") ||
      t?.closest?.("input.flatpickr-input") ||
      t?.closest?.("select") ||
      t?.closest?.(".editable-badge") ||
      t?.closest?.(".fp-clear-btn") ||
      t?.closest?.(".money-edit-wrapper") ||
      t?.closest?.(".text-edit-wrapper")
  ) return
  cancelEdit()
}
onMounted(() => { document.addEventListener("mousedown", onDocMouseDown) })
onBeforeUnmount(() => { document.removeEventListener("mousedown", onDocMouseDown) })

/* 배지 색상 */
const badgeClass = (value) => {
  switch (value) {
    case "본사":
    case "관리자":
    case "탈퇴":
    case "거절":
    case "회수":
      return "bg-[#F8D7DA] text-[#B23A48] dark:bg-[#4A1C1C]/80 dark:text-[#F28B82]/85"
    case "프로":
    case "신규":
    case "승인":
    case "최초":
      return "bg-[#D6E4FF] text-[#1E40AF] dark:bg-[#1A2B4C] dark:text-[#8AB4F8]"
    case "팀장":
    case "유효":
    case "재콜":
      return "bg-[#D1F2D6] text-[#2F855A] dark:bg-[#1B3A2E] dark:text-[#5FA97A]"
    case "센터장":
    case "부재1":
    case "부재2":
    case "부재3":
    case "부재4":
    case "부재5":
    case "대기":
    case "미할당":
      return "bg-[#FFF3CD] text-[#B7791F] dark:bg-[#4A3A12]/60 dark:text-[#C9A94B]/80"
    case "자연풀":
    case "공개":
      return "bg-[#D0F0F0] text-[#00796B] dark:bg-[#1B3C3C] dark:text-[#66C2A5]"
    case "전문가":
    case "카피":
    case "허용":
      return "bg-[#E0D7F8] text-[#5B4B9A] dark:bg-[#312C5C] dark:text-[#B6A9E3]"
    case "가망":
    case "미지정":
      return "bg-[#F4E1CC] text-[#944E2A] dark:bg-[#3A2417]/70 dark:text-[#F3C6A6]/80"
    case "내방":
      return "bg-[#FCE7F3] text-[#C026D3] dark:bg-[#3B1126] dark:text-[#F9A8D4]/80"
    case '':
      return "bg-[#FFE7D1] text-[#9A3412] dark:bg-[#3A2417] dark:text-[#FDBA74]"
    default:
      return "bg-[#E5E7EB] text-[#4B5563] dark:bg-[#374151] dark:text-[#D1D5DB]"
  }
}

/* 페이지네이션 */
const visiblePages = computed(() => {
  const total = props.totalPages
  const current = props.page
  const delta = 2
  const pages = []
  if (total <= 7) { for (let i=1;i<=total;i++) pages.push(i); return pages }
  if (current <= 3) { pages.push(1,2,3,4,5,'...',total) }
  else if (current >= total - 2) { pages.push(1,'...',total-4,total-3,total-2,total-1,total) }
  else { pages.push(1,'...',current-delta,current-1,current,current+1,current+delta,'...',total) }
  return pages
})

// 선택 해제 메서드
function clearSelection() {
  selectedIds.value = new Set();
  selectedCache.value = new Map();
  emitSelected();
}
// 개별 선택 해제 (선택 목록 모달에서 특정 행만 빼기)
function deselect(row) {
  deselectRow(row);
  commitSelection();
}
defineExpose({ clearSelection, applyBulkSelect, deselect });

// 스크롤/휠 시 달력 모두 닫기
const tableScrollHost = ref(null)

// --- 스크롤/휠 시 테이블 내 모든 달력 강제 닫기 ---
const closeAllPickersOnScroll = (e) => {
  // 달력 위에서의 스크롤은 무시하고 싶다면 주석 해제
  // const t = e.target
  // if (t && t.closest && t.closest('.flatpickr-calendar')) return

  const map = fpInstances.value || {}
  for (const k in map) {
    const ins = map[k]
    if (ins?.isOpen) {
      try { ins.close() } catch {}
    }
  }
}

// 표 높이 동적 계산 - 스크롤 사용을 위하여
const scrollMaxHeight = ref('500px')
function updateScrollHeight() {
  const el = tableScrollHost.value
  if (!el) return

  const rect = el.getBoundingClientRect()
  const viewportHeight = window.innerHeight || document.documentElement.clientHeight
  const bottomPadding = 110// 카드 아래 여백 조금 남기기

  let h = viewportHeight - rect.top - bottomPadding
  if (h < 200) h = 200 // 너무 작아지지 않게 최소 높이

  scrollMaxHeight.value = `${h}px`
}

// 컨트롤 키 + 휠로 가로 스크롤
const handleHorizontalWheel = (e) => {
  const el = tableScrollHost.value
  if (!el) return

  // 컨트롤 키 누른 상태여야지만 가로 스크롤 가능
  if (!e.ctrlKey) return

  // (if문 통과했음 -> 컨트롤 키 누른 상태란 뜻)
  // 따라서 기본 세로 스크롤을 막아 줘야함
  e.preventDefault()

  // deltaMode == 1 이면 "줄" 단위라 조금 키워줌
  const base = e.deltaY !== 0 ? e.deltaY : e.deltaX
  const factor = e.deltaMode === 1 ? 20 : 1

  el.scrollLeft += base * factor
}

onMounted(() => {
  // 새 표가 뜰 때(라우트 전환 등) 이전에 열려있던 읽기전용 메모 펼침 상태 초기화
  resetMemo()

  // 윈도우 스크롤/휠/터치
  window.addEventListener('wheel',     closeAllPickersOnScroll, { passive: true, capture: true })
  window.addEventListener('scroll',    closeAllPickersOnScroll, { passive: true, capture: true })
  window.addEventListener('touchmove', closeAllPickersOnScroll, { passive: true, capture: true })

  // 테이블 자체 스크롤(overflow 컨테이너)도 별도로 잡아줘야 함 (scroll은 버블 안 됨)
  const host = tableScrollHost.value
  if (host) {
    host.addEventListener('wheel',     closeAllPickersOnScroll, { passive: true, capture: true })
    host.addEventListener('scroll',    closeAllPickersOnScroll, { passive: true, capture: true })
    host.addEventListener('touchmove', closeAllPickersOnScroll, { passive: true, capture: true })
  }

  // 표 높이 동적 계산
  updateScrollHeight()
  window.addEventListener('resize', updateScrollHeight)
  window.addEventListener('orientationchange', updateScrollHeight)

  // 컨트롤 키 + 휠로 가로 스크롤
  host.addEventListener('wheel', handleHorizontalWheel, { passive: false })
})

onBeforeUnmount(() => {
  window.removeEventListener('wheel',     closeAllPickersOnScroll, true)
  window.removeEventListener('scroll',    closeAllPickersOnScroll, true)
  window.removeEventListener('touchmove', closeAllPickersOnScroll, true)

  const host = tableScrollHost.value
  if (host) {
    host.removeEventListener('wheel',     closeAllPickersOnScroll, true)
    host.removeEventListener('scroll',    closeAllPickersOnScroll, true)
    host.removeEventListener('touchmove', closeAllPickersOnScroll, true)
  }

  window.removeEventListener('resize', updateScrollHeight)
  window.removeEventListener('orientationchange', updateScrollHeight)

  host.removeEventListener('wheel', handleHorizontalWheel)
})

</script>

<style scoped>
/* 크롬, 사파리, 엣지, 오페라 */
input.no-spinner::-webkit-outer-spin-button,
input.no-spinner::-webkit-inner-spin-button {
  -webkit-appearance: none;
  margin: 0;
}

/* 파이어폭스 */
input.no-spinner {
  -moz-appearance: textfield;
}
</style>