package com.blue.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalesPartitionDto {
  private Long partitionId;
  private String partitionType; // BRANCH / TEAM / PERSON
  private Long segment;         // upper bound (이하)
  private String name;          // 예: 100 이하
  private String color;         // #RRGGBB
  private Long updatedBy;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}