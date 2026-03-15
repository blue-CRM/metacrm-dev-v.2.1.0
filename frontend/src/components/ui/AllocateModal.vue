<template>
  <div class="fixed inset-0 z-[100000] flex items-center justify-center bg-black/40 p-4"
       @click.self="$emit('close')"
       @keydown.esc.prevent.stop="$emit('close')">
    <div class="w-full max-w-[700px] rounded-2xl bg-white dark:bg-gray-900 shadow-xl border border-gray-200 dark:border-gray-800">
      <!-- 헤더 -->
      <div class="px-5 py-4 border-b border-gray-200 dark:border-gray-800 flex items-center justify-between">
        <h3 class="text-base font-semibold text-gray-800 dark:text-gray-100">{{ title }}</h3>
        <button class="text-gray-400 hover:text-gray-600 dark:hover:text-gray-300" @click="$emit('close')">✕</button>
      </div>

      <!-- 바디 -->
      <div class="px-5 py-4 space-y-4">
        <!-- 선택 개수 -->
        <p class="text-sm text-gray-500 dark:text-gray-400">선택된 고객: <b>{{ selectedCount }}</b>명</p>

        <!-- 슈퍼계정 전용 전문가 DB 갯수 집계 표 출력 slot -->
        <slot name="extra" />

        <!-- HQ 전용: 팀 선택 -->
        <div v-if="mode==='HQ'" class="space-y-1">
          <label class="block text-sm text-gray-600 dark:text-gray-300">팀 선택</label>
          <select v-model.number="centerId"
                  class="w-full h-11 border border-gray-200 dark:border-gray-700 rounded-lg px-3 py-2
                  text-sm focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10
                  dark:bg-gray-800 dark:text-gray-200">
            <option :value="null" disabled>팀를 선택하세요</option>
            <option v-for="c in centers" :key="c.centerId" :value="c.centerId">
              {{ c.centerName }} (인원 {{ c.userCount }}명)
            </option>
          </select>

<!--          <p v-if="selectedCenter && !selectedCenter.hasManager"-->
<!--             class="text-xs text-amber-600 mt-1">-->
<!--            이 팀은 팀장이 없습니다. 팀 소속 직원을 선택해야 분배할 수 있어요.-->
<!--          </p>-->
          <p v-if="centerId === null" class="text-xs text-gray-500 mt-1">
            먼저 팀을 선택하세요.
          </p>
        </div>

        <!-- 직원 검색 -->
        <div class="space-y-2">
          <label class="block text-sm text-gray-600 dark:text-gray-300">직원 검색</label>
          <div class="flex gap-2">
            <input
                v-model.trim="query"
                @input="debouncedSearch"
                type="text"
                placeholder="이름으로 검색"
                :disabled="mode==='HQ' && !centerId"
                class="h-11 w-full rounded-lg border px-3
                   bg-white text-gray-800 focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10
                   dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100"/>
            <button
                @click="searchUsers"
                :disabled="submitting || (mode==='HQ' && !centerId)"
                class="inline-flex h-11 items-center justify-center
                   rounded-lg border border-gray-200 px-4 text-sm
                   hover:bg-gray-50 disabled:opacity-60
                   dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100 dark:hover:bg-gray-700/60">
              <span class="whitespace-nowrap">검색</span>
            </button>
          </div>

          <!-- 팀 미선택 시엔 리스트 숨김 -->
          <div
              v-if="!(mode==='HQ' && !centerId) && users.length"
              class="max-h-52 overflow-auto border border-gray-100 dark:border-gray-800 rounded-md">
            <ul>
              <li
                  v-for="u in users"
                  :key="u.userId"
                  @click="selectUser(u)"
                  class="flex items-center justify-between px-3 py-2 cursor-pointer hover:bg-gray-50 dark:hover:bg-gray-800"
                  :class="pickedUser?.userId === u.userId ? 'bg-gray-100 dark:bg-gray-800/70' : ''"
              >
                <div class="text-sm text-gray-700 dark:text-gray-200">
                  <b>{{ u.userName }}</b>
                  <span class="text-gray-400 ml-2">({{ u.role === 'MANAGER' ? '팀장' : '프로' }})</span>
<!--                  <span class="text-gray-400 ml-2">{{ u.centerName }}</span>-->
                </div>

                <!-- 버튼: 선택됨/선택 -->
                <button
                    class="px-2 py-1 text-xs rounded-md border transition"
                    @click.stop="selectUser(u)"
                    :class="pickedUser?.userId === u.userId
                        ? 'bg-gray-900 text-white border-gray-900 hover:bg-gray-800 dark:bg-white dark:text-gray-900 dark:border-white dark:hover:bg-gray-200'
                        : 'text-gray-700 border-gray-300 hover:bg-gray-50 dark:text-gray-300 dark:border-gray-700 dark:hover:bg-gray-800/70'"
                >
                  {{ pickedUser?.userId === u.userId ? '선택됨' : '선택' }}
                </button>
              </li>
            </ul>
          </div>

