package com.notification.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * outbox 발행 poller 설정입니다.
 *
 * @param enabled 발행 주기 실행 여부. 테스트는 스케줄러가 멋대로 도는 것을 막기 위해 끕니다
 * @param intervalMillis 발행 주기. 짧을수록 발송이 빨리 시작되지만 빈 조회가 늘어납니다
 * @param batchSize 한 주기에 발행할 최대 건수
 */
@ConfigurationProperties(prefix = "notification.outbox.publish")
public record OutboxPublishProperties(
		boolean enabled,
		long intervalMillis,
		int batchSize
) {
}
