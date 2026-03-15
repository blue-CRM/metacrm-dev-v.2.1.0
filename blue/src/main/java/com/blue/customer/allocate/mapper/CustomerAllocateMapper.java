package com.blue.customer.allocate.mapper;

import com.blue.customer.allocate.dto.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CustomerAllocateMapper {
  
  // 로그인 사용자 컨텍스트
  UserContextDto findUserContextByEmail(@Param("email") String email);
  
  // 목록 (HQ)
  List<AllocateListRowDto> findListForHq(@Param("offset") int offset,
                                         @Param("size") int size,
                                         @Param("keyword") String keyword,
                                         @Param("dateFrom") String dateFrom,
                                         @Param("dateTo") String dateTo,
                                         @Param("category") String category,
                                         @Param("division") String division,
                                         @Param("prevStatus") String prevStatus,
                                         @Param("sort") String sort,
                                         @Param("visible") String visible);
  int countListForHq(@Param("keyword") String keyword,
                     @Param("dateFrom") String dateFrom,
                     @Param("dateTo") String dateTo,
                     @Param("category") String category,
                     @Param("division") String division,
                     @Param("prevStatus") String prevStatus,
                     @Param("visible") String visible);
  
  // 목록 (MANAGER)
  List<AllocateListRowDto> findListForManager(@Param("offset") int offset,
                                              @Param("size") int size,
                                              @Param("keyword") String keyword,
                                              @Param("dateFrom") String dateFrom,
                                              @Param("dateTo") String dateTo,
                                              @Param("category") String category,
                                              @Param("prevStatus") String prevStatus,
                                              @Param("sort") String sort,
                                              @Param("managerUserId") Long managerUserId,
                                              @Param("visible") String visible);
  int countListForManager(@Param("keyword") String keyword,
                          @Param("dateFrom") String dateFrom,
                          @Param("dateTo") String dateTo,
                          @Param("category") String category,
                          @Param("prevStatus") String prevStatus,
                          @Param("managerUserId") Long managerUserId,
                          @Param("visible") String visible);
  
  // 목록 (CENTERHEAD)
  List<AllocateListRowDto> findListForCenterHead(@Param("offset") int offset,
                                                 @Param("size") int size,
                                                 @Param("keyword") String keyword,
                                                 @Param("dateFrom") String dateFrom,
                                                 @Param("dateTo") String dateTo,
                                                 @Param("category") String category,
                                                 @Param("division") String division,
                                                 @Param("prevStatus") String prevStatus,
                                                 @Param("sort") String sort,
                                                 @Param("userId") Long userId,
                                                 @Param("visible") String visible,
                                                 @Param("centerId") Long centerId);
  int countListForCenterHead(@Param("keyword") String keyword,
                             @Param("dateFrom") String dateFrom,
                             @Param("dateTo") String dateTo,
                             @Param("category") String category,
                             @Param("division") String division,
                             @Param("prevStatus") String prevStatus,
                             @Param("userId") Long userId,
                             @Param("visible") String visible,
                             @Param("centerId") Long centerId);
  
  // 목록 (EXPERT)
  List<AllocateListRowDto> findListForExpert(@Param("offset") int offset,
                                             @Param("size") int size,
                                             @Param("keyword") String keyword,
                                             @Param("dateFrom") String dateFrom,
                                             @Param("dateTo") String dateTo,
                                             @Param("category") String category,
                                             @Param("division") String division,
                                             @Param("prevStatus") String prevStatus,
                                             @Param("sort") String sort,
                                             @Param("userId") Long userId,
                                             @Param("visible") String visible,
                                             @Param("expertId") Long expertId);
  int countListForExpert(@Param("keyword") String keyword,
                         @Param("dateFrom") String dateFrom,
                         @Param("dateTo") String dateTo,
                         @Param("category") String category,
                         @Param("division") String division,
                         @Param("prevStatus") String prevStatus,
                         @Param("userId") Long userId,
                         @Param("visible") String visible,
                         @Param("expertId") Long expertId);
  
  // 검증/조회
  Integer userBelongsToCenter(@Param("userId") Long userId, @Param("centerId") Long centerId);
  String  findUserRole(@Param("userId") Long userId);
  
  // 유저가 센터에 소속되어있는지 검사
  Integer staffInSameCenter(@Param("managerUserId") Long managerUserId,
                            @Param("staffUserId") Long staffUserId);
  
  // 잠금(선정)
  List<Long> lockCustomersForHq(@Param("ids") List<Long> ids);
  List<Long> lockCustomersForManager(@Param("ids") List<Long> ids,
                                     @Param("managerUserId") Long managerUserId);
  List<Long> lockCustomersForCenterHead(@Param("ids") List<Long> ids,
                                        @Param("userId") Long userId,
                                        @Param("centerId") Long centerId);
  List<Long> lockCustomersForExpert(@Param("ids") List<Long> ids,
                                    @Param("userId") Long userId,
                                    @Param("expertId") Long expertId);
  
  // 이력 (phone 기반)
  int insertPastForNewOwner(@Param("ids") List<Long> ids, @Param("userId") Long userId);
  int deleteManagerFromPast(@Param("ids") List<Long> ids, @Param("managerUserId") Long managerUserId);
  
  // 소유자/상태
  int updateOwner(@Param("ids") List<Long> ids, @Param("userId") Long userId);
  void updateStatusToNew(@Param("ids") List<Long> ids);
  void updateStatusToNone(@Param("ids") List<Long> ids);
  
  // 직원 검색 (센터 제한 + 키워드)
  List<UserPickDto> searchStaffForAllocate(
      @Param("centerId") Long centerId,
      @Param("q") String q
  );
  
  // 센터 조회
  List<CenterPickDto> findCentersForAllocate();
  
  // 담당자 이력 일괄 초기화
  void deleteHistoryByCustomerIds(@Param("ids") List<Long> ids);
  
  // 모달에서 - 담당자 이력 리스트 조회
  List<CustomerHistoryRowDto> findHistoryByCustomerId(@Param("customerId") Long customerId);
  
  // 모달에서 - 담당자 이력 선택 삭제 (현재 담당자는 실제로도 안 지워지게 가드)
  int deleteHistorySafely(@Param("ids") List<Long> ids);
  
  // 모달에서 : 슈퍼계정 전용 전문가 DB 집계 표 출력
  List<AllocateExpertDailySummaryRowDto> findExpertDailySummary(@Param("ids") List<Long> ids);
  
}
