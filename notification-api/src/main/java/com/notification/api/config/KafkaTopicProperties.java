package com.notification.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 발송 토픽 설정입니다.
 *
 * <p>파티션 수는 부하 테스트에서 바꿔가며 측정할 값이므로 설정으로 분리했습니다.
 *
 * @param name 발송 토픽 이름
 * @param partitions 파티션 수. 동시에 발송할 수 있는 컨슈머 수의 상한이 됩니다.
 * @param replicas 복제 계수. 브로커가 1대이므로 1입니다.
 * @param retentionDays 메시지 보존 기간. 구간 재처리로 되돌아볼 수 있는 범위입니다.
 */
@ConfigurationProperties(prefix = "notification.kafka.send-topic")
public record KafkaTopicProperties(
		String name,
		int partitions,
		short replicas,
		int retentionDays
) {
}
