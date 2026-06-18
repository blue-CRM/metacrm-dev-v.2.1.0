<template>
  <div class="min-h-screen xl:flex bg-white dark:bg-[#0e1626]">
    <app-sidebar />

    <!-- 모바일에서 사이드바 반투명하게 하는 기능 -->
    <Backdrop />
    <div
        class="flex-1 bg-white dark:bg-[#0e1626]"
        :class="[isExpanded || isHovered ? 'lg:ml-[240px]' : 'lg:ml-[90px]']"
    >

      <app-header />
      <div class="p-4 mx-auto max-w-screen-3xl md:p-6 sm:pt-[180px] md:pt-[220px] pt-[150px] lg:pt-6">
        <!-- 카드 가로 넓이 -->
        <!-- <div class="p-4 mx-auto max-w-screen-2xl md:p-6 sm:pt-[180px] md:pt-[220px] pt-[150px] lg:pt-6">-->
        <slot></slot>
      </div>
    </div>
  </div>
</template>

<script setup>
import AppSidebar from './AppSidebar.vue'
import AppHeader from './AppHeader.vue'
import { useSidebar } from '@/composables/useSidebar'
import Backdrop from './Backdrop.vue'
const { isExpanded, isHovered } = useSidebar()
</script>
