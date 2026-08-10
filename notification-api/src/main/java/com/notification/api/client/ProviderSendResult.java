package com.notification.api.client;

import com.notification.api.domain.FailureReason;

/**
 * 외부 발송사 호출 한 건의 결과입니다.
 *
 * @param success 발송사가 접수에 성공했는지 여부
 * @param providerMessageId 발송사가 생성한 메시지 식별자. 실패 시 null입니다.
 * @param failureReason 실패 사유. 성공 시 null입니다.
 */
public record ProviderSendResult(
		boolean success,
		String providerMessageId,
		FailureReason failureReason
) {

	/**
	 * 발송 성공 결과를 생성합니다.
	 *
	 * @param providerMessageId 발송사가 생성한 메시지 식별자
	 * @return 성공 결과
	 */
	public static ProviderSendResult success(String providerMessageId) {
		return new ProviderSendResult(true, providerMessageId, null);
	}

	/**
	 * 발송 실패 결과를 생성합니다.
	 *
	 * @param failureReason 실패 사유
	 * @return 실패 결과
	 */
	public static ProviderSendResult fail(FailureReason failureReason) {
		return new ProviderSendResult(false, null, failureReason);
	}
}
