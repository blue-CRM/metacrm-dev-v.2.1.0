package com.blue.sales.mapper;

import com.blue.customer.all.dto.UserContextDto;
import com.blue.sales.dto.SalesAggRowDto;
import com.blue.sales.dto.SalesPartitionRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SalesMapper {
  
  // 로그인만 체크용
  UserContextDto findUserContextByEmail(@Param("email") String email);
  
  // PartitionBar 구간 정의
  List<SalesPartitionRow> findSalesPartitions(@Param("partitionType") String partitionType);
  
  // 대시보드 rows (month/range/all에서 공통 호출)
  List<SalesAggRowDto> findAggRows(@Param("viewMode") String viewMode,
                                   @Param("includeResigned") boolean includeResigned,
                                   @Param("startTs") String startTs,   // yyyy-MM-dd HH:mm:ss
                                   @Param("endTs") String endTs);      // yyyy-MM-dd HH:mm:ss (exclusive)
  
  List<String> findAdminEmails();
  List<Long> findPartitionStops(@Param("partitionType") String partitionType);
  List<String> findPartitionColors(@Param("partitionType") String partitionType);
}