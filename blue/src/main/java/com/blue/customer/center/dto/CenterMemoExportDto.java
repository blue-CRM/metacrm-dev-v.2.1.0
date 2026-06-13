package com.blue.customer.center.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 엑셀 export용: 고객별 전체 채팅을 합친 메모 */
@Getter @Setter
@NoArgsConstructor
public class CenterMemoExportDto {
  private Long customerId;
  private String memo;
}
