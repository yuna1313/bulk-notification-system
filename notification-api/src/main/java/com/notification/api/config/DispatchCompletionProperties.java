package com.notification.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 발송 완료 판정 설정입니다.
 *
 * @param enabled 판정 주기 실행 여부. 테스트는 주기 실행이 끼어들지 않도록 끕니다
 * @param intervalMillis 판정 주기. 발송이 끝난 시점과 상태가 바뀌는 시점의 최대 간격입니다
 * @param batchSize 한 주기에 확인할 발송 요청 수
 */
@ConfigurationProperties(prefix = "notification.dispatch.completion")
public record DispatchCompletionProperties(
		boolean enabled,
		long intervalMillis,
		int batchSize
) {
}
