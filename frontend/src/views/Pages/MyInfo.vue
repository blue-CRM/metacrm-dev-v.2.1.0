<template>
  <AdminLayout>
    <PageBreadcrumb pageTitle="내 정보 수정" />

    <div
        class="relative overflow-x-auto overflow-y-visible rounded-2xl border border-gray-200 bg-white p-0
             dark:border-gray-800 dark:bg-white/[0.03]"
    >
      <!-- 장식 그리드 -->
      <div
          class="pointer-events-none absolute inset-y-0 left-1/2 -translate-x-1/2 w-[100%] max-w-none select-none"
          aria-hidden="true"
      >
        <CommonGridShape
            src="/images/shape/grid-01.svg"
            mode="mask"
            size="w-56 h-56"
            position="-top-1 -right-4"
            opacity="opacity-[0.06] dark:opacity-[0.10]"
            lightColor="bg-gray-900"
            darkColor="dark:bg-gray-900"
        />
      </div>

      <!-- 가운데 정렬 컨테이너 -->
      <div class="relative z-10 mx-auto my-10 w-full max-w-[min(48rem,calc(100%-48px))]">
        <!-- 헤더 -->
        <div class="min-w-0 pt-5 lg:pt-6 mb-15">
          <h2 class="truncate text-2xl font-semibold text-gray-800 dark:text-white/90">
            {{ name || '사용자' }}
          </h2>
          <div class="mt-1 flex flex-wrap items-center gap-x-4 gap-y-1 text-m text-gray-500 dark:text-gray-400">
            <span>구분: <b class="text-gray-700 dark:text-gray-300">{{ roleLabel }}</b></span>
            <span>소속: <b class="text-gray-700 dark:text-gray-300">{{ orgLabel }}</b></span>
          </div>
<!--          <span v-if="!isVerified" class="text-gray-500 dark:text-gray-400 text-sm" >이메일 인증 후 내 정보 수정이 가능합니다.</span>-->
        </div>

        <!-- 공통 2열 폼 그리드 -->
        <div class="form-grid grid grid-cols-[6rem,1fr] items-start gap-x-10 gap-y-3 mx-2">

          <!-- 이메일 -->
<!--          <div class="text-sm font-medium text-gray-700 dark:text-gray-300 mt-2">이메일</div>-->
<!--          <div class="col-start-2">-->
<!--            <div class="flex items-center gap-3">-->
<!--              <span-->
<!--                  class="truncate flex-1 min-w-[12rem] text-gray-800 dark:text-gray-300 ml-2"-->
<!--                  :title="email || '-'"-->
<!--              >-->
<!--                {{ email || '-' }}-->
<!--              </span>-->
<!--              <button-->
<!--                  type="button"-->
<!--                  class="h-11 px-4 rounded-lg text-sm font-medium text-white transition-->
<!--               bg-brand-500 hover:bg-brand-600 disabled:opacity-50 ml-auto"-->
<!--                  :disabled="sendingCode || isVerified"-->
<!--                  @click="sendVerify"-->
<!--              >-->
<!--                {{ isVerified ? '인증완료' : (sendingCode ? '전송 중...' : (codeSent ? '&nbsp;&nbsp;재요청&nbsp;&nbsp;' : '인증요청')) }}-->
<!--              </button>-->
<!--            </div>-->
<!--          </div>-->

          <!-- (2번째 화면) 인증코드 입력: codeSent && !isVerified -->
<!--          <template v-if="codeSent && !isVerified">-->
<!--            <div class="text-sm font-medium text-gray-700 dark:text-gray-300 mt-2">인증번호</div>-->
<!--            <div class="col-start-2">-->
<!--              <div class="flex items-center gap-2 flex-wrap sm:flex-nowrap">-->
<!--                <input-->
<!--                    v-model="verifyCode"-->
<!--                    placeholder="메일로 받은 6자리"-->
<!--                    class="h-11 flex-1 min-w-[12rem] rounded-lg border px-3-->
<!--                 bg-white text-gray-800 focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10-->
<!--                 dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100"-->
<!--                />-->
<!--                <button-->
<!--                    type="button"-->
<!--                    class="h-11 px-4 rounded-lg text-sm font-medium-->
<!--                 bg-gray-100 hover:bg-gray-200 text-gray-700-->
<!--                 dark:bg-white/5 dark:text-white/90 dark:hover:bg-white/10"-->
<!--                    :disabled="verifying"-->
<!--                    @click="verifyNow"-->
<!--                >-->
<!--                  {{ verifying ? '확인 중...' : '인증하기' }}-->
<!--                </button>-->
<!--              </div>-->
<!--            </div>-->

<!--            <div class="text-sm font-medium text-gray-700 dark:text-gray-300">남은시간</div>-->
<!--            <div class="col-start-2">-->
<!--              <div class="flex items-center justify-between">-->
<!--                <p class="text-sm text-blue-600">{{ mm }}:{{ ss }}</p>-->
<!--                <button-->
<!--                    type="button"-->
<!--                    class="px-3 py-2 rounded-lg text-sm-->
<!--                 text-gray-700 dark:text-gray-300-->
<!--                 hover:text-brand-500 disabled:opacity-50"-->
<!--                    :disabled="extendedOnce"-->
<!--                    @click="extendTime"-->
<!--                >-->
<!--                  &nbsp;&nbsp;1회 연장&nbsp;&nbsp;-->
<!--                </button>-->
<!--              </div>-->
<!--            </div>-->
<!--          </template>-->

          <!-- (3번째 화면) 인증 완료 후: isVerified -->
