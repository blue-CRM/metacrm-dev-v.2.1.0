/**
 * 같은 브라우저 내 메모 창 동기화 채널명(BroadcastChannel).
 * 메시지 타입: {type:'logout'}(팝업 닫기) / {type:'customer-updated', customerId}(헤더+채팅 재조회).
 */
export const MEMO_SYNC_CHANNEL = 'memo-sync'

/** 로그아웃 시 열린 메모 창들에 닫기 신호 전송(같은 브라우저). */
export function broadcastMemoLogout() {
  try {
    const ch = new BroadcastChannel(MEMO_SYNC_CHANNEL)
    ch.postMessage({ type: 'logout' })
    ch.close()
  } catch {
    /* BroadcastChannel 미지원 환경은 무시 */
  }
}

/**
 * 표에서 고객 정보(이름/상태/재콜시간/내방일정 등)를 수정한 뒤 호출.
 * 같은 브라우저에 그 고객 메모 팝업이 열려 있으면 헤더+채팅을 통째 재조회하게 한다.
 * (같은 브라우저 = 작성자 본인 한정. 다른 사용자/PC 실시간은 3차 WebSocket.)
 */
export function broadcastCustomerUpdated(customerId) {
  if (customerId == null) return
  try {
    const ch = new BroadcastChannel(MEMO_SYNC_CHANNEL)
    ch.postMessage({ type: 'customer-updated', customerId })
    ch.close()
  } catch {
    /* 미지원 환경 무시 */
  }
}

/**
 * 메모 채팅창을 브라우저 별도 창으로 여는 헬퍼.
 *
 * - 고객당 창 1개 보장: named target `memo_<id>` 재사용(같은 고객 재클릭 = 기존 창 재사용 + focus,
 *   다른 고객 = 새 창). 팝업↔팝업 동기화 불필요.
 * - ⚠️ 반드시 클릭 핸들러에서 **동기 호출**(브라우저 팝업 차단 회피). 기존 onVisitSchedule(window.open) 패턴과 동일.
 * - ⚠️ 이 코드베이스는 기존에 `window.open(..., '_blank')`만 썼음(named target·focus 선례 없음) → 재클릭 포커스는 직접 테스트.
 *
 * @param {number|string} customerId
 * @param {{ view?: boolean }} [opts]  view=true → `?view=1` 읽기전용(입력창·수정·삭제 숨김). 분배 등에서 사용.
 * @returns {Window|null}
 */
export function openMemoWindow(customerId, opts = {}) {
  if (customerId == null) return null
  const query = opts.view ? '?view=1' : ''
  const features = 'width=560,height=760,menubar=no,toolbar=no,location=no,status=no,resizable=yes,scrollbars=yes'
  const win = window.open(`/memo/${customerId}${query}`, `memo_${customerId}`, features)
  // 이미 열린 창을 재사용한 경우 브라우저가 자동 포커스 안 할 수 있어 명시 호출
  win?.focus?.()
  return win
}
