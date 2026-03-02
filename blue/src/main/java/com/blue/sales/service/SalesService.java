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
  
  public void sendSnapshotEmail(
      String callerEmail,
      SalesSnapshotMeta meta,
      MultipartFile month,
      MultipartFile range,
      MultipartFile all
  ) {
    UserContextDto me = requireMe(callerEmail);
    
    if (!"SUPERADMIN".equals(me.getRole())) {
      throw new AuthException("관리자만 스냅샷 촬영이 가능합니다.", HttpStatus.FORBIDDEN);
    }
    
    meta = (meta == null) ? new SalesSnapshotMeta() : meta;
    
    String now = LocalDateTime.now(KST).format(SUBJECT_TS);
    String subject = "매출현황 스냅샷 - " + now;
    
    String text =
        "매출현황 스냅샷입니다.\n"
            + "viewMode=" + nvl(meta.getViewMode()) + "\n"
            + "includeResigned=" + (Boolean.TRUE.equals(meta.getIncludeResigned()) ? "true" : "false") + "\n"
            + "dateFrom=" + nvl(meta.getDateFrom()) + "\n"
            + "dateTo=" + nvl(meta.getDateTo()) + "\n";
    
    mailService.sendSalesSnapshot(
        me.getEmail(),
        subject,
        text,
        month, range, all
    );
  }
  
  private String nvl(String s) {
    return (s == null) ? "" : s.trim();
  }
}