/**
 * 메모 채팅 포맷/그룹/검색/색상 유틸 (순수 함수 모음)
 *
 * ⚠️ 이 프로젝트엔 `utils/` 폴더가 없어 비-use 유틸도 `composables/`에 둔다(선례: globalFilters.js).
 * MEMO.vue가 삭제되며 그 안의 포맷 함수(toKRReservationString 등)는 여기로 흡수된다.
 *
 * 화면 표시 포맷 전용(순수 함수). 데이터 조회는 useMemoChats가 담당.
 */

/* ──────────────── 시간/날짜 포맷 ──────────────── */

/** "4/15" — 월/일만 */
export function fmtMonthDay(iso) {
  if (!iso) return ''
  const d = new Date(iso)
  return `${d.getMonth() + 1}/${d.getDate()}`
}

/** "오전 8:00" — 시각만 */
export function fmtTimeOnly(iso) {
  if (!iso) return ''
  const d = new Date(iso)
  let h = d.getHours()
  const min = d.getMinutes()
  const ampm = h < 12 ? '오전' : '오후'
  if (h === 0) h = 12
  else if (h > 12) h -= 12
  return `${ampm} ${h}:${String(min).padStart(2, '0')}`
}

/** "4/15 오전 8:00" — 날짜 + 시각(말풍선 오른쪽 시간영역용) */
export function fmtDateTime(iso) {
  if (!iso) return ''
  return `${fmtMonthDay(iso)} ${fmtTimeOnly(iso)}`
}

/** "2026년 4월 15일 수요일" — (미사용/예비) */
export function fmtDateLong(iso) {
  if (!iso) return ''
  const d = new Date(iso)
  const days = ['일', '월', '화', '수', '목', '금', '토']
  return `${d.getFullYear()}년 ${d.getMonth() + 1}월 ${d.getDate()}일 ${days[d.getDay()]}요일`
}

/** "5/4 오후 2시" / "5/4 오후 2시 30분" — 약속 일시 */
export function fmtAppointment(iso) {
  if (!iso) return ''
  const d = new Date(iso)
  const m = d.getMonth() + 1
  const day = d.getDate()
  const h = d.getHours()
  const min = d.getMinutes()
  const ampm = h < 12 ? '오전' : '오후'
  const h12 = h % 12 === 0 ? 12 : h % 12
  const minStr = min === 0 ? '' : ` ${min}분`
  return `${m}/${day} ${ampm} ${h12}시${minStr}`
}

/** YYYY-MM-DD 날짜 키 */
function dateKey(iso) {
  return (iso || '').slice(0, 10)
}

/* ──────────────── 채팅 그룹화 ──────────────── */

/**
 * 같은 작성자(+같은 타입)가 5분 안에 연속으로 남긴 채팅을 한 그룹으로 묶는다.
 * 날짜가 바뀌면 새 그룹.
 *
 * 입력: createdAt 오름차순 채팅 배열
 * 출력: [{ authorName, chatType, isSystem, createdAt, items: [...] }]
 *
 * ※ 그룹 헤더엔 아바타+이름만 표시(타임스탬프 X). 타임스탬프는 각 말풍선 오른쪽(MemoChatItem).
 */
export function groupChats(chats) {
  const groups = []
  for (const c of chats) {
    const last = groups[groups.length - 1]
    const sys = isSystemType(c.chatType)
    const sameDate = last && dateKey(last.createdAt) === dateKey(c.createdAt)
    const sameAuthor =
      last && last.authorName === c.authorName && last.isSystem === sys
    const closeTime =
      last && new Date(c.createdAt) - new Date(last.createdAt) < 5 * 60 * 1000
    if (last && sameDate && sameAuthor && closeTime) {
      last.items.push(c)
      last.createdAt = c.createdAt // 그룹 마지막 시각으로 갱신
    } else {
      groups.push({
        authorName: c.authorName,
        chatType: c.chatType,
        isSystem: sys,
        createdAt: c.createdAt,
        items: [c],
      })
    }
  }
  return groups
}

/** SYSTEM(마이그레이션·시트 종목명) = 작성자 없이 '시스템'으로 표시 */
export function isSystemType(chatType) {
  return chatType === 'SYSTEM'
}

/** STATUS(상태변경 자동) = 수정자 이름은 그대로 보이되, 말풍선만 강조 배경으로 구분 */
export function isStatusType(chatType) {
  return chatType === 'STATUS'
}

/* ──────────────── 검색(프런트 필터) ──────────────── */

/**
 * 날짜 매칭 — query는 "4/15", "2026-05-01" 등 substring 매칭
 */
