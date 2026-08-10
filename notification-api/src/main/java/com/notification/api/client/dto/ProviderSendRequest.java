package com.notification.api.client.dto;

/**
 * 외부 발송사로 보내는 발송 요청 정보를 표현합니다.
 *
 * @param messageId 발송 건 식별자
 * @param recipientId 수신자 식별자
 * @param channel 발송 수단
 * @param content 발송 내용
 */
public record ProviderSendRequest(
		String messageId,
		String recipientId,
		String channel,
		String content
) {
}
