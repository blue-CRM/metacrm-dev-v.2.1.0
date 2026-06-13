# 메모 채팅화 개편 — 작업 체크리스트 (검수본)

> 단일 `customers.customer_memo` TEXT → 시간순 **채팅 메시지(`memo_chats`)** 로 전환.
> 메모가 쓰이는 메뉴: **전체**(아이콘→별도 브라우저 창), **분배(Allocate)**(아이콘→읽기전용 창 `?view=1`), **회수·센터**(셀 인라인 펼치기, 읽기전용). 중복은 `duplicate_memo` 그대로 유지(채팅화 X).
> **개발 순서: 프런트 먼저 → 화면 확정 후 백엔드.**

## 확정 결정 요약
- 신규 테이블 **`memo_chats`**. 삭제 감사 컬럼 포함.
- 삭제 = **소프트 삭제**(`is_deleted=1`) + `deleted_by`, `deleted_at` 기록.
- **이름 표시**: **USER=작성자명(users JOIN, 이름변경 자동반영) / SYSTEM·STATUS='시스템'**(고객이름 아님 — 환원, customers JOIN 불필요). 고객 이름 변경은 자동채팅 안 만들고 헤더 표시만 자동 갱신.
- 수정·삭제: **USER 채팅** = 메모 쓰기권한 보유자(SUPERADMIN 전체 / MANAGER 자기팀 / STAFF 본인 보유건, CENTERHEAD·EXPERT 읽기전용). **SYSTEM·STATUS 채팅** = **총관리자(`is_super='Y'`)만** 수정·삭제 가능(그 외 전원 보호).
- **수정 시각 기록**(`updated_at`, `updated_by`).
- **말풍선 오른쪽 시간 영역** 배치: `[말풍선] [시간영역] [hover ✏️수정·✕삭제]`
  - 최초 업로드 → **1줄**: `created_at`
  - 수정됨 → **2줄 세로 스택**: 윗줄 `updated_at` 시간 / 아랫줄 **"수정됨"** (created_at 자리를 updated_at이 대체, hover 아님)
  - hover 시 ✏️수정·✕삭제 버튼은 **시간영역의 오른쪽**에 노출(editable일 때만)
- **[2차] 모든 상태 변경 → SYSTEM 채팅 자동**: `customers` **`AFTER UPDATE` DB 트리거** 하나로(앱 수동 훅 없음). content `상태 변경: A → B`, **실행자 이름 없음**(author_user_id=NULL → '시스템'). **이관 2건 허용**. no-op(`OLD<>NEW`)·bulk(row단위)·같은TX 자동. 표·회수·분배·이관·autoRecall·미래 코드까지 자동 포착. **1차엔 없음.**
- 시트 동기화 신규 고객의 종목명(F+B) → `시스템` 최초 SYSTEM 채팅.
- 모달 = **브라우저 별도 창**(`window.open`, 라우트 `/memo/:customerId`). 순수 채팅 + 읽기전용 헤더 + 검색 UI(프런트만).
- **동기화(단계별)**: 1차=**새로고침 버튼만** / 2차=**BroadcastChannel**(내 브라우저 표→팝업, 리스닝 시 `GET /memo/{id}` 전체 재조회) / 3차=**WebSocket**(다른 사용자/PC 실시간).
- 기존 상태/트뷰/텔레그램/체크박스/매출 입력은 창에서 제거(표에서 이미 처리됨).
- UI 변형: 헤더 **H1**, 검색 **Split**. 타임스탬프는 **각 말풍선 오른쪽**(이미지2).
- 회수·**센터** 메모 칸 = **인라인 펼치기(읽기 전용)**, 미리보기 = **최근 채팅(STATUS만 제외)** — 마이그레이션 메모·종목명(SYSTEM)은 보임, 상태변경(STATUS)만 숨김. 중복은 `duplicate_memo` 유지.
- 디자인 원본: `C:\Users\qkrwm\Desktop\고객 메모창 (단일파일).html` (React 목업 → Vue 포팅).

---

## 개발 단계 분리 (1차 / 2차 / 3차) — 의뢰인 요청

| 차수 | 범위 | 해당 Phase |
|------|------|-----------|
| **1차 (MVP)** | memo_chats DB + 기존 메모 마이그레이션 + 시트 최초 종목명 채팅 / **채팅 타이핑(작성·수정·삭제) + 읽기** / 별도 창 + **새로고침 버튼만** (자동 상태채팅 ❌, 리스닝 ❌) | Phase 0·1·2(BroadcastChannel 제외, 새로고침만)·3·4·6·7·9(**읽기 repoint·쓰기 중단까지만. `customer_memo` 컬럼은 1차에선 보존**) |
| **2차** | **상태변경 자동채팅 = DB 트리거**(이관 2건, '시스템') + **BroadcastChannel 리스닝**(내 브라우저 표→팝업) + **`customer_memo` 컬럼 DROP**(1차 운영 안정 확인 후) | Phase 5(트리거) + Phase 2의 BroadcastChannel + Phase 9의 `ALTER ... DROP` |
| **3차** | **WebSocket** cross-user 실시간(다른 사용자/PC 변경도) (+ 선택: 검색 백엔드 연동) | Phase 10 |

> 1차엔 상태변경이 채팅을 안 만드니 **푸시할 이벤트가 없음 → 리스너 불필요, 새로고침 버튼이면 충분**. 트뷰/텔레그램 등은 폐기, 분배 랜덤·통계 등은 이 프로젝트 밖 별개.

---

## Phase 0 · 착수 준비 ✅ 완료
- [x] 브랜치 생성: `feature/memo-chat` (main 분기 완료)
- [x] DB 백업 완료: `D:\Program\java\blue\backup_meta26_20260607.sql` (meta26 **전체** 스냅샷, `--single-transaction`, repo 밖)

---

## Phase 1 · 프런트 (목업) — 화면 먼저 확정 ★ 검수 포인트 ✅ 검수 통과

대상: `frontend/src`. 프로토타입(React) → Vue 3 + Tailwind 포팅. **목업 데이터로 화면부터 완성/확정.**

> **✅ 2026-06-08 의뢰인 검수 통과.** 확정 사항:
> - 상태배지 = **표(PsnsTable) 색상**(목업 파스텔 아님) + **`text-theme-xs`(12px)·`font-medium`** 까지 표와 픽셀 동일(처음엔 11px·semibold라 미묘하게 달랐음 → 수정).
> - **날짜 구분선 없음**(의뢰인 결정). 대신 **말풍선 오른쪽 시간영역에 날짜+시각** 표시(`4/15 오전 8:00`). 수정 시 윗줄=수정시각(날짜+시각), 아랫줄="수정됨".
> - 담당자 헤더 배치 = **전화번호 옆 `담당 OOO`**(미배정이면 숨김).
> - 검수용 임시 파일(**Phase 2서 제거**): `views/Pages/Customer/MemoPreview.vue` + `router/common.js`의 `/memo-preview`(무인증) 라우트. 권한·상태·약속·읽기전용 토글 포함.
> - 디코드한 React 목업 소스로 충실 포팅(검색 Split, 5분 그룹, 인라인 수정, 이니셜 아바타, 시스템 ⓘ 아바타).

> **파일 최소화**: 헤더/검색을 H1·Split로 확정 → 변형 추상화 불필요. MemoWindow 하나에 헤더·검색·바디·입력창 인라인. "메시지 1건"만 별도(재사용·v-for·편집상태). 새 파일 **4개**.

