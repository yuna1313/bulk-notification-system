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

	private final boolean success;
	private final String code;
	private final String message;
	private final T data;

	/**
	 * 성공 API 응답을 생성합니다.
	 *
	 * @param responseCode 성공 응답 코드와 메시지
	 * @param data 응답 데이터
	 * @return success가 true인 공통 API 응답
	 */
	public static <T> ApiResponse<T> success(ApiResponseCode responseCode, T data) {
		return new ApiResponse<>(true, responseCode.getCode(), responseCode.getMessage(), data);
	}

	/**
	 * 실패 API 응답을 생성합니다.
	 *
	 * @param responseCode 실패 응답 코드와 메시지
	 * @param data 응답 데이터
	 * @return success가 false인 공통 API 응답
	 */
	public static <T> ApiResponse<T> fail(ApiResponseCode responseCode, T data) {
		return new ApiResponse<>(false, responseCode.getCode(), responseCode.getMessage(), data);
	}
}
