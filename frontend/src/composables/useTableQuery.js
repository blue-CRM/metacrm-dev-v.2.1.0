// composables/useTableQuery.js
import { ref, watch } from 'vue'
import axios from '@/plugins/axios.js'

/**
 * 범용 테이블 쿼리 훅
 * - 단독 모드: 내부 state(data/page/totalPages/filters)만 반환
 * - 바인딩 모드: 기존 ref들(예: all.vue의 common/admin)을 받아 그 ref에 값을 채워넣음
 *
 * @param {Object} options
 *  - url:            API 엔드포인트 (필수)
 *  - pageSize:       기본 페이지 크기 (기본 10). 바인딩 모드에서 sizeRef 없으면 이 값 사용
 *  - defaultFilters: 훅 내부에서 관리할 기본 필터(페이지 전용/화면 전용)
 *  - externalFilters: 외부 전역 필터 객체(reactive) — ex) globalFilters
 *  - useExternalKeys: externalFilters를 어떤 파라미터로 보낼지 키 매핑
 *                     { from:'from', to:'to', category:'category', keyword:'keyword' }
 *  - paramNames:     공통 파라미터명 매핑 { page:'page', size:'size' ... }
 *  - extraParams:    () => ({}) 화면/권한별로 추가 파라미터 동적 주입
 *  - bind:           바인딩 모드용 { items, page, totalPages, size? } (모두 ref)
 *  - immediate:      생성 시 즉시 1회 fetch (기본 true)
 *  - debounce:       ms, 필터 변경시 디바운스 (기본 200)
 *  - mapper:         (res) => ({ items, totalPages, totalCount }) 응답 매퍼(선택)
 */
