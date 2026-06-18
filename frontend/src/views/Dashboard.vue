<template>
  <AdminLayout>

    <!-- SUPERADMIN 전용 -->
    <div v-if="isSuperAdmin">
      <!-- KPI -->
      <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 md:gap-6">
        <KpiCardCompact title="메타CRM 총 직원 수" :value="kpi.users" delta="11.0%" trend="up">
          <template #icon>
            <UserGroupIcon class="w-6 h-6 fill-gray-800 dark:fill-white/90" />
          </template>
        </KpiCardCompact>

        <!-- 팀 수 -->
        <KpiCardCompact title="메타CRM 지점 수 / 팀 수"
                        :value="`${kpi.branches.toLocaleString()} / ${kpi.teams.toLocaleString()}`"
                        delta="9.1%" trend="down">
          <template #icon>
            <HomeIcon class="w-6 h-6 fill-gray-800 dark:fill-white/90" />
          </template>
        </KpiCardCompact>
      </div>

      <!-- 통계: 지점별 / 팀별 / 개인별 -->
      <div class="mt-6 grid grid-cols-12 gap-4 md:gap-6">

        <!-- 지점별 -->
        <div
            class="col-span-12 xl:col-span-4 rounded-2xl border p-5
                 border-gray-200
                 dark:border-[#1f2a47] dark:bg-[#14203a]"
        >
          <!-- 헤더 + 버튼 -->
          <div class="mb-4 flex items-center justify-between">
            <div class="text-lg font-semibold text-gray-900 dark:text-gray-100">지점별</div>
            <div class="flex items-center gap-2">
              <button
                  class="h-11 shrink-0 rounded-lg border border-gray-300 dark:border-[#28365a] bg-white dark:bg-[#16213b] px-4 text-sm text-gray-800 dark:text-gray-100 hover:bg-gray-50 dark:hover:bg-[#233152]
                       dark:border-[#28365a] dark:bg-[#16213b] dark:text-gray-100 dark:hover:bg-[#233152]/60"
                  @click="openBranchPicker = true"
              >
                지점 선택
                <span class="ml-1 text-gray-500 dark:text-gray-400">
                  ({{ pickedBranches.size }}개)
                </span>
              </button>
              <button
                  class="h-11 shrink-0 rounded-lg border border-gray-300 dark:border-[#28365a] bg-white dark:bg-[#16213b] px-4 text-sm text-gray-800 dark:text-gray-100 hover:bg-gray-50 dark:hover:bg-[#233152]
                       disabled:opacity-50
                       dark:border-[#28365a] dark:bg-[#16213b] dark:text-gray-100 dark:hover:bg-[#233152]/60"
                  @click="clearPickedBranches"
                  :disabled="pickedBranches.size === 0"
              >
                초기화
              </button>
            </div>
          </div>

          <!-- 값 리스트 -->
          <ul
              class="divide-y divide-gray-200 rounded-xl border border-gray-200 overflow-hidden
                   dark:divide-white/10 dark:border-white/10"
          >
            <li class="flex items-center justify-between px-4 py-3">
              <span class="text-sm text-gray-600 dark:text-gray-300">총 직원 수</span>
              <b class="text-base text-gray-900 dark:text-gray-100">
                {{ branchAgg.totalUsers.toLocaleString() }}
              </b>
            </li>
            <li class="flex items-center justify-between px-4 py-3">
              <span class="text-sm text-gray-600 dark:text-gray-300">
                기간내 DB 갯수 (중복 포함)
              </span>
              <b class="text-base text-gray-900 dark:text-gray-100">
                {{ branchAgg.dbRangeWithDup.toLocaleString() }}
              </b>
            </li>
            <li class="flex items-center justify-between px-4 py-3">
              <span class="text-sm text-gray-600 dark:text-gray-300">
                기간내 DB 갯수 (유효DB만)
              </span>
              <b class="text-base text-gray-900 dark:text-gray-100">
                {{ branchAgg.dbRangeOnly.toLocaleString() }}
              </b>
            </li>
            <li class="flex items-center justify-between px-4 py-3">
              <span class="text-sm text-gray-600 dark:text-gray-300">
                전체 DB 갯수 (중복 포함)
              </span>
              <b class="text-base text-gray-900 dark:text-gray-100">
                {{ branchAgg.dbAllWithDup.toLocaleString() }}
              </b>
            </li>
            <li class="flex items-center justify-between px-4 py-3">
              <span class="text-sm text-gray-600 dark:text-gray-300">
                전체 DB 갯수 (유효DB만)
              </span>
              <b class="text-base text-gray-900 dark:text-gray-100">
                {{ branchAgg.dbAllOnly.toLocaleString() }}
              </b>
            </li>
          </ul>
        </div>

        <!-- 팀별 -->
        <div
            class="col-span-12 xl:col-span-4 rounded-2xl border p-5
                 border-gray-200
                 dark:border-[#1f2a47] dark:bg-[#14203a]"
        >
          <!-- 헤더 + 버튼 -->
          <div class="mb-4 flex items-center justify-between">
            <div class="text-lg font-semibold text-gray-900 dark:text-gray-100">
              팀별
            </div>
            <div class="flex items-center gap-2">
              <button
                  class="h-11 shrink-0 rounded-lg border border-gray-300 dark:border-[#28365a] bg-white dark:bg-[#16213b] px-4 text-sm text-gray-800 dark:text-gray-100 hover:bg-gray-50 dark:hover:bg-[#233152]
                       dark:border-[#28365a] dark:bg-[#16213b] dark:text-gray-100 dark:hover:bg-[#233152]/60"
                  @click="openCenterPicker = true"
              >
                팀 선택
                <span class="ml-1 text-gray-500 dark:text-gray-400">
                  ({{ pickedCenters.size }}개)
                </span>
              </button>
              <button
                  class="h-11 shrink-0 rounded-lg border border-gray-300 dark:border-[#28365a] bg-white dark:bg-[#16213b] px-4 text-sm text-gray-800 dark:text-gray-100 hover:bg-gray-50 dark:hover:bg-[#233152]
                       disabled:opacity-50
                       dark:border-[#28365a] dark:bg-[#16213b] dark:text-gray-100 dark:hover:bg-[#233152]/60"
                  @click="clearPickedCenters"
                  :disabled="pickedCenters.size === 0"
              >
                초기화
              </button>
            </div>
          </div>

          <!-- 값 리스트 -->
          <ul
              class="divide-y divide-gray-200 rounded-xl border border-gray-200 overflow-hidden
                   dark:divide-white/10 dark:border-white/10"
          >
            <li class="flex items-center justify-between px-4 py-3">
              <span class="text-sm text-gray-600 dark:text-gray-300">총 직원 수</span>
              <b class="text-base text-gray-900 dark:text-gray-100">
                {{ centerAgg.totalUsers.toLocaleString() }}
              </b>
            </li>
            <li class="flex items-center justify-between px-4 py-3">
              <span class="text-sm text-gray-600 dark:text-gray-300">
                기간내 DB 갯수 (중복 포함)
              </span>
              <b class="text-base text-gray-900 dark:text-gray-100">
                {{ centerAgg.dbRangeWithDup.toLocaleString() }}
              </b>
            </li>
            <li class="flex items-center justify-between px-4 py-3">
              <span class="text-sm text-gray-600 dark:text-gray-300">
                기간내 DB 갯수 (유효DB만)
              </span>
              <b class="text-base text-gray-900 dark:text-gray-100">
                {{ centerAgg.dbRangeOnly.toLocaleString() }}
              </b>
            </li>
            <li class="flex items-center justify-between px-4 py-3">
              <span class="text-sm text-gray-600 dark:text-gray-300">
                전체 DB 갯수 (중복 포함)
              </span>
              <b class="text-base text-gray-900 dark:text-gray-100">
                {{ centerAgg.dbAllWithDup.toLocaleString() }}
              </b>
            </li>
            <li class="flex items-center justify-between px-4 py-3">
              <span class="text-sm text-gray-600 dark:text-gray-300">
                전체 DB 갯수 (유효DB만)
              </span>
              <b class="text-base text-gray-900 dark:text-gray-100">
                {{ centerAgg.dbAllOnly.toLocaleString() }}
              </b>
            </li>
          </ul>
        </div>

        <!-- 개인별 (프로 단일 선택) -->
        <div
            class="col-span-12 xl:col-span-4 rounded-2xl border p-5
                 border-gray-200
                 dark:border-[#1f2a47] dark:bg-[#14203a]"
        >
          <!-- 제목 + 검색창 + 버튼 같은 행 -->
          <div class="mb-4 flex items-center gap-2">
            <div class="text-lg font-semibold text-gray-900 dark:text-gray-100 shrink-0 mr-2">
              개인별
            </div>

            <!-- 검색창 + 버튼 묶음 -->
            <div class="flex items-center gap-2 flex-1">
              <input
                  v-model="userQuery"
                  placeholder="프로 검색"
                  @keydown.enter.prevent="pickFirstUser"
                  class="h-11 w-full rounded-lg border px-3
                       bg-white dark:bg-[#16213b] text-gray-800 dark:text-gray-100 focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10
                       dark:border-[#28365a] dark:bg-[#16213b] dark:text-gray-100"
              />
              <button
                  type="button"
                  class="h-11 shrink-0 rounded-lg border border-gray-300 dark:border-[#28365a] bg-white dark:bg-[#16213b] px-4 text-sm text-gray-800 dark:text-gray-100 hover:bg-gray-50 dark:hover:bg-[#233152]
                       dark:border-[#28365a] dark:bg-[#16213b] dark:text-gray-100 dark:hover:bg-[#233152]/60"
                  @click="pickFirstUser"
                  :disabled="!canSearchUser"
              >
                선택
              </button>
            </div>
          </div>

          <!-- 값 리스트 -->
          <ul
              class="divide-y divide-gray-200 rounded-xl border border-gray-200 overflow-hidden
                   dark:divide-white/10 dark:border-white/10"
          >
            <!-- 프로 정보 -->
            <li class="flex items-center justify-between px-4 py-3">
              <span class="text-sm text-gray-600 dark:text-gray-300">선택된 프로 정보</span>
              <b class="text-base text-gray-900 dark:text-gray-100">
                <template v-if="pickedUserInfo">
                  {{ pickedUserInfo.name }}
                  ({{ pickedUserInfo.center }}, {{ roleLabel(pickedUserInfo.role) }})
                </template>
                <template v-else>
                  조회 결과 없음
                </template>
              </b>
            </li>
            <li class="flex items-center justify-between px-4 py-3">
              <span class="text-sm text-gray-600 dark:text-gray-300">
                기간내 DB 갯수 (중복 포함)
              </span>
              <b
                  v-if="userAggLoading"
                  class="text-base text-gray-900 dark:text-gray-100"
              >
                로딩 중 ...
              </b>
              <b
                  v-else
                  class="text-base text-gray-900 dark:text-gray-100"
              >
                {{ userAgg.dbRangeWithDup.toLocaleString() }}
              </b>
            </li>
            <li class="flex items-center justify-between px-4 py-3">
              <span class="text-sm text-gray-600 dark:text-gray-300">
                기간내 DB 갯수 (유효DB만)
              </span>
              <b
                  v-if="userAggLoading"
                  class="text-base text-gray-900 dark:text-gray-100"
              >
                로딩 중 ...
              </b>
              <b
                  v-else
                  class="text-base text-gray-900 dark:text-gray-100"
              >
                {{ userAgg.dbRangeOnly.toLocaleString() }}
              </b>
            </li>
            <li class="flex items-center justify-between px-4 py-3">
              <span class="text-sm text-gray-600 dark:text-gray-300">
                전체 DB 갯수 (중복 포함)
              </span>
              <b
                  v-if="userAggLoading"
                  class="text-base text-gray-900 dark:text-gray-100"
              >
                로딩 중 ...
              </b>
              <b
                  v-else
                  class="text-base text-gray-900 dark:text-gray-100"
              >
                {{ userAgg.dbAllWithDup.toLocaleString() }}
              </b>
            </li>
            <li class="flex items-center justify-between px-4 py-3">
              <span class="text-sm text-gray-600 dark:text-gray-300">
                전체 DB 갯수 (유효DB만)
              </span>
              <b
                  v-if="userAggLoading"
                  class="text-base text-gray-900 dark:text-gray-100"
              >
                로딩 중 ...
              </b>
              <b
                  v-else
                  class="text-base text-gray-900 dark:text-gray-100"
              >
                {{ userAgg.dbAllOnly.toLocaleString() }}
              </b>
            </li>
          </ul>
        </div>
      </div>
    </div>

    <!-- 팀장/프로 용 -->
    <div v-else class="mt-2 grid grid-cols-12 gap-4 md:gap-6 items-stretch">
      <!-- KPI -->
      <div
          class="col-span-12 xl:col-span-4 space-y-4 xl:space-y-0 xl:grid xl:grid-rows-[1fr_1fr] xl:gap-4 xl:h-full xl:min-h-0"
      >
        <KpiCardCompact title="메타CRM 총 직원 수" :value="kpi.users" delta="11.0%" trend="up">
          <template #icon>
            <UserGroupIcon class="w-6 h-6 fill-gray-800 dark:fill-white/90" />
          </template>
        </KpiCardCompact>

        <KpiCardCompact title="메타CRM 총 팀 수" :value="kpi.teams" delta="9.1%" trend="down">
          <template #icon>
            <HomeIcon class="w-6 h-6 fill-gray-800 dark:fill-white/90" />
          </template>
        </KpiCardCompact>
      </div>

      <!-- 통계: 개인별 (내/동료 프로) -->
      <div
          class="col-span-12 xl:col-span-8 xl:h-full rounded-2xl border dark:border-[#1f2a47] bg-white dark:bg-[#14203a] p-5
             dark:border-[#1f2a47] dark:bg-[#14203a]"
      >
        <!-- 제목 + 검색창 + 버튼 같은 행 -->
        <div class="mb-4 flex items-center gap-2">
          <div class="text-lg font-semibold text-gray-900 dark:text-gray-100 shrink-0 mr-2">
            개인별
          </div>

          <!-- 검색창 + 버튼 묶음 -->
          <div class="flex items-center gap-2 flex-1">
            <input
                v-model="userQuery"
                placeholder="프로 검색"
                @keydown.enter.prevent="pickFirstUser"
                class="h-11 w-full rounded-lg border px-3
                     bg-white dark:bg-[#16213b] text-gray-800 dark:text-gray-100 focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10
                     dark:border-[#28365a] dark:bg-[#16213b] dark:text-gray-100"
            />
            <button
                type="button"
                class="h-11 shrink-0 rounded-lg border border-gray-300 dark:border-[#28365a] bg-white dark:bg-[#16213b] px-4 text-sm text-gray-800 dark:text-gray-100 hover:bg-gray-50 dark:hover:bg-[#233152]
                     dark:border-[#28365a] dark:bg-[#16213b] dark:text-gray-100 dark:hover:bg-[#233152]/60"
                @click="pickFirstUser"
                :disabled="!canSearchUser"
            >
              선택
            </button>
          </div>
        </div>

        <!-- 값 리스트 -->
        <ul
            class="divide-y divide-gray-200 rounded-xl border border-gray-200 overflow-hidden
                 dark:divide-white/10 dark:border-white/10"
        >
          <!-- 프로 정보 -->
          <li class="flex items-center justify-between px-4 py-3">
            <span class="text-sm text-gray-600 dark:text-gray-300">선택된 프로 정보</span>
            <b class="text-base text-gray-900 dark:text-gray-100">
              <template v-if="pickedUserInfo">
                {{ pickedUserInfo.name }}
                ({{ pickedUserInfo.center }}, {{ roleLabel(pickedUserInfo.role) }})
              </template>
              <template v-else>
                조회 결과 없음
              </template>
            </b>
          </li>
          <li class="flex items-center justify-between px-4 py-3">
            <span class="text-sm text-gray-600 dark:text-gray-300">
              기간내 DB 갯수 (중복 포함)
            </span>
            <b
                v-if="userAggLoading"
                class="text-base text-gray-900 dark:text-gray-100"
            >
              로딩 중 ...
            </b>
            <b
                v-else
                class="text-base text-gray-900 dark:text-gray-100"
            >
              {{ userAgg.dbRangeWithDup.toLocaleString() }}
            </b>
          </li>
          <li class="flex items-center justify-between px-4 py-3">
            <span class="text-sm text-gray-600 dark:text-gray-300">
              기간내 DB 갯수 (유효DB만)
            </span>
            <b
                v-if="userAggLoading"
                class="text-base text-gray-900 dark:text-gray-100"
            >
              로딩 중 ...
            </b>
            <b
                v-else
                class="text-base text-gray-900 dark:text-gray-100"
            >
              {{ userAgg.dbRangeOnly.toLocaleString() }}
            </b>
          </li>
          <li class="flex items-center justify-between px-4 py-3">
            <span class="text-sm text-gray-600 dark:text-gray-300">
              전체 DB 갯수 (중복 포함)
            </span>
            <b
                v-if="userAggLoading"
                class="text-base text-gray-900 dark:text-gray-100"
            >
              로딩 중 ...
            </b>
            <b
                v-else
                class="text-base text-gray-900 dark:text-gray-100"
            >
              {{ userAgg.dbAllWithDup.toLocaleString() }}
            </b>
          </li>
          <li class="flex items-center justify-between px-4 py-3">
            <span class="text-sm text-gray-600 dark:text-gray-300">
              전체 DB 갯수 (유효DB만)
            </span>
            <b
                v-if="userAggLoading"
                class="text-base text-gray-900 dark:text-gray-100"
            >
              로딩 중 ...
            </b>
            <b
                v-else
                class="text-base text-gray-900 dark:text-gray-100"
            >
              {{ userAgg.dbAllOnly.toLocaleString() }}
            </b>
          </li>
        </ul>
      </div>
    </div>

    <!-- 배치 기준 안내 -->
    <p class="col-span-12 mt-3 text-xs text-gray-500 dark:text-gray-400 text-right">
      ※ 통계 데이터는 매일 새벽 1시에 일괄 업데이트된 기준값입니다.
    </p>

    <!-- 지점 선택 모달 -->
    <Teleport v-if="isSuperAdmin" to="body">
      <div
          v-if="openBranchPicker"
          class="fixed inset-0 z-[100000] flex items-center justify-center bg-black/40 p-4"
          role="dialog"
          aria-modal="true"
          @click.self="cancelBranches()"
      >
        <div
            class="w-full max-w-[700px] rounded-2xl bg-white dark:bg-[#0e1626] shadow-xl
                 border border-gray-200 dark:border-[#1f2a47]"
        >
          <!-- 헤더 -->
          <div
              class="px-5 py-4 border-b border-gray-200 dark:border-[#1f2a47] flex items-center justify-between"
          >
            <h3 class="text-base font-semibold text-gray-800 dark:text-gray-100">지점 선택</h3>
            <button
                class="text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"
                @click="cancelBranches()"
            >
              ✕
            </button>
          </div>

          <!-- 바디 -->
          <div class="px-5 py-4 space-y-4">
            <!-- 검색 -->
            <div class="flex gap-2">
              <input
                  v-model="branchQuery"
                  placeholder="지점 검색"
                  class="h-11 w-full rounded-lg border px-3
                       bg-white dark:bg-[#16213b] text-gray-800 dark:text-gray-100 placeholder:text-gray-400
                       focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10
                       dark:border-[#28365a] dark:bg-[#16213b] dark:text-gray-100 dark:placeholder:text-gray-400"
              />
              <div class="flex gap-2">
                <button
                    class="inline-flex h-11 items-center justify-center rounded-lg border border-gray-200 px-4 text-sm
                         hover:bg-gray-50 disabled:opacity-60
                         dark:border-[#28365a] dark:bg-[#16213b] dark:text-gray-100 dark:hover:bg-[#233152]/60"
                    @click="selectAllFilteredBranches"
                >
                  <span class="whitespace-nowrap">전체</span>
                </button>
                <button
                    class="inline-flex h-11 items-center justify-center rounded-lg border border-gray-200 px-4 text-sm
                         hover:bg-gray-50 disabled:opacity-60
                         dark:border-[#28365a] dark:bg-[#16213b] dark:text-gray-100 dark:hover:bg-[#233152]/60"
                    @click="modalBranches = new Set()"
                >
                  <span class="whitespace-nowrap">해제</span>
                </button>
              </div>
            </div>

            <!-- 리스트 -->
            <div
                class="max-h-[56vh] overflow-auto border border-gray-200 dark:border-[#1f2a47] rounded-md"
            >
              <ul>
                <li
                    v-for="b in filteredBranches"
                    :key="b.id"
                    @click="toggleBranch(b.id)"
                    class="flex items-center justify-between px-3 py-2 cursor-pointer hover:bg-gray-50 dark:hover:bg-[#16213b]"
                    :class="modalBranches.has(b.id)
                    ? 'bg-gray-100 dark:bg-[#16213b]/70'
                    : ''"
                >
                  <div class="truncate text-sm text-gray-900 dark:text-gray-100">
                    {{ b.name }}
                  </div>

                  <!-- 버튼: 선택됨/선택 -->
                  <button
                      class="px-2 py-1 text-xs rounded-md border transition"
                      @click.stop="toggleBranch(b.id)"
                      :class="modalBranches.has(b.id)
                      ? 'bg-gray-900 text-white border-gray-900 hover:bg-gray-800 dark:bg-white dark:text-gray-900 dark:border-white dark:hover:bg-gray-200'
                      : 'text-gray-700 border-gray-300 hover:bg-gray-50 dark:text-gray-300 dark:border-[#28365a] dark:hover:bg-[#16213b]/70'"
                  >
                    {{ modalBranches.has(b.id) ? '선택됨' : '선택' }}
                  </button>
                </li>
              </ul>
            </div>

            <!-- 선택 현황 -->
            <div class="text-sm text-gray-500 dark:text-gray-400">
              총 {{ filteredBranches.length.toLocaleString() }}개 중 선택
              {{ modalBranches.size.toLocaleString() }}개
            </div>
          </div>

          <!-- 푸터 -->
          <div
              class="px-5 py-3 border-t border-gray-200 dark:border-[#1f2a47] flex items-center justify-end gap-2"
          >
            <button
                class="inline-flex h-10 items-center justify-center rounded-lg border border-gray-200 px-4 text-sm
                     hover:bg-gray-50
                     dark:border-[#28365a] dark:bg-[#16213b] dark:text-gray-100 dark:hover:bg-[#233152]/60"
                @click="cancelBranches"
            >
              취소
            </button>
            <button
                class="inline-flex h-10 items-center justify-center rounded-lg bg-blue-600 text-white px-4 text-sm hover:bg-blue-700 disabled:opacity-60"
                @click="confirmBranches"
            >
              확인
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 팀(센터) 선택 모달 -->
    <Teleport v-if="isSuperAdmin" to="body">
      <div
          v-if="openCenterPicker"
          class="fixed inset-0 z-[100000] flex items-center justify-center bg-black/40 p-4"
          role="dialog"
          aria-modal="true"
          @click.self="cancelCenters()"
      >
        <div
            class="w-full max-w-[700px] rounded-2xl bg-white dark:bg-[#0e1626] shadow-xl
                 border border-gray-200 dark:border-[#1f2a47]"
        >
          <!-- 헤더 -->
          <div
              class="px-5 py-4 border-b border-gray-200 dark:border-[#1f2a47] flex items-center justify-between"
          >
            <h3 class="text-base font-semibold text-gray-800 dark:text-gray-100">팀 선택</h3>
            <button
                class="text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"
                @click="cancelCenters()"
            >
              ✕
            </button>
          </div>

          <!-- 바디 -->
          <div class="px-5 py-4 space-y-4">
            <!-- 검색 -->
            <div class="flex gap-2">
              <input
                  v-model="centerQuery"
                  placeholder="팀 검색"
                  class="h-11 w-full rounded-lg border px-3
                       bg-white dark:bg-[#16213b] text-gray-800 dark:text-gray-100 placeholder:text-gray-400
                       focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10
                       dark:border-[#28365a] dark:bg-[#16213b] dark:text-gray-100 dark:placeholder:text-gray-400"
              />
              <div class="flex gap-2">
                <button
                    class="inline-flex h-11 items-center justify-center rounded-lg border border-gray-200 px-4 text-sm
                         hover:bg-gray-50 disabled:opacity-60
                         dark:border-[#28365a] dark:bg-[#16213b] dark:text-gray-100 dark:hover:bg-[#233152]/60"
                    @click="selectAllFilteredCenters"
                >
                  <span class="whitespace-nowrap">전체</span>
                </button>
                <button
                    class="inline-flex h-11 items-center justify-center rounded-lg border border-gray-200 px-4 text-sm
                         hover:bg-gray-50 disabled:opacity-60
                         dark:border-[#28365a] dark:bg-[#16213b] dark:text-gray-100 dark:hover:bg-[#233152]/60"
                    @click="modalCenters = new Set()"
                >
                  <span class="whitespace-nowrap">해제</span>
                </button>
              </div>
            </div>

            <!-- 리스트 -->
            <div
                class="max-h-[56vh] overflow-auto border border-gray-200 dark:border-[#1f2a47] rounded-md"
            >
              <ul>
                <li
                    v-for="c in filteredCenters"
                    :key="c.id"
                    @click="toggleCenter(c.id)"
                    class="flex items-center justify-between px-3 py-2 cursor-pointer hover:bg-gray-50 dark:hover:bg-[#16213b]"
                    :class="modalCenters.has(c.id)
                    ? 'bg-gray-100 dark:bg-[#16213b]/70'
                    : ''"
                >
                  <div class="truncate text-sm text-gray-900 dark:text-gray-100">
                    {{ c.name }}
                  </div>

                  <!-- 버튼: 선택됨/선택 -->
                  <button
                      class="px-2 py-1 text-xs rounded-md border transition"
                      @click.stop="toggleCenter(c.id)"
                      :class="modalCenters.has(c.id)
                      ? 'bg-gray-900 text-white border-gray-900 hover:bg-gray-800 dark:bg-white dark:text-gray-900 dark:border-white dark:hover:bg-gray-200'
                      : 'text-gray-700 border-gray-300 hover:bg-gray-50 dark:text-gray-300 dark:border-[#28365a] dark:hover:bg-[#16213b]/70'"
                  >
                    {{ modalCenters.has(c.id) ? '선택됨' : '선택' }}
                  </button>
                </li>
              </ul>
            </div>

            <!-- 선택 현황 -->
            <div class="text-sm text-gray-500 dark:text-gray-400">
              총 {{ filteredCenters.length.toLocaleString() }}개 중 선택
              {{ modalCenters.size.toLocaleString() }}개
            </div>
          </div>

          <!-- 푸터 -->
          <div
              class="px-5 py-3 border-t border-gray-200 dark:border-[#1f2a47] flex items-center justify-end gap-2"
          >
            <button
                class="inline-flex h-10 items-center justify-center rounded-lg border border-gray-200 px-4 text-sm
                     hover:bg-gray-50
                     dark:border-[#28365a] dark:bg-[#16213b] dark:text-gray-100 dark:hover:bg-[#233152]/60"
                @click="cancelCenters"
            >
              취소
            </button>
            <button
                class="inline-flex h-10 items-center justify-center rounded-lg bg-blue-600 text-white px-4 text-sm hover:bg-blue-700 disabled:opacity-60"
                @click="confirmCenters"
            >
              확인
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </AdminLayout>

  <!-- 전역 로딩 오버레이 -->
  <Teleport to="body">
    <transition name="fade">
      <div
          v-if="showTableSpinner"
          class="fixed inset-0 z-[2147483646]"
          aria-live="polite"
          aria-busy="true"
          role="status"
      >
        <!-- 배경 -->
        <div class="absolute inset-0 bg-black/5 dark:bg-black/60"></div>

        <!-- 스피너 -->
        <div class="absolute inset-0 z-[2147483647] flex items-center justify-center">
          <div class="flex flex-col items-center gap-3">
            <svg class="animate-spin h-8 w-8 text-blue-500" viewBox="0 0 24 24">
              <circle
                  class="opacity-25"
                  cx="12"
                  cy="12"
                  r="10"
                  stroke="currentColor"
                  stroke-width="4"
                  fill="none"
              />
              <path
                  class="opacity-75"
                  fill="currentColor"
                  d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z"
              />
            </svg>
            <p class="text-sm text-gray-900 dark:text-white/90">불러오는 중…</p>
          </div>
        </div>
      </div>
    </transition>
  </Teleport>
</template>

<script setup>
import { ref, computed, onMounted, watch, onUnmounted } from 'vue'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import KpiCardCompact from '@/components/ui/KpiCardCompact.vue'
import { globalFilters as gf } from '@/composables/globalFilters'
import axios from '@/plugins/axios.js'
import { useAuthStore } from '@/stores/auth'
import { UserGroupIcon, HomeIcon } from '@/icons'

const auth = useAuthStore()
const roleLabel = (r) => {
  if (r === 'SUPERADMIN') return '관리자'
  if (r === 'MANAGER') return '팀장'
  if (r === 'STAFF') return '프로'
  if (r === 'CENTERHEAD') return '센터장'
  if (r === 'EXPERT') return '전문가'
}

const makeEmptyAgg = () => ({
  totalUsers: 0,
  dbRangeWithDup: 0,
  dbRangeOnly: 0,
  dbAllWithDup: 0,
  dbAllOnly: 0,
})

const isSuperAdmin = computed(() => auth.grants.role === 'SUPERADMIN')
const myCenterId = ref(null) // 내 센터ID를 한 번만 채워서 재사용
const isScopedToCenter = computed(() => !isSuperAdmin.value && myCenterId.value != null)

const branches = ref([]) // 지점 목록
const branchesLoaded = ref(false)
const centers = ref([]) // 센터 목록 (모달 열릴 때만 채움)
const centersLoaded = ref(false)
const usersFound = ref([]) // 최근 검색 결과(라벨링을 위해)

const dbsCenters = ref([]) // 현재 선택된 "팀들(센터들)"의 DB 묶음
const usersForCenters = ref([]) // 선택된 팀들의 직원 목록(본사 제외, STAFF만)

const dbsBranches = ref([]) // 선택된 지점들의 DB 묶음
const usersForBranches = ref([]) // 선택된 지점의 직원 목록

// 로딩 상태
const userAggLoading = ref(false)
const uiLoading = ref(false)
const busy = computed(() => uiLoading.value)
const showTableSpinner = ref(false)
let delayTimer = null

async function runBusy(task) {
  if (uiLoading.value) return
  uiLoading.value = true
  try {
    await task()
  } finally {
    uiLoading.value = false
  }
}

watch(busy, (v) => {
  if (v) {
    // 짧은 로딩은 스피너 숨김
    delayTimer = setTimeout(() => {
      showTableSpinner.value = true
    }, 200)
  } else {
    if (delayTimer) {
      clearTimeout(delayTimer)
      delayTimer = null
    }
    showTableSpinner.value = false
  }
})

onUnmounted(() => {
  if (delayTimer) {
    clearTimeout(delayTimer)
    delayTimer = null
  }
})

/* KPI */
const kpiUsers = ref(0)
const kpiTeams = ref(0)
const kpiBranches  = ref(0)
const kpi = computed(() => ({
  users:    kpiUsers.value,
  teams:    kpiTeams.value,
  branches: kpiBranches.value,
}))

/* 날짜 범위 (헤더 전역 필터만 적용) */
const inRange = (ymd) =>
    (!gf.dateFrom || ymd >= gf.dateFrom) && (!gf.dateTo || ymd <= gf.dateTo)

/* 지점 멀티 선택 */
const pickedBranches = ref(new Set())
const openBranchPicker = ref(false)
const branchQuery = ref('')
const modalBranches = ref(new Set())

/* 센터(팀) 멀티 선택 */
const pickedCenters = ref(new Set())
const openCenterPicker = ref(false)
const centerQuery = ref('')
const modalCenters = ref(new Set()) // 모달 내부 임시 선택 -> 확인 버튼 클릭시 확정

// 선택된 프로 정보 (이름/소속/권한)
const pickedUserInfo = computed(() => {
  if (!pickedUserId.value) return null
  return usersFound.value.find((u) => u.id === pickedUserId.value) || null
})

const filteredBranches = computed(() => {
  const q = branchQuery.value.trim().toLowerCase()
  const list = branches.value ?? []
  return q ? list.filter((b) => (b.name ?? '').toLowerCase().includes(q)) : list
})

function toggleBranch(id) {
  const s = new Set(modalBranches.value)
  s.has(id) ? s.delete(id) : s.add(id)
  modalBranches.value = s
}
function selectAllFilteredBranches() {
  const s = new Set(modalBranches.value)
  for (const b of filteredBranches.value) s.add(b.id)
  modalBranches.value = s
}
function clearPickedBranches() {
  pickedBranches.value = new Set()
  branchAgg.value = makeEmptyAgg()
  dbsBranches.value = []
  usersForBranches.value = []
  modalBranches.value = new Set()
}

async function confirmBranches() {
  if (!isSuperAdmin.value) return

  pickedBranches.value = new Set(modalBranches.value)
  const ids = Array.from(pickedBranches.value)

  await runBusy(async () => {
    try {
      await loadBranchStats(ids)
    } finally {
      openBranchPicker.value = false
    }
  })
}

function cancelBranches() {
  modalBranches.value = new Set(pickedBranches.value)
  branchQuery.value = ''
  openBranchPicker.value = false
}

/* 센터(팀) 목록 필터 */
const filteredCenters = computed(() => {
  const q = centerQuery.value.trim().toLowerCase()
  const list = centers.value ?? []
  return q ? list.filter((c) => (c.name ?? '').toLowerCase().includes(q)) : list
})
function toggleCenter(id) {
  const s = new Set(modalCenters.value)
  s.has(id) ? s.delete(id) : s.add(id)
  modalCenters.value = s
}
function selectAllFilteredCenters() {
  const s = new Set(modalCenters.value)
  for (const c of filteredCenters.value) s.add(c.id)
  modalCenters.value = s
}
function clearPickedCenters() {
  pickedCenters.value = new Set()
  centerAgg.value = makeEmptyAgg()
  dbsCenters.value = []
  usersForCenters.value = []
  modalCenters.value = new Set()
}

// 센터 확인 버튼
async function confirmCenters() {
  if (!isSuperAdmin.value) return

  pickedCenters.value = new Set(modalCenters.value)
  const ids = Array.from(pickedCenters.value)

  await runBusy(async () => {
    try {
      await loadCenterStats(ids)
    } finally {
      openCenterPicker.value = false
    }
  })
}

// 집계 값: 지점 / 팀 / 개인
const branchAgg = ref(makeEmptyAgg())
const centerAgg = ref(makeEmptyAgg())
const userAgg   = ref(makeEmptyAgg())

/* 프로 단일 선택 */
const userQuery = ref('')
const pickedUserId = ref(null)
const canSearchUser = computed(() => userQuery.value.trim().length > 0)

async function pickFirstUser() {
  const s = userQuery.value.trim()
  if (!s) return

  await searchUsersExactByName(s) // 프로명 정확히 일치해야함

  const hit = usersFound.value[0]

  if (isScopedToCenter.value && hit && hit.centerId !== myCenterId.value) {
    // 다른 센터면 선택 불가
    pickedUserId.value = null
    userAgg.value = makeEmptyAgg()
    return
  }

  pickedUserId.value = hit ? hit.id : null

  // 해당 프로의 DB 로드
  userAggLoading.value = true
  try {
    if (pickedUserId.value) {
      await loadUserStats(pickedUserId.value)
    } else {
      userAgg.value = makeEmptyAgg()
    }
  } finally {
    userAggLoading.value = false
  }
}

// 센터 모달 닫기 버튼
function cancelCenters() {
  modalCenters.value = new Set(pickedCenters.value)
  centerQuery.value = ''
  openCenterPicker.value = false
}

// ---------- API helpers ----------
const normCenters = (raw = []) =>
    raw.map((r) => ({
      id: r.id ?? r.centerId,
      name: r.name ?? r.centerName,
      branchId: r.branchId ?? r.branch_id ?? null,
      branchName: r.branchName ?? r.branch_name ?? null,
    }))

const normUsers = (raw = []) =>
    raw.map((r) => ({
      id: r.id ?? r.userId,
      name: r.name ?? r.userName,
      email: r.email ?? r.userEmail,
      centerId: r.centerId,
      center: r.center,
      branchId: r.branchId ?? r.branch_id ?? null,
      branch: r.branch ?? r.branchName ?? null,
      role: r.role,
    }))

async function ensureBranchesOnce() {
  if (branchesLoaded.value) return
  const res = await axios.get('/api/common/dashboard/branches')
  branches.value = (res.data || []).map((b) => ({
    id: b.id ?? b.branchId,
    name: b.name ?? b.branchName,
  }))
  branchesLoaded.value = true
}

async function ensureCentersOnce() {
  if (centersLoaded.value) return
  const res = await axios.get('/api/common/dashboard/centers') // 모달 열릴 때만 호출
  centers.value = normCenters(res.data)
  centersLoaded.value = true
}

function scopeUsers(list = []) {
  return list
      .filter((u) => u.centerId !== 1) // HQ 제외
      .filter(
          (u) => !isScopedToCenter.value || u.centerId === myCenterId.value,
      ) // 내 센터만
}

async function searchUsersExactByName(name) {
  const params = { name, excludeHq: true }
  if (isScopedToCenter.value) params.centerId = myCenterId.value

  const res = await axios.get('/api/common/dashboard/users/find', {
    params,
  })
  usersFound.value = scopeUsers(normUsers(res.data || []))
}

async function searchUsersExactByEmail(email) {
  const params = { email, excludeHq: true }
  if (isScopedToCenter.value) params.centerId = myCenterId.value

  const res = await axios.get('/api/common/dashboard/users/find', {
    params,
  })
  usersFound.value = scopeUsers(normUsers(res.data || []))
}

async function loadUserStats(userId) {
  const { data } = await axios.get('/api/common/dashboard/stats/user', {
    params: {
      userId,
      dateFrom: gf.dateFrom,
      dateTo: gf.dateTo,
    },
  })

  userAgg.value = {
    totalUsers: 1,
    dbRangeWithDup: data.dbRangeWithDup ?? 0,
    dbRangeOnly:    data.dbRangeOnly ?? 0,
    dbAllWithDup:   data.dbAllWithDup ?? 0,
    dbAllOnly:      data.dbAllOnly ?? 0,
  }
}

async function loadCenterStats(centerIds = []) {
  const { data } = await axios.get('/api/common/dashboard/stats/centers', {
    params: {
      centerIds: centerIds.join(','),
      dateFrom: gf.dateFrom,
      dateTo: gf.dateTo,
    },
  })

  centerAgg.value = {
    totalUsers:     data.totalUsers ?? 0,
    dbRangeWithDup: data.dbRangeWithDup ?? 0,
    dbRangeOnly:    data.dbRangeOnly ?? 0,
    dbAllWithDup:   data.dbAllWithDup ?? 0,
    dbAllOnly:      data.dbAllOnly ?? 0,
  }
}

async function loadBranchStats(branchIds = []) {
  const { data } = await axios.get('/api/common/dashboard/stats/branches', {
    params: {
      branchIds: branchIds.join(','),
      dateFrom: gf.dateFrom,
      dateTo: gf.dateTo,
    },
  })

  branchAgg.value = {
    totalUsers:     data.totalUsers ?? 0,
    dbRangeWithDup: data.dbRangeWithDup ?? 0,
    dbRangeOnly:    data.dbRangeOnly ?? 0,
    dbAllWithDup:   data.dbAllWithDup ?? 0,
    dbAllOnly:      data.dbAllOnly ?? 0,
  }
}

async function loadCenterUsers(centerIds = []) {
  if (!Array.isArray(centerIds) || centerIds.length === 0) {
    usersForCenters.value = []
    return
  }
  const params = {
    centerIds: centerIds.join(','), // 선택한 센터만
    excludeHq: true, // 본사 제외
    role: 'STAFF', // 담당자만(팀장 제외)
  }
  if (auth.grants.role !== 'SUPERADMIN' && auth.centerId) {
    params.centerId = auth.centerId
  }
  const res = await axios.get('/api/common/dashboard/users', { params })
  usersForCenters.value = normUsers(res.data || [])
}

async function loadBranchUsers(branchIds = []) {
  if (!Array.isArray(branchIds) || branchIds.length === 0) {
    usersForBranches.value = []
    return
  }
  const params = {
    branchIds: branchIds.join(','),
    excludeHq: true,
    role: 'STAFF',
  }
  const res = await axios.get('/api/common/dashboard/users', { params })
  usersForBranches.value = normUsers(res.data || [])
}

// ---------- lazy flows ----------
watch(openCenterPicker, async (v) => {
  if (v) {
    await ensureCentersOnce()
    modalCenters.value = new Set(pickedCenters.value)
  }
})

watch(openBranchPicker, async (v) => {
  if (v) {
    await ensureBranchesOnce()
    modalBranches.value = new Set(pickedBranches.value)
  }
})

onMounted(async () => {
  // KPI 로딩 (전체 직원수, 전체 팀수)
  try {
    const { data } = await axios.get('/api/common/dashboard/summary')
    kpiUsers.value = data.users ?? 0
    // 백엔드가 teams 또는 centers 둘 중 하나만 줄 수도 있어서 둘 다 대응
    kpiTeams.value = (data.teams ?? data.centers) ?? 0
  } catch (e) {
    console.warn('KPI 로드 실패', e)
  }

  // 지점 수 KPI
  try {
    const { data: branchList } = await axios.get('/api/common/dashboard/branches')
    kpiBranches.value = (branchList || []).length
  } catch (e) {
    console.warn('지점 KPI 로드 실패', e)
  }

  // 팀장/프로는 자기 계정 기준으로 자동 세팅
  if (auth.grants.role === 'MANAGER' || auth.grants.role === 'STAFF') {
    if (auth.name) {
      await searchUsersExactByName(auth.name)
    } else if (auth.email) {
      await searchUsersExactByEmail(auth.email)
    }

    const me = usersFound.value[0]

    if (me) {
      myCenterId.value = me.centerId
      pickedUserId.value = me.id
      userQuery.value = me.name || ''

      userAggLoading.value = true
      try {
        await loadUserStats(me.id)
      } finally {
        userAggLoading.value = false
      }
    }
  }
})
</script>
