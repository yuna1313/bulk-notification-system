package com.notification.mock.controller;

import com.notification.mock.common.ApiResponse;
import com.notification.mock.common.ProviderConfigResponseCode;
import com.notification.mock.dto.ProviderConfig;
import com.notification.mock.dto.ProviderConfigRequest;
import com.notification.mock.service.ProviderConfigStore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * mock-provider의 동작 설정을 조회하는 API를 제공합니다.
 */
@Slf4j
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

	/**
	 * mock-provider 설정값을 변경합니다.
	 *
	 * @param request 변경할 설정값이 담긴 요청 정보
	 * @return 변경 후 적용된 mock-provider 설정값
	 */
	@Operation(
			summary = "mock-provider 설정 변경",
			description = "요청에 포함된 필드만 변경하고, 누락된 필드는 기존 설정값을 유지합니다."
	)
	@PutMapping("/config")
	public ApiResponse<ProviderConfig> updateConfig(@RequestBody ProviderConfigRequest request) {
		ProviderConfig updatedConfig = configStore.update(request);
		log.info(
				"[updateConfig] mock-provider 설정 변경: latencyMs={}, failureRate={}, rateLimitPerSecond={}",
				updatedConfig.latencyMs(),
				updatedConfig.failureRate(),
				updatedConfig.rateLimitPerSecond()
		);
		return ApiResponse.success(ProviderConfigResponseCode.CONFIG_UPDATE_SUCCESS, updatedConfig);
	}
}
