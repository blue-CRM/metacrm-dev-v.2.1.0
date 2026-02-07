<template>
  <FullScreenLayout>
    <div class="relative xl:bg-gray-100 dark:xl:bg-gray-900 dark:bg-gray-800 bg-white z-1 sm:p-0">
      <div class="relative flex flex-col justify-center w-full min-h-screen overflow-y-auto lg:flex-row">
        <!-- 카드 (왼쪽 : 로그인 / 오른쪽 : 회사소개) -->
        <div :class="[ 'flex w-full max-w-6xl max-h-[700px] xl:bg-white xl:rounded-2xl xl:shadow-lg overflow-y-auto dark:bg-gray-800 dark:xl:bg-gray-800 dark:xl:rounded-2xl dark:xl:shadow-lg', mt ]">

        <!-- LEFT -->
        <div class="flex flex-col flex-1 w-full lg:w-1/2 p-10">
          <div class="flex flex-col justify-center flex-1 w-full max-w-md mx-auto">
            <div>
              <div class="mb-5 sm:mb-8 flex justify-between items-start">
                <!-- 제목 + 설명 -->
                <div>
                  <h1
                      class="mb-2 font-semibold text-gray-800 text-title-sm dark:text-white/90 sm:text-title-md"
                  >
                    Sign In
                  </h1>
                  <p class="text-sm text-gray-500 dark:text-gray-400">
                    이메일 인증 후 비밀번호를 입력하세요
                  </p>
                </div>
              </div>

              <form class="space-y-5">
                <!-- Email -->
                <div>
                  <label class="mb-1.5 block text-sm font-medium text-gray-700 dark:text-gray-400">
                    Email<span class="text-error-500">*</span>
                  </label>
                  <div class="flex gap-2">
                    <input
                        v-model="email"
                        type="email"
                        placeholder="info@gmail.com"
                        :disabled="REQUIRE_EMAIL_VERIFY && verified"
                        autocomplete="username"
                        class="h-11 w-full rounded-lg border border-gray-300 bg-transparent px-4 text-sm text-gray-800 shadow-theme-xs placeholder:text-gray-400
                             focus:outline-hidden focus:border-brand-300 focus:ring-3 focus:ring-brand-500/10 dark:border-gray-700 dark:bg-gray-900 dark:text-white/90
                             dark:placeholder:text-white/30 dark:focus:border-brand-800"/>
                    <button
                        v-if="REQUIRE_EMAIL_VERIFY"
                        type="button"
                        class="shrink-0 px-4 py-2.5 rounded-lg text-sm font-medium text-white transition bg-brand-500 hover:bg-brand-600 disabled:opacity-50"
                        :disabled="verified"
                        @click="sendCode">
                      {{ verified ? '인증완료' : (codeSent ? '&nbsp;&nbsp;재요청&nbsp;&nbsp;' : '인증요청') }}
                    </button>
                  </div>
                </div>

                <!-- 인증코드 -->
                <div v-if="REQUIRE_EMAIL_VERIFY && codeSent && !verified">
                  <label class="mb-1.5 block text-sm font-medium text-gray-700 dark:text-gray-400">
                    인증코드<span class="text-error-500">*</span>
                  </label>
                  <div class="flex gap-2">
                    <input
                        ref="codeInput"
                        v-model="code"
                        placeholder="인증코드 입력"
                        class="h-11 w-full rounded-lg border border-gray-300 bg-transparent px-4 text-sm text-gray-800 shadow-theme-xs
                             placeholder:text-gray-400 focus:outline-hidden focus:border-brand-300 focus:ring-3 focus:ring-brand-500/10 dark:border-gray-700
                             dark:bg-gray-900 dark:text-white/90 dark:placeholder:text-white/30 dark:focus:border-brand-800"/>
                    <button
                        type="button"
                        class="shrink-0 px-4 py-2.5 rounded-lg text-sm font-medium text-gray-700 transition bg-gray-100 hover:bg-gray-200
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
                        class="px-3 py-2 rounded-lg text-sm
                             text-gray-700 dark:text-gray-300
                             hover:text-brand-500 disabled:opacity-50
                             disabled:hover:text-gray-700 dark:disabled:hover:text-gray-300"
                        :disabled="extendedOnce"
                        @click="extendTime">
                      &nbsp;&nbsp;1회 연장&nbsp;&nbsp;
                    </button>
                  </div>
                </div>

                <!-- Password -->
                <div>
                  <label class="mb-1.5 block text-sm font-medium text-gray-700 dark:text-gray-400">
                    Password<span class="text-error-500">*</span>
                  </label>
                  <div class="relative">
                    <input
                        ref="passwordInput"
                        v-model="password"
                        :type="showPassword ? 'text' : 'password'"
                        placeholder="비밀번호를 입력하세요"
                        @keyup.enter.prevent="doLogin"
                        autocomplete="current-password"
                        class="h-11 w-full rounded-lg border border-gray-300 bg-transparent py-2.5 pl-4 pr-11 text-sm text-gray-800 shadow-theme-xs
                             placeholder:text-gray-400 focus:outline-hidden focus:border-brand-300 focus:ring-3 focus:ring-brand-500/10 dark:border-gray-700
                             dark:bg-gray-900 dark:text-white/90 dark:placeholder:text-white/30 dark:focus:border-brand-800"/>
                    <span
                        @click="togglePasswordVisibility"
                        class="absolute z-30 -translate-y-1/2 cursor-pointer right-4 top-1/2"
                    >
                      <EyeIcon v-if="showPassword" class="w-5 h-5 text-gray-500 dark:text-gray-400"/>
                      <EyeSlashIcon v-else class="w-5 h-5 text-gray-500 dark:text-gray-400"/>
                    </span>
                  </div>
                </div>

                <!-- Button -->
                <div>
                  <button
                      type="button"
                      @click="doLogin"
                      class="flex items-center justify-center w-full px-4 py-3 text-sm font-medium text-white transition rounded-lg shadow-theme-xs
                             disabled:opacity-50 bg-brand-500 hover:bg-brand-600 disabled:hover:bg-brand-500"
                      :disabled="!canSignIn">
                    Sign In
                  </button>
                </div>
              </form>

              <div class="mt-5">
                <div class="flex items-center justify-between text-sm font-normal">
                  <!-- 왼쪽 묶음 -->
                  <span class="text-sm text-gray-500 dark:text-gray-400">
                    아직 계정이 없나요?
                    <router-link to="/signup" class="ml-1 text-brand-500 hover:text-brand-600 dark:text-brand-400">
                      회원가입
                    </router-link>
                  </span>
                  <!-- 오른쪽 -->
                  <router-link to="/find-password" class="text-brand-500 hover:text-brand-600 dark:text-brand-400">
                    비밀번호 초기화
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
<!--              <p class="text-center text-gray-400 dark:text-white/60">-->
<!--                회사소개 한줄 소개 / 회사소개 한줄 소개 / 회사소개 한줄 소개 / 회사소개 한줄 소개-->
<!--              </p>-->
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
import {ref, computed, onMounted, onUnmounted, nextTick} from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useMailStore } from '@/stores/mail'
import { useRouter } from 'vue-router'
import CommonGridShape from '@/components/common/CommonGridShape.vue'
import FullScreenLayout from '@/components/layout/FullScreenLayout.vue'
import { EyeIcon, EyeSlashIcon } from '@heroicons/vue/24/outline'
import ThemeToggler from "@/components/common/ThemeToggler.vue";
import { useIdleRefresh } from '@/composables/useIdleRefresh.js'

