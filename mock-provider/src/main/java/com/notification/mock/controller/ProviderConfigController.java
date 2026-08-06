package com.notification.mock.controller;

import com.notification.mock.common.ApiResponse;
import com.notification.mock.config.ProviderConfig;
import com.notification.mock.config.ProviderConfigResponseCode;
import com.notification.mock.config.ProviderConfigStore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * mock-provider의 동작 설정을 조회하는 API를 제공합니다.
 */
@Tag(name = "ProviderConfig", description = "mock-provider 동작 설정 API")
@RestController
@RequiredArgsConstructor
public class ProviderConfigController {

	private final ProviderConfigStore configStore;

	/**
	 * 현재 mock-provider 설정값을 조회합니다.
	 *
	 * @return 현재 지연 시간, 실패율, 초당 요청 제한 설정값
	 */
	@Operation(
			summary = "mock-provider 설정 조회",
			description = "현재 적용 중인 지연 시간, 실패율, 초당 요청 제한 설정값을 조회합니다."
	)
	@GetMapping("/config")
	public ApiResponse<ProviderConfig> getConfig() {
		return ApiResponse.success(ProviderConfigResponseCode.CONFIG_GET_SUCCESS, configStore.get());
	}
}
