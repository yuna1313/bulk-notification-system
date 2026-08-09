package com.notification.api.dto;

/**
 * 접수된 발송 요청의 식별자와 규모를 표현합니다.
 *
 * @param notificationId 접수된 발송 요청 식별자
 * @param recipientCount 접수된 수신자 수
 */
public record NotificationCreateResponse(
		Long notificationId,
		int recipientCount
) {
}
