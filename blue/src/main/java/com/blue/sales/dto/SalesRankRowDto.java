package com.blue.sales.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesRankRowDto {
  private String key;
  private String label;
  private Boolean resigned;
  private Long total;
}