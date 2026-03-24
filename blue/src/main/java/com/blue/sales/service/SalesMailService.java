//package com.blue.sales.service;
//
//import com.blue.auth.dto.MailAttachment;
//import com.blue.auth.service.AsyncMailService;
//import com.blue.sales.dto.SalesSnapshotMeta;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.util.ArrayList;
//import java.util.List;
//
//@Service
//@RequiredArgsConstructor
//public class SalesMailService {
//
//  private final AsyncMailService asyncMailService;
//
//  private static final String BRAND = "#2563EB";
//  private static final String BG = "#F5F7FB";
//  private static final String CARD_BORDER = "#E8EEF7";
//  private static final String TEXT = "#0F172A";
//  private static final String MUTED = "#64748B";
//  private static final String SOFT = "#EEF2F7";
//
//  public void sendSalesSnapshotBytes(
//      String toEmail,
//      String subject,
//      String plainText,
//      String capturedAtStr,
//      String scheduleTitle,
//      SalesSnapshotMeta meta,
//      byte[] img1, String name1,
//      byte[] img2, String name2,
//      byte[] img3, String name3
//  ) {
//    List<MailAttachment> attachments = new ArrayList<>(3);
//
//    attachments.add(MailAttachment.builder()
//        .filename(name1).contentType("image/png").bytes(img1).build());
//    attachments.add(MailAttachment.builder()
//        .filename(name2).contentType("image/png").bytes(img2).build());
//    attachments.add(MailAttachment.builder()
//        .filename(name3).contentType("image/png").bytes(img3).build());
//
//    String html = buildSnapshotHtmlFromNames(subject, capturedAtStr, scheduleTitle, meta);
//
//    asyncMailService.sendHtmlWithLogoAndAttachments(toEmail, subject, html, plainText, attachments);
//  }
//
//  public void sendSalesSnapshot(
//      String toEmail,
//      String subject,
//      String plainText,
//      String capturedAtStr,
//      String scheduleTitle,
//      SalesSnapshotMeta meta,
//      MultipartFile month,
//      MultipartFile range,
//      MultipartFile all
//  ) {
//    List<MailAttachment> attachments = new ArrayList<>(3);
//    add(attachments, month, "매출현황_이번달.png");
//    add(attachments, range, "매출현황_기간별.png");
//    add(attachments, all, "매출현황_전체.png");
//
//    String html = buildSnapshotHtml(subject, capturedAtStr, scheduleTitle, meta);
//
//    asyncMailService.sendHtmlWithLogoAndAttachments(
//        toEmail,
//        subject,
//        html,
//        plainText,
//        attachments
//    );
//  }
//
//  private void add(List<MailAttachment> list, MultipartFile f, String fallbackName) {
//    try {
//      if (f == null || f.isEmpty()) return;
//
//      String filename = (f.getOriginalFilename() == null || f.getOriginalFilename().isBlank())
//          ? fallbackName
//          : f.getOriginalFilename();
//
//      String contentType = (f.getContentType() == null || f.getContentType().isBlank())
//          ? "image/png"
//          : f.getContentType();
//
//      list.add(MailAttachment.builder()
//          .filename(filename)
//          .contentType(contentType)
//          .bytes(f.getBytes())
//          .build());
//
//    } catch (Exception e) {
//      throw new RuntimeException("스냅샷 첨부 처리 실패: " + fallbackName, e);
//    }
//  }
//
//  private String buildSnapshotHtml(
//      String subject,
//      String capturedAtStr,
//      String scheduleTitle,
//      SalesSnapshotMeta meta
//  ) {
//    String modeRaw = nvl(meta != null ? meta.getViewMode() : "");
//    String modeLabel = viewModeLabel(modeRaw);
//    String resignedText = (meta != null && Boolean.TRUE.equals(meta.getIncludeResigned())) ? "ON" : "OFF";
//    String rangeText = fmtRange(meta != null ? meta.getDateFrom() : null, meta != null ? meta.getDateTo() : null);
//
//    boolean isScheduled = (scheduleTitle != null && !scheduleTitle.isBlank());
//    String badgeText = isScheduled ? "스케줄" : "수동";
//    String headline = isScheduled ? scheduleTitle : "매출현황 스냅샷이 생성되었습니다.";
//
//    String preheader = "이번달, 기간내, 전체기간 스냅샷 이미지가 첨부되었습니다.";
//
//    // 값은 전부 escape
//    String escSubject = escapeHtml(nvl(subject));
//    String escBadge = escapeHtml(badgeText);
//    String escHeadline = escapeHtml(headline);
//    String escCaptured = escapeHtml(nvl(capturedAtStr));
//
//    String escModeLabel = escapeHtml(modeLabel);
//    String escResigned = escapeHtml(resignedText);
//    String escRange = escapeHtml(rangeText);
//
//    return """
//<!doctype html>
//<html lang="ko">
//<head>
//  <meta charset="UTF-8">
//  <title>%1$s</title>
//  <meta name="viewport" content="width=device-width,initial-scale=1">
//</head>
//
//<body style="margin:0;padding:0;background:%2$s;">
//  <!-- preheader (메일 목록 미리보기용) -->
//  <div style="display:none;max-height:0;overflow:hidden;opacity:0;color:transparent;">
//    %3$s
//  </div>
//
//  <div style="width:100%%;padding:32px 16px;">
//    <table role="presentation" cellspacing="0" cellpadding="0" border="0" align="center" width="100%%"
//           style="max-width:640px;margin:0 auto;background:#ffffff;border-radius:16px;overflow:hidden;
//                  box-shadow:0 8px 24px rgba(16,24,40,0.06);">
//
//      <!-- Header -->
//      <tr>
//        <td style="padding:28px 28px 12px;text-align:center;">
//          <img src="cid:logo" alt="MARK CRM" style="display:block;margin:0 auto 10px;height:70px;width:auto;">
//          <span style="display:inline-block;margin-top:2px;padding:6px 12px;border-radius:999px;
//                       background:rgba(37,99,235,0.10);color:%4$s;
//                       font-family:Segoe UI,Roboto,Apple SD Gothic Neo,Noto Sans KR,Arial,sans-serif;
//                       font-size:12px;">%5$s</span>
//        </td>
//      </tr>
//
//      <!-- Title -->
//      <tr>
//        <td style="padding:6px 28px 0;text-align:center;">
//          <h1 style="margin:0 0 8px;font-family:Segoe UI,Roboto,Apple SD Gothic Neo,Noto Sans KR,Arial,sans-serif;
//                     font-size:22px;line-height:1.35;color:%6$s;">%7$s</h1>
//          <p style="margin:0 0 2px;font-family:Segoe UI,Roboto,Apple SD Gothic Neo,Noto Sans KR,Arial,sans-serif;
//                    font-size:13px;line-height:1.7;color:%8$s;">
//            캡처 일시: <strong style="color:%6$s;">%9$s</strong>
//          </p>
//          <p style="margin:0 0 16px;font-family:Segoe UI,Roboto,Apple SD Gothic Neo,Noto Sans KR,Arial,sans-serif;
//                    font-size:12px;line-height:1.7;color:%8$s;">
//            첨부된 이미지 파일에서 스냅샷을 확인하세요.
//          </p>
//        </td>
//      </tr>
//
//      <!-- Conditions Card -->
//      <tr>
//       <td style="padding:0 28px 10px;">
//         <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%%"
//                style="background:#ffffff;border:1px solid %10$s;border-radius:12px;overflow:hidden;">
//           <tr>
//             <td style="padding:14px 16px;border-bottom:1px solid %11$s;">
//               <div style="font-family:Segoe UI,Roboto,Apple SD Gothic Neo,Noto Sans KR,Arial,sans-serif;
//                           font-size:12px;color:%8$s;letter-spacing:.2px;">
//                 조건
//               </div>
//             </td>
//           </tr>
//
//           %12$s
//
//         </table>
//       </td>
//     </tr>
//
//     <!-- Footer는 별도 행 -->
//     <tr>
//       <td align="center" style="padding:18px 12px 28px;text-align:center;">
//         <div style="font-family:Segoe UI,Roboto,Apple SD Gothic Neo,Noto Sans KR,Arial,sans-serif;
//                     font-size:11px;color:#94A3B8;">
//           © 마크CRM. All rights reserved.
//         </div>
//       </td>
//     </tr>
//
//    </table>
//  </div>
//</body>
//</html>
//""".formatted(
//        escSubject,                 // 1
//        BG,                         // 2
//        escapeHtml(preheader),      // 3
//        BRAND,                      // 4
//        escBadge,                   // 5
//        TEXT,                       // 6
//        escHeadline,                // 7
//        MUTED,                      // 8
//        escCaptured,                // 9
//        CARD_BORDER,                // 10
//        SOFT,                       // 11
//        buildConditionRowsTable(escModeLabel, escResigned, escRange)     // 12
//    );
//  }
//
//  // 조건 영역: table row들만 반환 (이메일 호환성 때문에 flex 금지)
//  private String buildConditionRowsTable(String modeLabel, String resigned, String range) {
//    return """
//      <tr>
//        <td style="padding:0 16px 14px;">
//          <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%%"
//                 style="border-collapse:collapse;">
//            %s
//            %s
//            %s
//          </table>
//        </td>
//      </tr>
//    """.formatted(
//        kvRow("집계 기준", modeLabel),
//        kvRow("퇴사자 포함", "ON".equals(resigned) ? "포함" : "미포함"),
//        kvRow("기간", range)
//    );
//  }
//
//  private String kvRow(String k, String v) {
//    return """
//      <tr>
//        <td style="padding:10px 0;border-bottom:1px solid %1$s;">
//          <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%%"
//                 style="border-collapse:collapse;">
//            <tr>
//              <td width="110" style="vertical-align:top;font-family:Segoe UI,Roboto,Apple SD Gothic Neo,Noto Sans KR,Arial,sans-serif;
//                                     font-size:12px;color:%2$s;padding-right:12px;">
//                %3$s
//              </td>
//              <td style="vertical-align:top;font-family:Segoe UI,Roboto,Apple SD Gothic Neo,Noto Sans KR,Arial,sans-serif;
//                         font-size:12px;color:%4$s;">
//                %5$s
//              </td>
//            </tr>
//          </table>
//        </td>
//      </tr>
//    """.formatted(
//        SOFT,
//        "#475569",
//        escapeHtml(k),
//        "#111827",
//        escapeHtml(v)
//    );
//  }
//
//  private String buildSnapshotHtmlFromNames(
//      String subject,
//      String capturedAtStr,
//      String scheduleTitle,
//      SalesSnapshotMeta meta
//  ) {
//    String preheader = "지점별, 팀별, 개인별 월간 스냅샷 이미지가 첨부되었습니다.";
//    return buildSnapshotHtmlCommon(
//        subject,
//        capturedAtStr,
//        scheduleTitle,
//        meta,
//        preheader
//    );
//  }
//
//  private String buildSnapshotHtmlCommon(
//      String subject,
//      String capturedAtStr,
//      String scheduleTitle,
//      SalesSnapshotMeta meta,
//      String preheader
//  ) {
//    String modeRaw = nvl(meta != null ? meta.getViewMode() : "");
//    String modeLabel = viewModeLabel(modeRaw);
//    String resignedText = (meta != null && Boolean.TRUE.equals(meta.getIncludeResigned())) ? "ON" : "OFF";
//    String rangeText = fmtRange(meta != null ? meta.getDateFrom() : null, meta != null ? meta.getDateTo() : null);
//
//    boolean isScheduled = (scheduleTitle != null && !scheduleTitle.isBlank());
//    String badgeText = isScheduled ? "스케줄" : "수동";
//    String headline = isScheduled ? scheduleTitle : "매출현황 스냅샷이 생성되었습니다.";
//
//    String escSubject = escapeHtml(nvl(subject));
//    String escBadge = escapeHtml(badgeText);
//    String escHeadline = escapeHtml(headline);
//    String escCaptured = escapeHtml(nvl(capturedAtStr));
//
//    return """
//<!doctype html>
//<html lang="ko">
//<head>
//  <meta charset="UTF-8">
//  <title>%1$s</title>
//  <meta name="viewport" content="width=device-width,initial-scale=1">
//</head>
//
//<body style="margin:0;padding:0;background:%2$s;">
//  <div style="display:none;max-height:0;overflow:hidden;opacity:0;color:transparent;">
//    %3$s
//  </div>
//
//  <div style="width:100%%;padding:32px 16px;">
//    <table role="presentation" cellspacing="0" cellpadding="0" border="0" align="center" width="100%%"
//           style="max-width:640px;margin:0 auto;background:#ffffff;border-radius:16px;overflow:hidden;
//                  box-shadow:0 8px 24px rgba(16,24,40,0.06);">
//
//      <tr>
//        <td style="padding:28px 28px 12px;text-align:center;">
//          <img src="cid:logo" alt="MARK CRM" style="display:block;margin:0 auto 10px;height:70px;width:auto;">
//          <span style="display:inline-block;margin-top:2px;padding:6px 12px;border-radius:999px;
//                       background:rgba(37,99,235,0.10);color:%4$s;
//                       font-family:Segoe UI,Roboto,Apple SD Gothic Neo,Noto Sans KR,Arial,sans-serif;
//                       font-size:12px;">%5$s</span>
//        </td>
//      </tr>
//
//      <tr>
//        <td style="padding:6px 28px 0;text-align:center;">
//          <h1 style="margin:0 0 8px;font-family:Segoe UI,Roboto,Apple SD Gothic Neo,Noto Sans KR,Arial,sans-serif;
//                     font-size:22px;line-height:1.35;color:%6$s;">%7$s</h1>
//          <p style="margin:0 0 2px;font-family:Segoe UI,Roboto,Apple SD Gothic Neo,Noto Sans KR,Arial,sans-serif;
//                    font-size:13px;line-height:1.7;color:%8$s;">
//            캡처 일시: <strong style="color:%6$s;">%9$s</strong>
//          </p>
//          <p style="margin:0 0 16px;font-family:Segoe UI,Roboto,Apple SD Gothic Neo,Noto Sans KR,Arial,sans-serif;
//                    font-size:12px;line-height:1.7;color:%8$s;">
//            첨부된 이미지 파일에서 스냅샷을 확인하세요.
//          </p>
//        </td>
//      </tr>
//
//      <tr>
//       <td style="padding:0 28px 10px;">
//         <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%%"
//                style="background:#ffffff;border:1px solid %10$s;border-radius:12px;overflow:hidden;">
//
//           %12$s
//
//         </table>
//       </td>
//     </tr>
//
//     <tr>
//       <td align="center" style="padding:18px 12px 28px;text-align:center;">
//         <div style="font-family:Segoe UI,Roboto,Apple SD Gothic Neo,Noto Sans KR,Arial,sans-serif;
//                     font-size:11px;color:#94A3B8;">
//           © 마크CRM. All rights reserved.
//         </div>
//       </td>
//     </tr>
//
//    </table>
//  </div>
//</body>
//</html>
//""".formatted(
//        escSubject,                 // 1
//        BG,                         // 2
//        escapeHtml(preheader),      // 3
//        BRAND,                      // 4
//        escBadge,                   // 5
//        TEXT,                       // 6
//        escHeadline,                // 7
//        MUTED,                      // 8
//        escCaptured,                // 9
//        CARD_BORDER,                // 10
//        SOFT,                       // 11
//        buildConditionRowsTable(modeLabel, resignedText, rangeText) // 12 (raw 넣고 내부에서 escape)
//    );
//  }
//
//  private String buildAttachmentCardTd(String title, String state, String filename, boolean attached) {
//    String stateBg = attached ? "rgba(37,99,235,0.10)" : "rgba(148,163,184,0.18)";
//    String stateColor = attached ? BRAND : "#64748B";
//
//    return """
//      <td width="33.33%%" valign="top" style="padding:6px;">
//        <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%%"
//               style="border:1px solid %1$s;border-radius:12px;overflow:hidden;">
//          <tr>
//            <td style="padding:12px 12px 8px;">
//              <div style="font-family:Segoe UI,Roboto,Apple SD Gothic Neo,Noto Sans KR,Arial,sans-serif;
//                          font-size:12px;color:%2$s;font-weight:700;margin:0 0 6px;">
//                %3$s
//              </div>
//
//              <span style="display:inline-block;padding:4px 10px;border-radius:999px;background:%4$s;color:%5$s;
//                           font-family:Segoe UI,Roboto,Apple SD Gothic Neo,Noto Sans KR,Arial,sans-serif;
//                           font-size:11px;">
//                %6$s
//              </span>
//
//              <div style="margin-top:10px;font-family:ui-monospace, SFMono-Regular, Menlo, Consolas, 'Roboto Mono', monospace;
//                          font-size:11px;line-height:1.5;color:%7$s;word-break:break-all;">
//                %8$s
//              </div>
//
//              <div style="margin-top:10px;font-family:Segoe UI,Roboto,Apple SD Gothic Neo,Noto Sans KR,Arial,sans-serif;
//                          font-size:11px;line-height:1.6;color:%7$s;">
//                메일 첨부에서 열기
//              </div>
//            </td>
//          </tr>
//        </table>
//      </td>
//    """.formatted(
//        CARD_BORDER,
//        TEXT,
//        escapeHtml(title),
//        stateBg,
//        stateColor,
//        escapeHtml(state),
//        MUTED,
//        escapeHtml(filename)
//    );
//  }
//
//  private String safeName(MultipartFile f, String fallback) {
//    if (f == null || f.isEmpty()) return fallback;
//    String n = f.getOriginalFilename();
//    if (n == null || n.isBlank()) return fallback;
//    return n.trim();
//  }
//
//  private String escapeHtml(String s) {
//    if (s == null) return "";
//    return s.replace("&", "&amp;")
//        .replace("<", "&lt;")
//        .replace(">", "&gt;")
//        .replace("\"", "&quot;")
//        .replace("'", "&#39;");
//  }
//
//  private String viewModeLabel(String v) {
//    if (v == null) return "지점별";
//    return switch (v.trim()) {
//      case "BRANCH" -> "지점별";
//      case "TEAM" -> "팀별";
//      case "PERSON" -> "개인별";
//      case "MULTI"  -> "통합(지점/팀/개인)";
//      default -> v;
//    };
//  }
//
//  private String fmtRange(String from, String to) {
//    String f = (from == null || from.isBlank()) ? "-" : from.trim();
//    String t = (to == null || to.isBlank()) ? "-" : to.trim();
//    return f + " ~ " + t;
//  }
//
//  private String nvl(String s) { return (s == null) ? "" : s.trim(); }
//}
package com.blue.sales.service;