<!--          <template>-->

            <!-- 전화번호 -->
            <div class="text-sm font-medium text-gray-700 dark:text-gray-300 mt-2">전화번호</div>
            <div class="col-start-2">
              <div class="flex gap-3">
                <input
                    v-model="phoneInput"
                    :disabled="savingPhone"
                    placeholder="010-1234-5678"
                    @input="formatPhoneInput"
                    @blur="validatePhoneInput"
                    class="h-11 w-full rounded-lg border px-3
                 bg-white text-gray-800 focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10
                 dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100"
                />
                <button
                    type="button"
                    class="h-11 shrink-0 rounded-lg bg-brand-500 px-4 text-sm font-medium text-white hover:bg-brand-600 disabled:opacity-50"
                    :disabled="savingPhone || phoneInput === phone"
                    @click="savePhone"
                >
                  {{ savingPhone ? '저장 중...' : '저장' }}
                </button>
              </div>
              <p v-if="phoneError" class="mt-1 text-sm text-error-500">{{ phoneError }}</p>
            </div>

            <!-- 구분선 -->
            <div class="col-span-2">
              <hr class="my-6 border-gray-200 dark:border-gray-700" />
            </div>

            <!-- 비밀번호 변경 -->
            <form @submit.prevent  class="contents">
              <!-- id-password의 안정적인 구조를 만들어 경고 제거 -->
              <input type="text" name="username" autocomplete="username" hidden />

              <div class="col-span-2">
                <div class="flex items-center justify-between">
                  <h3 class="mb-2 text-sm font-medium text-gray-700 dark:text-gray-300">비밀번호 변경</h3>
                  <div class="flex gap-2">
                    <button
                        v-if="!pwEditing"
                        type="button"
                        class="h-9 rounded-lg bg-gray-200 px-3 text-gray-800 hover:bg-gray-100 dark:bg-gray-700 dark:text-gray-100"
                        :disabled="changingPw"
                        @click="onPwEdit"
                    >수정</button>

                    <template v-else>
                      <button
                          type="button"
                          class="h-9 rounded-lg bg-gray-200 px-3 text-gray-800 hover:bg-gray-100 dark:bg-gray-700 dark:text-gray-100"
                          :disabled="changingPw"
                          @click="onPwCancel"
                      >취소</button>
                      <button
                          type="button"
                          class="h-9 rounded-lg bg-brand-500 px-3 text-white hover:bg-brand-600 disabled:opacity-50"
                          :disabled="changingPw"
                          @click="changePassword"
                      >{{ changingPw ? '변경 중...' : '변경' }}</button>
                    </template>
                  </div>
                </div>
              </div>

              <!-- 현재 비밀번호 -->
              <div class="text-sm font-medium text-gray-700 dark:text-gray-300">현재 비밀번호</div>
              <div class="col-start-2">
                <div class="relative">
                  <input
                      v-model="currentPassword"
                      :type="showCurrentPw ? 'text' : 'password'"
                      placeholder="현재 비밀번호"
                      :disabled="!pwEditing || changingPw"
                      autocomplete="current-password"
                      class="h-11 w-full rounded-lg border px-3
                         bg-white text-gray-800 focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10
                         dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100
                         disabled:bg-gray-50 disabled:text-gray-400 disabled:border-gray-200
                         dark:disabled:bg-gray-900/40 dark:disabled:text-gray-500 dark:disabled:border-gray-700"
                      @blur="validateCurrentPassword"
                  />
                  <span
                      @click="pwEditing && !changingPw ? toggleCurrentPwVisibility() : null"
                      :class="['absolute right-4 top-1/2 -translate-y-1/2',
                 (!pwEditing || changingPw) ? 'pointer-events-none opacity-50' : 'cursor-pointer']"
                  >
                    <EyeIcon v-if="showCurrentPw" class="w-5 h-5 text-gray-500 dark:text-gray-400" />
                    <EyeSlashIcon v-else class="w-5 h-5 text-gray-500 dark:text-gray-400" />
                  </span>
                </div>
                <p v-if="currentPwError" class="text-sm text-error-500">{{ currentPwError }}</p>
              </div>

              <!-- 새 비밀번호 -->
              <div class="text-sm font-medium text-gray-700 dark:text-gray-300">새 비밀번호</div>
              <div class="col-start-2">
                <div class="relative">
                  <input
                      v-model="newPassword"
                      :type="showNewPw ? 'text' : 'password'"
                      placeholder="새 비밀번호 (6자 이상)"
                      :disabled="!pwEditing || changingPw"
                      autocomplete="new-password"
                      class="h-11 w-full rounded-lg border px-3
                         bg-white text-gray-800 focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10
                         dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100
                         disabled:bg-gray-50 disabled:text-gray-400 disabled:border-gray-200
                         dark:disabled:bg-gray-900/40 dark:disabled:text-gray-500 dark:disabled:border-gray-700"
                      @blur="validateNewPassword"
                  />
                  <span
                      @click="pwEditing && !changingPw ? togglePasswordVisibility() : null"
                      :class="['absolute right-4 top-1/2 -translate-y-1/2',
                 (!pwEditing || changingPw) ? 'pointer-events-none opacity-50' : 'cursor-pointer']"
                  >
                    <EyeIcon v-if="showNewPw" class="w-5 h-5 text-gray-500 dark:text-gray-400" />
                    <EyeSlashIcon v-else class="w-5 h-5 text-gray-500 dark:text-gray-400" />
                  </span>
                </div>
                <p v-if="newPwError" class="text-sm text-error-500">{{ newPwError }}</p>
              </div>

              <!-- 새 비밀번호 확인 -->
              <div class="text-sm font-medium text-gray-700 dark:text-gray-300">비밀번호 확인</div>
              <div class="col-start-2">
                <div class="relative">
                  <input
                      v-model="newPassword2"
                      :type="showNewPw2 ? 'text' : 'password'"
                      placeholder="새 비밀번호 확인"
                      :disabled="!pwEditing || changingPw"
                      autocomplete="new-password"
                      class="h-11 w-full rounded-lg border px-3
                         bg-white text-gray-800 focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10
                         dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100
                         disabled:bg-gray-50 disabled:text-gray-400 disabled:border-gray-200
                         dark:disabled:bg-gray-900/40 dark:disabled:text-gray-500 dark:disabled:border-gray-700"
                      @blur="validateNewPasswordConfirm"
                  />
                  <span
                      @click="pwEditing && !changingPw ? togglePassword2Visibility() : null"
                      :class="['absolute right-4 top-1/2 -translate-y-1/2',
                 (!pwEditing || changingPw) ? 'pointer-events-none opacity-50' : 'cursor-pointer']"
                  >
                    <EyeIcon v-if="showNewPw2" class="w-5 h-5 text-gray-500 dark:text-gray-400" />
                    <EyeSlashIcon v-else class="w-5 h-5 text-gray-500 dark:text-gray-400" />
                  </span>
                </div>
                <p v-if="newPw2Error" class="text-sm text-error-500">{{ newPw2Error }}</p>
              </div>
            </form>

            <!-- SUPER 이메일 전용 -->
            <template v-if="isSuperEmail">

              <!-- ===== 접속 로그 다운로드 ===== -->
              <div class="col-span-2">
                <hr class="my-6 border-gray-200 dark:border-gray-700" />
              </div>

              <div class="col-span-2">
                <div class="flex items-center justify-between">
                  <h3 class="mb-2 text-sm font-medium text-gray-700 dark:text-gray-300">
                    접속 로그 다운로드 (기간별 Excel)
                  </h3>
                </div>
              </div>

              <!-- 날짜 선택 -->
              <div class="text-sm font-medium text-gray-700 dark:text-gray-300">기간</div>
              <div class="col-start-2">
                <div class="flex gap-2">
                  <!-- 날짜 필터 -->
                  <div class="inline-flex items-stretch whitespace-nowrap">
                    <!-- 시작일 -->
                    <input
                        type="text"
                        ref="logStartInput"
                        class="w-full h-11 border border-gray-200 dark:border-gray-700 rounded-l-lg px-3 py-2 text-sm text-center
                             focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10
                             dark:bg-gray-800 dark:text-gray-200"
                        placeholder="시작일"
                    />

                    <!-- 구분자 -->
                    <span
                        class="flex items-center justify-center w-20 h-11 border-t border-b border-gray-200 dark:border-gray-700
                              text-sm text-gray-500 dark:text-gray-400 bg-gray-50 dark:bg-gray-900">~</span>

                    <!-- 종료일 -->
                    <input
                        type="text"
                        ref="logEndInput"
                        class="w-full h-11 border border-gray-200 dark:border-gray-700 rounded-r-lg px-3 py-2 text-sm text-center
                           focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10
                           dark:bg-gray-800 dark:text-gray-200"
                        placeholder="종료일"
                    />
                  </div>
                  <button
                      type="button"
                      class="ml-auto h-11 shrink-0 rounded-lg bg-brand-500 px-4 text-sm font-medium text-white hover:bg-brand-600 disabled:opacity-50"
                      :disabled="downloading || !logFrom || !logTo"
                      @click="downloadLogs"
                  >
                    엑셀 다운로드
                  </button>
                </div>
                <p v-if="logRangeError" class="mt-1 text-sm text-error-500">{{ logRangeError }}</p>
              </div>

              <!-- ===== google 스프레드 시트 연동 ===== -->
              <div class="col-span-2">
                <hr class="my-6 border-gray-200 dark:border-gray-700" />
              </div>

              <!-- 제목 + 우측 버튼 -->
              <div class="col-span-2">
                <div class="flex items-center justify-between">
                  <h3 class="mb-2 text-sm font-medium text-gray-700 dark:text-gray-300">Google 스프레드시트 연동</h3>
                  <div class="flex gap-2">
                    <button
                        v-if="!sheetEditing"
                        type="button"
                        class="h-9 rounded-lg bg-gray-200 px-3 text-gray-800 hover:bg-gray-100 dark:bg-gray-700 dark:text-gray-100"
                        @click="sheetEditing = true"
                    >수정</button>
                    <template v-else>
                      <button
                          type="button"
                          class="h-9 rounded-lg bg-gray-200 px-3 text-gray-800 hover:bg-gray-100 dark:bg-gray-700 dark:text-gray-100"
                          :disabled="savingSheet"
                          @click="onSheetCancel"
                      >취소</button>
                      <button
                          type="button"
                          class="h-9 rounded-lg bg-brand-500 px-3 text-white hover:bg-brand-600 disabled:opacity-50"
                          :disabled="savingSheet"
                          @click="onSheetSave"
                      >{{ savingSheet ? '저장 중...' : '저장' }}</button>
                    </template>
                  </div>
                </div>
              </div>

              <!-- Spreadsheet ID -->
              <div class="text-sm font-medium text-gray-700 dark:text-gray-300">Spreadsheet ID</div>
              <div class="col-start-2">
                <input
                    v-model="sheetId"
                    :disabled="savingSheet || !sheetEditing"
                    placeholder="예) 1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms"
                    @blur="validateSheetId"
                    class="h-11 w-full rounded-lg border px-3 bg-white text-gray-800
                     focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10
                     dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100
                     disabled:bg-gray-50 disabled:text-gray-400 disabled:border-gray-200
                     dark:disabled:bg-gray-900/40 dark:disabled:text-gray-500 dark:disabled:border-gray-700"
                />
                <p v-if="sheetIdError" class="mt-1 text-sm text-error-500">{{ sheetIdError }}</p>
              </div>

              <!-- 시트 제목 -->
              <div class="text-sm font-medium text-gray-700 dark:text-gray-300">시트 제목</div>
              <div class="col-start-2">
                <input
                    v-model="sheetName"
                    :disabled="savingSheet || !sheetEditing"
                    placeholder="예) Sheet1"
                    @blur="validateSheetName"
                    class="h-11 w-full rounded-lg border px-3 bg-white text-gray-800
                     focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10
                     dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100
                     disabled:bg-gray-50 disabled:text-gray-400 disabled:border-gray-200
                     dark:disabled:bg-gray-900/40 dark:disabled:text-gray-500 dark:disabled:border-gray-700"
                />
                <p v-if="sheetNameError" class="mt-1 text-sm text-error-500">{{ sheetNameError }}</p>
              </div>

              <!-- 마지막 행 -->
              <div class="text-sm font-medium text-gray-700 dark:text-gray-300">마지막 행</div>
              <div class="col-start-2">
                <input
                    v-model.number="startRow"
                    :disabled="savingSheet || !sheetEditing"
                    type="number"
                    min="1"
                    step="1"
                    placeholder="예) 2"
                    @blur="validateStartRow"
                    class="h-11 w-full rounded-lg border px-3 bg-white text-gray-800
                     focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10
                     dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100
                     disabled:bg-gray-50 disabled:text-gray-400 disabled:border-gray-200
                     dark:disabled:bg-gray-900/40 dark:disabled:text-gray-500 dark:disabled:border-gray-700"
                />
                <p v-if="startRowError" class="mt-1 text-sm text-error-500">{{ startRowError }}</p>
              </div>

              <!-- ===== IP 화이트리스트 ===== -->
              <div class="col-span-2">
                <hr class="my-6 border-gray-200 dark:border-gray-700" />
              </div>

              <div class="col-span-2">
                <div class="flex items-center justify-between">
                  <h3 class="mb-2 text-sm font-medium text-gray-700 dark:text-gray-300">
                    IP 화이트리스트
                  </h3>
                  <div class="flex gap-2">
                    <button
                        v-if="!ipEditing"
                        type="button"
                        class="h-9 rounded-lg bg-gray-200 px-3 text-gray-800 hover:bg-gray-100
                             dark:bg-gray-700 dark:text-gray-100"
                        @click="ipEditing = true"
                    >
                      수정
                    </button>
                    <button
                        v-else
                        type="button"
                        class="h-9 rounded-lg bg-gray-200 px-3 text-gray-800 hover:bg-gray-100
                             dark:bg-gray-700 dark:text-gray-100"
                        @click="onIpCancel"
                    >
                      취소
                    </button>
                  </div>
                </div>
              </div>

              <!-- IP 검색 (편집 여부와 상관없이 항상 표시) -->
              <div class="text-sm font-medium text-gray-700 dark:text-gray-300 mt-4">
                검색
              </div>
              <div class="col-start-2">
                <div class="flex gap-2 w-full">
                  <!-- 입력창 + X 버튼 -->
                  <div class="relative flex-1">
                    <input
                        v-model="ipSearchInput"
                        type="text"
                        placeholder="IP, 메모, 수정일(YYYY-MM-DD) 검색"
                        @keyup.enter="applyIpSearch"
                        class="h-10 w-full rounded-lg border px-3 pr-8 bg-white text-gray-800
                            focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10
                            dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100
                            disabled:bg-gray-50 disabled:text-gray-400 disabled:border-gray-200
                            dark:disabled:bg-gray-900/40 dark:disabled:text-gray-500 dark:disabled:border-gray-700"
                    />
                    <!-- 값 있을 때는 항상 X 보이게 -->
                    <button
                        v-if="ipSearchInput"
                        type="button"
                        class="absolute inset-y-0 right-4 my-auto text-xs text-gray-400 hover:text-gray-600
                              dark:text-gray-500 dark:hover:text-gray-300"
                        @click="clearIpSearch"
                    >
                      ✕
                    </button>
                  </div>

                  <!-- 검색 버튼 (항상 파란색, 비활성 없음) -->
                  <button
                      type="button"
                      class="h-10 px-4 rounded-lg bg-brand-500 text-xs sm:text-sm font-medium text-white
                            hover:bg-brand-600 disabled:opacity-50 disabled:cursor-not-allowed"
                      @click="applyIpSearch"
                  >
                    검색
                  </button>
                </div>
              </div>

              <!-- 수정 모드에서만 새 IP 등록 폼 노출 (DOM 유지: v-show) -->
              <div class="contents" v-show="ipEditing">
                <div class="text-sm font-medium text-gray-700 dark:text-gray-300 mt-2">
                  새 IP 등록
                </div>
                <div class="col-start-2">
                  <div class="flex flex-col gap-2">
                    <div class="flex flex-col gap-2 xl:flex-row">
                      <input
                          v-model="newIp"
                          :disabled="ipSaving"
                          placeholder="예) 118.235.10.27"
                          class="h-11 w-full rounded-lg border px-3 bg-white text-gray-800
                               focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10
                               dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100"
                      />
                      <input
                          v-model="newIpMemo"
                          :disabled="ipSaving"
                          placeholder="메모 (예: 본사 팀장 PC)"
                          class="h-11 w-full rounded-lg border px-3 bg-white text-gray-800
                               focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10
                               dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100"
                      />
                      <button
                          type="button"
                          class="h-11 shrink-0 rounded-lg bg-brand-500 px-4 text-sm font-medium text-white
                               hover:bg-brand-600 disabled:opacity-50"
                          :disabled="ipSaving || !canAddIp"
                          @click="addIp"
                      >
                        {{ ipSaving ? '추가 중...' : '추가' }}
                      </button>
                    </div>
                    <p v-if="ipFormError" class="text-xs text-error-500">
                      {{ ipFormError }}
                    </p>
                  </div>
                </div>
              </div>

              <!-- 등록된 IP 목록 : 항상 표시 & 그리드 2열 전체 사용 -->
              <div class="col-span-2 mt-4">
                <div
                    class="ip-scroll rounded-md border border-gray-100 dark:border-gray-800
                         max-h-85 overflow-auto spin-dark"
                >
                  <!-- 로딩 -->
                  <div
                      v-if="ipLoading"
                      class="p-4 text-sm text-gray-500 dark:text-gray-400"
                  >
                    불러오는 중…
                  </div>

                  <!-- 비어있음 -->
                  <div
                      v-else-if="!ipList || ipList.length === 0"
                      class="p-4 text-sm text-gray-500 dark:text-gray-400"
                  >
                    등록된 IP가 없습니다.
                  </div>

                  <!-- 검색 결과 없음 -->
                  <div
                      v-else-if="filteredIpList.length === 0"
                      class="p-4 text-sm text-gray-500 dark:text-gray-400"
                  >
                    검색 결과가 없습니다.
                  </div>

                  <!-- 테이블 -->
                  <table
                      v-else
                      class="min-w-full table-fixed text-sm"
                  >
                    <thead
                        class="sticky top-0 z-10 bg-white dark:bg-gray-900
                             shadow-[0_1px_0_0_rgba(229,231,235,0.7)]
                             dark:shadow-[0_1px_0_0_rgba(31,41,55,0.8)]"
                    >
                    <tr
                        class="border-b border-gray-200 dark:border-gray-700
                               text-xs text-gray-500 dark:text-gray-400"
                    >
                      <th class="w-10 px-3 py-2 text-center whitespace-nowrap">번호</th>
                      <th class="w-40 px-3 py-2 text-left whitespace-nowrap">IP</th>
                      <th class="w-55 px-3 py-2 text-left">메모</th>
                      <th class="w-20 px-3 py-2 text-center whitespace-nowrap">상태</th>
                      <th class="w-20 px-3 py-2 text-right whitespace-nowrap">수정일</th>
                      <th class="w-20 px-3 py-2 text-center whitespace-nowrap">관리</th>
                    </tr>
                    </thead>
                    <tbody>
                    <tr
                        v-for="(row, idx) in filteredIpList"
                        :key="row.ipId ?? idx"
                        :class="[
                          'border-b border-gray-100 dark:border-gray-800',
                          idx % 2 === 0
                            ? 'bg-white dark:bg-gray-900'
                            : 'bg-gray-50 dark:bg-gray-800/60'
                        ]"
                    >
                      <!-- 행번호 -->
                      <td class="px-3 py-3 text-center text-xs text-gray-400 whitespace-nowrap">
                        {{ idx + 1 }}
                      </td>

                      <!-- IP / 메모 두 줄 (메모만 줄바꿈 허용) -->
                      <td class="px-3 py-3 align-middle">
                        <!-- 편집 모드 -->
                        <template v-if="editingRowId === row.ipId">
                          <input
                              v-model="editingRow.ipAddress"
                              class="mb-1 w-full rounded-md border border-gray-200 px-2 py-1 text-xs
                                     bg-white text-gray-800
                                     focus:border-brand-300 focus:outline-hidden focus:ring-2 focus:ring-brand-500/10
                                     dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100"
                              placeholder="IP 주소"
                          />
                        </template>

                        <!-- 표시 모드 -->
                        <template v-else>
                          <div
                              class="font-mono text-[13px] text-gray-900 dark:text-gray-400 truncate"
                          >
                            {{ row.ipAddress }}
                          </div>
                        </template>
                      </td>

                      <td class="px-3 py-3 align-middle">
                        <!-- 편집 모드 -->
                        <template v-if="editingRowId === row.ipId">
                          <input
                              v-model="editingRow.memo"
                              class="w-full rounded-md border border-gray-200 px-2 py-1 text-xs
                                     bg-white text-gray-800
                                     focus:border-brand-300 focus:outline-hidden focus:ring-2 focus:ring-brand-500/10
                                     dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100"
                              placeholder="메모"
                          />
                        </template>

                        <!-- 표시 모드 -->
                        <template v-else>
                          <div
                              class="mt-0.5 text-xs text-gray-400 dark:text-gray-500 break-all"
                          >
                            {{ row.memo || '-' }}
                          </div>
                        </template>
                      </td>

                      <!-- 상태 -->
                      <td class="px-3 py-3 text-center whitespace-nowrap">
                        <!-- 편집 모드 -->
                        <template v-if="editingRowId === row.ipId">
                          <select
                              v-model="editingRow.isActive"
                              class="h-8 rounded-md border border-gray-300 px-2 text-xs
                                     bg-white text-gray-800
                                     focus:border-brand-300 focus:outline-hidden focus:ring-2 focus:ring-brand-500/10
                                     dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100"
                          >
                            <option value="Y">사용</option>
                            <option value="N">중지</option>
                          </select>
                        </template>

                        <!-- 표시 모드 -->
                        <template v-else>
                            <span
                                class="inline-flex items-center justify-center rounded-full px-3 py-1 text-[11px] font-medium"
                                :class="row.isActive === 'Y'
                                ? 'bg-[#D1F2D6] text-[#2F855A] dark:bg-[#1B3A2E] dark:text-[#5FA97A]'
                                : 'bg-[#E5E7EB] text-[#4B5563] dark:bg-[#374151] dark:text-[#D1D5DB]'"
                            >
                              {{ row.isActive === 'Y' ? '사용' : '중지' }}
                            </span>
                        </template>
                      </td>

                      <!-- 수정일: 날짜 / 시간 두 줄 -->
                      <td
                          class="px-3 py-3 text-right text-xs text-gray-500 dark:text-gray-400 whitespace-nowrap"
                      >
                        <template v-if="row.updatedAt || row.createdAt">
                            <span class="block">
                              {{ splitDateTime(row.updatedAt || row.createdAt)[0] }}
                            </span>
                          <span class="block">
                              {{ splitDateTime(row.updatedAt || row.createdAt)[1] }}
                            </span>
                        </template>
                        <span v-else>-</span>
                      </td>

                      <!-- 관리: 수정 / 저장·취소 / 삭제 -->
                      <td class="px-3 py-3 text-right whitespace-nowrap">
                        <template v-if="editingRowId === row.ipId">
                          <button
                              type="button"
                              class="mr-1 px-2 py-1 text-xs rounded-md border border-brand-500
                                     bg-brand-500 text-white hover:bg-brand-600
                                     disabled:opacity-50"
                              :disabled="ipSaving"
                              @click="saveIpRow(row)"
                          >
                            저장
                          </button>
                          <button
                              type="button"
                              class="px-2 py-1 text-xs rounded-md border border-gray-200 dark:border-gray-700
                                     bg-white text-gray-700 hover:bg-gray-50
                                     dark:bg-transparent dark:text-gray-200 dark:hover:bg-gray-700
                                     disabled:opacity-50"
                              :disabled="ipSaving"
                              @click="cancelIpRowEdit"
                          >
                            취소
                          </button>
                        </template>

                        <template v-else>
                          <button
                              type="button"
                              class="px-2 py-1 text-xs rounded-md border border-gray-200 dark:border-gray-700
                                     bg-white text-gray-700 hover:bg-gray-50
                                     dark:bg-transparent dark:text-gray-200 dark:hover:bg-gray-700
                                     disabled:opacity-50"
                              :disabled="!ipEditing || ipSaving"
                              @click="startIpRowEdit(row)"
                          >
                            수정
                          </button>
                          <button
                              type="button"
                              class="ml-1 px-2 py-1 text-xs rounded-md border border-red-200 dark:border-red-700
                                     bg-white text-red-600 hover:bg-red-50
                                     dark:bg-transparent dark:text-red-400 dark:hover:bg-red-900/40
                                     disabled:opacity-50"
                              :disabled="!ipEditing || ipSaving"
                              @click="hardDeleteIpRow(row)"
                          >
                            삭제
                          </button>
                        </template>
                      </td>
                    </tr>
                    </tbody>
                  </table>
                </div>
              </div>

              <!-- ===== 지점 / 팀 관리 ===== -->
              <div class="col-span-2">
                <hr class="my-6 border-gray-200 dark:border-gray-700" />
              </div>

              <!-- 지점 관리 -->
              <div class="col-span-2">
                <div class="flex items-center justify-between">
                  <h3 class="mb-2 text-sm font-medium text-gray-700 dark:text-gray-300">
                    지점 관리
                  </h3>
                  <div class="flex gap-2">
                    <button
                        v-if="!branchesEditing"
                        type="button"
                        class="h-9 rounded-lg bg-gray-200 px-3 text-gray-800 hover:bg-gray-100 dark:bg-gray-700 dark:text-gray-100"
                        @click="branchesEditing = true"
                    >
                      수정
                    </button>
                    <button
                        v-else
                        type="button"
                        class="h-9 rounded-lg bg-gray-200 px-3 text-gray-800 hover:bg-gray-100 dark:bg-gray-700 dark:text-gray-100"
                        @click="onBranchesCancel"
                    >
                      취소
                    </button>
                  </div>
                </div>
              </div>

              <!-- 지점 검색 -->
              <div class="text-sm font-medium text-gray-700 dark:text-gray-300 mt-2">
                검색
              </div>
              <div class="col-start-2">
                <div class="flex gap-2 w-full">
                  <div class="relative flex-1">
                    <input
                        v-model="branchSearchInput"
                        type="text"
                        placeholder="지점명 검색"
                        @keyup.enter="applyBranchSearch"
                        class="h-10 w-full rounded-lg border px-3 pr-8 bg-white text-gray-800
                             focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10
                             dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100"
                    />
                    <button
                        v-if="branchSearchInput"
                        type="button"
                        class="absolute inset-y-0 right-4 my-auto text-xs text-gray-400 hover:text-gray-600
                            dark:text-gray-500 dark:hover:text-gray-300"
                        @click="clearBranchSearch"
                    >
                      ✕
                    </button>
                  </div>
                  <button
                      type="button"
                      class="h-10 px-4 rounded-lg bg-brand-500 text-xs sm:text-sm font-medium text-white
                          hover:bg-brand-600 disabled:opacity-50"
                      @click="applyBranchSearch"
                  >
                    검색
                  </button>
                </div>
              </div>

              <!-- 신규 지점 -->
              <div v-if="branchesEditing" class="text-sm font-medium text-gray-700 dark:text-gray-300 mt-2">
                신규 지점
              </div>
              <div v-if="branchesEditing" class="col-start-2">
                <div class="flex gap-2">
                  <input
                      v-model="newBranchName"
                      placeholder="예) 본사 지점"
                      class="h-11 w-full rounded-lg border px-3 bg-white text-gray-800
                             focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10
                             dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100
                             disabled:bg-gray-50 disabled:text-gray-400 disabled:border-gray-200
                             dark:disabled:bg-gray-900/40 dark:disabled:text-gray-500 dark:disabled:border-gray-700"
                  />
                  <button
                      type="button"
                      class="h-11 shrink-0 rounded-lg bg-brand-500 px-4 text-sm font-medium text-white hover:bg-brand-600 disabled:opacity-50"
                      :disabled="!branchesEditing || !canAddBranch"
                      @click="addBranch"
                  >
                    추가
                  </button>
                </div>
                <p v-if="branchFormError" class="text-xs text-error-500">
                  {{ branchFormError }}
                </p>
              </div>

              <!-- 지점 목록 -->
              <div class="col-span-2 mt-4">
                <div
                    class="ip-scroll rounded-md border border-gray-100 dark:border-gray-800
                           max-h-60 overflow-auto spin-dark"
                >
                  <div v-if="branchesLoading" class="p-4 text-sm text-gray-500 dark:text-gray-400">
                    불러오는 중…
                  </div>
                  <div v-else-if="!branches || branches.length === 0" class="p-4 text-sm text-gray-500 dark:text-gray-400">
                    등록된 지점이 없습니다.
                  </div>
                  <div v-else-if="filteredBranches.length === 0" class="p-4 text-sm text-gray-500 dark:text-gray-400">
                    검색 결과가 없습니다.
                  </div>
                  <table
                      v-else
                      class="min-w-full table-fixed text-sm"
                  >
                    <thead
                        class="sticky top-0 z-10 bg-white dark:bg-gray-900
                               shadow-[0_1px_0_0_rgba(229,231,235,0.7)]
                               dark:shadow-[0_1px_0_0_rgba(31,41,55,0.8)]"
                    >
                    <tr
                        class="border-b border-gray-200 dark:border-gray-700
                               text-xs text-gray-500 dark:text-gray-400"
                    >
                      <th class="w-10 px-3 py-2 text-center whitespace-nowrap">번호</th>
                      <th class="px-3 py-2 text-left whitespace-nowrap">지점명</th>
                      <th class="w-24 px-3 py-2 text-center whitespace-nowrap">관리</th>
                    </tr>
                    </thead>
                    <tbody>
                    <tr
                        v-for="(b, idx) in filteredBranches"
                        :key="b.branchId ?? idx"
                        :class="[
                          'border-b border-gray-100 dark:border-gray-800',
                          idx % 2 === 0
                            ? 'bg-white dark:bg-gray-900'
                            : 'bg-gray-50 dark:bg-gray-800/60'
                        ]"
                    >
                      <td class="px-3 py-3 text-center text-xs text-gray-400 whitespace-nowrap">
                        {{ idx + 1 }}
                      </td>
                      <td class="px-3 py-3">
                        <template v-if="editingBranchId === b.branchId">
                          <input
                              v-model="editingBranchName"
                              class="w-full rounded-md border border-gray-200 px-2 py-1 text-xs
                                     bg-white text-gray-800
                                     focus:border-brand-300 focus:outline-hidden focus:ring-2 focus:ring-brand-500/10
                                     dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100"
                              placeholder="지점명"
                          />
                        </template>
                        <template v-else>
                          <span class="text-sm text-gray-800 dark:text-gray-200">
                            {{ b.branchName }}
                          </span>
                        </template>
                      </td>
                      <td class="px-3 py-3 text-center whitespace-nowrap">
                        <template v-if="editingBranchId === b.branchId">
                          <button
                              type="button"
                              class="mr-1 px-2 py-1 text-xs rounded-md border border-brand-500
                                     bg-brand-500 text-white hover:bg-brand-600 disabled:opacity-50"
                              @click="saveBranchRow(b)"
                          >
                            저장
                          </button>
                          <button
                              type="button"
                              class="px-2 py-1 text-xs rounded-md border border-gray-200 dark:border-gray-700
                                     bg-white text-gray-700 hover:bg-gray-50
                                     dark:bg-transparent dark:text-gray-200 dark:hover:bg-gray-700"
                              @click="cancelBranchEdit"
                          >
                            취소
                          </button>
                        </template>
                        <template v-else>
                          <button
                              type="button"
                              class="px-2 py-1 text-xs rounded-md border border-gray-200 dark:border-gray-700
                                     bg-white text-gray-700 hover:bg-gray-50
                                     dark:bg-transparent dark:text-gray-200 dark:hover:bg-gray-700
                                     disabled:opacity-50"
                              :disabled="!branchesEditing"
                              @click="startBranchEdit(b)"
                          >
                            수정
                          </button>
                          <button
                              type="button"
                              class="ml-1 px-2 py-1 text-xs rounded-md border border-red-200 dark:border-red-700
                                     bg-white text-red-600 hover:bg-red-50
                                     dark:bg-transparent dark:text-red-400 dark:hover:bg-red-900/40
                                     disabled:opacity-50"
                              :disabled="!branchesEditing"
                              @click="deleteBranch(b.branchId)"
                          >
                            삭제
                          </button>
                        </template>
                      </td>
                    </tr>
                    </tbody>
                  </table>
                </div>
              </div>

              <!-- 구분선 -->
              <div class="col-span-2">
                <hr class="my-6 border-gray-200 dark:border-gray-700" />
              </div>

              <!-- 팀 관리 -->
              <div class="col-span-2">
                <div class="flex items-center justify-between">
                  <h3 class="mb-2 text-sm font-medium text-gray-700 dark:text-gray-300">팀 관리</h3>
                  <div class="flex gap-2">
                    <button
                        v-if="!centersEditing"
                        type="button"
                        class="h-9 rounded-lg bg-gray-200 px-3 text-gray-800 hover:bg-gray-100 dark:bg-gray-700 dark:text-gray-100"
                        @click="centersEditing = true"
                    >
                      수정
                    </button>
                    <button
                        v-else
                        type="button"
                        class="h-9 rounded-lg bg-gray-200 px-3 text-gray-800 hover:bg-gray-100 dark:bg-gray-700 dark:text-gray-100"
                        @click="onCentersCancel"
                    >
                      취소
                    </button>
                  </div>
                </div>
              </div>

              <!-- 팀 검색 -->
              <div class="text-sm font-medium text-gray-700 dark:text-gray-300 mt-2">
                검색
              </div>
              <div class="col-start-2">
                <div class="flex gap-2 w-full">
                  <div class="relative flex-1">
                    <input
                        v-model="centerSearchInput"
                        type="text"
                        placeholder="팀명 / 지점명 검색"
                        @keyup.enter="applyCenterSearch"
                        class="h-10 w-full rounded-lg border px-3 pr-8 bg-white text-gray-800
                             focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10
                             dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100"
                    />
                    <button
                        v-if="centerSearchInput"
                        type="button"
                        class="absolute inset-y-0 right-4 my-auto text-xs text-gray-400 hover:text-gray-600
                            dark:text-gray-500 dark:hover:text-gray-300"
                        @click="clearCenterSearch"
                    >
                      ✕
                    </button>
                  </div>
                  <button
                      type="button"
                      class="h-10 px-4 rounded-lg bg-brand-500 text-xs sm:text-sm font-medium text-white
                          hover:bg-brand-600 disabled:opacity-50"
                      @click="applyCenterSearch"
                  >
                    검색
                  </button>
                </div>
              </div>

              <!-- 신규 팀 -->
              <div v-if="centersEditing" class="text-sm font-medium text-gray-700 dark:text-gray-300 mt-2">
                신규 팀
              </div>
              <div v-if="centersEditing" class="col-start-2">
                <div class="flex flex-col gap-2 md:flex-row">
                  <input
                      v-model="newCenterName"
                      placeholder="예) 강남1팀"
                      class="h-11 w-full rounded-lg border px-3 bg-white text-gray-800
                             focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10
                             dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100
                             disabled:bg-gray-50 disabled:text-gray-400 disabled:border-gray-200
                             dark:disabled:bg-gray-900/40 dark:disabled:text-gray-500 dark:disabled:border-gray-700"
                  />

                  <select
                      v-model="newCenterBranchId"
                      class="h-11 w-full md:w-48 rounded-lg border px-3 bg-white text-gray-800
                             focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10
                             dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100
                             disabled:bg-gray-50 disabled:text-gray-400 disabled:border-gray-200
                             dark:disabled:bg-gray-900/40 dark:disabled:text-gray-500 dark:disabled:border-gray-700"
                  >
                    <option value="">지점 지정 안 함</option>
                    <option
                        v-for="b in branches"
                        :key="b.branchId"
                        :value="b.branchId"
                    >
                      {{ b.branchName }}
                    </option>
                  </select>

                  <!-- 컬러피커 + 스와치 10개 -->
                  <div class="flex items-center gap-2">
                    <div class="relative flex items-center gap-2">
                      <!-- 트리거: 현재 색 -->
                      <button
                          type="button"
                          class="h-11 w-14 rounded-lg border border-gray-200 dark:border-gray-700 p-1 bg-white dark:bg-gray-800"
                          :style="{ backgroundColor: newCenterColor }"
                          @click="toggleColorPopover"
                          ref="colorBtnRef"
                      />

                      <Teleport to="body">
                        <div v-if="colorOpen" class="fixed inset-0 z-[3000]" @mousedown="onBackdropDown">
                          <!-- 실제 팝오버 -->
                          <div
                              ref="colorPopoverRef"
                              class="absolute w-80 rounded-xl border border-gray-200 dark:border-gray-700
                                      bg-white dark:bg-gray-900 p-3 shadow-lg"
                              :style="colorPopoverStyle"
                              @mousedown.stop
                          >
                            <!-- 헤더 -->
                            <div class="flex items-center justify-end mb-2">
                            </div>

                            <!-- 컬러 선택 + 미리보기 -->
                            <div class="flex gap-3 items-start">
                              <!-- 왼쪽: 컬러 피커 -->
                              <input
                                  type="color"
                                  v-model="colorDraft"
                                  class="h-10 w-27 rounded-md border p-1
                                         border-gray-200 dark:border-gray-700
                                         bg-white dark:bg-gray-800"
                              />

                              <!-- 오른쪽: 미리보기 배지 -->
                              <div class="flex-1 flex items-center justify-center gap-2">
                                <span
                                    class="inline-flex items-center h-10 rounded-full border px-4 text-sm font-medium"
                                    :style="centerBadgeStyle(colorDraft)"
                                >
                                  미리보기
                                </span>

                                <div
                                    class="inline-flex items-center justify-center h-10 px-3 rounded-lg border
                                           text-xs tabular-nums text-gray-600 dark:text-gray-300
                                           border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800"
                                >
                                  {{ colorDraft }}
                                </div>
                              </div>
                            </div>

                            <!-- 프리셋 -->
                            <div class="ml-2 mt-4 grid grid-cols-5 gap-1">
                              <button
                                  v-for="c in CENTER_COLOR_PRESETS"
                                  :key="c"
                                  type="button"
                                  class="h-8 w-8 rounded-md ring-1 ring-black/10 dark:ring-white/10"
                                  :class="colorDraft === c ? 'outline outline-2 outline-brand-500' : ''"
                                  :style="{ backgroundColor: c }"
                                  @click="colorDraft = c"
                              />
                            </div>

                            <!-- 하단 버튼 -->
                            <div class="mt-4 flex justify-end gap-2">
                              <button
                                  type="button"
                                  class="h-9 px-4 rounded-lg text-sm
                                       bg-gray-200 text-gray-800 hover:bg-gray-300
                                       dark:bg-gray-700 dark:text-gray-100"
                                  @click="cancelColor"
                              >
                                취소
                              </button>
                              <button
                                  type="button"
                                  class="h-9 px-4 rounded-lg text-sm text-white
                                          bg-brand-500 hover:bg-brand-600"
                                  @click="confirmColor"
                              >
                                확인
                              </button>
                            </div>
                          </div>
                        </div>
                      </Teleport>
                    </div>
                  </div>

                  <button
                      type="button"
                      class="h-11 shrink-0 rounded-lg bg-brand-500 px-4 text-sm font-medium text-white hover:bg-brand-600 disabled:opacity-50"
                      :disabled="!centersEditing || !canAddCenter"
                      @click="addCenter"
                  >
                    추가
                  </button>
                </div>

                <p v-if="centerFormError" class="text-xs text-error-500">
                  {{ centerFormError }}
                </p>
              </div>

              <!-- 팀 목록 -->
              <div class="col-span-2 mt-4">
                <div
                    class="ip-scroll rounded-md border border-gray-100 dark:border-gray-800
                           max-h-60 overflow-auto spin-dark"
                >
                  <div v-if="centersLoading" class="p-4 text-sm text-gray-500 dark:text-gray-400">
                    불러오는 중…
                  </div>
                  <div v-else-if="!centers || centers.length === 0" class="p-4 text-sm text-gray-500 dark:text-gray-400">
                    등록된 팀이 없습니다.
                  </div>
                  <div v-else-if="filteredCenters.length === 0" class="p-4 text-sm text-gray-500 dark:text-gray-400">
                    검색 결과가 없습니다.
                  </div>
                  <table
                      v-else
                      class="min-w-full table-fixed text-sm"
                  >
                    <thead
                        class="sticky top-0 z-10 bg-white dark:bg-gray-900
                               shadow-[0_1px_0_0_rgba(229,231,235,0.7)]
                               dark:shadow-[0_1px_0_0_rgba(31,41,55,0.8)]"
                    >
                    <tr
                        class="border-b border-gray-200 dark:border-gray-700
                               text-xs text-gray-500 dark:text-gray-400"
                    >
                      <th class="w-10 px-3 py-2 text-center whitespace-nowrap">번호</th>
                      <th class="w-28 px-3 py-2 text-left whitespace-nowrap">색상</th>
                      <th class="px-3 py-2 text-left whitespace-nowrap">팀명</th>
                      <th class="w-40 px-3 py-2 text-left whitespace-nowrap">지점</th>
                      <th class="w-24 px-3 py-2 text-center whitespace-nowrap">관리</th>
                    </tr>
                    </thead>
                    <tbody>
                    <tr
                        v-for="(c, idx) in filteredCenters"
                        :key="c.centerId ?? idx"
                        :class="[
                          'border-b border-gray-100 dark:border-gray-800',
                          idx % 2 === 0
                            ? 'bg-white dark:bg-gray-900'
                            : 'bg-gray-50 dark:bg-gray-800/60'
                        ]"
                    >
                      <td class="px-3 py-3 text-center text-xs text-gray-400 whitespace-nowrap">
                        {{ idx + 1 }}
                      </td>

                      <td class="px-3 py-3">
                        <template v-if="editingCenterId === c.centerId">
                          <div class="flex items-center gap-2">
                            <!-- 트리거: 현재 색(버튼) -->
                            <button
                                type="button"
                                class="h-11 w-14 rounded-lg border border-gray-200 dark:border-gray-700 p-1 bg-white dark:bg-gray-800"
                                :style="{ backgroundColor: (editingCenter.centerColor || '#4B5563') }"
                                @click.stop="toggleColorPopover($event, 'EDIT')"
                            />

                            <!-- HEX 표시 -->
                            <div
                                class="inline-flex items-center justify-center h-10 px-3 rounded-lg border
                                       text-xs tabular-nums text-gray-600 dark:text-gray-300
                                       border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800"
                            >
                              {{ editingCenter.centerColor || '미지정' }}
                            </div>
                          </div>
                        </template>
                        <template v-else>
                          <span
                              class="inline-flex items-center rounded-full border px-3 py-1 text-xs"
                              :style="centerBadgeStyle(c.centerColor)"
                          >
                            {{ c.centerColor || '미지정' }}
                          </span>
                        </template>
                      </td>

                      <!-- 팀명 -->
                      <td class="px-3 py-3">
                        <template v-if="editingCenterId === c.centerId">
                          <input
                              v-model="editingCenter.centerName"
                              class="w-full rounded-md border border-gray-200 px-2 py-1 text-xs
                                     bg-white text-gray-800
                                     focus:border-brand-300 focus:outline-hidden focus:ring-2 focus:ring-brand-500/10
                                     dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100"
                              placeholder="팀명"
                          />
                        </template>
                        <template v-else>
                          <span class="text-sm text-gray-800 dark:text-gray-200">
                            {{ c.centerName }}
                          </span>
                        </template>
                      </td>

                      <!-- 지점 -->
                      <td class="px-3 py-3">
                        <template v-if="editingCenterId === c.centerId">
                          <select
                              v-model="editingCenter.branchId"
                              class="h-9 w-full rounded-md border border-gray-300 px-2 text-xs
                                     bg-white text-gray-800
                                     focus:border-brand-300 focus:outline-hidden focus:ring-2 focus:ring-brand-500/10
                                     dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100"
                          >
                            <option :value="null">지점 지정 안 함</option>
                            <option
                                v-for="b in branches"
                                :key="b.branchId"
                                :value="b.branchId"
                            >
                              {{ b.branchName }}
                            </option>
                          </select>
                        </template>
                        <template v-else>
                          <span class="text-xs text-gray-600 dark:text-gray-300">
                            {{ getBranchName(c.branchId) }}
                          </span>
                        </template>
                      </td>

                      <!-- 관리 -->
                      <td class="px-3 py-3 text-center whitespace-nowrap">
                        <template v-if="editingCenterId === c.centerId">
                          <button
                              type="button"
                              class="mr-1 px-2 py-1 text-xs rounded-md border border-brand-500
                                     bg-brand-500 text-white hover:bg-brand-600 disabled:opacity-50"
                              @click="saveCenterRow(c)"
                          >
                            저장
                          </button>
                          <button
                              type="button"
                              class="px-2 py-1 text-xs rounded-md border border-gray-200 dark:border-gray-700
                                     bg-white text-gray-700 hover:bg-gray-50
                                     dark:bg-transparent dark:text-gray-200 dark:hover:bg-gray-700"
                              @click="cancelCenterEdit"
                          >
                            취소
                          </button>
                        </template>
                        <template v-else>
                          <button
                              type="button"
                              class="px-2 py-1 text-xs rounded-md border border-gray-200 dark:border-gray-700
                                     bg-white text-gray-700 hover:bg-gray-50
                                     dark:bg-transparent dark:text-gray-200 dark:hover:bg-gray-700
                                     disabled:opacity-50"
                              :disabled="!centersEditing"
                              @click="startCenterEdit(c)"
                          >
                            수정
                          </button>
                          <button
                              type="button"
                              class="ml-1 px-2 py-1 text-xs rounded-md border border-red-200 dark:border-red-700
                                     bg-white text-red-600 hover:bg-red-50
                                     dark:bg-transparent dark:text-red-400 dark:hover:bg-red-900/40
                                     disabled:opacity-50"
                              :disabled="!centersEditing"
                              @click="deleteCenter(c.centerId)"
                          >
                            삭제
                          </button>
                        </template>
                      </td>
                    </tr>
                    </tbody>
                  </table>
                </div>
              </div>

              <!-- ===== 전문가 관리 ===== -->
              <div class="col-span-2">
                <hr class="my-6 border-gray-200 dark:border-gray-700" />
              </div>

              <div class="col-span-2">
                <div class="flex items-center justify-between">
                  <h3 class="mb-2 text-sm font-medium text-gray-700 dark:text-gray-300">
                    전문가 관리
                  </h3>
                  <div class="flex gap-2">
                    <button
                        v-if="!expertsEditing"
                        type="button"
                        class="h-9 rounded-lg bg-gray-200 px-3 text-gray-800 hover:bg-gray-100 dark:bg-gray-700 dark:text-gray-100"
                        @click="expertsEditing = true"
                    >
                      수정
                    </button>
                    <button
                        v-else
                        type="button"
                        class="h-9 rounded-lg bg-gray-200 px-3 text-gray-800 hover:bg-gray-100 dark:bg-gray-700 dark:text-gray-100"
                        @click="onExpertsCancel"
                    >
                      취소
                    </button>
                  </div>
                </div>
              </div>

              <!-- 전문가 검색 -->
              <div class="text-sm font-medium text-gray-700 dark:text-gray-300 mt-2">
                검색
              </div>
              <div class="col-start-2">
                <div class="flex gap-2 w-full">
                  <div class="relative flex-1">
                    <input
                        v-model="expertSearchInput"
                        type="text"
                        placeholder="전문가명 검색"
                        @keyup.enter="applyExpertSearch"
                        class="h-10 w-full rounded-lg border px-3 pr-8 bg-white text-gray-800
                             focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10
                             dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100"
                    />
                    <button
                        v-if="expertSearchInput"
                        type="button"
                        class="absolute inset-y-0 right-4 my-auto text-xs text-gray-400 hover:text-gray-600
                             dark:text-gray-500 dark:hover:text-gray-300"
                        @click="clearExpertSearch"
                    >
                      ✕
                    </button>
                  </div>
                  <button
                      type="button"
                      class="h-10 px-4 rounded-lg bg-brand-500 text-xs sm:text-sm font-medium text-white
                           hover:bg-brand-600 disabled:opacity-50"
                      @click="applyExpertSearch"
                  >
                    검색
                  </button>
                </div>
              </div>

              <!-- 신규 전문가 -->
              <div v-if="expertsEditing" class="text-sm font-medium text-gray-700 dark:text-gray-300 mt-2">
                신규 전문가
              </div>
              <div v-if="expertsEditing" class="col-start-2">
                <div class="flex gap-2">
                  <input
                      v-model="newExpertName"
                      placeholder="예) 리드남"
                      class="h-11 w-full rounded-lg border px-3 bg-white text-gray-800
                           focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10
                           dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100"
                  />
                  <button
                      type="button"
                      class="h-11 shrink-0 rounded-lg bg-brand-500 px-4 text-sm font-medium text-white hover:bg-brand-600 disabled:opacity-50"
                      :disabled="!expertsEditing || !canAddExpert"
                      @click="addExpert"
                  >
                    추가
                  </button>
                </div>
                <p v-if="expertFormError" class="text-xs text-error-500">
                  {{ expertFormError }}
                </p>
              </div>

              <!-- 전문가 목록 -->
              <div class="col-span-2 mt-4">
                <div
                    class="ip-scroll rounded-md border border-gray-100 dark:border-gray-800
                         max-h-60 overflow-auto spin-dark"
                >
                  <div v-if="expertsLoading" class="p-4 text-sm text-gray-500 dark:text-gray-400">
                    불러오는 중…
                  </div>
                  <div v-else-if="!experts || experts.length === 0" class="p-4 text-sm text-gray-500 dark:text-gray-400">
                    등록된 전문가가 없습니다.
                  </div>
                  <div v-else-if="filteredExperts.length === 0" class="p-4 text-sm text-gray-500 dark:text-gray-400">
                    검색 결과가 없습니다.
                  </div>

                  <table v-else class="min-w-full table-fixed text-sm">
                    <thead
                        class="sticky top-0 z-10 bg-white dark:bg-gray-900
                             shadow-[0_1px_0_0_rgba(229,231,235,0.7)]
                             dark:shadow-[0_1px_0_0_rgba(31,41,55,0.8)]"
                    >
                    <tr
                        class="border-b border-gray-200 dark:border-gray-700
                               text-xs text-gray-500 dark:text-gray-400"
                    >
                      <th class="w-10 px-3 py-2 text-center whitespace-nowrap">번호</th>
                      <th class="px-3 py-2 text-left whitespace-nowrap">전문가명</th>
                      <th class="w-28 px-3 py-2 text-right whitespace-nowrap">등록일</th>
                      <th class="w-28 px-3 py-2 text-right whitespace-nowrap">수정일</th>
                      <th class="w-24 px-3 py-2 text-center whitespace-nowrap">관리</th>
                    </tr>
                    </thead>
                    <tbody>
                    <tr
                        v-for="(e, idx) in filteredExperts"
                        :key="e.expertId ?? idx"
                        :class="[
                          'border-b border-gray-100 dark:border-gray-800',
                          idx % 2 === 0
                            ? 'bg-white dark:bg-gray-900'
                            : 'bg-gray-50 dark:bg-gray-800/60'
                        ]"
                    >
                      <!-- 번호 -->
                      <td class="px-3 py-3 text-center text-xs text-gray-400 whitespace-nowrap">
                        {{ idx + 1 }}
                      </td>

                      <!-- 전문가명 -->
                      <td class="px-3 py-3">
                        <template v-if="editingExpertId === e.expertId">
                          <input
                              v-model="editingExpertName"
                              class="w-full rounded-md border border-gray-200 px-2 py-1 text-xs
                                     bg-white text-gray-800
                                     focus:border-brand-300 focus:outline-hidden focus:ring-2 focus:ring-brand-500/10
                                     dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100"
                              placeholder="전문가명"
                          />
                        </template>
                        <template v-else>
                            <span class="text-sm text-gray-800 dark:text-gray-200">
                              {{ e.expertName }}
                            </span>
                        </template>
                      </td>

                      <!-- 등록일 -->
                      <td class="px-3 py-3 text-right text-xs text-gray-500 dark:text-gray-400 whitespace-nowrap">
                        <template v-if="e.createdAt">
                            <span class="block">
                              {{ splitDateTime(e.createdAt)[0] }}
                            </span>
                          <span class="block">
                              {{ splitDateTime(e.createdAt)[1] }}
                            </span>
                        </template>
                        <span v-else>-</span>
                      </td>

                      <!-- 수정일 -->
                      <td class="px-3 py-3 text-right text-xs text-gray-500 dark:text-gray-400 whitespace-nowrap">
                        <template v-if="e.updatedAt || e.createdAt">
                            <span class="block">
                              {{ splitDateTime(e.updatedAt || e.createdAt)[0] }}
                            </span>
                          <span class="block">
                              {{ splitDateTime(e.updatedAt || e.createdAt)[1] }}
                            </span>
                        </template>
                        <span v-else>-</span>
                      </td>

                      <!-- 관리 -->
                      <td class="px-3 py-3 text-center whitespace-nowrap">
                        <template v-if="editingExpertId === e.expertId">
                          <button
                              type="button"
                              class="mr-1 px-2 py-1 text-xs rounded-md border border-brand-500
                                     bg-brand-500 text-white hover:bg-brand-600 disabled:opacity-50"
                              @click="saveExpertRow(e)"
                          >
                            저장
                          </button>
                          <button
                              type="button"
                              class="px-2 py-1 text-xs rounded-md border border-gray-200 dark:border-gray-700
                                     bg-white text-gray-700 hover:bg-gray-50
                                     dark:bg-transparent dark:text-gray-200 dark:hover:bg-gray-700"
                              @click="cancelExpertEdit"
                          >
                            취소
                          </button>
                        </template>
                        <template v-else>
                          <button
                              type="button"
                              class="px-2 py-1 text-xs rounded-md border border-gray-200 dark:border-gray-700
                                     bg-white text-gray-700 hover:bg-gray-50
                                     dark:bg-transparent dark:text-gray-200 dark:hover:bg-gray-700
                                     disabled:opacity-50"
                              :disabled="!expertsEditing"
                              @click="startExpertEdit(e)"
                          >
                            수정
                          </button>
                          <button
                              type="button"
                              class="ml-1 px-2 py-1 text-xs rounded-md border border-red-200 dark:border-red-700
                                     bg-white text-red-600 hover:bg-red-50
                                     dark:bg-transparent dark:text-red-400 dark:hover:bg-red-900/40
                                     disabled:opacity-50"
                              :disabled="!expertsEditing"
                              @click="deleteExpert(e.expertId)"
                          >
                            삭제
                          </button>
                        </template>
                      </td>
                    </tr>
                    </tbody>
                  </table>
                </div>
              </div>

              <!-- ===== 회의실 관리 ===== -->
              <div class="col-span-2">
                <hr class="my-6 border-gray-200 dark:border-gray-700" />
              </div>

              <div class="col-span-2">
                <div class="flex items-center justify-between">
                  <h3 class="mb-2 text-sm font-medium text-gray-700 dark:text-gray-300">
                    회의실 관리
                  </h3>
                  <div class="flex gap-2">
                    <button
                        v-if="!roomsEditing"
                        type="button"
                        class="h-9 rounded-lg bg-gray-200 px-3 text-gray-800 hover:bg-gray-100 dark:bg-gray-700 dark:text-gray-100"
                        @click="roomsEditing = true"
                    >수정</button>

                    <button
                        v-else
                        type="button"
                        class="h-9 rounded-lg bg-gray-200 px-3 text-gray-800 hover:bg-gray-100 dark:bg-gray-700 dark:text-gray-100"
                        @click="roomsEditing = false; cancelRoomEdit(); newRoomName=''; roomFormError='';"
                    >취소</button>
                  </div>
                </div>
              </div>

              <!-- 검색 -->
              <div class="text-sm font-medium text-gray-700 dark:text-gray-300 mt-2">
                검색
              </div>
              <div class="col-start-2">
                <div class="flex gap-2 w-full">
                  <div class="relative flex-1">
                    <input
                        v-model="roomSearchInput"
                        type="text"
                        placeholder="회의실명 검색"
                        @keyup.enter="applyRoomSearch"
                        class="h-10 w-full rounded-lg border px-3 pr-8 bg-white text-gray-800
                               focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10
                               dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100"
                    />
                    <button
                        v-if="roomSearchInput"
                        type="button"
                        class="absolute inset-y-0 right-4 my-auto text-xs text-gray-400 hover:text-gray-600
                                dark:text-gray-500 dark:hover:text-gray-300"
                        @click="clearRoomSearch"
                    >✕</button>
                  </div>

                  <button
                      type="button"
                      class="h-10 px-4 rounded-lg bg-brand-500 text-xs sm:text-sm font-medium text-white hover:bg-brand-600"
                      @click="applyRoomSearch"
                  >검색</button>
                </div>
              </div>

              <!-- 신규 회의실 -->
              <div v-if="roomsEditing" class="text-sm font-medium text-gray-700 dark:text-gray-300 mt-2">
                신규 회의실
              </div>
              <div v-if="roomsEditing" class="col-start-2">
                <div class="flex gap-2">
                  <input
                      v-model="newRoomName"
                      placeholder="예) 1회의실"
                      class="h-11 w-full rounded-lg border px-3 bg-white text-gray-800
                             focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10
                             dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100"
                  />
                  <button
                      type="button"
                      class="h-11 shrink-0 rounded-lg bg-brand-500 px-4 text-sm font-medium text-white hover:bg-brand-600 disabled:opacity-50"
                      :disabled="!roomsEditing || !canAddRoom"
                      @click="addRoom"
                  >추가</button>
                </div>
                <p v-if="roomFormError" class="text-xs text-error-500">{{ roomFormError }}</p>
              </div>

              <!-- 목록 -->
              <div class="col-span-2 mt-4">
                <div class="ip-scroll rounded-md border border-gray-100 dark:border-gray-800 max-h-60 overflow-auto spin-dark">
                  <div v-if="roomsLoading" class="p-4 text-sm text-gray-500 dark:text-gray-400">불러오는 중…</div>

                  <div v-else-if="!rooms || rooms.length === 0" class="p-4 text-sm text-gray-500 dark:text-gray-400">
                    등록된 회의실이 없습니다.
                  </div>

                  <div v-else-if="filteredRooms.length === 0" class="p-4 text-sm text-gray-500 dark:text-gray-400">
                    검색 결과가 없습니다.
                  </div>

                  <table v-else class="min-w-full table-fixed text-sm">
                    <thead class="sticky top-0 z-10 bg-white dark:bg-gray-900
                    shadow-[0_1px_0_0_rgba(229,231,235,0.7)]
                    dark:shadow-[0_1px_0_0_rgba(31,41,55,0.8)]">
                    <tr class="border-b border-gray-200 dark:border-gray-700 text-xs text-gray-500 dark:text-gray-400">
                      <th class="w-10 px-3 py-2 text-center whitespace-nowrap">번호</th>
                      <th class="px-3 py-2 text-left whitespace-nowrap">회의실명</th>
                      <th class="w-20 px-3 py-2 text-center whitespace-nowrap">상태</th>
                      <th class="w-28 px-3 py-2 text-right whitespace-nowrap">수정일</th>
                      <th class="w-24 px-3 py-2 text-center whitespace-nowrap">관리</th>
                    </tr>
                    </thead>

                    <tbody>
                    <tr
                        v-for="(r, idx) in filteredRooms"
                        :key="r.roomId ?? idx"
                        :class="[
                          'border-b border-gray-100 dark:border-gray-800',
                          idx % 2 === 0 ? 'bg-white dark:bg-gray-900' : 'bg-gray-50 dark:bg-gray-800/60'
                        ]"
                    >
                      <td class="px-3 py-3 text-center text-xs text-gray-400 whitespace-nowrap">{{ idx + 1 }}</td>

                      <td class="px-3 py-3">
                        <template v-if="editingRoomId === r.roomId">
                          <input
                              v-model="editingRoom.roomName"
                              class="w-full rounded-md border border-gray-200 px-2 py-1 text-xs
                       bg-white text-gray-800
                       focus:border-brand-300 focus:outline-hidden focus:ring-2 focus:ring-brand-500/10
                       dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100"
                          />
                        </template>
                        <template v-else>
                          <span class="text-sm text-gray-800 dark:text-gray-200">{{ r.roomName }}</span>
                        </template>
                      </td>

                      <td class="px-3 py-3 text-center whitespace-nowrap">
                        <template v-if="editingRoomId === r.roomId">
                          <select
                              v-model="editingRoom.isActive"
                              class="h-8 rounded-md border border-gray-300 px-2 text-xs
                       bg-white text-gray-800
                       focus:border-brand-300 focus:outline-hidden focus:ring-2 focus:ring-brand-500/10
                       dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100"
                          >
                            <option :value="true">사용</option>
                            <option :value="false">중지</option>
                          </select>
                        </template>
                        <template v-else>
                          <span
                              class="inline-flex items-center justify-center rounded-full px-3 py-1 text-[11px] font-medium"
                              :class="r.isActive
                              ? 'bg-[#D1F2D6] text-[#2F855A] dark:bg-[#1B3A2E] dark:text-[#5FA97A]'
                              : 'bg-[#E5E7EB] text-[#4B5563] dark:bg-[#374151] dark:text-[#D1D5DB]'"
                          >
                            {{ r.isActive ? '사용' : '중지' }}
                          </span>
                        </template>
                      </td>

                      <td class="px-3 py-3 text-right text-xs text-gray-500 dark:text-gray-400 whitespace-nowrap">
                        <template v-if="r.updatedAt || r.createdAt">
                          <span class="block">{{ splitDateTime(r.updatedAt || r.createdAt)[0] }}</span>
                          <span class="block">{{ splitDateTime(r.updatedAt || r.createdAt)[1] }}</span>
                        </template>
                        <span v-else>-</span>
                      </td>

                      <!-- 관리 -->
                      <td class="px-3 py-3 text-center whitespace-nowrap">
                        <template v-if="editingRoomId === r.roomId">
                          <button type="button"
                            class="mr-1 px-2 py-1 text-xs rounded-md border border-brand-500 bg-brand-500 text-white hover:bg-brand-600"
                            :disabled="roomsSaving"
                            @click="saveRoomRow(r)">
                            저장
                          </button>
                          <button type="button"
                            class="px-2 py-1 text-xs rounded-md border border-gray-200 dark:border-gray-700
                               bg-white text-gray-700 hover:bg-gray-50
                               dark:bg-transparent dark:text-gray-200 dark:hover:bg-gray-700"
                            :disabled="roomsSaving"
                            @click="cancelRoomEdit">
                            취소
                          </button>
                        </template>

                        <template v-else>
                          <button type="button"
                            class="px-2 py-1 text-xs rounded-md border border-gray-200 dark:border-gray-700
                               bg-white text-gray-700 hover:bg-gray-50
                               dark:bg-transparent dark:text-gray-200 dark:hover:bg-gray-700
                               disabled:opacity-50"
                            :disabled="!roomsEditing || roomsSaving"
                            @click="startRoomEdit(r)">
                            수정
                          </button>

                          <!-- 하드 삭제(완전 삭제) -->
                          <button type="button"
                            class="ml-1 px-2 py-1 text-xs rounded-md border border-red-200 dark:border-red-700
                                 bg-white text-red-600 hover:bg-red-50
                                 dark:bg-transparent dark:text-red-400 dark:hover:bg-red-900/40
                                 disabled:opacity-50"
                            :disabled="!roomsEditing || roomsSaving"
                            @click="hardDeleteRoom(r)">
                            삭제
                          </button>
                        </template>
                      </td>
                    </tr>
                    </tbody>
                  </table>
                </div>
              </div>

              <!-- ===== 슈퍼계정 위임 ===== -->
              <div class="col-span-2">
                <hr class="my-6 border-gray-200 dark:border-gray-700" />
              </div>

              <div class="col-span-2">
                <div class="flex items-center justify-between">
                  <h3 class="mb-2 text-sm font-medium text-gray-700 dark:text-gray-300">슈퍼계정 위임</h3>
                  <div class="flex gap-2">
                    <button
                        v-if="!delegateEditing"
                        type="button"
                        class="h-9 rounded-lg bg-gray-200 px-3 text-gray-800 hover:bg-gray-100 dark:bg-gray-700 dark:text-gray-100"
                        @click="onDelegateEdit"
                    >수정</button>
                    <button
                        v-else
                        type="button"
                        class="h-9 rounded-lg bg-gray-200 px-3 text-gray-800 hover:bg-gray-100 dark:bg-gray-700 dark:text-gray-100"
                        @click="onDelegateCancel"
                    >취소</button>
                  </div>
                </div>
              </div>

              <!-- 대상 ID 입력 -->
              <div class="text-sm font-medium text-gray-700 dark:text-gray-300 mt-2">대상 사용자 ID(Email)</div>
              <div class="col-start-2">
                <div class="flex gap-2">
                  <input
                      v-model="delegateIdInput"
                      placeholder="예) example@naver.com"
                      :disabled="!delegateEditing"
                      class="h-11 w-full rounded-lg border px-3
                           bg-white text-gray-800 focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10
                           dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100
                           disabled:bg-gray-50 disabled:text-gray-400 disabled:border-gray-200
                           dark:disabled:bg-gray-900/40 dark:disabled:text-gray-500 dark:disabled:border-gray-700"
                  />
                  <button
                      type="button"
                      class="h-11 shrink-0 rounded-lg bg-brand-500 px-4 text-sm font-medium text-white hover:bg-brand-600 disabled:opacity-50"
                      :disabled="!delegateEditing || lookingUp || !delegateIdInput"
                      @click="lookupDelegate"
                  >
                    {{ lookingUp ? '조회 중...' : '검색' }}
                  </button>
                </div>
              </div>
              <div class="col-start-2" v-if="delegateLookupError">
                <p class="mt-1 text-sm text-error-500">{{ delegateLookupError }}</p>
              </div>

              <!-- 조회 결과 카드 -->
              <template v-if="delegateEditing && candidate">
                <div class="col-span-2"></div>
                <div ref="delegateCardRef" class="col-start-2 w-full rounded-lg border border-gray-200 dark:border-gray-800 p-4">
                  <div class="flex items-center justify-between">
                    <div>
                      <p class="text-sm text-gray-600 dark:text-gray-400">
                        이름 :
                        <b class="text-gray-800 dark:text-gray-200">{{ candidate.userName }}</b>
                      </p>
                      <p class="text-sm text-gray-600 dark:text-gray-400">
                        이메일 :
                        <b class="text-gray-800 dark:text-gray-200">{{ candidate.userEmail }}</b>
                      </p>
                      <p class="text-sm text-gray-600 dark:text-gray-400">
                        구분/소속 :
                        <b class="text-gray-800 dark:text-gray-200">
                          {{ roleHuman(candidate.userRole) }} / {{ candidate.centerName || '미할당' }}
                        </b>
                      </p>

                      <p class="mt-2 text-xs text-gray-500 dark:text-gray-400">
                        * 위임 후 현재 계정의 슈퍼 권한은 해제됩니다. 진행 전에 꼭 확인하세요.
                      </p>
                    </div>
                    <button
                        type="button"
                        class="h-10 shrink-0 rounded-lg bg-brand-500 px-4 text-sm font-medium text-white hover:bg-brand-600 disabled:opacity-50"
                        :disabled="!delegateEditing || delegating"
                        @click="delegateNow"
                    >
                      {{ delegating ? '위임 중...' : '위임 확정' }}
                    </button>
                  </div>
                </div>
              </template>

            </template>

