package com.blue.customer.common.memo.controller;

import com.blue.customer.common.memo.dto.MemoChatCreateDto;
import com.blue.customer.common.memo.dto.MemoChatDto;
import com.blue.customer.common.memo.dto.MemoChatUpdateDto;
import com.blue.customer.common.memo.dto.MemoDetailDto;
import com.blue.customer.common.memo.service.MemoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/work/db/memo")
public class MemoController {

  private final MemoService memoService;

  /** 창 로드: 헤더 + 채팅 목록(editable 포함) + canWrite */
  @GetMapping("/{customerId}")
  public ResponseEntity<MemoDetailDto> getDetail(@PathVariable Long customerId,
                                                 Authentication auth) {
    return ResponseEntity.ok(memoService.getMemo(customerId, auth.getName()));
  }

  /** 읽기전용 셀 펼침 전용: 채팅 목록만(경량). 헤더/권한쓰기/editable 계산 생략 */
  @GetMapping("/{customerId}/chats")
  public ResponseEntity<java.util.List<MemoChatDto>> getChats(@PathVariable Long customerId,
                                                              Authentication auth) {
    return ResponseEntity.ok(memoService.getChats(customerId, auth.getName()));
  }

  /** 채팅 작성 */
  @PostMapping("/{customerId}/chats")
  public ResponseEntity<MemoChatDto> addChat(@PathVariable Long customerId,
                                             @RequestBody MemoChatCreateDto body,
                                             Authentication auth) {
    MemoChatDto saved = memoService.addChat(auth.getName(), customerId, body.getContent());
    return ResponseEntity.status(HttpStatus.CREATED).body(saved);
  }

  /** 채팅 수정 */
  @PatchMapping("/chats/{chatId}")
  public ResponseEntity<MemoChatDto> editChat(@PathVariable Long chatId,
                                              @RequestBody MemoChatUpdateDto body,
                                              Authentication auth) {
    return ResponseEntity.ok(memoService.editChat(auth.getName(), chatId, body.getContent()));
  }

  /** 채팅 삭제(소프트) */
  @DeleteMapping("/chats/{chatId}")
  public ResponseEntity<Void> deleteChat(@PathVariable Long chatId,
                                         Authentication auth) {
    memoService.deleteChat(auth.getName(), chatId);
    return ResponseEntity.noContent().build();
  }
}
