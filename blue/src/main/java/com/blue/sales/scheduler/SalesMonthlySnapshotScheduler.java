package com.blue.sales.scheduler;

import com.blue.sales.service.SalesSnapshotBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SalesMonthlySnapshotScheduler {
  
  private final SalesSnapshotBatchService batchService;
  
  // 매월 1일 새벽 1시 (KST)
  // @Scheduled(cron = "0 1 * * * *", zone = "Asia/Seoul")
  @Scheduled(cron = "0 0 1 1 * *", zone = "Asia/Seoul")
  public void runMonthlySnapshot() {
    log.info("[SalesMonthlySnapshotScheduler] start");
    batchService.sendPrevMonthSnapshotToAdmins();
    log.info("[SalesMonthlySnapshotScheduler] end");
  }
}