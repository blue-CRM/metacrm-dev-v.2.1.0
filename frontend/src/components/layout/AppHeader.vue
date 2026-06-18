<template>
  <header
      class="fixed whitespace-nowrap inset-x-0 top-0 z-[9998] w-screen bg-white dark:bg-[#0e1626] lg:sticky lg:inset-x-auto lg:w-full lg:border-b dark:border-[#1f2a47]"
  >
    <div class="flex flex-col items-center justify-between grow lg:flex-row lg:px-6">
      <div
          class="flex items-center justify-between w-full gap-2 px-3 py-3 border-b border-gray-200 dark:border-[#1f2a47] sm:gap-4 lg:justify-normal lg:border-b-0 lg:px-0 lg:py-4"
      >
        <!-- 모바일에서만 -> 사이드바를 접을 수 있는 버튼 -->
        <button
            @click="handleToggle"
            class="flex items-center justify-center w-10 h-10 text-gray-500 border-gray-200 rounded-lg z-[99999] dark:border-[#1f2a47] dark:text-gray-400 lg:h-11 lg:w-11 lg:border lg:hidden"
            :class="[
            isMobileOpen
              ? 'lg:bg-transparent dark:lg:bg-transparent bg-gray-100 dark:bg-[#16213b]'
              : '',
          ]"
        >
          <svg
              v-if="isMobileOpen"
              class="fill-current"
              width="24"
              height="24"
              viewBox="0 0 24 24"
              fill="none"
              xmlns="http://www.w3.org/2000/svg"
          >
            <path
                fill-rule="evenodd"
                clip-rule="evenodd"
                d="M6.21967 7.28131C5.92678 6.98841 5.92678 6.51354 6.21967 6.22065C6.51256 5.92775 6.98744 5.92775 7.28033 6.22065L11.999 10.9393L16.7176 6.22078C17.0105 5.92789 17.4854 5.92788 17.7782 6.22078C18.0711 6.51367 18.0711 6.98855 17.7782 7.28144L13.0597 12L17.7782 16.7186C18.0711 17.0115 18.0711 17.4863 17.7782 17.7792C17.4854 18.0721 17.0105 18.0721 16.7176 17.7792L11.999 13.0607L7.28033 17.7794C6.98744 18.0722 6.51256 18.0722 6.21967 17.7794C5.92678 17.4865 5.92678 17.0116 6.21967 16.7187L10.9384 12L6.21967 7.28131Z"
                fill=""
            />
          </svg>
          <svg
              v-else
              width="16"
              height="12"
              viewBox="0 0 16 12"
              fill="none"
              xmlns="http://www.w3.org/2000/svg"
          >
            <path
                fill-rule="evenodd"
                clip-rule="evenodd"
                d="M0.583252 1C0.583252 0.585788 0.919038 0.25 1.33325 0.25H14.6666C15.0808 0.25 15.4166 0.585786 15.4166 1C15.4166 1.41421 15.0808 1.75 14.6666 1.75L1.33325 1.75C0.919038 1.75 0.583252 1.41422 0.583252 1ZM0.583252 11C0.583252 10.5858 0.919038 10.25 1.33325 10.25L14.6666 10.25C15.0808 10.25 15.4166 10.5858 15.4166 11C15.4166 11.4142 15.0808 11.75 14.6666 11.75L1.33325 11.75C0.919038 11.75 0.583252 11.4142 0.583252 11ZM1.33325 5.25C0.919038 5.25 0.583252 5.58579 0.583252 6C0.583252 6.41421 0.919038 6.75 1.33325 6.75L7.99992 6.75C8.41413 6.75 8.74992 6.41421 8.74992 6C8.74992 5.58579 8.41413 5.25 7.99992 5.25L1.33325 5.25Z"
                fill="currentColor"
            />
          </svg>
        </button>

        <HeaderLogo />

        <!-- 모바일에서만 -> 유저 프로필 접히는 버튼 -->
        <button
            @click="toggleApplicationMenu"
            class="flex items-center justify-center w-10 h-10 text-gray-700 rounded-lg z-[99999] hover:bg-gray-100 dark:text-gray-400 dark:hover:bg-[#16213b] lg:hidden"
        >
          <svg
              width="24"
              height="24"
              viewBox="0 0 24 24"
              fill="none"
              xmlns="http://www.w3.org/2000/svg"
          >
            <path
                fill-rule="evenodd"
                clip-rule="evenodd"
                d="M5.99902 10.4951C6.82745 10.4951 7.49902 11.1667 7.49902 11.9951V12.0051C7.49902 12.8335 6.82745 13.5051 5.99902 13.5051C5.1706 13.5051 4.49902 12.8335 4.49902 12.0051V11.9951C4.49902 11.1667 5.1706 10.4951 5.99902 10.4951ZM17.999 10.4951C18.8275 10.4951 19.499 11.1667 19.499 11.9951V12.0051C19.499 12.8335 18.8275 13.5051 17.999 13.5051C17.1706 13.5051 16.499 12.8335 16.499 12.0051V11.9951C16.499 11.1667 17.1706 10.4951 17.999 10.4951ZM13.499 11.9951C13.499 11.1667 12.8275 10.4951 11.999 10.4951C11.1706 10.4951 10.499 11.1667 10.499 11.9951V12.0051C10.499 12.8335 11.1706 13.5051 11.999 13.5051C12.8275 13.5051 13.499 12.8335 13.499 12.0051V11.9951Z"
                fill="currentColor"
            />
          </svg>
        </button>

        <!-- PC용 필터 (헤더 오른쪽) -->
        <div
            class="hidden lg:flex lg:flex-row lg:items-center lg:justify-end lg:gap-4 lg:px-0 lg:py-0"
        >
          <!-- 날짜 필터 -->
          <div class="flex items-center">
            <!-- 시작일 -->
            <input
                type="text"
                ref="startPicker"
                class="w-26 h-11 border border-gray-200 dark:border-[#28365a] rounded-l-lg px-3 py-2 text-sm text-center
                      focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10 dark:bg-[#16213b] dark:text-gray-200"
                placeholder="시작일"
            />

            <!-- 구분자 -->
            <span
                class="flex items-center justify-center w-8 h-11 border-t border-b border-gray-200 dark:border-[#28365a]
                 text-sm text-gray-500 dark:text-gray-400 bg-gray-50 dark:bg-[#0e1626]">~</span>

            <!-- 종료일 -->
            <input
                type="text"
                ref="endPicker"
                class="w-26 h-11 border border-gray-200 dark:border-[#28365a] rounded-r-lg px-3 py-2 text-sm text-center
                      focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10 dark:bg-[#16213b] dark:text-gray-200"
                placeholder="종료일"
            />
          </div>

          <!-- 카테고리 필터 -->
          <select
              v-model="selectedCategory"
              class="w-20 h-11 border border-gray-200 dark:border-[#28365a] rounded-lg px-2 py-1
             text-sm focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10
             dark:bg-[#16213b] dark:text-gray-400 text-gray-500"
          >
            <option value="all">전체</option>
            <option value="주식">주식</option>
            <option value="코인">코인</option>
          </select>

          <!-- 검색창 -->
          <SearchBar />

          <!-- 핑 테스트 -->
<!--          <button @click="test">ping</button>-->
        </div>
      </div>

      <!-- 모바일용 세션 타이머, 프로필 (로고 바로 밑에) -->
      <div
          :class="[isApplicationMenuOpen ? 'flex' : 'hidden']"
          class="w-full px-5 py-4 lg:flex lg:flex-row lg:items-center lg:justify-end lg:gap-4 lg:px-0 lg:py-0 lg:shadow-none
         flex-row justify-between"
      >
        <!-- 세션 타이머 -->
        <div v-if="timeLeft > 0" class="flex items-center gap-2 text-sm text-gray-700 dark:text-gray-400">
          <span class="text-blue-500 dark:text-blue-400">
            {{ String(Math.floor(timeLeft / 3600)).padStart(2, '0') }}:
            {{ String(Math.floor((timeLeft % 3600) / 60)).padStart(2, '0') }}:
            {{ String(timeLeft % 60).padStart(2, '0') }}
          </span>
          <button
              @click="extendSession"
              class="items-center gap-0.5 rounded-lg border border-gray-200 bg-gray-50 px-[7px] py-[4.5px]
                   text-xs text-gray-500 hover:bg-gray-100 dark:border-[#1f2a47] dark:bg-white/5 dark:text-gray-400 dark:hover:bg-[#16213b]"
          >
            연장
          </button>
        </div>

        <!-- 유저 프로필 -->
        <UserMenu />
      </div>

      <!-- 모바일용 필터 (로고 바로 밑에) -->
      <div
          v-if="isApplicationMenuOpen"
          class="shadow-theme-md flex flex-row justify-between items-center w-full px-5 py-4 border-gray-200 dark:border-[#28365a] lg:hidden"
      >
        <!-- 왼쪽 그룹 -->
        <div class="flex flex-row gap-2">
          <!-- 날짜 필터 -->
          <div class="flex items-center">
            <!-- 시작일 -->
            <input
                type="text"
                ref="startPicker"
                class="w-26 h-11 border border-gray-200 dark:border-[#28365a] rounded-l-lg px-3 py-2 text-sm text-center
                      focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10 dark:bg-[#16213b] dark:text-gray-200"
                placeholder="시작일"
            />

            <!-- 구분자 -->
            <span
                class="flex items-center justify-center w-8 h-11 border-t border-b border-gray-200 dark:border-[#28365a]
                 text-sm text-gray-500 dark:text-gray-400 bg-gray-50 dark:bg-[#0e1626]">~</span>

            <!-- 종료일 -->
            <input
                type="text"
                ref="endPicker"
                class="w-26 h-11 border border-gray-200 dark:border-[#28365a] rounded-r-lg px-3 py-2 text-sm text-center
                      focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10 dark:bg-[#16213b] dark:text-gray-200"
                placeholder="종료일"
            />
          </div>

          <!-- 카테고리 필터 -->
          <select
              v-model="selectedCategory"
              class="w-20 h-11 border border-gray-200 dark:border-[#28365a] rounded-lg px-2 py-1
             text-sm focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10
             dark:bg-[#16213b] dark:text-gray-400 text-gray-500"
          >
            <option value="all">전체</option>
            <option value="주식">주식</option>
            <option value="코인">코인</option>
          </select>
        </div>

        <!-- 오른쪽 그룹 -->
        <div>
          <SearchBar />
        </div>
      </div>

    </div>
  </header>
</template>

<script setup lang="ts">
import {onMounted, onUnmounted, watch, ref, nextTick} from 'vue'
import { useSidebar } from '@/composables/useSidebar'
import { useAuthStore } from '@/stores/auth'
import SearchBar from './header/SearchBar.vue'
import HeaderLogo from './header/HeaderLogo.vue'
import UserMenu from './header/UserMenu.vue'

// 날짜 설정 관련
import flatpickr from 'flatpickr'
import { Korean } from 'flatpickr/dist/l10n/ko.js'
import 'flatpickr/dist/flatpickr.css'
import {onBeforeRouteLeave} from "vue-router";

// 전역 필터 관련
import { globalFilters } from '@/composables/globalFilters.js'

const startPicker = ref<HTMLInputElement | null>(null)
const endPicker = ref<HTMLInputElement | null>(null)
const startDate = ref<Date | null>(null)
const endDate = ref<Date | null>(null)

const SELECTOR_START = 'header input[placeholder="시작일"]'
const SELECTOR_END   = 'header input[placeholder="종료일"]'

function collectStartInputs(): HTMLInputElement[] {
  return Array.from(document.querySelectorAll<HTMLInputElement>(SELECTOR_START))
}
function collectEndInputs(): HTMLInputElement[] {
  return Array.from(document.querySelectorAll<HTMLInputElement>(SELECTOR_END))
}
function firstVisible(els: HTMLInputElement[]): HTMLInputElement | null {
  return els.find(el => el.offsetParent !== null) ?? els[0] ?? null
}

let startFP: any | null = null
let endFP: any | null = null
let boundStartEl: HTMLInputElement | null = null
let boundEndEl: HTMLInputElement | null = null

function mountStartIfNeeded() {
  const el = firstVisible(collectStartInputs())
  if (!el || el === boundStartEl) return
  try { startFP?.destroy() } catch {}
  startFP = flatpickr(el, {
    locale: Korean,
    dateFormat: 'Y-m-d',
    allowInput: true,
    disableMobile: true,
    //appendTo: el.closest('header') as HTMLElement | undefined,
    onReady: (_d, _s, fp) => {
      fp.calendarContainer.style.zIndex = '999999'

      const anyFp = fp as any
      if (!anyFp._clearBtn) {
        const btn = document.createElement('button')
        btn.type = 'button'
        btn.title = '입력값 지우기'
        btn.textContent = '✕'
        btn.className =
            'fp-clear-btn absolute top-1 right-1 w-6 h-6 flex items-center justify-center ' +
            'text-xs text-gray-500 hover:text-gray-700 rounded'

        btn.addEventListener('mousedown', (e) => {
          e.preventDefault()
          e.stopPropagation()
          fp.clear()   // onChange가 startDate.value = null 로 갱신
          fp.close()
        })

        fp.calendarContainer.appendChild(btn)
        anyFp._clearBtn = btn
      }
    },
    onChange: (dates) => { startDate.value = dates[0] ?? null },
  })
  boundStartEl = el
  if (startDate.value) startFP.setDate(startDate.value, false, 'Y-m-d')
}

function mountEndIfNeeded() {
  const el = firstVisible(collectEndInputs())
  if (!el || el === boundEndEl) return
  try { endFP?.destroy() } catch {}
  endFP = flatpickr(el, {
    locale: Korean,
    dateFormat: 'Y-m-d',
    allowInput: true,
    disableMobile: true,
    //appendTo: el.closest('header') as HTMLElement | undefined,
    onReady: (_d, _s, fp) => {
      fp.calendarContainer.style.zIndex = '999999'

      const anyFp = fp as any
      if (!anyFp._clearBtn) {
        const btn = document.createElement('button')
        btn.type = 'button'
        btn.title = '입력값 지우기'
        btn.textContent = '✕'
        btn.className =
            'fp-clear-btn absolute top-1 right-1 w-6 h-6 flex items-center justify-center ' +
            'text-xs text-gray-500 hover:text-gray-700 rounded'

        btn.addEventListener('mousedown', (e) => {
          e.preventDefault()
          e.stopPropagation()
          fp.clear()   // onChange가 endDate.value = null 로 갱신
          fp.close()
        })

        fp.calendarContainer.appendChild(btn)
        anyFp._clearBtn = btn
      }
    },
    onChange: (dates) => { endDate.value = dates[0] ?? null },
  })
  boundEndEl = el
  if (endDate.value) endFP.setDate(endDate.value, false, 'Y-m-d')
}

// 공통 정리 함수
function cleanupCalendars() {
  try { startFP?.close(); startFP?.destroy() } catch {}
  try { endFP?.close();   endFP?.destroy() } catch {}
  startFP = endFP = null
  boundStartEl = boundEndEl = null
  document.querySelectorAll('.flatpickr-calendar').forEach(el => el.remove())
}

// 세션 타이머 시작
onMounted(() => {
  updateTime()
  timer = window.setInterval(updateTime, 1000)
})

// 카테고리 선택 관련
const selectedCategory = ref('all')

// 날짜 / 카테고리 외 설정에 관하여
const auth = useAuthStore()

// ping 테스트
// const test = async () => {
//   try {
//     const res = await auth.pingPong()
//     console.log("Ping 성공:", res)
//     // alert("서버 응답: " + JSON.stringify(res))
//   } catch (e) {
//     console.error("Ping 실패:", e)
//     // alert("Ping 실패: " + e)
//   }
// }

// 사이드바 토글
const { toggleSidebar, toggleMobileSidebar, isMobileOpen, closeMobileSidebar } = useSidebar()

// 모바일용 -> 프로필 메뉴 토글
const isApplicationMenuOpen = ref(false)
const toggleApplicationMenu = () => {
  const next = !isApplicationMenuOpen.value
  isApplicationMenuOpen.value = next
  if (next) closeMobileSidebar() // 헤더앱 메뉴 열리면 사이드바 닫기
}

const handleToggle = () => {
  if (window.innerWidth >= 1024) {
    toggleSidebar()
  } else {
    // 사이드바 열 땐 헤더앱 메뉴 닫기
    if (!isMobileOpen.value) isApplicationMenuOpen.value = false
    toggleMobileSidebar()
  }
}

// 세이프가드: 어떤 경로로 열려도 일관성 보장1
watch(isMobileOpen, (open) => { if (open) isApplicationMenuOpen.value = false })
watch(isApplicationMenuOpen, (open) => { if (open) closeMobileSidebar() })

// 세션 (로그인 시간) 관련
const timeLeft = ref(0)
let timer: number | undefined
const updateTime = async () => {
  if (auth.refreshExp) {
    const diff = Math.floor((auth.refreshExp - Date.now()) / 1000)
    timeLeft.value = diff > 0 ? diff : 0

    // 만료 시 자동 로그아웃
    if (timeLeft.value === 0) {
      await auth.logout()
    }
  }
}

// 보이는 ref가 바뀌면 자동 재바인딩
watch([startPicker, endPicker, () => isApplicationMenuOpen.value], async () => {
  await nextTick()
  mountStartIfNeeded()
  mountEndIfNeeded()
}, { immediate: true })

// 창 크기 바뀌어 PC↔모바일 레이아웃 전환될 때도 재바인딩
async function rebindOnResize() {
  // CSS 브레이크포인트 적용 후에 실행되도록 한 템포 보류
  await nextTick()
  // 지금 보이는 인풋 기준으로만 다시 붙이기
  mountStartIfNeeded()
  mountEndIfNeeded()
}

// 리사이즈 시 flatpickr 재바인딩
onMounted(() => window.addEventListener('resize', rebindOnResize))
onUnmounted(() => window.removeEventListener('resize', rebindOnResize))

watch(() => isApplicationMenuOpen.value, async () => {
  await nextTick()
  mountStartIfNeeded()
  mountEndIfNeeded()
}, { flush: 'post' })

// 날짜 값을 외부에서 바꾸면 피커들도 동기화
watch(startDate, d => startFP?.setDate(d ?? '', false, 'Y-m-d'))
watch(endDate,   d => endFP?.setDate(d ?? '', false, 'Y-m-d'))

onBeforeRouteLeave(() => cleanupCalendars())
onUnmounted(() => cleanupCalendars())
auth.$onAction?.(({ name, after }) => { if (name === 'logout') after(() => cleanupCalendars()) })

onUnmounted(() => {
  if (timer) clearInterval(timer)
  cleanupCalendars()
})

// 라우트 이탈 시에도(강제 로그아웃 포함) 잔상 방지
onBeforeRouteLeave(() => cleanupCalendars())

// ★ 강제 로그아웃 액션 뒤에도 정리
auth.$onAction?.(({ name, after }) => {
  if (name === 'logout') after(() => cleanupCalendars())
})

const extendSession = async () => {
  await auth.extendSession()
  await updateTime()
}

// --- 전역 필터랑 동기화 ---
// globalFilters.category 의 값을 프론트 셀렉트값으로 복원
watch(
    () => globalFilters.category,
    (val) => {
      selectedCategory.value = val ?? 'all'
    },
    { immediate: true }   // 첫 마운트 때도 바로 반영
)

// 프론트 셀렉트에서 선택된 값을 globalFilters 로 반영
watch(selectedCategory, (val) => {
  globalFilters.category = val === 'all' ? null : val
})
// ----------------------------

const fmtYMD = (d: Date) => {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`            // 로컬(브라우저) 기준 YYYY-MM-DD
}

// --- 스크롤/휠 시 달력 강제 닫기 ---
const closePickerOnScroll = (e: Event) => {
  // (선택) 달력 위에서의 스크롤은 무시하고 싶으면 아래 3줄 주석 해제
  // const t = e.target as HTMLElement | null
  // if (t && t.closest('.flatpickr-calendar')) return

  if (startFP?.isOpen) startFP.close()
  if (endFP?.isOpen) endFP.close()
}

// 초기 날짜 셋팅
function daysInMonth(year: number, month0: number) {
  return new Date(year, month0 + 1, 0).getDate()
}
function shiftMonthsClamped(date: Date, deltaMonths: number) {
  const y = date.getFullYear()
  const m = date.getMonth() // 0-based
  const d = date.getDate()
  const total = y * 12 + m + deltaMonths
  const ty = Math.floor(total / 12)
  const tm = total % 12
  const dim = daysInMonth(ty, tm)
  return new Date(ty, tm, Math.min(d, dim))
}

// --- 전역 필터랑 동기화 ---
onMounted(() => {
  const hasFrom = !!globalFilters.dateFrom
  const hasTo   = !!globalFilters.dateTo

  // 1) 전역 필터에 날짜가 있으면 입력값 복원, 없는 건 그대로 null 유지
  if (hasFrom || hasTo) {
    startDate.value = hasFrom ? new Date(globalFilters.dateFrom) : null
    endDate.value   = hasTo   ? new Date(globalFilters.dateTo)   : null
  } else {
    // 2) 처음 진입: 기본 3개월 범위
    // 종료일 = 오늘(로컬 기준, 시분초 제거)
    const today = new Date()
    const end = new Date(today.getFullYear(), today.getMonth(), today.getDate())

    // 시작일 = 종료일 기준 3개월 전(말일 보정)
    // const start = shiftMonthsClamped(end, -3)
    // 시작일을 2025년 8월 1일로 고정 (월은 0부터 시작)
    const start = new Date(2025, 7, 1)

    // 리액티브 값 주입 → 기존 watch가 flatpickr에 반영
    startDate.value = start
    endDate.value   = end
  }

  updateTime()
  timer = window.setInterval(updateTime, 1000)
})

watch(startDate, (val) => {
  globalFilters.dateFrom = val ? fmtYMD(val) : null
})

watch(endDate, (val) => {
  globalFilters.dateTo = val ? fmtYMD(val) : null
})
// ----------------------------

onMounted(() => {
  // capture:true 로 위쪽에서 먼저 잡아서 확실하게 닫음
  window.addEventListener('wheel',     closePickerOnScroll, { passive: true, capture: true })
  window.addEventListener('scroll',    closePickerOnScroll, { passive: true, capture: true })
  window.addEventListener('touchmove', closePickerOnScroll, { passive: true, capture: true })
})

onUnmounted(() => {
  window.removeEventListener('wheel',     closePickerOnScroll, true)
  window.removeEventListener('scroll',    closePickerOnScroll, true)
  window.removeEventListener('touchmove', closePickerOnScroll, true)
})

</script>

<style>
/* 오늘 날짜 */
.flatpickr-day.today,
.flatpickr-day.today:hover {
  border-radius: 6px !important;
  border: 1px solid var(--color-brand-500) !important;
  background: transparent !important;
  color: var(--color-brand-500) !important;
}

/* 오늘 날짜 + 선택됨 */
.flatpickr-day.today.selected,
.flatpickr-day.today.selected:hover {
  background: var(--color-brand-500) !important;
  border: 1px solid var(--color-brand-500) !important;
  color: #fff !important;
}

/* 라이트 모드: hover → 달력 배경색 (#fff) */
.flatpickr-day:hover,
.flatpickr-day:focus,
.flatpickr-day.inRange:hover,
.flatpickr-day.inRange:focus,
.flatpickr-day.prevMonthDay:hover,
.flatpickr-day.nextMonthDay:hover,
.flatpickr-day.prevMonthDay:focus,
.flatpickr-day.nextMonthDay:focus {
  background: #ffffff !important; /* 라이트 모드 달력 배경색 */
  border: none !important;
  box-shadow: none !important;
  color: var(--color-gray-800) !important;
}

/* 전월/다음월 날짜 (흐린 회색) */
.flatpickr-day.prevMonthDay:hover,
.flatpickr-day.nextMonthDay:hover,
.flatpickr-day.prevMonthDay:focus,
.flatpickr-day.nextMonthDay:focus {
  background: #ffffff !important; /* 라이트 모드 달력 배경색 */
  color: var(--color-gray-400) !important;
}

/* 월 선택 드롭다운 */
.flatpickr-monthDropdown-months:hover,
.numInputWrapper:hover,
.flatpickr-monthDropdown-months:focus,
.numInputWrapper:focus {
  background: transparent !important;
}

/* 오늘 날짜(today)는 hover해도 스타일 유지 */
.flatpickr-day.today:hover,
.flatpickr-day.today:focus {
  border: 1px solid var(--color-brand-500) !important;
  background: transparent !important;
  color: var(--color-brand-500) !important;
}

/* 선택된 날짜 */
.flatpickr-day.selected,
.flatpickr-day.startRange,
.flatpickr-day.endRange {
  border-radius: 6px !important;
  background: var(--color-brand-500) !important;
  border: 1px solid var(--color-brand-500) !important;
  color: #fff !important;
}

/* 라이트 모드: 선택된 날짜 hover 시에도 스타일 유지 */
.flatpickr-day.selected:hover,
.flatpickr-day.startRange:hover,
.flatpickr-day.endRange:hover,
.flatpickr-day.selected:focus,
.flatpickr-day.startRange:focus,
.flatpickr-day.endRange:focus {
  background: var(--color-brand-500) !important;
  border: 1px solid var(--color-brand-500) !important;
  color: #fff !important; /* 글자색 고정 */
}

/* ------------------------- */
/* ------------------------- */
/* ------------------------- */

/* 다크 모드: hover 제거 (우선순위 ↑) */
.dark .flatpickr-calendar .flatpickr-day:hover,
.dark .flatpickr-calendar .flatpickr-day:focus,
.dark .flatpickr-calendar .flatpickr-day.inRange:hover,
.dark .flatpickr-calendar .flatpickr-day.inRange:focus,
.dark .flatpickr-calendar .flatpickr-day.prevMonthDay:hover,
.dark .flatpickr-calendar .flatpickr-day.nextMonthDay:hover,
.dark .flatpickr-calendar .flatpickr-day.prevMonthDay:focus,
.dark .flatpickr-calendar .flatpickr-day.nextMonthDay:focus {
  background: none !important;
  border: 1px solid transparent !important;
  box-shadow: none !important;
  color:
      color-mix(in oklab, var(--color-white) 90%, transparent) !important;
}

/* 다크 모드: 이번 달 날짜 hover 제거 */
.dark .flatpickr-calendar .dayContainer .flatpickr-day:hover,
.dark .flatpickr-calendar .dayContainer .flatpickr-day:focus {
  background: transparent !important;
  border: 1px solid transparent !important;
  box-shadow: none !important;
  color:
      color-mix(in oklab, var(--color-white) 90%, transparent) !important;
}

/* 다크 모드: 전월/다음달 날짜는 흐린 회색 */
.dark .flatpickr-calendar .flatpickr-day.prevMonthDay:hover,
.dark .flatpickr-calendar .flatpickr-day.nextMonthDay:hover {
  color: var(--color-gray-400) !important;
}

/* 다크 모드: 오늘 날짜 */
.dark .flatpickr-calendar .flatpickr-day.today,
.dark .flatpickr-calendar .flatpickr-day.today:hover,
.dark .flatpickr-calendar .flatpickr-day.today:focus {
  border: 1px solid var(--color-brand-500) !important;
  background: transparent !important;
  color: var(--color-brand-500) !important;
}

/* 다크 모드: 오늘 + 선택됨 */
.dark .flatpickr-calendar .flatpickr-day.today.selected,
.dark .flatpickr-calendar .flatpickr-day.today.selected:hover,
.dark .flatpickr-calendar .flatpickr-day.today.selected:focus {
  background: var(--color-brand-500) !important;
  border: 1px solid var(--color-brand-500) !important;
  color: #fff !important;
}

/* 다크 모드: 선택된 날짜 */
.dark .flatpickr-calendar .flatpickr-day.selected,
.dark .flatpickr-calendar .flatpickr-day.startRange,
.dark .flatpickr-calendar .flatpickr-day.endRange,
.dark .flatpickr-calendar .flatpickr-day.selected:hover,
.dark .flatpickr-calendar .flatpickr-day.startRange:hover,
.dark .flatpickr-calendar .flatpickr-day.endRange:hover {
  background: var(--color-brand-500) !important;
  border: 1px solid var(--color-brand-500) !important;
  color: #fff !important;
}

/* 다크 모드: 월 선택 드롭다운 */
.dark .flatpickr-calendar .flatpickr-monthDropdown-months {
  background: #131827 !important;   /* 드롭다운 배경 */
  color: var(--color-gray-200) !important; /* 기본 글자색 */
  border: 1px solid transparent !important;
  border-radius: 6px !important;
}

/* 다크 모드: 드롭다운 옵션 */
.dark .flatpickr-calendar .flatpickr-monthDropdown-months .flatpickr-monthDropdown-month {
  background: #131827 !important;
  color: var(--color-gray-200) !important;
}

/* 다크 모드: 옵션 hover */
.dark .flatpickr-calendar .flatpickr-monthDropdown-months .flatpickr-monthDropdown-month:hover {
  background: var(--color-brand-500) !important;
  color: #ffffff !important;
}

/* 다크 모드: 연도 선택 화살표 (컨테이너 테두리까지 흰색) */
.dark .flatpickr-calendar .numInputWrapper span {
  border: 1px solid #838383 !important;  /* 흰색 테두리 */
}

/* 다크 모드: 연도 선택 화살표 */
.dark .flatpickr-calendar .numInputWrapper span.arrowUp:after {
  border-bottom-color: #ffffff !important; /* 위쪽 화살표 흰색 */
}

.dark .flatpickr-calendar .numInputWrapper span.arrowDown:after {
  border-top-color: #ffffff !important; /* 아래쪽 화살표 흰색 */
}

/* ------------------------- */
/* ------------------------- */
/* ------------------------- */

</style>