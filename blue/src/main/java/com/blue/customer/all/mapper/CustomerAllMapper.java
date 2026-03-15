package com.blue.customer.all.mapper;

import com.blue.customer.all.dto.AllDbRowDto;
import com.blue.customer.all.dto.ExpertDto;
import com.blue.customer.all.dto.SalesUpdateDto;
import com.blue.customer.all.dto.UserContextDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CustomerAllMapper {
  
  // 로그인 사용자 컨텍스트
  UserContextDto findUserContextByEmail(@Param("email") String email);
  // 전문가 id로 출처 id 조회
  Long findExpertIdByUserId(@Param("userId") Long userId);
  
  // SUPERADMIN — customers + customers_duplicate(display=1)
  List<AllDbRowDto> findAllForAdmin(@Param("offset") int offset,
                                    @Param("size") int size,
                                    @Param("keyword") String keyword,
                                    @Param("dateFrom") String dateFrom,
                                    @Param("dateTo") String dateTo,
                                    @Param("category") String category,
                                    @Param("division") String division,
                                    @Param("sort") String sort,
                                    @Param("expertName") String expertName,
                                    @Param("status") String status,
                                    @Param("prevStatus") String prevStatus,
                                    @Param("visible") String visible);
  int countAllForAdmin(@Param("keyword") String keyword,
                       @Param("dateFrom") String dateFrom,
                       @Param("dateTo") String dateTo,
                       @Param("category") String category,
                       @Param("division") String division,
                       @Param("expertName") String expertName,
                       @Param("status") String status,
                       @Param("prevStatus") String prevStatus,
                       @Param("visible") String visible);
  
  // MANAGER — 자기 센터 소속 직원 담당 고객
  List<AllDbRowDto> findAllForManager(@Param("offset") int offset,
                                      @Param("size") int size,
                                      @Param("keyword") String keyword,
                                      @Param("dateFrom") String dateFrom,
                                      @Param("dateTo") String dateTo,
                                      @Param("category") String category,
                                      @Param("division") String division,
                                      @Param("sort") String sort,
                                      @Param("expertName") String expertName,
                                      @Param("pStatus") String status,
                                      @Param("prevStatus") String prevStatus,
                                      @Param("centerId") Long centerId);
  int countAllForManager(@Param("keyword") String keyword,
                         @Param("dateFrom") String dateFrom,
                         @Param("dateTo") String dateTo,
                         @Param("category") String category,
                         @Param("division") String division,
                         @Param("expertName") String expertName,
                         @Param("pStatus") String status,
                         @Param("prevStatus") String prevStatus,
                         @Param("centerId") Long centerId);
  
  // STAFF — 본인 담당 고객
  List<AllDbRowDto> findAllForStaff(@Param("offset") int offset,
                                    @Param("size") int size,
                                    @Param("keyword") String keyword,
                                    @Param("dateFrom") String dateFrom,
                                    @Param("dateTo") String dateTo,
                                    @Param("category") String category,
                                    @Param("division") String division,
                                    @Param("sort") String sort,
                                    @Param("expertName") String expertName,
                                    @Param("pStatus") String status,
                                    @Param("prevStatus") String prevStatus,
                                    @Param("userId") Long userId);
  int countAllForStaff(@Param("keyword") String keyword,
                       @Param("dateFrom") String dateFrom,
                       @Param("dateTo") String dateTo,
                       @Param("category") String category,
                       @Param("division") String division,
                       @Param("expertName") String expertName,
                       @Param("pStatus") String status,
                       @Param("prevStatus") String prevStatus,
                       @Param("userId") Long userId);
  
  // CENTERHEAD - 자기 센터 소속 전문가 담당 출처
  List<AllDbRowDto> findAllForCenterHead(
      @Param("offset") int offset,
      @Param("size") int size,
      @Param("keyword") String keyword,
      @Param("dateFrom") String dateFrom,
      @Param("dateTo") String dateTo,
      @Param("category") String category,
      @Param("division") String division,
      @Param("sort") String sort,
      @Param("expertName") String expertName,
      @Param("status") String status,
      @Param("prevStatus") String prevStatus,
      @Param("visible") String visible,
      @Param("centerId") Long centerId
  );
  int countAllForCenterHead(
      @Param("keyword") String keyword,
      @Param("dateFrom") String dateFrom,
      @Param("dateTo") String dateTo,
      @Param("category") String category,
      @Param("division") String division,
      @Param("expertName") String expertName,
      @Param("status") String status,
      @Param("prevStatus") String prevStatus,
      @Param("visible") String visible,
      @Param("centerId") Long centerId
  );
  
  // EXPERT - 본인 담당 출처
  List<AllDbRowDto> findAllForExpert(
      @Param("offset") int offset,
      @Param("size") int size,
      @Param("keyword") String keyword,
      @Param("dateFrom") String dateFrom,
      @Param("dateTo") String dateTo,
      @Param("category") String category,
      @Param("division") String division,
      @Param("sort") String sort,
      @Param("expertName") String expertName,
      @Param("status") String status,
      @Param("prevStatus") String prevStatus,
      @Param("visible") String visible,
      @Param("myExpertId") Long myExpertId
  );
  int countAllForExpert(
      @Param("keyword") String keyword,
      @Param("dateFrom") String dateFrom,
      @Param("dateTo") String dateTo,
      @Param("category") String category,
      @Param("division") String division,
      @Param("expertName") String expertName,
      @Param("status") String status,
      @Param("prevStatus") String prevStatus,
      @Param("visible") String visible,
      @Param("myExpertId") Long myExpertId
  );
  
  // 검사/권한
  Integer existsCustomerById(@Param("customerId") Long customerId);
  Integer customerOwnedByCenter(@Param("customerId") Long customerId, @Param("centerId") Long centerId);
  Integer customerOwnedByUser(@Param("customerId") Long customerId, @Param("userId") Long userId);
  Integer customerAccessibleForCenterHead(@Param("customerId") Long customerId,
                                          @Param("centerId") Long centerId);
  
  Integer customerAccessibleForExpert(@Param("customerId") Long customerId,
                                      @Param("expertId") Long expertId);
  
  // 수정
  int updateCustomerReservation(@Param("customerId") Long customerId, @Param("when") LocalDateTime when);
  int updateCustomerStatus(@Param("customerId") Long customerId, @Param("status") String status);
  int updateCustomerName(@Param("customerId") Long customerId, @Param("name") String name);
  
  // 현재 상태값 조회
  String selectStatusOnly(@Param("customerId") Long customerId);
  
  // 중복 숨김
  int hideDuplicates(@Param("ids") List<Long> duplicateIds);
  // 일정 존재여부 확인
  Integer existsVisitSchedule(@Param("customerId") Long customerId);
  
  // 전문가 리스트 조회
  List<ExpertDto> findAllExperts();
  List<ExpertDto> findExpertsByCenterId(@Param("centerId") Long centerId);
  
  // 매출 업데이트 (최초)
  void updateCustomerInitialPrice(@Param("customerId") Long customerId, @Param("amount") Long amount);
  // 매출 업데이트 (업셀)
  void updateCustomerUpsellPrice(@Param("customerId") Long customerId, @Param("amount") Long amount);
  // 매출 로그 기록
  void insertSalesLog(@Param("customerId") Long customerId,
                      @Param("type") String type,
                      @Param("amount") Long amount,
                      @Param("userId") Long userId);
  // 현재 매출 상태 조회
  SalesUpdateDto findSalesInfo(@Param("customerId") Long customerId);
}
