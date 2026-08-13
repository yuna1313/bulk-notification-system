package com.notification.worker.domain;

/**
 * 발송 건이 실패한 사유입니다.
 *
 * <p>notification-api의 같은 이름 enum과 값이 일치해야 합니다. 두 프로그램이 같은
 * {@code notification_message} 테이블에 쓰기 때문에, 여기에만 있는 값을 저장하면
 * 현황 조회를 담당하는 API가 그 행을 읽지 못합니다.
 */
public enum FailureReason {

	/** 외부 발송사가 발송 실패로 응답한 경우입니다. */
	PROVIDER_FAIL,

	/** 외부 발송사의 초당 요청 한도를 초과한 경우입니다. */
	RATE_LIMIT,

	/** 외부 발송사가 응답 시간 안에 답하지 못해 끊긴 경우입니다. 발송사가 느린 상황입니다. */
	TIMEOUT,

	/**
	 * 외부 발송사에 연결하지 못한 경우입니다.
	 *
	 * <p>발송사가 떠 있지 않거나, 호출하는 쪽의 소켓 자원이 고갈된 상황입니다.
	 * 발송사가 느린 것과는 원인이 다르므로 타임아웃과 구분해 기록합니다.
	 */
	CONNECT_FAIL,

	/** 위 어디에도 해당하지 않는 경우입니다. */
	UNKNOWN
}
