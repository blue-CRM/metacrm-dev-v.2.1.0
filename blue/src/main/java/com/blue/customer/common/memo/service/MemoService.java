package com.blue.customer.common.memo.service;

import com.blue.customer.common.memo.dto.MemoChatDto;
import com.blue.customer.common.memo.dto.MemoDetailDto;
import com.blue.customer.common.memo.dto.UserContextDto;
import com.blue.customer.common.memo.mapper.MemoMapper;
import com.blue.global.exception.AuthException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemoService {

  private final MemoMapper mapper;

  /* ===================== 조회 ===================== */

  @Transactional(readOnly = true)
  public MemoDetailDto getMemo(Long customerId, String callerEmail) {
    UserContextDto me = me(callerEmail);
    requireCustomer(customerId);
    assertReadAccess(me, customerId);

    MemoDetailDto dto = mapper.selectMemoHeader(customerId);
    if (dto == null) throw gone("고객을 찾을 수 없습니다.");

    boolean canWrite = canWrite(me, customerId);
    dto.setCanWrite(canWrite);

    List<MemoChatDto> chats = mapper.selectChatsByCustomer(customerId);
    for (MemoChatDto c : chats) {
      c.setEditable(computeEditable(me, c, canWrite));
    }
    dto.setChats(chats);
    return dto;
  }

  /**
   * 읽기전용 셀(표 펼침) 전용 채팅 목록 조회.
   * 헤더(회의실 서브쿼리 포함)·작성권한·채팅별 editable 계산을 모두 생략해
   * 전체 getMemo 대비 쿼리 수와 응답 크기를 줄인 경량 버전이다.
   */
  @Transactional(readOnly = true)
  public List<MemoChatDto> getChats(Long customerId, String callerEmail) {
    UserContextDto me = me(callerEmail);
    requireCustomer(customerId);
    assertReadAccess(me, customerId);
    return mapper.selectChatsByCustomer(customerId);
  }

  /* ===================== 작성 ===================== */

  @Transactional
  public MemoChatDto addChat(String callerEmail, Long customerId, String content) {
    UserContextDto me = me(callerEmail);
    requireCustomer(customerId);
    if (!canWrite(me, customerId)) {
      throw new AuthException("메모 작성 권한이 없습니다.", HttpStatus.FORBIDDEN);
    }
    String body = requireContent(content);

    MemoChatDto chat = new MemoChatDto();
    chat.setCustomerId(customerId);
    chat.setAuthorUserId(me.getUserId());
    chat.setChatType("USER");
    chat.setContent(body);
    mapper.insertChat(chat); // chat.chatId 세팅됨

    MemoChatDto saved = mapper.selectChatById(chat.getChatId());
    saved.setEditable(computeEditable(me, saved, true));
    return saved;
  }

  /** 실행자 없는 시스템 채팅(author_user_id=NULL). 시트 동기화 최초 메모 등 내부용. */
  @Transactional
  public MemoChatDto addSystemChat(Long customerId, String content) {
    MemoChatDto chat = new MemoChatDto();
    chat.setCustomerId(customerId);
    chat.setAuthorUserId(null);
    chat.setChatType("SYSTEM");
    chat.setContent(content);
    mapper.insertChat(chat);
    return chat;
  }

  /* ===================== 수정 / 삭제 ===================== */

  @Transactional
  public MemoChatDto editChat(String callerEmail, Long chatId, String content) {
    UserContextDto me = me(callerEmail);
    MemoChatDto meta = requireChat(chatId);
    assertCanModify(me, meta);
    String body = requireContent(content);

    int n = mapper.updateChatContent(chatId, body, me.getUserId());
    if (n == 0) throw gone("메모를 찾을 수 없습니다."); // 동시 삭제 등으로 대상이 사라짐

    MemoChatDto saved = mapper.selectChatById(chatId);
    boolean canWrite = canWrite(me, saved.getCustomerId());
    saved.setEditable(computeEditable(me, saved, canWrite));
    return saved;
  }

  @Transactional
  public void deleteChat(String callerEmail, Long chatId) {
    UserContextDto me = me(callerEmail);
    MemoChatDto meta = requireChat(chatId);
    assertCanModify(me, meta);
    int n = mapper.softDeleteChat(chatId, me.getUserId());
    if (n == 0) throw gone("메모를 찾을 수 없습니다."); // 동시 삭제로 이미 사라짐
  }

  /* ===================== 권한/공통 ===================== */

  private UserContextDto me(String callerEmail) {
    UserContextDto me = mapper.findUserContextByEmail(callerEmail);
    if (me == null) throw gone("인증 사용자 정보를 찾을 수 없습니다.");
    return me;
  }

  private void requireCustomer(Long customerId) {
    Integer exists = mapper.existsCustomerById(customerId);
    if (exists == null || exists == 0) throw gone("고객을 찾을 수 없습니다.");
  }

  private MemoChatDto requireChat(Long chatId) {
    MemoChatDto meta = mapper.selectChatMeta(chatId);
    if (meta == null || (meta.getIsDeleted() != null && meta.getIsDeleted() == 1)) {
      throw gone("메모를 찾을 수 없습니다.");
    }
    return meta;
  }

  /** 조회 권한: SUPERADMIN·CENTERHEAD·EXPERT 전체 / MANAGER 자기팀 / STAFF 본인 */
  private void assertReadAccess(UserContextDto me, Long customerId) {
    switch (me.getRole()) {
      case "SUPERADMIN", "CENTERHEAD", "EXPERT" -> {}
      case "MANAGER" -> {
        Integer owns = mapper.customerOwnedByCenter(customerId, me.getCenterId());
        if (owns == null || owns == 0) throw gone("권한이 없습니다.");
      }
      case "STAFF" -> {
        Integer owns = mapper.customerOwnedByUser(customerId, me.getUserId());
        if (owns == null || owns == 0) throw gone("권한이 없습니다.");
      }
      default -> throw gone("알 수 없는 권한: " + me.getRole());
    }
  }

  /** 작성 권한(=addChat 가능): SUPERADMIN 전체 / MANAGER 자기팀 / STAFF 본인 / CENTERHEAD·EXPERT 불가 */
  private boolean canWrite(UserContextDto me, Long customerId) {
    return switch (me.getRole()) {
      case "SUPERADMIN" -> true;
      case "MANAGER" -> {
        Integer owns = mapper.customerOwnedByCenter(customerId, me.getCenterId());
        yield owns != null && owns == 1;
      }
      case "STAFF" -> {
        Integer owns = mapper.customerOwnedByUser(customerId, me.getUserId());
        yield owns != null && owns == 1;
      }
      default -> false; // CENTERHEAD, EXPERT
    };
  }

  /**
   * 채팅 수정/삭제 가능 여부:
   *  - USER         → 호출자 작성권한(canWrite)
   *  - SYSTEM/STATUS → 총관리자(is_super='Y')만
   */
  private boolean computeEditable(UserContextDto me, MemoChatDto chat, boolean canWrite) {
    if (isSystemType(chat.getChatType())) return isSuper(me);
    return canWrite;
  }

  private void assertCanModify(UserContextDto me, MemoChatDto meta) {
    if (isSystemType(meta.getChatType())) {
      if (!isSuper(me)) {
        throw new AuthException("시스템 메모는 총관리자만 수정·삭제할 수 있습니다.", HttpStatus.FORBIDDEN);
      }
    } else {
      if (!canWrite(me, meta.getCustomerId())) {
        throw new AuthException("권한이 없습니다.", HttpStatus.FORBIDDEN);
      }
    }
  }

  private boolean isSystemType(String chatType) {
    return "SYSTEM".equals(chatType) || "STATUS".equals(chatType);
  }

  private boolean isSuper(UserContextDto me) {
    return "Y".equals(me.getIsSuper());
  }

  private String requireContent(String content) {
    String c = content == null ? null : content.trim();
    if (c == null || c.isEmpty()) {
      throw new AuthException("내용이 비어 있습니다.", HttpStatus.BAD_REQUEST);
    }
    return c;
  }

  private AuthException gone(String message) {
    return new AuthException(message, HttpStatus.GONE);
  }
}