// 임시로 인증 우회할 때 false
const REQUIRE_EMAIL_VERIFY = false
const canSignIn = computed(() => !REQUIRE_EMAIL_VERIFY || verified.value)

// 줌 비율 적용 시작
const mt = ref('mt-[0vh]')

const updateMargin = () => {
  const logicalWidth = window.innerWidth       // CSS px
  const ratio = window.devicePixelRatio        // 배율
  const width = logicalWidth * ratio           // 물리적 px 비슷하게

  if (width >= 1950) mt.value = 'mt-[15vh]'
  else if (width >= 950) mt.value = 'mt-[10vh]'
  else mt.value = 'mt-[0vh]'
}

onMounted(() => {
  updateMargin()
  window.addEventListener('resize', updateMargin)
})
onUnmounted(() => {
  window.removeEventListener('resize', updateMargin)
})
// 줌 비율 적용 끝

const auth = useAuthStore()
const mail = useMailStore()
const router = useRouter()

// 로그인 필드
const email = ref('')
const password = ref('')

// 인증번호 필드
const code = ref('')
const codeSent = ref(false)
const verified = ref(false)
const ttl = ref(0)
let timer = null
const extendedOnce = ref(false)


// 인증 완료 후부터 3분 유휴 감시
useIdleRefresh({ enabled: () => verified.value, timeoutMs: 1000 * 60 * 3 })

