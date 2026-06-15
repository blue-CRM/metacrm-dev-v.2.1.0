<template>
  <!--
    읽기전용 메모 셀.
    ⚡ 표를 늘리지 않는다 — 클릭하면 그 자리에 작은 '팝업'(body로 텔레포트한 고정 오버레이)을 띄운다.
       셀/행 높이가 변하지 않으므로 table-auto/fixed 표의 레이아웃 재계산(reflow)이 0 → 100개에서도 가볍다.
    - 팝업 안: 휠 스크롤로 읽기, 드래그로 텍스트 선택 가능(닫히지 않음).
    - 클릭(팝업 안이든 밖이든) / 바깥 스크롤·드래그 / 다른 메모 클릭 → 닫힘(단일 오픈).
  -->
  <div>
    <!-- 미리보기(앵커). 행 높이 고정 — 절대 커지지 않음 -->
    <!-- 클릭 영역을 '글자'에만 한정(inline-block) — 셀의 빈 여백을 눌러도 아무 일 없음 -->
    <button
        ref="anchorEl"
        type="button"
        class="inline-block align-middle max-w-full rounded focus:outline-none text-left"
        @click="toggle"
    >
      <span
          class="block text-theme-sm leading-tight truncate whitespace-nowrap overflow-hidden text-gray-500 dark:text-gray-400"
          :style="{ width: width + 'px' }"
      >
        {{ preview ?? '' }}
      </span>
    </button>

    <!-- 클릭한 자리에 뜨는 팝업 (우하단 핸들로 크기 조절 가능 / 본문과 구분되는 진한 테두리·그림자) -->
    <Teleport to="body">
      <div v-if="expanded" ref="popupEl" class="fixed z-[2147483000]" :style="popupStyle">
        <!-- 텍스트 전용(중복 행) -->
        <div
            v-if="textOnly"
            class="resize overflow-auto rounded-lg border border-gray-200/90 dark:border-gray-700 ring-1 ring-gray-900/[0.04] shadow-xl bg-white dark:bg-gray-900 px-3 py-2.5 w-[300px] min-w-[160px] max-w-[85vw] min-h-[44px] max-h-[280px] text-theme-sm leading-relaxed whitespace-pre-wrap break-words text-gray-700 dark:text-gray-200"
        >
          {{ preview ?? '' }}
        </div>
        <!-- 채팅 메모 -->
        <div
            v-else
            class="resize overflow-auto rounded-lg border border-gray-200/90 dark:border-gray-700 ring-1 ring-gray-900/[0.04] shadow-xl bg-white dark:bg-gray-900 w-[300px] min-w-[160px] max-w-[85vw] min-h-[48px] max-h-[280px]"
        >
          <MemoCellChats :customer-id="customerId" />
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, watch, onBeforeUnmount, nextTick } from 'vue'
import MemoCellChats from './MemoCellChats.vue'
import { toggleMemo, clearMemoIf } from '@/composables/useMemoExpand'

const props = defineProps({
  customerId: { type: [Number, String], required: true },
  // 행마다 유일한 키(예: origin-id). customerId만 쓰면 고객/중복 id가 겹칠 수 있어 별도로 받는다.
  cellKey: { type: [String, Number], required: true },
  preview: { type: [String, Number, null], default: '' },
  width: { type: Number, default: 150 },
  textOnly: { type: Boolean, default: false },
})

const anchorEl = ref(null)
const popupEl = ref(null)
const popupStyle = ref({})
// 펼침 상태는 이 셀의 로컬 ref(전역 구독 X) → 다른 셀을 열어도 이 셀은 리렌더되지 않음.
const expanded = ref(false)
const key = () => String(props.cellKey)

const PW = 300, PH = 300, GAP = 4
function place() {
  const a = anchorEl.value
  if (!a) return
  const r = a.getBoundingClientRect()
  const vw = window.innerWidth, vh = window.innerHeight
  // 가로: 셀 왼쪽 정렬하되 화면 밖으로 안 나가게
  const left = Math.max(8, Math.min(r.left, vw - PW - 8))
  const style = { left: `${left}px` }
  // 세로: 아래 공간 부족하면 위로
  if (vh - r.bottom < PH + GAP + 8 && r.top > vh - r.bottom) {
    style.bottom = `${vh - r.top + GAP}px`
  } else {
    style.top = `${r.bottom + GAP}px`
  }
  style.visibility = 'visible'
  popupStyle.value = style
}

function toggle() {
  // 레지스트리가 "이전 셀 닫기 + 이 셀 열기"를 2개 셀만 토글로 처리(단일 오픈)
  toggleMemo(key(), (v) => {
    // 위치를 잡기 전(다음 틱 place 전)에는 화면 밖에 숨겨 깜빡임 방지
    if (v) popupStyle.value = { left: '-9999px', top: '0px', visibility: 'hidden' }
    expanded.value = v
  })
}
function close() {
  if (!expanded.value) return
  expanded.value = false
  clearMemoIf(key())
}

// 클릭과 드래그를 구분한다.
//  - 팝업 '밖' mousedown(클릭이든 드래그 시작이든) → 즉시 닫기
//  - 팝업 '안' → mousedown 위치 기록 후, mouseup에서 거의 안 움직였으면(=클릭) 닫고, 움직였으면(=드래그) 유지
let downInside = false
let downX = 0, downY = 0
const DRAG_THRESHOLD = 5

function onPointerDown(e) {
  if (anchorEl.value?.contains(e.target)) return // 앵커 토글은 @click이 처리
  if (popupEl.value?.contains(e.target)) {
    downInside = true; downX = e.clientX; downY = e.clientY
    return
  }
  close() // 바깥 클릭/드래그 시작 → 닫기
}
function onPointerUp(e) {
  if (!downInside) return
  downInside = false
  const moved = Math.abs(e.clientX - downX) > DRAG_THRESHOLD || Math.abs(e.clientY - downY) > DRAG_THRESHOLD
  if (!moved) close() // 움직임 거의 없음 = 클릭 → 닫기 (드래그면 유지)
}
// 스크롤/휠은 '바깥'에서만 닫는다 — 팝업 내부 스크롤(내용 읽기)은 유지.
function onScrollOrWheel(e) { if (!popupEl.value?.contains(e.target)) close() }
function onResize() { close() }

function bind() {
  document.addEventListener('mousedown', onPointerDown, true)
  document.addEventListener('mouseup', onPointerUp, true)
  window.addEventListener('scroll', onScrollOrWheel, true)       // 캡처: 표 내부 스크롤도 잡음
  window.addEventListener('wheel', onScrollOrWheel, true)
  window.addEventListener('resize', onResize)
}
function unbind() {
  document.removeEventListener('mousedown', onPointerDown, true)
  document.removeEventListener('mouseup', onPointerUp, true)
  window.removeEventListener('scroll', onScrollOrWheel, true)
  window.removeEventListener('wheel', onScrollOrWheel, true)
  window.removeEventListener('resize', onResize)
  downInside = false
}

watch(expanded, async (open) => {
  if (open) {
    await nextTick()
    place()
    bind()
  } else {
    unbind()
  }
})

onBeforeUnmount(() => {
  unbind()
  if (expanded.value) clearMemoIf(key())
})
</script>
