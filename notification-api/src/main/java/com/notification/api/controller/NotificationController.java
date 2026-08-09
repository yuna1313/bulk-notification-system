package com.notification.api.controller;

import com.notification.api.common.ApiResponse;
import com.notification.api.common.NotificationResponseCode;
import com.notification.api.dto.NotificationCreateRequest;
import com.notification.api.dto.NotificationCreateResponse;
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
 * 알림 발송 요청을 접수하는 API를 제공합니다.
 */
@Tag(name = "Notification", description = "알림 발송 API")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

	private final NotificationService notificationService;

	/**
	 * 수신자 목록과 예약 시각을 받아 발송 요청을 접수합니다.
	 *
	 * @param request 발송 요청 접수 정보
	 * @return 접수된 발송 요청 식별자와 수신자 수가 담긴 공통 API 응답
	 */
	@Operation(
			summary = "발송 요청 접수",
			description = "수신자 목록을 받아 발송 요청과 수신자별 발송 건을 저장합니다. 발송은 실행하지 않습니다."
	)
	@PostMapping
	public ResponseEntity<ApiResponse<NotificationCreateResponse>> create(
			@Valid @RequestBody NotificationCreateRequest request) {
		NotificationCreateResponse response = notificationService.create(request);
		return ResponseEntity
				.status(HttpStatus.CREATED)
				.body(ApiResponse.of(NotificationResponseCode.NOTIFICATION_CREATE_SUCCESS, response));
	}
}