export function useTableQuery({
                                  url,
                                  pageSize = 100,
                                  defaultFilters = {},
                                  externalFilters = null,
                                  useExternalKeys = { from: 'from', to: 'to', category: 'category', keyword: 'keyword' },
                                  paramNames = { page: 'page', size: 'size' },
                                  extraParams = () => ({}),
                                  bind = null,
                                  immediate = true,
                                  debounce = 200,
                                  mapper = null
                              } = {}) {
    if (!url) throw new Error('useTableQuery: url is required')

    // ===== 상태 정의 (단독/바인딩을 모두 지원) =====
    // items/data
    const _items  = bind?.items      ?? ref([])
    // pagination
    const _page   = bind?.page       ?? ref(1)
    const _size   = bind?.size       ?? ref(pageSize)
    const _total  = bind?.totalPages ?? ref(1)

    const totalCount = ref(0)
    const loading    = ref(false)
    const error      = ref(null)

    // 내부(훅 전용) 필터
    const filters = ref({ ...defaultFilters })

    // ===== 유틸 =====
    const pickDefined = (obj) => {
        const out = {}
        for (const [k, v] of Object.entries(obj)) {
            if (v === null || v === undefined) continue
            if (typeof v === 'string' && v.trim() === '') continue
            out[k] = v
        }
        return out
    }

    const normalizeCategory = (cat) => {
        if (!cat) return undefined
        if (Array.isArray(cat)) return cat.length ? cat.join(',') : undefined
        return String(cat)
    }

    let timer = null
    let aborter = null

    function scheduleFetch() {
        clearTimeout(timer)
        timer = setTimeout(fetchData, debounce)
    }

    // ===== 핵심: fetch =====
    async function fetchData() {
        // 이전 요청 취소
        if (aborter) aborter.abort()
        aborter = new AbortController()

        loading.value = true
        error.value = null

        try {
            // 외부 전역 필터가 있다면 이름 매핑해서 병합
            const extern = externalFilters
                ? {
                    [useExternalKeys.from]:     externalFilters.dateFrom || undefined,
                    [useExternalKeys.to]:       externalFilters.dateTo   || undefined,
                    [useExternalKeys.category]: normalizeCategory(externalFilters.category),
                    [useExternalKeys.keyword]:  externalFilters.keyword?.trim() || undefined
                }
                : {}

            const paramsBase = {
                [paramNames.page]: _page.value,
                [paramNames.size]: _size.value,
                ...filters.value,
                ...extern,
                ...extraParams()
            }

            const params = pickDefined(paramsBase)

            const res = await axios.get(url, { params, signal: aborter.signal })

            // 응답 매핑
            if (typeof mapper === 'function') {
                const m = mapper(res)
                _items.value = m.items ?? []
                _total.value = m.totalPages ?? 1
                totalCount.value = m.totalCount ?? 0
            } else {
                // 기본 키명 가정: { items, totalPages, totalCount }
                _items.value = res.data?.items ?? []
                _total.value = res.data?.totalPages ?? 1
                totalCount.value = res.data?.totalCount ?? 0
            }
        } catch (e) {
            if (e.name !== 'CanceledError' && e.code !== 'ERR_CANCELED') {
                error.value = e
                console.error('useTableQuery fetch error:', e)
            }
        } finally {
            loading.value = false
        }
    }

    // 상위 N개 행 조회: 현재 필터/정렬 그대로, page=1·size=N. state는 건드리지 않고 items만 반환.
    // (1~N 일괄선택용 — 목록과 동일 조건의 위에서부터 N개)
    async function fetchTopN(n) {
        const num = Number(n)
        if (!Number.isInteger(num) || num < 1) return []
        const extern = externalFilters
            ? {
                [useExternalKeys.from]:     externalFilters.dateFrom || undefined,
                [useExternalKeys.to]:       externalFilters.dateTo   || undefined,
                [useExternalKeys.category]: normalizeCategory(externalFilters.category),
                [useExternalKeys.keyword]:  externalFilters.keyword?.trim() || undefined
            }
            : {}
        const params = pickDefined({
            [paramNames.page]: 1,
            [paramNames.size]: num,
            ...filters.value,
            ...extern,
            ...extraParams()
        })
        const res = await axios.get(url, { params })
        if (typeof mapper === 'function') return mapper(res).items ?? []
        return res.data?.items ?? []
    }

    // ===== watch: 페이지, 사이즈, 필터, 외부필터 =====
    watch(_page, () => scheduleFetch())
    watch(_size, () => { _page.value = 1; scheduleFetch() })
    watch(filters, () => { _page.value = 1; scheduleFetch() }, { deep: true })

    // 외부 전역 필터(externalFilters)가 존재할 경우 감시
    if (externalFilters) {
        watch(
            // - dateFrom, dateTo : 날짜 범위 필터
            // - category         : 카테고리 필터
            // - keyword          : 검색 키워드
            () => [externalFilters.dateFrom, externalFilters.dateTo, externalFilters.category, externalFilters.keyword],
            () => { _page.value = 1; scheduleFetch() },
            { deep: true }
        )
    }

    // 초기 로딩
    if (immediate) scheduleFetch()

    // ===== 외부에 노출하는 API =====
    function changePage(newPage) {
        if (newPage >= 1 && newPage <= (_total.value || 1)) {
            _page.value = newPage
        }
    }
    function setSize(newSize) {
        _size.value = Number(newSize) || pageSize
    }
    function setFilter(key, value) {
        // filters.value = { category: 'VIP', ... }
        filters.value[key] = value
    }
    function setFilters(partial = {}) {
        // filters.value = { category: 'VIP', status: 'ACTIVE', ... }
        Object.assign(filters.value, partial)
    }
    function resetFilters() {
        filters.value = { ...defaultFilters }
    }

    // 단독/바인딩 상관없이 동일한 인터페이스로 반환
    return {
        // state
        items: _items,
        page: _page,
        size: _size,
        totalPages: _total,
        totalCount,
        loading,
        error,
        filters,

        // actions
        fetchData,
        fetchTopN,
        changePage,
        setSize,
        setFilter,
        setFilters,
        resetFilters,
    }
}