<!--          </template>-->

          <!-- 바닥 여백 확보를 위한 구분선 -->
          <div class="col-span-2 mb-6">
<!--            <hr class="my-6 border-gray-200 dark:border-gray-700" />-->
          </div>

        </div>
      </div>
    </div>
  </AdminLayout>

  <!-- 전역 로딩 오버레이 (메모 모달과 동일하게 body로 텔레포트) -->
  <Teleport to="body">
    <transition name="fade">
      <div
          v-if="downloading"
          class="fixed inset-0 z-[2147483646]"
          aria-live="polite" aria-busy="true" role="status"
      >
        <!-- 배경 -->
        <div class="absolute inset-0 bg-black/5 dark:bg-black/60"></div>

        <!-- 스피너 -->
        <div class="absolute inset-0 z-[2147483647] flex items-center justify-center">
          <div class="flex flex-col items-center gap-3">
            <svg class="animate-spin h-8 w-8 text-blue-500" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10"
                      stroke="currentColor" stroke-width="4" fill="none"/>
              <path class="opacity-75" fill="currentColor"
                    d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z"/>
            </svg>
            <p class="text-sm text-gray-900 dark:text-white/90">불러오는 중…</p>
          </div>
        </div>
      </div>
    </transition>
  </Teleport>

</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import CommonGridShape from '@/components/common/CommonGridShape.vue'
import { EyeIcon, EyeSlashIcon } from '@heroicons/vue/24/outline'
import axios from '@/plugins/axios.js'
import { onBeforeRouteLeave } from 'vue-router'

