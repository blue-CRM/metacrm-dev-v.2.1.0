package com.blue.customer.allocate.controller;

import com.blue.customer.all.dto.PagedResponse;
import com.blue.customer.allocate.dto.*;
import com.blue.customer.allocate.service.CustomerAllocateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class CustomerAllocateController {
  
  private final CustomerAllocateService service;
  
  // 목록 (본사/센터장)
  @GetMapping("/work/allocate/list")
  public PagedResponse<AllocateListRowDto> list(Authentication auth,
                                                @RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "10") int size,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) String dateFrom,
                                                @RequestParam(required = false) String dateTo,
                                                @RequestParam(required = false) String category,
                                                @RequestParam(required = false) String division, // HQ only: '최초'|'유효'
                                                @RequestParam(required = false) String prevStatus, // 직전상태 필터 (상태 필터는 어차피 없음 아니면 회수라서 필요 없음)
                                                @RequestParam(required = false) String sort) {
    return service.list(auth.getName(), page, size, keyword, dateFrom, dateTo, category, division, prevStatus, sort);
  }
  
  // 본사 분배
  @PostMapping("/work/allocate/hq")
  public AllocateResult allocateByHq(Authentication auth, @RequestBody AllocateHqReq req) {
    return service.allocateByHq(auth.getName(), req);
  }

  // 본사 전용 - 여러 명에게 랜덤 균등분배
  @PostMapping("/work/allocate/hq/random")
  public AllocateResult allocateByHqRandom(Authentication auth, @RequestBody AllocateHqRandomReq req) {
    return service.allocateByHqRandom(auth.getName(), req);
  }
  
  // 팀장 재분배
  @PostMapping("/work/allocate/manager")
  public AllocateResult allocateByManager(Authentication auth, @RequestBody AllocateMgrReq req) {
    return service.allocateByManager(auth.getName(), req);
  }
  
  // 센터장/전문가 분배
  @PostMapping("/work/allocate/experthead")
  public AllocateResult allocateByExpertHead(Authentication auth, @RequestBody AllocateHqReq req) {
    return service.allocateByExpertHead(auth.getName(), req);
  }
  
  // 직원 검색
  @GetMapping("/work/allocate/users")
  public List<UserPickDto> searchUsers(
      Authentication auth,
      @RequestParam(required = false) Long centerId,
      @RequestParam(required = false) String q
  ) {
    return service.searchUsersForAllocate(auth.getName(), centerId, q);
  }
  
  // 팀 조회
  @GetMapping("/work/allocate/centers")
  public List<CenterPickDto> centers(Authentication auth) {
    return service.centersForAllocate(auth.getName());
  }
  
  // 담당자이력 - 일괄 초기화
  @PostMapping("/work/history/reset/bulk")
  public void resetHistoryBulk(Authentication auth, @RequestBody List<Long> customerIds) {
    service.resetHistory(auth.getName(), customerIds);
  }
  
  // 모달에서 : 담당자 이력 리스트 조회
  @GetMapping("/work/history/{customerId}")
  public List<CustomerHistoryRowDto> getHistory(@PathVariable Long customerId) {
    return service.getHistory(customerId);
  }
  
  // 모달에서 : 담당자 이력 선택 삭제
  @PostMapping("/work/history/delete")
  public void deleteHistory(@RequestBody HistoryDeleteRequest request) {
    service.deleteHistory(request.getLogIds());
  }
  
  // 모달에서 : 슈퍼계정 전용 전문가 DB 집계 표 출력
  @PostMapping("/work/allocate/expert-daily-summary")
  public List<AllocateExpertDailySummaryRowDto> getExpertDailySummary(
      Authentication auth,
      @RequestBody CustomerIdListReq req
  ) {
    return service.getExpertDailySummary(auth.getName(), req.getCustomerIds());
  }
}
