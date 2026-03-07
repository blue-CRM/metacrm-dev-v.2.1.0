package com.blue.customer.revoke.dto;

import lombok.Data;
import java.util.List;

@Data
public class TransferReq {
  private List<Long> customerIds;
  private Long targetCenterId;
  private Long targetUserId;
  private Boolean assignToManagerAsStaff;
}