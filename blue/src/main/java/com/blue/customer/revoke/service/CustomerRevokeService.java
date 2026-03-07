package com.blue.customer.revoke.service;

import com.blue.customer.all.dto.PagedResponse;
import com.blue.customer.allocate.dto.CenterPickDto;
import com.blue.customer.allocate.dto.UserPickDto;
import com.blue.customer.alloclog.dto.AllocLogInsertDto;
import com.blue.customer.alloclog.dto.CustomerOwnerSnapshotDto;
import com.blue.customer.alloclog.mapper.AllocLogMapper;
import com.blue.customer.center.dto.CenterSimpleDto;
import com.blue.customer.revoke.dto.*;
import com.blue.customer.revoke.mapper.CustomerRevokeMapper;
import com.blue.customer.allocate.mapper.CustomerAllocateMapper;
import com.blue.global.exception.AuthException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerRevokeService {
  
  private final CustomerRevokeMapper mapper;
  private final AllocLogMapper allocLogMapper;
  private final CustomerAllocateMapper allocateMapper;
  
  public PagedResponse<RevokeListRowDto> list(String callerEmail, int page, int size,
                                              String keyword, String dateFrom, String dateTo,
                                              String category, String division, String status,
                                              String prevStatus, String sort, Long filterCenterId) {
    UserContextDto me = mapper.findUserContextByEmail(callerEmail);
    if (me == null) throw new AuthException("인증 사용자 정보를 찾을 수 없습니다.",HttpStatus.GONE);
    
    int offset = (page - 1) * size;
    List<RevokeListRowDto> items;
    int total;
    
    switch (me.getRole()) {
      case "SUPERADMIN" -> {
        items = mapper.findListForHq(offset, size, keyword, dateFrom, dateTo, category, division, status, prevStatus, sort, me.getVisible(), filterCenterId);
        total = mapper.countListForHq(keyword, dateFrom, dateTo, category, division, status, prevStatus, me.getVisible(), filterCenterId);
      }
      case "MANAGER" -> {
        items = mapper.findListForManager(offset, size, keyword, dateFrom, dateTo, category, status, prevStatus, sort, me.getCenterId(), me.getVisible(), me.getUserId());
        total = mapper.countListForManager(keyword, dateFrom, dateTo, category, status, prevStatus, me.getCenterId(), me.getVisible(), me.getUserId());
      }
      case "CENTERHEAD" -> {
        items = mapper.findListForCenterHead(offset, size, keyword, dateFrom, dateTo, category, status, prevStatus, sort, me.getCenterId(), me.getVisible(), me.getUserId(), filterCenterId);
        total = mapper.countListForCenterHead(keyword, dateFrom, dateTo, category, status, prevStatus, me.getCenterId(), me.getVisible(), me.getUserId(), filterCenterId);
      }
      case "EXPERT" -> {
        items = mapper.findListForExpert(offset, size, keyword, dateFrom, dateTo, category, status, prevStatus, sort, me.getExpertId(), me.getVisible(), filterCenterId);
        total = mapper.countListForExpert(keyword, dateFrom, dateTo, category, status, prevStatus, me.getExpertId(), me.getVisible(), filterCenterId);
      }
      default -> throw new AuthException("이 메뉴는 분배권한이 있는 사용자만 사용할 수 있습니다.",HttpStatus.GONE);
    }
    
    int totalPages = (int) Math.ceil((double) total / size);
    return new PagedResponse<>(items, totalPages, total);
  }
  
  @Transactional
  public RevokeResult revokeByHq(String callerEmail, RevokeReq req) {
    UserContextDto me = mapper.findUserContextByEmail(callerEmail);
    if (me == null || !"SUPERADMIN".equals(me.getRole())) {
      throw new AuthException("본사만 수행할 수 있습니다.",HttpStatus.GONE);
    }
    if (req.getCustomerIds() == null || req.getCustomerIds().isEmpty()) {
      return new RevokeResult(0, 0);
    }
    
    // 대상 잠금 (상태 NOT IN 없음/회수)
    List<Long> lockIds = mapper.lockCustomersForRevokeByHq(req.getCustomerIds());
    if (lockIds.isEmpty()) return new RevokeResult(0, req.getCustomerIds().size());
    
    // 회수 직전 담당자 스냅샷
    List<CustomerOwnerSnapshotDto> owners = allocLogMapper.findOwnersByCustomerIds(lockIds);
    
//    // [사용안함] 매출 초기화 로그 기록
//    mapper.insertRevokeSalesLogs(lockIds, me.getUserId());
//
//    // [사용안함] 내방 이벤트 삭제
//    mapper.deleteSchedulesByCustomerIds(lockIds);
    
    // 회수 처리: 상태=회수, 프로=NULL, 예약시간=NULL, 최초/업셀 매출 = 0
    mapper.updateToRevoked(lockIds);
    
    // 회수 로그 기록
    writeRevokeLogs(lockIds, owners, me.getUserId(), null);
    
    return new RevokeResult(lockIds.size(), req.getCustomerIds().size() - lockIds.size());
  }
  
  @Transactional
  public RevokeResult revokeByManager(String callerEmail, RevokeReq req) {
    UserContextDto me = mapper.findUserContextByEmail(callerEmail);
    if (me == null || !"MANAGER".equals(me.getRole())) {
      throw new AuthException("팀장만 수행할 수 있습니다.",HttpStatus.GONE);
    }
    if (req.getCustomerIds() == null || req.getCustomerIds().isEmpty()) {
      return new RevokeResult(0, 0);
    }
    
    // 본인 포함 + 같은 팀 범위만 락
    List<Long> lockIds = mapper.lockCustomersForRevokeByManager(req.getCustomerIds(), me.getCenterId(), me.getUserId());
    if (lockIds.isEmpty()) return new RevokeResult(0, req.getCustomerIds().size());
    
    // 회수 직전 담당자 스냅샷
    List<CustomerOwnerSnapshotDto> owners = allocLogMapper.findOwnersByCustomerIds(lockIds);
    
//    // [사용안함] 매출 초기화 로그 기록
//    mapper.insertRevokeSalesLogs(lockIds, me.getUserId());
//
//    // [사용안함] 내방 이벤트 삭제
//    mapper.deleteSchedulesByCustomerIds(lockIds);
    
    // 회수 처리
    mapper.updateToManagerPool(lockIds, me.getUserId());
    
    // 회수 로그 기록
    writeRevokeLogs(lockIds, owners, me.getUserId(), "MANAGER_POOL");
    
    return new RevokeResult(lockIds.size(), req.getCustomerIds().size() - lockIds.size());
  }
  
  @Transactional
  public RevokeResult revokeByCenterHead(String callerEmail, RevokeReq req) {
    UserContextDto me = mapper.findUserContextByEmail(callerEmail);
    if (me == null || !"CENTERHEAD".equals(me.getRole())) {
      throw new AuthException("센터장만 수행할 수 있습니다.",HttpStatus.GONE);
    }
    if (req.getCustomerIds() == null || req.getCustomerIds().isEmpty()) {
      return new RevokeResult(0, 0);
    }
    
    // 센터 범위 내 대상만 락
    List<Long> lockIds = mapper.lockCustomersForRevokeByCenterHead(req.getCustomerIds(), me.getCenterId(), me.getUserId());
    if (lockIds.isEmpty()) return new RevokeResult(0, req.getCustomerIds().size());
    
    // 회수 직전 담당자 스냅샷
    List<CustomerOwnerSnapshotDto> owners = allocLogMapper.findOwnersByCustomerIds(lockIds);
    
//    // [사용안함] 매출 초기화 로그 기록
//    mapper.insertRevokeSalesLogs(lockIds, me.getUserId());
//
//    // [사용안함] 내방 이벤트 삭제
//    mapper.deleteSchedulesByCustomerIds(lockIds);
    
    // 회수 처리
    mapper.updateToRevoked(lockIds);
    
    // 회수 로그 기록
    writeRevokeLogs(lockIds, owners, me.getUserId(), null);
    
    return new RevokeResult(lockIds.size(), req.getCustomerIds().size() - lockIds.size());
  }
  
  @Transactional
  public RevokeResult revokeByExpert(String callerEmail, RevokeReq req) {
    UserContextDto me = mapper.findUserContextByEmail(callerEmail);
    if (me == null || !"EXPERT".equals(me.getRole())) {
      throw new AuthException("전문가만 수행할 수 있습니다.",HttpStatus.GONE);
    }
    if (req.getCustomerIds() == null || req.getCustomerIds().isEmpty()) {
      return new RevokeResult(0, 0);
    }
    
    // 본인 담당만 락
    List<Long> lockIds = mapper.lockCustomersForRevokeByExpert(req.getCustomerIds(), me.getExpertId());
    if (lockIds.isEmpty()) return new RevokeResult(0, req.getCustomerIds().size());
    
    // 회수 직전 담당자 스냅샷
    List<CustomerOwnerSnapshotDto> owners = allocLogMapper.findOwnersByCustomerIds(lockIds);
    
//    // [사용안함] 매출 초기화 로그 기록
//    mapper.insertRevokeSalesLogs(lockIds, me.getUserId());
//
//    // [사용안함] 내방 이벤트 삭제
//    mapper.deleteSchedulesByCustomerIds(lockIds);
    
    // 회수 처리
    mapper.updateToRevoked(lockIds);
    
    // 회수 로그 기록
    writeRevokeLogs(lockIds, owners, me.getUserId(), null);
    
    return new RevokeResult(lockIds.size(), req.getCustomerIds().size() - lockIds.size());
  }
  
  
  /**
   * 회수 로그 기록 공용 메서드
   * - from_user_id : 회수 직전 담당자
   * - to_user_id   : NULL (회수)
   * - is_final_assign:
   *      MANAGER + 상태 '없음'  => 0 (팀장 풀 회수, 통계 제외)
   *      그 외                    => 1 (확정 DB 회수, 통계에 반영)
   */
  private void writeRevokeLogs(List<Long> customerIds,
                               List<CustomerOwnerSnapshotDto> owners,
                               Long actedByUserId,
                               String memo) {
    if (customerIds == null || customerIds.isEmpty()) return;
    if (owners == null || owners.isEmpty()) return;
    
    Map<Long, CustomerOwnerSnapshotDto> ownerMap = owners.stream()
        .collect(Collectors.toMap(CustomerOwnerSnapshotDto::getCustomerId, o -> o));
    
    List<AllocLogInsertDto> logs = new ArrayList<>();
    
    for (Long customerId : customerIds) {
      CustomerOwnerSnapshotDto snap = ownerMap.get(customerId);
      if (snap == null || snap.getUserId() == null) {
        // 담당자 없는 건은 통계/로그 대상에서 제외
        continue;
      }
      
      boolean isManagerPool = "MANAGER".equals(snap.getUserRole()) && "없음".equals(snap.getStatus());
      
      int finalFlag = isManagerPool ? 0 : 1;
      
      AllocLogInsertDto dto = new AllocLogInsertDto();
      dto.setCustomerId(customerId);
      dto.setActionType("REVOKE");
      dto.setFromUserId(snap.getUserId());
      
      Long toUserId = "MANAGER_POOL".equals(memo) ? actedByUserId : null;
      dto.setToUserId(toUserId);
      
      dto.setActedByUserId(actedByUserId);
      dto.setIsFinalAssign(finalFlag);
      dto.setMemo(memo);
      logs.add(dto);
    }
    
    if (!logs.isEmpty()) {
      allocLogMapper.insertLogs(logs);
    }
  }
  
  // 이관기능
  @Transactional
  public RevokeResult transferBySuper(String callerEmail, TransferReq req) {
    UserContextDto me = mapper.findUserContextByEmail(callerEmail);
    
    if (me == null || !"SUPERADMIN".equals(me.getRole()) || !"Y".equals(me.getIsSuper())) {
      throw new AuthException("이관 권한이 없습니다.", HttpStatus.GONE);
    }
    
    if (req.getCustomerIds() == null || req.getCustomerIds().isEmpty()) {
      return new RevokeResult(0, 0);
    }
    if (req.getTargetCenterId() == null) {
      throw new AuthException("팀을 선택하세요.", HttpStatus.GONE);
    }
    if (req.getTargetUserId() == null) {
      throw new AuthException("직원을 선택하세요.", HttpStatus.GONE);
    }
    
    Integer ok = allocateMapper.userBelongsToCenter(req.getTargetUserId(), req.getTargetCenterId());
    if (ok == null || ok == 0) {
      throw new AuthException("선택한 직원이 해당 팀 소속이 아닙니다.", HttpStatus.GONE);
    }
    
    String targetRole = allocateMapper.findUserRole(req.getTargetUserId());
    boolean treatAsPersonal =
        !"MANAGER".equals(targetRole) || Boolean.TRUE.equals(req.getAssignToManagerAsStaff());
    
    List<Long> lockIds = mapper.lockCustomersForRevokeByHq(req.getCustomerIds());
    if (lockIds.isEmpty()) {
      return new RevokeResult(0, req.getCustomerIds().size());
    }
    
    // 이관 직전 담당자 스냅샷
    List<CustomerOwnerSnapshotDto> owners = allocLogMapper.findOwnersByCustomerIds(lockIds);
    
    // [1] 자기 자신에게 이관 금지
    boolean hasSameOwner = owners.stream()
        .anyMatch(o -> o.getUserId() != null && o.getUserId().equals(req.getTargetUserId()));
    
    if (hasSameOwner) {
      throw new AuthException("현재 담당자와 동일한 사용자에게는 이관할 수 없습니다.", HttpStatus.GONE);
    }

    // [2] 같은 팀으로 이관 금지
    boolean hasSameCenter = owners.stream()
        .anyMatch(o -> o.getCenterId() != null && o.getCenterId().equals(req.getTargetCenterId()));
    
    if (hasSameCenter) {
      throw new AuthException("현재 담당 팀과 동일한 팀으로는 이관할 수 없습니다.", HttpStatus.GONE);
    }
    
    // 실제 상태 변화는 "회수 후 분배"와 동일 효과
    mapper.updateToRevoked(lockIds); // prev_status 백업 + 일정 정리 + user_id NULL + status='회수'
    
    allocateMapper.insertPastForNewOwner(lockIds, req.getTargetUserId());
    allocateMapper.updateOwner(lockIds, req.getTargetUserId());
    
    if (treatAsPersonal) {
      allocateMapper.updateStatusToNew(lockIds);
    } else {
      allocateMapper.updateStatusToNone(lockIds);
    }
    
    writeTransferLogs(
        lockIds,
        owners,
        me.getUserId(),
        req.getTargetUserId(),
        treatAsPersonal,
        treatAsPersonal ? null : "MANAGER_POOL"
    );
    
    return new RevokeResult(lockIds.size(), req.getCustomerIds().size() - lockIds.size());
  }
  
  // 이관기능 전용 로그 기록 메소드
  private void writeTransferLogs(List<Long> customerIds,
                                 List<CustomerOwnerSnapshotDto> owners,
                                 Long actedByUserId,
                                 Long toUserId,
                                 boolean treatAsPersonal,
                                 String memo) {
    if (customerIds == null || customerIds.isEmpty()) return;
    if (owners == null || owners.isEmpty()) return;
    
    Map<Long, CustomerOwnerSnapshotDto> ownerMap = owners.stream()
        .collect(Collectors.toMap(CustomerOwnerSnapshotDto::getCustomerId, o -> o));
    
    List<AllocLogInsertDto> logs = new ArrayList<>();
    
    for (Long customerId : customerIds) {
      CustomerOwnerSnapshotDto snap = ownerMap.get(customerId);
      if (snap == null || snap.getUserId() == null) continue;
      
      boolean fromIsManagerPool =
          "MANAGER".equals(snap.getUserRole()) && "없음".equals(snap.getStatus());
      
      int fromFinalFlag = fromIsManagerPool ? 0 : 1;
      int toFinalFlag = treatAsPersonal ? 1 : 0;
      
      AllocLogInsertDto dto = new AllocLogInsertDto();
      dto.setCustomerId(customerId);
      dto.setActionType("TRANSFER");
      dto.setFromUserId(snap.getUserId());
      dto.setToUserId(toUserId);
      dto.setActedByUserId(actedByUserId);
      
      dto.setIsFinalAssign(0); // 기존 컬럼 호환용
      dto.setFromIsFinalAssign(fromFinalFlag);
      dto.setToIsFinalAssign(toFinalFlag);
      
      dto.setMemo(memo);
      logs.add(dto);
    }
    
    if (!logs.isEmpty()) {
      allocLogMapper.insertLogs(logs);
    }
  }
  
  // 이관가능 팀 조회 (현재 소속된 팀 제외하고 리턴)
  public List<CenterPickDto> getTransferCenters(String callerEmail, List<Long> excludeCenterIds) {
    UserContextDto me = mapper.findUserContextByEmail(callerEmail);
    if (me == null || !"SUPERADMIN".equals(me.getRole()) || !"Y".equals(me.getIsSuper())) {
      throw new AuthException("이관 권한이 없습니다.", HttpStatus.GONE);
    }
    
    List<CenterPickDto> centers = allocateMapper.findCentersForAllocate();
    if (excludeCenterIds == null || excludeCenterIds.isEmpty()) return centers;
    
    return centers.stream()
        .filter(c -> c.getCenterId() == null || !excludeCenterIds.contains(c.getCenterId()))
        .toList();
  }
  
  // 이관가능 팀원 조회 (현재 팀원이 소속된 팀 제외하고 리턴)
  public List<UserPickDto> getTransferUsers(String callerEmail, Long centerId, String q, List<Long> excludeCenterIds) {
    UserContextDto me = mapper.findUserContextByEmail(callerEmail);
    if (me == null || !"SUPERADMIN".equals(me.getRole()) || !"Y".equals(me.getIsSuper())) {
      throw new AuthException("이관 권한이 없습니다.", HttpStatus.GONE);
    }
    
    if (centerId == null) return Collections.emptyList();
    if (excludeCenterIds != null && excludeCenterIds.contains(centerId)) {
      return Collections.emptyList();
    }
    
    return allocateMapper.searchStaffForAllocate(centerId, q);
  }
  
  // 팀 드롭박스
  public List<CenterSimpleDto> getRevokeTeams(String callerEmail) {
    UserContextDto me = mapper.findUserContextByEmail(callerEmail);
    if (me == null) {
      throw new AuthException("인증 사용자 정보를 찾을 수 없습니다.", HttpStatus.GONE);
    }
    
    return switch (me.getRole()) {
      case "SUPERADMIN" -> mapper.findRevokeTeamsForHq();
      case "CENTERHEAD" -> mapper.findRevokeTeamsForCenterHead(me.getCenterId());
      case "EXPERT" -> mapper.findRevokeTeamsForExpert(me.getExpertId());
      default -> throw new AuthException("팀 필터 조회 권한이 없습니다.", HttpStatus.GONE);
    };
  }
}
