package com.blue.sales.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesDashboardRes {
  
  private String viewMode;          // BRANCH / TEAM / PERSON
  private boolean includeResigned;  // 서버 적용 결과
  
  // PartitionBar 설정 (partition_type 기준으로 내려줌)
  private List<Long> partitionStops;     // segment 오름차순
  private List<String> partitionColors;  // 구간별 색상 (stops와 매칭)
  
  // 여기 3개에 각각 rows(List<SalesAggRowDto>)가 들어감
  private SalesSectionDataDto month; // 이번달(항상 이번달)
  private SalesSectionDataDto range; // 요청기간
  private SalesSectionDataDto all;   // 전체기간
}