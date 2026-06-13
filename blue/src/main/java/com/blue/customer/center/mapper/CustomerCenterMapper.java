package com.blue.customer.center.mapper;

import com.blue.customer.all.dto.UserContextDto;
import com.blue.customer.center.dto.BranchSimpleDto;
import com.blue.customer.center.dto.CenterDbRowDto;
import com.blue.customer.center.dto.CenterSimpleDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CustomerCenterMapper {
  
  UserContextDto findUserContextByEmail(@Param("email") String email);
  
  List<BranchSimpleDto> findBranchesExcludingHQ();
  
  List<CenterSimpleDto> findCentersByBranchId(@Param("branchId") Long branchId);
  
  Long findExpertIdByUserId(@Param("userId") Long userId);
  
  List<CenterDbRowDto> findForAdmin(@Param("offset") int offset,
                                    @Param("size") int size,
                                    @Param("keyword") String keyword,
                                    @Param("dateFrom") String dateFrom,
                                    @Param("dateTo") String dateTo,
                                    @Param("category") String category,
                                    @Param("division") String division,
                                    @Param("sort") String sort,
                                    @Param("expertName") String expertName,
                                    @Param("status") String status,
                                    @Param("branchId") Long branchId,
                                    @Param("centerId") Long centerId,
                                    @Param("visible") String visible);
  
  int countForAdmin(@Param("keyword") String keyword,
                    @Param("dateFrom") String dateFrom,
                    @Param("dateTo") String dateTo,
                    @Param("category") String category,
                    @Param("division") String division,
                    @Param("expertName") String expertName,
                    @Param("status") String status,
                    @Param("branchId") Long branchId,
                    @Param("centerId") Long centerId,
                    @Param("visible") String visible);
  
  List<CenterDbRowDto> findForCenterHead(@Param("offset") int offset,
                                         @Param("size") int size,
                                         @Param("keyword") String keyword,
                                         @Param("dateFrom") String dateFrom,
                                         @Param("dateTo") String dateTo,
                                         @Param("category") String category,
                                         @Param("division") String division,
                                         @Param("sort") String sort,
                                         @Param("expertName") String expertName,
                                         @Param("status") String status,
                                         @Param("branchId") Long branchId,
                                         @Param("centerId") Long centerId,
                                         @Param("visible") String visible,
                                         @Param("myCenterId") Long myCenterId);
  
  int countForCenterHead(@Param("keyword") String keyword,
                         @Param("dateFrom") String dateFrom,
                         @Param("dateTo") String dateTo,
                         @Param("category") String category,
                         @Param("division") String division,
                         @Param("expertName") String expertName,
                         @Param("status") String status,
                         @Param("branchId") Long branchId,
                         @Param("centerId") Long centerId,
                         @Param("visible") String visible,
                         @Param("myCenterId") Long myCenterId);
  
  List<CenterDbRowDto> findForExpert(@Param("offset") int offset,
                                     @Param("size") int size,
                                     @Param("keyword") String keyword,
                                     @Param("dateFrom") String dateFrom,
                                     @Param("dateTo") String dateTo,
                                     @Param("category") String category,
                                     @Param("division") String division,
                                     @Param("sort") String sort,
                                     @Param("status") String status,
                                     @Param("branchId") Long branchId,
                                     @Param("centerId") Long centerId,
                                     @Param("visible") String visible,
                                     @Param("myExpertId") Long myExpertId);
  
  int countForExpert(@Param("keyword") String keyword,
                     @Param("dateFrom") String dateFrom,
                     @Param("dateTo") String dateTo,
                     @Param("category") String category,
                     @Param("division") String division,
                     @Param("status") String status,
                     @Param("branchId") Long branchId,
                     @Param("centerId") Long centerId,
                     @Param("visible") String visible,
                     @Param("myExpertId") Long myExpertId);

  /* ===== 엑셀 export: 메모=전체 채팅 합치기 ===== */

  /** GROUP_CONCAT 길이 제한 상향(기본 1024 → 길면 잘림). exportExcel @Transactional 안에서 호출(같은 커넥션). */
  void setGroupConcatMaxLen();

  /** 주어진 고객들의 전체 채팅(STATUS 포함, is_deleted=0)을 고객별로 한 문자열로 합쳐 반환. */
  List<com.blue.customer.center.dto.CenterMemoExportDto> selectMemoExport(@Param("ids") List<Long> ids);
}
