package com.blue.customer.all.service;

import com.blue.customer.all.dto.*;
import com.blue.customer.all.mapper.CustomerAllMapper;
import com.blue.global.exception.AuthException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerAllService {
  
  private final CustomerAllMapper mapper;
  
  public PagedResponse<AllDbRowDto> getAll(
      String callerEmail, int page, int size,
      String keyword, String dateFrom, String dateTo,
      String category, String division, String sort, String expertName,
      String status, String prevStatus, String mine, Long staffUserId
  ) {
    UserContextDto me = mapper.findUserContextByEmail(callerEmail);
    if (me == null) throw new AuthException("인증 사용자 정보를 찾을 수 없습니다.", HttpStatus.GONE);
    
    int offset = (page - 1) * size;
    List<AllDbRowDto> items;
    int total;
    
    switch (me.getRole()) {
      case "SUPERADMIN" -> {
//        System.out.println("datefrom: " + dateFrom);
//        System.out.println("dateto: " + dateTo);
        items = mapper.findAllForAdmin(offset, size, keyword, dateFrom, dateTo, category, division, sort, expertName, status, prevStatus, me.getVisible());
        total = mapper.countAllForAdmin(keyword, dateFrom, dateTo, category, division, expertName, status, prevStatus, me.getVisible());
      }
      case "MANAGER" -> {
        // mine=Y이면 '내 DB만' → 클라 staffUserId 무시, 토큰의 본인 ID 강제
        boolean mineOnly = "Y".equalsIgnoreCase(mine);
        if (mineOnly) {
          Long myUserId = me.getUserId();
          items = mapper.findAllForStaff(
              offset, size, keyword, dateFrom, dateTo, category, division, sort, expertName, status, prevStatus, myUserId
          );
          total = mapper.countAllForStaff(
              keyword, dateFrom, dateTo, category, division, expertName, status, prevStatus, myUserId
          );
        } else {
          // 팀 범위
          items = mapper.findAllForManager(
              offset, size, keyword, dateFrom, dateTo, category, division, sort, expertName, status, prevStatus, me.getCenterId()
          );
          total = mapper.countAllForManager(
              keyword, dateFrom, dateTo, category, division, expertName, status, prevStatus, me.getCenterId()
          );
        }
      }
      case "STAFF" -> {
        // STAFF는 원래 '내 DB'만
        items = mapper.findAllForStaff(offset, size, keyword, dateFrom, dateTo, category, division, sort, expertName, status, prevStatus, me.getUserId());
        total = mapper.countAllForStaff(keyword, dateFrom, dateTo, category, division, expertName, status, prevStatus, me.getUserId());
      }
      case "CENTERHEAD" -> {
        // 센터장: 본인 센터 소속 전문가들의 출처 데이터 조회 (중복DB 포함, 이동 권한 O)
        items = mapper.findAllForCenterHead(offset, size, keyword, dateFrom, dateTo, category, division, sort, expertName, status, prevStatus, me.getVisible(), me.getCenterId());
        total = mapper.countAllForCenterHead(keyword, dateFrom, dateTo, category, division, expertName, status, prevStatus, me.getVisible(), me.getCenterId());
      }
      case "EXPERT" -> {
        // 전문가: 본인 출처 데이터 조회 (중복DB 포함, 이동 권한 O)
        Long myExpertId = mapper.findExpertIdByUserId(me.getUserId());
        if (myExpertId == null) {
          // 전문가 계정인데 연결된 전문가 정보가 없는 경우
          // 조회 결과 없음
          items = List.of();
          total = 0;
        } else {
          items = mapper.findAllForExpert(offset, size, keyword, dateFrom, dateTo, category, division, sort, expertName, status, prevStatus, me.getVisible(), myExpertId);
          total = mapper.countAllForExpert(keyword, dateFrom, dateTo, category, division, expertName, status, prevStatus, me.getVisible(), myExpertId);
        }
      }
      default -> throw new AuthException("Unknown role: " + me.getRole(), HttpStatus.GONE);
    }
    
    // SUPERADMIN, CENTERHEAD, EXPERT이지만 가시권한이 N이면 전화번호 마스킹
    if (List.of("SUPERADMIN", "CENTERHEAD", "EXPERT").contains(me.getRole()) && "N".equalsIgnoreCase(me.getVisible())) {
      items.forEach(r -> r.setPhone(maskPhone(r.getPhone())));
    }
    
    int totalPages = (int) Math.ceil((double) total / size);
    return new PagedResponse<>(items, totalPages, total);
  }
  
  private String maskPhone(String phone) {
    if (phone == null) return null;
    
    // 숫자만 뽑고 재조립
    String digits = phone.replaceAll("\\D", "");
    if (digits.length() < 7) return phone; // 너무 짧으면 원본
    
    String first = digits.substring(0, 3);
    String last  = digits.substring(digits.length() - 4);
    return first + "-****-" + last;
  }
  
  // 배지/예약 수정 — 현재 담당 프로, 그 팀장, 본사만
  @Transactional
  public void updateField(String callerEmail, Long customerId, UpdateFieldDto dto) {
    UserContextDto me = mapper.findUserContextByEmail(callerEmail);
    if (me == null) throw new AuthException("인증 사용자 정보를 찾을 수 없습니다.", HttpStatus.GONE);
    
    // customers만 수정 가능(중복 차단)
    Integer exists = mapper.existsCustomerById(customerId);
    if (exists == null || exists == 0) throw new AuthException("중복 DB는 수정할 수 없습니다.", HttpStatus.GONE);
    
    // 권한 재검증
    switch (me.getRole()) {
      case "SUPERADMIN" -> { /* ok */ }
      case "MANAGER" -> {
        Integer ownsCenter = mapper.customerOwnedByCenter(customerId, me.getCenterId());
        if (ownsCenter == null || ownsCenter == 0) throw new AuthException("권한이 없습니다.", HttpStatus.GONE);
      }
      case "STAFF" -> {
        Integer ownsSelf = mapper.customerOwnedByUser(customerId, me.getUserId());
        if (ownsSelf == null || ownsSelf == 0) throw new AuthException("권한이 없습니다.", HttpStatus.GONE);
      }
      case "CENTERHEAD" -> {
        Integer ok = mapper.customerAccessibleForCenterHead(customerId, me.getCenterId());
        if (ok == null || ok == 0) throw new AuthException("권한이 없습니다.", HttpStatus.GONE);
      }
      
      case "EXPERT" -> {
        Long myExpertId = mapper.findExpertIdByUserId(me.getUserId());
        if (myExpertId == null) throw new AuthException("전문가 정보가 없습니다.", HttpStatus.GONE);
        Integer ok = mapper.customerAccessibleForExpert(customerId, myExpertId);
        if (ok == null || ok == 0) throw new AuthException("권한이 없습니다.", HttpStatus.GONE);
      }
      default -> throw new AuthException("Unknown role: " + me.getRole(), HttpStatus.GONE);
    }
    
    String field = dto.getField() == null ? "" : dto.getField().toLowerCase();
    switch (field) {
      case "reservation" -> {
        LocalDateTime when;
        
        try {
          if (dto.getValue() == null || dto.getValue().isBlank()) {
            when = null;
          } else {
            DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            when = LocalDateTime.parse(dto.getValue(), FMT);
          }
        } catch (DateTimeParseException e) {
          throw new AuthException("날짜 형식이 올바르지 않습니다. 예) 2025-09-21 14:00", HttpStatus.GONE);
        }
        mapper.updateCustomerReservation(customerId, when);
      }
      case "status" -> {
        String next = (dto.getValue() == null) ? null : dto.getValue().trim();
        if (next == null || next.isBlank()) throw new AuthException("상태값이 비어있습니다.", HttpStatus.GONE);
        
        String current = mapper.selectStatusOnly(customerId);
        
        // 내방 -> 다른 상태로 변경 시도, 일정이 존재하는 상태라면 변경 불가.
        if ("내방".equals(current) && !"내방".equals(next)) {
          Integer has = mapper.existsVisitSchedule(customerId);
          if (has != null && has > 0) {
            throw new AuthException("내방 예약시간이 있어 상태 변경이 불가합니다.\n내방일정 메뉴에서 일정삭제 후 시도하세요.", HttpStatus.GONE);
          }
        }
        
        // 다른 상태 -> 내방 으로 바뀌는 순간, promise_time(=reservation) 강제 null
        if ("내방".equals(next) && !"내방".equals(current)) {
          mapper.updateCustomerReservation(customerId, null);
        }
        
        mapper.updateCustomerStatus(customerId, dto.getValue());
      }
      case "name" -> {
        if (!"SUPERADMIN".equals(me.getRole())) {
          throw new AuthException("권한이 없습니다.", HttpStatus.GONE);
        }
        
        String nextName = dto.getValue() == null ? "" : dto.getValue().trim();
        if (nextName.isBlank()) {
          throw new AuthException("고객명이 비어있습니다.", HttpStatus.GONE);
        }
        
        mapper.updateCustomerName(customerId, nextName);
      }
      default -> throw new AuthException("지원하지 않는 필드: " + dto.getField(), HttpStatus.GONE);
    }
  }
  
  // 중복 숨김 — 본사만
  @Transactional
  public void hideDuplicates(String callerEmail, List<Long> duplicateIds) {
    UserContextDto me = mapper.findUserContextByEmail(callerEmail);
    if (me == null) throw new AuthException("인증 사용자 정보를 찾을 수 없습니다.", HttpStatus.GONE);
    if (!List.of("SUPERADMIN", "CENTERHEAD", "EXPERT").contains(me.getRole())) throw new AuthException("권한이 없습니다.", HttpStatus.GONE);
    if (duplicateIds == null || duplicateIds.isEmpty()) return;
    
    mapper.hideDuplicates(duplicateIds); // duplicate_display = 0
  }
  
  // 전문가 목록 조회
  public List<ExpertDto> getExpertList(String callerEmail) {
    UserContextDto me = mapper.findUserContextByEmail(callerEmail);
    if (me == null) throw new AuthException("사용자 정보를 찾을 수 없습니다.", HttpStatus.GONE);
    
    return switch (me.getRole()) {
      // 1. 본사, 매니저, 스탭 -> 모든 전문가 조회
      case "SUPERADMIN", "MANAGER", "STAFF" -> mapper.findAllExperts();
      
      // 2. 센터헤드 -> 자기 센터 소속원이 담당하는 전문가만 조회
      case "CENTERHEAD" -> mapper.findExpertsByCenterId(me.getCenterId());
      
      // 3. 그 외(EXPERT 등) -> 조회 권한 없음 (빈 리스트)
      default -> List.of();
    };
  }
  
  // 최초/업셀 매출 금액 업데이트 및 로그 기록
  @Transactional
  public void updateSales(String callerEmail, SalesUpdateDto dto) {
    UserContextDto me = mapper.findUserContextByEmail(callerEmail);
    if (me == null) throw new AuthException("인증 사용자 정보를 찾을 수 없습니다.", HttpStatus.GONE);
    
    Long customerId = dto.getCustomerId();
    
    // 1. DB 존재 확인
    Integer exists = mapper.existsCustomerById(customerId);
    if (exists == null || exists == 0) throw new AuthException("존재하지 않거나 중복 DB입니다.", HttpStatus.GONE);
    
    // 2. 권한 검증
    switch (me.getRole()) {
      case "SUPERADMIN" -> { }
      case "MANAGER" -> {
        Integer ownsCenter = mapper.customerOwnedByCenter(customerId, me.getCenterId());
        if (ownsCenter == null || ownsCenter == 0) throw new AuthException("권한이 없습니다.", HttpStatus.GONE);
      }
      case "STAFF" -> {
        Integer ownsSelf = mapper.customerOwnedByUser(customerId, me.getUserId());
        if (ownsSelf == null || ownsSelf == 0) throw new AuthException("권한이 없습니다.", HttpStatus.GONE);
      }
      case "CENTERHEAD" -> {
        Integer ok = mapper.customerAccessibleForCenterHead(customerId, me.getCenterId());
        if (ok == null || ok == 0) throw new AuthException("권한이 없습니다.", HttpStatus.GONE);
      }
      
      case "EXPERT" -> {
        Long myExpertId = mapper.findExpertIdByUserId(me.getUserId());
        if (myExpertId == null) throw new AuthException("전문가 정보가 없습니다.", HttpStatus.GONE);
        Integer ok = mapper.customerAccessibleForExpert(customerId, myExpertId);
        if (ok == null || ok == 0) throw new AuthException("권한이 없습니다.", HttpStatus.GONE);
      }
      default -> throw new AuthException("권한이 없습니다.", HttpStatus.GONE);
    }
    
    // 3. 기존값과 비교하여, 변동사항이 있는지 체크
    SalesUpdateDto current = mapper.findSalesInfo(customerId);
    long newAmount = (dto.getAmount() == null) ? 0L : dto.getAmount();
    long currentAmount = 0L;
    
    if ("INITIAL".equalsIgnoreCase(dto.getType())) {
      currentAmount = (current.getInitialPrice() == null) ? 0L : current.getInitialPrice();
    } else if ("UPSELL".equalsIgnoreCase(dto.getType())) {
      currentAmount = (current.getUpsellPrice() == null) ? 0L : current.getUpsellPrice();
    }
    
    // 값이 같으면 조용히 종료 (로그 X, 업데이트 X)
    if (newAmount == currentAmount) {
      return;
    }
    
    // 4. 로그 기록
    mapper.insertSalesLog(customerId, dto.getType(), dto.getAmount(), me.getUserId());
    
    // 5. Customer 테이블 값 업데이트
    if ("INITIAL".equalsIgnoreCase(dto.getType())) {
      mapper.updateCustomerInitialPrice(customerId, dto.getAmount());
    } else if ("UPSELL".equalsIgnoreCase(dto.getType())) {
      mapper.updateCustomerUpsellPrice(customerId, dto.getAmount());
    } else {
      throw new AuthException("잘못된 매출 타입입니다.", HttpStatus.GONE);
    }
  }
}
