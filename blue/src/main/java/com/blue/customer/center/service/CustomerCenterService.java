package com.blue.customer.center.service;

import com.blue.customer.all.dto.PagedResponse;
import com.blue.customer.all.dto.UserContextDto;
import com.blue.customer.center.dto.BranchSimpleDto;
import com.blue.customer.center.dto.CenterDbRowDto;
import com.blue.customer.center.dto.CenterSimpleDto;
import com.blue.customer.center.mapper.CustomerCenterMapper;
import com.blue.global.exception.AuthException;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerCenterService {
  
  private final CustomerCenterMapper mapper;
  
  /* =========================
     권한/컨텍스트
  ========================= */
  private UserContextDto requireMe(String callerEmail) {
    UserContextDto me = mapper.findUserContextByEmail(callerEmail);
    if (me == null) throw new AuthException("인증 사용자 정보를 찾을 수 없습니다.",HttpStatus.GONE);
    if (!List.of("SUPERADMIN", "CENTERHEAD", "EXPERT").contains(me.getRole())) {
      throw new AuthException("접근 권한이 없습니다.",HttpStatus.GONE);
    }
    return me;
  }
  
  /* =========================
     드롭다운: 지점/팀
  ========================= */
  public List<BranchSimpleDto> getBranches(String callerEmail) {
    UserContextDto me = requireMe(callerEmail);
    // 필요하면 여기서 role별로 범위 제한 가능(현재는 목록 전체 제공)
    return mapper.findBranchesExcludingHQ();
  }
  
  public List<CenterSimpleDto> getTeams(String callerEmail, Long branchId) {
    UserContextDto me = requireMe(callerEmail);
    if (branchId == null) return List.of();
    // 필요하면 여기서 role별로 범위 제한 가능(현재는 branch 기준 팀 목록 제공)
    return mapper.findCentersByBranchId(branchId);
  }
  
  /* =========================
     센터DB 목록 (조회 전용)
  ========================= */
  public PagedResponse<CenterDbRowDto> getCenterDb(
      String callerEmail,
      int page, int size,
      String keyword, String dateFrom, String dateTo,
      String category, String division, String sort,
      String expertName, String status,
      Long branchId, Long centerId
  ) {
    UserContextDto me = requireMe(callerEmail);
    
    int offset = (page - 1) * size;
    List<CenterDbRowDto> items;
    int total;
    
    switch (me.getRole()) {
      case "SUPERADMIN" -> {
        items = mapper.findForAdmin(
            offset, size,
            keyword, dateFrom, dateTo,
            category, division, sort,
            expertName, status,
            branchId, centerId,
            me.getVisible()
        );
        total = mapper.countForAdmin(
            keyword, dateFrom, dateTo,
            category, division,
            expertName, status,
            branchId, centerId,
            me.getVisible()
        );
      }
      case "CENTERHEAD" -> {
        // 센터헤드는 자기 팀(center) 범위로 강제
        Long enforcedCenterId = me.getCenterId();
        items = mapper.findForCenterHead(
            offset, size,
            keyword, dateFrom, dateTo,
            category, division, sort,
            expertName, status,
            branchId, centerId,
            me.getVisible(),
            enforcedCenterId
        );
        total = mapper.countForCenterHead(
            keyword, dateFrom, dateTo,
            category, division,
            expertName, status,
            branchId, centerId,
            me.getVisible(),
            enforcedCenterId
        );
      }
      case "EXPERT" -> {
        Long myExpertId = mapper.findExpertIdByUserId(me.getUserId());
        if (myExpertId == null) {
          items = List.of();
          total = 0;
        } else {
          items = mapper.findForExpert(
              offset, size,
              keyword, dateFrom, dateTo,
              category, division, sort,
              status,
              branchId, centerId,
              me.getVisible(),
              myExpertId
          );
          total = mapper.countForExpert(
              keyword, dateFrom, dateTo,
              category, division,
              status,
              branchId, centerId,
              me.getVisible(),
              myExpertId
          );
        }
      }
      default -> throw new AuthException("Unknown role: " + me.getRole(),HttpStatus.GONE);
    }
    
    // visible=N: 전화 마스킹 (센터DB 허용 역할 전부 적용)
    if ("N".equalsIgnoreCase(me.getVisible())) {
      items.forEach(r -> r.setPhone(maskPhone(r.getPhone())));
    }
    
    // 매출: 0이면 null(화면 빈칸)
    normalizePrices(items);
    
    int totalPages = (int) Math.ceil((double) total / size);
    if (totalPages == 0) totalPages = 1;
    
    return new PagedResponse<>(items, totalPages, total);
  }
  
  /* =========================
     엑셀 다운로드
  ========================= */
  public ResponseEntity<byte[]> exportExcel(
      String callerEmail,
      String keyword, String dateFrom, String dateTo,
      String category, String division, String sort,
      String expertName, String status,
      Long branchId, Long centerId
  ) {
    UserContextDto me = requireMe(callerEmail);
    
    final int MAX = 50_000;
    
    List<CenterDbRowDto> data;
    
    switch (me.getRole()) {
      case "SUPERADMIN" -> data = mapper.findForAdmin(
          0, MAX,
          keyword, dateFrom, dateTo,
          category, division, sort,
          expertName, status,
          branchId, centerId,
          me.getVisible()
      );
      case "CENTERHEAD" -> {
        Long enforcedCenterId = me.getCenterId();
        data = mapper.findForCenterHead(
            0, MAX,
            keyword, dateFrom, dateTo,
            category, division, sort,
            expertName, status,
            branchId, centerId,
            me.getVisible(),
            enforcedCenterId
        );
      }
      case "EXPERT" -> {
        Long myExpertId = mapper.findExpertIdByUserId(me.getUserId());
        if (myExpertId == null) {
          data = List.of();
        } else {
          data = mapper.findForExpert(
              0, MAX,
              keyword, dateFrom, dateTo,
              category, division, sort,
              status,
              branchId, centerId,
              me.getVisible(),
              myExpertId
          );
        }
      }
      default -> throw new AuthException("Unknown role: " + me.getRole(),HttpStatus.GONE);
    }
    
    if ("N".equalsIgnoreCase(me.getVisible())) {
      data.forEach(r -> r.setPhone(maskPhone(r.getPhone())));
    }
    
    try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sh = wb.createSheet("센터DB");
      
      Row h = sh.createRow(0);
      h.createCell(0).setCellValue("생성일");
      h.createCell(1).setCellValue("이름");
      h.createCell(2).setCellValue("전화번호");
      h.createCell(3).setCellValue("팀");
      h.createCell(4).setCellValue("출처");
      
      int r = 1;
      for (CenterDbRowDto row : data) {
        Row rr = sh.createRow(r++);
        rr.createCell(0).setCellValue(row.getCreatedAt() == null ? "" : row.getCreatedAt().toString().replace('T', ' '));
        rr.createCell(1).setCellValue(nz(row.getName()));
        rr.createCell(2).setCellValue(nz(row.getPhone()));
        rr.createCell(3).setCellValue(nz(row.getCenterName()));
        rr.createCell(4).setCellValue(nz(row.getSource()));
      }
      
      wb.write(out);
      
      String filename = URLEncoder.encode("center-db.xlsx", StandardCharsets.UTF_8).replace("+", "%20");
      return ResponseEntity.ok()
          .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
          .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
          .body(out.toByteArray());
    } catch (Exception e) {
      throw new AuthException("엑셀 생성 실패",HttpStatus.GONE);
    }
  }
  
  private void normalizePrices(List<CenterDbRowDto> items) {
    if (items == null || items.isEmpty()) return;
    for (CenterDbRowDto r : items) {
      if (r.getInitialPrice() != null && r.getInitialPrice() == 0) r.setInitialPrice(null);
      if (r.getUpsellPrice() != null && r.getUpsellPrice() == 0) r.setUpsellPrice(null);
    }
  }
  
  private String nz(String s) { return s == null ? "" : s; }
  
  private String maskPhone(String phone) {
    if (phone == null) return null;
    String digits = phone.replaceAll("\\D", "");
    if (digits.length() < 7) return phone;
    return digits.substring(0, 3) + "-****-" + digits.substring(digits.length() - 4);
  }
}
