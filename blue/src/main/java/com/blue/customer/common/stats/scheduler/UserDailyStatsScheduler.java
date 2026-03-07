package com.blue.customer.common.stats.scheduler;

import com.blue.customer.common.stats.service.UserDailyStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserDailyStatsScheduler {
  
  private final UserDailyStatsService statsService;
  
  // 매일 새벽 1시 (KST)
  @Scheduled(cron = "0 1 * * * *", zone = "Asia/Seoul")
  public void runDailyIncrementalJob() {
    log.info("[UserDailyStatsScheduler] user_daily_stats incremental batch start");
    statsService.applyIncrementalFromAllocLogs();
    log.info("[UserDailyStatsScheduler] user_daily_stats incremental batch end");
  }
}