/* ===== 기본 프로필 ===== */
const email = ref('')
const name  = ref('')
const phone = ref('')

const roleLabel = ref('-')
const orgLabel  = ref('-')

/* 전화번호 */
const phoneInput  = ref('')
const savingPhone = ref(false)
const phoneError  = ref('')

/* 비밀번호 */
const currentPassword = ref('')
const newPassword     = ref('')
const newPassword2    = ref('')
const changingPw      = ref(false)

const currentPwError = ref('')
const newPwError     = ref('')
const newPw2Error    = ref('')

/* SUPER(스프레드시트) */
const isSuperEmail = ref(false)
const sheetId       = ref('')
const startRow      = ref(1)
const savingSheet   = ref(false)
const sheetIdError  = ref('')
const startRowError = ref('')
const sheetName      = ref('')
const sheetNameError = ref('')

const sheetEditing  = ref(false)
const originalSheet = ref({ sheetId: '', startRow: 1, sheetName: '' })

/* 비번 보기 토글 */
const showCurrentPw = ref(false)
const showNewPw     = ref(false)
const showNewPw2    = ref(false)
const toggleCurrentPwVisibility = () => { showCurrentPw.value = !showCurrentPw.value }
const togglePasswordVisibility   = () => { showNewPw.value = !showNewPw.value }
const togglePassword2Visibility  = () => { showNewPw2.value = !showNewPw2.value }

