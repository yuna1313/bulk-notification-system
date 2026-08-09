package com.notification.api.client.dto;

/**
 * 외부 발송사가 발송 성공 시 반환하는 응답 정보를 표현합니다.
 *
 * @param messageId 발송 요청에서 전달한 메시지 식별자
 * @param status 발송 처리 상태
 * @param providerMessageId 발송사가 생성한 메시지 식별자
 */
public record ProviderSendResponse(
		String messageId,
		String status,
		String providerMessageId
) {
}
