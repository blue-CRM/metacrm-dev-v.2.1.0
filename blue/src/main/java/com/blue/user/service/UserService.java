package com.blue.user.service;

import com.blue.global.exception.AuthException;
import com.blue.user.dto.BulkApproveResponse;
import com.blue.user.dto.CenterDto;
import com.blue.user.dto.PageResponse;
import com.blue.user.dto.UserSelectDto;
import com.blue.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UserService {
  
  private final UserMapper userMapper;
  
  // 슈퍼계정인지 판단
  private boolean isRequesterSuper(String email) {
    Boolean b = userMapper.isSuperByEmail(email);
    return Boolean.TRUE.equals(b);
  }
  
  // 센터 목록 조회
  public List<CenterDto> findCenters() {
    return userMapper.findCenters();
  }
  
  // 페이지 로딩시 최초 조회
  public PageResponse<UserSelectDto> getUsers(int page, int size, String keyword) {
    int offset = (page - 1) * size;
    List<UserSelectDto> items = userMapper.findUsers(offset, size, keyword);
    int totalCount = userMapper.countUsers(keyword);
    int totalPages = (int) Math.ceil((double) totalCount / size);
    
    return new PageResponse<>(items, totalPages, totalCount);
  }
  
  // 특정 팀에 팀장 몇 명인지 조회 (본사면 0 반환)
  public int countManagersInCenter(String centerName, Long excludeUserId) {
    if ("본사".equals(centerName)) return 0;
    return userMapper.countManagersInCenter(centerName, excludeUserId);
  }
  
  // 특정 센터에 센터장이 몇 명인지 조회 (본사면 0 반환)
  public int countCenterHeadsInCenter(String centerName, Long excludeUserId) {
    if ("본사".equals(centerName)) return 0;
    return userMapper.countCenterHeadsInCenter(centerName, excludeUserId);
  }
  
  // 직원관리 페이지에서 배지 수정이 발생한 경우
  @Transactional
  public void updateUserField(Long userId, String field, String value, String requesterEmail) {
    // 수정 대상 조회
    UserSelectDto target = userMapper.findUserById(userId);
    if (target == null) {
      throw new AuthException("해당 직원을 찾을 수 없습니다.",HttpStatus.GONE);
    }
    
    // 1. 본인 계정 수정 금지
    if (target.getUserEmail().equals(requesterEmail)) {
      throw new AuthException("본인 계정은 수정할 수 없습니다.",HttpStatus.GONE);
    }
    
    // 2. 특별 계정이 아닌 경우 → 본사/관리자 계정 수정 불가
    boolean isRestricted = "SUPERADMIN".equals(target.getUserRole()) || "본사".equals(target.getCenterName());
    if (isRestricted && !isRequesterSuper(requesterEmail)) {
      throw new AuthException("권한이 없습니다.",HttpStatus.GONE);
    }
    
    boolean requesterIsSuper = isRequesterSuper(requesterEmail);
    String targetRole = target.getUserRole();
    // 3-공통) 가시권한/분배권한은 둘 다 super만 수정 가능
    if (("visible".equals(field) || "allocate".equals(field)) && !requesterIsSuper) {
      throw new AuthException("권한이 없습니다.",HttpStatus.GONE);
    }
    
    // 3-1) 가시권한: 대상이 관리자(SUPERADMIN), 센터장, 전문가인 경우만 허용
    if ("visible".equals(field)) {
      boolean canChangeVisibleTarget =
          "SUPERADMIN".equals(targetRole) ||
              "CENTERHEAD".equals(targetRole) ||
              "EXPERT".equals(targetRole);
      
      if (!canChangeVisibleTarget) {
        throw new AuthException("관리자/센터장/전문가의 가시권한만 변경할 수 있습니다.", HttpStatus.GONE);
      }
    }
    
    // 3-2) 분배권한: 대상이 관리자/센터장/전문가인 경우만 허용
    if ("allocate".equals(field)) {
      if (!"SUPERADMIN".equals(targetRole)
          && !"CENTERHEAD".equals(targetRole)
          && !"EXPERT".equals(targetRole)) {
        throw new AuthException("관리자/센터장/전문가의 분배권한만 변경할 수 있습니다.",HttpStatus.GONE);
      }
    }
    
    // 3-3) 관리자(SUPERADMIN) 승격은 총관리자(is_super)만 가능 (프론트 가드와 동일 기준, C-5)
    if ("type".equals(field) && "관리자".equals(value) && !requesterIsSuper) {
      throw new AuthException("관리자로의 승격은 총관리자만 가능합니다.", HttpStatus.GONE);
    }

    // 4) 팀장 1명 제한(서버 가드)
    // 4-1) 구분을 '팀장'으로 바꾸는 경우 → 현재 소속에 다른 팀장이 있으면 불가 (본사 제외)
    if ("type".equals(field) && "팀장".equals(value)) {
      String centerName = target.getCenterName();
      if (centerName != null && !"본사".equals(centerName)) {
        int cnt = countManagersInCenter(centerName, userId);
        if (cnt > 0) {
          throw new AuthException("'" + centerName + "'에는 이미 팀장이 있습니다.",HttpStatus.GONE);
        }
      }
    }
    
    // 4-2) 소속을 바꾸는 경우 → 센터/팀/본사 규칙 가드
    if ("center".equals(field)) {
      boolean targetIsManager = "MANAGER".equals(targetRole);
      boolean targetIsCenterHead = "CENTERHEAD".equals(targetRole);
      
      CenterDto newCenter = findCenterByNameInternal(value);
      
      if (newCenter != null) {
        boolean centerType = isCenterType(newCenter); // 브랜치=본사, 센터≠본사센터
        boolean teamType = isTeamType(newCenter);     // 브랜치≠본사
        
        // 6) 어떤 DB의 소속을 센터로 변경할 경우 → 권한이 센터장/전문가가 아니면 막기
        if (centerType) {
          if (!"CENTERHEAD".equals(targetRole) && !"EXPERT".equals(targetRole)) {
            throw new AuthException("센터 소속은 센터장/전문가만 가능합니다.",HttpStatus.GONE);
          }
        }
        
        // 7) 어떤 DB의 소속을 팀으로 변경할 경우 → 권한이 팀장/팀원이 아니면 막기
        if (teamType) {
          if (!"MANAGER".equals(targetRole) && !"STAFF".equals(targetRole)) {
            throw new AuthException("팀 소속은 팀장/팀원만 가능합니다.",HttpStatus.GONE);
          }
        }
        
        // 팀장 1명 가드 (팀 타입에서만 의미 있음)
        if (targetIsManager && teamType) {
          int cnt = countManagersInCenter(value, userId);
          if (cnt > 0) {
            throw new AuthException("'" + value + "'에는 이미 팀장이 있습니다.",HttpStatus.GONE);
          }
        }
        
        // 센터장 1명 가드 (센터 타입에서만)
        // 8) 어떤 DB의 소속을 센터로 변경하는데 해당 센터에 센터장이 이미 있을 경우
        if (targetIsCenterHead && centerType) {
          int cnt = userMapper.countCenterHeadsInCenter(value, userId);
          if (cnt > 0) {
            throw new AuthException("'" + value + "'에는 이미 센터장이 있습니다.",HttpStatus.GONE);
          }
        }
      }
    }
    
    // 4-3) 구분을 '센터장'으로 바꾸는 경우 → 현재 소속에 다른 센터장이 있으면 불가 (본사 제외)
    if ("type".equals(field) && "센터장".equals(value)) {
      String centerName = target.getCenterName();
      if (centerName != null && !"본사".equals(centerName)) {
        int cnt = userMapper.countCenterHeadsInCenter(centerName, userId);
        if (cnt > 0) {
          throw new AuthException("'" + centerName + "'에는 이미 센터장이 있습니다.",HttpStatus.GONE);
        }
      }
    }
    
    // 5. 전문가 배지 변경 (experts 테이블 연동) — 다른 필드와 분리 처리
    if ("expert".equals(field)) {
      // C-6) 전문가 지정은 총관리자만 (프론트 editable 기준과 동일)
      if (!requesterIsSuper) {
        throw new AuthException("전문가 지정은 총관리자만 가능합니다.",HttpStatus.GONE);
      }
      if (!"CENTERHEAD".equals(targetRole) && !"EXPERT".equals(targetRole)) {
        throw new AuthException("권한이 '센터장' 또는 '전문가'인 직원만 전문가를 지정할 수 있습니다.",HttpStatus.GONE);
      }
    }
    
    // 어느 직급에서 어느 직급으로 변경되었는지 확인하기 위해,
    // 업데이트 이전 직급과 업데이트 희망 직급을 둘 다 저장
    String oldRole = target.getUserRole();
    String newRole = oldRole;
    if ("type".equals(field)) {
      if ("관리자".equals(value)) newRole = "SUPERADMIN";
      else if ("팀장".equals(value)) newRole = "MANAGER";
      else if ("프로".equals(value)) newRole = "STAFF";
      else if ("센터장".equals(value)) newRole = "CENTERHEAD";
      else if ("전문가".equals(value)) newRole = "EXPERT";
    }
    
    // 조건 통과한 경우만 실제 업데이트 실행
    userMapper.updateUserField(userId, field, value);
    
    // 업데이트가 성공적일 경우 + 역할이 변경된 경우
    // 각각에 해당하는 부가적인 처리를 실행 (ex. 가시권한 변경 등)
    if ("type".equals(field)) {
      handleRoleChangeSideEffects(userId, oldRole, newRole);
    }
  }
  
  // 직원관리 페이지에서 직원에 대한 일괄수정이 발생한 경우
  @Transactional
  public BulkApproveResponse bulkApprove(List<Long> userIds, String requesterEmail) {
    List<UserSelectDto> users = userMapper.findUsersByIds(userIds);
    
    List<Long> approved = new ArrayList<>(); // 승인 성공한 사람들
    List<Long> skipped = new ArrayList<>(); // 제외된 사람들
    
    boolean requesterIsSuper = isRequesterSuper(requesterEmail);
    for (UserSelectDto u : users) {
      // 승인하고 싶은 계정이 관리자권한 혹은 본사소속인지 확인
      boolean isRestricted = "SUPERADMIN".equals(u.getUserRole()) || "본사".equals(u.getCenterName());
      
      // 만약 승인대상에 관리자/본사가 포함되어있으면서
      // 동시에 요청한 사람의 계정이 특별한 계정이 아니라면
      if (isRestricted && !requesterIsSuper) {
        // 승인불가 리스트에 추가
        skipped.add(u.getUserId());
      } else {
        approved.add(u.getUserId());
      }
    }
    
    // 승인 가능한 사용자만 일괄 승인
    if (!approved.isEmpty()) {
      userMapper.approveUsers(approved);
    }
    
    return new BulkApproveResponse(approved, skipped);
  }
  
  /**
   * 역할 전환 시 부가 처리
   * 팀장/팀원 -> 센터장/전문가 : 센터 미배정, 요청상태 유지, 분배권한 차단, 가시권한 차단
   * 센터장 -> 전문가 : 센터 유지, 요청상태 유지, 가시권한 차단, 분배권한 차단, 전문가 미지정
   * 전문가 -> 센터장 : 센터에 센터장이 없었다면 가능(사전 가드), 요청상태/센터 유지, 가시권한 차단, 분배권한 차단
   * 센터장/전문가 -> 본사 : 소속 본사, 요청상태 유지, 가시권한 공개, 분배권한 허용, 전문가 미지정
   * 본사 -> 센터장/전문가 : 소속 미배정, 요청상태 유지, 가시권한 차단, 분배권한 차단, 전문가 미지정
   * + 기존 팀장 회수 로직 + 예전 CASE(관리자<->팀장/프로) 보완
   */
  private void handleRoleChangeSideEffects(Long userId, String oldRole, String newRole) {
    boolean oldIsTeam        = "MANAGER".equals(oldRole) || "STAFF".equals(oldRole);
    boolean newIsTeam        = "MANAGER".equals(newRole) || "STAFF".equals(newRole);
    boolean oldIsCenterRole  = "CENTERHEAD".equals(oldRole) || "EXPERT".equals(oldRole);
    boolean newIsCenterRole  = "CENTERHEAD".equals(newRole) || "EXPERT".equals(newRole);

    /* =========================
     A. 예전 CASE 보완: 관리자 <-> 팀장/프로
     ========================= */
    
    // A-1) SUPERADMIN -> MANAGER/STAFF (관리자 → 팀장/프로)
    // => 서비스 레벨에서도 의도를 명시적으로 표현
    if ("SUPERADMIN".equals(oldRole) && newIsTeam) {
      // 소속 제거 + 가시권한 공개
      userMapper.updateUserField(userId, "center", null);   // center_id = NULL
      userMapper.updateUserField(userId, "visible", "Y");// manager_phone_access = '공개'
    }
    
    // A-2) MANAGER/STAFF -> SUPERADMIN (팀장/프로 → 관리자)
    if (oldIsTeam && "SUPERADMIN".equals(newRole)) {
      userMapper.updateUserField(userId, "center", "본사"); // center_id = 1
      userMapper.updateUserField(userId, "visible", "N");// manager_phone_access = '차단'
      // 분배권한은 아래 B-6에서 역할 기본값('N', 슈퍼가 사후 부여)으로 일원화
    }

     /* =========================
     B. 팀/센터/전문가/본사 규칙
     ========================= */

    // B-1) 팀장/팀원 -> 센터장/전문가
    if (oldIsTeam && newIsCenterRole) {
      // 센터 미배정 + 가시권한 차단
      userMapper.updateUserField(userId, "center", null);
      userMapper.updateUserField(userId, "visible", "N");
    }

    // B-1.5) 센터장/전문가 -> 팀장/프로 (강등): 센터 소속 비움(팀 재배정 필요) + 가시권한 공개(팀 기본값).
    // 전문가 배지 해제는 아래 B-5.5에서 일괄 처리. (A-1/A-2/G1 정합성 수정)
    if (oldIsCenterRole && newIsTeam) {
      userMapper.updateUserField(userId, "center", null);
      userMapper.updateUserField(userId, "visible", "Y");
    }

    // B-2) 센터장 -> 전문가
    if ("CENTERHEAD".equals(oldRole) && "EXPERT".equals(newRole)) {
      userMapper.updateUserField(userId, "visible", "N");
      // 전문가 전환 시 배지는 '미지정'부터 시작
      userMapper.updateUserField(userId, "expert", null);
    }

    // B-3) 전문가 -> 센터장 (센터장 1명 가드는 updateUserField에서 이미 체크)
    if ("EXPERT".equals(oldRole) && "CENTERHEAD".equals(newRole)) {
      userMapper.updateUserField(userId, "visible", "N");
      // 센터장이 되면 전문가 배지 제거
      userMapper.updateUserField(userId, "expert", null);
    }

    // B-4) 센터장/전문가 -> 본사
    if (oldIsCenterRole && "SUPERADMIN".equals(newRole)) {
      // 소속 본사, 가시권한 차단, 전문가 배지 제거 (분배권한은 B-6에서 'N')
      userMapper.updateUserField(userId, "center", "본사");
      userMapper.updateUserField(userId, "visible", "N");
      userMapper.updateUserField(userId, "expert", null);
    }

    // B-5) 본사 -> 센터장/전문가
    if ("SUPERADMIN".equals(oldRole) && newIsCenterRole) {
      // 소속 미배정, 가시권한 차단, 전문가 배지 제거 (분배권한은 B-6에서 'Y')
      userMapper.updateUserField(userId, "center", null);
      userMapper.updateUserField(userId, "visible", "N");
      userMapper.updateUserField(userId, "expert", null);
    }

    // B-5.5) 역할이 바뀌어 'EXPERT'가 아니게 되면 전문가 배지(expert_id) 무조건 해제.
    // (B-3/B-4가 못 잡던 EXPERT·CENTERHEAD -> 팀장/프로 강등까지 전부 '미지정'으로 정리)
    if (!oldRole.equals(newRole) && !"EXPERT".equals(newRole)) {
      userMapper.updateUserField(userId, "expert", null);
    }

    /* =========================
     B-6. 분배권한(can_allocate) 표준화 — 역할 기본값으로 일원화
       역할이 실제로 바뀌면, 새 역할의 '기본 분배권한'으로 동기화한다.
       · MANAGER(팀장)·CENTERHEAD(센터장)·EXPERT(전문가) = 'Y'
         - MANAGER: 분배 메뉴가 역할 기반(useAcl base)으로 상시 노출 + 수동토글 불가 → 항상 'Y'
         - CENTERHEAD/EXPERT: 기본 분배권한 보유(가입 기본값과 동일). 슈퍼가 이후 수동 'N' 토글 가능
       · STAFF(프로)      = 'N' (수동토글 불가)
       · SUPERADMIN(관리자) = 'N' (슈퍼계정이 사후에 수동 'Y' 부여)
       ※ 교정된 버그 2건:
         (1) '→팀장/프로' 전환에서 allocate 미갱신 → STAFF→MANAGER 승급 시 'N' 잔존(회원관리 표시 불일치)
         (2) '→센터장/전문가' 전환에서 의도와 달리 'N'으로 강등(센터장/전문가는 기본 'Y'여야 함)
     ========================= */
    if (!oldRole.equals(newRole)) {
      if ("MANAGER".equals(newRole) || "CENTERHEAD".equals(newRole) || "EXPERT".equals(newRole)) {
        userMapper.updateUserField(userId, "allocate", "Y");
      } else if ("STAFF".equals(newRole) || "SUPERADMIN".equals(newRole)) {
        userMapper.updateUserField(userId, "allocate", "N");
      }
    }

    /* =========================
     C. 팀장 → 다른 역할로 바뀔 때 상태 '없음' DB 자동 회수
     ========================= */
    
    boolean managerToStaff      = "MANAGER".equals(oldRole) && "STAFF".equals(newRole);
    boolean managerToHq         = "MANAGER".equals(oldRole) && "SUPERADMIN".equals(newRole);
    boolean managerToCenterRole = "MANAGER".equals(oldRole)
        && ("CENTERHEAD".equals(newRole) || "EXPERT".equals(newRole));
    
    if (managerToStaff || managerToHq || managerToCenterRole) {
      userMapper.autoRecallStatusNoneByOwner(userId);
    }
  }
  
  // centerName(화면에서 선택한 소속 이름)으로 CenterDto 찾기
  private CenterDto findCenterByNameInternal(String centerName) {
    if (centerName == null || centerName.isBlank()) {
      return null;
    }
    // 이미 있는 findCenters() 재사용 (센터 수가 많지 않으니 이 정도는 괜찮음)
    List<CenterDto> centers = userMapper.findCenters();
    for (CenterDto c : centers) {
      if (centerName.equals(c.getCenterName())) {
        return c;
      }
    }
    return null;
  }
  
  // "센터 타입" 판별
  //  - 브랜치가 본사지점(1)
  //  - 센터아이디가 본사센터(1)은 아님
  private boolean isCenterType(CenterDto center) {
    if (center == null) return false;
    return Objects.equals(center.getBranchId(), 1L)
        && !Objects.equals(center.getCenterId(), 1L);
  }
  
  // "팀 타입" 판별
  //  - 브랜치가 본사지점이 아닌 경우
  private boolean isTeamType(CenterDto center) {
    if (center == null) return false;
    return center.getBranchId() != null
        && !Objects.equals(center.getBranchId(), 1L);
  }
}
