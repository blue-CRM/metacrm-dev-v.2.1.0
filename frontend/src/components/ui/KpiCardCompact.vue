<template>
  <div class="rounded-2xl border border-gray-200 p-5 md:p-6
         bg-gradient-to-r from-blue-50/80 via-blue-50/20 to-white
         dark:border-[#1f2a47]
         dark:bg-none dark:bg-[#14203a]">
    <div class="icon-scope flex items-center justify-center w-12 h-12 rounded-xl
             bg-white/95 shadow-sm shadow-blue-100/80 ring-1 ring-blue-100
             text-blue-600
             dark:bg-[#16213b] dark:text-white/90 dark:shadow-none dark:ring-0">
      <!-- 아이콘 슬롯 -->
      <slot name="icon">
        <!-- 기본 아이콘 (없어도 됨) -->
        <svg class="fill-gray-800 dark:fill-white/90" width="24" height="24" viewBox="0 0 24 24"><circle cx="12" cy="12" r="10"/></svg>
      </slot>
    </div>

    <!-- 제목(좌)  |  값(우) -->
    <div class="mt-5 flex items-center justify-between">
      <span class="text-sm text-gray-500 dark:text-gray-400">{{ title }}</span>
      <h4 class="font-bold text-gray-800 text-title-sm dark:text-white/90">{{ fmt(value) }}</h4>
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  title: { type: String, default: '' },   // 예: '직원수'
  value: { type: [Number, String], default: 0 }, // 예: 3782
  delta: { type: String, default: '' },   // 예: '11.0%'
  trend: { type: String, default: 'up' }  // 'up' | 'down'
})
const fmt = (v) => {
  const n = Number(v)
  return Number.isFinite(n) ? n.toLocaleString() : v
}
</script>