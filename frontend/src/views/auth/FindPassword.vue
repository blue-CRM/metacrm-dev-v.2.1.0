<!-- src/pages/FindPassword.vue -->
<template>
  <FullScreenLayout>
    <div class="relative xl:bg-gray-100 dark:xl:bg-[#0e1626] dark:bg-[#16213b] bg-white z-1 sm:p-0">
      <div class="relative flex flex-col justify-center w-full min-h-screen overflow-y-auto lg:flex-row">
        <!-- 카드 (왼쪽 : 재설정 / 오른쪽 : 회사소개) -->
        <div :class="['flex w-full max-w-6xl max-h-[700px] xl:bg-white xl:rounded-2xl xl:shadow-lg overflow-y-auto dark:bg-[#16213b] dark:xl:bg-[#16213b] dark:xl:rounded-2xl dark:xl:shadow-lg', mt]">

          <!-- LEFT -->
          <div class="flex flex-col flex-1 w-full lg:w-1/2 p-10">
            <div class="flex flex-col justify-center flex-1 w-full max-w-md mx-auto">
              <div>
                <div class="mb-5 sm:mb-8 flex justify-between items-start">
                  <div>
                    <h1 class="mb-2 font-semibold text-gray-800 text-title-sm dark:text-white/90 sm:text-title-md">
                      Reset Password
                    </h1>
                    <p class="text-sm text-gray-500 dark:text-gray-400">
                      이메일 인증 후 새 비밀번호를 설정하세요
                    </p>
                  </div>
                </div>

                <form class="space-y-5" @submit.prevent>
                  <!-- Email -->
                  <div>
                    <label class="mb-1.5 block text-sm font-medium text-gray-700 dark:text-gray-400">
                      Email<span class="text-error-500">*</span>
                    </label>
                    <div class="flex gap-2">
                      <input
                          v-model="email"
                          type="email"
                          autocomplete="email"
                          placeholder="info@gmail.com"
                          :disabled="verified"
                          class="h-11 w-full rounded-lg border border-gray-300 bg-transparent px-4 text-sm text-gray-800 shadow-theme-xs placeholder:text-gray-400
                               focus:outline-hidden focus:border-brand-300 focus:ring-3 focus:ring-brand-500/10 dark:border-[#28365a] dark:bg-[#0e1626] dark:text-white/90
                               dark:placeholder:text-white/30 dark:focus:border-brand-800"/>
                      <button
                          type="button"
                          class="shrink-0 px-4 py-2.5 rounded-lg text-sm font-medium text-white transition bg-brand-500 hover:bg-brand-600 disabled:opacity-50"
                          :disabled="verified"
                          @click="sendCode">
                        {{ verified ? '인증완료' : (codeSent ? '&nbsp;&nbsp;재요청&nbsp;&nbsp;' : '인증요청') }}
                      </button>
                    </div>
                  </div>

                  <!-- 인증코드 -->
                  <div v-if="codeSent && !verified">
                    <label class="mb-1.5 block text-sm font-medium text-gray-700 dark:text-gray-400">
                      인증코드<span class="text-error-500">*</span>
                    </label>
                    <div class="flex gap-2">
                      <input
                          ref="codeInput"
                          v-model="code"
                          autocomplete="one-time-code"
                          placeholder="인증코드 입력"
                          @keydown.enter.prevent
                          class="h-11 w-full rounded-lg border border-gray-300 bg-transparent px-4 text-sm text-gray-800 shadow-theme-xs
                               placeholder:text-gray-400 focus:outline-hidden focus:border-brand-300 focus:ring-3 focus:ring-brand-500/10 dark:border-[#28365a]
                               dark:bg-[#0e1626] dark:text-white/90 dark:placeholder:text-white/30 dark:focus:border-brand-800"/>
                      <button
                          type="button"
                          class="shrink-0 px-4 py-2.5 rounded-lg text-sm text-gray-700 transition bg-gray-100 hover:bg-gray-200
                               dark:bg-white/5 dark:text-white/90 dark:hover:bg-white/10"
                          @click="verifyCode">
                        인증하기
                      </button>
                    </div>

                    <!-- 남은 시간 / 연장 -->
                    <div class="flex items-center justify-between mt-2">
                      <p class="text-sm text-blue-600">남은시간 {{ mm }}:{{ ss }}</p>
                      <button
                          type="button"
                          class="px-3 py-2 rounded-lg text-sm text-gray-700 dark:text-gray-300 hover:text-brand-500 disabled:opacity-50
                               disabled:hover:text-gray-700 dark:disabled:hover:text-gray-300"
                          :disabled="extendedOnce"
                          @click="extendTime">
                        &nbsp;&nbsp;1회 연장&nbsp;&nbsp;
                      </button>
                    </div>
                  </div>

                  <!-- 새 비밀번호 (이메일 인증 후 노출) -->
                  <div v-if="verified">
                    <label class="mb-1.5 block text-sm font-medium text-gray-700 dark:text-gray-400">
                      새 비밀번호<span class="text-error-500">*</span>
                    </label>
                    <div class="relative">
                      <input
                          ref="pw1Input"
                          v-model.trim="password1"
                          :type="showPw1 ? 'text' : 'password'"
                          autocomplete="new-password"
                          placeholder="새 비밀번호 (최소 6자)"
                          @keyup.enter.prevent="doReset"
                          class="h-11 w-full rounded-lg border border-gray-300 bg-transparent py-2.5 pl-4 pr-11 text-sm text-gray-800 shadow-theme-xs
                               placeholder:text-gray-400 focus:outline-hidden focus:border-brand-300 focus:ring-3 focus:ring-brand-500/10 dark:border-[#28365a]
                               dark:bg-[#0e1626] dark:text-white/90 dark:placeholder:text-white/30 dark:focus:border-brand-800"/>
                      <span
                          @click="showPw1 = !showPw1"
                          class="absolute z-30 -translate-y-1/2 cursor-pointer right-4 top-1/2"
                      >
                        <EyeIcon v-if="showPw1" class="w-5 h-5 text-gray-500 dark:text-gray-400"/>
                        <EyeSlashIcon v-else class="w-5 h-5 text-gray-500 dark:text-gray-400"/>
                      </span>
                    </div>
                    <p v-if="pwTooShort" class="mt-1 text-xs text-error-500">최소 6자 이상 입력하세요.</p>
                  </div>

                  <!-- 새 비밀번호 확인 -->
                  <div v-if="verified">
                    <label class="mb-1.5 block text-sm font-medium text-gray-700 dark:text-gray-400">
                      새 비밀번호 확인<span class="text-error-500">*</span>
                    </label>
                    <div class="relative">
                      <input
                          ref="pw2Input"
                          v-model.trim="password2"
                          :type="showPw2 ? 'text' : 'password'"
                          autocomplete="new-password"
                          placeholder="새 비밀번호를 다시 입력"
                          @keyup.enter.prevent="doReset"
                          class="h-11 w-full rounded-lg border border-gray-300 bg-transparent py-2.5 pl-4 pr-11 text-sm text-gray-800 shadow-theme-xs
                               placeholder:text-gray-400 focus:outline-hidden focus:border-brand-300 focus:ring-3 focus:ring-brand-500/10 dark:border-[#28365a]
                               dark:bg-[#0e1626] dark:text-white/90 dark:placeholder:text-white/30 dark:focus:border-brand-800"/>
                      <span
                          @click="showPw2 = !showPw2"
                          class="absolute z-30 -translate-y-1/2 cursor-pointer right-4 top-1/2"
                      >
                        <EyeIcon v-if="showPw2" class="w-5 h-5 text-gray-500 dark:text-gray-400"/>
                        <EyeSlashIcon v-else class="w-5 h-5 text-gray-500 dark:text-gray-400"/>
                      </span>
                    </div>
                    <p v-if="pwMismatch" class="mt-1 text-xs text-error-500">비밀번호가 일치하지 않습니다.</p>
                  </div>

                  <!-- Button -->
                  <div v-if="verified">
                    <button
                        type="button"
                        @click="doReset"
                        class="flex items-center justify-center w-full px-4 py-3 text-sm font-medium text-white transition rounded-lg shadow-theme-xs
                             disabled:opacity-50 bg-brand-500 hover:bg-brand-600 disabled:hover:bg-brand-500"
                        :disabled="!canSubmit">
                      비밀번호 변경
                    </button>
                  </div>
                </form>

                <div class="mt-5">
                  <div class="flex items-center justify-between text-sm font-normal">
                    <span class="text-sm text-gray-500 dark:text-gray-400">
                      로그인 화면으로 돌아가기
                      <router-link to="/login" class="ml-1 text-brand-500 hover:text-brand-600 dark:text-brand-400">
                        로그인
                      </router-link>
                    </span>
                    <router-link to="/signup" class="text-brand-500 hover:text-brand-600 dark:text-brand-400">
                      회원가입
                    </router-link>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- RIGHT -->
          <div class="relative items-center hidden w-full h-full lg:w-1/2 bg-brand-950 dark:bg-white/5 xl:grid">
            <div class="flex items-center justify-center z-1">
              <CommonGridShape />
              <div class="flex flex-col items-center max-w-xs">
                <router-link to="/" class="block mb-4">
<!--                  <img width="231" height="48" src="/images/logo/dark_menu_logo.png" alt="Logo" />-->
                  <img class="h-auto w-auto" src="/images/logo/dark_menu_logo.png" alt="Logo" />
                </router-link>
<!--                <p class="text-center text-gray-400 dark:text-white/60">-->
<!--                  회사소개 한줄 소개 / 회사소개 한줄 소개 / 회사소개 한줄 소개 / 회사소개 한줄 소개-->
<!--                </p>-->
              </div>
            </div>
          </div>

        </div>
      </div>

      <!-- 다크모드 토글: 오른쪽 하단 고정 -->
      <div class="absolute bottom-4 right-4">
        <ThemeToggler />
      </div>
    </div>
  </FullScreenLayout>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useMailStore } from '@/stores/mail'
import { useRouter } from 'vue-router'
import CommonGridShape from '@/components/common/CommonGridShape.vue'
import FullScreenLayout from '@/components/layout/FullScreenLayout.vue'
import { EyeIcon, EyeSlashIcon } from '@heroicons/vue/24/outline'
import ThemeToggler from '@/components/common/ThemeToggler.vue'
import { useIdleRefresh } from '@/composables/useIdleRefresh.js'
import axios from '@/plugins/axios.js'

/** ── 상단 여백(줌/배율 대응) ────────────────────────────────────────────── */
const mt = ref('mt-[0vh]')
const updateMargin = () => {
  const logicalWidth = window.innerWidth
  const ratio = window.devicePixelRatio
  const width = logicalWidth * ratio
  if (width >= 1950) mt.value = 'mt-[15vh]'
  else if (width >= 950) mt.value = 'mt-[10vh]'
  else mt.value = 'mt-[0vh]'
}
onMounted(() => { updateMargin(); window.addEventListener('resize', updateMargin) })
onUnmounted(() => window.removeEventListener('resize', updateMargin))

/** ── 상태 ─────────────────────────────────────────────────────────────── */
const auth = useAuthStore()
const mail = useMailStore()
const router = useRouter()

// 입력값
const email = ref('')
const code = ref('')
const codeSent = ref(false)
const verified = ref(false)

// 인증 완료 후부터 3분 유휴 감시
useIdleRefresh({ enabled: () => verified.value, timeoutMs: 1000 * 60 * 3 })

// 타이머
const ttl = ref(0)
let timer = null
const extendedOnce = ref(false)
const mm = computed(() => String(Math.floor(ttl.value / 60)).padStart(2, '0'))
const ss = computed(() => String(ttl.value % 60).padStart(2, '0'))

// 새 비밀번호
const password1 = ref('')
const password2 = ref('')
const showPw1 = ref(false)
const showPw2 = ref(false)

// 유효성
const pwTooShort = computed(() => password1.value.length > 0 && password1.value.length < 6)
const pwMismatch  = computed(() => password2.value.length > 0 && password1.value !== password2.value)
const canSubmit   = computed(() =>
    verified.value &&
    password1.value.length >= 6 &&
    password2.value.length >= 6 &&
    password1.value === password2.value
)

// 포커스
const codeInput = ref(null)
const pw1Input = ref(null)
const pw2Input = ref(null)

/** ── 메일 인증 흐름 ───────────────────────────────────────────────────── */
const sendCode = async () => {
  const regex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!regex.test(email.value)) { alert('이메일 형식이 올바르지 않습니다.'); return }
  const { ttl: newTtl } = await mail.sendCode(email.value)
  if (!newTtl) return
  ttl.value = newTtl
  codeSent.value = true
  verified.value = false
  extendedOnce.value = false
  startCountdown()
  await nextTick(() => codeInput.value?.focus())
}

const verifyCode = async () => {
  mail.email = email.value
  await mail.verifyCode(code.value)
  if (mail.codeVerified) {
    verified.value = true
    stopCountdown()
    await nextTick(() => pw1Input.value?.focus())
  } else {
    console.log('인증번호가 올바르지 않습니다')
  }
}

const extendTime = async () => {
  if (extendedOnce.value) return
  const { ttl: newTtl } = await mail.extendCode()
  if (!newTtl) return
  ttl.value = newTtl
  startCountdown()
  extendedOnce.value = true
}

const startCountdown = () => {
  clearInterval(timer)
  timer = setInterval(() => {
    if (ttl.value > 0) ttl.value--
    else {
      clearInterval(timer)
      codeSent.value = false
      verified.value = false
    }
  }, 1000)
}
const stopCountdown = () => clearInterval(timer)
onUnmounted(() => stopCountdown())

/** ── 비밀번호 재설정 (axios 직접 호출) ─────────────────────────────────── */
const isSubmitting = ref(false)

const doReset = async () => {
  if (!canSubmit.value || isSubmitting.value) return
  isSubmitting.value = true
  try {
    // 필요 시 헤더를 통해 인터셉터의 Authorization 부착을 스킵하도록 신호를 줄 수도 있음
    // const headers = { 'X-Anonymous-Request': 'true' }

    // 백엔드 엔드포인트 예시: POST /api/auth/password/reset
    await axios.post('/api/auth/password/reset', {
      email: email.value,
      code: code.value,
      newPassword: password1.value
    }/*, { headers }*/)

    alert('비밀번호가 변경되었습니다. 다시 로그인해주세요.')
    await router.push('/login')
  } catch (e) {
    const s = e?.response?.status
    const msg = e?.response?.data?.message
    if (s === 404) alert(msg || '가입된 이메일이 아닙니다.')
    else if (s === 410) alert(msg || '계정이 비활성화되어 변경할 수 없습니다.')
    else if (s === 429) alert(msg || '요청이 너무 잦습니다. 잠시 후 다시 시도하세요.')
    else alert(msg || '비밀번호 변경에 실패했습니다.')
    console.error(e)
  } finally {
    isSubmitting.value = false
  }
}
</script>
