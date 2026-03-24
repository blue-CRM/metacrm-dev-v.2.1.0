package com.blue.customer.revoke.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor
public class UserContextDto {
  private Long userId;
  private String role;
  private Long centerId;
  private Long expertId;
  private String email;
  private String visible;
  private Boolean canAllocate;
  private String isSuper;
}