- [x] **유틸** `composables/memoFormat.js` (⚠️ 이 프로젝트엔 `utils/` 폴더 없음 — **비-`use` 유틸 파일도 `composables/`에 두는 선례 = `globalFilters.js`**. ※단 globalFilters.js는 reactive 상태+세터라 "순수함수 모음"은 아님 — 네이밍 선례로만 인용. 기존 포맷 함수 `MEMO.vue:557 toKRReservationString`은 MEMO.vue와 함께 삭제되므로 여기로 흡수) — `fmtMonthDay`/`fmtTimeOnly`/`fmtAppointment`, `groupChats`(같은 작성자 5분 묶음), `applySearch`/`matchDate`, `statusBadgeColor`(상태→색)
  - ⚠️ 상태배지 색: `ui/Badge.vue`는 **데모 전용 죽은 컴포넌트**(`views/UiElements/Badges.vue`에서만 import·라우팅 없음, 실로직 사용 0건)이고 의미토큰 7색뿐이라 표 색(재콜·내방·부재 등 세분 hex)을 **재현 불가** → **재사용 대상 아님**. 헤더 배지는 **`components/tables/basic-tables/PsnsTable.vue:762 badgeClass(value)`의 hex 매핑을 추출·공유**(또는 Tailwind 클래스 직접)해 표와 동일 색. (※ PsnsTable 실제 경로 = `components/tables/basic-tables/`, `components/customer/` 아님)
- [x] **컴포저블** `composables/useMemoChats.js` — 채팅 상태·액션(추가/수정/삭제). **목업 데이터 내장**(이후 Phase 7서 실제 API로 교체), 별도 mocks 파일 없음. (검수용 주입 옵션 `forceError`(GONE)·`customerOverride`(상태/약속) 포함 — Phase 7서 제거)
- [x] **`components/memo/MemoWindow.vue`** — 한 SFC에 인라인:
  - 헤더(H1): 이름 · 전화 · **담당자 = 전화 옆 `담당 OOO`(미배정이면 숨김)** · 우측 상태배지+약속칩(읽기전용) — **상태배지는 `statusBadgeColor`(=`PsnsTable.badgeClass` 복제) + `text-theme-xs`·`font-medium`로 표와 동일**(line 53·68; `ui/Badge.vue` 미사용). 약속칩 = **재콜·내방** 상태에서: 약속시간 있으면 표시(**내방이면 회의실명**도), **약속시간 없으면 "일정없음"**. ✅확정
  - 검색(Split): 텍스트("내용·이름으로 검색") + 날짜 픽커 — **프런트 필터만**
  - 바디: 스크롤(하단 자동), `groupChats` v-for, 작성자 그룹헤더(이니셜 아바타+이름, **타임스탬프는 여기 아님**)
  - 입력창: textarea **Enter 전송**(Shift+Enter 줄바꿈, IME 가드 — ⚠️ 코드베이스에 `isComposing`/composition 가드 선례 0건 → 직접 구현) + "전송". **`canWrite && !view`일 때만 노출**(CENTERHEAD·EXPERT 등 작성권한 없으면 숨김)
  - **view(읽기전용) 모드**(`?view=1`): 입력창·✏️수정·✕삭제 전부 숨김 (Allocate 등에서 사용)
  - 헤더에 **새로고침 버튼**: 현재 고객 채팅 재조회(부모서 상태변경 등 발생 시 수동 갱신 백업)
  - **빈/오류 상태**: 채팅 0건이면 빈 화면 안내. `getMemo`가 권한거부(GONE)/없는 고객이면 "권한 없음 / 없는 고객" 표시(STAFF가 남의 `/memo/:id` 직접 접근 등 — 서버가 막음). 비로그인 시 가드가 `/login`
- [x] **`components/memo/MemoChatItem.vue`** — 메시지 1건(v-for 단위, 회수 펼치기서 읽기전용 재사용)
  - **USER vs SYSTEM·STATUS**: USER=아바타+작성자명 / **SYSTEM·STATUS=ⓘ"시스템"**. **수정·삭제는 `editable`일 때만** — USER=작성권한자(+SUPERADMIN 전체) / **SYSTEM·STATUS=총관리자만**(서버 editable로 제어)
  - 보기/인라인수정 모드 전환(Esc 취소·저장)
  - 배치 `[말풍선] [시간영역] [hover ✏️✕]`. 시간영역: 최초=1줄(**날짜+시각** `4/15 오전 8:00`) / 수정=2줄 세로(윗줄 수정 날짜+시각, 아랫줄 "수정됨"). hover ✏️수정·✕삭제는 시간영역 오른쪽, editable일 때만
  - 이니셜 아바타는 인라인 신규 구현 — 기존 `ui/Avatar.vue`는 **데모 전용 죽은 컴포넌트**(UiElements/Avatars.vue만 사용)+이미지형이라 부적합. **코드베이스에 이니셜(첫글자) 아바타 자체가 없음**(헤더 `UserMenu`·사이드바도 이름 텍스트 직접 표시) → 재사용할 기존 것 없음, 새로 만든다
- [x] ⚠️ `components/ui/Badge.vue`·`components/ui/Avatar.vue`는 **둘 다 데모 전용 죽은 컴포넌트**(`views/UiElements/*`에서만 import, 실화면 사용 0건) → **재사용 안 함**. 상태배지 색=`statusBadgeColor`(PsnsTable.badgeClass 복제), 아바타=이니셜 인라인 신규(MemoChatItem/MemoWindow)
- [x] **→ 사용자 화면 검수 ✅ 통과(2026-06-08)**

---

## Phase 2 · 프런트 — 별도 창 + 메뉴 연결 (목업 유지) ◑ All/Allocate 완료, Revoke/Center는 Phase 7로

> **2026-06-08 진행**: 라우트·창페이지·헬퍼·All·Allocate **완료**(빌드 통과). **Revoke/Center 인라인 펼치기는 Phase 7로 이동** — 멀티행 표라 행마다 "그 고객의 채팅"이 필요한데 Phase 2 목업은 고객 1명뿐이라 의미 있는 구현 불가. 플랜 자체도 "펼칠 때 `GET /memo/{id}` 지연 로드"라 백엔드 채팅 API 선행 필요. **1차에선 `customer_memo` 보존되므로 Revoke/Center 메모 셀(`type:'text'`, `row.memo`)은 현행 그대로 정상 작동** → Phase 7서 채팅 인라인으로 교체.

- [x] **라우트** `router/common.js` 에 `/memo/:customerId` 추가. (`MemoWindowPage.vue` = bare, AdminLayout 미사용) ⚠️ 이 프로젝트는 **레이아웃을 라우트 meta가 아니라 각 뷰 SFC 안에서** `<AdminLayout>`으로 적용함(All.vue:2 등) → MemoWindow.vue가 **`<AdminLayout>`을 안 쓰면 곧 bare**(사이드바 없음). `meta.requiresAuth=true` + **`meta.role=[SUPERADMIN,MANAGER,STAFF,CENTERHEAD,EXPERT]`**(조회 전원 허용, 가드가 role 배열 체크)
  - 인증: 새 창 → `router.beforeEach`(index.js:25)의 `if(!auth.accessToken)` 분기(index.js:39)에서 `auth.initialize()`(refresh 쿠키)로 자동 로그인 → 토큰 전달 불필요 (main.js 아님, 가드에서 처리)
- [x] **창 열기 헬퍼** `composables/useMemoWindow.js` — `openMemoWindow(id, {view})` = `window.open('/memo/'+id(+'?view=1'), 'memo_'+id, 'width=560,height=760...')` + `.focus()`
  - 같은 고객 재클릭 = 기존 창 재사용(named target `memo_<id>`) → **반환된 window에 `.focus()` 명시 호출**(브라우저별 자동 포커스 안 될 수 있음). 고객당 팝업 1개 보장(팝업↔팝업 동기화 불필요). 다른 고객 = 새 창. ⚠️ 이 코드베이스는 기존에 `window.open(..., '_blank')`만 씀(named target·focus 선례 없음) → **1차에서 재클릭 포커스 직접 테스트**
  - ⚠️ **클릭 핸들러에서 동기 호출**(팝업 차단 회피). 기존 `onVisitSchedule`의 `/visit-calendar` 패턴과 동일
