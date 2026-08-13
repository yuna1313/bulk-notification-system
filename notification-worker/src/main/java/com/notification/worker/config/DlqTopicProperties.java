package com.notification.worker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 재시도를 소진한 발송 지시를 모아두는 토픽 설정입니다.
 *
 * <p>이 토픽은 워커만 씁니다. 그래서 발송 토픽과 달리 notification-api가 아니라
 * 워커가 만듭니다.
 *
 * @param name DLQ 토픽 이름
 * @param partitions 파티션 수
 * @param replicas 복제 계수. 브로커가 1대이므로 1입니다
 * @param retentionDays 보존 기간. 실패 원인을 들여다볼 시간이므로 발송 토픽보다 길게 둡니다
 */
@ConfigurationProperties(prefix = "notification.kafka.dlq-topic")
public record DlqTopicProperties(
		String name,
		int partitions,
		short replicas,
		int retentionDays
) {
}
