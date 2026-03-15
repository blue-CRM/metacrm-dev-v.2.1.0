package com.blue.sales.dto;

import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;

@Component
public class SalesSnapshotDashboardRenderer {
  
  private static final int W = 1600;
  private static final int H = 900;
  
  private static final Color BG = new Color(0xF5, 0xF7, 0xFB);
  private static final Color CARD = Color.WHITE;
  private static final Color BORDER = new Color(0xE5, 0xE7, 0xEB);
  private static final Color TEXT = new Color(0x11, 0x18, 0x27);
  private static final Color MUTED = new Color(0x6B, 0x72, 0x80);
  
  private static final Color TRACK = new Color(0xF3, 0xF4, 0xF6);
  private static final Color INIT = new Color(0x3B, 0x82, 0xF6);   // 프론트: #3B82F6
  private static final Color UPSELL = new Color(0x22, 0xC5, 0x5E); // 프론트: #22C55E
  private static final Color TOP_ROW_BG = new Color(0xEC, 0xF6, 0xFF);
  
  private static final Font H1 = new Font(Font.SANS_SERIF, Font.BOLD, 22);
  private static final Font H2 = new Font(Font.SANS_SERIF, Font.BOLD, 16);
  private static final Font BODY = new Font(Font.SANS_SERIF, Font.PLAIN, 14);
  private static final Font SMALL = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
  private static final Font BOLD = new Font(Font.SANS_SERIF, Font.BOLD, 14);
  
  private static final String DEFAULT_OVER_PARTITION_COLOR = "#f9bf31";
  
  private static final NumberFormat MONEY;
  static {
    MONEY = NumberFormat.getCurrencyInstance(Locale.US);
    MONEY.setMaximumFractionDigits(0);
    MONEY.setMinimumFractionDigits(0);
  }
  