- [x] **[1차] 동기화 = 새로고침 버튼만.** 1차엔 상태변경 채팅이 없어 푸시할 이벤트가 없음 → 리스너 불필요. (MemoWindow 헤더 새로고침 버튼으로 충족)
- [x] **표 → 팝업 동기화 = `BroadcastChannel`(같은 브라우저) + 전체 재조회 (1차로 당겨 구현 완료 2026-06-08)**
  - 송신 `broadcastCustomerUpdated(customerId)`(`useMemoWindow.js`) `{type:'customer-updated', customerId}` → 수신 `MemoWindow.vue` onMounted(자기 customerId 매칭 시 `refresh()` = `GET /memo/{id}` 통째 재조회). 헤더(이름/상태/약속/회의실/담당)+권한(canWrite·editable) 갱신. 권한 잃으면 410→'권한없음'.
  - 송신처: All.vue(이름/상태/재콜시간) · VisitCalendar(내방 등록·수정·삭제) · Allocate(분배) · Revoke(회수·이관). 제외: 매출(헤더 무관)·전화(편집불가)·직원개명(드묾, cross-user)
  - [x] **로그아웃 → `{type:'logout'}` → 팝업 `window.close()`**: 전송=`stores/auth.js` `logout()`, 수신=`MemoWindowPage.vue`
  - **다크/라이트 동기화**(추가): 본창 토글 → `storage` 이벤트로 팝업도 즉시 전환(`MemoWindowPage`)
  - **한계**: BroadcastChannel은 **내 브라우저 변경만** 감지. 다른 사용자/PC 변경 실시간은 **3차(Phase 10 WebSocket)**
  - (참고) 반대 방향(팝업 → 전체 표)은 표시 영향 없음(전체 표는 메모=아이콘)
- [x] `views/Pages/Customer/All.vue` — 메모 아이콘 클릭 `onTableButtonClick`에서 `openMemoWindow(row.id)`. 제거 완료: `memoReadOnly`/`memoOpen`/`memoRow`/Teleport+`<Memo>`/`Memo` import/`onMemoSaved`/`closeMemo`. (DUPLICATE 가드는 유지)
  - ⚠️ `onMemoSaved`는 **현재 살아있는 로직**(MEMO.vue가 `emit('saved')` → 표 상태/매출 국지 갱신, 641~657줄). 제거 안전한 이유: **상태·매출은 이제 표에서 직접 편집(이미 반영)**, 창은 이 필드들을 안 건드림. (열린 창의 헤더 갱신은 2차 BroadcastChannel/새로고침)
- [x] **`views/Pages/Customer/Allocate.vue`**: `openMemoWindow(row.customer_id||row.id, {view:true})` (읽기전용 ?view=1). 제거: `<Memo :read-only>`/`Memo` import/`memoOpen`/`memoRow`/`closeMemo`. `openMemo(row)` = 헬퍼 래퍼로 유지(핸들러 `onTableButtonClick` 그대로).
- [ ] **→ Phase 7로 이동** `views/Pages/Customer/Revoke.vue` **+ `Center.vue`** — 메모 셀 **인라인 펼치기**(읽기 전용): 말줄임=**최근 채팅 1건(STATUS 제외)**, 클릭 시 전체 채팅(STATUS 포함). (`Duplicate.vue`는 duplicate_memo 그대로 유지)
  - ⚠️ `PsnsTable.vue`(`components/tables/basic-tables/`)의 셀 확장은 **텍스트 토글 전용**(`expandedCell` 토글, toggleExpand/isExpanded 406~412 — 슬롯/커스텀 렌더 없음) → 전체 채팅 리스트는 불가. **권장(중간 난이도)**: PsnsTable 본체 변경 최소화 — 신규 `type:'memoChat'` 또는 named slot로 **`MemoChatItem`(읽기전용) 재사용** 렌더 + 펼칠 때 `GET /memo/{id}` 지연 로드
  - **왜 Phase 7?**: 멀티행 표 → 행마다 그 고객의 실제 채팅 필요(단일 목업 불가). 백엔드 `GET /memo/{id}` 선행 필수. 1차에선 `customer_memo` 보존돼 현행 `type:'text'` 셀이 정상 작동하므로 미루어도 기능 공백 없음.

---

## Phase 3 · 백엔드 — DB 스키마 + 마이그레이션 ✅ 로컬(meta26) 적용 완료

> **2026-06-08**: SQL 파일 `work/db/memo_chats.sql`(CREATE IF NOT EXISTS + 멱등 이관) 작성 → 로컬 meta26 적용 성공. 검증: 테이블 생성, **19건 이관**(전부 SYSTEM·author NULL·created_at=customer_created_at), 인덱스 OK, customer_memo 보존. **⚠️ 운영 배포 시 prod `blue`에도 동일 SQL 실행 필요**(마이그레이션 툴 없음). 스키마 사전검증: customers.customer_id BIGINT PK / customer_created_at DATETIME NOT NULL / users.user_id BIGINT PK 확인됨.

- [x] **`memo_chats` 테이블 생성**
  ```sql
  CREATE TABLE memo_chats (
      chat_id            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '메모 채팅 PK',
      customer_id        BIGINT       NOT NULL COMMENT '대상 고객',
      author_user_id     BIGINT       NULL     COMMENT 'USER=작성자(이름은 users JOIN) / SYSTEM(시트·마이그레이션)=NULL / STATUS(2차 상태변경)=NULL. SYSTEM·STATUS는 프런트가 시스템으로 표시',
      chat_type          VARCHAR(10)  NOT NULL DEFAULT 'USER' COMMENT 'USER / SYSTEM(메모: 마이그레이션·시트종목명, 미리보기 포함) / STATUS(상태변경 이벤트, 2차 트리거, 미리보기 제외)',
      content            TEXT         NOT NULL COMMENT '본문',
      created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '작성 시각(불변)',
      updated_at         DATETIME     NULL     COMMENT '마지막 수정 시각(NULL = 수정 안 됨). 내용 수정 시에만 수동 set',
      updated_by         BIGINT       NULL     COMMENT '마지막 수정한 user_id',
      is_deleted         TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '소프트 삭제',
      deleted_by         BIGINT       NULL     COMMENT '삭제한 user_id',
      deleted_at         DATETIME     NULL     COMMENT '삭제 시각',
      CONSTRAINT fk_memo_chats_customer FOREIGN KEY (customer_id)    REFERENCES customers(customer_id) ON DELETE CASCADE,
      CONSTRAINT fk_memo_chats_author   FOREIGN KEY (author_user_id) REFERENCES users(user_id)         ON DELETE SET NULL,
      CONSTRAINT fk_memo_chats_updater  FOREIGN KEY (updated_by)     REFERENCES users(user_id)         ON DELETE SET NULL,
      CONSTRAINT fk_memo_chats_deleter  FOREIGN KEY (deleted_by)     REFERENCES users(user_id)         ON DELETE SET NULL
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='고객 메모 채팅 메시지';
  CREATE INDEX idx_memo_chats_customer ON memo_chats(customer_id, created_at, chat_id);
  ```
- [x] **기존 데이터 이관**: `customer_memo` 있는 고객 → `시스템` 최초 SYSTEM 채팅 1건 (19건 완료)
  ```sql
  -- 멱등(재실행 안전): 이미 채팅이 있는 고객은 건너뜀
  INSERT INTO memo_chats (customer_id, author_user_id, chat_type, content, created_at)
  SELECT c.customer_id, NULL, 'SYSTEM', c.customer_memo, c.customer_created_at
  FROM customers c
  WHERE c.customer_memo IS NOT NULL AND c.customer_memo <> ''
    AND NOT EXISTS (SELECT 1 FROM memo_chats mc WHERE mc.customer_id = c.customer_id);
  ```
