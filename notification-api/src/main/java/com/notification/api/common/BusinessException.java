package com.notification.api.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 업무 규칙 위반을 알리는 예외입니다.
 *
 * <p>이 예외를 던지면 {@link GlobalExceptionHandler}가 담고 있는 응답 코드와 HTTP 상태로 응답을 만듭니다.
 */
@Getter
public class BusinessException extends RuntimeException {

	private final ApiResponseCode responseCode;
	private final HttpStatus httpStatus;

	/**
	 * 업무 예외를 생성합니다.
	 *
	 * @param responseCode 클라이언트에 전달할 응답 코드와 메시지
	 * @param httpStatus 응답에 사용할 HTTP 상태
	 */
	public BusinessException(ApiResponseCode responseCode, HttpStatus httpStatus) {
		super(responseCode.getMessage());
		this.responseCode = responseCode;
		this.httpStatus = httpStatus;
	}
}
