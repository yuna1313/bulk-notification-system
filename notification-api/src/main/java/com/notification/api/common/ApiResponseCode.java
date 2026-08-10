package com.notification.api.common;

/**
 * API 응답 코드가 제공해야 하는 코드와 메시지 규격입니다.
 */
public interface ApiResponseCode {

	/**
	 * API 응답 코드를 반환합니다.
	 *
	 * @return SUCCESS 또는 FAIL로 끝나는 API 응답 코드
	 */
	String getCode();

	/**
	 * API 응답 메시지를 반환합니다.
	 *
	 * @return 클라이언트에 전달할 API 응답 메시지
	 */
	String getMessage();
}