// 비밀번호 변경 편집 토글
const pwEditing = ref(false)
function onPwEdit() { pwEditing.value = true }
function onPwCancel() {
  pwEditing.value = false
  resetPwForm()
}

onMounted(async () => {
  await loadMe()
})

/* 서버에서 내 정보 로드 */
async function loadMe() {
  try {
    const { data } = await axios.get('/api/me', { withCredentials: true })
    name.value  = data.userName
    email.value = data.userEmail
    phone.value = data.userPhone

    // console.log(data)
    if (data.grants.role === 'SUPERADMIN') roleLabel.value = '관리자'
    else if (data.grants.role === 'MANAGER') roleLabel.value = '팀장'
    else if (data.grants.role === 'STAFF') roleLabel.value = '프로'
    else if (data.grants.role === 'CENTERHEAD') roleLabel.value = '센터장'
    else if (data.grants.role === 'EXPERT') roleLabel.value = '전문가'
    else roleLabel.value = '-'

    orgLabel.value   = data.centerName || '미할당'
    isSuperEmail.value = data.grants.isSuper

    phoneInput.value = phone.value || ''
  } catch (_) {
    /* 프로필 로드 실패는 조용히 무시 */
  }
}

/* ===== 전화번호 ===== */
const formatPhoneInput = () => {
  let digits = (phoneInput.value || '').replace(/\D/g, '')
  if (digits.length <= 3) phoneInput.value = digits
  else if (digits.length <= 7) phoneInput.value = `${digits.slice(0,3)}-${digits.slice(3)}`
  else phoneInput.value = `${digits.slice(0,3)}-${digits.slice(3,7)}-${digits.slice(7,11)}`
}
const validatePhoneInput = () => {
  if (!phoneInput.value) { phoneError.value = ''; return }
  phoneError.value = /^010-\d{4}-\d{4}$/.test(phoneInput.value)
      ? '' : '전화번호는 010-1234-5678 형식으로 입력해야 합니다.'
}
async function savePhone() {
  validatePhoneInput()
  if (phoneError.value) {
    alert(phoneError.value)
    return
  }
  try {
    savingPhone.value = true
    await axios.put('/api/me/phone', phoneInput.value, {
      headers: { 'Content-Type': 'text/plain;charset=UTF-8' },
      withCredentials: true
    })
    phone.value = phoneInput.value
    alert('전화번호가 저장되었습니다.')
  } catch (e) {
    alert(e?.response?.data || '전화번호 저장 실패')
  } finally {
    savingPhone.value = false
  }
}

/* ===== 비밀번호 ===== */
function resetPwForm() {
  currentPassword.value = ''
  newPassword.value = ''
  newPassword2.value = ''
  currentPwError.value = ''
  newPwError.value = ''
  newPw2Error.value = ''
}
const validateCurrentPassword = () => {
  currentPwError.value = currentPassword.value ? '' : ''
}
const validateNewPassword = () => {
  if (!newPassword.value) { newPwError.value = ''; return }
  newPwError.value = newPassword.value.length < 6 ? '비밀번호는 최소 6자리 이상이어야 합니다.' : ''
}
const validateNewPasswordConfirm = () => {
  if (!newPassword2.value) { newPw2Error.value = ''; return }
  newPw2Error.value = (newPassword.value !== newPassword2.value) ? '비밀번호가 일치하지 않습니다.' : ''
}
watch([newPassword, newPassword2], () => {
  if (!newPassword2.value) { newPw2Error.value = ''; return }
  newPw2Error.value = (newPassword.value !== newPassword2.value) ? '비밀번호가 일치하지 않습니다.' : ''
})
async function changePassword() {
  if (!currentPassword.value || !newPassword.value) {
    currentPwError.value = !currentPassword.value ? '현재 비밀번호를 입력하세요.' : ''
    newPwError.value = !newPassword.value ? '새 비밀번호를 입력하세요.' : newPwError.value
    alert('비밀번호를 입력하세요.')
    return
  }
  if (newPassword.value.length < 6) {
    newPwError.value = '비밀번호는 최소 6자리 이상이어야 합니다.'
    alert('새 비밀번호는 6자 이상이어야 합니다.')
    return
  }
  if (newPassword.value !== newPassword2.value) {
    newPw2Error.value = '비밀번호가 일치하지 않습니다.'
    alert('새 비밀번호가 일치하지 않습니다.')
    return
  }

  try {
    changingPw.value = true
    await axios.put('/api/me/password', {
      currentPassword: currentPassword.value,
      newPassword: newPassword.value
    }, { withCredentials: true })
    alert('비밀번호가 변경되었습니다. 다음 로그인부터 적용됩니다.')
    resetPwForm()
  } catch (e) {
    if (e.response?.status === 400) {
      alert(e?.response?.data || '입력값 확인')
    } else if (e.response?.status === 401) {
      alert('다시 로그인 후 시도하세요.')
    } else {
      alert('변경 실패')
    }
  } finally {
    changingPw.value = false
  }
}