- [x] `customers.customer_memo` 전환기 보존(롤백 대비). 신규 쓰기 중단(앱)은 Phase 9. **최종 제거는 Phase 9.** (중복DB `duplicate_memo`는 채팅화 대상 아님 → 유지)

---

## Phase 4 · 백엔드 — 채팅 CRUD API ✅ 4a·4b 완료

> **2026-06-08 — Phase 4a(채팅 CRUD) 완료**. 작성/재작성:
> - DTO: `MemoChatDto`·`MemoChatCreateDto`·`MemoChatUpdateDto` 신설, `MemoDetailDto` 개편(헤더+canWrite+chats), 메모 전용 `UserContextDto`(is_super 포함) 신설. 구 `MemoUpdateDto` 삭제.
> - `MemoMapper`(.java/.xml) 재작성: findUserContextByEmail(+is_super)/selectMemoHeader(내방 회의실 서브쿼리)/selectChatsByCustomer/selectChatById/selectChatMeta/insertChat(useGeneratedKeys)/updateChatContent/softDeleteChat.
> - `MemoService` 재작성: getMemo/addChat/editChat/deleteChat/addSystemChat + 권한(조회·canWrite·editable·assertCanModify). USER editable=canWrite, SYSTEM·STATUS=총관리자(is_super='Y').
> - `MemoController` 재작성: GET `/{id}` · POST `/{id}/chats` · PATCH `/chats/{chatId}` · DELETE `/chats/{chatId}`. 구 PATCH `/{id}` 제거.
> - **검증 ✅ 라이브 통과(2026-06-08)**: compileJava / SQL 4종 / 바인딩 11=11 + **실엔드포인트 호출**: SUPERADMIN GET200(canWrite=T)·POST201(author=박건희,JOIN)·PATCH200(updatedAt)·DELETE204·재조회 사라짐·빈내용400 / CENTERHEAD GET200(canWrite=F,editable=F)·POST403 / STAFF 미배정고객 GET410(GONE). 테스트행 정리 완료(19건 원복).
> - **4b 완료(2026-06-08, 이중기록 방식)**: `SheetsSyncService` 신규고객 INSERT(449) 직후 `addSystemChat`. `CustomerUpsertDto.customerId` 추가 + `insertCustomerMinimal` `keyProperty="customerId"`로 PK 수령. **customer_memo도 그대로 기록(이중기록)** → dedup 무변경(원래 plan의 customer_memo 비교 제거·쓰기중단은 **Phase 9로 이동**, Revoke/Center 텍스트 셀이 1차에 customer_memo 쓰므로). compileJava 통과. **✅ 실제 시트→채팅 E2E 라이브 검증(2026-06-08)**: 신규번호 행 동기화 → 고객 #20(division=최초) 생성 + memo_chats #34(SYSTEM, author NULL, "시청한 종목명…\n선택한 항목…") 생성 + customer_memo 동일내용(이중기록). 같은 03:19 동기화에서 중복행(양선옥)은 채팅 미생성 = 양 분기 동시 확인.

대상 패키지: `blue/src/main/java/com/blue/customer/common/memo/`

> **권한 컨텍스트(개선)**: 기존 memo는 `all.dto.UserContextDto` 재사용 중이나 거기엔 `isSuper`가 없음. **패키지 관례대로 `customer.common.memo.dto.UserContextDto` 신설**(클래스명 `UserContextDto`) `{userId, role(String), centerId, isSuper}` + memo 전용 `findUserContextByEmail`(`u.is_super AS isSuper`). ⚠️ **`isSuper`는 코드 관례상 `String`('Y'/'N')**(revoke-DTO 선례 `revoke.dto.UserContextDto.isSuper`). 총관리자 판정 = `"Y".equals(isSuper)`.
> **이름은 write에 불필요**: 표시 이름은 read 시 JOIN(USER=users.user_name / SYSTEM·STATUS='시스템'). addChat은 `author_user_id`만, editChat/삭제는 `updated_by`/`deleted_by`만 기록. (상태변경 자동채팅은 2차 트리거 = `author_user_id=NULL`)

### DTO (코드베이스 관례 `~Dto`)
- [x] `MemoChatDto` (응답 1건): `chatId, authorUserId, authorName(조회시 계산), chatType, content, createdAt, updatedAt, editable`
  - `authorName` = USER일 때만 `users.user_name`(JOIN), SYSTEM·STATUS는 NULL → 프런트가 '시스템' 표시. 표시: USER→아바타+작성자명 / SYSTEM·STATUS→'시스템'
  - `updatedAt` 있으면 프런트에서 "수정됨" 표시
  - `editable` = 서버 계산: **(USER && 호출자 작성권한) 또는 ((SYSTEM 또는 STATUS) && 호출자 총관리자 `is_super`)**
- [x] `MemoDetailDto` (창 로드 응답, 개편): `customerId, name, phone, status, promiseTime, staff, (내방시 visitRoom), canWrite, chats:[MemoChatDto]`
  - 기존 비-메모 필드(tradingviewId/telegram/flags 등) 제거
  - **`staff`(현재 담당 프로) 포함.** `staffHistory`(과거이력)·`category` 제외
  - **`canWrite`**(서버 계산) = 이 고객에 채팅 작성 가능 여부(=addChat 권한). **CENTERHEAD·EXPERT=false**(조회만), SUPERADMIN/MANAGER(자기팀)/STAFF(본인)=true. 프런트는 입력창을 `canWrite && !view`일 때만 노출(없으면 CENTERHEAD/EXPERT가 전송 시 403)
- [x] `MemoChatCreateDto` (요청): `content`
- [x] `MemoChatUpdateDto` (요청): `content`

### 라우트 (`MemoController`)
- [x] `GET    /api/work/db/memo/{customerId}`        → `MemoDetailDto`
- [x] `POST   /api/work/db/memo/{customerId}/chats`  (`MemoChatCreateDto`) → `MemoChatDto` (201)
- [x] `PATCH  /api/work/db/memo/chats/{chatId}`       (`MemoChatUpdateDto`) → `MemoChatDto`
- [x] `DELETE /api/work/db/memo/chats/{chatId}`       → 204
- [x] 기존 `PATCH /api/work/db/memo/{customerId}`(메모+상태+트뷰+텔그+플래그+promiseTime 통합 수정, `updateCustomerMemoBlock`) 제거
  - **✅ 표 안 깨짐(검증 확정)**: 이 PATCH의 **유일한 호출처는 삭제 예정 `MEMO.vue`(600줄)**. 표의 상태/재콜시간/이름 인라인 편집은 **별도 엔드포인트 `PATCH /api/work/db/all/update/{id}`**(`UpdateFieldDto` field=status/reservation/name, `CustomerAllController`), 매출은 **`POST /api/work/db/sales`**(`SalesUpdateDto`)를 씀 → 메모 PATCH와 무관. 신규 채팅창은 상태/매출/약속시간을 **읽기전용 헤더로만** 표시(쓰기 안 함)라 이 PATCH 제거가 안전.

