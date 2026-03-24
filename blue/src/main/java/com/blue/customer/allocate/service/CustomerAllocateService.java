package com.blue.customer.allocate.service;

import com.blue.customer.all.dto.PagedResponse;
import com.blue.customer.allocate.dto.*;
import com.blue.customer.allocate.mapper.CustomerAllocateMapper;
import com.blue.global.exception.AuthException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import com.blue.customer.alloclog.mapper.AllocLogMapper;
import com.blue.customer.alloclog.dto.AllocLogInsertDto;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerAllocateService {
  
  private final CustomerAllocateMapper mapper;
  private final AllocLogMapper allocLogMapper;
  
  public PagedResponse<AllocateListRowDto> list(String callerEmail, int page, int size,
                                                String keyword, String dateFrom, String dateTo,
                                                String category, String division, String prevStatus, String sort) {
    UserContextDto me = mapper.findUserContextByEmail(callerEmail);
    if (me == null) throw new AuthException("인증 사용자 정보를 찾을 수 없습니다.", HttpStatus.GONE);
    
    int offset = (page - 1) * size;
    List<AllocateListRowDto> items;
    int total;
    
    switch (me.getRole()) {
      case "SUPERADMIN" -> {
        // HQ 리스트는 "담당 프로 없음 AND 상태 ∈ {없음, 회수}"가 XML에서 강제됨
        items = mapper.findListForHq(offset, size, keyword, dateFrom, dateTo, category, division, prevStatus, sort, me.getVisible());
        total = mapper.countListForHq(keyword, dateFrom, dateTo, category, division, prevStatus, me.getVisible());
      }
      case "MANAGER" -> {
        // MANAGER 리스트는 "담당 프로=나 AND 상태=없음"이 XML에서 강제됨
        items = mapper.findListForManager(offset, size, keyword, dateFrom, dateTo, category, prevStatus, sort, me.getUserId(), me.getVisible());
        total = mapper.countListForManager(keyword, dateFrom, dateTo, category, prevStatus, me.getUserId(), me.getVisible());
      }
      case "CENTERHEAD" -> {
        if (!"Y".equals(me.getCanAllocate())) {
          return new PagedResponse<>(Collections.emptyList(), 0, 0);
        }
        items = mapper.findListForCenterHead(offset, size, keyword, dateFrom, dateTo, category, division, prevStatus, sort, me.getUserId(), me.getVisible(), me.getCenterId());
        total = mapper.countListForCenterHead(keyword, dateFrom, dateTo, category, division, prevStatus, me.getUserId(), me.getVisible(), me.getCenterId());
      }
      case "EXPERT" -> {
        if (!"Y".equals(me.getCanAllocate())) {
          return new PagedResponse<>(Collections.emptyList(), 0, 0);
        }
        items = mapper.findListForExpert(offset, size, keyword, dateFrom, dateTo, category, division, prevStatus, sort, me.getUserId(), me.getVisible(), me.getExpertId());
        total = mapper.countListForExpert(keyword, dateFrom, dateTo, category, division, prevStatus, me.getUserId(), me.getVisible(), me.getExpertId());
      }
      
      default -> throw new AuthException("권한이 없습니다.", HttpStatus.GONE);
    }
    
    int totalPages = (int) Math.ceil((double) total / size);
    return new PagedResponse<>(items, totalPages, total);
  }
  
  @Transactional
  public AllocateResult allocateByHq(String callerEmail, AllocateHqReq req) {
    UserContextDto me = mapper.findUserContextByEmail(callerEmail);
    if (me == null || !"SUPERADMIN".equals(me.getRole())) {
      throw new AuthException("권한이 없습니다.", HttpStatus.GONE);
    }
    if (req.getCustomerIds() == null || req.getCustomerIds().isEmpty()) {
      return new AllocateResult(0, 0);
    }
    if (req.getTargetCenterId() == null) {
      throw new AuthException("팀을 선택하세요.", HttpStatus.GONE);
    }
    if (req.getTargetUserId() == null) {
      throw new AuthException("팀장 또는 직원을 반드시 선택해야 합니다.", HttpStatus.GONE);
    }
    
    Long targetUserId = req.getTargetUserId();
    Integer ok = mapper.userBelongsToCenter(targetUserId, req.getTargetCenterId());
    if (ok == null || ok == 0) {
      throw new AuthException("선택한 직원이 해당 팀 소속이 아닙니다.", HttpStatus.GONE);
    }
    
    String targetRole = mapper.findUserRole(targetUserId);
    
    // 조건에 맞는 대상만 잠금
    List<Long> lockIds = mapper.lockCustomersForHq(req.getCustomerIds());
    if (lockIds.isEmpty()) return new AllocateResult(0, req.getCustomerIds().size());
    
    // 새로 배정받는 사람(팀장 or 선택한 직원)을 과거이력에 추가
    mapper.insertPastForNewOwner(lockIds, targetUserId);
    
    // 소유자 변경
    mapper.updateOwner(lockIds, targetUserId);
    
    // 본사 기준 상태 변경 규칙
    // 1. 대상이 직원이거나
    // 2. 대상이 팀장이지만 '개인(Staff)처럼 분배(treatAsPersonal)' 옵션이 켜져있는 경우
    boolean treatAsPersonal = !"MANAGER".equals(targetRole) || Boolean.TRUE.equals(req.getAssignToManagerAsStaff());
    if (treatAsPersonal) {
      // 확정 분배: 상태 '신규' (팀장에게 직접 꽂아줄 때도 포함)
      mapper.updateStatusToNew(lockIds);
    } else {
      // 팀으로 분배: 상태 '없음' (팀장이 관리하는 Pool)
      mapper.updateStatusToNone(lockIds);
    }
    
    // ---- 분배 로그 기록 ----
    // HQ → MANAGER : 팀장풀에 쌓이는 것이므로 isFinalAssign = 0
    // HQ → STAFF   : 확정 DB 이므로 isFinalAssign = 1
    // boolean isFinalAssign = !"MANAGER".equals(targetRole);
    writeAssignLogs(
        lockIds,
        me.getUserId(),       // 분배를 실행한 사람 (현재 로그인한 사람)
        null,                 // from_user_id (담당자 없음 상태에서 분배되는 것이므로 null)
        targetUserId,
        treatAsPersonal,
        null                  // memo (필요하면 나중에 req.getMemo() 등으로 교체)
    );
    
    return new AllocateResult(lockIds.size(), req.getCustomerIds().size() - lockIds.size());
  }
  
  @Transactional
  public AllocateResult allocateByManager(String callerEmail, AllocateMgrReq req) {
    UserContextDto me = mapper.findUserContextByEmail(callerEmail);
    if (me == null || !"MANAGER".equals(me.getRole())) {
      throw new AuthException("권한이 없습니다.", HttpStatus.GONE);
    }
    if (req.getCustomerIds() == null || req.getCustomerIds().isEmpty()) {
      return new AllocateResult(0, 0);
    }
    if (req.getTargetUserId() == null) {
      throw new AuthException("직원을 선택하세요.", HttpStatus.GONE);
    }
    
    Integer ok = mapper.staffInSameCenter(me.getUserId(), req.getTargetUserId());
    if (ok == null || ok == 0) throw new AuthException("같은 팀의 직원만 분배할 수 있습니다.", HttpStatus.GONE);
    
    // 조건에 맞는 대상만 잠금(현재담당=팀장 본인 AND 상태=없음)
    List<Long> lockIds = mapper.lockCustomersForManager(req.getCustomerIds(), me.getUserId());
    if (lockIds.isEmpty()) return new AllocateResult(0, req.getCustomerIds().size());
    
    // 팀장이 '팀원에게' 분배하는 경우, 팀장 과거 이력 삭제 후 팀원 본인 이력 추가
    boolean selfReallocate = req.getTargetUserId().equals(me.getUserId());
    if (!selfReallocate) {
      mapper.deleteManagerFromPast(lockIds, me.getUserId());
      mapper.insertPastForNewOwner(lockIds, req.getTargetUserId());
    }
    
    // 소유자 변경
    mapper.updateOwner(lockIds, req.getTargetUserId());
    
    // 팀장→본인/팀원 모두 '신규'
    mapper.updateStatusToNew(lockIds);
    
    // ---- 분배 로그 기록 ----
    Long managerId = me.getUserId();
    Long targetUserId = req.getTargetUserId();
    
    // 여기서부터는 전부 "팀장 풀 → 확정 DB" 케이스이므로 isFinalAssign = 1 고정
    if (selfReallocate) {
      // 팀장 풀 → 팀장 본인 확정
      writeAssignLogs(
          lockIds,
          managerId,       // 분배를 실행한 사람 (현재 로그인한 사람)
          managerId,       // from_user_id (풀의 주인)
          managerId,       // to_user_id (팀장 본인 확정 - 스스로에게 재분배)
          true,
          null
      );
    } else {
      // 팀장 풀 → 직원 확정
      writeAssignLogs(
          lockIds,
          managerId,       // 분배를 실행한 사람 (현재 로그인한 사람 -> 팀장이 실행)
          managerId,       // from_user_id (풀의 주인)
          targetUserId,    // to_user_id (팀장이 하위 팀원에게 분배한 경우)
          true,
          null
      );
    }
    
    return new AllocateResult(lockIds.size(), req.getCustomerIds().size() - lockIds.size());
  }
  
  @Transactional
  public AllocateResult allocateByExpertHead(String callerEmail, AllocateHqReq req) {
    UserContextDto me = mapper.findUserContextByEmail(callerEmail);
    if (me == null) throw new AuthException("사용자 정보가 없습니다.", HttpStatus.GONE);
    
    // 1. 권한 체크
    boolean isCenterHead = "CENTERHEAD".equals(me.getRole());
    boolean isExpert = "EXPERT".equals(me.getRole());
    
    if (!isCenterHead && !isExpert) throw new AuthException("권한이 없습니다.", HttpStatus.GONE);
    if (!"Y".equals(me.getCanAllocate())) throw new AuthException("분배 권한이 없습니다.", HttpStatus.GONE);
    
    // 2. 검증
    if (req.getCustomerIds() == null || req.getCustomerIds().isEmpty()) return new AllocateResult(0, 0);
    if (req.getTargetCenterId() == null) throw new AuthException("팀을 선택하세요.", HttpStatus.GONE);
    if (req.getTargetUserId() == null) throw new AuthException("팀장 또는 직원을 반드시 선택해야 합니다.", HttpStatus.GONE);
    
    // 3. 타겟 직원이 "타겟 센터"에 소속되어 있는지는 확인해야 함 (데이터 무결성)
    Integer ok = mapper.userBelongsToCenter(req.getTargetUserId(), req.getTargetCenterId());
    if (ok == null || ok == 0) {
      throw new AuthException("선택한 직원이 해당 팀 소속이 아닙니다.", HttpStatus.GONE);
    }
    
    // 4. 잠금 (Lock) - 내 권한(출처)에 맞는 DB인지 확인
    List<Long> lockIds;
    if (isCenterHead) {
      lockIds = mapper.lockCustomersForCenterHead(req.getCustomerIds(), me.getUserId(), me.getCenterId());
    } else {
      lockIds = mapper.lockCustomersForExpert(req.getCustomerIds(), me.getUserId(), me.getExpertId());
    }
    if (lockIds.isEmpty()) return new AllocateResult(0, req.getCustomerIds().size());
    
    // 5. 분배 실행
    Long targetUserId = req.getTargetUserId();
    String targetRole = mapper.findUserRole(targetUserId);
    
    // 이력 추가 & 소유자 변경
    mapper.insertPastForNewOwner(lockIds, targetUserId);
    mapper.updateOwner(lockIds, targetUserId);
    
    // 상태 변경
    boolean treatAsPersonal = !"MANAGER".equals(targetRole) || Boolean.TRUE.equals(req.getAssignToManagerAsStaff());
    if (treatAsPersonal) {
      mapper.updateStatusToNew(lockIds);
    } else {
      mapper.updateStatusToNone(lockIds);
    }
    
    // 로그
    writeAssignLogs(lockIds, me.getUserId(), me.getUserId(), targetUserId, treatAsPersonal, null);
    
    return new AllocateResult(lockIds.size(), req.getCustomerIds().size() - lockIds.size());
  }
  
  // 직원 검색
  public List<UserPickDto> searchUsersForAllocate(String email, Long centerId, String q) {
    UserContextDto me = mapper.findUserContextByEmail(email);
    if (me == null) throw new AuthException("No user", HttpStatus.GONE);
    
    Long targetCenterId = null;
    switch (me.getRole()) {
      case "SUPERADMIN", "CENTERHEAD", "EXPERT" -> {
        targetCenterId = centerId; // null이면 전체
      }
      case "MANAGER" -> {
        // 매니저는 항상 본인 팀 강제 (전달 centerId는 무시)
        if (me.getCenterId() == null) throw new AuthException("팀이 미배정 상태입니다.", HttpStatus.GONE);
        targetCenterId = me.getCenterId();
      }
      default -> throw new AuthException("Forbidden", HttpStatus.GONE);
    }
    
    // 같은 팀의 MANAGER/STAFF 모두 반환
    return mapper.searchStaffForAllocate(targetCenterId, q);
  }
  
  // 팀 조회
  public List<CenterPickDto> centersForAllocate(String callerEmail) {
    UserContextDto me = mapper.findUserContextByEmail(callerEmail);
    if (me == null || (
        !"SUPERADMIN".equals(me.getRole()) &&
        !"CENTERHEAD".equals(me.getRole()) &&
        !"EXPERT".equals(me.getRole())
    )) {
      throw new AuthException("권한이 없습니다.", HttpStatus.GONE);
    }
    return mapper.findCentersForAllocate();
  }
  
  // 분배기록 공통 헬퍼 매서드
  private void writeAssignLogs(List<Long> customerIds,
                               Long actedByUserId,
                               Long fromUserId,
                               Long toUserId,
                               boolean finalAssign,
                               String memo) {
    
    if (customerIds == null || customerIds.isEmpty()) return;
    
    List<AllocLogInsertDto> logs = new java.util.ArrayList<>();
    Integer finalFlag = finalAssign ? 1 : 0;
    
    for (Long id : customerIds) {
      AllocLogInsertDto dto = new AllocLogInsertDto();
      dto.setCustomerId(id);
      dto.setActionType("ASSIGN");
      dto.setFromUserId(fromUserId);
      dto.setToUserId(toUserId);
      dto.setActedByUserId(actedByUserId);
      dto.setIsFinalAssign(finalFlag);
      dto.setMemo(memo);
      logs.add(dto);
    }
    
    allocLogMapper.insertLogs(logs);
  }
  
  // 담당자 이력 초기화 함수
  @Transactional
  public void resetHistory(String callerEmail, List<Long> customerIds) {
    // 1. 권한 검사
    UserContextDto me = mapper.findUserContextByEmail(callerEmail);
    if (me == null || !"SUPERADMIN".equals(me.getRole())) {
      throw new AuthException("권한이 없습니다.", HttpStatus.GONE);
    }
    
    // 2. 유효성 검사
    if (customerIds == null || customerIds.isEmpty()) {
      return;
    }
    
    // 3. 삭제 실행
    mapper.deleteHistoryByCustomerIds(customerIds);
  }
  
  // 모달에서 : 담당자 이력 리스트 조회
  public List<CustomerHistoryRowDto> getHistory(Long customerId) {
    if (customerId == null) {
      return Collections.emptyList();
    }
    return mapper.findHistoryByCustomerId(customerId);
  }
  
  // 모달에서 : 담당자 이력 선택 삭제
  @Transactional
  public int deleteHistory(List<Long> logIds) {
    if (logIds == null || logIds.isEmpty()) {
      return 0;
    }
    return mapper.deleteHistorySafely(logIds);
  }
  
  // 모달에서 : 슈퍼계정 전용 전문가 DB 집계 표 출력
  public List<AllocateExpertDailySummaryRowDto> getExpertDailySummary(String callerEmail, List<Long> customerIds) {
    UserContextDto me = mapper.findUserContextByEmail(callerEmail);
    
    if (me == null || !"SUPERADMIN".equals(me.getRole()) || !"Y".equals(me.getIsSuper())) {
      throw new AuthException("권한이 없습니다.", HttpStatus.GONE);
    }
    
    if (customerIds == null || customerIds.isEmpty()) {
      return Collections.emptyList();
    }
    
    return mapper.findExpertDailySummary(customerIds);
  }
}
