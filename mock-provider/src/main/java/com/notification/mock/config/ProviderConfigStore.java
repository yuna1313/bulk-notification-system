package com.notification.mock.config;

import org.springframework.stereotype.Component;

/**
 * mock-provider 설정값을 메모리에 보관하고 조회합니다.
 */
@Component
public class ProviderConfigStore {

	private final ProviderConfig config = ProviderConfig.defaults();

	/**
	 * 현재 mock-provider 설정값을 조회합니다.
	 *
	 * @return 현재 메모리에 저장된 mock-provider 설정값
	 */
	public ProviderConfig get() {
		return config;
	}
}
