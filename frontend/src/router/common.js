export default [
    { path: '/login', component: () => import('@/views/auth/LoginView.vue') },
    { path: '/signup', component: () => import('@/views/auth/SignupView.vue') },
    { path: '/find-password', component: () => import('@/views/auth/FindPassword.vue') },
    // 메모 채팅 별도 창 (모든 역할 조회 가능, 쓰기 권한은 창 내부서 판정)
    {
        path: '/memo/:customerId',
        component: () => import('@/views/Pages/Customer/MemoWindowPage.vue'),
        meta: { requiresAuth: true, role: ['SUPERADMIN','MANAGER','STAFF','CENTERHEAD','EXPERT'] }
    },
    // 로그인이 필요한 페이지
    // 대시보드
    {
        path: '/',
        component: () => import('@/views/Dashboard.vue'),
        meta: { requiresAuth: true, role: ['SUPERADMIN','MANAGER','STAFF','CENTERHEAD','EXPERT'] }
    },
    // 내정보 수정
    {
        path: '/profile',
        component: () => import('@/views/Pages/MyInfo.vue'),
        meta: { requiresAuth: true, role: ['SUPERADMIN','MANAGER','STAFF','CENTERHEAD','EXPERT'] }
    },
    // 매출현황
    {
        path: '/sales',
        component: () => import('@/views/Pages/SalesPage.vue'),
        meta: { requiresAuth: true, role: ['SUPERADMIN','MANAGER','STAFF','CENTERHEAD','EXPERT'] }
    },
    // DB분배
    {
        path: '/db/allocate',
        component: () => import('@/views/Pages/Customer/Allocate.vue'),
        meta: { requiresAuth: true, role: ['SUPERADMIN','MANAGER','CENTERHEAD','EXPERT'] }
    },
    // 센터DB
    {
        path: '/db/center',
        component: () => import('@/views/Pages/Customer/Center.vue'),
        meta: { requiresAuth: true, role: ['SUPERADMIN','CENTERHEAD','EXPERT'] }
    },
    // 전체DB
    {
        path: '/db/all',
        component: () => import('@/views/Pages/Customer/All.vue'),
        meta: { requiresAuth: true, role: ['SUPERADMIN','MANAGER','STAFF','CENTERHEAD','EXPERT'] }
    },
    // 중복DB
    {
        path: '/db/duplicate',
        component: () => import('@/views/Pages/Customer/Duplicate.vue'),
        meta: { requiresAuth: true, role: ['SUPERADMIN','MANAGER','STAFF','CENTERHEAD','EXPERT'] }
    },
    // DB회수
    {
        path: '/db/revoke',
        component: () => import('@/views/Pages/Customer/Revoke.vue'),
        meta: { requiresAuth: true, role: ['SUPERADMIN','MANAGER','CENTERHEAD','EXPERT'] }
    },
    // 소속정보
    {
        path: '/info',
        component: () => import('@/views/Pages/Info.vue'),
        meta: { requiresAuth: true, role: ['SUPERADMIN','MANAGER','STAFF','CENTERHEAD','EXPERT'] }
    },
    // 내방일정
    {
        path: '/visit-calendar',
        component: () => import('@/views/Pages/VisitCalendar.vue'),
        meta: { requiresAuth: true, role: ['SUPERADMIN','MANAGER','STAFF','CENTERHEAD','EXPERT'] }
    },
    // 직원관리
    {
        path: '/user',
        component: () => import('@/views/Pages/User.vue'),
        meta: { requiresAuth: true, role: ['SUPERADMIN'] }
    },
]