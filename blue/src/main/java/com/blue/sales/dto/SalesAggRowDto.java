package com.blue.sales.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesAggRowDto {
  
  private String key;       // ex) "b1", "t2", "p10"
  private String label;     // 표시명
  
  private Long a;           // 최초
  private Long b;           // 업셀
  private Long c;           // (프론트가 b+c를 초록으로 쓰고 있으니 유지)
  
  private Boolean resigned; // 퇴사자
  
  private Long total;       // a+b+c (서버 계산해서 내려줌)
}