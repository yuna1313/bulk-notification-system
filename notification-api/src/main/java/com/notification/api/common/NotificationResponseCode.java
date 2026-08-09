package com.notification.api.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 알림 발송 API에서 사용하는 응답 코드와 메시지를 관리합니다.
 */
@Getter
@RequiredArgsConstructor
public enum NotificationResponseCode implements ApiResponseCode {

	NOTIFICATION_CREATE_SUCCESS("NOTIFICATION_CREATE_SUCCESS", "발송 요청을 접수하였습니다.");

	private final String code;
	private final String message;
}