export function matchDate(chat, dateQuery) {
  if (!dateQuery) return true
  // UI 전반이 createdAt 기준 → 날짜 검색도 작성시각(createdAt) 기준. 수정시각은 기록만 하고 미표시.
  const ts = chat.createdAt
  if (!ts) return false
  const d = new Date(ts)
  const m = d.getMonth() + 1
  const day = d.getDate()
  const y = d.getFullYear()
  if (/^\d{4}-\d{2}-\d{2}$/.test(dateQuery)) {
    return ts.slice(0, 10) === dateQuery
  }
  const p2 = (n) => String(n).padStart(2, '0')
  const candidates = [
    `${m}/${day}`,
    `${m}-${day}`,
    `${p2(m)}/${p2(day)}`,
    `${p2(m)}-${p2(day)}`,
    `${y}-${p2(m)}-${p2(day)}`,
    ts.slice(0, 10),
  ]
  return candidates.some((c) => c.includes(dateQuery))
}

/**
 * Split 검색(텍스트 + 날짜 픽커) 적용. 메모창은 Split 한 가지만 사용.
 * - text: 내용·작성자명 substring
 * - dateSearch: 날짜 매칭
 */
export function applySearch(chats, { text = '', dateSearch = '' } = {}) {
  let r = chats
  if (text.trim()) {
    const q = text.toLowerCase()
    r = r.filter(
      (c) =>
        (c.content || '').toLowerCase().includes(q) ||
        (c.authorName || '').toLowerCase().includes(q),
    )
  }
  if (dateSearch.trim()) {
    r = r.filter((c) => matchDate(c, dateSearch))
  }
  return r
}

/* ──────────────── 상태배지 색 ──────────────── */

/**
 * 상태 → Tailwind 클래스. 표(PsnsTable.badgeClass:762)와 100% 동일 색으로 화면 간 일관성 유지.
 * (의뢰인 결정: 목업 자체 파스텔이 아니라 표 색상 사용)
 */
export function statusBadgeColor(value) {
  switch (value) {
    case '본사':
    case '관리자':
    case '탈퇴':
    case '거절':
    case '회수':
      return 'bg-[#F8D7DA] text-[#B23A48] dark:bg-[#4a1f28] dark:text-[#f7a8b4]'
    case '프로':
    case '신규':
    case '승인':
    case '최초':
      return 'bg-[#D6E4FF] text-[#1E40AF] dark:bg-[#1d3a6b] dark:text-[#9cc4ff]'
    case '팀장':
    case '유효':
    case '재콜':
      return 'bg-[#D1F2D6] text-[#2F855A] dark:bg-[#1c4636] dark:text-[#7fd6a6]'
    case '센터장':
    case '부재1':
    case '부재2':
    case '부재3':
    case '입장':
    case '가입':
    case '대기':
    case '미할당':
      return 'bg-[#FFF3CD] text-[#B7791F] dark:bg-[#4a3a1a] dark:text-[#e8c87a]'
    case '자연풀':
    case '공개':
      return 'bg-[#D0F0F0] text-[#00796B] dark:bg-[#16403c] dark:text-[#79d4c4]'
    case '전문가':
    case '카피':
    case '허용':
      return 'bg-[#E0D7F8] text-[#5B4B9A] dark:bg-[#332a5c] dark:text-[#c4b6f0]'
    case '가망':
    case '미지정':
      return 'bg-[#F4E1CC] text-[#944E2A] dark:bg-[#4a3320] dark:text-[#f0bf94]'
    case '내방':
      return 'bg-[#FCE7F3] text-[#C026D3] dark:bg-[#4a1f3a] dark:text-[#f3a8d4]'
    case '':
      return 'bg-[#FFE7D1] text-[#9A3412] dark:bg-[#4a2f1a] dark:text-[#fdba74]'
    default:
      return 'bg-[#E5E7EB] text-[#4B5563] dark:bg-[#233152] dark:text-[#b9c8e6]'
  }
}

/** 약속칩 노출 대상 상태 */
export const NEEDS_APPOINTMENT = new Set(['재콜', '내방'])

/* ──────────────── 이니셜 아바타 ──────────────── */

const AVATAR_COLORS = [
  '#8b7cf6', '#3b82f6', '#22c55e', '#fb923c', '#ec4899', '#06b6d4',
]

/** 이름 → 아바타 배경색(첫 글자 코드 기반) */
export function avatarColor(name) {
  const idx = (name?.charCodeAt(0) || 0) % AVATAR_COLORS.length
  return AVATAR_COLORS[idx]
}

/** 이름 → 이니셜(첫 글자) */
export function avatarInitial(name) {
  return name?.[0] || '?'
}
