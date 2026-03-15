package com.blue.sales.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesDashboardReq {
  
  private String viewMode;         // BRANCH / TEAM / PERSON
  private Boolean includeResigned; // 퇴사자 포함 여부
  
  // 기간내(range) 섹션용
  private String dateFrom;         // "2026-01-01"
  private String dateTo;           // "2026-01-31"
}