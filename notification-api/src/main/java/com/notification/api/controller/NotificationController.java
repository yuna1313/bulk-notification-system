package com.notification.api.controller;

import com.notification.api.common.ApiResponse;
import com.notification.api.common.NotificationResponseCode;
import com.notification.api.dto.NotificationCreateRequest;
import com.notification.api.dto.NotificationCreateResponse;
import com.notification.api.dto.NotificationDispatchResponse;
import com.notification.api.dto.NotificationStatusResponse;
import com.notification.api.service.NotificationDispatchService;
import com.notification.api.service.NotificationQueryService;
import com.notification.api.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
	private final NotificationDispatchService dispatchService;
	private final NotificationQueryService queryService;

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

	/**
	 * 접수된 발송 요청을 발송 대기열에 올립니다.
	 *
	 * <p>응답 시점에는 아직 발송되지 않았으므로 200이 아니라 202로 응답합니다.
	 * 진행 상황은 발송 현황 조회 API로 확인해야 합니다.
	 *
	 * @param id 발송 요청 식별자
	 * @return 대기열에 올린 건수와 접수 소요시간이 담긴 공통 API 응답
	 */
	@Operation(
			summary = "발송 실행",
			description = "수신자별 발송 지시를 대기열에 쌓습니다. 실제 발송은 워커가 이어서 처리하므로 응답 시점에는 아직 발송되지 않았습니다."
	)
	@PostMapping("/{id}/dispatch")
	public ResponseEntity<ApiResponse<NotificationDispatchResponse>> dispatch(@PathVariable Long id) {
		NotificationDispatchResponse response = dispatchService.dispatch(id);
		return ResponseEntity
				.status(HttpStatus.ACCEPTED)
				.body(ApiResponse.of(NotificationResponseCode.NOTIFICATION_DISPATCH_SUCCESS, response));
	}

	/**
	 * 발송 요청의 진행 현황을 조회합니다.
	 *
	 * @param id 발송 요청 식별자
	 * @return 성공, 실패, 대기 건수가 담긴 공통 API 응답
	 */
	@Operation(
			summary = "발송 현황 조회",
			description = "발송 요청의 상태와 성공, 실패, 대기 건수를 조회합니다. 발송 도중에도 진행 상황을 볼 수 있습니다."
	)
	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<NotificationStatusResponse>> getStatus(@PathVariable Long id) {
		NotificationStatusResponse response = queryService.getStatus(id);
		return ResponseEntity.ok(
				ApiResponse.of(NotificationResponseCode.NOTIFICATION_GET_SUCCESS, response));
	}
}
