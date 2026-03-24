package com.blue.sales.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SalesSnapshotMeta {
  private String viewMode;          // BRANCH/TEAM/PERSON
  private Boolean includeResigned;  // true/false
  private String dateFrom;          // YYYY-MM-DD or null
  private String dateTo;            // YYYY-MM-DD or null
}