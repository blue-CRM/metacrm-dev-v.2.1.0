package com.blue.customer.alloclog.dto;

import lombok.Data;

@Data
public class CustomerOwnerSnapshotDto {
  private Long customerId;
  private Long userId;      // 회수 직전 담당자
  private String userRole;  // 회수 직전 담당자의 직책
  private String status;    // 고객 상태(없음/신규/가망/...)
  private Long centerId;    // 추가: 회수 직전 담당자의 소속 팀
}