import { ref, reactive } from 'vue'
import api from '@/plugins/axios'
import { MEMO_SYNC_CHANNEL } from '@/composables/useMemoWindow'

/**
 * 메모 채팅 상태 + 액션 컴포저블 (실제 API 연동).
 *
 * 서버(MemoController)가 권한을 전부 계산해 내려줌:
 *  - 응답 chats[].editable : 이 사용자가 그 채팅을 수정/삭제 가능한지
 *  - 응답 canWrite          : 이 고객에 새 채팅 작성 가능한지(입력창 노출 여부)
 * 따라서 프런트는 권한을 다시 계산하지 않는다.
 *
 * @param {number|string} customerId
 * @param {{ view?: boolean }} opts  view=true → 읽기전용 UI(분배창 ?view=1): 입력창·수정·삭제 숨김
 */
export function useMemoChats(customerId, opts = {}) {
  const view = ref(opts.view ?? false)

  // 헤더(읽기전용 표시용). 서버 MemoDetailDto → 프런트 필드 매핑.
  const customer = reactive({
    customerId: customerId ?? null,
    name: '',
    phone: '',
    status: '',
    appointmentAt: null,   // ← promiseTime
    roomName: '',          // ← visitRoom (내방 회의실명)
    assignedStaffName: '', // ← staff (현재 담당 프로)
  })

  const chats = ref([])      // 서버가 editable 포함해 내려줌
  const canWrite = ref(false)
  const loading = ref(false)
  const error = ref(null)    // 'GONE'(권한없음/없는고객) | 'NETWORK' | null

  const editingId = ref(null)
  const editText = ref('')

  /* ──────────── 로드 ──────────── */
  async function load() {
    loading.value = true
    error.value = null
    try {
      const { data } = await api.get(`/api/work/db/memo/${customerId}`)
      customer.name = data.name ?? ''
      customer.phone = data.phone ?? ''
      customer.status = data.status ?? ''
      customer.appointmentAt = data.promiseTime ?? null
      customer.roomName = data.visitRoom ?? ''
      customer.assignedStaffName = data.staff ?? ''
      canWrite.value = !!data.canWrite
      chats.value = data.chats ?? []
    } catch (e) {
      const st = e?.response?.status
      error.value = st === 410 || st === 404 ? 'GONE' : 'NETWORK'
      chats.value = []
      canWrite.value = false
    } finally {
      loading.value = false
    }
  }

  /** 새로고침 — 헤더+채팅 통째 재조회 */
  function refresh() {
    load()
  }

  /* 표의 '최근메모' 미리보기를 실시간 갱신하기 위한 브로드캐스트.
     백엔드 미리보기 기준과 동일하게 STATUS 채팅은 제외한 '최신 채팅' 내용을 보낸다. */
  function latestMemoContent() {
    for (let i = chats.value.length - 1; i >= 0; i--) {
      const c = chats.value[i]
      if (c && c.chatType !== 'STATUS') return c.content ?? ''
    }
    return ''
  }
  function broadcastLatestMemo() {
    try {
      const ch = new BroadcastChannel(MEMO_SYNC_CHANNEL)
      ch.postMessage({ type: 'memo-latest', customerId: String(customerId), content: latestMemoContent() })
      ch.close()
    } catch {
      /* BroadcastChannel 미지원 환경 무시 */
    }
  }

  /* ──────────── 쓰기 액션 ──────────── */
  async function send(text) {
    const t = (text || '').trim()
    if (!t || !canWrite.value || view.value) return
    try {
      const { data } = await api.post(`/api/work/db/memo/${customerId}/chats`, { content: t })
      chats.value = [...chats.value, data]
      broadcastLatestMemo()
    } catch (e) {
      handleWriteError(e)
    }
  }

  function startEdit(chat) {
    editingId.value = chat.chatId
    editText.value = chat.content
  }

  function cancelEdit() {
    editingId.value = null
    editText.value = ''
  }

  async function saveEdit() {
    const t = (editText.value || '').trim()
    if (!t || editingId.value == null) return
    const targetId = editingId.value
    try {
      const { data } = await api.patch(`/api/work/db/memo/chats/${targetId}`, { content: t })
      const idx = chats.value.findIndex((c) => c.chatId === targetId)
      if (idx !== -1) chats.value.splice(idx, 1, data)
      cancelEdit()
      broadcastLatestMemo()
    } catch (e) {
      handleWriteError(e)
    }
  }

  async function remove(chatId) {
    try {
      await api.delete(`/api/work/db/memo/chats/${chatId}`)
      chats.value = chats.value.filter((c) => c.chatId !== chatId)
      broadcastLatestMemo()
    } catch (e) {
      handleWriteError(e)
    }
  }

  function handleWriteError(e) {
    const st = e?.response?.status
    if (st === 403) alert('권한이 없습니다.')
    else if (st === 400) alert('내용을 입력하세요.')
    else if (st === 410 || st === 404) {
      alert('대상을 찾을 수 없습니다. 새로고침합니다.')
      refresh()
    } else {
      alert('처리 중 오류가 발생했습니다.')
    }
  }

  load()

  return {
    customer,
    chats,
    canWrite,
    view,
    loading,
    error,
    editingId,
    editText,
    send,
    startEdit,
    cancelEdit,
    saveEdit,
    remove,
    refresh,
  }
}
