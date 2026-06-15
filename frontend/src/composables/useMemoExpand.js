/**
 * 읽기전용 메모 셀의 "한 번에 하나만 펼침"을 구독 없이 관리한다.
 *
 * ⚠️ 왜 reactive ref를 안 쓰는가:
 *   전역 reactive 값을 100개 셀이 모두 구독하면, 메모 하나를 열 때 openKey가 바뀌면서
 *   100개 MemoReadCell이 전부 리렌더된다(화면 변화 없는 98개까지). 100개씩 보기에서
 *   클릭당 ~130ms 스크립트 비용의 원인이 바로 이것이다.
 *
 * ✅ 대신: 현재 열린 셀의 setter만 들고 있다가, 전환 시 "이전 셀 닫기 + 새 셀 열기"
 *   딱 2개 셀만 직접 토글한다. 나머지 98개는 건드리지 않는다 → 클릭당 2개만 갱신.
 */
let openId = null
let openSetter = null

/**
 * 셀 클릭 토글.
 * @param {string} id      셀 고유 키(행마다 유일)
 * @param {(v:boolean)=>void} setter  이 셀의 펼침상태를 바꾸는 함수(보통 ref 설정)
 */
export function toggleMemo(id, setter) {
  // 같은 셀 다시 클릭 → 닫기
  if (openId === id) {
    setter(false)
    openId = null
    openSetter = null
    return
  }
  // 다른 셀이 열려 있으면 그 셀만 닫는다(1개 갱신)
  if (openSetter) openSetter(false)
  // 이 셀을 연다(1개 갱신)
  openId = id
  openSetter = setter
  setter(true)
}

/** 표 재로딩/페이지 이동 등에서 열린 메모를 닫는다. */
export function resetMemo() {
  if (openSetter) openSetter(false)
  openId = null
  openSetter = null
}

/** 열려 있던 셀이 언마운트될 때 레지스트리 정리(스테일 setter 방지). */
export function clearMemoIf(id) {
  if (openId === id) {
    openId = null
    openSetter = null
  }
}
