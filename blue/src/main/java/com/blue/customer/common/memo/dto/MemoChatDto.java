package com.blue.customer.common.memo.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 메모 채팅 1건 (응답). insert/권한검증에도 재사용.
 *
 * - authorName : USER 일 때만 users.user_name(JOIN). SYSTEM·STATUS 는 NULL → 프런트가 '시스템' 표시.
 * - updatedAt  : NULL = 수정 안 됨. 값 있으면 프런트에서 "수정됨" 표시.
 * - editable   : 서버 계산. (USER && 호출자 작성권한) 또는 ((SYSTEM 또는 STATUS) && 총관리자)
 * - customerId / isDeleted : insert·권한검증(selectChatMeta)용. 응답에도 포함(프런트는 무시).
 */
@Getter @Setter
@NoArgsConstructor
public class MemoChatDto {
  private Long chatId;
  private Long customerId;
  private Long authorUserId;
  private String authorName;
  private String chatType;     // USER / SYSTEM / STATUS
  private String content;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private Integer isDeleted;
  private Boolean editable;
}
