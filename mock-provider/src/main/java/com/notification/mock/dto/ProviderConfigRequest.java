package com.notification.mock.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * mock-provider 설정 변경 요청 정보를 표현합니다.
 */
@Getter
@NoArgsConstructor
public class ProviderConfigRequest {

	private Long latencyMs;
	private Double failureRate;
	private Integer rateLimitPerSecond;
}
