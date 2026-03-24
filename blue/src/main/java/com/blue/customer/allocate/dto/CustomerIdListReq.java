package com.blue.customer.allocate.dto;

import lombok.Data;
import java.util.List;

@Data
public class CustomerIdListReq {
  private List<Long> customerIds;
}