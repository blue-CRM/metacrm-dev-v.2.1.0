package com.blue.customer.common.sheets.service;

import com.blue.customer.common.sheets.client.GoogleSheetsClient;
import com.blue.customer.common.sheets.dto.CustomerUpsertDto;
import com.blue.customer.common.sheets.dto.DuplicateUpsertDto;
import com.blue.customer.common.sheets.dto.GsheetSourceDto;
import com.blue.customer.common.sheets.mapper.SyncMapper;
import com.blue.customer.common.memo.service.MemoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class SheetsSyncService {
  
  private final GoogleSheetsClient sheetsClient;
  private final SyncMapper mapper;
  private final StringRedisTemplate redis;
  private final MemoService memoService;
  
  // ===== 운영 파라미터 =====
  private static final int BATCH_SIZE = 200;
  private static final String END_COL = "J"; // A~J
  private static final int MANUAL_DEBOUNCE_SEC = 1; // 수동 1초
  private static final int MANUAL_MAX_PER_MIN = 59; // 수동 분당 상한
  private static final ZoneId Z_SEOUL = ZoneId.of("Asia/Seoul");
  private static final LocalTime MAINT_START = LocalTime.of(0, 0); // [00:00
  private static final LocalTime MAINT_END   = LocalTime.of(1, 0); //  ,01:00)
  
//  // 유지보수시간 임시 확장
//  private static final LocalTime MAINT_START = LocalTime.of(0, 0); // [00:00
//  private static final LocalTime MAINT_END   = LocalTime.of(5, 0); //  ,05:00)
  
  public record SyncResult(boolean executed, int rows, String reason) {}
  
  // ====== 락 키 ======
  private static String minuteBucketKey(long sid) {
    var bucket = ZonedDateTime.now(Z_SEOUL)
        .format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
    return "gsync:auto:lock:" + sid + ":" + bucket;
  }
  private static String jobLockKey(long sid) { return "gsync:job:" + sid; }
  
  // ====== 유지보수 시간 확인 함수 ======
  private boolean inMaintenanceWindow() {
    LocalTime now = LocalTime.now(Z_SEOUL);
    return !now.isBefore(MAINT_START) && now.isBefore(MAINT_END);
  }
  
  // ====== 수동 ======
  @Transactional
  public SyncResult manualRefresh(long sourceId) {
    // 아래줄 제거시 유지보수 시간대에도 새로고침 가능
    if (inMaintenanceWindow()) return new SyncResult(false, 0, "maintenance_window");
    
    String rateKey = "gsync:rate:" + sourceId;
    Long c = redis.opsForValue().increment(rateKey);
    if (c != null && c == 1L) redis.expire(rateKey, Duration.ofSeconds(60));
    if (c != null && c > MANUAL_MAX_PER_MIN) return new SyncResult(false, 0, "rate_guard");
    
    String debounceKey = "gsync:lock:" + sourceId;
    boolean passed = Boolean.TRUE.equals(
        redis.opsForValue().setIfAbsent(debounceKey, "1", Duration.ofSeconds(MANUAL_DEBOUNCE_SEC)));
    if (!passed) return new SyncResult(false, 0, "debounced");
    
    String jobKey = jobLockKey(sourceId);
    boolean job = Boolean.TRUE.equals(
        redis.opsForValue().setIfAbsent(jobKey, "manual", Duration.ofSeconds(30)));
    if (!job) return new SyncResult(false, 0, "busy");
    
    try {
      return doIncrementalSync(sourceId, "manual");
    } finally {
      redis.delete(jobKey);
    }
  }
  
  // ====== 자동 ======
  @Transactional
  public SyncResult autoRefresh(long sourceId) {
    if (inMaintenanceWindow()) return new SyncResult(false, 0, "maintenance_window");
    
    String minuteKey = minuteBucketKey(sourceId);
    boolean firstThisMinute = Boolean.TRUE.equals(
        redis.opsForValue().setIfAbsent(minuteKey, "1", Duration.ofSeconds(120)));
    if (!firstThisMinute) return new SyncResult(false, 0, "auto_locked");
    
    String jobKey = jobLockKey(sourceId);
    boolean job = Boolean.TRUE.equals(
        redis.opsForValue().setIfAbsent(jobKey, "auto", Duration.ofSeconds(30)));
    if (!job) return new SyncResult(false, 0, "busy");
    
    try {
      return doIncrementalSync(sourceId, "auto");
    } finally {
      redis.delete(jobKey);
    }
  }
  
  // ====== 01:00 재개 ======
  @Transactional
  public SyncResult nightlyResume(long sourceId, int lookback) {
    if (inMaintenanceWindow()) return new SyncResult(false, 0, "maintenance_window");
    
    GsheetSourceDto src = mapper.findSourceById(sourceId);
    if (src == null) return new SyncResult(false, 0, "no_source");
    int cursor = Optional.ofNullable(src.getCursorRow()).orElse(1);
    
    String jobKey = jobLockKey(sourceId);
    boolean job = Boolean.TRUE.equals(
        redis.opsForValue().setIfAbsent(jobKey, "nightly", Duration.ofSeconds(300)));
    if (!job) return new SyncResult(false, 0, "busy");
    
    try {
      int total = drainFromCursor(sourceId);   // 커서가 1이든 아니든 새벽 1시에는 항상 드레인
      return new SyncResult(true, total, "nightly_drain_ok");
    } finally {
      redis.delete(jobKey);
    }
  }

// 룩백 비활성화
//  // 1) 룩백 경로만 lookback 전용 persist 호출
//  protected int recheckLookback(long sourceId, int lookback) {
//    GsheetSourceDto src = mapper.findSourceById(sourceId);
//    if (src == null) return 0;
//
//    int cursor = Optional.ofNullable(src.getCursorRow()).orElse(1);
//    int start = Math.max(2, cursor - lookback + 1);
//    if (start > cursor) return 0;
//
//    String range = src.getSheetName() + "!A" + start + ":" + END_COL + cursor;
//    List<List<Object>> raw = fetchWithRetry(src.getSpreadsheetId(), range);
//    if (raw == null || raw.isEmpty()) return 0;
//
//    List<CustomerUpsertDto> parsed = mapCompleteRows(raw);
//
//    int n = persistRows30dLookback(parsed);
//    log.info("[NightlyRecheck] sid={} start={} end={} parsed={} persisted={}",
//        sourceId, start, cursor, parsed.size(), n);
//    return n;
//  }
//
//  // 2) 룩백 전용 persist: 존재하면 건너뛴다
//  protected int persistRows30dLookback(List<CustomerUpsertDto> rows) {
//    int n = 0;
//    for (CustomerUpsertDto row : rows) {
//      LocalDateTime createdAt = Optional.ofNullable(row.getCustomerCreatedAt()).orElse(LocalDateTime.now());
//      LocalDateTime threshold = createdAt.minusDays(30);
//
//      String phoneDigits = digitsOnly(row.getCustomerPhone());
//
//      // 1) 본체 이벤트(같은 번호/시각/출처)가 이미 있으면 무조건 스킵 (동일 이벤트 재처리 방지)
//      if (mapper.existsCustomerEvent(phoneDigits, createdAt, row.getCustomerSource())) {
//        log.debug("[LookbackSkip] base event already exists phone={} at={} src={}", phoneDigits, createdAt, row.getCustomerSource());
//        continue;
//      }
//
//      // 2) 30일 이내 본체 존재 여부 확인
//      Long recentBaseId = mapper.findBaseCustomerIdWithinDays(phoneDigits, threshold, createdAt);
//
//      if (recentBaseId != null) {
//        // 2-1) 이미 동일 duplicate 이벤트가 있으면 스킵 (중복 중복 방지)
//        if (mapper.existsDuplicateEvent(recentBaseId, createdAt, row.getCustomerSource())) {
//          log.debug("[LookbackSkip] duplicate exists cid={} at={} src={}", recentBaseId, createdAt, row.getCustomerSource());
//          continue;
//        }
//        // 2-2) 새로운 duplicate 삽입
//        DuplicateUpsertDto dup = new DuplicateUpsertDto();
//        dup.setCustomerId(recentBaseId);
//        dup.setDuplicateCreatedAt(createdAt);
//        dup.setDuplicateName(row.getCustomerName());
//        dup.setDuplicateMemo(row.getCustomerMemo());
//        dup.setDuplicateContent(row.getCustomerContent());
//        dup.setDuplicateSource(row.getCustomerSource());
//        mapper.insertDuplicateMinimal(dup);
//        n++;
//      } else {
//        // 3) 본체 신규 삽입
//        boolean existed = mapper.existsAnyBaseCustomer(phoneDigits);
//        row.setCustomerDivision(existed ? "유효" : "최초");
//        row.setCustomerStatus("없음");
//        row.setCustomerCategory("주식");
//        mapper.insertCustomerMinimal(row);
//        n++;
//      }
//    }
//    return n;
//  }
  
  protected int drainFromCursor(long sourceId) {
    int total = 0, calls = 0;
    while (true) {
      SyncResult r = doIncrementalSync(sourceId, "drain");
      if (!r.executed()) break;
      total += r.rows();
      calls++;
      if (r.rows() == 0) break;
      if (calls >= 59) break;   // 사용자당 60/min 보호 (자동 1회 고려)
      try { Thread.sleep(1100); } catch (InterruptedException ignored) {}
    }
    log.info("[Drain] sid={} calls={} totalRows={}", sourceId, calls, total);
    return total;
  }
  
  // ====== 증분 동기화 ======
  private SyncResult doIncrementalSync(long sourceId, String mode) {
    GsheetSourceDto src = mapper.findSourceById(sourceId);
    if (src == null) return new SyncResult(false, 0, "no_source");
    
    int start = (src.getCursorRow() == null ? 1 : src.getCursorRow()) + 1;
    String range = "'" + src.getSheetName() + "'!A" + start + ":" + END_COL + (start + BATCH_SIZE - 1);
    
    List<List<Object>> raw = fetchWithRetry(src.getSpreadsheetId(), range);
    if (raw == null || raw.isEmpty()) return new SyncResult(true, 0, "no_new");
    
    // 앞에서부터 '완전' 행만 연속 처리
    List<CustomerUpsertDto> toPersist = new ArrayList<>();
    int leadingComplete = 0;
    for (List<Object> r : raw) {
      if (!isCompleteRow(r)) break;
      toPersist.add(mapRow(r));
      leadingComplete++;
    }
    if (leadingComplete == 0) return new SyncResult(true, 0, "incomplete_head");
    
    int processed = persistRows30d(toPersist);
    
    // 커서는 '완전 연속 구간'만큼 전진
    int newCursor = start + leadingComplete - 1;
    mapper.advanceSourceCursor(sourceId, newCursor);
    log.info("[SheetsSync] {} processed={} advanced={} cursor {}->{}",
        mode, processed, leadingComplete, start - 1, newCursor);
    
    return new SyncResult(true, processed, "ok");
  }
  
  // ====== 시트 읽기 재시도 ======
  private List<List<Object>> fetchWithRetry(String spreadsheetId, String range) {
    final int MAX_TRIES = 3;
    int attempt = 0;
    while (true) {
      try {
        return sheetsClient.readRange(spreadsheetId, range);
      } catch (Exception e) {
        attempt++;
        if (attempt >= MAX_TRIES) {
          log.warn("[Sheets] readRange failed (last). range={} err={}", range, e.toString());
          return Collections.emptyList();
        }
        long backoff = (long) (Math.pow(2, attempt - 1) * 300);
        long jitter  = ThreadLocalRandom.current().nextLong(200, 600);
        try { Thread.sleep(backoff + jitter); } catch (InterruptedException ignored) {}
        log.debug("[Sheets] retry attempt={} range={}", attempt + 1, range);
      }
    }
  }
  
  // ====== 행 검증/매핑 ======
  private static final Locale KO = Locale.KOREAN;
  private static final DateTimeFormatter KOREAN_TS =
      new DateTimeFormatterBuilder()
          .parseCaseInsensitive()
          .appendPattern("yyyy. M. d ")
          .appendPattern("a ")       // 오전/오후
          .appendPattern("h:mm")
          .optionalStart().appendPattern(":ss").optionalEnd()
          .toFormatter(KO);
  
  private boolean isCompleteRow(List<Object> r) {
    // A(생성일)
    LocalDateTime at = parseCreatedAtKST(asStr(get(r, 0)));
    if (at == null) return false;
    
    // C(이름)
    String name = asStr(get(r, 2));
    if (name == null || name.isEmpty()) return false;
    
    // E(전화): 한국 포맷 하이픈 표준화 가능해야 함
    String phoneFmt = formatPhoneKR(asStr(get(r, 4)));
    if (phoneFmt == null) return false;
    
    // I(출처)
    String src = asStr(get(r, 8));
    if (src == null || src.isEmpty()) return false;
    
    return true;
  }
  
  private List<CustomerUpsertDto> mapCompleteRows(List<List<Object>> rows) {
    List<CustomerUpsertDto> out = new ArrayList<>();
    for (List<Object> r : rows) {
      if (!isCompleteRow(r)) continue;
      out.add(mapRow(r));
    }
    return out;
  }
  
  private CustomerUpsertDto mapRow(List<Object> r) {
    CustomerUpsertDto dto = new CustomerUpsertDto();
    
    // A : 생성일 -> customer_created_at
    LocalDateTime at = parseCreatedAtKST(asStr(get(r, 0)));
    if (at == null) at = LocalDateTime.now(Z_SEOUL);
    dto.setCustomerCreatedAt(at);
    
    // C : 이름 -> customer_name
    dto.setCustomerName(asStr(get(r, 2)));
    
    // E : 연락처 -> customer_phone
    dto.setCustomerPhone(formatPhoneKR(asStr(get(r, 4))));
    
    // F : 종목명 + B : 선택한 항목 -> customer_memo
    String memoF = asStr(get(r, 5)); // F (종목명)
    String valueB  = asStr(get(r, 1)); // B (선택한 항목)
    if (memoF != null && !memoF.isEmpty()) {
      memoF = "시청한 종목명 : " + memoF.trim();
    } else {
      memoF = "시청한 종목명 : X";
    }
    if (valueB != null && !valueB.isEmpty()) {
      valueB = "선택한 항목 : " + valueB.trim();
    } else {
      valueB = "선택한 항목 : X";
    }
    dto.setCustomerMemo(memoF + '\n' + valueB);
    
    // G : 하고싶은말(메모 아님) -> customer_content
    dto.setCustomerContent(asStr(get(r,6)));
    
    // I : 출처 -> customer_source
    dto.setCustomerSource(asStr(get(r, 8)));
    
    // J : 카테고리 -> customer_category (기본값 : 주식, 그 외 -> 입력된 그대로)
    String category = asStr(get(r, 9));
    dto.setCustomerCategory((category == null || category.isBlank()) ? "주식" : category);
    
    return dto;
  }
  
  private static LocalDateTime parseCreatedAtKST(String s) {
    if (s == null || s.trim().isEmpty()) return null;
    String t = s.trim();
    try { return LocalDateTime.parse(t, KOREAN_TS); } catch (Exception ignored) {}
    String[] patterns = {
        "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd", "yyyy.M.d. HH:mm:ss", "yyyy.M.d."
    };
    for (String p : patterns) {
      try { return LocalDateTime.parse(t, DateTimeFormatter.ofPattern(p, KO)); }
      catch (Exception ignored) {}
    }
    return null;
  }
  
  // 숫자만
  private static String digitsOnly(String s) {
    if (s == null) return "";
    return s.replaceAll("\\D", "");
  }
  
  /**
   * 한국 전화 포맷팅:
   * - 02: 9자리 => 02-XXX-XXXX, 10자리 => 02-XXXX-XXXX
   * - 0시작(010/011/070/050X 등): 10자리 => 3-3-4, 11자리 => 3-4-4
   * - 8자리 => 휴대전화로 간주, 010 붙여서 3-4-4
   * - 그 외는 null(불완전)
   */
  private static String formatPhoneKR(String raw) {
    if (raw == null) return null;
    String d = digitsOnly(raw);
    int len = d.length();
    if (len < 8) return null;
    
    // 서울번호
    if (d.startsWith("02")) {
      if (len == 9)  return "02-" + d.substring(2,5) + "-" + d.substring(5);
      if (len == 10) return "02-" + d.substring(2,6) + "-" + d.substring(6);
      return null;
    }
    
    // 일반 휴대폰/지역번호 (0으로 시작)
    if (d.startsWith("0")) {
      if (len == 11) return d.substring(0,3) + "-" + d.substring(3,7) + "-" + d.substring(7);
      if (len == 10) return d.substring(0,3) + "-" + d.substring(3,6) + "-" + d.substring(6);
      return null;
    }
    
    // 8자리 → 휴대전화로 간주하여 010 붙여서 처리
    if (len == 8) {
      return "010-" + d.substring(0,4) + "-" + d.substring(4);
    }
    
    return null;
  }
  
  // ====== DB 지속 ======
  protected int persistRows30d(List<CustomerUpsertDto> rows) {
    int n = 0;
    for (CustomerUpsertDto row : rows) {
      LocalDateTime createdAt = Optional.ofNullable(row.getCustomerCreatedAt()).orElse(LocalDateTime.now());
      LocalDateTime threshold = createdAt.minusDays(30);
      
      // 숫자만 키로 중복판정
      String phoneDigits = digitsOnly(row.getCustomerPhone());
      
      // 1) 완전 동일 "본체" 이벤트 스킵
      if (mapper.existsCustomerEvent(
          phoneDigits, createdAt, row.getCustomerSource(),
          row.getCustomerName(), row.getCustomerMemo(), row.getCustomerContent())) {
        log.debug("[Skip] base same-all-fields phone={} at={} src={}", phoneDigits, createdAt, row.getCustomerSource());
        continue;
      }
      
      Long recentBaseId = mapper.findBaseCustomerIdWithinDays(phoneDigits, threshold, createdAt);
      if (recentBaseId != null) {
        // 2) 같은 본체에 동일 duplicate 이벤트가 이미 있으면 스킵
        if (mapper.existsDuplicateEvent(
            recentBaseId, createdAt, row.getCustomerSource(),
            row.getCustomerName(), row.getCustomerMemo(), row.getCustomerContent())) {
          log.debug("[Skip] dup same-all-fields cid={} at={} src={}", recentBaseId, createdAt, row.getCustomerSource());
          continue;
        }
        
        DuplicateUpsertDto dup = new DuplicateUpsertDto();
        dup.setCustomerId(recentBaseId);
        dup.setDuplicateCreatedAt(createdAt);
        dup.setDuplicateName(row.getCustomerName());
        dup.setDuplicateMemo(row.getCustomerMemo());
        dup.setDuplicateContent(row.getCustomerContent());
        dup.setDuplicateSource(row.getCustomerSource());
        dup.setDuplicateCategory(row.getCustomerCategory());
        dup.setDuplicateDisplay(1); // 기본값 명시
        mapper.insertDuplicateMinimal(dup);
        n++;
      } else {
        boolean existed = mapper.existsAnyBaseCustomer(phoneDigits);
        row.setCustomerDivision(existed ? "유효" : "최초");
        row.setCustomerStatus("없음");
        row.setCustomerCategory(row.getCustomerCategory());
        mapper.insertCustomerMinimal(row); // useGeneratedKeys → row.customerId 채워짐

        // [메모 채팅화] 신규 고객 최초 SYSTEM 채팅(종목명) = customer_memo 와 동일 내용.
        // ⚠️ 전환기 이중기록: customer_memo 도 그대로 둠(Revoke/Center 텍스트 셀 유지). 같은 TX.
        memoService.addSystemChat(row.getCustomerId(), row.getCustomerMemo());
        n++;
      }
    }
    return n;
  }
  
  // ====== helpers ======
  private static Object get(List<Object> r, int i){ return i<r.size()? r.get(i): null; }
  private static String asStr(Object o){ return o==null? null: String.valueOf(o).trim(); }
  
// B열 백필 비활성화
//  // ===== B열 → memo 백필 전용 (임시/원샷) =====
//  public record BBackfillResult(
//      int scanned, int withB, int updated, int unmatched, int skippedInvalid,
//      List<String> unmatchedRows // 백필 실패한 데이터를 보관할 리스트
//  ) {}
//
//  @Transactional
//  public BBackfillResult backfillBIntoMemoOnce(long sourceId) {
//    // 1) 소스/커서 조회 (커서까지만)
//    GsheetSourceDto src = mapper.findSourceById(sourceId);
//    if (src == null) {
//      log.warn("[B-Backfill] no source id={}", sourceId);
//      return new BBackfillResult(0,0,0,0,0, Collections.emptyList());
//    }
//    final int endRow = Optional.ofNullable(src.getCursorRow()).orElse(1);
//    if (endRow < 2) return new BBackfillResult(0,0,0,0,0, Collections.emptyList());
//
//    int scanned = 0, withB = 0, updated = 0, unmatched = 0, skippedInvalid = 0;
//    List<String> unmatchedRows = new ArrayList<>();
//
//    // 2) 2행~cursor까지 A~I 범위를 batch로 읽음 (A:생성일, B:백필값, E:전화)
//    for (int start = 2; start <= endRow; start += BATCH_SIZE) {
//      int last = Math.min(endRow, start + BATCH_SIZE - 1);
//      String range = "'" + src.getSheetName() + "'!A" + start + ":" + END_COL + last;
//
//      List<List<Object>> raw = fetchWithRetry(src.getSpreadsheetId(), range);
//      if (raw == null || raw.isEmpty()) continue;
//
//      for (List<Object> r : raw) {
//        scanned++;
//
//        LocalDateTime createdAt = parseCreatedAtKST(asStr(get(r, 0))); // A
//        String bValue           = asStr(get(r, 1));                    // B
//        String phoneFmt         = formatPhoneKR(asStr(get(r, 4)));     // E
//
//        if (createdAt == null || phoneFmt == null) { skippedInvalid++; continue; }
//        if (bValue == null || bValue.isEmpty()) continue;
//        withB++;
//
//        String line = "선택한 항목 : " + bValue;
//
//        // 3) 메모만 업데이트 (기존 메모 유지 + 줄바꿈 추가)
//        int n = mapper.appendMemoByPhoneAt(phoneFmt, createdAt, line);
//        if (n > 0) {
//          // base(customers)에서 매칭됨
//          updated++;
//        } else {
//          // base에 없으면 duplicate(customers_duplicate) 쪽도 시도
//          int m = mapper.appendDuplicateMemoByPhoneAt(phoneFmt, createdAt, line);
//          if (m > 0) {
//            updated++;   // 이 시트 행은 성공 처리로 카운트
//          } else {
//            unmatched++; // 둘 다 못 찾음
//
//            // 구글 스프레드 시트 기준 - 매칭되지 못한 데이터 로깅
//            unmatchedRows.add(String.format(
//                "날짜=%s, 전화=%s, 선택항목=%s, 이름=%s, 출처=%s",
//                createdAt, phoneFmt, bValue, asStr(get(r, 2)), asStr(get(r, 8))
//            ));
//          }
//        }
//      }
//    }
//
//    log.info("[B-Backfill] sid={} scanned={} withB={} updated={} unmatched={} skippedInvalid={}",
//        sourceId, scanned, withB, updated, unmatched, skippedInvalid);
//    if (!unmatchedRows.isEmpty()) {
//      log.warn("[B-Backfill] unmatched list ↓↓↓");
//      unmatchedRows.forEach(v -> log.warn("  {}", v));
//    }
//    return new BBackfillResult(scanned, withB, updated, unmatched, skippedInvalid, unmatchedRows);
//  }
}
