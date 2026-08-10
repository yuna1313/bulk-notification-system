package com.notification.api.domain;

/**
 * outbox에 쌓인 이벤트의 발행 상태입니다.
 *
 * <p>실제 발송의 성공 여부와는 무관합니다. Kafka로 내보냈는지만 나타냅니다.
 * 발송 결과는 {@link MessageStatus}가 따로 관리합니다.
 */
public enum OutboxStatus {

	/** 아직 Kafka로 발행하지 않은 상태입니다. poller가 집어갈 대상입니다. */
	PENDING,

	/** Kafka로 발행을 마친 상태입니다. */
	PUBLISHED
}
