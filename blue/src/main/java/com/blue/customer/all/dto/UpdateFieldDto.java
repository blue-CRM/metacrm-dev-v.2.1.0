package com.blue.customer.all.dto;

import lombok.Data;

/**
 * all.vue 테이블의 "인라인 편집" 패치용 DTO.
 *
 * <p>사용 맥락</p>
 * <ul>
 *   <li>PATCH /api/super/db/all/update/{id}</li>
 *   <li>지원 필드:
 *     <ul>
 *       <li><b>reservation</b> : 약속시간(달력) 인라인 변경. 값 예) "2025-09-21 14:00"</li>
 *       <li><b>status</b>      : 상태 배지 인라인 변경. 값 예) "부재1" / "재콜" / "완료" 등</li>
 *     </ul>
 *   </li>
 *   <li>중복(DUPLICATE) 행은 <b>수정 불가</b> (서비스 레이어에서 차단).</li>
 *   <li>향후 확장 시 field 값만 추가하면 동일 엔드포인트로 인라인 변경을 일원화 가능.</li>
 * </ul>
 */
@Data
public class UpdateFieldDto {
  /** 예: "reservation" | "status" | "name" */
  private String field;
  
  /** 값. reservation: "YYYY-MM-DD HH:mm", status: 상태 문자열, name: 고객명 */
  private String value;
}
