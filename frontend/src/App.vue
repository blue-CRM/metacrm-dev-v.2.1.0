<script setup>
import { RouterView } from 'vue-router'
import { useUiStore } from '@/stores/ui'
import ThemeProvider from './components/layout/ThemeProvider.vue'
import SidebarProvider from './components/layout/SidebarProvider.vue'

const ui = useUiStore()
</script>


<template>
  <header>
    <div class="wrapper">
      <nav>
        <!-- 네비게이션 메뉴 -->
      </nav>
    </div>
  </header>

  <!-- 메인 화면 -->
  <main class="bg-gray-50 dark:bg-[#0e1626] min-w-0" oncontextmenu="return false" oncopy="return true">
    <ThemeProvider>
      <SidebarProvider>
        <div class="fixed inset-0 z-[9999] flex items-center justify-center bg-gray-300/70 dark:bg-[#0e1626]/70"
             v-if="ui.loading"> </div>
        <RouterView v-else />
      </SidebarProvider>
    </ThemeProvider>
  </main>

</template>

<style scoped>
</style>
