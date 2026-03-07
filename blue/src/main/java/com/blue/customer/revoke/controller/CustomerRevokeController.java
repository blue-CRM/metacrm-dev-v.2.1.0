package com.blue.customer.revoke.controller;

import com.blue.customer.all.dto.PagedResponse;
import com.blue.customer.allocate.dto.CenterPickDto;
import com.blue.customer.allocate.dto.UserPickDto;
import com.blue.customer.center.dto.CenterSimpleDto;
import com.blue.customer.revoke.dto.RevokeListRowDto;
import com.blue.customer.revoke.dto.RevokeReq;
import com.blue.customer.revoke.dto.RevokeResult;
import com.blue.customer.revoke.dto.TransferReq;
import com.blue.customer.revoke.service.CustomerRevokeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class CustomerRevokeController {
  
  private final CustomerRevokeService service;
  
  // 목록 (본사/팀장)
  @GetMapping("/work/revoke/list")
  public PagedResponse<RevokeListRowDto> list(Authentication auth,
          @RequestParam(defaultValue = "1") int page,
          @RequestParam(defaultValue = "10") int size,
          @RequestParam(required = false) String keyword,
          @RequestParam(required = false) String dateFrom,
          @RequestParam(required = false) String dateTo,
          @RequestParam(required = false) String category,
          @RequestParam(required = false) String division, // HQ only
          @RequestParam(required = false) String status,
          @RequestParam(required = false) String prevStatus,
          @RequestParam(required = false) String sort,
          @RequestParam(required = false) Long centerId) {
    return service.list(auth.getName(), page, size, keyword, dateFrom, dateTo,
        category, division, status, prevStatus, sort, centerId);
  }
  
  // 본사 회수 실행
  @PostMapping("/work/revoke/hq")
  public RevokeResult revokeByHq(Authentication auth, @RequestBody RevokeReq req) {
    return service.revokeByHq(auth.getName(), req);
  }
  
  // 팀장 회수 실행
  @PostMapping("/work/revoke/manager")
  public RevokeResult revokeByManager(Authentication auth, @RequestBody RevokeReq req) {
    return service.revokeByManager(auth.getName(), req);
  }
  
  // 센터장 회수 실행
  @PostMapping("/work/revoke/centerhead")
  public RevokeResult revokeByCenterHead(Authentication auth, @RequestBody RevokeReq req) {
    return service.revokeByCenterHead(auth.getName(), req);
  }
  
  // 전문가 회수 실행
  @PostMapping("/work/revoke/expert")
  public RevokeResult revokeByExpert(Authentication auth, @RequestBody RevokeReq req) {
    return service.revokeByExpert(auth.getName(), req);
  }
  
  // 이관기능 (super만 사용 가능)
  @PostMapping("/work/revoke/transfer")
  public RevokeResult transfer(Authentication auth, @RequestBody TransferReq req) {
    return service.transferBySuper(auth.getName(), req);
  }
  
  // 이관가능 팀 조회 (현재 소속된 팀 제외하고 리턴)
  @GetMapping("/work/revoke/transfer/centers")
  public List<CenterPickDto> transferCenters(Authentication auth,
                                             @RequestParam(required = false) List<Long> excludeCenterIds) {
    return service.getTransferCenters(auth.getName(), excludeCenterIds);
  }
  
  // 이관가능 팀원 조회 (현재 팀원이 소속된 팀 제외하고 리턴)
  @GetMapping("/work/revoke/transfer/users")
  public List<UserPickDto> transferUsers(Authentication auth,
                                         @RequestParam(required = false) Long centerId,
                                         @RequestParam(required = false) String q,
                                         @RequestParam(required = false) List<Long> excludeCenterIds) {
    return service.getTransferUsers(auth.getName(), centerId, q, excludeCenterIds);
  }
  
  // 팀 드롭박스
  @GetMapping("/work/revoke/teams")
  public List<CenterSimpleDto> revokeTeams(Authentication auth) {
    return service.getRevokeTeams(auth.getName());
  }
}
