package com.blue.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class MailAttachment {
  private final String filename;
  private final String contentType;
  private final byte[] bytes;
}