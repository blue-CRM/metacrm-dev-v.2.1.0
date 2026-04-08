package com.blue.customer.revoke.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RevokeListRowDto {
  private Long id;
  private String name;
  private String phone;
  private String staff;
  private String division;
  private String category;
  private String status;
  private String prevStatus;
  private String centerName;
  private Long centerId;
  private String source;
  private String content;
  private String memo;
  
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
  private LocalDateTime createdAt;
}