/* ===== 스프레드시트 ===== */
async function loadSheetConfig() {
  try {
    const { data } = await axios.get('/api/me/sheet-settings', { withCredentials: true })

    // 데이터 로드
    sheetId.value  = (data?.sheetId ?? '')
    startRow.value = Number(data?.startRow ?? 1)
    sheetName.value = (data?.sheetName ?? '')

    originalSheet.value = { sheetId: sheetId.value, startRow: startRow.value, sheetName: sheetName.value }
    sheetEditing.value = false // 초기엔 읽기전용
  } catch {
    // 값 없으면 기본값 유지, 편집 off
    sheetEditing.value = false
  }
}

async function onSheetSave() {
  await saveSheetConfig()
  originalSheet.value = { sheetId: sheetId.value, startRow: startRow.value, sheetName: sheetName.value }
  sheetEditing.value = false
}
function onSheetCancel() {
  sheetId.value  = originalSheet.value.sheetId
  startRow.value = originalSheet.value.startRow
  sheetName.value = originalSheet.value.sheetName
  sheetEditing.value = false
}

function validateSheetId() {
  const v = (sheetId.value || '').trim()
  sheetIdError.value = v ? '' : 'Spreadsheet ID를 입력하세요.'
}
function validateSheetName() {
  const v = (sheetName.value || '').trim()
  sheetNameError.value = v ? '' : '시트 제목을 입력하세요.'
}
function validateStartRow() {
  const n = Number(startRow.value || 0)
  startRowError.value = n >= 1 ? '' : '마지막 행은 1 이상의 정수여야 합니다.'
}
async function saveSheetConfig() {
  validateSheetId()
  validateSheetName()
  validateStartRow()
  if (sheetIdError.value || startRowError.value || sheetNameError.value) {
    alert('입력값을 확인하세요.')
    return
  }

  if(!confirm("스프레드시트 연동 정보를 정말 변경하시겠습니까?")) {
    onSheetCancel()
    return
  }

  try {
    savingSheet.value = true
    await axios.put('/api/me/sheet-settings', {
      sheetId: sheetId.value,
      startRow: startRow.value,
      sheetName: sheetName.value
    }, { withCredentials: true })
    alert('시트 설정이 저장되었습니다.')
  } catch (e) {
    alert(e?.response?.data || '시트 정보 저장 실패')
  } finally {
    savingSheet.value = false
  }
}

/* ===== 지점 / 센터(팀) 관리 ===== */

// 지점 검색 상태
const branchSearchInput = ref('')
const branchSearch = ref('')

const filteredBranches = computed(() => {
  // 1) 본사 지점(branchId = 1)은 지점 관리 테이블에서는 숨김
  let list = (branches.value || []).filter(b => b.branchId !== 1)

  const q = (branchSearch.value || '').trim().toLowerCase()
  if (!q) return list

  // 2) 검색어 필터
  return list.filter(b =>
      String(b.branchName || '').toLowerCase().includes(q)
  )
})

async function applyBranchSearch() {
  branchSearch.value = (branchSearchInput.value || '').trim()
  await fetchBranches(branchSearch.value)
}

async function clearBranchSearch() {
  branchSearchInput.value = ''
  branchSearch.value = ''
  await fetchBranches('')
}

// 팀 검색 상태
const centerSearchInput = ref('')
const centerSearch = ref('')

const filteredCenters = computed(() => {
  const list = centers.value || []
  const q = (centerSearch.value || '').trim().toLowerCase()
  if (!q) return list

  return list.filter(c => {
    const name = String(c.centerName || '').toLowerCase()
    const bname = String(getBranchName(c.branchId) || '').toLowerCase()
    return name.includes(q) || bname.includes(q)
  })
})

async function applyCenterSearch() {
  centerSearch.value = (centerSearchInput.value || '').trim()
  await fetchCenters(centerSearch.value)
}

async function clearCenterSearch() {
  centerSearchInput.value = ''
  centerSearch.value = ''
  await fetchCenters('')
}

// ----- 공통: 지점 이름 매핑 -----
const branches = ref([])
const branchesLoading = ref(false)
const branchesEditing = ref(false)
const newBranchName = ref('')
// 지점 입력 에러 메시지
const branchFormError = ref('')

// 지점 추가 가능 여부 (에러 없고, 최소 2자 이상일 때)
const canAddBranch = computed(() => {
  const name = (newBranchName.value || '').trim()
  if (!name) return false
  return !branchFormError.value
})

// 지점 이름 검증 (IP처럼 자동 검증)
watch(newBranchName, (val) => {
  const name = (val || '').trim()
  if (!name) {
    branchFormError.value = ''
    return
  }
  if (name.length < 2) {
    branchFormError.value = '지점명은 2자 이상이어야 합니다.'
  } else if (name.length > 40) {
    branchFormError.value = '지점명은 40자 이하로 입력하세요.'
  } else {
    branchFormError.value = ''
  }
})

const editingBranchId = ref(null)
const editingBranchName = ref('')

// 센터(팀)
const centers = ref([])
const centersLoading = ref(false)
const centersEditing = ref(false)
const newCenterName = ref('')
const newCenterBranchId = ref('') // '' = 지점 지정 안 함
// 팀 입력 에러 메시지
const centerFormError = ref('')

// 팀 추가 가능 여부
const canAddCenter = computed(() => {
  const name = (newCenterName.value || '').trim()
  if (!name) return false
  return !centerFormError.value
})

// 팀 이름 검증
watch(newCenterName, (val) => {
  const name = (val || '').trim()
  if (!name) {
    centerFormError.value = ''
    return
  }
  if (name.length < 2) {
    centerFormError.value = '팀명은 2자 이상이어야 합니다.'
  } else if (name.length > 40) {
    centerFormError.value = '팀명은 40자 이하로 입력하세요.'
  } else {
    centerFormError.value = ''
  }
})

const editingCenterId = ref(null)
const editingCenter = ref({
  centerName: '',
  branchId: null,
  centerColor: null,
})

// 지점 id → 이름 맵
const branchNameMap = computed(() => {
  const m = new Map()
  for (const b of branches.value || []) {
    if (b.branchId != null) m.set(b.branchId, b.branchName)
  }
  return m
})

const getBranchName = (branchId) => {
  if (!branchId) return '지정 안 함'
  return branchNameMap.value.get(branchId) || '지정 안 함'
}

/* ----- 지점(Branch) ----- */

async function fetchBranches(keyword = '') {
  try {
    branchesLoading.value = true
    const { data } = await axios.get('/api/me/branches', { withCredentials: true, params: { keyword: keyword || '' } })
    branches.value = Array.isArray(data) ? data : []
  } catch (e) {
    alert(e?.response?.data || '지점 목록을 불러오지 못했습니다.')
  } finally {
    branchesLoading.value = false
  }
}

async function addBranch() {
  const name = (newBranchName.value || '').trim()
  if (!name) {
    alert('지점 이름을 입력하세요.')
    return
  }
  try {
    await axios.post(
        '/api/me/branches',
        { branchName: name },
        { withCredentials: true },
    )
    newBranchName.value = ''
    await fetchBranches(branchSearch.value)
  } catch (e) {
    const s = e?.response?.status
    if (s === 409) alert(e?.response?.data || '이미 존재하는 지점명입니다.')
    else if (s === 400) alert(e?.response?.data || '요청 값이 올바르지 않습니다.')
    else if (s === 403) alert('접근 권한이 없습니다.')
    else alert('지점 추가에 실패했습니다.')
  }
}

function startBranchEdit(b) {
  if (!branchesEditing.value) return
  editingBranchId.value = b.branchId
  editingBranchName.value = b.branchName || ''
}

function cancelBranchEdit() {
  editingBranchId.value = null
  editingBranchName.value = ''
}

async function saveBranchRow(b) {
  const name = (editingBranchName.value || '').trim()
  if (!name) {
    alert('지점 이름을 입력하세요.')
    return
  }
  try {
    await axios.put(
        `/api/me/branches/${b.branchId}`,
        { branchName: name },
        { withCredentials: true },
    )
    cancelBranchEdit()
    await fetchBranches(branchSearch.value)
  } catch (e) {
    const s = e?.response?.status
    if (s === 409) alert(e?.response?.data || '이미 존재하는 지점명입니다.')
    else if (s === 404) alert(e?.response?.data || '존재하지 않는 지점입니다.')
    else if (s === 403) alert('접근 권한이 없습니다.')
    else alert('지점 수정에 실패했습니다.')
  }
}

async function deleteBranch(branchId) {
  const id = Number(branchId)
  if (!Number.isFinite(id)) {
    alert('잘못된 지점 ID 입니다.')
    return
  }
  if (!confirm('해당 지점을 삭제하시겠습니까?\n(소속 팀이 있으면 삭제할 수 없습니다.)')) return

  try {
    await axios.delete(`/api/me/branches/${id}`, { withCredentials: true })
    await fetchBranches(branchSearch.value)
  } catch (e) {
    const s = e?.response?.status
    if (s === 409) {
      alert(e?.response?.data || '소속 팀이 있어 삭제할 수 없습니다.')
    } else if (s === 404) {
      alert(e?.response?.data || '존재하지 않는 지점입니다.')
    } else if (s === 403) {
      alert('접근 권한이 없습니다.')
    } else {
      alert('지점 삭제에 실패했습니다.')
    }
  }
}

function onBranchesCancel() {
  branchesEditing.value = false
  newBranchName.value = ''
  cancelBranchEdit()
}

/* ----- 센터(Center / 팀) ----- */

// 색상 팔레트
const CENTER_COLOR_PRESETS = [
  '#B23A48', '#9A3412', '#C026D3', '#1E40AF', '#B7791F',
  '#2F855A', '#00796B', '#5B4B9A', '#944E2A', '#4B5563',
]

const newCenterColor = ref(CENTER_COLOR_PRESETS[0])

const isDark = ref(false)
let themeObs = null

const colorDraft = ref(newCenterColor.value)   // 팝업 내부 임시값
const colorOrigin = ref(newCenterColor.value)  // 팝업 열기 전 원본

// 팀색상 수정용
const editColorPopoverOpen = ref(false)
const editColorPopoverStyle = ref({})

const colorMode = ref('NEW');
const colorOpen = ref(false)
const colorBtnRef = ref(null)
const colorPopoverRef = ref(null)

const colorPopoverStyle = ref({
  left: '0px',
  top: '0px',
})

function placeColorPopover() {
  const btn = currentTargetEl.value;
  if (!btn) return
  const r = btn.getBoundingClientRect()

  const popW = 288 // w-72 = 18rem = 288px
  const popH = 220 // 대충(스와치+컬러픽커). 정확히 하려면 nextTick 후 offsetHeight로

  const gap = 8
  const vw = window.innerWidth
  const vh = window.innerHeight

  // 기본: 버튼 오른쪽에 띄우기
  let left = r.right + gap
  let top = r.top

  // 오른쪽 공간 없으면 왼쪽으로
  if (left + popW > vw - 8) {
    left = Math.max(8, r.left - gap - popW)
  }

  // 아래로 벗어나면 위로 보정
  if (top + popH > vh - 8) {
    top = Math.max(8, vh - popH - 8)
  }

  colorPopoverStyle.value = {
    left: `${Math.round(left)}px`,
    top: `${Math.round(top)}px`,
  }
}

// 팀색상 모달 열기
const currentTargetEl = ref(null);
async function toggleColorPopover(event, mode = 'NEW') {
  colorMode.value = mode;

  currentTargetEl.value = event?.currentTarget || colorBtnRef.value;
  if (!colorOpen.value) {
    // 열 때: 모드에 따라 초기색상 설정
    const currentVal = (mode === 'EDIT') ? editingCenter.value.centerColor : newCenterColor.value;
    colorOrigin.value = currentVal || '#4B5563';
    colorDraft.value = currentVal || '#4B5563';

    colorOpen.value = true;
    await nextTick();

    // 버튼 위치 계산 (event가 있으면 해당 버튼 기준으로 위치 잡기)
    const btn = event?.currentTarget || colorBtnRef.value;
    if (btn) {
      const r = btn.getBoundingClientRect();
      // ... (기존 placeColorPopover 로직을 여기에 넣거나 호출)
      placeColorWithTarget(r);
    }
  } else {
    cancelColor();
  }
}

// 팀색상 모달 위치 계산용 공통 함수
function placeColorWithTarget(r) {
  const popW = 288;
  const popH = 220;
  const gap = 8;
  const vw = window.innerWidth;
  const vh = window.innerHeight;

  let left = r.right + gap;
  let top = r.top;
  if (left + popW > vw - 8) left = Math.max(8, r.left - gap - popW);
  if (top + popH > vh - 8) top = Math.max(8, vh - popH - 8);

  colorPopoverStyle.value = {
    left: `${Math.round(left)}px`,
    top: `${Math.round(top)}px`,
  };
}

function confirmColor() {
  if (colorMode.value === 'EDIT') {
    editingCenter.value.centerColor = colorDraft.value;
  } else {
    newCenterColor.value = colorDraft.value;
  }
  colorOpen.value = false;
}

function cancelColor() {
  colorDraft.value = colorOrigin.value
  colorOpen.value = false
}

function onBackdropDown() {
  cancelColor()
}

// 스크롤/리사이즈 시 재배치 or 닫기
function onWindowChange() {
  if (!colorOpen.value) return
  placeColorPopover()
}

onMounted(() => {
  window.addEventListener('resize', onWindowChange, { passive: true })
  window.addEventListener('scroll', onWindowChange, { passive: true, capture: true })
})
onUnmounted(() => {
  window.removeEventListener('resize', onWindowChange)
  window.removeEventListener('scroll', onWindowChange, true)
})

onMounted(() => {
  isDark.value = document.documentElement.classList.contains('dark')
  themeObs = new MutationObserver(() => {
    isDark.value = document.documentElement.classList.contains('dark')
  })
  themeObs.observe(document.documentElement, { attributes: true, attributeFilter: ['class'] })
})
onUnmounted(() => themeObs?.disconnect())

function normalizeHex6(v) {
  const s = String(v || '').trim()
  return /^#[0-9a-fA-F]{6}$/.test(s) ? s : null
}
function hexToRgb(hex) {
  const h = normalizeHex6(hex)
  if (!h) return null
  const n = parseInt(h.slice(1), 16)
  return { r: (n >> 16) & 255, g: (n >> 8) & 255, b: n & 255 }
}
function rgba(hex, a) {
  const rgb = hexToRgb(hex)
  if (!rgb) return null
  return `rgba(${rgb.r}, ${rgb.g}, ${rgb.b}, ${a})`
}
function srgbToLin(c) {
  const v = c / 255
  return v <= 0.04045 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4)
}
function luminance(rgb) {
  const r = srgbToLin(rgb.r)
  const g = srgbToLin(rgb.g)
  const b = srgbToLin(rgb.b)
  return 0.2126 * r + 0.7152 * g + 0.0722 * b
}
function contrast(a, b) {
  const L1 = luminance(a)
  const L2 = luminance(b)
  const hi = Math.max(L1, L2)
  const lo = Math.min(L1, L2)
  return (hi + 0.05) / (lo + 0.05)
}
function blend(fg, alpha, bg) {
  return {
    r: Math.round(fg.r * alpha + bg.r * (1 - alpha)),
    g: Math.round(fg.g * alpha + bg.g * (1 - alpha)),
    b: Math.round(fg.b * alpha + bg.b * (1 - alpha)),
  }
}

