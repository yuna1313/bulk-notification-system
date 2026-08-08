package com.notification.mock.dto;

/**
 * 외부 발송사가 발송 요청을 성공 처리했을 때 반환하는 응답 정보를 표현합니다.
 *
 * @param messageId 발송 요청에서 전달받은 메시지 식별자
 * @param status 발송 처리 상태
 * @param providerMessageId mock-provider가 생성한 발송사 메시지 식별자
 */
public record SendResponse(
		String messageId,
		String status,
		String providerMessageId
) {
}
