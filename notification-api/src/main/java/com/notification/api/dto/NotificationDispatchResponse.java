package com.notification.api.dto;

/**
 * 발송 접수 결과를 표현합니다.
 *
 * <p>응답 시점에는 아직 아무것도 발송되지 않았습니다. 발송 지시를 outbox에 쌓았을 뿐이며
 * 실제 발송은 워커가 이어서 처리합니다. 그래서 v1과 달리 성공, 실패 건수를 담지 않습니다.
 * 진행 상황은 발송 현황 조회 API로 확인해야 합니다.
 *
 * @param notificationId 발송 요청 식별자
 * @param queuedCount outbox에 쌓은 발송 지시 건수
 * @param elapsedMillis 접수에 걸린 시간. 발송에 걸린 시간이 아닙니다
 */
public record NotificationDispatchResponse(
		Long notificationId,
		int queuedCount,
		long elapsedMillis
) {
}
