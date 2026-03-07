package com.blue.customer.alloclog.dto;

import lombok.Data;

@Data
public class AllocLogInsertDto {
  private Long customerId;
  private String actionType;     // "ASSIGN" / "REVOKE"
  private Long fromUserId;       // 이전 담당자 (없으면 null)
  private Long toUserId;         // 새 담당자 (없으면 null)
  private Long actedByUserId;    // 버튼 누른 사람
  private Integer isFinalAssign; // 0=팀장 풀용(확정 분배 아님, 통계 제외), 1=확정 DB 카운트에 영향을 주는 이벤트(ASSIGN/REVOKE 공통)
  private Integer fromIsFinalAssign;  // TRANSFER 시 이전 담당자 기준
  private Integer toIsFinalAssign;    // TRANSFER 시 새 담당자 기준
  private String memo;           // 추후 기록 용
}