package com.notification.api.domain;

/**
 * 알림을 전달하는 수단입니다.
 *
 * <p>외부 발송사로 보내는 발송 요청의 channel 값으로 그대로 사용합니다.
 */
public enum NotificationChannel {

	SMS,
	EMAIL,
	PUSH
}
