package com.blue.customer.common.memo.mapper;

import com.blue.customer.common.memo.dto.MemoChatDto;
import com.blue.customer.common.memo.dto.MemoDetailDto;
import com.blue.customer.common.memo.dto.UserContextDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MemoMapper {

  /** 로그인 사용자 컨텍스트(is_super 포함 — 메모 전용 UserContextDto) */
  UserContextDto findUserContextByEmail(@Param("email") String email);

  /** customers 테이블 존재 여부(중복DB 제외 보장) */
  Integer existsCustomerById(@Param("customerId") Long customerId);

  /** 센터 소유 확인(MANAGER) */
  Integer customerOwnedByCenter(@Param("customerId") Long customerId,
                                @Param("centerId") Long centerId);

  /** 개인 소유 확인(STAFF) */
  Integer customerOwnedByUser(@Param("customerId") Long customerId,
                              @Param("userId") Long userId);

  /** 창 헤더(이름/전화/상태/약속시각/담당프로/내방회의실명) */
  MemoDetailDto selectMemoHeader(@Param("customerId") Long customerId);

  /** 고객의 채팅 목록(is_deleted=0, created_at ASC, author JOIN) */
  List<MemoChatDto> selectChatsByCustomer(@Param("customerId") Long customerId);

  /** 단건 조회(작성/수정 후 응답용, author JOIN) */
  MemoChatDto selectChatById(@Param("chatId") Long chatId);

  /** 권한 검증용 메타(customer_id, chat_type, is_deleted, author_user_id) */
  MemoChatDto selectChatMeta(@Param("chatId") Long chatId);

  /** 채팅 INSERT (useGeneratedKeys → chat.chatId 세팅). USER/SYSTEM 공통 */
  void insertChat(MemoChatDto chat);

  /** 내용 수정(updated_at=NOW, updated_by). is_deleted=0 만 */
  int updateChatContent(@Param("chatId") Long chatId,
                        @Param("content") String content,
                        @Param("updatedBy") Long updatedBy);

  /** 소프트 삭제(is_deleted=1, deleted_by, deleted_at=NOW). is_deleted=0 만 */
  int softDeleteChat(@Param("chatId") Long chatId,
                     @Param("deletedBy") Long deletedBy);
}
