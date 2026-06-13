package com.blue.customer.common.memo.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 채팅 수정 요청 본문 */
@Getter @Setter
@NoArgsConstructor
public class MemoChatUpdateDto {
  private String content;
}