// 포커스 이동
const codeInput = ref(null)
const passwordInput = ref(null)

// 상태토글 버튼
const showPassword = ref(false)

// 타이머
const mm = computed(() => String(Math.floor(ttl.value / 60)).padStart(2, '0'))
const ss = computed(() => String(ttl.value % 60).padStart(2, '0'))

// 인증코드 전송
const sendCode = async () => {
  // 예외처리
  const regex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!regex.test(email.value)) {
    alert('이메일 형식이 올바르지 않습니다.')
    return
  }

  const { ttl: newTtl } = await mail.sendCode(email.value)
  if (!newTtl) return

  ttl.value = newTtl
  codeSent.value = true
  verified.value = false
  extendedOnce.value = false
  startCountdown()

  // 포커스 이동
  await nextTick(() => codeInput.value?.focus())
}

// 인증코드 확인
const verifyCode = async () => {
  mail.email = email.value
  await mail.verifyCode(code.value)
  if (mail.codeVerified) {
    verified.value = true
    stopCountdown()

    // 포커스 이동
    await nextTick(() => passwordInput.value?.focus())
  } else {
    console.log('인증번호가 올바르지 않습니다')
  }
}

// 시간 연장 (1회)
const extendTime = async () => {
  if (extendedOnce.value) return
  const { ttl: newTtl } = await mail.extendCode()
  if (!newTtl) return

  ttl.value = newTtl
  startCountdown()
  extendedOnce.value = true
}

// 카운트다운
const startCountdown = () => {
  clearInterval(timer)
  timer = setInterval(() => {
    if (ttl.value > 0) {
      ttl.value--
    } else {
      clearInterval(timer)
      codeSent.value = false
      verified.value = false
    }
  }, 1000)
}
const stopCountdown = () => clearInterval(timer)
onUnmounted(() => stopCountdown())

// 비밀번호 보기 토글
const togglePasswordVisibility = () => {
  showPassword.value = !showPassword.value
}

// 로그인
const doLogin = async () => {
  // 예외처리
  const regex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!regex.test(email.value)) {
    alert('이메일 형식이 올바르지 않습니다.')
    return
  }
  if (REQUIRE_EMAIL_VERIFY && !verified.value) {
    alert('이메일 인증을 먼저 완료해주세요')
    return
  }

  // 로그인 시도
  try {
    await auth.login({ email: email.value, password: password.value })
    console.log('로그인 성공')
    console.log(auth.name)
    console.log(auth.grants.isSuper)
    console.log(auth.grants.role)
    console.log(JSON.stringify(auth.grants.perms, null, 2))
    await router.push('/')
  } catch (err) {
    console.log('로그인 실패')
  }
}
</script>