// 요구사항: light 0.12 / dark 0.22
function centerBadgeStyle(centerColor) {
  const base = normalizeHex6(centerColor) || '#4B5563'
  const a = isDark.value ? 0.32 : 0.18
  const borderA = isDark.value ? 0.5 : 0.4

  // 배경이 흰/다크(#111827) 위에 깔린다고 가정해서 합성 색으로 대비 계산
  const fg = hexToRgb(base)
  const bg = isDark.value ? { r: 17, g: 24, b: 39 } : { r: 255, g: 255, b: 255 }
  const mixed = blend(fg, a, bg)

  const white = { r: 255, g: 255, b: 255 }
  const darkText = { r: 17, g: 24, b: 39 }

  const text = contrast(mixed, white) >= contrast(mixed, darkText) ? '#FFFFFF' : '#111827'

  return {
    backgroundColor: rgba(base, a),
    borderColor: rgba(base, borderA),
    color: text,
  }
}

async function fetchCenters(keyword = '') {
  try {
    centersLoading.value = true
    const { data } = await axios.get('/api/me/centers', { withCredentials: true, params: { keyword: keyword || '' } })
    centers.value = Array.isArray(data) ? data : []
  } catch (e) {
    alert(e?.response?.data || '팀 목록을 불러오지 못했습니다.')
  } finally {
    centersLoading.value = false
  }
}

async function addCenter() {
  const name = (newCenterName.value || '').trim()
  if (!name) {
    alert('팀 이름을 입력하세요.')
    return
  }

  const payload = {
    centerName: name,
    branchId: newCenterBranchId.value || null, // '' → null
    centerColor: normalizeHex6(newCenterColor.value) || null,
  }

  try {
    await axios.post('/api/me/centers', payload, { withCredentials: true })
    newCenterName.value = ''
    newCenterBranchId.value = ''
    await fetchCenters(centerSearch.value)
  } catch (e) {
    const s = e?.response?.status
    if (s === 409) alert(e?.response?.data || '이미 존재하는 팀명입니다.')
    else if (s === 400) alert(e?.response?.data || '요청 값이 올바르지 않습니다.')
    else if (s === 403) alert('접근 권한이 없습니다.')
    else alert('팀 추가에 실패했습니다.')
  }

  newCenterColor.value = CENTER_COLOR_PRESETS[centers.value.length % CENTER_COLOR_PRESETS.length]
}

function startCenterEdit(c) {
  if (!centersEditing.value) return
  editingCenterId.value = c.centerId
  editingCenter.value = {
    centerName: c.centerName || '',
    branchId: c.branchId ?? null,
    centerColor: normalizeHex6(c.centerColor) || null,
  }
}

function cancelCenterEdit() {
  editingCenterId.value = null
  editingCenter.value = { centerName: '', branchId: null }
}

async function saveCenterRow(c) {
  const name = (editingCenter.value.centerName || '').trim()
  if (!name) {
    alert('팀 이름을 입력하세요.')
    return
  }

  const payload = {
    centerName: name,
    branchId: editingCenter.value.branchId || null,
    centerColor: normalizeHex6(editingCenter.value.centerColor) || null,
  }

  try {
    await axios.put(`/api/me/centers/${c.centerId}`, payload, { withCredentials: true })
    cancelCenterEdit()
    await fetchCenters(centerSearch.value)
  } catch (e) {
    const s = e?.response?.status
    if (s === 409) alert(e?.response?.data || '이미 존재하는 팀명입니다.')
    else if (s === 404) alert(e?.response?.data || '존재하지 않는 팀입니다.')
    else if (s === 403) alert('접근 권한이 없습니다.')
    else alert('팀 수정에 실패했습니다.')
  }
}

async function deleteCenter(centerId) {
  const cid = Number(centerId)
  if (!Number.isFinite(cid)) {
    alert('잘못된 팀 ID 입니다.')
    return
  }
  if (!confirm('해당 팀을 삭제하시겠습니까?\n(소속 직원이 있으면 삭제할 수 없습니다.)')) return

  try {
    await axios.delete(`/api/me/centers/${cid}`, { withCredentials: true })
    await fetchCenters(centerSearch.value)
  } catch (e) {
    const s = e?.response?.status
    if (s === 409) {
      alert(e?.response?.data || '팀에 소속된 직원이 있어 삭제할 수 없습니다.')
    } else if (s === 404) {
      alert(e?.response?.data || '존재하지 않는 팀입니다.')
    } else if (s === 403) {
      alert('접근 권한이 없습니다.')
    } else {
      alert('팀 삭제에 실패했습니다.')
    }
  }
}

function onCentersCancel() {
  centersEditing.value = false
  newCenterName.value = ''
  newCenterBranchId.value = ''
  cancelCenterEdit()
}

/* ----- 전문가(Experts) ----- */

// 목록 & 상태
const experts = ref([])
const expertsLoading = ref(false)
const expertsEditing = ref(false)

const newExpertName = ref('')
const expertFormError = ref('')

const editingExpertId = ref(null)
const editingExpertName = ref('')

// 검색 상태
const expertSearchInput = ref('')
const expertSearch = ref('')

// 필터된 리스트
const filteredExperts = computed(() => {
  const list = experts.value || []
  const q = (expertSearch.value || '').trim().toLowerCase()
  if (!q) return list

  return list.filter(e =>
      String(e.expertName || '').toLowerCase().includes(q)
  )
})

// 추가 가능 여부
const canAddExpert = computed(() => {
  const name = (newExpertName.value || '').trim()
  if (!name) return false
  return !expertFormError.value
})

// 이름 검증 (지점/팀과 동일한 룰)
watch(newExpertName, (val) => {
  const name = (val || '').trim()
  if (!name) {
    expertFormError.value = ''
    return
  }
  if (name.length < 2) {
    expertFormError.value = '전문가명은 2자 이상이어야 합니다.'
  } else if (name.length > 100) {
    expertFormError.value = '전문가명은 100자 이하로 입력하세요.'
  } else {
    expertFormError.value = ''
  }
})

async function fetchExperts(keyword = '') {
  try {
    expertsLoading.value = true
    const { data } = await axios.get('/api/me/experts', {
      withCredentials: true,
      params: { keyword: keyword || '' },
    })
    experts.value = Array.isArray(data) ? data : []
  } catch (e) {
    alert(e?.response?.data || '전문가 목록을 불러오지 못했습니다.')
  } finally {
    expertsLoading.value = false
  }
}

async function applyExpertSearch() {
  expertSearch.value = (expertSearchInput.value || '').trim()
  await fetchExperts(expertSearch.value)
}

async function clearExpertSearch() {
  expertSearchInput.value = ''
  expertSearch.value = ''
  await fetchExperts('')
}

async function addExpert() {
  const name = (newExpertName.value || '').trim()
  if (!name) {
    alert('전문가명을 입력하세요.')
    return
  }
  if (expertFormError.value) {
    alert(expertFormError.value)
    return
  }

  try {
    await axios.post(
        '/api/me/experts',
        { expertName: name },
        { withCredentials: true },
    )
    newExpertName.value = ''
    await fetchExperts(expertSearch.value)
  } catch (e) {
    const s = e?.response?.status
    if (s === 409) alert(e?.response?.data || '이미 존재하는 전문가명입니다.')
    else if (s === 400) alert(e?.response?.data || '요청 값이 올바르지 않습니다.')
    else if (s === 403) alert('접근 권한이 없습니다.')
    else alert('전문가 추가에 실패했습니다.')
  }
}

function startExpertEdit(e) {
  if (!expertsEditing.value) return
  editingExpertId.value = e.expertId
  editingExpertName.value = e.expertName || ''
}

function cancelExpertEdit() {
  editingExpertId.value = null
  editingExpertName.value = ''
}

async function saveExpertRow(e) {
  const name = (editingExpertName.value || '').trim()
  if (!name) {
    alert('전문가명을 입력하세요.')
    return
  }

  try {
    await axios.put(
        `/api/me/experts/${e.expertId}`,
        { expertName: name },
        { withCredentials: true },
    )
    cancelExpertEdit()
    await fetchExperts(expertSearch.value)
  } catch (err) {
    const s = err?.response?.status
    if (s === 409) alert(err?.response?.data || '이미 존재하는 전문가명입니다.')
    else if (s === 404) alert(err?.response?.data || '존재하지 않는 전문가입니다.')
    else if (s === 403) alert('접근 권한이 없습니다.')
    else alert('전문가 수정에 실패했습니다.')
  }
}

async function deleteExpert(expertId) {
  const id = Number(expertId)
  if (!Number.isFinite(id)) {
    alert('잘못된 전문가 ID 입니다.')
    return
  }
  if (!confirm('해당 전문가를 삭제하시겠습니까?')) return

  try {
    await axios.delete(`/api/me/experts/${id}`, { withCredentials: true })
    await fetchExperts(expertSearch.value)
  } catch (e) {
    const s = e?.response?.status
    if (s === 404) alert(e?.response?.data || '존재하지 않는 전문가입니다.')
    else if (s === 403) alert('접근 권한이 없습니다.')
    else alert('전문가 삭제에 실패했습니다.')
  }
}

function onExpertsCancel() {
  expertsEditing.value = false
  newExpertName.value = ''
  expertFormError.value = ''
  cancelExpertEdit()
}

// -----------------------

// 날짜 피커 + 다운로드 + 위임용 상태/메서드 (상단 import 아래쪽에 추가)
import flatpickr from 'flatpickr'
import { Korean } from 'flatpickr/dist/l10n/ko.js'
import 'flatpickr/dist/flatpickr.css'

// ====== 접속 로그 다운로드 ======
const logStartInput = ref(null)
const logEndInput   = ref(null)
const logFrom = ref(null) // Date | null
const logTo   = ref(null) // Date | null
const logRangeError = ref('')
const downloading = ref(false)

let fpStart = null
let fpEnd = null

function mountLogPickers() {
  // 시작일
  try { fpStart?.destroy() } catch {}
  if (logStartInput.value) {
    fpStart = flatpickr(logStartInput.value, {
      locale: Korean,
      dateFormat: 'Y-m-d',
      allowInput: true,
      disableMobile: true,
      onReady: (_d, _s, fp) => {
        fp.calendarContainer.style.zIndex = '999999'
        // --- ✕ 클리어 버튼 (중복 방지) ---
        const anyFp = fp
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
            fp.clear()   // 로그 시작일 제거
            logFrom.value = null
            fp.close()
          })
          fp.calendarContainer.appendChild(btn)
          anyFp._clearBtn = btn
        }
      },
      onChange: (dates) => { logFrom.value = dates[0] ?? null }
    })
    if (logFrom.value) fpStart.setDate(logFrom.value, false, 'Y-m-d')
  }

  // 종료일
  try { fpEnd?.destroy() } catch {}
  if (logEndInput.value) {
    fpEnd = flatpickr(logEndInput.value, {
      locale: Korean,
      dateFormat: 'Y-m-d',
      allowInput: true,
      disableMobile: true,
      onReady: (_d, _s, fp) => {
        fp.calendarContainer.style.zIndex = '999999'
        // --- ✕ 클리어 버튼 (중복 방지) ---
        const anyFp = fp
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
            fp.clear()   // 로그 종료일 제거
            logTo.value = null
            fp.close()
          })
          fp.calendarContainer.appendChild(btn)
          anyFp._clearBtn = btn
        }
      },
      onChange: (dates) => { logTo.value = dates[0] ?? null }
    })
    if (logTo.value) fpEnd.setDate(logTo.value, false, 'Y-m-d')
  }
}
function cleanupLogPickers() {
  try { fpStart?.close(); fpStart?.destroy() } catch {}
  try { fpEnd?.close();   fpEnd?.destroy() } catch {}
  fpStart = fpEnd = null
}

function rebindLogPickers() {
  // DOM이 바뀐 뒤 실행되도록 한 템포 미룸
  nextTick().then(() => {
    cleanupLogPickers()
    mountLogPickers()
  })
}

// 윈도우 리사이즈 시 재바인딩
onMounted(() => window.addEventListener('resize', rebindLogPickers))
onUnmounted(() => window.removeEventListener('resize', rebindLogPickers))

// 스크롤/휠 시 열려있으면 닫기 (달력 잔상 방지)
const closeLogPickersOnScroll = () => {
  if (fpStart?.isOpen) fpStart.close()
  if (fpEnd?.isOpen) fpEnd.close()
}
onMounted(() => {
  window.addEventListener('wheel', closeLogPickersOnScroll, { passive: true, capture: true })
  window.addEventListener('scroll', closeLogPickersOnScroll, { passive: true, capture: true })
  window.addEventListener('touchmove', closeLogPickersOnScroll, { passive: true, capture: true })
})
onUnmounted(() => {
  window.removeEventListener('wheel', closeLogPickersOnScroll, true)
  window.removeEventListener('scroll', closeLogPickersOnScroll, true)
  window.removeEventListener('touchmove', closeLogPickersOnScroll, true)
})

onMounted(() => mountLogPickers())
onUnmounted(() => cleanupLogPickers())
watch([logStartInput, logEndInput], () => nextTick().then(mountLogPickers))

const ymd = (d) => {
  const y = d.getFullYear()
  const m = String(d.getMonth()+1).padStart(2,'0')
  const day = String(d.getDate()).padStart(2,'0')
  return `${y}-${m}-${day}`
}

async function downloadLogs() {
  logRangeError.value = ''
  if (!logFrom.value || !logTo.value) return
  if (logFrom.value > logTo.value) {
    logRangeError.value = '시작일이 종료일보다 늦을 수 없습니다.'
    return
  }

  try {
    downloading.value = true
    // 백엔드: GET /api/super/logs/export?from=YYYY-MM-DD&to=YYYY-MM-DD
    const params = { from: ymd(logFrom.value), to: ymd(logTo.value) }
    const res = await axios.get('/api/me/logs/export', {
      params,
      responseType: 'blob',
      withCredentials: true
    })

    // 파일명 추출
    const cd = res.headers['content-disposition'] || ''
    const match = /filename\*\=UTF-8''([^;]+)/i.exec(cd)
    const fname = match ? decodeURIComponent(match[1]) : `login_logs_${params.from}_to_${params.to}.xlsx`

    const url = window.URL.createObjectURL(new Blob([res.data]))
    const a = document.createElement('a')
    a.href = url
    a.download = fname
    document.body.appendChild(a)
    a.click()
    a.remove()
    window.URL.revokeObjectURL(url)
  } catch (e) {
    alert(e?.response?.data || '다운로드 실패')
  } finally {
    downloading.value = false
  }
}

// ====== 슈퍼계정 위임 ======
const delegateIdInput = ref(null)
const lookingUp = ref(false)
const delegating = ref(false)
const delegateLookupError = ref('')

// 스크롤 smooth
const delegateCardRef = ref(null)
function smoothScrollTo(el, offset = 80) {
  const rect = el.getBoundingClientRect()
  const scrollTopNow =
      (typeof window.scrollY === 'number' ? window.scrollY : null) ??
      document.scrollingElement?.scrollTop ??
      document.documentElement.scrollTop ??
      document.body.scrollTop ?? 0

  const top = Math.max(0, scrollTopNow + rect.top - offset)
  window.scrollTo({ top, behavior: 'smooth' })
}

// 슈퍼계정 위임 편집 토글
const delegateEditing = ref(false)
function onDelegateEdit() { delegateEditing.value = true }
function onDelegateCancel() {
  delegateEditing.value = false
  // 입력/에러/결과 초기화
  delegateIdInput.value = null
  delegateLookupError.value = ''
  candidate.value = null
}

const candidate = ref(null) // { userId, userName, userEmail, userRole, centerName? }

