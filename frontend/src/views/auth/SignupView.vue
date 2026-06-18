<template>
  <FullScreenLayout>
    <div class="relative xl:bg-gray-100 dark:xl:bg-[#0e1626] dark:bg-[#16213b] bg-white z-1 sm:p-0">
      <div class="relative flex flex-col justify-center w-full min-h-screen overflow-y-auto lg:flex-row">
        <!-- 카드 -->
        <div :class="['flex w-full max-w-6xl max-h-[850px] xl:bg-white xl:rounded-2xl xl:shadow-lg overflow-y-auto dark:bg-[#16213b] dark:xl:bg-[#16213b] dark:xl:rounded-2xl dark:xl:shadow-lg', mt]">

        <!-- LEFT -->
        <div class="flex flex-col flex-1 w-full lg:w-1/2 p-10">
          <div class="flex flex-col justify-center flex-1 w-full max-w-md mx-auto">
            <div>
              <div class="mb-5 sm:mb-8">
                <h1 class="mb-2 font-semibold text-gray-800 text-title-sm dark:text-white/90 sm:text-title-md">
                  Sign Up
                </h1>
                <p class="text-sm text-gray-500 dark:text-gray-400">
                  이메일 인증 후 정보를 입력해주세요
                </p>
              </div>

              <form @submit.prevent="doSignup" class="space-y-2">
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
                        :disabled="verified"
                        autocomplete="email"
                        class="h-11 w-full rounded-lg border border-gray-300 bg-transparent px-4 text-sm text-gray-800 shadow-theme-xs
                             placeholder:text-gray-400 focus:outline-hidden focus:border-brand-300 focus:ring-3 focus:ring-brand-500/10
                             dark:border-[#28365a] dark:bg-[#0e1626] dark:text-white/90 dark:placeholder:text-white/30"
                    />
                    <button
                        type="button"
                        class="shrink-0 px-4 py-2.5 rounded-lg text-sm font-medium text-white transition bg-brand-500 hover:bg-brand-600 disabled:opacity-50"
                        :disabled="verified"
                        @click="sendCode"
                    >
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
                        placeholder="인증코드 입력"
                        @keyup.enter="verifyCode"
                        class="h-11 w-full rounded-lg border border-gray-300 bg-transparent px-4 text-sm text-gray-800 shadow-theme-xs
                             placeholder:text-gray-400 focus:outline-hidden focus:border-brand-300 focus:ring-3 focus:ring-brand-500/10
                             dark:border-[#28365a] dark:bg-[#0e1626] dark:text-white/90 dark:placeholder:text-white/30"
                    />
                    <button
                        type="button"
                        class="shrink-0 px-4 py-2.5 rounded-lg text-sm font-medium text-gray-700 transition bg-gray-100 hover:bg-gray-200
                             dark:bg-white/5 dark:text-white/90 dark:hover:bg-white/10"
                        @click="verifyCode"
                    >
                      인증하기
                    </button>
                  </div>
                  <div class="flex items-center justify-between mt-0">
                    <p class="text-sm text-blue-600">남은시간 {{ mm }}:{{ ss }}</p>
                    <button
                        type="button"
                        class="px-3 py-2 rounded-lg text-sm text-gray-700 dark:text-gray-300 hover:text-brand-500 disabled:opacity-50 disabled:hover:text-gray-700"
                        :disabled="extendedOnce"
                        @click="extendTime"
                    >
                      &nbsp;&nbsp;1회 연장&nbsp;&nbsp;
                    </button>
                  </div>
                </div>

                <!-- 이름 -->
                <div>
                  <label class="mb-1.5 block text-sm font-medium text-gray-700 dark:text-gray-400">
                    이름<span class="text-error-500">*</span>
                    <span class="text-xs text-gray-400">&nbsp;(2글자 이상)</span>
                  </label>
                  <input
                      v-model="name"
                      @input="handleNameInput"
                      @blur="validateName"
                      placeholder="홍길동"
                      lang="ko"
                      inputmode="text"
                      class="h-11 w-full rounded-lg border border-gray-300 bg-transparent px-4 text-sm text-gray-800 shadow-theme-xs
                           placeholder:text-gray-400 focus:outline-hidden focus:border-brand-300 focus:ring-3 focus:ring-brand-500/10
                           dark:border-[#28365a] dark:bg-[#0e1626] dark:text-white/90 dark:placeholder:text-white/30"
                  />
                  <p v-if="nameError" class="mt-1 text-sm text-error-500">{{ nameError }}</p>
                </div>

                <!-- 전화번호 -->
                <div>
                  <label class="mb-1.5 block text-sm font-medium text-gray-700 dark:text-gray-400">
                    전화번호<span class="text-error-500">*</span>
                  </label>
                  <input
                      v-model="phone"
                      @input="formatPhone"
                      @blur="validatePhone"
                      type="tel"
                      placeholder="010-1234-5678"
                      autocomplete="tel-national"
                      class="h-11 w-full rounded-lg border border-gray-300 bg-transparent px-4 text-sm text-gray-800 shadow-theme-xs
                           placeholder:text-gray-400 focus:outline-hidden focus:border-brand-300 focus:ring-3 focus:ring-brand-500/10
                           dark:border-[#28365a] dark:bg-[#0e1626] dark:text-white/90 dark:placeholder:text-white/30"
                  />
                  <p v-if="phoneError" class="mt-1 text-sm text-error-500">{{ phoneError }}</p>
                </div>

                <!-- Password -->
                <div>
                  <label class="mb-1.5 block text-sm font-medium text-gray-700 dark:text-gray-400">
                    Password<span class="text-error-500">*</span>
                    <span class="text-xs text-gray-400">&nbsp;(6글자 이상)</span>
                  </label>
                  <div class="relative">
                    <input
                        ref="passwordInput"
                        v-model="password"
                        :type="showPassword ? 'text' : 'password'"
                        placeholder="비밀번호 입력"
                        @blur="validatePassword"
                        autocomplete="new-password"
                        class="h-11 w-full rounded-lg border border-gray-300 bg-transparent py-2.5 pl-4 pr-11 text-sm text-gray-800 shadow-theme-xs
                             placeholder:text-gray-400 focus:outline-hidden focus:border-brand-300 focus:ring-3 focus:ring-brand-500/10
                             dark:border-[#28365a] dark:bg-[#0e1626] dark:text-white/90 dark:placeholder:text-white/30"
                    />
                    <span
                        @click="togglePasswordVisibility"
                        class="absolute z-30 -translate-y-1/2 cursor-pointer right-4 top-1/2"
                    >
                      <EyeIcon v-if="showPassword" class="w-5 h-5 text-gray-500 dark:text-gray-400"/>
                      <EyeSlashIcon v-else class="w-5 h-5 text-gray-500 dark:text-gray-400"/>
                    </span>
                  </div>
                  <p v-if="passwordError" class="mt-1 text-sm text-error-500">{{ passwordError }}</p>
                </div>

                <!-- 비밀번호 확인 -->
                <div>
                  <label class="mb-1.5 block text-sm font-medium text-gray-700 dark:text-gray-400">
                    Password 확인<span class="text-error-500">*</span>
                  </label>
                  <div class="relative">
                    <input
                        ref="passwordConfirmInput"
                        v-model="passwordConfirm"
                        :type="showPasswordConfirm ? 'text' : 'password'"
                        placeholder="비밀번호 재입력"
                        autocomplete="new-password"
                        class="h-11 w-full rounded-lg border border-gray-300 bg-transparent py-2.5 pl-4 pr-11 text-sm text-gray-800 shadow-theme-xs
                               placeholder:text-gray-400 focus:outline-hidden focus:border-brand-300 focus:ring-3 focus:ring-brand-500/10
                               dark:border-[#28365a] dark:bg-[#0e1626] dark:text-white/90 dark:placeholder:text-white/30"
                    />
                    <span
                        @click="togglePasswordConfirmVisibility"
                        class="absolute z-30 -translate-y-1/2 cursor-pointer right-4 top-1/2"
                    >
                      <EyeIcon v-if="showPasswordConfirm" class="w-5 h-5 text-gray-500 dark:text-gray-400"/>
                      <EyeSlashIcon v-else class="w-5 h-5 text-gray-500 dark:text-gray-400"/>
                    </span>
                  </div>
                  <p v-if="passwordConfirm && password !== passwordConfirm" class="mt-1 text-sm text-error-500">
                    비밀번호가 일치하지 않습니다.
                  </p>
                </div>

                <!-- 구분 선택 -->
                <div>
                  <label class="mb-1.5 block text-sm font-medium text-gray-700 dark:text-gray-400">
                    구분<span class="text-error-500">*</span>
                  </label>
                  <select
                      v-model="role"
                      class="h-11 w-full rounded-lg border border-gray-300 bg-transparent px-4 text-sm text-gray-800 shadow-theme-xs
                           focus:outline-hidden focus:border-brand-300 focus:ring-3 focus:ring-brand-500/10 dark:border-[#28365a] dark:bg-[#0e1626] dark:text-white/30"
                  >
                    <option value="SUPERADMIN">본사</option>
                    <option value="MANAGER">팀장</option>
                    <option value="STAFF">프로</option>
                    <option value="CENTERHEAD">센터장</option>
                    <option value="EXPERT">전문가</option>
                  </select>
                </div>

                <!-- Submit -->
                <div>
                  <button
                      type="submit"
                      class="flex items-center justify-center w-full px-4 py-3 text-sm font-medium text-white transition rounded-lg shadow-theme-xs
                           bg-brand-500 hover:bg-brand-600 disabled:opacity-50"
                      :disabled="!verified"
                  >
                    회원가입
                  </button>
                </div>
              </form>

              <div class="mt-1">
                <router-link to="/login" class="ml-1 text-brand-500 hover:text-brand-600 dark:text-brand-400">
                  로그인으로 돌아가기
                </router-link>
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
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useMailStore } from '@/stores/mail'
import CommonGridShape from '@/components/common/CommonGridShape.vue'
import FullScreenLayout from '@/components/layout/FullScreenLayout.vue'
import { EyeIcon, EyeSlashIcon } from '@heroicons/vue/24/outline'
import ThemeToggler from "@/components/common/ThemeToggler.vue";
import { useIdleRefresh } from "@/composables/useIdleRefresh.js";

const auth = useAuthStore()
const mail = useMailStore()

// 필드
const email = ref('')
const password = ref('')
const passwordConfirm = ref('')
const role = ref('STAFF')
const name = ref('')
const phone = ref('')

// 인증
const code = ref('')
const codeSent = ref(false)
const verified = ref(false)
const ttl = ref(0)
let timer = null
const extendedOnce = ref(false)

// 인증 완료 후부터 3분 유휴 감시
useIdleRefresh({ enabled: () => verified.value, timeoutMs: 1000 * 60 * 3 })

const codeInput = ref(null)
const passwordInput = ref(null)
const showPassword = ref(false)
const passwordConfirmInput = ref(null)
const showPasswordConfirm = ref(false)

// 실시간 유저 피드백 (blur시 유효성 검사)
const nameError = ref('')
const phoneError = ref('')
const passwordError = ref('')

// 타이머 계산
const mm = computed(() => String(Math.floor(ttl.value / 60)).padStart(2, '0'))
const ss = computed(() => String(ttl.value % 60).padStart(2, '0'))

// 코드 전송
const sendCode = async () => {
  // 이메일 형식 검사
  const regex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!regex.test(email.value)) {
    alert('이메일 형식이 올바르지 않습니다.')
    return
  }

  // 이메일 중복확인
  const exists = await auth.checkEmailDuplicate(email.value)
  if (exists) {
    alert('이미 사용 중인 이메일입니다.')
    verified.value = false
    return
  }

  const { ttl: newTtl } = await mail.sendCode(email.value)
  if (!newTtl) return

  ttl.value = newTtl
  codeSent.value = true
  verified.value = false
  extendedOnce.value = false
  startCountdown()
  await nextTick(() => codeInput.value?.focus())
}

// 코드 확인
const verifyCode = async () => {
  mail.email = email.value
  await mail.verifyCode(code.value)

  if (mail.codeVerified) {
    verified.value = true
    stopCountdown()
    await nextTick(() => passwordInput.value?.focus())
  }
}

// 시간 연장
const extendTime = async () => {
  if (extendedOnce.value) return
  const { ttl: newTtl } = await mail.extendCode()

  if (!newTtl) return
  ttl.value = newTtl
  startCountdown()
  extendedOnce.value = true
}

// 타이머 관리
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

// 이름 입력 처리
const handleNameInput = () => {
  name.value = name.value.replace(/[a-z]+/g, (match) => match.toUpperCase())
}
const validateName = () => {
  if (!name.value) {
    nameError.value = ''
    return
  }
  if (name.value.length < 2) {
    nameError.value = '이름은 최소 2글자 이상이어야 합니다.'
  } else {
    nameError.value = ''
  }
}

// 전화번호 포맷팅
const formatPhone = () => {
  let digits = phone.value.replace(/\D/g, '')
  if (digits.length <= 3) phone.value = digits
  else if (digits.length <= 7) phone.value = `${digits.slice(0,3)}-${digits.slice(3)}`
  else phone.value = `${digits.slice(0,3)}-${digits.slice(3,7)}-${digits.slice(7,11)}`
}
const validatePhone = () => {
  if (!phone.value) {
    phoneError.value = ''
    return
  }
  if (!/^010-\d{4}-\d{4}$/.test(phone.value)) {
    phoneError.value = '전화번호는 010-1234-5678 형식으로 입력해야 합니다.'
  } else {
    phoneError.value = ''
  }
}

// 비밀번호 보기
const togglePasswordVisibility = () => {
  showPassword.value = !showPassword.value
}
const togglePasswordConfirmVisibility = () => {
  showPasswordConfirm.value = !showPasswordConfirm.value
}
const validatePassword = () => {
  if (password.value && password.value.length < 6) {
    passwordError.value = '비밀번호는 최소 6자리 이상이어야 합니다.'
  } else {
    passwordError.value = ''
  }
}

// 회원가입
const doSignup = async () => {
  // 1. 이메일 인증 여부
  if (!verified.value) {
    alert('이메일 인증을 먼저 완료해주세요')
    return
  }

  // 2. 이름 검사
  if (name.value.length < 2) {
    alert('이름은 최소 2글자 이상이어야 합니다')
    return
  }

  // 3. 전화번호 검사
  if (!/^010-\d{4}-\d{4}$/.test(phone.value)) {
    alert('전화번호는 010-1234-5678 형식으로 입력해야 합니다')
    return
  }

  // 4. 비밀번호 검사
  if (password.value.length < 6) {
    alert('비밀번호는 최소 6자리 이상이어야 합니다')
    return
  }
  if (password.value !== passwordConfirm.value) {
    alert('비밀번호와 비밀번호 확인이 일치하지 않습니다')
    return
  }

  try {
    await auth.signup({
      email: email.value,
      password: password.value,
      role: role.value,
      name: name.value,
      phone: phone.value
    })
  } catch (err) {
    console.error('회원가입 실패', err)
  }
}

// 줌 비율 적용 (로그인 디자인 참고)
const mt = ref('mt-[0vh]')
const updateMargin = () => {
  const logicalWidth = window.innerWidth
  const ratio = window.devicePixelRatio
  const width = logicalWidth * ratio
  if (width >= 1950) mt.value = 'mt-[7vh]'
  else if (width >= 950) mt.value = 'mt-[5vh]'
  else mt.value = 'mt-[0vh]'
}
onMounted(() => {
  updateMargin()
  window.addEventListener('resize', updateMargin)
})
onUnmounted(() => {
  window.removeEventListener('resize', updateMargin)
})
</script>
