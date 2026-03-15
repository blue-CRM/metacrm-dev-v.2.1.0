package com.blue.sales.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesPartitionRow {
  
  // sales_partitions.partition_id
  private Long partitionId;
  
  // BRANCH / TEAM / PERSON
  private String partitionType;
  
  // 경계값 upper bound (예: 10000, 30000, 50000 ...)
  private Long segment;
  
  // 표시용 구간명
  private String name;
  
  // 색상(#RRGGBB)
  private String color;
}