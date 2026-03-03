package com.blue.sales.service;

import com.blue.customer.all.dto.UserContextDto;
import com.blue.sales.dto.*;
import com.blue.sales.mapper.SalesMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import com.blue.global.exception.AuthException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SalesService {
  
  private final SalesMapper mapper;
  private final SalesMailService mailService;
  
  private static final ZoneId KST = ZoneId.of("Asia/Seoul");
  private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  private static final DateTimeFormatter SUBJECT_TS = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");
  
  private UserContextDto requireMe(String callerEmail) {
    UserContextDto me = mapper.findUserContextByEmail(callerEmail);
    if (me == null) throw new AuthException("인증 사용자 정보를 찾을 수 없습니다.", HttpStatus.GONE);
    return me;
  }
  
  public SalesDashboardRes getDashboard(String callerEmail, SalesDashboardReq req) {
    requireMe(callerEmail);
    
    String viewMode = (req.getViewMode() == null || req.getViewMode().isBlank())
        ? "BRANCH" : req.getViewMode().trim();
    
    boolean includeResigned = Boolean.TRUE.equals(req.getIncludeResigned());
    
    // PartitionBar
    List<SalesPartitionRow> parts = mapper.findSalesPartitions(viewMode);
    List<Long> stops = parts.stream().map(SalesPartitionRow::getSegment).toList();
    List<String> colors = parts.stream().map(SalesPartitionRow::getColor).toList();
    
    LocalDateTime now = LocalDateTime.now(KST);
    
    // month: 항상 이번달
    LocalDateTime monthStart = now.toLocalDate().withDayOfMonth(1).atStartOfDay();
    LocalDateTime monthEndExclusive = now;
    
    // range: dateFrom/dateTo 있으면 사용, 없으면 최근 30일
    LocalDate rangeFromD;
    LocalDate rangeToD;
    if (req.getDateFrom() != null && !req.getDateFrom().isBlank()
        && req.getDateTo() != null && !req.getDateTo().isBlank()) {
      rangeFromD = LocalDate.parse(req.getDateFrom().trim());
      rangeToD = LocalDate.parse(req.getDateTo().trim());
    } else {
      rangeToD = now.toLocalDate();
      rangeFromD = rangeToD.minusDays(29);
    }
    LocalDateTime rangeStart = rangeFromD.atStartOfDay();
    LocalDateTime rangeEndExclusive = rangeToD.plusDays(1).atStartOfDay();
    
    // all: 2025-01-01 ~ now
    LocalDateTime allStart = LocalDate.of(2025, 1, 1).atStartOfDay();
    LocalDateTime allEndExclusive = now;
    
    List<SalesAggRowDto> monthRows = mapper.findAggRows(viewMode, includeResigned, ts(monthStart), ts(monthEndExclusive));
    List<SalesAggRowDto> rangeRows = mapper.findAggRows(viewMode, includeResigned, ts(rangeStart), ts(rangeEndExclusive));
    List<SalesAggRowDto> allRows   = mapper.findAggRows(viewMode, includeResigned, ts(allStart), ts(allEndExclusive));
    
    monthRows = normalizeAndSort(monthRows);
    rangeRows = normalizeAndSort(rangeRows);
    allRows   = normalizeAndSort(allRows);
    
    return SalesDashboardRes.builder()
        .viewMode(viewMode)
        .includeResigned(includeResigned)
        .partitionStops(stops)
        .partitionColors(colors)
        .month(SalesSectionDataDto.builder().rows(monthRows).topRanks(top3(monthRows)).build())
        .range(SalesSectionDataDto.builder().rows(rangeRows).topRanks(top3(rangeRows)).build())
        .all(SalesSectionDataDto.builder().rows(allRows).topRanks(top3(allRows)).build())
        .build();
  }
  
  private String ts(LocalDateTime t) { return t.format(TS); }
  
  private List<SalesAggRowDto> normalizeAndSort(List<SalesAggRowDto> rows) {
    if (rows == null || rows.isEmpty()) return List.of();
    
    for (SalesAggRowDto r : rows) {
      long a = r.getA() == null ? 0 : r.getA();
      long b = r.getB() == null ? 0 : r.getB();
      long c = r.getC() == null ? 0 : r.getC();
      r.setA(a); r.setB(b); r.setC(c);
      r.setTotal(a + b + c);
      if (r.getResigned() == null) r.setResigned(false);
    }
    
    rows.sort(Comparator.comparingLong(SalesAggRowDto::getTotal).reversed()
        .thenComparing(SalesAggRowDto::getLabel, Comparator.nullsLast(String::compareTo)));
    return rows;
  }
  
  private List<SalesRankRowDto> top3(List<SalesAggRowDto> rows) {
    if (rows == null || rows.isEmpty()) return List.of();
    return rows.stream().limit(3).map(r -> SalesRankRowDto.builder()
        .key(r.getKey())
        .label(r.getLabel())
        .resigned(r.getResigned())
        .total(r.getTotal())
        .build()).toList();
  }
  
  public void sendSnapshotEmail(String callerEmail, SalesSnapshotMeta meta,
                                MultipartFile month, MultipartFile range, MultipartFile all) {
    UserContextDto me = requireMe(callerEmail);
    
    if (!"SUPERADMIN".equals(me.getRole())) {
      throw new AuthException("관리자만 스냅샷 촬영이 가능합니다.", HttpStatus.FORBIDDEN);
    }
    
    meta = (meta == null) ? new SalesSnapshotMeta() : meta;
    
    var now = java.time.ZonedDateTime.now(KST);
    String nowStr = now.format(java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd (E) HH:mm"));
    
    String subject = "매출현황 스냅샷 - " + now.format(SUBJECT_TS);
    
    // 보기모드 라벨
    String modeLabel = viewModeLabel(meta.getViewMode());
    
    // 스케줄용 문구(옵션) — meta에 값이 없으면 그냥 빈값
    String scheduleTitle = buildScheduleTitleIfAny(meta); // 아래 helper 참고
    
    String plain =
        "매출현황 스냅샷\n"
            + "캡처 일시: " + nowStr + "\n"
            + (scheduleTitle.isBlank() ? "" : (scheduleTitle + "\n"))
            + "조건\n"
            + "- 보기 모드: " + modeLabel + " (" + nvl(meta.getViewMode()) + ")\n"
            + "- 퇴사자 포함: " + (Boolean.TRUE.equals(meta.getIncludeResigned()) ? "ON" : "OFF") + "\n"
            + "- 기간내: " + fmtRange(meta.getDateFrom(), meta.getDateTo()) + "\n"
            + "\n첨부: 이번달 / 기간내 / 전체기간 스냅샷 이미지\n";
    
    // html은 메타로 예쁘게 만들기 (SalesMailService에서)
    mailService.sendSalesSnapshot(
        me.getEmail(),
        subject,
        plain,
        nowStr,
        scheduleTitle,
        meta,
        month, range, all
    );
  }
  
  private String viewModeLabel(String v) {
    if (v == null) return "지점별";
    return switch (v.trim()) {
      case "BRANCH" -> "지점별";
      case "TEAM" -> "팀별";
      case "PERSON" -> "개인별";
      default -> v;
    };
  }
  
  private String fmtRange(String from, String to) {
    String f = (from == null || from.isBlank()) ? "-" : from.trim();
    String t = (to == null || to.isBlank()) ? "-" : to.trim();
    return f + " ~ " + t;
  }
  
  /**
   * 스케줄 스냅샷일 때만 예: 2026년 03월 전체 데이터 스냅샷
   * (meta에 scheduled/snapshotYm 같은 필드를 나중에 추가해도 되고, 없으면 빈 문자열 반환)
   */
  private String buildScheduleTitleIfAny(SalesSnapshotMeta meta) {
    // 아래처럼 meta에 필드 추가하면 바로 동작하게 만들어둠 (없으면 무조건 빈 문자열)
    try {
      java.lang.reflect.Method mScheduled = meta.getClass().getMethod("getScheduled");
      Object scheduled = mScheduled.invoke(meta);
      if (!(scheduled instanceof Boolean) || !((Boolean) scheduled)) return "";
      
      java.lang.reflect.Method mYm = meta.getClass().getMethod("getSnapshotYm");
      Object ym = mYm.invoke(meta);
      String s = (ym == null) ? "" : ym.toString().trim(); // "2026-03" 기대
      if (s.matches("\\d{4}-\\d{2}")) {
        String y = s.substring(0, 4);
        String mm = s.substring(5, 7);
        return y + "년 " + mm + "월 전체 데이터 스냅샷";
      }
      return "스케줄 전체 데이터 스냅샷";
    } catch (Exception ignore) {
      return "";
    }
  }
  
  private String nvl(String s) {
    return (s == null) ? "" : s.trim();
  }
}