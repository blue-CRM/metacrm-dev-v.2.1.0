package com.blue.sales.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesSectionDataDto {
  
  private List<SalesAggRowDto> rows;        // 그래프 rows (정렬: total desc)
  private List<SalesRankRowDto> topRanks;   // 상위 3명
}