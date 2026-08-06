package com.notification.mock.service;

import com.notification.mock.common.SendResponseCode;
import com.notification.mock.dto.SendResponse;

/**
 * mock-provider 발송 처리 결과를 표현합니다.
 *
 * @param responseCode 발송 처리 결과에 해당하는 응답 코드
 * @param response 성공 시 반환할 발송 응답 정보
 */
public record SendResult(
		SendResponseCode responseCode,
		SendResponse response
) {

	/**
	 * 성공 발송 처리 결과를 생성합니다.
	 *
	 * @param response 발송 성공 응답 정보
	 * @return 성공 응답 코드와 발송 응답 정보가 담긴 처리 결과
	 */
	public static SendResult success(SendResponse response) {
		return new SendResult(SendResponseCode.SEND_SUCCESS, response);
	}

	/**
	 * 실패 발송 처리 결과를 생성합니다.
	 *
	 * @param responseCode 실패 응답 코드
	 * @return 실패 응답 코드와 빈 응답 데이터가 담긴 처리 결과
	 */
	public static SendResult fail(SendResponseCode responseCode) {
		return new SendResult(responseCode, null);
	}

	/**
	 * 발송 처리 성공 여부를 반환합니다.
	 *
	 * @return 성공 응답 코드이면 true, 실패 응답 코드이면 false
	 */
	public boolean isSuccess() {
		return responseCode == SendResponseCode.SEND_SUCCESS;
	}
}
