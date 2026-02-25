package com.blue.sales.controller;

import com.blue.sales.dto.SalesDashboardReq;
import com.blue.sales.dto.SalesDashboardRes;
import com.blue.sales.service.SalesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

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
}