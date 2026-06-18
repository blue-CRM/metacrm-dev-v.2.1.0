package com.blue.customer.allocate.dto;

import lombok.Data;
import java.util.List;

/**
 * 본사(SUPERADMIN) 전용 — 여러 명에게 랜덤 균등분배 요청.
 * 선택된 고객(customerIds)을 선택된 직원(targetUserIds)에게 floor(N/K)개씩 랜덤 분배하고,
 * 나머지(N mod K)는 분배하지 않고 남긴다.
 */
@Data
public class AllocateHqRandomReq {
  private List<Long> customerIds;    // 분배할 customers.customer_id 목록
  private Long targetCenterId;       // 필수: 센터 드롭다운 선택값
  private List<Long> targetUserIds;  // 필수: 분배 대상 직원(팀장/프로) 2명 이상

  // 선택에 팀장이 포함된 경우: 팀장 몫을 개인(확정/신규)처럼 분배할지(true) 팀장풀(없음)로 둘지(false)
  private Boolean assignToManagerAsStaff;
}
