package com.notification.api.controller;

import com.notification.api.common.ApiResponse;
import com.notification.api.common.NotificationResponseCode;
import com.notification.api.dto.NotificationCreateResponse;
import com.notification.api.dto.TestNotificationCreateRequest;
import com.notification.api.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 부하 테스트 전용 발송 요청 접수 API를 제공합니다.
 *
 * <p>수신자 10만 명을 요청 본문에 담으면 본문 크기 자체가 측정 변수가 되므로,
 * 수신자 수만 받아 서버가 식별자를 만들어 냅니다. 운영 용도로 쓰지 않습니다.
 */
@Tag(name = "Test Notification", description = "부하 테스트 전용 알림 발송 API")
@RestController
@RequestMapping("/api/test/notifications")
@RequiredArgsConstructor
public class TestNotificationController {

	private final NotificationService notificationService;

	/**
	 * 수신자 수만 받아 발송 요청을 접수합니다.
	 *
	 * @param request 부하 테스트용 발송 요청 접수 정보
	 * @return 접수된 발송 요청 식별자와 수신자 수가 담긴 공통 API 응답
	 */
	@Operation(
			summary = "부하 테스트용 발송 요청 접수",
			description = "수신자 수만 받아 user-1부터 user-N까지 수신자를 생성한 뒤 발송 요청을 저장합니다."
	)
	@PostMapping
	public ResponseEntity<ApiResponse<NotificationCreateResponse>> create(
			@Valid @RequestBody TestNotificationCreateRequest request) {
		NotificationCreateResponse response = notificationService.createWithGeneratedRecipients(request);
		return ResponseEntity
				.status(HttpStatus.CREATED)
				.body(ApiResponse.of(NotificationResponseCode.NOTIFICATION_CREATE_SUCCESS, response));
	}
}
