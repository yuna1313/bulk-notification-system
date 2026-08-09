package com.notification.api.common;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 컨트롤러에서 처리하지 못한 예외를 공통 API 응답 구조로 변환합니다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	/**
	 * 업무 규칙 위반 예외를 예외가 지정한 HTTP 상태로 응답합니다.
	 *
	 * @param exception 발생한 업무 예외
	 * @return 예외가 담고 있는 응답 코드가 실린 공통 API 응답
	 */
	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
		log.warn("업무 예외가 발생하였습니다. code={}", exception.getResponseCode().getCode(), exception);
		return ResponseEntity
				.status(exception.getHttpStatus())
				.body(ApiResponse.of(exception.getResponseCode(), null));
	}

	/**
	 * 요청 본문 검증에 실패한 경우 어떤 필드가 왜 잘못되었는지를 함께 응답합니다.
	 *
	 * @param exception 검증 실패로 발생한 예외
	 * @return 필드명과 실패 사유가 데이터로 담긴 공통 API 응답
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(
			MethodArgumentNotValidException exception) {
		Map<String, String> fieldMessages = new LinkedHashMap<>();
		for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
			fieldMessages.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
		}
		log.warn("요청 검증에 실패하였습니다. fields={}", fieldMessages.keySet());
		return ResponseEntity
				.badRequest()
				.body(ApiResponse.of(CommonResponseCode.INVALID_REQUEST_FAIL, fieldMessages));
	}

	/**
	 * 요청 본문을 읽지 못한 경우 잘못된 요청으로 응답합니다.
	 *
	 * <p>정의되지 않은 enum 값이나 형식이 맞지 않는 날짜가 들어오면 본문 변환 단계에서 실패합니다.
	 *
	 * @param exception 본문 변환 실패로 발생한 예외
	 * @return 잘못된 요청 응답 코드가 실린 공통 API 응답
	 */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiResponse<Void>> handleMessageNotReadableException(
			HttpMessageNotReadableException exception) {
		log.warn("요청 본문을 읽지 못하였습니다.", exception);
		return ResponseEntity
				.badRequest()
				.body(ApiResponse.of(CommonResponseCode.INVALID_REQUEST_FAIL, null));
	}

	/**
	 * 예상하지 못한 예외를 서버 오류로 응답합니다.
	 *
	 * @param exception 발생한 예외
	 * @return 서버 내부 오류 응답 코드가 실린 공통 API 응답
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleException(Exception exception) {
		log.error("처리하지 못한 예외가 발생하였습니다.", exception);
		return ResponseEntity
				.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ApiResponse.of(CommonResponseCode.INTERNAL_ERROR_FAIL, null));
	}
}
