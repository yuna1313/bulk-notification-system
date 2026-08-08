package com.notification.mock.controller;

import com.notification.mock.common.ApiResponse;
import com.notification.mock.common.SendResponseCode;
import com.notification.mock.dto.SendRequest;
import com.notification.mock.dto.SendResponse;
import com.notification.mock.service.MockSendService;
import com.notification.mock.service.SendResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 외부 발송사 역할을 흉내 내는 발송 API를 제공합니다.
 */
@Tag(name = "Send", description = "mock-provider 발송 API")
@RestController
@RequiredArgsConstructor
public class SendController {

	private final MockSendService sendService;

	/**
	 * 발송 요청을 처리하고 현재 설정에 따라 성공, 실패, 요청 제한 응답을 반환합니다.
	 *
	 * @param request 발송 요청 정보
	 * @return 발송 처리 결과가 담긴 공통 API 응답
	 */
	@Operation(
			summary = "발송 요청 처리",
			description = "실제 발송은 하지 않고 현재 설정된 지연 시간, 실패율, 초당 요청 제한에 따라 응답합니다."
	)
	@PostMapping("/send")
	public ResponseEntity<ApiResponse<SendResponse>> send(@RequestBody SendRequest request) {
		SendResult result = sendService.send(request);
		if (result.isSuccess()) {
			return ResponseEntity.ok(ApiResponse.of(result.responseCode(), result.response()));
		}
		return ResponseEntity
				.status(httpStatus(result.responseCode()))
				.body(ApiResponse.of(result.responseCode(), null));
	}

	private HttpStatus httpStatus(SendResponseCode responseCode) {
		if (responseCode == SendResponseCode.SEND_RATE_LIMIT_FAIL) {
			return HttpStatus.TOO_MANY_REQUESTS;
		}
		return HttpStatus.INTERNAL_SERVER_ERROR;
	}
}