const roleHuman = (role) =>
    role === 'SUPERADMIN' ? '관리자'
        : role === 'MANAGER'  ? '팀장'
        : role === 'STAFF'    ? '프로'
        : role

async function lookupDelegate() {
  candidate.value = null
  delegateLookupError.value = ''

  // 이메일 형식 검사
  const email = String(delegateIdInput.value ?? '').trim()
  const ok = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)
  if (!ok) {
    delegateLookupError.value = '올바른 이메일을 입력하세요.'
    return
  }

  try {
    lookingUp.value = true
    const { data } = await axios.get('/api/me/delegate/lookup', {
      params: { email },
      withCredentials: true
    })
    candidate.value = data
  } catch (e) {
    candidate.value = null
    const s = e?.response?.status
    if (s === 404) delegateLookupError.value = '대상 사용자를 찾을 수 없습니다.'
    else if (s === 403) delegateLookupError.value = '접근 권한이 없습니다.'
    else delegateLookupError.value = '조회 실패'
  } finally {
    lookingUp.value = false
  }

  await nextTick()
  if (delegateCardRef.value) smoothScrollTo(delegateCardRef.value, 80)
}

async function delegateNow() {
  if (!candidate.value) return
  const c = candidate.value
  const ok = confirm(
      `[위임 확인]\n\n대상: ${c.userName} (${c.userEmail})\n구분: ${roleHuman(c.userRole)} / ${c.centerName || '미할당'}\n\n정말 슈퍼 권한을 위 사용자에게 위임하시겠습니까?\n(현재 계정의 슈퍼 권한은 해제됩니다)`
  )
  if (!ok) return

  try {
    delegating.value = true
    await axios.post('/api/me/delegate', { userId: c.userId }, { withCredentials: true })
    alert('슈퍼 권한이 위임되었습니다. 다시 로그인해 주세요.')

    // 권한 위임 즉시 로그아웃/리다이렉트:
    await axios.post('/api/auth/token/logout', {}, { withCredentials: true })
    location.replace('/login')
  } catch (e) {
    const s = e?.response?.status
    if (s === 403) alert('접근 권한이 없습니다.')
    else alert(e?.response?.data || '위임 실패')
  } finally {
    delegating.value = false
  }
}

// ===== IP 화이트리스트 =====
const ipList = ref([])
const ipLoading = ref(false)

const ipEditing = ref(false)    // 편집 모드
const ipSaving  = ref(false)

const newIp = ref('')
const newIpMemo = ref('')
const ipFormError = ref('')

const editingRowId = ref(null)  // 행 편집 ID
const editingRow = ref({
  ipAddress: '',
  memo: '',
  isActive: 'Y',
})

const canAddIp = computed(() => {
  const ip = (newIp.value || '').trim()
  if (!ip) return false
  return validateIpFormat(ip)
})

// 검색어: input에 타이핑용 / 실제 필터용 분리
const ipSearchInput = ref('')
const ipSearch = ref('')

const filteredIpList = computed(() => {
  const list = ipList.value || []
  const raw = ipSearch.value || ''
  const q = raw.trim().toLowerCase()
  if (!q) return list

  return list.filter((row) => {
    const ip = String(row.ipAddress || '').toLowerCase()
    const memo = String(row.memo || '').toLowerCase()
    const dt = String(row.updatedAt || row.createdAt || '').toLowerCase()
    return ip.includes(q) || memo.includes(q) || dt.includes(q)
  })
})

function applyIpSearch() {
  ipSearch.value = (ipSearchInput.value || '').trim()
}

function clearIpSearch() {
  ipSearchInput.value = ''
  ipSearch.value = ''
}

function splitDateTime(str) {
  if (!str) return ['', '']
  const s = String(str).replace('T', ' ')
  const [d, t] = s.split(' ')
  return [d || '', t || '']
}

// IPv4 검사
function isValidIPv4(ip) {
  const v = (ip || '').trim()
  const v4 =
      /^(25[0-5]|2[0-4]\d|1\d{2}|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d{2}|[1-9]?\d)){3}$/
  return v4.test(v)
}

// IPv4 → 두 개의 헥스텟으로 변환
function ipv4ToHextets(ip) {
  const parts = ip.split('.').map(n => Number(n))
  if (parts.length !== 4 || parts.some(n => !Number.isInteger(n) || n < 0 || n > 255)) return null
  const hi = (parts[0] << 8) + parts[1]
  const lo = (parts[2] << 8) + parts[3]
  return [hi.toString(16), lo.toString(16)]
}

// 순수 IPv6 검사
function isValidPureIPv6(ip) {
  const v = (ip || '').trim()
  if (!v) return false
  const isValidBlock = (s) => /^[0-9a-fA-F]{1,4}$/.test(s)
  const hasDoubleColon = v.includes('::')
  if (hasDoubleColon) {
    if (v.indexOf('::') !== v.lastIndexOf('::')) return false
    const parts = v.split('::')
    if (parts.length !== 2) return false
    const head = parts[0] ? parts[0].split(':') : []
    const tail = parts[1] ? parts[1].split(':') : []
    if (head.some(g => !isValidBlock(g)) || tail.some(g => !isValidBlock(g))) return false
    const numGroups = head.length + tail.length
    return numGroups < 8
  } else {
    const groups = v.split(':')
    if (groups.length !== 8) return false
    return groups.every(isValidBlock)
  }
}

// 최종 IP 형식 검사
function validateIpFormat(ip) {
  const v = (ip || '').trim()
  if (!v) return false
  if (!v.includes('.')) {
    if (isValidIPv4(v)) return true
    return isValidPureIPv6(v)
  }
  if (isValidIPv4(v)) return true
  const lastColon = v.lastIndexOf(':')
  if (lastColon === -1) return false
  const ipv6Part = v.slice(0, lastColon)
  const ipv4Part = v.slice(lastColon + 1)
  if (!isValidIPv4(ipv4Part)) return false
  const hextets = ipv4ToHextets(ipv4Part)
  if (!hextets) return false
  const reconstructed = `${ipv6Part}:${hextets[0]}:${hextets[1]}`
  return isValidPureIPv6(reconstructed)
}

// 새 IP 입력 감시
watch(newIp, (val) => {
  const ip = (val || '').trim()
  if (!ip) {
    ipFormError.value = ''
    return
  }
  ipFormError.value = validateIpFormat(ip) ? '' : '올바른 IPv4/IPv6 형식이 아닙니다.'
})

// IP 목록 조회
async function fetchIpWhitelist() {
  if (!isSuperEmail.value) return
  try {
    ipLoading.value = true
    const { data } = await axios.get('/api/me/ip-whitelist', { withCredentials: true })
    ipList.value = Array.isArray(data) ? data : []
  } catch (e) {
    alert(e?.response?.data || 'IP 목록을 불러오지 못했습니다.')
  } finally {
    ipLoading.value = false
  }
}

// 새 IP 추가
async function addIp() {
  const ip   = (newIp.value || '').trim()
  const memo = (newIpMemo.value || '').trim()
  if (!ip) {
    ipFormError.value = 'IP 주소를 입력하세요.'
    return
  }
  if (!validateIpFormat(ip)) {
    ipFormError.value = '올바른 IPv4/IPv6 형식이 아닙니다.'
    return
  }
  try {
    ipSaving.value = true
    await axios.post(
        '/api/me/ip-whitelist',
        {
          ipId: null,
          ipAddress: ip,
          memo: memo || null,
          isActive: 'Y',
        },
        { withCredentials: true },
    )
    newIp.value = ''
    newIpMemo.value = ''
    ipFormError.value = ''
    await fetchIpWhitelist()
  } catch (e) {
    alert(e?.response?.data || 'IP 추가에 실패했습니다.')
  } finally {
    ipSaving.value = false
  }
}

// 행 편집 시작
function startIpRowEdit(row) {
  if (!ipEditing.value) return
  editingRowId.value = row.ipId
  editingRow.value = {
    ipAddress: row.ipAddress || '',
    memo: row.memo || '',
    isActive: row.isActive || 'Y',
  }
}

// 행 편집 취소
function cancelIpRowEdit() {
  editingRowId.value = null
  editingRow.value = { ipAddress: '', memo: '', isActive: 'Y' }
}

// 행 저장
async function saveIpRow(row) {
  const ip   = (editingRow.value.ipAddress || '').trim()
  const memo = (editingRow.value.memo || '').trim()
  const next = editingRow.value.isActive || 'Y'
  if (!ip) {
    alert('IP 주소를 입력하세요.')
    return
  }
  if (!validateIpFormat(ip)) {
    alert('올바른 IPv4/IPv6 형식이 아닙니다.')
    return
  }
  try {
    ipSaving.value = true
    await axios.put(
        `/api/me/ip-whitelist/${row.ipId}`,
        {
          ipAddress: ip,
          memo: memo || null,
          isActive: next,
        },
        { withCredentials: true },
    )
    cancelIpRowEdit()
    await fetchIpWhitelist()
  } catch (e) {
    alert(e?.response?.data || 'IP 수정에 실패했습니다.')
  } finally {
    ipSaving.value = false
  }
}

// 행 완전삭제
async function hardDeleteIpRow(row) {
  if (!row || !row.ipId) return
  if (!confirm('이 IP를 정말 완전히 삭제하시겠습니까?\n(기록이 복구되지 않습니다)')) return

  try {
    ipSaving.value = true
    await axios.delete(`/api/me/ip-whitelist/${row.ipId}/hard`, { withCredentials: true })
    ipList.value = (ipList.value || []).filter(r => r.ipId !== row.ipId)
  } catch (e) {
    alert(e?.response?.data || 'IP 완전 삭제에 실패했습니다.')
  } finally {
    ipSaving.value = false
  }
}

// 전체 수정 취소
function onIpCancel() {
  ipEditing.value  = false
  newIp.value      = ''
  newIpMemo.value  = ''
  ipFormError.value = ''
  cancelIpRowEdit()
}

// 특별계정 켜지면 지점/팀 목록 + IP 화이트리스트 불러오기
watch(
    () => isSuperEmail.value,
    async (ok) => {
      if (!ok) return
      await Promise.all([
        loadSheetConfig(),
        fetchBranches(branchSearch.value),
        fetchCenters(centerSearch.value),
        fetchExperts(expertSearch.value),
        fetchIpWhitelist(),
        fetchRooms(''),
      ])
    },
    { immediate: true },
)

// ===== 회의실 관리 =====
const rooms = ref([])
const roomsLoading = ref(false)
const roomsSaving = ref(false)
const roomsEditing = ref(false)

const newRoomName = ref('')
const roomFormError = ref('')

const editingRoomId = ref(null)
const editingRoom = ref({ roomName: '', isActive: true })

const roomSearchInput = ref('')
const roomSearch = ref('')

const filteredRooms = computed(() => {
  const list = rooms.value || []
  const q = (roomSearch.value || '').trim().toLowerCase()
  if (!q) return list
  return list.filter(r => String(r.roomName || '').toLowerCase().includes(q))
})

const canAddRoom = computed(() => {
  const name = (newRoomName.value || '').trim()
  if (!name) return false
  return !roomFormError.value
})

watch(newRoomName, (val) => {
  const name = (val || '').trim()
  if (!name) { roomFormError.value = ''; return }
  if (name.length < 2) roomFormError.value = '회의실명은 2자 이상이어야 합니다.'
  else if (name.length > 40) roomFormError.value = '회의실명은 40자 이하로 입력하세요.'
  else roomFormError.value = ''
})

async function fetchRooms(keyword = '') {
  try {
    roomsLoading.value = true
    const { data } = await axios.get('/api/me/meeting-rooms', {
      withCredentials: true,
      params: { keyword: keyword || '' }
    })
    rooms.value = Array.isArray(data) ? data : []
  } catch (e) {
    alert(e?.response?.data || '회의실 목록을 불러오지 못했습니다.')
  } finally {
    roomsLoading.value = false
  }
}

async function applyRoomSearch() {
  roomSearch.value = (roomSearchInput.value || '').trim()
  await fetchRooms(roomSearch.value)
}
async function clearRoomSearch() {
  roomSearchInput.value = ''
  roomSearch.value = ''
  await fetchRooms('')
}

async function addRoom() {
  const name = (newRoomName.value || '').trim()
  if (!name) return alert('회의실명을 입력하세요.')
  if (roomFormError.value) return alert(roomFormError.value)

  try {
    roomsSaving.value = true
    await axios.post('/api/me/meeting-rooms', { roomName: name }, { withCredentials: true })
    newRoomName.value = ''
    await fetchRooms(roomSearch.value)
  } catch (e) {
    const s = e?.response?.status
    if (s === 409) alert(e?.response?.data || '이미 존재하는 회의실명입니다.')
    else if (s === 403) alert('접근 권한이 없습니다.')
    else alert(e?.response?.data || '회의실 추가 실패')
  } finally {
    roomsSaving.value = false
  }
}

function startRoomEdit(r) {
  if (!roomsEditing.value) return
  editingRoomId.value = r.roomId
  editingRoom.value = {
    roomId: r.roomId,
    roomName: r.roomName || '',
    isActive: !!r.isActive,
  }
}

function cancelRoomEdit() {
  editingRoomId.value = null
  editingRoom.value = { roomName: '', isActive: true }
}

async function saveRoomRow(r) {
  const name = (editingRoom.value.roomName || '').trim()
  if (!name) return alert('회의실명을 입력하세요.')

  try {
    roomsSaving.value = true
    await axios.put(`/api/me/meeting-rooms/${r.roomId}`, {
      roomName: name,
      isActive: !!editingRoom.value.isActive
    }, { withCredentials: true })

    cancelRoomEdit()
    await fetchRooms(roomSearch.value)
  } catch (e) {
    const s = e?.response?.status
    if (s === 409) alert(e?.response?.data || '이미 존재하는 회의실명입니다.')
    else if (s === 404) alert(e?.response?.data || '존재하지 않는 회의실입니다.')
    else if (s === 403) alert('접근 권한이 없습니다.')
    else alert(e?.response?.data || '회의실 수정 실패')
  } finally {
    roomsSaving.value = false
  }
}

// 소프트 삭제(중지)
async function softDeleteRoom(r) {
  try {
    roomsSaving.value = true
    await axios.patch(`/api/me/meeting-rooms/${r.roomId}/soft-delete`, null, { withCredentials: true })
    await fetchRooms(roomSearch.value)
  } catch (e) {
    alert(e?.response?.data || '회의실 비활성화 실패')
  } finally {
    roomsSaving.value = false
  }
}

// 하드 삭제(완전 삭제)
async function hardDeleteRoom(r) {
  if (!confirm('이 회의실을 정말 완전히 삭제하시겠습니까?\n(기록이 복구되지 않습니다)')) return
  try {
    roomsSaving.value = true
    await axios.delete(`/api/me/meeting-rooms/${r.roomId}/hard-delete`, { withCredentials: true })
    await fetchRooms(roomSearch.value)
  } catch (e) {
    const s = e?.response?.status
    if (s === 409) alert(e?.response?.data || '연결된 데이터가 있어 삭제할 수 없습니다.')
    else alert(e?.response?.data || '회의실 삭제 실패')
  } finally {
    roomsSaving.value = false
  }
}

// 라우트 이탈 시 초기화
onBeforeRouteLeave(() => { onDelegateCancel() })
</script>

<style>
.fade-enter-active, .fade-leave-active { transition: opacity .15s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

/* 다크모드에서 네이티브 컨트롤(스피너 포함)을 다크 스킨으로 */
.dark .spin-dark { color-scheme: dark; }
.dark .spin-dark::-webkit-inner-spin-button,
.dark .spin-dark::-webkit-outer-spin-button {
  filter: invert(2) brightness(0.5) contrast(1.6) opacity(.2);
}

/* 슬라이드 전환 */
.slide-enter-active, .slide-leave-active {
  transition: max-height .28s ease, opacity .2s ease;
}
.slide-enter-from, .slide-leave-to {
  max-height: 0;
  opacity: 0;
}
.slide-enter-to, .slide-leave-from {
  max-height: 2000px;
  opacity: 1;
}

.ip-scroll { scrollbar-gutter: stable; }
</style>
