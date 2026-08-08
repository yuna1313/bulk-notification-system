package com.notification.mock.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * mock-provider 설정 API에서 사용하는 응답 코드와 메시지를 관리합니다.
 */
@Getter
@RequiredArgsConstructor
public enum ProviderConfigResponseCode implements ApiResponseCode {

	CONFIG_GET_SUCCESS("CONFIG_GET_SUCCESS", "mock-provider 설정 조회를 성공하였습니다."),
	CONFIG_UPDATE_SUCCESS("CONFIG_UPDATE_SUCCESS", "mock-provider 설정 변경을 성공하였습니다.");

	private final String code;
	private final String message;
}