  public byte[] renderPng(
      String pageTitle,
      String subtitle,
      List<SalesAggRowDto> rows,
      List<Long> partitionStops,
      List<String> partitionColors
  ) {
    try {
      List<Row> safe = normalize(rows);
      safe.sort(Comparator.comparingLong((Row r) -> r.total).reversed());
      
      long maxTotalRaw = safe.stream().mapToLong(r -> r.total).max().orElse(1L);
      long maxValue = (long) Math.ceil(Math.max(1L, maxTotalRaw) * 1.15); // 프론트 maxValueOf 유지
      
      List<Row> top3 = safe.stream().limit(3).toList();
      
      BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
      Graphics2D g = img.createGraphics();
      try {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        g.setColor(BG);
        g.fillRect(0, 0, W, H);
        
        int pad = 28;
        
        // Header
        g.setFont(H1);
        g.setColor(TEXT);
        g.drawString(nvl(pageTitle), pad, 44);
        
        g.setFont(SMALL);
        g.setColor(MUTED);
        g.drawString(nvl(subtitle), pad, 68);
        
        // Cards layout
        int top = 96;
        int gap = 22;
        
        int leftX = pad;
        int leftY = top;
        int leftW = 1030;
        int leftH = H - top - pad;
        
        int rightX = leftX + leftW + gap;
        int rightY = top;
        int rightW = W - rightX - pad;
        int rightH = leftH;
        
        drawCard(g, leftX, leftY, leftW, leftH);
        drawCard(g, rightX, rightY, rightW, rightH);
        
        // Left title
        g.setFont(H2);
        g.setColor(TEXT);
        g.drawString("매출현황", leftX + 22, leftY + 36);
        
        // Bars area metrics
        int listTop = leftY + 56;
        int rowH = 60;
        int visible = Math.min(10, safe.size());
        int labelW = 180;
        
        int barX = leftX + 22 + labelW;
        int barW = leftW - (22 * 2) - labelW - 140;
        int amountX = barX + barW + 16;
        
        // 2줄 바(프론트 h-4 + h-4 느낌)
        int barH = 14;         // 한 줄 높이
        int barGapY = 0;       // 위/아래 줄 간격
        int boxH = barH * 2 + barGapY; // 트랙(컨테이너) 높이
        int boxR = 10;
        
        // 1) 일반 그리드 tick (원하면 유지)
        List<Long> gridTicks = buildGridTicks(maxValue, 5);
        drawGridLines(g, barX, barW, listTop, visible * rowH, maxValue, gridTicks);

        // 2) ✅ 파티션 구분선은 DB stops만 그대로
        drawPartitionLines(g, barX, barW, listTop, visible * rowH, maxValue, partitionStops);
        
        int axisLineY = listTop + visible * rowH + 10;
        drawAxisLine(g, barX, barW, axisLineY);

        // 라벨도 분리 (원하면 grid 라벨은 빼도 됨)
        drawGridLabels(g, barX, barW, axisLineY + 18, maxValue, gridTicks);
        drawPartitionLabels(g, barX, barW, axisLineY + 18, maxValue, partitionStops);
        
        g.setFont(BODY);
        
        for (int i = 0; i < visible; i++) {
          Row r = safe.get(i);
          int y = listTop + i * rowH;
          
          // label
          g.setColor(TEXT);
          g.drawString(ellipsis(g, r.label, labelW - 10), leftX + 22, y + 18);
          
          // track container
          int ty = y + 10;
          g.setColor(TRACK);
          fillRound(g, barX, ty, barW, boxH, boxR);
          g.setColor(BORDER);
          g.draw(new RoundRectangle2D.Double(barX, ty, barW, boxH, boxR, boxR));
          
          // total length relative to maxValue
          int totalLen = (int) Math.round(barW * (r.total / (double) maxValue));
          totalLen = Math.max(0, Math.min(barW, totalLen));
          
          // ─────────────────────────────────────────────
          // 위: PartitionBar (총액 단색 + 파티션 색)
          // ─────────────────────────────────────────────
          int topY = ty;
          if (totalLen > 0) {
            Color bucket = parseHex(bucketColor(r.total, partitionStops, partitionColors));
            g.setColor(bucket);
            // 프론트처럼 오른쪽은 꽉 차지 않으면 각지게(overflow-hidden 느낌)
            fillRound(g, barX, topY, totalLen, barH, 8);
          }
          
          // ─────────────────────────────────────────────
          // 아래: ABStackBar (initial + upsell)
          // ─────────────────────────────────────────────
          int bottomY = ty + barH + barGapY;
          if (totalLen > 0 && r.total > 0) {
            int initLen = (int) Math.round(totalLen * (r.initial / (double) r.total));
            initLen = Math.max(0, Math.min(totalLen, initLen));
            int upsLen = totalLen - initLen;
            
            if (initLen > 0) {
              g.setColor(INIT);
              fillRoundLeft(g, barX, bottomY, initLen, barH, 8, totalLen == initLen);
            }
            if (upsLen > 0) {
              g.setColor(UPSELL);
              fillRoundRight(g, barX + initLen, bottomY, upsLen, barH, 8, initLen == 0);
            }
            
            // 퍼센트 텍스트(프론트처럼 공간 있을 때만)
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
            g.setColor(new Color(255, 255, 255, 230));
            if (initLen >= 44) {
              int pct = (int) Math.round(100.0 * r.initial / (double) r.total);
              drawCentered(g, pct + "%", barX, bottomY, initLen, barH);
            }
            if (upsLen >= 44) {
              int pct = (int) Math.round(100.0 * r.upsell / (double) r.total);
              drawCentered(g, pct + "%", barX + initLen, bottomY, upsLen, barH);
            }
            g.setFont(BODY);
          }
          
          // amount (가운데 정렬 느낌)
          g.setColor(TEXT);
          g.setFont(BOLD);
          g.drawString(MONEY.format(r.total), amountX, y + 28);
          g.setFont(BODY);
        }
        
        // Right card: top3 table
        g.setFont(H2);
        g.setColor(TEXT);
        g.drawString("누적매출액 순위표", rightX + 22, rightY + 36);
        
        int tx = rightX + 22;
        int ty = rightY + 56;
        int tw = rightW - 44;
        int th = 40;
        
        g.setColor(new Color(0xF8, 0xFA, 0xFC));
        g.fillRect(tx, ty, tw, th);
        g.setColor(BORDER);
        g.drawRect(tx, ty, tw, th);
        
        int colRankW = 70;
        int colNameW = tw - colRankW - 170;
        
        g.setFont(SMALL);
        g.setColor(MUTED);
        g.drawString("순위", tx + 14, ty + 26);
        g.drawString("명칭", tx + colRankW + 14, ty + 26);
        g.drawString("누적매출액", tx + colRankW + colNameW + 14, ty + 26);
        
        int trH = 56;
        for (int i = 0; i < top3.size(); i++) {
          Row r = top3.get(i);
          int ry = ty + th + i * trH;
          
          g.setColor(i == 0 ? TOP_ROW_BG : Color.WHITE);
          g.fillRect(tx, ry, tw, trH);
          g.setColor(BORDER);
          g.drawRect(tx, ry, tw, trH);
          
          g.setFont(BOLD);
          g.setColor(TEXT);
          g.drawString(String.valueOf(i + 1), tx + 16, ry + 34);
          
          g.setFont(BODY);
          g.drawString(ellipsis(g, r.label, colNameW - 20), tx + colRankW + 14, ry + 34);
          
          String money = MONEY.format(r.total);
          g.setFont(BOLD);
          int mw = g.getFontMetrics().stringWidth(money);
          g.drawString(money, tx + tw - 14 - mw, ry + 34);
        }
        
      } finally {
        g.dispose();
      }
      
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ImageIO.write(img, "png", baos);
      return baos.toByteArray();
      
    } catch (Exception e) {
      throw new RuntimeException("스냅샷 이미지 렌더링 실패", e);
    }
  }
  
