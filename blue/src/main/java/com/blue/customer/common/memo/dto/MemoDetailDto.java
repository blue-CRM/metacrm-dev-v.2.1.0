package com.blue.customer.common.memo.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 메모 채팅창 로드 응답 (헤더 + 채팅 목록).
 *
 * 기존 비-메모 필드(tradingviewId/telegram/flags/category/staffHistory/memo)는 제거.
 * 헤더는 읽기전용 표시용: 이름·전화·상태·약속시각·담당프로·(내방)회의실명.
 * canWrite = 이 고객에 채팅 작성 가능 여부(=addChat 권한). 프런트 입력창은 canWrite && !view 일 때만.
 */
@Getter @Setter
@NoArgsConstructor
public class MemoDetailDto {
  private Long customerId;
  private String name;
  private String phone;
  private String status;
  private LocalDateTime promiseTime; // 약속 시각(재콜/내방). NULL=일정없음
  private String staff;              // 현재 담당 프로(미배정이면 NULL)
  private String visitRoom;          // 내방 회의실명(내방+예약 있을 때)
  private Boolean canWrite;
  private List<MemoChatDto> chats;
}
