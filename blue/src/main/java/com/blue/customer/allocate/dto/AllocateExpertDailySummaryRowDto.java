package com.blue.customer.allocate.dto;

import lombok.Data;

@Data
public class AllocateExpertDailySummaryRowDto {
  private String statDate;     // yyyy-MM-dd
  private String expertName;   // 전문가명(= source 기준)
  private int count;
}