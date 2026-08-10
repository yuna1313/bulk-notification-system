package com.notification.worker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 발송 실패 시 재시도 설정입니다.
 *
 * @param intervalMillis 재시도 사이에 기다리는 시간. 이 시간 동안 해당 파티션은 멈춰 있습니다
 * @param maxRetries 최초 시도 이후 추가로 시도할 횟수. 2면 최초 1회 + 재시도 2회로 총 3회입니다
 */
@ConfigurationProperties(prefix = "notification.retry")
public record RetryProperties(
		long intervalMillis,
		long maxRetries
) {
}
