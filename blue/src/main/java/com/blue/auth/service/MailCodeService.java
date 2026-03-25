package com.blue.auth.service;

import com.blue.global.exception.AuthException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
// 이메일 인증코드 발송/검증을 담당하는 서비스
public class MailCodeService {
  
  private final StringRedisTemplate redisTemplate;
  private final AsyncMailService asyncMailService;
  
  // 인증코드 유효기간
  private static final long CODE_TTL = 60 * 10; // 10분
  private static final String BRAND = "#2563EB"; // 브랜드 컬러
  private static final java.security.SecureRandom SECURE_RANDOM = new java.security.SecureRandom();
  
  // 인증코드 생성 및 이메일 발송
  public long sendCode(String email) {
    if (email == null || email.isBlank()) {
      throw new AuthException("이메일이 필요합니다.", HttpStatus.BAD_REQUEST);
    }
    
    try {
      // 6자리 난수 생성
      String code = String.format("%06d", SECURE_RANDOM.nextInt(900_000) + 100_000);
      
      // Redis에 "auth:code:이메일" / "code:연장여부" / TTL 형태로 저장 (TTL 후 자동 삭제)
      redisTemplate.opsForValue().set("auth:code:" + email, code + ":0", CODE_TTL, TimeUnit.SECONDS);
      
      String subject = "[메타CRM] 이메일 인증번호 안내";
      String html = buildVerifyHtml(code, (int)(CODE_TTL / 60));
      
      // 요청일시 치환 (KST)
      var now = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Seoul"));
      var nowStr = now.format(java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd (E) HH:mm"));
      html = html.replace("{{REQUEST_DATETIME}}", nowStr);
      
      String text = "인증코드: " + code + "\n10분 안에 입력해주세요.";
      
      // 인라인 로고 포함 버전으로 전송
      asyncMailService.sendHtmlWithLogo(email, subject, html, text);
      
      return CODE_TTL; // 초 단위 TTL 반환
    } catch (Exception e) {
      throw new AuthException("이메일 발송 실패", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
  
  // HTML 템플릿
  private String buildVerifyHtml(String code, int ttlMin) {
    return """
      <!doctype html>
      <html lang="ko">
      <head>
        <meta charset="UTF-8">
        <title>이메일 인증번호</title>
        <meta name="viewport" content="width=device-width,initial-scale=1">
      </head>
      <body style="margin:0;padding:0;background:#F5F7FB;">
        <div style="width:100%%;padding:32px 16px;">
          <table role="presentation" cellspacing="0" cellpadding="0" border="0" align="center" width="100%%" style="max-width:560px;margin:0 auto;background:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 8px 24px rgba(16,24,40,0.06);">
              <tr>
                <td style="padding:28px 28px 12px;text-align:center;">
                  <img src="cid:logo" alt="META CRM" style="display:block;margin:0 auto 10px;height:70px;width:auto;">
                  <span style="display:inline-block;margin-top:2px;padding:6px 12px;border-radius:999px;
                               background:rgba(37,99,235,0.08);color:%1$s;
                               font-family:Segoe UI,Roboto,Apple SD Gothic Neo,Noto Sans KR,Arial,sans-serif;
                               font-size:12px;">이메일 확인</span>
                </td>
              </tr>
              
            <tr>
              <td style="padding:6px 28px 4px;text-align:center;">
                <h1 style="margin:0 0 8px;font-family:Segoe UI,Roboto,Apple SD Gothic Neo,Noto Sans KR,Arial,sans-serif;font-size:22px;line-height:1.35;color:#0F172A;">인증번호를 입력해 주세요</h1>
                <p style="margin:0 0 20px;font-family:Segoe UI,Roboto,Apple SD Gothic Neo,Noto Sans KR,Arial,sans-serif;font-size:14px;line-height:1.7;color:#334155;">
                  아래 <strong>6자리 인증번호</strong>를 <strong>%2$d분</strong> 내에 입력하면 인증이 완료됩니다.
                </p>
              </td>
            </tr>
      
            <!-- 코드 박스 -->
            <tr>
              <td style="padding:0 28px 8px;">
                <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%%" style="max-width:400px;margin:0 auto;background:#F8FAFF;border:1px solid #E6ECFF;border-radius:12px;">
                  <tr>
                    <td style="padding:14px 16px;text-align:center;">
                      <div style="font-family:Segoe UI,Roboto,Apple SD Gothic Neo,Noto Sans KR,Arial,sans-serif;font-size:11px;letter-spacing:.4px;color:#64748B;text-transform:uppercase;">인증번호</div>
                      <!-- 큰 코드 -->
                      <div style="margin-top:8px;font-family:ui-monospace, SFMono-Regular, Menlo, Consolas, 'Roboto Mono', monospace;font-weight:800;font-size:32px;letter-spacing:8px;color:#0B1220;">
                        %3$s
                      </div>
                    </td>
                  </tr>
                </table>
              </td>
            </tr>
      
            <!-- 메타 -->
            <tr>
              <td style="padding:16px 28px 24px;">
                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="border-collapse:collapse;">
                  <tr>
                    <td style="padding:10px 0;border-top:1px solid #EEF2F7;border-bottom:1px solid #EEF2F7;">
                      <div style="display:flex;gap:12px;align-items:center;">
                        <span style="min-width:92px;display:inline-block;font-family:Segoe UI,Roboto,Apple SD Gothic Neo,Noto Sans KR,Arial,sans-serif;font-size:12px;color:#475569;">요청 일시</span>
                        <span style="font-family:Segoe UI,Roboto,Apple SD Gothic Neo,Noto Sans KR,Arial,sans-serif;font-size:12px;color:#111827;">{{REQUEST_DATETIME}}</span>
                      </div>
                    </td>
                  </tr>
                  <tr>
                    <td style="padding:10px 0;border-bottom:1px solid #EEF2F7;">
                      <div style="display:flex;gap:12px;align-items:center;">
                        <span style="min-width:92px;display:inline-block;font-family:Segoe UI,Roboto,Apple SD Gothic Neo,Noto Sans KR,Arial,sans-serif;font-size:12px;color:#475569;">만료 시간</span>
                        <span style="font-family:Segoe UI,Roboto,Apple SD Gothic Neo,Noto Sans KR,Arial,sans-serif;font-size:12px;color:#111827;">%2$d분 후 자동 만료</span>
                      </div>
                    </td>
                  </tr>
                  <tr>
                    <td style="padding:10px 0;">
                      <div style="display:flex;gap:12px;align-items:flex-start;">
                        <span style="min-width:92px;display:inline-block;font-family:Segoe UI,Roboto,Apple SD Gothic Neo,Noto Sans KR,Arial,sans-serif;font-size:12px;color:#475569;">도움말</span>
                        <span style="font-family:Segoe UI,Roboto,Apple SD Gothic Neo,Noto Sans KR,Arial,sans-serif;font-size:12px;color:#111827;">인증을 요청하지 않았다면 본 메일을 무시해 주세요.</span>
                      </div>
                    </td>
                  </tr>
                </table>
              </td>
            </tr>
      
            <!-- 푸터 -->
            <tr>
              <td style="padding:18px 12px 28px;text-align:center;">
                <div style="font-family:Segoe UI,Roboto,Apple SD Gothic Neo,Noto Sans KR,Arial,sans-serif;font-size:11px;color:#94A3B8;">© 메타CRM. All rights reserved.</div>
              </td>
            </tr>
          </table>
        </div>
      </body>
      </html>
    """.formatted(BRAND, ttlMin, code);
  }
  
  // 인증코드 검증
  public boolean verifyCode(String email, String code) {
    String key = "auth:code:" + email;
    String saved = redisTemplate.opsForValue().get(key);
    
    if (saved == null) {
      throw new AuthException("인증코드가 존재하지 않거나 만료되었습니다.", HttpStatus.GONE);
    }
    
    String savedCode = saved.split(":")[0];
    if (!savedCode.equals(code)) {
      throw new AuthException("인증코드가 일치하지 않습니다.", HttpStatus.UNPROCESSABLE_ENTITY);
    }
    
    // 인증 성공시 코드 삭제
    redisTemplate.delete(key);
    return true;
  }
  
  // 시간 연장 (1회 한정)
  public long extendTime(String email) {
    String key = "auth:code:" + email;
    String saved = redisTemplate.opsForValue().get(key);
    
    if (saved == null) {
      throw new AuthException("인증코드가 존재하지 않거나 만료되었습니다.", HttpStatus.GONE);
    }
    
    String[] parts = saved.split(":");
    String code = parts[0];
    String extendedFlag = parts[1];
    
    // 이미 연장한 경우
    if ("1".equals(extendedFlag)) {
      throw new AuthException("이미 연장하였습니다.", HttpStatus.BAD_REQUEST);
    }
    
    // 연장 처리 (남은 시간과 무관하게 리셋)
    redisTemplate.opsForValue().set(key, code + ":1", CODE_TTL, TimeUnit.SECONDS);
    return CODE_TTL;
  }
  
  // 남은 인증시간 확인
  public Long getRemainingTime(String email) {
    String key = "auth:code:" + email;
    
    // 남은 TTL(초 단위) 조회, 없으면 -2 (키 없음) 또는 -1 (만료시간 없음)
    long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
    
    if (ttl <= 0) {
      throw new AuthException("인증코드가 존재하지 않거나 만료되었습니다.", HttpStatus.GONE);
    }
    return ttl;
  }
}