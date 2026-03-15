package com.blue.customer.allocate.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor
public class UserContextDto {
  private Long userId;
  private String role;     // "SUPERADMIN" | "MANAGER" | "STAFF"
  private Long centerId;
  private String email;
  private String visible;  // 전화 가시권한 (여기서는 미사용)
  
  private String isSuper; // Y / N
  private String canAllocate; // 'Y' or 'N'
  private Long expertId; // 전문가(EXPERT)일 경우 매칭되는 출처 ID
}