import com.blue.auth.dto.MailAttachment;
import com.blue.auth.service.AsyncMailService;
import com.blue.sales.dto.SalesSnapshotMeta;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SalesMailService {
  
  private final AsyncMailService asyncMailService;
  
  private static final String BRAND = "#2563EB";
  private static final String BG = "#F5F7FB";
  private static final String CARD_BORDER = "#E8EEF7";
  private static final String TEXT = "#0F172A";
  private static final String MUTED = "#64748B";
  private static final String SOFT = "#EEF2F7";
  
  public void sendSalesSnapshotBytes(
      String toEmail,
      String subject,
      String plainText,
      String capturedAtStr,
      String scheduleTitle,
      SalesSnapshotMeta meta,
      byte[] img1, String name1,
      byte[] img2, String name2,
      byte[] img3, String name3
  ) {
    List<MailAttachment> attachments = new ArrayList<>(3);
    
    attachments.add(MailAttachment.builder()
        .filename(name1).contentType("image/png").bytes(img1).build());
    attachments.add(MailAttachment.builder()
        .filename(name2).contentType("image/png").bytes(img2).build());
    attachments.add(MailAttachment.builder()
        .filename(name3).contentType("image/png").bytes(img3).build());
    
    String preheader = "지점별, 팀별, 개인별 스냅샷 이미지가 첨부되었습니다.";
    String html = buildSnapshotHtml(subject, capturedAtStr, scheduleTitle, meta, preheader);
    
    asyncMailService.sendHtmlWithLogoAndAttachments(toEmail, subject, html, plainText, attachments);
  }
  
  public void sendSalesSnapshot(
      String toEmail,
      String subject,
      String plainText,
      String capturedAtStr,
      String scheduleTitle,
      SalesSnapshotMeta meta,
      MultipartFile month,
      MultipartFile range,
      MultipartFile all
  ) {
    List<MailAttachment> attachments = new ArrayList<>(3);
    add(attachments, month, "매출현황_이번달.png");
    add(attachments, range, "매출현황_기간별.png");
    add(attachments, all, "매출현황_전체.png");
    
    String preheader = "이번달, 기간별, 전체 스냅샷 이미지가 첨부되었습니다.";
    String html = buildSnapshotHtml(subject, capturedAtStr, scheduleTitle, meta, preheader);
    
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
  
  /**
   * ✅ 메일 HTML 템플릿(단 하나) - Footer는 무조건 별도 <tr>로 분리
   */
  private String buildSnapshotHtml(
      String subject,
      String capturedAtStr,
      String scheduleTitle,
      SalesSnapshotMeta meta,
      String preheader
  ) {
    boolean isScheduled = (scheduleTitle != null && !scheduleTitle.isBlank());
    String badgeText = isScheduled ? "월간 자동 스냅샷" : "수동 스냅샷";
    String headline = isScheduled ? scheduleTitle : "매출현황 스냅샷이 생성되었습니다.";
    
    String escSubject = escapeHtml(nvl(subject));
    String escBadge = escapeHtml(badgeText);
    String escHeadline = escapeHtml(headline);
    String escCaptured = escapeHtml(nvl(capturedAtStr));
    String escPreheader = escapeHtml(nvl(preheader));
    
    String conditionRows = buildConditionRows(meta);
    
    return """
<!doctype html>
<html lang="ko">
<head>
  <meta charset="UTF-8">
  <title>%1$s</title>
  <meta name="viewport" content="width=device-width,initial-scale=1">
</head>

<body style="margin:0;padding:0;background:%2$s;">
  <div style="display:none;max-height:0;overflow:hidden;opacity:0;color:transparent;">
    %3$s
  </div>

  <div style="width:100%%;padding:32px 16px;">
    <table role="presentation" cellspacing="0" cellpadding="0" border="0" align="center" width="100%%"
           style="max-width:640px;margin:0 auto;background:#ffffff;border-radius:16px;overflow:hidden;
                  box-shadow:0 8px 24px rgba(16,24,40,0.06);">

      <!-- Header -->
      <tr>
        <td align="center" style="padding:28px 28px 12px;text-align:center;">
          <img src="cid:logo" alt="MARK CRM" style="display:block;margin:0 auto 10px;height:70px;width:auto;">
          <span style="display:inline-block;margin-top:2px;padding:6px 12px;border-radius:999px;
                       background:rgba(37,99,235,0.10);color:%4$s;
                       font-family:Segoe UI,Roboto,Apple SD Gothic Neo,Noto Sans KR,Arial,sans-serif;
                       font-size:12px;">%5$s</span>
        </td>
      </tr>

      <!-- Title -->
      <tr>
        <td align="center" style="padding:6px 28px 0;text-align:center;">
          <h1 style="margin:0 0 8px;font-family:Segoe UI,Roboto,Apple SD Gothic Neo,Noto Sans KR,Arial,sans-serif;
                     font-size:22px;line-height:1.35;color:%6$s;">%7$s</h1>
          <p style="margin:0 0 2px;font-family:Segoe UI,Roboto,Apple SD Gothic Neo,Noto Sans KR,Arial,sans-serif;
                    font-size:13px;line-height:1.7;color:%8$s;">
            캡처 일시: <strong style="color:%6$s;">%9$s</strong>
          </p>
          <p style="margin:0 0 16px;font-family:Segoe UI,Roboto,Apple SD Gothic Neo,Noto Sans KR,Arial,sans-serif;
                    font-size:12px;line-height:1.7;color:%8$s;">
            첨부된 이미지 파일에서 스냅샷을 확인하세요.
          </p>
        </td>
      </tr>

      <!-- Conditions Card -->
      <tr>
        <td style="padding:0 28px 10px;">
          <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%%"
                 style="background:#ffffff;border:1px solid %10$s;border-radius:12px;overflow:hidden;">
            <tr>
              <td style="padding:14px 16px;border-bottom:1px solid %11$s;">
                <div style="font-family:Segoe UI,Roboto,Apple SD Gothic Neo,Noto Sans KR,Arial,sans-serif;
                            font-size:12px;color:%8$s;letter-spacing:.2px;">
                  조건
                </div>
              </td>
            </tr>

            %12$s

          </table>
        </td>
      </tr>

      <!-- Footer (✅ 무조건 단독 행) -->
      <tr>
        <td align="center" style="padding:18px 12px 28px;text-align:center;">
          <div style="font-family:Segoe UI,Roboto,Apple SD Gothic Neo,Noto Sans KR,Arial,sans-serif;
                      font-size:11px;line-height:16px;mso-line-height-rule:exactly;
                      color:#94A3B8;">
            © 마크CRM. All rights reserved.
          </div>
        </td>
      </tr>

    </table>
  </div>
</body>
</html>
""".formatted(
        escSubject,       // 1
        BG,               // 2
        escPreheader,     // 3
        BRAND,            // 4
        escBadge,         // 5
        TEXT,             // 6
        escHeadline,      // 7
        MUTED,            // 8
        escCaptured,      // 9
        CARD_BORDER,      // 10
        SOFT,             // 11
        conditionRows     // 12
    );
  }
  
  /**
   * %12$s 에 들어갈 row들만 생성
   * - "보기 모드" 같은 개발자 용어 제거
   * - 기간내 -> 기간
   * - ON/OFF -> 포함/미포함
   */
  private String buildConditionRows(SalesSnapshotMeta meta) {
    String modeRaw = nvl(meta != null ? meta.getViewMode() : "");
    String modeLabel = viewModeLabel(modeRaw);
    
    boolean includeResigned = (meta != null && Boolean.TRUE.equals(meta.getIncludeResigned()));
    String resignedText = includeResigned ? "포함" : "미포함";
    
    String rangeText = fmtRange(meta != null ? meta.getDateFrom() : null, meta != null ? meta.getDateTo() : null);
    
    // 마지막 row는 구분선 없애기
    return kvRow("집계 기준", modeLabel, false)
        + kvRow("퇴사자 포함", resignedText, false)
        + kvRow("기간", rangeText, true);
  }
  
  private String kvRow(String k, String v, boolean last) {
    String border = last ? "border-bottom:0;" : ("border-bottom:1px solid " + SOFT + ";");
    
    return """
      <tr>
        <td style="padding:10px 16px;%1$s">
          <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%%"
                 style="border-collapse:collapse;">
            <tr>
              <td width="110" style="vertical-align:top;
                                     font-family:Segoe UI,Roboto,Apple SD Gothic Neo,Noto Sans KR,Arial,sans-serif;
                                     font-size:12px;color:#475569;padding-right:12px;">
                %2$s
              </td>
              <td style="vertical-align:top;
                         font-family:Segoe UI,Roboto,Apple SD Gothic Neo,Noto Sans KR,Arial,sans-serif;
                         font-size:12px;color:#111827;">
                %3$s
              </td>
            </tr>
          </table>
        </td>
      </tr>
    """.formatted(
        border,
        escapeHtml(nvl(k)),
        escapeHtml(nvl(v))
    );
  }
  
  private String viewModeLabel(String v) {
    if (v == null || v.isBlank()) return "통합(지점/팀/개인)";
    return switch (v.trim().toUpperCase()) {
      case "BRANCH" -> "지점별";
      case "TEAM" -> "팀별";
      case "PERSON", "USER" -> "개인별";
      case "MULTI" -> "통합(지점/팀/개인)";
      default -> v.trim();
    };
  }
  
  private String fmtRange(String from, String to) {
    String f = (from == null || from.isBlank()) ? "-" : from.trim();
    String t = (to == null || to.isBlank()) ? "-" : to.trim();
    return f + " ~ " + t;
  }
  
  private String escapeHtml(String s) {
    if (s == null) return "";
    return s.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;");
  }
  
  private String nvl(String s) {
    return (s == null) ? "" : s.trim();
  }
}