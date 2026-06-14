package com.blue.user.dto;

import lombok.Data;

@Data
public class UserSelectDto {
  private Long userId;
  private String userName;
  private String userPhone;
  private String userEmail;
  private String userRole;
  private String userApproved;
  private String centerName;
  private String managerPhoneAccess;
  private String canAllocate;
  private String expertName;
  private String userCreatedAt;
}