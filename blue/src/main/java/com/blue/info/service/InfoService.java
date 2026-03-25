package com.blue.info.service;


import com.blue.global.exception.AuthException;
import com.blue.info.dto.CenterDto;
import com.blue.info.dto.UserRow;
import com.blue.info.mapper.InfoMapper;
import com.blue.user.dto.UpdateUserRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InfoService {
  
  private final InfoMapper infoMapper;
  
  // 센터 목록 조회
  public List<CenterDto> findCenters() {
    return infoMapper.findCenters();
  }
  
  // 회사명 상수
  private static final String COMPANY_NAME = "메타CRM";
  
  /**
   * 조직도 트리 + 현재 사용자
   * - centerId == null 사용자는 제외
   * - 보기 범위: STAFF=본인만, MANAGER=본인 센터 전체, HQ/SUPERADMIN=전체
   * - 정렬: 권한(관리자>센터장>전문가>팀장>프로) → 입사일(오름차순)
   */
  public Map<String, Object> getOrgTree(Authentication auth) {
    UserRow me = (auth != null) ? infoMapper.findByEmail(auth.getName()) : null;
    if (me == null) {
      return Map.of("nodes", List.of(), "currentUser", null);
    }
    
    // centers(=팀) 목록(브랜치 포함) 1번만 조회
    List<CenterDto> centers = infoMapper.findCenters();
    
    // 유저 전체(승인 Y만 내려옴) + centerId null 제외
    List<UserRow> users = infoMapper.findAllUsers().stream()
        .filter(u -> u.getCenterId() != null)
        .toList();
    
    // 유저 정렬: 권한 우선(내림차순) → 입사순(오름차순)
    Comparator<UserRow> userComp = Comparator
        .comparingInt((UserRow u) -> rank(u.getUserRole())).reversed()
        .thenComparing(UserRow::getUserCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
    users = users.stream().sorted(userComp).toList();
    
    // 보기 범위 필터
    List<UserRow> visibleUsers = users.stream()
        .filter(u -> canSee(me, u))
        .toList();
    
    // centerId -> 멤버
    Map<Long, List<UserRow>> usersByCenter = visibleUsers.stream()
        .collect(Collectors.groupingBy(UserRow::getCenterId));
    
    // 각 센터 멤버도 정렬 유지
    usersByCenter.replaceAll((k, list) -> list.stream().sorted(userComp).toList());
    
    // 보이는 센터(=팀) 목록 결정 (새 규칙)
    List<CenterDto> visibleCenters = filterCenters(me, centers);
    
    // 센터 정렬: 본사브랜치(1) 먼저 -> 지점명 -> 본사팀(centerId=1) 먼저 -> 팀명
    Comparator<CenterDto> centerComp = Comparator
        .comparing((CenterDto c) -> !Objects.equals(c.getBranchId(), 1L)) // false(본사) 먼저
        .thenComparing(CenterDto::getBranchName, Comparator.nullsLast(Comparator.naturalOrder()))
        .thenComparing((CenterDto c) -> !Objects.equals(c.getCenterId(), 1L)) // false(본사팀) 먼저
        .thenComparing(CenterDto::getCenterName, Comparator.nullsLast(Comparator.naturalOrder()));
    
    visibleCenters = visibleCenters.stream().sorted(centerComp).toList();
    
    // branchId -> centers
    Map<Long, List<CenterDto>> centersByBranch = visibleCenters.stream()
        .collect(Collectors.groupingBy(CenterDto::getBranchId, LinkedHashMap::new, Collectors.toList()));
    
    // 루트(HQ)
    Map<String, Object> root = new LinkedHashMap<>();
    root.put("userId", null);
    root.put("userName", COMPANY_NAME);
    root.put("userRole", "HQ");
    
    List<Map<String, Object>> branchNodes = new ArrayList<>();
    
    for (Map.Entry<Long, List<CenterDto>> e : centersByBranch.entrySet()) {
      List<CenterDto> branchCenters = e.getValue();
      if (branchCenters.isEmpty()) continue;
      
      String branchName = branchCenters.get(0).getBranchName();
      
      List<Map<String, Object>> centerNodes = new ArrayList<>();
      for (CenterDto c : branchCenters) {
        List<UserRow> members = usersByCenter.getOrDefault(c.getCenterId(), List.of());
        
        Map<String, Object> centerNode = new LinkedHashMap<>();
        centerNode.put("userId", null);
        centerNode.put("userName", c.getCenterName());
        centerNode.put("userRole", "CENTER");
        centerNode.put("children", toUserNodeList(members));
        
        centerNodes.add(centerNode);
      }
      
      // 브랜치 노드(프론트 호환 위해 userRole=GROUP 유지)
      Map<String, Object> branchNode = new LinkedHashMap<>();
      branchNode.put("userId", null);
      branchNode.put("userName", branchName);
      branchNode.put("userRole", "GROUP");
      branchNode.put("children", centerNodes);
      
      branchNodes.add(branchNode);
    }
    
    root.put("children", branchNodes);
    
    Map<String, Object> currentUser = new LinkedHashMap<>();
    currentUser.put("userId", me.getUserId());
    currentUser.put("userEmail", me.getUserEmail());
    currentUser.put("userRole", me.getUserRole());
    currentUser.put("centerId", me.getCenterId());
    currentUser.put("isSuper", isSuper(me));
    
    return Map.of("nodes", List.of(root), "currentUser", currentUser);
  }
  
  // ===================== 보기 범위(새 규칙) =====================
  private boolean canSeeAllCenters(UserRow u) {
    return isSuper(u) || isHqTeam(u); // 슈퍼 또는 본사팀(centerId=1)
  }
  
  // 본사팀/슈퍼만 전체 열람, 그 외는 역할별로 허용된 범위(센터장·전문가: 내 센터 + 타지점 팀 / 매니저·스탭: 내 팀)만 열람
  private boolean canSee(UserRow viewer, UserRow target) {
    if (viewer == null || target == null) return false;
    if (canSeeAllCenters(viewer)) return true;
    
    // 본사팀은 본사팀/슈퍼만
    if (isHqTeam(target)) return false;
    
    // 센터장/전문가: 내 센터 + 다른 지점 팀들(= branchId != 1)
    if (isCenterHead(viewer) || isExpert(viewer)) {
      boolean sameCenter = Objects.equals(viewer.getCenterId(), target.getCenterId());
      boolean otherTeamBranches = isTeamBranch(target); // branchId != 1
      return sameCenter || otherTeamBranches;
    }
    
    // 매니저/스탭: 자기 팀만
    if (isManager(viewer) || isStaff(viewer)) {
      return Objects.equals(viewer.getCenterId(), target.getCenterId());
    }
    
    return false;
  }
  
  // canSee 규칙을 그대로 센터(팀) 목록에도 적용해서, 트리에 "보일 수 있는 팀"만 포함시킴
  private List<CenterDto> filterCenters(UserRow me, List<CenterDto> centers) {
    if (me == null) return List.of();
    if (canSeeAllCenters(me)) return centers;
    
    // 센터장/전문가: 내 센터 + 다른 지점 팀들(branchId != 1), 단 본사팀(centerId=1)은 제외
    if (isCenterHead(me) || isExpert(me)) {
      return centers.stream()
          .filter(c -> !Objects.equals(c.getCenterId(), 1L)) // 본사팀 제외
          .filter(c -> Objects.equals(c.getCenterId(), me.getCenterId()) || isTeamBranch(c))
          .toList();
    }
    
    // 매니저/스탭: 내 팀만
    return centers.stream()
        .filter(c -> Objects.equals(c.getCenterId(), me.getCenterId()))
        .toList();
  }
  
//  // 센터장/전문가: 본사팀 제외 전 지점 조회 가능
//  private boolean canSee(UserRow viewer, UserRow target) {
//    if (viewer == null || target == null) return false;
//    if (canSeeAllCenters(viewer)) return true;
//
//    // 센터장/전문가: 본사팀(centerId=1)만 제외
//    if (isCenterHead(viewer) || isExpert(viewer)) {
//      return !isHqTeam(target);
//    }
//
//    // 매니저/스탭: 자기 팀만(팀원 전체)
//    if (isManager(viewer) || isStaff(viewer)) {
//      return Objects.equals(viewer.getCenterId(), target.getCenterId());
//    }
//
//    return false;
//  }
//
//  // 트리에 포함할 센터(팀) 목록 결정
//  private List<CenterDto> filterCenters(UserRow me, List<CenterDto> centers) {
//    if (me == null) return List.of();
//
//    if (canSeeAllCenters(me)) {
//      return centers;
//    }
//
//    if (isCenterHead(me) || isExpert(me)) {
//      // 본사팀(centerId=1)만 제외
//      return centers.stream()
//          .filter(c -> !Objects.equals(c.getCenterId(), 1L))
//          .toList();
//    }
//
//    // 매니저/스탭: 내 팀만
//    return centers.stream()
//        .filter(c -> Objects.equals(c.getCenterId(), me.getCenterId()))
//        .toList();
//  }
  
  // ===================== 수정 권한 =====================
  // super: 모두 가능(본인 포함)
  // super 아님: 본인 불가 + 동급 불가 + 하위만
  private boolean canEdit(UserRow viewer, UserRow target) {
    if (viewer == null || target == null) return false;
    
    if (isSuper(viewer)) return true;
    if (Objects.equals(viewer.getUserId(), target.getUserId())) return false;
    if (!canSee(viewer, target)) return false;
    
    return rank(viewer.getUserRole()) > rank(target.getUserRole());
  }
  
  private List<Map<String, Object>> toUserNodeList(List<UserRow> list) {
    return list.stream().map(u -> {
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("userId", u.getUserId());
      m.put("userName", u.getUserName());
      m.put("userEmail", u.getUserEmail());
      m.put("userRole", u.getUserRole());
      m.put("userPhone", u.getUserPhone());
      m.put("userCreatedAt", u.getUserCreatedAt());
      m.put("centerId", u.getCenterId());
      m.put("children", List.of());
      return m;
    }).toList();
  }
  
  /** 이름/전화 수정 전용 (조직도에서) */
  @Transactional
  public void updateUserFromOrg(Long targetUserId, UpdateUserRequest req, Authentication auth) {
    UserRow viewer = infoMapper.findByEmail(auth.getName());
    UserRow target = infoMapper.findById(targetUserId);
    
    if (viewer == null || target == null) {
      throw new AuthException("사용자 없음", HttpStatus.GONE);
    }
    if (!canEdit(viewer, target)) {
      throw new AuthException("권한이 없습니다.", HttpStatus.GONE);
    }
    
    String field = Optional.ofNullable(req.getField()).orElse("").trim();
    String value = Optional.ofNullable(req.getValue()).orElse("").trim();
    
    switch (field) {
      case "name" -> {
        if (value.isBlank()) throw new AuthException("이름은 비어 있을 수 없습니다.", HttpStatus.GONE);
        infoMapper.updateUserName(targetUserId, value); // ※ XML에서 updated_at 사용하지 마세요
      }
      case "phone" -> {
        String phone = normalizePhone(value);
        infoMapper.updateUserPhone(targetUserId, phone);
      }
      default -> throw new AuthException("지원하지 않는 필드: " + field, HttpStatus.GONE);
    }
  }
  
  /** 프론트 옵션: 센터명 목록 반환 */
  public List<String> centerNames() {
    return infoMapper.findCenters().stream()
        .map(CenterDto::getCenterName)
        .toList();
  }
  
  /* ======================= 내부 유틸 ======================= */
  // 편의 메서드
  private boolean isAdmin(UserRow u)   { return u != null && "SUPERADMIN".equals(u.getUserRole()); }
  private boolean isManager(UserRow u) { return u != null && "MANAGER".equals(u.getUserRole()); }
  private boolean isStaff(UserRow u)   { return u != null && "STAFF".equals(u.getUserRole()); }
  private boolean isCenterHead(UserRow u) { return u != null && "CENTERHEAD".equals(u.getUserRole()); }
  private boolean isExpert(UserRow u)   { return u != null && "EXPERT".equals(u.getUserRole()); }
  private boolean isSuper(UserRow u)   { return u != null && u.isSuper(); }
  private boolean isHqTeam(UserRow u) { return u != null && Objects.equals(u.getCenterId(), 1L); }
  private boolean isHqBranch(UserRow u){ return u != null && Objects.equals(u.getBranchId(), 1L); }
  private boolean isTeamBranch(UserRow u){ return u != null && u.getBranchId() != null && !Objects.equals(u.getBranchId(), 1L); }
  private boolean isHqBranch(CenterDto c){ return c != null && Objects.equals(c.getBranchId(), 1L); }
  private boolean isTeamBranch(CenterDto c){ return c != null && c.getBranchId() != null && !Objects.equals(c.getBranchId(), 1L); }
  private int rank(String r) {
    return switch (r) {
      case "SUPERADMIN" -> 5;
      case "CENTERHEAD" -> 4;
      case "EXPERT"     -> 3;
      case "MANAGER"    -> 2;
      case "STAFF"      -> 1;
      default           -> 0;
    };
  }
  
  /** 010-xxxx-xxxx 포맷 보정 (빈값 허용 X) */
  private String normalizePhone(String v) {
    String d = (v == null ? "" : v).replaceAll("\\D", "");
    if (!d.startsWith("010") || (d.length() != 10 && d.length() != 11)) {
      throw new AuthException("전화번호 형식 오류(예: 010-1234-5678)", HttpStatus.GONE);
    }
    return (d.length() == 10)
        ? String.format("010-%s-%s", d.substring(3, 6), d.substring(6))
        : String.format("010-%s-%s", d.substring(3, 7), d.substring(7));
  }
}