<!--          <p v-if="pickedUser" class="text-xs text-gray-500 dark:text-gray-400">-->
<!--            선택됨: <b>{{ pickedUser.userName }}</b>-->
<!--          </p>-->
        </div>
      </div>

      <!-- 푸터 -->
      <div class="px-5 py-3 border-t border-gray-200 dark:border-gray-800 flex items-center justify-end gap-2">
        <button
            class="h-10 px-4 rounded-lg border border-gray-200 text-sm
                 hover:bg-gray-50
                 dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100 dark:hover:bg-gray-700/60"
            @click="$emit('close')">
          취소
        </button>

        <template v-if="mode === 'HQ' && pickedUser && pickedUser.role === 'MANAGER'">
          <button
              class="h-10 px-4 rounded-lg bg-gray-600 text-white text-sm hover:bg-gray-700 disabled:opacity-60"
              :disabled="submitting"
              @click="confirm(false)"> {{ managerPoolLabel }}
          </button>
          <button
              class="h-10 px-4 rounded-lg bg-blue-600 text-white text-sm hover:bg-blue-500 disabled:opacity-60"
              :disabled="submitting"
              @click="confirm(true)"> {{ managerPersonalLabel }}
          </button>
        </template>

        <template v-else>
          <button
              class="h-10 px-4 rounded-lg bg-blue-600 text-white text-sm disabled:opacity-60"
              :disabled="submitting ||
                    (mode==='HQ' && (!centerId || !pickedUser)) ||
                    (mode==='MANAGER' && !pickedUser)"
              @click="confirm(false)">
            {{ submitting ? '처리 중...' : primaryLabel }}
          </button>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import axios from '@/plugins/axios.js'

const props = withDefaults(defineProps<{
  mode: 'HQ' | 'MANAGER'
  centerId?: number | null
  selectedCount: number
  title?: string
  primaryLabel?: string
  managerPoolLabel?: string
  managerPersonalLabel?: string
  excludeCenterIds?: number[]
  apiBase?: string
}>(), {
  title: 'DB 일괄 분배',
  primaryLabel: '분배하기',
  managerPoolLabel: '팀 공용 분배',
  managerPersonalLabel: '팀장 개인 분배',
  apiBase: '/api/work/allocate'
})
const emit = defineEmits<{
  (e:'close'):void,
  (e:'confirm', payload:{
    centerId?:number|null,
    userId?:number|null,
    assignToManagerAsStaff?: boolean // 팀장풀 / 팀장개인 분배 구분
  }):void
}>()

type CenterItem = { centerId:number; centerName:string; userCount:number }
const centers = ref<CenterItem[]>([])
const centerId = ref<number|null>(null)

const users = ref<any[]>([])
const query = ref('')
const pickedUser = ref<any|null>(null)
const submitting = ref(false)

async function loadCenters() {
  if (props.mode !== 'HQ') return

  try {
    const params: any = {}

    if (props.excludeCenterIds?.length) {
      params.excludeCenterIds = props.excludeCenterIds
    }

    const { data } = await axios.get(`${props.apiBase}/centers`, {
      params,
      paramsSerializer: {
        indexes: null,
      },
    })

    if (props.excludeCenterIds?.length) {
      const excludeSet = new Set(props.excludeCenterIds)
      centers.value = (data || []).filter((c: any) => !excludeSet.has(c.centerId))

      if (centerId.value && excludeSet.has(centerId.value)) {
        centerId.value = null
      }
    } else {
      centers.value = data || []
    }
  } catch (e) {
    console.error(e)
  }
}

async function searchUsers() {
  try {
    if (props.mode === 'HQ' && !centerId.value) {
      users.value = []
      return
    }

    const params: any = {}

    if (props.mode === 'HQ') params.centerId = centerId.value || undefined
    if (props.mode === 'MANAGER') params.centerId = props.centerId || undefined
    if (query.value) params.q = query.value

    if (props.excludeCenterIds?.length) {
      params.excludeCenterIds = props.excludeCenterIds
    }

    const { data } = await axios.get(`${props.apiBase}/users`, {
      params,
      paramsSerializer: {
        indexes: null,
      },
    })

    if (props.excludeCenterIds?.length) {
      const excludeSet = new Set(props.excludeCenterIds)
      users.value = (data || []).filter((u: any) => !excludeSet.has(u.centerId))
    } else {
      users.value = data || []
    }
  } catch (e) {
    console.error(e)
  }
}

let t:any = null
function debouncedSearch(){
  // HQ에서 팀 미선택이면 디바운스 없이 무시
  if (props.mode === 'HQ' && !centerId.value) return
  clearTimeout(t)
  t = setTimeout(searchUsers, 250)
}

function selectUser(u:any){ pickedUser.value = u }

async function confirm(assignToManagerAsStaff: boolean = false){
  submitting.value = true
  try {
    if (props.mode === 'HQ' && (!centerId.value)) {
      alert('팀을 선택하세요.')
      return
    } else if (props.mode === 'HQ' && (!pickedUser.value?.userId)) {
      alert('직원을 선택하세요.')
      return
    }

    // 부모 컴포넌트로 플래그와 함께 이벤트 전달
    emit('confirm', {
      centerId: centerId.value ?? undefined,
      userId: pickedUser.value?.userId ?? undefined,
      assignToManagerAsStaff
    })
  } finally { submitting.value = false }
}

// 모달 모드 변경 시
watch(
    () => props.mode,
    async (m) => {
      await loadCenters()
      users.value = []
      pickedUser.value = null
      // 매니저 모드는 모달 열리자마자 빈 검색 허용(팀 고정)
      if (m === 'MANAGER') searchUsers()
    },
    { immediate: true }
)

// HQ에서 팀 선택이 바뀌면 즉시 검색(또는 미선택이면 비우기)
watch(centerId, () => {
  users.value = []
  pickedUser.value = null
  searchUsers()
})

watch(
    () => props.excludeCenterIds,
    async () => {
      await loadCenters()
      users.value = []
      pickedUser.value = null
    },
    { deep: true }
)
</script>
