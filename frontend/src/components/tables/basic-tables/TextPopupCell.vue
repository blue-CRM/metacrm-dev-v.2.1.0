<template>
  <!--
    잘린 텍스트 셀(내용/DB출처/전화번호 등). 클릭하면 메모와 동일하게 '그 자리 팝업'으로 전체 텍스트를 보여준다.
    - 표를 늘리지 않음(reflow 0) → 100개에서도 가벼움.
    - 클릭(팝업 안/밖) / 바깥 스크롤·드래그 / 다른 팝업 열기 → 닫힘. 팝업 안 휠 스크롤·드래그(선택)는 유지.
    - 실제로 잘린 셀일 때만 팝업이 뜬다(짧아서 다 보이는 셀은 클릭해도 무반응).
  -->
  <div>
    <!-- 클릭 영역을 '글자'에만 한정(inline-block) — 셀의 빈 여백을 눌러도 아무 일 없음 -->
    <button
        ref="anchorEl"
        type="button"
        class="inline-block align-middle max-w-full rounded focus:outline-none text-left"
        @click="onClick"
    >
      <span
          ref="previewEl"
          class="block text-theme-sm leading-tight truncate whitespace-nowrap overflow-hidden text-gray-500 dark:text-gray-400"
          :class="previewClass"
          :style="{ width: width + 'px' }"
      >
        <span v-if="renderHtml" v-html="renderHtml"></span>
        <template v-else>{{ text ?? '' }}</template>
      </span>
    </button>

    <!-- 우하단 핸들로 크기 조절 가능 / 본문과 구분되는 진한 테두리·그림자 -->
    <Teleport to="body">
      <div v-if="open" ref="popupEl" class="fixed z-[2147483000]" :style="popupStyle">
        <div
            class="resize overflow-auto rounded-lg border border-gray-200/90 dark:border-[#28365a] ring-1 ring-gray-900/[0.04] shadow-xl bg-white dark:bg-[#0e1626] px-3 py-2.5 w-[300px] min-w-[160px] max-w-[85vw] min-h-[44px] max-h-[280px] text-theme-sm leading-relaxed whitespace-pre-wrap break-words text-gray-700 dark:text-gray-200"
        >
          <!-- 과거 이력 등 색상(파랑/초록/회색) 유지: render HTML 있으면 그대로, 없으면 일반 텍스트 -->
          <span v-if="renderHtml" v-html="renderHtml"></span>
          <template v-else>{{ text ?? '' }}</template>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, watch, onBeforeUnmount, nextTick } from 'vue'
import { toggleMemo, clearMemoIf } from '@/composables/useMemoExpand'

const props = defineProps({
  cellKey: { type: [String, Number], required: true },
  text: { type: [String, Number, null], default: '' },
  renderHtml: { type: [String, null], default: null },
  width: { type: Number, default: 100 },
  previewClass: { type: [String, Array, Object], default: '' }, // 미리보기 글자 추가 스타일(이름 강조 등)
})

const anchorEl = ref(null)
const previewEl = ref(null)
const popupEl = ref(null)
const popupStyle = ref({})
const open = ref(false)
const key = () => String(props.cellKey)

const PW = 300, PH = 300, GAP = 4
function place() {
  const a = anchorEl.value
  if (!a) return
  const r = a.getBoundingClientRect()
  const vw = window.innerWidth, vh = window.innerHeight
  const left = Math.max(8, Math.min(r.left, vw - PW - 8))
  const style = { left: `${left}px` }
  if (vh - r.bottom < PH + GAP + 8 && r.top > vh - r.bottom) style.bottom = `${vh - r.top + GAP}px`
  else style.top = `${r.bottom + GAP}px`
  style.visibility = 'visible'
  popupStyle.value = style
}

function isTruncated() {
  const el = previewEl.value
  if (!el) return false
  return el.scrollWidth > el.clientWidth + 1
}
function onClick() {
  // 이미 열려 있으면 닫기 토글. 닫혀 있으면 '잘린 셀'일 때만 연다.
  if (open.value || isTruncated()) toggleMemo(key(), (v) => {
    // 위치 잡기 전엔 화면 밖에 숨겨 깜빡임 방지
    if (v) popupStyle.value = { left: '-9999px', top: '0px', visibility: 'hidden' }
    open.value = v
  })
}
function close() { if (open.value) { open.value = false; clearMemoIf(key()) } }

// 클릭/드래그 구분: 바깥 mousedown은 즉시 닫기, 팝업 안은 mouseup에서 거의 안 움직였으면(클릭) 닫기
let downInside = false, downX = 0, downY = 0
const TH = 5
function onPointerDown(e) {
  if (anchorEl.value?.contains(e.target)) return
  if (popupEl.value?.contains(e.target)) { downInside = true; downX = e.clientX; downY = e.clientY; return }
  close()
}
function onPointerUp(e) {
  if (!downInside) return
  downInside = false
  if (Math.abs(e.clientX - downX) <= TH && Math.abs(e.clientY - downY) <= TH) close()
}
function onScrollOrWheel(e) { if (!popupEl.value?.contains(e.target)) close() }
function onResize() { close() }

function bind() {
  document.addEventListener('mousedown', onPointerDown, true)
  document.addEventListener('mouseup', onPointerUp, true)
  window.addEventListener('scroll', onScrollOrWheel, true)
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

watch(open, async (v) => {
  if (v) { await nextTick(); place(); bind() } else unbind()
})
onBeforeUnmount(() => { unbind(); if (open.value) clearMemoIf(key()) })
</script>
