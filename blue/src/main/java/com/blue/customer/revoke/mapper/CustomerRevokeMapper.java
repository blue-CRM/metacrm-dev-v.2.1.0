package com.blue.customer.revoke.mapper;

import com.blue.customer.center.dto.CenterSimpleDto;
import com.blue.customer.revoke.dto.RevokeListRowDto;
import com.blue.customer.revoke.dto.UserContextDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CustomerRevokeMapper {
  
  UserContextDto findUserContextByEmail(@Param("email") String email);
  
  List<RevokeListRowDto> findListForHq(@Param("offset") int offset,
                                       @Param("size") int size,
                                       @Param("keyword") String keyword,
                                       @Param("dateFrom") String dateFrom,
                                       @Param("dateTo") String dateTo,
                                       @Param("category") String category,
                                       @Param("division") String division,
                                       @Param("status")   String status,
                                       @Param("prevStatus") String prevStatus,
                                       @Param("sort") String sort,
                                       @Param("visible") String visible,
                                       @Param("filterCenterId") Long filterCenterId);
  int countListForHq(@Param("keyword") String keyword,
                     @Param("dateFrom") String dateFrom,
                     @Param("dateTo") String dateTo,
                     @Param("category") String category,
                     @Param("division") String division,
                     @Param("status")   String status,
                     @Param("prevStatus") String prevStatus,
                     @Param("visible") String visible,
                     @Param("filterCenterId") Long filterCenterId);
  
  List<RevokeListRowDto> findListForManager(@Param("offset") int offset,
                                            @Param("size") int size,
                                            @Param("keyword") String keyword,
                                            @Param("dateFrom") String dateFrom,
                                            @Param("dateTo") String dateTo,
                                            @Param("category") String category,
                                            @Param("status")   String status,
                                            @Param("prevStatus") String prevStatus,
                                            @Param("sort") String sort,
                                            @Param("managerCenterId") Long managerCenterId,
                                            @Param("visible") String visible,
                                            @Param("excludeUserId") Long excludeUserId);
  int countListForManager(@Param("keyword") String keyword,
                          @Param("dateFrom") String dateFrom,
                          @Param("dateTo") String dateTo,
                          @Param("category") String category,
                          @Param("status")   String status,
                          @Param("prevStatus") String prevStatus,
                          @Param("managerCenterId") Long managerCenterId,
                          @Param("visible") String visible,
                          @Param("excludeUserId") Long excludeUserId);
  
  List<RevokeListRowDto> findListForCenterHead(@Param("offset") int offset,
                                               @Param("size") int size,
                                               @Param("keyword") String keyword,
                                               @Param("dateFrom") String dateFrom,
                                               @Param("dateTo") String dateTo,
                                               @Param("category") String category,
                                               @Param("status") String status,
                                               @Param("prevStatus") String prevStatus,
                                               @Param("sort") String sort,
                                               @Param("centerId") Long centerId,
                                               @Param("visible") String visible,
                                               @Param("excludeUserId") Long excludeUserId,
                                               @Param("filterCenterId") Long filterCenterId);
  
  int countListForCenterHead(@Param("keyword") String keyword,
                             @Param("dateFrom") String dateFrom,
                             @Param("dateTo") String dateTo,
                             @Param("category") String category,
                             @Param("status") String status,
                             @Param("prevStatus") String prevStatus,
                             @Param("centerId") Long centerId,
                             @Param("visible") String visible,
                             @Param("excludeUserId") Long excludeUserId,
                             @Param("filterCenterId") Long filterCenterId);
  
  List<RevokeListRowDto> findListForExpert(@Param("offset") int offset,
                                           @Param("size") int size,
                                           @Param("keyword") String keyword,
                                           @Param("dateFrom") String dateFrom,
                                           @Param("dateTo") String dateTo,
                                           @Param("category") String category,
                                           @Param("status") String status,
                                           @Param("prevStatus") String prevStatus,
                                           @Param("sort") String sort,
                                           @Param("expertId") Long expertId,
                                           @Param("visible") String visible,
                                           @Param("filterCenterId") Long filterCenterId);
  
  int countListForExpert(@Param("keyword") String keyword,
                         @Param("dateFrom") String dateFrom,
                         @Param("dateTo") String dateTo,
                         @Param("category") String category,
                         @Param("status") String status,
                         @Param("prevStatus") String prevStatus,
                         @Param("expertId") Long expertId,
                         @Param("visible") String visible,
                         @Param("filterCenterId") Long filterCenterId);
  
  List<Long> lockCustomersForRevokeByHq(@Param("ids") List<Long> ids);
  List<Long> lockCustomersForRevokeByManager(@Param("ids") List<Long> ids,
                                             @Param("managerCenterId") Long managerCenterId,
                                             @Param("excludeUserId") Long excludeUserId);
  List<Long> lockCustomersForRevokeByCenterHead(@Param("ids") List<Long> ids,
                                                @Param("centerId") Long centerId,
                                                @Param("excludeUserId") Long excludeUserId);
  List<Long> lockCustomersForRevokeByExpert(@Param("ids") List<Long> ids,
                                            @Param("expertId") Long expertId);
  
  int updateToRevoked(@Param("ids") List<Long> ids);
  int updateToManagerPool(@Param("ids") List<Long> ids,
                          @Param("managerId") Long managerId);
  
  // 내방 이벤트 삭제
  void deleteSchedulesByCustomerIds(@Param("ids") List<Long> ids);
  // 회수 후 매출 로그 작성
  void insertRevokeSalesLogs(@Param("ids") List<Long> ids, @Param("userId") Long userId);
  
  // 팀 드롭박스
  List<CenterSimpleDto> findRevokeTeamsForHq();
  List<CenterSimpleDto> findRevokeTeamsForCenterHead(@Param("centerId") Long centerId);
  List<CenterSimpleDto> findRevokeTeamsForExpert(@Param("expertId") Long expertId);
}
