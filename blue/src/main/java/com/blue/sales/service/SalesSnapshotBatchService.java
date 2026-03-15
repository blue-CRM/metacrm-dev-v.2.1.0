package com.blue.sales.service;

import com.blue.sales.dto.SalesAggRowDto;
import com.blue.sales.dto.SalesSnapshotDashboardRenderer;
import com.blue.sales.dto.SalesSnapshotMeta;
import com.blue.sales.mapper.SalesMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SalesSnapshotBatchService {
  
  private final SalesMapper salesMapper;
  private final SalesMailService mailService;
  private final SalesSnapshotDashboardRenderer dashboardRenderer;
  private final StringRedisTemplate redisTemplate;
  
  private static final ZoneId KST = ZoneId.of("Asia/Seoul");
  private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  
  public void sendPrevMonthSnapshotToAdmins() {
    ZonedDateTime now = ZonedDateTime.now(KST);
    
    YearMonth ym = YearMonth.from(now.minusMonths(1)); // 직전 달
    LocalDateTime start = ym.atDay(1).atStartOfDay();
    LocalDateTime endExclusive = ym.plusMonths(1).atDay(1).atStartOfDay();
    
    String lockKey = "sales:snapshot:monthly:" + ym + ":lock";
    
    // in-progress 락 (다중 인스턴스 대비)
    Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", Duration.ofMinutes(60));
    if (!Boolean.TRUE.equals(locked)) {
      log.info("[SalesSnapshotBatchService] already running: {}", lockKey);
      return;
    }
    
    try {
      List<String> emails = salesMapper.findAdminEmails();
      if (emails == null || emails.isEmpty()) {
        log.info("[SalesSnapshotBatchService] admin email empty");
        return;
      }
      
      SalesSnapshotMeta meta = new SalesSnapshotMeta();
      meta.setViewMode("MULTI");
      meta.setIncludeResigned(true);
      meta.setDateFrom(ym.atDay(1).toString());
      meta.setDateTo(ym.atEndOfMonth().toString());
      
      // 3장: BRANCH / TEAM / PERSON (직전달 전체)
      byte[] branchPng = renderOne("지점별", "BRANCH", true, start, endExclusive, ym, meta);
      byte[] teamPng   = renderOne("팀별",   "TEAM",   true, start, endExclusive, ym, meta);
      byte[] personPng = renderOne("개인별", "PERSON", true, start, endExclusive, ym, meta);
      
      String capturedAt = now.format(DateTimeFormatter.ofPattern("yyyy.MM.dd (E) HH:mm"));
      String subject = "매출현황 월간 스냅샷 - " + ym; // 2026-02
      String scheduleTitle = ym.getYear() + "년 " + String.format("%02d", ym.getMonthValue()) + "월 전체 데이터 스냅샷";
      
      String plain =
          "매출현황 월간 스냅샷\n"
              + "캡처 일시: " + capturedAt + "\n"
              + scheduleTitle + "\n"
              + "조건\n"
              + "- 보기 모드: 지점/팀/개인 (첨부 3장)\n"
              + "- 퇴사자 포함: ON\n"
              + "- 기간: " + meta.getDateFrom() + " ~ " + meta.getDateTo() + "\n";
      
      int ok = 0;
      for (String to : emails) {
        try {
          mailService.sendSalesSnapshotBytes(
              to,
              subject,
              plain,
              capturedAt,
              scheduleTitle,
              meta,
              branchPng, "매출현황_지점별 (" + ym + ").png",
              teamPng,   "매출현황_팀별 (" + ym + ").png",
              personPng, "매출현황_개인별 (" + ym + ").png"
          );
          ok++;
        } catch (Exception e) {
          log.error("[SalesSnapshotBatchService] send fail to={}", to, e);
        }
      }
      
    } finally {
      // 정상/실패와 무관하게 in-progress 락은 해제 (프로세스 다운 대비 TTL도 있음)
      redisTemplate.delete(lockKey);
    }
  }
  
  private byte[] renderOne(
      String titlePrefix,
      String viewMode,
      boolean includeResigned,
      LocalDateTime start,
      LocalDateTime endExclusive,
      YearMonth ym,
      SalesSnapshotMeta meta
  ) {
    List<SalesAggRowDto> rows = salesMapper.findAggRows(
        viewMode,
        includeResigned,
        start.format(TS),
        endExclusive.format(TS)
    );
    List<Long> partitionStops = salesMapper.findPartitionStops(viewMode);
    List<String> partitionColors = salesMapper.findPartitionColors(viewMode);
    
    String pageTitle = ym + " " + titlePrefix + " 매출현황";
    String subtitle = "퇴사자 포함 " + (includeResigned ? "ON" : "OFF")
        + " · 기간 " + meta.getDateFrom() + " ~ " + meta.getDateTo();
    
    return dashboardRenderer.renderPng(pageTitle, subtitle, rows, partitionStops, partitionColors);
  }
}