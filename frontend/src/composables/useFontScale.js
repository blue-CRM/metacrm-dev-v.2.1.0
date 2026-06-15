import { ref, watch } from 'vue'

/**
 * 글자 크기 배율(--font-scale). 본문 텍스트(--text-theme-* 토큰)에 곱해져 표·메모 등이 함께 커/작아진다.
 * localStorage에 저장되어 새로고침·재접속에도 유지된다.
 */
const KEY = 'app-font-scale'
const MIN = 0.8
const MAX = 1.4
const STEP = 0.05

function clamp(v) {
  if (!Number.isFinite(v)) return 1
  return Math.min(MAX, Math.max(MIN, Math.round(v * 100) / 100))
}

const stored = parseFloat(localStorage.getItem(KEY) || '1')
const fontScale = ref(clamp(stored))

function apply(v) {
  document.documentElement.style.setProperty('--font-scale', String(v))
}
apply(fontScale.value)

watch(fontScale, (v) => {
  apply(v)
  try { localStorage.setItem(KEY, String(v)) } catch { /* 무시 */ }
})

export function useFontScale() {
  const inc = () => { fontScale.value = clamp(fontScale.value + STEP) }
  const dec = () => { fontScale.value = clamp(fontScale.value - STEP) }
  const reset = () => { fontScale.value = 1 }
  const percent = () => Math.round(fontScale.value * 100)
  const atMin = () => fontScale.value <= MIN + 1e-6
  const atMax = () => fontScale.value >= MAX - 1e-6
  return { fontScale, inc, dec, reset, percent, atMin, atMax }
}
