package com.notification.api.event;

/**
 * 수신자 한 명에게 발송하라는 지시를 담은 이벤트입니다.
 *
 * <p>워커가 이 이벤트만 보고 발송할 수 있도록 필요한 값을 모두 담습니다.
 * 워커가 발송할 때마다 DB를 다시 조회하면 컨슈머를 늘려도 DB가 병목이 되기 때문입니다.
 *
 * <p>대신 같은 발송 요청의 수신자 수만큼 {@code content}가 중복 저장됩니다.
 * 10만 건 발송이면 outbox 테이블과 Kafka 양쪽에 같은 내용이 10만 번 들어갑니다.
 *
 * @param eventId 이벤트 식별자. 워커가 중복 발송을 걸러내는 멱등성 키입니다.
 * @param notificationId 이 발송 건이 속한 발송 요청 식별자
 * @param messageId 수신자별 발송 건 식별자. Kafka 파티션 키로 씁니다.
 * @param recipientId 수신자 식별자
 * @param channel 발송 수단
 * @param content 수신자에게 전달할 내용
 */
public record NotificationSendEvent(
		String eventId,
		Long notificationId,
		Long messageId,
		String recipientId,
		String channel,
		String content
) {
}
