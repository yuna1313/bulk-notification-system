package com.notification.mock.common;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 모든 API가 동일한 JSON 구조로 응답하기 위한 공통 응답 객체입니다.
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiResponse<T> {

	private final String code;
	private final String message;
	private final T data;

	/**
	 * API 응답을 생성합니다.
	 *
	 * @param responseCode 응답 코드와 메시지
	 * @param data 응답 데이터
	 * @return 코드, 메시지, 데이터가 담긴 공통 API 응답
	 */
	public static <T> ApiResponse<T> of(ApiResponseCode responseCode, T data) {
		return new ApiResponse<>(responseCode.getCode(), responseCode.getMessage(), data);
	}
}
