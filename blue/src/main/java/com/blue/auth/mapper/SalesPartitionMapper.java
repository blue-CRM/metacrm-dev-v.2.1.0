package com.blue.auth.mapper;

import com.blue.auth.dto.SalesPartitionDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SalesPartitionMapper {
  
  List<SalesPartitionDto> findPartitions(@Param("partitionType") String partitionType,
                                         @Param("keyword") String keyword);
  
  boolean existsPartitionTypeAndSegment(@Param("partitionType") String partitionType,
                                        @Param("segment") Long segment,
                                        @Param("partitionId") Long partitionId);
  
  int insertPartition(SalesPartitionDto dto);
  
  int updatePartition(SalesPartitionDto dto);
  
  int deletePartition(@Param("partitionId") Long partitionId);
}