package com.blue.customer.common.memo.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 메모 전용 로그인 사용자 컨텍스트.
 *
 * all.dto.UserContextDto 에는 is_super 가 없어, 메모 권한(시스템/상태 채팅 = 총관리자만 수정·삭제)
 * 판정을 위해 메모 패키지에 별도 신설한다.
 *
 * ⚠️ isSuper 는 코드 관례상 users.is_super = char(1) 'Y'/'N' 문자열.
 *    총관리자 판정 = "Y".equals(isSuper).
 */
@Getter @Setter
@NoArgsConstructor
public class UserContextDto {
  private Long userId;
  private String email;
  private String role;     // SUPERADMIN / MANAGER / STAFF / CENTERHEAD / EXPERT
  private Long centerId;
  private String isSuper;  // 'Y' / 'N'
}
