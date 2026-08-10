package com.notification.api.dto;

/**
 * 발송 실행 결과를 표현합니다.
 *
 * <p>일부 수신자에 대한 발송이 실패해도 발송 작업 자체는 수행된 것이므로 성공 응답에 담습니다.
 *
 * @param notificationId 발송 요청 식별자
 * @param totalCount 전체 발송 건수
 * @param successCount 발송에 성공한 건수
 * @param failCount 발송에 실패한 건수
 * @param elapsedMillis 발송 시작부터 종료까지 걸린 시간
 */
public record NotificationDispatchResponse(
		Long notificationId,
		int totalCount,
		int successCount,
		int failCount,
		long elapsedMillis
) {
}