  // ───────── helpers ─────────
  
  private int xOf(int barX, int barW, long value, long maxValue) {
    double ratio = value / (double) maxValue;
    return barX + (int) Math.round(barW * ratio);
  }
  
  private List<Long> buildGridTicks(long maxValue, int targetTicks) {
    if (maxValue <= 1) return List.of();
    long step = niceStep(maxValue, targetTicks);
    List<Long> out = new ArrayList<>();
    for (long v = step; v < maxValue; v += step) out.add(v);
    return out;
  }
  
  private void drawGridLines(Graphics2D g, int barX, int barW, int topY, int height,
                             long maxValue, List<Long> ticks) {
    if (barW <= 0 || maxValue <= 0 || ticks == null) return;
    
    g.setColor(new Color(0xE5, 0xE7, 0xEB)); // 연한 선
    for (Long t : ticks) {
      if (t == null) continue;
      long v = t;
      if (v <= 0 || v >= maxValue) continue;
      int x = xOf(barX, barW, v, maxValue);
      g.drawLine(x, topY, x, topY + height);
    }
  }
  
  private void drawPartitionLines(Graphics2D g, int barX, int barW, int topY, int height,
                                  long maxValue, List<Long> partitionStops) {
    if (barW <= 0 || maxValue <= 0 || partitionStops == null) return;
    
    Stroke old = g.getStroke();
    g.setStroke(new BasicStroke(2f));           // 파티션은 조금 두껍게
    g.setColor(new Color(0xD1, 0xD5, 0xDB));    // 파티션 색
    
    for (Long s : partitionStops) {
      if (s == null) continue;
      long v = s;                                // DB 값 그대로
      if (v <= 0 || v >= maxValue) continue;     // 범위 밖이면 안 보이게
      int x = xOf(barX, barW, v, maxValue);
      g.drawLine(x, topY, x, topY + height);
    }
    
    g.setStroke(old);
  }
  
  private void drawGridLabels(Graphics2D g, int barX, int barW, int textY,
                              long maxValue, List<Long> ticks) {
    if (barW <= 0 || maxValue <= 0 || ticks == null) return;
    
    g.setFont(SMALL);
    g.setColor(new Color(0x9C, 0xA3, 0xAF));
    
    FontMetrics fm = g.getFontMetrics();
    for (Long t : ticks) {
      if (t == null) continue;
      long v = t;
      if (v <= 0 || v >= maxValue) continue;
      
      int x = xOf(barX, barW, v, maxValue);
      String s = MONEY.format(v);
      
      int sw = fm.stringWidth(s);
      int left = x - sw / 2;
      left = Math.max(barX, Math.min(barX + barW - sw, left));
      
      g.drawString(s, left, textY);
    }
  }
  
  private void drawPartitionLabels(Graphics2D g, int barX, int barW, int textY,
                                   long maxValue, List<Long> partitionStops) {
    if (barW <= 0 || maxValue <= 0 || partitionStops == null) return;
    
    g.setFont(SMALL);
    g.setColor(new Color(0x6B, 0x72, 0x80));
    
    FontMetrics fm = g.getFontMetrics();
    for (Long s0 : partitionStops) {
      if (s0 == null) continue;
      long v = s0;                                // DB 값 그대로
      if (v <= 0 || v >= maxValue) continue;      // 범위 밖이면 안 보이게
      
      int x = xOf(barX, barW, v, maxValue);
      String s = MONEY.format(v);
      
      int sw = fm.stringWidth(s);
      int left = x - sw / 2;
      left = Math.max(barX, Math.min(barX + barW - sw, left));
      
      g.drawString(s, left, textY);
    }
  }
  
  private record Row(String label, long initial, long upsell, long total) {}

  private void drawAxisLine(Graphics2D g, int barX, int barW, int y) {
    g.setColor(new Color(0xE5, 0xE7, 0xEB));
    g.drawLine(barX, y, barX + barW, y);
  }
  
