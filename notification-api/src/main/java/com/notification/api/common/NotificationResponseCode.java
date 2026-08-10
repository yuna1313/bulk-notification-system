package com.notification.api.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 알림 발송 API에서 사용하는 응답 코드와 메시지를 관리합니다.
 */
@Getter
@RequiredArgsConstructor
public enum NotificationResponseCode implements ApiResponseCode {

	NOTIFICATION_CREATE_SUCCESS("NOTIFICATION_CREATE_SUCCESS", "발송 요청을 접수하였습니다."),
	NOTIFICATION_DISPATCH_SUCCESS("NOTIFICATION_DISPATCH_SUCCESS", "발송 처리를 완료하였습니다."),
	NOTIFICATION_GET_SUCCESS("NOTIFICATION_GET_SUCCESS", "발송 현황을 조회하였습니다."),
	NOTIFICATION_NOT_FOUND_FAIL("NOTIFICATION_NOT_FOUND_FAIL", "발송 요청을 찾을 수 없습니다."),
	NOTIFICATION_ALREADY_DISPATCHED_FAIL("NOTIFICATION_ALREADY_DISPATCHED_FAIL", "이미 발송을 시작한 요청입니다.");

	private final String code;
	private final String message;
}
