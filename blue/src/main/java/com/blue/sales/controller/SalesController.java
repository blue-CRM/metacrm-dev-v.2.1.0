package com.blue.sales.controller;

import com.blue.sales.dto.SalesDashboardReq;
import com.blue.sales.dto.SalesDashboardRes;
import com.blue.sales.dto.SalesSnapshotMeta;
import com.blue.sales.service.SalesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/work/sales")
public class SalesController {
  
  private final SalesService service;
  
  @GetMapping("/dashboard")
  public ResponseEntity<SalesDashboardRes> getDashboard(
      Authentication auth,
      @RequestParam(defaultValue = "BRANCH") String viewMode,
      @RequestParam(defaultValue = "false") boolean includeResigned,
      @RequestParam(required = false) String dateFrom,
      @RequestParam(required = false) String dateTo
  ) {
    SalesDashboardReq req = SalesDashboardReq.builder()
        .viewMode(viewMode)
        .includeResigned(includeResigned)
        .dateFrom(dateFrom)
        .dateTo(dateTo)
        .build();
    
    return ResponseEntity.ok(service.getDashboard(auth.getName(), req));
  }
  
  @PostMapping(value = "/snapshot", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<Void> snapshot(
      Authentication auth,
      @RequestPart("meta") SalesSnapshotMeta meta,
      @RequestPart("month") MultipartFile month,
      @RequestPart("range") MultipartFile range,
      @RequestPart("all") MultipartFile all
  ) {
    service.sendSnapshotEmail(auth.getName(), meta, month, range, all);
    return ResponseEntity.ok().build();
  }
}