  private List<Row> normalize(List<SalesAggRowDto> rows) {
    List<Row> out = new ArrayList<>();
    if (rows == null) return out;
    
    for (SalesAggRowDto r : rows) {
      if (r == null) continue;
      
      String label = (r.getLabel() == null || r.getLabel().isBlank()) ? "미지정" : r.getLabel().trim();
      
      Long initObj = r.getA();
      Long upsObj  = r.getB();
      Long totObj  = r.getTotal();
      
      long init = initObj == null ? 0L : initObj;
      long ups  = upsObj  == null ? 0L : upsObj;
      long tot  = (totObj == null) ? (init + ups) : totObj;
      
      if (tot < 0) tot = 0;
      if (init < 0) init = 0;
      if (ups < 0) ups = 0;
      
      long sum = init + ups;
      if (tot < sum) tot = sum;
      
      out.add(new Row(label, init, ups, tot));
    }
    return out;
  }
  
  private long niceStep(long max, int targetTicks) {
    long m = Math.max(1, max);
    double raw = m / (double) targetTicks;
    double pow = Math.pow(10, Math.floor(Math.log10(raw)));
    double[] candidates = {1, 2, 5, 10};
    for (double c : candidates) {
      if (raw <= c * pow) return (long) Math.ceil(c * pow);
    }
    return (long) Math.ceil(10 * pow);
  }
  
  private String bucketColor(long total, List<Long> stopsRaw, List<String> colorsRaw) {
    long t = Math.max(0, total);
    
    List<Long> stops = new ArrayList<>();
    if (stopsRaw != null) for (Long s : stopsRaw) if (s != null) stops.add(s);
    stops.sort(Long::compareTo);
    
    List<String> colors = (colorsRaw == null) ? List.of() : colorsRaw;
    
    Long lastStop = stops.isEmpty() ? null : stops.get(stops.size() - 1);
    boolean overflow = lastStop != null && t > lastStop;
    
    if (overflow) {
      return (colors.size() > stops.size() ? colors.get(stops.size()) : DEFAULT_OVER_PARTITION_COLOR);
    }
    
    int idx = 0;
    for (; idx < stops.size(); idx++) {
      if (t <= stops.get(idx)) break;
    }
    if (idx < colors.size()) return colors.get(idx);
    if (!colors.isEmpty()) return colors.get(colors.size() - 1);
    return "#D1D5DB";
  }
  
  private Color parseHex(String hex) {
    if (hex == null) return new Color(0xD1, 0xD5, 0xDB);
    String s = hex.trim();
    if (!s.startsWith("#")) return new Color(0xD1, 0xD5, 0xDB);
    s = s.substring(1);
    if (s.length() == 3) {
      char r = s.charAt(0), g = s.charAt(1), b = s.charAt(2);
      s = "" + r + r + g + g + b + b;
    }
    try {
      int rgb = Integer.parseInt(s, 16);
      return new Color((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
    } catch (Exception e) {
      return new Color(0xD1, 0xD5, 0xDB);
    }
  }
  
  private void drawCard(Graphics2D g, int x, int y, int w, int h) {
    g.setColor(CARD);
    fillRound(g, x, y, w, h, 18);
    g.setColor(BORDER);
    g.draw(new RoundRectangle2D.Double(x, y, w, h, 18, 18));
  }
  
  private void fillRound(Graphics2D g, int x, int y, int w, int h, int r) {
    g.fill(new RoundRectangle2D.Double(x, y, w, h, r, r));
  }
  
  private void fillRoundLeft(Graphics2D g, int x, int y, int w, int h, int r, boolean both) {
    if (w <= 0 || h <= 0) return;
    if (both) { fillRound(g, x, y, w, h, r); return; }
    g.fillRoundRect(x, y, w, h, r, r);
    g.fillRect(x + Math.max(0, w - r), y, Math.min(r, w), h);
  }
  
  private void fillRoundRight(Graphics2D g, int x, int y, int w, int h, int r, boolean both) {
    if (w <= 0 || h <= 0) return;
    if (both) { fillRound(g, x, y, w, h, r); return; }
    g.fillRoundRect(x, y, w, h, r, r);
    g.fillRect(x, y, Math.min(r, w), h);
  }
  
  private void drawCentered(Graphics2D g, String s, int x, int y, int w, int h) {
    FontMetrics fm = g.getFontMetrics();
    int sw = fm.stringWidth(s);
    int sh = fm.getAscent();
    int cx = x + (w - sw) / 2;
    int cy = y + (h + sh) / 2 - 2;
    g.drawString(s, cx, cy);
  }
  
  private String ellipsis(Graphics2D g, String s, int maxPx) {
    if (s == null) return "";
    FontMetrics fm = g.getFontMetrics();
    if (fm.stringWidth(s) <= maxPx) return s;
    String dots = "...";
    int dotsW = fm.stringWidth(dots);
    
    int end = s.length();
    while (end > 0 && fm.stringWidth(s.substring(0, end)) + dotsW > maxPx) end--;
    return (end <= 0) ? dots : s.substring(0, end) + dots;
  }
  
  private String nvl(String s) { return s == null ? "" : s; }
}