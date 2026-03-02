package com.blue.sales.service;

import com.blue.auth.dto.MailAttachment;
import com.blue.auth.service.AsyncMailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SalesMailService {
  
  private final AsyncMailService asyncMailService;
  
  public void sendSalesSnapshot(
      String toEmail,
      String subject,
      String plainText,
      MultipartFile month,
      MultipartFile range,
      MultipartFile all
  ) {
    List<MailAttachment> attachments = new ArrayList<>(3);
    add(attachments, month, "sales-month.png");
    add(attachments, range, "sales-range.png");
    add(attachments, all, "sales-all.png");
    
    String html = buildSimpleHtml(subject, plainText);
    
    asyncMailService.sendHtmlWithLogoAndAttachments(
        toEmail,
        subject,
        html,
        plainText,
        attachments
    );
  }
  
  private void add(List<MailAttachment> list, MultipartFile f, String fallbackName) {
    try {
      if (f == null || f.isEmpty()) return;
      
      String filename = (f.getOriginalFilename() == null || f.getOriginalFilename().isBlank())
          ? fallbackName
          : f.getOriginalFilename();
      
      String contentType = (f.getContentType() == null || f.getContentType().isBlank())
          ? "image/png"
          : f.getContentType();
      
      list.add(MailAttachment.builder()
          .filename(filename)
          .contentType(contentType)
          .bytes(f.getBytes())
          .build());
      
    } catch (Exception e) {
      throw new RuntimeException("스냅샷 첨부 처리 실패: " + fallbackName, e);
    }
  }
  
  private String buildSimpleHtml(String title, String plainText) {
    String safe = (plainText == null) ? "" : plainText.replace("\n", "<br/>");
    return """
      <!doctype html>
      <html lang="ko">
      <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"></head>
      <body style="margin:0;padding:0;background:#F5F7FB;">
        <div style="width:100%%;padding:24px 16px;">
          <table role="presentation" cellspacing="0" cellpadding="0" border="0" align="center" width="100%%"
                 style="max-width:640px;margin:0 auto;background:#fff;border-radius:14px;overflow:hidden;box-shadow:0 8px 24px rgba(16,24,40,0.06);">
            <tr>
              <td style="padding:22px 22px 12px;text-align:center;">
                <img src="cid:logo" alt="MARK CRM" style="display:block;margin:0 auto 10px;height:56px;width:auto;">
                <div style="font-family:Segoe UI,Apple SD Gothic Neo,Noto Sans KR,Arial,sans-serif;font-size:16px;font-weight:700;color:#0F172A;">%s</div>
              </td>
            </tr>
            <tr>
              <td style="padding:0 22px 22px;">
                <div style="font-family:Segoe UI,Apple SD Gothic Neo,Noto Sans KR,Arial,sans-serif;font-size:13px;line-height:1.7;color:#334155;">
                  %s
                </div>
                <div style="margin-top:14px;font-family:Segoe UI,Apple SD Gothic Neo,Noto Sans KR,Arial,sans-serif;font-size:11px;color:#94A3B8;">
                  첨부된 스냅샷 이미지를 확인해 주세요.
                </div>
              </td>
            </tr>
          </table>
        </div>
      </body>
      </html>
    """.formatted(title, safe);
  }
}