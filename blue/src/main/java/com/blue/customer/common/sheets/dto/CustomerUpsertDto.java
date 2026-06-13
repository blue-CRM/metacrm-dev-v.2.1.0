package com.blue.customer.common.sheets.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CustomerUpsertDto {
  private Long customerId;                  // INSERT 후 생성된 PK(useGeneratedKeys로 채워짐) — 최초 SYSTEM 채팅 부착용
  private LocalDateTime customerCreatedAt;  // A열
  private String customerName;              // C열
  private String customerPhone;             // D열
  private String customerMemo;              // F열(시청 종목명)
  private String customerContent;           // G열(하고 싶은 말)
  private String customerSource;            // I열(디비출처)
  
  // 서비스에서 세팅
  private String customerDivision;          // '최초'|'유효' (※ '중복'은 customers에 저장 안 함)
  private String customerStatus;            // 기본 '없음'
  private String customerCategory;          // 기본 '주식'
}