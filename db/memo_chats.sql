-- =====================================================================
-- 메모 채팅화 개편 — Phase 3: memo_chats 테이블 + 기존 메모 이관
-- 적용: 로컬 dev = meta26 / 운영 = blue (배포 시 운영 DB에도 동일 적용)
-- 이 프로젝트는 마이그레이션 툴(Flyway 등)이 없어 DB에 직접 실행한다.
-- 재실행 안전: CREATE는 IF NOT EXISTS, 이관 INSERT는 멱등(NOT EXISTS 가드).
-- =====================================================================

-- 1) 신규 테이블 -------------------------------------------------------
CREATE TABLE IF NOT EXISTS memo_chats (
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

-- 2) 기존 데이터 이관: customer_memo 있는 고객 → '시스템' 최초 SYSTEM 채팅 1건 ----
--    멱등(재실행 안전): 이미 채팅이 있는 고객은 건너뜀
INSERT INTO memo_chats (customer_id, author_user_id, chat_type, content, created_at)
SELECT c.customer_id, NULL, 'SYSTEM', c.customer_memo, c.customer_created_at
FROM customers c
WHERE c.customer_memo IS NOT NULL AND c.customer_memo <> ''
  AND NOT EXISTS (SELECT 1 FROM memo_chats mc WHERE mc.customer_id = c.customer_id);

-- 3) customers.customer_memo 는 전환기 보존(롤백 대비). 신규 쓰기 중단(앱).
--    최종 DROP 은 Phase 9(2차, 운영 안정 후). 중복DB duplicate_memo 는 대상 아님(유지).
