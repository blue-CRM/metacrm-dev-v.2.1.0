package com.blue.customer.allocate.dto;

import lombok.Data;
import java.util.List;

/**
 * 팀장(MANAGER) 전용 — 자기 팀장풀(상태 없음) DB를 같은 팀의 여러 팀원에게 랜덤 균등분배.
 * floor(N/K)개씩 랜덤 배분하고, 나머지(N mod K)는 분배하지 않고 팀장풀에 남긴다.
 */
@Data
public class AllocateMgrRandomReq {
  private List<Long> customerIds;    // 분배할 customers.customer_id 목록(현재 담당=팀장 본인, 상태 없음)
  private List<Long> targetUserIds;  // 필수: 같은 팀의 팀원(프로) 2명 이상
}
