package com.blue.auth.service;

import com.blue.auth.dto.GrantsDto;
import com.blue.auth.dto.MyInfoResponse;
import com.blue.auth.dto.SalesPartitionDto;
import com.blue.auth.mapper.MyInfoMapper;
import com.blue.auth.mapper.SalesPartitionMapper;
import com.blue.global.exception.AuthException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SalesPartitionService {
  
  private static final Set<String> ALLOWED_TYPES = Set.of("BRANCH", "TEAM", "PERSON");
  
  private final SalesPartitionMapper salesPartitionMapper;
  private final MyInfoMapper myInfoMapper;
  
  private MyInfoResponse ensureSuper(String email) {
    MyInfoResponse user = myInfoMapper.findByEmail(email);
    if (user == null) {
      throw new AuthException("사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
    }
    
    GrantsDto grants = user.getGrants();
    if (grants == null || !grants.isSuper()) {
      throw new AuthException("권한이 없습니다.", HttpStatus.FORBIDDEN);
    }
    return user;
  }
  
  private String normalizeType(String partitionType) {
    String v = partitionType == null ? "" : partitionType.trim().toUpperCase();
    if (!ALLOWED_TYPES.contains(v)) {
      throw new AuthException("partitionType은 BRANCH, TEAM, PERSON 중 하나여야 합니다.", HttpStatus.BAD_REQUEST);
    }
    return v;
  }
  
  private Long normalizeSegment(Long segment) {
    if (segment == null || segment < 1) {
      throw new AuthException("구간값(segment)은 1 이상의 정수여야 합니다.", HttpStatus.BAD_REQUEST);
    }
    return segment;
  }
  
  private String normalizeName(String name) {
    String v = name == null ? "" : name.trim();
    if (v.isEmpty()) {
      throw new AuthException("구간명을 입력하세요.", HttpStatus.BAD_REQUEST);
    }
    if (v.length() > 50) {
      throw new AuthException("구간명은 50자 이하여야 합니다.", HttpStatus.BAD_REQUEST);
    }
    return v;
  }
  
  private String normalizeHex6(String color) {
    String v = color == null ? "" : color.trim().toUpperCase();
    if (!v.matches("^#[0-9A-F]{6}$")) {
      throw new AuthException("색상은 #RRGGBB 형식이어야 합니다.", HttpStatus.BAD_REQUEST);
    }
    return v;
  }
  
  @Transactional(readOnly = true)
  public List<SalesPartitionDto> getPartitions(String callerEmail, String partitionType, String keyword) {
    ensureSuper(callerEmail);
    
    String type = (partitionType == null || partitionType.isBlank())
        ? null
        : normalizeType(partitionType);
    
    String k = (keyword == null || keyword.isBlank())
        ? null
        : keyword.trim();
    
    return salesPartitionMapper.findPartitions(type, k);
  }
  
  @Transactional
  public void createPartition(String callerEmail, SalesPartitionDto body) {
    MyInfoResponse me = ensureSuper(callerEmail);
    
    String type = normalizeType(body.getPartitionType());
    Long segment = normalizeSegment(body.getSegment());
    String name = normalizeName(body.getName());
    String color = normalizeHex6(body.getColor());
    
    if (salesPartitionMapper.existsPartitionTypeAndSegment(type, segment, null)) {
      throw new AuthException("같은 구분에 동일한 구간값이 이미 존재합니다.", HttpStatus.CONFLICT);
    }
    
    SalesPartitionDto dto = new SalesPartitionDto();
    dto.setPartitionType(type);
    dto.setSegment(segment);
    dto.setName(name);
    dto.setColor(color);
    dto.setUpdatedBy(me.getUserId());
    
    salesPartitionMapper.insertPartition(dto);
  }
  
  @Transactional
  public void updatePartition(String callerEmail, Long partitionId, SalesPartitionDto body) {
    MyInfoResponse me = ensureSuper(callerEmail);
    
    if (partitionId == null || partitionId < 1) {
      throw new AuthException("잘못된 partitionId 입니다.", HttpStatus.BAD_REQUEST);
    }
    
    String type = normalizeType(body.getPartitionType());
    Long segment = normalizeSegment(body.getSegment());
    String name = normalizeName(body.getName());
    String color = normalizeHex6(body.getColor());
    
    if (salesPartitionMapper.existsPartitionTypeAndSegment(type, segment, partitionId)) {
      throw new AuthException("같은 구분에 동일한 구간값이 이미 존재합니다.", HttpStatus.CONFLICT);
    }
    
    SalesPartitionDto dto = new SalesPartitionDto();
    dto.setPartitionId(partitionId);
    dto.setPartitionType(type);
    dto.setSegment(segment);
    dto.setName(name);
    dto.setColor(color);
    dto.setUpdatedBy(me.getUserId());
    
    int updated = salesPartitionMapper.updatePartition(dto);
    if (updated == 0) {
      throw new AuthException("존재하지 않는 파티션입니다.", HttpStatus.NOT_FOUND);
    }
  }
  
  @Transactional
  public void deletePartition(String callerEmail, Long partitionId) {
    ensureSuper(callerEmail);
    
    if (partitionId == null || partitionId < 1) {
      throw new AuthException("잘못된 partitionId 입니다.", HttpStatus.BAD_REQUEST);
    }
    
    int deleted = salesPartitionMapper.deletePartition(partitionId);
    if (deleted == 0) {
      throw new AuthException("존재하지 않는 파티션입니다.", HttpStatus.NOT_FOUND);
    }
  }
}