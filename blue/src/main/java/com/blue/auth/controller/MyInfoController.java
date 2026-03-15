package com.blue.auth.controller;

import com.blue.auth.dto.*;
import com.blue.auth.service.*;
import com.blue.global.exception.AuthException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.List;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MyInfoController {
  
  private final MyInfoService myInfoService;
  private final IpWhitelistService ipWhitelistService;
  private final OrgAdminService orgAdminService;
  private final ExpertAdminService expertAdminService;
  private final MeetingRoomService meetingRoomService;
  private final SalesPartitionService salesPartitionService;
  
  // 내 정보 조회
  @GetMapping
  public ResponseEntity<MyInfoResponse> getMe(Authentication auth) {
    String email = auth.getName();
//    System.out.println(email);
    MyInfoResponse me = myInfoService.getMeByEmail(email);
    return ResponseEntity.ok(me);
  }
  
  // 전화번호 변경
  @PutMapping("/phone")
  public ResponseEntity<Void> updatePhone(@RequestBody String phone, Authentication auth) {
    String email = auth.getName();
//    System.out.println(phone);
    myInfoService.updatePhone(email, phone);
    return ResponseEntity.ok().build();
  }
  
  // 비밀번호 변경
  @PutMapping("/password")
  public ResponseEntity<Void> changePassword(@RequestBody ChangePasswordRequest req, Authentication auth) {
    String email = auth.getName();
//    System.out.println(email);
    myInfoService.changePassword(email, req.getCurrentPassword(), req.getNewPassword());
    return ResponseEntity.ok().build();
  }
  
  // 스프레스 시트 조회
  @GetMapping("/sheet-settings")
  public ResponseEntity<SheetSettingsDto> getSheetSettings(Authentication auth) {
    String email = auth.getName();
    return ResponseEntity.ok(myInfoService.getSheetSettings(email));
  }
  
  // 스프레드 시트 수정
  @PutMapping("/sheet-settings")
  public ResponseEntity<Void> putSheetSettings(@RequestBody SheetSettingsDto dto, Authentication auth) {
    String email = auth.getName();
    myInfoService.saveSheetSettings(email, dto);
    return ResponseEntity.ok().build();
  }
  
  /** 접속 로그 엑셀 (기간 내 전체 사용자, login_at 기준) */
  @GetMapping("/logs/export")
  public ResponseEntity<byte[]> exportLoginLogs(Authentication auth,
                                                @RequestParam("from") String fromYmd,
                                                @RequestParam("to") String toYmd) {
    return myInfoService.exportLoginLogsExcel(auth.getName(), fromYmd, toYmd);
  }
  
  /** 위임 대상 조회 (userId 기준) */
  @GetMapping("/delegate/lookup")
  public ResponseEntity<MyInfoResponse> lookup(@RequestParam("email") String email,
                                               Authentication auth) {
    return ResponseEntity.ok(myInfoService.lookupDelegateTargetByEmail(auth.getName(), email));
  }
  
  /** 슈퍼 권한 위임 */
  @PostMapping("/delegate")
  public ResponseEntity<Void> delegate(@RequestBody DelegateRequest req, Authentication auth) {
    myInfoService.delegateSuper(auth.getName(), req.userId());
    return ResponseEntity.ok().build();
  }
  
  // POST 바디 바인딩용 경량 DTO (record → 불변, 보일러플레이트 없음)
  public static record DelegateRequest(Long userId) {}
  
  // ============================
  //   IP 화이트리스트 관리
  // ============================
  // 목록
  @GetMapping("/ip-whitelist")
  public ResponseEntity<List<IpWhitelistDto>> getIpWhitelist(Authentication auth) {
    String email = auth.getName();
    return ResponseEntity.ok(ipWhitelistService.getAll(email));
  }
  
  // 추가
  @PostMapping("/ip-whitelist")
  public ResponseEntity<Void> addIp(@RequestBody IpWhitelistDto body,
                                    Authentication auth) {
    String email = auth.getName();
    ipWhitelistService.create(email, body);
    return ResponseEntity.ok().build();
  }
  
  // 수정
  @PutMapping("/ip-whitelist/{ipId}")
  public ResponseEntity<Void> updateIp(@PathVariable Long ipId,
                                       @RequestBody IpWhitelistDto body,
                                       Authentication auth) {
    String email = auth.getName();
    ipWhitelistService.update(email, ipId, body);
    return ResponseEntity.ok().build();
  }
  
  // 비활성화
  @DeleteMapping("/ip-whitelist/{ipId}")
  public ResponseEntity<Void> deleteIp(@PathVariable Long ipId,
                                       Authentication auth) {
    String email = auth.getName();
    ipWhitelistService.deactivate(email, ipId);
    return ResponseEntity.ok().build();
  }
  
  // 완전 삭제
  @DeleteMapping("/ip-whitelist/{ipId}/hard")
  public ResponseEntity<Void> hardDeleteIp(@PathVariable Long ipId,
                                           Authentication auth) {
    String email = auth.getName();
    ipWhitelistService.deleteHard(email, ipId);
    return ResponseEntity.ok().build();
  }
  
  // ============================
  //   지점(Branch) 관리
  // ============================
  
  @GetMapping("/branches")
  public ResponseEntity<List<BranchDto>> listBranches(
      @RequestParam(value = "keyword", required = false) String keyword,
      Authentication auth
  ) {
    String email = auth.getName();
    return ResponseEntity.ok(orgAdminService.getBranches(email, keyword));
  }
  
  @PostMapping("/branches")
  public ResponseEntity<Void> addBranch(@RequestBody BranchDto body,
                                        Authentication auth) {
    String email = auth.getName();
    orgAdminService.createBranch(email, body.getBranchName());
    return ResponseEntity.ok().build();
  }
  
  @PutMapping("/branches/{branchId}")
  public ResponseEntity<Void> renameBranch(@PathVariable Long branchId,
                                           @RequestBody BranchDto body,
                                           Authentication auth) {
    String email = auth.getName();
    orgAdminService.renameBranch(email, branchId, body.getBranchName());
    return ResponseEntity.ok().build();
  }
  
  @DeleteMapping("/branches/{branchId}")
  public ResponseEntity<Void> deleteBranch(@PathVariable Long branchId,
                                           Authentication auth) {
    String email = auth.getName();
    orgAdminService.deleteBranch(email, branchId);
    return ResponseEntity.ok().build();
  }
  
  // ============================
  //   센터(Team) 관리
  // ============================
  
  /** 팀 목록 */
  @GetMapping("/centers")
  public ResponseEntity<List<CenterDto>> listCenters(
      @RequestParam(value = "keyword", required = false) String keyword,
      Authentication auth
  ) {
    String email = auth.getName();
    return ResponseEntity.ok(orgAdminService.getCenters(email, keyword));
  }
  
  /** 팀 추가 */
  @PostMapping("/centers")
  public ResponseEntity<Void> addCenter(@RequestBody CenterDto body,
                                        Authentication auth) {
    String email = auth.getName();
    // body.getBranchId() 는 null 가능. 기본 본사(1)로 고정하고 싶으면 OrgAdminService에서 처리.
    orgAdminService.createCenter(email, body.getCenterName(), body.getBranchId(), body.getCenterColor());
    return ResponseEntity.ok().build();
  }
  
  /** 팀 수정 */
  @PutMapping("/centers/{centerId}")
  public ResponseEntity<Void> updateCenter(@PathVariable Long centerId,
                                           @RequestBody CenterDto body,
                                           Authentication auth) {
    String email = auth.getName();
    orgAdminService.updateCenter(email, centerId, body.getCenterName(), body.getBranchId(), body.getCenterColor());
    return ResponseEntity.ok().build();
  }
  
  /** 팀 삭제 */
  @DeleteMapping("/centers/{id}")
  public ResponseEntity<Void> deleteCenter(@PathVariable("id") long id,
                                           Authentication auth) {
    String email = auth.getName();
    orgAdminService.deleteCenter(email, id);
    return ResponseEntity.ok().build();
  }
  
  // ============================
  //   전문가(Expert) 관리
  // ============================
  
  /** 전문가 목록 */
  @GetMapping("/experts")
  public ResponseEntity<List<ExpertDto>> listExperts(
      @RequestParam(value = "keyword", required = false) String keyword,
      Authentication auth
  ) {
    String email = auth.getName();
    return ResponseEntity.ok(expertAdminService.getExperts(email, keyword));
  }
  
  /** 전문가 추가 */
  @PostMapping("/experts")
  public ResponseEntity<Void> addExpert(@RequestBody ExpertDto body,
                                        Authentication auth) {
    String email = auth.getName();
    expertAdminService.createExpert(email, body);
    return ResponseEntity.ok().build();
  }
  
  /** 전문가 이름 수정 */
  @PutMapping("/experts/{expertId}")
  public ResponseEntity<Void> updateExpert(@PathVariable Long expertId,
                                           @RequestBody ExpertDto body,
                                           Authentication auth) {
    String email = auth.getName();
    expertAdminService.renameExpert(email, expertId, body);
    return ResponseEntity.ok().build();
  }
  
  /** 전문가 삭제 */
  @DeleteMapping("/experts/{expertId}")
  public ResponseEntity<Void> deleteExpert(@PathVariable Long expertId,
                                           Authentication auth) {
    String email = auth.getName();
    expertAdminService.deleteExpert(email, expertId);
    return ResponseEntity.ok().build();
  }
  
  // ============================
  //   전문가(Expert) 관리
  // ============================
  
  // 회의실 목록을 키워드로 조회
  @GetMapping("/meeting-rooms")
  public List<MeetingRoomDto> list(Authentication auth,
                                   @RequestParam(required = false) String keyword) {
    return meetingRoomService.list(auth.getName(), keyword);
  }
  
  // 회의실 신규 생성
  @PostMapping("/meeting-rooms")
  public ResponseEntity<?> create(Authentication auth, @RequestBody @Valid MeetingRoomCreateReq req) {
    meetingRoomService.create(auth.getName(), req);
    return ResponseEntity.ok().build();
  }
  
  // 회의실 정보 수정
  @PutMapping("/meeting-rooms/{roomId}")
  public ResponseEntity<?> update(Authentication auth,
                                  @PathVariable long roomId,
                                  @RequestBody @Valid MeetingRoomUpdateReq req) {
    meetingRoomService.update(auth.getName(), roomId, req);
    return ResponseEntity.ok().build();
  }
  
  // 회의실 소프트 삭제
  @DeleteMapping("/meeting-rooms/{roomId}/soft-delete")
  public ResponseEntity<?> delete(Authentication auth, @PathVariable long roomId) {
    meetingRoomService.softDelete(auth.getName(), roomId);
    return ResponseEntity.ok().build();
  }
  
  // 회의실 하드 삭제
  @DeleteMapping("/meeting-rooms/{roomId}/hard-delete")
  public void hardDelete(Authentication auth, @PathVariable long roomId) {
    meetingRoomService.hardDelete(auth.getName(), roomId);
  }
  
  // ============================
  //   매출 파티션 관리
  // ============================
  
  @GetMapping("/sales-partitions")
  public ResponseEntity<List<SalesPartitionDto>> listSalesPartitions(
      @RequestParam(value = "partitionType", required = false) String partitionType,
      @RequestParam(value = "keyword", required = false) String keyword,
      Authentication auth
  ) {
    String email = auth.getName();
    return ResponseEntity.ok(
        salesPartitionService.getPartitions(email, partitionType, keyword)
    );
  }
  
  @PostMapping("/sales-partitions")
  public ResponseEntity<Void> addSalesPartition(@RequestBody SalesPartitionDto body,
                                                Authentication auth) {
    String email = auth.getName();
    salesPartitionService.createPartition(email, body);
    return ResponseEntity.ok().build();
  }
  
  @PutMapping("/sales-partitions/{partitionId}")
  public ResponseEntity<Void> updateSalesPartition(@PathVariable Long partitionId,
                                                   @RequestBody SalesPartitionDto body,
                                                   Authentication auth) {
    String email = auth.getName();
    salesPartitionService.updatePartition(email, partitionId, body);
    return ResponseEntity.ok().build();
  }
  
  @DeleteMapping("/sales-partitions/{partitionId}")
  public ResponseEntity<Void> deleteSalesPartition(@PathVariable Long partitionId,
                                                   Authentication auth) {
    String email = auth.getName();
    salesPartitionService.deletePartition(email, partitionId);
    return ResponseEntity.ok().build();
  }
  
}