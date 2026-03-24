import { useAuthStore } from '@/stores/auth'

// 권한별 허용된 메뉴/기능 목록 정의
// menu 접두사 : 사이드바
const policy = {
    // 본사
    SUPERADMIN: [
        // 사이드바
        'menu.dashboard',    // 대시보드
        'menu.sales',        // 매출현황
        'menu.db.allocate',  // DB분배
        'menu.db.center',    // 센터DB
        'menu.db.all',       // 전체DB
        'menu.db.duplicate', // 중복DB
        'menu.db.revoke',    // DB회수
        'menu.info',         // 소속정보
        'menu.calendar',     // 내방일정
        'menu.user',         // 직원관리
    ],
    // 팀장
    MANAGER: [
        // 사이드바
        'menu.dashboard',    // 대시보드
        'menu.sales',        // 매출현황
        'menu.db.allocate',  // DB분배
        'menu.db.all',       // 전체DB
        'menu.db.duplicate', // 중복DB
        'menu.db.revoke',    // DB회수
        'menu.info',         // 소속정보
        'menu.calendar',     // 내방일정
    ],
    // 프로
    STAFF: [
        // 사이드바
        'menu.dashboard',    // 대시보드
        'menu.sales',        // 매출현황
        'menu.db.all',       // 전체DB
        'menu.db.duplicate', // 중복DB
        'menu.info',         // 소속정보
        'menu.calendar',     // 내방일정
    ],
    // 센터장
    CENTERHEAD: [
        'menu.dashboard',    // 대시보드
        'menu.sales',        // 매출현황
        'menu.db.center',    // 센터DB
        'menu.db.all',       // 전체DB
        'menu.db.duplicate', // 중복DB
        'menu.info',         // 소속정보
        'menu.calendar',     // 내방일정
    ],
    // 전문가
    EXPERT: [
        'menu.dashboard',    // 대시보드
        'menu.sales',        // 매출현황
        'menu.db.center',    // 센터DB
        'menu.db.all',       // 전체DB
        'menu.db.duplicate', // 중복DB
        'menu.info',         // 소속정보
        'menu.calendar',     // 내방일정
    ],
}

// 권한 체크용 훅 (컴포저블)
export function useCan() {
    // 현재 로그인된 유저의 role 가져오기
    const auth = useAuthStore()

    // 백엔드의 grants.perms → 실제 메뉴 권한으로 매핑
    const permToMenu = {
        allocate: ['menu.db.allocate', 'menu.db.revoke']
    }

    // 특정 권한 문자열(perm)을 체크하는 함수
    const can = (perm) => {
        // perm이 없으면(=null, undefined) 기본적으로 허용
        if (!perm) return true

        // 1. 현재 유저의 role에 맞는 디폴트 사이드바 권한 리스트 가져오기
        const base = policy[auth.grants.role] || []

        // 2. 유저 개인에게 동적으로 부과된 세부 권한 중 true 권한만 골라서 메뉴 목록으로 변환
        const extraPermKeys = Object.entries(auth.grants?.perms || {})
            .filter(([_, v]) => v) // true인 것만
            .flatMap(([k]) => permToMenu[k] || []) // 여러 개 매핑 허용

        // 3. 1번과 2번을 합친다
        const allowed = new Set([...base, ...extraPermKeys])

        // 4. 최종 리스트에 해당 권한이 포함돼 있으면 true 반환
        return allowed.has(perm)
    }

    // 다른 곳에서 can() 함수를 사용할 수 있게 반환
    return { can }
}
