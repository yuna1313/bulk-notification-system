package com.notification.api.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 특정 도메인에 속하지 않고 모든 API에서 공통으로 사용하는 응답 코드와 메시지를 관리합니다.
 */
@Getter
@RequiredArgsConstructor
public enum CommonResponseCode implements ApiResponseCode {

	INVALID_REQUEST_FAIL("INVALID_REQUEST_FAIL", "요청 값이 올바르지 않습니다."),
	INTERNAL_ERROR_FAIL("INTERNAL_ERROR_FAIL", "서버 내부 오류가 발생하였습니다.");

	private final String code;
	private final String message;
}
