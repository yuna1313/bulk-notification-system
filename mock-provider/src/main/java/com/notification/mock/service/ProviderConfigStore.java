package com.notification.mock.service;

import com.notification.mock.dto.ProviderConfig;
import com.notification.mock.dto.ProviderConfigRequest;
import org.springframework.stereotype.Component;

/**
 * mock-provider 설정값을 메모리에 보관하고 조회하거나 변경합니다.
 */
@Component
public class ProviderConfigStore {

	private volatile ProviderConfig config = ProviderConfig.defaults();

	/**
	 * 현재 mock-provider 설정값을 조회합니다.
	 *
	 * @return 현재 메모리에 저장된 mock-provider 설정값
	 */
	public ProviderConfig get() {
		return config;
	}

	/**
	 * 설정 변경 요청에 포함된 필드만 새 값으로 바꾸고 누락된 필드는 기존 값을 유지합니다.
	 *
	 * @param request 변경할 설정값이 담긴 요청 정보
	 * @return 변경 후 메모리에 저장된 mock-provider 설정값
	 */
	public synchronized ProviderConfig update(ProviderConfigRequest request) {
		config = new ProviderConfig(
				request.getLatencyMs() == null ? config.latencyMs() : request.getLatencyMs(),
				request.getFailureRate() == null ? config.failureRate() : request.getFailureRate(),
				request.getRateLimitPerSecond() == null ? config.rateLimitPerSecond() : request.getRateLimitPerSecond()
		);
		return config;
	}
}
