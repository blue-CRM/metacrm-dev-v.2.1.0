<template>
  <div
    :class="[
      'rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03]',
      className,
    ]"
  >
    <!-- Card Header -->
    <div class="px-6 py-4">
<!--      <h3 class="text-base font-medium text-gray-800 dark:text-white/90">-->
<!--        {{ title }}-->
<!--      </h3>-->
<!--      <p v-if="desc" class="mt-1 text-sm text-gray-500 dark:text-gray-400">-->
<!--        {{ desc }}-->
<!--      </p>-->

      <!-- 옵션들 (좌측: 셀렉트/버튼, 우측: 새로고침) -->
      <div class="flex flex-wrap items-center gap-1 px-1 py-1">
        <!-- 왼쪽 그룹 -->
        <div class="flex flex-wrap items-center gap-1">
          <!-- 고정 select (30, 50, 100) -->
          <select
              @input="$emit('changeSize', $event.target.value)"
              class="w-25 h-11 border border-gray-200 dark:border-gray-700 rounded-lg px-2 py-1
               text-sm focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10
               dark:bg-gray-800 dark:text-gray-400 text-gray-500">
            <option v-for="n in [20,30,50,100]" :key="n" :value="n" :selected="n === 50">{{ n }}개</option>
          </select>

          <!-- 부모에서 넘겨준 select 리스트 -->
          <template v-for="(selectOptions, idx) in selects" :key="'sel-' + idx">
            <select
                @input="$emit('selectChange', { idx, value: $event.target.value })"
                class="w-30 h-11 border border-gray-200 dark:border-gray-700 rounded-lg px-2 py-1
                text-sm focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10
                dark:bg-gray-800 dark:text-gray-200 text-gray-500"
            >
              <option v-for="opt in selectOptions" :key="opt" :value="opt">{{ opt }}</option>
            </select>
          </template>

          <!-- 부모에서 넘겨준 버튼 리스트 -->
          <template v-for="(btn, idx) in buttons" :key="'btn-' + idx">
            <button
                :disabled="isDisabled(btn)"
                @click="!isDisabled(btn) && $emit('buttonClick', btn)"
                :aria-pressed="active && active.includes(btn) ? 'true' : 'false'"
                :data-state="active && active.includes(btn) ? 'on' : 'off'"
                :class="[
                  'w-30 h-11 px-3 py-1 rounded-lg border text-sm font-medium transition-colors',
                  isDisabled(btn)
                    ? 'border-gray-200 bg-gray-50 text-gray-300 dark:border-gray-700 dark:bg-gray-900/40 dark:text-gray-600'
                    : 'border-gray-200 text-gray-500 hover:bg-gray-100 dark:border-gray-700 dark:text-gray-400 dark:hover:bg-gray-800',
                  active && active.includes(btn) && !isDisabled(btn)
                    ? 'bg-gray-100 dark:bg-gray-800'
                    : ''
                ]"
            >
              {{ btn }}
            </button>
          </template>
        </div>

        <!-- 오른쪽: 새로고침 -->
        <button
            v-if="showRefresh"
            :disabled="refreshing"
            @click="$emit('refresh')"
            title="새로고침"
            class="ml-auto inline-flex h-9 w-9 items-center justify-center rounded-full
                 border border-gray-200 bg-white text-gray-600 hover:bg-gray-100
                 disabled:opacity-60 disabled:cursor-not-allowed
                 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-200
                 dark:border-gray-700 dark:bg-white/5 dark:text-gray-400 dark:hover:bg-gray-800 dark:focus-visible:ring-blue-900"
        >
          <RefreshIcon :class="['h-4 w-4', refreshing ? 'animate-spin' : '']"/>
        </button>
      </div>
    </div>

    <!-- Card Body -->
    <div class="px-6 pt-0 pb-5 border-gray-100 dark:border-gray-800">
      <div class="space-y-5">
        <slot></slot>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { defineProps, defineEmits } from 'vue'
import { RefreshIcon } from '../../icons' // 아이콘

interface Props {
  title?: string
  className?: string
  desc?: string
  selects?: string[][] // select option 문자열 배열들의 배열
  buttons?: string[] // 버튼 문자열 배열
  disabledButtons?: string[] // 비활성 버튼 배열
  active?: string[] // 버튼 문자열 토글 가시화 (프론트 디자인)
  showRefresh?: boolean // 새로고침 아이콘 표시 여부
  refreshing?: boolean // 새로고침 중(스피너)
}

const props = defineProps<Props>()
defineEmits(['changeSize', 'selectChange', 'buttonClick', 'refresh'])

function isDisabled(btn: string) {
  return props.disabledButtons?.includes(btn) ?? false
}
</script>
