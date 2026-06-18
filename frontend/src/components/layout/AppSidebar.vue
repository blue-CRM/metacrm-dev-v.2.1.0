<template>
  <aside
    :class="[
     'fixed top-0 left-0 flex flex-col px-5 bg-white dark:bg-[#0e1626] dark:border-[#1f2a47] text-gray-900 h-screen border-r border-gray-200 z-[100000]',
      {
        'lg:w-[240px]': isExpanded || isMobileOpen || isHovered,
        'lg:w-[90px]': !isExpanded && !isHovered,
        'translate-x-0 w-[240px]': isMobileOpen,
        '-translate-x-full': !isMobileOpen,
        'lg:translate-x-0': true,
      },
    ]"
    @mouseenter="!isExpanded && (isHovered = true)"
    @mouseleave="isHovered = false"
  >
    <div class="py-4 mt-3 flex justify-center items-center">
      <router-link to="/">
<!--     라이트 모드 로고-->
        <img
          v-if="isExpanded || isHovered || isMobileOpen"
          class="dark:hidden"
          src="/images/logo/menu_logo.png"
          alt="Logo"
          width="180"
          height="48"
        />
<!--        다크모드 로고-->
        <img
          v-if="isExpanded || isHovered || isMobileOpen"
          class="hidden dark:block"
          src="/images/logo/dark_menu_logo.png"
          alt="Logo"
          width="180"
          height="48"
        />
<!--        화면 접혔을 때 보이는 아이콘-->
<!--        <img-->
<!--          v-else-->
<!--          src="/favicon.png"-->
<!--          alt="Logo"-->
<!--          width="32"-->
<!--          height="32"-->
<!--        />-->
      </router-link>
    </div>

    <div
      class="flex flex-col overflow-y-auto duration-300 ease-linear no-scrollbar"
    >
      <nav class="mb-6">
        <div class="flex flex-col gap-4">
          <div v-for="(menuGroup, groupIndex) in visibleMenuGroups" :key="groupIndex">
            <h2
              :class="[
                'mb-4 text-xs uppercase flex leading-[20px] text-gray-400',
                !isExpanded && !isHovered
                  ? 'lg:justify-center'
                  : 'justify-start',
              ]"
            >
<!--              <template v-if="isExpanded || isHovered || isMobileOpen">-->
<!--                {{ menuGroup.title }}-->
<!--              </template>-->
<!--              <HorizontalDots v-else />-->
            </h2>
            <ul class="flex flex-col gap-4">
              <li v-for="(item, index) in menuGroup.items" :key="item.name">
                <button
                  v-if="item.subItems"
                  @click="toggleSubmenu(groupIndex, index)"
                  :class="[
                    'menu-item group w-full',
                    {
                      'menu-item-active': isSubmenuOpen(groupIndex, index),
                      'menu-item-inactive': !isSubmenuOpen(groupIndex, index),
                    },
                    !isExpanded && !isHovered
                      ? 'lg:justify-center'
                      : 'lg:justify-start',
                  ]"
                >
                  <span
                    :class="[
                      isSubmenuOpen(groupIndex, index)
                        ? 'menu-item-icon-active'
                        : 'menu-item-icon-inactive',
                    ]"
                  >
                    <component :is="item.icon" />
                  </span>
                  <span
                    v-if="isExpanded || isHovered || isMobileOpen"
                    class="menu-item-text"
                    >{{ item.name }}</span
                  >
<!--                  <ChevronDownIcon-->
<!--                    v-if="isExpanded || isHovered || isMobileOpen"-->
<!--                    :class="[-->
<!--                      'ml-auto w-5 h-5 transition-transform duration-200',-->
<!--                      {-->
<!--                        'rotate-180 text-brand-500': isSubmenuOpen(-->
<!--                          groupIndex,-->
<!--                          index-->
<!--                        ),-->
<!--                      },-->
<!--                    ]"-->
<!--                  />-->
                </button>
                <router-link
                  v-else-if="item.path"
                  :to="item.path"
                  :class="[
                    'menu-item group',
                    {
                      'menu-item-active': isActive(item.path),
                      'menu-item-inactive': !isActive(item.path),
                    },
                  ]"
                >
                  <span
                    :class="[
                      isActive(item.path)
                        ? 'menu-item-icon-active'
                        : 'menu-item-icon-inactive',
                    ]"
                  >
                    <component :is="item.icon" />
                  </span>
                  <span
                    v-if="isExpanded || isHovered || isMobileOpen"
                    class="menu-item-text"
                    >{{ item.name }}</span
                  >
                </router-link>
                <transition
                  @enter="startTransition"
                  @after-enter="endTransition"
                  @before-leave="startTransition"
                  @after-leave="endTransition"
                >
                  <div
                    v-show="
                      isSubmenuOpen(groupIndex, index) &&
                      (isExpanded || isHovered || isMobileOpen)
                    "
                  >
                    <ul class="mt-2 space-y-1 ml-9">
                      <li v-for="subItem in item.subItems" :key="subItem.name">
                        <router-link
                          :to="subItem.path"
                          :class="[
                            'menu-dropdown-item',
                            {
                              'menu-dropdown-item-active': isActive(
                                subItem.path
                              ),
                              'menu-dropdown-item-inactive': !isActive(
                                subItem.path
                              ),
                            },
                          ]"
                        >
                          {{ subItem.name }}
                          <span class="flex items-center gap-1 ml-auto">
                            <span
                              v-if="subItem.new"
                              :class="[
                                'menu-dropdown-badge',
                                {
                                  'menu-dropdown-badge-active': isActive(
                                    subItem.path
                                  ),
                                  'menu-dropdown-badge-inactive': !isActive(
                                    subItem.path
                                  ),
                                },
                              ]"
                            >
                              new
                            </span>
                            <span
                              v-if="subItem.pro"
                              :class="[
                                'menu-dropdown-badge',
                                {
                                  'menu-dropdown-badge-active': isActive(
                                    subItem.path
                                  ),
                                  'menu-dropdown-badge-inactive': !isActive(
                                    subItem.path
                                  ),
                                },
                              ]"
                            >
                              pro
                            </span>
                          </span>
                        </router-link>
                      </li>
                    </ul>
                  </div>
                </transition>
              </li>
            </ul>
          </div>
        </div>
      </nav>
