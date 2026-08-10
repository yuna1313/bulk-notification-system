package com.notification.api.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 외부 발송사가 사용하는 공통 응답 구조를 표현합니다.
 *
 * @param code 발송사 응답 코드
 * @param message 발송사 응답 메시지
 * @param data 응답 데이터. 실패 시 비어 있습니다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProviderApiResponse<T>(
		String code,
		String message,
		T data
) {
}