### Mapper (`MemoMapper.java` / `MemoMapper.xml`)
- [x] `selectChatsByCustomer(customerId)` — `is_deleted=0`, created_at ASC. **users LEFT JOIN author_user_id**로 `authorName = u.user_name`(USER만 값, NULL 가능). SYSTEM·STATUS는 프런트가 '시스템' 표시
- [x] `insertChat(...)` — USER/SYSTEM 공통
- [x] `updateChatContent(chatId, content, updatedBy)` — `content`, `updated_at=NOW()`, `updated_by` 갱신. `is_deleted=0` (chat_type 게이트는 Service에서 판단: USER=작성권한 / SYSTEM=총관리자)
- [x] `softDeleteChat(chatId, deletedBy)` — `is_deleted=1, deleted_by, deleted_at=NOW()` (chat_type 게이트는 Service)
- [x] `selectChatMeta(chatId)` — 권한 검증용(customer_id, chat_type, is_deleted)
- [x] `selectMemoHeader(customerId)` — 헤더 메타(name/phone/status/promiseTime/**staff**=`u.user_name`). 내방이면 `visit_schedule`→`meeting_rooms` JOIN으로 회의실명(표 `reservation` CASE 재사용). **staffHistory·category 제외**

### Service (`MemoService.java`)
- [x] `getMemo(customerId, callerEmail)` (⚠️ 기존 코드 인자 순서 = `(customerId, callerEmail)`, 신규 채팅 메서드도 통일) → 헤더 + 채팅 목록 + **`canWrite`**(작성권한=addChat 가능 여부, CENTERHEAD·EXPERT=false). **조회 권한 = 기존 getMemo 전략**(SUPERADMIN·CENTERHEAD·EXPERT 전체, MANAGER 자기팀, STAFF 본인). 각 채팅 `editable` = **(USER && 호출자 작성권한) 또는 ((SYSTEM 또는 STATUS) && 호출자 총관리자 is_super)**. CENTERHEAD·EXPERT는 USER도 false
- [x] `addChat(email, customerId, content)` — **작성 권한** 검증(SUPERADMIN/MANAGER 자기팀/STAFF 본인, CENTERHEAD·EXPERT 거부), `author_user_id=호출자 userId`(이름은 JOIN, 스냅샷 X)
- [x] `editChat(email, chatId, content)` — USER=작성권한 / **SYSTEM=총관리자(`is_super`)만**. `updated_at`/`updated_by` 기록
- [x] `deleteChat(email, chatId)` — USER=작성권한 / **SYSTEM=총관리자(`is_super`)만**, 소프트삭제(deleter 기록)
- [x] `addSystemChat(customerId, content)` (메서드 구현 완료. 시트 동기화 호출은 4b) — 실행자 없는 시스템 채팅(author_user_id=NULL, chat_type='SYSTEM'). 예: 시트 최초 메모
- [x] **[1차] 시트 동기화 최초 채팅 ✅완료(이중기록)** — `SheetsSyncService` 신규고객 INSERT(449) 직후 `memoService.addSystemChat(row.getCustomerId(), row.getCustomerMemo())`. content = `시청한 종목명 : {F}\n선택한 항목 : {B}`(mapRow가 customer_memo와 동일 문자열 생성, 빈값 ` : X`). **customer_memo도 그대로 기록**.
  - ✅ **선행 처리 완료**(원래 진단): `insertCustomerMinimal`(SyncMapper:47)은 `useGeneratedKeys`만 있고 `keyProperty` 없어 생성 PK를 못 받았음 → `CustomerUpsertDto`에 `Long customerId` 추가 + `insertCustomerMinimal`에 `keyProperty="customerId"` 추가 완료. `row.getCustomerId()`로 `addSystemChat` 호출. (insertDuplicateMinimal의 `keyProperty="duplicateId"`는 받아만 두고 미사용 = 검증된 선례였음)
  - 트랜잭션: `manualRefresh/autoRefresh/nightlyResume` 모두 `@Transactional` → 채팅 INSERT 같은 TX(검증됨)
  - ⚠️ **신규 경로(`insertCustomerMinimal` 449)만** 채팅 생성, **customer_memo도 그대로 기록(이중기록)**. **중복 경로(`insertDuplicateMinimal` 442)는 채팅 X, `duplicate_memo` 현행 유지**
  - ⚠️ **dedup = 1차엔 무변경**(이중기록이라 customer_memo 값이 그대로 있어 멱등성 유지). 채팅은 신규 INSERT(dedup-miss) 때만 생성되므로 재동기화 시 dedup-hit→스킵→중복채팅 없음. **customer_memo 쓰기중단 + 아래 dedup 정리는 Phase 9로 이동** — (원래 계획) `existsCustomerEvent` customer_memo 비교 제거 3곳: ①SyncMapper.xml:94 ②`SyncMapper.java`의 `@Param("memo")` ③SheetsSyncService 호출 인자. `existsDuplicateEvent`·`mapRow` 종목명 조합은 유지(duplicate_memo 존속)
  - *(상태변경 자동채팅 헬퍼는 2차 트리거로 대체 → Phase 5)*

---

## Phase 5 · [2차] 상태변경 자동채팅 = DB 트리거

> **1차에선 생략**(상태변경 채팅 없음 — 사용자 타이핑 채팅만). **2차에서 트리거 하나로 일괄** — 앱 코드 수동 훅 없음(어느 메뉴/버튼이 status를 바꾸든 자동 포착).

- [ ] **`customers` `AFTER UPDATE` 트리거** — `customer_status` 바뀌면 자동 INSERT
  ```sql
  -- MySQL CLI 실행 시 DELIMITER 래핑 필수(BEGIN 내부 ; 조기종료 방지). customer_status는 NOT NULL이라 <> 안전.
  DELIMITER $$
  CREATE TRIGGER trg_memo_status_chat
  AFTER UPDATE ON customers FOR EACH ROW
  BEGIN
    IF OLD.customer_status <> NEW.customer_status THEN
      INSERT INTO memo_chats (customer_id, author_user_id, chat_type, content, created_at)
      VALUES (NEW.customer_id, NULL, 'STATUS',
              CONCAT('상태 변경: ', OLD.customer_status, ' → ', NEW.customer_status), NOW());
    END IF;
  END$$
  DELIMITER ;
  ```
  - 운영 전제(코드만으론 미검증): MySQL `TRIGGER` 권한, `log_bin_trust_function_creators` 등 — 배포 DB에서 점검
  - **`chat_type='STATUS'`**: 표시 = **'시스템' 라벨**(SYSTEM·STATUS 모두 '시스템' — 동일). 권한 = SYSTEM과 동일(**총관리자만 수정**). **미리보기에서 제외**
  - **실행자 이름 없음**(`author_user_id=NULL` → '시스템'으로 표시, 누가 바꿨는지는 안 적음 — 결정)
  - **no-op 가드 내장**(`OLD<>NEW`). **row 단위라 대량 UPDATE도 status가 실제 바뀐 행에 한해 고객당 1건**(예: 분배 시 prev_status='내방'이라 결과도 '내방'인 행은 OLD=NEW→0건). UPDATE와 **같은 트랜잭션**(롤백되면 채팅도 롤백)
  - **모든 status UPDATE 자동 포착**: 표(updateField)·회수·분배·이관·autoRecall(팀장 권한변경)·미래 코드까지 → **앱 수동 훅 전부 불필요**
  - **이관(transfer) = 2건 허용**(→회수, 회수→신규). net 1건 안 함(트리거 깔끔 유지)
  - `AFTER UPDATE`만 → 신규 고객 INSERT(초기 status)엔 안 걸림(채팅 X). 시트 최초 종목명 채팅은 **1차(Phase 4)** 에서 별도 처리
  - (검토) '약속 설정'(재콜/내방 시간) 자동채팅은 **미구현**, 약속시간은 헤더 약속칩에만

---

## Phase 6 · 백엔드 — 회수·센터 최근메모(latestMemo) ✅ 완료(2026-06-08)

> 검증: compileJava + 서브쿼리/GROUP_CONCAT DB 직접 실행 통과. **목록 미리보기=최신 비-STATUS 채팅 1건**(회수4·센터3 find 교체), **센터 엑셀=전체 채팅 합본**(`[작성자 날짜] 내용`, STATUS 포함). ⚠️ count 쿼리(345/505/657)는 customer_memo 그대로 둠(미사용·Phase 9 정리, 상관서브쿼리 성능회피). 라이브 반영은 백엔드 재시작 후. **DTO 변경 불필요**(기존 `memo` 필드 소스만 교체 → 프런트 `key:"memo"` 그대로 표시).

- [x] ~~RevokeListRowDto에 latestMemo 추가~~ → **기존 `memo` 필드 소스만 교체**(신규 필드 불필요)
- [x] **회수** `CustomerRevokeMapper.xml` find 4곳(106/159/211/281)의 `c.customer_memo AS memo` → **최근 채팅 1건**(`is_deleted=0 AND chat_type <> 'STATUS'`) 상관 서브쿼리 `ORDER BY created_at DESC LIMIT 1` 교체 (replace_all 4곳)
  - **STATUS(상태변경 이벤트)만 제외** → 회수 목록이 "상태 변경: → 회수"로 도배되는 것 방지. **단 마이그레이션 메모·시트 종목명(SYSTEM)은 포함**(1차에서 미리보기 비지 않게)
- [x] **센터** `CustomerCenterMapper.xml` `findForAdmin/CenterHead/Expert`(274/429/584)의 `c.customer_memo AS memo` → 동일 최근 1건 서브쿼리 교체 (⚠️ findForCenterHead는 공백 7칸이라 따로 처리). count·검색필터는 Phase 9
- [x] **센터 엑셀 = 전체 채팅 합본 (완료)**: `exportExcel` 별도 쿼리 `selectMemoExport(ids)` `GROUP_CONCAT(CONCAT('[',작성자,' ',날짜,'] ',content) ORDER BY created_at SEPARATOR '\n')`(STATUS 포함). ①`setGroupConcatMaxLen()`=1,000,000 + `exportExcel @Transactional`(같은 커넥션) ②`cellSafe()` 32,767자 가드 ③목록 find의 latestMemo는 export서 `fullMemo` 맵으로 덮어씀(폴백=row.memo). 작성자: USER=user_name / SYSTEM·STATUS=시스템(COALESCE). 신규 `CenterMemoExportDto`. ⚠️인덱스 `(customer_id,chat_type,created_at)`는 미추가(페이지/export 행만 평가라 현 인덱스로 충분 판단, 필요시 후속).
  - 전체·회수 메뉴엔 엑셀 다운로드 없음(센터만)
- [ ] **중복(Duplicate)은 제외**: `duplicate_memo`(그 중복 자체 메모, 채팅화 X·DROP 안 함) → **현행 유지**, latestMemo 불필요
  - 서브쿼리는 **페이지네이션된 행만** 평가(반환행당 1회). 전체 스캔 금지. `chat_type<>'STATUS'` 필터 때문에 인덱스에 `chat_type` 포함 검토(예: `(customer_id, chat_type, created_at)`)
- [ ] **펼치기 = 전체 채팅(STATUS 포함)**: 미리보기는 STATUS 제외 최신이지만 셀 확장 시엔 STATUS 포함 전체를 Phase 4 `GET .../memo/{customerId}`(또는 chats 전용)로 지연 로드. 프런트 `Revoke.vue`·`Center.vue` 인라인 펼치기(읽기). `Duplicate.vue`는 그대로

---

## Phase 7 · 연결 — 목업 → 실제 API ◑ 코어 완료 / 회수·센터는 Phase 6 후

> **2026-06-08 코어 완료**: `useMemoChats.js` 실 axios 연동(GET/POST/PATCH/DELETE), 서버가 editable·canWrite 계산 → 클라 권한로직 제거. `MemoWindow`/`MemoWindowPage` props 정리(customerId·view만). 임시 `MemoPreview.vue`+`/memo-preview` 라우트 삭제, 고아 `MEMO.vue` 삭제. 빌드 통과 + **GET /memo/20 응답 확인**(신규고객 SYSTEM 채팅 렌더됨). 회수·센터 셀 펼치기는 Phase 6(latestMemo) 후.

- [x] `composables/useMemoChats.js` — 목업 → 실제 axios(GET/POST/PATCH/DELETE) 교체
- [x] **회수·센터 셀 펼치기 → 실제 chats 지연 로드 (완료 2026-06-08)**: 접기=최신1건(Phase 6) / **클릭→펼침=전체 채팅 최신순 스크롤**(STATUS 포함). 신규 `MemoCellChats.vue`(GET /memo/{id} lazy-load, reverse=최신순, 작성자·시간·수정됨 표시, 권한오류 graceful). `PsnsTable`에 범용 펼침 슬롯 `#expand-{col.key}` 추가(타 뷰 무영향). Revoke(2 테이블)·Center에 `#expand-memo`로 연결. row.id=customer_id(매퍼 AS id) 확인. ✅**권한 OK 확인**: 회수 목록은 "프로 보유 + status!='회수'"(아직 배정된 회수*대상*)만 보임 → getMemo의 MANAGER `customerOwnedByCenter`(고객→프로→내센터)와 정확히 일치 → 410 안 남. (앞선 "user_id NULL이라 410" 우려는 착오 — 이미 회수된 미배정 고객은 이 목록에 없음, 분배에서 다룸). 센터(SUPER·CENTERHEAD·EXPERT)는 전체 읽기권한. MemoCellChats는 그래도 권한오류 graceful 처리 유지.
- [~] `customer_memo` 직접 참조하던 프런트 잔재 제거 (All·Allocate 완료. Revoke/Center는 1차에 `row.memo`(customer_memo) 유지 → Phase 6/9서 정리)
- [x] **`components/ui/MEMO.vue` 제거**(All·Allocate가 `<Memo>` 뗀 후 고아) + 잔존 참조 0 확인

---

## 전체 검토 기록 (2026-06-08, 독립 에이전트 2명)
- **수정한 버그**: ①센터 메모 키워드검색 count↔find 불일치(페이지수 오류) → 검색필터 `t.memo` 제거(메모검색 1차 제외). ②팝업 NETWORK 오류가 "메모없음"으로 표시 → NETWORK 분기+재시도. ③로딩 중 "메모없음" 깜빡임 → loading 분기. ④editChat/deleteChat 영향행 미검사 → n==0이면 410. ⑤stale 주석 정리.
- **정상 확인**: 권한 매트릭스/`is_super`, SQL인젝션 없음, BroadcastChannel 누수 없음, canWrite·editable 서버계산만, keyProperty PK, export GROUP_CONCAT/32767가드/@Transactional.
- **수용(미수정)**: 인라인 펼침 클릭-접힘(사용자 요청), MemoWindow customerId 비반응형(창당 고정이라 무해), view 그림자·410 시맨틱(cosmetic).
- FE 빌드·BE 컴파일 green. ⚠️반영은 백엔드 재시작 필요.

---

## Phase 8 · 검증 설계

### 서버 검증 로직 (UI 안 믿고 서버에서 강제)
- [ ] **입력**: `content` trim 후 빈값 거부, 최대 길이(예: 2000자) 제한
- [ ] **권한 매트릭스 — 기존 `MemoService` 전략 그대로 미러링 (조회 ≠ 작성/수정/삭제)**
  - **조회(GET)** = 기존 `getMemo` 전략: SUPERADMIN·CENTERHEAD·EXPERT = 전체 열람(스코프 없음) / MANAGER = 자기 팀(center_id) / STAFF = 본인건. 권한 밖이면 거부
  - **작성·수정·삭제(POST/PATCH/DELETE)** = 기존 `updateMemo` 전략: SUPERADMIN 전체 / MANAGER 자기 팀 담당건 / STAFF 본인건. **CENTERHEAD·EXPERT 거부**(읽기전용), 중복DB 거부
  - 수정·삭제 시 chat_type 게이트: **USER = 작성권한자 / SYSTEM = 총관리자(`is_super`)만**. 그 외 SYSTEM 수정·삭제는 403
  - `chatId` 작업 시 그 채팅의 `customer_id` 기준 권한 **재확인**(URL 위조 방지)
  - 소프트삭제된 채팅은 조회·수정·삭제 대상에서 제외
- [ ] **무결성**: 상태 자동채팅은 `old != new`일 때만 1건(Phase 5 no-op 가드, 중복·헛 채팅 방지)

> 유저 입장 E2E 수동 테스트 시나리오(개발 후 직접 클릭 검증)는 별도 파일 **`MEMO_CHAT_E2E.md`** 참조.

### QA 체크리스트
- [ ] 마이그레이션 건수 일치(`customer_memo` 비어있지 않은 고객 수 == 생성 SYSTEM 채팅 수) + **재실행 멱등**(두 번 돌려도 중복 0)
- [ ] 권한 매트릭스 전수(5역할 × 조회/작성/수정/삭제). **CENTERHEAD·EXPERT 입력창 없음(canWrite=false)·작성 403**
- [ ] 시스템 채팅 보호: 일반 역할 수정·삭제 403, **총관리자(`is_super`)는 성공**
- [ ] 채팅 수정 시 `updated_at` 기록 + "수정됨" 표시 + 시간 updated_at으로 교체
- [ ] **[2차] 트리거 동작**: 표·회수·분배·autoRecall 등 모든 status 변경에 자동 1건, **이관은 2건**(→회수, 회수→신규), `OLD==NEW`면 0건, 대량 UPDATE도 고객당 1건. (1차엔 자동채팅 없음)
- [ ] **no-op 가드**: 같은 상태 재선택·HQ풀(없음)→팀장풀(없음) → 채팅 **안 생김**
- [ ] **약속설정 자동채팅 없음**(재콜/내방 시간 세팅 시 채팅 X, 헤더 약속칩만). 내방으로 막 바꾸면 헤더 `내방 (일정없음)`
- [ ] **이름 자동 반영**: 직원 개명 → 그 사람 과거 USER·자동채팅 표시이름 갱신 / 고객 개명 → 열린 팝업 헤더 갱신(BroadcastChannel)
- [ ] **표→팝업 동기화(BroadcastChannel)**: 표 상태/시간/이름 변경 → 같은 브라우저 열린 팝업이 `GET /memo/{id}` 전체 재조회로 즉시 갱신 / 로그아웃 → 팝업 자동 닫힘. (다른 유저 실시간은 Phase 10 후속)
- [ ] **회수·센터 미리보기 = 최근 채팅(STATUS만 제외)**: 마이그레이션 메모·종목명(SYSTEM)은 미리보기에 **보임**(1차 빈칸 방지), 상태변경(STATUS)은 안 보임(2차 '→회수' 도배 방지). 펼치기는 전체(STATUS 포함) 지연 로드
- [ ] **시트 dedup**: customer_memo 미기록 후에도 재동기화 시 **동일 행 재삽입 안 됨**
- [ ] 별도 창: 로그인 상태 새 창 정상 로드/인증(가드 refresh). 다중 창: 다른 고객 동시 오픈·같은 고객 재클릭 포커스
- [ ] 검색(내용·이름·날짜) 프런트 필터

---

## Phase 9 · `customer_memo` 컬럼 제거 (최종 정리)

> **선행조건 전부 충족 후에만 DROP.** 코드가 컬럼을 읽는 동안 지우면 앱이 깨짐.

- [ ] Phase 3 이관 완료 + 건수 검증 통과
> ⚠️ **`customer_memo`(드롭)와 `duplicate_memo`(유지) 구분.** `duplicate_memo`는 **존속**(중복 채팅화 X, 중복 메뉴 표시·시트 중복 INSERT에 계속 사용) → 그 readers/writers는 **건드리지 않음**. 아래는 **`customer_memo`만**:
- [ ] `MemoMapper.xml` (62 select, 81 update) — Phase 4에서 대체
- [ ] `CustomerAllMapper.xml` — **customers 분기** `c.customer_memo AS memo`(179/**282**/447/503/565/**649**/704/**781**; ※282·649·781[countAllForExpert] 누락이었음) + `CUSTOMER_ONLY_FILTERS`(357) 제거/repoint. `ADMIN_FILTERS`(55) `t.memo`는 **customers 파트만** 정리(⚠️ latestMemo로 바꾸면 검색이 customers는 최근채팅만·duplicate는 전체라 비대칭 — 검색 백엔드 후속이라 1차엔 메모검색 제외 권장). **중복 분기 `d.duplicate_memo`(242/328/610/672/746/802)는 유지**
- [ ] **`CustomerCenterMapper.xml`(누락이었음)** — `c.customer_memo` find(274/429/584)+count(345/505/653)+필터(80/143) → latestMemo(Phase 6)
- [ ] `CustomerRevokeMapper.xml`(106/159/211/281) → `latestMemo`로 교체 (Phase 6)
- [ ] `SyncMapper.xml` — **customer_memo insert(52/58) 제거 + `existsCustomerEvent` 매퍼 94줄 비교 제거 + 자바 호출(SheetsSyncService 416)의 memo 인자 제거.** **`duplicate_memo` insert(72/78)·`existsDuplicateEvent`(107)는 유지**(duplicate_memo 존속)
- [ ] DTO: `CustomerUpsertDto.customerMemo`(12) 제거. **`DuplicateUpsertDto.duplicateMemo`(13)·`AllDbRowDto.memo`(60, 중복행 duplicate_memo 운반)는 유지**
- [ ] `SheetsSyncService` mapRow memo 조합(322~334)은 **유지**(신규 고객 채팅 content + 중복 `duplicate_memo` 양쪽에 사용). **customer_memo set만 제거**
- [ ] 프런트: `All.vue`(아이콘→창), `Revoke.vue`·**`Center.vue`(211)**(latestMemo, Phase 6), **`Duplicate.vue`(82)는 duplicate_memo 유지(변경 없음)**
- [ ] **`customer_memo`** 쓰는·읽는 코드 0건 확인 (grep `customer_memo`/`customerMemo`; `duplicate_memo`는 제외). ⚠️ **프런트는 컬럼 config의 `key:"memo"`** 라 `customer_memo` grep으로 안 잡힘 → `key:"memo"` 컬럼 정의(All/Revoke/Center/Duplicate)를 **직접 제거/repoint** 확인
  > 참고: `VisitCalendar.vue`(visit_schedule.memo), `MyInfo.vue`(ip_whitelist.memo)의 'memo'는 무관.
- [ ] 운영 검증 통과
- [ ] **[2차] `ALTER TABLE customers DROP COLUMN customer_memo;`** (**`customers_duplicate.duplicate_memo`는 DROP 안 함**)
  - ⚠️ **이 DROP만 2차**(1차에선 컬럼 보존 — 롤백 안전망). 위의 읽기 repoint·쓰기 중단은 1차에서 끝내고, **DROP은 1차 운영 안정 확인 후 2차에서 실행.**

---

## Phase 10 · [3차] 실시간 동기화 (WebSocket/STOMP + Redis)

> **3차.** 2차의 BroadcastChannel(같은 브라우저)을 넘어 **다른 사용자/PC 변경까지 실시간**이 필요할 때 진행.
> 진행 시 순서: 코어(REST 채팅 Phase 1~7) 완성 후 **그 위에 실시간 레이어**로 추가(REST 유지, WS는 "변경 알림"만 얹음).

- [ ] **백엔드 — Spring WebSocket + STOMP**
  - `/ws` 엔드포인트 + **JWT 핸드셰이크 인증**(STOMP CONNECT 프레임 `Authorization: Bearer`, 기존 `JwtUtil` 재사용). (SSE는 EventSource 헤더 인증 미지원 → WebSocket 권장)
  - 토픽: **고객별** `/topic/customer/{customerId}`
  - **발행**: 커밋 후 해당 고객 토픽에 push. 채팅 CRUD는 `MemoService`에서(chat-added/updated/deleted). ⚠️ **상태변경 STATUS 채팅은 DB 트리거가 만들지만 WS는 앱 레벨** → 상태변경 엔드포인트(updateField/revoke/allocate/transfer/권한변경)가 `customer-updated` **알림**을 push(팝업이 재조회). 즉 3차 WS는 **발행 훅이 필요**(트리거는 채팅 생성만). 중앙 `MemoRealtimePublisher`로 모음
  - **다중 인스턴스**: **Redis pub/sub**(이미 있음)로 인스턴스 간 fan-out → 한 인스턴스의 변경을 모든 인스턴스 구독자에게 전달
- [ ] **프런트 — STOMP 클라이언트**(`@stomp/stompjs`)
  - 팝업 열릴 때 `/topic/customer/{id}` 구독, 닫힐 때 해제. 이벤트 수신 → 채팅/헤더 **즉시 갱신**(증분 또는 재조회). 한 창=한 WS 연결(여러 구독 멀티플렉싱)
  - **재접속 시 전체 재조회**(끊긴 동안 놓친 이벤트 보정)
  - **BroadcastChannel 데이터 동기화는 WS로 대체**(표↔팝업도 WS로 커버). BroadcastChannel은 로그아웃 즉시 닫기 용도로만 선택적
- [ ] (검토) 연결/부하: 팝업당 1 WS 연결. 동시 다수 팝업·다수 사용자 시 커넥션·Redis 부하 모니터링. 인증 만료 시 재연결(토큰 refresh 후 reconnect)
- [ ] (검토) 회수·센터 목록(인라인)도 실시간 반영할지: 목록은 페이지 단위라 토픽 구독이 과할 수 있음 → 우선 **상세 팝업만 실시간**, 목록은 진입/새로고침 시 최신

---

## 재검토 보강 (2026-06-03, 코드 정밀검증)

- **자동 시스템 채팅 = 상태변경과 같은 트랜잭션 + batch insert**(롤백 일관성). 비동기/별도TX 아님. 대량 회수·분배도 `alloc_logs`처럼 한 TX에서 배치 INSERT.
- **`?view=1`은 UI 전용 모드**(입력창·✏️✕ 숨김). **실제 권한 경계는 서버** — 작성/수정/삭제는 서버가 `editable`/권한 재검증으로 강제(URL·쿼리 변조해도 막힘). `?view=1` 제거해도 서버가 거부.
- **프런트는 채팅별 `chat.editable`(서버 계산)만 사용** → 프런트에서 `isSuper` 따로 계산 불필요(SYSTEM 채팅 ✏️✕ 노출도 `editable`로 제어).
- **신규 memo 엔드포인트는 `/api/work/**` = authenticated만**(Security 설정상 역할 URL매칭 없음). 역할 격리는 `MemoService` 계층에서(기존 패턴 동일).
- **상태변경 자동채팅 = [2차] DB 트리거 일괄**(수동 훅 폐기): `customers AFTER UPDATE`, content `상태 변경: A → B`, chat_type='STATUS', 실행자명 없음('시스템'). 표·회수·분배·이관·autoRecall·미래 코드 자동 포착, 이관 2건.
- (flag) 메모 헤더 전화번호: 현재 메모 상세는 **마스킹 미적용** → 신규 창도 현행대로 비마스킹 유지(별도 요구 없으면).
- (확인 완료) `Revoke.vue`·`Center.vue`는 `<Memo>` 미사용, **메모는 `type:'text'` 셀**(Revoke 214/234, Center 211) → 인라인 펼치기 대상 맞음.
- **무변경(no-op) 가드**: 상태 자동채팅은 `old != new`일 때만(표 같은상태 재선택·분배/회수 중 이미 목표상태 건 → 헛 채팅 방지). Phase 5 공통규칙에 반영.
- 총관리자가 SYSTEM '상태 변경' 채팅을 삭제/수정해도 **실제 customer_status는 안 바뀜**(로그 줄만 조정) — 의도된 동작.
- 마이그레이션된 기존 `customer_memo`는 SYSTEM 채팅 → **총관리자만 수정·삭제 가능**(일반 메모였던 내용이 보호됨). 수용.
- **죽은코드 감사(import 역추적 완료)**: 이 프로젝트는 템플릿(TailAdmin류) 기반이라 데모 컴포넌트 다수. 계획이 의존하는 것 중 **실사용 확인**: `PsnsTable.vue`(All·Allocate·Revoke·Center·Duplicate·User 6곳=REAL), `MEMO.vue`(All·Allocate=REAL, 삭제예정), 고객 5화면 전부 라우팅+사이드바 도달. **데모 죽은코드(재사용 금지)**: `ui/Badge.vue`·`ui/Avatar.vue`(둘 다 `views/UiElements/*`에서만 import). **고아**: `BasicTableOne.vue`·`ui/Modal.vue`(계획 무관). 메모 사용처는 All·Allocate(모달)+Revoke·Center·Duplicate(text셀)뿐 — **계획이 놓친 추가 사용처 없음**.

---

## 성능 고려 (조인·인덱스)

- **전체(All) 목록 쿼리는 `memo_chats`를 JOIN하지 않는다.** 전체 메뉴는 메모를 아이콘으로만 열어 목록엔 `customer_id`만 필요 → 채팅 분리로 인한 목록 추가 부하 0. 오히려 `c.customer_memo AS memo` 제거로 가벼워짐.
- **클릭 시 단건 로드**(`GET /memo/{customerId}`): `idx_memo_chats_customer(customer_id, created_at, chat_id)`로 해당 고객 채팅만 레인지 스캔 → 고객당 수~수십 건, 전체 20만과 무관. author 이름 JOIN은 행당 users PK 룩업이라 무시 가능.
- **회수·센터 `latestMemo`**(`chat_type<>'STATUS' AND is_deleted=0` 최신): 상관 서브쿼리 + `LIMIT 1`, **페이지네이션된 행만 평가**(페이지당 N건 인덱스 시크). 전체 스캔 아님. 필요 시 보조 인덱스 `(customer_id, chat_type, created_at)`(Phase 6과 동일) 검토.
- **Phase 9에서 `customer_memo` TEXT 제거** → `customers` 행 작아져 목록 스캔 I/O 소폭 감소(이득).
- **기존 전체목록 20만건 지연**(UNION ALL + 깊은 OFFSET 페이지네이션)은 **본 개편과 무관한 선재 이슈** — 악화 없음(소폭 개선), 별도 최적화 대상.

---

## 미해결/후속 (이번 범위 아님)
- 모달에서 제거되는 입력은 이미 표에서 처리됨 — **전부 메모 PATCH와 무관한 별도 엔드포인트**(검증): 상태=onBadgeUpdate→`PATCH /db/all/update/{id}`(field=status), 재콜시간=onDateUpdate→`PATCH /db/all/update/{id}`(field=reservation), 이름=onTextUpdate→`PATCH /db/all/update/{id}`(field=name), 매출=onSalesUpdate→`POST /db/sales`. → 기능 손실 없음. ⚠️ 단 **재콜 약속시간은 2스텝**(표에서 상태를 '재콜'로 바꿔 행이 재콜로 리렌더된 뒤 예약칼럼 클릭 입력 — PsnsTable이 `status==='재콜'` 행만 입력 렌더). 검수 시 사용자 안내 필요.
- 검색 **백엔드 연동**은 후속.
- 트레이딩뷰/텔레그램/가입 체크박스 5종은 기능 폐기.
- 전체 메뉴 컬럼 순서 변경(상태/직전상태/메모/최근메모) + 예약칼럼 숨김(항목 2)은 별도 작업.