<!--      사이드바 위젯-->
<!--      <SidebarWidget v-if="isExpanded || isHovered || isMobileOpen" />-->
    </div>
  </aside>

</template>

<script setup>
import { computed } from "vue";
import { useRoute } from "vue-router";

import {
  LayoutDashboardIcon,
  ListIcon,
  TableIcon,
  RefreshIcon,
  TrashIconLg,
  SettingsIcon,
  UserCircleIcon,
  PieChartIcon,
  CalenderIcon,
  BarChartIcon
} from "../../icons";
import { useSidebar } from "@/composables/useSidebar";
import { useCan } from "@/composables/useAcl"
import {useAuthStore} from "@/stores/auth.js";

const route = useRoute();
const { can } = useCan();
const auth = useAuthStore();

const { isExpanded, isMobileOpen, isHovered, openSubmenu } = useSidebar();

const menuGroups = [
  {
    items: [
      {
        icon: LayoutDashboardIcon,
        name: "대시보드",
        path: "/",
        perm: 'menu.dashboard'
      },
      {
        icon: BarChartIcon,
        name: "매출현황",
        path: "/sales",
        perm: 'menu.sales'
      },
      {
        icon: PieChartIcon,
        name: "DB분배",
        path: "/db/allocate",
        perm: 'menu.db.allocate'
      },
      {
        icon: ListIcon,
        name: "센터DB",
        path: "/db/center",
        perm: 'menu.db.center'
      },
      {
        icon: TableIcon,
        name: "전체DB",
        path: "/db/all",
        perm: 'menu.db.all'
      },
      {
        icon: TrashIconLg,
        name: "중복DB",
        path: "/db/duplicate",
        perm: 'menu.db.duplicate'
      },
      {
        icon: RefreshIcon,
        name: "DB회수",
        path: "/db/revoke",
        perm: 'menu.db.revoke'
      },
      {
        icon: UserCircleIcon,
        name: "소속정보",
        path: "/info",
        perm: 'menu.info'
      },
      {
        icon: CalenderIcon,
        name: "내방일정",
        path: "/visit-calendar",
        perm: "menu.calendar"
      },
      {
        icon: SettingsIcon,
        name: "직원관리",
        path: "/user",
        perm: 'menu.user'
      },
      // 하위 메뉴 추가하고 싶은 경우
      // {
      //   name: "Pages",
      //   icon: PageIcon,
      //   subItems: [
      //     { name: "Black Page", path: "/blank", pro: false },
      //     { name: "404 Page", path: "/error-404", pro: false },
      //   ],
      // },
    ],
  },
];

// 권한 기반 필터링된 메뉴 제공
const visibleMenuGroups = computed(() => {
  return menuGroups
      .map(g => ({
        ...g,
        items: g.items
            .filter(it => can(it.perm)) // 항목 필터
            .map(it => it.subItems
                ? { ...it, subItems: it.subItems.filter(si => can(si.perm)) } // 서브항목 필터
                : it
            )
      }))
      .filter(g => g.items.length > 0) // 빈 그룹 제거
})

const isActive = (path) => route.path === path;

const toggleSubmenu = (groupIndex, itemIndex) => {
  const key = `${groupIndex}-${itemIndex}`;
  openSubmenu.value = openSubmenu.value === key ? null : key;
};

// 서브메뉴 활성화 여부
const isAnySubmenuRouteActive = computed(() => {
  return menuGroups.some((group) =>
    group.items.some(
      (item) =>
        item.subItems && item.subItems.some((subItem) => isActive(subItem.path))
    )
  );
});

const isSubmenuOpen = (groupIndex, itemIndex) => {
  const key = `${groupIndex}-${itemIndex}`;
  return (
    openSubmenu.value === key ||
    (isAnySubmenuRouteActive.value &&
      menuGroups[groupIndex].items[itemIndex].subItems?.some((subItem) =>
        isActive(subItem.path)
      ))
  );
};

const startTransition = (el) => {
  el.style.height = "auto";
  const height = el.scrollHeight;
  el.style.height = "0px";
  el.offsetHeight; // force reflow
  el.style.height = height + "px";
};

const endTransition = (el) => {
  el.style.height = "";
};
</script>
