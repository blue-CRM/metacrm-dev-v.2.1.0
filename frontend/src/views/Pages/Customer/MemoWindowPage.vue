<template>
  <!-- 메모 채팅 별도 창 (라우트 /memo/:customerId). bare 레이아웃(사이드바 없음) — 팝업 전용. -->
  <div class="h-screen w-screen overflow-hidden bg-white dark:bg-gray-900">
    <MemoWindow
      :customer-id="customerId"
      :view="view"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import MemoWindow from '@/components/memo/MemoWindow.vue'
import { MEMO_SYNC_CHANNEL } from '@/composables/useMemoWindow.js'

const route = useRoute()

const customerId = computed(() => route.params.customerId)
// ?view=1 → 읽기전용 UI 모드(분배창 등). 그 외 권한(canWrite/editable)은 서버가 판정.
const view = computed(() => route.query.view === '1')

/* ─── 다크/라이트 모드: 본창과 동기화 ───
 * 팝업은 열 때 ThemeProvider가 localStorage로 초기 적용함.
 * 열린 동안 본창에서 토글하면 같은 origin localStorage 변경 → 다른 창에 'storage' 이벤트 발생 →
 * 그걸 받아 <html>.dark 를 즉시 갱신(실시간 따라가기). */
function applyTheme(val) {
  const dark = (val ?? localStorage.getItem('theme')) === 'dark'
  document.documentElement.classList.toggle('dark', dark)
}
function onStorage(e) {
  if (e.key === 'theme') applyTheme(e.newValue)
}

// 부모(메인 창)에서 로그아웃하면 이 팝업도 닫는다(같은 브라우저, BroadcastChannel)
let syncChannel = null
onMounted(() => {
  applyTheme() // 열 때 현재 테마 반영(ThemeProvider와 중복돼도 무해)
  window.addEventListener('storage', onStorage)
  try {
    syncChannel = new BroadcastChannel(MEMO_SYNC_CHANNEL)
    syncChannel.onmessage = (e) => {
      if (e.data?.type === 'logout') window.close()
    }
  } catch {
    /* 미지원 환경 무시 */
  }
})
onUnmounted(() => {
  window.removeEventListener('storage', onStorage)
  try {
    syncChannel?.close()
  } catch {
    /* noop */
  }
})
</script>
