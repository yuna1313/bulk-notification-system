package com.notification.mock.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 발송 API에서 사용하는 응답 코드와 메시지를 관리합니다.
 */
@Getter
@RequiredArgsConstructor
public enum SendResponseCode implements ApiResponseCode {

	SEND_SUCCESS("SEND_SUCCESS", "발송 요청 처리를 성공하였습니다."),
	SEND_FAIL("SEND_FAIL", "발송 요청 처리에 실패하였습니다."),
	SEND_RATE_LIMIT_FAIL("SEND_RATE_LIMIT_FAIL", "초당 요청 한도를 초과하였습니다.");

	private final String code;
	private final String message;
}
